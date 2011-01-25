/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import org.eclipse.swt.widgets.Display;

/**
 * Superclass for template based export formats. Provides default implementation of {@link ExportTourExtension}.
 * <p>
 * Subclasses need to override <code>getFormatTemplate</code>.
 */
public abstract class AbstractExportTourFormat extends ExportTourExtension {

	/**
	 * @return resource path to the template file for the export file format
	 */
	public abstract String getFormatTemplate();
	
	@Override
	public void exportTours(final ArrayList<TourData> tourDataList, final int tourStartIndex, final int tourEndIndex) {

		DialogExportTour dialogExportTour = new DialogExportTour(
				Display.getCurrent().getActiveShell(),
				this,
				tourDataList,
				tourStartIndex,
				tourEndIndex,
				getFormatTemplate()
		);

		dialogExportTour.open();
	}

	@Override
	public List<String> exportTourHeadless(final List<TourData> tourDataList, final String targetDir) throws IOException {

		HeadlessExportTour headlessExportTour = new HeadlessExportTour(this);
		return headlessExportTour.doExportTour(tourDataList, targetDir);
	}
}
