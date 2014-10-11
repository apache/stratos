# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

class RepositoryInformation:
    """
    Holds repository information to be used in artifact management
    """

    def __init__(self, repo_url, repo_username, repo_password, repo_path, tenant_id, is_multitenant, commit_enabled):
        self.repo_url = repo_url
        """ :type : str  """
        self.repo_username = repo_username
        """ :type : str  """
        self.repo_password = repo_password
        """ :type : str  """
        self.repo_path = repo_path
        """ :type : str  """
        self.tenant_id = tenant_id
        """ :type : int  """
        self.is_multitenant = is_multitenant
        """ :type : bool  """
        self.commit_enabled = commit_enabled
        """ :type : bool  """