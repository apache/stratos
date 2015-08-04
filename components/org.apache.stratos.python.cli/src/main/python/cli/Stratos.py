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

import requests
import Configs
from cli.exceptions.AuthenticationError import AuthenticationError


class Stratos:
    """Apache Stratos Python API"""

    def __init__(self):
        pass

    """
    # Users

    """
    @staticmethod
    def list_users():
        return Stratos.get('users', errorMessage='No applications found')

    @staticmethod
    def add_users(username, password, role_name, first_name, last_name, email, profile_name):
        data = [username]
        r = requests.post(Configs.stratos_api_url + 'users', data,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)

    """
    # Network Partitions

    """
    @staticmethod
    def list_network_partitions():
        return Stratos.get('networkPartitions', errorMessage='No network partitions found')

    """
    # Applications

    """
    @staticmethod
    def list_applications():
        r = requests.get(Configs.stratos_api_url + 'applications',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == "No applications found":
                return []
            else:
                raise requests.HTTPError()

    """
    # Cartridges

    """
    @staticmethod
    def list_cartridges():
        return Stratos.get('cartridges', errorMessage='No cartridges found')

    @staticmethod
    def list_cartridge_groups():
        return Stratos.get('cartridgeGroups', errorMessage='No cartridge groups found')

    """
    # Kubernetes Clusters

    """
    @staticmethod
    def list_kubernetes_clusters():
        return Stratos.get('kubernetesClusters', errorMessage='Kubernetes cluster not found')

    @staticmethod
    def list_kubernetes_hosts(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+kubernetes_cluster_id+'/hosts',
                           errorMessage='Kubernetes cluster not found')

    @staticmethod
    def list_deployment_policies():
        return Stratos.get('deploymentPolicies',
                           errorMessage='Deployment policies not found')

    """
    # Utils

    """
    @staticmethod
    def get(resource, errorMessage):
        r = requests.get(Configs.stratos_api_url + resource,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == errorMessage:
                return []
            else:
                raise requests.HTTPError()

