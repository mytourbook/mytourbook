<?php
/*******************************************************************************
 * Copyright (c) 2007-2009 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc.
 *******************************************************************************/
 
require_once(dirname(__FILE__) . "/../classes/system/common_functions.php");

require_once(dirname(__FILE__) . "/../classes/system/addons_management.php");

$addon->load_html_functions();

function imageRoot() {
    global $addon;
    return $addon->callHook('image_root');
}

?>