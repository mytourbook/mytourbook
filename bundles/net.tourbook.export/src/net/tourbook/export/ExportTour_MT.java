/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.export;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.extension.export.ExportTourExtension;

import org.eclipse.swt.widgets.Display;

/**
 * Export tours in with MT data format
 */
public class ExportTour_MT extends ExportTourExtension {

   public static final String TEMPLATE = "/format-templates/mt-1.0.vm"; //$NON-NLS-1$

   /**
    * Plugin extension constructor
    */
   public ExportTour_MT() {}

   @Override
   public void exportTours(final ArrayList<TourData> tourDataList, final int tourStartIndex, final int tourEndIndex) {

      new DialogExportTour(
            Display.getCurrent().getActiveShell(),
            this,
            tourDataList,
            tourStartIndex,
            tourEndIndex,
            TEMPLATE).open();
   }
}
