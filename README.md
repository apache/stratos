Apache Stratos
===========================
Apache Stratos is a PaaS framework that supports polyglot environments (e.g., PHP, MySQL) and 
can be extended to support many more. Apache Stratos is also the foundation for multiple flavors of PaaSes, 
such as Application PaaS (aPaaS), Integration PaaS (iPaaS), or Data PaaS (dPaaS). 
Stratos is licensed under the Apache License, Version 2.0

Features
--------
* Cloud Controller (CC)  
CC leverages Apache jclouds' APIs and provides a generic interface to communicate with different IaaS. 

* Multi-factored auto-scaling  	
The Auto-scaler uses a Complex Event Processor (CEP) for real-time decision making and it integrates both real-time and rule-base decision making in order to provide better control over scaling of platforms. Auto-scaling policies can be defined with the following multiple factors, which are considered when scaling up or down: requests in flight, memory consumption and load average. The Auto-scaler also support scaling for non-HTTP transport.

* Smart policies  
The Auto-scaler uses the following two smart policies when making auto-scaling decisions: Auto-scaling policy and deployment policy. Instances will be automatically spawned based on the smart policies defined in the cartridge.

* Multiple IaaS support  
Apache Stratos is tested on the following IaaS providers: AWS EC2, Openstack and vClouds. However, it is very easy to extend Apache Stratos to support any IaaS that is supported by Apache jclouds.

* Multi-cloud bursting  	
Apache Stratos supports multiple IaaS. When the maximum limit of instances have been reached in an IaaS, instances will be spawned on another IaaS, which is in another partition group. Thereby, this will enable resource peak times to be off-loaded to another cloud.

* Controlling IaaS resources  	
DevOps can define partitions and network partitions to control IaaS resources. Thereby, Apache Stratos can control resources per cloud, region and zone. Controlling of IaaS resources will provide high availability and will solve disaster recovery concerns.

* Loosely coupled communication  
AMQP messaging technology is used to communicate with all the components. Apache Stratos uses an AMQP Message Broker (MB) to communicate in a loosely coupled fashion.

* Multi-tenancy  	
Support for in-container multi-tenancy. Thereby, optimizing resource utilization.

* Cartridges  
Support for PHP, MySQL, Tomcat, Windows based (.NET) cartridges.

* Pluggable architecture support for cartridges  
A cartridge is a package of code that includes a Virtual Machine (VM) image plus additional configuration, which can be plugged into Apache Stratos to offer a new PaaS service. Apache Stratos supports single tenant and multi-tenant cartridges. If needed, tenants can seamlessly add their own cartridges to Apache Stratos.

* Cartridge automation using Puppet  
Cartridges can be easily configured with the use of Puppet.

* Load Balancer (LB) as a cartridge  
LBs in Apache Stratos accepts dynamic cluster domain registrations. In addition, LBs accepts static cluster domain registrations at start-up. Apache Stratos also supports third party LBs (e.g., HAproxy). Thereby, allowing users to use their own LB if required.

* Artifact Distribution Coordinator (ADC)  
ADC takes complete applications and breaks it into per-instance components that are then loaded into instances. ADC supports external Git and GitHub repositories based deployment synchronization. Users are able to use their own Git repository to sync artifacts with a service instance. 

* Stratos Manager Console  
Administrators and tenants can use the Stratos Manager Console, which is a web-based UI management console, to carryout various actions.
Tenants can carry out the following actions: 
 * View the list of available cartridges. 
 *  Subscribe to cartridges. 
 * Unsubscribe from cartridges. 
 * View list of subscribed cartridges. 
Administrators can carry out the following actions: 
 * Register tenants
 * Activate and deactivate tenants
 * View the list of available cartridges
 * View list of subscribed cartridges
 * Subscribe to a cartridge
 * Unsubscribe from a cartridge
 * Deploy a partition
 * Deploy an auto-scaling policy
 * Deploy a deployment policy
 * Deploy a Load Balancer
 * Deploy a single tenant or multi-tenant cartridge
 * Deploy a multi-tenant service cluster for a multi-tenant cartridge

* Interactive CLI Tool  
Tenants use the command line interface (CLI) tool to manage their subscriptions.

* Monitoring and metering  
Apache Stratos provides centralized monitoring and metering. Metering is used to measure the levels of resource utilization. 

* REST API  
DevOps can use REST APIs to carry out various administering functions (e.g., deploying a cartridge, registering a tenant and more).

* Persistent volume support for cartridges  	
If required the DevOps can enable persistent volume for cartridges. If persistent volume has been enabled, Apache Stratos will automatically attach a volume when a new cartridge instance is created.

* Gracefully shutdown instances  
When scaling down, before terminating an instance the Auto-scaler will allow all the existing requests to the instance to gracefully shutdown; while, not accepting any new requests to that instance.

Resources
---------
* Project page: http://stratos.apache.org/ 
* Documentation: https://cwiki.apache.org/confluence/display/STRATOS/Home 
* Dev group: dev@stratos.apache.org
* Issue Tracker: https://issues.apache.org/jira/browse/stratos 
* Twitter: http://twitter.com/ApacheStratos
* Facebook: https://www.facebook.com/apache.stratos

License
-------
Copyright (C) 2013-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0
                                                          
