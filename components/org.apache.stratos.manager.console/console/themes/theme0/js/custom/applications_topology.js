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
 
//create JSON from topology
function genTree(data){
    var rawout = [];

    var rootnode ={};
    rootnode.name = data.id;
    rootnode.parent = null;
    rootnode.status = data.status;
    //create initial root node
    rawout.push(rootnode);

    //application instances
    function applicationInstances(items, collector, parent){
        for(var prop in items){
            if (items.hasOwnProperty(prop)) {
                var cur_name = items[prop].instanceId,
                    status = items[prop].status,
                    type = 'applicationInstances';
                rawout.push({"name": cur_name, "parent": parent, "type": type, "status": status});

                clusterInstances(items[prop].clusterInstances, collector, cur_name);
                groupInstances(items[prop].groupInstances, collector, cur_name)
            }
        }
    }

    function clusterInstances(items, collector, parent){
        for(var prop in items){
            if (items.hasOwnProperty(prop)) {
                var cur_name = items[prop].clusterId + items[prop].instanceId,
                    alias = items[prop].alias,
                    hostNames = items[prop].hostNames.toString(),
                    serviceName = items[prop].serviceName,
                    status = items[prop].status;

                if(items[prop].accessUrls){
                    accessUrls = items[prop].accessUrls;
                }else{
                    accessUrls = '';
                }
                var type = 'clusters';
                rawout.push({"name": cur_name, "parent": parent, "type": type, "status": status,
                    "alias":alias, "hostNames": hostNames, "serviceName": serviceName,
                    "accessUrls":accessUrls
                });
                clustermembers(items[prop].member, collector, cur_name)
            }
        }
    }

    function groupInstances(items, collector, parent){
        for(var prop in items){
            if (items.hasOwnProperty(prop)) {
                var cur_name = items[prop].groupId + items[prop].instanceId,
                    instanceId = items[prop].instanceId,
                    groupId = items[prop].groupId,
                    status = items[prop].status;
                var type = 'groups';
                rawout.push({"name": cur_name, "parent": parent, "type": type, "status": status,
                    "groupId":groupId, "instanceId":instanceId
                });

                clusterInstances(items[prop].clusterInstances, collector, cur_name);
                if(items[prop].hasOwnProperty('groupInstances')){
                    groupInstances(items[prop].groupInstances, collector, cur_name)
                }

            }
        }
    }

    function clustermembers(items, collector, parent){
        for(var prop in items){
            if (items.hasOwnProperty(prop)) {
                var cur_name = items[prop].memberId,
                    defaultPrivateIP = items[prop].defaultPrivateIP,
                    defaultPublicIP = items[prop].defaultPublicIP,
                    ports = items[prop].ports,
                    networkPartitionId = items[prop].networkPartitionId,
                    partitionId = items[prop].partitionId,
                    status = items[prop].status;
                var type = 'members';
                rawout.push({"name": cur_name, "parent": parent, "type": type, "status": status,
                    "defaultPrivateIP":defaultPrivateIP, "defaultPublicIP":defaultPublicIP,"ports":ports,
                    "networkPartitionId":networkPartitionId, "partitionId":partitionId
                });
            }
        }
    }

    //getting execution logic
    applicationInstances(data.applicationInstances, rawout, data.id);

    //generate tree from raw data
    var data = rawout;
    //data mapping with d3js tree
    var dataMap = data.reduce(function (map, node) {
        map[node.name] = node;
        return map;
    }, {});
    var treeData = [];
    data.forEach(function (node) {
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

    return treeData[0];
}

function update(source) {

    // ************** Generate the tree diagram	 *****************
    var margin = {top: 80, right: 120, bottom: 20, left: 120},
        width = 900 - margin.right - margin.left,
        height = 900 - margin.top - margin.bottom;

    var i = 0;

    var tree = d3.layout.tree()
        .separation(function(a, b) { return ((a.parent == source) && (b.parent == source)) ? 5 : 4; })
        .size([height+100, width]);

    var diagonal = d3.svg.diagonal()
        .projection(function (d) {
            return [d.x, d.y];
        });
    function redraw() {
        svg.attr("transform",
                "translate(" + d3.event.translate + ")"
                + " scale(" + d3.event.scale + ")");
    }

    var svg = d3.select(".application-topology").append("svg")
        .attr("width", width + margin.right + margin.left)
        .attr("height", height + margin.top + margin.bottom)
        .call(d3.behavior.zoom().on("zoom", redraw))
        .append("g");

    var i = 0;
    duration = 750;

    // Compute the new tree layout.
    var nodes = tree.nodes(source).reverse(),
        links = tree.links(nodes);

    // Normalize for fixed-depth.
    nodes.forEach(function (d) {
        d.y = d.depth * 100 + 80;
    });

    // Declare the nodes…
    var node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id || (d.id = ++i);
        });

    // Enter the nodes.
    var div_html;
    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.x + "," + d.y + ")";
        })
        .attr('data-content', function (d) {
            if (d.type == 'clusters') {
                if(d.accessUrls != ''){
                    var accessURLHTML = "<strong>Access URLs: </strong>";
                    for(var i=0;i<d.accessUrls.length;i++){
                        accessURLHTML +=  "<a href='"+ d.accessUrls[i] +"' target='_blank'>"+ d.accessUrls[i] +
                            "</a><br/>" ;
                    }

                }else{
                    var accessURLHTML ='';
                }
                div_html = "<strong>Cluster Id: </strong>" + d.name + "<br/>" +
                            "<strong>Cluster Alias: </strong>" + d.alias + "<br/>" +
                            accessURLHTML +
                            "<strong>HostNames: </strong>" + d.hostNames + "<br/>" +
                            "<strong>Service Name: </strong>" + d.serviceName + "<br/>" +
                            "<strong>Status: </strong>" + d.status;

            } else if (d.type == 'members') {
                if((typeof d.ports != 'undefined') && (d.ports.length > 0)) {
                    var portsHTML = "<strong>Ports: </strong></br>";
                    for(var i=0;i<d.ports.length;i++){
                        portsHTML += "Port: " + d.ports[i].port + ", Protocol: " + d.ports[i].protocol;
                        if(i < (d.ports.length - 1)) {
                            portsHTML += "</br>";
                        }
                    }
                    portsHTML += "</br>"

                } else{
                    var portsHTML ='';
                }
                div_html = "<strong>Member Id: </strong>" + d.name + "<br/>" +
                        "<strong>Default Private IP: </strong>" + d.defaultPrivateIP + "<br/>" +
                        "<strong>Default Public IP: </strong>" + d.defaultPublicIP + "<br/>" +
                        portsHTML +
                        "<strong>Network Partition Id: </strong>" + d.networkPartitionId + "<br/>" +
                        "<strong>Partition Id: </strong>" + d.partitionId + "<br/>" +
                        "<strong>Status: </strong>" + d.status;
            } else if (d.type == 'groups') {

                div_html = "<strong>Group Instance Id: </strong>" + d.instanceId + "<br/>" +
                        "<strong>Status: </strong>" + d.status;

            } else if (d.type == 'applicationInstances') {
                div_html = "<strong>Instance Id: </strong>" + d.name + "<br/>" +
                        "<strong>Status: </strong>" + d.status;

            } else {
                div_html = "<strong>Alias: </strong>" + d.name + "<br/>"+
                        "<strong>Status: </strong>" + d.status;

            }
           return div_html;
        });
        // add popover on nodes
    nodeEnter.append("rect")
        .attr("x", -15)
        .attr("y", -15)
        .attr("width", 30)
        .attr("height", 30)
        .style("fill", function (d) {
            if (d.status == 'Active' || d.status == 'Activated') {
                return "#1abc9c";
            } else if (d.status == 'Created') {
                return "#e67e22";
            } else if (d.status == 'Inactive') {
                return "#7f8c8d";
            } else if (d.status == 'Terminated') {
                return "#c0392b";
            } else if (d.status == 'Terminating') {
                return "#c0392b";
            }else{
                return "#CCC";
            }
        });

    nodeEnter.append("image")
        .attr("xlink:href",
        function (d) {
            if (d.type == 'clusters') {
                return "../../../themes/theme0/images/topology/cluster.png";
            } else if (d.type == 'groups') {
                return "../../../themes/theme0/images/topology/group.png";
            } else if (d.type == 'members') {
                return "../../../themes/theme0/images/topology/member.png";
            } else {
                return "../../../themes/theme0/images/topology/application.png";
            }
        })
        .attr("class", "created")
        .attr("x", -16)
        .attr("y", -16)
        .attr("width", 32)
        .attr("height", 32);


    nodeEnter.append("text")
        .attr("y", function (d) {
            return d.children || d._children ? -20 : 20;
        })
        .attr("dy", ".35em")
        .attr("text-anchor", "middle")
        .text(function (d) {
            if(d.type == 'members') {
                return '';
            }else if(d.type == 'clusters') {
                return d.alias;
            }else if(d.type == 'groups'){
                return d.groupId;
            }else{
                return d.name;
            }

        })
        .style("fill-opacity", 1);

    // Declare the links…
    var link = svg.selectAll("path.link")
        .data(links, function (d) {
            return d.target.id;
        });

    // Enter the links.
    link.enter().insert("path", "g")
        .style('fill','none')
        .style('stroke-width','2')
        .style('stroke','#ccc')
        .attr("class", "link")
        .attr("d", diagonal);

    //enable popovers on nodes
    $('svg .node').popover({
        'trigger': 'manual'
        ,'container': '.application-topology'
        ,'placement': 'auto'
        ,'white-space': 'nowrap'
        ,'html':'true'
        ,delay: {show: 50, hide: 400}
    });

    var timer,
        popover_parent;
    function hidePopover(elem) {
        $(elem).popover('hide');
    }
    $('svg .node').hover(
        function() {
            var self = this;
            clearTimeout(timer);
            $('.popover').hide(); //Hide any open popovers on other elements.
            popover_parent = self
            $(self).popover('show');
        },
        function() {
            var self = this;
            timer = setTimeout(function(){hidePopover(self)},300);
        });
    $(document).on({
        mouseenter: function() {
            clearTimeout(timer);
        },
        mouseleave: function() {
            var self = this;
            timer = setTimeout(function(){hidePopover(popover_parent)},300);
        }
    }, '.popover');

}


//Application view
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
        .attr('title',groupJSON.alias)
        .addClass('stepnode')
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
            var divCartridge = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[prop].type} )
                .text(item[prop].type)
                .addClass('input-false')
                .addClass('stepnode')
                .attr('data-toggle', 'tooltip')
                .attr('title',item[prop].type )
                .appendTo('#whiteboard');


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
                .attr('title',item[prop]['name'])
                .addClass('input-false')
                .appendTo('#whiteboard');


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

var initapp = 0;

$("a[href='#application']").on('shown.bs.tab', function(e) {
    if(initapp == 0){
        addJsplumbGroup(applicationJSON, cartridgeCounter);
        //reposition after group add
        dagrePosition();
        initapp++;
    }
});



