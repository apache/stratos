from cmd2 import *
from Utils import *
from Stratos import *


class CLI(Cmd):
    """Apache Stratos CLI"""

    prompt = 'stratos> '

    @options([
        make_option('-u', '--username', type="str", help="Username of the user"),
        make_option('-p', '--password', type="str", help="Password of the user")
    ])
    def do_repositories(self, line, opts=None):
        """ Shows the git repositories of the user identified by given the username and password
          eg: repositories -u agentmilindu  -p agentmilindu123 """

        if opts.username and opts.password:
            r = requests.get('https://api.github.com/users/' + opts.username + '/repos?per_page=5',
                             auth=(opts.username, opts.password))
            repositories = r.json()
            table = PrintableTable()
            rows = [["Name", "language"]]
            table.set_cols_align(["l", "r"])
            table.set_cols_valign(["t", "m"])

            for repo in repositories:
                rows.append([repo['name'], repo['language']])
            print(rows)
            table.add_rows(rows)
            table.print_table()

        else:
            print("Some required argument(s) missing")