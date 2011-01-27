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

require (dirname(__FILE__) . "/../html_functions.php");

// Use a class to define the hooks to avoid bugs with already defined functions.
class BabelEclipseOrgStaging extends BabelEclipseOrg {

    function head() {
        BabelEclipseOrg::head();
        echo "<h1 style='color: red'>This is the staging area. Don't do any serious translation work here!</h1><br/>";
    }
}

function __register_html_staging($addon) {
    __register_html($addon);
    $addon->register('head', array('BabelEclipseOrgStaging', 'head'));
}

global $register_function_html;
$register_function_html = '__register_html_staging';
?>
