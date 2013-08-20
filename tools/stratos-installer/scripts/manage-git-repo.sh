#!/bin/bash

user="wso2"
action=""
username=""
tenant_domain=""
cartridge=""
tenant_key=""
cartridge_key=""
gitolite_admin="/home/wso2/gitolite-admin/"
git_domain="git.slive.com"
#gitolite_admin="/tmp/gitolite-admin/"
git_repo="/home/git/repositories/"

function help {
    echo "Usage: manage-git-repo <action> <mandatory arguments>"
    echo "    Action can be one of the following"
    echo "        create : create git repo"
    echo "        delete : delete git repo"
    echo "    Usage:"
    echo "    	  manage-git-repo create <username> <tenant domain> <cartridge>"
    echo "    	  manage-git-repo delete <tenant name> <cartridge>"
    echo "    eg:"
    echo "    	  manage-git-repo create foo abc.com php /tmp/foo-php.pub"
    echo ""
}

function main {

if [[ ( -z $action || ( -n $action && $action == "help" ) ) ]]; then
    help
    exit 1
fi
if [[ (( -n $action && $action == "create") && ( -z $tenant_domain || -z $username || -z $cartridge )) ]]; then
    help
    exit 1
fi

}

action=$1
username=$2
tenant_domain=$3
cartridge=$4
if [[ $action == "create" ]]; then
	echo "1233454444"    > /tmp/file2
     # hack until stratos manager support key pair for every user
     ssh-keygen -t rsa -N ''  -f /tmp/${username}
     cd ${gitolite_admin}
     git pull
     # set public keys
     cat /tmp/${username}.pub > keydir/${username}.pub
     # add repo and permission to conf 
     echo "" >> conf/gitolite.conf 
     echo "repo ${tenant_domain}.${cartridge}.git" >> conf/gitolite.conf
     echo "	RW+    = ${username} ${user}  daemon" >> conf/gitolite.conf
     echo "     config  gitweb.url                  = git@${git_domain}:${tenant_domain}.${cartridge}" >> conf/gitolite.conf
     echo "     config  receive.denyNonFastforwards = true" >> conf/gitolite.conf
     echo "     config  receive.denyDeletes         = true" >> conf/gitolite.conf
     echo "" >> conf/gitolite.conf
     # git operations
     git add keydir/${username}.pub
     git commit -a -m "${username} keys added and ${tenant_domain}.${cartridge} repo created"
     git pull
     git push

     # set git push trigger
     sudo -s sh -c "echo '<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://org.apache.axis2/xsd\">
   <soapenv:Header/>
   <soapenv:Body>
      <xsd:notifyRepoUpdate>
         <xsd:tenantDomain>${tenant_domain}</xsd:tenantDomain>
         <xsd:cartridgeType>${cartridge}</xsd:cartridgeType>
      </xsd:notifyRepoUpdate>
   </soapenv:Body>
</soapenv:Envelope>' > ${git_repo}${tenant_domain}.${cartridge}.git/hooks/request.xml"
	#sudo cp -a ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update.sample ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update
	sudo -s sh -c "echo '#!/bin/bash' > ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update"
    sudo -s sh -c "echo 'curl -X POST -H \"Content-Type: text/xml\"   -d @${git_repo}${tenant_domain}.${cartridge}.git/hooks/request.xml \"https://localhost:9446/services/RepoNotificationService/\" --insecure' >> ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update"
    sudo -s sh -c "echo 'exec git update-server-info' >> ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update" 
    sudo chown git:git ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update
    sudo chmod 700 ${git_repo}${tenant_domain}.${cartridge}.git/hooks/post-update

fi
if [[ $action == "delete" ]]; then
     echo 'todo - delete'
#     cd ${gitolite_admin}
#     git rm keydir/${tenant}.pub
#     git rm keydir/${tenant}-${cartridge}.pub


fi


main
