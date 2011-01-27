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
 *    Kit Lo (IBM) - patch, bug 261739, Inconsistent use of language names
*******************************************************************************/

require_once("cb_global.php");



$query = "SELECT language_id, IF(locale <> '', CONCAT(CONCAT(CONCAT(name, ' ('), locale), ')'), name) as name FROM languages WHERE is_active AND iso_code != 'en' ORDER BY name";
$res = mysql_query($query,$dbh);


$return = Array();

while($line = mysql_fetch_array($res, MYSQL_ASSOC)){
    if(isset($_SESSION['language']) and $line['language_id'] == $_SESSION['language']){
    	$line['current'] = true;
    }
	$return[] = $line;
}

print json_encode($return);
exit();

?>