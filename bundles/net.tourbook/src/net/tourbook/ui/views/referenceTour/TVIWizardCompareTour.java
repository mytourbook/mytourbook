/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

public class TVIWizardCompareTour extends TVIWizardCompareItem {

   long tourId;

   int  tourYear;
   int  tourMonth;
   int  tourDay;

   long colDistance;
   long colElapsedTime;
   long colAltitudeUp;
   long tourTypeId;

   public TVIWizardCompareTour(final TVIWizardCompareItem parentItem) {

      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {}

   /**
    * Tour items do not have children
    */
   @Override
   public boolean hasChildren() {
      return false;
   }

   @Override
   public String toString() {

      return "TVIWizardCompareTour [\n" //$NON-NLS-1$

            + "tourId         =" + tourId + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "tourYear       =" + tourYear + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "tourMonth      =" + tourMonth + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "tourDay        =" + tourDay + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "colDistance    =" + colDistance + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "colElapsedTime =" + colElapsedTime + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "colAltitudeUp  =" + colAltitudeUp + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//            + "tourTypeId     =" + tourTypeId + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "]\n"; //$NON-NLS-1$
   }
}
