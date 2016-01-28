# Apache Stratos DAS Extension

This directory contains DAS Extensions needed for Stratos.

1. Add org.apache.stratos.das.extension-<stratos-version>.jar file to `<DAS-HOME>/repository/components/lib/`.

2. Add UDF class `<class-name>org.apache.stratos.das.extension.TimeUDF</class-name>` to 'spark-udf-config.xml'
file in `<DAS-HOME>/repository/conf/analytics/spark/ folder`.