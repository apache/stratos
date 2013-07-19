================================================================================
                        Apache Stratos CLI v1.0.1
================================================================================

Welcome to the Apache Stratos CLI release

The command line interface (CLI) is a powerful tool that clients can use
to communicate with Stratos services. 

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
add-domain-mapping        Map domain for the subscribed cartridge
help                      Help for commands
subscribe                 Subscribe to a cartridge
sync                      Synchronize GIT repository for the subscribed cartridge
remove-domain-mapping     Remove domain mapping for the subscribed cartridge
list                      List subscribed cartridges with details
unsubscribe               Unsubscribe from a subscribed cartridge
policies                  List available policies
exit                      Exit from Stratos Client Tool
info                      Get information about a subscribed cartridge
cartridges                List available cartridges


Installation & Running
==================================

1. Extract the zip and go to the extracted directory

Add execute permission to the script in Linux.
chmod +x ./stratos.sh

2. Export the host and port of the SC as environment variables

Linux:
export STRATOS_URL=https://s2demo.apache.com:9445

Windows:
set STRATOS_URL=https://s2demo.apache.com:9445

3. Run the stratos.sh or stratos.bat as appropriate

4. Export the following environment variables.

Linux: (Add to your bashrc file)
export JAVA_HOME=/opt/jdk1.6.0_24
export PATH=$JAVA_HOME/bin:$PATH

Windows: (Set in System Properties)
set JAVA_HOME=C:\Java\jdk1.6.0_24
set PATH=%JAVA_HOME%\bin;%PATH%

4. Use the tentant username and password to login

If you wish, you can also export your username and password as environment variables.
If you export only the username, you will be prompted to enter the password.

Linux:
export STRATOS_USERNAME=<username>
export STRATOS_PASSWORD=<password>

Windows:
set STRATOS_USERNAME=<username>
set STRATOS_PASSWORD=<password>

Issue Tracker
==================================


--------------------------------------------------------------------------------

