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

class AddonsManagement {

    private $addon;
    private $hooks = array();
  
    /**
     * Constructor. Registers the addon name to use.
     * The addon name should be the name of the folder in use.
     * You can directly pass the addon name, or have it be consumed
     * from a properties file as a separate location, under the key "addon".
     */
    function AddonsManagement($addon = null) {
        if (!isSet($addon)) {
            if (!isSet($ini_file_path)) {
                $ini_file_path = dirname(__FILE__) . '/../../addons/addons.conf';
            }
            
            if (($ini = @parse_ini_file($ini_file_path)) && isSet($ini['addons'])) {
                $addon = $ini['addons'];
            }
            if (!isSet($addon)) {
                $addon = $_ENV['BABEL_ADDONS'];
            }
            if (!isSet($addon)) {
                exitTo();
            }
        }
        $this->addon = $addon;
    }
    
    /**
     * Loads the addon, register the hooks for html functions.
     */
    public function load_html_functions() {
        global $register_function_html;
        if (!function_exists($register_function_html)) {
            require(dirname(__FILE__) . "/../../addons/" . $this->addon . "/html_functions.php");
            call_user_func($register_function_html, $this);
        }
    }
    
    /**
     * Loads the addon, register the hooks for backend functions.
     */
    public function load_backend_functions() {
        global $register_function_backend;
        if (!function_exists($register_function_backend)) {
            require(dirname(__FILE__) . "/../../addons/" . $this->addon . "/backend_functions.php");
            call_user_func($register_function_backend, $this);
        }
    }
    
    /**
     * Registers a function for a specific key.
     * The function will be called later on.
     */
    public function register($hook_key, $function_name) {
        $this->hooks[$hook_key] = $function_name;
    }
    
    /**
     * Returns the name of the function to be used in the hook.
     */ 
    public function hook($hook_key) {
        return $this->hooks[$hook_key];
    }
    
    /**
     * Executes the function associated with the hook and returns the result
     */ 
    public function callHook($hook_key, $args = null) {
        return call_user_func_array($this->hook($hook_key), $args);
    }
}

/*
 * The default addon instance, to use in the product.
 */
global $addon;
$addon = new AddonsManagement();

?>
