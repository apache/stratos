Nested Group Application
========================
This application consists of a nested group which is a group inside a group.

Application view
----------------
nested-group-app (single-level-nested-group-app)        <br />
-- nested-group-app-1                                   <br />
-- -- my-tomcat                                         <br />
-- -- my-group6                                         <br />
-- -- -- my-tomcat2-group6                              <br />
-- -- -- my-group7                                      <br />
-- -- -- -- my-tomcat1-group7                           <br />

Application folder structure
----------------------------
-- artifacts/<iaas>/ IaaS specific artifacts            <br />
-- scripts/common/ Common scripts for all iaases        <br />
-- scripts/<iaas> IaaS specific scripts                 <br />

How to run
----------
cd scripts/<iaas>/      <br />
./deploy.sh             <br />

How to undeploy
---------------
cd scripts/<iaas>/      <br />
./undeploy.sh           <br />