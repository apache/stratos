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
from CLI import CLI
import Configs

# Fix Python 2.x.
from Stratos import Stratos
from Exceptions import BadResponseError

try:
    input = raw_input
except NameError:
    pass


def prompt_for_credentials():
    """Prompt for user credentials"""
    while Configs.stratos_username is "" or Configs.stratos_password is "":
        if Configs.stratos_username is "":
            Configs.stratos_username = input("Username: ")

        if Configs.stratos_password is "":
            Configs.stratos_password = getpass.getpass("Password: ")
    if Stratos.authenticate():
        print("Successfully authenticated [%s]" % Configs.stratos_url)
    else:
        print("Could not authenticate")
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
