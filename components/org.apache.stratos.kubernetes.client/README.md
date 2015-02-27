Kubernetes API Client Live Tests
================================

Kubernetes API client supports running live tests against a Kubernetes cluster. Following parameters are needed to run the tests.

Parameters
----------
###kubernetes.api.endpoint
The Kubernetes API endpoint URL, default value: http://172.17.8.101:8080/api/v1beta1

###minion.public.ips
The list of public IP addresses of minions in the Kubernetes cluster, default value: 172.17.8.101

###docker.image
The name of the docker image to be used by the test, default value: fnichol/uhttpd

###container.port
A port exposed by the docker image to test Kubernetes services, default value: 6379

###test.pod.activation
A boolean property to enable or disable pod activation test, default value: true

###test.service.socket
A boolean property to enable or disable service socket test, default value: true


Pre-requisites
==============

* Setup a Kubernetes cluster with at least one minion.
* Login to the minion host and pull the preferred docker image.

How to run the tests
====================

* Clone Stratos source code:

  ```
  git clone https://git-wip-us.apache.org/repos/asf/stratos.git
  ```

* Navigate to the Kubernetes API client component:

  ```
  cd <stratos-source-home>/components/org.apache.stratos.kubernetes.client
  ```

* Run the live tests with proper parameter values:

    ### Run with default values:
    ```
    mvn clean test -Plive
    ```

    ### Run with custom values:
    ```
    mvn clean test -Plive -Dkubernetes.api.endpoint="http://192.168.1.101:8080/api/v1beta1/" \
                          -Dminion.public.ips="192.168.1.102"
                          -Ddocker.image="gurpartap/redis" \
                          -Dcontainer.port=6379 \
                          -Dtest.pod.activation="true" \
                          -Dtest.service.socket="true"
    ```
    
