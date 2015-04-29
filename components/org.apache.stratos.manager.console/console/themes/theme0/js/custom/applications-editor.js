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

jsPlumb.ready(function() {
    //create application level block
    jsPlumb.addEndpoint('applicationId', {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);
});

var cartridgeCounter=0;
//add cartridge to editor
function addJsplumbCartridge(idname, cartridgeCounter) {

    var Div = $('<div>').attr({'id':cartridgeCounter+'-'+idname, 'data-type':'cartridge', 'data-ctype':idname } )
        .addClass('input-false')
        .attr('data-toggle', 'tooltip')
        .attr('title',idname)
        .appendTo('#whiteboard');
    $(Div).append('<span>'+idname+'</span>');
    $(Div).addClass('stepnode');
    jsPlumb.addEndpoint($(Div), {
        anchor: "TopCenter"
    }, endpointOptions);
    // jsPlumb.addEndpoint($(Div), sourceEndpoint);
    $(Div).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
    DragEl($(Div));
    Repaint();
}

//add group to editor
function addJsplumbGroup(groupJSON, cartridgeCounter){

    var divRoot = $('<div>').attr({'id':cartridgeCounter+'-'+groupJSON.name,'data-type':'group','data-ctype':groupJSON.name})
        .addClass('input-false')
        .attr('data-toggle', 'tooltip')
        .attr('title',groupJSON.name)
        .addClass('stepnode')
        .appendTo('#whiteboard');
    $(divRoot).append('<span>'+groupJSON.name+'</span>');
    $(divRoot).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
    jsPlumb.addEndpoint($(divRoot), {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);

    jsPlumb.addEndpoint($(divRoot), {
        anchor: "TopCenter"
    }, groupOptions);
    DragEl($(divRoot));

    for (var prop in groupJSON) {
        if(prop == 'cartridges'){
            genJsplumbCartridge(groupJSON[prop], divRoot, groupJSON.name)
        }
        if(prop == 'groups'){
            genJsplumbGroups(groupJSON[prop], divRoot, groupJSON.name)
        }
    }

    function genJsplumbCartridge(item, currentParent, parentName){
        for (var i = 0; i < item.length; i++) {
            var id = item[i];
            var divCartridge = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[i],'data-type':'cartridge','data-ctype':item[i]} )
                .addClass('input-false')
                .attr('data-toggle', 'tooltip')
                .attr('title',item[i])
                .addClass('stepnode')
                .appendTo('#whiteboard');
            $(divCartridge).append('<span>'+item[i]+'</span>');
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
                .addClass('stepnode')
                .attr('data-toggle', 'tooltip')
                .attr('title',item[prop]['name'])
                .addClass('input-false')
                .appendTo('#whiteboard');
            $(divGroup).append('<span>'+item[prop]['name']+'</span>');
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
//use to activate tab
function activateTab(tab){
    $('.nav-tabs a[href="#' + tab + '"]').tab('show');
};
//generate treefor Groups
function generateGroupTree(groupJSON){

    var rawout = [];
    //create initial node for tree
    var rootnode ={};
    rootnode.name = groupJSON.name;
    rootnode.parent = null;
    rootnode.type = 'groups';
    rawout.push(rootnode);

    for (var prop in groupJSON) {
        if(prop == 'cartridges'){
            getCartridges(groupJSON[prop],rawout, rootnode.name)
        }
        if(prop == 'groups'){
            getGroups(groupJSON[prop], rawout, rootnode.name)
        }
    }

    function getCartridges(item, collector, parent){
        for (var i = 0; i < item.length; i++) {
            var type = 'cartridges';
            var cur_name = item[i];
            collector.push({"name": cur_name, "parent": parent, "type": type});
        }
    }

    function getGroups(item, collector, parent){
        for (var prop in item) {
            var cur_name = item[prop]['name'];
            var type = 'groups';
            collector.push({"name": cur_name, "parent": parent, "type": type});
            if(item[prop].hasOwnProperty('cartridges')) {
                getCartridges(item[prop].cartridges, collector, cur_name);
            }
            if(item[prop].hasOwnProperty('groups')) {
                getGroups(item[prop].groups, collector, cur_name)
            }
        }
    }

    return rawout;

}

// ************** Generate the tree diagram	 *****************
function generateGroupPreview(data) {
    //clean current graph and text
    $(".description-section").html('');

    //mapping data
    var dataMap = data.reduce(function(map, node) {
        map[node.name] = node;
        return map;
    }, {});
    var treeData = [];
    data.forEach(function(node) {
        // add to parent
        var parent = dataMap[node.parent];
        if (parent) {
            // create child array if it doesn't exist
            (parent.children || (parent.children = []))
                // add node to child array
                .push(node);
        } else {
            // parent is null or missing
            treeData.push(node);
        }
    });

    var source = treeData[0];

//generate position for tree view
    var margin = {top: 25, right: 5, bottom: 5, left: 5},
        width = 320 - margin.right - margin.left,
        height = 500 - margin.top - margin.bottom;

    var i = 0;

    var tree = d3.layout.tree()
        .size([height, width]);

    var diagonal = d3.svg.diagonal()
        .projection(function(d) { return [d.x, d.y]; });

    function redraw() {
        svg.attr("transform",
                "translate(" + d3.event.translate + ")"
                + " scale(" + d3.event.scale + ")");
    }

    var svg = d3.select(".description-section").append("svg")
        .attr("width", width)
        .attr("height", height)
        .call(d3.behavior.zoom().on("zoom", redraw))
        .append("g")
        .attr("transform", "translate(" + -90+ "," + margin.top + ")");

    // Compute the new tree layout.
    var nodes = tree.nodes(source).reverse(),
        links = tree.links(nodes);

    // Normalize for fixed-depth.
    nodes.forEach(function(d) { d.y = d.depth * 60; });

    // Declare the nodesâ€¦
    var node = svg.selectAll("g.node")
        .data(nodes, function(d) { return d.id || (d.id = ++i); });

    // Enter the nodes.
    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function(d) {
            return "translate(" + d.x + "," + d.y + ")"; });

    nodeEnter.append("rect")
        .attr("x", -10)
        .attr("y", -10)
        .attr("rx",2)
        .attr("ry",2)
        .attr("width", 20)
        .attr("height", 20)
        .attr("stroke-width", 1)
        .attr("stroke", "silver")
        .style("fill", "#fff");

    nodeEnter.append("text")
        .attr("y", function(d) {
            return d.children || d._children ? -20 : 20; })
        .attr("dy", ".35em")
        .attr("text-anchor", "middle")
        .text(function(d) { return d.name; })
        .style("fill-opacity", 1);

    // Declare the links
    var link = svg.selectAll("path.link")
        .data(links, function(d) { return d.target.id; });

    // Enter the links.
    link.enter().insert("path", "g")
        .attr("class", "link")
        .attr("d", diagonal);

}

var applicationJson = {};
//Definition JSON builder
function generateJsplumbTree(collector, connections, appeditor){

    collector = appeditor.getValue();
    collector['components']={};
    collector['components']['groups']=[];
    collector['components']['cartridges']=[];
    collector['components']['dependencies']=appeditor['dependencies'];
    delete collector['dependencies'];

    console.log(collector)

    //generate raw data tree from connections
    var rawtree = [];
    $.each(jsPlumb.getConnections(), function (idx, connection) {
        var dataType = $('#'+connection.targetId).attr('data-type');
        var jsonContent = JSON.parse(decodeURIComponent($('#'+connection.targetId).attr('data-generated')));
        rawtree.push({
            parent: connection.sourceId,
            content: jsonContent,
            dtype:dataType,
            id: connection.targetId
        });
    });

    //generate heirache by adding json and extra info
    var nodes = [];
    var toplevelNodes = [];
    var lookupList = {};

    for (var i = 0; i < rawtree.length; i++) {
        var n = rawtree[i].content;
        if(rawtree[i].dtype == 'cartridge'){
            n.id =  rawtree[i].id;
            n.parent_id = ((rawtree[i].parent == 'applicationId') ? 'applicationId': rawtree[i].parent);
            n.dtype =rawtree[i].dtype;
        }else if(rawtree[i].dtype == 'group'){
            n.id =  rawtree[i].id;
            n.parent_id = ((rawtree[i].parent == 'applicationId') ? 'applicationId': rawtree[i].parent);
            n.dtype =rawtree[i].dtype;
            n.groups = [];
            n.cartridges =[];
        }

        lookupList[n.id] = n;
        nodes.push(n);

        if (n.parent_id == 'applicationId' && rawtree[i].dtype == 'cartridge') {
            collector['components']['cartridges'].push(n);
        }else if(n.parent_id == 'applicationId' && rawtree[i].dtype == 'group'){
            collector['components']['groups'].push(n);
        }

    }

    //merge any root level stuffs
    for (var i = 0; i < nodes.length; i++) {
        var n = nodes[i];
        if (!(n.parent_id == 'applicationId') && n.dtype == 'cartridge') {
            lookupList[n.parent_id]['cartridges'] = lookupList[n.parent_id]['cartridges'].concat([n]);
        }else if(!(n.parent_id == 'applicationId') && n.dtype == 'group'){
            lookupList[n.parent_id]['groups'] = lookupList[n.parent_id]['groups'].concat([n]);
        }
    }

    //cleanup JSON, remove extra items added to object level
    function traverse(o) {
        for (var i in o) {
            if(i == 'id' || i == 'parent_id' || i == 'dtype'){
                delete o[i];
            }else if(i == 'groups' && o[i].length == 0){
                delete o[i];
            }
            if (o[i] !== null && typeof(o[i])=="object") {
                //going on step down in the object tree!!
                traverse(o[i]);
            }
        }
    }

    traverse(collector);

    return collector;
}

//UUID generator
function uuid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

//setting up schema and defaults
var cartridgeBlockTemplate = {
    "type":"object",
    "$schema": "http://json-schema.org/draft-03/schema",
    "id": "root",
    "format":"grid",
    "properties":{
        "type": {
            "type":"string",
            "id": "root/type",
            "default": "name",
            "title": "Cartridge Type: ",
            "required":false
        },
        "cartridgeMax": {
            "type":"number",
            "id": "root/cartridgeMax",
            "default":2,
            "title":"Cartridge Max",
            "required":false
        },
        "cartridgeMin": {
            "type":"number",
            "id": "root/cartridgeMin",
            "title":"Cartridge Min",
            "default":1,
            "required":false
        },
        "subscribableInfo": {
            "type":"object",
            "id": "root/subscribableInfo",
            "title":"Subscribable Info: ",
            "required":false,
            "properties":{
                "alias": {
                    "type":"string",
                    "id": "root/subscribableInfo/alias",
                    "default": "alias2",
                    "title":"Alias: ",
                    "required":false
                },
                "autoscalingPolicy": {
                    "type":"string",
                    "id": "root/subscribableInfo/autoscalingPolicy",
                    "default": "autoscale_policy_1",
                    "title":"Auto-scaling Policy: ",
                    "enum": [],
                    "required":false
                },
                "deploymentPolicy": {
                    "type":"string",
                        "id": "root/subscribableInfo/deploymentPolicy",
                        "default": "deployment_policy_1",
                        "title":"Deployment Policy: ",
                        "enum": [],
                        "required":false
                },
                "artifactRepository": {
                    "id": "root/subscribableInfo/artifactRepository",
                    "type": "object",
                    "properties": {
                        "privateRepo": {
                            "id": "root/subscribableInfo/artifactRepository/privateRepo",
                            "title":"Private Repository: ",
                            "type": "boolean"
                        },
                        "repoUrl": {
                            "id": "root/subscribableInfo/artifactRepository/repoUrl",
                            "title":"Repository URL: ",
                            "type": "string"
                        },
                        "repoUsername": {
                            "id": "root/subscribableInfo/artifactRepository/repoUsername",
                            "title":"Repository Username: ",
                            "type": "string"
                        },
                        "repoPassword": {
                            "id": "root/subscribableInfo/artifactRepository/repoPassword",
                            "title":"Repository Password: ",
                            "type": "string",
                            "format":"password"
                        }
                    }
                }
            }
        }
    }
};

var cartridgeBlockDefault = {
    "type":"tomcat",
    "cartridgeMin":1,
    "cartridgeMax":2,
    "subscribableInfo":{
        "alias":"alias2",
        "autoscalingPolicy":"",
        "deploymentPolicy":"",
        "artifactRepository":{
            "privateRepo":"true",
            "repoUrl":"http://xxx:10080/git/default.git",
            "repoUsername":"user",
            "repoPassword":"password",
        }

    }
};

var groupBlockTemplate = {
    "type":"object",
    "$schema": "http://json-schema.org/draft-03/schema",
    "id": "root",
    "required":false,
    "properties":{
        "name": {
            "type":"string",
            "id": "root/name",
            "default": "name",
            "required":false
        },
        "alias": {
            "type":"string",
            "id": "root/alias",
            "default": "alias",
            "required":false
        },
        "groupMaxInstances": {
            "type":"number",
            "id": "root/groupMaxInstances",
            "default":2,
            "required":false
        },
        "groupMinInstances": {
            "type":"number",
            "id": "root/groupMinInstances",
            "default":1,
            "required":false
        },
        "groupScalingEnabled": {
            "type":"boolean",
            "id": "root/groupScalingEnabled",
            "default": "false",
            "required":false
        }
    }
};

var groupBlockDefault = {
    "name":"group2",
    "alias":"group2alias",
    "groupMinInstances":1,
    "groupMaxInstances":2,
    "groupScalingEnabled":"false"
};

var applicationBlockTemplate = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "id": "root",
    "type": "object",
    "title":" ",
    "options":{
        "disable_properties":true,
        "disable_collapse": true
    },
    "properties": {
        "applicationId": {
            "id": "root/applicationId",
            "title": "Application Id",
            "name": "Application Id",
            "type": "string"
        },
        "alias": {
            "id": "root/alias",
            "type": "string",
            "title": "Application Alias",
            "name": "Application Alias"
        },
        "multiTenant": {
            "id": "root/multiTenant",
            "type": "boolean",
            "title": "Application Multi Tenancy",
            "name": "Application Multi Tenancy"
        },
        "dependencies": {
            "id": "root/dependencies",
            "type": "object",
            "title": "Dependencies",
            "name": "Dependencies",
            "options": {
                "hidden": false,
                "disable_properties":true,
                "collapsed": true
            },
            "properties": {
                "startupOrders": {
                    "id": "root/dependencies/startupOrders",
                    "type": "array",
                    "title": "Startup Orders",
                    "name": "Startup Orders",
                    "format":"tabs",
                    "items": {
                        "id": "root/dependencies/startupOrders/0",
                        "type": "string",
                        "title": "Order",
                        "name": "Order"
                    }
                },
                "scalingDependents": {
                    "id": "root/dependencies/scalingDependents",
                    "type": "array",
                    "title": "Scaling Dependents",
                    "name": "Scaling Dependents",
                    "format":"tabs",
                    "items": {
                        "id": "root/dependencies/scalingDependents/0",
                        "type": "string",
                        "title": "Dependent",
                        "name": "Dependent"
                    }
                },
                "terminationBehaviour": {
                    "id": "root/dependencies/terminationBehaviour",
                    "type": "string",
                    "title": "Termination Behaviour",
                    "name": "Termination Behaviour",
                    "enum": ["terminate-none","terminate-dependents","terminate-all"],
                }
            }
        }
    }
};

var applicationBlockDefault = {
    "applicationId": "",
    "alias": "",
    "multiTenant": false,
    "dependencies": {
        "startupOrders": [
            "group.my-group1,cartridge.my-c4"
        ],
        "scalingDependents": [
            "group.my-group1,cartridge.my-c4"
        ],
        "terminationBehaviour": "terminate-dependents"
    }
};

//create cartridge list
var cartridgeListHtml='';
function generateCartridges(data){
    if(data.length == 0){
        cartridgeListHtml = 'No Cartridges found..';
    }else{
        for(var cartridge in data){
            var cartridgeData = data[cartridge];
            cartridgeListHtml += '<div class="block-cartridge" ' +
                'data-info="'+cartridgeData.description+ '"'+
                'data-toggle="tooltip" data-placement="bottom" title="Single Click to view details. Double click to add"'+
                'id="'+cartridgeData.type+'">'
                + cartridgeData.displayName+
                '</div>'
        }
    }

    //append cartridge into html content
    $('#cartridge-list').append(cartridgeListHtml);
}

//create group list
var groupListHtml='';
function generateGroups(data){
    if(data.length == 0){
        groupListHtml = 'No Groups found..';
    }else {
        for (var group in data) {
            var groupData = data[group];
            groupListHtml += '<div class="block-group" ' +
                ' data-info="' + encodeURIComponent(JSON.stringify(groupData)) + '"' +
                'data-toggle="tooltip" data-placement="bottom" title="'+groupData.name+'"'+
                'id="' + groupData.name + '">'
                + groupData.name +
                '</div>'
        }
    }
    //append cartridge into html content
    $('#group-list').append(groupListHtml);
}

//node positioning algo with dagre js
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

// Document ready events
$(document).ready(function(){

    $('#deploy').attr('disabled','disabled');

    $('#deploy').on('click', function(){
        var appJSON = generateJsplumbTree(applicationJson, jsPlumb.getConnections(), appeditor);
        var btn = $(this);
        var formtype = 'applications';
        btn.html("<i class='fa fa-spinner fa-spin'></i> Adding...");
        $.ajax({
            type: "POST",
            url: caramel.context + "/controllers/applications/application_requests.jag",
            dataType: 'json',
            data: { "formPayload": JSON.stringify(appJSON), "formtype": formtype },
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
                btn.html('Add New Application Definition');
            });

    });

    //*******************Adding JSON editor *************//
    JSONEditor.defaults.theme = 'bootstrap3';
    JSONEditor.defaults.iconlib = 'fontawesome4';
    JSONEditor.defaults.show_errors = "always";
    var editor, blockId, appeditor;

    //set hidden UUID
    applicationBlockDefault.applicationId = uuid();

    // Initialize the editor for main section
    appeditor = new JSONEditor(document.getElementById('general'), {
        ajax: false,
        disable_edit_json: true,
        schema: applicationBlockTemplate,
        format: "grid",
        startval: applicationBlockDefault
    });


    DragEl(".stepnode");
    Repaint();

    $('#whiteboard').on('click', '.stepnode', function(){
        tabData($(this));
    });

    $('#whiteboard').on('dblclick', '.stepnode', function(){
        var target = $('#component-data');
        if( target.length ) {
            event.preventDefault();
            $('html, body').animate({
                scrollTop: target.offset().top
            }, 1000);
        }
    });

    function tabData(node){
        //get tab activated
        if(node.attr('id') == 'applicationId'){
            activateTab('general');
        }else{
            activateTab('components');
            $('#component-info-update').prop("disabled", false);
        }

        blockId = node.attr('id');
        var blockType = node.attr('data-type');
        var startval;
        var ctype = node.attr('data-ctype');
        if(blockType == 'cartridge' || blockType == 'group-cartridge'){
            startval = cartridgeBlockDefault;
            startval['type'] = ctype;
            //get list of autosacles
            var policies = editorAutoscalePolicies;
            var policiesEnum = [];
            for(var i=0; i<policies.length; i++){
                policiesEnum.push(policies[i].id);
            }
            cartridgeBlockTemplate['properties']['subscribableInfo']['properties']['autoscalingPolicy']['enum']
                =policiesEnum;
            //get list of deploymentpolicies
            var dpolicies = editorDeploymentPolicies;
            var dpoliciesEnum = [];
            for(var i=0; i<dpolicies.length; i++){
                dpoliciesEnum.push(dpolicies[i].id);
            }
            cartridgeBlockTemplate['properties']['subscribableInfo']['properties']['deploymentPolicy']['enum']
                =dpoliciesEnum;

        }else{
            startval = groupBlockDefault;
            startval['name'] = ctype;
        }

        if(node.attr('data-generated')) {
            startval = JSON.parse(decodeURIComponent(node.attr('data-generated')));
        }
        $('#component-data').html('');

        switch (blockType){
            case 'cartridge':
                generateHtmlBlock(cartridgeBlockTemplate, startval);
                break;

            case 'group':
                generateHtmlBlock(groupBlockTemplate, startval);
                break;

            case 'group-cartridge':
                generateHtmlBlock(cartridgeBlockTemplate, startval);
                break;
        }
    }

    function generateHtmlBlock(schema, startval){
        // Initialize the editor
        editor = new JSONEditor(document.getElementById('component-data'), {
            ajax: false,
            disable_edit_json: true,
            schema: schema,
            format: "grid",
            startval: startval
        });
        if(editor.getEditor('root.type')){
            editor.getEditor('root.type').disable();
        }else{
            editor.getEditor('root.name').disable();
        }

    }

    //get component JSON data
    $('#component-info-update').on('click', function(){
        $('#'+blockId).attr('data-generated', encodeURIComponent(JSON.stringify(editor.getValue())));
        $('#'+blockId).removeClass('input-false');
        $('#'+blockId).find('div>i').removeClass('fa-exclamation-circle').addClass('fa-check-circle-o').css('color','#2ecc71');
        $('#deploy').prop("disabled", false);
    });

    //get create cartridge list
    generateCartridges(cartridgeList);
    //get group JSON
    generateGroups(groupList);

    //handle single click for cartridge
    $('#cartridge-list').on('click', ".block-cartridge", function(){
        $('.description-section').html($(this).attr('data-info'));
    });
    //handle double click for cartridge
    $('#cartridge-list').on('dblclick', ".block-cartridge", function(){
        addJsplumbCartridge($(this).attr('id'),cartridgeCounter);
        //reposition after cartridge add
        dagrePosition();
        //increase global count for instances
        cartridgeCounter++;
    });

    //handle single click for groups
    $('#group-list').on('click', ".block-group", function(){
        var groupJSON = JSON.parse(decodeURIComponent($(this).attr('data-info')));
        mydata = generateGroupTree(groupJSON);
        generateGroupPreview(mydata);


    });
    //handle double click event for groups
    $('#group-list').on('dblclick', ".block-group", function(){
        var groupJSON = JSON.parse(decodeURIComponent($(this).attr('data-info')));
        addJsplumbGroup(groupJSON,cartridgeCounter);
        //reposition after group add
        dagrePosition();
        //increase global count for instances
        cartridgeCounter++;
    });

    //reposition on click event on editor
    $('.reposition').on('click', function(){
        dagrePosition();
    });

    //genrate context menu for nodes
    $.contextMenu({
        selector: '.stepnode',
        callback: function(key, options) {
            var m = "clicked: " + key + $(this);
            if(key == 'delete'){
                deleteNode($(this));
            }else if(key == 'edit'){
                document.getElementById('component-data').scrollIntoView();
                tabData($(this));
            }
        },
        items: {
            "edit": {name: "Edit", icon: "edit"},
            "delete": {name: "Delete", icon: "delete"}
        }
    });

});

//bootstrap tooltip added
$(function () {
    $('[data-toggle="tooltip"]').tooltip()
})


// ************* Add context menu for nodes ******************
//remove nodes from workarea
function deleteNode(endPoint){
    if(endPoint.attr('id') != 'applicationId'){
        var allnodes = $(".stepnode");
        var superParent = endPoint.attr('id').split("-")[0]+endPoint.attr('id').split("-")[1];
        var nodeName = endPoint.attr('data-ctype');
        var nodeType = endPoint.attr('data-type');
        var notyText = '';

        if(nodeType == 'group'){
            notyText = 'This will remove related nodes from the Editor. Are you sure you want to delete '
                            +nodeType + ': '+nodeName+'?';
        }else{
            notyText = 'Are you sure you want to delete '+nodeType + ': '+nodeName+'?';
        }
        noty({
            layout: 'bottomRight',
            type: 'warning',
            text:  notyText,
            buttons: [
                {addClass: 'btn btn-primary', text: 'Yes', onClick: function($noty) {
                    $noty.close();

                    allnodes.each(function(){
                        var currentId = $(this).attr('id').split("-")[0]+$(this).attr('id').split("-")[1];
                        if(currentId == superParent){
                            var that=$(this);      //get all of your DIV tags having endpoints
                            for (var i=0;i<that.length;i++) {
                                var endpoints = jsPlumb.getEndpoints($(that[i])); //get all endpoints of that DIV
                                if(endpoints){
                                    for (var m=0;m<endpoints.length;m++) {
                                        // if(endpoints[m].anchor.type=="TopCenter") //Endpoint on right side
                                        jsPlumb.deleteEndpoint(endpoints[m]);  //remove endpoint
                                    }
                                }

                            }
                            jsPlumb.detachAllConnections($(this));
                            $(this).remove();
                        }

                    });

                    //clear html area
                    $('#component-data').html('');
                    activateTab('general');
                }
                },
                {addClass: 'btn btn-danger', text: 'No', onClick: function($noty) {
                    $noty.close();
                }
                }
            ]
        });



    }else{
        var n = noty({text: 'Sorry you can\'t remove application node' , layout: 'bottomRight', type: 'warning'});
    }

}

