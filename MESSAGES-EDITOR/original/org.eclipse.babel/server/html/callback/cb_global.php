<?php
/*******************************************************************************
 * Copyright (c) 2007 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Paul Colton (Aptana)- initial API and implementation
 *    Eclipse Foundation
*******************************************************************************/

require_once(dirname(__FILE__) . "/../global.php");

InitPage("login");


if( !function_exists('json_encode') ){
	require("/home/data/httpd/babel.eclipse.org/html/json_encode.php");
 
	function json_encode($encode){
 		$jsons = new Services_JSON();
		return $jsons->encode($encode);
	}
}
