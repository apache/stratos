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

import pytest
import json
import shutil
import os
from .. cartridgeagent.modules.artifactmgt.repository import Repository
from .. cartridgeagent.modules.artifactmgt.git.agentgithandler import *


@pytest.mark.parametrize("input, expected", [
    ("simple_repo.json", True),
    ("auth_repo.json", True),
    ("auth_repo2.json", True),
])
def test_clone(input, expected):
    with open("conf/git/" + input, "r") as f:
        repo_string = f.read()

    repo_info = json.loads(repo_string, object_hook=repo_object_decoder)
    sub_run, repo_context = AgentGitHandler.checkout(repo_info)

    assert sub_run, "Not detected as subscription run"

    result, msg = verify_git_repo(repo_info)
    assert result == expected, msg


def setup_module(module):
    # clear the temp folder path to clone new folders
    try:
        shutil.rmtree("/tmp/apachestratos")
    except:
        pass

    try:
        os.makedirs("/tmp/apachestratos")
    except:
        pass


def verify_git_repo(repo_info):
    """
    Assert the status of the git repository
    :param repo_info:
    :return:
    """
    if not os.path.isdir(repo_info.repo_path):
        return False, "Local repository directory not created."

    output, errors = AgentGitHandler.execute_git_command(["status"], repo_info.repo_path)
    if not (len(errors) == 0 and "nothing to commit, working directory clean" in output):
        return False, "Git clone failed. "

    return True, None


def repo_object_decoder(obj):
    """ Repository object decoder from JSON
    :param obj: json object
    :return:
    """
    return Repository(str(obj["repoURL"]), str(obj["repoUsername"]), str(obj["repoPassword"]), str(obj["repoPath"]),
                      str(obj["tenantId"]), str(obj["multitenant"]), str(obj["commitEnabled"]))
