Dependent Scaling With Groups Application
=========================================

Application View
----------------
dependency-scaling-groups-app (sample-groups)
-- dependency-scaling-groups-app-1
-- -- my-tomcat
-- -- my-esb-php-group
-- -- -- my-esb
-- -- -- my-php

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