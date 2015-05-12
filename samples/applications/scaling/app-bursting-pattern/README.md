App Bursting Pattern Application
================================


Application view
----------------
app-bursting-pattern-app
-- app-bursting-pattern-app-1
-- -- my-php
-- app-bursting-pattern-app-2
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