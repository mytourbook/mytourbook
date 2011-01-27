<h1><?= $pageTitle ?></h1>
<form method="post">
<table>
<tr><td>Show only this project</td>
 <td><select name="project_version">
 <option value="">All projects</option>
<?php
	while($myrow = mysql_fetch_assoc($rs_p_list)) {
		$selected = "";
		if($myrow['project_id'] . "|" . $myrow['version'] == $PROJECT_ID . "|" . $VERSION) {
			$selected = 'selected="selected"';
		}
		echo "<option value='" . $myrow['project_id'] . "|" . $myrow['version']  . "' $selected>" . $myrow['project_id'] . " " . $myrow['version'] . "</option>";
	}
 ?></select></td>
 <td>Show only this language</td>
   <td><select name="language_id">
   <option value="All">All languages</option>
<?php
	while($myrow = mysql_fetch_assoc($rs_l_list)) {
		$selected = "";
		if($myrow['language_id'] == $LANGUAGE_ID) {
			$selected = 'selected="selected"';
		}
		echo "<option value='" . $myrow['language_id'] . "' $selected>" . $myrow['name'] . "</option>";
	}
 ?></select></td>
 <td>Show last <select name="limit">
   <option value="25">25</option>
   <option value="50">50</option>
   <option value="100">100</option>
   <option value="200">200</option>
   <option value="500">500</option>
   <option value="1000">1000</option>
   <option value="2000">2000</option>
   <option value="5000">5000</option>
   <option value="10000">10000</option>
   <option value="20000">20000</option>
</select> translations</td></tr>
<tr><td>Layout <select name="layout">
   <option value="list" <?= $LAYOUT == "list" ? "selected='selected'" : "" ?>>list</option>
   <option value="table" <?= $LAYOUT == "table" ? "selected='selected'" : "" ?>>table</option>
</select></td>
<td><input type="checkbox" name="fuzzy" <?= $FUZZY ? "checked" : "" ?> value="1" /> Show Possibly Incorrect only</td>
 </tr>
 <tr><td colspan="4"><input type="submit" value="Apply filter" /></td></tr></table></form>