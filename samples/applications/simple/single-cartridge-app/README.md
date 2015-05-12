Single Cartridge Application
============================
A simple application with a php cartridge.

Application view
----------------
single-cartridge-app
-- single-cartridge-app-1
-- -- my-php

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