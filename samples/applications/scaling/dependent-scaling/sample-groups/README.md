Dependent Scaling With Groups Application
=========================================

Application View
----------------
dependency-scaling-groups-app (sample-groups)               <br />
-- dependency-scaling-groups-app-1                          <br />
-- -- my-tomcat                                             <br />
-- -- my-esb-php-group                                      <br />
-- -- -- my-esb                                             <br />
-- -- -- my-php                                             <br />

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