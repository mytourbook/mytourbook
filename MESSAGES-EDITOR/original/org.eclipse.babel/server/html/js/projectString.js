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

YAHOO.projectStringsManager = {
	trCounter : 0,
	
	getAjaxProjectStrings : function(){

		if(!YAHOO.languageManager.getSelected() || 
			!YAHOO.projectManager.getSelected() ||
			!YAHOO.versionManager.getSelected() ||
			!YAHOO.filesManager.getSelected()
		  ){
			var domNode = document.getElementById('projecs-strings-area');
			domNode.innerHTML = "";
			translationClear();
			
			return false;
		} 
	
		var callback = 
		{ 
			sp : this,
			start:function(eventType, args){ 
			},
			success: function(o) {
				var titleNode = document.getElementById('string-title');
				var filename = YAHOO.filesManager.getSelected().filename;
				if (filename.length > 135) {
					filename = filename.substr(0, 65) + "(...)" + filename.substr(filename.length - 65);
				}
				var href = '<a href="?project=' + YAHOO.projectManager.getSelected().project;
				href += '&version=' + YAHOO.versionManager.getSelected().version;
				href += '&file=' + YAHOO.filesManager.getSelected().filename + '">';

				var someString = 'Strings In File <span id="title-link">\"';
				someString += href;
				someString += filename;
				someString += '</a>';
				someString += '\" ';
				someString += href;
				someString += '[link to this file]';
				someString += '</a>';
				titleNode.innerHTML = someString;
				
				var domNode = document.getElementById('projecs-strings-area');
				domNode.innerHTML = "";
				var values = new Object();
				values.cssID = "translatable-strings-labels-area";
				values.cssClass = "";
				values.string = "String";
				values.translation = "Last Translation";
				values.translator = "User";
				values.createdon = "Created On";
				this.tableDom = this.sp.createHTML(values)
				domNode.appendChild(this.tableDom);

//				translationClear();

				var ntDomNode = document.getElementById('not-translated');
				this.sp.tableDom = document.createElement("table")
				this.sp.tableDom.id = "strings-in-file-table";
				this.sp.tableDom.className = "translatable";
				this.sp.tableDom.cellSpacing = 0;
				this.sp.tableDom.width = "100%"
				ntDomNode.innerHTML = "";
				ntDomNode.appendChild(this.sp.tableDom);
				
				if(o.responseText){
					YAHOO.projectStringsManager.createStringUI(o,this.sp);	
				}
			},
			failure: function(o) {
				YAHOO.log('failed!');
			} 
		} 
		
		//start spinner
		var domNode = document.getElementById('not-translated');
		YAHOO.spinable.attach(domNode);
		YAHOO.util.Connect.asyncRequest('GET', "callback/getStringsforProject.php", callback, null);
	},

	createStringUI: function(o){
		var response = eval("("+o.responseText+")");			
		if(response.length == 0){
			return 0;
		}
		
		var callback2 = 
		{ 
			sp : this,
			start:function(eventType, args){ 
			},
			success: function(o) {
				YAHOO.projectStringsManager.createStringUI(o);
			}
		}		
		
		if(response[response.length-1].paged > 0){
			YAHOO.util.Connect.asyncRequest('GET', "callback/getStringsforProject.php?paged="+response[response.length-1].paged, callback2, null);
		}
		
		var ntDomNode = document.getElementById('not-translated');
		for(var i = 0; i < response.length; i++){
			if(response[i].paged > 0){
				//do nothing
			}else{
				var proj = new projectString(response[i]);
				proj.createHTML(this.tableDom);
				if(response[i]['current']){
					YAHOO.projectStringsManager.updateSelected(proj, ntDomNode.scrollHeight);
				}
			}
		}
	},
		
	createHTML : function(values,appenToDOm){
		var tableDom;
		var tr;
		if(typeof appenToDOm == "undefined"){
			tableDom = document.createElement("table");
			tableDom.cellSpacing = 0;
			tableDom.width = "100%";
			tr = tableDom.insertRow(0);
			this.trCounter = 0;
		}else{
			tableDom = appenToDOm;
			tr = tableDom.insertRow(this.trCounter);
			this.trCounter++;
		}
		
		tr.id =  values['cssID'];
		tr.className =  values['cssClass'];
		
		td = tr.insertCell(0);
		td.innerHTML = values['string'];
		td.width = "30%";

		td = tr.insertCell(1);
		td.innerHTML = values['translation'];
		td.width = "50%";

		td = tr.insertCell(2);
		td.innerHTML = values['translator'];
		td.width = "8%";
		
		td = tr.insertCell(3);
		td.innerHTML = values['createdon'];
		td.width = "12%";
		
		if(typeof appenToDOm == "undefined"){
			return tableDom;
		}else{
			return tr;
		}
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
		
		var domNode = document.getElementById('not-translated');
		if(domNode.scrollTop == 0) {
			domNode.scrollTop = scrollto_position;
		}
		
		var titleNode = document.getElementById('translation-title');
		var keyname = this.selected.data['stringName']
		if (keyname.length > 135) {
			keyname = keyname.substr(0, 65) + "(...)" + keyname.substr(filename.length - 65);
		}
		var href = '<a href="?project=' + YAHOO.projectManager.getSelected().project;
		href += '&version=' + YAHOO.versionManager.getSelected().version;
		href += '&file=' + YAHOO.filesManager.getSelected().filename;
		href += '&string=' + this.selected.data['stringName'] + '">';

		var someString = 'Translation For Key <span id="title-link">\"';
		someString += href;
		someString += keyname;
		someString += '</a>';
		someString += '\" ';
		someString += href;
		someString += '[link to this key]';
		someString += '</a>';
		titleNode.innerHTML = someString;
		showTranslateStringForm(this.selected.data['stringId'],this.selected.domElem.rowIndex);
	},
	
	updateStringTableCurrentTranslation: function(stringTableIndex,trans){
		this.tableDom.rows[stringTableIndex].cells[1].innerHTML = trans;
	},

	updateStringTableFuzzy: function(stringTableIndex, fuzzy){
		if(fuzzy == 1) {
			this.tableDom.rows[stringTableIndex].cells[1].innerHTML = '<img src="images/fuzzy.png" />' + this.tableDom.rows[stringTableIndex].cells[1].innerHTML;
		}
	}
//$stringTableIndex	
};

function projectString(dataIn){
	projectString.superclass.constructor.call();
	this.initSelectable();
	this.data = dataIn;
}
YAHOO.extend(projectString,selectable);
	projectString.prototype.isSelected = function(){
 	return (this == YAHOO.projectStringsManager.selected);
}

projectString.prototype.clicked = function(e){
	YAHOO.projectStringsManager.updateSelected(this);
}
projectString.prototype.createHTML = function(tableDom){
	var values = new Object();
	values.cssID = "";
	values.cssClass = "";
	if(this.data['nontranslatable']){
		values.cssClass = "nontranslatable";
	}
	var temp = this.data['text'] ? this.data['text'] : ''
	values.string = "<div style='width: 100%; overflow: hidden;'>"+temp+"</div>";
	
	temp = this.data['translationString'] ? this.data['translationString'] : ''
	
	if(this.data['fuzzy'] == 1 && this.data['nontranslatable'] != 1) {
		temp = "<img src='images/fuzzy.png' title='Possibly incorrect' />" + temp;
	}
	
	values.translation = "<div style='width: 100%; overflow: hidden;'>"+temp+"</div>";
	values.translator = this.data['translator'] ? this.data['translator'] : '';
	values.createdon = this.data['createdOn'];
	values.stringname = this.data['stringname'];
	values.fuzzy = this.data['fuzzy']
	
	var lineDome = YAHOO.projectStringsManager.createHTML(values,tableDom);
	this.domElem = lineDome;
	this.addEvents();
	this.initSelectable();
}
