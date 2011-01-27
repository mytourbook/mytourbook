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
 *    Scott Reynen scott at randomchaos com - toescapedunicode
*******************************************************************************/

require_once(dirname(__FILE__) . "/common_functions.php");

// load the backend addon functions.
require_once(dirname(__FILE__) . "/addons_management.php");
global $addon;
$addon->load_backend_functions();

require_once(dirname(__FILE__) . "/user.class.php");
// some methods reused accross the backend.

/**
 * Converts string to escaped unicode format
 * Based on work by Scott Reynen - CQ 2498
 *
 * @param string $str
 * @return string
 * @since 2008-07-18
 */
if(!function_exists('toescapedunicode')) {
function toescapedunicode($str) {
    $unicode = array();       
    $values = array();
    $lookingFor = 1;
       
    for ($i = 0; $i < strlen( $str ); $i++ ) {
        $thisValue = ord( $str[ $i ] );
        if ( $thisValue < 128)
            $unicode[] = $str[ $i ];
        else {
            if ( count( $values ) == 0 ) $lookingFor = ( $thisValue < 224 ) ? 2 : 3;               
                $values[] = $thisValue;               
            if ( count( $values ) == $lookingFor ) {
                $number = ( $lookingFor == 3 ) ?
                    ( ( $values[0] % 16 ) * 4096 ) + ( ( $values[1] % 64 ) * 64 ) + ( $values[2] % 64 ):
                    ( ( $values[0] % 32 ) * 64 ) + ( $values[1] % 64 );
                $number = dechex($number);
                
                if(strlen($number) == 3) {
                    $unicode[] = "\u0" . $number;
                }
                elseif(strlen($number) == 2) {
                    $unicode[] = "\u00" . $number;
                }
                else {
                    $unicode[] = "\u" . $number;
                }
                $values = array();
                $lookingFor = 1;
            }
        }
    }
    return implode("",$unicode);
}
}

/**
* Returns the genie user to be used for headless applications.
* The user is found by using a hook.
*/
function getGenieUser() {
  global $addon;
  return $addon->callHook('genie_user');
}
/**
* Returns the syncup user to be used for headless applications.
* The user is found by looking for syncup_id in the base.conf file.
*/
function getSyncupUser() {
  global $addon;
  return $addon->callHook('syncup_user');
}

/*
 * jars the content of a directory $dir, places it in a file named $output
 */
function internalJar($dir, $output) {
    $cmd = "cd $dir; jar cfM $output .";
    $retval = system($cmd, $return_code);
    if ($return_code != 0) {
        echo "### ERROR during the execution of: $cmd\n";
        echo "$retval\n";
    }
}

function errorLog() {
	global $addon;
	$args = func_get_args();
	$addon->callHook('error_log', $args);
}

?>