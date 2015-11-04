 #
 # Licensed to the Apache Software Foundation (ASF) under one
 # or more contributor license agreements. See the NOTICE file
 # distributed with this work for additional information
 # regarding copyright ownership. The ASF licenses this file
 # to you under the Apache License, Version 2.0 (the
 # "License"); you may not use this file except in compliance
 # with the License. You may obtain a copy of the License at
 # 
 # http://www.apache.org/licenses/LICENSE-2.0
 # 
 # Unless required by applicable law or agreed to in writing,
 # software distributed under the License is distributed on an
 # "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 # KIND, either express or implied. See the License for the
 # specific language governing permissions and limitations
 # under the License.
 #

# Installing Apache Stratos AWS Extension

Apache Stratos AWS Extension could be used for integrating AWS load balancer with Apache Stratos. Please follow
below steps to proceed with the installation:

1. Extract org.apache.stratos.aws.extension-<version>.zip to a desired location: <aws-extension-home>.

2. Open <aws-extension-home>/conf/aws.properties file in text editor and update AWS access key and secret key information.
   If you are using HTTPS as the FE protocol for the AWS LBs, upload a certificate [1] for the LBs and update 
   load-balancer-ssl-certificate-id with the ARN [2].
   To enable application level sticky sessions, update app-sticky-session-cookie-name with the relevant cookie name. By default
   its using JSESSIONID.

3. Open <aws-extension-home>/bin/aws-extension.sh file in a text editor and update following system properties:
   ```
   # Enable/disable cep statistics publisher:
   -Dcep.stats.publisher.enabled=false

   # If cep statistics publisher is enabled define the following properties:
   -Dthrift.receiver.ip=127.0.0.1
   -Dthrift.receiver.port=7615
   -Dnetwork.partition.id=network-partition-1

   # if running in a VPC, set:
   -Doperating.in.vpc=true

   # if cross-zone loadbalancing is required, set:
   -Denable.cross.zone.load.balancing=true
   ```

4. Open <aws-extension-home>/conf/jndi.properties file in a text editor and update message broker information:
   ```
   java.naming.provider.url=tcp://localhost:61616
   ```
5. Run <aws-extension-home>/bin/aws-extension.sh as the root user.


[1]. http://docs.aws.amazon.com/cli/latest/reference/iam/upload-server-certificate.html

[2]. http://docs.aws.amazon.com/cli/latest/reference/iam/get-server-certificate.html
