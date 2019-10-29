/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.trainingstress;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class PrefPageBikeScore implements IPrefPageTrainingStressModel {
   private Group _bikeScoreGroup;

   @Override
   public void dispose() {
      _bikeScoreGroup = null;

   }

   /**
    * UI for the BikeScore preferences
    */
   @Override
   public Group getGroupUI(final Composite parent) {

      if (_bikeScoreGroup == null) {
         _bikeScoreGroup = new Group(parent, SWT.NONE);
         GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_bikeScoreGroup);
         {
            final Label label = new Label(_bikeScoreGroup, SWT.NONE);
            label.setText("BIKESCORE");
         }
      }

      return _bikeScoreGroup;

   }

   @Override
   public String getId() {

      return "BikeScore";
   }
}
