<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->

<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Data"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Service"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Link"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.beans.Story"%>
<%@ page import="org.wso2.stratos.manager.feature.dashbord.ui.utils.Utils"%>

<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.Map"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="serviceData" class="org.wso2.stratos.manager.feature.dashbord.ui.beans.Data"
             scope="session"></jsp:useBean>
<%
	serviceData = Utils.pupulateDashboardFeatures();
	request.getSession().setAttribute("serviceData", serviceData);

	// Data data = Utils.pupulateDashboardFeatures();
	Map<String, Service> allServices = serviceData.getServices();
	String serviceNames[] = serviceData.getServiceNames();
	String keys[] = serviceData.getKeys();
%>
<script type="text/javascript">
 function openServer(url) {
	 window.open(url,'_newtab')  ;
 }
</script>


<!DOCTYPE HTML>
<html>
<head>
    <title>Dashboard</title>
    <link type="text/css" href="../../../../features-dashboard/tenant-dashboard/css/features-dashboard-new.css" rel="stylesheet"/>
    <!--[if IE 7]>
	    <link rel="stylesheet" type="text/css" href="../../../../features-dashboard/tenant-dashboard/css/ie7.css">
    <![endif]-->
    <script type="text/javascript" src="../../../../features-dashboard/tenant-dashboard/js/jquery-1.7.1.min.js"></script>
    <script type="text/javascript" src="../../../../features-dashboard/tenant-dashboard/js/jquery.masonry.min.js"></script>

    <script type="text/javascript">
        var def = 'manager'; //This is the default section to display images
        var hideOnMouseLeave = false; //Set this to true if you want to hide the popup on mouse out from the popup

        var nameObj = {
        		<%for (int i = 0; i < serviceNames.length; i++) {
				String name = serviceNames[i];
				String key = keys[i];
				if (i == serviceNames.length - 1) {%><%=key%>:'<%=name%>'
				<%} else {%><%=key%>:'<%=name%>',
				<%}%>
        		<%}%>
        };



        $(document).ready(function() {
            for (var name in nameObj) {
                $.ajax({
                    url:'../../../../features-dashboard/dashboards/server.jsp?name='+name,
                    async:false,
                    success:function(data){
                        $('#container').append($('<div id="'+name + '_content"'+'>'+data+'</div>').hide());
                    }
                });
            }

            //Showing title and content for the default manager div
                $('#db_content_title').html(nameObj[def]);
                $('#' + def + '_info').parent().addClass("selected");
//                $('#page-popup').addClass("page-popup");
            var loadDefalut = function() {
                $('#db_content_title').show();
                var container = $('#' + def + '_content');
                container.show(function() {
                    container.imagesLoaded(function() {
                        container.masonry({
                            itemSelector : '.story',
                            columnWidth : 250,
                            isAnimated: true,
                            animationOptions: {
                                duration: 750,
                                easing: 'linear',
                                queue: false
                            }
                        });
                    });
                });
            };
            loadDefalut();

            //Handle mouse over event
            $('.db_menu li a.service-menu-left').click(
                    function() {
                        //Handle the menu styles
                        $('.db_menu li').removeClass("selected");


                        //Get the relevent catagory from the id of the link that has been mouse overred.
                        var cat = this.id.split('_')[0];

                        var href = $('a.goto-link1',$(this).parent().parent()).attr('href');

                        //hide all content and show and init the  mansonry for the relevent one
                        $('#container > div').hide();
                        $('#db_content_title').show();
						
						if(cat == "manager"){
							$('#db_content_title').html('<div class="goto-link2-prev">'+nameObj[cat]+'</div>');
						}else{
							$('#db_content_title').html('<div class="goto-link2-prev">'+nameObj[cat]+'</div><a class="goto-link2" href="'+href+'" target="_blank"></a>');
						}
						
					
                        if(cat == def){
                            $('#page-popup').removeClass("page-popup");
							 $(this.parentNode).addClass("selected");
                        }else{

                            //Show the gray box and register an event handler to remove it when the mouse is ouside
                            $('#page-popup').addClass("page-popup");
                            $(this.parentNode.parentNode).addClass("selected");

                        }

                        var container = $('#' + cat + "_content");
                        $('#' + cat + '_content').show();
                        $('#' + cat + '_content div.story').die();
                        $('#' + cat + '_content').masonry({
                          itemSelector: '.story',
                          columnWidth: 250,
                            isAnimated: true,
                                        animationOptions: {
                                            duration: 750,
                                            easing: 'linear',
                                            queue: false
                                        }
                        });
                    }
                    );
        });


    </script>
</head>
<body>

<div class="page-background">
<div class="page-picture">
<div id="page-popup">
    <table class="db_table">
        <tr>
            <div class="main-title">Services</div>
            <td class="db_menu">
                <ul>
                  <li class="manager-leftBar"><a class="service-menu-left" id="manager_info">Cloud Services</a></li>
                  <%

                  	for (Map.Entry<String, Service> entry : allServices.entrySet()) {
                  		Service myservice = entry.getValue();
                  		String name = myservice.getName();
                  		String link = myservice.getLink();
                  		String id = myservice.getKey()+"_info";
                  		if (!id.equals("manager_info")) {
                  %>
                          <li class="<%=myservice.getKey()%>-leftBar"><div class="service-menu-left-wrapper"><a class="service-menu-left" id="<%=id%>"><%=name%></a></div><div class="goto-link1-wrapper"><a href="<%=link%>" target="_blank" class="goto-link1"></a></div><div style="clear:both"></div> </li>

                    <%
                     	                    	}}
                     	                    %>
                </ul>

            </td>
            <td class="db_content">
                <div class="popup-circle">
                    <div id="db_content_title"></div>
                    <div id="container"></div>
                </div>
            </td>
        </tr>
    </table>
</div>
</div>
</div>


</html>
