Group Scaling v1
================

Application view
----------------
group-scaling-v1
-- group-scaling-v1-1
-- -- my-group2
-- -- -- my-c3
-- -- -- my-c2

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