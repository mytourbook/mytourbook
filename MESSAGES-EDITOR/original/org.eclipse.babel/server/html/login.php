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
 *    Eclipse Foundation
 *    IBM corporation (Matthew Mazaika) - bug 242915 
*******************************************************************************/
include("global.php");
InitPage("");

require_once(dirname(__FILE__) . "/../classes/system/user.class.php");
require_once(dirname(__FILE__) . "/../classes/system/session.class.php");

$pageTitle 		= "Contribute Translations to Babel";
$pageKeywords 	= "translation,language,nlpack,pack,eclipse,babel";

$USERNAME 	= getHTTPParameter("username", "POST");
$PASSWORD 	= getHTTPParameter("password", "POST");
$REMEMBER 	= getHTTPParameter("remember", "POST");
$SUBMIT 	= getHTTPParameter("submit");

if(!isset($_SESSION['login_failed_attempts'])){
	$_SESSION['login_failed_attempts'] = array();
}

if($SUBMIT == "Login") {
	if($USERNAME != "" && $PASSWORD != ""){
		$User = new User();
		if(!$User->load($USERNAME, $PASSWORD)) {
			foreach($_SESSION['login_failed_attempts'] as $timestamp){
				if($timestamp < strtotime("2 minute ago")){
					unset($_SESSION['login_failed_attempts'][$timestamp]);
				}
			}
			$_SESSION['login_failed_attempts'][] = strtotime('now');
			if(count($_SESSION['login_failed_attempts']) > 2){
				$GLOBALS['g_ERRSTRS'][0] = "Authentication failed.  <b>If you just created a NEW BUGZILLA ACCOUNT wait a few minutes and try again</b>.";
			}else{
				$GLOBALS['g_ERRSTRS'][0] = "Authentication failed.  Please verify your username and/or password are correct.";
			}
			// we couldn't authenticate, therefore we don't have a user anymore
			$User = null;
		}
		else {
			# create session
			$Session = new Session();
			$Session->create($User->userid, $REMEMBER);
			SetSessionVar('User', $User);
			if(isset($_SESSION['s_pageLast'])) {
				if($_SESSION['s_pageLast'] != "") {
					exitTo($_SESSION['s_pageLast']);
				}
				else {
					exitTo("translate.php");
				}
			}
			else {
				exitTo("translate.php");
			}
		}
	}
	else {
		$GLOBALS['g_ERRSTRS'][0] = "Your username and password must not be empty.";
	}
}
if($SUBMIT == "Logout") {
	$Session = new Session();
	$Session->destroy();
	// we're logging out, therefore we don't have a user anymore
	$User = null;
	$GLOBALS['g_ERRSTRS'][0] = "You have successfully logged out.  You can login again using the form below.";
}

# TODO: finish the intro text


global $addon;
$addon->callHook("head");

include("content/en_login.php");

global $addon;
$addon->callHook("footer");
?>