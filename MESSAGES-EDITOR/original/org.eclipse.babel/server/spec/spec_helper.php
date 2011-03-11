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
// this file should be included in the spec files.
// it will open a connection to a test database.

error_reporting(E_ALL);
ini_set('display_errors', '1');

require_once(dirname(__FILE__) . "/../classes/system/dbconnection.class.php");
	
DBConnection::connect(dirname(__FILE__) . '/test.ini');
?>
