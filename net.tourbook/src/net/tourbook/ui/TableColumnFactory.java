/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.data.TourData;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;

public abstract class TableColumnFactory {

	public static final TableColumnFactory ALTITUDE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitude", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_label);
			colDef.setColumnHeader(UI.UNIT_LABEL_ALTITUDE);
			colDef.setColumnUnit(UI.UNIT_LABEL_ALTITUDE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_DIFF_SEGMENT_COMPUTED = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final String unitLabel = UI.SYMBOL_DIFFERENCE_WITH_SPACE + UI.UNIT_LABEL_ALTITUDE + UI.SYMBOL_DOUBLE_HORIZONTAL;

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeComputedDiffSegment", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_difference_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_computed_difference_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_DIFF_SEGMENT_BORDER = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final String unitLabel = UI.SYMBOL_DIFFERENCE_WITH_SPACE + UI.UNIT_LABEL_ALTITUDE + UI.SYMBOL_DOUBLE_VERTICAL;

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeDiffSegBorder", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_difference_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_difference_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_DOWN_SUMMARIZED_BORDER = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeDown", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + "\\ " + UI.UNIT_LABEL_ALTITUDE; //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_down_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_DOWN_SUMMARIZED_COMPUTED = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + "\\ " + UI.UNIT_LABEL_ALTITUDE + UI.SYMBOL_DOUBLE_HORIZONTAL; //$NON-NLS-1$
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeDownSummarizedComputed", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_computed_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_down_computed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TableColumnFactory ALTITUDE_DOWN_H = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeDownH", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = "\\ " + UI.UNIT_LABEL_ALTITUDE + Messages.ColumnFactory_hour; //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_h_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_down_h_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_UP_SUMMARIZED_BORDER = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeUp", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + "/ " + UI.UNIT_LABEL_ALTITUDE;//$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_up_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_UP_SUMMARIZED_COMPUTED = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + "/ " + UI.UNIT_LABEL_ALTITUDE + UI.SYMBOL_DOUBLE_HORIZONTAL;//$NON-NLS-1$

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeUpSummarizedComputed", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_computed_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_up_computed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory ALTITUDE_UP_H = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "altitudeUpH", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = "/ " + UI.UNIT_LABEL_ALTITUDE + Messages.ColumnFactory_hour; //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_h_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_altitude_up_h_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory AVG_PACE = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "avgPace", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_avg_pace_label);
			colDef.setColumnHeader(UI.SYMBOL_AVERAGE_WITH_SPACE  + UI.UNIT_LABEL_PACE);
			colDef.setColumnUnit(UI.SYMBOL_AVERAGE_WITH_SPACE  + UI.UNIT_LABEL_PACE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_avg_pace_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory AVG_PACE_DIFFERENCE = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "avgPaceDiff", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_avg_pace_difference_label);
			colDef.setColumnHeader(UI.SYMBOL_DIFFERENCE_WITH_SPACE  + UI.UNIT_LABEL_PACE);
			colDef.setColumnUnit(UI.SYMBOL_DIFFERENCE_WITH_SPACE  + UI.UNIT_LABEL_PACE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_avg_pace_difference_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory AVG_PULSE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "avgPulse", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_avg_pulse_label);
			colDef.setColumnHeader(UI.SYMBOL_AVERAGE_WITH_SPACE  + Messages.ColumnFactory_pulse);
			colDef.setColumnUnit(UI.SYMBOL_AVERAGE_WITH_SPACE  + Messages.ColumnFactory_pulse);
			colDef.setColumnToolTipText(Messages.ColumnFactory_avg_pulse_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory AVG_PULSE_DIFFERENCE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "avgPulseDiff", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_avg_pulse_difference_label);
			colDef.setColumnHeader(Messages.ColumnFactory_pulse_difference);
			colDef.setColumnUnit( Messages.ColumnFactory_pulse_difference);
			colDef.setColumnToolTipText(Messages.ColumnFactory_avg_pulse_difference_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TableColumnFactory AVG_SPEED = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "avgSpeed", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_avg_speed_label);
			colDef.setColumnHeader(UI.SYMBOL_AVERAGE_WITH_SPACE  + UI.UNIT_LABEL_SPEED);
			colDef.setColumnUnit(UI.SYMBOL_AVERAGE_WITH_SPACE  + UI.UNIT_LABEL_SPEED);
			colDef.setColumnToolTipText(Messages.ColumnFactory_avg_speed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory BREAK_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "breakTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_BreakTime_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_BreakTime_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_BreakTime_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(3));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory CADENCE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "cadence", SWT.TRAIL); //$NON-NLS-1$
	
			colDef.setColumnLabel(Messages.ColumnFactory_cadence_label);
			colDef.setColumnHeader(Messages.ColumnFactory_cadence);
			colDef.setColumnUnit(Messages.ColumnFactory_cadence);
			colDef.setColumnToolTipText(Messages.ColumnFactory_cadence_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};

	public static final TableColumnFactory CALORIES = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "calories", SWT.TRAIL); //$NON-NLS-1$
	
			colDef.setColumnLabel(Messages.ColumnFactory_calories_label);
			colDef.setColumnHeader(Messages.ColumnFactory_calories);
			colDef.setColumnUnit(Messages.ColumnFactory_calories);
			colDef.setColumnToolTipText(Messages.ColumnFactory_calories_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};

	public static final TableColumnFactory CLOUDS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "weatherClouds", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_clouds_label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_clouds_tooltip);

			colDef.setDefaultColumnWidth(18);
			
			return colDef;
		};
	};

	public static final TableColumnFactory DB_STATUS = new TableColumnFactory() {

		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "dbStatus", SWT.CENTER); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_db_status_label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_db_status_tooltip);
			colDef.setDefaultColumnWidth(20);

			return colDef;
		};
	};

	public static final TableColumnFactory DEVICE_PROFILE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "deviceProfile", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_profile_label);
			colDef.setColumnHeader(Messages.ColumnFactory_profile);
			colDef.setColumnToolTipText(Messages.ColumnFactory_profile_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((TourData) cell.getElement()).getDeviceModeName());
				}
			});
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory DEVICE_NAME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "deviceName", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_device_label);
			colDef.setColumnHeader(Messages.ColumnFactory_device);
			colDef.setColumnToolTipText(Messages.ColumnFactory_device_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(((TourData) cell.getElement()).getDeviceName());
				}
			});
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory DISTANCE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "distance", SWT.TRAIL); //$NON-NLS-1$
																			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(11);
		
			colDef.setColumnLabel(Messages.ColumnFactory_distance_label);
			colDef.setColumnHeader(UI.UNIT_LABEL_DISTANCE);
			colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_distance_tooltip);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

			return colDef;
		};
	};
	
	public static final TableColumnFactory DISTANCE_TOTAL = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "distanceTotal", SWT.TRAIL); //$NON-NLS-1$
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(11);
			final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + UI.UNIT_LABEL_DISTANCE;

			colDef.setColumnLabel(Messages.ColumnFactory_distanceTotal_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_distanceTotal_tooltip);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};

	public static final TableColumnFactory DRIVING_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "drivingTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_driving_time_label);
			colDef.setColumnHeader(Messages.ColumnFactory_driving_time);
			colDef.setColumnUnit(Messages.ColumnFactory_driving_time);
			colDef.setColumnToolTipText(Messages.ColumnFactory_driving_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory FIRST_COLUMN = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "firstColumn", SWT.LEAD); //$NON-NLS-1$
	
			colDef.setDefaultColumnWidth(0);
			
			return colDef;
		};
	};

	public static final TableColumnFactory GRADIENT = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "gradient", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_gradient_label);
			colDef.setColumnHeader(Messages.ColumnFactory_gradient);
			colDef.setColumnUnit(Messages.ColumnFactory_gradient);
			colDef.setColumnToolTipText(Messages.ColumnFactory_gradient_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};

	public static final TableColumnFactory IMPORT_FILE_PATH = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "importFilePath", SWT.LEAD); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_import_filepath_label);
			colDef.setColumnHeader(Messages.ColumnFactory_import_filepath);
			colDef.setColumnToolTipText(Messages.ColumnFactory_import_filepath_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
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
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "importFileName", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_import_filename_label);
			colDef.setColumnHeader(Messages.ColumnFactory_import_filename);
			colDef.setColumnToolTipText(Messages.ColumnFactory_import_filename_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final String importRawDataFile = ((TourData) cell.getElement()).importRawDataFile;
					if (importRawDataFile != null) {
						cell.setText(new File(importRawDataFile).getName());
					}
				}
			});
	
			return colDef;
		};
	};

	public static final TableColumnFactory ID = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "id", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Id_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_Id_Label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_Id_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TableColumnFactory LATITUDE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "latitude", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_latitude_label);
			colDef.setColumnHeader(Messages.ColumnFactory_latitude);
			colDef.setColumnToolTipText(Messages.ColumnFactory_latitude_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory LONGITUDE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "longitude", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_longitude_label);
			colDef.setColumnHeader(Messages.ColumnFactory_longitude);
			colDef.setColumnToolTipText(Messages.ColumnFactory_longitude_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));
			
			return colDef;
		};
	};

	
	public static final TableColumnFactory MARKER = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "marker", SWT.LEAD); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_marker_label);
			colDef.setColumnHeader(Messages.ColumnFactory_marker_label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_marker_label_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
			colDef.setColumnWeightData(new ColumnWeightData(100, true));
			
			return colDef;
		};
	};

	public static final TableColumnFactory NUMBER_OF_GPS_PHOTOS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "numberOfGPSPhotos", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_NumberOfGPSPhotos_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_NumberOfGPSPhotos_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_NumberOfGPSPhotos_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory NUMBER_OF_NO_GPS_PHOTOS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "numberOfNoGPSPhotos", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_NumberOfNoGPSPhotos_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_NumberOfNoGPSPhotos_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_NumberOfNoGPSPhotos_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};

	public static final TableColumnFactory NUMBER_OF_PHOTOS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "numberOfTourPhotos", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_NumberOfTourPhotos_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_NumberOfTourPhotos_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_NumberOfTourPhotos_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};

	public static final TableColumnFactory PACE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "pace", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_pace_label);
			colDef.setColumnHeader(UI.UNIT_LABEL_PACE);
			colDef.setColumnUnit(UI.UNIT_LABEL_PACE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_pace_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};

	public static final TableColumnFactory PAUSED_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "pausedTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_paused_time_label);
			colDef.setColumnHeader(Messages.ColumnFactory_paused_time);
			colDef.setColumnUnit(Messages.ColumnFactory_paused_time);
			colDef.setColumnToolTipText(Messages.ColumnFactory_paused_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory PHOTO_TIME_ADJUSTMENT = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "photoTimeAdjustment", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_PhotoTimeAdjustment_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_PhotoTimeAdjustment_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_PhotoTimeAdjustment_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			 
			return colDef;
		};
	};
	
	public static final TableColumnFactory POWER = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "power", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_power_label);
			colDef.setColumnHeader(Messages.ColumnFactory_power);
			colDef.setColumnUnit(Messages.ColumnFactory_power);
			colDef.setColumnToolTipText(Messages.ColumnFactory_power_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory PULSE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "pulse", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_pulse_label);
			colDef.setColumnHeader(Messages.ColumnFactory_pulse);
			colDef.setColumnUnit(Messages.ColumnFactory_pulse);
			colDef.setColumnToolTipText(Messages.ColumnFactory_pulse_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory RECORDING_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "recordingTime", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_recording_time_label);
			colDef.setColumnHeader(Messages.ColumnFactory_recording_time);
			colDef.setColumnUnit(Messages.ColumnFactory_recording_time);
			colDef.setColumnToolTipText(Messages.ColumnFactory_recording_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
		
			return colDef;
		};
	};
	
	public static final TableColumnFactory RECORDING_TIME_TOTAL = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "recordingTimeTotal", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + Messages.ColumnFactory_recording_time;

			colDef.setColumnLabel(Messages.ColumnFactory_recording_timeTotal_label);
			colDef.setColumnHeader(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnToolTipText(Messages.ColumnFactory_recording_timeTotal_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory SPEED = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "speed", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_speed_label);
			colDef.setColumnHeader(UI.UNIT_LABEL_SPEED);
			colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
			colDef.setColumnToolTipText(Messages.ColumnFactory_speed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TableColumnFactory SPEED_DIFF = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final String header = Messages.ColumnFactory_Diff_Header
					+ UI.SPACE
					+ UI.UNIT_LABEL_SPEED;
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "speedDiff", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_SpeedDiff_Label);
			colDef.setColumnHeader(header);
			colDef.setColumnUnit(header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_SpeedDiff_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TEMPERATURE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "temperature", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_temperature_label);
			colDef.setColumnHeader(UI.UNIT_LABEL_TEMPERATURE);
			colDef.setColumnUnit(UI.UNIT_LABEL_TEMPERATURE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_temperature_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TIME_INTERVAL = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "timeInterval", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_time_interval_label);
			colDef.setColumnHeader(Messages.ColumnFactory_time_interval);
			colDef.setColumnUnit(Messages.ColumnFactory_time_interval);
			colDef.setColumnToolTipText(Messages.ColumnFactory_time_interval_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			colDef.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					cell.setText(Integer.toString(((TourData) cell.getElement())
							.getDeviceTimeInterval()));
				}
			});
	
			return colDef;
		};
	};

	public static final TableColumnFactory	TOUR_CAMERA	= new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourCamera", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourCamera_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourCamera_Label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourCamera_Label_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_DATE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourdate", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnHeader(Messages.ColumnFactory_date);
			colDef.setColumnLabel(Messages.ColumnFactory_date_label);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

			return colDef;
		};
	};
	
	public static final TableColumnFactory	TOUR_MARKERS	= new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourMarkers", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_tour_marker_label);
			colDef.setColumnHeader(Messages.ColumnFactory_tour_marker_header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_tour_marker_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
			
			return colDef;
		};
	};

	public static final TableColumnFactory TOUR_DURATION_TIME = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "TourDurationTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourDurationTime_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourDurationTime_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourDurationTime_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_END_DATE = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "TourEndDate", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourEndDate_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourEndDate_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourEndDate_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_END_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "TourEndTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourEndTime_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourEndTime_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourEndTime_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(13));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_START_DATE = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "TourStartDate", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourStartDate_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourStartDate_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourStartDate_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_START_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "TourStartTime", SWT.TRAIL); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_TourStartTime_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourStartTime_Header);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourStartTime_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(13));

			return colDef;
		};
	};
	
	public static final TableColumnFactory	TOUR_TAGS	= new TableColumnFactory() {

		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTags", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_tour_tag_label);
			colDef.setColumnHeader(Messages.ColumnFactory_tour_tag_label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_tour_tag_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));


			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTime", SWT.TRAIL); //$NON-NLS-1$
	
			colDef.setColumnLabel(Messages.ColumnFactory_tour_time_label);
			colDef.setColumnHeader(Messages.ColumnFactory_tour_time);
			colDef.setColumnUnit(Messages.ColumnFactory_tour_time);
			colDef.setColumnToolTipText(Messages.ColumnFactory_tour_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TIME_DIFF = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTimeDiff", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourTimeDiff_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_TourTimeDiff_Header);
			colDef.setColumnUnit(Messages.ColumnFactory_tour_time);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourTimeDiff_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory DISTANCE_DIFF = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final String header = Messages.ColumnFactory_Diff_Header
					+ UI.SPACE
					+ UI.UNIT_LABEL_DISTANCE;

			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourDistanceDiff", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_TourDistanceDiff_Label);
			colDef.setColumnHeader(header);
			colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_TourDistanceDiff_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
			
			return colDef;
		};
	};

	public static final TableColumnFactory TOUR_TIME_HH_MM_SS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(12);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTimeHHMMSS", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_tour_time_label_hhmmss);
			colDef.setColumnHeader(Messages.ColumnFactory_tour_time_label_hhmmss);
			colDef.setColumnUnit(Messages.ColumnFactory_tour_time_hhmmss);
			colDef.setColumnToolTipText(Messages.ColumnFactory_tour_time_tooltip_hhmmss);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

			return colDef;
		};
	};

	public static final TableColumnFactory TOUR_TIME_OF_DAY_HH_MM_SS = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(12);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourDayTimeHHMMSS", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Tour_DayTime);
			colDef.setColumnHeader(Messages.ColumnFactory_Tour_DayTime);
			colDef.setColumnUnit(Messages.ColumnFactory_tour_time_hhmmss);
			colDef.setColumnToolTipText(Messages.ColumnFactory_Tour_DayTime_Tooltip);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TITLE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTitle", SWT.LEAD); //$NON-NLS-1$
		
			colDef.setColumnLabel(Messages.ColumnFactory_tour_title_label);
			colDef.setColumnHeader(Messages.ColumnFactory_tour_title);
			colDef.setColumnToolTipText(Messages.ColumnFactory_tour_title_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(25));

			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TYPE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourType", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_tour_type_label);
			colDef.setColumnToolTipText(Messages.ColumnFactory_tour_type_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory TOUR_TYPE_TEXT = new TableColumnFactory() {
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "tourTypeText", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnHeader(Messages.ColumnFactory_TourTypeText_Header);
			colDef.setColumnLabel(Messages.ColumnFactory_TourTypeText_Label);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));
			
			return colDef;
		};
	};

	public static final TableColumnFactory SERIE_START_END_INDEX = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "serieStartEndIndex", SWT.LEAD); //$NON-NLS-1$
	
			colDef.setColumnLabel(Messages.ColumnFactory_SerieStartEndIndex_Label);
			colDef.setColumnHeader(Messages.ColumnFactory_SerieStartEndIndex);
			colDef.setColumnToolTipText(Messages.ColumnFactory_SerieStartEndIndex_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
			
			return colDef;
		};
	};

	public static final TableColumnFactory SEQUENCE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "sequence", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_sequence_label);
			colDef.setColumnHeader(Messages.ColumnFactory_sequence);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};

	/*
	 * Waypoint columns
	 */
	
	public static final TableColumnFactory WAYPOINT_DATE = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(15);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpDate", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Date);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Date);
			colDef.setColumnUnit(Messages.ColumnFactory_Waypoint_Date_Unit);
			colDef.setColumnToolTipText(Messages.ColumnFactory_Waypoint_Date_Tooltip);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory WAYPOINT_NAME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpName", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Name);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Name);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory WAYPOINT_DESCRIPTION = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpDescription", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Description);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Description);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory WAYPOINT_COMMENT = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpComment", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Comment);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Comment);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory	WAYPOINT_ALTITUDE				= new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(10);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpAltitude", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Altitude_Label);
			colDef.setColumnHeader(UI.UNIT_LABEL_ALTITUDE);
			colDef.setColumnUnit(UI.UNIT_LABEL_ALTITUDE);
			colDef.setColumnToolTipText(Messages.ColumnFactory_Waypoint_Altitude_Label);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory	WAYPOINT_CATEGORY				= new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpCategory", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Category);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Category);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory	WAYPOINT_SYMBOL					= new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpSymbol", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Symbol);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Symbol);
			colDef.setDefaultColumnWidth(pixelWidth);
			colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
			
			return colDef;
		};
	};
	
	public static final TableColumnFactory WAYPOINT_TIME = new TableColumnFactory() {
		
		@Override
		public ColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(15);
			final ColumnDefinition colDef = new TableColumnDefinition(columnManager, "wpTime", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Time);
			colDef.setColumnHeader(Messages.ColumnFactory_Waypoint_Time);
			colDef.setColumnUnit(Messages.ColumnFactory_Waypoint_Time_Unit);
			colDef.setColumnToolTipText(Messages.ColumnFactory_Waypoint_Time_Tooltip);
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
