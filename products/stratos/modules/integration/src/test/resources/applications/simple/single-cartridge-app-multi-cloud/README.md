Single Cartridge Application in a multi cloud environment
=========================================================
A simple application with a php cartridge in two AWS EC2 regions and Openstack on-premise deployment 

Application view
----------------

                                            single-cartridge-app
                                                     |
                _____________________________________|__________________________________
                |                                    |                                 |
    single-cartridge-app-1(ec2 R1)    single-cartridge-app-2(ec2 R2)   single-cartridge-app-3(Openstack region)
                |                                    |                                 |
         my-php(member 1)                     my-php(member 2)                  my-php(member 3)

Application folder structure
----------------------------
-- artifacts/multi/     IaaS specific artifacts                <br />
-- scripts/common/      Common scripts for all iaases            <br />
-- scripts/multi        IaaS specific scripts                     <br />

How to run
----------
cd scripts/multi/          <br />
./deploy.sh                 <br />

How to undeploy
---------------
cd scripts/multi/          <br />
./undeploy.sh               <br />