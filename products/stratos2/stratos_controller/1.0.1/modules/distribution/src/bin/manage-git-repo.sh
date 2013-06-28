#!/bin/bash

user="wso2"
action=""
username=""
tenant_domain=""
cartridge=""
tenant_key=""
cartridge_key=""
gitolite_admin="/home/wso2/gitolite-admin/"
git_domain=""
git_repo="/home/git/repositories/"
ADC_repo_notification_url=""


function help {
    echo "Usage: manage-git-repo <action> <mandatory arguments>"
    echo "    Action can be one of the following"
    echo "        create : create git repo"
    echo "        destroy: destroy git repo"
    echo "        delete : delete user from a git repo"
    echo "    Usage:"
    echo "    	  manage-git-repo create <username> <tenant domain> <cartridge alias/name> <ADC repo notification url> <git_domain>"
    echo "    	  manage-git-repo destroy <username> <tenant domain> <cartridge alias/name>"
    echo "    eg:"
    echo "    	  manage-git-repo create foo abc.com myphp https://localhost:9445/services/RepoNotificationService/"
    echo ""
}

function main {

if [[ ( -z $action || ( -n $action && $action == "help" ) ) ]]; then
    help
    exit 1
fi
if [[ (( -n $action && $action == "create") && ( -z $tenant_domain || -z $username || -z $cartridge || -z $ADC_repo_notification_url)) ]]; then
    help
    exit 1
fi

if [[ (( -n $action && $action == "destroy") && ( -z $tenant_domain || -z $username || -z $cartridge)) ]]; then
    help
    exit 1
fi

}

action=$1
username=$2
tenant_domain=$3
cartridge=$4
ADC_repo_notification_url=$5
git_domain=$6
if [[ $action == "create" ]]; then

     # hack until stratos manager support key pair for every user
     rm -fr /tmp/${username}*
     ssh-keygen -t rsa -N ''  -f /tmp/${username}
     cd ${gitolite_admin}
     git pull
     # set public keys
     cp -f /tmp/${username}.pub keydir/${username}.pub
     #remove temparaly created files
     rm /tmp/${username}.pub
     # add repo and permission to conf 


     echo "" > conf/repos/${tenant_domain}-${cartridge}.conf 
     echo "repo ${tenant_domain}/${cartridge}.git" >> conf/repos/${tenant_domain}-${cartridge}.conf
     echo "	RW+    = ${username} ${user}  daemon" >> conf/repos/${tenant_domain}-${cartridge}.conf
     echo "     config  gitweb.url                  = git@${git_domain}:${tenant_domain}/${cartridge}" >> conf/repos/${tenant_domain}-${cartridge}.conf
     echo "     config  receive.denyNonFastforwards = true" >> conf/repos/${tenant_domain}-${cartridge}.conf
     echo "     config  receive.denyDeletes         = true" >> conf/repos/${tenant_domain}-${cartridge}.conf
     echo "" >> conf/repos/${tenant_domain}-${cartridge}.conf
     # git operations
     git add keydir/${username}.pub
     git add conf/repos/${tenant_domain}-${cartridge}.conf
     git commit -a -m "${username} keys added and ${tenant_domain}/${cartridge} repo created"
     git pull
     git push
     # set git push trigger
    
   echo "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://org.apache.axis2/xsd\">
   <soapenv:Header/>
   <soapenv:Body>
      <xsd:notifyRepoUpdate>
         <xsd:tenantDomain>${tenant_domain}</xsd:tenantDomain>
         <xsd:cartridgeType>${cartridge}</xsd:cartridgeType>
      </xsd:notifyRepoUpdate>
   </soapenv:Body>
</soapenv:Envelope>" > /tmp/${tenant_domain}-request.xml
   
    echo "#!/bin/bash" > /tmp/${tenant_domain}-post-update
    echo "curl -X POST -H \"Content-Type: text/xml\"   -d @${git_repo}${tenant_domain}/${cartridge}.git/hooks/request.xml \"${ADC_repo_notification_url}\" --insecure" >> /tmp/${tenant_domain}-post-update
    echo "exec git update-server-info" >> /tmp/${tenant_domain}-post-update
    
    sudo mv /tmp/${tenant_domain}-request.xml ${git_repo}${tenant_domain}/${cartridge}.git/hooks/request.xml
    sudo mv /tmp/${tenant_domain}-post-update ${git_repo}${tenant_domain}/${cartridge}.git/hooks/post-update
    sudo chown git:git ${git_repo}${tenant_domain}/${cartridge}.git/hooks/post-update
    sudo chmod 700 ${git_repo}${tenant_domain}/${cartridge}.git/hooks/post-update
fi
if [[ $action == "destroy" ]]; then
 
     cd ${gitolite_admin}
     # remove user keys
     git rm keydir/${username}.pub
     # remove repo from config
     git rm conf/repos/${tenant_domain}-${cartridge}.conf
     # git push to execute
     git pull
     git push
     # remove repo from repository. ** this should done from above. but it doesnt happend. So removing manualy.
     sudo rm -fr /home/git/repositories/${tenant_domain}/${cartridge}.git

fi


main
