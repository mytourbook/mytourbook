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
require(BABEL_BASE_DIR . "classes/system/release_train.class.php");

class DescribeReleaseTrainClass extends PHPSpec_Context {
    public function before() {
            
    }
    
    public function itShouldSetATimeStampOnCreation() {
      $rt = new ReleaseTrain("some_id");
      $this->spec($rt->timestamp)->shouldNot->beNull();
    }

// TODO add a spec for the version of the release train
    
   public function itShouldLoadAllTheReleaseTrains() {
	 $train_result = mysql_query("SELECT DISTINCT train_id FROM release_train_projects");
     $all = ReleaseTrain::all();
	 $this->spec(count($all))->should->equal(mysql_num_rows($train_result));
   }
}
?>