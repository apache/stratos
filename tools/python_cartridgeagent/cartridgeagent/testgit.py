# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


from modules.extensions.defaultextensionhandler import *
from modules.event.instance.notifier.events import ArtifactUpdatedEvent

event_msg = '{"clusterId":"newoice.php.domain","repoUserName":"chamilad","repoPassword":"eng3lhimm3lsonn3","repoURL":"https://github.com/chamilad/NeWoice","tenantId":"-1234","commitEnabled":false}'
event = ArtifactUpdatedEvent.create_from_json(event_msg)

extension_handler = DefaultExtensionHandler()
extension_handler.on_artifact_updated_event(event)

