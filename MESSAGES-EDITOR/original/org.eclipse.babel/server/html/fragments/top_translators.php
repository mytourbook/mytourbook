<?php
/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
  *    Eclipse Foundation - initial API and implementation
*******************************************************************************/


require_once("frag_global.php");

$query = "SELECT value, quantity FROM scoreboard WHERE itemid = 'TOPTR' ORDER BY quantity DESC";

$res = mysql_query($query);

?>
<div id="top-translators-area">
	<h2>Top Translators</h2>
	<dl>
	<?
		while($row = mysql_fetch_assoc($res)){
			?><dt><?=$row['value'] ?></dt>
			<dd><?=$row['quantity'];?></dd><?
		}
	?>
	</dl>
	<br />
	<a href="stats.php">More stats...</a>
</div>

<br style='clear: both;'>
