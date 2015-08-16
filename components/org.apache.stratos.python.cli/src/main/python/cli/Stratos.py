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
import json
import Configs
from Logging import logging
from cli.exceptions.AuthenticationError import AuthenticationError


class Stratos:
    """Apache Stratos Python API"""

    def __init__(self):
        pass

    """
    # Users
     * list-users
     * add-users
     * update-users
     * remove-user

    """
    @staticmethod
    def list_users():
        return Stratos.get('users', error_message='No users found')

    @staticmethod
    def add_users(username, password, role_name, first_name, last_name, email, profile_name):
        data = {
            "userName": username,
            "credential": password,
            "role": role_name,
            "firstName": first_name,
            "lastName": last_name,
            "email": email
        }
        return Stratos.post('users', json.dumps(data),  error_message='No applications found')

    @staticmethod
    def update_user(username, password, role_name, first_name, last_name, email, profile_name):
        data = {
            "userName": username,
            "credential": password,
            "role": role_name,
            "firstName": first_name,
            "lastName": last_name,
            "email": email
        }
        return Stratos.put('users', json.dumps(data),  error_message='No applications found')

    @staticmethod
    def remove_user(name):
        return Stratos.delete('users/'+name, error_message="Requested user "+name+" does not exist")

    """
    # Applications
     * list-applications
     * describe-applications
     * add-application
     * remove-application
     * deploy-application
     * describe-application-runtime

    """
    @staticmethod
    def list_applications():
        return Stratos.get('applications', error_message='No applications found')

    @staticmethod
    def describe_application(application_id):
        return Stratos.get('applications/'+application_id,
                           error_message='No application found')

    @staticmethod
    def remove_application(application):
        return Stratos.delete('applications/'+application)

    @staticmethod
    def deploy_application(application_id, application_policy_id):
        return Stratos.post('applications/'+application_id+'/deploy/'+application_policy_id, None,
                            error_message='No application found')
    @staticmethod
    def describe_application_runtime(application_id):
        return Stratos.get('applications/'+application_id+"/runtime", error_message='No application runtime found')

    """
    # Application signup
     * describe-application-signup
     * remove-application-signup

    """
    @staticmethod
    def describe_application_signup(application_id):
        return Stratos.get('applications/'+application_id+'/signup',
                           error_message='No signup application found')
    @staticmethod
    def add_application_signup(application_id, json):
        return Stratos.post('applications/'+application_id+"/runtime", json, error_message='No application runtime found')

    @staticmethod
    def remove_application_signup(application_id):
        return Stratos.delete('applications/'+application_id + '/signup')

    """
    # Tenants
     * list-tenants
     * list-tenants-by-partial-domain
     * describe-tenant
     * add-tenant
     * activate-tenant
     * deactivate-tenant

    """
    @staticmethod
    def list_tenants():
        return Stratos.get('tenants', error_message='No cartridges found')

    @staticmethod
    def list_tenants_by_partial_domain(partial_domain):
        return Stratos.get('tenants/search/'+partial_domain, error_message='No cartridges found')

    @staticmethod
    def describe_tenant(tenant_domain_name):
        return Stratos.get('tenants/'+tenant_domain_name, error_message='No cartridge found')

    @staticmethod
    def add_tenant(username, first_name, last_name, password, domain_name, email):
        data = {
            "admin": username,
            "firstName": first_name,
            "lastName": last_name,
            "adminPassword": password,
            "tenantDomain": domain_name,
            "email": email,
            "active": "true"
        }
        return Stratos.post('tenants', json.dumps(data),  error_message='No tenant found')

    @staticmethod
    def update_tenant(username, first_name, last_name, password, domain_name, email, tenant_id):
        data = {
            "tenantId": tenant_id,
            "admin": username,
            "firstName": first_name,
            "lastName": last_name,
            "adminPassword": password,
            "tenantDomain": domain_name,
            "email": email,
            "active": "true"
        }
        return Stratos.put('tenants', json.dumps(data),  error_message='No tenant found')

    @staticmethod
    def activate_tenant(tenant_domain):
        return Stratos.put('tenants/activate/'+tenant_domain, "",  error_message='No tenant found')

    @staticmethod
    def deactivate_tenant(tenant_domain):
        return Stratos.put('tenants/deactivate/'+tenant_domain, "",  error_message='No tenant found')

    """
    # Cartridges
     * list-cartridges
     * describe-cartridge
     * add-cartridge
     * remove-cartridges

    """
    @staticmethod
    def list_cartridges():
        return Stratos.get('cartridges', error_message='No cartridges found')

    @staticmethod
    def describe_cartridge(cartridge_type):
        return Stratos.get('cartridges/'+cartridge_type, error_message='Cartridge not found')

    @staticmethod
    def add_cartridge(json):
        return Stratos.post('cartridges', json,  error_message='No cartridge found')

    @staticmethod
    def update_cartridge(json):
        return Stratos.put('cartridges', json,  error_message='No cartridge found')

    @staticmethod
    def remove_cartridge(cartridge_type):
        return Stratos.delete('cartridges/'+cartridge_type)

    """
    # Cartridge groups
     * list-cartridge-groups
     * describe-cartridge-group
     * update-cartridges-group
     * remove-cartridges-group

    """

    @staticmethod
    def list_cartridge_groups():
        return Stratos.get('cartridgeGroups', error_message='No cartridge groups found')

    @staticmethod
    def describe_cartridge_group(group_definition_name):
        return Stratos.get('cartridgeGroups/'+group_definition_name, error_message='No cartridge groups found')

    @staticmethod
    def add_cartridge_group(json):
        return Stratos.post('cartridgeGroups', json,  error_message='No cartridge group found')

    @staticmethod
    def update_cartridge_group(json):
        return Stratos.put('cartridgeGroups', json,  error_message='No cartridge found')

    @staticmethod
    def remove_cartridge_group(group_definition_name):
        return Stratos.delete('cartridgeGroups/'+group_definition_name)

    """
    # Deployment Policy
     * list-deployment-policies
     * describe-deployment-policy
     * add-deployment-policy
     * update-deployment-policy
     * remove-deployment-policy

    """
    @staticmethod
    def list_deployment_policies():
        return Stratos.get('deploymentPolicies',
                           error_message='Deployment policies not found')
    @staticmethod
    def describe_deployment_policy(deployment_policy_name):
        return Stratos.get('deploymentPolicies/'+ deployment_policy_name,
                           error_message='No deployment policies found')
    @staticmethod
    def add_deployment_policy(json):
        return Stratos.post('deploymentPolicies', json,  error_message='No deployment policy found')

    @staticmethod
    def update_deployment_policy(json):
        return Stratos.put('deploymentPolicies', json,  error_message='No deployment policies found')

    @staticmethod
    def remove_deployment_policy(deployment_policy_id):
        return Stratos.delete('deploymentPolicies/'+deployment_policy_id)

    """
    # Application Policy
     * list-application-policies
     * describe-application-policy
     * update-application-policy
     * remove-application-policy

    """
    @staticmethod
    def list_application_policies():
        return Stratos.get('applicationPolicies',
                           error_message='Deployment policies not found')
    @staticmethod
    def describe_application_policy(application_policy_name):
        return Stratos.get('applicationPolicies/'+ application_policy_name,
                           error_message='No application policies found')
    @staticmethod
    def update_application_policy(json):
        return Stratos.put('applicationPolicies', json,  error_message='No application policies found')

    @staticmethod
    def remove_application_policy(application_policy_id):
        return Stratos.delete('applicationPolicies/'+application_policy_id)

    """
    # Network partitions
     * list-network-partitions
     * describe-network-partition
     * add-network-partition
     * update-network-partition
     * remove-network-partition

    """
    @staticmethod
    def list_network_partitions():
        return Stratos.get('networkPartitions', error_message='No network partitions found')

    @staticmethod
    def describe_network_partition(network_partition_id):
        return Stratos.get('networkPartitions/'+network_partition_id,
                           error_message='No network partitions found')
    @staticmethod
    def add_network_partition(json):
        return Stratos.post('networkPartitions', json,  error_message='No network partition found')
    @staticmethod
    def update_network_partition(json):
        return Stratos.put('networkPartitions', json,  error_message='No cartridge found')


    @staticmethod
    def remove_network_partition(network_partition_id):
        return Stratos.delete('networkPartitions/'+network_partition_id)

    """
    # Auto-scaling policies
     * list-autoscaling-policies
     * describe-autoscaling-policy
     * update-autoscaling-policy
     * remove-autoscaling-policy

    """
    @staticmethod
    def list_autoscaling_policies():
        return Stratos.get('autoscalingPolicies',
                           error_message='No Autoscaling policies found')
    @staticmethod
    def describe_autoscaling_policy(autoscaling_policy_id):
        return Stratos.get('autoscalingPolicies/'+autoscaling_policy_id,
                           error_message='No autoscaling policy found')
    @staticmethod
    def update_autoscaling_policy(json):
        return Stratos.put('autoscalingPolicies', json,  error_message='No cartridge found')

    @staticmethod
    def remove_autoscaling_policy(autoscaling_policy_id):
        return Stratos.delete('autoscalingPolicies/'+autoscaling_policy_id, error_message="Autoscaling policy not found")

    """
    # Kubernetes clusters/hosts
     * list-kubernetes-clusters
     * describe-kubernetes-cluster
     * describe-kubernetes-master
     * add-kubernetes-cluster
     * list-kubernetes-hosts
     * update-kubernetes-host
     * update-kubernetes-master
     * remove-kubernetes-cluster
     * remove-kubernetes-host

    """
    @staticmethod
    def list_kubernetes_clusters():
        return Stratos.get('kubernetesClusters', error_message='Kubernetes cluster not found')

    @staticmethod
    def describe_kubernetes_cluster(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+kubernetes_cluster_id,
                           error_message='Kubernetes cluster not found')
    @staticmethod
    def describe_kubernetes_master(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+kubernetes_cluster_id+'/master', error_message='No kubernetes clusters found')

    @staticmethod
    def add_kubernetes_cluster(json):
        return Stratos.post('kubernetesClusters', json,  error_message='No kubernetes cluster found')

    @staticmethod
    def add_kubernetes_host(kubernetes_cluster_id, json):
        return Stratos.post('kubernetesClusters/'+kubernetes_cluster_id+'/minion', json,  error_message='No kubernetes cluster found')

    @staticmethod
    def list_kubernetes_hosts(kubernetes_cluster_id):
        return Stratos.get('kubernetesClusters/'+kubernetes_cluster_id+'/hosts',
                           error_message='Kubernetes cluster not found')

    @staticmethod
    def update_kubernetes_master(cluster_id, json):
        return Stratos.put('kubernetesClusters/'+cluster_id+'/master', json,  error_message='No cartridge found')

    @staticmethod
    def update_kubernetes_host(json):
        return Stratos.put('kubernetesClusters/update/host', json,  error_message='No cartridge found')

    @staticmethod
    def remove_kubernetes_cluster(kubernetes_cluster_id):
        return Stratos.delete('kubernetesClusters/'+kubernetes_cluster_id,
                              error_message="Autoscaling policy not found")
    @staticmethod
    def remove_kubernetes_host(kubernetes_cluster_id, host_id):
        return Stratos.delete('kubernetesClusters/'+kubernetes_cluster_id+"/hosts/"+host_id,
                              error_message="Autoscaling policy not found")

    """
    # Domain Mapping
     * list-domain-mappings
     * add-domain-mapping
     * remove-domain-mapping

    """

    @staticmethod
    def list_domain_mappings(application_id):
        return Stratos.get('applications/'+application_id+'/domainMappings',
                           error_message='No domain mapping found')

    @staticmethod
    def remove_domain_mappings(application_id):
        return Stratos.delete('applications/'+application_id+'/domainMappings')

    @staticmethod
    def add_domain_mapping(application_id, json):
        return Stratos.post('applications/'+application_id+'/domainMappings', json,
                            error_message='No domain mapping found')

    """
    # Utils

    """

    @staticmethod
    def authenticate():
        try:
            Stratos.get('init')
            return True
        except AuthenticationError as e:
            return False

    @staticmethod
    def get(resource, error_message=""):
        r = requests.get(Configs.stratos_api_url + resource,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r)
        print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.text and r.json() and r.json()['message'] == error_message:
                return False
            else:
                raise requests.HTTPError()
        else:
            logging.error(r.status_code+" : "+r.text)

    @staticmethod
    def delete(resource, error_message=None):
        r = requests.delete(Configs.stratos_api_url + resource,
                            auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r)
        print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.text and r.json() and r.json()['message']:
                return False
        elif r.status_code == 404:
            if r.text and r.json() and r.json()['message']:
                return False
            else:
                raise requests.HTTPError()
        else:
            logging.error(r.status_code+" : "+r.text)

    @staticmethod
    def post(resource, data,  error_message=None):
        headers = {'content-type': 'application/json'}
        r = requests.post(Configs.stratos_api_url + resource, data, headers=headers,
                          auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r)
        print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 201:
            return True
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.text and r.json() and r.json()['message'] == error_message:
                return []
            else:
                raise requests.HTTPError()
        else:
            logging.error(r.status_code+" : "+r.text)

    @staticmethod
    def put(resource, data,  error_message):
        headers = {'content-type': 'application/json'}
        r = requests.put(Configs.stratos_api_url + resource, data, headers=headers,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r)
        print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 201:
            return True
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.text and r.json() and r.json()['message'] == error_message:
                return []
            else:
                raise requests.HTTPError()
        else:
            logging.error(r.status_code+" : "+r.text)


    @staticmethod
    def add_autoscaling_policy(json):
        return Stratos.post('autoscalingPolicies', json,  error_message='No autoscaling policy found')

    @staticmethod
    def add_application(json):
        return Stratos.post('applications', json,  error_message='No application found')


