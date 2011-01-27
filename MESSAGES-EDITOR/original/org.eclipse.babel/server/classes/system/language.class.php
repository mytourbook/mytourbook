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
 *    Kit Lo (IBM) - patch, bug 261739, Inconsistent use of language names
 *    Kit Lo (IBM) - Beautify Babel Supported Languages table
*******************************************************************************/

class Language {
  
	public $name                = '';
	public $iso                 = '';
	public $locale              = '';
	public $id                  = '';
  
	/**
	* Default constructor
	*/
	function Language($name = '', $iso= '', $locale = '', $id = '') {
		$this->name   = $name;
		$this->iso    = $iso;
		$this->locale = $locale;
		$this->id     = $id;
    
		if (strcmp($iso, "en") == 0) {
			$this->iso = "en_AA";
			$this->name = "Pseudo Translations";
		}
    
		if ($locale != null) {
			$this->name = $this->name . " (" . $this->locale . ")";
		}
	}
  
	/**
	* A constructor expecting an associative array as its unique parameter
	*/
	static function fromRow($row) {
		return new Language($row['name'], $row['iso_code'], $row['locale'], $row['language_id']);
	}
  
	static function all() {
		$langs = array();
		$language_result = mysql_query("SELECT * FROM languages WHERE languages.is_active ORDER BY name, locale");
		while (($language_row = mysql_fetch_assoc($language_result)) != null) {
			$langs[] = Language::fromRow($language_row);
		}
		return $langs;
	}
}