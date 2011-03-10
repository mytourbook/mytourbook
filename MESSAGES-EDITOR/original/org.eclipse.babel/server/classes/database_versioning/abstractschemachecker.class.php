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


function ourTrim(&$item1, $key){$item1 = trim($item1);}

abstract class AbstractSchemaChecker {
	abstract public function check_and_modify( $context );

	public function check_and_modify_table( $databasename, $tablename, $schemas, $context ) {
		$dbh = $context->database( $databasename );
		$tablenamesuffix = $context->initmode;
		
		echo "working on table '$tablename$tablenamesuffix' \n";
		
		$createfunction = "create_$tablename";
		if( $context->testmode && $context->initmode === false ) {
			echo "..creating in memory <br>\n";
		 	$sql = "DROP TABLE IF EXISTS $tablename ";
		 	mysql_remember_query( $sql, $dbh );
		 	mysql_error_check();
		 	$sql = $this->$createfunction('');
		 	$translations = array(
				'/param(\d+) TEXT/' => 'param\1 varchar(1024)',
				'/\s[Tt][Ee][Xx][Tt]/' => ' varchar(1024)',
				'/blob/' => 'varbinary(8192)',
				'/ENGINE=([A-Za-z]+)/' => '',
				'/mediumtext/' => 'varchar(256)',
				'/tinytext/' => 'varchar(256)',
				'/FULLTEXT/' => '',
				);
		 	$a1 = array();
		 	$a2 = array();
		 	foreach( $translations as $k => $v ) {
		 		$a1[] = $k;
		 		$a2[] = $v;
		 	}
		 	$sql = preg_replace( $a1, $a2, $sql );
		 	$sql = $sql . " ENGINE=MEMORY SELECT * FROM $tablename" . '_prototype';
			$this->create_db( $sql,  $dbh );
			return true;
		} else {
			$table_name = $tablename.$tablenamesuffix;		
			$result = mysql_remember_query( "DESCRIBE $tablename$tablenamesuffix", $dbh );
			if( strlen(mysql_error()) > 0 ) {
				if( $context->devmode
				 || $context->testmode
				 || $databasename == 'myfoundation' ) {
					echo "..does not exist, creating <br>\n";
					$this->create_db( $this->$createfunction($tablenamesuffix),  $dbh );
					$result = mysql_remember_query( "DESCRIBE $tablename$tablenamesuffix", $dbh );
					$str = $this->table_has_schema($result, $tablename . $tablenamesuffix, $schemas[count($schemas)], $dbh);
					if( $str === false )
						return true;
					else {
						echo $str;
						echo "..schema does not match after creation of table $tablename$tablenamesuffix, error <br>\n";
						return false;
					}
				} else {
//				print mysql_error();
					echo "..does not exist, error <br>\n";
					$this->createTableFromSchema($table_name,$schemas[count($schemas)],$dbh,$context);				
					$result = mysql_remember_query( "DESCRIBE $tablename$tablenamesuffix", $dbh );
					$str = $this->table_has_schema($result, $tablename . $tablenamesuffix, $schemas[count($schemas)], $dbh);
					if( $str === false )
						return true;
					else {
						echo $str;
						echo "..schema does not match after creation, error <br>\n";
						return false;
					}
				}
			}
			$lastmatch = 0;
			$laststr = '';
			for( $i = 1; $i <= count($schemas); $i++ ) {
				if( $i > 1 )
					$result = mysql_remember_query( "DESCRIBE $tablename$tablenamesuffix", $dbh );
				$str = $this->table_has_schema($result, $tablename . $tablenamesuffix, $schemas[$i], $dbh);
				if( $str === false )
					$lastmatch = $i;
				else
					$laststr = $str;
			}
			if( $lastmatch == 0 ) {
				if( $context->devmode
				 || $context->testmode ) {
		  			echo "..no matching schema, deleting and recreating <br>\n";
		  			mysql_remember_query( "DROP TABLE $tablename$tablenamesuffix", $dbh);
					mysql_error_check();
					$this->create_db( $this->$createfunction($tablenamesuffix),  $dbh );
					$result = mysql_remember_query( "DESCRIBE $tablename$tablenamesuffix", $dbh );
					$str = $this->table_has_schema($result, $tablename . $tablenamesuffix, $schemas[count($schemas)], $dbh);
					if( $str === false )
						return true;
					else {
						echo $str;
						echo "..schema does not match after creation, error <br>\n";
						return false;
					}
				} else {
					echo $laststr;
					echo "..no matching schema, error <br>\n";
					return false;
				}
			}
			
			if( $lastmatch == count($schemas) ) {
				echo "..correct <br>\n";
				return true;
			}
			if( $context->devmode
			 || $context->testmode
			 || $databasename == 'myfoundation' ) {
				echo "..old schema, updating <br>\n";
			 	for( $i = $lastmatch; $i < count($schemas); $i++ ) {
			 		$modifyfunction = 'modify_' . $tablename . '_' . $i . '_' . ($i+1);
			 		$this->$modifyfunction( $tablenamesuffix, $dbh );
			 	}
				$result = mysql_remember_query( "DESCRIBE $tablename$tablenamesuffix", $dbh );
				$str = $this->table_has_schema($result, $tablename . $tablenamesuffix, $schemas[count($schemas)], $dbh);
				if( $str === false )
					return true;
				else {
					echo $str;
					echo "..schema does not match after modification, error <br>\n";
					return false;
				}
			} else {
				$this->updateTableFromSchema($table_name,$schemas[count($schemas)],$dbh,$context);				
			
				echo "..old schema, error <br>\n";
				return false;
				
			}
		}
	}
	
	private function createTableFromSchema($table_name,$schema,$dbh,$context){
		$fields = $this->splitSchema($schema);
		$primary_keys = array();
		foreach($fields as $field){
			$query_guts .= $this->fieldToQuery($field,$primary_keys).",";
		}
		$query = "create table $table_name ( ".trim($query_guts,",\n");
		if(!empty($primary_keys['PRI'])){
			$query .= ", primary key(".implode($primary_keys['PRI'],",").")";
		}
		if(!empty($primary_keys['MUL'])){
			foreach($primary_keys['MUL'] as $mul){
				$query .= ", index($mul)";
			}
		}
		$query .= ")";
		
		mysql_query($query,$dbh);
	}
	
	
	//NEED TO HANDLE EDGES CASES
	// - WHEN ALL FIELDS ARE DROPPED FROM TABLE.. TREAT LIKE DROP TABLE CREATE TABLE WITH NEW SCHEMA.
	
	private function updateTableFromSchema($table_name,$schema,$dbh,$context){
		$old_fields = $this->getTableDescription($table_name,$dbh);
		$new_fields = $this->splitSchema($schema);
		//run prerefactor quries
		
print "\n";//gogo

		//find all old fields that have changed
		//find all fields to drop
		foreach($old_fields as $k => $v){
			if($diff = array_diff($new_fields[$k], $v)){
				print "MODIFY - $k\n";
				if($diff['Default'] or $diff['Field']){
					$modq = "alter table $table_name modify  $k ".$new_fields[$k]['Type']." ".$new_fields[$k]['Default']. " ".$diff['Extra'];
					mysql_remember_query($modq,$dbh);
				}
			}else{
				//drop new field
				print "DROP - $k\n";
				$query = "alter table $table_name drop $k ";
				if($v['Key'] == "PRI"){
					$query .= " DROP PRIMARY KEY ";
				}
				if($v['Key'] == "MUL"){
					$query .= " DROP INDEX ";
				}
				mysql_query($query);
			}
		}

		//find all to add
		$endqueries[] = array();
		foreach($new_fields as $k => $v){
			if(!isset($old_fields[$k])){
				print "add!!!! $k\n";
				
				$keys = array();
				$query = "alter table $table_name add ";
				$query.= $this->fieldToquery($v,&$keys);
				
				if(is_array($keys['PRI']) and in_array($k,$keys['PRI'])){
					$query .= " PRIMARY KEY ";
				}
				if(is_array($keys['MUL']) and in_array($k,$keys['MUL'])){
//					$query .= " INDEX ";
					$endqueries[] = "alter table $table_name add index ($k) ";
				}
				
				print $query."\n";
				mysql_query($query);
				foreach($endqueries as $q){
					mysql_query($q);
				}
				
			}
		}
		//check schema
		
	}
	
	private function fieldToquery($fields,&$keys){
//gogo
//		print_r($fields);	
		list($type,$size) = explode("(" , trim($fields['Type'],") ") );

		foreach($fields as $k => $v){
			if($k == 'Null'){
				if($v == 'NO'){
					$v = "not null";
				}else{
					$v = "null";
				}
			}
			if($k == 'Default'){
				if($v != "")
					$v = "DEFAULT $v";
				else
					$v = "";
					
			}
			if($k == 'Key'){
				switch($v){
				case 'PRI':
					$keys['PRI'][] = $fields['Field'];
					break;
				case 'MUL':
					$keys['MUL'][] = $fields['Field'];
					break;
				default:
					break;
				}
				$v = ""; //clear the value they keys will be appened to end of query
			}
			
			$line .= " $v ";
		}
		return $line;		
	}

	private function getTableDescription($table_name,$dbh){
		$query = "DESCRIBE $table_name";
		print $query;
		$result = mysql_query($query,$dbh);
		$ret = array();
		while( $row = mysql_fetch_assoc($result) ) {
			$ret[$row['Field']] = $row;
		}	
		return $ret;
	}
	
	private function splitSchema($schema){ //todo need to break out the same functionality from table_has_schema
		$lines = explode("\n",trim($schema));
		
		$breaker = array_shift($lines);
		$fieldKeys = explode("|",trim(array_shift($lines),"|"));
		array_walk($fieldKeys, 'ourTrim');	
		
		array_shift($lines); //shift off the next filler line +----+---
		
		while($line = array_shift($lines)){
			if($line == $breaker){
				break;
			}
			$fieldValues = explode("|",trim(trim($line),"|"));
			$aray = array_combine($fieldKeys,$fieldValues); 

			$aray['seen'] = false;
			array_walk($aray, 'ourTrim');	
			$ret[$aray['Field']] = $aray;
		}
		
//		print_r($ret);
		return $ret;
	}
	
	private function create_db( $sql, $dbh ) {
		mysql_remember_query( $sql, $dbh );
		mysql_error_check();
	}
	
	/* returns error string if error, null if schema matches */
	private function table_has_schema( $result, $tablename, $schemadescription, $dbh ) {
		$schema = $this->splitSchema($schemadescription);
		
		while( $row = mysql_fetch_assoc($result) ) {
			if( !array_key_exists($row['Field'], $schema) ) {
				$rtrn .= "..column " . $row['Field'] . " not in schema  <br>\n";
				continue;
			}
			$schema[$row['Field']]['seen'] = true;
			$rec = $schema[$row['Field']];
			if( $rec['Type'] != $row['Type'] ) {
				$rtrn .= "..column " . $row['Field'] . " with different type (" . $rec['Type'] . " vs " . $row['Type'] . ") <br>\n";
				continue;
			}
			$a = $rec['Null'];
			$b = $row['Null'];
			if( $a == '' ) $a = 'NO';
			if( $b == '' ) $b = 'NO';
			/* some versions of MySQL seem to ignore NOT NULL when applied to timestamps */
			if( $row['Type'] == 'timestamp' ) $b = 'NO';
			/* */
			if( $a != $b ) {
				$rtrn .= "..column " .  $row['Field']. " with different NULL (" . $rec['Null'] . " vs " . $row['Null'] . ") <br>\n";
				continue;
			}
			if( $rec['Key'] != $row['Key']) {
				if( $rec['Key']) {
				  	$rtrn .= "..column " . $row['Field']. " with different keys (" . $rec['Key'] . ") vs (" . $row['Key']. ") <br>\n";
					continue;
					// note that we allow the table to have more keys than the schema, but not fewer
				}
			}
			if( $rec['Extra'] != $row['Extra']) {
			  	$rtrn .= "..column " . $row['Field']. " with different extra infomration (" . $rec['Extra'] . " vs " . $row['Extra'] . ") <br>\n";
				continue;
			}
			// not able to check 'default' at this time
		}

		foreach( $schema as $rec ) {
			if( !$rec['seen']) {
				$rtrn .= "..column " . $rec['Field'] . " not in table <br>\n";
				continue;
			}
		}
		if( $rtrn == '' ) return false;
		return $rtrn;
	}
}


?>