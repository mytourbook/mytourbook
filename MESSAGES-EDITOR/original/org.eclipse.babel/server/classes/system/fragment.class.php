<?php
/*******************************************************************************
 * Copyright (c) 2007-2008 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc. bug 248845: Refactoring generate1.php into different files with a functional approach
*******************************************************************************/

require_once(dirname(__FILE__) . "/project.class.php"); 
require_once(dirname(__FILE__) . "/../file/file.class.php"); 

require_once(dirname(__FILE__) . "/backend_functions.php");

// constants
define("LEGAL_FILES_DIR", dirname(__FILE__) . "/../export/source_files_for_generate/");

class Fragment {
	public $files;
	public $plugin_id;
	public $language;
	public $train;
	public $filesize;
	
	/*
	 * Default constructor
	 */
	function Fragment($plugin_id, $files, $language, $train) {
		$this->plugin_id = $plugin_id;
		$this->files = $files;
		$this->language = $language;
		$this->train = $train;
	}
	
	
	static function select($language, $train) {
		/*
		 * Determine which plug-ins need to be in this language pack.
		 */
		if (strcmp($language->iso, "en_AA") == 0) {
			$sql = "SELECT DISTINCT f.project_id, f.version, f.file_id, f.name, f.plugin_id
				FROM files AS f
				INNER JOIN strings AS s ON f.file_id = s.file_id
				INNER JOIN release_train_projects as v ON (f.project_id = v.project_id AND f.version = v.version)
				WHERE f.is_active
				AND v.train_id = '" . $train->id . "'";
		} else {
			$sql = "SELECT DISTINCT f.project_id, f.version, f.file_id, f.name, f.plugin_id
				FROM files AS f
				INNER JOIN strings AS s ON f.file_id = s.file_id
				INNER JOIN translations AS t ON (s.string_id = t.string_id AND t.is_active)
				INNER JOIN release_train_projects as v ON (f.project_id = v.project_id AND f.version = v.version)
				WHERE t.language_id = " . $language->id . "
				AND f.is_active
				AND v.train_id = '" . $train->id . "'";
		}
		$file_result = mysql_query($sql);
		$plugins = array();
		while (($file_row = mysql_fetch_assoc($file_result)) != null) {
			$f = new File();
			$f->file_id = $file_row['file_id'];
			$f->name = $file_row['name'];
			$f->plugin_id = $file_row['plugin_id'];
			$f->project_id = $file_row['project_id'];
			$f->version = $file_row['version'];
			$plugins[$file_row['plugin_id']][] = $f;
		}
		$fragments = array();
		foreach($plugins as $plugin_id => $files) {
			$fragment = new Fragment($plugin_id, $files, $language, $train);
			$fragments[] = $fragment;
		}
		
		return $fragments;
	}
	
	/*
	 * Generates a manifest file in its META-INF folder in the $fragment_root directory.
	 */
	function generate_manifest($fragment_root) {
		$fragment_id = $this->plugin_id .".nl._". $this->language->iso;
		exec("mkdir $fragment_root/META-INF" );
		$outp = fopen("$fragment_root/META-INF/MANIFEST.MF", "w");
		fwrite($outp, "Manifest-Version: 1.0\n");
		fwrite($outp, "Bundle-Name: ".$this->plugin_id." ".$this->language->name." NLS Support\n");
		fwrite($outp, "Bundle-SymbolicName: $fragment_id ;singleton=true\n");
		fwrite($outp, "Bundle-Version: ". $this->train->version ."_". $this->train->timestamp . "\n");
		fwrite($outp, "Bundle-Vendor: Eclipse.org\n");
		fwrite($outp, "Fragment-Host: $this->plugin_id\n");
		fclose($outp);
	}
	
	function generateFragment($fragment_root, $output_dir) {
		if (file_exists($fragment_root)) {
			exec("rm -rf $fragment_root; mkdir $fragment_root");
		} else {
			exec("mkdir -p $fragment_root");
		}
		exec("cp ". LEGAL_FILES_DIR. "about.html $fragment_root");
		$this->generate_manifest($fragment_root);
		foreach($this->files as $file) {
			$fullpath = $fragment_root . $file->appendLangCode($this->language->iso);
			$this->generate_properties_file($fullpath, $file->strings4PropertiesFile($this->language));
		}
		$this->jar($fragment_root, $output_dir);
	}
	
	/*
     * Outputs a properties file at the filepath location, creating the needed folders if need be.
     * The hash passed as the second parameter is supposed to contain a mapping from key to translation.
     *
     */
	function generate_properties_file($filepath, $keys2values) {
		preg_match("/^((.*)\/)?(.+?)$/", $filepath, $matches);
		exec("mkdir -p \"" . $matches[1] . "\"");
		$outp = fopen($filepath, "w");
		fwrite($outp, "# Copyright by many contributors; see http://babel.eclipse.org/\n");
		foreach ($keys2values as $key => $trans) {
  			fwrite($outp, "\n" . $key . "=");
			# echo "${leader1S}${leaderS}${leaderS}${leaderS}" . $key . "=";
			if ($trans) {
				# json_encode returns the string with quotes fore and aft.  Need to strip them.
				# $tr_string = preg_replace('/^"(.*)"$/', '${1}', json_encode($trans));
				# $tr_string = str_replace('\\\\', '\\', $tr_string);
				$tr_string = toescapedunicode($trans);
				fwrite($outp, $tr_string);
				# echo $trans;
			}
			fwrite($outp, "\n");
		}
		fclose($outp);
	}
	
	
	function jar($dir, $output_dir) {
		$jar_name = $output_dir . "/" . $this->fragment_filename();
		internalJar($dir, $jar_name);
		$this->filesize = filesize($jar_name);
	}
	
	function fragment_filename() {
		$fragment_filename = $this->fragment_id() ."_". $this->train->version ."_". $this->train->timestamp .".jar";
		return $fragment_filename;
	}
	
	function fragment_id() {
		return $this->plugin_id .".nl_". $this->language->iso;
	}
	
	function associated_projects() {
		$projects = array();
		foreach($this->files as $file) {
			$projects[$file->project_id] = new Project($file->project_id, $file->version);
		}	
		return array_values($projects);
	}
}