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

var paper, paper_chart;

var ELB_WIDTH = 200;
var ELB_HEIGHT = 60;
var ELB_ROUND = 10;
//down colors
var NODE_DOWN_FILL = "90-#d0d0d0-#e6e6e6";
var NODE_DOWN_OVER_FILL = "#fafafa";

//ELB colors
var ELB_UP_FILL = "90-#ffb100-#ff8900";
var ELB_UP_OVER_FILL = "#ff9226";

//Cluster colors
var CLUSTER_UP_FILL = "90-#007eff-#007edd";
var CLUSTER_UP_OVER_FILL = "#00b9ff";

//Error colors
var ERROR_FILL = "90-#ff221a-#ba0600";
var ERROR_OVER_FILL = "#ff221a";

var DARK_TXT_COLOR = '#404040';
var LIGHT_TXT_COLOR = '#FFFFFF';

var INIT_X = 15;
var INIT_Y = 30;

var UP_TIME_TITLE = "Up Time";
var DOWN_TIME_TITLE = "Down Time";

var NODE_SHIFT_H = 250;
var NODE_SHIFT_V = 200;
var NODE_SHIFT_V_SUB = 80;

var LINE_COLOR = "#91de02";

var CIRCLE_FILL = ["#dcff9b", "#ff9bfc", "#fec5ac"];
var CIRCLE_STROKE = ["#91de02", "#c402de", "#de4402"];

$(function () {
    spinner("canvas_container", 70, 120, 12, 25, '#18c3f4', "Generating topology view ...");
});

var draw_node = function (cnode) {
    var rectangleSet = paper.set();
    var rectangle = paper.rect(cnode.x, cnode.y, ELB_WIDTH + 10, ELB_HEIGHT + 10, 10);
    var mainFill = NODE_DOWN_FILL;
    var txtColor = DARK_TXT_COLOR;
    var titleTxtColor = DARK_TXT_COLOR;
    /* satus for a cluster member can take the following values.
     Created
     Starting
     Activated
     In_Maintenance
     ReadyToShutDown
     Terminated
     Suspended
     ShuttingDown
     */
    if (cnode.state == "Created" || cnode.state == "Starting" || cnode.state == "Activated") {
        if (cnode.nodeType == 'elb') {
            mainFill = ELB_UP_FILL;
        } else {
            mainFill = CLUSTER_UP_FILL;
        }
        txtColor = LIGHT_TXT_COLOR;
        titleTxtColor = LIGHT_TXT_COLOR;
    } else if (cnode.state == "Suspended" || cnode.state == "ShuttingDown" || cnode.state == "In_Maintenance" || cnode.state == "ReadyToShutDown" || cnode.state == "Terminated") {
        titleTxtColor = LIGHT_TXT_COLOR;
        txtColor = DARK_TXT_COLOR;
        mainFill = ERROR_FILL;
    }


    rectangle.attr({fill: mainFill, stroke: '#259cbc', 'stroke-width': 0, 'stroke-linejoin': 'round'});
    rectangle.glow({"width": 7, "color": "#000000", "offsetx": 3, "offsety": 3, "fill": true});

    if (cnode.nodeType == "node") {
        var rectangle_sub = paper.rect(cnode.x + 5, cnode.y + 20, rectangle.getBBox().width - 10, rectangle.getBBox().height - 20, 10);
        rectangle_sub.attr({fill: '#fff', stroke: '#259cbc', 'stroke-width': 0, 'stroke-linejoin': 'round'});
        rectangle_sub.glow({"width": 7, "color": "#000000", "offsetx": 3, "offsety": 3, "fill": true});
    }
    //elb title
    var word_elb = paper.text(rectangle.getBBox().x, rectangle.getBBox().y + 10, cnode.title).attr({"font-size": 13, fill: titleTxtColor, stroke: "none"});
    word_elb.translate(rectangle.getBBox().width / 2, 0);

    //up time title
    var word_up_time = paper.text(rectangle.getBBox().x, rectangle.getBBox().y + 15, cnode.time_title).attr({"font-size": 10, fill: '#000', stroke: "none"});
    word_up_time.translate(word_up_time.getBBox().width / 2 + 5, 15);

    //up time value
    var word_up_time_value = paper.text(rectangle.getBBox().x, rectangle.getBBox().y + 15, cnode.time_value).attr({"font-size": 11, fill: txtColor, stroke: "none"});
    word_up_time_value.translate(word_up_time_value.getBBox().width / 2 + 5, 30);

    //cup%
    var word_cpu = paper.text(rectangle.getBBox().x + 10, rectangle.getBBox().y + 15, '%CPU').attr({"font-size": 10, fill: '#000', stroke: "none"});
    word_cpu.translate(rectangle.getBBox().width / 2 - word_cpu.getBBox().width / 2, 15);


    //value cup%
    var value_cpu = paper.text(rectangle.getBBox().x + 10, rectangle.getBBox().y + 15, '20').attr({"font-size": 10, fill: txtColor, stroke: "none"});
    value_cpu.translate(rectangle.getBBox().width / 2 - word_cpu.getBBox().width / 2, 30);

    //algo title
    var word_policy = paper.text(rectangle.getBBox().x + 10, rectangle.getBBox().y + 15, cnode.policy).attr({"font-size": 10, fill: '#000', stroke: "none"});
    word_policy.translate(rectangle.getBBox().width * 2 / 3 + word_policy.getBBox().width * 1 / 3, 15);

    if (cnode.state == "Created" || cnode.state == "Starting" || cnode.state == "Activated") {
        var rectangle_btn_sd = paper.rect(word_policy.getBBox().x, word_policy.getBBox().y + 15, 23, 17, 0);
        rectangle_btn_sd.attr({fill: 'url({{url "/themes/theme1/ui/img/btn-down.png"}})', 'stroke-width': 0});

        var rectangle_btn_restart = paper.rect(word_policy.getBBox().x + 30, word_policy.getBBox().y + 15, 23, 17, 0);
        rectangle_btn_restart.attr({fill: 'url({{url "/themes/theme1/ui/img/btn-restart.png"}})', 'stroke-width': 0});

        rectangleSet.push(rectangle, word_elb, word_up_time, word_up_time_value, word_policy);

        rectangle_btn_sd.mouseup(executeAction({elem: cnode, action: 'shout_down'}));
        rectangle_btn_restart.mouseup(executeAction({elem: cnode, action: 'restart'}));
    } else {
        var rectangle_btn_start = paper.rect(word_policy.getBBox().x, word_policy.getBBox().y + 15, 23, 17, 0);
        rectangle_btn_start.attr({fill: 'url({{url "/themes/theme1/ui/img/btn-start.png"}})', 'stroke-width': 0});
        rectangleSet.push(rectangle, word_elb, word_up_time, word_up_time_value, word_policy);

        rectangle_btn_start.mouseup(executeAction({elem: cnode, action: 'start'}));
    }
    if (cnode.nodeType == "node") {
        rectangleSet.push(rectangle_sub);
    }


    rectangleSet.state = cnode.state;
    rectangleSet.nodeType = cnode.nodeType;

    rectangleSet.mouseup(loadNode(cnode));
    rectangleSet.mouseover(elbMouseOverHandler(rectangleSet));
    rectangleSet.mouseout(elbMouseOutHandler(rectangleSet));
};

var draw_connector = function (elb, cluster) {
    var x1 = elb.x + ELB_WIDTH / 2;
    var y1 = elb.y + ELB_HEIGHT + 10;

    var x2 = cluster.x + ELB_WIDTH / 2;
    var y2 = cluster.y - 10;

    var linePath = paper.path("M " + x1 + " " + y1 + " L " + x2 + " " + y2).attr({'stroke': CIRCLE_STROKE[elb.index], 'stroke-width': 3});
    var c1 = paper.circle(x1, y1, 10).attr({fill: CIRCLE_FILL[elb.index], stroke: CIRCLE_STROKE[elb.index], 'stroke-width': 4});
    var c2 = paper.circle(x2, y2, 10).attr({fill: CIRCLE_FILL[elb.index], stroke: CIRCLE_STROKE[elb.index], 'stroke-width': 4});
};
/*
 Sample structure expected for data_elb
 [
 {
 "name":"LB1",
 "policy":"{'partition':{'id': 'P1','provider': 'ec2','property': [{'name': 'region','value': 'ap-southeast-1'}],'partitionMin': '1','partitionMax': '3' }}"
 }
 ]
 */
$(document).ready(function () {
    $.ajax({
            url: "/console/controllers/dashboard.jag",
            data: {action: "get_topology"},
            dataType: "json",
            success: function (data) {
                $('#canvas_container').empty();
                var maxNodesForCluster = 0;
                var clustersDrew = [], clustersDrewNodes = [], data_elb = [], data_cluster = [];
                var data1 = data.topology.serviceMap;

                // Recreate the data structure so that it's easy to do the UI
                $.each(data1, function (k1, v1) {
                    var data2 = data1[k1].clusterIdClusterMap;
                    $.each(data2, function (k2, v2) {
                        if (data2[k2].isLbCluster) {
                            data_elb.push({"id": data2[k2].clusterId, "name": data1[k1].serviceName});
                        } else {
                            var lbs_for_cluster = [], nodes_for_cluster = [];
                            var data3 = data2[k2].memberMap;
                            $.each(data3, function (k3, v3) {
                                //Constructing an array of lb ids from nodes
                                var not_found = true;
                                for (var i = 0; i < lbs_for_cluster.length; i++) {
                                    if (lbs_for_cluster[i] == data3[k3].lbClusterId) {
                                        not_found = false
                                    }
                                }

                                if (not_found) {
                                    lbs_for_cluster.push(data3[k3].lbClusterId);
                                }


                                //Constructing the node array for the cluster
                                nodes_for_cluster.push({"id": data3[k3].memberId, "name": data3[k3].memberIp, "up_time": "25hrs", "down_time": "", "cpu": "20", "policy": "Round Robin", "state": data3[k3].status})

                            });
                            data_cluster.push({"id": data2[k2].clusterId, "name": data1[k1].serviceName, "elb": lbs_for_cluster, "nodes": nodes_for_cluster});
                        }

                    });
                });


                paper = new Raphael(document.getElementById('canvas_container'), 1000, 500);
                for (var i = 0; i < data_elb.length; i++) {
                    //drawing the elb
                    if (i == 0) {
                        data_elb[0].x = INIT_X;
                    } else {
                        data_elb[i].x = data_elb[i - 1].x + NODE_SHIFT_H * data_elb[i - 1].clustersForElb;
                    }
                    if (data_elb[i].state == "down") {
                        draw_node({x: data_elb[i].x, y: INIT_Y, title: data_elb[i].name, time_title: DOWN_TIME_TITLE, time_value: data_elb[i].down_time, policy: data_elb[i].policy, state: data_elb[i].state, nodeType: 'elb'});
                    } else {
                        draw_node({x: data_elb[i].x, y: INIT_Y, title: data_elb[i].name, time_title: UP_TIME_TITLE, time_value: data_elb[i].up_time, policy: data_elb[i].policy, state: data_elb[i].state, nodeType: 'elb'});
                    }
                    data_elb[i].y = INIT_Y;
                    data_elb[i].index = i;
                    //drawing the clusters
                    var clustersForElb = 0;
                    for (var j = 0; j < data_cluster.length; j++) {
                        var foundIndex = searchStringInArray(data_elb[i].id, data_cluster[j].elb);
                        if (foundIndex != -1) {
                            var clusterIndex = searchStringInArray(data_cluster[j].id, clustersDrew);
                            if (clusterIndex == -1) {
                                clustersForElb++;
                                var cx = data_elb[i].x;
                                if (data_elb[i].cluster_x != undefined) {
                                    cx = data_elb[i].cluster_x + NODE_SHIFT_H;
                                }
                                data_elb[i].cluster_x = cx;
                                if (data_cluster[j].state == "down") {
                                    draw_node({x: cx, y: INIT_Y + NODE_SHIFT_V, title: data_cluster[j].name, time_title: DOWN_TIME_TITLE, time_value: data_cluster[j].down_time, policy: data_cluster[j].policy, state: data_cluster[j].state, nodeType: 'cluster'});
                                } else {
                                    draw_node({x: cx, y: INIT_Y + NODE_SHIFT_V, title: data_cluster[j].name, time_title: UP_TIME_TITLE, time_value: data_cluster[j].up_time, policy: data_cluster[j].policy, state: data_cluster[j].state, nodeType: 'cluster'});
                                }
                                //Draw a connector
                                data_cluster[j].x = cx;
                                data_cluster[j].y = INIT_Y + NODE_SHIFT_V;
                                draw_connector(data_elb[i], data_cluster[j]);

                                // Drawing cluster nodes
                                var data_node = data_cluster[j].nodes;
                                if (maxNodesForCluster < data_node.length) {
                                    maxNodesForCluster = data_node.length;
                                }
                                for (var k = 0; k < data_node.length; k++) {
                                    var nx, ny;
                                    if (k == 0) {
                                        nx = data_cluster[j].x;
                                        ny = data_cluster[j].y + NODE_SHIFT_V_SUB;
                                    } else {
                                        nx = data_node[k - 1].x;
                                        ny = data_node[k - 1].y + NODE_SHIFT_V_SUB;
                                    }
                                    data_node[k].x = nx;
                                    data_node[k].y = ny;

                                    if (data_node[k].state == "down") {
                                        draw_node({x: nx, y: ny, title: data_node[k].name, time_title: DOWN_TIME_TITLE, time_value: data_node[k].down_time, policy: data_node[k].policy, state: data_node[k].state, nodeType: 'node'});
                                    } else {
                                        draw_node({x: nx, y: ny, title: data_node[k].name, time_title: UP_TIME_TITLE, time_value: data_node[k].up_time, policy: data_node[k].policy, state: data_node[k].state, nodeType: 'node'});
                                    }
                                }
                                clustersDrew.push(data_cluster[j].id);
                                clustersDrewNodes.push(data_cluster[j]);
                            } else {
                                //draw the connector
                                draw_connector(data_elb[i], clustersDrewNodes[clusterIndex]);
                            }
                        }
                    }
                    data_elb[i].clustersForElb = clustersForElb;
                }
                //draw_node({x:15,y:30,title:'ELB1',time_title:'Up Time',time_value:'3h:25m:10s',algo:'Round Robin',state:'',nodeType:'elb'});
                //draw_node({x:330,y:30,title:'ELB1',time_title:'Up Time',time_value:'3h:25m:10s',algo:'Round Robin',state:'up',nodeType:'elb'});

                //draw_node({x: 15, y: 230, title: 'ESB Cluster', time_title: 'Up Time', time_value: '3h:25m:10s', policy: 'Round Robin', state: 'up', nodeType: 'cluster'});


                // Resize the canves
                var paperHeight = 350 + 80 * maxNodesForCluster;
                paper.setSize(1000, paperHeight);
            }
        }
    )
});
var elbMouseOverHandler = function (rect) {
    return function () {
        if (rect.state  == "Created" || rect.state == "Starting" || rect.state == "Activated") {
            if (rect.nodeType == "elb") {
                rect[0].attr({ "stroke-width": 2, "stroke": '#ff000', "fill": ELB_UP_OVER_FILL });
            } else if (rect.nodeType == "cluster") {
                rect[0].attr({ "stroke-width": 2, "stroke": '#ff000', "fill": CLUSTER_UP_OVER_FILL });
            }
        } else if (rect.state == "Suspended" || rect.state == "ShuttingDown" || rect.state == "In_Maintenance" || rect.state == "ReadyToShutDown" || rect.state == "Terminated") {
            rect[0].attr({ "stroke-width": 0, "fill": ERROR_OVER_FILL});
        } else {
            rect[0].attr({ "stroke-width": 2, "stroke": '#a5a5a5', "fill": NODE_DOWN_OVER_FILL});
        }
    }
};


var elbMouseOutHandler = function (rect) {
    return function () {
        if (rect.state  == "Created" || rect.state == "Starting" || rect.state == "Activated") {
            if (rect.nodeType == "elb") {
                rect[0].attr({ "stroke-width": 0, "fill": ELB_UP_FILL});
            } else if (rect.nodeType == "cluster") {
                rect[0].attr({ "stroke-width": 0, "fill": CLUSTER_UP_FILL});
            }
        } else if (rect.state == "Suspended" || rect.state == "ShuttingDown" || rect.state == "In_Maintenance" || rect.state == "ReadyToShutDown" || rect.state == "Terminated") {
            rect[0].attr({ "stroke-width": 0, "fill": ERROR_FILL});
        } else {
            rect[0].attr({ "stroke-width": 0, "fill": NODE_DOWN_FILL});

        }
    }
};
function loadNode(node) {
    var node = node;
    return function () {
        $('#nodeDetails').css({"margin-left": node.x + ELB_WIDTH + 30 + "px", "margin-top": node.y + "px"}).show().animate({height: "550px"});
    }
}
$(function () {
    $('.close-btn').click(function () {
        $(this).parent().animate({height: "0px"}, function () {
            $(this).hide()
        })
    });
});

function executeAction(info) {
    var tmpInfo = info;
    return function (event) {
        alert('Action is ' + tmpInfo.action + ' - Node is ' + tmpInfo.elem.title);
    }
}
function searchStringInArray(str, strArray) {
    for (var j = 0; j < strArray.length; j++) {
        if (strArray[j].match(str)) return j;
    }
    return -1;
}

function spinner(holderid, R1, R2, count, stroke_width, colour, text) {
    var sectorsCount = count || 12,
        color = colour || "#fff",
        width = stroke_width || 15,
        r1 = Math.min(R1, R2) || 35,
        r2 = Math.max(R1, R2) || 60,
        cx = r2 + width,
        cy = r2 + width,
        r = Raphael(holderid, r2 * 2 + width * 2, r2 * 2 + width * 2),

        sectors = [],
        opacity = [],
        beta = 2 * Math.PI / sectorsCount,

        pathParams = {stroke: color, "stroke-width": width, "stroke-linecap": "round"};
    Raphael.getColor.reset();
    for (var i = 0; i < sectorsCount; i++) {
        var alpha = beta * i - Math.PI / 2,
            cos = Math.cos(alpha),
            sin = Math.sin(alpha);
        opacity[i] = 1 / sectorsCount * i;
        sectors[i] = r.path([
            ["M", cx + r1 * cos, cy + r1 * sin],
            ["L", cx + r2 * cos, cy + r2 * sin]
        ]).attr(pathParams);
        if (color == "rainbow") {
            sectors[i].attr("stroke", Raphael.getColor());
        }
    }
    if (text != null) {
        var t = r.text(0, 0, text).attr({"font-size": 30, fill: "#999999", "stroke": "none"})
        t.translate(t.getBBox().width / 2 + r2 * 2 + width * 2, r2);
        r.setSize(r2 * 2 + width * 2 + t.getBBox().width, r2 * 2 + width * 2);
    }

    var tick;
    (function ticker() {
        opacity.unshift(opacity.pop());
        for (var i = 0; i < sectorsCount; i++) {
            sectors[i].attr("opacity", opacity[i]);
        }
        r.safari();
        tick = setTimeout(ticker, 1000 / sectorsCount);
    })();
    return function () {
        clearTimeout(tick);
        r.remove();
    };
}
