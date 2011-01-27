<?php
/*******************************************************************************
 * Copyright (c) 2007-2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Paul Colton (Aptana)- initial API and implementation
 *    Eclipse Foundation
 *    Scott Reynen scott at randomchaos com - toescapedunicode
*******************************************************************************/

define("COOKIE_REMEMBER",	"cBABEL");
define("COOKIE_SESSION" ,	"sBABEL");

require(dirname(__FILE__) . '/html_functions.php');

$GLOBALS['g_LOADTIME'] = microtime();
require(dirname(__FILE__) . "/../classes/system/dbconnection.class.php");
require(dirname(__FILE__) . "/../classes/system/event_log.class.php");
require_once(dirname(__FILE__) . "/../classes/system/user.class.php");



session_name(COOKIE_SESSION);
session_start();
extract($_SESSION);


function InitPage($login) {
  $page = $login;
  $lastPage = GetSessionVar('s_pageName');
  $User = GetSessionVar('User');
  
  if (empty($GLOBALS['page']))
	  $GLOBALS['page'] = '';
	
  if((strpos($_SERVER['REQUEST_URI'], "login.php") == FALSE) &&
	 (strpos($_SERVER['REQUEST_URI'], "callback") == FALSE)) {
	  	SetSessionVar('s_pageLast', $_SERVER['REQUEST_URI']);
  }
  
  $dbc = new DBConnection();
  global $dbh;
  $dbh = $dbc->connect();

  if($login == "login" && !$User) {
  	# Login required, but the User object isn't there.
  
  	if(isset($_COOKIE[COOKIE_REMEMBER])) {
  		# Try to fetch username from session
  		require_once(dirname(__FILE__) . "/../classes/system/session.class.php");
  		$Session = new Session();

  		if(!$Session->validate()) {
    		exitTo("login.php");
  		}
  		else {
  			$User = new User();
  			$User->loadFromID($Session->_userid);
  			SetSessionVar("User", $User);
  		}
  	}
  	else {
  		exitTo("login.php");
  	}
  }
  
  $GLOBALS['g_PHPSELF']  = $GLOBALS['page'];
  $GLOBALS['g_PAGE']     = $page;
  $GLOBALS['g_SITEURL']  = $_SERVER['HTTP_HOST'];
  $GLOBALS['g_SITENAME'] = substr($GLOBALS['g_SITEURL'],0,strlen($GLOBALS['g_SITEURL'])-4);
  $GLOBALS['g_TITLE']    = $GLOBALS['g_SITENAME'];
  $GLOBALS['g_ERRSTRS']  = array("","","","","","","","","","","",);
  $GLOBALS['DEBUG']      = "";
}



?>