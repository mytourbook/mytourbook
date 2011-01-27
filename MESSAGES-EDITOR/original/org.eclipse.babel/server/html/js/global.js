/*******************************************************************************
 * Copyright (c) 2007 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Paul Colton (Aptana)- initial API and implementation
 *    Eclipse Foundation
*******************************************************************************/

YAHOO.widget.Logger.enableBrowserConsole();

YAHOO.spinable = {
	spinningIconPath :"animations/process-working.png",

	attach: function(domIN){
		if(domIN){
			domIN.innerHTML = "<div id='spinner'><img src='http://babel.eclipse.org/images/spinner.gif' alt='spinner'><h1>...loading...</h1></div>";
		}
	}
};

function selectable(){
}

selectable.prototype.initSelectable = function(){
	this.hoverColor = "LightSkyBlue";
	if(this.domElem){
		this.bgColor = this.domElem.style.backgroundColor;
	}else{
		this.bgColor = "white";
	}
	this.selectedColor = "LightSkyBlue";
}
selectable.prototype.mouseOver = function(){
	YAHOO.util.Dom.setStyle(this.domElem,"background",this.hoverColor);
}
selectable.prototype.mouseOut = function(){
	if(this.isSelected()){
		this.selected();
	}else{
		YAHOO.util.Dom.setStyle(this.domElem,"background",this.bgColor);
	}
}
selectable.prototype.selected = function(){
	YAHOO.util.Dom.setStyle(this.domElem,"background",this.selectedColor);
}
selectable.prototype.unselect = function(){
	YAHOO.util.Dom.setStyle(this.domElem,"background",this.bgColor);
}

selectable.prototype.addEvents = function(){
	YAHOO.util.Event.addListener(this.domElem,"click",this.clicked,this,true);
	YAHOO.util.Event.addListener(this.domElem,"mouseover",this.mouseOver,this,true);
	YAHOO.util.Event.addListener(this.domElem,"mouseout",this.mouseOut,this,true);
}

function setupFilesOrder() {
	var orderName = document.getElementById("files-order-name");
	YAHOO.util.Event.addListener("files-order-name", "click", filesOrderRadioButtonClicked);

	var orderCompletion = document.getElementById("files-order-completion");
	YAHOO.util.Event.addListener("files-order-completion", "click", filesOrderRadioButtonClicked);
}

function filesOrderRadioButtonClicked() {
	var callback = 
	{ 
		start: function(eventType, args) { 
		},
		success: function(o) {
			var domNode = document.getElementById('files-area');
			var response;
			if(o.responseText){
				response =  eval("("+o.responseText+")");
			}
			if (response) {
				domNode.innerHTML = "";

				for (var i = 0; i < response.length; i++) {
					var proj = new afile(response[i]);
					domNode.appendChild(proj.createHTML());
					if(response[i]['current']){
						YAHOO.filesManager.updateSelected(proj);
					}
				}
			} else {
				domNode.innerHTML = "";
			}
			YAHOO.projectStringsManager.getAjaxProjectStrings();
		},
		failure: function(o) {
			YAHOO.log('failed!');
		} 
	}

	var parameter;
	if (this.id == "files-order-name") {
		parameter = "order=name";
	} else {
		parameter = "order=completion";
	}

	var domNode = document.getElementById('files-area');
	YAHOO.spinable.attach(domNode);
	YAHOO.util.Connect.asyncRequest('POST', "callback/getFilesForProject.php", callback, parameter);
}

YAHOO.util.Event.onDOMReady(setupFilesOrder);


function catchSelection() {
	var sel = "";
   	if(window.getSelection) {
   	    objSel = window.getSelection();
   	    sel = objSel.toString();
   	    // objSel.removeAllRanges();
   	} 
   	else if(document.selection && document.selection.createRange) {
		sel = document.selection.createRange().text;
		event.cancelBubble = true;
		// document.selection.empty();
	}
	if(sel != "") {
		if(document.getElementById('translation-hints')) {
			var domNode = document.getElementById('translation-hints');
			
			domNode.innerHTML = "Please wait, looking for : <b>" + sel + "</b>";			
			showTranslationHints(sel);
		}
	}
}