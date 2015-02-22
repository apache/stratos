Tomcat Application
==================
Tomcat application provides an Apache Tomcat 7 cluster. It only contains the tomcat cartridge and has no cartridge
groups or any dependencies.

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