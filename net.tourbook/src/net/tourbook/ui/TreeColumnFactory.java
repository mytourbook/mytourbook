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

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;

public abstract class TreeColumnFactory {

	public static final TreeColumnFactory DATE = new TreeColumnFactory() {

		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {

			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "date", SWT.TRAIL);
			
			colDef.setLabel("Date");
			colDef.setText(Messages.TourBook_Column_date);
			colDef.setToolTipText("Tourdate");
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(16));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TITLE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourTitle", SWT.BEGINNING);
			
			colDef.setLabel(Messages.TourBook_Column_tour_title);
			colDef.setText(Messages.TourBook_Column_tour_title);
			colDef.setToolTipText(Messages.TourBook_Column_tour_title_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(25));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory DISTANCE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "distance", SWT.TRAIL);
			
			colDef.setLabel("Distance (km)");
			colDef.setText(Messages.TourBook_Column_distance);
			colDef.setToolTipText(Messages.TourBook_Column_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory RECORDING_TIME = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "recordingTime", SWT.TRAIL);
			colDef.setLabel("Recording Time (h)");
			colDef.setText(Messages.TourBook_Column_recording_time);
			colDef.setToolTipText(Messages.TourBook_Column_recording_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory DRIVING_TIME = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "drivingTime", SWT.TRAIL);
			colDef.setLabel("Driving Time (h)");
			colDef.setText(Messages.TourBook_Column_driving_time);
			colDef.setToolTipText(Messages.TourBook_Column_driving_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory ALTITUDE_UP = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "altitudeUp", SWT.TRAIL);
			colDef.setLabel("Altitude Up (m)");
			colDef.setText(Messages.TourBook_Column_altitude_up);
			colDef.setToolTipText(Messages.TourBook_Column_altitude_up_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory DEVICE_DISTANCE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "deviceDistance", SWT.TRAIL);
			colDef.setLabel("Device start distance");
			colDef.setToolTipText("Device start distance");
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_COUNTER = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourCounter", SWT.TRAIL);
			colDef.setText("#");
			colDef.setLabel("Number of tours");
			colDef.setToolTipText("Number of tours");
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(5));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_TYPE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourType", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_tour_type_label);
			colDef.setToolTipText(Messages.RawData_Column_tour_type_tooltip);
			colDef.setWidth(18);

			return colDef;
		};
	};
	
	public static final TreeColumnFactory TIME_INTERVAL = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "timeInterval", SWT.TRAIL);
			colDef.setLabel(Messages.RawData_Column_time_interval_label);
			colDef.setText(Messages.RawData_Column_time_interval);
			colDef.setToolTipText(Messages.RawData_Column_time_interval_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};
	
//	public static final TreeColumnFactory ? = new TreeColumnFactory() {
//		
//		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
//			
//			
//			
//			return colDef;
//		};
//	};
	
//	public static final TreeColumnFactory ? = new TreeColumnFactory() {
//		
//		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
//			
//			
//			
//			return colDef;
//		};
//	};
	
//	public static final TreeColumnFactory ? = new TreeColumnFactory() {
//		
//		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
//			
//			
//			
//			return colDef;
//		};
//	};
	
//    protected TreeColumnFactory() {}
    
	public abstract TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter);
}
