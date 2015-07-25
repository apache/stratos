import sys
import unittest
from cli import Utils


class MyTestCase(unittest.TestCase):
    def test_something(self):
        tree = Utils.PrintableTree(
            """[{"id":"network-partition-2","partitions":[{"id":"partition-2","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false},{"id":"partition-3","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false}]},{"id":"network-partition-1","partitions":[{"id":"partition-1","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false}]}]""")
        tree.print_tree()
        output = "hello world!"
        self.assertEqual(output, 'hello world!')


if __name__ == '__main__':
    unittest.main()
