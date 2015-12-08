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

import os

stratos_prompt = "stratos> "

stratos_dir = "~/.stratos"
log_file_name = "stratos-cli.log"

stratos_dir_path = os.path.expanduser(stratos_dir)
log_file_path = os.path.join(stratos_dir_path, log_file_name)

stratos_url = os.getenv('STRATOS_URL', "https://localhost:9443/")
stratos_api_url = stratos_url + "api/"
stratos_username = os.getenv('STRATOS_USERNAME', "")
stratos_password = os.getenv('STRATOS_PASSWORD', "")
debug_cli = os.getenv('STRATOS_CLI_DEBUG', "False")
