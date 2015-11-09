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

from __future__ import print_function
from cmd2 import *
from texttable import *
from restclient import *
from exception import BadResponseError
from utils import *


class CLI(Cmd):
    """Apache Stratos CLI"""

    prompt = config.stratos_prompt
    # resolving the '-' issue
    Cmd.legalChars = '-' + Cmd.legalChars

    def __init__(self):
        # resolving the '-' issue
        [Cmd.shortcuts.update({a[3:].replace('_', '-'): a[3:]}) for a in self.get_names() if a.startswith('do_')]
        Cmd.__init__(self)

    def completenames(self, text, *ignored):
        # resolving the '-' issue
        return [a[3:].replace('_', '-') for a in self.get_names() if a.replace('_', '-').startswith('do-' + text)]

    def do_help(self, arg):
        """
        Override help display function
        """

        if arg:
            Cmd.do_help(self, arg)
        else:
            cmds_doc = ["list-applications",
                        "list-application-policies",
                        "list-autoscaling-policies",
                        "list-cartridges",
                        "list-cartridges-by-filter",
                        "list-cartridge-groups",
                        "list-deployment-policies",
                        "list-domain-mappings",
                        "list-kubernetes-clusters",
                        "list-kubernetes-hosts",
                        "list-network-partitions",
                        "list-tenants",
                        "list-tenants-by-partial-domain",
                        "list-users",
                        "",
                        "activate-tenant",
                        "deactivate-tenant",
                        "",
                        "add-application",
                        "add-application-policy",
                        "add-application-signup",
                        "add-autoscaling-policy",
                        "add-cartridge",
                        "add-cartridge-group",
                        "add-deployment-policy",
                        "add-domain-mapping",
                        "add-kubernetes-cluster",
                        "add-kubernetes-host",
                        "add-network-partition",
                        "add-tenant",
                        "add-user",
                        "",
                        "describe-application",
                        "describe-application-policy",
                        "describe-application-runtime",
                        "describe-application-signup",
                        "describe-autoscaling-policy",
                        "describe-cartridge",
                        "describe-cartridge-group",
                        "describe-deployment-policy",
                        "describe-kubernetes-cluster",
                        "describe-kubernetes-master",
                        "describe-network-partition",
                        "describe-tenant",
                        "",
                        "remove-application",
                        "remove-application-policy",
                        "remove-application-signup",
                        "remove-autoscaling-policy",
                        "remove-cartridge",
                        "remove-cartridge-group",
                        "remove-deployment-policy",
                        "remove-domain-mappings",
                        "remove-kubernetes-cluster",
                        "remove-kubernetes-host",
                        "remove-network-partition",
                        "remove-user",
                        "",
                        "deploy-application",
                        "undeploy-application",
                        "",
                        "update-application",
                        "update-application-policy",
                        "update-autoscaling-policy",
                        "update-cartridge",
                        "update-cartridge-group",
                        "update-deployment-policy",
                        "update-kubernetes-host",
                        "update-kubernetes-master",
                        "update-network-partition"
                        "update-tenant",
                        "update-user",
                        "",
                        "history"]

            self.stdout.write("%s\n" % str(self.doc_leader))
            self.print_topics(self.doc_header, cmds_doc, 15, 80)
            # self.print_topics(self.misc_header, help_commands.keys(), 15, 80)
            # self.print_topics(self.undoc_header, cmds_undoc, 15, 80)

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
    def do_list_users(self, line, opts=None):
        """Retrieve details of all users."""
        try:
            users = StratosClient.list_users()
            table = PrintableTable()
            rows = [["Username", "Role"]]
            for user in users:
                rows.append([user['userName'], user['role']])
            table.add_rows(rows)
            table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

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
    def do_add_user(self, line, opts=None):
        """Add a user."""
        try:
            if not opts.username_user or not opts.password_user:
                print("usage: add-user [-s <username>] [-a <credential>] [-r <role>] [-e <email>] [-f <first name>]" +
                      " [-l <last name>] [-o <profile name>]")
                return
            else:
                user = StratosClient.add_users(opts.username_user, opts.password_user, opts.role_name, opts.first_name,
                                               opts.last_name, opts.email, opts.profile_name)
                if user:
                    print("User successfully created")
                else:
                    print("Error creating the user")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-s', '--username_user', type="str", help="Username of the user to be created"),
        make_option('-a', '--password_user', type="str", help="Password of the user to be created"),
        make_option('-r', '--role_name', type="str", help="Role name of the user to be created"),
        make_option('-f', '--first_name', type="str", help="First name of the user to be created"),
        make_option('-l', '--last_name', type="str", help="Last name of the user to be created"),
        make_option('-e', '--email', type="str", help="Email of the user to be created"),
        make_option('-o', '--profile_name', type="str", help="Profile name of the user to be created")
    ])
    @auth
    def do_update_user(self, line, opts=None):
        """Update a specific user."""
        try:
            user = StratosClient.update_user(opts.username_user, opts.password_user, opts.role_name, opts.first_name,
                                             opts.last_name, opts.email, opts.profile_name)
            if user:
                print("User successfully updated")
            else:
                print("Error updating the user")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_user(self, name, opts=None):
        """Delete a user."""
        try:
            if not name:
                print("usage: remove-user [username]")
            else:
                user_removed = StratosClient.remove_user(name)
                if user_removed:
                    print("You have successfully deleted user: " + name)
                else:
                    print("Could not delete user: " + name)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Applications
     * list-applications
     * describe-application
     * add-application
     * update-application
     * remove-application
     * describe-application-runtime
     * deploy-application

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_applications(self, line, opts=None):
        """Retrieve details of all the applications."""
        try:
            applications = StratosClient.list_applications()
            if not applications:
                print("No applications found")
            else:
                table = PrintableTable()
                rows = [["Application ID", "Alias", "Status"]]
                for application in applications:
                    rows.append([application['applicationId'], application['alias'], application['status']])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application(self, application_id, opts=None):
        """Describe an application."""
        try:
            if not application_id:
                print("usage: describe-application [cluster-id]")
                return
            application = StratosClient.describe_application(application_id)
            if not application:
                print("Application not found in : " + application_id)
            else:
                print("Application : " + application_id)
                PrintableTree(application).print_tree()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_application(self, line, opts=None):
        """Add an application."""
        try:
            if not opts.json_file_path:
                print("usage: add-application [-f <resource path>]")
            else:
                add_application = StratosClient.add_application(open(opts.json_file_path, 'r').read())
                if add_application:
                    print("Application added successfully")
                else:
                    print("Error adding application")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_application(self, application, opts=None):
        """Update an application."""
        try:
            if not opts.json_file_path:
                print("usage: update-application [-f <resource path>] [application]")
            else:
                update_application = StratosClient.update_application(application,
                                                                      open(opts.json_file_path, 'r').read())
                if update_application:
                    print("Application updated successfully")
                else:
                    print("Error updating application")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_application(self, application, opts=None):
        """Delete an application."""
        try:
            if not application:
                print("usage: remove-application [application]")
            else:
                application_removed = StratosClient.remove_application(application)
                if application_removed:
                    print("You have successfully removed application: " + application)
                else:
                    print("Could not delete application : " + application)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-a', '--application_id', type="str", help="Unique ID of the application"),
        make_option('-o', '--application_policy_id', type="str", help="Unique ID of the application policy")
    ])
    @auth
    def do_deploy_application(self, line, opts=None):
        """Deploy an application."""
        try:
            if not opts.application_id or not opts.application_policy_id:
                print("usage: deploy-application [-a <applicationId>] [-o <applicationPolicyId>]")
            else:
                application_removed = StratosClient.deploy_application(opts.application_id, opts.application_policy_id)
                if application_removed:
                    print("You have successfully deployed application: " + opts.application_id)
                else:
                    print("Could not deployed application : " + opts.application_id)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-a', '--application_id', type="str", help="Unique ID of the application"),
        make_option('-o', '--application_policy_id', type="str", help="Unique ID of the application policy")
    ])
    @auth
    def do_undeploy_application(self, line, opts=None):
        """Undeploy an application."""
        try:
            if not opts.application_id:
                print("usage: undeploy-application [-a <applicationId>]")
            else:
                application_removed = StratosClient.undeploy_application(opts.application_id)
                if application_removed:
                    print("You have successfully undeployed application: " + opts.application_id)
                else:
                    print("Could not undeployed application : " + opts.application_id)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application_runtime(self, application_id, opts=None):
        """Describe the runtime topology of an application."""
        try:
            if not application_id:
                print("usage: describe-application-runtime [application-id]")
                return
            application_runtime = StratosClient.describe_application_runtime(application_id)
            if not application_runtime:
                print("Application runtime not found")
            else:
                print("Application : " + application_id)
                PrintableJSON(application_runtime).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Application signup
     * describe-application-signup
     * add-application-signup
     * remove-application-signup

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application_signup(self, application_id, opts=None):
        """Retrieve details of a specific application signup."""
        try:
            if not application_id:
                print("usage: describe-application-signup [application-id]")
                return
            application_signup = StratosClient.describe_application_signup(application_id)
            if not application_signup:
                print("Application signup not found")
            else:
                PrintableJSON(application_signup).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_application_signup(self, application_id, opts=None):
        """Add a new application signup to the system"""
        try:
            if not opts.json_file_path:
                print("usage: add-application-signup [-f <resource path>] [application_id]")
            else:
                application_signup = StratosClient.add_application_signup(application_id,
                                                                          open(opts.json_file_path, 'r').read())
                if application_signup:
                    print("Application signup added successfully")
                else:
                    print("Error creating application signup")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_application_signup(self, signup, opts=None):
        """Delete an application sign up."""
        try:
            if not signup:
                print("usage: remove-application-signup [signup]")
            else:
                signup_removed = StratosClient.remove_application_signup(signup)
                if signup_removed:
                    print("You have successfully remove signup: " + signup)
                else:
                    print("Could not delete application signup: " + signup)
        except BadResponseError as e:
            self.perror(str(e))

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
    def do_list_tenants(self, line, opts=None):
        """Retrieve details of all tenants."""
        try:
            tenants = StratosClient.list_tenants()
            table = PrintableTable()
            rows = [["Domain", "Tenant ID", "Email", " State", "Created Date"]]
            for tenant in tenants:
                rows.append([tenant['tenantDomain'], tenant['tenantId'], tenant['email'],
                             "Active" if tenant['active'] else "De-Active",
                             datetime.datetime.fromtimestamp(tenant['createdDate'] / 1000).strftime(
                                 '%Y-%m-%d %H:%M:%S')])
            table.add_rows(rows)
            table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_tenants_by_partial_domain(self, partial_domain, opts=None):
        """Search for tenants based on the partial domain value entered."""
        try:
            tenants = StratosClient.list_tenants_by_partial_domain(partial_domain)
            table = PrintableTable()
            rows = [["Domain", "Tenant ID", "Email", " State", "Created Date"]]
            for tenant in tenants:
                rows.append([tenant['tenantDomain'], tenant['tenantId'], tenant['email'],
                             "Active" if tenant['active'] else "De-Active",
                             datetime.datetime.fromtimestamp(tenant['createdDate'] / 1000).strftime(
                                 '%Y-%m-%d %H:%M:%S')])
            table.add_rows(rows)
            table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

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
                tenant = StratosClient.describe_tenant(tenant_domain_name)
                if not tenant:
                    print("Tenant not found")
                else:
                    print("-------------------------------------")
                    print("Tenant Information:")
                    print("-------------------------------------")
                    print("Tenant domain: " + tenant['tenantDomain'])
                    print("ID: " + str(tenant['tenantId']))
                    print("Active: " + str(tenant['active']))
                    print("Email: " + tenant['email'])
                    print("Created date: " + datetime.datetime.fromtimestamp(tenant['createdDate'] / 1000).strftime(
                        '%Y-%m-%d %H:%M:%S'))
                    print("-------------------------------------")
            except BadResponseError as e:
                self.perror(str(e))

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
    def do_add_tenant(self, line, opts=None):
        """Add a tenant."""
        try:
            tenant = StratosClient.add_tenant(opts.username_user, opts.first_name, opts.last_name, opts.password_user,
                                              opts.domain_name, opts.email)
            if tenant:
                print("Tenant added successfully : " + opts.domain_name)
            else:
                print("Error creating the tenant : " + opts.domain_name)
        except BadResponseError as e:
            self.perror(str(e))

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
    def do_update_tenant(self, line, opts=None):
        """Update a specific tenant."""
        try:
            tenant = StratosClient.update_tenant(opts.username_user, opts.first_name, opts.last_name,
                                                 opts.password_user,
                                                 opts.domain_name, opts.email, opts.tenant_id)
            if tenant:
                print("Tenant updated successfully : " + opts.domain_name)
            else:
                print("Error updating the tenant : " + opts.domain_name)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_activate_tenant(self, tenant_domain, opts=None):
        """Activate a tenant."""
        try:
            if not tenant_domain:
                print("usage: activate-tenant <TENANT_DOMAIN> ")
            else:
                activate_tenant = StratosClient.activate_tenant(tenant_domain)
                if activate_tenant:
                    print("You have successfully activated the tenant : " + tenant_domain)
                else:
                    print("Could not activate tenant : " + tenant_domain)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_deactivate_tenant(self, tenant_domain, opts=None):
        """Deactivate a tenant."""
        try:
            if not tenant_domain:
                print("usage: deactivate-tenant <TENANT_DOMAIN> ")
            else:
                activate_tenant = StratosClient.deactivate_tenant(tenant_domain)
                if activate_tenant:
                    print("You have successfully deactivated the tenant : " + tenant_domain)
                else:
                    print("Could not deactivate tenant : " + tenant_domain)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Cartridges
     * list-cartridges
     * list-cartridges-by-filter
     * describe-cartridge
     * add-cartridge
     * remove-cartridge

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_cartridges(self, line, opts=None):
        """Retrieve details of available cartridges."""
        try:
            cartridges = StratosClient.list_cartridges()
            table = PrintableTable()
            rows = [["Type", "Category", "Name", "Description", "Version", "Multi-Tenant"]]
            for cartridge in cartridges:
                rows.append(
                    [cartridge['type'], cartridge['category'], cartridge['displayName'], cartridge['description'],
                     cartridge['version'], "True" if cartridge['multiTenant'] == 1 else "False"])
            table.add_rows(rows)
            table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_cartridges_by_filter(self, filter_text, opts=None):
        """Retrieve details of available cartridges."""
        try:
            if not filter_text:
                print("usage: describe-cartridge-by-filter [filter]")
            else:
                cartridges = StratosClient.list_cartridges_by_filter(filter_text)
                table = PrintableTable()
                rows = [["Type", "Category", "Name", "Description", "Version", "Multi-Tenant"]]
                for cartridge in cartridges:
                    rows.append(
                        [cartridge['type'], cartridge['category'], cartridge['displayName'], cartridge['description'],
                         cartridge['version'], "True" if cartridge['multiTenant'] == 1 else "False"])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_cartridge(self, cartridge_type, opts=None):
        """Retrieve details of a specific cartridge."""
        if not cartridge_type:
            print("usage: describe-cartridge [cartridge-type]")
        else:
            try:
                cartridge = StratosClient.describe_cartridge(cartridge_type)
                if not cartridge:
                    print("Cartridge not found")
                else:
                    print("-------------------------------------")
                    print("Cartridge Information:")
                    print("-------------------------------------")
                    print("Type: " + cartridge['type'])
                    print("Category: " + cartridge['category'])
                    print("Name: " + cartridge['displayName'])
                    print("Description: " + cartridge['description'])
                    print("Version: " + str(cartridge['version']))
                    print("Multi-Tenant: " + str(cartridge['multiTenant']))
                    print("Host Name: " + cartridge['host'])
                    print("-------------------------------------")
            except requests.HTTPError as e:
                self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_cartridge(self, line, opts=None):
        """Add a cartridge definition."""
        try:
            if not opts.json_file_path:
                print("usage: add-cartridge [-f <resource path>]")
            else:
                cartridge = StratosClient.add_cartridge(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Cartridge added successfully")
                else:
                    print("Error adding Cartridge")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_cartridge(self, line, opts=None):
        """Update a cartridge"""
        try:
            if not opts.json_file_path:
                print("usage: update-cartridge [-f <resource path>]")
            else:
                cartridge = StratosClient.update_cartridge(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Cartridge updated successfully")
                else:
                    print("Error updating Cartridge")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_cartridge(self, cartridge_type, opts=None):
        """Delete a cartridge"""
        try:
            if not cartridge_type:
                print("usage: remove-cartridge [cartridge-type]")
            else:
                cartridge_removed = StratosClient.remove_cartridge(cartridge_type)
                if cartridge_removed:
                    print("Successfully un-deployed cartridge : " + cartridge_type)
                else:
                    print("Could not un-deployed cartridge : " + cartridge_type)
        except BadResponseError as e:
            self.perror(str(e))

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
    def do_list_cartridge_groups(self, line, opts=None):
        """Retrieve details of all the cartridge groups."""
        try:
            cartridge_groups = StratosClient.list_cartridge_groups()
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
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_cartridge_group(self, group_definition_name, opts=None):
        """Retrieve details of a cartridge group."""
        try:
            if not group_definition_name:
                print("usage: describe-cartridge-group [cartridge-group-name]")
                return
            cartridge_group = StratosClient.describe_cartridge_group(group_definition_name)
            if not cartridge_group:
                print("Cartridge group not found")
            else:
                print("Service Group : " + group_definition_name)
                PrintableJSON(cartridge_group).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_cartridge_group(self, line, opts=None):
        """Add a cartridge group."""
        try:
            if not opts.json_file_path:
                print("usage: add-cartridge-group [-f <resource path>]")
            else:
                cartridge_group = StratosClient.add_cartridge_group(open(opts.json_file_path, 'r').read())
                if cartridge_group:
                    print("Cartridge group added successfully")
                else:
                    print("Error adding Cartridge group")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_cartridge_group(self, line, opts=None):
        """Add a new user to the system"""
        try:
            if not opts.json_file_path:
                print("usage: update-cartridge-group [-f <resource path>]")
            else:
                cartridge = StratosClient.update_cartridge_group(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Cartridge group updated successfully")
                else:
                    print("Error updating Cartridge group")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_cartridge_group(self, group_definition_name, opts=None):
        """Delete a cartridge group."""
        try:
            if not group_definition_name:
                print("usage: remove-cartridge-group [cartridge-group-name]")
            else:
                cartridge_removed = StratosClient.remove_cartridge_group(group_definition_name)
                if cartridge_removed:
                    print("Successfully un-deployed cartridge group : " + group_definition_name)
                else:
                    print("Could not un-deployed cartridge group : " + group_definition_name)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Deployment Policies
     * list-deployment-policies
     * describe-deployment-policy
     * add-deployment-policy
     * update-deployment-policy
     * remove-deployment-policy

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_deployment_policies(self, line, opts=None):
        """Retrieve details of a deployment policy."""
        try:
            deployment_policies = StratosClient.list_deployment_policies()
            if not deployment_policies:
                print("No deployment policies found")
            else:
                table = PrintableTable()
                rows = [["Id", "Accessibility"]]
                for deployment_policy in deployment_policies:
                    rows.append([deployment_policy['id'], len(deployment_policy['networkPartitions'])])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_deployment_policy(self, line, opts=None):
        """Describe a deployment policy."""
        try:
            if not line:
                print("usage: describe-deployment-policy [deployment-policy-id]")
                return
            deployment_policy = StratosClient.describe_deployment_policy(line)
            if not deployment_policy:
                print("Deployment policy not found")
            else:
                PrintableJSON(deployment_policy).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_deployment_policy(self, line, opts=None):
        """Add a deployment policy definition."""
        try:
            if not opts.json_file_path:
                print("usage: add-deployment-policy [-f <resource path>]")
            else:
                deployment_policy = StratosClient.add_deployment_policy(open(opts.json_file_path, 'r').read())
                if deployment_policy:
                    print("Deployment policy added successfully")
                else:
                    print("Error creating deployment policy")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_deployment_policy(self, line, opts=None):
        """Update a deployment policy."""
        try:
            if not opts.json_file_path:
                print("usage: update-deployment-policy [-f <resource path>]")
            else:
                cartridge = StratosClient.update_deployment_policy(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Deployment policy updated successfully")
                else:
                    print("Error updating Deployment policy")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_deployment_policy(self, deployment_policy_id, opts=None):
        """Delete a deployment policy."""
        try:
            if not deployment_policy_id:
                print("usage: remove-deployment-policy [deployment-policy-id]")
            else:
                cartridge_removed = StratosClient.remove_deployment_policy(deployment_policy_id)
                if cartridge_removed:
                    print("Successfully deleted deployment policy : " + deployment_policy_id)
                else:
                    print("Could not deleted deployment policy : " + deployment_policy_id)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Deployment Policies
     * list-application-policies
     * describe-application-policy
     * update-application-policy
     * remove-application-policy

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_application_policies(self, line, opts=None):
        """Retrieve details of all the application policies."""
        try:
            application_policies = StratosClient.list_application_policies()
            if not application_policies:
                print("No application policies found")
            else:
                table = PrintableTable()
                rows = [["Id", "Accessibility"]]
                for application_policy in application_policies:
                    rows.append([application_policy['id'], len(application_policy['networkPartitions'])])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_application_policy(self, application_policy_id, opts=None):
        """Retrieve details of a specific application policy."""
        try:
            if not application_policy_id:
                print("usage: describe-application-policy [application-policy-id]")
                return
            application_policy = StratosClient.describe_application_policy(application_policy_id)
            if not application_policy:
                print("Deployment policy not found")
            else:
                PrintableJSON(application_policy).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_application_policy(self, line, opts=None):
        """Add an application policy."""
        try:
            if not opts.json_file_path:
                print("usage: add-application-policy [-f <resource path>]")
            else:
                application_policy = StratosClient.add_application_policy(open(opts.json_file_path, 'r').read())
                if application_policy:
                    print("Deployment policy added successfully")
                else:
                    print("Error creating application policy")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_application_policy(self, line, opts=None):
        """Update an application policy."""
        try:
            if not opts.json_file_path:
                print("usage: update-application-policy [-f <resource path>]")
            else:
                cartridge = StratosClient.update_application_policy(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Deployment policy updated successfully")
                else:
                    print("Error updating Deployment policy")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_application_policy(self, application_policy_id, opts=None):
        """Delete an application policy."""
        try:
            if not application_policy_id:
                print("usage: remove-application-policy [application-policy-id]")
            else:
                cartridge_removed = StratosClient.remove_application_policy(application_policy_id)
                if cartridge_removed:
                    print("Successfully deleted application policy : " + application_policy_id)
                else:
                    print("Could not deleted application policy : " + application_policy_id)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Network Partitions
     * list-network-partitions
     * describe-network-partition
     * add-network-partition
     * update-network-partition
     * remove-network-partition

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_network_partitions(self, line, opts=None):
        """Retrieve details of all the network partitions."""
        try:
            network_partitions = StratosClient.list_network_partitions()
            table = PrintableTable()
            rows = [["Network Partition ID", "Number of Partitions"]]
            for network_partition in network_partitions:
                rows.append([network_partition['id'], len(network_partition['partitions'])])
            table.add_rows(rows)
            table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_network_partition(self, network_partition_id, opts=None):
        """Describe a network partition."""
        try:
            if not network_partition_id:
                print("usage: describe-network-partition [network-partition]")
                return
            deployment_policy = StratosClient.describe_network_partition(network_partition_id)
            if not deployment_policy:
                print("Network partition not found: " + network_partition_id)
            else:
                print("Partition: " + network_partition_id)
                PrintableJSON(deployment_policy).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_network_partition(self, line, opts=None):
        """Add a new network partition."""
        try:
            if not opts.json_file_path:
                print("usage: add-network-partition [-f <resource path>]")
            else:
                tenant = StratosClient.add_network_partition(open(opts.json_file_path, 'r').read())
                if tenant:
                    print("Network partition added successfully")
                else:
                    print("Error creating network partition")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_network_partition(self, line, opts=None):
        """Update a specific network partition."""
        try:
            if not opts.json_file_path:
                print("usage: update-network-partition [-f <resource path>]")
            else:
                cartridge = StratosClient.update_network_partition(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Network partition updated successfully")
                else:
                    print("Error updating Network partition")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_network_partition(self, network_partition_id, opts=None):
        """Delete a network partition."""
        try:
            if not network_partition_id:
                print("usage: remove-network-partition [network-partition-id]")
            else:
                cartridge_removed = StratosClient.remove_network_partition(network_partition_id)
                if cartridge_removed:
                    print("Successfully deleted network-partition : " + network_partition_id)
                else:
                    print("Could not deleted network-partition : " + network_partition_id)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Auto-scaling policies
     * list-autoscaling-policies
     * describe-autoscaling-policy
     * add-autoscaling-policy
     * update-autoscaling-policy
     * remove-autoscaling-policy

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_autoscaling_policies(self, line, opts=None):
        """Retrieve details of auto-scaling policies."""
        try:
            autoscaling_policies = StratosClient.list_autoscaling_policies()
            if not autoscaling_policies:
                print("No autoscaling policies found")
            else:
                table = PrintableTable()
                rows = [["Id", "Accessibility"]]
                for autoscaling_policy in autoscaling_policies:
                    rows.append([autoscaling_policy['id'], "Public" if autoscaling_policy['isPublic'] else "Private"])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_autoscaling_policy(self, autoscaling_policy_id, opts=None):
        """Retrieve details of a specific auto-scaling policy."""
        try:
            if not autoscaling_policy_id:
                print("usage: describe-autoscaling-policy [autoscaling-policy-id]")
                return
            autoscaling_policy = StratosClient.describe_autoscaling_policy(autoscaling_policy_id)
            if not autoscaling_policy:
                print("Autoscaling policy not found : " + autoscaling_policy_id)
            else:
                print("Autoscaling policy : " + autoscaling_policy_id)
                PrintableJSON(autoscaling_policy).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_autoscaling_policy(self, line, opts=None):
        """Add an auto-scaling policy definition."""
        try:
            if not opts.json_file_path:
                print("usage: add-autoscaling-policy [-f <resource path>]")
            else:
                autoscaling_policy = StratosClient.add_autoscaling_policy(open(opts.json_file_path, 'r').read())
                if autoscaling_policy:
                    print("Autoscaling policy added successfully")
                else:
                    print("Error adding autoscaling policy")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_autoscaling_policy(self, line, opts=None):
        """Update an auto-scaling policy."""
        try:
            if not opts.json_file_path:
                print("usage: update-autoscaling-policy [-f <resource path>]")
            else:
                autoscaling_policy = StratosClient.update_autoscaling_policy(open(opts.json_file_path, 'r').read())
                if autoscaling_policy:
                    print("Autoscaling policy updated successfully:")
                else:
                    print("Error updating Autoscaling policy")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_autoscaling_policy(self, autoscaling_policy_id, opts=None):
        """Delete an autoscaling_policy."""
        try:
            if not autoscaling_policy_id:
                print("usage: remove-autoscaling-policy [application-id]")
            else:
                autoscaling_policy_removed = StratosClient.remove_autoscaling_policy(autoscaling_policy_id)
                if autoscaling_policy_removed:
                    print("Successfully deleted Auto-scaling policy : " + autoscaling_policy_id)
                else:
                    print("Auto-scaling policy not found : " + autoscaling_policy_id)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Kubernetes clusters/hosts
     * list-kubernetes-clusters
     * describe-kubernetes-cluster
     * describe-kubernetes-master
     * add-kubernetes-cluster
     * add-kubernetes-host
     * list-kubernetes-hosts
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
    def do_list_kubernetes_clusters(self, line, opts=None):
        """Retrieving details of all Kubernetes-CoreOS Clusters."""
        try:
            kubernetes_clusters = StratosClient.list_kubernetes_clusters()
            if not kubernetes_clusters:
                print("No Kubernetes clusters found")
            else:
                table = PrintableTable()
                rows = [["Group ID", "Description"]]
                for kubernetes_cluster in kubernetes_clusters:
                    rows.append([kubernetes_cluster['clusterId'], kubernetes_cluster['description']])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_kubernetes_cluster(self, kubernetes_cluster_id, opts=None):
        """Describe a Kubernetes-CoreOS Cluster."""
        try:
            if not kubernetes_cluster_id:
                print("usage: describe-kubernetes-cluster [cluster-i]]")
                return
            kubernetes_cluster = StratosClient.describe_kubernetes_cluster(kubernetes_cluster_id)
            if not kubernetes_cluster:
                print("Kubernetes cluster not found")
            else:
                print("Kubernetes cluster: " + kubernetes_cluster_id)
                PrintableJSON(kubernetes_cluster).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_describe_kubernetes_master(self, kubernetes_cluster_id, opts=None):
        """Retrieve details of the master in a Kubernetes-CoreOS Cluster."""
        try:
            if not kubernetes_cluster_id:
                print("usage: describe-kubernetes-master [cluster-id]")
                return
            kubernetes_master = StratosClient.describe_kubernetes_master(kubernetes_cluster_id)
            if not kubernetes_master:
                print("Kubernetes master not found in : " + kubernetes_cluster_id)
            else:
                print("Cluster : " + kubernetes_cluster_id)
                PrintableJSON(kubernetes_master).pprint()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_kubernetes_cluster(self, opts=None):
        """Add a Kubernetes-CoreOS Cluster."""
        try:
            if not opts.json_file_path:
                print("usage: add-kubernetes-cluster [-f <resource path>]")
            else:
                kubernetes_cluster = StratosClient.add_kubernetes_cluster(open(opts.json_file_path, 'r').read())
                if kubernetes_cluster:
                    print("You have successfully deployed the Kubernetes cluster")
                else:
                    print("Error deploying the Kubernetes cluster ")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_kubernetes_host(self, kubernetes_cluster_id, opts=None):
        """Add a host to a Kubernetes-CoreOS Cluster."""
        try:
            if not kubernetes_cluster_id or not opts.json_file_path:
                print("usage: add-kubernetes-host [-f <resource path>] [kubernetes cluster id]")
            else:
                kubernetes_host = StratosClient.add_kubernetes_host(kubernetes_cluster_id,
                                                                    open(opts.json_file_path, 'r').read())
                if kubernetes_host:
                    print("You have successfully deployed host to Kubernetes cluster: " + kubernetes_cluster_id)
                else:
                    print("Error deploying host to Kubernetes cluster: " + kubernetes_cluster_id)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-c', '--cluster_id', type="str", help="Cluster ID")
    ])
    def do_list_kubernetes_hosts(self, line, opts=None):
        """Retrieve details of all hosts of a Kubernetes-CoreOS Cluster."""
        try:
            if not opts.cluster_id:
                print("usage: list-kubernetes-hosts [-c <cluster id>]")
                return
            kubernetes_cluster_hosts = StratosClient.list_kubernetes_hosts(opts.cluster_id)
            if not kubernetes_cluster_hosts:
                print("No kubernetes hosts found")
            else:
                table = PrintableTable()
                rows = [["Host ID", "Hostname", "Private IP Address", "Public IP Address"]]
                for kubernetes_cluster_host in kubernetes_cluster_hosts:
                    rows.append([kubernetes_cluster_host['hostId'], kubernetes_cluster_host['hostname'],
                                 kubernetes_cluster_host['privateIPAddress'],
                                 kubernetes_cluster_host['publicIPAddress']])
                table.add_rows(rows)
                table.print_table()
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-c', '--cluster_id', type="str", help="Cluster id of the cluster"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_kubernetes_master(self, line, opts=None):
        """Update the master node of the Kubernetes-CoreOS Cluster."""
        try:
            if not opts.json_file_path:
                print("usage: update-kubernetes-master [-c <cluster id>] [-p <resource path>]")
            else:
                cartridge = StratosClient.update_kubernetes_master(opts.cluster_id,
                                                                   open(opts.json_file_path, 'r').read())
                if cartridge:
                    print("Kubernetes master updated successfully")
                else:
                    print("Error updating Kubernetes master")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_update_kubernetes_host(self, line, opts=None):
        """Update the host of a Kubernetes-CoreOS Cluster."""
        try:
            if not opts.json_file_path:
                print("usage: update-kubernetes-host [-f <resource path>]")
            else:
                cartridge = StratosClient.update_kubernetes_host(open(opts.json_file_path, 'r').read())
                if cartridge:
                    print(cartridge)
                    print("You have succesfully updated host to Kubernetes cluster")
                else:
                    print("Error updating Kubernetes host")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_kubernetes_cluster(self, kubernetes_cluster_id, opts=None):
        """Delete a Kubernetes-CoreOS Cluster."""
        try:
            if not kubernetes_cluster_id:
                print("usage: remove-kubernetes-cluster [cluster-id]")
            else:
                kubernetes_cluster_removed = StratosClient.remove_kubernetes_cluster(kubernetes_cluster_id)
                if kubernetes_cluster_removed:
                    print("Successfully un-deployed kubernetes cluster : " + kubernetes_cluster_id)
                else:
                    print("Kubernetes cluster not found : " + kubernetes_cluster_id)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-c', '--cluster_id', type="str", help="Cluster id of Kubernets cluster"),
        make_option('-o', '--host_id', type="str", help="Host id of Kubernets cluster")
    ])
    @auth
    def do_remove_kubernetes_host(self, line, opts=None):
        """Delete the host of a Kubernetes-CoreOS Cluster."""
        try:
            if not opts.cluster_id or not opts.host_id:
                print("usage: remove-kubernetes-host [-c cluster-id] [-o host-id]")
            else:
                kubernetes_host_removed = StratosClient.remove_kubernetes_host(opts.cluster_id, opts.host_id)
                if kubernetes_host_removed:
                    print("Successfully un-deployed kubernetes host : " + opts.host_id)
                else:
                    print("Kubernetes host not found : " + opts.cluster_id + "/" + opts.host_id)
        except BadResponseError as e:
            self.perror(str(e))

    """
    # Domain Mapping
     * list-domain-mappings
     * add-domain-mapping
     * remove-domain-mapping

    """

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_domain_mappings(self, application_id, opts=None):
        """Retrieve details of domain mappings of an application."""
        try:
            if not application_id:
                print("usage: list-domain-mappings [application-id]")
            else:
                domain_mappings = StratosClient.list_domain_mappings(application_id)
                if domain_mappings:
                    table = PrintableTable()
                    rows = [["Domain", "Tenant ID", "Email", " State", "Created Date"]]
                    for domain_mapping in domain_mappings:
                        rows.append([domain_mapping['domain_mappingsDomain'], domain_mapping['domain_mappingId'],
                                     domain_mapping['email'],
                                     "Active" if domain_mapping['active'] else "De-Active",
                                     datetime.datetime.fromtimestamp(domain_mapping['createdDate'] / 1000).strftime(
                                         '%Y-%m-%d %H:%M:%S')])
                    table.add_rows(rows)
                    table.print_table()
                else:
                    print("No domain mappings found in application: " + application_id)
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user"),
        make_option('-f', '--json_file_path', type="str", help="Path of the JSON file")
    ])
    @auth
    def do_add_domain_mapping(self, application_id, opts=None):
        """Map domain to a subscribed cartridge."""
        try:
            if not application_id or not opts.json_file_path:
                print("usage: add-domain-mapping [-f <resource path>] [application id]")
            else:
                domain_mapping = StratosClient.add_domain_mapping(application_id, open(opts.json_file_path, 'r').read())
                if domain_mapping:
                    print(" Domain mapping added successfully")
                else:
                    print("Error creating domain mapping")
        except BadResponseError as e:
            self.perror(str(e))

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_remove_domain_mappings(self, domain, opts=None):
        """Remove domain mappings of an application."""
        try:
            if not domain:
                print("usage: remove-domain-mappings [domain]")
            else:
                domain_removed = StratosClient.remove_domain_mappings(domain)
                if domain_removed:
                    print("You have successfully deleted domain: " + domain)
                else:
                    print("Could not delete domain: " + domain)
        except BadResponseError as e:
            self.perror(str(e))


"""


Display related util classes


"""


class PrintableTree:
    def __init__(self, tree_data):
        self.tree_data = tree_data
        pass

    def print_tree(self):
        def _print_tree(t, level=0, ups=""):
            if isinstance(t, list):
                print('|')
                for element in t[:-1]:
                    print(ups + "+-", end='')
                    _print_tree(element, level + 1, ups + "| ")
                else:
                    print(ups + "+-", end='')
                    _print_tree(t[-1], level + 1, ups + "  ")
            elif isinstance(t, dict):
                print('|')
                l = []
                for k, v in t.items():
                    if isinstance(v, list) or isinstance(v, dict):
                        l.extend([k, v])
                    else:
                        l.extend([str(k) + ":" + str(v)])
                t = l
                for element in t[:-1]:
                    print(ups + "+-", end='')
                    _print_tree(element, level + 1, ups + "| ")
                else:
                    print(ups + "+-", end='')
                    _print_tree(t[-1], level + 1, ups + "  ")
            else:
                print(str(t))

        print("_")
        _print_tree(self.tree_data)


class PrintableTable(Texttable):
    def __init__(self):
        Texttable.__init__(self)
        self.set_deco(Texttable.BORDER | Texttable.HEADER | Texttable.VLINES)

    def print_table(self):
        print(self.draw())


class PrintableJSON(Texttable):
    def __init__(self, json_input):
        Texttable.__init__(self)
        self.json = json_input

    def pprint(self):
        print(json.dumps(self.json, indent=4, separators=(',', ': ')))
