single-group-group-scaling
==========================


Application view
----------------
single-group-group-scaling
-- single-group-group-scaling-1
-- -- my-group6
-- -- -- my-tomcat

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