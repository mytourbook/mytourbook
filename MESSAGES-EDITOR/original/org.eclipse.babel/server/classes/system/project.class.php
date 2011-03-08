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

class Project {
	public $id = '';
	public $version = '';

	function Project($id, $version) {
		$this->id = $id;
		$this->version = $version;
	}
	
	function toString() {
		return "$id $version";
	}
}