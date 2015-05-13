Group Scaling v1
================

Application view
----------------
group-scaling-v1            <br />
-- group-scaling-v1-1       <br />
-- -- my-group2             <br />
-- -- -- my-c3              <br />
-- -- -- my-c2              <br />

Application folder structure
----------------------------
-- artifacts/<iaas>/ IaaS specific artifacts                <br />
-- scripts/common/ Common scripts for all iaases            <br />
-- scripts/<iaas> IaaS specific scripts                     <br />

How to run
----------
cd scripts/<iaas>/          <br />
./deploy.sh                 <br />

How to undeploy
---------------
cd scripts/<iaas>/          <br />
./undeploy.sh               <br />