import json,subprocess,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

class ToolsTests(unittest.TestCase):
    def test_entrypoints_help(self):
        for tool in ('time_agent','run_matrix','validate'):
            with self.subTest(tool=tool):
                r=subprocess.run([sys.executable,str(ROOT/'tools'/f'{tool}.py'),'--help'],capture_output=True,text=True,timeout=5)
                self.assertEqual(0,r.returncode,r.stderr)
    def test_timing_records_only_observed_values(self):
        with tempfile.TemporaryDirectory()as t:
            out=Path(t)/'timing';workspace=Path(t)/'work';workspace.mkdir()
            cmd=[sys.executable,str(ROOT/'tools/time_agent.py'),'--cwd',str(workspace),'--out',str(out),'--timeout','3','--allow-exec','--',sys.executable,'-c','print("agent-evidence")']
            r=subprocess.run(cmd,capture_output=True,text=True,timeout=5);self.assertEqual(0,r.returncode,r.stderr)
            v=json.loads((out/'timing.json').read_text());self.assertGreater(v['wall_seconds'],0);self.assertIsNone(v['input_tokens']);self.assertIsNone(v['argv']);self.assertFalse(v['timed_out'])
            self.assertIn('agent-evidence',(out/'agent.log').read_text())
    def test_timing_timeout_retains_evidence(self):
        with tempfile.TemporaryDirectory()as t:
            out=Path(t)/'timing';workspace=Path(t)/'work';workspace.mkdir()
            cmd=[sys.executable,str(ROOT/'tools/time_agent.py'),'--cwd',str(workspace),'--out',str(out),'--timeout','.1','--allow-exec','--',sys.executable,'-c','import time;time.sleep(10)']
            r=subprocess.run(cmd,capture_output=True,text=True,timeout=5);self.assertEqual(124,r.returncode,r.stderr)
            v=json.loads((out/'timing.json').read_text());self.assertTrue(v['timed_out']);self.assertIsNone(v['output_tokens'])
    def test_timing_refuses_unconfirmed_execution(self):
        with tempfile.TemporaryDirectory()as t:
            r=subprocess.run([sys.executable,str(ROOT/'tools/time_agent.py'),'--cwd',t,'--out',t+'x','--',sys.executable,'-c','print(1)'],capture_output=True,text=True,timeout=5)
            self.assertEqual(2,r.returncode);self.assertIn('--allow-exec',r.stderr)

if __name__=='__main__':unittest.main()
