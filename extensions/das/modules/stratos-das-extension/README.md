# Apache Stratos DAS Extension

This directory contains DAS Extensions needed for Stratos. <br />
1. Add org.apache.stratos.das.extension-<stratos-version>.jar file to '<DAS-HOME>/repository/components/lib/'. <br />
2. Add below UDF class path to 'spark-udf-config.xml' file in '<DAS-HOME>/repository/conf/analytics/spark/' folder. <br />
   <class-name>org.apache.stratos.das.extension.TimeUDF</class-name> <br />