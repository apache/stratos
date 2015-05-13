Nested Groups Application
=========================
This application consists of two nested groups in the same level.

Application view
----------------
complex-app (single-level-nested-groups-app)            <br />
-- complex-app-1                                        <br />
-- -- mytomcat                                          <br />
-- -- my-group8                                         <br />
-- -- -- my-tomcat2-group8                              <br />
-- -- -- my-group9                                      <br />
-- -- -- -- my-tomcat1-group9                           <br />
-- -- my-group6                                         <br />
-- -- -- my-tomcat2-group6                              <br />
-- -- -- my-group7                                      <br />
-- -- -- -- my-tomcat1-group7                           <br />

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