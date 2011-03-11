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

.odd {
	background-color: LightSteelBlue;
}

.foot {
	background-color: LightGray;
}
</style>

<?include 'en_recent_html_common.php' ?>
<table cellspacing=1 cellpadding=2 border=0>
<tr class="head">
<?php
    $i = 0;
	while($i < mysql_num_fields($rs_p_stat)) {
		 $meta = mysql_fetch_field($rs_p_stat, $i);
		 $align = "";
		 if($meta->numeric) {
		 	$align="align='right'";
		 }
		 echo "<td $align><b>" . $meta->name . "</b></td>";
		 $i++;
	}
 ?></tr>
<?php
	$rowcount=0;
	while($myrow = mysql_fetch_assoc($rs_p_stat)) {
		$rowcount++;
		$class="";
		if($rowcount % 2) {
			$class="class='odd'";
		}
		echo "<tr $class>";
		$i = 0;
		while($i < mysql_num_fields($rs_p_stat)) {
			$meta = mysql_fetch_field($rs_p_stat, $i);
			$align = "";
		 	if($meta->numeric) {
		 		$align="align='right'";
			 }
			
			echo "<td $align>" . $myrow[$meta->name] . "</td>";
			$i++;
		}
		echo "</tr>";
	}
 ?>
 <tr class="foot">
 	<td colspan="<?= $i ?>"> <?= $rowcount ?> row<?= $rowcount > 1 || $rowcount == 0 ? "s" : "" ?> found</td>
 </tr>
 </table>
 