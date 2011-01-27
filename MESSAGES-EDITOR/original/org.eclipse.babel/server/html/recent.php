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

$pageTitle 		= "Babel - Recent Translations";
$pageKeywords 	= "";
$incfile 		= "content/en_recent_html_list.php";

$PROJECT_VERSION = getHTTPParameter("project_version");
$PROJECT_ID = "";
$VERSION	= "";

if($PROJECT_VERSION != "") {
	$items = explode("|", $PROJECT_VERSION);
	$PROJECT_ID = $items[0];
	$VERSION	= $items[1];
}
$LANGUAGE_ID= getHTTPParameter("language_id");
if($LANGUAGE_ID == "") {
	$LANGUAGE_ID = $_SESSION["language"];
}

$FUZZY		= getHTTPParameter("fuzzy");
if($FUZZY == "" || $FUZZY != 1) {
	$FUZZY = 0;
}

if($LANGUAGE_ID == "All") {
	$LANGUAGE_ID = "";
}
$LIMIT 		= getHTTPParameter("limit");
if($LIMIT == "" || $LIMIT <= 0 || $LIMIT > 20000) {
	$LIMIT = 200;
}
$LAYOUT 		= getHTTPParameter("layout");
if($LAYOUT == "list" || $LAYOUT == "table") {
	$incfile = "content/en_recent_html_" . $LAYOUT . ".php";
}
$FORMAT		= getHTTPParameter("format");
if($FORMAT == "rss") {
	$incfile 		= "content/en_recent_rss.php";
}
$USERID		= getHTTPParameter("userid");
$SUBMIT 	= getHTTPParameter("submit");

$sql = "SELECT DISTINCT pv.project_id, pv.version FROM project_versions AS pv INNER JOIN map_files as m ON pv.project_id = m.project_id AND pv.version = m.version WHERE pv.is_active ORDER BY pv.project_id ASC, pv.version DESC";
$rs_p_list = mysql_query($sql, $dbh);

$sql = "SELECT language_id, IF(locale <> '', CONCAT(CONCAT(CONCAT(name, ' ('), locale), ')'), name) as name FROM languages WHERE is_active AND iso_code != 'en' ORDER BY name";
$rs_l_list = mysql_query($sql, $dbh);

$where = " t.is_active ";

if($PROJECT_ID != "") {
	$where = addAndIfNotNull($where) . " f.project_id = ";
	$where .= returnQuotedString(sqlSanitize($PROJECT_ID, $dbh));
}
if($LANGUAGE_ID != "") {
	$where = addAndIfNotNull($where) . " t.language_id = ";
	$where .= returnQuotedString(sqlSanitize($LANGUAGE_ID, $dbh));
}
if($VERSION != "") {
	$where = addAndIfNotNull($where) . "f.version = ";
	$where .= returnQuotedString(sqlSanitize($VERSION, $dbh));
}
if($USERID != "") {
	$where = addAndIfNotNull($where) . "u.userid = ";
	$where .= sqlSanitize($USERID, $dbh);
}
if($FUZZY == 1) {
	$where = addAndIfNotNull($where) . "t.possibly_incorrect = 1 ";
}

if($where != "") {
	$where = " WHERE " . $where;
}


$sql = "SELECT 
  s.name AS string_key, s.value as string_value, 
  t.value as translation,
  t.possibly_incorrect as fuzzy, 
  IF(u.last_name <> '' AND u.first_name <> '', 
  	CONCAT(CONCAT(first_name, ' '), u.last_name), 
  	IF(u.first_name <> '', u.first_name, u.last_name)) AS who,
  u.userid, 
  t.created_on, l.iso_code as language,
  f.project_id, f.version, f.name
FROM 
  translations as t 
  LEFT JOIN strings as s on s.string_id = t.string_id 
  LEFT JOIN files as f on s.file_id = f.file_id 
  LEFT JOIN users as u on u.userid = t.userid
  LEFT JOIN languages as l on l.language_id = t.language_id 
$where
ORDER BY t.created_on desc 
LIMIT $LIMIT";
$rs_p_stat = mysql_query($sql, $dbh);
global $addon;
$addon->callHook("head");
include($incfile);
$addon->callHook("footer");

?>