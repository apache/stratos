single-group-group-scaling
==========================


Application view
----------------
single-group-group-scaling          <br />
-- single-group-group-scaling-1     <br />
-- -- my-group6                     <br />
-- -- -- my-tomcat                  <br />

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