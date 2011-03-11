/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *    Eclipse Foundation - initial API and implementation
*******************************************************************************/

function showTranslationHints(trString){
	var callback = 
	{ 
		start:function(eventType, args){ 
		},
		success: function(o) {
			var domNode = document.getElementById('translation-hints');
			domNode.innerHTML = o.responseText;		
			
			var domNode = document.getElementById('translation-hints-title');
			domNode.innerHTML = "Translation Hints [<a id=\"clear-btn\"href=\"javascript:void(0);\">Clear</a>]";	
		},
		failure: function(o) {
			YAHOO.log('failed!');
		} 
	} 
	YAHOO.util.Connect.asyncRequest('POST', "callback/getTranslationHints.php", callback, "tr_string="+trString);
	this.setupCB();
}

function setupCB(){
	document.onmouseup = null;
	YAHOO.util.Event.addListener("clear-btn","click",clearHints);
}

function clearHints() {
	if(window.getSelection) {
   	    objSel = window.getSelection();
   	    sel = objSel.toString();
   	    objSel.removeAllRanges();
   	} 
   	else if(document.selection && document.selection.createRange) {
		sel = document.selection.createRange().text;
		event.cancelBubble = true;
		document.selection.empty();
	}

	var domNode = document.getElementById('translation-hints');
	domNode.innerHTML = "Select some English text above to find similar translations.";		
		
	var domNode = document.getElementById('translation-hints-title');
	domNode.innerHTML = "Translation Hints";
		
	document.onmouseup = catchSelection;
}