Five Level Nested Group Application
===================================
This application consists of five levels of cartridge groups with tomcat and php cartridges.

Application View
----------------
n-level-nesting (five-levels-nested-groups-app)
-- n-level-nesting-1
-- -- n-level-nesting-group
-- -- -- level-one-group
-- -- -- -- level-one-group-tomcat
-- -- -- -- level-one-group-esb
-- -- -- -- level-two-group
-- -- -- -- -- level-two-group-tomcat
-- -- -- -- -- level-two-group-esb
-- -- -- -- -- level-three-group
-- -- -- -- -- -- level-three-group-tomcat
-- -- -- -- -- -- level-three-group-esb
-- -- -- -- -- -- level-four-group
-- -- -- -- -- -- -- level-four-group-tomcat
-- -- -- -- -- -- -- level-four-group-esb
-- -- -- -- -- -- -- level-five-group
-- -- -- -- -- -- -- -- level-five-group-tomcat
-- -- -- -- -- -- -- -- level-five-group-esb

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