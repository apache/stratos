# Apache Stratos Metering Dashboard

## This directory contains following artifacts:

(1) capps - Includes stratos-metering-service car file which bundles all Event Stream, Event receiver, Even Store,
            Gadgets, SparkScripts and Dashboard artifacts.

(2) jaggery-files

(3) ues-patch

## Follow the below steps to generate the metering dashboard:

1. Follow instruction given in <Stratos-SOURCE-HOME>/extensions/das/stratos-das-extension/README.md file to add
stratos-das-extension jar to DAS.

2. Add jaggery files which can be found in <Stratos-DAS-Distribution>/metering-dashboard/jaggery-files/ to DAS server
 path <DAS_HOME/repository/deployment/server/jaggeryapps/portal/controllers/apis/

3. Create MySQL database and tables using queries in
<Stratos-DAS-Distribution>/metering-dashboard/metering-mysqlscript.sql manually.

4. Apply ues-patch files in <Stratos-DAS-Distribution>/metering-dashboard/ues-patch/ to DAS as mentioned in its
README file.

5. Add stratos-metering-service car file in <Stratos-DAS-Distribution>/metering-dashboard/ to
<DAS-HOME>/repository/deployment/server/carbonapps/ to generate the metering dashboard.