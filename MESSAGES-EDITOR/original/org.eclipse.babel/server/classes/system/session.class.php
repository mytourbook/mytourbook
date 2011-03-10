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
*******************************************************************************/

class Session {
	public $_id         = '';
	public $_userid     = '';
	public $_gid		= '';
	public $_subnet     = '';
	public $_updated_at = '';

	function validate() {
		$cookie = (isset($_COOKIE[COOKIE_REMEMBER]) ? $_COOKIE[COOKIE_REMEMBER] : "");
		$rValue = 0;
		if ($cookie != "") {
			if ( (!$this->load($cookie))
				|| $this->getSubnet() != $this->_subnet) {
				# Failed - no such session, or session no match.  Need to relogin
				setcookie(COOKIE_REMEMBER, "", -36000, "/");
			}
			else {
				# Update the session updated_at
				$this->touch();
				$this->maintenance();
				$rValue = 1;
			}
		}
		return $rValue;
	}
	
	function load($_gid) {
		$rValue = false;
		global $dbh;
		$_gid = sqlSanitize($_gid, $dbh);
		
		$sql = "SELECT id, userid, gid, subnet, updated_at FROM sessions WHERE gid = " . returnQuotedString($_gid);
		
		$result = mysql_query($sql, $dbh);
		if($result && mysql_num_rows($result) > 0) {
			$rValue = true;
			$myrow = mysql_fetch_assoc($result);
			$this->_id			= $myrow['id'];
			$this->_userid		= $myrow['userid'];
			$this->_gid			= $myrow['gid'];
			$this->_subnet		= $myrow['subnet'];
			$this->updated_at	= $myrow['updated_at'];
		}
		else {
			$GLOBALS['g_ERRSTRS'][1] = mysql_error();
		}
		
		return $rValue;
	}
	
	function touch() {
		global $dbh;
		$_gid = sqlSanitize($this->_gid, $dbh);
		
		$sql = "UPDATE sessions SET updated_at = NOW() WHERE gid = " . returnQuotedString($_gid);
		
		mysql_query($sql, $dbh);
	}

	function destroy() {
		$cookie = (isset($_COOKIE[COOKIE_REMEMBER]) ? $_COOKIE[COOKIE_REMEMBER] : "");
		if($cookie != "" && $this->load($cookie)) {
			global $dbh;
			$sql = "DELETE FROM sessions WHERE userid = " . $this->_userid;
			mysql_query($sql, $dbh);
		}
		setcookie(COOKIE_REMEMBER, "", -36000, "/");
		session_destroy();
	}
	
	function create($_userid, $_remember) {
		global $dbh;
		$this->_userid 	= sqlSanitize($_userid, $dbh);
		$this->_gid 	= $this->guidNbr();
		$this->_subnet 	= $this->getSubnet();
		$this->_updated_at = getCURDATE();

		$sql = "INSERT INTO sessions (
				id,
				userid,
				gid,
				subnet,
				updated_at) VALUES (
				NULL,
				" . $this->_userid . ",
				" . returnQuotedString($this->_gid) . ",
				" . returnQuotedString($this->_subnet) . ",
				NOW())";
		mysql_query($sql, $dbh);
		$cookieTime = 0;
		if($_remember) {
			$cookieTime = time()+3600*24*365;
		}
		setcookie(COOKIE_REMEMBER, $this->_gid, $cookieTime, "/");
		
		$this->maintenance();
	}
	
	function maintenance() {
		# Delete sessions older than 14 days
		# and sessions where the same subnet,user has different gids
		global $dbh;
		$sql = "DELETE FROM sessions 
				WHERE updated_at < DATE_SUB(NOW(), INTERVAL 14 DAY) 
					OR (userid = " . $this->_userid . "
						AND subnet = " . returnQuotedString($this->getSubnet()) . "
						AND gid <> " . returnQuotedString($this->_gid) . ")";
		mysql_query($sql, $dbh);
	}
		
	function getSubnet() {
		# return class-c subnet
		return substr($_SERVER['REMOTE_ADDR'], 0, strrpos($_SERVER['REMOTE_ADDR'], ".")) . ".0";
	}
	
	function guidNbr() {
  		return md5(uniqid(rand(),true));
	}
}
?>