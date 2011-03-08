<?php

class refactor_test_SchemaChecker extends AbstractSchemaChecker {

  public function check_and_modify( $context ) {
	return  $this->check_test_table($context);
  }

  public function check_test_table($context){
		$schemas = array();
		$schemas[1] = "
+--------+--------------+------+-----+---------+-------+
| Field  | Type         | Null | Key | Default | Extra |
+--------+--------------+------+-----+---------+-------+
| bug_id | mediumint(9) | NO   | PRI | 0       |       |
| who    | mediumint(9) | NO   |     | 0       |       |
+--------+--------------+------+-----+---------+-------+
";
		
$schemas[2] = "
+------------------+--------------+------+-----+---------+----------------+
| Field            | Type         | Null | Key | Default | Extra          |
+------------------+--------------+------+-----+---------+----------------+
| bug_id           | smallint(6)  | NO   | PRI | NULL    | auto_increment | 
| name             | varchar(64)  | NO   | MUL |         |                | 
| product_id       | smallint(6)  | NO   | MUL | 0       |                | 
| initialowner     | mediumint(9) | NO   |     | 0       |                | 
| initialqacontact | mediumint(9) | YES  |     | NULL    |                | 
| description      | mediumtext   | NO   |     |         |                | 
+------------------+--------------+------+-----+---------+----------------+
";

	return $this->check_and_modify_table("refactor_test","created_table",$schemas,$context);  
  }
  
}

?>