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
            user = Stratos.list_users(opts.username, opts.password, opts.role_name, opts.first_name, opts.last_name,
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
            users = Stratos.add_user()
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
            rows.append([cartridge['type'], cartridge['category'], cartridge['displayName'], cartridge['description'], cartridge['version'], cartridge['multiTenant']])
        table.add_rows(rows)
        table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_cartridges_group(self, line , opts=None):
        """Illustrate the base class method use."""
        cartridges_groups = Stratos.list_cartridges_group()
        table = PrintableTable()
        rows = [["Name", "No. of cartridges", "No of groups", "Dependency scaling"]]
        for cartridges_group in cartridges_groups:
            rows.append([cartridges_group['name'], cartridges_group['category'], cartridges_group['displayName'], cartridge['description'], cartridge['version'], cartridge['multiTenant']])
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
                rows.append([application['type'], application['category'], application['displayName'], application['description'], application['version'], application['multiTenant']])
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
            print("No Kubernetes-CoreOS clusters found")
        else:
            table = PrintableTable()
            rows = [["Group ID", "Description"]]
            for kubernetes_cluster in kubernetes_clusters:
                rows.append([kubernetes_cluster['clusterId'], kubernetes_cluster['description']])
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