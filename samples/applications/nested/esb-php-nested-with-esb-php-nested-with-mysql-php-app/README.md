Complex Nested Application
==========================
This is a complex nested application with three levels of groups and cartridge.

Application View
----------------
my-esb-php-nested-with-esb-php-nested-with-mysql-php-app
-- esb
-- php
-- esb-php-nested-with-esb-php-nested-with-mysql-php
-- -- esb
-- -- php
-- -- esb-php-nested-with-mysql-php
-- -- -- esb
-- -- -- php
-- -- -- mysql-php
-- -- -- -- mysql
-- -- -- -- php

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