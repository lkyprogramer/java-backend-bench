import copy,json,os,sys,tempfile,time,unittest
from pathlib import Path
from judge.runner import snapshot,manual_review,critical_failures,failure_clusters
from judge.runtime import run_limited
from judge.aggregate import aggregate
from judge.scoring import DIMENSIONS

class SnapshotTests(unittest.TestCase):
    def test_ignores_runtime_data_but_preserves_source_package_data(self):
        with tempfile.TemporaryDirectory() as t:
            base=Path(t);src=base/'src';(src/'src/data').mkdir(parents=True);(src/'data').mkdir()
            (src/'src/data/A.java').write_text('package data; class A {}');(src/'data/journal').write_text('runtime')
            snapshot(src,base/'snapshot')
            self.assertTrue((base/'snapshot/src/data/A.java').exists())
            self.assertFalse((base/'snapshot/data').exists())
    def test_refuses_nested_output(self):
        with tempfile.TemporaryDirectory() as t:
            with self.assertRaises(ValueError):snapshot(t,Path(t)/'nested')
    @unittest.skipUnless(os.name=='posix','symlink test requires POSIX')
    def test_refuses_symlink(self):
        with tempfile.TemporaryDirectory() as t:
            base=Path(t);src=base/'src';src.mkdir();(src/'escape').symlink_to('/etc/passwd')
            with self.assertRaises(ValueError):snapshot(src,base/'snapshot')

class ProcessTests(unittest.TestCase):
    def test_success_output_and_returncode(self):
        r=run_limited([sys.executable,'-c','print("evidence")'],'.',3)
        self.assertFalse(r['timeout']);self.assertEqual(0,r['returncode']);self.assertIn('evidence',r['output'])
    @unittest.skipUnless(os.name=='posix','process group requires POSIX')
    def test_timeout_kills_child_before_delayed_file_write(self):
        with tempfile.TemporaryDirectory() as t:
            marker=Path(t)/'must-not-exist'
            child='import time,pathlib;time.sleep(0.8);pathlib.Path('+repr(str(marker))+').write_text("escaped")'
            parent='import subprocess,sys,time;subprocess.Popen([sys.executable,"-c",'+repr(child)+']);time.sleep(10)'
            r=run_limited([sys.executable,'-c',parent],t,.2);self.assertTrue(r['timeout'])
            time.sleep(.9);self.assertFalse(marker.exists())

class ReviewTests(unittest.TestCase):
    def test_requires_evidence_and_exact_score_types(self):
        with tempfile.TemporaryDirectory() as t:
            path=Path(t)/'review.json';obj={'reviewer':'auditor'}
            for key in ('H01','H02'):obj[key]=[{'score':2,'evidence':'src/app/File.java:10; reviewed'}for _ in range(5)]
            path.write_text(json.dumps(obj));self.assertEqual({'H01':10,'H02':10},manual_review(path)['scores_10'])
            obj['H01'][0]['score']=True;path.write_text(json.dumps(obj))
            with self.assertRaises(ValueError):manual_review(path)
            obj['H01'][0]['score']=2;obj['H01'][0]['evidence']='';path.write_text(json.dumps(obj))
            with self.assertRaises(ValueError):manual_review(path)

def fixture(label='test',score=8,hardware='cpu1',quant='Q4',rankable=True):
    return {'model_label':label,'benchmark_version':'1.0.0','judge_sha256':'j1',
            'environment':{'fingerprint':hardware},'metadata':{'protocol':{'id':'p1'},'model':{'quantization':quant,'generation_seed':1}},
            'java_source_sha256':'source1','validity':{'rankable':rankable},
            'summary':{'official_overall_10':score,'gate':'NO_CRITICAL_FAILURE_OBSERVED'if score else 'NOT_BUILDABLE','dimensions':{d:score for d in DIMENSIONS}}}

class AggregateTests(unittest.TestCase):
    def compare(self,records):
        with tempfile.TemporaryDirectory()as t:
            root=Path(t);paths=[]
            for i,r in enumerate(records):
                p=root/f'{i}.json';p.write_text(json.dumps(r));paths.append(p)
            return aggregate(paths,root/'out')
    def test_splits_same_label_with_different_hardware(self):
        r=self.compare([fixture(),fixture(hardware='cpu2')]);self.assertEqual(2,len(r['groups']));self.assertFalse(r['cross_group_conditions_identical'])
    def test_splits_same_label_with_different_model_quantization(self):
        r=self.compare([fixture(),fixture(quant='Q8')]);self.assertEqual(2,len(r['groups']))
    def test_generation_seed_does_not_split_same_configuration(self):
        a=fixture();b=fixture();b['metadata']['model']['generation_seed']=2;b['java_source_sha256']='source2'
        r=self.compare([a,b]);self.assertEqual(1,len(r['groups']));self.assertEqual(2,r['groups'][0]['attempts'])
    def test_keeps_valid_zero_failures_excludes_infrastructure(self):
        r=self.compare([fixture(score=10),fixture(score=0),fixture(score=0,rankable=False)])
        self.assertEqual(5,r['groups'][0]['mean_10']);self.assertEqual(1,len(r['excluded_invalid_runs']))
    def test_repeated_source_does_not_claim_generation_ci(self):
        r=self.compare([fixture(),fixture(),fixture()]);self.assertIsNone(r['groups'][0]['bootstrap_mean_ci95'])

class GateTests(unittest.TestCase):
    def test_s0_restart_failure_is_critical_even_in_unflagged_case(self):
        rows=[{'id':'O02','status':'fail','critical':False,'severity':'S0','message':'x'},
              {'id':'C01','status':'fail','critical':True,'message':'y'},
              {'id':'F09','status':'fail','critical':False,'message':'z'},
              {'id':'R02','status':'pass','critical':True,'message':'ok'}]
        self.assertEqual(['O02','C01'],critical_failures(rows))
    def test_clusters_mask_paths_and_numbers_but_keep_routes(self):
        rows=[{'id':'O02','status':'fail','message':'replay failed: data-dir=/a/b/c/d/cases/O02/data 1 inconsistency for 15'},
              {'id':'O03','status':'fail','message':'replay failed: data-dir=/a/b/c/d/cases/O03/data 1 inconsistency for 10'},
              {'id':'V05','status':'fail','message':'POST /v1/items: expected 400, got 201'},
              {'id':'B01','status':'pass','message':'ok'}]
        c=failure_clusters(rows)
        self.assertEqual(['O02','O03'],c[0]['cases']);self.assertEqual(1,c[1]['count']);self.assertIn('/v#/items',c[1]['signature'])

if __name__=='__main__':unittest.main()
