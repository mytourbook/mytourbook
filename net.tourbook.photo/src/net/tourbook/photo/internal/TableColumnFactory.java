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
package net.tourbook.photo.internal;

import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.TableColumnDefinition;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.SWT;

public abstract class TableColumnFactory {

	public static final TableColumnFactory PHOTO_FILE_ALTITUDE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(8);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoAltitude", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Altitude);
//			colDef.setColumnHeader(UI.UNIT_LABEL_ALTITUDE);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Photo_Altitude_Tooltip);
//			colDef.setColumnUnit(UI.UNIT_LABEL_ALTITUDE);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory PHOTO_FILE_DATE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(10);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoDate", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Date);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_Date);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory PHOTO_FILE_IMAGE_DIRECTION_DEGREE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(6);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoImageDirectionDegree", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_ImageDirectionDegree_Label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_ImageDirectionDegree_Header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Photo_ImageDirectionDegree_Tooltip);
			colDef.setColumnUnit(UI.SYMBOL_DEGREE);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory PHOTO_FILE_IMAGE_DIRECTION_TEXT = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(6);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoImageDirectionText", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_ImageDirectionText_Label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_ImageDirectionDegree_Header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Photo_ImageDirectionText_Tooltip);
			colDef.setColumnUnit(UI.SYMBOL_DEGREE);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};

//	public static final TableColumnFactory LATITUDE = new TableColumnFactory() {
//
//		@Override
//		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
//
//			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "latitude", SWT.LEAD); //$NON-NLS-1$
//
//			colDef.setColumnLabel(Messages.ColumnFactory_latitude_label);
//			colDef.setColumnHeader(Messages.ColumnFactory_latitude);
//			colDef.setColumnToolTipText(Messages.ColumnFactory_latitude_tooltip);
//			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));
//
//			return colDef;
//		};
//	};
	
	public static final TableColumnFactory PHOTO_FILE_LOCATION = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(40);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoFileLocation", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Location);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_Location);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};

//	public static final TableColumnFactory LONGITUDE = new TableColumnFactory() {
//
//		@Override
//		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
//
//			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "longitude", SWT.LEAD); //$NON-NLS-1$
//
//			colDef.setColumnLabel(Messages.ColumnFactory_longitude_label);
//			colDef.setColumnHeader(Messages.ColumnFactory_longitude);
//			colDef.setColumnToolTipText(Messages.ColumnFactory_longitude_tooltip);
//			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));
//
//			return colDef;
//		};
//	};

	public static final TableColumnFactory PHOTO_FILE_NAME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoFileName", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Name);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_Name);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	

	public static final TableColumnFactory PHOTO_FILE_ORIENTATION = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(5);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoOrientation", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Orientation);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_Orientation_Header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Photo_Orientation_Tooltip);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};

	public static final TableColumnFactory PHOTO_FILE_OTHER_TAGS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(50);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoOtherTags", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_OtherTags);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_OtherTags);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};

	public static final TableColumnFactory PHOTO_FILE_DIMENSION = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(14);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoDimension", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Dimension);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_Dimension);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};

	public static final TableColumnFactory PHOTO_FILE_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(14);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Photo_Time);
			colDef.setColumnHeaderText(Messages.ColumnFactory_Photo_Time);
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
