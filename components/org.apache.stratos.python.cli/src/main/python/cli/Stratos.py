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
        return Stratos.get('users', error_message='No applications found')

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
        return Stratos.get('networkPartitions', error_message='No network partitions found')

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
        return Stratos.get('cartridges', error_message='No cartridges found')

    @staticmethod
    def list_cartridge_groups():
        return Stratos.get('cartridgeGroups', error_message='No cartridge groups found')

    """
    # Kubernetes Clusters

    """
    @staticmethod
    def list_kubernetes_clusters():
        return Stratos.get('kubernetesClusters', error_message='Kubernetes cluster not found')

    @staticmethod
    def list_kubernetes_hosts(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+kubernetes_cluster_id+'/hosts',
                           error_message='Kubernetes cluster not found')

    @staticmethod
    def list_deployment_policies():
        return Stratos.get('deploymentPolicies',
                           error_message='Deployment policies not found')

    @staticmethod
    def list_cartridge_groups():
        return Stratos.get('cartridgeGroups',
                           error_message='cartridge groups not found')
    @staticmethod
    def list_autoscaling_policies():
        return Stratos.get('autoscalingPolicies',
                           error_message='No Autoscaling policies found')

    @staticmethod
    def describe_cartridge(cartridge_type):
        return Stratos.get('cartridges/'+cartridge_type,
                           error_message='No Autoscaling policies found')

    @staticmethod
    def describe_cartridge_group(group_definition_name):
        return Stratos.get('cartridgeGroups/'+group_definition_name,
                           error_message='No cartridge groups found')

    @staticmethod
    def describe_deployment_policy(deployment_policy_name):
        return Stratos.get('deploymentPolicies/'+ deployment_policy_name,
                           error_message='No deployment policies found')

    @staticmethod
    def describe_kubernetes_cluster(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+ kubernetes_cluster_id,
                           error_message='No kubernetes clusters found')

    @staticmethod
    def describe_kubernetes_master(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+ kubernetes_cluster_id+'/master',
                           error_message='No kubernetes clusters found')

    @staticmethod
    def describe_network_partition(network_partition_id):
        return Stratos.get('networkPartitions/'+ network_partition_id,
                           error_message='No nerwork partitions found')

    @staticmethod
    def describe_autoscaling_policy(autoscaling_policy_id):
        return Stratos.get('autoscalingPolicies/'+ autoscaling_policy_id,
                           error_message='No autoscaling policy found')

    @staticmethod
    def describe_application_signup(application_id):
        return Stratos.get('applications/'+ application_id + '/signup',
                           error_message='No signup application found')



    """
    # Utils

    """
    @staticmethod
    def get(resource, error_message):
        r = requests.get(Configs.stratos_api_url + resource,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        #print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == error_message:
                return []
            else:
                raise requests.HTTPError()

    @staticmethod
    def post(resource, data,  error_message):
        r = requests.post(Configs.stratos_api_url + resource, data,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        #print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == error_message:
                return []
            else:
                raise requests.HTTPError()

