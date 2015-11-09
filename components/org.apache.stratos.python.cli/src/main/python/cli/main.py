#!/usr/bin/env python
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

import sys
import readline
import getpass
from cli import CLI
import config
from exception import BadResponseError
import requests

# Fix Python 2.x.
from restclient import StratosClient

try:
    user_input = raw_input
except NameError:
    pass


def prompt_for_credentials():
    """Prompt for user credentials"""
    while config.stratos_username is "" or config.stratos_password is "":
        if config.stratos_username is "":
            config.stratos_username = user_input("Username: ")

        if config.stratos_password is "":
            config.stratos_password = getpass.getpass("Password: ")
    try:
        StratosClient.authenticate()
        print("Successfully Authenticated with [%s]" % config.stratos_url)
    except BadResponseError:
        print("Authentication Failed.")
        exit()
    except requests.exceptions.ConnectionError:
        print("Connection to Server at [%s] failed. Terminating Stratos CLI." % config.stratos_url)
        exit()


def main():
    # resolving the '-' issue
    readline.set_completer_delims(readline.get_completer_delims().replace('-', ''))

    cli = CLI()

    if len(sys.argv) > 1:
        try:
            cli.onecmd(' '.join(sys.argv[1:]))
        except BadResponseError as e:
            print(str(e))
    else:
        prompt_for_credentials()
        cli.cmdloop()

if __name__ == '__main__':
    main()
