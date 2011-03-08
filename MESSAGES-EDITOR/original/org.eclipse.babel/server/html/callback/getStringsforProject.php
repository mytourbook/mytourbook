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
*******************************************************************************/
error_reporting('E_NONE'); ini_set("display_errors", false);
require_once("cb_global.php");

$project_id = getHTTPParameter("proj", "POST");
$version = getHTTPParameter("version", "POST");

//THE 3 VALID STATES
//	UNSTRANSLATED (DEFUALT)
//	FLAGGED (FLAGGED INCORRECT IN DATABASE)
//	AWAITING (TRANSLATED BUT NO RAITINGS YET)
$state = getHTTPParameter("state", "POST"); 

if(!isset($proj_post)){
	if(isset($_SESSION['project']))
		$project_id = $_SESSION['project'];
		
	if(isset($_SESSION['version']))
		$version =  $_SESSION['version'];
		
	if(isset($_SESSION['language']))
		$language =  $_SESSION['language'];
		
	if(isset($_SESSION['file']))
		$file =  $_SESSION['file'];

	if(isset($_SESSION['string']))
		$string =  $_SESSION['string'];
}

switch($state){
	case "flagged" :
	break;
	$query = "select 
				strings.value as string,
				strings.name as stringName,
				strings.non_translatable,
				translations.value as translation,
				translations.possibly_incorrect as fuzzy
			  from 
			  	strings,
			  	files
			  	left join translations on
			  		translations.language_id = '".addslashes($language)."'
			  	  and
			  		string_id = translations.string_id
			  where 
			  	strings.is_active != 0 
			  and 
			  	strings.file_id = files.file_id 
			  and 
				files.project_id = '".addslashes($project_id)."'
			  and	
				files.version = '".addslashes($version)."'
			";
	case "translated" :
	break;
	$query = "select 
				strings.value as string,
				strings.name as stringName,
				strings.non_translatable,
				translations.value as translation,
				translations.possibly_incorrect as fuzzy,
				users.username as translator
				from 
			  	strings,
			  	users,
			  	files
			  	left join translations on
			  		translations.language_id = '".addslashes($language)."'
			  	  and
			  		string_id = translations.string_id
			  where 
			  	strings.is_active != 0 
			  and 
			  	strings.file_id = files.file_id 
			  and 
				files.project_id = '".addslashes($project_id)."'
			  and	
				files.version = '".addslashes($version)."'
			  and 
			  	users.userid = translations.userid
			";
	
	case "untranslated" :
	default:
		$query = "select 
					strings.string_id as stringId,
					strings.name as stringName,
					strings.non_translatable,
					strings.value as text,
					strings.created_on as createdOn,
					translations.value as translationString,
					translations.possibly_incorrect as fuzzy,
					users.first_name as first,
					users.last_name as last
					from 
					files,
				  	strings
				  	left join translations on (
				  		translations.language_id = '".addslashes($language)."'
				  	  and
				  		translations.string_id  = strings.string_id
				  	  and 
				  	  	translations.is_active = 1
				  	)
				  	
				  	left join users on (
				  		translations.userid = users.userid
				  	)
				  where 
				  	strings.is_active = 1 
				  and 
					files.file_id = strings.file_id
				  and	
					files.version = '".addslashes($version)."'
				  and
				  	files.name = '".addslashes($file)."'
				  and 
					files.project_id = '".addslashes($project_id)."'
  				  group by strings.string_id,translations.version desc
				";
}

//print $query."<br>";

$res = mysql_query($query,$dbh);

//print mysql_error();

$stringids = Array();
$return = Array();
while($line = mysql_fetch_array($res, MYSQL_ASSOC)){
    if(isset($stringids[$line['stringId']])){
 		  continue;
    }else{
    	$line['text'] = nl2br(htmlspecialchars($line['text']));
        if(isset($string) && $line['stringName'] == $string){
			$line['current'] = true;
		}
		else {
			$line['current'] = false;
		} 
    	$line['translationString'] = nl2br(htmlspecialchars($line['translationString']));
    	$line['translator'] = nl2br(htmlspecialchars($line['first']." ".$line['last']));
    	if($line['non_translatable']){
    		$line['translationString'] = "<span style='font-style: italic;'>non-translatable string";
    		$line['nontranslatable'] = true;
    	} 
		$return[] = $line;
		$stringids[$line['stringId']] = 1;
    }
}

$pagesize = 50;

if(count($return) > $pagesize and $_GET['paged'] < count($return) ){
	if(isset($_GET['paged'])){
		$return = array_slice($return,$_GET['paged'],$pagesize);
		$pg->paged = $_GET['paged']+$pagesize;
		$return[] = $pg;
	}else{
		$return = array_slice($return,0,$pagesize);
		$pg->paged = $pagesize;
		$return[] = $pg;
	}
	print json_encode($return);
}else{	
	if($_GET['paged'] < count($return) ){
		print json_encode($return);
	}else{
		print json_encode(array());
	}
}

exit();
?>