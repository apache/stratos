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
from rpm._rpm import te
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
     * update-user
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
            user = Stratos.add_users(opts.username_user, opts.password_user, opts.role_name, opts.first_name, opts.last_name,
                                       opts.email, opts.profile_name)
            if user:
                print("User successfully created")
            else:
                print("Error creating the user")
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
    def do_update_user(self, line , opts=None):
        """Add a new user to the system"""
        try:
            user = Stratos.update_user(opts.username_user, opts.password_user, opts.role_name, opts.first_name, opts.last_name,
                                       opts.email, opts.profile_name)
            if user:
                print("User successfully updated")
            else:
                print("Error updating the user")
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
    # Applications
     * list-applications
     * describe-application
     * add-application
     * remove-application

    """

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
            rows = [["Application ID", "Alias", "Status"]]
            for application in applications:
                PrintableJSON(application).pprint()
                rows.append([application['applicationId'], application['alias'], application['status']])
            table.add_rows(rows)
            table.print_table()


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application(self, application_id , opts=None):
        """Retrieve detailed information on the master node in a specific Kubernetes-CoreOS group"""
        if not application_id:
            print("usage: describe-application [cluster-id]")
            return
        application = Stratos.describe_application(application_id)
        if not application:
            print("Application not found in : "+application_id)
        else:
            print("Application : "+application_id)
            PrintableJSON(application).pprint()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_application(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-application [-f <resource path>]")
            else:
                add_application = Stratos.add_application(open(opts.json_file_path, 'r').read())
                if add_application:
                    print("Application added successfully")
                else:
                    print("Error adding application")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_application(self, application , opts=None):
        """Delete a specific user"""
        try:
            if not application:
                print("usage: remove-application [application]")
            else:
                application_removed = Stratos.remove_application(application)
                if application_removed:
                    print("You have successfully removed application: "+application)
                else:
                    print("Could not delete application : "+application)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    """
    # Application deployment
     * describe-application-runtime
     * deploy-application

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application_runtime(self, application_id , opts=None):
        """Retrieve details of a specific auto-scaling policy."""
        if not application_id:
            print("usage: describe-application-runtime [application-id]")
            return
        application_runtime = Stratos.describe_application_runtime(application_id)
        if not application_runtime:
            print("Application runtime not found")
        else:
            print("Application : "+application_id)
            PrintableJSON(application_runtime).pprint()

    """
    # Application signup
     * describe-application-signup

    """
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



    """
    # Tenants
     * list-tenants
     * list-tenants-by-partial-domain
     * describe-tenant
     * add-tenant
     * activate-tenant
     * deactivate-tenant

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_tenants(self, line , opts=None):
        """Illustrate the base class method use."""
        tenants = Stratos.list_tenants()
        table = PrintableTable()
        rows = [["Domain", "Tenant ID", "Email", " State", "Created Date"]]
        for tenant in tenants:
            rows.append([tenant['tenantDomain'], tenant['tenantId'], tenant['email'],
                         "Active" if tenant['active'] else "De-Active", datetime.datetime.fromtimestamp(tenant['createdDate']/1000).strftime('%Y-%m-%d %H:%M:%S')])
        table.add_rows(rows)
        table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_tenants_by_partial_domain(self, partial_domain , opts=None):
        """Illustrate the base class method use."""
        tenants = Stratos.list_tenants_by_partial_domain(partial_domain)
        table = PrintableTable()
        rows = [["Domain", "Tenant ID", "Email", " State", "Created Date"]]
        for tenant in tenants:
            rows.append([tenant['tenantDomain'], tenant['tenantId'], tenant['email'],
                         "Active" if tenant['active'] else "De-Active", datetime.datetime.fromtimestamp(tenant['createdDate']/1000).strftime('%Y-%m-%d %H:%M:%S')])
        table.add_rows(rows)
        table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_tenant(self, tenant_domain_name, opts=None):
        """Retrieve details of a specific tenant."""
        if not tenant_domain_name:
            print("usage: describe-tenant [Domain-Name]")
        else:
            try:
                tenant = Stratos.describe_tenant(tenant_domain_name)
                if not tenant:
                    print("Tenant not found")
                else:
                    print("-------------------------------------")
                    print("Tenant Information:")
                    print("-------------------------------------")
                    print("Tenant domain: "+tenant['tenantDomain'])
                    print("ID: "+str(tenant['tenantId']))
                    print("Active: "+str(tenant['active']))
                    print("Email: "+tenant['email'])
                    print("Created date: "+datetime.datetime.fromtimestamp(tenant['createdDate']/1000).strftime('%Y-%m-%d %H:%M:%S'))
                    print("-------------------------------------")
            except requests.HTTPError as e:
                self.perror("Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-s', '--username_user', type="str", help="Username of the tenant"),
        make_option('-a', '--password_user', type="str", help="Password of the tenant"),
        make_option('-d', '--domain_name', type="str", help="domain name of the tenant"),
        make_option('-f', '--first_name', type="str", help="First name of the tenant"),
        make_option('-l', '--last_name', type="str", help="Last name of the tenant"),
        make_option('-e', '--email', type="str", help="Email of the tenant")
    ])
    @auth
    def do_add_tenant(self, line , opts=None):
        """Add a new user to the system"""
        try:
            tenant = Stratos.add_tenant(opts.username_user, opts.first_name, opts.last_name, opts.password_user,
                                        opts.domain_name, opts.email)
            if tenant:
                print("Tenant added successfully : "+opts.domain_name)
            else:
                print("Error creating the tenant : "+opts.domain_name)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-s', '--username_user', type="str", help="Username of the tenant"),
        make_option('-a', '--password_user', type="str", help="Password of the tenant"),
        make_option('-d', '--domain_name', type="str", help="domain name of the tenant"),
        make_option('-f', '--first_name', type="str", help="First name of the tenant"),
        make_option('-l', '--last_name', type="str", help="Last name of the tenant"),
        make_option('-e', '--email', type="str", help="Email of the tenant"),
        make_option('-i', '--tenant_id', type="str", help="ID of the tenant")
    ])
    @auth
    def do_update_tenant(self, line , opts=None):
        """Add a new user to the system"""
        try:
            tenant = Stratos.update_tenant(opts.username_user, opts.first_name, opts.last_name, opts.password_user,
                                           opts.domain_name, opts.email, opts.tenant_id)
            if tenant:
                print("Tenant updated successfully : "+opts.domain_name)
            else:
                print("Error updating the tenant : "+opts.domain_name)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_activate_tenant(self, tenant_domain, opts=None):
        """Add a new user to the system"""
        try:
            if not tenant_domain:
                print("usage: activate-tenant <TENANT_DOMAIN> ")
            else:
                activate_tenant = Stratos.activate_tenant(tenant_domain)
                if activate_tenant:
                    print("You have successfully activated the tenant : "+tenant_domain)
                else:
                    print("Could not activate tenant : "+tenant_domain)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_deactivate_tenant(self, tenant_domain, opts=None):
        """Add a new user to the system"""
        try:
            if not tenant_domain:
                print("usage: deactivate-tenant <TENANT_DOMAIN> ")
            else:
                activate_tenant = Stratos.deactivate_tenant(tenant_domain)
                if activate_tenant:
                    print("You have successfully deactivated the tenant : "+tenant_domain)
                else:
                    print("Could not deactivate tenant : "+tenant_domain)
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
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_cartridge(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-cartridge [-f <resource path>]")
            else:
                cartridge = Stratos.add_cartridge(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Cartridge added successfully")
                else:
                    print("Error adding Cartridge")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_cartridge(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-cartridge [-f <resource path>]")
            else:
                cartridge = Stratos.update_cartridge(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Cartridge updated successfully")
                else:
                    print("Error updating Cartridge")
        except AuthenticationError as e:
            self.perror("Authentication Error")

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
     * add-cartridge-group
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
            rows = [["Name", "No. of cartridges", "No of groups"]]
            for cartridge_group in cartridge_groups:
                rows.append([cartridge_group['name'], len(cartridge_group['cartridges']),
                             len(cartridge_group['cartridges'])])
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
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_cartridge_group(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-cartridge-group [-f <resource path>]")
            else:
                cartridge_group = Stratos.add_cartridge_group(open(opts.json_file_path, 'r').read())
                if cartridge_group:
                    print("Cartridge group added successfully")
                else:
                    print("Error adding Cartridge group")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_cartridge_group(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-cartridge-group [-f <resource path>]")
            else:
                cartridge = Stratos.update_cartridge_group(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Cartridge group updated successfully")
                else:
                    print("Error updating Cartridge group")
        except AuthenticationError as e:
            self.perror("Authentication Error")

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
     * update-deployment-policy
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
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_deployment_policy(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-deployment-policy [-f <resource path>]")
            else:
                cartridge = Stratos.update_deployment_policy(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Deployment policy updated successfully")
                else:
                    print("Error updating Deployment policy")
        except AuthenticationError as e:
            self.perror("Authentication Error")

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
     * update-deployment-policy
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
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_network_partition(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-network-partition [-f <resource path>]")
            else:
                cartridge = Stratos.update_network_partition(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Network partition updated successfully")
                else:
                    print("Error updating Network partition")
        except AuthenticationError as e:
            self.perror("Authentication Error")

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
     * update-autoscaling-policy
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
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_autoscaling_policy(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-autoscaling-policy [-f <resource path>]")
            else:
                autoscaling_policy = Stratos.update_autoscaling_policy(open(opts.json_file_path, 'r').read())
                if autoscaling_policy:
                    print("Autoscaling policy updated successfully:")
                else:
                    print("Error updating Autoscaling policy")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_autoscaling_policy(self, autoscaling_policy_id , opts=None):
        """Delete a autoscaling_policy"""
        try:
            if not autoscaling_policy_id:
                print("usage: remove-autoscaling-policy [application-id]")
            else:
                autoscaling_policy_removed = Stratos.remove_autoscaling_policy(autoscaling_policy_id)
                if autoscaling_policy_removed:
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
     * update-kubernetes-host
     * update-kubernetes-master
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
            print("Kubernetes cluster: "+kubernetes_cluster_id)
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
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-c', '--cluster_id', type="str", help="Cluster id of the cluster"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_kubernetes_master(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-kubernetes-master [-c <cluster id>] [-p <resource path>]")
            else:
                cartridge = Stratos.update_kubernetes_master(opts.cluster_id, open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Kubernetes master updated successfully")
                else:
                    print("Error updating Kubernetes master")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
         make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_kubernetes_host(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-kubernetes-host [-f <resource path>]")
            else:
                cartridge = Stratos.update_kubernetes_host(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Kubernetes host updated successfully")
                else:
                    print("Error updating Kubernetes host")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_kubernetes_cluster(self, kubernetes_cluster_id, opts=None):
        """Delete a kubernetes cluster"""
        try:
            if not kubernetes_cluster_id:
                print("usage: remove-kubernetes-cluster [cluster-id]")
            else:
                kubernetes_cluster_removed = Stratos.remove_kubernetes_cluster(kubernetes_cluster_id)
                if kubernetes_cluster_removed:
                    print("Successfully un-deployed kubernetes cluster : "+kubernetes_cluster_id)
                else:
                    print("Kubernetes cluster not found : "+kubernetes_cluster_id)
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-c', '--cluster_id', type="str", help="Cluster id of Kubernets cluster"),
        make_option('-o', '--host_id', type="str", help="Host id of Kubernets cluster")
    ])
    @auth
    def do_remove_kubernetes_host(self, line, opts=None):
        """Delete a kubernetes host"""
        try:
            if not opts.cluster_id or not opts.host_id:
                print("usage: remove-kubernetes-host [-c cluster-id] [-o host-id]")
            else:
                kubernetes_host_removed = Stratos.remove_kubernetes_host(opts.cluster_id, opts.host_id)
                if kubernetes_host_removed:
                    print("Successfully un-deployed kubernetes host : "+opts.host_id)
                else:
                    print("Kubernetes host not found : "+opts.cluster_id+"/"+opts.host_id)
        except AuthenticationError as e:
            self.perror("Authentication Error")


    """
    # Domain Mapping
     * list-domain-mappings

    """
    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_domain_mappings(self, application_id , opts=None):
        """Illustrate the base class method use."""
        tenants = Stratos.list_domain_mappings(application_id)
        table = PrintableTable()
        rows = [["Domain", "Tenant ID", "Email", " State", "Created Date"]]
        for tenant in tenants:
            rows.append([tenant['tenantDomain'], tenant['tenantId'], tenant['email'],
                         "Active" if tenant['active'] else "De-Active", datetime.datetime.fromtimestamp(tenant['createdDate']/1000).strftime('%Y-%m-%d %H:%M:%S')])
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
    def do_remove_domain_mappings(self, domain , opts=None):
        """Delete a specific user"""
        try:
            if not domain:
                print("usage: remove-domain-mappings [domain]")
            else:
                domain_removed = Stratos.remove_domain_mappings(domain)
                if domain_removed:
                    print("You have successfully deleted domain: "+domain)
                else:
                    print("Could not delete domain: "+domain)
        except AuthenticationError as e:
            self.perror("Authentication Error")


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_application_signup(self, signup , opts=None):
        """Delete a specific user"""
        try:
            if not signup:
                print("usage: remove-application-signup [signup]")
            else:
                signup_removed = Stratos.remove_application_signup(signup)
                if signup_removed:
                    print("You have successfully remove signup: "+signup)
                else:
                    print("Could not delete application signup: "+signup)
        except AuthenticationError as e:
            self.perror("Authentication Error")


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_network_partition(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-network-partition [-f <resource path>]")
            else:
                tenant = Stratos.add_network_partition(open(opts.json_file_path, 'r').read())
                if tenant:
                    print("Network partition added successfully")
                else:
                    print("Error creating network partition")
        except AuthenticationError as e:
            self.perror("Authentication Error")


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_kubernetes_cluster(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-kubernetes-cluster [-f <resource path>]")
            else:
                tenant = Stratos.add_kubernetes_cluster(open(opts.json_file_path, 'r').read())
                if tenant:
                    print("Kubernertes cluster added successfully")
                else:
                    print("Error creating network partition")
        except AuthenticationError as e:
            self.perror("Authentication Error")


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_domain_mapping(self, application_id, opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-domain-mapping [-f <resource path>]")
            else:
                tenant = Stratos.add_domain_mapping(application_id, """{
  "domainMappings": [
    {
      "cartridgeAlias": "tomcat",
      "domainName": "agentmilindu.com",
      "contextPath": "/abc/app"
    }
  ]
}""")
                if tenant:
                    print(" Domain mapping added successfully")
                else:
                    print("Error creating domain mapping")
        except AuthenticationError as e:
            self.perror("Authentication Error")


    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_deployment_policy(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-deployment-policy [-f <resource path>]")
            else:
                deployment_policy = Stratos.add_deployment_policy(open(opts.json_file_path, 'r').read())
                if deployment_policy:
                    print("Deployment policy added successfully")
                else:
                    print("Error creating deployment policy")
        except AuthenticationError as e:
            self.perror("Authentication Error")

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_autoscaling_policy(self, line , opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-autoscaling-policy [-f <resource path>]")
            else:
                autoscaling_policy = Stratos.add_autoscaling_policy(open(opts.json_file_path, 'r').read())
                if autoscaling_policy:
                    print("Autoscaling policy added successfully")
                else:
                    print("Error adding autoscaling policy")
        except AuthenticationError as e:
            self.perror("Authentication Error")

