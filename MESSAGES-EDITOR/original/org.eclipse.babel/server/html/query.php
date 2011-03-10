<?php
/*******************************************************************************
 * Copyright (c) 2007-2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc. bug 255775: Open a REST API for people to query for translations
*******************************************************************************/

header("Content-type: text/plain");
include("global.php");
InitPage("");

global $dbh;


$value = $_GET['string'];
$nl = $_GET['nl'];

if (!$value || !$nl) {
	exit();
}

// we could add more checks on the values being entered.

// we also need to discuss how to deal with the encoding of the value, 
// since & and spaces should be encoded for example

$value = html_entity_decode($value);

$possible_translations = mysql_query(
	"SELECT t.value 
		from strings As s inner join translations AS t on s.string_id = t.string_id
					inner join languages As l on l.language_id = t.language_id
					where s.value = BINARY '" . addslashes($value) . "' 
					and l.iso_code = '" . addslashes($nl) . "' ");
		
if ($possible_translations and (($translation_row = mysql_fetch_assoc($possible_translations)) != null)) {
		echo $translation_row['value'];
}

?>