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

YAHOO.tranlsation = new Object();

YAHOO.tranlsation.posted = false;


function showTranslateStringForm(stringIdIn,stringTableIndex){
	var callback = 
	{ 
		start:function(eventType, args){ 
		},
		success: function(o) {
			var langDomNode = document.getElementById('translation-form-container');
			langDomNode.innerHTML = o.responseText;
			YAHOO.util.Event.onAvailable("translation-form",setupTranslatFormCB);
		},
		failure: function(o) {
			YAHOO.log('failed!');
		} 
	} 
	YAHOO.util.Connect.asyncRequest('POST', "callback/getCurrentStringTranslation.php", callback, "string_id="+stringIdIn+"&stringTableIndex="+stringTableIndex);
	document.onmouseup = catchSelection
}

function setupTranslatFormCB(){
	YAHOO.util.Event.addListener("allversions","click",translateAll);
	YAHOO.util.Event.addListener("translation-form","submit",translationSumbitStop);	
	YAHOO.util.Event.addListener("non-translatable-checkbox","click",notTranslatable);
	YAHOO.util.Event.addListener("copy-english-string-link","click",copyEnglishString);
	YAHOO.util.Event.addListener("reset-current-translation-link","click",resetCurrentTranslation);
	YAHOO.util.Event.addListener("clear-current-translation-link","click",clearCurrentTranslation);
}


function notTranslatable(){
	var target = document.getElementById('translation-form');
	var post = "string_id="+target.string_id.value+"&check="+target.non_translatable_string.checked;
	
	var callback = 
	{ 
		start:function(eventType, args){
		},
		success: function(o) {
			YAHOO.projectStringsManager.getAjaxProjectStrings();
			var langDomNode = document.getElementById('translation-form-container');
			langDomNode.innerHTML = o.responseText;
		},
		failure: function(o) {
		} 
	} 
	var request = YAHOO.util.Connect.asyncRequest('POST', "callback/setStringNonTranslatable.php", callback, post);
}

function translationClear(){
	if(YAHOO.tranlsation.posted == true){
		YAHOO.tranlsation.posted = false;
	}else{
		var langDomNode = document.getElementById('translation-form-container');
		langDomNode.innerHTML = "";
	}
}


function translateAll(e){
	translationSumbit("all",document.getElementById('translation-form').stringTableIndex.value);
}

function translationSumbitStop(e){
	YAHOO.util.Event.stopEvent(e);
}

function translationSumbit(allornot,translationIndex){
	var target = document.getElementById('translation-form');
	var tr_value = target.translation.value;
	var fuzzy_value = (target.fuzzy_checkbox.checked ? 1 : 0);
	
	var callback = 
	{ 
		start:function(eventType, args){
		},
		success: function(o) {
			var response = eval("("+o.responseText+")");			
			YAHOO.projectStringsManager.updateStringTableCurrentTranslation(translationIndex, response.translationString);
			YAHOO.projectStringsManager.updateStringTableFuzzy(translationIndex, fuzzy_value);
			target.innerHTML = response.translationArea;
		},
		failure: function(o) {
			YAHOO.log('failed!');
		} 
	} 
	
	YAHOO.tranlsation.posted = true;
	var post = "string_id="+target.string_id.value+
			   "&translation="+sub(tr_value)+
			   "&fuzzy="+fuzzy_value+
			   "&translate_action="+allornot;
	spin();
	var request = YAHOO.util.Connect.asyncRequest('POST', "callback/setStringTranslation.php", callback, post);
}

function sub(it){
	it = it.replace(/\+/g,"%2b");
	return it.replace(/&/g,"%26"); 
}

function spin() {
	var domNode = document.getElementById('translation-form');
	YAHOO.spinable.attach(domNode);
}

function copyEnglishString() {
	// PHP's nl2br() function produced different output in IE and Firefox. Need to adjust accordingly.
	var englishStringElement = document.getElementById('english-string')
	var appName = navigator.appName;
	if (appName == "Microsoft Internet Explorer")
		englishString = englishStringElement.innerText.replace(/<BR>/g, "\r\n");
	else
		englishString = englishStringElement.textContent.replace(/<br>/g, "");
	var currentTranslation = document.getElementById('current-translation');
	if (currentTranslation != null)
		currentTranslation.value = currentTranslation.value + englishString;
}

function resetCurrentTranslation() {
	var currentTranslation = document.getElementById('current-translation');
	currentTranslation.value = currentTranslation.defaultValue;
}

function clearCurrentTranslation() {
	var currentTranslation = document.getElementById('current-translation');
	currentTranslation.value = "";
}
