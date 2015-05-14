from texttable import *


class PrintableTree:

    def __init__(self, tree_data):
        self.tree_data = tree_data
        pass

    def print_tree(self):
        def _print(t, level=0, ups=""):
            if isinstance(t, list):
                print('|')
                for element in t[:-1]:
                    print(ups + "+-")
                    _print(element, level + 1, ups + "| ")
                else:
                    print(ups + "+-")
                    _print(t[-1], level + 1, ups + "  ")
            elif isinstance(t, dict):
                print('|')
                l = []
                for k, v in t.items():
                    if isinstance(v, list) or isinstance(v, dict):
                        l.extend([k, v])
                    else:
                        l.extend([k + " : " + v])
                t = l
                for element in t[:-1]:
                    print(ups + "+-")
                    _print(element, level + 1, ups + "| ")
                else:
                    print(ups + "+-")
                    _print(t[-1], level + 1, ups + "  ")
            else:
                print(t)

            _print(self.tree_data)


class PrintableTable(Texttable):

    def __init__(self):
        Texttable.__init__(self)
        self.set_deco(Texttable.BORDER | Texttable.HEADER | Texttable.VLINES)

    def print_table(self):
        print self.draw()

