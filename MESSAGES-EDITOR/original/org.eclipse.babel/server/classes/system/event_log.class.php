<?php
/*******************************************************************************
 * Copyright (c) 2007 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eclipse Foundation - initial API and implementation
*******************************************************************************/

class EventLog {

  public $event_id 		= 0;
  public $table_name	= '';
  public $key_name		= '';
  public $key_value		= '';
  public $action		= '';
  public $userid		= 0;
  public $created_on	= '';

  	/**
  	 * Default constructor
  	 *
  	 * @param String $_table_name
  	 * @param String $_key_name
  	 * @param String $_key_value
  	 * @param String $_action
  	 * @return EventLog
  	 */
	function EventLog ($_table_name, $_key_name, $_key_value, $_action) {
		$this->table_name 	= $_table_name;
		$this->key_name 	= $_key_name;
		$this->key_value 	= $_key_value;
		$this->action 		= $_action;
	}

	/**
	 * add event log entry to the table
	 *
	 * @return String Error message (if any)
	 */
	function add() {
		$rValue = "";
		global $User, $dbh;
		
		# remove anything after a space
		$has_space = strpos($this->action, ' ');
		if($has_space !== FALSE && $has_space > 0) {
			$this->action = substr($this->action, 0, $has_space);
		}

		if($this->table_name != "" && $this->key_name != "" && $this->key_value != "" && $this->action != "") {
			$sql = "INSERT INTO event_log SET
					event_id = NULL,
					table_name = " 	. returnQuotedString(sqlSanitize($this->table_name, $dbh)) . ",
					key_name = " 	. returnQuotedString(sqlSanitize($this->key_name, $dbh)) . ",
					key_value = " 	. returnQuotedString(sqlSanitize($this->key_value, $dbh)) . ",
					action = " 		. returnQuotedString(sqlSanitize($this->action, $dbh)) . ",
					userid = " 		. sqlSanitize($User->userid, $dbh) . ",
					created_on = NOW()";

			mysql_query($sql, $dbh);
			if(mysql_error() != "") {
				echo "An unknown database error has occurred while logging information.  Please contact the System Administrator.";
				echo mysql_error();
				$rValue = "MYSQL: " . mysql_error();
			}
		}
		else {
			$rValue = "CRIT: Missing critical information for logging";
		}
		
		return $rValue;
	}
}
?>