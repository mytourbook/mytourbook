<?php
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
require("global.php");
InitPage("login");

$pageTitle 		= "Babel Project";
$pageKeywords 	= "translation,language,nlpack,pack,eclipse,babel";

global $addon;
$addon->callHook("head");


//$_SESSION['language'] = "";
//$_SESSION['project'] = "";
//$_SESSION['version'] = "";

# Bug 221420 Allow bookmarking file/string/translation
if(isset($_GET['project'])) {
	$_SESSION['project'] = stripslashes($_GET['project']);
}
if(isset($_GET['version'])) {
	$_SESSION['version'] = stripslashes($_GET['version']);
}
if(isset($_GET['file'])) {
	$_SESSION['file'] = stripslashes($_GET['file']);
}
if(isset($_GET['string'])) {
	$_SESSION['string'] = htmlspecialchars($_GET['string']);
}

$filesOrderNameChecked = "";
if (!isset($_SESSION['filesOrder']) or $_SESSION['filesOrder'] == 'name') {
	$filesOrderNameChecked = "checked";
}

$filesOrderCompletionChecked = "";
if (isset($_SESSION['filesOrder']) and $_SESSION['filesOrder'] == 'completion') {
	$filesOrderCompletionChecked = "checked";
}

?>

<h1 id="page-message">Welcome to the Babel Project</h1>
<? include("fragments/motd.php");?>
<div id="contentArea">

	<h2>Languages / Projects / Versions / Files</h2>
	<div id="language" class="side-component-small">
		<h4 id="language-selection">Languages</h4>
		<ul id="language-area"class="scrollable-area"></ul>
	</div>

	<div id="project" class="side-component-small">
		<h4 id="project-selection">Projects</h4>
		<ul id="project-area" class="scrollable-area"></ul>
	</div>
	
	<div id="version" class="side-component-small">
		<h4 id="version-selection">Versions</h4>
		<ul id="version-area" class="scrollable-area"></ul>
	</div>

	<div id="files" class="side-component-small files">
		<h4 id="files-selection">
			Files
			<input id="files-order-name" name="files-order" type="radio" <?= $filesOrderNameChecked ?>>ordered by name
			<input id="files-order-completion" name="files-order" type="radio" <?= $filesOrderCompletionChecked ?>>ordered by completion
		</h4>
		<ul id="files-area" class="scrollable-area"></ul>
	</div>

	<div class="clearing"></div>
	
	
<!--  	
<script type="text/javascript"> 
	var myTabs = new YAHOO.widget.TabView("string-area"); 
</script>  
-->


	
	<div id="string-area" class="yui-navset full-component">
	<h2 id="string-title">Strings In File</h2>
<!--
	    <ul class="yui-nav"> 
        <li class="selected"><a href="#tab1"><em>Untranslated</em></a></li> 
	        <li><a href="#tab2"><em>Flagged Incorrect</em></a></li> 
	        <li><a href="#tab3"><em>Awaiting Rating</em></a></li> 
	    </ul>             
  	  	<div class="yui-content" style="clear: both;"> 
			<div id="not-translated">
			</div>
			
			<ul id="flagged-incorrect">
			</ul>
			
			<ul id="awaiting-ratings">
			</ul>
		</div>
	    
-->	    
  	  	<div id="projecs-strings-area" class="yui-content"> </div>
		
		<div id="not-translated"></div>
	</div>
	
	
	<div id="translation-area" class="full-component">
	   <h2 id="translation-title">Translation For Key</h2>
	   <div id="translation-form-container"></div>
	   <div class="clearing"></div>
	</div>
	
	<div class="clearing"></div>
	
</div>

<script language="javascript">
	YAHOO.languageManager.getAjaxLanguages();
</script>


<?php
	global $addon;
    $addon->callHook("footer");
?>