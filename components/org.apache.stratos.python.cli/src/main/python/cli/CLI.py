from cmd2 import *
from Utils import *
from Stratos import *
import Configs


class CLI(Cmd):
    """Apache Stratos CLI"""

    prompt = Configs.stratos_prompt
    # resolving the '-' issue
    Cmd.legalChars += '-'
    Cmd.shortcuts.update({'list-user': 'list_user'})

    def completenames(self, text, *ignored):
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



    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_list_users(self, line , opts=None):
        """Illustrate the base class method use."""
        r = requests.get(Configs.stratos_api_url + 'users',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        users = r.json()
        table = PrintableTable()
        rows = [["Name", "language"]]
        table.set_cols_align(["l", "r"])
        table.set_cols_valign(["t", "m"])
        for user in users:
                    rows.append([user['role'], user['userName']])
        table.add_rows(rows)
        table.print_table()

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    @auth
    def do_networkpartitions(self, line , opts=None):
        """Illustrate the base class method use."""
        r = requests.get(Configs.stratos_api_url + 'networkPartitions',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r)
        print(r.text)
        repositories = r.json()
        tree = PrintableTree(repositories)
        tree.print_tree()

    def do_deploy_user(self, line , opts=None):
        """Illustrate the base class method use."""
        print("hello User")