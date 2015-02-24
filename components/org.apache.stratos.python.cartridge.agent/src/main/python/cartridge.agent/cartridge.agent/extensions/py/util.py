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
import subprocess


def execute_bash(bash_file):
    """ Execute the given bash files in the <PCA_HOME>/extensions/bash folder
    :param bash_file: name of the bash file to execute
    :return: tuple of (output, errors)
    """
    working_dir = os.path.abspath(os.path.dirname(__file__)).split("modules")[0]
    command = working_dir[:-2] + "bash/" + bash_file
    extension_values = os.environ.copy()

    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=extension_values)
    output, errors = p.communicate()

    return output, errors