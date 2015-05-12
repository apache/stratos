Nested Group Application
========================
This application consists of a nested group which is a group inside a group.

Application view
----------------
nested-group-app (single-level-nested-group-app)
-- nested-group-app-1
-- -- my-tomcat
-- -- my-group6
-- -- -- my-tomcat2-group6
-- -- -- my-group7
-- -- -- -- my-tomcat1-group7

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