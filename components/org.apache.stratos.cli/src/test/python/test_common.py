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

class TestCommon(unittest.TestCase):

    cli_cmd = "java -jar " + os.environ["CLI_JAR"]

    @classmethod
    def setUpClass(cls):
        TestCommon.wiremock = WiremockClient()
        TestCommon.wiremock.start()

    @classmethod
    def tearDownClass(cls):
        TestCommon.wiremock.stop()

    def setUp(self):
        # unset these environment variables
        if 'STRATOS_USERNAME' in os.environ: del os.environ["STRATOS_USERNAME"] # unset env var
        if 'STRATOS_PASSWORD' in os.environ: del os.environ["STRATOS_PASSWORD"] # unset env var

    def tearDown(self):
        TestCommon.wiremock.reset()

    def test_error_if_stratos_url_not_set(self):
        if 'STRATOS_URL' in os.environ: del os.environ["STRATOS_URL"]      # unset env var
        child = pexpect.spawn(TestCommon.cli_cmd)
        child.expect ('Could not find required "STRATOS_URL" variable in your environment.')
        child.expect (pexpect.EOF)

    def test_error_if_port_not_provided_in_stratos_url(self):
        os.environ["STRATOS_URL"] = "https://localhost"  # no port
        child = pexpect.spawn(TestCommon.cli_cmd)
        child.expect ('The "STRATOS_URL" variable in your environment is not a valid URL. You have provided "https://localhost"')
        child.expect ('Please provide the Stratos Controller URL as follows')
        child.expect ('https://<host>:<port>')
        child.expect (pexpect.EOF)

    def test_error_if_context_path_is_provided_in_stratos_url(self):
        os.environ["STRATOS_URL"] = "https://localhost:9443/somecontext/" # context path
        child = pexpect.spawn(TestCommon.cli_cmd)
        child.expect ('The "STRATOS_URL" variable in your environment is not a valid URL. You have provided "https://localhost:9443/somecontext/"')
        child.expect ('Please provide the Stratos Controller URL as follows')
        child.expect ('https://<host>:<port>')
        child.expect (pexpect.EOF)

    def test_error_if_non_https_scheme_is_provided_in_stratos_url(self):
        os.environ["STRATOS_URL"] = "http://localhost:9443" # http scheme
        child = pexpect.spawn(TestCommon.cli_cmd)
        child.expect ('The "STRATOS_URL" variable in your environment is not a valid URL. You have provided "http://localhost:9443"')
        child.expect ('Please provide the Stratos Controller URL as follows')
        child.expect ('https://<host>:<port>')
        child.expect (pexpect.EOF)

    def test_error_if_invalid_format_is_given_for_stratos_url(self):
        # we need to ensure the url is valid and not that it just has 2 colons and 3 or less slashes!
        os.environ["STRATOS_URL"] = ":://"
        child = pexpect.spawn(TestCommon.cli_cmd)
        child.expect ('The "STRATOS_URL" variable in your environment is not a valid URL. You have provided ":://"')
        child.expect ('Please provide the Stratos Controller URL as follows')
        child.expect ('https://<host>:<port>')
        child.expect (pexpect.EOF)

if __name__ == '__main__':
    try: 
        unittest.main()
    # handle CTRL-C
    except KeyboardInterrupt:
        # shut down wiremock 
        TestCommon.wiremock.stop()
        exit(1) 
