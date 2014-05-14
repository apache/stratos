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

import os
import signal
import subprocess
import urllib
import urllib2
import json
import base64

class WiremockClient():

    reset_url = 'http://localhost:8080/__admin/mappings/reset'

    find_url = 'http://localhost:8080/__admin/requests/find'

    count_url = 'http://localhost:8080/__admin/requests/count'

    cookies_req_json = '{ "method": "GET", "url": "/stratos/admin/cookie" }'

    tenant_list_req_json = '{ "method": "GET", "url": "/stratos/admin/tenant/list" }'

    tenant_create_req_json = '{ "method": "POST", "url": "/stratos/admin/tenant" }'

    tenant_deactivate_req_json = '{ "method": "POST", "url": "/stratos/admin/tenant/deactivate/tenant.com" }'

    wiremock = "java -jar " + os.environ["WIREMOCK_JAR"] + " --https-port 9443"

    def start(self):
        # execute wiremock and return handle so it can be torn down
        # Note: the requests that wiremock handles and the response it will return 
        # for a request can be found in the 'mapping' directory
        self.wiremock_process = subprocess.Popen(self.wiremock.split(), 
                               stdout=subprocess.PIPE, 
                               preexec_fn=os.setsid) 

    def __del__(self):
        self.stop()

    def stop(self):
        # kill wiremock process
        os.killpg(self.wiremock_process.pid, signal.SIGTERM)

    def reset(self):
        # ignore errors when resetting
        try:
            req = urllib2.Request(WiremockClient.reset_url, data="")
            urllib2.urlopen(req)
        except:
            pass

    def get_cookie_requests_and_responses(self):
        # send GET request to mock server url /stratos/admin/coookie
        req = urllib2.Request(WiremockClient.find_url)
        req.add_header('Content-Type', 'application/json')
        response = urllib2.urlopen(req, WiremockClient.cookies_req_json)
        return json.load(response)

    def get_cookie_auth_header(self):
        data = self.get_cookie_requests_and_responses()
        encoded_username_password = data["requests"][0]["headers"]["Authorization"]
        return base64.b64decode(encoded_username_password.split(" ")[1])
   
    # lots of repeated code below - TODO refactor to method
    def get_cookie_req_count(self):
        req = urllib2.Request(WiremockClient.count_url)
        req.add_header('Content-Type', 'application/json')
        response = urllib2.urlopen(req, WiremockClient.cookies_req_json)
        return json.load(response)["count"]

    def get_tenant_list_req_count(self):
        req = urllib2.Request(WiremockClient.count_url)
        req.add_header('Content-Type', 'application/json')
        response = urllib2.urlopen(req, WiremockClient.tenant_list_req_json)
        return json.load(response)["count"]
             
    def tenant_create_req_count(self):
        req = urllib2.Request(WiremockClient.count_url)
        req.add_header('Content-Type', 'application/json')
        response = urllib2.urlopen(req, WiremockClient.tenant_create_req_json)
        return json.load(response)["count"]

    def tenant_deactivate_req_count(self):
        req = urllib2.Request(WiremockClient.count_url)
        req.add_header('Content-Type', 'application/json')
        response = urllib2.urlopen(req, WiremockClient.tenant_deactivate_req_json)
        return json.load(response)["count"]
