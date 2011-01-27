<?php
/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eclipse Foundation - initial API and implementation
*******************************************************************************/
require_once("cb_global.php");

$return = array();

$tr_string = getHTTPParameter("tr_string", "POST");

if(isset($_SESSION['language']) and isset($_SESSION['version']) and isset($_SESSION['project'])){
	$language = $_SESSION['language'];
	$version = $_SESSION['version'];
	$project_id = $_SESSION['project'];
}else{
	return false;
}


$query = "SELECT DISTINCT t.value 
	FROM translations as t 
		INNER JOIN strings AS s ON s.string_id = t.string_id 
	WHERE s.value like '%" . addslashes($tr_string). "%' 
		AND t.is_active
		AND t.language_id = '".addslashes($language)."'
	ORDER BY LENGTH(t.value) ASC LIMIT 15";
# print $query."\n";

$res = mysql_query($query,$dbh);
if(mysql_affected_rows($dbh) > 0) {
	echo "<ul>";
	while($line = mysql_fetch_array($res, MYSQL_ASSOC)){
		echo "<li>" . $line['value'] . "</li>";
	}
	echo "</ul>";
}
else {
	echo "No hints found.";
}

?>