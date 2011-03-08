<?php
/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eclipse Foundation - Initial API and implementation
 *    Kit Lo (IBM) - patch, bug 266010, Map file table does not show release train and file name info
*******************************************************************************/
include("global.php");

InitPage("");

global $User;
global $dbh;

if(!isset($User->userid)) {
	exitTo("importing.php");
}

if($User->is_committer != 1) {
	exitTo("login.php?errNo=3214","error: 3214 - you must be an Eclipse committer to access this page.");
}

require(dirname(__FILE__) . "/../classes/file/file.class.php");


$pageTitle 		= "Babel - Define Map Files";
$pageKeywords 	= "";
$incfile 		= "content/en_map_files.php";

$PROJECT_ID = getHTTPParameter("project_id");
$VERSION	= getHTTPParameter("version");
$TRAIN_ID 	= getHTTPParameter("train_id");
$LOCATION	= getHTTPParameter("location");
$FILENAME	= getHTTPParameter("filename");
$SUBMIT 	= getHTTPParameter("submit");

if($SUBMIT == "Save") {
	if($PROJECT_ID != "" && $VERSION != "" && $LOCATION != "") {
		$sql = "INSERT INTO map_files VALUES ("
			. returnQuotedString(sqlSanitize($PROJECT_ID, $dbh))
			. "," . returnQuotedString(sqlSanitize($VERSION, $dbh))
			. "," . returnQuotedString(sqlSanitize($FILENAME, $dbh))
			. "," . returnQuotedString(sqlSanitize($LOCATION, $dbh))
			. ", 1)";
		mysql_query($sql, $dbh);
		$LOCATION = "";
		$FILENAME = "";
		
		# Save the project/train association
		$sql = "DELETE FROM release_train_projects WHERE project_id = "
			. returnQuotedString(sqlSanitize($PROJECT_ID, $dbh)) 
			. " AND version = " . returnQuotedString(sqlSanitize($VERSION, $dbh));
		mysql_query($sql, $dbh);
		$sql = "INSERT INTO release_train_projects SET project_id = "
			. returnQuotedString(sqlSanitize($PROJECT_ID, $dbh)) 
			. ", version = " . returnQuotedString(sqlSanitize($VERSION, $dbh))
			. ", train_id = " . returnQuotedString(sqlSanitize($TRAIN_ID, $dbh));
		mysql_query($sql, $dbh);
	}
	else {
		$GLOBALS['g_ERRSTRS'][0] = "Project, version and URL cannot be empty.";  
	}
}
if($SUBMIT == "delete") {
	$SUBMIT = "showfiles";
	$sql = "DELETE FROM map_files WHERE  
	project_id = " . returnQuotedString(sqlSanitize($PROJECT_ID, $dbh)) . "
	AND version = " . returnQuotedString(sqlSanitize($VERSION, $dbh)) . "
	AND filename = ". returnQuotedString(sqlSanitize($FILENAME, $dbh)) . " LIMIT 1";
	mysql_query($sql, $dbh);
}

if($SUBMIT == "showfiles") {
	$incfile 	= "content/en_map_files_show.php";
	$sql = "SELECT m.project_id, m.version, r.train_id, m.location, m.filename FROM map_files m
	LEFT JOIN release_train_projects r ON m.project_id = r.project_id AND m.version = r.version
	WHERE m.is_active = 1 
	AND m.project_id = " . returnQuotedString(sqlSanitize($PROJECT_ID, $dbh)) . "
	AND m.version = " . returnQuotedString(sqlSanitize($VERSION, $dbh));
	$rs_map_file_list = mysql_query($sql, $dbh);
	include($incfile);
}
else {
	$sql = "SELECT project_id FROM projects WHERE is_active = 1 ORDER BY project_id";
	$rs_project_list = mysql_query($sql, $dbh);
	
	$sql = "SELECT project_id, version FROM project_versions WHERE is_active = 1 and version != 'unspecified' ORDER BY project_id ASC, version DESC";
	$rs_version_list = mysql_query($sql, $dbh);

	$sql = "SELECT DISTINCT train_id FROM release_train_projects ORDER BY train_id ASC";
	$rs_train_list = mysql_query($sql, $dbh);
	
	$sql = "SELECT train_id, project_id, version FROM release_train_projects ORDER BY project_id, version ASC";
	$rs_train_project_list = mysql_query($sql, $dbh);
	
	global $addon;
    $addon->callHook("head");
	include($incfile);
    $addon->callHook("footer");
}


?>