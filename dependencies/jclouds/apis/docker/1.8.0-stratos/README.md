# Docker as a local cloud provider
jclouds-docker is a local cloud provider modelled on [docker](http://www.docker.io). Similar to other jclouds supported
providers, it supports the same portable abstractions offered by jclouds.

##Setup

Please follow these steps to configure your workstation for jclouds-docker:

- install the latest Docker release (please visit http://www.docker.io/gettingstarted/)

If you are on OS X, please consider [boot2docker](http://docs.docker.io/en/latest/installation/mac/) as simplified
way to install docker locally.

#How it works


                                               ---------------   -------------
                                              |   Image(s)    | |   Node(s)   |
                                              ---------------   -------------
     ---------    docker remote api           ----------------------------------------
    | jclouds | ---------------------------> |              DOCKER_HOST              |
     ---------                               ----------------------------------------

##Components

- jclouds \- acts as a java client to access to docker features
- DOCKER_HOST \- hosts Docker API, NB: jclouds-docker assumes that the latest Docker is installed
- Image \- it is a docker image that can be started.
- Node \- is a docker container

## Assumptions

- jclouds-docker assumes that the images specified using the template are ssh'able.
- jclouds-docker will mount ${user.home} to /root of the container

--------------

#Notes:
- jclouds-docker is still at alpha stage please report any issues you find at [jclouds issues](https://github.com/jclouds/jclouds/issues?state=open)
- jclouds-docker has been tested on Mac OSX, it might work on Linux iff vbox is running and set up correctly. However, it will not currently run on Windows.

--------------

#Troubleshooting
As jclouds docker support is quite new, issues may occasionally arise. Please follow these steps to get things going again:

1. Remove all containers

      `$ docker ps -a -q | xargs docker stop | xargs docker rm`
2. remove all the images

    `$ docker images -q | xargs docker rmi`
