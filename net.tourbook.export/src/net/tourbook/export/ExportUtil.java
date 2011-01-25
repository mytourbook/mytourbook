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

import net.tourbook.data.TourData;
import net.tourbook.ui.UI;

/**
 * Helper methods
 */
public class ExportUtil {

	/**
	 * Builds an export file name for a tour.
	 * 
	 * @param tourData
	 *            tour to build the file name for
	 * @param fileExtension
	 *            extension of the export file according to the export format, e.g. 'gpx'
	 * @return export file name
	 */
	// TODO NR: refactoring: common method with DialogExportTour
	public static String getFileName(final TourData tourData, final String fileExtension) {
		final String fileName = new StringBuilder(UI.format_yyyymmdd_hhmmss(tourData)).append(".") //$NON-NLS-1$
				.append(fileExtension)
				.toString();

		return fileName;
	}
}
