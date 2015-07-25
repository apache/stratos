from __future__ import print_function
import sys
from texttable import *
import Configs
from Logging import logging


class PrintableTree:

    def __init__(self, tree_data):
        self.tree_data = tree_data
        pass

    def print_tree(self):
        def _print_tree(t, level=0, ups=""):
            if isinstance(t, list):
                print('|')
                for element in t[:-1]:
                    print(ups + "+-", end='')
                    _print_tree(element, level + 1, ups + "| ")
                else:
                    print(ups + "+-", end='')
                    _print_tree(t[-1], level + 1, ups + "  ")
            elif isinstance(t, dict):
                print('|')
                l = []
                for k, v in t.items():
                    if isinstance(v, list) or isinstance(v, dict):
                        l.extend([k, v])
                    else:
                        l.extend([str(k) + ":" + str(v)])
                t = l
                for element in t[:-1]:
                    print(ups + "+-", end='')
                    _print_tree(element, level + 1, ups + "| ")
                else:
                    print(ups + "+-", end='')
                    _print_tree(t[-1], level + 1, ups + "  ")
            else:
                print(str(t))
        print("_")
        _print_tree(self.tree_data)


class PrintableTable(Texttable):

    def __init__(self):
        Texttable.__init__(self)
        self.set_deco(Texttable.BORDER | Texttable.HEADER | Texttable.VLINES)

    def print_table(self):
        print(self.draw())

def auth(func):
    """Authenticate"""
    def auth_inner(self, *args, **kwargs):

        if len(args) > 1 and hasattr(args[1], 'username') and args[1].username is not None:
            Configs.stratos_username = args[1].username
        if len(args) > 1 and hasattr(args[1], 'password') and args[1].password is not None:
            Configs.stratos_password = args[1].password

        if Configs.stratos_username is "" and Configs.stratos_password is "":
            print("Pre authentication failed. Some authentication details are missing")
            logging.warning("Pre authentication failed. Some authentication details are missing")
        else:
            return func(self, *args, **kwargs)
    return auth_inner

