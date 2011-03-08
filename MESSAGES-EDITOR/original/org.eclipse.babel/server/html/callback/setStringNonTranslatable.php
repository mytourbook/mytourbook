<?php
require_once("cb_global.php");

$string_id = getHTTPParameter("string_id", "POST");
$checked_state = getHTTPParameter("check", "POST");

$query = "select value from strings where string_id = '".addslashes($string_id)."'";
$res = mysql_query($query,$dbh);
$row = mysql_fetch_assoc($res);

if($checked_state == "true"){
	$message = "String '".$row['value']."' has been marked as non-translatable.";
	$checked_state = 1;
}else{
	$message = "String '".$row['value']."' has been marked as translatable.";
	$checked_state = 0;
}

$query = "update 
			strings
		  set
			non_translatable = '".addslashes($checked_state)."' 
		  where 		  	
			string_id = '".addslashes($string_id)."'
		  ";

$res = mysql_query($query,$dbh);


print "<br><br><br><center><b>$message</b></center>";
?>