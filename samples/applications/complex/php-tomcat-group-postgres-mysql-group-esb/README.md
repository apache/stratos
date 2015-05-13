Composite Application
=====================
This sample can be used to test the startup order pattern. As defined, the application is using dbgroup(mysql, postgres),
app-group(tomcat, php) and esb.
In this case, appgroup and esb are depending on dbgroup. So, dbgroup should start first.
Other two can come up in parallel after dbgroup started. When starting of appgroup, tomcat depends on php.
In that case, tomcat will have to come up first and then php.
By running this sample, this particular scenario can be simulated.

Application View
----------------
my-compositeapp (php-tomcat-group-postgres-mysql-group-esb) <br />
-- my-compositeapp-1                                        <br />
-- -- my-esb                                                <br />
-- -- my-dbgroup                                            <br />
-- -- -- my-postgres                                        <br />
-- -- -- my-mysql                                           <br />
-- -- my-appgroup                                           <br />
-- -- -- my-tomcat                                          <br />
-- -- -- my-php                                             <br />

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
