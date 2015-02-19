Wordpress Extended Application
=====================
Wordpress extended application consists of a cartridge group which includes a MySQL cartridge, a PHP cartridge and
an extra Tomcat cartridge on the top level.


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
