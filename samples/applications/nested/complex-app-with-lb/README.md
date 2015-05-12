Complex App with a Load Balancer
================================
This is a complex application with a load balancer.

Application View
----------------
complex-app-with-lb
-- complex-app-with-lb-1
-- -- my-tomcat
-- -- my-stratos-lb
-- -- my-group8
-- -- -- my-tomcat2-group8
-- -- -- my-group9
-- -- -- -- my-tomcat1-group9
-- -- my-group6
-- -- -- my-tomcat2-group6
-- -- -- my-group7
-- -- -- -- my-tomcat1-group7

Application folder structure
----------------------------
-- artifacts/<iaas>/ IaaS specific artifacts
-- scripts/common/ Common scripts for all iaases
-- scripts/<iaas> IaaS specific scripts

How to run
----------
cd scripts/<iaas>/
./deploy.sh

How to undeploy
---------------
cd scripts/<iaas>/
./undeploy.sh
