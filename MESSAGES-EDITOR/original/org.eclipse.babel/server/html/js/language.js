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
 *    Kit Lo (IBM) - patch, bug 261739, Inconsistent use of language names
*******************************************************************************/

YAHOO.languageManager = {
	getAjaxLanguages: function(selectedIn){
		var callback = 
		{ 
			start:function(eventType, args){ 
			},
			success: function(o) {
				if(!o.responseText){
					return false;
				}
				var response = eval("("+o.responseText+")");
				var domNode = document.getElementById('language-area');
				if(!domNode){
					return false;
				}
				domNode.innerHTML = "";
//				YAHOO.log(o.responseText);

				for(var i = 0; i < response.length; i++){
					var proj = new language(response[i]);
					domNode.appendChild(proj.createHTML());
					if(response[i]['current']){
						YAHOO.languageManager.updateSelected(proj);
					}
				}
				
				//start project
				YAHOO.projectManager.getAjaxProject();
			},
			failure: function(o) {
				YAHOO.log('failed!');
			} 
		} 
		//start spining;
		var domNode = document.getElementById('language-area');
		YAHOO.spinable.attach(domNode);
		YAHOO.util.Connect.asyncRequest('GET', "callback/getLanguages.php", callback, null); 
	},

	getSelected: function(){
		return this.selected;
	},
	
	updateSelected: function(selec){
		if(this.selected){
			this.selected.unselect();
		}
		this.selected = selec;
		this.selected.selected();
	}
};



function language(dataIn){
	language.superclass.constructor.call();
	this.initSelectable();
	
	this.languageId = dataIn['language_id'];
	this.name = dataIn['name'];
	this.iso = dataIn['iso_code'];
	this.locale = dataIn['locale'];
}

YAHOO.extend(language,selectable);
language.prototype.isSelected = function(){
 return (this == YAHOO.languageManager.selected);
}

language.prototype.clicked = function(e){
	YAHOO.util.Event.stopEvent(e);
	var callback = 
	{ 
		start:function(eventType, args){ 
		},
		success: function(o) {
		YAHOO.log("language success about to ajax!");
			YAHOO.projectManager.getAjaxProject();
			YAHOO.projectStringsManager.getAjaxProjectStrings();
		},
		failure: function(o) {
			YAHOO.log('failed!');
		} 
	} 
	var target = YAHOO.util.Event.getTarget(e);
	YAHOO.languageManager.updateSelected(this);
	YAHOO.util.Connect.asyncRequest('POST', "callback/setCurrentLangue.php", callback, "lang="+this.languageId);
}
language.prototype.createHTML = function(){
	this.domElem = document.createElement("li");
	this.domElem.innerHTML = this.name;
	if(this.locale){
		this.domElem.innerHTML += " ("+this.locale+")";
	}
	this.addEvents();
	
	return this.domElem;
}
