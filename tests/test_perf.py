import unittest
from judge.perf import percentile,compare,combine,PROFILE_HASH
class PerformanceTests(unittest.TestCase):
    def sample(self):
        return {'status':'measured','environment':{'fingerprint':'same'},'profile_sha256':PROFILE_HASH,'requested_reps':3,'summary':{'read_rps':100,'lifecycle_ops_per_sec':50,'read_p95_ms':10,'growth_ratio':1,'peak_rss_bytes':100000000}}
    def test_nearest_rank(self):
        self.assertEqual(percentile(list(range(1,101)),.95),95)
        self.assertIsNone(percentile([],.95))
    def test_identical_reference_is_ten(self):self.assertEqual(compare(self.sample(),self.sample())['score_10'],10)
    def test_errors_do_not_count_as_fast(self):
        actual=self.sample();actual['status']='candidate_failure';actual['summary']['read_rps']=1e9
        self.assertEqual(compare(actual,self.sample())['score_10'],0)
    def test_hardware_mismatch_is_not_comparable(self):
        actual=self.sample();actual['environment']['fingerprint']='other'
        self.assertEqual(compare(actual,self.sample())['status'],'not_comparable')
    def test_missing_memory_invalidates_metric(self):
        actual=self.sample();actual['summary']['peak_rss_bytes']=None
        self.assertEqual(compare(actual,self.sample())['status'],'infrastructure_error')
    def test_missing_data_not_silently_ignored(self):
        self.assertIsNone(combine([{'read_rps':10},{}])['read_rps'])
    def test_repetition_mismatch(self):
        actual=self.sample();actual['requested_reps']=1
        self.assertEqual(compare(actual,self.sample())['status'],'not_comparable')
if __name__=='__main__':unittest.main()
