Dependent Scaling With Cartridges Application
=============================================

Application View
----------------
dependency-scaling-cartridges-app (sample-cartridges)       <br />
-- dependency-scaling-cartridges-app1                       <br />
-- -- my-tomcat                                             <br />
-- -- my-php                                                <br />

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