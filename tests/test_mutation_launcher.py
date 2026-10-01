import json,sys,tempfile,unittest
from pathlib import Path
from judge.mutation import LAUNCHER
from judge.runtime import run_limited

SUITE='''import argparse,sys,unittest
class T(unittest.TestCase):
    def test_ok(self):self.assertTrue(True)
    def test_broken(self):self.assertEqual(1,2)
    def test_sub(self):
        for i in range(2):
            with self.subTest(i=i):self.assertEqual(i,0)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--base-url',required=True);a,rest=p.parse_known_args()
    assert a.base_url=='http://x'
    unittest.main(argv=[sys.argv[0]]+rest)
'''

class LauncherTests(unittest.TestCase):
    def run_suite(self,root,source,exclude=None):
        (root/'candidate_tests.py').write_text(source);(root/'launcher.py').write_text(LAUNCHER)
        record=root/'record.json';ex='-'
        if exclude is not None:ex=str(root/'exclude.json');Path(ex).write_text(json.dumps(exclude))
        r=run_limited([sys.executable,'-I',str(root/'launcher.py'),str(record),ex,str(root/'candidate_tests.py'),'--base-url','http://x'],str(root),20)
        return r,json.loads(record.read_text())
    def test_records_failures_including_subtests_and_passes_argv(self):
        with tempfile.TemporaryDirectory()as t:
            r,rec=self.run_suite(Path(t),SUITE)
            self.assertNotEqual(0,r['returncode'],r['output'])
            self.assertEqual({'__main__.T.test_ok','__main__.T.test_broken','__main__.T.test_sub'},set(rec['started']))
            self.assertEqual({'__main__.T.test_broken','__main__.T.test_sub'},set(rec['failed']))
    def test_excluded_tests_are_skipped(self):
        with tempfile.TemporaryDirectory()as t:
            r,_=self.run_suite(Path(t),SUITE,['__main__.T.test_broken','__main__.T.test_sub'])
            self.assertEqual(0,r['returncode'],r['output']);self.assertIn('skipped=2',r['output'])
    def test_non_unittest_suite_has_no_attributable_ids(self):
        with tempfile.TemporaryDirectory()as t:
            r,rec=self.run_suite(Path(t),'raise SystemExit(1)\n')
            self.assertEqual(1,r['returncode']);self.assertEqual({'started':[],'failed':[]},rec)

if __name__=='__main__':unittest.main()
