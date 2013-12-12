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

class ssh ( $port=22, $bind_address, $root_login=no, $pubkey_auth=yes, $password_auth=yes, $use_pam=yes ) {
	
	$service      = "ssh"
       	$template     = "sshd_config.erb"
        $provider     = "apt"

	package { "openssh-server":
		provider	=> $provider,
                ensure 		=> installed,
        }

        service { $service:
                ensure    	=> running,
                enable    	=> true,
                subscribe 	=> File["/etc/ssh/sshd_config"],
                require   	=> Package["openssh-server"],
        }

        file { "/etc/ssh/sshd_config":
                ensure  	=> present,
                content 	=> template("ssh/${template}"),
                require 	=> Package["openssh-server"],
                notify  	=> Service[$service];

		"/etc/pam.d/sshd":
                ensure  	=> present,
                source  	=> "puppet:///ssh/pam/sshd",
                require 	=> Package["openssh-server"],
                notify  	=> Service[$service];
	
		"/home/ubuntu/.ssh/authorized_keys":
		ensure 		=> present,
		source		=> "puppet:///ssh/authorized_keys",
		require		=> Package["openssh-server"];

		"/home/kurumba/.ssh":
		ensure		=> directory;
        }
}

