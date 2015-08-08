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

from cmd2 import *
from Utils import *
from Stratos import *
import Configs
from cli.exceptions import AuthenticationError


class CLI(Cmd):
    """Apache Stratos CLI"""

    prompt = Configs.stratos_prompt
    # resolving the '-' issue
    Cmd.legalChars = '-' + Cmd.legalChars

    def __init__(self):
        # resolving the '-' issue
        [Cmd.shortcuts.update({a[3:].replace('_', '-'): a[3:]}) for a in self.get_names() if a.startswith('do_')]
        Cmd.__init__(self)

    def completenames(self, text, *ignored):
        # resolving the '-' issue
        return [a[3:].replace('_', '-') for a in self.get_names() if a.replace('_', '-').startswith('do-'+text)]

    """

    Stratos CLI specific methods
    ====================================================================================================================

    # User
     * list-users
     * add-user
     * remove-user

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_users(self, line , opts=None):
        """Illustrate the base class method use."""
        try:
            users = Stratos.list_users()
            table = PrintableTable()
            rows = [["Username", "Role"]]
            for user in users:
                rows.append([user['userName'], user['role']])
            table.add_rows(rows)
            table.print_table()
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-s', '--username_user', type="str", help="Username of the user"),
        make_option('-a', '--password_user', type="str", help="Password of the user"),
        make_option('-r', '--role_name', type="str", help="Role name of the user"),
        make_option('-f', '--first_name', type="str", help="First name of the user"),
        make_option('-l', '--last_name', type="str", help="Last name of the user"),
        make_option('-e', '--email', type="str", help="Email of the user"),
        make_option('-o', '--profile_name', type="str", help="Profile name of the user")
    ])
    @auth
    def do_add_user(self, line , opts=None):
        """Add a new user to the system"""
        try:
            user = Stratos.add_users(opts.username, opts.password, opts.role_name, opts.first_name, opts.last_name,
                                       opts.email, opts.profile_name)
            if user:
                print("User successfully created")
            else:
                print("Error creating the user")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_user(self, name , opts=None):
        """Delete a specific user"""
        try:
            if not name:
                print("usage: remove-user [username]")
            else:
                user_removed = Stratos.remove_user(name)
                if user_removed:
                    print("You have successfully deleted user: "+name)
                else:
                    print("Could not delete user: "+name)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Cartridges
     * list-cartridges
     * describe-cartridge
     * add-cartridge
     * remove-cartridge

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_cartridges(self, line , opts=None):
        """Illustrate the base class method use."""
        cartridges = Stratos.list_cartridges()
        table = PrintableTable()
        rows = [["Type", "Category", "Name", "Description", "Version", "Multi-Tenant"]]
        for cartridge in cartridges:
            rows.append([cartridge['type'], cartridge['category'], cartridge['displayName'], cartridge['description'],
                         cartridge['version'], cartridge['multiTenant']])
        table.add_rows(rows)
        table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_cartridge(self, cartridge_type , opts=None):
        """Retrieve details of a specific cartridge."""
        if not cartridge_type:
            print("usage: describe-cartridge [cartridge-type]")
        else:
            try:
                cartridge = Stratos.describe_cartridge(cartridge_type)
                if not cartridge:
                    print("Cartridge not found")
                else:
                    print("-------------------------------------")
                    print("Cartridge Information:")
                    print("-------------------------------------")
                    print("Type: "+cartridge['type'])
                    print("Category: "+cartridge['category'])
                    print("Name: "+cartridge['displayName'])
                    print("Description: "+cartridge['description'])
                    print("Version: "+str(cartridge['version']))
                    print("Multi-Tenant: "+str(cartridge['multiTenant']))
                    print("Host Name: "+cartridge['host'])
                    print("-------------------------------------")
            except requests.HTTPError as e:
                self.perror("Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_cartridge(self, cartridge_type , opts=None):
        """Delete a cartridge"""
        try:
            if not cartridge_type:
                print("usage: remove-cartridge [cartridge-type]")
            else:
                cartridge_removed = Stratos.remove_cartridge(cartridge_type)
                if cartridge_removed:
                    print("Successfully un-deployed cartridge : "+cartridge_type)
                else:
                    print("Could not un-deployed cartridge : "+cartridge_type)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Cartridge groups
     * list-cartridge-groups
     * describe-cartridge-group
     * remove-cartridge-group

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_cartridge_groups(self, line , opts=None):
        """Illustrate the base class method use."""
        cartridge_groups = Stratos.list_cartridge_groups()
        if not cartridge_groups:
            print("No cartridge groups found")
        else:
            table = PrintableTable()
            rows = [["Name", "No. of cartridges", "No of groups", "Dependency scaling"]]
            for cartridge_group in cartridge_groups:
                rows.append([cartridge_group['name'], len(cartridge_group['cartridges']),
                             len(cartridge_group['cartridges']), len(cartridge_group['dependencies'])])
            table.add_rows(rows)
            table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_cartridge_group(self, group_definition_name , opts=None):
        """Retrieve details of a cartridge group."""
        if not group_definition_name:
            print("usage: describe-cartridge-group [cartridge-group-name]")
            return
        cartridge_group = Stratos.describe_cartridge_group(group_definition_name)
        if not cartridge_group:
            print("Cartridge group not found")
        else:
            print("Service Group : "+group_definition_name)
            PrintableJSON(cartridge_group).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_cartridge_group(self, group_definition_name , opts=None):
        """Delete a cartridge"""
        try:
            if not group_definition_name:
                print("usage: remove-cartridge-group [cartridge-group-name]")
            else:
                cartridge_removed = Stratos.remove_cartridge_group(group_definition_name)
                if cartridge_removed:
                    print("Successfully un-deployed cartridge group : "+group_definition_name)
                else:
                    print("Could not un-deployed cartridge group : "+group_definition_name)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Deployment Policies
     * list-deployment-policies
     * describe-deployment-policy
     * remove-deployment-policy

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_deployment_policies(self, line , opts=None):
        """Illustrate the base class method use."""
        deployment_policies = Stratos.list_deployment_policies()
        if not deployment_policies:
            print("No deployment policies found")
        else:
            table = PrintableTable()
            rows = [["Id", "Accessibility"]]
            for deployment_policy in deployment_policies:
                rows.append([deployment_policy['id'], len(deployment_policy['networkPartitions'])])
            table.add_rows(rows)
            table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_deployment_policy(self, line , opts=None):
        """Retrieve details of a specific deployment policy."""
        if not line.split():
            print("usage: describe-deployment-policy [deployment-policy-id]")
            return
        deployment_policy = Stratos.describe_deployment_policy(line)
        if not deployment_policy:
            print("Deployment policy not found")
        else:
            PrintableJSON(deployment_policy).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_deployment_policy(self, deployment_policy_id , opts=None):
        """Delete a cartridge"""
        try:
            if not deployment_policy_id:
                print("usage: remove-deployment-policy [deployment-policy-id]")
            else:
                cartridge_removed = Stratos.remove_deployment_policy(deployment_policy_id)
                if cartridge_removed:
                    print("Successfully deleted deployment policy : "+deployment_policy_id)
                else:
                    print("Could not deleted deployment policy : "+deployment_policy_id)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Network Partitions
     * list-deployment-policies
     * describe-deployment-policy
     * remove-deployment-policy

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_network_partitions(self, line , opts=None):
        """Illustrate the base class method use."""
        network_partitions = Stratos.list_network_partitions()
        table = PrintableTable()
        rows = [["Network Partition ID", "Number of Partitions"]]
        for network_partition in network_partitions:
            rows.append([network_partition['id'], len(network_partition['partitions'])])
        table.add_rows(rows)
        table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_network_partition(self, network_partition_id , opts=None):
        """Retrieve details of a specific deployment policy."""
        if not network_partition_id:
            print("usage: describe-network-partition [network-partition]")
            return
        deployment_policy = Stratos.describe_network_partition(network_partition_id)
        if not deployment_policy:
            print("Network partition not found: "+network_partition_id)
        else:
            print("Partition: "+network_partition_id)
            PrintableJSON(deployment_policy).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_network_partition(self, network_partition_id, opts=None):
        """Delete a cartridge"""
        try:
            if not network_partition_id:
                print("usage: remove-network-partition [network-partition-id]")
            else:
                cartridge_removed = Stratos.remove_network_partition(network_partition_id)
                if cartridge_removed:
                    print("Successfully deleted network-partition : "+network_partition_id)
                else:
                    print("Could not deleted network-partition : "+network_partition_id)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Auto-scaling policies
     * list-autoscaling-policies
     * describe-autoscaling-policy
     * remove-autoscaling-policy

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_autoscaling_policies(self, line , opts=None):
        """Retrieve details of all the cartridge groups that have been added."""
        autoscaling_policies = Stratos.list_autoscaling_policies()
        if not autoscaling_policies:
            print("No autoscaling policies found")
        else:
            table = PrintableTable()
            rows = [["Id", "Accessibility"]]
            for autoscaling_policy in autoscaling_policies:
                rows.append([autoscaling_policy['id'], "Public"  if autoscaling_policy['isPublic'] else "Private"])
            table.add_rows(rows)
            table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_autoscaling_policy(self, autoscaling_policy_id , opts=None):
        """Retrieve details of a specific auto-scaling policy."""
        if not autoscaling_policy_id:
            print("usage: describe-autoscaling-policy [autoscaling-policy-id]")
            return
        autoscaling_policy = Stratos.describe_autoscaling_policy(autoscaling_policy_id)
        if not autoscaling_policy:
            print("Autoscaling policy not found : "+autoscaling_policy_id)
        else:
            print("Autoscaling policy : "+autoscaling_policy_id)
            PrintableJSON(autoscaling_policy).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_autoscaling_policy(self, autoscaling_policy_id , opts=None):
        """Delete a cartridge"""
        try:
            if not autoscaling_policy_id:
                print("usage: remove-autoscaling-policy [application-id]")
            else:
                cartridge_removed = Stratos.remove_autoscaling_policy(autoscaling_policy_id)
                if cartridge_removed:
                    print("Successfully deleted Auto-scaling policy : "+autoscaling_policy_id)
                else:
                    print("Auto-scaling policy not found : "+autoscaling_policy_id)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Kubernetes clusters/hosts
     * list-kubernetes-clusters
     * describe-kubernetes-cluster
     * list-kubernetes-hosts
     * describe-kubernetes-master
     * remove-kubernetes-cluster
     * remove-kubernetes-host

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_kubernetes_clusters(self, line , opts=None):
        """Retrieve detailed information on all Kubernetes-CoreOS Clusters."""
        kubernetes_clusters = Stratos.list_kubernetes_clusters()
        if not kubernetes_clusters:
            print("No Kubernetes clusters found")
        else:
            table = PrintableTable()
            rows = [["Group ID", "Description"]]
            for kubernetes_cluster in kubernetes_clusters:
                rows.append([kubernetes_cluster['clusterId'], kubernetes_cluster['description']])
            table.add_rows(rows)
            table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_kubernetes_cluster(self, kubernetes_cluster_id, opts=None):
        """Retrieve detailed information on a specific Kubernetes-CoreOS group"""
        if not kubernetes_cluster_id:
            print("usage: describe-kubernetes-cluster [cluster-i]]")
            return
        kubernetes_cluster = Stratos.describe_kubernetes_cluster(kubernetes_cluster_id)
        if not kubernetes_cluster:
            print("Kubernetes cluster not found")
        else:
            PrintableJSON(kubernetes_cluster).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-c', '--cluster_id', type="str", help="Cluster ID")
    ])
    @auth
    def do_list_kubernetes_hosts(self, line , opts=None):
        """Retrieve detailed information on all Kubernetes-CoreOS Clusters."""
        if not opts.cluster_id:
            print("usage: list-kubernetes-hosts [-c <cluster id>]")
            return
        kubernetes_cluster_hosts = Stratos.list_kubernetes_hosts(opts.cluster_id)
        if not kubernetes_cluster_hosts:
            print("No kubernetes hosts found")
        else:
            table = PrintableTable()
            rows = [["Host ID", "Hostname", "Private IP Address", "Public IP Address"]]
            for kubernetes_cluster_host in kubernetes_cluster_hosts:
                rows.append([kubernetes_cluster_host['hostId'], kubernetes_cluster_host['hostname'],
                             kubernetes_cluster_host['privateIPAddress'], kubernetes_cluster_host['publicIPAddress']])
            table.add_rows(rows)
            table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_kubernetes_master(self, kubernetes_cluster_id , opts=None):
        """Retrieve detailed information on the master node in a specific Kubernetes-CoreOS group"""
        if not kubernetes_cluster_id:
            print("usage: describe-kubernetes-master [cluster-id]")
            return
        kubernetes_master = Stratos.describe_kubernetes_master(kubernetes_cluster_id)
        if not kubernetes_master:
            print("Kubernetes master not found in : "+kubernetes_cluster_id)
        else:
            print("Cluster : "+kubernetes_cluster_id)
            PrintableJSON(kubernetes_master).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_kubernetes_cluster(self, kubernetes_cluster_id, opts=None):
        """Delete a cartridge"""
        try:
            if not kubernetes_cluster_id:
                print("usage: remove-kubernetes-cluster [cluster-id]")
            else:
                cartridge_removed = Stratos.remove_autoscaling_policy(kubernetes_cluster_id)
                if cartridge_removed:
                    print("Successfully un-deployed kubernetes cluster : "+kubernetes_cluster_id)
                else:
                    print("Kubernetes cluster not found : "+kubernetes_cluster_id)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_applications(self, line , opts=None):
        """Illustrate the base class method use."""
        applications = Stratos.list_applications()
        if not applications:
            print("No applications found")
        else:
            table = PrintableTable()
            rows = [["Type", "Category", "Name", "Description", "Version", "Multi-Tenant"]]
            for application in applications:
                rows.append([application['type'], application['category'], application['displayName'],
                             application['description'], application['version'], application['multiTenant']])
            table.add_rows(rows)
            table.print_table()


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-t', '--tenant_domain', type="str", help="Cluster ID")
    ])
    @auth
    def do_activate_tenant(self, line , opts=None):
        """Retrieve detailed information on all Kubernetes-CoreOS Clusters."""
        if not opts.tenant_domain:
            print("usage: list-kubernetes-hosts [-c <tenant domain>]")
            return
        kubernetes_cluster_hosts = Stratos.list_kubernetes_hosts(opts.cluster_id)
        if not kubernetes_cluster_hosts:
            print("No kubernetes hosts found")
        else:
            table = PrintableTable()
            rows = [["Host ID", "Hostname", "Private IP Address", "Public IP Address"]]
            for kubernetes_cluster_host in kubernetes_cluster_hosts:
                rows.append([kubernetes_cluster_host['hostId'], kubernetes_cluster_host['hostname'],
                             kubernetes_cluster_host['privateIPAddress'], kubernetes_cluster_host['publicIPAddress']])
            table.add_rows(rows)
            table.print_table()

    @options([])
    def do_deploy_user(self, line , opts=None):
        """Illustrate the base class method use."""
        print("hello User")
        try:
            Stratos.deploy_user()
        except ValueError as e:
            self.perror("sdc")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application_signup(self, line , opts=None):
        """Retrieve details of a specific auto-scaling policy."""
        if not line.split():
            print("usage: describe-application-signup [application-id]")
            return
        application_signup = Stratos.describe_application_signup(line)
        if not application_signup:
            print("Application signup not found")
        else:
            PrintableJSON(application_signup).pprint()