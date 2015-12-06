# Apache Stratos CEP Extensions

Apache Stratos Complex Event Processor (CEP) extensions include Window Processors for processing health statistic
events. These extensions are available in Stratos binary distribution, in a distributed deployment where CEP is run
externally, these extensions need to be deployed manually.

You can setup stratos with either WSO2 CEP 3.0.0 or WSO2 CEP 3.1.0. Please note that only WSO2 CEP 3.1.0 supports
highly available clustered deployment.

1. Copy jar files in apache-stratos-cep-extension-4.1.5/lib folder to <CEP_HOME>/repository/components/lib. These jar
files are common to both WSO2 CEP 3.0.0 and WSO2 CEP 3.1.0

2. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/eventbuilders to
<CEP_HOME>/repository/deployment/server/eventbuilders

3. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/eventformatters to
<CEP_HOME>/repository/deployment/server/eventformatters

4. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/executionplans to
<CEP_HOME>/repository/deployment/server/executionplans

5. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/inputeventadaptors to
<CEP_HOME>/repository/deployment/server/inputeventadaptors

6. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/outputeventadaptors to
<CEP_HOME>/repository/deployment/server/outputeventadaptors

7. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/streamdefinitions to
<CEP_HOME>/repository/deployment/server/streamdefinitions

8. Update message broker endpoint in <CEP_HOME>/repository/deployment/server/outputeventadaptors/JMSOutputAdaptor.xml

9. Copy files in apache-stratos-cep-extension-4.1.5/wso2cep-<version>/lib to <CEP_HOME>/repository/components/lib

Please refer below link for more information on WSO2 CEP.
http://wso2.com/products/complex-event-processor/


Thank you for using Apache Stratos!
The Stratos Team