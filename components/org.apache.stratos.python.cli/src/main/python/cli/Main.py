import sys
import readline
import getpass
from CLI import CLI
import Configs

# Fix Python 2.x.
try:
    input = raw_input
except NameError:
    pass


def prompt_for_credentials():
    """Prompt for user credentials"""
    while True:
        _username = input("Username: ")
        _password = getpass.getpass("Password: ")
        if _username is not "" and _password is not "":
            Configs.stratos_username = _username
            Configs.stratos_password = _password
            break


def main():
    # resolving the '-' issue
    readline.set_completer_delims(readline.get_completer_delims().replace('-', ''))

    cli = CLI()

    if len(sys.argv) > 1:
        cli.onecmd(' '.join(sys.argv[1:]))
    else:
        prompt_for_credentials()
        cli.cmdloop()

if __name__ == '__main__':
    main()
