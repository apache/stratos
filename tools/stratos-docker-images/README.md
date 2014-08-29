stratos-docker
==============

docker images for installing stratos

### Building

To build mysql, activemq and stratos images:

`mvn install -Pdocker-build`

The above command requires you to be able to execute the docker command without sudo usually by being in the 'docker' group.


### Upload to registry.hub.docker.com

To upload images:

`./push-all.sh`

### Running

Have a look at the example, in particular configuring the IaaS with your settings:

`./run-example.sh`

