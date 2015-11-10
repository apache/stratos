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

import config
from logutils import logging


def auth(func):
    """Authenticate decorator"""
    def auth_inner(self, *args, **kwargs):
        if len(args) > 1 and hasattr(args[1], 'username') and args[1].username is not None:
            config.stratos_username = args[1].username
        if len(args) > 1 and hasattr(args[1], 'password') and args[1].password is not None:
            config.stratos_password = args[1].password

        if config.stratos_username is "" and config.stratos_password is "":
            print("Pre authentication failed. Some authentication details are missing.")
            logging.warning("Pre authentication failed. Some authentication details are missing.")
        else:
            return func(self, *args, **kwargs)

    auth_inner.__name__ = func.__name__.replace('_', '-')
    auth_inner.__doc__ = func.__doc__
    return auth_inner
