/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import java.io.File;

import net.tourbook.data.TourData;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;

public abstract class TableColumnFactory {

	

	public static final TableColumnFactory DB_STATUS = new TableColumnFactory() {

		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {

			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "dbStatus", SWT.CENTER); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_db_status_label);
			colDef.setToolTipText(Messages.ColumnFactory_db_status_tooltip);
			colDef.setWidth(20);

			return colDef;
		};
	};

	public static final TableColumnFactory TOUR_DATE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourdate", SWT.TRAIL); //$NON-NLS-1$
			colDef.setText(Messages.ColumnFactory_date);
			colDef.setLabel(Messages.ColumnFactory_date_label);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(12));

			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_START_TIME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "startTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_time_label);
			colDef.setText(Messages.ColumnFactory_time);
			colDef.setToolTipText(Messages.ColumnFactory_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TYPE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourType", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_tour_type_label);
			colDef.setToolTipText(Messages.ColumnFactory_tour_type_tooltip);
			colDef.setWidth(18);

			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TITLE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTitle", SWT.BEGINNING); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_tour_title_label);
			colDef.setText(Messages.ColumnFactory_tour_title);
			colDef.setToolTipText(Messages.ColumnFactory_tour_title_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(25));

			return colDef;
		};
	};
	
	public static final TableColumnFactory RECORDING_TIME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "recordingTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_recording_time_label);
			colDef.setText(Messages.ColumnFactory_recording_time);
			colDef.setToolTipText(Messages.ColumnFactory_recording_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory DRIVING_TIME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "drivingTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_driving_time_label);
			colDef.setText(Messages.ColumnFactory_driving_time);
			colDef.setToolTipText(Messages.ColumnFactory_driving_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
		};
	};
	
	public static final TableColumnFactory DISTANCE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "distance", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_distance_label + " (" + UI.UNIT_LABEL_DISTANCE + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			colDef.setText(UI.UNIT_LABEL_DISTANCE);
			colDef.setToolTipText(Messages.ColumnFactory_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};
	
	public static final TableColumnFactory SPEED = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "speed", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_speed_label);
			colDef.setText(UI.UNIT_LABEL_SPEED);
			colDef.setToolTipText(Messages.ColumnFactory_speed_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(9));

			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_UP = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeUp", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_altitude_up_label  + " (" + UI.UNIT_LABEL_ALTITUDE + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			colDef.setText(UI.UNIT_LABEL_ALTITUDE);
			colDef.setToolTipText(Messages.ColumnFactory_altitude_up_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_DOWN = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeDown", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_altitude_down_label  + " (" + UI.UNIT_LABEL_ALTITUDE + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			colDef.setText(UI.UNIT_LABEL_ALTITUDE);
			colDef.setToolTipText(Messages.ColumnFactory_altitude_down_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory DEVICE_PROFILE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "deviceProfile", SWT.LEAD); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_profile_label);
			colDef.setText(Messages.ColumnFactory_profile);
			colDef.setToolTipText(Messages.ColumnFactory_profile_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
		
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					cell.setText(((TourData) cell.getElement()).getDeviceModeName());
				}
			});

			return colDef;
		};
	};
	
	public static final TableColumnFactory TIME_INTERVAL = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "timeInterval", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_time_interval_label);
			colDef.setText(Messages.ColumnFactory_time_interval);
			colDef.setToolTipText(Messages.ColumnFactory_time_interval_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					cell.setText(Integer.toString(((TourData) cell.getElement())
							.getDeviceTimeInterval()));
				}
			});
	
			return colDef;
		};
	};
	
	public static final TableColumnFactory DEVICE_NAME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "deviceName", SWT.LEAD); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_device_label);
			colDef.setText(Messages.ColumnFactory_device);
			colDef.setToolTipText(Messages.ColumnFactory_device_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					cell.setText(((TourData) cell.getElement()).getDeviceName());
				}
			});
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory IMPORT_FILE_PATH = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "importFilePath", SWT.LEAD); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_import_filepath_label);
			colDef.setText(Messages.ColumnFactory_import_filepath);
			colDef.setToolTipText(Messages.ColumnFactory_import_filepath_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					final String importRawDataFile = ((TourData) cell.getElement()).importRawDataFile;
					if (importRawDataFile != null) {
						cell.setText(new File(importRawDataFile).getParentFile().getPath());
					}
				}
			});
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory IMPORT_FILE_NAME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "importFileName", SWT.LEAD); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_import_filename_label);
			colDef.setText(Messages.ColumnFactory_import_filename);
			colDef.setToolTipText(Messages.ColumnFactory_import_filename_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					final String importRawDataFile = ((TourData) cell.getElement()).importRawDataFile;
					if (importRawDataFile != null) {
						cell.setText(new File(importRawDataFile).getName());
					}
				}
			});
	
			return colDef;
		};
	};
	
//	public static final TableColumnFactory ? = new TableColumnFactory() {
//		
//		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
//			
//			
//			return colDef;
//		};
//	};
	
	
	public abstract TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter);
}
