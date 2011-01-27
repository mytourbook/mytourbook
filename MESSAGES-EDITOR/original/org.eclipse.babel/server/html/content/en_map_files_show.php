<link href='babel.css' rel="stylesheet">
<h1>Map files for project: <?= $PROJECT_ID ?></h1>
<table cellspacing=1 cellpadding=3 border=1 width="100%">
<tr>
  <td>Project</td><td align="right">Version</td><td>Train</td><td>URL</td><td>File name</td><td>Delete</td></tr>
<?php
	while($myrow = mysql_fetch_assoc($rs_map_file_list)) {
		$train_id = $myrow['train_id'];
		if ($train_id == null)
			$train_id = "&nbsp";
		echo "<tr><td>"	. $myrow['project_id'] . "</td>
		<td align='right'>" . $myrow['version'] . "</td>
		<td>" . $train_id . "</td>
		<td><a href='" . $myrow['location'] . "' target='new'>" . $myrow['location'] . "</a></td>
		<td>" . $myrow['filename'] . "</td>
		<td><a onclick=\"javascript:return fnConfirm();\" href='map_files.php?submit=delete&project_id=" . $PROJECT_ID . "&version=" . $VERSION . "&filename=" . $myrow['filename'] . "'><img border=0 src='";
		echo imageRoot();
		echo "/small_icons/actions/process-stop.png'></a></td></tr>";
	}
?>
</table>
<script language="javascript">
	function fnConfirm() {
		return confirm('Sure?');
	}
</script>