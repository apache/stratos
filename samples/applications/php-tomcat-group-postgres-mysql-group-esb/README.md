single_cartridge
================
i) In this artifact sample you can find we have listed them as mock and openstack.

ii) In this sample artifact, it deployes simple application with a php cartridge and start it.

iii) You can choose the IaaS and navigate to it and simply run the single_cartridge.sh file. It'll deploy the relevant artifacts and start the application.


This sample can be used to test the startup order pattern. As defined, the application is using dbgroup(mysql, postgres), app-group(tomcat, php) and esb. In this case, appgroup and esb are depending on dbgroup. So, dbgroup should start first.  Other two can come up in parallel after dbgroup started. When starting of appgroup, tomcat depends on php. In that case, tomcat will have to come up first and then php. By running this sample, this particular scenario can be simulated.
