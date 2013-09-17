;    Licensed to the Apache Software Foundation (ASF) under one
;    or more contributor license agreements.  See the NOTICE file
;    distributed with this work for additional information
;    regarding copyright ownership.  The ASF licenses this file
;    to you under the Apache License, Version 2.0 (the
;    "License"); you may not use this file except in compliance
;    with the License.  You may obtain a copy of the License at

;      http://www.apache.org/licenses/LICENSE-2.0

;    Unless required by applicable law or agreed to in writing,
;    software distributed under the License is distributed on an
;    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
;    KIND, either express or implied.  See the License for the
;    specific language governing permissions and limitations
;    under the License.


;
$TTL 604800
$ORIGIN STRATOS_DOMAIN.
@       14400   IN      SOA     a.STRATOS_DOMAIN.    admin.STRATOS_DOMAIN. (
                                2012112614 ; Serial
                                28800 ; Refresh
                                3600 ; Retry
                                604800 ; Expire
                                38400 ) ; Negative Cache TTL
;
@       IN      A       ELB_IP
@       IN      NS      a.STRATOS_DOMAIN.
git     IN      NS      ELB_IP
adp     IN      NS      ELB_IP
notify.git        IN      NS      ADC_IP
