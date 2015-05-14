/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

// repaint
function Repaint(){
    $("#whiteboard").resize(function(){
        jsPlumb.repaintEverything();
    });
}
// drag
function DragEl(el){
    jsPlumb.draggable($(el) ,{
        containment:"#whiteboard"
    });
}


// JsPlumb Config
var color = "gray",
    exampleColor = "#00f",
    arrowCommon = { foldback:0.7, fillStyle:color, width:14 };

jsPlumb.importDefaults({
    Connector : [ "Bezier", { curviness:63 } ],
    /*Overlays: [
     [ "Arrow", { location:0.7 }, arrowCommon ],
     ]*/
});


var nodeDropOptions = {
    activeClass:"dragActive"
};

var bottomConnectorOptions = {
    endpoint:"Rectangle",
    paintStyle:{ width:25, height:21, fillStyle:'#666' },
    isSource:true,
    connectorStyle : { strokeStyle:"#666" },
    isTarget:false,
    maxConnections:20
};

var endpointOptions = {
    isTarget:true,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: nodeDropOptions,
    maxConnections:1
};

var groupOptions = {
    isTarget:true,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: nodeDropOptions,
    maxConnections:1
};

var generatedCartridgeEndpointOptions = {
    isTarget:false,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: '',
    maxConnections:1
};

var generatedGroupOptions = {
    isTarget:false,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: nodeDropOptions,
    maxConnections:1
};

function dagrePosition(){
    // construct dagre graph from JsPlumb graph
    var g = new dagre.graphlib.Graph();
    g.setGraph({ranksep:'80'});
    g.setDefaultEdgeLabel(function() { return {}; });
    var nodes = $(".stepnode");

    for (var i = 0; i < nodes.length; i++) {
        var n = nodes[i];
        g.setNode(n.id, {width: 52, height: 52});
    }
    var edges = jsPlumb.getAllConnections();
    for (var i = 0; i < edges.length; i++) {
        var c = edges[i];
        g.setEdge(c.source.id,c.target.id );
    }
    // calculate the layout (i.e. node positions)
    dagre.layout(g);

    // Applying the calculated layout
    g.nodes().forEach(function(v) {
        $("#" + v).css("left", g.node(v).x + "px");
        $("#" + v).css("top", g.node(v).y + "px");
    });
    jsPlumb.repaintEverything();
}
//add group to editor
var cartridgeCounter =0;
//add group to editor
function addJsplumbGroup(groupJSON, cartridgeCounter){

    var divRoot = $('<div>').attr({'id':cartridgeCounter+'-'+groupJSON.alias,'data-type':'group','data-ctype':groupJSON.alias})
        .text(groupJSON.alias)
        .addClass('input-false')
        .addClass('application')
        .attr('data-toggle', 'tooltip')
        .attr('title',groupJSON.alias )
        .addClass('stepnode')
        .appendTo('#whiteboard');
    $(divRoot).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
    jsPlumb.addEndpoint($(divRoot), {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);

    DragEl($(divRoot));
    if(groupJSON['components']['cartridges']) {
        genJsplumbCartridge(groupJSON['components']['cartridges'], divRoot, groupJSON.alias);
    }
    if(groupJSON['components']['groups']){
        genJsplumbGroups(groupJSON['components']['groups'], divRoot, groupJSON.alias);
    }

    function genJsplumbCartridge(item, currentParent, parentName){
        for (var prop in item) {
            var id = item[prop].type;
            var divCartridge = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[prop].type} )
                .text(item[prop].type)
                .addClass('input-false')
                .attr('data-toggle', 'tooltip')
                .attr('title',item[prop].type )
                .addClass('stepnode')
                .appendTo('#whiteboard');

            $(divCartridge).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');

            jsPlumb.addEndpoint($(divCartridge), {
                anchor: "TopCenter"
            }, generatedCartridgeEndpointOptions);

            //add connection options
            jsPlumb.connect({
                source:$(currentParent),
                target:$(divCartridge),
                paintStyle:{strokeStyle:"blue", lineWidth:1 },
                Connector : [ "Bezier", { curviness:63 } ],
                anchors:["BottomCenter", "TopCenter"],
                endpoint:"Dot"
            });

            DragEl($(divCartridge));
        }
    }

    function genJsplumbGroups(item, currentParent, parentName) {
        for (var prop in item) {
            var divGroup = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[prop]['name'],'data-type':'group','data-ctype':item[prop]['name'] })
                .text(item[prop]['name'])
                .addClass('stepnode')
                .attr('data-toggle', 'tooltip')
                .attr('title',item[prop]['name'] )
                .addClass('input-false')
                .appendTo('#whiteboard');

            $(divGroup).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');

            jsPlumb.addEndpoint($(divGroup), {
                anchor:"BottomCenter"
            }, bottomConnectorOptions);

            jsPlumb.addEndpoint($(divGroup), {
                anchor: "TopCenter"
            }, generatedGroupOptions);

            //add connection options
            jsPlumb.connect({
                source:$(currentParent),
                target:$(divGroup),
                paintStyle:{strokeStyle:"blue", lineWidth:1 },
                Connector : [ "Bezier", { curviness:63 } ],
                anchors:["BottomCenter", "TopCenter"],
                endpoint:"Dot"
            });

            DragEl($(divGroup));

            if(item[prop].hasOwnProperty('cartridges')) {
                genJsplumbCartridge(item[prop].cartridges, divGroup, parentName+'-'+item[prop]['name'] );
            }
            if(item[prop].hasOwnProperty('groups')) {
                genJsplumbGroups(item[prop].groups, divGroup, parentName+'-'+item[prop]['name'])
            }
        }
    }



}


jsPlumb.bind("ready", function() {
    addJsplumbGroup(applicationJSON, cartridgeCounter);
    //reposition after group add
    dagrePosition();
});

//use to activate tab
function activateTab(tab){
    $('.nav-tabs a[href="#' + tab + '"]').tab('show');
};

var applicationPolicyTemplate = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "id": "root",
    "type": "object",
    "options": {
        "disable_properties": true,
        "disable_collapse": true
    },
    "properties": {
        "applicationId": {
            "id": "root/applicationId",
            "type": "string",
            "title":"Application Id"
        },
        "applicationPolicy": {
            "id": "root/applicationPolicy",
            "type": "object",
            "options": {
                "disable_properties": true,
                "disable_collapse": true
            },
            "properties": {
                "networkPartition": {
                    "id": "root/applicationPolicy/networkPartition",
                    "type": "array",
                    "items": {
                        "id": "root/applicationPolicy/networkPartition/0",
                        "type": "object",
                        "properties": {
                            "id": {
                                "id": "root/applicationPolicy/networkPartition/0/id",
                                "type": "string"
                            },
                            "activeByDefault": {
                                "id": "root/applicationPolicy/networkPartition/0/activeByDefault",
                                "type": "boolean"
                            },
                            "partitions": {
                                "id": "root/applicationPolicy/networkPartition/0/partitions",
                                "type": "array",
                                "items": {
                                    "id": "root/applicationPolicy/networkPartition/0/partitions/0",
                                    "type": "object",
                                    "properties": {
                                        "id": {
                                            "id": "root/applicationPolicy/networkPartition/0/partitions/0/id",
                                            "type": "string"
                                        },
                                        "provider": {
                                            "id": "root/applicationPolicy/networkPartition/0/partitions/0/provider",
                                            "type": "string"
                                        },
                                        "property": {
                                            "id": "root/applicationPolicy/networkPartition/0/partitions/0/property",
                                            "type": "array",
                                            "items": {
                                                "id": "root/applicationPolicy/networkPartition/0/partitions/0/property/0",
                                                "type": "object",
                                                "properties": {
                                                    "name": {
                                                        "id": "root/applicationPolicy/networkPartition/0/partitions/0/property/0/name",
                                                        "type": "string"
                                                    },
                                                    "value": {
                                                        "id": "root/applicationPolicy/networkPartition/0/partitions/0/property/0/value",
                                                        "type": "string"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
};

var applicationPolicyDefault = {
    "applicationId": "app_cartridge_v1",
    "applicationPolicy": {
        "networkPartition": [
            {
                "id": "openstack_R1",
                "activeByDefault": "true",
                "partitions": [
                    {
                        "id": "P1",
                        "provider": "mock",
                        "property": [
                            {
                                "name": "region",
                                "value": "RegionOne"
                            }
                        ]
                    }
                ]
            }
        ]
    }
};
// Document ready events
$(document).ready(function(){

    //handled Ajax base session expire issue
    $(document).ajaxError(function (e, xhr, settings) {
        window.location.href = '../';
    });

    //*******************Adding JSON editor *************//
    JSONEditor.defaults.theme = 'bootstrap3';
    JSONEditor.defaults.iconlib = 'fontawesome4';
    JSONEditor.defaults.show_errors = "always";
    var applicationPolicyEditor, childPolicyEditor;

/*    applicationPolicyDefault['applicationId']= applicationJSON.applicationId;

    applicationPolicyEditor = new JSONEditor(document.getElementById('deploy-ui'), {
        ajax: false,
        disable_edit_json: true,
        schema: applicationPolicyTemplate,
        format: "grid",
        startval: applicationPolicyDefault
    });
    applicationPolicyEditor.getEditor('root.applicationId').disable();*/

    $('#whiteboard').on('click', '.stepnode', function(){
        tabData($(this));
        treeActivation($(this));
    });

    function tabData(node){
        //get tab activated
        if(node.hasClass( "application" )){
            activateTab('general');
        }else{
            activateTab('components');
        }

    }

    function treeActivation(node){
        var treePath = jsPlumb.getAllConnections();

        for (var i = 0; i < treePath.length; i++) {
            var nodeitem = treePath[i];
            var nodeid = node.attr('id');
            if(nodeitem.source.id == nodeid){
                $('#'+nodeitem.target.id).addClass('stepnode-disable');
            }else if(nodeitem.target.id == nodeid){
                $('#'+nodeitem.source.id).addClass('stepnode-disable');
            }
        }

    }

    function generateHtmlBlock(schema, startval){
        // Initialize the editor
        childPolicyEditor = new JSONEditor(document.getElementById('component-data'), {
            ajax: false,
            disable_edit_json: true,
            schema: schema,
            format: "grid",
            startval: startval
        });

    }

    //trigger deploy button
    $('#deploy').click(function(){
        var deployjson = $('#app-policy-id').val();
        var formtype = 'deployments';
        var applicationId = applicationJSON.applicationId;
        var btn = $(this);

        btn.html("<i class='fa fa-spinner fa-spin'></i> Adding Application Policy ");
        $.ajax({
            type: "POST",
            url: caramel.context + "/controllers/applications/application_requests.jag",
            dataType: 'json',
            data: { "formPayload": deployjson, "formtype": formtype, "applicationId":applicationId },
            success: function (data) {
                if (data.status == 'error') {
                    var n = noty({text: data.message, layout: 'bottomRight', type: 'error'});
                } else if (data.status == 'warning') {
                    var n = noty({text: data.message, layout: 'bottomRight', type: 'warning'});
                } else {
                    var n = noty({text: data.message, layout: 'bottomRight', type: 'success'});
                    window.setTimeout(function(){
                        window.location.href = '../';
                    }, 1500);
                }
            }
        })
            .always(function () {
                btn.html('Add '+formtype);
            });

    });


});