import math
import unittest
from judge.scoring import case_score, summarize, rate_score

class ScoringTests(unittest.TestCase):
    def test_missing_and_timeout_remain_in_denominator(self):
        self.assertEqual(case_score([{'status':'pass'},{'status':'timeout'},{'status':'not_run'},{'status':'fail'}]), 2.5)
    def test_empty_is_zero_not_perfect(self):
        self.assertEqual(case_score([]), 0)
    def test_all_pass_is_ten(self):
        self.assertEqual(case_score([{'status':'pass'}]*10), 10)
    def test_overall_mean_and_critical_cap(self):
        scores = {'D%02d'%i:10.0 for i in range(1,11)}
        self.assertEqual(summarize(scores)['overall_10'], 10)
        r = summarize(scores, critical_failures=['oversell'])
        self.assertEqual(r['raw_overall_10'], 10)
        self.assertEqual(r['overall_10'], 4)
    def test_build_failure_zeroes_deliverability(self):
        self.assertEqual(summarize({'D01':10}, build_ok=False)['overall_10'], 0)
    def test_missing_dimensions_are_zero(self):
        self.assertEqual(summarize({'D01':10})['raw_overall_10'], 1)
    def test_relative_scores_are_bounded(self):
        self.assertEqual(rate_score(100,100), 10)
        self.assertAlmostEqual(rate_score(50,100), 6)
        self.assertAlmostEqual(rate_score(200,100,higher=False), 6)
        self.assertEqual(rate_score(None,100), 0)
        self.assertEqual(rate_score(float('nan'),100), 0)
        self.assertEqual(rate_score(1,0), 0)
        self.assertEqual(rate_score(-1,100), 0)

if __name__ == '__main__':
    unittest.main()
