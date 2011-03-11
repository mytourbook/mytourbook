<div id="maincontent">
<div id="rightcolumn">
		<div class="sideitem">
			<h6>Committer checklist</h6>
			<ul>
				<li>Externalize strings to .properties files</li>
				<br />
				<li>Maintain your map files on the Babel server (this page) with every release</li>
				<br />
				<li>Tell your community about Babel, how to help translate, link to the <a href="http://www.eclipse.org/babel/downloads.php">Babel download page</a></li>
				<br />
				<li>Optionally download the Babel fragments for your project and repackage them on your own download site</li>
			</ul>
		</div>
</div>

<div id="midcolumn">

<h1><?= $pageTitle ?></h1>
<p>Use this form to define the map files for your project. The map files are read nightly, and any .properties files (except build.properties) contained in the plugins they reference will be parsed and imported into Babel, allowing the community to translate the externalized strings.</p>  
<p>This page is only accessible by Eclipse committers.</p>
<form name="form1" method="post">
<table cellspacing=4 cellpadding=0 border=0>
<tr><td></td><td colspan=2 style="color:red;"><?= $GLOBALS['g_ERRSTRS'][0] ?></td></tr>
<tr>
  <td>Project:</td><td><select name="project_id" onchange="fnSetVersionList();">
<?php
	while($myrow = mysql_fetch_assoc($rs_project_list)) {
		$selected = "";
		if($myrow['project_id'] == $PROJECT_ID) {
			$selected = 'selected="selected"';
		}
		echo "<option value='" . $myrow['project_id'] . "' $selected>" . $myrow['project_id'] . "</option>";
	}
 ?></select></td>
  <td style='width:100px; color:red;'><?= $GLOBALS['g_ERRSTRS'][1] ?></td>
</tr>
<tr>
  <td>Release Version</td><td><select name="version" onchange="fnUpdateFileList();">
</select></td>
  <td style='width:100px; color:red;'><?= $GLOBALS['g_ERRSTRS'][4] ?></td>
</tr>
<tr>
  <td>Release Train</td><td><select name="train_id">
  <?php
	while($myrow = mysql_fetch_assoc($rs_train_list)) {
		$selected = "";
		if($myrow['train_id'] == $TRAIN_ID) {
			$selected = 'selected="selected"';
		}
		echo "<option value='" . $myrow['train_id'] . "' $selected>" . $myrow['train_id'] . "</option>";
	}
 ?>
</select></td>
  <td></td>
</tr>
<tr>
	<td>&#160;</td><td>Babel builds one update site per Train.  Even if your project does not participate in the actual train, please pick the Train that your project is targetting.</td>
</tr>
<tr>
	<td>&#160;</td><td><b>NOTE: </b>The Release Train applies to all map files for the selected Project Release Version.</td>
</tr>
<tr>
	<td>&#160;</td><td></td>
</tr>

<tr>
  <td><a href="<?php echo imageRoot() ?>/viewcvs/index.cgi">ViewCVS</a> download URL to map file:</td><td><input type="text" name="location" value="<?= $LOCATION ?>" size="80" onchange="fnCheckUrl();" /></td>
  <td style='width:100px; color:red;'><?= $GLOBALS['g_ERRSTRS'][2] ?></td>
</tr>
<tr>
  <td>&#160;</td><td>e.g. <a href="<?php echo imageRoot() ?>/viewcvs/index.cgi/org.eclipse.releng/maps/core.map?view=co">http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.releng/maps/core.map?view=co</a></td>
</tr>
<tr>
  <td>&#160;</td><td><b>NOTE: </b>If you're defining map files for a Release, you must use the download link to the CVS TAG of that release.  This is the pathrev=R3_4 parameter in this example:<br />
  <a href="http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.releng/maps/core.map?view=co&pathrev=R3_4">http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.releng/maps/core.map?view=co&pathrev=R3_4</a>
  </td>
</tr>
<tr>
  <td>File name: </td><td><input type="text" name="filename" value="<?= $FILENAME ?>" size="32" /></td>
  <td style='width:100px; color:red;'><?= $GLOBALS['g_ERRSTRS'][5] ?></td>
</tr>

<tr>
  <td></td><td><input type="submit" name="submit" value="Save" style="font-size:14px;" /></td></tr>
</table>
<iframe id="fileShow" name="somefiles" width="950" height="200"
		  style="border: 1px black solid"
		  src="">
		</iframe>
</form>
<script language="javascript">
	function fnSetVersionList() {
		document.form1.version.options.length = 0;
		
		if(typeof(versions[document.form1.project_id.value]) != "undefined") {
			for(i = 0; i < versions[document.form1.project_id.value].length; i++) {
				var opt = document.createElement("OPTION");
				document.form1.version.options.add(opt);
				document.form1.version.options[i].text 		= versions[document.form1.project_id.value][i];
				document.form1.version.options[i].value 	= versions[document.form1.project_id.value][i];
				if(versions[document.form1.project_id.value][i] == "<?= $VERSION ?>") {
					document.form1.version.options[i].selected = "selected";
				}
			}
		}
		else {
			var opt = document.createElement("OPTION");
			document.form1.version.options.add(opt);
			document.form1.version.options[0].text 		= "unspecified";
			document.form1.version.options[0].value 	= "unspecified";
		}
		
		fnUpdateFileList();
	}
	
	function fnUpdateFileList() {
		source = "map_files.php?submit=showfiles&project_id=" + document.form1.project_id.value + "&version=" + document.form1.version.options[document.form1.version.selectedIndex].value;
		document.getElementById("fileShow").src = source;
		fnSetTrain();
	}
	
	function fnSetTrain() {
		<?# Update train according to selected project/version  ?>
		if(typeof(project_trains[document.form1.project_id.value][document.form1.version.options[document.form1.version.selectedIndex].value]) != "undefined") {
			for(i = 0; i < document.form1.train_id.length; i++) {
				document.form1.train_id.options[i].selected = "";
				if(document.form1.train_id.options[i].value == project_trains[document.form1.project_id.value][document.form1.version.options[document.form1.version.selectedIndex].value]) {
					document.form1.train_id.options[i].selected = "selected";
				}
			}
		}
	}
	
<?php
global $addon;
echo $addon->callHook('validate_map_file_url');
?>

	var versions = new Array();
	
<?php
	$prev_project = "";
	$count = 0;
	while($myrow = mysql_fetch_assoc($rs_version_list)) {
		if($prev_project != $myrow['project_id']) {
			if($count > 0) {
				echo "];
";
			}
			echo "versions['" . $myrow['project_id'] . "'] = [";
			$count = 0;
		}
		if($count > 0) {
			echo ",";
		}
		echo "\"" . $myrow['version'] . "\"";
		$count++;
		$prev_project = $myrow['project_id'];
	}
	echo "];";
 ?>
 

	var project_trains = new Array();
	
<?php
	$prev_project = "";
	$count = 0;
	while($myrow = mysql_fetch_assoc($rs_train_project_list)) {
		if($prev_project != $myrow['project_id']) {
			if($count > 0) {
				echo "};
";
			}
			echo "project_trains['" . $myrow['project_id'] . "'] = {";
			$count = 0;
		}
		if($count > 0) {
			echo ",";
		}
		echo "'" . $myrow['version'] . "' : '" . $myrow['train_id'] . "'";
		$count++;
		$prev_project = $myrow['project_id'];
	}
	echo "};";
 ?>
	fnSetVersionList();
  
	document.form1.submit.disabled = "disabled";
 
 </script>