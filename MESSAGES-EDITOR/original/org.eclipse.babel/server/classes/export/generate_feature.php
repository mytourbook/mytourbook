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

require(dirname(__FILE__) . "/../system/dbconnection.class.php");
require(dirname(__FILE__) . "/../system/feature.class.php");
$dbc = new DBConnection();
$dbh = $dbc->connect();


$work_dir = $addon->callHook('babel_working');

$work_context_dir = $work_dir . $context . "_feature/";
$tmp_dir = $work_context_dir . "tmp/";
$output_dir = $work_context_dir . "output/";
$features_dir = $work_context_dir . "features/";

exec("rm -rf $work_context_dir*");
exec("mkdir -p $output_dir");
exec("mkdir -p $features_dir");

//iterate over all the release trains
foreach(ReleaseTrain::all() as $train) {
	// create a dedicated folder for each of them
	exec("mkdir -p $features_dir/$train->id");
	// create the output folder for temporary artifacts
	$output_dir_for_train = "$output_dir/$train->id/";
	// iterate over each language
	foreach(Language::all() as $lang) {
		// create a new feature object
		$feature = new Feature($lang, $train, $tmp_dir, $output_dir_for_train);
		// make it generate itself
		$feature->generateAll();
		// now zip it directly
		$featureZip = $feature->zip("$features_dir/$train->id");
		// output the creation of the feature notification
		echo "Feature created here: $featureZip\n";
	}
}
