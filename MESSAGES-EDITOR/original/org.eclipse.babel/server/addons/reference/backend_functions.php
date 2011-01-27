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

// Use a class to define the hooks to avoid bugs with already defined functions.
class Reference_backend {
    /*
     * Authenticate a user.
     * Returns the User object if the user is found, or false
     */
    function authenticate($User, $email, $password) {
        $User->userid = 5;
    }
    
    /**
     * Returns a user that is specialized in running the syncup script.
     */
    function syncupUser() {
        $User = new User();
        $User->loadFromID(1);
    }
    
    /**
     * Returns the genie user that represents the headless admin for most operations,
     * like importing a zip of translations.
     */
    function genieUser() {
        $User = new User();
        $User->loadFromID(1);
    }

	/**
	 * Returns the name of the current context, one of live, staging or dev.
	 */
    function context() {
        return "reference";
	}
	
	/**
	 * Returns a hash of the parameters.
	 */
	function db_params() {
		return array('db_read_host' => '',
					 'db_read_user' => '',
					 'db_read_pass' => '', 
					 'db_read_name' => '');
	}
	
	/**
	 * Deals with error messages.
	 */
	function error_log() {
		$args = func_get_args();
		error_log($args);
	}
	
	/**
	 * Returns the name of the directory Babel should use to work in.
	 */
	function babel_working() {
		return "/home/babel-working/";
	}
}

function __register_backend_ref($addon) {
    $addon->register('user_authentication', array('Reference_backend', 'authenticate'));
    $addon->register('syncup_user', array('Reference_backend', 'syncupUser'));
    $addon->register('genie_user', array('Reference_backend', 'genieUser'));
	$addon->register('context', array('Reference_backend', 'context'));
	$addon->register('db_params', array('Reference_backend', 'db_parameters'));
	$addon->register('error_log', array('Reference_backend', 'error_log'));
	$addon->register('babel_working', array('Reference_backend', 'babel_working'));
}

global $register_function_backend;
$register_function_backend = '__register_backend_ref';

?>
