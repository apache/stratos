Tomcat Single Sign On Sample Application
========================================
This sample application demonstrate how Single Sign On (SSO) can be implemented with WSO2 Identity Server and Tomcat
application server. It includes a cartridge for Tomcat 7 and WSO2 Identity Server 5.0.0 and set of cartridge agent
extensions for handling the SSO configuration.

Tomcat Dockerfile:
(https://github.com/apache/stratos/tree/master/tools/docker-images/cartridge-docker-images/service-images/tomcat-saml-sso)

WSO2 Identity Server Dockerfile:
(https://github.com/apache/stratos/tree/master/tools/docker-images/cartridge-docker-images/service-images/wso2is-saml-sso)

Application folder structure
----------------------------
```
artifacts/<iaas>/ IaaS specific artifacts
scripts/common/ Common scripts for all iaases
scripts/<iaas> IaaS specific scripts
```

How to run
----------
```
cd scripts/<iaas>/
./deploy.sh
```