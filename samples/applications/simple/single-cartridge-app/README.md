Single Cartridge Application
============================
A simple application with a php cartridge.

Application view
----------------
single-cartridge-app            <br />
-- single-cartridge-app-1       <br />
-- -- my-php                    <br />

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