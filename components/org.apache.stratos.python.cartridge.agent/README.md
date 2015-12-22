Python Cartridge Agent (PCA)
============================

Cartridge agent manages cartridge instance lifecycle and publishes its health statistics
to Complex Event Processor (CEP). It provides a set of extension points for implementing
logic required for configuring the server.

# Configuration
The PCA depends on a few mandatory configurations.

1. Message broker configuration
2. CEP configuration
3. Application Path configuration

## Message broker configuration
The PCA conducts communication with the rest of the Stratos Components mainly via the message broker. Therefore, without the message broker configuration the PCA wouldn't start properly. Following are the configurations related to message broker communication in the PCA.

1. mb.port
2. mb.ip
3. mb.urls
4. mb.username
5. mb.password
6. mb.publisher.timeout

The first two options allow to define a single message broker host and a port. The third option `mb.urls` allows to define a comma separated list of message broker urls, out of which the PCA will failover until a successful connection is made. It should be noted that `mb.urls` has precedence over `mb.ip` and `mb.ip` pair. `mb.ip` and `mb.port` will only be taken in to consideration if only `mb.urls` is empty. If none of the two options are specified, the PCA will not start.
 
Also, when using a list of message brokers, the PCA only allows to specify a single pair of `mb.username` and `mb.password`.

`mb.publisher.timeout` specifies the maximum timeout value (in seconds) for the message publisher to retry publishing an event, before giving up and dropping the event. 

```ini
mb.port = 1883
mb.host = 10.100.4.33
mb.urls = 10.100.4.21:1883,10.100.4.22:1883,10.100.4.23:1885
mb.username = system
mb.password = manager
```

## CEP configuration
The PCA publishes health statistics to a CEP via Thrift. For this, the list of Thrift receivers should be specified using the following configuration options. 

1. thrift.receiver.urls
2. thrift.server.admin.username
3. thrift.server.admin.password
4. cep.stats.publisher.enabled

```ini
thrift.receiver.urls = 10.100.4.21:7711,10.100.4.22:7711,10.100.4.23:7711
thrift.server.admin.username = admin
thrift.server.admin.password = admin
cep.stats.publisher.enabled = true
```

## Application Path configuration

`APPLICATION_PATH` refers to the document root of the application service that is running in the specific instance which the PCA manages. For example, in a PHP instance this would be `/var/www`. This has to be either passed through the IaaS metadata service or passed in via the `agent.conf` configuration file.

# How to run Live Tests
1. Install following packages

   apt-get install -y git python python-pip python-dev gcc zip 

2. Install dependent python modules:
   ```
   pip install paho-mqtt
   pip install psutil
   pip install pexpect
   pip install pycrypto
   pip install gitpython
   pip install yapsy
   ```

3. Start ActiveMQ:
   ```
   <activemq-home>/bin/activemq start
   ```

4. Run python cartridge agent live tests:
   ```
   mvn clean install -Plive
   ```
