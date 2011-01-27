<div id="maincontent">
<div id="midcolumn">

<style>
#container{
	width: 100%;
	cursor: auto;
	margin-left:5px;
}

.head {
	background-color: SteelBlue;
  	color: white;
	margin: 0px;	
	font-size: 14px;
	padding: 2px;
}
li {
	padding-bottom: 5px;
}
img {
	vertical-align: bottom;
}
</style>

<?include 'en_recent_html_common.php' ?>
<ul>
<?php
	$prev_date = "";
	$rowcount=0;
	while($myrow = mysql_fetch_assoc($rs_p_stat)) {
		$rowcount++;
		if($prev_date != substr($myrow['created_on'],0,10)) {
			$prev_date = substr($myrow['created_on'],0,10);
			echo "<h2>$prev_date</h2>";
		}
		$fuzzy = "";
		if($myrow['fuzzy'] == 1) {
			$fuzzy = "<img src='images/fuzzy.png' /> ";
		}
		
		echo "<li>" . 
			substr($myrow['created_on'],11,5) . " " . $myrow['string_value'] . 
				" -> " . $myrow['translation'] .
				$fuzzy .  
				" [" . $myrow['language'] . ": <a href='translate.php?project=" . $myrow['project_id'] . "&version=" . $myrow['version'] . "&file=" . $myrow['name'] . "&string=" . $myrow['string_key'] . "'>" .$myrow['string_key'] . "</a>] <b>" . 
				$myrow['project_id'] . " " . 
				$myrow['version'] . "</b> 
				(<a href='?userid=" . $myrow['userid'] . "'>" . $myrow['who'] . "</a>)"; 
		echo "</li>";
		
		// $myrow['string_key'] . " " . 
	}
 ?>
 </ul>
 <?= $rowcount ?> row<?= $rowcount > 1 || $rowcount == 0 ? "s" : "" ?> found</td>