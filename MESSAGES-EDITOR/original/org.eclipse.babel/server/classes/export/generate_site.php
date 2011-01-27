<?php
/*******************************************************************************
 * Copyright (c) 2007-2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc. bug 248845: Refactoring generate1.php into different files with a functional approach
*******************************************************************************/

/*
 * Documentation: http://wiki.eclipse.org/Babel_/_Server_Tool_Specification#Outputs
 */

ini_set("memory_limit", "64M");
require(dirname(__FILE__) . "/../system/backend_functions.php");

global $addon;
$context = $addon->callHook('context');
$work_dir = $addon->callHook('babel_working');

require(dirname(__FILE__) . "/../system/dbconnection.class.php");
require(dirname(__FILE__) . "/../system/feature.class.php");
$dbc = new DBConnection();
$dbh = $dbc->connect();

$work_context_dir = $work_dir . $context . "_site/";
$tmp_dir = $work_context_dir . "tmp/";
$output_dir = $work_context_dir . "output/";
$sites_dir = $work_context_dir . "sites/";

exec("rm -rf $work_context_dir*");
exec("mkdir -p $output_dir");
exec("mkdir -p $sites_dir");


//iterate over all the release trains
foreach(ReleaseTrain::all() as $train) {
	$features = array();
	// create a dedicated folder for each of them
	exec("mkdir -p $sites_dir/$train->id");
	// create the output folder for temporary artifacts
	$output_dir_for_train = "$output_dir/$train->id/";
	// iterate over each language
	foreach(Language::all() as $lang) {
		// create a new feature object
		$feature = new Feature($lang, $train, $tmp_dir, $output_dir_for_train);
		// make it generate itself
		$feature->generateAll();
		$feature->jar();
		$features[] = $feature;
	}
	$site = fopen("$output_dir_for_train/eclipse/site.xml", "w");
	$head = <<<HEAD
<site mirrorsURL="http://www.eclipse.org/downloads/download.php?file=/technology/babel/update-site/ganymede/site.xml&format=xml">
<description url="http://babel.eclipse.org/">

		This update site contains user-contributed translations of the strings in all Eclipse projects.
		Please see the http://babel.eclipse.org/ Babel project web pages for a full how-to-use explanation of
		these translations as well as how you can contribute to the translations of this and future versions of Eclipse.
	
</description>
HEAD;
	fwrite($site, $head);
	$version = $train->version ."_". $train->timestamp;
	foreach($features as $f) {
		$language_name = $f->language->name;
		$filename = $f->filename();
		
		$feature_text = <<<FEATURE_TEXT
<category-def name="Babel Language Packs in $language_name" label="Babel Language Packs in $language_name">
	<description>Babel Language Packs in Pseudo Translations</description>
</category-def>

<feature url="features/$filename.jar" id="$filename" version="$version">
	<category name="Babel Language Packs in $language_name"/>
</feature>
FEATURE_TEXT;
		fwrite($site, $feature_text);
	}
	fclose($site);
	exec("mv $output_dir_for_train/eclipse $sites_dir/" . $train->id . "_" . $train->timestamp);
}



