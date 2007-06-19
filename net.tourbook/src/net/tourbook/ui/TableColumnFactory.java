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

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;

public abstract class TableColumnFactory {

	

	public static final TableColumnFactory DB_STATUS = new TableColumnFactory() {

		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {

			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "dbStatus", SWT.CENTER);
			colDef.setLabel(Messages.RawData_Column_db_status_label);
			colDef.setToolTipText(Messages.RawData_Column_db_status_tooltip);
			colDef.setWidth(20);

			return colDef;
		};
	};

	public static final TableColumnFactory TOUR_DATE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourdate", SWT.TRAIL);
			colDef.setText(Messages.RawData_Column_date);
			colDef.setLabel(Messages.RawData_Column_date_label);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(12));

			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_START_TIME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "startTime", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_time_label);
			colDef.setText(Messages.RawData_Column_time);
			colDef.setToolTipText(Messages.RawData_Column_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TYPE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourType", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_tour_type_label);
			colDef.setToolTipText(Messages.RawData_Column_tour_type_tooltip);
			colDef.setWidth(18);

			return colDef;
		};
	};
	
	public static final TableColumnFactory RECORDING_TIME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "recordingTime", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_recording_time_label);
			colDef.setText(Messages.RawData_Column_recording_time);
			colDef.setToolTipText(Messages.RawData_Column_recording_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory DRIVING_TIME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "drivingTime", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_driving_time_label);
			colDef.setText(Messages.RawData_Column_driving_time);
			colDef.setToolTipText(Messages.RawData_Column_driving_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
		};
	};
	
	public static final TableColumnFactory DISTANCE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "distance", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_distance_label);
			colDef.setText(Messages.RawData_Column_distance);
			colDef.setToolTipText(Messages.RawData_Column_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};
	
	public static final TableColumnFactory SPEED = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "speed", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_speed_label);
			colDef.setText(Messages.RawData_Column_speed);
			colDef.setToolTipText(Messages.RawData_Column_speed_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(9));

			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_UP = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeUp", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_altitude_up_label);
			colDef.setText(Messages.RawData_Column_altitude_up);
			colDef.setToolTipText(Messages.RawData_Column_altitude_up_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_DOWN = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeDown", SWT.TRAIL);
			colDef.setLabel("Altitude down (m)");
			colDef.setText("m");
			colDef.setToolTipText("Altitude down");
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory DEVICE_PROFILE = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "deviceProfile", SWT.LEAD);
			colDef.setLabel(Messages.RawData_Column_profile_label);
			colDef.setText(Messages.RawData_Column_profile);
			colDef.setToolTipText(Messages.RawData_Column_profile_tooltip);
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
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "timeInterval", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_time_interval_label);
			colDef.setText(Messages.RawData_Column_time_interval);
			colDef.setToolTipText(Messages.RawData_Column_time_interval_tooltip);
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
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "deviceName", SWT.LEAD);
			colDef.setLabel(Messages.RawData_Column_device_label);
			colDef.setText(Messages.RawData_Column_device);
			colDef.setToolTipText(Messages.RawData_Column_device_tooltip);
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
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "importFilePath", SWT.LEAD);
			colDef.setLabel(Messages.RawData_Column_import_filepath_label);
			colDef.setText(Messages.RawData_Column_import_filepath);
			colDef.setToolTipText(Messages.RawData_Column_import_filepath_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					cell.setText(new File(((TourData) cell.getElement()).importRawDataFile)
							.getParentFile()
							.getPath());
				}
			});
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory IMPORT_FILE_NAME = new TableColumnFactory() {
		
		public TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "importFileName", SWT.LEAD);
			colDef.setLabel(Messages.RawData_Column_import_filename_label);
			colDef.setText(Messages.RawData_Column_import_filename);
			colDef.setToolTipText(Messages.RawData_Column_import_filename_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					cell.setText(new File(((TourData) cell.getElement()).importRawDataFile).getName());
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
