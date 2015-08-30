# Installing Apache Stratos LVS Extension

Apache Stratos LVS Extension could be used for integrating LVS load balancer with Apache Stratos. Please follow
below steps to proceed with the installation:

1. Install keepalived and ipvsadm:
   ```
   apt-get install keepalived ipvsadm
   ```

2. Open <lvs-extension-home>/bin/lvs-extension.sh file in a text editor and update following system properties:
   ```
   # Keepalived configuration file location:
   -Dconf.file.path=/etc/keepalived/keepalived.conf

   # Enable/disable cep statistics publisher:
   -Dcep.stats.publisher.enabled=false

   # If cep statistics publisher is enabled define the following properties:
   -Dthrift.receiver.ip=127.0.0.1
   -Dthrift.receiver.port=7615
   -Dnetwork.partition.id=network-partition-1

   # LVS server Virtual IP set for services
   -Dlvs.service.virtualip.set=tomcat2|192.168.56.40,tomcat1|192.168.56.41,tomcat|192.168.56.40
   # Server state (MASTER|BACKUP)
   -Dserver.state=MASTER

   ```

4. Open <lvs-extension-home>/conf/jndi.properties file in a text editor and update message broker information:
   ```
   java.naming.provider.url=tcp://localhost:61616
   ```

5. Run <lvs-extension-home>/bin/lvs-extension.sh as the root user.

