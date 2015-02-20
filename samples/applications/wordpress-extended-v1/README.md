Wordpress Extended V1 Application
=================================
Wordpress extended v1 application consists of a cartridge group which includes a MySQL cartridge and PHP cartridge and
a Tomcat cartridge at the top level. The cartridge group defines a startup dependency to first start MySQL cluster and
then the PHP cluster once the MySQL cluster is active. Group scaling has been disabled in MySQL, PHP group. The
application has defined a startup dependency to first start the MySQL, PHP group clusters and then the Tomcat cluster.


Application folder structure
----------------------------
```
artifacts/<iaas>/ IaaS specific artifacts
scripts/common/ Common scripts for all iaases
scripts/<iaas> IaaS specific scripts
```

How to run
----------
```
cd scripts/<iaas>/
./deploy.sh
```
