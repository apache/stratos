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
    Connector : [ "Bezier", { curviness:63 } ]
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
        .addClass('stepnode')
        .attr('data-toggle', 'tooltip')
        .attr('title',groupJSON.alias )
        .appendTo('#whiteboard');
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
            var divCartridge = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[prop].type, 'data-type':'cartridge',
                'data-calias':item[prop]['subscribableInfo']['alias']} )
                .text(item[prop].type)
                .addClass('input-false')
                .addClass('stepnode')
                .attr('data-toggle', 'tooltip')
                .attr('title',item[prop].type )
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
            //$(divGroup).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
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


var signupBlockTemplate = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "id": "root",
    "type": "object",
    "title": " ",
    "options": {
        "disable_properties": true,
        "disable_collapse": true
    },
    "properties": {
        "alias": {
            "id": "root/alias",
            "title": "Cartridge Alias: ",
            "type": "string"
        },
        "privateRepo": {
            "id": "root/privateRepo",
            "title": "Is private repository: ",
            "type": "boolean"
        },
        "repoUrl": {
            "id": "root/repoUrl",
            "title": "Repository URL: ",
            "type": "string"
        },
        "repoUsername": {
            "id": "root/repoUsername",
            "title": "Repository User Name: ",
            "type": "string"
        },
        "repoPassword": {
            "id": "root/repoPassword",
            "title": "Repository Password: ",
            "type": "string",
            "format" : "password"
        }
    }
};

var signupBlockDefault = {
    "alias": "php",
    "privateRepo": false,
    "repoUrl": "https://github.com/imesh/stratos-php-applications.git",
    "repoUsername": "",
    "repoPassword": ""
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
    var editor,blockId;

    $('#whiteboard').on('click', '.stepnode', function(){
        $('.signup-details').show();
        $('.signup-default').hide();
        $('.stepnode').removeClass("highlightme");
        $(this).addClass('highlightme');
        tabData($(this));
    });

    function tabData(node){

        blockId = node.attr('id');
        var blockType = node.attr('data-type');
        var startval;
        var calias = node.attr('data-calias');

        if(blockType == 'cartridge'){
            startval = signupBlockDefault;
            startval['alias'] = calias;
        }

        if(node.attr('data-generated')) {
            startval = JSON.parse(decodeURIComponent(node.attr('data-generated')));
        }
        $('#signup-details').html('');

        switch (blockType){
            case 'cartridge':
                generateHtmlBlock(signupBlockTemplate, startval);
                break;
        }
    }

    function generateHtmlBlock(schema, startval){
        // Initialize the editor
        editor = new JSONEditor(document.getElementById('signup-details'), {
            ajax: false,
            disable_edit_json: true,
            schema: schema,
            format: "grid",
            startval: startval
        });

        if(editor.getEditor('root.alias')){
            editor.getEditor('root.alias').disable();
        }

    }


    //get component JSON data
    $('#component-info-update').on('click', function(){
        $('#'+blockId).attr('data-generated', encodeURIComponent(JSON.stringify(editor.getValue())));
        $('#'+blockId).removeClass('input-false');
        $('#'+blockId).find('div>i').removeClass('fa-exclamation-circle').addClass('fa-check-circle-o').css('color','#2ecc71');
    });


    $('#signup').on('click', function(){
        var btn = $(this);
        var appid = btn.attr('data-appid');
        var signupJSON = {"artifactRepositories":[]};
        //generate raw data tree from connections
        $.each(jsPlumb.getConnections(), function (idx, connection) {
           var dataGen = $('#'+connection.targetId).attr('data-generated');
            if(dataGen!=undefined){
                var jsonContent = JSON.parse(decodeURIComponent(dataGen));
                signupJSON['artifactRepositories'].push(jsonContent);
            }

        });

        var formtype = 'signupapplication';
        btn.html("<i class='fa fa-spinner fa-spin'></i> Adding...");
        $.ajax({
            type: "POST",
            url: caramel.context + "/controllers/applications/application_requests.jag",
            dataType: 'json',
            data: { "formPayload": JSON.stringify(signupJSON), "formtype": formtype, "applicationId": appid },
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
                btn.html('Add Signup');
            });

    });



});