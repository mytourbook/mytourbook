<?php
/*******************************************************************************
 * Copyright (c) 2007-2008 Eclipse Foundation and others.
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
InitPage("");

$pageTitle 		= "Babel Project - Eclipse translation";
$pageKeywords 	= "translation,language,nlpack,pack,eclipse,babel,english,french,german,chinese,japanese,spanish,arabic,hebrew,hungarian,polish,italian,russian,dutch,finnish,greek,norwegian,sweedish,turkish";

global $addon;
$addon->callHook("head");
?>
<h1 id="page-message">Welcome to the Babel Project</h1>
<? include("fragments/motd.php");?>
<div id="index-page" style='width: 510px; padding-right: 190px;'>
	  <div style='float: right;height: 290px; border: 0px solid red;'>
	      <? include("fragments/language_progress.php");?>
		  <? include("fragments/top_translators.php");?>	  
	  </div>

	  <div style='float: left;border: 0px solid red;'>
		  <a href="http://www.eclipse.org/babel/downloads.php"><img src="<?php echo imageRoot() ?>/large_icons/apps/internet-web-browser.png"><h2>Eclipse Speaks your Language</h2></a>
	      <br style='clear: both;'>
		  <p><a href="http://www.eclipse.org/babel/downloads.php">Download a language pack</a> in one of many different languages.</p>
		  <p><a href="languages.php">Languages</a> supported by Babel</p>.
	         
		  <a href="translate.php"><img src="<?php echo imageRoot() ?>/large_icons/apps/accessories-text-editor.png"><h2>Help Translate Eclipse</h2></a>
	      <br style='clear: both;'>
		  <p>Eclipse needs help from everyone in the community to <a href="translate.php">speak in many tongues</a>.</p>
	      
		  <a href="map_files.php"><img src="<?php echo imageRoot() ?>/large_icons/apps/system-users.png"><h2>Add an Existing Eclipse Project to Babel</h2></a>
	      <br style='clear: both;'>
		  <p>Eclipse committers, find out how simple it is to include any existing Eclipse.org project <a href="map_files.php">in Babel</a>.</p>
		  
		  <a href="help_babel.php"><img src="<?php echo imageRoot() ?>/large_icons/categories/preferences-desktop-peripherals.png"><h2>Become a Babel Team Player</h2></a>
	      <br style='clear: both;'>
		  <p>The Babel project has lots of ideas and we need your help to take this project to the <a href="help_babel.php">next level</a>.</p>

	  </div>	  
	  <br class='clearing'>
</div>

<script>YAHOO.languageManager.getAjaxLanguages();</script>

<?php
	global $addon;
    $addon->callHook("footer");
?>