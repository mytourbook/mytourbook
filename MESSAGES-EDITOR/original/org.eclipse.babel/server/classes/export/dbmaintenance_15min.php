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
*******************************************************************************/

	/*
	 * This is a cronjob-driven database maintenance script
	 * It is run every 15 minutes
	 */

	require_once(dirname(__FILE__) . "/../system/backend_functions.php");
	global $addon;
	$context = $addon->callHook('context');
	
	require(dirname(__FILE__) . "/../system/dbconnection.class.php");
	  
	
	if($context == "") {
		$context = "staging";
	}
	$dbc = new DBConnection();
	$dbh = $dbc->connect();


	# refresh the scoreboard -- not every 15 minutes!
	$forceRefresh = strcasecmp(getenv("FORCE_BABEL_REFRESH"), "true");
	if(rand(1, 100) < 25 || $forceRefresh) {
		require_once(dirname(__FILE__) . "/../system/scoreboard.class.php");
		$sb = new Scoreboard();
		$sb->refresh();
		
		# Refresh file progress
		# This only needs to happen once in a while too.
		# See also: babel-setup.sql
		$sql = "select f.file_id, l.language_id, IF(COUNT(s.string_id) > 0, COUNT(t.string_id)/COUNT(s.string_id)*100,100) AS translate_percent
FROM files AS f
        INNER JOIN languages as l ON l.is_active = 1
        LEFT JOIN strings as s ON (s.file_id = f.file_id AND s.is_active AND s.value <> '' AND s.non_translatable <> 1) 
        LEFT JOIN translations AS t ON (s.string_id = t.string_id 
           AND t.language_id = l.language_id AND t.is_active = 1)
WHERE f.is_active = 1 
GROUP BY f.file_id, l.language_id
HAVING translate_percent > 0";
		$rs = mysql_query($sql, $dbh);
		while($myrow = mysql_fetch_assoc($rs)) {
			mysql_query("INSERT INTO file_progress (file_id, language_id, pct_complete)
			VALUES(" . $myrow['file_id'] . ", " . $myrow['language_id'] . ", " . $myrow['translate_percent'] . ")
			ON DUPLICATE KEY UPDATE pct_complete=" . $myrow['translate_percent'], $dbh);
		}
		mysql_query("DELETE FROM file_progress WHERE pct_complete = 0", $dbh);
	}
	
	# Update project/version/language progress 
	$sql = "SELECT * FROM project_progress WHERE is_stale";
	$rs = mysql_query($sql, $dbh);
	while($myrow = mysql_fetch_assoc($rs)) {
		mysql_query("LOCK TABLES project_progress WRITE, 
			project_versions AS v READ, 
			files AS f READ, 
			strings AS s READ, 
			translations AS t READ,
			languages AS l READ
			", $dbh);
		$sql = "DELETE /* dbmaintenance_15min.php */ FROM project_progress where project_id = '" . addslashes($myrow['project_id']) . "'
   					AND version = '" . addslashes($myrow['version']) . "' 
   					AND language_id = " . $myrow['language_id'];
		mysql_query($sql, $dbh);

		$sql = "INSERT /* dbmaintenance_15min.php */ INTO project_progress SET project_id = '" . addslashes($myrow['project_id']) . "',
   					version = '" . addslashes($myrow['version']) . "',
   					language_id = " . $myrow['language_id'] . ",
   					is_stale = 0,
   					pct_complete = (
					     SELECT	IF(COUNT(s.string_id) > 0, ROUND(COUNT(t.string_id)/COUNT(s.string_id) * 100, 2), 0) AS pct_complete
					       FROM project_versions AS v 
					       INNER JOIN files AS f 
					           ON (f.project_id = v.project_id AND f.version = v.version AND f.is_active) 
					       INNER JOIN strings AS s 
					           ON (s.file_id = f.file_id AND s.is_active AND s.value <> '' AND s.non_translatable = 0) 
					       INNER JOIN languages AS l ON l.language_id = " . $myrow['language_id'] . "
					       LEFT JOIN translations AS t 
					          ON (t.string_id = s.string_id AND t.language_id = l.language_id AND t.is_active) 
					       WHERE
					        v.project_id = '" . addslashes($myrow['project_id']) . "'
					        AND v.version = '" . addslashes($myrow['version']) . "'
					 )";
		mysql_query($sql, $dbh);
		echo mysql_error();
		
		# Let's lock and unlock in the loop to allow other queries to go through. There's no rush on completing these stats.
		mysql_query("UNLOCK TABLES", $dbh);
		sleep(2);
	}
?>
