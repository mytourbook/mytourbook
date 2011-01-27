<?php
/*******************************************************************************
 * Copyright (c) 2007-2009 Intalio, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc.
*******************************************************************************/

require (dirname(__FILE__) . "/../backend_functions.php");

// Use a class to define the hooks to avoid bugs with already defined functions.
class BabelEclipseOrg_backend_staging extends BabelEclipseOrg_backend {
    
	/**
	 * Returns the name of the current context, one of live, staging or dev.
	 */
    function context() {
        $ini = @parse_ini_file(dirname(__FILE__) . '/base.conf');
		if (!$ini) {
			die("Could not read the configuration file " . dirname(__FILE__) . "/base.conf");
		}
		return $ini["context"];
	}
	
	/**
	 * Returns a hash of the parameters.
	 */
	function db_params() {
		$ini = @parse_ini_file(dirname(__FILE__) . '/base.conf');
		if (!$ini) {
			die("Could not read the configuration file " . dirname(__FILE__) . "/base.conf");
		}
		return array('db_read_host' => $ini['db_read_host'],
					 'db_read_user' => $ini['db_read_user'],
					 'db_read_pass' => $ini['db_read_pass'], 
					 'db_read_name' => $ini['db_read_name']);
	}

}

function __register_backend_staging($addon) {
    __register_backend($addon);
	$addon->register('context', array('BabelEclipseOrg_backend_staging', 'context'));
	$addon->register('db_params', array('BabelEclipseOrg_backend_staging', 'db_params'));
}

global $register_function_backend;
$register_function_backend = '__register_backend_staging';

?>
