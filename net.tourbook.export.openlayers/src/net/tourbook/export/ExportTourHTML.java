/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.util.StatusUtil;

import org.eclipse.swt.widgets.Display;

/**
 * HTML export tour extension. Creates a HTML file showing the exported tours with OpenLayers.
 */
public class ExportTourHTML extends ExportTourExtension {

	//
	// Delegates the actual tour export to another file format exporter, as defined by GPX_EXTENSION.
	// The HTML file only references this/these separate tour file(s).
	// 

	public static final String	GPX_EXTENSION	= "gpx";	//$NON-NLS-1$

	/**
	 * plugin extension constructor
	 */
	public ExportTourHTML() {}

	@Override
	public void exportTours(ArrayList<TourData> tourDataList, int tourStartIndex, int tourEndIndex) {

		// The tour export format the actual tour export is delegated to. Obtained by searching all registered export
		// extensions for the given file extension.
		final ExportTourExtension exportTour = OpenLayersExportUtil.getExportTour(GPX_EXTENSION);

		if (exportTour instanceof AbstractExportTourFormat) {
			final AbstractExportTourFormat delegate = (AbstractExportTourFormat) exportTour;

			// open the extended export dialog with the delegate export format
			DialogExportTourHTML dialogExportTour = new DialogExportTourHTML(
					Display.getCurrent().getActiveShell(),
					delegate,
					tourDataList,
					tourStartIndex,
					tourEndIndex,
					delegate.getFormatTemplate(),
					this);

			dialogExportTour.open();

		} else {

			StatusUtil.log("exportTour is not a AbstractExportTourFormat: '" + exportTour + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public List<String> exportTourHeadless(List<TourData> tourDataList, String completeFilePath) throws IOException {

		throw new RuntimeException("not implemented"); //$NON-NLS-1$
	}
}
