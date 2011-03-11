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
class BabelEclipseOrg {
    /**
     * Returns the root path to the images.
     * May be a distant server or a local folder.
     */
    function _imageRoot() {
        return "http://dev.eclipse.org";
    }
    
    function validateMapFileUrl($url) {
        return <<<JS
        function fnCheckUrl() {
            if(!document.form1.location.value.match(/view=co/)) {
               alert("The ViewCVS URL must contain view=co");
                document.form1.submit.disabled = "disabled";
            }
            else {
                document.form1.submit.disabled = "";

                var re = /\/([A-Za-z0-9_-]+\.map)/;
                var match = re.exec(document.form1.location.value)
                document.form1.filename.value = match[1];
            }
        }
JS;
    }
    
    /**
     * Outputs the head of the html page.
     */
    function head() {
        include(dirname(__FILE__) . "/html/head.php");
    }
    
    /**
     * Outputs the footer of the html page.
     */
    function footer() {
        include(dirname(__FILE__) . "/html/foot.php");
    }

}

function __register_html($addon) {
    $addon->register('image_root', array('BabelEclipseOrg', '_imageRoot'));
    $addon->register('validate_map_file_url', array('BabelEclipseOrg', 'validateMapFileUrl'));
    $addon->register('head', array('BabelEclipseOrg', 'head'));
    $addon->register('footer', array('BabelEclipseOrg', 'footer'));
}

global $register_function_html;
$register_function_html = '__register_html';
?>
