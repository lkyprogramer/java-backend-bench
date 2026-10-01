import unittest
from judge.cases import eq,reservation_view
from judge.runtime import CheckFailure

class ContractMatchingTests(unittest.TestCase):
    def test_extra_response_fields_are_allowed(self):
        eq({'items':[],'nextAfter':None,'traceId':'x'},{'items':[],'nextAfter':None})
    def test_extra_nested_fields_are_allowed(self):
        eq([{'sku':'A','quantity':1,'traceId':'x'}],[{'sku':'A','quantity':1}])
    def test_missing_required_response_field_fails(self):
        with self.assertRaises(CheckFailure):eq({'items':[]},{'items':[],'nextAfter':None})
    def test_json_number_output_type_is_enforced(self):
        for actual in (True,1.0,'1'):
            with self.subTest(actual=actual),self.assertRaises(CheckFailure):eq(actual,1)
    def test_array_length_and_order_remain_strict(self):
        with self.assertRaises(CheckFailure):eq([1,2,3],[1,2])
        with self.assertRaises(CheckFailure):eq([2,1],[1,2])
if __name__=='__main__':unittest.main()
