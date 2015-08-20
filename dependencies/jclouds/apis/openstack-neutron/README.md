Why OpenStack Neutron forked?
============================

Openstack-neutron 1.8.1 manifest file is malformed in released version. 
This message is passed to Jclouds team and they fixed it in master branch. But it will not be back ported to 1.8.1 release.
So we have to clone openstack-neutron temporarly. 
When we upgrade to next version we can safely remove openstack-neutron clone from Stratos code base. 

Custom changes
==============

In openstack-neutron/pom.xml,

- <jclouds.osgi.export>org.jclouds.openstack.neutron.v2_0*;version="${project.version}"</jclouds.osgi.export>
- <jclouds.osgi.import>
- org.jclouds.rest.internal;version="${jclouds.version}",
- org.jclouds.labs*;version="${project.version}",
- org.jclouds*;version="${jclouds.version}",
- *
- </jclouds.osgi.import>
+ <jclouds.osgi.export>org.jclouds.openstack.neutron.v2*;version="${project.version}"</jclouds.osgi.export>
+ <jclouds.osgi.import>org.jclouds*;version="${project.version}",*</jclouds.osgi.import>

