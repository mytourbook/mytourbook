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

class ReleaseTrain {

	public $id = '';
	public $version = '';
	public $timestamp = '';
	
	/**
	* Default constructor for now.
	**/
	function ReleaseTrain($id) {
		$this->id = $id;
		$this->version = "3.4.0";
		if (strcmp($id, "europa") == 0) {
			$this->version = "3.3.0";
		}
		$this->timestamp = date("Ymdhis");
	}
	
	static function all() {
		$trains = array();
		$train_result = mysql_query("SELECT DISTINCT train_id FROM release_train_projects");
		while (($train_row = mysql_fetch_assoc($train_result)) != null) {
			$trains[] = new ReleaseTrain($train_row['train_id']);
		}
		return $trains;
	}
	
}