/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.IReferenceTourProvider;

import org.eclipse.jface.action.Action;

class ActionCompareByElevation_AllTours extends Action {

   private final IReferenceTourProvider _refTourProvider;

   public ActionCompareByElevation_AllTours(final IReferenceTourProvider refTourProvider) {

      super(UI.EMPTY_STRING, AS_PUSH_BUTTON);
      _refTourProvider = refTourProvider;

      setText(Messages.Elevation_Compare_Action_CompareAllTours);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourCatalog_CompareWizard));
   }

   @Override
   public void run() {

      final ArrayList<RefTourItem> selectedRefTourItems = _refTourProvider.getSelectedRefTourItems();

      final ArrayList<Long> allTourIds = _refTourProvider.isUseFastAppFilter()

            ? TourDatabase.getAllTourIds_WithFastAppFilter()
            : TourDatabase.getAllTourIds();

      final Long[] allTourIdsAsArray = allTourIds.toArray(new Long[allTourIds.size()]);

      TourCompareManager.compareTours(selectedRefTourItems, allTourIdsAsArray);
   }
}
