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

import unittest
from cli import Stratos
from cli import Configs
from cli.exceptions import BadResponseError
import responses


class MyTestCase(unittest.TestCase):


    @responses.activate
    def test_http_get_handler_on_200(self):
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @responses.activate
    def test_http_get_handler_on_400(self):
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='', status=400,
                      content_type='application/json')

        self.assertRaises(BadResponseError, Stratos.get(""))

    @responses.activate
    def test_http_get_handler_on_404(self):
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='', status=404,
                      content_type='application/json')

        self.assertRaises(BadResponseError, Stratos.get(""))

    @responses.activate
    def test_http_get_handler_on_500(self):
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='', status=500,
                      content_type='application/json')

        self.assertRaises(BadResponseError, Stratos.get(""))

    @responses.activate
    def test_http_post_handler_on_200(self):
        responses.add(responses.POST, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.post("")

        assert r is True

    @responses.activate
    def test_http_post_handler_on_400(self):
        responses.add(responses.POST, Configs.stratos_api_url,
                      body='', status=400,
                      content_type='application/json')

        self.assertRaises(BadResponseError, Stratos.post(""))

    @responses.activate
    def test_http_post_handler_on_404(self):
        responses.add(responses.POST, Configs.stratos_api_url,
                      body='', status=404,
                      content_type='application/json')

        self.assertRaises(BadResponseError, Stratos.post(""))

    @responses.activate
    def test_http_post_handler_on_500(self):
        responses.add(responses.POST, Configs.stratos_api_url,
                      body='', status=500,
                      content_type='application/json')

        self.assertRaises(BadResponseError, Stratos.post(""))

if __name__ == '__main__':
    unittest.main()
