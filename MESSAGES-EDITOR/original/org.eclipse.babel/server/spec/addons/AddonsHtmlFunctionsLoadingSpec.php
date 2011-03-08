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
require(dirname(__FILE__) . "/../../classes/system/addons_management.php");

global $addon;
$addon = new AddonsManagement('reference');
$addon->load_html_functions();

class DescribeAddonsHtmlFunctionsLoading extends PHPSpec_Context {

    public function before() {
        
    }
    
    public function itShouldHaveAddedAHookForImageRoot() {
        global $addon;
        $this->spec($addon->hook("image_root"))->shouldNot->beNull();
        $this->spec(call_user_func($addon->hook("image_root")))->should->equal("http://dev.eclipse.org");
    }
    
    public function itShouldProvideAWayToValidateTheUrlOfAMapFile() {
        global $addon;
        $this->spec($addon->hook("validate_map_file_url"))->shouldNot->beNull();
        $this->spec(call_user_func($addon->hook("validate_map_file_url")))->should->beTrue();
    }
    
    
}

?>
