Single Cartridge Application in a multi cloud environment
=========================================================
A simple application with a php cartridge.

Application view
----------------
single-cartridge-app            <br />
-- single-cartridge-app-1       <br />
-- -- my-php                    <br />
-- single-cartridge-app-2       <br />
-- -- my-php                    <br />
-- single-cartridge-app-3       <br />
-- -- my-php                    <br />

Application folder structure
----------------------------
-- artifacts/multi/ IaaS specific artifacts                <br />
-- scripts/common/ Common scripts for all iaases            <br />
-- scripts/multi IaaS specific scripts                     <br />

How to run
----------
cd scripts/multi/          <br />
./deploy.sh                 <br />

How to undeploy
---------------
cd scripts/multi/          <br />
./undeploy.sh               <br />