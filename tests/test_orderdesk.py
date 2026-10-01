import shutil,tempfile,unittest
from pathlib import Path
from judge import orderdesk as od

class OrderDeskJudgeTests(unittest.TestCase):
    def test_overlays_are_disjoint_per_bug(self):
        seen={}
        for b in od.BUGS:
            for f in (od.TRACK_DIR/'fixes'/b).rglob('*'):
                if f.is_file():
                    rel=f.relative_to(od.TRACK_DIR/'fixes'/b).as_posix()
                    self.assertNotIn(rel,seen,f'{b} and {seen.get(rel)} both change {rel}');seen[rel]=b
    def test_compose_applies_fixes_and_delete_markers(self):
        with tempfile.TemporaryDirectory()as t:
            ws=od.compose(Path(t)/'ws',od.BUGS,extra=['t3/reference','t3/naive/edit-v1'])
            self.assertFalse((ws/'src/main/resources/db/migration/V3__shipments.sql').exists())
            self.assertFalse((ws/'_delete.txt').exists())
            self.assertFalse(od.protected_ok(ws,od.t3_start(Path(t)/'start')))
    def test_changed_lines_and_protected_files(self):
        with tempfile.TemporaryDirectory()as t:
            ws=od.compose(Path(t)/'ws')
            self.assertEqual(0,od.changed_lines(ws));self.assertTrue(od.protected_ok(ws))
            f=ws/'src/main/java/com/orderdesk/service/ReportService.java'
            f.write_text(f.read_text().replace('ZoneId.systemDefault()','properties.businessZone()'))
            self.assertEqual(2,od.changed_lines(ws))
            (ws/'pom.xml').write_text((ws/'pom.xml').read_text()+'\n')
            self.assertFalse(od.protected_ok(ws))
    def test_unit_requires_every_class_present_and_green(self):
        results={'classes':{'A':{'tests':2,'passed':2},'B':{'tests':1,'passed':0},'E':{'tests':0,'passed':0}}}
        self.assertTrue(od.unit_pass(results,['A']))
        self.assertFalse(od.unit_pass(results,['A','B']))
        self.assertFalse(od.unit_pass(results,['A','missing']))
        self.assertFalse(od.unit_pass(results,['E']))
    def test_prepared_workspaces_hold_no_hidden_material(self):
        with tempfile.TemporaryDirectory()as t:
            for track,doc in (('t2-bugfix','ISSUES.md'),('t3-change','REQUIREMENT.md')):
                ws=od.prepare(track,Path(t)/track)
                self.assertTrue((ws/doc).is_file())
                names={p.name for p in ws.rglob('*')}
                self.assertFalse(any(n.startswith('Hidden')or n.startswith('Snapshot')for n in names))
                self.assertFalse((ws/'src/main/resources/db/migration/V3__shipments.sql').exists())
            t3=Path(t)/'t3-change'/'src/main/java/com/orderdesk/service/ReportService.java'
            self.assertIn('properties.businessZone()',t3.read_text())

if __name__=='__main__':unittest.main()
