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

import net.tourbook.util.PixelConverter;

import org.eclipse.swt.SWT;

public abstract class TreeColumnFactory {

	public static final TreeColumnFactory DATE = new TreeColumnFactory() {

		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {

			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "date", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_date_label);
			colDef.setText(Messages.ColumnFactory_date);
			colDef.setToolTipText(Messages.ColumnFactory_date_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(16));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TITLE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourTitle", SWT.BEGINNING); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_tour_title);
			colDef.setText(Messages.ColumnFactory_tour_title);
			colDef.setToolTipText(Messages.ColumnFactory_tour_title_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(25));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory DISTANCE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "distance", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_distance_label);
			colDef.setText(Messages.ColumnFactory_distance);
			colDef.setToolTipText(Messages.ColumnFactory_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory RECORDING_TIME = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "recordingTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_recording_time_label);
			colDef.setText(Messages.ColumnFactory_recording_time);
			colDef.setToolTipText(Messages.ColumnFactory_recording_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory DRIVING_TIME = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "drivingTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_driving_time_label);
			colDef.setText(Messages.ColumnFactory_driving_time);
			colDef.setToolTipText(Messages.ColumnFactory_driving_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory ALTITUDE_UP = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "altitudeUp", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_altitude_up_label);
			colDef.setText(Messages.ColumnFactory_altitude_up);
			colDef.setToolTipText(Messages.ColumnFactory_altitude_up_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory DEVICE_DISTANCE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "deviceDistance", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_device_start_distance_label);
			colDef.setToolTipText(Messages.ColumnFactory_device_start_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_COUNTER = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourCounter", SWT.TRAIL); //$NON-NLS-1$
			colDef.setText(Messages.ColumnFactory_tour_numbers);
			colDef.setLabel(Messages.ColumnFactory_tour_numbers_lable);
			colDef.setToolTipText(Messages.ColumnFactory_tour_numbers_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(5));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_TYPE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourType", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_tour_type_label);
			colDef.setToolTipText(Messages.ColumnFactory_tour_type_tooltip);
			colDef.setWidth(18);

			return colDef;
		};
	};
	
	public static final TreeColumnFactory TIME_INTERVAL = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "timeInterval", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_time_interval_label);
			colDef.setText(Messages.ColumnFactory_time_interval);
			colDef.setToolTipText(Messages.ColumnFactory_time_interval_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory MAX_SPEED = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "maxSpeed", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_max_speed_label);
			colDef.setText(Messages.ColumnFactory_max_speed);
			colDef.setToolTipText(Messages.ColumnFactory_max_speed_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory AVG_SPEED = new TreeColumnFactory() {
	
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgSpeed", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_speed_label);
		colDef.setText(Messages.ColumnFactory_speed);
		colDef.setToolTipText(Messages.ColumnFactory_speed_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
		return colDef;
	};
};

	public static final TreeColumnFactory MAX_ALTITUDE = new TreeColumnFactory() {
	
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "maxAltitude", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_max_altitude_label);
		colDef.setText(Messages.ColumnFactory_max_altitude);
		colDef.setToolTipText(Messages.ColumnFactory_max_altitude_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
		return colDef;
	};
};

	public static final TreeColumnFactory MAX_PULSE = new TreeColumnFactory() {
	
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "maxPulse", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_max_pulse_label);
		colDef.setText(Messages.ColumnFactory_max_pulse);
		colDef.setToolTipText(Messages.ColumnFactory_max_pulse_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
		return colDef;
	};
};

	public static final TreeColumnFactory AVG_PULSE = new TreeColumnFactory() {
	
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgPulse", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_avg_pulse_label);
		colDef.setText(Messages.ColumnFactory_avg_pulse);
		colDef.setToolTipText(Messages.ColumnFactory_avg_pulse_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
		return colDef;
	};
};

	public static final TreeColumnFactory AVG_CADENCE = new TreeColumnFactory() {
	
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgCadence", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_avg_cadence_label);
		colDef.setText(Messages.ColumnFactory_avg_cadence);
		colDef.setToolTipText(Messages.ColumnFactory_avg_cadence_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(6));
		
		return colDef;
	};
};

	public static final TreeColumnFactory AVG_TEMPERATURE = new TreeColumnFactory() {
		
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgTemperature", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_avg_temperature_label);
			colDef.setText(Messages.ColumnFactory_avg_temperature);
			colDef.setToolTipText(Messages.ColumnFactory_avg_temperature_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(6));
			
			return colDef;
		};
	};
	
//public static final TreeColumnFactory ? = new TreeColumnFactory() {
//
//public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
//	
//	
//	
//	return colDef;
//};
//};

//public static final TreeColumnFactory ? = new TreeColumnFactory() {
//
//public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
//	
//	
//	
//	return colDef;
//};
//};

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
