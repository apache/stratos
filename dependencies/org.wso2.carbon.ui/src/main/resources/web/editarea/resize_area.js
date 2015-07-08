	
	/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

EditAreaLoader.prototype.start_resize_area= function(){
		var d=document,a,div,width,height,father;
		
		d.onmouseup= editAreaLoader.end_resize_area;
		d.onmousemove= editAreaLoader.resize_area;
		editAreaLoader.toggle(editAreaLoader.resize["id"]);		
		
		a	= editAreas[editAreaLoader.resize["id"]]["textarea"];
		div	= d.getElementById("edit_area_resize");
		if(!div){
			div= d.createElement("div");
			div.id="edit_area_resize";
			div.style.border="dashed #888888 1px";
		}
		width	= a.offsetWidth -2;
		height	= a.offsetHeight -2;
		
		div.style.display	= "block";
		div.style.width		= width+"px";
		div.style.height	= height+"px";
		father= a.parentNode;
		father.insertBefore(div, a);
		
		a.style.display="none";
				
		editAreaLoader.resize["start_top"]= calculeOffsetTop(div);
		editAreaLoader.resize["start_left"]= calculeOffsetLeft(div);		
	};
	
	EditAreaLoader.prototype.end_resize_area= function(e){
		var d=document,div,a,width,height;
		
		d.onmouseup="";
		d.onmousemove="";		
		
		div		= d.getElementById("edit_area_resize");		
		a= editAreas[editAreaLoader.resize["id"]]["textarea"];
		width	= Math.max(editAreas[editAreaLoader.resize["id"]]["settings"]["min_width"], div.offsetWidth-4);
		height	= Math.max(editAreas[editAreaLoader.resize["id"]]["settings"]["min_height"], div.offsetHeight-4);
		if(editAreaLoader.isIE==6){
			width-=2;
			height-=2;	
		}
		a.style.width		= width+"px";
		a.style.height		= height+"px";
		div.style.display	= "none";
		a.style.display		= "inline";
		a.selectionStart	= editAreaLoader.resize["selectionStart"];
		a.selectionEnd		= editAreaLoader.resize["selectionEnd"];
		editAreaLoader.toggle(editAreaLoader.resize["id"]);
		
		return false;
	};
	
	EditAreaLoader.prototype.resize_area= function(e){		
		var allow,newHeight,newWidth;
		allow	= editAreas[editAreaLoader.resize["id"]]["settings"]["allow_resize"];
		if(allow=="both" || allow=="y")
		{
			newHeight	= Math.max(20, getMouseY(e)- editAreaLoader.resize["start_top"]);
			document.getElementById("edit_area_resize").style.height= newHeight+"px";
		}
		if(allow=="both" || allow=="x")
		{
			newWidth= Math.max(20, getMouseX(e)- editAreaLoader.resize["start_left"]);
			document.getElementById("edit_area_resize").style.width= newWidth+"px";
		}
		
		return false;
	};
	
	editAreaLoader.waiting_loading["resize_area.js"]= "loaded";
