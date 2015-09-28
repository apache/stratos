# Apache Stratos Metering Dashboard

This directory contains Composite Applications (CApps) required for Stratos Metering Service.
It includes all Event Stream, Event receiver, Even Store, Gadgets and Dashboard artifacts bundle inside the car file.
Follow the below steps to generate the metering dashboard:
1. Run a external DAS server
2. Enable thrift stats publisher for das in thrift-client-config.xml file and update DAS server IP and thrift port in the same file as follow:
    <config>
        <name>das</name>
        <statsPublisherEnabled>false</statsPublisherEnabled>
        <username>admin</username>
        <password>admin</password>
        <ip>localhost</ip> <!-- DAS server IP -->
        <port>7612</port> <!-- DAS thrift port -->
    </config>
2. Add the jaggery files which can be found inside directory 'jaggery-files' to inside server path '/jaggeryapps/portal/controllers/apis'
3. Create MySQL data tables using queries in 'mysqlscript.sql'
4. Add the car file to server to generate the metering dashboard.