"""Replace/extend this self-contained black-box test suite.
Only Python standard-library imports are allowed. Accept --base-url.
Exit nonzero on any failed assertion; do not start or inspect the server process.
The starter health check intentionally detects none of the business mutants.
"""
import argparse, json, urllib.request
p=argparse.ArgumentParser();p.add_argument('--base-url',required=True);args=p.parse_args()
with urllib.request.urlopen(args.base_url+'/health',timeout=3) as r:
    assert r.status==200
    assert json.load(r)['status']=='ok'
print(json.dumps({'executed':1,'failed':0}))
