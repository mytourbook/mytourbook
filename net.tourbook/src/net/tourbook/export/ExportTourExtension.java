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

import java.util.ArrayList;

import net.tourbook.data.TourData;

public abstract class ExportTourExtension {

	private String	fExportId;
	private String	fVisibleName;
	private String	fFileExtension;

	public abstract void exportTours(ArrayList<TourData> tourDataList);

	public String getExportId() {
		return fExportId;
	}

	public String getFileExtension() {
		return fFileExtension;
	}

	public String getVisibleName() {
		return fVisibleName;
	}

	public void setExportId(final String fExportId) {
		this.fExportId = fExportId;
	}

	public void setFileExtension(final String fFileExtension) {
		this.fFileExtension = fFileExtension;
	}

	public void setVisibleName(final String fVisibleName) {
		this.fVisibleName = fVisibleName;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("id: ")//$NON-NLS-1$
				.append(fExportId)
				.append(" \t") //$NON-NLS-1$
				//
				.append("name: ") //$NON-NLS-1$
				.append(fVisibleName)
				.append(" \t") //$NON-NLS-1$
				//
				.append("extension: ")//$NON-NLS-1$
				.append(fFileExtension)
				.append(" \t") //$NON-NLS-1$
				//
				.toString();

	}

}
