import re,shutil,tempfile,unittest
from pathlib import Path
from judge.cases import CASES as BASE
from judge.cases_spring import CASES as SPRING
from judge.spring import FROZEN_POM,frozen_ok
from judge.spring_mutation import report
from judge.tracks import track

REPORT='''<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="JUnit Jupiter" tests="3">
  <testcase name="ok()" classname="contract.LedgerTest"/>
  <testcase name="broken()" classname="contract.LedgerTest"><failure message="x"/></testcase>
  <testcase name="crashes(String)" classname="contract.Other"><error message="y"/></testcase>
</testsuite>'''

class SpringTrackTests(unittest.TestCase):
    def test_same_fixed_case_set_and_dimensions_as_base(self):
        self.assertEqual(list(BASE),list(SPRING))
        self.assertEqual({c:BASE[c]['dimension']for c in BASE},{c:SPRING[c]['dimension']for c in SPRING})
        self.assertEqual(80,len(SPRING))
    def test_only_build_delivery_and_journal_cases_are_replaced(self):
        replaced={c for c in SPRING if SPRING[c]['fn']is not BASE[c]['fn']}
        self.assertEqual({'B01','B02','B03','B04','B10','R09','R10'},replaced)
    def test_track_registry(self):
        self.assertEqual('judge.spring_mutation',track('t1-spring').mutation_module)
        self.assertEqual('judge.mutation',track('base').mutation_module)
        with self.assertRaises(ValueError):track('nope')
    def test_frozen_pom_and_reserved_package(self):
        with tempfile.TemporaryDirectory()as t:
            w=Path(t);shutil.copy(FROZEN_POM,w/'pom.xml');self.assertTrue(frozen_ok(w))
            (w/'src/main/java/benchjudge').mkdir(parents=True);self.assertFalse(frozen_ok(w))
            shutil.rmtree(w/'src');(w/'pom.xml').write_text((w/'pom.xml').read_text().replace('<dependencies>','<dependencies><!-- x -->'))
            self.assertFalse(frozen_ok(w))
    def test_junit_report_ids_and_exclusion_pattern(self):
        with tempfile.TemporaryDirectory()as t:
            Path(t,'TEST-junit-jupiter.xml').write_text(REPORT)
            observed,failed=report(t)
        self.assertEqual(['contract.LedgerTest#ok','contract.LedgerTest#broken','contract.Other#crashes'],observed)
        self.assertEqual(['contract.LedgerTest#broken','contract.Other#crashes'],failed)
        pattern='^'+re.escape('contract.Other#crashes')+r'(\(.*\))?$'
        self.assertTrue(re.match(pattern,'contract.Other#crashes(java.lang.String)'))
        self.assertFalse(re.match(pattern,'contract.Other#crashesToo()'))

if __name__=='__main__':unittest.main()
