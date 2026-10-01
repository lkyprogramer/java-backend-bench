"""Private self-test exemplar for validating the D10 mechanism, not candidate input."""
import argparse,http.client,json,os,unittest,urllib.parse
parser=argparse.ArgumentParser();parser.add_argument('--base-url',required=True);args,remaining=parser.parse_known_args();url=urllib.parse.urlsplit(args.base_url)

def request(method,path,body=None,status=200):
    conn=http.client.HTTPConnection(url.hostname,url.port,timeout=3)
    try:
        hs={'Content-Type':'application/json','Connection':'close','X-Test-Token':os.environ.get('BENCH_TEST_TOKEN','')}
        conn.request(method,path,json.dumps(body)if body is not None else None,hs)
        r=conn.getresponse();value=json.loads(r.read());assert r.status==status,(status,r.status,value);return value
    finally:conn.close()
def item(sku,q):return request('POST','/v1/items',{'sku':sku,'quantity':q},201)
def stock(sku):return request('GET','/v1/items/'+sku)
def reserve(key,ls,ttl=10000,status=201):return request('POST','/v1/reservations',{'requestId':key,'lines':[{'sku':s,'quantity':q}for s,q in ls],'ttlMillis':ttl},status)
def act(r,action,status=200):return request('POST',f'/v1/reservations/{r["id"]}/{action}',{},status)

class ContractTests(unittest.TestCase):
    def test_no_oversell(self):
        item('m1',1);reserve('m1',[('m1',2)],status=409);self.assertEqual(stock('m1')['available'],1)
    def test_idempotent(self):
        item('m2',10);a=reserve('m2',[('m2',3)]);b=reserve('m2',[('m2',3)],status=200);self.assertEqual(a['id'],b['id']);self.assertEqual(stock('m2')['held'],3)
    def test_conflict(self):
        item('m3',10);reserve('m3',[('m3',1)]);reserve('m3',[('m3',2)],status=409)
    def test_expiry_boundary(self):
        item('m4',2);a=reserve('m4',[('m4',1)],ttl=1);request('POST','/__test__/advance',{'millis':1});self.assertEqual(request('GET','/v1/reservations/'+a['id'])['state'],'EXPIRED');self.assertEqual(stock('m4')['available'],2)
    def test_cancel_once(self):
        item('m5',10);a=reserve('m5',[('m5',3)]);act(a,'cancel');act(a,'cancel');s=stock('m5');self.assertEqual((s['available'],s['held'],s['sold']),(10,0,0))
    def test_confirm_sells(self):
        item('m6',10);a=reserve('m6',[('m6',3)]);act(a,'confirm');s=stock('m6');self.assertEqual((s['available'],s['held'],s['sold']),(7,0,3))
    def test_terminal_transition(self):
        item('m7',10);a=reserve('m7',[('m7',1)]);act(a,'cancel');act(a,'confirm',409)
    def test_batch_rollback(self):
        item('m8a',10);item('m8b',1);reserve('m8',[('m8a',2),('m8b',2)],status=409);self.assertEqual(stock('m8a')['available'],10)
    def test_zero_reservation_invalid(self):
        item('m9',1);reserve('m9',[('m9',0)],status=400)
    def test_state_filter(self):
        item('m10',10);reserve('m10a',[('m10',1)]);b=reserve('m10b',[('m10',1)]);act(b,'confirm');page=request('GET','/v1/reservations?state=CONFIRMED&limit=100');self.assertTrue(page['items']);self.assertTrue(all(r['state']=='CONFIRMED'for r in page['items']))

if __name__=='__main__':unittest.main(argv=['candidate_tests.py']+remaining)
