# Installing Apache Stratos HAProxy Extension

Apache Stratos HAProxy Extension could be used for integrating HAProxy load balancer with Apache Stratos. Please follow
below steps to proceed with the installation:

1. Download and extract HAProxy binary distribution to a desired location: <haproxy-home>.

2. Extract org.apache.stratos.haproxy.extension-<version>.zip to a desired location: <haproxy-extension-home>.

3. Open <haproxy-extension-home>/bin/haproxy-extension.sh file in a text editor and update following system properties:
   ```
   # Define haproxy host private ip address:
   -Dhaproxy.private.ip=127.0.0.1

   # Define the haproxy executable file path:
   -Dexecutable.file.path=<haproxy-home>/haproxy

   # Enable/disable cep statistics publisher:
   -Dcep.stats.publisher.enabled=false

   # If cep statistics publisher is enabled define the following properties:
   -Dthrift.receiver.ip=127.0.0.1
   -Dthrift.receiver.port=7615
   -Dnetwork.partition.id=network-partition-1
   ```

4. Open <haproxy-extension-home>/conf/jndi.properties file in a text editor and update message broker information:
   ```
   java.naming.provider.url=tcp://localhost:61616
   ```
5. Run <haproxy-extension-home>/bin/haproxy-extension.sh as the root user.

