Wordpress Application
=====================
Wordpress application consists of a cartridge group which includes a MySQL cartridge and PHP cartridge. The cartridge
group defines a startup dependency to first start MySQL cluster and then the PHP cluster second once the MySQL cluster
is active. Group scaling has been disabled in MySQL, PHP group.

Application View
----------------
wordpress
-- wordpress-1
-- -- mysql-php-group
-- -- -- my-php
-- -- -- my-mysql

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
