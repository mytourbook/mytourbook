<?php
/*******************************************************************************
 * Copyright (c) 2007,2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eclipse Foundation - initial API and implementation
 *    Antoine Toulmé - Bug 248917
 *    Kit Lo (IBM) - patch, bug 266250, Map file processor not running properly on live server
 *    Kit Lo (IBM) - patch, bug 258749, Keep spaces at the end of value string
*******************************************************************************/
require(dirname(__FILE__) . "/../system/language.class.php"); 
require(dirname(__FILE__) . "/../system/release_train.class.php"); 

require(dirname(__FILE__) . "/../string/string.class.php");

class File {
  public $errStrs;
  
  public $file_id		= 0;
  public $project_id	= '';
  public $version		= '';
  public $name			= '';
  public $is_active 	= 0;
  public $plugin_id = '';

	
	function save() {
		$rValue = false;
		if($this->name != "" && $this->project_id != "" && $this->version != "") {
			global $dbh;

			if($this->file_id == 0) {
				$this->file_id = $this->getFileID($this->name, $this->project_id, $this->version);
			}
			
			$sql 	= "INSERT INTO";
			$where = "";
			if($this->file_id > 0) {
				$sql = "UPDATE";
				$where = " WHERE file_id = " . sqlSanitize($this->file_id, $dbh);
			}
			
			$Event = new EventLog("files", "file_id", $this->file_id, $sql);
			
			$sql .= " files 
						SET file_id 	= " . sqlSanitize($this->file_id, $dbh) . ",
							project_id	= " . returnQuotedString(sqlSanitize($this->project_id, $dbh)) . ", 
							version		= " . returnQuotedString(sqlSanitize($this->version, $dbh)) . ", 
							name		= " . returnQuotedString(sqlSanitize($this->name, $dbh)) . ",
							plugin_id	= " . returnQuotedString(sqlSanitize($this->plugin_id, $dbh)) . ",
							is_active	= " . $this->is_active . $where;
			if(mysql_query($sql, $dbh)) {
				if($this->file_id == 0) {
					$this->file_id = mysql_insert_id($dbh);
					$Event->key_value = $this->file_id;
				}
				$rValue = true;
				$Event->add();
			}
			else {
				echo $sql;
				$GLOBALS['g_ERRSTRS'][1] = mysql_error();
			}
		}
		else {
			echo "ERROR: One missing:Name: " . $this->name . "Project: " . $this->project_id  . "Version: " . $this->version;
		}
		return $rValue;
	}
	
	static function getFileID($_name, $_project_id, $_version) {
		$rValue = 0;
		if($_name != "" && $_project_id != "" && $_version != "") {
			global $dbh;

			$sql = "SELECT file_id
				FROM 
					files 
				WHERE name = " . returnQuotedString(sqlSanitize($_name, $dbh)) . "
					AND project_id = " . returnQuotedString(sqlSanitize($_project_id, $dbh)) . "	
					AND version = '" . sqlSanitize($_version, $dbh) . "'";

			$result = mysql_query($sql, $dbh);
			if($result && mysql_num_rows($result) > 0) {
				$myrow = mysql_fetch_assoc($result);
				$rValue = $myrow['file_id'];
			}
		}
		return $rValue;
	}
	
	function parseProperties($_content) {
		$rValue = "";
		if($_content != "") {
			
			global $User;
			
			# step 1 - import existing strings.  $String->save() will deal with merges
			$previous_line 	= "";
			$lines 			= explode("\n", $_content);
			foreach($lines as $line) {
				if(strlen($line) > 0 && $line[0] != "#" && $line[0] != ";") {
					# Bug 235553 - don't trim the space at the end of a line!
					# $line = trim($line);
					
					# Does line end with a \ ?
					if(preg_match("/\\\\$/", $line)) {
						# Line ends with \
						
						# strip the backslash
						$previous_line .= $line . "\n";
					}
					else {
						if($previous_line != "") {
							$line 			= $previous_line . $line;
							$previous_line 	= "";
						}

						$tags = explode("=", $line, 2);
						if(count($tags) > 1) {
							if($rValue != "") {
								$rValue .= ",";
							}
							$tags[0] = trim($tags[0]);
							# Bug 235553 - don't trim the space at the end of a line!
							# Bug 258749 - use ltrim() to remove spaces at the beginning of value string
							$tags[1] = ltrim($tags[1]);
							
							$rValue .= $tags[0];
							
							$String = new String();
							$String->file_id 	= $this->file_id;
							$String->name 		= $tags[0];
							$String->value 		= $tags[1];
							$String->userid 	= $User->userid;
							$String->created_on = getCURDATE();
							$String->is_active 	= 1;
							$String->save();
						}
					}
				}
			}
			
			# step 2 - remove strings that are no longer in the properties file
			$String = new String();
			$aStrings = $String->getActiveStrings($this->file_id);
			foreach ($aStrings as $String) {
				$found = false;
				
				$aStringList = explode(",",$rValue);
				foreach($aStringList as $strName) {
					if($strName == $String->name) {
						$found = true;
						break;
					}
				}
				
				if(!$found) {
					$String->deactivate($String->string_id);
				}
			}
		}
		return $rValue;
	}
	
	/**
	 * Returns the fragment relative path.
	 */
	function findFragmentRelativePath() {
		# strip useless CVS structure before the plugin name (bug 221675 c14):
		$pattern = '/^([a-zA-Z0-9\/_-])+\/([a-zA-Z0-9_-]+)\.([a-zA-Z0-9_-]+)(.*)\.properties$/i';
		$replace = '${2}.${3}${4}.properties';
		$path = preg_replace($pattern, $replace, $this->name);
		
		# strip source folder (bug 221675) (org.eclipse.plugin/source_folder/org/eclipse/plugin)
		$pattern = '/^([a-zA-Z0-9_-]+)\.([a-zA-Z0-9_-]+)\.([a-zA-Z0-9\._-]+)(.*)\/(\1)([\.\/])(\2)([\.\/])(.*)\.properties$/i';
		$replace = '${1}.${2}.${3}/${5}${6}${7}${8}${9}.properties';
		$path = preg_replace($pattern, $replace, $path);
		
		return $path;
	}
	
	/*
	 * Convert the filename to *_lang.properties, e.g., foo_fr.properties
	*/
	function appendLangCode($language_iso, $filename = null) {
		if (!$filename) {
			$filename = $this->findFragmentRelativePath();
		}
		if (preg_match( "/^(.*)\.properties$/", $filename, $matches)) {
			$filename = $matches[1] . '_' . $language_iso . '.properties';
		}
		return $filename;
	}
	
	/**
	 * returns a hash that contains a mapping from the translation keys to the translation values.
	 */
	function strings4PropertiesFile($language) {
		$result = array();
		if (strcmp($language->iso, "en_AA") == 0) {
			$sql = "SELECT string_id, name, value FROM strings WHERE file_id = " . $this->file_id .
			" AND is_active AND non_translatable = 0";
			$strings_result = mysql_query($sql);
			while (($strings_row = mysql_fetch_assoc($strings_result)) != null) {
				$result[$strings_row['name']] = $this->project_id . $strings_row['string_id'] . ":" . $strings_row['value'];
			}
		} else {
			$sql = "SELECT
				strings.name AS 'key', 
				strings.value AS orig, 
				translations.value AS trans
				FROM strings, translations
				WHERE strings.string_id = translations.string_id
				AND strings.file_id = " . $this->file_id . "
				AND strings.is_active
				AND strings.non_translatable = 0
				AND translations.language_id = " . $language->id . "
				AND translations.is_active";
			$strings_result = mysql_query($sql);
			while (($strings_row = mysql_fetch_assoc($strings_result)) != null) {
				$result[$strings_row['key']] = $strings_row['trans'];
			}
		}
		return $result;
	}
}
?>
