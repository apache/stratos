Dependent Scaling With Cartridges Application
=============================================

Application View
----------------
dependency-scaling-cartridges-app (sample-cartridges)
-- dependency-scaling-cartridges-app1
-- -- my-tomcat
-- -- my-php

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