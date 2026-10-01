"""Frozen public examples. This is not the acceptance suite or D10 test submission."""
import argparse, http.client, json, urllib.parse
p=argparse.ArgumentParser();p.add_argument('--base-url',required=True);a=p.parse_args();url=urllib.parse.urlsplit(a.base_url)
def req(method,path,body=None,expected=200):
    c=http.client.HTTPConnection(url.hostname,url.port,timeout=3)
    try:
        c.request(method,path,json.dumps(body)if body is not None else None,{'Content-Type':'application/json','Connection':'close'})
        r=c.getresponse();data=json.loads(r.read());assert r.status==expected,(r.status,data);return data
    finally:c.close()
assert req('GET','/health')['status']=='ok'
req('POST','/v1/items',{'sku':'A','quantity':10},201)
body={'requestId':'public','lines':[{'sku':'A','quantity':3}],'ttlMillis':10000}
r=req('POST','/v1/reservations',body,201)
assert req('GET','/v1/items/A')['held']==3
assert req('POST','/v1/reservations',body,200)['id']==r['id']
assert req('POST',f'/v1/reservations/{r["id"]}/confirm',{})['state']=='CONFIRMED'
s=req('GET','/v1/items/A');assert (s['available'],s['held'],s['sold'])==(7,0,3)
req('POST','/v1/items',{'sku':'bad','quantity':-1},400)
print('PUBLIC_SMOKE_PASS: examples passed; hidden acceptance is broader.')
