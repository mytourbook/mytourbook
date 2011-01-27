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
 *    Kit Lo (IBM) - patch, bug 261739, Inconsistent use of language names
*******************************************************************************/
include("global.php");

InitPage("");

global $User;
global $dbh;

$pageTitle 		= "Babel - Translation statistics";
$pageKeywords 	= "";
$incfile 		= "content/en_stats.php";

$PROJECT_VERSION = getHTTPParameter("project_version");
$PROJECT_ID = "";
$VERSION	= "";

if($PROJECT_VERSION != "") {

	$items = explode("|", $PROJECT_VERSION);
	$PROJECT_ID = $items[0];
	$VERSION	= $items[1];
}
$LANGUAGE_ID= getHTTPParameter("language_id");
$SUBMIT 	= getHTTPParameter("submit");

$sql = "SELECT DISTINCT pv.project_id, pv.version FROM project_versions AS pv INNER JOIN map_files as m ON pv.project_id = m.project_id AND pv.version = m.version WHERE pv.is_active ORDER BY pv.project_id ASC, pv.version DESC";
$rs_p_list = mysql_query($sql, $dbh);

$sql = "SELECT language_id, IF(locale <> '', CONCAT(CONCAT(CONCAT(name, ' ('), locale), ')'), name) as name FROM languages WHERE is_active AND iso_code != 'en' ORDER BY name";
$rs_l_list = mysql_query($sql, $dbh);


$where = "";

if($PROJECT_ID != "") {
	$where = addAndIfNotNull($where) . " p.project_id = ";
	$where .= returnQuotedString(sqlSanitize($PROJECT_ID, $dbh));
}
if($LANGUAGE_ID != "") {
	$where = addAndIfNotNull($where) . " l.language_id = ";
	$where .= returnQuotedString(sqlSanitize($LANGUAGE_ID, $dbh));
}
if($VERSION != "") {
	$where = addAndIfNotNull($where) . "p.version = ";
	$where .= returnQuotedString(sqlSanitize($VERSION, $dbh));
}

if($where != "") {
	$where = " WHERE " . $where;
}

$sql = "SELECT p.project_id, p.version, l.name, l.locale, p.pct_complete FROM project_progress AS p INNER JOIN languages AS l ON l.language_id = p.language_id $where ORDER BY p.pct_complete DESC, p.project_id, p.version, l.name";
$rs_p_stat = mysql_query($sql, $dbh);

global $addon;
$addon->callHook("head");
include($incfile);
$addon->callHook("footer");

?>