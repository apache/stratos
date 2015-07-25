import sys
from cli import Utils


class TestClass:

    def __init__(self):
        pass

    @staticmethod
    def test_zero():
        i = 1
        assert i == 1

    @staticmethod
    def test_one():
        tree = Utils.PrintableTree(
            """[{"id":"network-partition-2","partitions":[{"id":"partition-2","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false},{"id":"partition-3","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false}]},{"id":"network-partition-1","partitions":[{"id":"partition-1","partitionMax":0,"property":[{"name":"region","value":"default"}],"public":false}]}]""")
        tree.print_tree()
        output = sys.stdout.getline().strip()  # because stdout is an StringIO instance
        assert output == 'hello world!'
