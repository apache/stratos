App Bursting Pattern Application
================================


Application view
----------------
app-bursting-pattern-app                <br />
-- app-bursting-pattern-app-1           <br />
-- -- my-php                            <br />
-- app-bursting-pattern-app-2           <br />
-- -- my-php                            <br />

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