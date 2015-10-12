# Apache Stratos Metering Dashboard

This directory contains following artifacts:
(1) capps - Includes stratos-metering-service car file which bundles all Event Stream, Event receiver, Even Store, Gadgets and Dashboard artifacts.
(2) jaggery-files
(3) ues-patch

Follow the below steps to generate the metering dashboard:
1. Add the jaggery files which can be found inside directory 'jaggery-files' to DAS server path '/jaggeryapps/portal/controllers/apis'
2. Create MySQL database and tables using queries in 'mysqlscript.sql' manually.
3. Apply ues-patch to DAS server as mentioned in its README file.
3. Add stratos-metering-service car file to DAS server to generate the metering dashboard.