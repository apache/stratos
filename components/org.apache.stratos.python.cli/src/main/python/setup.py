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
from setuptools import setup


# Utility function to read the README file.
def read(file_name):
    return open(os.path.join(os.path.dirname(__file__), file_name)).read()

setup(
    name="stratos-cli",
    version="4.1.5",
    version="4.1.5",
    author="Apache Stratos",
    author_email="dev@stratos.apache.org",
    description="CLI tool to interact with Apache Stratos",
    keywords="stratos",
    url="http://stratos.apache.org/",
    packages=['cli'],
    install_requires=['cmd2', 'requests', 'texttable'],
    long_description=read('README.rst'),
    classifiers=[
        "Development Status :: 1 - Planning",
        "Topic :: Utilities",
        "License :: OSI Approved :: Apache Software License",
    ],
    entry_points='''
        [console_scripts]
        stratos-cli=cli.Main:main
    ''',
)
