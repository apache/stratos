from ..thriftcom.Publisher import *
from ..config.cartridgeagentconfiguration import CartridgeAgentConfiguration
import json


def get_valid_tenant_id(tenant_id):
    raise NotImplementedError


def get_alias(cluster_id):
    raise NotImplementedError


def get_current_date():
    raise NotImplementedError

ip = '192.168.1.2'	# IP address of the server
port = 7711		# Thrift listen port of the server
username = 'admin'	# username
password = 'admin' 	# passowrd

# Initialize publisher with ip and port of server
publisher = Publisher(ip, port)

# Connect to server with username and password
publisher.connect(username, password)

# Define stream definition
valid_tenant_id = get_valid_tenant_id(CartridgeAgentConfiguration.tenant_id)
alias = get_alias(CartridgeAgentConfiguration.cluster_id)
stream_name = "logs." + valid_tenant_id + "." \
              + alias + "." + get_current_date()

stream_version = "1.0.0"
payload_data = '{"name": "tenantID", "type": "STRING"}, {"name": "serverName", "type": "STRING"}, {"name": "appName", "type": "STRING"}, {"name": "logTime", "type": "STRING"}, {"name": "priority", "type": "STRING"}, {"name": "message", "type": "STRING"}, {"name": "logger", "type": "STRING"}, {"name": "ip", "type": "STRING"}, {"name": "instance", "type": "STRING"}, {"name": "stacktrace", "type": "STRING"}'

meta_data = '{"name": "memberId", "type": "STRING"}'

streamDefinition = "{ 'name': '" + stream_name + "', 'version':'" + stream_version + \
                   "', 'metaData':'" + meta_data + \
                   "', 'payloadData':'" + payload_data + "' }"

# streamDefinition = "{ 'name': '" + stream_name + "', 'version':'" + stream_version + \
#                    "', 'payloadData':'" + json.dumps(payload_data) + "' }"

publisher.defineStream(streamDefinition)

#compile the event
event = EventBundle()
#add meta data
event.addStringAttribute(CartridgeAgentConfiguration.member_id)
#add correlation data

#add payload data
event.addStringAttribute(valid_tenant_id)
event.addStringAttribute(alias)
event.addStringAttribute("")
event.addStringAttribute(get_current_date())
event.addStringAttribute("")
event.addStringAttribute("this line")
event.addStringAttribute("")
event.addStringAttribute("")
event.addStringAttribute(CartridgeAgentConfiguration.member_id)
event.addStringAttribute("")

# Publish sample message
publisher.publish(event)

# Disconnect
publisher.disconnect()

