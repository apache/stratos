Python Cartridge Agent (PCA)
============================

Cartridge agent manages cartridge instance lifecycle and publishes its health statistics
to Complex Event Processor (CEP). It provides a set of extension points for implementing
logic required for configuring the server.

How to run Live Tests
---------------------

1. Install dependent python modules:
   ```
   pip install paho-mqtt
   pip install psutil
   pip install pexpect
   pip install pycrypto
   pip install gitpython
   ```

2. Start ActiveMQ:
   ```
   <activemq-home>/bin/activemq start
   ```

3. Run python cartridge agent live tests:
   ```
   mvn clean install -Plive
   ```