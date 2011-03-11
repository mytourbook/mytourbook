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

error_reporting(E_ALL);
ini_set('display_errors', '1');
// This script exports all the translations for each language as a CSV file.

ini_set("memory_limit", "256M");
require(dirname(__FILE__) . "/../system/backend_functions.php");
global $addon;
$context = $addon->callHook('context');

require(dirname(__FILE__) . "/../../classes/system/dbconnection.class.php");
require(dirname(__FILE__) . "/../system/feature.class.php");
$dbc = new DBConnection();
$dbh = $dbc->connect();

$work_dir = $addon->callHook('babel_working');

$work_context_dir = $work_dir . $context . "_csv/";
$tmp_dir = $work_context_dir . "tmp/";
$output_dir = $work_context_dir . "output/";

exec("rm -rf $tmp_dir");
exec("mkdir -p $tmp_dir");
exec("mkdir -p $output_dir");

//iterate over all the release trains
foreach(ReleaseTrain::all() as $train) {
	// create a dedicated folder for each of them
	exec("mkdir -p $work_context_dir/$train->id");
	// create the output folder for temporary artifacts
	$output_dir_for_train = "$output_dir/$train->id/";
	exec("mkdir -p $output_dir_for_train");
	// iterate over each language
	foreach(Language::all() as $lang) {
		// create a new feature object
		$feature = new Feature($lang, $train, $tmp_dir, $output_dir_for_train);
		// make it generate itself
		$feature->generateAsCSV();
		// now zip it directly
		$featureZip = $feature->zipAsCSV("$work_context_dir/$train->id");
		// output the creation of the feature notification
		echo "Feature created here: $featureZip\n";
	}
	break;
}