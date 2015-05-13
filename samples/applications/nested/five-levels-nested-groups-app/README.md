Five Level Nested Group Application
===================================
This application consists of five levels of cartridge groups with tomcat and php cartridges.

Application View
----------------
n-level-nesting (five-levels-nested-groups-app)         <br />
-- n-level-nesting-1                                    <br />
-- -- n-level-nesting-group                             <br />
-- -- -- level-one-group                                <br />
-- -- -- -- level-one-group-tomcat                      <br />
-- -- -- -- level-one-group-esb                         <br />
-- -- -- -- level-two-group                             <br />
-- -- -- -- -- level-two-group-tomcat                   <br />
-- -- -- -- -- level-two-group-esb                      <br />
-- -- -- -- -- level-three-group                        <br />
-- -- -- -- -- -- level-three-group-tomcat              <br />
-- -- -- -- -- -- level-three-group-esb                 <br />
-- -- -- -- -- -- level-four-group                      <br />
-- -- -- -- -- -- -- level-four-group-tomcat            <br />
-- -- -- -- -- -- -- level-four-group-esb               <br />
-- -- -- -- -- -- -- level-five-group                   <br />
-- -- -- -- -- -- -- -- level-five-group-tomcat         <br />
-- -- -- -- -- -- -- -- level-five-group-esb            <br />

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