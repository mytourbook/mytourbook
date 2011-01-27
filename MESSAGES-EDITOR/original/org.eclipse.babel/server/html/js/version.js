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

YAHOO.versionManager = {
	getAjaxVersions: function(selectedIn){
		var callback = 
		{ 
			start:function(eventType, args){ 
			},
			success: function(o) {
				var domNode = document.getElementById('version-area');
				var response;
				if(o.responseText){
					response = eval("("+o.responseText+")");
				}
				if(response){
	//				YAHOO.log(o.responseText);
					domNode.innerHTML = "";
					
					for(var i = 0; i < response.length; i++){
						var proj = new version(response[i]);
						domNode.appendChild(proj.createHTML());
						if(response[i]['current']){
							YAHOO.versionManager.updateSelected(proj, domNode.scrollHeight);
						}
					}
					YAHOO.filesManager.getAjax();
				} else{
					domNode.innerHTML = "Please select a project to continue.";
				}
			},
			failure: function(o) {
				YAHOO.log('failed!');
			} 
		} 
		//start spinner
		var domNode = document.getElementById('version-area');
		YAHOO.spinable.attach(domNode);
		YAHOO.util.Connect.asyncRequest('GET', "callback/getVersionsforProject.php", callback, null); 
	},

	getSelected: function(){
		return this.selected;
	},
	
	updateSelected: function(selec, scrollto_position){
		if(this.selected){
			this.selected.unselect();
		}
		this.selected = selec;
		this.selected.selected();
		var domNode = document.getElementById('version-area');
		if(domNode.scrollTop == 0) {
			domNode.scrollTop = scrollto_position;
		}
	}
};

function version(dataIn){
	this.version = dataIn['version'];
	this.pct = dataIn['pct'];
	version.superclass.constructor.call();
	this.initSelectable();
}
YAHOO.extend(version,selectable);
version.prototype.isSelected = function(){
 return (this == YAHOO.versionManager.selected);
}


version.prototype.clicked = function(e){
	YAHOO.util.Event.stopEvent(e);
	var callback = 
	{ 
		start:function(eventType, args){ 
		},
		success: function(o) {
			YAHOO.filesManager.getAjax();
		},
		failure: function(o) {
			YAHOO.log('failed!');
		} 
	} 
	var target = YAHOO.util.Event.getTarget(e);
	YAHOO.versionManager.updateSelected(this);
	YAHOO.util.Connect.asyncRequest('POST', "callback/setCurrentProjectVersion.php", callback, "version="+this.version);
}
version.prototype.createHTML = function(){
	this.domElem = document.createElement("li");
	// this.domElem.innerHTML = this.version;
	this.domElem.innerHTML = this.version + " <span class='percentage_indicator'>&#160; &#160;  (" + (this.pct > 0 ? new Number(this.pct).toFixed(1) : 0) + "%)</span>";
	this.addEvents();
	return this.domElem;
}

