"""Contract-derived black-box cases. No reference implementation imports."""
from __future__ import annotations
import concurrent.futures as cf
import hashlib, json, random, shutil, struct, threading
from pathlib import Path
from .runtime import ROOT, NOW, CheckFailure, Server, build, check, digest_tree, frozen_ok, matches

CASES={}
def case(cid,dim,title,critical=False):
    def register(fn):
        CASES[cid]={'id':cid,'dimension':dim,'title':title,'critical':critical,'fn':fn}
        return fn
    return register

def eq(actual,expected,message=''):
    check(matches(actual,expected),f'{message}: expected {expected!r}, got {actual!r}')

def stock(c,sku='A',total=None,available=None,held=None,sold=None):
    d=c.stock(sku)
    eq(d['sku'],sku)
    for k,v in [('total',total),('available',available),('held',held),('sold',sold)]:
        check(type(d.get(k)) is int and d[k]>=0,'invalid inventory field '+k)
        if v is not None:eq(d[k],v,k)
    eq(d['total'],d['available']+d['held']+d['sold'],'inventory conservation')
    return d

def lines(*pairs):return [{'sku':s,'quantity':q}for s,q in pairs]

def race(functions):
    barrier=threading.Barrier(len(functions))
    def one(f):barrier.wait(timeout=4);return f()
    with cf.ThreadPoolExecutor(max_workers=len(functions))as pool:
        return list(pool.map(one,functions))

def reservation_view(r):
    for k in ('id','requestId','state','createdAt','expiresAt','lines'):check(k in r,'missing reservation field '+k)
    check(isinstance(r['id'],str) and r['id'].isdigit() and int(r['id'])>0,'id must be positive decimal string')
    check(type(r['createdAt'])is int and type(r['expiresAt'])is int,'timestamps must be integers')
    return {k:r[k]for k in ('id','requestId','state','createdAt','expiresAt','lines')}

@case('B01','D01','源码布局与禁止符号链接')
def b01(x):
    check((x.workspace/'src/app/Main.java').is_file(),'missing src/app/Main.java')
    check(not any(p.is_symlink()for p in x.workspace.rglob('*')),'symlink is forbidden')
    check(not any(p.suffix.lower()in ('.jar','.class','.so','.dll','.dylib')for p in x.workspace.rglob('*')),'prebuilt binary is forbidden in submission snapshot')

@case('B02','D01','冻结辅助类保持不变')
def b02(x):check(frozen_ok(x.workspace),'benchkit has been changed or removed')

@case('B03','D01','使用 Java 17 目标从源码干净编译')
def b03(x):check((x.classes/'app/Main.class').is_file(),'missing compiled app.Main')

@case('B04','D01','第二次干净编译产物一致')
def b04(x):
    target=x.artifacts/'rebuild';r=build(x.workspace,target)
    check(r['returncode']==0 and not r['timeout'],r['output'])
    eq(digest_tree(x.classes,'.class'),digest_tree(target,'.class'),'deterministic bytecode')

@case('B05','D01','健康检查与正确 JSON 类型')
def b05(x):
    with x.server()as s:eq(s.client.expect('GET','/health')['status'],'ok')

@case('B06','D01','自定义端口、含空格数据路径可用')
def b06(x):
    with x.server()as s:s.client.item('path_case',3);stock(s.client,'path_case',3,3,0,0)

@case('B07','D01','系统时钟模式禁用测试接口')
def b07(x):
    with x.server(manual=False)as s:s.client.advance(1,404,'NOT_FOUND')

@case('B08','D01','同一数据目录互斥与失败退出')
def b08(x):
    with x.server()as s:
        s.client.item('A',3)
        other=Server(x.classes,s.directory)
        other.expect_start_failure()
        stock(s.client,'A',3,3,0,0)

@case('B09','D01','不同数据目录相互隔离')
def b09(x):
    with x.server()as a,x.server()as b:
        a.client.item('A',3)
        b.client.expect('GET','/v1/items/A',status=404,code='NOT_FOUND')
        b.client.item('A',7);stock(a.client,'A',3,3,0,0);stock(b.client,'A',7,7,0,0)

@case('B10','D01','交付自测入口与工程说明文件')
def b10(x):
    for rel in ('tests/candidate_tests.py','README.md','DESIGN.md','TESTING.md'):
        p=x.workspace/rel;check(p.is_file() and p.stat().st_size>=80,'missing or trivial delivery file '+rel)

@case('F01','D02','创建与查询库存')
def f01(x):
    with x.server()as s:
        d=s.client.item('A',13);eq(d['sku'],'A');stock(s.client,'A',13,13,0,0)

@case('F02','D02','单 SKU 预占与返回结构')
def f02(x):
    with x.server()as s:
        c=s.client;c.item();r=reservation_view(c.reserve(lines=lines(('A',4))))
        eq(r['state'],'ACTIVE');eq(r['requestId'],'k');eq(r['lines'],lines(('A',4)));eq(r['createdAt'],NOW);eq(r['expiresAt'],NOW+10000)
        stock(c,'A',100,96,4,0);eq(reservation_view(c.get(r['id'])),r)

@case('F03','D02','确认预占转为已售')
def f03(x):
    with x.server()as s:
        c=s.client;c.item();r=c.reserve(lines=lines(('A',4)));z=c.action(r['id'],'confirm');eq(z['state'],'CONFIRMED');stock(c,'A',100,96,0,4)

@case('F04','D02','取消预占释放库存')
def f04(x):
    with x.server()as s:
        c=s.client;c.item();r=c.reserve(lines=lines(('A',4)));z=c.action(r['id'],'cancel');eq(z['state'],'CANCELLED');stock(c,'A',100,100,0,0)

@case('F05','D02','多个 SKU 成功并统一排序')
def f05(x):
    with x.server()as s:
        c=s.client;c.item('A',8);c.item('B',9);r=c.reserve(lines=lines(('B',5),('A',3)))
        eq(r['lines'],lines(('A',3),('B',5)));stock(c,'A',8,5,3,0);stock(c,'B',9,4,5,0)
        c.action(r['id'],'confirm');stock(c,'A',8,5,0,3);stock(c,'B',9,4,0,5)

@case('F06','D02','跨 SKU 失败全回滚',True)
def f06(x):
    with x.server()as s:
        c=s.client;c.item('A',8);c.item('B',1)
        c.reserve(lines=lines(('A',3),('B',2)),status=409,code='INSUFFICIENT_STOCK')
        stock(c,'A',8,8,0,0);stock(c,'B',1,1,0,0);eq(c.stats()['reservations'],0)

@case('F07','D02','重复创建不能覆盖原库存')
def f07(x):
    with x.server()as s:
        c=s.client;c.item('A',7);c.expect('POST','/v1/items',{'sku':'A','quantity':99},409,'ITEM_EXISTS');stock(c,'A',7,7,0,0)

@case('F08','D02','不存在的资源返回 404')
def f08(x):
    with x.server()as s:
        c=s.client
        for p in ('/v1/items/missing','/v1/reservations/999'):
            c.expect('GET',p,status=404,code='NOT_FOUND')
        c.action('999','confirm',404,'NOT_FOUND');c.action('999','cancel',404,'NOT_FOUND')
        c.reserve(lines=lines(('missing',1)),status=404,code='NOT_FOUND')

@case('F09','D02','游标分页无重复无遗漏')
def f09(x):
    with x.server()as s:
        c=s.client;c.item();ids=[c.reserve('key'+str(i))['id']for i in range(7)]
        found=[];after='0'
        for _ in range(5):
            p=c.expect('GET','/v1/reservations?limit=2&after='+after)
            check(len(p['items'])<=2,'limit ignored');found.extend(r['id']for r in p['items'])
            if p['nextAfter']is None:break
            eq(p['nextAfter'],p['items'][-1]['id']);after=p['nextAfter']
        eq(found,ids);eq(len(set(found)),7)
        c.action(ids[1],'confirm');c.action(ids[4],'confirm')
        p=c.expect('GET','/v1/reservations?state=CONFIRMED&limit=1');eq([r['id']for r in p['items']],[ids[1]])
        p=c.expect('GET','/v1/reservations?state=CONFIRMED&limit=1&after='+p['nextAfter']);eq([r['id']for r in p['items']],[ids[4]]);eq(p['nextAfter'],None)

@case('F10','D02','统计数据与真实终态一致')
def f10(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('a');b=c.reserve('b');d=c.reserve('d');c.reserve('e',ttl=1)
        c.action(b['id'],'confirm');c.action(d['id'],'cancel');c.advance(1)
        z=c.stats()
        for k,v in {'items':1,'reservations':4,'active':1,'confirmed':1,'cancelled':1,'expired':1,'now':NOW+1}.items():eq(z[k],v,k)
        stock(c,'A',100,98,1,1)

@case('V01','D03','SKU 格式、长度、路径边界')
def v01(x):
    with x.server()as s:
        c=s.client
        for value in ('','a'*33,'../a','a/b','a b','中文','a\n',None,42,True):
            c.expect('POST','/v1/items',{'sku':value,'quantity':1},400,'INVALID_REQUEST')
        c.item('a',1);c.item('a'*32,1)
        eq(c.stats()['items'],2)

@case('V02','D03','数量类型、零、上限与溢出')
def v02(x):
    with x.server()as s:
        c=s.client
        for value in (-1,1000000001,9223372036854775808,True,None,'1',1.0):
            c.expect('POST','/v1/items',{'sku':'A','quantity':value},400,'INVALID_REQUEST')
        c.item('zero',0);c.item('A',1000000000)
        for value in (0,-1,1000000001,True,None,'1',1.5):
            c.reserve('bad',lines(('A',value)),status=400,code='INVALID_REQUEST')
        r=c.reserve('max',lines(('A',1000000000)));stock(c,'A',1000000000,0,1000000000,0);c.action(r['id'],'cancel')

@case('V03','D03','明细空数组、重复项、20 项上限')
def v03(x):
    with x.server()as s:
        c=s.client;c.item()
        bad=[[],lines(('A',1),('A',2)),[{'sku':'A'}],[{'sku':'A','quantity':1,'extra':1}],None,{},lines(*[(str(i),1)for i in range(21)])]
        for value in bad:c.expect('POST','/v1/reservations',{'requestId':'bad','lines':value,'ttlMillis':10},400,'INVALID_REQUEST')
        for i in range(20):c.item('z'+str(i),1)
        r=c.reserve('twenty',lines(*[('z'+str(i),1)for i in range(20)]));eq(len(r['lines']),20)

@case('V04','D03','TTL 的类型、上下界')
def v04(x):
    with x.server()as s:
        c=s.client;c.item()
        for value in (0,-1,86400001,True,None,'1000',1.0,9223372036854775808):c.reserve('bad',ttl=value,status=400,code='INVALID_REQUEST')
        r=c.reserve('good',ttl=86400000);eq(r['expiresAt']-r['createdAt'],86400000)

@case('V05','D03','JSON 语法、重复键、字段完整性')
def v05(x):
    with x.server()as s:
        c=s.client
        for body in ('','{','[]','null','true','{"sku":"A","quantity":1,}', '{"sku":"A","sku":"B","quantity":1}', '{"sku":"A","quantity":1} garbage','{"sku":"A","quantity":1e0}','{"sku":"A","quantity":01}'):
            c.expect('POST','/v1/items',raw=body,status=400,code='INVALID_REQUEST')
        for body in ({},{'sku':'A'},{'quantity':1},{'sku':'A','quantity':1,'extra':2}):c.expect('POST','/v1/items',body,400,'INVALID_REQUEST')
        c.item();r=c.reserve();c.expect('POST',f'/v1/reservations/{r["id"]}/confirm',{'extra':1},400,'INVALID_REQUEST');eq(c.get(r['id'])['state'],'ACTIVE')

@case('V06','D03','分页参数非法、重复与未知参数')
def v06(x):
    with x.server()as s:
        c=s.client
        for query in ('limit=0','limit=101','limit=x','limit=1&limit=2','after=-1','after=x','after=','state=unknown','extra=1'):
            c.expect('GET','/v1/reservations?'+query,status=400,code='INVALID_REQUEST')
        p=c.expect('GET','/v1/reservations?after=999999&limit=100');eq(p,{'items':[],'nextAfter':None})

@case('V07','D03','HTTP 方法与精确路由匹配')
def v07(x):
    with x.server()as s:
        c=s.client
        for method,path in [('GET','/v1/items'),('PUT','/v1/items/A'),('DELETE','/v1/reservations/1'),('POST','/health')]:c.expect(method,path,{},405,'METHOD_NOT_ALLOWED')
        for path in ('/healthz','/v1/items/A/nope','/v1/other','/v1/reservations/1/nope'):c.expect('GET',path,status=404,code='NOT_FOUND')

@case('V08','D03','Content-Type 拒绝与容忍 charset')
def v08(x):
    with x.server()as s:
        c=s.client
        for ct in ('text/plain','','application/xml'):
            c.expect('POST','/v1/items',{'sku':'A','quantity':1},415,'UNSUPPORTED_MEDIA_TYPE',headers={'Content-Type':ct})
        c.expect('POST','/v1/items',{'sku':'A','quantity':1},201,headers={'Content-Type':'application/json; charset=utf-8'})

@case('V09','D03','请求体上限、嵌套深度与非法 UTF-8')
def v09(x):
    with x.server()as s:
        c=s.client
        c.expect('POST','/v1/items',raw=' '*65537,status=413,code='PAYLOAD_TOO_LARGE')
        c.expect('POST','/v1/items',raw='['*70+'0'+']'*70,status=400,code='INVALID_REQUEST')
        c.expect('POST','/v1/items',raw=b'{"sku":"\xff","quantity":1}',status=400,code='INVALID_REQUEST')
        small='{"sku":"A","quantity":1}';c.expect('POST','/v1/items',raw=small+' '*(65536-len(small)),status=201)
        eq(c.stats()['items'],1)

@case('V10','D03','测试令牌与错误响应不泄露内部信息')
def v10(x):
    with x.server()as s:
        c=s.client
        c.expect('POST','/__test__/advance',{'millis':1},403,'FORBIDDEN',headers={'X-Test-Token':'wrong'})
        eq(c.stats()['now'],NOW)
        d=c.expect('POST','/v1/items',raw='{',status=400,code='INVALID_REQUEST')
        text=json.dumps(d)
        for bad in ('java.lang.','Exception at','app.Main','/tmp/','/Users/'):
            check(bad not in text,'internal implementation leaked')

@case('I01','D04','相同请求不重复扣减')
def i01(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('one',lines(('A',3)))
        for _ in range(4):eq(reservation_view(c.reserve('one',lines(('A',3)),status=200)),reservation_view(a))
        stock(c,'A',100,97,3,0);eq(c.stats()['reservations'],1)

@case('I02','D04','等价明细顺序与 JSON 字段顺序')
def i02(x):
    with x.server()as s:
        c=s.client;c.item('A',10);c.item('B',10);a=c.reserve('same',lines(('B',2),('A',1)))
        b=c.expect('POST','/v1/reservations',{'ttlMillis':10000,'lines':lines(('A',1),('B',2)),'requestId':'same'},200);eq(a['id'],b['id']);stock(c,'B',10,8,2,0)

@case('I03','D04','同键异参冲突')
def i03(x):
    with x.server()as s:
        c=s.client;c.item();c.item('B',10);a=c.reserve('same')
        c.reserve('same',lines(('A',2)),status=409,code='IDEMPOTENCY_CONFLICT')
        c.reserve('same',ttl=10001,status=409,code='IDEMPOTENCY_CONFLICT')
        c.reserve('same',lines(('B',1)),status=409,code='IDEMPOTENCY_CONFLICT')
        stock(c,'A',100,99,1,0);eq(c.stats()['reservations'],1)

@case('I04','D04','库存不足失败不占用幂等键')
def i04(x):
    with x.server()as s:
        c=s.client;c.item('A',1);a=c.reserve('owner');c.reserve('retry',status=409,code='INSUFFICIENT_STOCK');c.action(a['id'],'cancel');b=c.reserve('retry');eq(b['state'],'ACTIVE')

@case('I05','D04','资源不存在失败不占用幂等键')
def i05(x):
    with x.server()as s:
        c=s.client;c.reserve('retry',status=404,code='NOT_FOUND');c.item('A',2);c.reserve('retry');stock(c,'A',2,1,1,0)

@case('I06','D04','确认之后重试返回当前终态')
def i06(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('same');c.action(a['id'],'confirm');b=c.reserve('same',status=200);eq(b['id'],a['id']);eq(b['state'],'CONFIRMED');stock(c,'A',100,99,0,1)

@case('I07','D04','取消和过期之后重试不重新激活')
def i07(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('cancel');c.action(a['id'],'cancel');eq(c.reserve('cancel',status=200)['state'],'CANCELLED')
        b=c.reserve('expire',ttl=1);c.advance(1);eq(c.reserve('expire',ttl=1,status=200)['state'],'EXPIRED');stock(c,'A',100,100,0,0)

@case('I08','D04','重启后幂等索引恢复')
def i08(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('same');s.restart(hard=True);b=c.reserve('same',status=200);eq(b['id'],a['id']);stock(c,'A',100,99,1,0)

@case('I09','D04','请求键格式、长度与大小写语义')
def i09(x):
    with x.server()as s:
        c=s.client;c.item()
        for key in ('','x'*65,'bad key','../x',None,1):c.reserve(key,status=400,code='INVALID_REQUEST')
        a=c.reserve('a');b=c.reserve('A');check(a['id']!=b['id'],'requestId must be case sensitive');c.reserve('x'*64);eq(c.stats()['reservations'],3)

@case('I10','D04','重试不续期、不改变创建时间')
def i10(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('same',ttl=10);c.advance(9);b=c.reserve('same',ttl=10,status=200);eq(b['expiresAt'],a['expiresAt']);eq(b['createdAt'],a['createdAt']);c.advance(1);eq(c.get(a['id'])['state'],'EXPIRED')

@case('C01','D05','竞争同一库存不超卖',True)
def c01(x):
    with x.server()as s:
        c=s.client;c.item('A',7)
        res=race([lambda i=i:c.request('POST','/v1/reservations',{'requestId':'r'+str(i),'lines':lines(('A',1)),'ttlMillis':10000})for i in range(32)])
        eq(sum(v[0]==201 for v in res),7)
        for status,d,_ in res:
            check(status in (201,409),str((status,d)))
            if status==409:eq(d['error']['code'],'INSUFFICIENT_STOCK')
        stock(c,'A',7,0,7,0);eq(c.stats()['reservations'],7)

@case('C02','D05','并发相同幂等键只创建一次',True)
def c02(x):
    with x.server()as s:
        c=s.client;c.item('A',100)
        res=race([lambda:c.request('POST','/v1/reservations',{'requestId':'same','lines':lines(('A',1)),'ttlMillis':10000})for _ in range(24)])
        eq(sum(v[0]==201 for v in res),1);eq(sum(v[0]==200 for v in res),23);eq(len({v[1]['id']for v in res}),1);stock(c,'A',100,99,1,0)

@case('C03','D05','逆序多 SKU 加锁与原子守恒',True)
def c03(x):
    with x.server()as s:
        c=s.client;c.item('A',10);c.item('B',10)
        def req(i):return c.request('POST','/v1/reservations',{'requestId':'r'+str(i),'lines':lines(*([('A',1),('B',1)]if i%2 else [('B',1),('A',1)])),'ttlMillis':10000})
        res=race([lambda i=i:req(i)for i in range(24)])
        eq(sum(v[0]==201 for v in res),10);check(all(v[0]in (201,409)for v in res),'unexpected status');stock(c,'A',10,0,10,0);stock(c,'B',10,0,10,0)

@case('C04','D05','重复并发取消只释放一次',True)
def c04(x):
    with x.server()as s:
        c=s.client;c.item();r=c.reserve(lines=lines(('A',7)))
        res=race([lambda:c.action(r['id'],'cancel')for _ in range(24)]);check(all(z['state']=='CANCELLED'for z in res));stock(c,'A',100,100,0,0)

@case('C05','D05','重复并发确认只扣账一次',True)
def c05(x):
    with x.server()as s:
        c=s.client;c.item();r=c.reserve(lines=lines(('A',7)))
        res=race([lambda:c.action(r['id'],'confirm')for _ in range(24)]);check(all(z['state']=='CONFIRMED'for z in res));stock(c,'A',100,93,0,7)

@case('C06','D05','确认与取消竞争只有一个终态',True)
def c06(x):
    with x.server()as s:
        c=s.client;c.item();r=c.reserve(lines=lines(('A',7)))
        res=race([lambda action=action:c.request('POST',f'/v1/reservations/{r["id"]}/{action}',{})for action in ['confirm','cancel']*12])
        state=c.get(r['id'])['state'];check(state in ('CONFIRMED','CANCELLED'),'invalid terminal state')
        for status,d,_ in res:
            if status==200:eq(d['state'],state)
            else:eq(status,409);eq(d['error']['code'],'INVALID_STATE')
        if state=='CONFIRMED':stock(c,'A',100,93,0,7)
        else:stock(c,'A',100,100,0,0)

@case('C07','D05','到期释放与新预占并发守恒',True)
def c07(x):
    with x.server()as s:
        c=s.client;c.item('A',1);r=c.reserve('old',ttl=1)
        funcs=[lambda:c.advance(1)]+[lambda i=i:c.request('POST','/v1/reservations',{'requestId':'r'+str(i),'lines':lines(('A',1)),'ttlMillis':100})for i in range(12)]
        res=race(funcs);eq(res[0]['now'],NOW+1);check(all(v[0]in (201,409)for v in res[1:]),'unexpected reserve status');winners=sum(v[0]==201 for v in res[1:]);check(winners in (0,1),'multiple winners');eq(c.get(r['id'])['state'],'EXPIRED');stock(c,'A',1,1-winners,winners,0)

@case('C08','D05','独立商品并发操作与终态统计')
def c08(x):
    with x.server()as s:
        c=s.client;c.item('A',50);c.item('B',50)
        rs=race([lambda i=i:c.reserve('r'+str(i),lines(('A'if i%2 else 'B',1)))for i in range(24)])
        race([lambda i=i,r=r:c.action(r['id'],'confirm'if i%2 else 'cancel')for i,r in enumerate(rs)])
        stock(c,'A',50,38,0,12);stock(c,'B',50,50,0,0)

@case('C09','D05','并发同键异参返回一致赢家')
def c09(x):
    with x.server()as s:
        c=s.client;c.item()
        res=race([lambda i=i:c.request('POST','/v1/reservations',{'requestId':'same','lines':lines(('A',1+i%2)),'ttlMillis':10000})for i in range(24)])
        created=[d for status,d,_ in res if status==201];eq(len(created),1);winner=created[0]
        for status,d,_ in res:
            if status in (200,201):eq(d['id'],winner['id']);eq(d['lines'],winner['lines'])
            else:eq(status,409);eq(d['error']['code'],'IDEMPOTENCY_CONFLICT')
        q=winner['lines'][0]['quantity'];stock(c,'A',100,100-q,q,0)

@case('C10','D05','并发逻辑时钟增量不丢失')
def c10(x):
    with x.server()as s:
        c=s.client;c.item();r=c.reserve(ttl=10)
        zs=race([lambda:c.advance(1)for _ in range(10)])
        eq(sorted(z['now']for z in zs),list(range(NOW+1,NOW+11)));eq(c.stats()['now'],NOW+10);eq(c.get(r['id'])['state'],'EXPIRED');stock(c,'A',100,100,0,0)

@case('R01','D06','正常重启保留库存')
def r01(x):
    with x.server()as s:s.client.item('A',17);s.restart();stock(s.client,'A',17,17,0,0)

@case('R02','D06','硬终止后恢复完整业务状态',True)
def r02(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('a',lines(('A',2)));b=c.reserve('b',lines(('A',3)));d=c.reserve('d',lines(('A',4)));c.action(b['id'],'confirm');c.action(d['id'],'cancel');s.restart(hard=True)
        eq(c.get(a['id'])['state'],'ACTIVE');eq(c.get(b['id'])['state'],'CONFIRMED');eq(c.get(d['id'])['state'],'CANCELLED');stock(c,'A',100,95,2,3)
    # v1.1: stock released by lazy expiry and re-reserved must replay at its historical time, not at restart time.
    with x.server()as s:
        c=s.client;c.item('B',1);old=c.reserve('old',lines(('B',1)),ttl=1);c.advance(1);stock(c,'B',1,1,0,0);new=c.reserve('new',lines(('B',1)));s.restart(hard=True)
        eq(c.get(old['id'])['state'],'EXPIRED');eq(c.get(new['id'])['state'],'ACTIVE');stock(c,'B',1,0,1,0)

@case('R03','D06','已应答写入紧接硬终止不丢失',True)
def r03(x):
    with x.server()as s:
        c=s.client;c.item('A',100)
        ids=[]
        for i in range(4):
            ids.append(c.reserve('r'+str(i))['id']);s.restart(hard=True)
            eq(c.get(ids[-1])['requestId'],'r'+str(i));stock(c,'A',100,99-i,1+i,0)

@case('R04','D06','硬终止后重试键与终态恢复',True)
def r04(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('same');c.action(a['id'],'confirm');s.restart(hard=True);b=c.reserve('same',status=200);eq(b['id'],a['id']);eq(b['state'],'CONFIRMED');stock(c,'A',100,99,0,1)

@case('R05','D06','逻辑时钟与 ID 分配跨重启单调')
def r05(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('a',ttl=3);c.advance(3);s.restart(hard=True);eq(c.stats()['now'],NOW+3);eq(c.get(a['id'])['state'],'EXPIRED');b=c.reserve('b');check(int(b['id'])>int(a['id']),'id not monotonic');eq(b['createdAt'],NOW+3)
        c.action(b['id'],'confirm');s.now=NOW+20000;s.restart(hard=True);eq(c.get(b['id'])['state'],'CONFIRMED');stock(c,'A',100,99,0,1);eq(c.stats()['now'],NOW+20000)

@case('R06','D06','日志创建失败不污染内存',True)
def r06(x):
    with x.server(extra={'BENCH_FAIL_APPEND_AT':'1'})as s:
        c=s.client;c.expect('POST','/v1/items',{'sku':'A','quantity':2},503,'STORAGE_ERROR');c.expect('GET','/v1/items/A',status=404,code='NOT_FOUND');c.item('A',2);s.restart(hard=True,extra={});stock(c,'A',2,2,0,0)

@case('R07','D06','预占落盘失败可安全原键重试',True)
def r07(x):
    with x.server(extra={'BENCH_FAIL_APPEND_AT':'2'})as s:
        c=s.client;c.item('A',2);c.reserve('retry',status=503,code='STORAGE_ERROR');stock(c,'A',2,2,0,0);eq(c.stats()['reservations'],0);a=c.reserve('retry');s.restart(hard=True,extra={});eq(c.reserve('retry',status=200)['id'],a['id']);stock(c,'A',2,1,1,0)

@case('R08','D06','确认与取消落盘失败保留原状态',True)
def r08(x):
    for action in ('confirm','cancel'):
        with x.server(extra={'BENCH_FAIL_APPEND_AT':'3'})as s:
            c=s.client;c.item('A',2);a=c.reserve();c.action(a['id'],action,503,'STORAGE_ERROR');eq(c.get(a['id'])['state'],'ACTIVE');stock(c,'A',2,1,1,0);c.action(a['id'],action);s.restart(hard=True,extra={});eq(c.get(a['id'])['state'],'CONFIRMED'if action=='confirm'else 'CANCELLED')

@case('R09','D06','尾部不完整帧恢复并可继续追加')
def r09(x):
    for tail in (b'\x00\x00',struct.pack('>II',100,0)+b'partial'):
        with x.server()as s:
            c=s.client;c.item('A',3);a=c.reserve('a');s.stop(hard=True)
            with (s.directory/'events.log').open('ab')as f:f.write(tail)
            s.start();stock(c,'A',3,2,1,0);b=c.reserve('b');s.restart(hard=True);eq(c.get(a['id'])['state'],'ACTIVE');eq(c.get(b['id'])['state'],'ACTIVE');stock(c,'A',3,1,2,0)

@case('R10','D06','完整帧校验失败拒绝静默丢数据',True)
def r10(x):
    s=x.server();s.start();s.client.item();s.stop(hard=True);log=s.directory/'events.log';data=bytearray(log.read_bytes());check(len(data)>9,'journal missing');data[8]^=1;log.write_bytes(data);before=log.read_bytes();s.expect_start_failure();eq(log.read_bytes(),before,'must not discard corrupt complete frames')

@case('T01','D07','到期前一毫秒与恰好到期边界')
def t01(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve(ttl=10);c.advance(9);eq(c.get(a['id'])['state'],'ACTIVE');stock(c,'A',100,99,1,0);c.advance(1);eq(c.get(a['id'])['state'],'EXPIRED');stock(c,'A',100,100,0,0)

@case('T02','D07','最小与最大 TTL 精度')
def t02(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('min',ttl=1);b=c.reserve('max',ttl=86400000);eq(a['expiresAt'],NOW+1);eq(b['expiresAt'],NOW+86400000);c.advance(1);eq(c.get(a['id'])['state'],'EXPIRED');eq(c.get(b['id'])['state'],'ACTIVE')

@case('T03','D07','已过期不能确认或复活',True)
def t03(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve(ttl=1);c.advance(1);c.action(a['id'],'confirm',409,'INVALID_STATE');stock(c,'A',100,100,0,0)

@case('T04','D07','终态非法转换与重复终态操作')
def t04(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('a');c.action(a['id'],'cancel');c.action(a['id'],'confirm',409,'INVALID_STATE');c.action(a['id'],'cancel')
        b=c.reserve('b');c.action(b['id'],'confirm');c.action(b['id'],'cancel',409,'INVALID_STATE');c.action(b['id'],'confirm')
        d=c.reserve('d',ttl=1);c.advance(1);c.action(d['id'],'cancel',409,'INVALID_STATE')
        stock(c,'A',100,99,0,1)

@case('T05','D07','已确认记录不再过期')
def t05(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve(ttl=1);c.action(a['id'],'confirm');c.advance(100);eq(c.get(a['id'])['state'],'CONFIRMED');stock(c,'A',100,99,0,1)

@case('T06','D07','已取消记录不重复释放')
def t06(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve(ttl=1);c.action(a['id'],'cancel');c.advance(100);eq(c.get(a['id'])['state'],'CANCELLED');stock(c,'A',100,100,0,0)

@case('T07','D07','乱序与相同到期时刻')
def t07(x):
    with x.server()as s:
        c=s.client;c.item();ttl=[7,1,11,7];rs=[c.reserve('r'+str(i),ttl=t)for i,t in enumerate(ttl)]
        c.advance(7)
        for r,t in zip(rs,ttl):eq(c.get(r['id'])['state'],'EXPIRED'if t<=7 else 'ACTIVE')
        stock(c,'A',100,99,1,0);eq(c.stats()['expired'],3)

@case('T08','D07','释放的库存可以立即重新预占')
def t08(x):
    with x.server()as s:
        c=s.client;c.item('A',1);old=c.reserve('old',ttl=1);c.advance(1);c.reserve('new');stock(c,'A',1,0,1,0);eq(c.get(old['id'])['state'],'EXPIRED')

@case('T09','D07','分页过滤与统计反映到期状态')
def t09(x):
    with x.server()as s:
        c=s.client;c.item();a=c.reserve('old',ttl=1);c.reserve('new',ttl=100);c.advance(1)
        p=c.expect('GET','/v1/reservations?state=EXPIRED');eq([r['id']for r in p['items']],[a['id']]);eq(c.stats()['active'],1);eq(c.stats()['expired'],1)

@case('T10','D07','时钟输入约束与零增量')
def t10(x):
    with x.server()as s:
        c=s.client
        for value in (-1,86400001,True,None,'1',1.0):c.advance(value,400,'INVALID_REQUEST')
        eq(c.advance(0)['now'],NOW);eq(c.stats()['now'],NOW);eq(c.advance(86400000)['now'],NOW+86400000)

class Oracle:
    """Small sequential model; independent data and transition implementation."""
    def __init__(self):self.now=NOW;self.items={};self.orders={};self.keys={}
    def expire(self):
        for r in self.orders.values():
            if r['state']=='ACTIVE'and r['expiresAt']<=self.now:r['state']='EXPIRED'
    def inventory(self,sku):
        held=sold=0
        for r in self.orders.values():
            for line in r['lines']:
                if line['sku']==sku:
                    if r['state']=='ACTIVE':held+=line['quantity']
                    elif r['state']=='CONFIRMED':sold+=line['quantity']
        total=self.items[sku];return {'sku':sku,'total':total,'available':total-held-sold,'held':held,'sold':sold}
    @staticmethod
    def fingerprint(p):return (p['ttlMillis'],tuple(sorted((d['sku'],d['quantity'])for d in p['lines'])))
    def reserve_expected(self,p):
        key=p['requestId']
        if key in self.keys:
            old,rid=self.keys[key]
            return (200,None,rid)if self.fingerprint(old)==self.fingerprint(p)else (409,'IDEMPOTENCY_CONFLICT',None)
        if any(d['sku']not in self.items for d in p['lines']):return 404,'NOT_FOUND',None
        if any(self.inventory(d['sku'])['available']<d['quantity']for d in p['lines']):return 409,'INSUFFICIENT_STOCK',None
        return 201,None,None
    def accept_reserve(self,p,result):
        rid=result['id'];check(rid not in self.orders,'duplicate new ID')
        if self.orders:check(int(rid)>max(map(int,self.orders)),'new ID not monotonic')
        r={'id':rid,'requestId':p['requestId'],'state':'ACTIVE','createdAt':self.now,'expiresAt':self.now+p['ttlMillis'],'lines':sorted(p['lines'],key=lambda d:d['sku'])}
        self.orders[rid]=r;self.keys[p['requestId']]=(p,rid);eq(reservation_view(result),r)
    def action_expected(self,rid,action):
        if rid not in self.orders:return 404,'NOT_FOUND'
        target='CONFIRMED'if action=='confirm'else 'CANCELLED';old=self.orders[rid]['state']
        if old==target or old=='ACTIVE':self.orders[rid]['state']=target;return 200,None
        return 409,'INVALID_STATE'
    def verify(self,c,full=False,rng=None):
        skus=list(self.items)if full else [rng.choice(list(self.items))]
        for sku in skus:
            d=c.stock(sku);eq({k:d[k]for k in ('sku','total','available','held','sold')},self.inventory(sku),'oracle stock')
        if full:
            for rid,r in self.orders.items():eq(reservation_view(c.get(rid)),r,'oracle reservation')
            z=c.stats();expected={'items':len(self.items),'reservations':len(self.orders),'now':self.now}
            for state in ('ACTIVE','CONFIRMED','CANCELLED','EXPIRED'):expected[state.lower()]=sum(r['state']==state for r in self.orders.values())
            for k,v in expected.items():eq(z[k],v,'oracle stats '+k)

def random_sequence(x,index):
    rng=random.Random(x.seed^(index*0x9e3779b1));m=Oracle()
    with x.server()as s:
        c=s.client
        for i in range(5):
            sku='s'+str(i);q=rng.randint(8,30);c.item(sku,q);m.items[sku]=q
        for step in range(120):
            op=rng.randrange(10)
            if op<5:
                if m.keys and rng.random()<.32:
                    key=rng.choice(list(m.keys));p=json.loads(json.dumps(m.keys[key][0]))
                    if rng.random()<.35:p['ttlMillis']+=1
                    else:p['lines'].reverse()
                else:
                    picks=rng.sample(list(m.items),rng.randint(1,3))
                    if rng.random()<.07:picks[0]='missing'
                    p={'requestId':f'k{index}_{step}','lines':[{'sku':sku,'quantity':rng.randint(1,7)}for sku in picks],'ttlMillis':rng.randint(1,30)}
                status,code,rid=m.reserve_expected(p);result=c.expect('POST','/v1/reservations',p,status,code)
                if status==201:m.accept_reserve(p,result)
                elif status==200:eq(reservation_view(result),m.orders[rid],'oracle replay')
            elif op<8:
                rid=rng.choice(list(m.orders))if m.orders and rng.random()<.9 else '99999999';action=rng.choice(['confirm','cancel']);status,code=m.action_expected(rid,action);result=c.action(rid,action,status,code)
                if status==200:eq(reservation_view(result),m.orders[rid])
            elif op==8:
                delta=rng.randint(0,9);m.now+=delta;m.expire();eq(c.advance(delta)['now'],m.now)
            else:
                state=rng.choice(['ACTIVE','CONFIRMED','CANCELLED','EXPIRED']);p=c.expect('GET','/v1/reservations?state='+state+'&limit=100')
                expected=[r for rid,r in sorted(m.orders.items(),key=lambda p:int(p[0]))if r['state']==state]
                eq([reservation_view(r)for r in p['items']],expected)
            m.verify(c,rng=rng)
            if step==59:s.restart(hard=True);m.verify(c,full=True)
        m.verify(c,full=True)

for _index in range(1,11):
    def generated(x,index=_index):return random_sequence(x,index)
    case(f'O{_index:02d}','D08',f'独立状态机随机序列 {_index}（120 步，含硬重启）')(generated)
