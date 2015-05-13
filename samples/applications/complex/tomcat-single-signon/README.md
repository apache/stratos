Tomcat Single Sign-on Application
========================================
This sample application demonstrate how Single Sign On (SAML2) configuration can be automated with Composite
Application Model using Tomcat and WSO2 Identity Server. It includes a cartridge for Tomcat 7 and WSO2 Identity
Server 5.0.0 and a set of cartridge agent plugins for handling the SSO configuration.

###Tomcat Dockerfile
https://github.com/apache/stratos/tree/master/tools/docker-images/cartridge-docker-images/service-images/tomcat-saml-sso/Dockerfile

###Tomcat Cartridge Agent Plugins
https://github.com/apache/stratos/tree/master/tools/docker-images/cartridge-docker-images/service-images/tomcat-saml-sso/packs/plugins

###WSO2 Identity Server Dockerfile:
https://github.com/apache/stratos/tree/master/tools/docker-images/cartridge-docker-images/service-images/wso2is-saml-sso/Dockerfile

###WSO2 Identity Server Cartridge Agent Plugins
https://github.com/apache/stratos/tree/master/tools/docker-images/cartridge-docker-images/service-images/wso2is-saml-sso/packs/plugins

Application view
----------------
tomcat-single-signon            <br />
-- tomcat-single-signon-1       <br />
-- -- mytomcat3                 <br />
-- -- mywso2is                  <br />

Application folder structure
----------------------------
-- artifacts/[iaas]/ IaaS specific artifacts                <br />
-- scripts/common/ Common scripts for all iaases            <br />
-- scripts/[iaas] IaaS specific scripts                     <br />

How to run
----------
cd scripts/[iaas]/          <br />
./deploy.sh                 <br />

How to undeploy
---------------
cd scripts/[iaas]/          <br />
./undeploy.sh               <br />
