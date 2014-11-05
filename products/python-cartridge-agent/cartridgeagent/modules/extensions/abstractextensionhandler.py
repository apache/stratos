# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

class AbstractExtensionHandler:

    def on_instance_started_event(self):
        raise NotImplementedError

    def on_instance_activated_event(self):
        raise NotImplementedError

    def on_artifact_updated_event(self, artifacts_updated_event):
        raise NotImplementedError

    def on_artifact_update_scheduler_event(self, tenant_id):
        raise NotImplementedError

    def on_instance_cleanup_cluster_event(self, instance_cleanup_cluster_event):
        raise NotImplementedError

    def on_instance_cleanup_member_event(self, instance_cleanup_member_event):
        raise NotImplementedError

    def on_member_activated_event(self, member_activated_event):
        raise NotImplementedError

    def on_complete_topology_event(self, complete_topology_event):
        raise NotImplementedError

    def on_instance_spawned_event(self, instance_spawned_event):
        raise NotImplementedError

    def on_complete_tenant_event(self, complete_tenant_event):
        raise NotImplementedError

    def on_member_terminated_event(self, member_terminated_event):
        raise NotImplementedError

    def on_member_suspended_event(self, member_suspended_event):
        raise NotImplementedError

    def on_member_started_event(self, member_started_event):
        raise NotImplementedError

    def start_server_extension(self):
        raise NotImplementedError

    def volume_mount_extension(self, persistence_mappings_payload):
        raise NotImplementedError

    def on_subscription_domain_added_event(self, subscription_domain_added_event):
        raise NotImplementedError

    def on_subscription_domain_removed_event(self, subscription_domain_removed_event):
        raise NotImplementedError

    def on_copy_artifacts_extension(self, src, des):
        raise NotImplementedError

    def on_tenant_subscribed_event(self, tenant_subscribed_event):
            raise NotImplementedError

    def on_tenant_unsubscribed_event(self, tenant_unsubscribed_event):
            raise NotImplementedError
