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

import urllib2, urllib
from urllib2 import URLError, HTTPError
import json
from modules.util.log import LogFactory
from config import CartridgeAgentConfiguration
import constants


log = LogFactory().get_log(__name__)
config = CartridgeAgentConfiguration()
mds_url = config.read_property(constants.METADATA_SERVICE_URL)
alias = config.read_property(constants.CARTRIDGE_ALIAS)
app_id = config.read_property(constants.APPLICATION_ID)
token = config.read_property(constants.TOKEN)
alias_resource_url = mds_url + "/metadata/api/applications/" + app_id + "/clusters/" + alias + "/properties"
app_resource_url = mds_url + "/metadata/api/applications/" + app_id + "/properties"


def put(put_req, app=False):
    """ Publish a set of key values to the metadata service
    :param MDSPutRequest put_req:
    :param
    :return: the response string or None if exception
    :rtype: str
    """
    # serialize put request object to json
    request_data = json.dumps(put_req, default=lambda o: o.__dict__)
    if app:
        put_request = urllib2.Request(app_resource_url)
    else:
        put_request = urllib2.Request(alias_resource_url)

    put_request.add_header("Authorization", "Bearer %s" % token)
    put_request.add_header('Content-Type', 'application/json')

    try:
        log.debug("Publishing metadata to Metadata service. [URL] %s, [DATA] %s" % (put_request.get_full_url(), request_data))
        handler = urllib2.urlopen(put_request, request_data)
        log.debug("Metadata service response: %s" % handler.getcode())

        return handler.read()
    except HTTPError as e:
        log.exception("Error while publishing to Metadata service. The server couldn\'t fulfill the request.: %s" % e)
        return None
    except URLError as e:
        log.exception("Error while publishing to Metadata service. Couldn't reach server URL. : %s" % e)
        return None


def get(app=False):
    """ Retrieves the key value pairs for the application ID from the metadata service
    :param
    :return : MDSResponse object with properties dictionary as key value pairs
    :rtype: MDSResponse
    """
    try:
        if app:
            log.debug("Retrieving metadata from the Metadata service. [URL] %s" % app_resource_url)
            req_response = urllib2.urlopen(app_resource_url)
        else:
            log.debug("Retrieving metadata from the Metadata service. [URL] %s" % alias_resource_url)
            req_response = urllib2.urlopen(alias_resource_url)

        get_response = json.loads(req_response.read())
        properties = get_response["properties"]
        log.debug("Retrieved values from Metadata service: %s" % properties)
        response_obj = MDSResponse()

        for md_property in properties:
            response_obj.properties[md_property["key"]] = md_property["values"]

        return response_obj
    except HTTPError as e:
        log.exception("Error while retrieving from Metadata service. The server couldn\'t fulfill the request.: %s" % e)
        return None
    except URLError as e:
        log.exception("Error while retrieving from Metadata service. Couldn't reach server URL. : %s" % e)
        return None


def update(data):
    raise NotImplementedError


def delete_property_value(property_name, value):
    log.info("Removing property %s value %s " % (property_name, value))
    opener = urllib2.build_opener(urllib2.HTTPHandler)
    request = urllib2.Request(mds_url + "/metadata/api/applications/" + app_id + "/properties/" + property_name + "/value/" + value)
    request.add_header("Authorization", "Bearer %s" % token)
    request.add_header('Content-Type', 'application/json')
    request.get_method = lambda: 'DELETE'
    url = opener.open(request)

    log.info("Property value removed %s " % (url))


class MDSPutRequest:
    """ Class to encapsulate the publish request.
    The properties member is a list of dictionaries with the format as follows.
    {"key": key_name, "values": value}
    """
    properties = []


class MDSResponse:
    """ Class to encapsulate the response from the metadata service retrieval.
    The properties member is a dictionary with the retrieved key value pairs.
    """
    properties = {}
