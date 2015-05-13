Single Group Application
========================
An application with a tomcat cartridge and a cartridge group which consists of esb and php cartridges.

Application view
----------------
single-group-app            <br />
-- cartridge-group-app-1    <br />
-- -- my-tomcat             <br />
-- -- my-esb-php-group      <br />
-- -- -- my-esb             <br />
-- -- -- my-php             <br />

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