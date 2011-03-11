<?php
/*******************************************************************************
 * Copyright (c) 2009 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
  *    Eclipse Foundation - initial API and implementation
*******************************************************************************/

require_once("frag_global.php");

$query = "SELECT value FROM sys_values WHERE itemid = 'MOTD' AND value IS NOT NULL AND value <> '' LIMIT 1";

if ($res = mysql_query($query)) {
	if ($row = mysql_fetch_assoc($res)) {
		echo "<div id='motd'>";
		echo $row['value'];
		echo "</div>";
	}
}
?>