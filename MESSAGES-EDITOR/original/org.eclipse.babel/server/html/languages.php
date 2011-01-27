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
 *    Kit Lo (IBM) - Beautify Babel Supported Languages table
*******************************************************************************/
require(dirname(__FILE__) . "/global.php");
require_once(dirname(__FILE__) . "/../classes/system/language.class.php");
InitPage("");

$pageTitle 		= "Babel Project - Eclipse translation";
$pageKeywords 	= "translation,language,nlpack,pack,eclipse,babel,english,french,german,chinese,japanese,spanish,arabic,hebrew,hungarian,polish,italian,russian,dutch,finnish,greek,norwegian,sweedish,turkish";

global $addon;
$addon->callHook("head");

?>
<style>
	table {
		margin-left: auto;
		margin-right: auto;
	}

	.head {
		background-color: SteelBlue;
		color: white;
		margin: 0px;	
		font-size: 14px;
		padding: 2px;
	}

	.odd {
		background-color: LightSteelBlue;
	}

	.foot {
		background-color: LightGray;
	}
</style>

<h1 id="page-message">Babel Supported Languages</h1></br>

<table width=512>
<tr class="head">
	<td><b>Language</b></td><td><b>ISO</b></td>
</tr>
<?php
$languages = Language::all();
foreach ($languages as $lang) {
	$rowcount++;
	$class="";
	if($rowcount % 2) {
		$class="class='odd'";
	}
	echo "<tr $class>";
	if ($lang->iso == "en_AA") {
		$rowcount--;
	    continue;
	}
$row = <<<ROW
	<td>$lang->name</td><td>$lang->iso</td>
ROW;
	echo $row;
	echo "</tr>";
}
?>
<tr class="foot">
	<td colspan=2><?= $rowcount ?> <?= $rowcount > 1 ? "languages" : "language" ?> found</td>
</tr>
</table>

<p align=center>Please <a href="https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Babel&component=Server&bug_file_loc=<?= $_SERVER['SCRIPT_NAME']; ?>">
contact us</a> if the language you need is missing.</p>

<?php
	global $addon;
    $addon->callHook("footer");
?>