This folder should have following:

1. apache-stratos-cartridge-agent-${version}.zip file

2. Folder having a name as $mb_type which is defined in the nodes.pp file.

eg:
if $mb_type = activemq, folder structure of this folder would be:
>$ls
>activemq  apache-stratos-cartridge-agent-4.0.0.zip

3. Under $mb_type folder, please add all the client jars, that should be copied to the agent's lib directory.
