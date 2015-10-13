# Installing Apache Stratos Nginx Extension

Apache Stratos Nginx Extension could be used for integrating Nginx load balancer with Apache Stratos. Please follow
below steps to proceed with the installation:

1. Download and extract Nginx binary distribution to a desired location: <nginx-home>.

2. Extract org.apache.stratos.nginx.extension-<version>.zip to a desired location: <nginx-extension-home>.

3. Open <nginx-extension-home>/bin/nginx-extension.sh file in a text editor and update following system properties:
   ```
      # Define nginx host private ip address:
      -Dnginx.private.ip=127.0.0.1

      # Define the nginx executable file path:
      -Dexecutable.file.path=<nginx-home>/nginx

      # Update the nginx.conf file patch
      -Dconf.file.path=/etc/nginx/nginx.conf

      #update the certificate for SSL configuration
      -Dnginx.cert.path=/etc/nginx/ssl/server.cert

      #update the server key for SSL configuration
      -Dnginx.key.path=/etc/nginx/ssl/server.key

      # Update the hostname length, if needed
      -Dnginx.server.names.hash.bucket.size=128

      # Enable/disable cep statistics publisher:
      -Dcep.stats.publisher.enabled=false

      # If cep statistics publisher is enabled define the following properties:
      -Dthrift.receiver.ip=127.0.0.1
      -Dthrift.receiver.port=7615
      -Dnetwork.partition.id=network-partition-1
      ```


4. Open <nginx-extension-home>/conf/jndi.properties file in a text editor and update message broker information:
   ```
   java.naming.provider.url=tcp://localhost:61616
   ```

5. Run <nginx-extension-home>/bin/nginx-extension.sh as the root user.

