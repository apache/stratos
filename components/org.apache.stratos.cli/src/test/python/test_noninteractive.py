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

class TestNonInteractive(unittest.TestCase):

    cli_cmd = "java -jar " + os.environ["CLI_JAR"]

    @classmethod
    def setUpClass(cls):
        TestNonInteractive.wiremock = WiremockClient()
        TestNonInteractive.wiremock.start()

    @classmethod
    def tearDownClass(cls):
        TestNonInteractive.wiremock.stop()

    def setUp(self):
        # set default STRATOS_URL
        os.environ["STRATOS_URL"] = "https://localhost:9443" 
        # ensure other env vars not set
        if 'STRATOS_USERNAME' in os.environ: del os.environ["STRATOS_USERNAME"] # unset env var
        if 'STRATOS_PASSWORD' in os.environ: del os.environ["STRATOS_PASSWORD"] # unset env var

    def tearDown(self):
        TestNonInteractive.wiremock.reset()

    def test_noninteractive_mode_list_tenants(self):
        child = pexpect.spawn(TestNonInteractive.cli_cmd + " -username admin -password admin list-tenants", timeout=10)
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
        # verify /stratos/admin/tenant/list was called
        self.assertEqual(self.wiremock.get_tenant_list_req_count(), 1)

    def test_noninteractive_mode_create_tenant(self):
        command = "create-tenant -u tenant1 -p secret -f John -l Doe -d tenant.com -e john@tenant.com"
        child = pexpect.spawn(TestNonInteractive.cli_cmd + " -u adminuser -p adminpass " + command, timeout=10)
        child.expect   ('Username: adminuser')
        child.expect   ('Tenant added successfully')
        child.sendline ('exit')
        child.expect   (pexpect.EOF)
        # verify POST /stratos/admin/tenant/list was called
        self.assertEqual(self.wiremock.tenant_create_req_count(), 1)

if __name__ == '__main__':
    try: 
        unittest.main()
    # handle CTRL-C
    except KeyboardInterrupt:
        # shut down wiremock 
        TestNonInteractive.wiremock.stop()
        exit(1) 
