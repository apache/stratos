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



    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_repositories(self, line, opts=None):
        """ Shows the git repositories of the user identified by given the username and password
          eg: repositories -u agentmilindu  -p agentmilindu123 """

        r = requests.get('https://api.github.com/users/' + Configs.stratos_username + '/repos?per_page=5',
                         auth=(Configs.stratos_username, Configs.stratos_password))
        repositories = r.json()
        print(r)
        print(repositories)
        table = PrintableTable()
        rows = [["Name", "language"]]
        table.set_cols_align(["l", "r"])
        table.set_cols_valign(["t", "m"])

        for repo in repositories:
            rows.append([repo['name'], repo['language']])
        print(rows)
        table.add_rows(rows)
        table.print_table()

    """
    # User Entity

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-r', '--role_name', type="str", help="Role name of the user"),
        make_option('-f', '--first_name', type="str", help="First name of the user"),
        make_option('-l', '--last_name', type="str", help="Last name of the user"),
        make_option('-e', '--email', type="str", help="Email of the user"),
        make_option('-x', '--profile_name', type="str", help="Profile name of the user")
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
    def do_list_users(self, line , opts=None):
        """Illustrate the base class method use."""
        try:
            users = Stratos.list_users()
            table = PrintableTable()
            rows = [["Name", "language"]]
            table.set_cols_align(["l", "r"])
            table.set_cols_valign(["t", "m"])
            for user in users:
                rows.append([user['role'], user['userName']])
            table.add_rows(rows)
            table.print_table()
        except AuthenticationError as e:
            self.perror("Authentication Error")


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
    def do_list_cartridge_groups(self, line , opts=None):
        """Illustrate the base class method use."""
        cartridge_groups = Stratos.list_cartridge_groups()
        table = PrintableTable()
        rows = [["Name", "No. of cartridges", "No of groups", "Dependency scaling"]]
        for cartridge_group in cartridge_groups:
            rows.append([cartridge_group['name'], len(cartridge_group['cartridges']),
                         len(cartridge_group['cartridges']), cartridge_group['groupScalingEnabled']])
        table.add_rows(rows)
        table.print_table()

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
    """
    # Kubernetes Cluster/Host

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
