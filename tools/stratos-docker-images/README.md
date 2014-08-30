stratos-docker
==============

docker images for installing stratos

### Building

To build mysql, activemq and stratos images:

`mvn install -Pdocker-build`

The above command requires you to be able to execute the docker command without sudo usually by being in the 'docker' group.


### Upload to registry.hub.docker.com

To upload images:

`mvn install -Pdocker-push`

### Running

Take a look at the Stratos wiki: https://cwiki.apache.org/confluence/display/STRATOS/Running+Stratos+inside+docker
