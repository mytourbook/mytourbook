<?php
/*******************************************************************************
 * Copyright (c) 2009 Intalio, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc. bug 260493: Provide a CSV export and import script
*******************************************************************************/

if ($argc != 4) {
	$help = <<<HELP
Copyright (c) 2009 Intalio, Inc.
Usage: import_from_csv.php language release_train csv_file

  language: the code of the language to use. See the languages supported by Babel.

  release_train: a release train name.

  csv_file: the path to the csv_file. The path should either be relative to the php file or absolute.
            The CSV file should be comma separated. It should have this structure: "translation_key", "translation_english_value", "translation_value"

  Example:
    import_from_csv.php fr_CA europa some_translations_fr.csv

  Authors:
    This script was written by the Babel project. Please see http://www.eclipse.org/babel for more information.


HELP;
    echo $help;
	exit;
}

error_reporting(E_ALL); ini_set("display_errors", true);


ini_set("memory_limit", "256M");
require_once(dirname(__FILE__) . "/../system/backend_functions.php");
require_once(dirname(__FILE__) . "/../system/dbconnection.class.php");
require_once(dirname(__FILE__) . "/../system/feature.class.php");
require_once(dirname(__FILE__) . "/../system/user.class.php");

$dbc = new DBConnection();
$dbh = $dbc->connect();

//translations coming from a CSV file are considered to come from professional translators that tested their work in context, and/or in relation with the developers
// change the variable below if you have doubts on the translation quality.
$fuzzy = 0;

//user that will be running the translations.
$USER = getGenieUser()->userid;

$language = $argv[1];
$release_train_id = $argv[2];
$csv_file = $argv[3];

$sql = "select language_id from languages where iso_code = '" . addslashes($language) ."'";
$lrow = mysql_fetch_assoc(mysql_query($sql));
if (!$lrow) {
	echo "This language code is not supported by Babel. Please see the Babel documentation for more information";
	exit;
}
$language_id = $lrow['language_id'];


$handle = fopen($csv_file, "r");
while (($data = fgetcsv($handle)) !== FALSE) {
	$sql = <<<SQL
SELECT s.string_id FROM files AS f INNER JOIN strings AS s ON f.file_id = s.file_id INNER JOIN release_train_projects as v ON (f.project_id = v.project_id AND f.version = v.version) WHERE f.is_active AND s.non_translatable <> 1 AND s.name = '$data[0]' AND s.value = BINARY '$data[1]' AND v.train_id = '$release_train_id'

SQL;
    $values = mysql_query($sql);
    $value_row = mysql_fetch_assoc($values);
    if (!$value_row) {
	    echo "Could not find the matching record for $data[0] with a value of $data[1]";
	    continue;
    }
    $string_id = $value_row['string_id'];
    
    $sql = "select possibly_incorrect from translations where string_id = $string_id and language_id = $language_id";
    $tr_row = mysql_fetch_assoc(mysql_query($sql));
    if ($tr_row) {
	    if ($fuzzy == 1) {
		    if ($tr_row['possibly_incorrect'] == 1) {
			    // replacing a fuzzy translation by a new fuzzy translation. Well.
		    } else {
			    //replacing a non-fuzzy translation by a fuzzy translation: no, thank you
			    echo "The entry " . $data[0] . " is already translated in a non-fuzzy way. Aborting";
			    continue;
		    }
	    } else {
		    // we are not fuzzy, for now let's assume it's ok to override non-fuzzy translations when yours aren't either.
	    }
	    $query = "UPDATE translations set is_active = 0 where string_id = " . $string_id . " and language_id = '" . $language_id . "'";
	    mysql_query($query);
    }
    $query = "INSERT INTO translations(string_id, language_id, value, userid, created_on, possibly_incorrect) values('". addslashes($string_id) ."','".  addslashes($language_id) ."','" . addslashes($data[2]) . "', '". addslashes($USER) ."', NOW(), $fuzzy)";
    mysql_query($query);
    echo "Added translation \"$data[2]\" for entry '$data[0]'\n";
}
fclose($handle);
echo "\n\nDone.\n";

?>