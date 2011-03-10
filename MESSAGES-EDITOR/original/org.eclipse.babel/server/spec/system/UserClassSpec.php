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

define('BABEL_BASE_DIR', "../../");

require("../spec_helper.php");
require (BABEL_BASE_DIR . "classes/system/user.class.php");

class DescribeUserClass extends PHPSpec_Context {

	public function before() {
      //delete all users from the test DB then recreate one.
      mysql_query("DELETE FROM users");
      mysql_query('insert into users set userid = 1, username = "babel@eclipse.org", first_name="babel", last_name="fish", email="babel@eclipse.org", primary_language_id = "", password_hash = "HSD9a.ShTTdvo", is_committer = true, updated_on = NOW(), updated_at="",created_on = NOW(), created_at=""');
    }

    public function itShouldBeAbleToFindAUserFromItsEmailAndItsPassword() {
	  $user = new User();
	  $user->load("babel@eclipse.org", "password");
	  $this->spec($user->first_name)->should->equal("babel");
    }

    public function itShouldBeAbleToFindAUserFromItsId() {
	  $user = new User();
	  $user->loadFromID(1);
	  $this->spec($user->first_name)->should->equal("babel");
    }
}

?>