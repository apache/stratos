Group Scaling
===========

Application view
----------------
group-scaling
-- group-scaling-1
-- -- my-c4
-- -- my-group1
-- -- -- my-c1-group1
-- -- -- my-group2
-- -- -- -- my-c2-group2
-- -- -- -- my-c3-group2

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