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

?>

<h1 id="page-message">Welcome to the Babel Project</h1>
<div id="contentArea">

<h2>Quick Stats</h2>
<ul id="users-quick-stats">
	<li>Total Translations: <?= getTotoalNumberOfTranslationsByUser(); ?>
	<li>Rating of Translations: <?= getOverallRattingofTranslationsbyUser(); ?>
</ul>

<h2>Your Translation History</h2>
<ul id="users-translation-history">
</ul>

<h2>Project that need your help</h2>
<ul id="projects-need-translations">
</ul>


</div>
<?php
	global $addon;
    $addon->callHook("footer");
?>