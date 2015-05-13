Complex Nested Application
==========================
This is a complex nested application with three levels of groups and cartridge.

Application View
----------------
my-esb-php-nested-with-esb-php-nested-with-mysql-php-app    <br />
-- esb                                                      <br />
-- php                                                      <br />
-- esb-php-nested-with-esb-php-nested-with-mysql-php        <br />
-- -- esb                                                   <br />
-- -- php                                                   <br />
-- -- esb-php-nested-with-mysql-php                         <br />
-- -- -- esb                                                <br />
-- -- -- php                                                <br />
-- -- -- mysql-php                                          <br />
-- -- -- -- mysql                                           <br />
-- -- -- -- php                                             <br />

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