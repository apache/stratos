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
    jsPlumb.addEndpoint('group-base', {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);
});

var cartridgeCounter=0;
//add cartridge to editor
function addJsplumbCartridge(idname, cartridgeCounter) {

    var Div = $('<div>').attr({'id':cartridgeCounter+'-'+idname, 'data-type':'cartridge', 'data-ctype':idname } )
        .attr('data-toggle', 'tooltip')
        .attr('title',idname)
        .appendTo('#whiteboard');
    $(Div).append('<span>'+idname+'</span>');
    $(Div).addClass('stepnode');
    jsPlumb.addEndpoint($(Div), {
        anchor: "TopCenter"
    }, endpointOptions);
    DragEl($(Div));
    Repaint();
}

//add group to editor
function addJsplumbGroup(cartridgeCounter) {

    var div = $('<div>').attr({'id':cartridgeCounter+'-group','data-type':'group','data-ctype':''})
        .addClass('input-false')
        .addClass('stepnode')
        .attr('data-toggle', 'tooltip')
        .attr('title','Group')
        .appendTo('#whiteboard');
    $(div).append('<span>Group</span>');
    $(div).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
    jsPlumb.addEndpoint($(div), {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);

    jsPlumb.addEndpoint($(div), {
        anchor: "TopCenter"
    }, groupOptions);
    DragEl($(div));
    Repaint();
}

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

function genGroupJSON(collector, connections){
    //generate raw data tree from connections
    var collector = collector || {};

    var rawtree = [];

    //get base-node data
    var groupBase = $('#group-base').attr('data-generated');
    if(groupBase){
        var groupBaseJSON =  JSON.parse(decodeURIComponent(groupBase));
        collector = groupBaseJSON;
    }
    //create empty nodes for group and cartridges
    collector['groups']=[];
    collector['cartridges']=[];

    $.each(connections, function (idx, connection) {
        var dataType = $('#'+connection.targetId).attr('data-type');
        var dataGenerated  = $('#'+connection.targetId).attr('data-generated');
        var jsonContent;
        if(dataGenerated != undefined){
            var jsonContent = JSON.parse(decodeURIComponent(dataGenerated));
        }else{

            var jsonContent = {cname: $('#'+connection.targetId).attr('data-ctype')};
        }
        rawtree.push({
            parent: connection.sourceId,
            content: jsonContent,
            dtype: dataType,
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
            n.parent_id = ((rawtree[i].parent == 'group-base') ? 'group-base': rawtree[i].parent);
            n.dtype =rawtree[i].dtype;
        }else if(rawtree[i].dtype == 'group'){
            n.id =  rawtree[i].id;
            n.parent_id = ((rawtree[i].parent == 'group-base') ? 'group-base': rawtree[i].parent);
            n.dtype =rawtree[i].dtype;
            n.groups = [];
            n.cartridges =[];
        }

        lookupList[n.id] = n;
        nodes.push(n);

        if (n.parent_id == 'group-base' && rawtree[i].dtype == 'cartridge') {
            collector['cartridges'].push(n.cname);
        }else if(n.parent_id == 'group-base' && rawtree[i].dtype == 'group'){
            collector['groups'].push(n);
        }

    }

    //merge any root level stuffs
    for (var i = 0; i < nodes.length; i++) {
        var n = nodes[i];
        if (!(n.parent_id == 'group-base') && n.dtype == 'cartridge') {
            lookupList[n.parent_id]['cartridges'] = lookupList[n.parent_id]['cartridges'].concat([n.cname]);
        }else if(!(n.parent_id == 'group-base') && n.dtype == 'group'){
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


var groupBlockTemplate = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "id": "root",
    "title":" ",
    "type": "object",
    "options": {
        "disable_properties": true,
        "disable_collapse": true
    },
    "properties": {
        "name": {
            "id": "root/name",
            "type": "string",
            "title": "Group Name: "
        },
        "groupScalingEnabled": {
            "id": "root/groupScalingEnabled",
            "type": "boolean",
            "title": "Group Scaling Enabled: "
        },
        "dependencies": {
            "id": "root/dependencies",
            "type": "object",
            "title": "Dependencies: ",
            "options": {
                "disable_properties": true
            },
            "properties": {
                "startupOrders": {
                    "id": "root/dependencies/startupOrders",
                    "type": "array",
                    "title": "Startup Orders: ",
                    "items": {
                        "id": "root/dependencies/startupOrders/0",
                        "type": "string"
                    }
                },
                "scalingDependants": {
                    "id": "root/dependencies/scalingDependants",
                    "type": "array",
                    "title": "Scaling Dependants: ",
                    "items": {
                        "id": "root/dependencies/scalingDependants/0",
                        "type": "string"
                    }
                },
                "terminationBehaviour": {
                    "id": "root/dependencies/terminationBehaviour",
                    "type": "string",
                    "title": "Termination Behavior: ",
                    "enum": ["terminate-none","terminate-dependents","terminate-all"]
                },
                "required": [
                    "startupOrders",
                    "scalingDependants",
                    "terminationBehaviour"
                ]
            }
        },
        "required": [
            "name",
            "groupScalingEnabled",
            "dependencies"
        ]
    }
};

var groupBlockDefault = {
    "name": "",
    "groupScalingEnabled": "true",
    "dependencies": {
        "startupOrders": [
            "cartridge.type, group.name"
        ],
        "scalingDependants": [
            "cartridge.type, group.name"
        ],
        "terminationBehaviour": "terminate-all"
    }
};

// Document ready events
$(document).ready(function(){

    $('#deploy').on('click', function(){
        var  payload = genGroupJSON({}, jsPlumb.getConnections());
        var btn = $(this);
        var formtype = 'groups';
        btn.html("<i class='fa fa-spinner fa-spin'></i> Adding...");
        $.ajax({
            type: "POST",
            url: caramel.context + "/controllers/configure/configure_requests.jag",
            dataType: 'json',
            data: { "formPayload": JSON.stringify(payload), "formtype": formtype },
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
                btn.html('Add Cartridge Group Definition');
            });

    });

    //*******************Adding JSON editor *************//
    JSONEditor.defaults.theme = 'bootstrap3';
    JSONEditor.defaults.iconlib = 'fontawesome4';
    JSONEditor.defaults.show_errors = "always";
    var editor, blockId = 'group-base';

    // Initialize the editor
    editor = new JSONEditor(document.getElementById('general-data'), {
        ajax: false,
        disable_edit_json: true,
        schema: groupBlockTemplate,
        format: "grid",
        startval: groupBlockDefault
    });

    DragEl(".stepnode");
    Repaint();

    function tabData(node){

        blockId = node.attr('id');
        var blockType = node.attr('data-type');
        var startval;
        if(blockType == 'group'){
            if(node.attr('data-generated')) {
                startval = JSON.parse(decodeURIComponent(node.attr('data-generated')));
            }else{
                startval = groupBlockDefault;
            }

            $('#general-data').html('');

            // Initialize the editor
            editor = new JSONEditor(document.getElementById('general-data'), {
                ajax: false,
                disable_edit_json: true,
                schema: groupBlockTemplate,
                format: "grid",
                startval: startval
            });
        }

    }

    $('#whiteboard').on('click', '.stepnode', function(){
        tabData($(this))
    });

    $('#whiteboard').on('dblclick', '.stepnode', function(){
        var target = $('#group-data-scroll');
        if( target.length ) {
            event.preventDefault();
            $('html, body').animate({
                scrollTop: target.offset().top - 140
            }, 1000);
        }
    });

    //get component JSON data
    $('#component-info-update').on('click', function(){
        $('#'+blockId).attr('data-generated', encodeURIComponent(JSON.stringify(editor.getValue())));
        $('#'+blockId).removeClass('input-false');
        $('#'+blockId).find('div>i').removeClass('fa-exclamation-circle').addClass('fa-check-circle-o').css('color','#2ecc71');
        if(blockId == 'group-base'){
            $('#deploy').prop("disabled", false);
        }

    });

    //get create cartridge list
    generateCartridges(cartridgeList);


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

    });
    //handle double click event for groups
    $('#group-list').on('dblclick', ".block-group", function(){

        addJsplumbGroup(cartridgeCounter);
        //reposition after group add
        dagrePosition();
        //increase global count for instances
        cartridgeCounter++;
    });

    //reposition on click event on editor
    $('.reposition').on('click', function(){
        dagrePosition();
    });

});

//bootstrap tooltip added
$(function () {
    $('[data-toggle="tooltip"]').tooltip();
})
