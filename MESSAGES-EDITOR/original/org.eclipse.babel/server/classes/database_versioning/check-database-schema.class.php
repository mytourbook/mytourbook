<?php
/*******************************************************************************
 * Copyright (c) 2006-2007 Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bjorn Freeman-Benson - initial API and implementation
 *    Ward Cunningham - initial API and implementation
 *    Karl Mathias - initial API and implementation
 *******************************************************************************/

//require_once( $GLOBALS['CLASSES_DIRECTORY'] . "functions.php" );
//require_once( $GLOBALS['CLASSES_DIRECTORY'] . "context.class.php" );



class CheckAndModifyDatabaseSchema {
	public function check_and_modify( $context ) {
	  	$rtrn = true;
	    $dir = $context->get("components_directory");
	    foreach( dirs($dir) as $component ) {
			print $dir . "$component/check-database-schema.php\n";
	
			if( file_exists( $dir . "$component/check-database-schema.php" ) ) {
				echo "Checking db schema for table '$component'\n";
				mysql_select_db($component);
				// the included code uses the context to check (and perhaps modify) the database(s) schemas
				include_once( $dir . "$component/check-database-schema.php" );
				$checkername = $component . "_SchemaChecker";
				$obj = new $checkername ;
				if( !$obj->check_and_modify( $context ) ) {
					echo "***Trouble during checking database schema for table: '$component' ****\n";
					$rtrn = false;
				}
			}
		}
		return $rtrn;
	}
}

?>