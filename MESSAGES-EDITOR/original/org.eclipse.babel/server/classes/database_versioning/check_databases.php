<?php
/*******************************************************************************
 * Copyright (c) 2007 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eclipse Foundation
*******************************************************************************/

include("check-database-schema.class.php");
include("abstractschemachecker.class.php");


global $spent_quries;
$spent_quries = array();

function mysql_remember_query($sql,$dbh){
	global $spent_quries;
	$spent_quries[] = $sql;
	return mysql_query($sql,$dbh);
}

function dirs($path){
	$dirs = array();
	$list = scandir($path);
	foreach($list as $dir){
		if($dir == "." || $dir == "..")
			continue;
		if(is_dir($path.$dir)){
			$dirs[] = $dir;
		}
	}
	
	return $dirs;
}

class context{
	function get($get){
		return $this->$get;
	}
	function database($dbname){
		return $this->dbhs[$dbname];
	}
}
$context = new context();
$context->components_directory = "tables/";
$context->dbhs['refactor_test'] = mysql_connect("localhost","root","","refactor_test");

$check = new CheckAndModifyDatabaseSchema();
$worked = $check->check_and_modify( $context );

print "\n";

print_r($spent_quries);

?>