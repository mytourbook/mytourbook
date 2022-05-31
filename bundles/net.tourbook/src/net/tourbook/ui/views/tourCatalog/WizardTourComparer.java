/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.IReferenceTourProvider;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;

public class WizardTourComparer extends Wizard {

   public static final String        DIALOG_SETTINGS_SECTION           = "WizardTourComparer";                            //$NON-NLS-1$

   static final String               GRID_PREFIX_REF_TOUR_COMPARE_TOUR = "GRID_PREFIX_REF_TOUR_COMPARE_TOUR";             //$NON-NLS-1$

   private final IDialogSettings     _state                            = TourbookPlugin.getState(DIALOG_SETTINGS_SECTION);

   private WizardPage_10_CompareTour _pageCompareTour;

   private IReferenceTourProvider    _refTourProvider;

   public WizardTourComparer(final IReferenceTourProvider refTourProvider) {

      _refTourProvider = refTourProvider;

      setWindowTitle(Messages.tourCatalog_wizard_Wizard_title);

      restoreState();

   }

   @Override
   public void addPages() {

      addPage(_pageCompareTour = new WizardPage_10_CompareTour(_refTourProvider));
   }

   @Override
   public boolean performCancel() {

      saveState();

      return true;
   }

   @Override
   public boolean performFinish() {

      saveState();

      final ArrayList<RefTourItem> allSelectedRefTourItems = _refTourProvider.getSelectedRefTourItems();
      final Object[] allComparedTours = _pageCompareTour.getComparedTours();

      // close wizard before running the compare
      _pageCompareTour.getControl().getDisplay().asyncExec(() -> {

         TourCompareManager.compareTours(allSelectedRefTourItems, allComparedTours);
      });

      return true;
   }

   private void restoreState() {

      super.setDialogSettings(_state);
   }

   private void saveState() {

      _pageCompareTour.saveState();
   }

}
