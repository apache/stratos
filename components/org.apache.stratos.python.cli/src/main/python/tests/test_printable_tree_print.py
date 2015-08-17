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

from cli import Stratos
from cli import Configs
import responses


class TestClass:

    def __init__(self):
        pass

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_400():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='', status=400,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}

    @staticmethod
    @responses.activate
    def test_http_get_handler_on_200():
        responses.add(responses.GET, Configs.stratos_api_url,
                      body='{"keyOne": "valueOne"}', status=200,
                      content_type='application/json')

        r = Stratos.get("")

        assert r == {"keyOne": "valueOne"}
