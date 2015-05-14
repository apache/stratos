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

// Variable to store the current mode of input
var isForm = true;

// Variable to store height of the title text
var titleHeight = $('#centered').height() - $('#content').height();

/**
 * Function to change the animate the change in height of the box
 */
function changeBoxHeight () {
	window.setTimeout(function () {
		var newContentHeight = $('#content').height();
		var newHeight = titleHeight + newContentHeight;
		$('#centered').animate({
			'height': newHeight
		}, 400);
	}, 350);
}

// Binding functions to collapsed box opening and closing
//$('.subform').on('hidden.bs.collapse', changeBoxHeight);
//$('.subform').on('shown.bs.collapse', changeBoxHeight);



/**
 * Function to change the css display mode of the elements
 * @param  {String} element1 Element identifier #1 (this is the element that is hidden)
 * @param  {String} element2 Element identifier #1 (this is the element that is shown)
 */
function changeDisplayMode (element1, element2) {
	$(element1).css('display', 'none');
	$(element2).css('display', 'inline');
}

/**
 * Handling editor view switching
 */
var jsonSkeleton;
var currentJson;

$(document).ready(function() {
    $('#list').click(function(event){
        $(this).addClass('active');
        $('#grid').removeClass('active');
       // event.preventDefault();
        $('.general-table .block').addClass('list-group-item');
        $('.general-table .block').removeClass('grid-group-item');
        $('.general-table .block .list-button').css("display","block");
    });
    $('#grid').click(function(event){
        //event.preventDefault();
        $(this).addClass('active');
        $('#list').removeClass('active');
        $('.general-table .block').removeClass('list-group-item');
        $('.general-table .block').addClass('grid-group-item');
        $('.general-table .block .list-button').css({"display":"none"});
    });

});


function toolbar_top() {
    var window_top = $(window).scrollTop();
    var div_top = $('.title-main').offset().top;
    if (window_top > div_top) {
        $('.form-toolbar').addClass('stick-to-top container');
    } else {
        $('.form-toolbar').removeClass('stick-to-top container');
    }
}

$(function () {
    $(window).scroll(toolbar_top);
    toolbar_top();
});

//changeBoxHeight();