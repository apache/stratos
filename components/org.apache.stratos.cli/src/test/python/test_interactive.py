#!/usr/bin/env python 
# ----------------------------------------------------------------------------
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

import unittest
import pexpect
import os
import signal
import subprocess
import urllib
import urllib2
import json
from wiremock import WiremockClient

class TestInteractive(unittest.TestCase):

    cli_cmd = "java -jar " + os.environ["CLI_JAR"]

    WIREMOCK_HTTPS_PORT = os.environ["WIREMOCK_HTTPS_PORT"]

    @classmethod
    def setUpClass(cls):
        TestInteractive.wiremock = WiremockClient()
        TestInteractive.wiremock.start()

    @classmethod
    def tearDownClass(cls):
        TestInteractive.wiremock.stop()

    def setUp(self):
        # set default STRATOS_URL
        os.environ["STRATOS_URL"] = "https://localhost:" + TestInteractive.WIREMOCK_HTTPS_PORT 
        # ensure other env vars not set
        if 'STRATOS_USERNAME' in os.environ: del os.environ["STRATOS_USERNAME"] # unset env var
        if 'STRATOS_PASSWORD' in os.environ: del os.environ["STRATOS_PASSWORD"] # unset env var

    def tearDown(self):
        TestInteractive.wiremock.reset()

    def test_interactive_mode_username_and_password_sent_to_server(self):
        child = pexpect.spawn(TestInteractive.cli_cmd, timeout=10)
        child.expect   ('Username: ')
        child.sendline ('1234')
        child.expect   ('Password: ')
        child.sendline ('abcd')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "1234:abcd")

    def test_interactive_mode_standard_username_parameter_provided(self):
        child = pexpect.spawn(TestInteractive.cli_cmd + " -username xxx", timeout=10)
        child.expect   ('Username: xxx')
        child.expect   ('Password: ')
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "xxx:zzz")

    def test_interactive_mode_short_username_parameter_provided(self):
        child = pexpect.spawn(TestInteractive.cli_cmd + " -u xxx", timeout=10)
        child.expect   ('Username: xxx')
        child.expect   ('Password: ')
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "xxx:zzz")

    def test_interactive_mode_long_username_parameter_provided(self):
        child = pexpect.spawn(TestInteractive.cli_cmd + " --username xxx", timeout=10)
        child.expect   ('Username: xxx')
        child.expect   ('Password: ')
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "xxx:zzz")

    def test_interactive_mode_username_env_var_provided(self):
        os.environ["STRATOS_USERNAME"] = "yyy" 
        # ensure other env vars not set
        child = pexpect.spawn(TestInteractive.cli_cmd, timeout=10)
        child.expect   ('Username: yyy')
        child.expect   ('Password: ')
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "yyy:zzz")

    def test_interactive_mode_standard_password_parameter_provided(self):
        child = pexpect.spawn(TestInteractive.cli_cmd + " -password xxx", timeout=10)
        child.expect   ('Username: ')
        child.sendline ('1234') 
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "1234:xxx")

    def test_interactive_mode_list_tenants(self):
        child = pexpect.spawn(TestInteractive.cli_cmd, timeout=10)
        child.expect   ('Username: ')
        child.sendline ('1234') 
        child.expect   ('\r\nPassword: ') # TODO - why do we need \r\n?
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('list-tenants')
        child.expect   ('Available Tenants')
        # in the table below, + characters have been replaced with .
        # because the + is a special regex character
        child.expect   ('.------------.-----------.-----------------.--------.----------------------.')
        child.expect   ('| Domain     | Tenant ID | Email           | State  | Created Date         |')
        child.expect   ('.------------.-----------.-----------------.--------.----------------------.')
        child.expect   ('| tenant.com | 1         | john@tenant.com | Active | 2014-05-09T05:40:11Z |')
        child.expect   ('.------------.-----------.-----------------.--------.----------------------.')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "1234:zzz")
        self.assertEqual(self.wiremock.get_cookie_req_count(), 1)
        self.assertEqual(self.wiremock.get_tenant_list_req_count(), 1)

    def test_interactive_mode_create_tenant(self):
        child = pexpect.spawn(TestInteractive.cli_cmd, timeout=10)
        child.expect   ('Username: ')
        child.sendline ('1234') 
        child.expect   ('\r\nPassword: ') # TODO - why do we need \r\n?
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('create-tenant --username tenant1 --password secret --first-name John --last-name Doe --domain-name tenant.com --email john@tenant.com')
        child.expect   ('Tenant added successfully')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "1234:zzz")
        self.assertEqual(self.wiremock.get_cookie_req_count(), 1)
        self.assertEqual(self.wiremock.tenant_create_req_count(), 1)

    def test_interactive_mode_deactivate_tenant(self):
        child = pexpect.spawn(TestInteractive.cli_cmd, timeout=10)
        child.expect   ('Username: ')
        child.sendline ('1234') 
        child.expect   ('\r\nPassword: ') # TODO - why do we need \r\n?
        child.sendline ('zzz')
        child.expect   ('Successfully authenticated')
        child.expect   ('stratos> ')
        child.sendline ('deactivate-tenant tenant.com')
        child.expect   ('You have succesfully deactivate tenant.com tenant')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # CLI sends GET request to mock server url /stratos/admin/coookie
        self.assertEqual(self.wiremock.get_cookie_auth_header(), "1234:zzz")
        self.assertEqual(self.wiremock.get_cookie_req_count(), 1)
        self.assertEqual(self.wiremock.tenant_deactivate_req_count(), 1)


if __name__ == '__main__':
    try: 
        unittest.main()
    # handle CTRL-C
    except KeyboardInterrupt:
        # shut down wiremock 
        TestInteractive.wiremock.stop()
        exit(1) 
