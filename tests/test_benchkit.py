import subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

class SuppliedJsonTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp=tempfile.TemporaryDirectory();cls.directory=Path(cls.temp.name)
        probe=cls.directory/'Probe.java'
        probe.write_text('import benchkit.Json; public class Probe { public static void main(String[] a) { try { Object x=Json.parse(a[0]); System.out.println("accepted:"+(x==null?"null":x.getClass().getSimpleName())); } catch(IllegalArgumentException e) { System.out.println("rejected"); } } }')
        subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',str(cls.directory),str(ROOT/'candidate/src/benchkit/Json.java'),str(probe)],capture_output=True,text=True,check=True,timeout=10)
    @classmethod
    def tearDownClass(cls):cls.temp.cleanup()
    def parse(self,text):
        return subprocess.run(['java','-cp',str(self.directory),'Probe',text],capture_output=True,text=True,check=True,timeout=5).stdout.strip()
    def test_ascii_integers_keep_integer_type(self):self.assertEqual('accepted:Long',self.parse('123'))
    def test_decimal_lexemes_remain_distinct(self):self.assertEqual('accepted:BigDecimal',self.parse('1e0'))
    def test_non_ascii_digits_rejected(self):self.assertEqual('rejected',self.parse('1\u0661'))
    def test_non_ascii_escape_hex_rejected(self):self.assertEqual('rejected',self.parse('"\\u００４１"'))
    def test_duplicate_object_keys_rejected(self):self.assertEqual('rejected',self.parse('{"x":1,"x":2}'))
if __name__=='__main__':unittest.main()
