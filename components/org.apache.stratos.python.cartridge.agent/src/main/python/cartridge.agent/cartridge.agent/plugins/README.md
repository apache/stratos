# Python Cartridge Agent Plugins
The Python Cartridge Agent (PCA) collects and loads plugins dropped in the <PCA_HOME>/plugins folder. For this, the PCA makes use of the simple [Yapsy plugin framework](http://yapsy.sourceforge.net/).

Each plugin can be mapped to one or more events that are received by the agent, and each event can have several plugins registered to it. Each plugin is executed in a blocking manner, sequentially.

## Plugin Descriptor
Each plugin comprises of the plugin implementation and the plugin descriptor. The plugin descriptor primarily specifies the plugin implementation file (or folder) and the events that the particular plugin registers to. If the plugin registers to several events, they are to be specified as a comma delimited string. The format of the plugin descriptor is as follows.

```
[Core]
Name = <A unique meaningful name to identify the plugin>
Module = <plugin implementation filename without ".py">

[Documentation]
Description = <A comma delimited list of events the plugin should be run against>
Author = <author>
Version = <version>
Website = <web site>
```

The plugin descriptor should be of extension ".yapsy-plugin".

## Plugin Implementation

A plugin should be implemented as a sub class of `plugins.contracts.ICartridgeAgentPlugin` abstract class. The execution entry point for a plugin is the `run_plugin()` method which accepts one argument. To this argument, a dictionary is passed from the plugin executor. The dictionary comprises of the payload parameters and other useful values used in the PCA.

A plugin can make use of the `LogFactory` class of the PCA to express its workflow in the PCA log. The use of the log is demonstrated in the sample that follows.


## Sample Plugin

This sample plugin, TestPlugin, will do nothing more than to print the values dictionary it gets from the PCA to the PCA log. It will be executed whenever the following events are received by the PCA.

* CompleteTopologyEvent
* ArtifactUpdatedEvent
* MemberTerminatedEvent

### TestPlugin Plugin Descriptor - TestPlugin.yapsy-plugin

```
[Core]
Name = TestPlugin to demonstrate the pluggable nature of the Apache Stratos PCA
Module = TestPlugin

[Documentation]
Description = CompleteTopologyEvent,ArtifactUpdatedEvent,MemberTerminatedEvent
Author = Stratos
Version = 0.1
Website = stratos.apache.org
```

### TestPlugin Plugin Implementation - TestPlugin.py

```Python
from plugins.contracts import ICartridgeAgentPlugin # Import the ICartridgeAgentPlugin parent class
from modules.util.log import LogFactory # Import the LogFactory module from PCA


class TestPlugin(ICartridgeAgentPlugin): # Subclass from the ICartridgeAgentPlugin

    def run_plugin(self, values): # Implement the plugin execution in the run_plugin() method
        log = LogFactory().get_log(__name__) # create the logger object
        log.debug("Running test plugin")
        for key, value in values.iteritems(): # iterate through the values dictionary
            log.debug("Key: %s => Value:%s" % (key, value))
```

## Metadata Service Client

A plugin can also make use of the Stratos Metadata Service Client that is included with the PCA.

### Importing

```Python
import mdsclient
```

### Publishing Metadata to the Metadata service
The metadata can be published at two levels, application or cartridge alias. If a certain set of entries should be made available to the whole application, then publish it under the application level, by setting `app` argument to `True`. If an entry is published to the alias level, the set the `app` argument to `False` (or simply do not set the `app` argument. It takes `False` as the default value).

```Python
pub_req = mdsclient.MDSPutRequest()
pub_entry_1 = {"key": "key1", "values": "value1"}
pub_entry_2 = {"key": "key2", "values": "value2"}

pub_properties = [pub_entry_1, pub_entry_2]
pub_req.properties = pub_properties

# App level
mdsclient.put(pub_req, app=True)

# Alias level
mdsclient.put(pub_req)
```

### Retrieving values from Metadata service

Like publishing, retrieving values from the Metadata service can be done at two levels, application and alias.

```Python
# App level
mds_response = mdsclient.get(app=True)

# Alias level
mds_response = mdsclient.get()

# mds_response is of type mdsclient.MDSResponse, with a properties dictionary

# If there are no values in the queried level, or if any connectivity related errors come, the response will be None
if mds_response is not None:
    value = mds_response.properties["KEY"]

```

