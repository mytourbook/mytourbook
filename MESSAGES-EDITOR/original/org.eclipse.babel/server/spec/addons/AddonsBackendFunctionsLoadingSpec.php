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

require("../spec_helper.php");
require_once(dirname(__FILE__) . "/../../classes/system/addons_management.php");

global $addon;
$addon = new AddonsManagement('reference');
$addon->load_backend_functions();
// load user after we have explicitly loaded the reference.
require_once(dirname(__FILE__) . "/../../classes/system/user.class.php");

class DescribeAddonsBackendFunctionsLoading extends PHPSpec_Context {
    
    public function before() {
        
    }
    
    public function itShouldHaveAddedAHookForUserAuthentication() {
        global $addon;
        $this->spec($addon->hook("user_authentication"))->shouldNot->beNull();
        $user = new User();
        $user->load("babel@babel.eclipse.org", "somepassword");
        $this->spec($user->userid > 0)->should->beTrue();
    }
    
}

?>
