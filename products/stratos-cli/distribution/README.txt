================================================================================
                        Apache Stratos CLI
================================================================================

Welcome to the Apache Stratos CLI.

The command line interface (CLI) is a powerful tool that clients can use
to communicate with Stratos services. The CLI distribution only includes one
jar file containing all dependent java libraries.

The tenant can use the CLI tool on two separate modes if needed, which 
are namely single command line mode and interactive mode. 

If the user executes with a command as an argument, the CLI tool 
will execute that command and exit with a status code. 

However, if there is no command as an argument, the CLI tool will 
enter into an interactive mode.

A user will be able to carryout all the following functions via the
CLI, with the exception of registering tenants and viewing logs.

The CLI tool also supports command history and auto-completing features 
in the interactive mode.

Help
==================================
usage: stratos [-debug] [-h] [-p <password>] [-trace] [-u <username>]
 -debug                     Enable debug logging
 -h,--help                  Display this help
 -p,--password <password>   Password
 -trace                     Enable trace logging
 -u,--username <username>   Username


Available Commands:
create-tenant             	Add new tenant
list-autoscale-policies   	List available autoscale policies
subscribe-cartridge       	Subscribe to a cartridge
exit                      	Exit from Stratos Client Tool
list-available-cartridges 	List available cartridges
help                      	Help for commands
list-available-partitions 	List available partitions
deploy-cartridge          	Add new cartridge deployment
unsubscribe-cartridge     	Unsubscribe from a subscribed cartridge
list-subscribe-cartridges 	List subscribed cartridges with details
deploy-partition          	Add new partition deployment
deploy-autoscaling-policy 	Add new autoscaling policy deployment
deploy-deployment-policy  	Add new deployment policy


Support
==================================

Any problem with this release can be reported to Apache Stratos mailing list
or in the JIRA issue tracker. If you are sending an email to the mailing
list make sure to add the [Apache Stratos] prefix to the subject.

Mailing list subscription:
    dev-subscribe@stratos.incubator.apache.org


Issue Tracker
==================================

Jira:
    https://issues.apache.org/jira/browse/stratos


Thank you for using Apache Stratos!
The Stratos Team

