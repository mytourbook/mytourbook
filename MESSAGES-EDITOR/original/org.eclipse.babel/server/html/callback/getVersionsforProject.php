<?php
/*******************************************************************************
 * Copyright (c) 2007 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Paul Colton (Aptana)- initial API and implementation

*******************************************************************************/
require_once("cb_global.php");



$return = "";
$language = "";
if(isset($_SESSION['language']) && isset($_SESSION['project'])) {
	$language =  $_SESSION['language'];
	$query = "select DISTINCT
		f.version,
		f.project_id, 
		IF(ISNULL(pct_complete),0,ROUND(pct_complete,1)) AS pct_complete
	from 
		project_versions AS v
		INNER JOIN files as f on (f.project_id = v.project_id AND f.version = v.version)
		LEFT JOIN project_progress AS p ON (p.project_id = v.project_id AND p.version = v.version and p.language_id = " . addslashes($language) . ")
	where 
		v.is_active = 1
		and f.version != 'unspecified'
		and v.project_id = '".addslashes($_SESSION['project'])."'
	order by
		f.version desc";

	$res = mysql_query($query,$dbh);

	$return = array();

	while($line = mysql_fetch_array($res, MYSQL_ASSOC)){
		$ret = Array();
		$ret['version'] = $line['version'];
		$ret['pct'] = $line['pct_complete'];
	
		if(isset($_SESSION['version']) and $line['version'] == $_SESSION['version']){
			$ret['current'] = true;
		}
		$return[] = $ret;
	}
}
//print $query."\n";
print json_encode($return);

?>