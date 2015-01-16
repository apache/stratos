//create JSON from topology
function genTree(data){
    var rawout = [];

    var rootnode ={};
    rootnode.name = data.id;
    rootnode.parent = null;
    //create initial root node
    rawout.push(rootnode);

    //application instances
    function applicationInstances(items, collector, parent){
        for(var prop in items){
            if (items.hasOwnProperty(prop)) {
                var cur_name = items[prop].instanceId,
                    status = items[prop].status;
                rawout.push({"name": cur_name, "parent": parent, "status": status});

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
                var type = 'clusters';
                rawout.push({"name": cur_name, "parent": parent, "type": type, "status": status,
                    "alias":alias, "hostNames": hostNames, "serviceName": serviceName
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
                    networkPartitionId = items[prop].networkPartitionId,
                    partitionId = items[prop].partitionId,
                    status = items[prop].status;
                var type = 'members';
                rawout.push({"name": cur_name, "parent": parent, "type": type, "status": status,
                    "defaultPrivateIP":defaultPrivateIP, "defaultPublicIP":defaultPublicIP,
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
    var margin = {top: 40, right: 120, bottom: 20, left: 120},
        width = 960 - margin.right - margin.left,
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
        d.y = d.depth * 100;
    });

    // Declare the nodes…
    var node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id || (d.id = ++i);
        });

    // Enter the nodes.
    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.x + "," + d.y + ")";
        })
        // add tool tip for ps -eo pid,ppid,pcpu,size,comm,ruser,s
        .on("mouseover", function (d) {
            div.transition()
                .duration(200)
                .style("opacity", .9);

            if (d.type == 'clusters') {
                div.html(
                        "<strong>Cluster Id: </strong>" + d.name + "<br/>" +
                        "<strong>Cluster Alias: </strong>" + d.alias + "<br/>" +
                        "<strong>HostNames: </strong>" + d.hostNames + "<br/>" +
                        "<strong>Service Name: </strong>" + d.serviceName + "<br/>" +
                        "<strong>Status: </strong>" + d.status
                ).style("left", (d3.event.pageX) + "px")
                    .style("top", (d3.event.pageY - 28) + "px");
            } else if (d.type == 'members') {
                div.html(
                        "<strong>Member Id: </strong>" + d.name + "<br/>" +
                        "<strong>Default Private IP: </strong>" + d.defaultPrivateIP + "<br/>" +
                        "<strong>Default Public IP: </strong>" + d.defaultPublicIP + "<br/>" +
                        "<strong>Network Partition Id: </strong>" + d.networkPartitionId + "<br/>" +
                        "<strong>Partition Id: </strong>" + d.partitionId + "<br/>" +
                        "<strong>Status: </strong>" + d.status
                ).style("left", (d3.event.pageX) + "px")
                    .style("top", (d3.event.pageY - 28) + "px");
            } else if (d.type == 'groups') {
                div.html(
                        "<strong>Group Instance Id: </strong>" + d.instanceId + "<br/>" +
                        "<strong>Status: </strong>" + d.status
                ).style("left", (d3.event.pageX) + "px")
                    .style("top", (d3.event.pageY - 28) + "px");
            } else {
                div.html(
                        "<strong>Alias: </strong>" + d.name + "<br/>"
                ).style("left", (d3.event.pageX) + "px")
                    .style("top", (d3.event.pageY - 28) + "px");
            }

        })
        .on("mouseout", function (d) {
            div.transition()
                .duration(500)
                .style("opacity", 0);
        });

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

    // add the tool tip
    var div = d3.select("body").append("div")
        .attr("class", "tooltip")
        .style("opacity", 0);

    // Declare the links…
    var link = svg.selectAll("path.link")
        .data(links, function (d) {
            return d.target.id;
        });

    // Enter the links.
    link.enter().insert("path", "g")
        .attr("class", "link")
        .attr("d", diagonal);

}
