#!/usr/bin/python
import optparse
import sys
import os
import logging
import cmdinterpretor
import cmd


log = logging.getLogger(__name__)

#this is the entry point of the commandline client.
#   1. enter in to stratos shell session
#   2. use the 'stratos' a unix command
def authenticate_authorize():
    parser = optparse.OptionParser(version='1.0', description='Command line tool to interact with stratos backend')
    parser.add_option('--username', '-u')
    parser.add_option('--password', '-p')

    options, arguments = parser.parse_args()
    if options.username == None or options.username == '' or options.password == None or options.password == '':
        log.debug("username/passoword empty")
        print 'please enter valid username/password'
        sys.exit(1)  #throw exception and handle it in the driver class. less readable

    print options.username
    print options.password

def activate_console():
    cmdinterpretor.CmdInterpretor().cmdloop('stratos>')



def start_stratos_client():
    authenticate_authorize()
    activate_console()


def main():
    try:
        #execute the stratos shell
       start_stratos_client()
    except Exception as e:
        log.info(e.message, e)
        print e.message
        print 'Exception'
        sys.exit(1)
    except KeyboardInterrupt as e:
        print('Shutting down stratos client')
        sys.exit(1)


if __name__ == '__main__':
    main()