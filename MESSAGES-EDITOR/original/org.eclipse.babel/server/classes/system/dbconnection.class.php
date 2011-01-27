<?php

require_once(dirname(__FILE__) . "/backend_functions.php");
class DBConnection {

	#*****************************************************************************
	#
	# dbconnection.class.php
	#
	# Author: 		Denis Roy
	# Date:			2004-08-05
	#
	# Description: Functions and modules related to the MySQL database connection
	#
	# HISTORY:
	#
	#*****************************************************************************

	function connect()
	{
		static $dbh;
		global $addon;
		$db_params = $addon->callHook('db_params');
  
		$dbh = mysql_connect($db_params['db_read_host'],$db_params['db_read_user'],$db_params['db_read_pass']);
		if (!$dbh) {
			errorLog("Failed attempt to connect to server - aborting.");
			exitTo("/error.php?errNo=101301","error: 101301 - data server can not be found");
		}

    	$database = $db_params['db_read_name'];
		if (isset($database)) {
			if (!mysql_select_db($database)) {
				errorLog("Failed attempt to open database: $database - aborting \n\t" . mysql_error());
				exitTo("/error.php?errNo=101303","error: 101303 - unknown database name");
			}
		}					
		
		return $dbh;
	}
	
	function disconnect() {
		mysql_close();
	}
}
?>