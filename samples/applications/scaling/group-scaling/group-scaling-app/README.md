Group Scaling
===========

Application view
----------------
group-scaling               <br />
-- group-scaling-1          <br />
-- -- my-c4                 <br />
-- -- my-group1             <br />
-- -- -- my-c1-group1       <br />
-- -- -- my-group2          <br />
-- -- -- -- my-c2-group2    <br />
-- -- -- -- my-c3-group2    <br />

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