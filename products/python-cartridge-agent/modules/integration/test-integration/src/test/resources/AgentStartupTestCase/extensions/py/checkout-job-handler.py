# ------------------------------------------------------------------------
#
# Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License
#
# ------------------------------------------------------------------------

from plugins.contracts import IArtifactCheckoutPlugin
from modules.util.log import LogFactory
from entity import *
from modules.artifactmgt.git.agentgithandler import *


class CheckoutJobHandler(IArtifactCheckoutPlugin):
    def checkout(self, repo_info):
        log = LogFactory().get_log(__name__)
        try:
            log.info("Running extension for checkout job")
            repo_info = values['REPO_INFO']
            git_repo = AgentGitHandler.create_git_repo(repo_info)
            AgentGitHandler.add_repo(git_repo)
        except Exception as e:
            log.exception("Error while executing CheckoutJobHandler extension: %s" % e)
