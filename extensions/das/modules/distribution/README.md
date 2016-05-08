# Apache Stratos DAS Extensions

Apache Stratos Data Analytics Server (DAS) extensions include DAS artifacts and stratos-das-extension jar which has
spark UDF (User Defined Funtion) used in spark script. You can configure an external DAS for stratos metering and
monitoring feature as below:

## Follow the below steps to configure DAS for metering dashboard:

1. Add org.apache.stratos.das.extension-<stratos-version>.jar file to <DAS-HOME>/repository/components/lib/.

2. Add UDF class `<class-name>org.apache.stratos.das.extension.TimeUDF</class-name>` to 'spark-udf-config.xml' file
in `<DAS-HOME>/repository/conf/analytics/spark/` folder.

3. Add jaggery files needed for your database type which can be found in
`<Stratos-DAS-Distribution>/metering-dashboard/jaggery-files/<db>/` to DAS server path
`<DAS_HOME/repository/deployment/server/jaggeryapps/portal/controllers/apis/`.

4. Create MySQL/Oracle database and tables using queries in
`<Stratos-DAS-Distribution>/metering-dashboard/database-scripts/<db>/metering-script.sql` manually.

5. Apply ues-patch files in `<Stratos-DAS-Distribution>/metering-dashboard/ues-patch/` to DAS below:
    -   Copy 'ues-gadgets.js' and 'ues-pubsub.js' files to
    `<DAS-HOME>/repository/deployment/server/jaggeryapps/portal/js/` folder.
    - Copy 'dashboard.jag' file to `<DAS-HOME>/repository/deployment/server/jaggeryapps/portal/theme/templates/` folder.

6. Add stratos-metering-service car file in `<Stratos-DAS-Distribution>/metering-dashboard/` to
`<DAS-HOME>/repository/deployment/server/carbonapps/` to generate the metering dashboard.

## Follow the below steps to configure DAS for monitoring dashboard:

1. Add jaggery files needed for your database type which can be found in
`<Stratos-DAS-Distribution>/monitoring-dashboard/jaggery-files/<db>/` to DAS path
`<DAS_HOME/repository/deployment/server/jaggeryapps/portal/controllers/apis/.

2. Create MySQL/Oracle database and tables using queries in
`<Stratos-DAS-Distribution>/monitoring-dashboard/database-scripts/<db>/monitoring-script.sql` manually.

3. Copy CEP  EventFormatter artifacts in `<Stratos-DAS-Distribution>/wso2cep-<version>/eventformatters/` to
`<CEP-HOME>/repository/deployment/server/eventformatters/`.

4. Copy CEP OutputEventAdapter artifact in `<Stratos-DAS-Distribution>/wso2cep-<version>/outputeventadaptors/` to
`<CEP-HOME>/repository/deployment/server/outputeventadaptors/`  and update the tcp and ssl ports according to DAS server
port offset.

5. Add stratos-monitoring-service car file in `<Stratos-DAS-Distribution>/monitoring-dashboard/` to
`<DAS-HOME>/repository/deployment/server/carbonapps/` to generate the monitoring dashboard.


Please refer below link for more information on WSO2 DAS.
https://docs.wso2.com/display/DAS301/WSO2+Data+Analytics+Server+Documentation

Thank you for using Apache Stratos!