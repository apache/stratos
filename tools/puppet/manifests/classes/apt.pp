## --------------------------------------------------------------
##
## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.
##
## --------------------------------------------------------------
#

class apt {
#	$packages = ["lsof","unzip","sysstat","telnet","nmap","less","nagios-nrpe-server","ganglia-monitor"]
	$packages = ["lsof","unzip","sysstat","telnet","nmap","less"]

        file { "/etc/apt/apt.conf.d/90forceyes":
                ensure  => present,
                source  => "puppet:///apt/90forceyes";
        }

#        exec { "update-apt":
#                path    => ['/bin', '/usr/bin'],
#                command => "apt-get update > /dev/null 2>&1 &",
#                require => File["/etc/apt/apt.conf.d/90forceyes"],
#        }

        package { $packages:
                provider        => apt,
                ensure          => installed,
                require         => File["/etc/apt/apt.conf.d/90forceyes"],
        }	
}
