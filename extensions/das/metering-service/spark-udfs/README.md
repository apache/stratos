This directory contains Spark UDFs (user Defined Function) required for executing the spark queries with UDFs.
Follow the below steps to use UDF in spark environment:
1. Add the jar files of each spark-udfs to '<DAS-HOME>/repository/components/lib'.
    Example: spark-time-udf-4.2.0-SNAPSHOT.jar
2. Add each UDF class path to 'spark-udf-config.xml' file in '<DAS-HOME>/repository/conf/spark/' folder.
    Example: <class-name>org.apache.stratos.das.extension.spark.udf.TimeUDF</class-name>