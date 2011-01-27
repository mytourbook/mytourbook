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
  	
# Note: the style attribute for the ol tag below was causing a truncation in IE.
# I removed the attribute. The layout is still acceptable. Future modifications to this file should be tested in IE also.

require("global.php");
InitPage("");

$pageTitle 		= "Babel Project";
$pageKeywords 	= "translation,language,nlpack,pack,eclipse,babel";

global $addon;
$addon->callHook("head");

?>

<h1 id="page-message">Help Getting Projects into Babel</h1>
<div id="index-page">
	


	<a href="https://bugs.eclipse.org/bugs/enter_bug_wizard.cgi"><img src="<?php echo imageRoot() ?>/large_icons/apps/system-users.png"><h2>Eager Translators</h2></a>
	<br style='clear: both;'>
	<p>
	If Babel is missing an Eclipse project and you would like to help translate this project, follow the steps below to open a Bugzilla bug. 
	Opening a bug is the first step in encourage the project to participate in Babel by letting them know translators are out there and willing to help.
	</p>
	<ol>
		<li><a target="_blank" href="https://bugs.eclipse.org/bugs/enter_bug_wizard.cgi">Click here</a> to start the process of opening a bug on Eclipse Bugzilla system.
		<li>Follow the instructions and find the project you want included in Babel.
		<li>Leave a comment that politely asks the project leads to import their project into Babel. 
			<br>Example:<p>"The Babel project aims to help Eclipse projects get translated into new languages.  Your project is not currently integrated with Babel.
			Please follow these instructions: http://babel.eclipse.org/babel/importing.php"
		<li>Submit the bug. 
	</ol> 

	<a href="login.php"><img src="<?php echo imageRoot() ?>/large_icons/apps/preferences-desktop-theme.png"><h2>Project Leads</h2></a>
	<br style='clear: both;'>
	<p>
	If you are a project lead and your project is not included in Babel then follow the steps below.
	</p>
	<ol>
		<li><a href="login.php">Log into Babel</a>.
		<li>Click on the 'FOR COMMITTERS' link at the top left hand corner of the web page.
		<li>Follow the instructions on that page, good luck!
	</ol>	
		
</div>

<script>YAHOO.languageManager.getAjaxLanguages();</script>

<?php
	global $addon;
    $addon->callHook("footer");
?>