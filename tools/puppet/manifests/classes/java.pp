# --------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------

class java { 

	$java_home 	= "jdk1.7.0_07"
	$package 	= "jdk-7u7-linux-x64.tar.gz"

	file {  "/opt/${package}":
		owner	=> root,
		group	=> root,
		mode	=> 755,
		source	=> "puppet:///java/${package}",
		ignore	=> ".svn",
		ensure  => present;

		"/opt/java/jre/lib/security/":
                owner   => root,
                group   => root,
                source  => ["puppet:///java/jars/"],
		ignore	=> ".svn",
                ensure  => present,
                recurse => true,
                require => File["/opt/java"];

		"/opt/java":
		ensure 	=> link,
		target	=> "/opt/${java_home}",
		require	=> Exec["install_java"];

		"/etc/environment":
		owner => root,
		group => root,
		source => 'puppet:///commons/environment';
	}
    
	exec {  "install_java":
		cwd	=> "/opt",
		command => "/bin/tar xzf ${package}",
		unless	=> "/usr/bin/test -d /opt/${java_home}",
		creates	=> "/opt/${java_home}/COPYRIGHT",
		require	=> File["/opt/${package}"],
	} 


}
 
