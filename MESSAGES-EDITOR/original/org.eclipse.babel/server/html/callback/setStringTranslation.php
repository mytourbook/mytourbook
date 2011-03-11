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
 * 	  Eclipse Contributors (bug 217257)
*******************************************************************************/

require_once("cb_global.php");


$string_id = getHTTPParameter("string_id", "POST");
$translation = getHTTPParameter("translation", "POST");
$fuzzy_state = getHTTPParameter("fuzzy", "POST");

$language_id = $_SESSION["language"];
$project_id = $_SESSION['project'];
$version = $_SESSION["version"];

$user_id =	$User->userid;

# TODO: refactor these ifs
$do_nothing = false;

$affected_rows = 0;

if (empty($translation) || (trim($translation) == '')) {

	$do_nothing = true;
	
} else if($_POST['translate_action'] != "all"){
	$query = "update 
				translations 
			  set
				is_active = 0 
			  where 		  	
				string_id = '".addslashes($string_id)."'
			  and
			  	language_id = '".addslashes($language_id)."'
			  and 
			  	is_active = 1
			  ";
	$res = mysql_query($query,$dbh);
	
	$query = "insert into 
					translations
				  set
				  	string_id = '".addslashes($string_id)."',
				  	language_id = '".addslashes($language_id)."',
				  	value = '".addslashes($translation)."',
				  	userid = '".addslashes($user_id)."',
				  	possibly_incorrect = '".addslashes($fuzzy_state)."',
				  	created_on = NOW()
				  	";
	$res = mysql_query($query,$dbh);
	$affected_rows += mysql_affected_rows();
	
//	print $query;
}else{
	//FIND ALL STRINGS THAT ARE THE SAME ACROSS VERSIONS
	$query = "select 
				s.string_id
			  from 
			  	strings as s
			  	INNER JOIN files AS f on s.file_id = f.file_id 
			  	INNER JOIN files AS the_file_selected_for_translation on the_file_selected_for_translation.file_id = (select file_id from strings where string_id = '".addslashes($string_id)."') 
			  where 
			  	s.value = (select value from strings where string_id = '".addslashes($string_id)."')
			  and
				s.name = (select name from strings where string_id = '".addslashes($string_id)."')
			  and f.name =  the_file_selected_for_translation.name
			  and f.project_id =  the_file_selected_for_translation.project_id
			  and s.is_active = 1";
		  	
	$res = mysql_query($query,$dbh);
	while($row = mysql_fetch_assoc($res)){
		$string_ids[] = $row['string_id'];
	}
	
	//GET CURRENT TRANSLATION FOR THIS STRING
	$query= "select value from translations where string_id = '".addslashes($string_id)."' and language_id = '".addslashes($language_id)."' and is_active = 1 order by version limit 1";
	$res = mysql_query($query,$dbh);
	$string_translation = "";
	while($row = mysql_fetch_assoc($res)){
		$string_translation = $row['value'];
	}
	
	//GET ALL STRINGS WITH SAME TRANSLATIONS
	if($string_translation){
		$query	= "
			select 
				translation_id,string_id,language_id
			from
				translations
			where
				string_id in (".addslashes(implode(',',$string_ids)).")
			and
				value = '".addslashes($string_translation)."'
			and
			  	is_active = 1
			and language_id = '" . addslashes($language_id)."'
		  ";
		
		$res = mysql_query($query,$dbh);
		while($row = mysql_fetch_assoc($res)){
			//DE-ACTIVATE ALL OLD TRANSLATIONS
			$query = "update translations set is_active = 0 where translation_id = '".addslashes($row['translation_id'])."'";	
			$res2 = mysql_query($query,$dbh);
			
			//INSERT NEW TRANSLATIONS
			$query = "insert into 
					 	translations
					 set
	 					string_id = '".addslashes($row['string_id'])."', 
						language_id = '".addslashes($row['language_id'])."' , 
						value = '".addslashes($translation)."', 
  						userid = '".addslashes($user_id)."',
  						possibly_incorrect = '".addslashes($fuzzy_state)."',
				   		created_on  = NOW()
					";
			$res2 = mysql_query($query,$dbh);
			$affected_rows += mysql_affected_rows();
			
		}
		
	}else{
		$query	= "
			select 
				strings.string_id
			from
				strings
				left join 
					translations
				on
					strings.string_id = translations.string_id
			and
				translations.value is NULL
			where
				strings.string_id in (".addslashes(implode(',',$string_ids)).")
		";
		
		$res = mysql_query($query,$dbh);
		
		while($row = mysql_fetch_assoc($res)){
			$translation_ids[] = $row['string_id'];
			//INSERT NEW TRANSLATIONS
			$query = "insert into 
					 	translations
					 set
	 					string_id = '".addslashes($row['string_id'])."', 
						language_id = '".addslashes($language)."' , 
						value = '".addslashes($translation)."', 
						possibly_incorrect = '".addslashes($fuzzy_state)."',
  						userid = '".addslashes($user_id)."',
				   		created_on  = NOW()
					";
			$res2 = mysql_query($query,$dbh);
			$affected_rows += mysql_affected_rows();
		}	
	}	
}

if(!$do_nothing) {
	# Find all string_id's that have the same binary value as the one we're translating
	# *and* have no translation yet, and update those too.
	if(!$fuzzy_state) {
		$sql = "SELECT s.string_id, COUNT(t.string_id) AS tr_count
		FROM strings AS s 
		LEFT JOIN translations AS t ON t.string_id = s.string_id AND t.language_id = '".addslashes($language_id)."'
		WHERE BINARY s.value = (select value from strings where string_id = '".addslashes($string_id)."')  
			AND s.is_active = 1 AND t.value IS NULL GROUP BY s.string_id HAVING tr_count = 0";

		$res 		= mysql_query($sql, $dbh);
		$str_count 	= mysql_affected_rows();
	
		while($myrow = mysql_fetch_assoc($res)) {
			$sql = "insert into 
						translations
					  set
				  		string_id = " . $myrow['string_id'] . ",
					  	language_id = '".addslashes($language_id)."',
					  	value = '".addslashes($translation)."',
					  	userid = '".addslashes($user_id)."',
					  	created_on = NOW()";
			mysql_query($sql, $dbh);
			$affected_rows += mysql_affected_rows();
		}
	}
}

$response['translationString'] = htmlspecialchars($translation);

$response['translationArea'] = "<br /><br /><br />
<center><b>Translated $affected_rows string". ($affected_rows > 1 || $affected_rows == 0 ? "s" : "" ) ." across all Babel projects.
</b></center>";

print json_encode($response);

?>