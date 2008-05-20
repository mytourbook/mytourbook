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

		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {

			TreeColumnDefinition colDef = new TreeColumnDefinition("date", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_date_label);
			colDef.setText(Messages.ColumnFactory_date);
			colDef.setToolTipText(Messages.ColumnFactory_date_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(16));
			
			columnManager.addColumn(colDef); 

			return colDef;
		};
	};

	public static final TreeColumnFactory TAG = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("tag", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_tag_label);
			colDef.setText(Messages.ColumnFactory_tag);
			colDef.setToolTipText(Messages.ColumnFactory_tag_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			columnManager.addColumn(colDef); 
			
			return colDef;
		};
	};

	public static final TreeColumnFactory TITLE = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("tourTitle", SWT.BEGINNING); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_tour_title);
			colDef.setText(Messages.ColumnFactory_tour_title);
			colDef.setToolTipText(Messages.ColumnFactory_tour_title_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(25));
			
			columnManager.addColumn(colDef); 

			return colDef;
		};
	};

	public static final TreeColumnFactory DISTANCE = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("distance", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setLabel(Messages.ColumnFactory_distance_label + " (" + UI.UNIT_LABEL_DISTANCE + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			colDef.setText(UI.UNIT_LABEL_DISTANCE);
			colDef.setToolTipText(Messages.ColumnFactory_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};

	public static final TreeColumnFactory RECORDING_TIME = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("recordingTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_recording_time_label);
			colDef.setText(Messages.ColumnFactory_recording_time);
			colDef.setToolTipText(Messages.ColumnFactory_recording_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};
	
	public static final TreeColumnFactory DRIVING_TIME = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("drivingTime", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_driving_time_label);
			colDef.setText(Messages.ColumnFactory_driving_time);
			colDef.setToolTipText(Messages.ColumnFactory_driving_time_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};
	
	public static final TreeColumnFactory ALTITUDE_UP = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("altitudeUp", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_altitude_up_label  + " (" + UI.UNIT_LABEL_ALTITUDE + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			colDef.setText(UI.UNIT_LABEL_ALTITUDE);
			colDef.setToolTipText(Messages.ColumnFactory_altitude_up_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));


			columnManager.addColumn(colDef); 

			return colDef;
		};
	};
	
	public static final TreeColumnFactory DEVICE_DISTANCE = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("deviceDistance", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_device_start_distance_label);
			colDef.setToolTipText(Messages.ColumnFactory_device_start_distance_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_COUNTER = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("tourCounter", SWT.TRAIL); //$NON-NLS-1$
			colDef.setText(Messages.ColumnFactory_tour_numbers);
			colDef.setLabel(Messages.ColumnFactory_tour_numbers_lable);
			colDef.setToolTipText(Messages.ColumnFactory_tour_numbers_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(5));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_TYPE = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("tourType", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_tour_type_label);
			colDef.setToolTipText(Messages.ColumnFactory_tour_type_tooltip);
			colDef.setWidth(18);

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};
	
	public static final TreeColumnFactory TIME_INTERVAL = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			
			
			TreeColumnDefinition colDef = new TreeColumnDefinition("timeInterval", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_time_interval_label);
			colDef.setText(Messages.ColumnFactory_time_interval);
			colDef.setToolTipText(Messages.ColumnFactory_time_interval_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};
	
	public static final TreeColumnFactory MAX_SPEED = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			TreeColumnDefinition colDef = new TreeColumnDefinition("maxSpeed", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_max_speed_label);
			colDef.setText("^" + UI.UNIT_LABEL_SPEED); //$NON-NLS-1$
			colDef.setToolTipText(Messages.ColumnFactory_max_speed_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(9));

			columnManager.addColumn(colDef); 

			return colDef;
		};
	};
	
	public static final TreeColumnFactory AVG_SPEED = new TreeColumnFactory() {
	
	@Override
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition("avgSpeed", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_speed_label);
		colDef.setText(UI.UNIT_LABEL_SPEED);
		colDef.setToolTipText(Messages.ColumnFactory_speed_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		columnManager.addColumn(colDef); 

		return colDef;
	};
};

	public static final TreeColumnFactory MAX_ALTITUDE = new TreeColumnFactory() {
	
	@Override
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition("maxAltitude", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_max_altitude_label);
		colDef.setText("^" + UI.UNIT_LABEL_ALTITUDE); //$NON-NLS-1$
		colDef.setToolTipText(Messages.ColumnFactory_max_altitude_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		columnManager.addColumn(colDef); 

		return colDef;
	};
};

	public static final TreeColumnFactory MAX_PULSE = new TreeColumnFactory() {
	
	@Override
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition("maxPulse", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_max_pulse_label);
		colDef.setText(Messages.ColumnFactory_max_pulse);
		colDef.setToolTipText(Messages.ColumnFactory_max_pulse_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		columnManager.addColumn(colDef); 

		return colDef;
	};
};

	public static final TreeColumnFactory AVG_PULSE = new TreeColumnFactory() {
	
	@Override
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition("avgPulse", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_avg_pulse_label);
		colDef.setText(Messages.ColumnFactory_avg_pulse);
		colDef.setToolTipText(Messages.ColumnFactory_avg_pulse_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		columnManager.addColumn(colDef); 

		return colDef;
	};
};

	public static final TreeColumnFactory AVG_CADENCE = new TreeColumnFactory() {
	
	@Override
	public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
		TreeColumnDefinition colDef = new TreeColumnDefinition("avgCadence", SWT.TRAIL); //$NON-NLS-1$
		colDef.setLabel(Messages.ColumnFactory_avg_cadence_label);
		colDef.setText(Messages.ColumnFactory_avg_cadence);
		colDef.setToolTipText(Messages.ColumnFactory_avg_cadence_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(6));

		columnManager.addColumn(colDef); 

		return colDef;
	};
};

	public static final TreeColumnFactory AVG_TEMPERATURE = new TreeColumnFactory() {
		
		@Override
		public TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter) {
			TreeColumnDefinition colDef = new TreeColumnDefinition("avgTemperature", SWT.TRAIL); //$NON-NLS-1$
			colDef.setLabel(Messages.ColumnFactory_avg_temperature_label + " (" + UI.UNIT_LABEL_TEMPERATURE + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			colDef.setText(UI.UNIT_LABEL_TEMPERATURE);
			colDef.setToolTipText(Messages.ColumnFactory_avg_temperature_tooltip);
			colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(6));

			columnManager.addColumn(colDef); 

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
