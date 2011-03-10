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
 *    Matthew Mazaika <mmazaik  us.ibm.com> - bug 242011
*******************************************************************************/

require_once(dirname(__FILE__) . "/backend_functions.php");

class User {
  public $errStrs;
  
  public $userid              = 0;
  public $username            = '';
  public $first_name          = '';
  public $last_name           = '';
  public $email               = '';
  public $primary_language_id = 0;
  public $hours_per_week      = 0;
  public $is_committer		  = 0;
  public $updated_on          = '';
  public $updated_at          = '';
  public $created_on          = '';
  public $created_at          = '';

	function load($email, $password) {
		if($email != "" && $password != "") {
			if (eregi('^[a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\.[a-zA-Z.]{2,5}$', $email)) {
				global $addon;
				$addon->callHook('user_authentication', array(&$this, $email, $password));
			}
		}
		
		if($this->userid > 0) {
			$Event = new EventLog("users", "userid", $this->userid, "__auth_success");
			$Event->add();
		}
		else {
			$Event = new EventLog("users", "userid", $_SERVER['REMOTE_ADDR'] . ":" . $email, "__auth_failure");
			$Event->add();
		}
		return $this->userid;
	}
	
	function loadFromID($_userid) {
		$rValue = false;
		if($_userid != "") {
			global $dbh;
			
			$_userid	= sqlSanitize($_userid, $dbh);

			$sql = "SELECT *
				FROM 
					users 
				WHERE userid = $_userid";
			$result = mysql_query($sql, $dbh);
			if($result && mysql_num_rows($result) > 0) {
				$rValue = true;
				$myrow = mysql_fetch_assoc($result);
				
				$this->userid              = $myrow['userid'];
				$this->username            = $myrow['username'];
				$this->first_name          = $myrow['first_name'];
				$this->last_name           = $myrow['last_name'];
				$this->email               = $myrow['email'];
				$this->primary_language_id = $myrow['primary_language_id'];
				$this->is_committer			= $myrow['is_committer'];
				$this->hours_per_week      = $myrow['hours_per_week'];
				$this->updated_on          = $myrow['updated_on'];
				$this->updated_at          = $myrow['updated_at'];
				$this->created_on          = $myrow['created_on'];
				$this->created_at			= $myrow['created_at'];
			}
			else {
				$GLOBALS['g_ERRSTRS'][1] = mysql_error();
			}
		}
		return $rValue;
	}
}
?>