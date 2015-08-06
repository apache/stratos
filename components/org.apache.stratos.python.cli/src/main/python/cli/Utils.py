# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from __future__ import print_function
import sys
from texttable import *
import json
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


class PrintableJSON(Texttable):

    def __init__(self, json):
        self.json = json

    def pprint(self):

        print(json.dumps(self.json, indent=4, separators=(',', ': ')))


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
