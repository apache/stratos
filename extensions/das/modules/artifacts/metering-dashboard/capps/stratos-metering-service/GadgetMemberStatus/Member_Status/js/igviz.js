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
(function () {

    var igviz = window.igviz || {};

    igviz.version = '1.0.0';

    igviz.val = 0;
    window.igviz = igviz;
    var persistedData = [];
    var maxValueForUpdate;
    var singleNumSvg;
    var singleNumCurveSvg;
    var mapChart;
    var mapSVG;
    var worldMapCodes;
    var usaMapCodes;

    /*************************************************** Initializtion functions ***************************************************************************************************/


    igviz.draw = function (canvas, config, dataTable) {
        var chart = new Chart(canvas, config, dataTable);

        if (config.chartType == "singleNumber") {
            this.drawSingleNumberDiagram(chart);
        } else if (config.chartType == "map") {
            this.drawMap(canvas, config, dataTable);
        } else if (config.chartType == "tabular") {
            this.drawTable(canvas, config, dataTable);
        } else if (config.chartType == "arc") {
            this.drawArc(canvas, config, dataTable);
        } else if (config.chartType == "drill") {
            this.drillDown(0, canvas, config, dataTable, dataTable);
        }
        return chart;
        //return
    };

    igviz.setUp = function (canvas, config, dataTable) {
        var chartObject = new Chart(canvas, config, dataTable);

        if (config.chartType == "bar") {
            this.drawBarChart(chartObject, canvas, config, dataTable);
        } else if (config.chartType == "scatter") {
            this.drawScatterPlot(chartObject);
        } else if (config.chartType == "line") {
            this.drawLineChart(chartObject);
        } else if (config.chartType == "area") {
            this.drawAreaChart(chartObject);
        }
        return chartObject;
    };


    /*************************************************** Line chart ***************************************************************************************************/

    igviz.drawLineChart = function (chartObj) {
        divId = chartObj.canvas;
        chartConfig = chartObj.config;
        dataTable = chartObj.dataTable;
        // table=setData(dataTable,chartConfig)

        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = [];
        for (i = 0; i < chartConfig.yAxis.length; i++) {
            yStrings[i] = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis[i]])

        }


        xScaleConfig = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "x",
            "range": "width",
            "zero": false,
            "clamp": false,
            "field": xString
        }

        yScaleConfig = {
            "index": chartConfig.yAxis[0],
            "schema": dataTable.metadata,
            "name": "y",
            "range": "height",
            "nice": true,
            "field": yStrings[0]
        }

        var xScale = setScale(xScaleConfig)
        var yScale = setScale(yScaleConfig);

        var xAxisConfig = {
            "type": "x",
            "scale": "x",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.xAxis],
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "right",
            "titleDy": 10,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "y",
            "angle": 0,
            "title": "values",
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);

        if (chartConfig.interpolationMode == undefined) {
            chartConfig.interpolationMode = "monotone";
        }
        var spec = {
            "width": chartConfig.width - 160,
            "height": chartConfig.height,
            //  "padding":{"top":40,"bottom":60,'left':90,"right":150},
            "data": [
                {
                    "name": "table"

                }
            ],
            "scales": [
                xScale, yScale,
                {
                    "name": "color", "type": "ordinal", "range": "category20"
                }
            ],
            "axes": [xAxis, yAxis
            ],
            "legends": [
                {

                    "orient": "right",
                    "fill": "color",
                    "title": "Legend",
                    "values": [],
                    "properties": {
                        "title": {
                            "fontSize": {"value": 14}
                        },
                        "labels": {
                            "fontSize": {"value": 12}
                        },
                        "symbols": {
                            "stroke": {"value": "transparent"}
                        },
                        "legend": {
                            "stroke": {"value": "steelblue"},
                            "strokeWidth": {"value": 1.5}

                        }
                    }
                }
            ],

            "marks": []
        }

        for (i = 0; i < chartConfig.yAxis.length; i++) {
            markObj = {
                "type": "line",
                "key": xString,
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"value": 400},
                        "interpolate": {"value": chartConfig.interpolationMode},
                        "y": {"scale": "y:prev", "field": yStrings[i]},
                        "stroke": {"scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]},
                        "strokeWidth": {"value": 1.5}
                    },
                    "update": {
                        "x": {"scale": "x", "field": xString},
                        "y": {"scale": "y", "field": yStrings[i]}
                    },
                    "exit": {
                        "x": {"value": -20},
                        "y": {"scale": "y", "field": yStrings[i]}
                    }
                }
            };
            pointObj = {
                "type": "symbol",
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"scale": "x", "field": xString},
                        "y": {"scale": "y", "field": yStrings[i]},
                        "fill": {
                            "scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]
                            //"fillOpacity": {"value": 0.5}
                        },
                        "update": {
                            //"size": {"scale":"r","field":rString},
                            // "stroke": {"value": "transparent"}
                        },
                        "hover": {
                            "size": {"value": 300},
                            "stroke": {"value": "white"}
                        }
                    }
                }
            }


            spec.marks.push(markObj);
            spec.marks.push(pointObj);
            spec.legends[0].values.push(dataTable.metadata.names[chartConfig.yAxis[i]])

        }


        chartObj.toolTipFunction = [];
        chartObj.toolTipFunction[0] = function (event, item) {

            console.log(tool, event, item);
            if (item.mark.marktype == 'symbol') {
                xVar = dataTable.metadata.names[chartConfig.xAxis]
                yVar = dataTable.metadata.names[chartConfig.yAxis]

                contentString = '<table><tr><td> X </td><td> (' + xVar + ') </td><td>' + item.datum.data[xVar] + '</td></tr>' + '<tr><td> Y </td><td> (' + yVar + ') </td><td>' + item.datum.data[yVar] + '</td></tr></table>';


                tool.html(contentString).style({
                    'left': event.pageX + 10 + 'px',
                    'top': event.pageY + 10 + 'px',
                    'opacity': 1
                })
                tool.selectAll('tr td').style('padding', "3px");
            }
        }

        chartObj.toolTipFunction[1] = function (event, item) {

            tool.html("").style({'left': event.pageX + 10 + 'px', 'top': event.pageY + 10 + 'px', 'opacity': 0})

        }

        chartObj.spec = spec;
        chartObj.toolTip = true;
        chartObj.spec = spec;

    }


    /*************************************************** Bar chart ***************************************************************************************************/
    igviz.drawBarChart = function (mychart, divId, chartConfig, dataTable) {
        //  console.log(this);
        divId = mychart.canvas;
        chartConfig = mychart.config;
        dataTable = mychart.dataTable;
        if (chartConfig.hasOwnProperty("groupedBy")) {
            var format = "grouped";
            if (chartConfig.hasOwnProperty("format")) {
                format = chartConfig.format;

            }
            if (format == "grouped") {
                console.log("groupedDFJSDFKSD:JFKDJF");
                if (chartConfig.orientation == 'H') {
                    console.log('horizontal');
                    return this.drawGroupedBarChart(mychart);

                }
                return this.drawGroupedBarChartVertical(mychart);
            }
            else {
                return this.drawStackedBarChart(mychart);
            }
        }

        var xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis]);
        var yString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis])

        xScaleConfig = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "x",
            "zero": false,
            "range": "width",
            "round": true,
            "field": xString
        }

        yScaleConfig = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "y",
            "range": "height",
            "nice": true,
            "field": yString
        }

        var xScale = setScale(xScaleConfig)
        var yScale = setScale(yScaleConfig);

        var xAxisConfig = {
            "type": "x",
            "scale": "x",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.xAxis],
            "grid": false,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": 30,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "y",
            "angle": 0,
            "title": dataTable.metadata.names[chartConfig.yAxis],
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -35,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);

        if (chartConfig.barColor == undefined) {
            chartConfig.barColor = "steelblue";
        }

//        console.log(table)
        var spec = {

            "width": chartConfig.width - 150,
            //"padding":{'top':30,"left":80,"right":80,'bottom':60},
            "height": chartConfig.height,
            "data": [
                {
                    "name": "table"
                }
            ],
            "scales": [
                xScale,
                yScale
            ],
            "axes": [
                xAxis,
                yAxis


            ],
            "marks": [
                {
                    "key": xString,
                    "type": "rect",
                    "from": {"data": "table"},
                    "properties": {
                        "enter": {
                            "x": {"scale": "x", "field": xString},
                            "width": {"scale": "x", "band": true, "offset": -10},
                            "y": {"scale": "y:prev", "field": yString, "duration": 2000},
                            "y2": {"scale": "y", "value": 0}

                        },
                        "update": {
                            "x": {"scale": "x", "field": xString},
                            "y": {"scale": "y", "field": yString},
                            "y2": {"scale": "y", "value": 0},
                            "fill": {"value": chartConfig.barColor}
                        },
                        "exit": {
                            "x": {"value": 0},
                            "y": {"scale": "y:prev", "field": yString},
                            "y2": {"scale": "y", "value": 0}
                        },

                        "hover": {

                            "fill": {'value': 'orange'}
                        }

                    }
                }
            ]
        }


//        var data = {table: table}

        mychart.originalWidth = chartConfig.width;
        mychart.originalHeight = chartConfig.height;

        mychart.spec = spec;
        //mychart.data = data;
        //mychart.table = table;
        ////vg.parse.spec(spec, function (chart) {
        //    mychart.chart = chart({
        //        el: divId,
        //        renderer: 'svg',
        //        data: data,
        //        hover: false
        //
        //    }).update();
        //
        //    // mychart.chart.data(data).update();
        //    //self.counter=0;
        //    //console.log('abc');
        //    //setInterval(updateTable,1500);
        //
        //});
    };

    igviz.drawStackedBarChart = function (chartObj) {

        var chartConfig = chartObj.config;
        var dataTable = chartObj.dataTable;
        //   var table = setData(dataTable,chartConfig);
        divId = chartObj.canvas;


        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis]);

        groupedBy = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.groupedBy]);

        // console.log(table,xString,yStrings,groupedBy);
        // sortDataSet(table);

        cat = {
            "index": chartConfig.groupedBy,
            "schema": dataTable.metadata,
            "name": "cat",
            "range": "width",
            "field": groupedBy,
            "padding": 0.2
        }


        val = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "val",
            "range": "height",
            "dataFrom": "stats",
            "field": "sum",
            "nice": true
        }


        var cScale = setScale(cat)
        var vScale = setScale(val);

        var xAxisConfig = {
            "type": "x",
            "scale": "cat",
            "angle": 0,
            "title": dataTable.metadata.names[chartConfig.groupedBy],
            "grid": false,
            "dx": -10,
            "dy": 10,
            "align": "right",
            "titleDy": 10,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "val",
            "angle": 0,
            "title": dataTable.metadata.names[chartConfig.yAxis],
            "grid": false,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);


        spec = {
            "width": chartConfig.width - 160,
            "height": chartConfig.height - 100,
            "padding": {"top": 10, "left": 60, "bottom": 60, "right": 100},
            "data": [
                {
                    "name": "table"
                },
                {
                    "name": "stats",
                    "source": "table",
                    "transform": [
                        {"type": "facet", "keys": [groupedBy]},
                        {"type": "stats", "value": yStrings}
                    ]
                }
            ],
            "scales": [
                cScale,
                vScale,
                {
                    "name": "color",
                    "type": "ordinal",
                    "range": "category20"
                }
            ],
            "legends": [
                {
                    "orient": {"value": "right"},
                    "fill": "color",
                    "title": dataTable.metadata.names[chartConfig.xAxis],
                    "values": [],
                    "properties": {
                        "title": {
                            "fontSize": {"value": 14}
                        },
                        "labels": {
                            "fontSize": {"value": 12}
                        },
                        "symbols": {
                            "stroke": {"value": "transparent"}
                        },
                        "legend": {
                            "stroke": {"value": "steelblue"},
                            "strokeWidth": {"value": 0.5}


                        }
                    }
                }
            ],

            "axes": [
                xAxis, yAxis
            ],

            "marks": [
                {
                    "type": "group",
                    "from": {
                        "data": "table",
                        "transform": [
                            {"type": "facet", "keys": [xString]},
                            {"type": "stack", "point": groupedBy, "height": yStrings}
                        ]
                    },
                    "marks": [
                        {
                            "type": "rect",
                            "properties": {
                                "enter": {
                                    "x": {"scale": "cat", "field": groupedBy},
                                    "width": {"scale": "cat", "band": true, "offset": -1},
                                    "y": {"scale": "val", "field": "y"},
                                    "y2": {"scale": "val", "field": "y2"},
                                    "fill": {"scale": "color", "field": xString}
                                },
                                "update": {
                                    "fillOpacity": {"value": 1}
                                },
                                "hover": {
                                    "fillOpacity": {"value": 0.5}
                                }
                            }
                        }
                    ]
                }
            ]
        }

        chartObj.legend = true;
        chartObj.legendIndex = chartConfig.xAxis;
        chartObj.spec = spec;

    }

    igviz.drawGroupedBarChart = function (chartObj) {
        var chartConfig = chartObj.config;
        var dataTable = chartObj.dataTable;
        //  var table = setData(dataTable,chartConfig);
        divId = chartObj.canvas;


        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis]);

        groupedBy = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.groupedBy]);

        //  console.log(table,xString,yStrings,groupedBy);
        // sortDataSet(table);

        cat = {
            "index": chartConfig.groupedBy,
            "schema": dataTable.metadata,
            "name": "cat",
            "range": "height",
            "field": groupedBy,
            "padding": 0.2
        }


        val = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "val",
            "range": "width",
            "round": 'true',
            "field": yStrings,
            "nice": true
        }


        var cScale = setScale(cat)
        var vScale = setScale(val);

        var xAxisConfig = {
            "type": "x",
            "scale": "val",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.yAxis],
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "right",
            "titleDy": 10,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "cat",
            "angle": 0,
            "tickSize": 0,
            "tickPadding": 8,
            "title": dataTable.metadata.names[chartConfig.groupedBy],
            "grid": false,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);


        spec = {
            "width": chartConfig.width,
            "height": chartConfig.height,

            "data": [
                {
                    "name": "table"
                }
            ],
            "scales": [
                cScale, vScale,
                {
                    "name": "color",
                    "type": "ordinal",
                    "range": "category20"
                }
            ],
            "axes": [
                xAxis, yAxis
            ],
            "legends": [
                {
                    "orient": {"value": "right"},
                    "fill": "color",
                    "title": dataTable.metadata.names[chartConfig.xAxis],
                    "values": [],
                    "properties": {
                        "title": {
                            "fontSize": {"value": 14}
                        },
                        "labels": {
                            "fontSize": {"value": 12}
                        },
                        "symbols": {
                            "stroke": {"value": "transparent"}
                        },
                        "legend": {
                            "stroke": {"value": "steelblue"},
                            "strokeWidth": {"value": 0.5}


                        }
                    }
                }
            ],


            "marks": [
                {
                    "type": "group",
                    "from": {
                        "data": "table",
                        "transform": [{"type": "facet", "keys": [groupedBy]}]
                    },
                    "properties": {
                        "enter": {
                            "y": {"scale": "cat", "field": "key"},
                            "height": {"scale": "cat", "band": true}
                        }
                    },
                    "scales": [
                        {
                            "name": "pos",
                            "type": "ordinal",
                            "range": "height",
                            "domain": {"field": xString}
                        }
                    ],
                    "marks": [
                        {
                            "type": "rect",
                            "properties": {
                                "enter": {
                                    "y": {"scale": "pos", "field": xString},
                                    "height": {"scale": "pos", "band": true},
                                    "x": {"scale": "val", "field": yStrings},
                                    "x2": {"scale": "val", "value": 0},
                                    "fill": {"scale": "color", "field": xString}
                                },
                                "hover": {
                                    "fillOpacity": {"value": 0.5}
                                }
                                ,

                                "update": {
                                    "fillOpacity": {"value": 1}
                                }
                            }
                        },
                        //{
                        //    "type": "text",
                        //    "properties": {
                        //        "enter": {
                        //            "y": {"scale": "pos", "field": xString},
                        //            "dy": {"scale": "pos", "band": true, "mult": 0.5},
                        //            "x": {"scale": "val", "field": yStrings, "offset": -4},
                        //            "fill": {"value": "white"},
                        //            "align": {"value": "right"},
                        //            "baseline": {"value": "middle"},
                        //            "text": {"field": xString}
                        //        }
                        //    }
                        //}
                    ]
                }
            ]
        }

        chartObj.legend = true;
        chartObj.legendIndex = chartConfig.xAxis;
        chartObj.spec = spec;

    }

    igviz.drawGroupedBarChartVertical = function (chartObj) {
        var chartConfig = chartObj.config;
        var dataTable = chartObj.dataTable;
        //  var table = setData(dataTable,chartConfig);
        divId = chartObj.canvas;


        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis]);

        groupedBy = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.groupedBy]);

        //  console.log(table,xString,yStrings,groupedBy);
        // sortDataSet(table);

        cat = {
            "index": chartConfig.groupedBy,
            "schema": dataTable.metadata,
            "name": "cat",
            "range": "width",
            "field": groupedBy,
            "padding": 0.2
        }


        val = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "val",
            "range": "height",
            "round": 'true',
            "field": yStrings,
            "nice": true
        }


        var cScale = setScale(cat)
        var vScale = setScale(val);

        var yAxisConfig = {
            "type": "y",
            "scale": "val",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.yAxis],
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "right",
            "titleDy": 10,
            "titleDx": 0
        }
        var xAxisConfig = {
            "type": "x",
            "scale": "cat",
            "angle": 0,
            "tickSize": 0,
            "tickPadding": 8,
            "title": dataTable.metadata.names[chartConfig.groupedBy],
            "grid": false,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);


        spec = {
            "width": chartConfig.width - 150,
            "height": chartConfig.height,
            "data": [
                {
                    "name": "table"
                }
            ],
            "scales": [
                cScale, vScale,
                {
                    "name": "color",
                    "type": "ordinal",
                    "range": "category20"
                }
            ],
            "axes": [
                xAxis, yAxis
            ],
            "legends": [
                {
                    "orient": {"value": "right"},
                    "fill": "color",
                    "title": dataTable.metadata.names[chartConfig.xAxis],
                    "values": [],
                    "properties": {
                        "title": {
                            "fontSize": {"value": 14}
                        },
                        "labels": {
                            "fontSize": {"value": 12}
                        },
                        "symbols": {
                            "stroke": {"value": "transparent"}
                        },
                        "legend": {
                            "stroke": {"value": "steelblue"},
                            "strokeWidth": {"value": 0.5}


                        }
                    }
                }
            ],


            "marks": [
                {
                    "type": "group",
                    "from": {
                        "data": "table",
                        "transform": [{"type": "facet", "keys": [groupedBy]}]
                    },
                    "properties": {
                        "enter": {
                            "x": {"scale": "cat", "field": "key"},
                            "width": {"scale": "cat", "band": true}
                        }
                    },
                    "scales": [
                        {
                            "name": "pos",
                            "type": "ordinal",
                            "range": "width",
                            "domain": {"field": xString}
                        }
                    ],
                    "marks": [
                        {
                            "type": "rect",
                            "properties": {
                                "enter": {
                                    "x": {"scale": "pos", "field": xString},
                                    "width": {"scale": "pos", "band": true},
                                    "y": {"scale": "val", "field": yStrings},
                                    "y2": {"scale": "val", "value": 0},
                                    "fill": {"scale": "color", "field": xString}
                                },
                                "hover": {
                                    "fillOpacity": {"value": 0.5}
                                }
                                ,

                                "update": {
                                    "fillOpacity": {"value": 1}
                                }
                            }
                        },
                        //{
                        //    "type": "text",
                        //    "properties": {
                        //        "enter": {
                        //            "y": {"scale": "pos", "field": xString},
                        //            "dy": {"scale": "pos", "band": true, "mult": 0.5},
                        //            "x": {"scale": "val", "field": yStrings, "offset": -4},
                        //            "fill": {"value": "white"},
                        //            "align": {"value": "right"},
                        //            "baseline": {"value": "middle"},
                        //            "text": {"field": xString}
                        //        }
                        //    }
                        //}
                    ]
                }
            ]
        }

        chartObj.legend = true;
        chartObj.legendIndex = chartConfig.xAxis;
        chartObj.spec = spec;

    }


    /*************************************************** Area chart ***************************************************************************************************/

    igviz.drawAreaChart = function (chartObj) {
        // var padding = chartConfig.padding;
        var chartConfig = chartObj.config;
        var dataTable = chartObj.dataTable;

        if (chartConfig.yAxis.constructor === Array) {
            return this.drawMultiAreaChart(chartObj)
        }
        if (chartConfig.hasOwnProperty("areaVar")) {
            return this.drawStackedAreaChart(chartObj);
        }

        divId = chartObj.canvas;


        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis]);

        //   console.log(table,xString,yStrings);
        // sortDataSet(table);

        xScaleConfig = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "x",
            "zero": false,
            "range": "width",
            "field": xString
        }


        yScaleConfig = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "y",
            "range": "height",
            "field": yStrings
        }


        var xScale = setScale(xScaleConfig)
        var yScale = setScale(yScaleConfig);

        var xAxisConfig = {
            "type": "x",
            "scale": "x",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.xAxis],
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "right",
            "titleDy": 10,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "y",
            "angle": 0,
            "title": dataTable.metadata.names[chartConfig.yAxis],
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);

        if (chartConfig.interpolationMode == undefined) {
            chartConfig.interpolationMode = "monotone"
        }


        var spec = {
            "width": chartConfig.width - 100,
            "height": chartConfig.height,
            //  "padding":{"top":40,"bottom":60,'left':60,"right":40},
            "data": [
                {
                    "name": "table"

                }
            ],
            "scales": [
                xScale, yScale,
                {
                    "name": "color", "type": "ordinal", "range": "category10"
                }
            ],

            "axes": [xAxis, yAxis]
            ,

            "marks": [
                {
                    "type": "area",
                    "key": xString,
                    "from": {"data": "table"},
                    "properties": {
                        "enter": {
                            "x": {"scale": "x", "field": xString},
                            "interpolate": {"value": chartConfig.interpolationMode},

                            "y": {"scale": "y", "field": yStrings},
                            "y2": {"scale": "y", "value": 0},
                            "fill": {"scale": "color", "value": 2},
                            "fillOpacity": {"value": 0.5}
                        },
                        "update": {
                            "fillOpacity": {"value": 0.5}

                        },
                        "hover": {
                            "fillOpacity": {"value": 0.2}

                        }

                    }
                },
                {
                    "type": "line",
                    "key": xString,

                    "from": {"data": "table"},
                    "properties": {
                        "enter": {
                            "x": {"value": 400},
                            "interpolate": {"value": chartConfig.interpolationMode},
                            "y": {"scale": "y:prev", "field": yStrings},
                            "stroke": {"scale": "color", "value": 2},
                            "strokeWidth": {"value": 1.5}
                        },
                        "update": {
                            "x": {"scale": "x", "field": xString},
                            "y": {"scale": "y", "field": yStrings}
                        },
                        "exit": {
                            "x": {"value": -20},
                            "y": {"scale": "y", "field": yStrings}
                        }
                    }
                },
                {
                    "type": "symbol",
                    "from": {"data": "table"},
                    "properties": {
                        "enter": {
                            "x": {"scale": "x", "field": xString},
                            "y": {"scale": "y", "field": yStrings},
                            "fill": {"scale": "color", "value": 2},
                            "size": {"value": 50}
                            //"fillOpacity": {"value": 0.5}
                        },
                        "update": {
                            "size": {"value": 50}

                            //"size": {"scale":"r","field":rString},
                            // "stroke": {"value": "transparent"}
                        },
                        "hover": {
                            "size": {"value": 100},
                            "stroke": {"value": "white"}
                        }
                    }
                }

            ]
        }

        chartObj.toolTipFunction = [];
        chartObj.toolTipFunction[0] = function (event, item) {


            console.log(tool, event, item);
            if (item.mark.marktype == 'symbol') {


                xVar = dataTable.metadata.names[chartConfig.xAxis]
                yVar = dataTable.metadata.names[chartConfig.yAxis]

                contentString = '<table><tr><td> X </td><td> (' + xVar + ') </td><td>' + item.datum.data[xVar] + '</td></tr>' + '<tr><td> Y </td><td> (' + yVar + ') </td><td>' + item.datum.data[yVar] + '</td></tr></table>';


                tool.html(contentString).style({
                    'left': event.pageX + 10 + 'px',
                    'top': event.pageY + 10 + 'px',
                    'opacity': 1
                })
                tool.selectAll('tr td').style('padding', "3px");
            }
        }

        chartObj.toolTipFunction[1] = function (event, item) {

            tool.html("").style({'left': event.pageX + 10 + 'px', 'top': event.pageY + 10 + 'px', 'opacity': 0})

        }

        chartObj.spec = spec;
        chartObj.toolTip = true;
        chartObj.spec = spec;


    };

    igviz.drawMultiAreaChart = function (chartObj) {

        divId = chartObj.canvas;
        chartConfig = chartObj.config;
        dataTable = chartObj.dataTable;
        // table=setData(dataTable,chartConfig)

        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yStrings = [];
        for (i = 0; i < chartConfig.yAxis.length; i++) {
            yStrings[i] = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis[i]])

        }


        xScaleConfig = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "x",
            "range": "width",
            "clamp": false,
            "field": xString
        }

        yScaleConfig = {
            "index": chartConfig.yAxis[0],
            "schema": dataTable.metadata,
            "name": "y",
            "range": "height",
            "nice": true,
            "field": yStrings[0]
        }

        var xScale = setScale(xScaleConfig)
        var yScale = setScale(yScaleConfig);

        var xAxisConfig = {
            "type": "x",
            "scale": "x",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.xAxis],
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "left",
            "titleDy": 10,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "y",
            "angle": 0,
            "title": "values",
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);


        if (chartConfig.interpolationMode == undefined) {
            chartConfig.interpolationMode = "monotone";
        }


        var spec = {
            "width": chartConfig.width - 160,
            "height": chartConfig.height,
            //    "padding":{"top":40,"bottom":60,'left':60,"right":145},
            "data": [
                {
                    "name": "table"

                }
            ],
            "scales": [
                xScale, yScale,
                {
                    "name": "color", "type": "ordinal", "range": "category20"
                }
            ],
            "legends": [
                {

                    "orient": "right",
                    "fill": "color",
                    "title": "Area",
                    "values": [],
                    "properties": {
                        "title": {
                            "fontSize": {"value": 14}
                        },
                        "labels": {
                            "fontSize": {"value": 12}
                        },
                        "symbols": {
                            "stroke": {"value": "transparent"}
                        },
                        "legend": {
                            "stroke": {"value": "steelblue"},
                            "strokeWidth": {"value": 1.5}

                        }
                    }
                }
            ],
            "axes": [xAxis, yAxis]
            ,

            "marks": []
        }

        for (i = 0; i < chartConfig.yAxis.length; i++) {
            areaObj = {
                "type": "area",
                "key": xString,
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"scale": "x", "field": xString},
                        "interpolate": {"value": chartConfig.interpolationMode},
                        "y": {"scale": "y", "field": yStrings[i]},
                        "y2": {"scale": "y", "value": 0},
                        "fill": {"scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]},
                        "fillOpacity": {"value": 0.5}
                    },
                    "update": {
                        "fillOpacity": {"value": 0.5}

                    },
                    "hover": {
                        "fillOpacity": {"value": 0.2}
                    }

                }
            }

            lineObj = {
                "type": "line",
                "key": xString,
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"value": 400},
                        "interpolate": {"value": chartConfig.interpolationMode},
                        "y": {"scale": "y:prev", "field": yStrings[i]},
                        "stroke": {"scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]},
                        "strokeWidth": {"value": 1.5}
                    },
                    "update": {
                        "x": {"scale": "x", "field": xString},
                        "y": {"scale": "y", "field": yStrings[i]}
                    },
                    "exit": {
                        "x": {"value": -20},
                        "y": {"scale": "y", "field": yStrings[i]}
                    }
                }
            }


            pointObj = {
                "type": "symbol",
                "from": {"data": "table"},
                "properties": {
                    "enter": {
                        "x": {"scale": "x", "field": xString},
                        "y": {"scale": "y", "field": yStrings[i]},
                        "fill": {"scale": "color", "value": dataTable.metadata.names[chartConfig.yAxis[i]]},
                        "size": {"value": 50}
                        //"fillOpacity": {"value": 0.5}
                    },
                    "update": {
                        "size": {"value": 50}
                        //"size": {"scale":"r","field":rString},
                        // "stroke": {"value": "transparent"}
                    },
                    "hover": {
                        "size": {"value": 100},
                        "stroke": {"value": "white"}
                    }
                }
            }


            spec.marks.push(areaObj);

            spec.marks.push(pointObj);
            spec.marks.push(lineObj);
            spec.legends[0].values.push(dataTable.metadata.names[chartConfig.yAxis[i]])

        }


        chartObj.toolTipFunction = [];
        chartObj.toolTipFunction[0] = function (event, item) {

            a = 4

            console.log(tool, event, item);
            if (item.mark.marktype == 'symbol') {
                // window.alert(a);

                xVar = dataTable.metadata.names[chartConfig.xAxis]
                yVar = dataTable.metadata.names[chartConfig.yAxis]

                contentString = '<table><tr><td> X </td><td> (' + xVar + ') </td><td>' + item.datum.data[xVar] + '</td></tr>' + '<tr><td> Y </td><td> (' + yVar + ') </td><td>' + item.datum.data[yVar] + '</td></tr></table>';


                tool.html(contentString).style({
                    'left': event.pageX + 10 + 'px',
                    'top': event.pageY + 10 + 'px',
                    'opacity': 1
                })
                tool.selectAll('tr td').style('padding', "3px");
            }
        }

        chartObj.toolTipFunction[1] = function (event, item) {

            tool.html("").style({'left': event.pageX + 10 + 'px', 'top': event.pageY + 10 + 'px', 'opacity': 0})

        }

        chartObj.spec = spec;
        chartObj.toolTip = true;
        chartObj.spec = spec;

        chartObj.spec = spec;


    };

    igviz.drawStackedAreaChart = function (chartObj) {

        var chartConfig = chartObj.config;
        var dataTable = chartObj.dataTable;
        //  var table = setData(dataTable,chartConfig);
        divId = chartObj.canvas;


        areaString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.areaVar])
        yStrings = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis]);

        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis]);

        //     console.log(table,xString,yStrings,groupedBy);
        // sortDataSet(table);

        cat = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "cat",
            "range": "width",
            "field": xString,
            "padding": 0.2,
            "zero": false,
            "nice": true
        }


        val = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "val",
            "range": "height",
            "dataFrom": "stats",
            "field": "sum",
            "nice": true
        }


        var cScale = setScale(cat)
        var vScale = setScale(val);

        var xAxisConfig = {
            "type": "x",
            "scale": "cat",
            "angle": 0,
            "title": dataTable.metadata.names[chartConfig.xAxis],
            "grid": true,
            "dx": -10,
            "dy": 10,
            "align": "left",
            "titleDy": 10,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "val",
            "angle": 0,
            "title": dataTable.metadata.names[chartConfig.yAxis],
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -10,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);


        spec = {
            "width": chartConfig.width - 160,
            "height": chartConfig.height - 100,
            "padding": {"top": 10, "left": 60, "bottom": 60, "right": 100},
            "data": [
                {
                    "name": "table"
                },
                {
                    "name": "stats",
                    "source": "table",
                    "transform": [
                        {"type": "facet", "keys": [xString]},
                        {"type": "stats", "value": yStrings}
                    ]
                }
            ],
            "scales": [
                cScale,
                vScale,
                {
                    "name": "color",
                    "type": "ordinal",
                    "range": "category20"
                }
            ],
            "legends": [
                {
                    "orient": {"value": "right"},
                    "fill": "color",
                    "title": dataTable.metadata.names[chartConfig.areaVar
                        ],
                    "values": [],
                    "properties": {
                        "title": {
                            "fontSize": {"value": 14}
                        },
                        "labels": {
                            "fontSize": {"value": 12}
                        },
                        "symbols": {
                            "stroke": {"value": "transparent"}
                        },
                        "legend": {
                            "stroke": {"value": "steelblue"},
                            "strokeWidth": {"value": 0.5}


                        }
                    }
                }
            ],

            "axes": [
                xAxis, yAxis
            ],
            "marks": [
                {
                    "type": "group",
                    "from": {
                        "data": "table",
                        "transform": [
                            {"type": "facet", "keys": [areaString]},
                            {"type": "stack", "point": xString, "height": yStrings}
                        ]
                    },
                    "marks": [
                        {
                            "type": "area",
                            "properties": {
                                "enter": {
                                    "interpolate": {"value": "monotone"},
                                    "x": {"scale": "cat", "field": xString},
                                    "y": {"scale": "val", "field": "y"},
                                    "y2": {"scale": "val", "field": "y2"},
                                    "fill": {"scale": "color", "field": areaString},
                                    "fillOpacity": {"value": 0.8}

                                },
                                "update": {
                                    "fillOpacity": {"value": 0.8}
                                },
                                "hover": {
                                    "fillOpacity": {"value": 0.5}
                                }
                            }
                        },
                        {
                            "type": "line",
                            "properties": {
                                "enter": {
                                    "x": {"scale": "cat", "field": xString},
                                    //"x": {"value": 400},
                                    "interpolate": {"value": "monotone"},
                                    "y": {"scale": "val", "field": "y"},
                                    "stroke": {"scale": "color", "field": areaString},
                                    "strokeWidth": {"value": 3}
                                }
                            }
                        }
                    ]
                }
            ]
        }

        chartObj.spec = spec;
        chartObj.legend = true;
        chartObj.legendIndex = chartConfig.areaVar;


    }


    /*************************************************** Arc chart ***************************************************************************************************/


    igviz.drawArc = function (divId, chartConfig, dataTable) {

        radialProgress(divId)
            .label(dataTable.metadata.names[chartConfig.percentage])
            .diameter(200)
            .value(dataTable.data[0][chartConfig.percentage])
            .render();


        function radialProgress(parent) {
            var _data = null,
                _duration = 1000,
                _selection,
                _margin = {
                    top: 0,
                    right: 0,
                    bottom: 30,
                    left: 0
                },
                __width = chartConfig.width,
                __height = chartConfig.height,
                _diameter,
                _label = "",
                _fontSize = 10;


            var _mouseClick;

            var _value = 0,
                _minValue = 0,
                _maxValue = 100;

            var _currentArc = 0,
                _currentArc2 = 0,
                _currentValue = 0;

            var _arc = d3.svg.arc()
                .startAngle(0 * (Math.PI / 180)); //just radians

            var _arc2 = d3.svg.arc()
                .startAngle(0 * (Math.PI / 180))
                .endAngle(0); //just radians


            _selection = d3.select(parent);


            function component() {

                _selection.each(function (data) {

                    // Select the svg element, if it exists.
                    var svg = d3.select(this).selectAll("svg").data([data]);

                    var enter = svg.enter().append("svg").attr("class", "radial-svg").append("g");

                    measure();

                    svg.attr("width", __width)
                        .attr("height", __height);


                    var background = enter.append("g").attr("class", "component")
                        .attr("cursor", "pointer")
                        .on("click", onMouseClick);


                    _arc.endAngle(360 * (Math.PI / 180))

                    background.append("rect")
                        .attr("class", "background")
                        .attr("width", _width)
                        .attr("height", _height);

                    background.append("path")
                        .attr("transform", "translate(" + _width / 2 + "," + _width / 2 + ")")
                        .attr("d", _arc);

                    background.append("text")
                        .attr("class", "label")
                        .attr("transform", "translate(" + _width / 2 + "," + (_width + _fontSize) + ")")
                        .text(_label);

                    //outer g element that wraps all other elements
                    var gx = chartConfig.width / 2 - _width / 2;
                    var gy = (chartConfig.height / 2 - _height / 2) - 17;
                    var g = svg.select("g")
                        .attr("transform", "translate(" + gx + "," + gy + ")");


                    _arc.endAngle(_currentArc);
                    enter.append("g").attr("class", "arcs");
                    var path = svg.select(".arcs").selectAll(".arc").data(data);
                    path.enter().append("path")
                        .attr("class", "arc")
                        .attr("transform", "translate(" + _width / 2 + "," + _width / 2 + ")")
                        .attr("d", _arc);

                    //Another path in case we exceed 100%
                    var path2 = svg.select(".arcs").selectAll(".arc2").data(data);
                    path2.enter().append("path")
                        .attr("class", "arc2")
                        .attr("transform", "translate(" + _width / 2 + "," + _width / 2 + ")")
                        .attr("d", _arc2);


                    enter.append("g").attr("class", "labels");
                    var label = svg.select(".labels").selectAll(".labelArc").data(data);
                    label.enter().append("text")
                        .attr("class", "labelArc")
                        .attr("y", _width / 2 + _fontSize / 3)
                        .attr("x", _width / 2)
                        .attr("cursor", "pointer")
                        .attr("width", _width)
                        // .attr("x",(3*_fontSize/2))
                        .text(function (d) {
                            return Math.round((_value - _minValue) / (_maxValue - _minValue) * 100) + "%"
                        })
                        .style("font-size", _fontSize + "px")
                        .on("click", onMouseClick);

                    path.exit().transition().duration(500).attr("x", 1000).remove();


                    layout(svg);

                    function layout(svg) {

                        var ratio = (_value - _minValue) / (_maxValue - _minValue);
                        var endAngle = Math.min(360 * ratio, 360);
                        endAngle = endAngle * Math.PI / 180;

                        path.datum(endAngle);
                        path.transition().duration(_duration)
                            .attrTween("d", arcTween);

                        if (ratio > 1) {
                            path2.datum(Math.min(360 * (ratio - 1), 360) * Math.PI / 180);
                            path2.transition().delay(_duration).duration(_duration)
                                .attrTween("d", arcTween2);
                        }

                        label.datum(Math.round(ratio * 100));
                        label.transition().duration(_duration)
                            .tween("text", labelTween);

                    }

                });

                function onMouseClick(d) {
                    if (typeof _mouseClick == "function") {
                        _mouseClick.call();
                    }
                }
            }

            function labelTween(a) {
                var i = d3.interpolate(_currentValue, a);
                _currentValue = i(0);

                return function (t) {
                    _currentValue = i(t);
                    this.textContent = Math.round(i(t)) + "%";
                }
            }

            function arcTween(a) {
                var i = d3.interpolate(_currentArc, a);

                return function (t) {
                    _currentArc = i(t);
                    return _arc.endAngle(i(t))();
                };
            }

            function arcTween2(a) {
                var i = d3.interpolate(_currentArc2, a);

                return function (t) {
                    return _arc2.endAngle(i(t))();
                };
            }


            function measure() {
                _width = _diameter - _margin.right - _margin.left - _margin.top - _margin.bottom;
                _height = _width;
                _fontSize = _width * .2;
                _arc.outerRadius(_width / 2);
                _arc.innerRadius(_width / 2 * .85);
                _arc2.outerRadius(_width / 2 * .85);
                _arc2.innerRadius(_width / 2 * .85 - (_width / 2 * .15));
            }


            component.render = function () {
                measure();
                component();
                return component;
            }

            component.value = function (_) {
                if (!arguments.length) return _value;
                _value = [_];
                _selection.datum([_value]);
                return component;
            }


            component.margin = function (_) {
                if (!arguments.length) return _margin;
                _margin = _;
                return component;
            };

            component.diameter = function (_) {
                if (!arguments.length) return _diameter
                _diameter = _;
                return component;
            };

            component.minValue = function (_) {
                if (!arguments.length) return _minValue;
                _minValue = _;
                return component;
            };

            component.maxValue = function (_) {
                if (!arguments.length) return _maxValue;
                _maxValue = _;
                return component;
            };

            component.label = function (_) {
                if (!arguments.length) return _label;
                _label = _;
                return component;
            };

            component._duration = function (_) {
                if (!arguments.length) return _duration;
                _duration = _;
                return component;
            }

            component.onClick = function (_) {
                if (!arguments.length) return _mouseClick;
                _mouseClick = _;
                return component;
            }

            return component;

        };

    };


    /*************************************************** Scatter chart ***************************************************************************************************/

    igviz.drawScatterPlot = function (chartObj) {
        divId = chartObj.canvas;
        chartConfig = chartObj.config;
        dataTable = chartObj.dataTable;
        //    table=setData(dataTable,chartConfig)

        xString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.xAxis])
        yString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.yAxis])
        rString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.pointSize])
        cString = "data." + createAttributeNames(dataTable.metadata.names[chartConfig.pointColor])


        xScaleConfig = {
            "index": chartConfig.xAxis,
            "schema": dataTable.metadata,
            "name": "x",
            "range": "width",
            "zero": false,
            "field": xString

        }

        rScaleConfig = {
            "index": chartConfig.pointSize,
            "range": [0, 576],
            "schema": dataTable.metadata,
            "name": "r",
            "field": rString
        }
        cScaleConfig = {
            "index": chartConfig.pointColor,
            "schema": dataTable.metadata,
            "name": "c",
            "range": [chartConfig.minColor, chartConfig.maxColor],
            "field": cString
        }

        yScaleConfig = {
            "index": chartConfig.yAxis,
            "schema": dataTable.metadata,
            "name": "y",
            "range": "height",
            "nice": true,
            "field": yString
        }

        var xScale = setScale(xScaleConfig)
        var yScale = setScale(yScaleConfig);
        var rScale = setScale(rScaleConfig);
        var cScale = setScale(cScaleConfig)

        var xAxisConfig = {
            "type": "x",
            "scale": "x",
            "angle": -35,
            "title": dataTable.metadata.names[chartConfig.xAxis],
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": 25,
            "titleDx": 0
        }
        var yAxisConfig = {
            "type": "y",
            "scale": "y",
            "angle": 0,
            "title": "values",
            "grid": true,
            "dx": 0,
            "dy": 0,
            "align": "right",
            "titleDy": -30,
            "titleDx": 0
        }
        var xAxis = setAxis(xAxisConfig);
        var yAxis = setAxis(yAxisConfig);

        var spec = {
            "width": chartConfig.width - 130,
            "height": chartConfig.height,
            //"padding":{"top":40,"bottom":60,'left':60,"right":60},
            "data": [
                {
                    "name": "table"

                }
            ],
            "scales": [
                xScale, yScale,
                {
                    "name": "color", "type": "ordinal", "range": "category20"
                },
                rScale, cScale
            ],
            "axes": [xAxis, yAxis
            ],
            //"legends": [
            //    {
            //
            //        "orient": "right",
            //        "fill": "color",
            //        "title": "Legend",
            //        "values": [],
            //        "properties": {
            //            "title": {
            //                "fontSize": {"value": 14}
            //            },
            //            "labels": {
            //                "fontSize": {"value": 12}
            //            },
            //            "symbols": {
            //                "stroke": {"value": "transparent"}
            //            },
            //            "legend": {
            //                "stroke": {"value": "steelblue"},
            //                "strokeWidth": {"value": 1.5}
            //
            //            }
            //        }
            //    }],


            //    "scales": [
            //    {
            //        "name": "x",
            //        "nice": true,
            //        "range": "width",
            //        "domain": {"data": "iris", "field": "data.sepalWidth"}
            //    },
            //    {
            //        "name": "y",
            //        "nice": true,
            //        "range": "height",
            //        "domain": {"data": "iris", "field": "data.petalLength"}
            //    },
            //    {
            //        "name": "c",
            //        "type": "ordinal",
            //        "domain": {"data": "iris", "field": "data.species"},
            //        "range": ["#800", "#080", "#008"]
            //    }
            //],
            //    "axes": [
            //    {"type": "x", "scale": "x", "offset": 5, "ticks": 5, "title": "Sepal Width"},
            //    {"type": "y", "scale": "y", "offset": 5, "ticks": 5, "title": "Petal Length"}
            //],
            //    "legends": [
            //    {
            //        "fill": "c",
            //        "title": "Species",
            //        "offset": 0,
            //        "properties": {
            //            "symbols": {
            //                "fillOpacity": {"value": 0.5},
            //                "stroke": {"value": "transparent"}
            //            }
            //        }
            //    }
            //],
            "marks": [
                {
                    "type": "symbol",
                    "from": {"data": "table"},
                    "properties": {
                        "enter": {
                            "x": {"scale": "x", "field": xString},
                            "y": {"scale": "y", "field": yString},
                            "fill": {"scale": "c", "field": cString}
                            //"fillOpacity": {"value": 0.5}
                        },
                        "update": {
                            "size": {"scale": "r", "field": rString}
                            // "stroke": {"value": "transparent"}
                        },
                        "hover": {
                            "size": {"value": 300},
                            "stroke": {"value": "white"}
                        }
                    }
                }
            ]
        }
        chartObj.toolTipFunction = [];
        chartObj.toolTipFunction[0] = function (event, item) {
            console.log(tool, event, item);
            xVar = dataTable.metadata.names[chartConfig.xAxis]
            yVar = dataTable.metadata.names[chartConfig.yAxis]
            pSize = dataTable.metadata.names[chartConfig.pointSize]
            pColor = dataTable.metadata.names[chartConfig.pointColor]

            contentString = '<table><tr><td> X </td><td> (' + xVar + ') </td><td>' + item.datum.data[xVar] + '</td></tr>' + '<tr><td> Y </td><td> (' + yVar + ') </td><td>' + item.datum.data[yVar] + '</td></tr>' + '<tr><td> Size </td><td> (' + pSize + ') </td><td>' + item.datum.data[pSize] + '</td></tr>' + '<tr><td bgcolor="' + item.fill + '">&nbsp; </td><td> (' + pColor + ') </td><td>' + item.datum.data[pColor] + '</td></tr>' +
                '</table>';


            tool.html(contentString).style({
                'left': event.pageX + 10 + 'px',
                'top': event.pageY + 10 + 'px',
                'opacity': 1
            })
            tool.selectAll('tr td').style('padding', "3px");

        }

        chartObj.toolTipFunction[1] = function (event, item) {

            tool.html("").style({'left': event.pageX + 10 + 'px', 'top': event.pageY + 10 + 'px', 'opacity': 0})

        }

        chartObj.spec = spec;
        chartObj.toolTip = true;
    }


    /*************************************************** Single Number chart ***************************************************************************************************/

    igviz.drawSingleNumberDiagram = function (chartObj) {

        divId = chartObj.canvas;
        chartConfig = chartObj.config;
        dataTable = chartObj.dataTable;

        //Width and height
        var w = chartConfig.width;
        var h = chartConfig.height;
        var padding = chartConfig.padding;

        var svgID = divId + "_svg";
        //Remove current SVG if it is already there
        d3.select(svgID).remove();

        //Create SVG element
        singleNumSvg = d3.select(divId)
            .append("svg")
            .attr("id", svgID.replace("#", ""))
            .attr("width", w)
            .attr("height", h);


        singleNumSvg.append("rect")
            .attr("id", "rect")
            .attr("width", w)
            .attr("height", h);

        /*singleNumCurveSvg = d3.select(divId)
         .append("svg")
         .attr("id", svgID.replace("#",""))
         .attr("width", 207)
         .attr("height", 161);*/

    };


    /*************************************************** Table chart ***************************************************************************************************/

    var cnt = 0;

    igviz.drawTable = function (divId, chartConfig, dataTable) {

        //remove the current table if it is already exist
        d3.select(divId).select("table").remove();

        var rowLabel = dataTable.metadata.names;

        //append the Table to the div
        var table = d3.select(divId).append("table").attr('class', 'table table-bordered');

        //create the table head
        thead = table.append("thead");
        tbody = table.append("tbody")

        //Append the header to the table
        thead.append("tr")
            .selectAll("th")
            .data(rowLabel)
            .enter()
            .append("th")
            .text(function (d) {
                return d;
            });
    };

    /*************************************************** map ***************************************************************************************************/
    function loadWorldMapCodes() {
        var fileName = document.location.protocol + "//" + document.location.host + '/portal/geojson/countryInfo/';
        $.ajaxSetup({async: false});
        $.getJSON(fileName, function (json) {
            worldMapCodes = json;
        });
        $.ajaxSetup({async: true});
    }

    function loadUSAMapCodes() {
        var fileName = document.location.protocol + "//" + document.location.host + '/portal/geojson/usaInfo/';
        $.ajaxSetup({async: false});
        $.getJSON(fileName, function (json) {
            usaMapCodes = json;
        });
        $.ajaxSetup({async: true});
    }

    function getMapCode(name, region) {
        if (region == "usa") {
            $.each(usaMapCodes, function (i, location) {
                if (usaMapCodes[name] != null && usaMapCodes[name] != "") {
                    name = "US" + usaMapCodes[name];
                }
            });

        } else {
            $.each(worldMapCodes, function (i, location) {
                if (name.toUpperCase() == location["name"].toUpperCase()) {
                    name = location["alpha-3"];
                }
            });
        }
        return name;
    };

    igviz.drawMap = function (divId, chartConfig, dataTable) {

        var fileName;
        var width = chartConfig.width;
        var height = chartConfig.height;
        var xAxis = chartConfig.xAxis;
        var yAxis = chartConfig.yAxis;

        if (chartConfig.region == "usa") {
            fileName = document.location.protocol + "//" + document.location.host + '/portal/geojson/usa/';
            loadUSAMapCodes();
            mapChart = d3.geomap.choropleth()
                .geofile(fileName)
                .projection(d3.geo.albersUsa)
                .unitId(xAxis)
                .width(width)
                .height(height)
                .colors(colorbrewer.RdPu[chartConfig.legendGradientLevel])
                .column(yAxis)
                .scale([width / 1.1])
                .translate([width / 2, height / 2.2])
                .legend(true);


        } else {
            fileName = document.location.protocol + "//" + document.location.host + '/portal/geojson/world/';

            var scaleDivision = 5.5;
            var widthDivision = 2;
            var heightDivision = 2;

            if (chartConfig.region == "europe") {

                scaleDivision = width / height;
                widthDivision = 3;
                heightDivision = 0.8;

            }
            loadWorldMapCodes();
            mapChart = d3.geomap.choropleth()
                .geofile(fileName)
                .unitId(xAxis)
                .width(width)
                .height(height)
                .colors(colorbrewer.RdPu[chartConfig.legendGradientLevel])
                .column(yAxis)
                .scale([width / scaleDivision])
                .translate([width / widthDivision, height / heightDivision])
                .legend(true);
        }
    };


    /*************************************************** Bar chart Drill Dowining Function  ***************************************************************************************************/

    igviz.drillDown = function drillDown(index, divId, chartConfig, dataTable, originaltable) {
        //  console.log(dataTable,chartConfig,divId);
        if (index == 0) {
            d3.select(divId).append('div').attr({id: 'links', height: 20, 'bgcolor': 'blue'})
            d3.select(divId).append('div').attr({id: 'chartDiv'})
            chartConfig.height = chartConfig.height - 20;
            divId = "#chartDiv";
        }
        var currentChartConfig = JSON.parse(JSON.stringify(chartConfig));
        var current_x = 0;
        if (index < chartConfig.xAxis.length)
            current_x = chartConfig.xAxis[index].index
        else
            current_x = chartConfig.xAxis[index - 1].child;

        var current_y = chartConfig.yAxis;
        var currentData = {
            metadata: {
                names: [dataTable.metadata.names[current_x], dataTable.metadata.names[current_y]],
                types: [dataTable.metadata.types[current_x], dataTable.metadata.types[current_y]]
            },
            data: []
        }

        var tempData = [];
        for (i = 0; i < dataTable.data.length; i++) {
            name = dataTable.data[i][current_x];
            currentYvalue = dataTable.data[i][current_y];
            isFound = false;
            var j = 0;
            for (; j < tempData.length; j++) {
                if (tempData[j][0] === name) {
                    isFound = true;
                    break;
                }
            }
            if (isFound) {
                tempData[j][1] += currentYvalue;
                console.log(name, currentYvalue, tempData[j][1]);
            } else {
                console.log("create", name, currentYvalue);
                tempData.push([name, currentYvalue])
            }
        }

        currentData.data = tempData;
        currentChartConfig.xAxis = 0;
        currentChartConfig.yAxis = 1;
        currentChartConfig.chartType = 'bar';


        var x = this.setUp(divId, currentChartConfig, currentData);
        x.plot(currentData.data, function () {

            var filters = d3.select('#links .root').on('click', function () {
                d3.select("#links").html('');
                igviz.drillDown(0, divId, chartConfig, originaltable, originaltable);

            })


            var filters = d3.select('#links').selectAll('.filter');
            filters.on('click', function (d, i) {

                filtersList = filters.data();

                console.log(filtersList)
                var filterdDataset = [];
                var selectionObj = JSON.parse(JSON.stringify(originaltable));
                itr = 0;
                for (l = 0; l < originaltable.data.length; l++) {
                    isFiltered = true;
                    for (k = 0; k <= i; k++) {

                        if (originaltable.data[l][filtersList[k][0]] !== filtersList[k][1]) {
                            isFiltered = false;
                            break;
                        }
                    }
                    if (isFiltered) {
                        filterdDataset[itr++] = originaltable.data[l];
                    }

                }

                d3.selectAll('#links g').each(function (d, indx) {
                    if (indx > i) {
                        this.remove();
                    }
                })


                selectionObj.data = filterdDataset;

                igviz.drillDown(i + 1, divId, chartConfig, selectionObj, originaltable, true);


            });


            if (index < chartConfig.xAxis.length) {
                console.log(x);
                d3.select(x.chart._el).selectAll('g.type-rect rect').on('click', function (d, i) {
                    // console.log(d, i, this);
                    console.log(d, i);
                    var selectedName = d.datum.data[x.dataTable.metadata.names[x.config.xAxis]];
                    //  console.log(selectedName);
                    var selectedCurrentData = JSON.parse(JSON.stringify(dataTable));
                    var innerText;

                    var links = d3.select('#links').append('g').append('text').text(dataTable.metadata.names[current_x] + " : ").attr({

                        "font-size": "10px",
                        "x": 10,
                        "y": 20

                    });

                    d3.select('#links:first-child').selectAll('text').attr('class', 'root');

                    d3.select('#links g:last-child').append('span').data([[current_x, selectedName]]).attr('class', 'filter').text(selectedName + "  >  ")

                    var l = selectedCurrentData.data.length;
                    var newdata = [];
                    b = 0;
                    for (a = 0; a < l; a++) {
                        if (selectedCurrentData.data[a][current_x] === selectedName) {
                            newdata[b++] = selectedCurrentData.data[a];
                        }
                    }


                    selectedCurrentData.data = newdata;


                    igviz.drillDown(index + 1, divId, chartConfig, selectedCurrentData, originaltable, true);


                });

            }
        });


    }


    /*************************************************** Specification Generation method ***************************************************************************************************/


    function setScale(scaleConfig) {
        var scale = {"name": scaleConfig.name};

        console.log(scaleConfig.schema, scaleConfig.index);

        dataFrom = "table";

        scale.range = scaleConfig.range;


        switch (scaleConfig.schema.types[scaleConfig.index]) {
            case 'T':
                scale["type"] = 'time'

                break;

            case 'C':
                scale["type"] = 'ordinal'
                if (scale.name === "c") {
                    scale.range = "category20";
                }

                break;
            case 'N':
                scale["type"] = 'linear'

                break;
        }
        if (scaleConfig.hasOwnProperty("dataFrom")) {
            dataFrom = scaleConfig.dataFrom;
        }

        scale.range = scaleConfig.range;
        scale.domain = {"data": dataFrom, "field": scaleConfig.field}

        //optional attributes
        if (scaleConfig.hasOwnProperty("round")) {
            scale["round"] = scaleConfig.round;
        }

        if (scaleConfig.hasOwnProperty("nice")) {
            scale["nice"] = scaleConfig.nice;
        }

        if (scaleConfig.hasOwnProperty("padding")) {
            scale["padding"] = scaleConfig.padding;
        }

        if (scaleConfig.hasOwnProperty("reverse")) {
            scale["reverse"] = scaleConfig.reverse;
        }

        if (scaleConfig.hasOwnProperty("sort")) {
            scale["sort"] = scaleConfig.sort;
        }

        if (scale.name == 'x' && scale.type == 'linear') {
            scale.sort = true;
        }
        if (scaleConfig.hasOwnProperty("clamp")) {
            scale["clamp"] = scaleConfig.clamp;
        }


        if (scaleConfig.hasOwnProperty("zero")) {
            scale["zero"] = scaleConfig.zero;
        }
        console.log(scale);
        return scale;

    }

    function setAxis(axisConfig) {

        console.log("Axis", axisConfig);

        axis = {
            "type": axisConfig.type,
            "scale": axisConfig.scale,
            'title': axisConfig.title,
            "grid": axisConfig.grid,

            "properties": {
                "ticks": {
                    // "stroke": {"value": "steelblue"}
                },
                "majorTicks": {
                    "strokeWidth": {"value": 2}
                },
                "labels": {
                    // "fill": {"value": "steelblue"},
                    "angle": {"value": axisConfig.angle},
                    // "fontSize": {"value": 14},
                    "align": {"value": axisConfig.align},
                    "baseline": {"value": "middle"},
                    "dx": {"value": axisConfig.dx},
                    "dy": {"value": axisConfig.dy}
                },
                "title": {
                    "fontSize": {"value": 16},

                    "dx": {'value': axisConfig.titleDx},
                    "dy": {'value': axisConfig.titleDy}
                },
                "axis": {
                    "stroke": {"value": "#333"},
                    "strokeWidth": {"value": 1.5}
                }

            }

        }

        if (axisConfig.hasOwnProperty("tickSize")) {
            axis["tickSize"] = axisConfig.tickSize;
        }


        if (axisConfig.hasOwnProperty("tickPadding")) {
            axis["tickPadding"] = axisConfig.tickPadding;
        }

        console.log("SpecAxis", axis);
        return axis;
    }

    function setLegends(chartConfig, schema) {

    }

    function setData(dataTableObj, chartConfig, schema) {

        var table = [];
        for (i = 0; i < dataTableObj.length; i++) {
            var ptObj = {};
            namesArray = schema.names;
            for (j = 0; j < namesArray.length; j++) {
                if (schema.types[j] == 'T') {
                    ptObj[createAttributeNames(namesArray[j])] = new Date(dataTableObj[i][j]);
                } else
                    ptObj[createAttributeNames(namesArray[j])] = dataTableObj[i][j];
            }

            table[i] = ptObj;
        }

        return table;
    }

    function createAttributeNames(str) {
        return str.replace(' ', '_');
    }

    function setGenericAxis(axisConfig, spec) {
        MappingObj = {};
        MappingObj["tickSize"] = "tickSize";
        MappingObj["tickPadding"] = "tickPadding";
        MappingObj["title"] = "title";
        MappingObj["grid"] = "grid";
        MappingObj["offset"] = "offset";
        MappingObj["ticks"] = "ticks";

        MappingObj["labelColor"] = "fill";
        MappingObj["labelAngle"] = "angle";
        MappingObj["labelAlign"] = "align";
        MappingObj["labelFontSize"] = "fontSize";
        MappingObj["labelDx"] = "dx";
        MappingObj["labelDy"] = "dy";
        MappingObj["labelBaseLine"] = "baseline";

        MappingObj["titleDx"] = "dx";
        MappingObj["titleDy"] = "dy";
        MappingObj["titleFontSize"] = "fontSize";

        MappingObj["axisColor"] = "stroke";
        MappingObj["axisWidth"] = "strokeWidth";

        MappingObj["tickColor"] = "ticks.stroke";
        MappingObj["tickWidth"] = "ticks.strokeWidth";


        console.log("previous Axis", spec)
        for (var propt in axisConfig) {

            if (propt == "tickSize" || propt == "tickPadding")
                continue;

            if (axisConfig.hasOwnProperty(propt)) {

                if (propt.indexOf("label") == 0)
                    spec.properties.labels[MappingObj[propt]].value = axisConfig[propt];
                else if (propt.indexOf("ticks") == 0)
                    spec.properties.ticks[MappingObj[propt]].value = axisConfig[propt];
                else if (propt.indexOf("title") == 0)
                    spec.properties.title[MappingObj[propt]].value = axisConfig[propt];
                else if (propt.indexOf("axis") == 0)
                    spec.properties.axis[MappingObj[propt]].value = axisConfig[propt];
                else
                    spec[MappingObj[propt]] = axisConfig[propt];
            }
        }

        console.log("NEW SPEC", spec);
    }

    function createScales(dataset, chartConfig, dataTable) {
        //Create scale functions

        var xScale;
        var yScale;
        var colorScale;
        if (dataTable.metadata.types[chartConfig.xAxis] == 'N') {
            xScale = d3.scale.linear()
                .domain([0, d3.max(dataset, function (d) {
                    return d.data[d.config.xAxis];
                })])
                .range([chartConfig.padding, chartConfig.width - chartConfig.padding]);
        } else {
            xScale = d3.scale.ordinal()
                .domain(dataset.map(function (d) {
                    return d.data[chartConfig.xAxis];
                }))
                .rangeRoundBands([chartConfig.padding, chartConfig.width - chartConfig.padding], .1)
        }

        //TODO hanle case r and color are missing

        if (dataTable.metadata.types[chartConfig.yAxis] == 'N') {
            yScale = d3.scale.linear()
                .domain([0, d3.max(dataset, function (d) {
                    return d.data[d.config.yAxis];
                })])
                .range([chartConfig.height - chartConfig.padding, chartConfig.padding]);
            //var yScale = d3.scale.linear()
            //    .range([height, 0])
            //    .domain([0, d3.max(dataset, function(d) { return d.data[d.config.yAxis]; })])
        } else {
            yScale = d3.scale.ordinal()
                .rangeRoundBands([0, chartConfig.width], .1)
                .domain(dataset.map(function (d) {
                    return d.data[chartConfig.yAxis];
                }))
        }


        //this is used to scale the size of the point, it will value between 0-20
        var rScale = d3.scale.linear()
            .domain([0, d3.max(dataset, function (d) {
                return d.config.pointSize ? d.data[d.config.pointSize] : 20;
            })])
            .range([0, 20]);

        //TODO have to handle the case color scale is categorical : Done
        //http://synthesis.sbecker.net/articles/2012/07/16/learning-d3-part-6-scales-colors
        // add color to circles see https://www.dashingd3js.com/svg-basic-shapes-and-d3js
        //add legend http://zeroviscosity.com/d3-js-step-by-step/step-3-adding-a-legend
        if (dataTable.metadata.types[chartConfig.pointColor] == 'N') {
            colorScale = d3.scale.linear()
                .domain([-1, d3.max(dataset, function (d) {
                    return d.config.pointColor ? d.data[d.config.pointColor] : 20;
                })])
                .range([chartConfig.minColor, chartConfig.maxColor]);
        } else {
            colorScale = d3.scale.category20c();
        }

        //TODO add legend


        return {
            "xScale": xScale,
            "yScale": yScale,
            "rScale": rScale,
            "colorScale": colorScale
        }
    }


    /*************************************************** Util  functions ***************************************************************************************************/


    /**
     * Get the average of a numeric array
     * @param data
     * @returns average
     */
    function getAvg(data) {

        var sum = 0;

        for (var i = 0; i < data.length; i++) {
            sum = sum + data[i];
        }

        var average = (sum / data.length).toFixed(4);
        return average;
    }

    /**
     * Function to calculate the standard deviation
     * @param values
     * @returns sigma(standard deviation)
     */
    function standardDeviation(values) {
        var avg = getAvg(values);

        var squareDiffs = values.map(function (value) {
            var diff = value - avg;
            var sqrDiff = diff * diff;
            return sqrDiff;
        });

        var avgSquareDiff = getAvg(squareDiffs);

        var stdDev = Math.sqrt(avgSquareDiff);
        return stdDev;
    }

    /**
     * Get the p(x) : Helper function for the standard deviation
     * @param x
     * @param sigma
     * @param u
     * @returns {number|*}
     */
    function pX(x, sigma, u) {

        p = (1 / Math.sqrt(2 * Math.PI * sigma * sigma)) * Math.exp((-(x - u) * (x - u)) / (2 * sigma * sigma));

        return p;
    }


    /**
     * Get the normalized values for a list of elements
     * @param xVals
     * @returns {Array} of normalized values
     *
     */
    function NormalizationCoordinates(xVals) {

        var coordinates = [];

        var u = getAvg(xVals);
        var sigma = standardDeviation(xVals);

        for (var i = 0; i < xVals.length; i++) {

            coordinates[i] = {
                x: xVals[i],
                y: pX(xVals[i], sigma, u)
            };
        }

        return coordinates;
    }

    /**
     * This function will extract a column from a multi dimensional array
     * @param 2D array
     * @param index of column to be extracted
     * @return array of values
     */

    function parseColumnFrom2DArray(dataset, index) {

        var array = [];

        //console.log(dataset.length);
        //console.log(dataset[0].data);
        //console.log(dataset[1].data);

        for (var i = 0; i < dataset.length; i++) {
            array.push(dataset[i][index])
        }

        return array;
    }


    /*************************************************** Data Table Generation class ***************************************************************************************************/


        //DataTable that holds data in a tabular format
        //E.g var dataTable = new igviz.DataTable();
        //dataTable.addColumn("OrderId","C");
        //dataTable.addColumn("Amount","N");
        //dataTable.addRow(["12SS",1234.56]);
    igviz.DataTable = function (data) {
        this.metadata = {};
        this.metadata.names = [];
        this.metadata.types = [];
        this.data = [];
    };

    igviz.DataTable.prototype.addColumn = function (name, type) {
        this.metadata.names.push(name);
        this.metadata.types.push(type);
    };

    igviz.DataTable.prototype.addRow = function (row) {
        this.data.push(row);
    };

    igviz.DataTable.prototype.addRows = function (rows) {
        for (var i = 0; i < rows.length; i++) {
            this.data.push(rows[i]);
        }
        ;
    };

    igviz.DataTable.prototype.getColumnNames = function () {
        return this.metadata.names;
    };

    igviz.DataTable.prototype.getColumnByName = function (name) {
        var column = {};
        for (var i = 0; i < this.metadata.names.length; i++) {
            //TODO Need to check for case sensitiveness
            if (this.metadata.names[i] == name) {
                column.name = this.metadata.names[i];
                column.type = this.metadata.types[i];
                return column;
            }
        }
        ;
    };

    igviz.DataTable.prototype.getColumnByIndex = function (index) {
        var column = this.metadata.names[index];
        if (column) {
            column.name = column;
            column.type = this.metadata.types[index];
            return column;
        }

    };

    igviz.DataTable.prototype.getColumnData = function (columnIndex) {
        var data = [];
        this.data.map(function (d) {
            data.push(d[columnIndex]);
        });
        return data;
    };

    igviz.DataTable.prototype.toJSON = function () {
        console.log(this);
    };


    /*************************************************** Chart Class And API ***************************************************************************************************/


    function Chart(canvas, config, dataTable) {
        //this.chart=chart;
        this.dataTable = dataTable;
        this.config = config;
        this.canvas = canvas;
    }

    Chart.prototype.setXAxis = function (xAxisConfig) {

        /*
         *         axis=  {
         "type": axisConfig.type,
         "scale": axisConfig.scale,
         'title': axisConfig.title,
         "grid":axisConfig.grid,

         "properties": {
         "ticks": {
         // "stroke": {"value": "steelblue"}
         },
         "majorTicks": {
         "strokeWidth": {"value": 2}
         },
         "labels": {
         // "fill": {"value": "steelblue"},
         "angle": {"value": axisConfig.angle},
         // "fontSize": {"value": 14},
         "align": {"value": axisConfig.align},
         "baseline": {"value": "middle"},
         "dx": {"value": axisConfig.dx},
         "dy": {"value": axisConfig.dy}
         },
         "title": {
         "fontSize": {"value": 16},

         "dx":{'value':axisConfig.titleDx},
         "dy":{'value':axisConfig.titleDy}
         },
         "axis": {
         "stroke": {"value": "#333"},
         "strokeWidth": {"value": 1.5}
         }

         }

         }

         if (axisConfig.hasOwnProperty("tickSize")) {
         axis["tickSize"] = axisConfig.tickSize;
         }


         if (axisConfig.hasOwnProperty("tickPadding")) {
         axis["tickPadding"] = axisConfig.tickPadding;
         }
         */
        var xAxisSpec = this.spec.axes[0];
        setGenericAxis(xAxisConfig, xAxisSpec);
        /*xAxisConfig.tickSize
         xAxisConfig.tickPadding
         xAxisConfig.title;
         xAxisConfig.grid;
         xAxisConfig.offset
         xAxisConfig.ticks


         xAxisConfig.labelFill
         xAxisConfig.labelFontSize
         xAxisConfig.labelAngle
         xAxisConfig.labelAlign
         xAxisConfig.labelDx
         xAxisConfig.labelDy
         xAxisConfig.labelBaseLine;

         xAxisConfig.titleDx;
         xAxisConfig.titleDy
         xAxisConfig.titleFontSize;

         xAxisConfig.axisColor;
         xAxisConfig.axisWidth;

         xAxisConfig.tickColor;
         xAxisConfig.tickWidth;
         */


        return this;
    }

    Chart.prototype.setYAxis = function (yAxisConfig) {

        var yAxisSpec = this.spec.axes[1];
        setGenericAxis(yAxisConfig, yAxisSpec);

        return this;
    }

    Chart.prototype.setPadding = function (paddingConfig) {

        if (this.spec.padding == undefined) {
            this.spec.padding = {}
            this.spec.padding.top = 0;
            this.spec.padding.bottom = 0;
            this.spec.padding.left = 0;
            this.spec.padding.right = 0;
        }
        for (var propt in paddingConfig) {
            if (paddingConfig.hasOwnProperty(propt)) {

                this.spec.padding[propt] = paddingConfig[propt];
            }
        }

        this.spec.width = this.originalWidth - this.spec.padding.left - this.spec.padding.right;
        this.spec.height = this.originalHeight - this.spec.padding.top - this.spec.padding.bottom;

        return this;
    }

    Chart.prototype.unsetPadding = function () {
        delete this.spec.padding;
        this.spec.width = this.originalWidth;
        this.spec.height = this.originalHeight;
        return this;
    }

    Chart.prototype.setDimension = function (dimensionConfig) {

        if (dimensionConfig.width != undefined) {

            this.spec.width = dimensionConfig.width;
            this.originalWidth = dimensionConfig.width;

        }

        if (dimensionConfig.height != undefined) {

            this.spec.height = dimensionConfig.height;
            this.originalHeight = dimensionConfig.height;

        }

    }

    Chart.prototype.update = function (pointObj) {
        console.log("+++ Inside update");

        if (this.config.chartType == "map") {
            config = this.config;
            $.each(mapSVG[0][0].__data__, function (i, val) {
                if (mapSVG[0][0].__data__[i][config.xAxis] == "DEF") {
                    mapSVG[0][0].__data__.splice(i, 1);
                }
            });

            $.each(pointObj, function (i, val) {
                pointObj[i][config.xAxis] = getMapCode(pointObj[i][config.xAxis], config.region);
                mapSVG[0][0].__data__.push(pointObj[i]);
            });

            $(this.canvas).empty();
            d3.select(this.canvas).datum(mapSVG[0][0].__data__).call(mapChart.draw, mapChart);
        } else {

            if (persistedData.length >= maxValueForUpdate) {

                var newTable = setData([pointObj], this.config, this.dataTable.metadata);
                var point = this.table.shift();
                this.dataTable.data.shift();
                this.dataTable.data.push(pointObj);
                this.table.push(newTable[0]);

                if (this.config.chartType == "tabular" || this.config.chartType == "singleNumber") {
                    this.plot(persistedData, maxValueForUpdate);
                } else {
                    this.chart.data(this.data).update({"duration": 500});
                }
            } else {
                persistedData.push(pointObj);
                this.plot(persistedData, null);
            }
        }
    }

    Chart.prototype.updateList = function (dataList, callback) {
        console.log("+++ Inside updateList");

        for (i = 0; i < dataList.length; i++) {
            this.dataTable.data.shift();
            this.dataTable.data.push(dataList[i]);
        }

        var newTable = setData(dataList, this.config, this.dataTable.metadata);

        for (i = 0; i < dataList.length; i++) {
            var point = this.table.shift();
            this.table.push(newTable[i]);
        }

        //     console.log(point,this.chart,this.data);
        this.chart.data(this.data).update();

    }

    Chart.prototype.resize = function () {
        var ref = this;
        var newH = document.getElementById(ref.canvas.replace('#', '')).offsetHeight
        var newW = document.getElementById(ref.canvas.replace('#', '')).offsetWidth
        console.log("Resized", newH, newW, ref)

        var left = 0, top = 0, right = 0, bottom = 0;

        var w = ref.spec.width;
        var h = ref.spec.height;
        //if(ref.spec.padding==undefined)
        //{
        //    w=newW;
        //    h=newH;
        //
        //}
        // else {
        //
        //    if (ref.spec.padding.left!=undefined){
        //        left=ref.spec.padding.left;
        //
        //    }
        //
        //    if (ref.spec.padding.bottom!=undefined){
        //        bottom=ref.spec.padding.bottom;
        //
        //    }
        //    if (ref.spec.padding.top!=undefined){
        //        top=ref.spec.padding.top;
        //
        //    }
        //    if (ref.spec.padding.right!=undefined){
        //        right=ref.spec.padding.right;
        //
        //    }
        //    w=newW-left-right;
        //    h=newH-top-bottom;
        //
        //}
        console.log(w, h);
        ref.chart.width(w).height(h).renderer('svg').update({props: 'enter'}).update();

    }

    Chart.prototype.plot = function (dataset, callback, maxValue) {

        var config = this.config;

        if (config.chartType == "singleNumber") {

            //configure font sizes
            var MAX_FONT_SIZE = config.width * config.height * 0.0002;
            var AVG_FONT_SIZE = config.width * config.height * 0.0004;
            var MIN_FONT_SIZE = config.width * config.height * 0.0002;

            //div elements to append single number diagram components
            var minDiv = "minValue";
            var maxDiv = "maxValue";
            var avgDiv = "avgValue";

            //removing if already exist group element
            singleNumSvg.select("#groupid").remove();
            //appending a group to the diagram
            var SingleNumberDiagram = singleNumSvg
                .append("g").attr("id", "groupid");

            if (maxValue !== undefined) {

                if (dataset.length >= maxValue) {
                    var allowedDataSet = [];
                    var startingPoint = dataset.length - maxValue;
                    for (var i = startingPoint; i < dataset.length; i++) {
                        allowedDataSet.push(dataset[i]);
                    }
                    dataset = allowedDataSet;
                } else {
                    maxValueForUpdate = maxValue;
                    persistedData = dataset;
                }
            }

            //  getting a reference to the data
            var tableData = dataset;
            var table = setData(dataset, this.config, this.dataTable.metadata);
            var data = {table: table}
            this.data = data;
            this.table = table;


            var datamap = tableData.map(function (d) {
                return {
                    "data": d,
                    "config": config
                }
            });

            //parse a column to calculate the data for the single number diagram
            var selectedColumn = parseColumnFrom2DArray(tableData, config.xAxis);


            //Minimum value goes here

            SingleNumberDiagram.append("text")
                .attr("id", minDiv)
                .text("Max: " + d3.max(selectedColumn))
                //.text(50)
                .attr("font-size", MIN_FONT_SIZE)
                .attr("x", 3 * config.width / 4)
                .attr("y", config.height / 4)
                .style("fill", "black")
                .style("text-anchor", "middle")
                .style("lignment-baseline", "middle")
            ;

            //Average value goes here
            SingleNumberDiagram.append("text")
                .attr("id", avgDiv)
                .text(getAvg(selectedColumn))
                .attr("font-size", AVG_FONT_SIZE)
                .attr("x", config.width / 2)
                .attr("y", config.height / 2 + d3.select("#" + avgDiv).attr("font-size") / 5)
                .style("fill", "black")
                .style("text-anchor", "middle")
                .style("lignment-baseline", "middle")
            ;

            //Maximum value goes here
            SingleNumberDiagram.append("text")
                .attr("id", maxDiv)
                .text("Min: " + d3.min(selectedColumn))
                .attr("font-size", MAX_FONT_SIZE)
                .attr("x", 3 * config.width / 4)
                .attr("y", 3 * config.height / 4)
                .style("fill", "black")
                .style("text-anchor", "middle")
                .style("lignment-baseline", "middle")
            ;

            //constructing curve

            var margin = {top: 10, right: 10, bottom: 10, left: 0};
            var width = config.width * 0.305 - margin.left - margin.right;
            var height = config.height * 0.5 - margin.top - margin.bottom;

            singleNumSvg.append("rect")
                .attr("id", "rectCurve")
                .attr("x", 3)
                .attr("y", config.height * 0.5)
                .attr("width", config.width * 0.305)
                .attr("height", config.height * 0.5);

            var normalizedCoordinates = NormalizationCoordinates(selectedColumn.sort(function (a, b) {
                return a - b
            }));
            //console.log(normalizedCoordinates);


            // Set the ranges
            var x = d3.time.scale().range([0, config.width * 0.305]);
            var y = d3.scale.linear().range([config.height * 0.5, 0]);

            // Define the x axis
            var xAxis = d3.svg.axis().scale(x)
                .orient("bottom").ticks(0);


            // Define the line
            var valueLines = d3.svg.line()
                .x(function (d) {
                    return x(d.x);
                })
                .y(function (d) {
                    return y(d.y);
                });

            //removing if already exist group element
            singleNumSvg.select("#curvegroupid").remove();
            // Adds the svg canvas
            var normalizationCurve = singleNumSvg
                .append("g").attr("id", "curvegroupid")
                .attr("transform", "translate(" + 2 + "," + ((config.height * 0.5) + 4) + ")");

            // Scale the range of the data
            x.domain(d3.extent(normalizedCoordinates, function (d) {
                return d.x;
            }));
            y.domain([0, d3.max(normalizedCoordinates, function (d) {
                return d.y;
            })]);

            // Add the valueLines path.
            normalizationCurve.append("path")
                .attr("class", "line")
                .transition()
                .attr("d", valueLines(normalizedCoordinates))
                .delay(function (d, i) {
                    return i * 100;
                })
                .duration(10000)
                .ease('linear');
            ;

            // Add the X Axis
            normalizationCurve.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(1," + ((config.height * 0.5) - 8) + ")")
                .call(xAxis)
            ;

        } else if (config.chartType == "map") {

            $.each(dataset, function (i, val) {
                dataset[i][config.xAxis] = getMapCode(dataset[i][config.xAxis], config.region);
            });

            var defaultRow = jQuery.extend({}, dataset[0])
            defaultRow[config.xAxis] = "DEF";
            defaultRow[config.yAxis] = 0;

            dataset.push(defaultRow);
            mapSVG = d3.select(this.canvas).datum(dataset).call(mapChart.draw, mapChart);

        } else if (config.chartType == "tabular") {

            var isColorBasedSet = this.config.colorBasedStyle;
            var isFontBasedSet = this.config.fontBasedStyle;

            if (maxValue !== undefined) {

                if (dataset.length >= maxValue) {
                    var allowedDataSet = [];
                    var startingPoint = dataset.length - maxValue;
                    for (var i = startingPoint; i < dataset.length; i++) {
                        allowedDataSet.push(dataset[i]);
                    }
                    dataset = allowedDataSet;
                } else {
                    maxValueForUpdate = maxValue;
                    persistedData = dataset;
                }
            }

            var tableData = dataset;
            tableData.reverse();

            var table = setData(dataset, this.config, this.dataTable.metadata);
            var data = {table: table}
            this.data = data;
            this.table = table;

            //Using RGB color code to represent colors
            //Because the alpha() function use these property change the contrast of the color
            var colors = [{
                r: 255,
                g: 0,
                b: 0
            }, {
                r: 0,
                g: 255,
                b: 0
            }, {
                r: 200,
                g: 100,
                b: 100
            }, {
                r: 200,
                g: 255,
                b: 250
            }, {
                r: 255,
                g: 140,
                b: 100
            }, {
                r: 230,
                g: 100,
                b: 250
            }, {
                r: 0,
                g: 138,
                b: 230
            }, {
                r: 165,
                g: 42,
                b: 42
            }, {
                r: 127,
                g: 0,
                b: 255
            }, {
                r: 0,
                g: 255,
                b: 255
            }];

            //function to change the color depth
            //default domain is set to [0, 100], but it can be changed according to the dataset
            var alpha = d3.scale.linear().domain([0, 100]).range([0, 1]);

            var colorRows = d3.scale.linear()
                .domain([2.5, 4])
                .range(['#F5BFE8', '#E305AF']);

            var fontSize = d3.scale.linear()
                .domain([0, 100])
                .range([15, 20]);


            var rows = tbody.selectAll("tr")
                .data(tableData);

            rows.enter()
                .append("tr");
            rows.exit().remove();

            rows.order();

            var cells;

            if (isColorBasedSet == true && isFontBasedSet == true) {

                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                    return d;
                })
                    .style("font-size", function (d, i) {
                        fontSize.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);
                        return fontSize(d) + "px";
                    })
                    .style('background-color', function (d, i) {

                        //This is where the color is decided for the cell
                        //The domain set according to the data set we have now
                        //Minimum & maximum values for the particular data column is used as the domain
                        alpha.domain([d3.min(parseColumnFrom2DArray(tableData, i)), d3.max(parseColumnFrom2DArray(tableData, i))]);

                        //return the color for the cell
                        return 'rgba(' + colors[i].r + ',' + colors[i].g + ',' + colors[i].b + ',' + alpha(d) + ')';

                    });

            } else if (isColorBasedSet && !isFontBasedSet) {
                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                    return d;
                })
                    .style('background-color', function (d, i) {

                        //This is where the color is decided for the cell
                        //The domain set according to the data set we have now
                        //Minimum & maximum values for the particular data column is used as the domain
                        alpha.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);

                        //return the color for the cell
                        return 'rgba(' + colors[i].r + ',' + colors[i].g + ',' + colors[i].b + ',' + alpha(d) + ')';

                    });

            } else if (!isColorBasedSet && isFontBasedSet) {

                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                    return d;
                })
                    .style("font-size", function (d, i) {
                        fontSize.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);
                        return fontSize(d) + "px";
                    });

            } else {
                //appending the rows inside the table body
                rows.style('background-color', function (d, i) {

                    colorRows.domain([
                        d3.min(parseColumnFrom2DArray(tableData, config.xAxis)),
                        d3.max(parseColumnFrom2DArray(tableData, config.xAxis))
                    ]);
                    return colorRows(d[config.xAxis]);
                })
                    .style("font-size", function (d, i) {

                        fontSize.domain([
                            d3.min(parseColumnFrom2DArray(tableData, i)),
                            d3.max(parseColumnFrom2DArray(tableData, i))
                        ]);
                        return fontSize(d) + "px";
                    });

                //adding the  data to the table rows
                cells = rows.selectAll("td")
                    .data(function (d, i) {

                        return d;
                    });

                cells.enter()
                    .append("td");

                cells.text(function (d, i) {
                    return d;
                });
            }
            tableData.reverse();
        } else {
            if (maxValue !== undefined) {
                if (dataset.length >= maxValue) {
                    var allowedDataSet = [];
                    var startingPoint = dataset.length - maxValue;
                    for (var i = startingPoint; i < dataset.length; i++) {
                        allowedDataSet.push(dataset[i]);
                    }
                    dataset = allowedDataSet;
                } else {
                    maxValueForUpdate = maxValue;
                    persistedData = dataset;
                }
            }

            var table = setData(dataset, this.config, this.dataTable.metadata);
            var data = {table: table}

            var divId = this.canvas;
            this.data = data;
            this.table = table;


            if (this.legend) {
                legendsList = [];
                for (i = 0; i < dataset.length; i++) {
                    a = dataset[i][this.legendIndex]
                    isfound = false;
                    for (j = 0; j < legendsList.length; j++) {
                        if (a == legendsList[j]) {
                            isfound = true;
                            break;
                        }
                    }

                    if (!isfound) {
                        legendsList.push(a);
                    }
                }

                this.spec.legends[0].values = legendsList;
            }

            var specification = this.spec;
            var isTool = this.toolTip;
            var toolTipFunction = this.toolTipFunction

            var ref = this

            vg.parse.spec(specification, function (chart) {
                ref.chart = chart({
                    el: divId,
                    renderer: 'svg',
                    data: data
                }).update();

                if (isTool) {

                    tool = d3.select('body').append('div').style({
                        'position': 'absolute',
                        'opacity': 0,
                        'padding': "4px",
                        'border': "2px solid ",
                        'background': 'white'
                    });
                    ref.chart.on('mouseover', toolTipFunction[0]);
                    ref.chart.on('mouseout', toolTipFunction[1]);
                }

                if (callback) {
                    callback.call(ref);
                }
            });
            console.log(this);
        }


    }


})();