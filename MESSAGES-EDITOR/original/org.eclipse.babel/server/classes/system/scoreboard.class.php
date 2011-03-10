<?php
/*******************************************************************************
 * Copyright (c) 2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Foundation - initial API and implementation
 *    
*******************************************************************************/

class Scoreboard {

	public function refresh() {
		global $dbh;
		$sql = "SELECT MAX(translation_id) as t, quantity FROM translations, scoreboard WHERE itemid = 'LASGEN' GROUP BY quantity HAVING t > quantity";
	
		$t = 0;
		
		$result = mysql_query($sql, $dbh);
		if($result && mysql_num_rows($result) > 0) {
			$myrow = mysql_fetch_assoc($result);
			
			$t = $myrow['t'];

			# "lock" the scoreboard so that 2 clients don't update it simultaneously
			mysql_query("UPDATE scoreboard SET quantity = 9999999999 WHERE itemid = 'LASGEN'", $dbh);

			# rebuilding the scoreboard takes time ... dump stuff to tmp
			mysql_query("CREATE TEMPORARY TABLE _tmp_scoreboard LIKE scoreboard", $dbh);
			$sql = "INSERT INTO _tmp_scoreboard SELECT NULL, 'LANGPR', IF(ISNULL(b.locale),b.name,CONCAT(b.name, CONCAT(' (', CONCAT(b.locale, ')')))), count(*) as StringCount from translations as a inner join languages as b on b.language_id = a.language_id where a.value <> '' and a.is_active = 1 group by a.language_id order by StringCount desc";
			mysql_query($sql, $dbh);
			$sql = "INSERT INTO _tmp_scoreboard SELECT NULL, 'TOPTR', CONCAT(first_name, IF(ISNULL(last_name),'',CONCAT(' ', last_name))), count(translations.string_id) as cnt from translations inner join users on users.userid = translations.userid where is_active=1 group by first_name, last_name order by cnt desc limit 20";
			mysql_query($sql, $dbh);

			$sql = "INSERT INTO _tmp_scoreboard SELECT NULL, 'LASGEN', 'Scoreboard Last Generated', MAX(translation_id) FROM translations";
			mysql_query($sql, $dbh);

			$sql = "INSERT INTO _tmp_scoreboard SELECT NULL, 'LGNOW', 'Scoreboard Last Generated Date/Time', NOW()";
			mysql_query($sql, $dbh);
			
			mysql_query("LOCK TABLES scoreboard WRITE", $dbh);
			mysql_query("DELETE FROM scoreboard", $dbh);
			mysql_query("INSERT INTO scoreboard SELECT * FROM _tmp_scoreboard", $dbh);
			mysql_query("UNLOCK TABLES", $dbh);
			mysql_query("DROP TABLE _tmp_scoreboard", $dbh);
		}
	}
}
?>
