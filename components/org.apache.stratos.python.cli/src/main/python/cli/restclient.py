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
import config
from logutils import logging
from exception import BadResponseError


class StratosClient:
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
        return StratosClient.get('users')

    @staticmethod
    def add_users(username, password, role_name, first_name, last_name, email, profile_name):
        data = {
            "userName": username,
            "credential": password,
            "role": role_name,
            "firstName": first_name,
            "lastName": last_name,
            "email": email,
            "profileName": profile_name
        }
        return StratosClient.post('users', json.dumps(data))

    @staticmethod
    def update_user(username, password, role_name, first_name, last_name, email, profile_name):
        data = {
            "userName": username,
            "credential": password,
            "role": role_name,
            "firstName": first_name,
            "lastName": last_name,
            "email": email,
            "profileName": profile_name
        }
        return StratosClient.put('users', json.dumps(data))

    @staticmethod
    def remove_user(name):
        return StratosClient.delete('users/' + name)

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
        return StratosClient.get('applications')

    @staticmethod
    def describe_application(application_id):
        return StratosClient.get('applications/' + application_id)

    @staticmethod
    def add_application(json_input):
        return StratosClient.post('applications', json_input)

    @staticmethod
    def update_application(application_id, json_input):
        return StratosClient.put('applications/' + application_id, json_input)

    @staticmethod
    def remove_application(application):
        return StratosClient.delete('applications/' + application)

    @staticmethod
    def deploy_application(application_id, application_policy_id):
        return StratosClient.post('applications/' + application_id + '/deploy/' + application_policy_id, None)

    @staticmethod
    def undeploy_application(application_id):
        return StratosClient.post('applications/' + application_id + '/undeploy', None)

    @staticmethod
    def describe_application_runtime(application_id):
        return StratosClient.get('applications/' + application_id + "/runtime")

    """
    # Application signup
     * describe-application-signup
     * remove-application-signup

    """
    @staticmethod
    def describe_application_signup(application_id):
        return StratosClient.get('applications/' + application_id + '/signup')

    @staticmethod
    def add_application_signup(application_id, json_input):
        return StratosClient.post('applications/' + application_id + "/runtime", json_input)

    @staticmethod
    def remove_application_signup(application_id):
        return StratosClient.delete('applications/' + application_id + '/signup')

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
        return StratosClient.get('tenants')

    @staticmethod
    def list_tenants_by_partial_domain(partial_domain):
        return StratosClient.get('tenants/search/' + partial_domain)

    @staticmethod
    def describe_tenant(tenant_domain_name):
        return StratosClient.get('tenants/' + tenant_domain_name)

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
        return StratosClient.post('tenants', json.dumps(data))

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
        return StratosClient.put('tenants', json.dumps(data))

    @staticmethod
    def activate_tenant(tenant_domain):
        return StratosClient.put('tenants/activate/' + tenant_domain, "")

    @staticmethod
    def deactivate_tenant(tenant_domain):
        return StratosClient.put('tenants/deactivate/' + tenant_domain, "")

    """
    # Cartridges
     * list-cartridges
     * describe-cartridge
     * add-cartridge
     * remove-cartridges

    """
    @staticmethod
    def list_cartridges():
        return StratosClient.get('cartridges')

    @staticmethod
    def list_cartridges_by_filter(filter_text):
        return StratosClient.get('cartridges/filter/' + filter_text)

    @staticmethod
    def describe_cartridge(cartridge_type):
        return StratosClient.get('cartridges/' + cartridge_type)

    @staticmethod
    def add_cartridge(json_input):
        return StratosClient.post('cartridges', json_input)

    @staticmethod
    def update_cartridge(json_input):
        return StratosClient.put('cartridges', json_input)

    @staticmethod
    def remove_cartridge(cartridge_type):
        return StratosClient.delete('cartridges/' + cartridge_type)

    """
    # Cartridge groups
     * list-cartridge-groups
     * describe-cartridge-group
     * update-cartridges-group
     * remove-cartridges-group

    """

    @staticmethod
    def list_cartridge_groups():
        return StratosClient.get('cartridgeGroups')

    @staticmethod
    def describe_cartridge_group(group_definition_name):
        return StratosClient.get('cartridgeGroups/' + group_definition_name)

    @staticmethod
    def add_cartridge_group(json_input):
        return StratosClient.post('cartridgeGroups', json_input)

    @staticmethod
    def update_cartridge_group(json_input):
        return StratosClient.put('cartridgeGroups', json_input)

    @staticmethod
    def remove_cartridge_group(group_definition_name):
        return StratosClient.delete('cartridgeGroups/' + group_definition_name)

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
        return StratosClient.get('deploymentPolicies')

    @staticmethod
    def describe_deployment_policy(deployment_policy_name):
        return StratosClient.get('deploymentPolicies/' + deployment_policy_name)

    @staticmethod
    def add_deployment_policy(json_input):
        return StratosClient.post('deploymentPolicies', json_input)

    @staticmethod
    def update_deployment_policy(json_input):
        return StratosClient.put('deploymentPolicies', json_input)

    @staticmethod
    def remove_deployment_policy(deployment_policy_id):
        return StratosClient.delete('deploymentPolicies/' + deployment_policy_id)

    """
    # Application Policy
     * list-application-policies
     * describe-application-policy
     * update-application-policy
     * remove-application-policy

    """
    @staticmethod
    def list_application_policies():
        return StratosClient.get('applicationPolicies')

    @staticmethod
    def describe_application_policy(application_policy_name):
        return StratosClient.get('applicationPolicies/' + application_policy_name)

    @staticmethod
    def update_application_policy(json_input):
        return StratosClient.put('applicationPolicies', json_input)

    @staticmethod
    def remove_application_policy(application_policy_id):
        return StratosClient.delete('applicationPolicies/' + application_policy_id)

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
        return StratosClient.get('networkPartitions')

    @staticmethod
    def describe_network_partition(network_partition_id):
        return StratosClient.get('networkPartitions/' + network_partition_id)

    @staticmethod
    def add_network_partition(json_input):
        return StratosClient.post('networkPartitions', json_input)

    @staticmethod
    def update_network_partition(json_input):
        return StratosClient.put('networkPartitions', json_input)

    @staticmethod
    def remove_network_partition(network_partition_id):
        return StratosClient.delete('networkPartitions/' + network_partition_id)

    """
    # Auto-scaling policies
     * list-autoscaling-policies
     * describe-autoscaling-policy
     * add-autoscaling-policy
     * update-autoscaling-policy
     * remove-autoscaling-policy

    """
    @staticmethod
    def list_autoscaling_policies():
        return StratosClient.get('autoscalingPolicies')

    @staticmethod
    def describe_autoscaling_policy(autoscaling_policy_id):
        return StratosClient.get('autoscalingPolicies/' + autoscaling_policy_id)

    @staticmethod
    def add_autoscaling_policy(json_input):
        return StratosClient.post('autoscalingPolicies', json_input)

    @staticmethod
    def add_application_policy(json_input):
        return StratosClient.post('applicationPolicies', json_input)

    @staticmethod
    def update_autoscaling_policy(json_input):
        return StratosClient.put('autoscalingPolicies', json_input)

    @staticmethod
    def remove_autoscaling_policy(autoscaling_policy_id):
        return StratosClient.delete('autoscalingPolicies/' + autoscaling_policy_id)

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
        return StratosClient.get('kubernetesClusters')

    @staticmethod
    def describe_kubernetes_cluster(kubernetes_cluster_id):
        return StratosClient.get('kubernetesClusters/' + kubernetes_cluster_id)

    @staticmethod
    def describe_kubernetes_master(kubernetes_cluster_id):
        return StratosClient.get('kubernetesClusters/' + kubernetes_cluster_id + '/master')

    @staticmethod
    def add_kubernetes_cluster(json_input):
        return StratosClient.post('kubernetesClusters', json_input)

    @staticmethod
    def add_kubernetes_host(kubernetes_cluster_id, json_input):
        return StratosClient.post('kubernetesClusters/' + kubernetes_cluster_id + '/minion', json_input)

    @staticmethod
    def list_kubernetes_hosts(kubernetes_cluster_id):
        return StratosClient.get('kubernetesClusters/' + kubernetes_cluster_id + '/hosts')

    @staticmethod
    def update_kubernetes_master(cluster_id, json_input):
        return StratosClient.put('kubernetesClusters/' + cluster_id + '/master', json_input)

    @staticmethod
    def update_kubernetes_host(json_input):
        return StratosClient.put('kubernetesClusters/update/host', json_input)

    @staticmethod
    def remove_kubernetes_cluster(kubernetes_cluster_id):
        return StratosClient.delete('kubernetesClusters/' + kubernetes_cluster_id)

    @staticmethod
    def remove_kubernetes_host(kubernetes_cluster_id, host_id):
        return StratosClient.delete('kubernetesClusters/' + kubernetes_cluster_id + "/hosts/" + host_id)

    """
    # Domain Mapping
     * list-domain-mappings
     * add-domain-mapping
     * remove-domain-mapping

    """

    @staticmethod
    def list_domain_mappings(application_id):
        return StratosClient.get('applications/' + application_id + '/domainMappings')

    @staticmethod
    def remove_domain_mappings(application_id):
        return StratosClient.delete('applications/' + application_id + '/domainMappings')

    @staticmethod
    def add_domain_mapping(application_id, json_input):
        return StratosClient.post('applications/' + application_id + '/domainMappings', json_input)

    """
    # Utils

    """

    @staticmethod
    def authenticate():
        StratosClient.get('init')

    @staticmethod
    def get(resource):
        r = requests.get(config.stratos_api_url + resource,
                         auth=(config.stratos_username, config.stratos_password), verify=False)
        return StratosClient.response(r)

    @staticmethod
    def delete(resource):
        r = requests.delete(config.stratos_api_url + resource,
                            auth=(config.stratos_username, config.stratos_password), verify=False)
        return StratosClient.response(r)

    @staticmethod
    def post(resource, data):
        headers = {'content-type': 'application/json'}
        r = requests.post(config.stratos_api_url + resource, data, headers=headers,
                          auth=(config.stratos_username, config.stratos_password), verify=False)
        return StratosClient.response(r)

    @staticmethod
    def put(resource, data):
        headers = {'content-type': 'application/json'}
        r = requests.put(config.stratos_api_url + resource, data, headers=headers,
                         auth=(config.stratos_username, config.stratos_password), verify=False)
        return StratosClient.response(r)

    @staticmethod
    def response(r):
        if "False" not in config.debug_cli:
            # print responses if debug is turned on
            print(r)
            print(r.text)

        if r.status_code == 200:
            return r.json()
        elif r.status_code == 201:
            return True
        elif r.status_code == 202:
            return True
        elif r.status_code >= 400:
            if r.text and r.json() and r.json()['message']:
                logging.error("HTTP "+str(r.status_code)+" : "+r.json()['message'])
                raise BadResponseError(str(r.status_code), r.json()['message'])
            else:
                logging.error("HTTP "+str(r.status_code)+" : Could not connect to Stratos server")
                raise BadResponseError(str(r.status_code), "Could not connect to Stratos server")
