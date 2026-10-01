"""Track t1-spring cases: the HTTP contract cases of the base track are reused unchanged; build,
delivery and journal-specific cases are replaced by Spring/database equivalents."""
from __future__ import annotations
import socket,threading,time
from .cases import CASES as BASE,eq,stock,lines,reservation_view
from .runtime import NOW,Server,check,digest_tree
from . import spring

CASES={cid:dict(spec)for cid,spec in BASE.items()}
def case(cid,dim,title,critical=False):
    def register(fn):
        check(cid in CASES,'replacement must keep the fixed case set: '+cid)
        CASES[cid]={'id':cid,'dimension':dim,'title':title,'critical':critical,'fn':fn}
        return fn
    return register

@case('B01','D01','源码布局、入口类与禁止二进制/符号链接')
def b01(x):
    main=x.workspace/'src/main/java/app/Main.java'
    check(main.is_file(),'missing src/main/java/app/Main.java')
    check('@SpringBootApplication'in main.read_text(encoding='utf-8',errors='replace'),'app.Main must be a @SpringBootApplication')
    check(not any(p.is_symlink()for p in x.workspace.rglob('*')),'symlink is forbidden')
    check(not any(p.suffix.lower()in ('.jar','.class','.so','.dll','.dylib')for p in x.workspace.rglob('*')),'prebuilt binary is forbidden in submission snapshot')

@case('B02','D01','冻结 pom 不变且不占用裁判包名')
def b02(x):check(spring.frozen_ok(x.workspace),'pom.xml differs from the frozen pom, or a benchjudge package is present')

@case('B03','D01','离线 Maven 以 Java 21 干净编译')
def b03(x):check(spring.classes_present(x.classes),'missing compiled app.Main')

@case('B04','D01','Schema 由 Flyway 迁移管理，Hibernate 不自动建表/改表')
def b04(x):
    with x.server()as s:
        info=spring.judge_info(s,'schema')
        check(info.get('flywayMigrations',0)>=1,f'no successful Flyway versioned migration: {info}')
        check(info.get('ddlAuto')in ('none','validate'),f'hibernate.hbm2ddl.auto must be none or validate: {info}')

@case('B10','D01','交付文档与黑盒契约测试入口')
def b10(x):
    for rel in ('README.md','DESIGN.md','TESTING.md'):
        p=x.workspace/rel;check(p.is_file()and p.stat().st_size>=80,'missing or trivial delivery file '+rel)
    tests=list((x.workspace/'src/test/java/contract').rglob('*.java'))
    check(any('@Test'in p.read_text(encoding='utf-8',errors='replace')for p in tests),'no JUnit contract test under src/test/java/contract')

@case('R09','D06','并发写入中硬终止：已应答写入全部恢复且库存守恒',True)
def r09(x):
    with x.server()as s:
        c=s.client;c.item('A',100000);acked={};stop=threading.Event();lock=threading.Lock()
        def writer(w):
            i=0
            while not stop.is_set():
                key=f'w{w}_{i}';q=1+i%3;i+=1
                try:status,d,_=c.request('POST','/v1/reservations',{'requestId':key,'lines':lines(('A',q)),'ttlMillis':86400000})
                except Exception:return
                if status==201:
                    with lock:acked[d['id']]=q
        threads=[threading.Thread(target=writer,args=(w,),daemon=True)for w in range(8)]
        for t in threads:t.start()
        time.sleep(1.5);s.proc.kill();stop.set()
        for t in threads:t.join(timeout=5)
        check(acked,'no reservation acknowledged before the kill')
        s.restart(hard=True)
        for rid,q in acked.items():
            r=c.get(rid);eq(r['state'],'ACTIVE');eq(r['lines'],lines(('A',q)))
        held=0;after='0'
        while True:
            p=c.expect('GET','/v1/reservations?limit=100&after='+after)
            held+=sum(r['lines'][0]['quantity']for r in p['items']if r['state']=='ACTIVE')
            if p['nextAfter']is None:break
            after=p['nextAfter']
        stock(c,'A',100000,100000-held,held,0)

@case('R10','D06','非法启动参数与端口占用必须非 0 退出')
def r10(x):
    for overrides in ({'bench.clock':'bogus'},{'bench.now':'-1'},{'bench.now':'8000000000000001'},{'bench.now':'abc'}):
        s=x.server();s.overrides=overrides;s.expect_start_failure()
    with socket.socket()as busy:
        busy.bind(('127.0.0.1',0));busy.listen(1)
        s=x.server(port=busy.getsockname()[1]);s.expect_start_failure()
    with x.server()as s:eq(s.client.expect('GET','/health')['status'],'ok')
