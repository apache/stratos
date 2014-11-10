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
        event.preventDefault();
        $('.general-table .block').addClass('list-group-item');
        $('.general-table .block').removeClass('grid-group-item');
    });
    $('#grid').click(function(event){
        event.preventDefault();
        $('.general-table .block').removeClass('list-group-item');
        $('.general-table .block').addClass('grid-group-item');
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