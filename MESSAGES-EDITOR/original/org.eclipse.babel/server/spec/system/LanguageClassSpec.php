<?php
/*******************************************************************************
 * Copyright (c) 2007-2009 Intalio, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antoine Toulme, Intalio Inc.
*******************************************************************************/

define('BABEL_BASE_DIR', "../../");

require("../spec_helper.php");
require (BABEL_BASE_DIR . "classes/system/language.class.php");

class DescribeLanguageClass extends PHPSpec_Context {
	
	public $lang_hash;
    public function before() {
      $lang_hash = array();
      $lang_hash['name'] = 'name';
      $lang_hash['iso_code'] = 'iso_code';
      $lang_hash['language_id'] = '1';
      $this->lang_hash =  $lang_hash;
    }

    public function itShouldBeCreatedFromAHash() {
	  $lang = Language::fromRow($this->lang_hash);
	  $this->spec($lang->name)->should->equal('name');
	  $this->spec($lang->iso)->should->equal('iso_code');
	  $this->spec($lang->id)->should->equal('1');
    }
    
    public function itShouldAddTheLocaleBeforeTheNameWhenThereIsOne() {
	  $this->lang_hash['locale'] = 'locale';
      $lang = Language::fromRow($this->lang_hash);
      $this->spec($lang->name)->should->equal('locale name');
    }
}
?>
