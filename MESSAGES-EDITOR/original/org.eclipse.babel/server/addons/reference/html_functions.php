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

// Use a class to define the hooks to avoid bugs with already defined functions.
class Reference {
    /**
     * Returns the root path to the images.
     * May be a distant server or a local folder.
     */
    function _imageRoot() {
        return "http://dev.eclipse.org";
    }
    
    /**
     * Returns a string representing a JS function named fnCheckUrl that
     * is called to check on the integrity of the url of the map file.
     */
    function validateMapFileUrl($url) {
        return "function fnCheckUrl() {}";
    }

    /**
     * Outputs the head of the html page.
     */
    function head() {
    }
    
    /**
     * Outputs the footer of the html page.
     */
    function footer() {
    }
}

function __register_html_ref($addon) {
    $addon->register('image_root', array('Reference', '_imageRoot'));
    $addon->register('validate_map_file_url', array('Reference', 'validateMapFileUrl'));
    $addon->register('head', array('Reference', 'head'));
    $addon->register('footer', array('Reference', 'footer'));
}

global $register_function_html;
$register_function_html = '__register_html_ref';

?>
