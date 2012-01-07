/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import net.tourbook.util.ColumnDefinition;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.TableColumnDefinition;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.SWT;

public abstract class TableColumnFactory {

	public static final TableColumnFactory PHOTO_FILE_NAME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoFileName", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Name);
			colDef.setColumnHeader(Messages.ColumnFactory_Photo_Name);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	


	/**
	 * @param columnManager
	 * @param pixelConverter
	 * @return Returns a {@link TableColumnDefinition}
	 */
	public abstract ColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter);
}
