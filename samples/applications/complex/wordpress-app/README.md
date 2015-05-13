Wordpress Application
=====================
Wordpress application consists of a cartridge group which includes a MySQL cartridge and PHP cartridge. The cartridge
group defines a startup dependency to first start MySQL cluster and then the PHP cluster second once the MySQL cluster
is active. Group scaling has been disabled in MySQL, PHP group.

Application View
----------------
wordpress                   <br />
-- wordpress-1              <br />
-- -- mysql-php-group       <br />
-- -- -- my-php             <br />
-- -- -- my-mysql           <br />

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
