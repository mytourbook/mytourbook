<?php
/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Intalio, Inc. - Initial API and implementation
 *    Antoine Toulme - Initial contribution.
*******************************************************************************/

ini_set("memory_limit", "64M");

error_reporting(E_ALL);
ini_set('display_errors', '1');

require(dirname(__FILE__) . "/../system/dbconnection.class.php");
$dbc = new DBConnection();
$dbh = $dbc->connect();

require_once(dirname(__FILE__) . "/../system/backend_functions.php");

if( !function_exists('json_encode') ){
	require("/home/data/httpd/babel.eclipse.org/html/json_encode.php");
	function json_encode($encode){
 		$jsons = new Services_JSON();
		return $jsons->encode($encode);
	}
}

$User = getSyncupUser();

$dbc = new DBConnection();
global $dbh;
$dbh = $dbc->connect();

echo "Connection established, ready to begin; The syncup user id is $User->userid \n";
$langs = mysql_query( "SELECT language_id FROM languages where languages.is_active" );
while( ($lang_row = mysql_fetch_assoc($langs)) != null ) {
	$language_id = $lang_row['language_id'];
    echo "Investigating language " . $language_id . "\n\n";
	$untranslated_strings = mysql_query( "SELECT * from strings where is_active and value <> '' and string_id not in(select string_id from translations where language_id=". $language_id .")" );
	$count = 0;
    while ( ($string_row = mysql_fetch_assoc($untranslated_strings)) != null) {
    	$count++;
    	
    	if($count % 10000 == 0) {
    		echo "Processed " . $count . " strings (language_id=$language_id)... \n";
    	}
    	
		$untranslated_value = $string_row['value'];
		$untranslated_id = $string_row['string_id'];
		
		# This query split in two for added performance.
		# See bug 270485
		$string_ids = "";
		# BINARY the lookup value instead of the field to support an index
		$rs = mysql_query( "SELECT s.string_id FROM strings AS s WHERE s.value = BINARY '" . addslashes($untranslated_value) . "'");
		while ( ($row = mysql_fetch_assoc($rs)) != null) {
			if(strlen($string_ids) > 0) {
				$string_ids .= ",";
			}
			$string_ids .= $row['string_id'];
		}
		$possible_translations = mysql_query( "SELECT t.value from strings As s inner join translations AS t on s.string_id = t.string_id where s.string_id IN ($string_ids) and t.language_id = '" . $language_id . "' and t.is_active ");
       	if ($possible_translations and (($translation_row = mysql_fetch_assoc($possible_translations)) != null)) {
			$translation = $translation_row['value'];
           	$query = "INSERT INTO translations(string_id, language_id, value, userid, created_on, possibly_incorrect) values('". addslashes($untranslated_id) ."','". addslashes($language_id) ."','" . addslashes($translation) . "', '". addslashes($User->userid) ."', NOW(), 1)";
           	echo "\tTranslating ". addslashes($untranslated_id) ." with: " . addslashes($translation) . "\n";
			mysql_query($query);
		}
    }
}
?>
