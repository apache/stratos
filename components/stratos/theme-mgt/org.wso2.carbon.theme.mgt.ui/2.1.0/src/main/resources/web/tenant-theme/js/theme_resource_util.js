var myEditor = null;
function loadThemeResourcePage(path, viewMode, consumerID, targetDiv) {
    return loadThemeJSPPage('theme_advanced', path, viewMode, consumerID, targetDiv);
}

function loadThemeJSPPage(pagePrefixName, path, viewMode, consumerID, targetDiv) {
    if (viewMode != undefined && viewMode != null && 'inlined' == viewMode) {
        pagePrefixName = "resource";
        var suffix = "&resourceViewMode=inlined";
        if (consumerID != undefined && consumerID != null && consumerID != "") {
            suffix += "&resourcePathConsumer=" + consumerID;
        }

        if (targetDiv == null || targetDiv == undefined || targetDiv == "" || targetDiv == "null") {
            targetDiv = 'registryBrowser';
        }

        suffix += "&targetDivID=" + targetDiv;

        if (path == "#") {
            path = '/';
        }

        var url = '../tenant-theme/' + pagePrefixName + '_ajaxprocessor.jsp?path=' + path + suffix;

        /* document.getElementById(targetDiv).style.display = ""; */
        jQuery("#popupContent").load(url, null,
                function(res, status, t) {
                    if (status != "success") {
                        CARBON.showWarningDialog(org_wso2_carbon_registry_resource_ui_jsi18n["error.occured"]);
                    }
                    // Add necessary logic to handle these scenarios if needed
                    if (res || t) {}
                });
        return false;
    } else {
        document.location.href = '../tenant-theme/' + pagePrefixName + '.jsp?path=' + path + "&screenWidth=" + screen.width;
    }
    return true;
}

function submitImportThemeContentForm() {

    var rForm = document.forms["resourceImportForm"];
    if (!rForm.redirectWith || rForm.redirectWith.value == "") {
        sessionAwareFunction(function() {}, org_wso2_carbon_registry_resource_ui_jsi18n["session.timed.out"]);
    }

    var reason = "";
    reason += validateEmpty(rForm.fetchURL, org_wso2_carbon_registry_resource_ui_jsi18n["url"]);
    if (reason == "") {
        reason += validateEmpty(rForm.resourceName, org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    }
    if (reason == "") {
        reason += validateIllegal(rForm.resourceName, org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    }
    if (reason == "") {
        reason += validateImage(rForm.resourceName);
    }
    if (reason == "") {
        reason += validateResourcePathAndLength(rForm.resourceName);
    }
    if (reason == "") {
        reason += validateForInput(rForm.mediaType, org_wso2_carbon_registry_resource_ui_jsi18n["media.type"]);
    }
    /*if (reason == "") {
        reason += validateForInput(rForm.description, org_wso2_carbon_registry_resource_ui_jsi18n["description"]);
    }
    if (reason == "") {
        reason += validateDescriptionLength(rForm.description);
    } */
    var resourcePath= rForm.path.value + '/' + rForm.resourceName.value;
    resourcePath = resourcePath.replace("//", "/");
    if (reason == "") {
        reason += validateExists(resourcePath);
    }

    if (reason != "") {
        CARBON.showWarningDialog(reason);
        if (document.getElementById('add-resource-div')) {
            document.getElementById('add-resource-div').style.display = "";
        }
        document.getElementById('whileUpload').style.display = "none";
        return false;
    }

    var parentPath = document.getElementById('irParentPath').value;
    var resourceName = document.getElementById('irResourceName').value;
    var mediaType = document.getElementById('irMediaType').value;
    var description = ""; //document.getElementById('irDescription').value;
    var fetchURL = document.getElementById('irFetchURL').value;
    var redirectWith = "";
    if (document.getElementById('irRedirectWith')) {
        redirectWith = document.getElementById('irRedirectWith').value;
    }

    var isAsync = "false";

    // If this is a wsdl we need to make a async call to make sure we dont timeout cause wsdl
    // validation takes long.
    var params;
    if ((mediaType == "application/wsdl+xml") || (mediaType == "application/x-xsd+xml")  || (mediaType == "application/policy+xml")) {
//                    isAsync = "true";
        params = {parentPath: parentPath, resourceName: resourceName,
            mediaType: mediaType, description: description, fetchURL: fetchURL,
            isAsync : isAsync, symlinkLocation: parentPath, redirectWith: redirectWith};
    } else {
        params = {parentPath: parentPath, resourceName: resourceName,
            mediaType: mediaType, description: description, fetchURL: fetchURL,
            isAsync : isAsync, redirectWith: redirectWith};
    }

    new Ajax.Request('../tenant-theme/import_resource_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: params,

        onSuccess: function() {
            refreshThemeMetadataSection(parentPath);
            refreshThemeContentSection(parentPath);
            //document.getElementById('add-resource-div').style.display = "";
            document.getElementById('whileUpload').style.display = "none";
            CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["successfully.uploaded"],function(){
                location.reload(true);
            });

        },

        onFailure: function() {
            refreshThemeMetadataSection(parentPath);
            refreshThemeContentSection(parentPath);
            document.getElementById('whileUpload').style.display = "none";
            CARBON.showErrorDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unable.to.upload"]);
        }
    });
}


function submitThemeTextContentForm() {
    var rForm = document.forms["textContentForm"];
    /* Validate the form before submit */

    var reason = "";
    reason += validateEmpty(rForm.filename, org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    if (reason == "") {
        reason += validateForInput(rForm.filename, org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    }
    if (reason == "") {
        reason += validateIllegal(rForm.filename, org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    }
    if (reason == "") {
        reason += validateResourcePathAndLength(rForm.filename);
    }
    if (reason == "") {
        reason += validateForInput(rForm.mediaType, org_wso2_carbon_registry_resource_ui_jsi18n["media.type"]);
    }
    /*if (reason == "") {
        reason += validateForInput(rForm.description, org_wso2_carbon_registry_resource_ui_jsi18n["description"]);
    }
    if (reason == "") {
        reason += validateDescriptionLength(rForm.description);
    } */
    var resourcePath= rForm.path.value + '/' + rForm.filename.value;
    resourcePath = resourcePath.replace("//", "/");
    if (reason == "") {
        reason += validateExists(resourcePath);
    }

    if (reason != "") {
        CARBON.showWarningDialog(reason);
        if (document.getElementById('add-resource-div')) {
            document.getElementById('add-resource-div').style.display = "";
        }
    	document.getElementById('whileUpload').style.display = "none";
        return false;
    }

    var parentPath = document.getElementById('trParentPath').value;
    var fileName = document.getElementById('trFileName').value;
    var mediaType = document.getElementById('trMediaType').value;
    var description = ""; //document.getElementById('trDescription').value;
    var content = '';

    var radioObj = document.textContentForm.richText;
    var selected = "";
    for(var i=0;i<radioObj.length;i++){
	if(radioObj[i].checked)selected = radioObj[i].value;
    }
    if(selected == 'rich'){
	if (textContentEditor) {
		textContentEditor.saveHTML();
		content = textContentEditor.get('textarea').value;
		textContentEditor.destroy();
		textContentEditor = null;
	}
    } else{
        if (textContentEditor) {
	        textContentEditor.destroy();
	        textContentEditor = null;
	}
    	content = $('trPlainContent').value;
    }

    new Ajax.Request('../tenant-theme/add_text_resource_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {parentPath: parentPath, fileName: fileName, mediaType: mediaType, description: description, content: content},

        onSuccess: function() {
            refreshThemeMetadataSection(parentPath);
            refreshThemeContentSection(parentPath);
            document.getElementById('whileUpload').style.display = "none";
            CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["successfully.added.text.content"],loadData);
        },

        onFailure: function() {
        }
    });
    return true;
}

function submitThemeCollectionAddForm() {
    sessionAwareFunction(function() {

        var parentPath = document.getElementById('parentPath').value;
        var collectionName = document.getElementById('collectionName').value;
        var mediaType = document.getElementById('mediaType').value;
        if (mediaType == "other") {
            mediaType = document.getElementById('mediaTypeOtherValue').value;
        }
        var description = ""; // document.getElementById('colDesc').value;

        var reason = "";
        reason += validateEmpty(document.getElementById('collectionName'), org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
        if (reason == "") {
            reason += validateIllegal(document.getElementById('collectionName'), org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
        }
        if (reason == "") {
            reason += validateResourcePathAndLength(document.getElementById('collectionName'));
        }
        /*if (reason == "") {
            reason += validateForInput(document.getElementById('colDesc'), org_wso2_carbon_registry_resource_ui_jsi18n["description"]);
        }
        if (reason == "") {
            reason += validateDescriptionLength(document.getElementById('colDesc'));
        } */
        var resourcePath= document.getElementById('parentPath').value + '/' + document.getElementById('collectionName').value;
        resourcePath = resourcePath.replace("//", "/");
        if (reason == "") {
            reason += validateExists(resourcePath);
        }

        if (document.getElementById('collectionName').value.indexOf('/') == 0) {
            document.getElementById('collectionName').value = document.getElementById('collectionName').value.slice(1);
        }
        if (document.getElementById('collectionName').value.indexOf('//') == 0) {
            document.getElementById('collectionName').value = document.getElementById('collectionName').value.slice(2);
        }
        if (document.getElementById('collectionName').value.indexOf('///') == 0) {
            document.getElementById('collectionName').value = document.getElementById('collectionName').value.slice(3);
        }
        if (document.getElementById('collectionName').value.indexOf('////') == 0) {
            document.getElementById('collectionName').value = document.getElementById('collectionName').value.slice(4);
        }


        if (reason != "") {
            CARBON.showWarningDialog(reason);
            return false;
        }

        var addSuccess = true;
        new Ajax.Request('../tenant-theme/add_collection_ajaxprocessor.jsp',
        {
            method:'post',
            parameters: {parentPath: parentPath, collectionName: collectionName, mediaType: mediaType, description: description},

            onSuccess: function() {
                refreshThemeMetadataSection(parentPath);
                refreshThemeContentSection(parentPath);
                CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["successfully.added.collection"],loadData);
            },

            onFailure: function(transport) {
                addSuccess = false;
                CARBON.showErrorDialog(org_wso2_carbon_registry_resource_ui_jsi18n["failed.to.add.collection"] + transport.responseText,loadData);
            }
        });
    }, org_wso2_carbon_registry_resource_ui_jsi18n["session.timed.out"]);

    loadData();
}

function navigateThemePages(wantedPage, resourcePath, viewMode, consumerID, targetDivID) {
    fillThemeContentSection(resourcePath, wantedPage, viewMode, consumerID, targetDivID);
}

function fillThemeContentSection(path, pageNumber, viewMode, consumerID, targetDivID) {
    var random = Math.floor(Math.random() * 2000);
    new Ajax.Updater('contentDiv', '../tenant-theme/content_ajaxprocessor.jsp', { method: 'get', parameters: {path: path, requestedPage: pageNumber, mode: 'standard',resourceViewMode:viewMode,resourcePathConsumer:consumerID,targetDivID:targetDivID,random:random} });
}


function viewAddResourceUI() {
    var addSelector = document.getElementById('addMethodSelector');
    var selectedValue = addSelector.options[addSelector.selectedIndex].value;

    var uploadUI = document.getElementById('uploadContentUI');
    var importUI = document.getElementById('importContentUI');

    if (selectedValue == "upload") {

        uploadUI.style.display = "";
        importUI.style.display = "none";

    } else if (selectedValue == "import") {

        uploadUI.style.display = "none";
        importUI.style.display = "";

    }
    if ($('add-folder-div')) {
        if ($('add-folder-div').style.display != "none")$('add-folder-div').style.display == "none";
    }
}

function validateImage(filename) {
    var validExts = [".bmp", ".gif", ".jpg", ".png", ".jpeg"];
    var filenameLower = filename.value.toLowerCase();
    for (i = 0; i < validExts.length; i ++) {
        if (filenameLower.lastIndexOf(validExts[i]) != -1) {
            return "";
        }
    }
    return "The image extension should be one of the following values, '.bmp', '.gif', '.png', '.jpeg'";
}



function submitUploadThemeContentForm() {

    var rForm = document.forms["resourceUploadForm"];

    if (!rForm.redirectWith || rForm.redirectWith.value == "") {
        sessionAwareFunction(function() {}, org_wso2_carbon_registry_resource_ui_jsi18n["session.timed.out"]);
    }
    
    /* Validate the form before submit */
    var filePath = rForm.upload;
    var reason = "";

    if (filePath.value.length == 0) {
        reason += "The required field File Path has not been filled in.<br />"
    }
    reason += validateEmpty(rForm.filename, "Name");
    if (reason == "") {
        reason = validateImage(rForm.upload);
    }
    reason += validateIllegal(rForm.filename, "Name");
    reason += validateResourcePathAndLength(rForm.filename);
    reason += validateForInput(rForm.filename, "Name");
    reason += validateForInput(rForm.mediaType, "Media type");
    //reason += validateForInput(rForm.description, "Description");

    if (reason != "") {
        if (document.getElementById('add-resource-div')) {
    	    document.getElementById('add-resource-div').style.display = "";
        }
    	document.getElementById('whileUpload').style.display = "none";
    	CARBON.showWarningDialog(reason);
        return false;
    } else {
        if (document.getElementById('add-resource-div')) {
            document.getElementById('add-resource-div').style.display = "none";
        }
        rForm.submit();
    }
}


function fillThemeResourceUploadMediaTypes() {

    var filepath = document.getElementById('uResourceFile').value;

    var filename = "";
    if (filepath.indexOf("\\") != -1) {
        filename = filepath.substring(filepath.lastIndexOf('\\') + 1, filepath.length);
    } else {
        filename = filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length);
    }

    var extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length);

    var mediaType = "";
    if (extension.length > 0) {
        mediaType = getMediaType(extension.toLowerCase());
        if (mediaType == undefined) {
            mediaType = "";
        }
    }

    document.getElementById('uResourceMediaType').value = mediaType;
}

function fillThemeResourceUploadDetails() {

    var filepath = document.getElementById('uResourceFile').value;

    var filename = "";
    if (filepath.indexOf("\\") != -1) {
        filename = filepath.substring(filepath.lastIndexOf('\\') + 1, filepath.length);
    } else {
        filename = filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length);
    }

    document.getElementById('uResourceName').value = filename;
    fillThemeResourceUploadMediaTypes();
}

function fillThemeResourceImportMediaTypes() {

    var filepath = document.getElementById('irFetchURL').value;

    var filename = "";
    if (filepath.indexOf("\\") != -1) {
        filename = filepath.substring(filepath.lastIndexOf('\\') + 1, filepath.length);
		filename = filename.replace("?", ".");
	} else {
        filename = filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length);
		filename = filename.replace("?", ".");
    }

    var extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length);

    var mediaType = "";
    if (extension.length > 0) {
        mediaType = getMediaType(extension);
        if (mediaType == undefined) {
            mediaType = "";
        }
    }
	else {
		extension = filename.substring(filename.lastIndexOf("?") + 1, filename.length);
	    if (extension.length > 0) {
	        mediaType = getMediaType(extension);
    	    if (mediaType == undefined) {
        	    mediaType = "";
	        }
    	}
	}

    document.getElementById('irMediaType').value = mediaType;
}

function fillThemeResourceImportDetails() {

    var filepath = document.getElementById('irFetchURL').value;

    var filename = "";
    if (filepath.indexOf("\\") != -1) {
        filename = filepath.substring(filepath.lastIndexOf('\\') + 1, filepath.length);
		filename = filename.replace("?", ".");
	} else {
        filename = filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length);
		filename = filename.replace("?", ".");
    }

    document.getElementById('irResourceName').value = filename;
    fillThemeResourceImportMediaTypes();
}

function refreshThemeMetadataSection(path) {
    var random = Math.floor(Math.random() * 2000);
    //new Ajax.Updater('metadataDiv', '../tenant-theme/metadata_ajaxprocessor.jsp', { method: 'get', parameters: {path: path,random:random} });
}

function refreshThemeContentSection(path) {
    var random = Math.floor(Math.random() * 2000);
    new Ajax.Updater('contentDiv', '../tenant-theme/content_ajaxprocessor.jsp', { method: 'get', parameters: {path: path,random:random} });
}

function fillThemeContentSection(path, pageNumber, viewMode, consumerID, targetDivID) {
    var random = Math.floor(Math.random() * 2000);
    new Ajax.Updater('contentDiv', '../tenant-theme/content_ajaxprocessor.jsp', { method: 'get', parameters: {path: path, requestedPage: pageNumber, mode: 'standard',resourceViewMode:viewMode,resourcePathConsumer:consumerID,targetDivID:targetDivID,random:random} });
}


function viewStandardThemeContentSection(path) {
    var random = Math.floor(Math.random() * 2000);
    new Ajax.Updater('contentDiv', '../tenant-theme/content_ajaxprocessor.jsp', { method: 'get', parameters: {path: path, mode: 'standard',random:random} });
}


function resetThemeResourceForms(){
    var addSelector = document.getElementById('addMethodSelector');
    addSelector.selectedIndex = 0;

    document.getElementById('uploadContentUI').style.display = "";
    document.getElementById('importContentUI').style.display = "none";
    document.getElementById('textContentUI').style.display = "none";

}



function displayThemeContentAsText(resourcePath) {

    new Ajax.Request('../tenant-theme/display_text_content_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {path: resourcePath},

        onSuccess: function(transport) {
            document.getElementById('generalContentDiv').innerHTML = transport.responseText;
        },

        onFailure: function(transport){
            if (trim(transport.responseText)) {
            	CARBON.showWarningDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unsupported.media.type.to.display"] + " " + transport.responseText);
            } else {
            	CARBON.showWarningDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unsupported.media.type.to.display"]);
            }
        }
    });

    var textDiv = document.getElementById('generalContentDiv');
    textDiv.style.display = "block";
}
var myEditor = null;
var imagePaths = null;
function cssEditorDisplay(resourcePath,toDiv,imagePaths){
    this.imagePaths = imagePaths;
    new Ajax.Request('../tenant-theme/edit_text_content_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {path: resourcePath},

        onSuccess: function(transport) {
            document.getElementById(toDiv).innerHTML = transport.responseText;
            YAHOO.util.Event.onAvailable('editTextContentID', function() {
                editAreaLoader.init({
                    id : "editTextContentID"        // textarea id
                    ,syntax: "css"            // syntax to be uses for highgliting
                    ,start_highlight: true        // to display with highlight mode on start-up
                });
            });
        },

        onFailure: function(transport) {
            if (trim(transport.responseText)) {
                CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unsupported.media.type.to.edit"] + " " + transport.responseText);
            } else {
                CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unsupported.media.type.to.edit"]);
            }
        }
    });
}
function save_text(id, content){

			alert("Here is the content of the EditArea '"+ id +"' as received by the save callback function:\n"+content);
		
}
function displayEditThemeContentAsText(resourcePath) {
    new Ajax.Request('../tenant-theme/edit_text_content_ajaxprocessor.jsp',
    {
        method:'post',
        parameters: {path: resourcePath},

        onSuccess: function(transport) {
            	document.getElementById('generalContentDiv').innerHTML = transport.responseText;
        },

        onFailure: function(transport){
        	 if (trim(transport.responseText)) {
        	 	CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unsupported.media.type.to.edit"] + " " + transport.responseText);
        	 } else {
        	 	CARBON.showInfoDialog(org_wso2_carbon_registry_resource_ui_jsi18n["unsupported.media.type.to.edit"]);
        	 }
        }
    });

    var textDiv = document.getElementById('generalContentDiv');
    textDiv.style.display = "block";
}
function cancelTextContentEdit(displayCssEditor){
    if(displayCssEditor){
         location.reload(true);
    }
}
function updateThemeTextContent(resourcePath,displayCssEditor) {
    var cssText = editAreaLoader.getValue("editTextContentID");

    //$('saveContentButtonID').disabled = true;
    new Ajax.Request('../tenant-theme/update_text_content.jsp',
    {
        method:'post',
        parameters: {resourcePath: resourcePath, contentText: cssText},

        onSuccess: function() {
            if(displayCssEditor){
                //alert(resourcePath);

                location.reload(true);
                
            }
            //refreshThemeContentSection(resourcePath);
        },

        onFailure: function(transport) {
            showRegistryError(org_wso2_carbon_registry_resource_ui_jsi18n["failed.to.update"] + " " + transport.responseText);
            document.getElementById('saveContentButtonID').disabled = false;
        }
    });

    var textDiv = document.getElementById('generalContentDiv');
    //textDiv.style.display = "block";
}



function renameThemeResource(parentPath, oldResourcePath, resourceEditDivID, wantedPage, type) {

    var reason = "";
    document.getElementById(resourceEditDivID).value = ltrim(document.getElementById(resourceEditDivID).value);
    reason += validateEmpty(document.getElementById(resourceEditDivID), org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    if (reason == "") {
        reason += validateIllegal(document.getElementById(resourceEditDivID), org_wso2_carbon_registry_resource_ui_jsi18n["name"]);
    }
    if (reason == "") {
        reason += validateResourcePathAndLength(document.getElementById(resourceEditDivID));
    }
    var resourcePath= parentPath + '/' + document.getElementById(resourceEditDivID).value;
    resourcePath = resourcePath.replace("//", "/");
    if (reason == "") {
        reason += validateExists(resourcePath);
    }

    if (reason != "") {
        CARBON.showWarningDialog(reason);
        return false;

    } else {

        var newName = document.getElementById(resourceEditDivID).value;

        new Ajax.Request('../tenant-theme/rename_resource_ajaxprocessor.jsp',
        {
            method:'post',
            parameters: {parentPath: parentPath, oldResourcePath: oldResourcePath, newName: newName, type: type},

            onSuccess: function() {
                displaySuccessMessage(resourcePath, org_wso2_carbon_registry_resource_ui_jsi18n["renamed"]);
                fillThemeContentSection(parentPath, wantedPage);
            },

            onFailure: function(transport) {
                addSuccess = false;
                CARBON.showErrorDialog(transport.responseText);
            }
        });
    }
    return true;
    loadData();
}


var deleteConfirms = 0;
function deleteThemeResource(pathToDelete, parentPath) {
    if(deleteConfirms != 0){
    return;
    }
    deleteConfirms++;


    sessionAwareFunction(function() {
        CARBON.showConfirmationDialog(org_wso2_carbon_registry_resource_ui_jsi18n["are.you.sure.you.want.to.delete"] + "<strong>'" + pathToDelete + "'</strong> " + org_wso2_carbon_registry_resource_ui_jsi18n["permanently"], function() {
        deleteConfirms = 0;
            var addSuccess = true;
            new Ajax.Request('../tenant-theme/delete_ajaxprocessor.jsp',
            {
                method:'post',
                parameters: {pathToDelete: pathToDelete},

                onSuccess: function() {
                    refreshThemeContentSection(parentPath);

                },

                onFailure: function() {
                    addSuccess = false;
                }
            });
        },
        function() {
            deleteConfirms = 0;
        }
        ,function() {
            deleteConfirms = 0;
        }
        );

     }, org_wso2_carbon_registry_resource_ui_jsi18n["session.timed.out"]);       
    loadData();

}


function hideThemeOthers(id, type) {
    var renamePanel = document.getElementById("rename_panel" + id);


    if (type == "rename") {
    }
    if (type == "del") {
        if (renamePanel.style.display != "none") renamePanel.style.display = "none";
    }
}
function toggleImagePathPicker(){
    
}
function filterImagePathPicker() {
    YAHOO.util.Event.onAvailable('flickr_search', function() {

        $('flickr_results').innerHTML = "";
        var selectedImages = new Array();
        var showAll = true;
        var illegalChars = /([^a-zA-Z0-9_\-\x2E\&\?\/\:\,\s\*])/;
if($('flickr_search').value == "")
{
//CARBON.showWarningDialog("Enter string");
}
else
{
        if (illegalChars.test($('flickr_search').value)) 
	{
            //CARBON.showWarningDialog("Ileagal characters");

        }
        if ( $('flickr_search').value == "*")
	{
            selectedImages = imagePaths;
        } else {
            var imgRegExp = new RegExp($('flickr_search').value);
            for (var i = 0; i < imagePaths.length; i++) {
                if (imagePaths[i].search(imgRegExp) != -1) {
                    selectedImages.push(imagePaths[i]);
                }
            }
        }
}
        var ulObj = document.createElement("UL");
        YAHOO.util.Dom.addClass(ulObj, "imageList");
        for (i = 0; i < selectedImages.length; i++) {

            var imgObj = '<li><a id="flickimg' + i + '">' + selectedImages[i] + '</a></li>';
            ulObj.innerHTML += imgObj;

        }
        $('flickr_results').appendChild(ulObj);

        for (i = 0; i < selectedImages.length; i++) {

            YAHOO.util.Event.addListener("flickimg" + i, "click", function(e, obj) {
                //insertAtCursor($('editTextContentID'),obj);
                addToSelected(obj);
            }, selectedImages[i]);

        }
    });
}
function addToSelected(obj){
    YAHOO.util.Event.onAvailable('flickr_selected', function() {
        $('flickr_selected').style.display = "";
        $('flickr_selected').innerHTML = obj;
    });
}
function previewImagePath() {
    var imagePath = $('flickr_selected').innerHTML;
    var imageData = '<div style="padding:20px"><img src="'+getThemeResourceViewAsImageURL(imagePath)+'" id="imagePreviwer" /></div>'
    CARBON.showPopupDialog(imageData,"Image Preview");
}
function insertImagePath(){
    var text = $('flickr_selected').innerHTML;
    editAreaLoader.setSelectedText('editTextContentID', text);
}
function getThemeResourceViewAsImageURL(path) {
        return "../../registry/themeResourceContent?path=" + path + "&viewImage=1";
}
//myField accepts an object reference, myValue accepts the text strint to add
function insertAtCursor(myField, myValue) {
    //IE support
    var scrollPos = myField.scrollTop;
    if (document.selection) {
        myField.focus();
        //in effect we are creating a text range with zero
        //length at the cursor location and replacing it
        //with myValue
        sel = document.selection.createRange();
        sel.text = myValue;
    }
   
    //Mozilla/Firefox/Netscape 7+ support
    else if (myField.selectionStart || myField.selectionStart == '0'){

    //Here we get the start and end points of the
    //selection. Then we create substrings up to the
    //start of the selection and from the end point
    //of the selection to the end of the field value.
    //Then we concatenate the first substring, myValue,
    //and the second substring to get the new value.
    var startPos = myField.selectionStart;
    var endPos = myField.selectionEnd;
    myField.value = myField.value.substring(0, startPos) + myValue + myField.value.substring(endPos, myField.value.length);
}
else
{
    myField.value += myValue;
}
    myField.scrollTop = scrollPos;
}

function whileThemeResourceUpload(){
    if(document.getElementById('add-resource-div')) {
        document.getElementById('add-resource-div').style.display = "none";
    }
    document.getElementById('whileUpload').style.display = "";

}
