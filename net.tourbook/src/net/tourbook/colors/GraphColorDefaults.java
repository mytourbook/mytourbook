/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.mapping.LegendColor;

import org.eclipse.swt.graphics.RGB;

public class GraphColorDefaults {

	public static final String			PREF_GRAPH_ALTITUDE		= "altitude";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_DISTANCE		= "distance";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_TIME			= "duration";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_SPEED		= "speed";				//$NON-NLS-1$
	public static final String			PREF_GRAPH_HEARTBEAT	= "heartbeat";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_TEMPTERATURE	= "tempterature";		//$NON-NLS-1$
	public static final String			PREF_GRAPH_POWER		= "power";				//$NON-NLS-1$
	public static final String			PREF_GRAPH_GRADIEND		= "gradiend";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_ALTIMETER	= "altimeter";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_CADENCE		= "cadence";			//$NON-NLS-1$
	public static final String			PREF_GRAPH_TOUR_COMPARE	= "tourCompare";		//$NON-NLS-1$
	public static final String			PREF_GRAPH_PACE			= "pace";				//$NON-NLS-1$

	public static final String			PREF_COLOR_BRIGHT		= "bright";			//$NON-NLS-1$
	public static final String			PREF_COLOR_DARK			= "dark";				//$NON-NLS-1$ 
	public static final String			PREF_COLOR_LINE			= "line";				//$NON-NLS-1$
	public static final String			PREF_COLOR_MAPPING		= "mapping";			//$NON-NLS-1$

	private ColorDefinition[]			fGraphDefinitionMap;

	public static String[][]			colorNames				= new String[][] {
			{ PREF_COLOR_BRIGHT, Messages.Graph_Pref_color_gradient_bright },
			{ PREF_COLOR_DARK, Messages.Graph_Pref_color_gradient_dark },
			{ PREF_COLOR_LINE, Messages.Graph_Pref_color_line },
			{ PREF_COLOR_MAPPING, Messages.Graph_Pref_color_mapping } };

	private static LegendColor			fLegendColorAltitude	= new LegendColor();
	private static LegendColor			fLegendColorGradient	= new LegendColor();
	private static LegendColor			fLegendColorPace		= new LegendColor();
	private static LegendColor			fLegendColorPulse		= new LegendColor();
	private static LegendColor			fLegendColorSpeed		= new LegendColor();

	private static ColorDefinition[]	fGraphDefinitions		= new ColorDefinition[] {

			new ColorDefinition(PREF_GRAPH_ALTITUDE,//
					Messages.Graph_Label_Altitude,
					new RGB(255, 255, 255),
					new RGB(0, 255, 0),
					new RGB(45, 188, 45),
					fLegendColorAltitude),

			new ColorDefinition(PREF_GRAPH_HEARTBEAT,
					Messages.Graph_Label_Heartbeat,
					new RGB(255, 255, 255),
					new RGB(253, 0, 0),
					new RGB(253, 0, 0),
					fLegendColorPulse),

			new ColorDefinition(PREF_GRAPH_SPEED,//
					Messages.Graph_Label_Speed,
					new RGB(255, 255, 255),
					new RGB(0, 135, 211),
					new RGB(0, 132, 210),
					fLegendColorSpeed),

			new ColorDefinition(PREF_GRAPH_PACE,//
					Messages.Graph_Label_Pace,
					new RGB(255, 255, 255),
					new RGB(0, 47, 211),
					new RGB(0, 43, 210),
					fLegendColorPace),

			new ColorDefinition(PREF_GRAPH_POWER,//
					Messages.Graph_Label_Power,
					new RGB(255, 255, 255),
					new RGB(240, 0, 150),
					new RGB(240, 0, 150),
					null),

			new ColorDefinition(PREF_GRAPH_TEMPTERATURE,
					Messages.Graph_Label_Temperature,
					new RGB(255, 255, 255),
					new RGB(0, 217, 240),
					new RGB(0, 216, 240),
					null),

			new ColorDefinition(PREF_GRAPH_GRADIEND,
					Messages.Graph_Label_Gradiend,
					new RGB(255, 255, 255),
					new RGB(249, 231, 0),
					new RGB(236, 206, 0),
					fLegendColorGradient),

			new ColorDefinition(PREF_GRAPH_ALTIMETER,
					Messages.Graph_Label_Altimeter,
					new RGB(255, 255, 255),
					new RGB(255, 180, 0),
					new RGB(249, 174, 0),
					null),

			new ColorDefinition(PREF_GRAPH_CADENCE,//
					Messages.Graph_Label_Cadence,
					new RGB(255, 255, 255),
					new RGB(228, 106, 16),
					new RGB(228, 106, 16),
					null),

			new ColorDefinition(PREF_GRAPH_TOUR_COMPARE,
					Messages.Graph_Label_Tour_Compare,
					new RGB(255, 255, 255),
					new RGB(255, 140, 26),
					new RGB(242, 135, 22),
					null),

			new ColorDefinition(PREF_GRAPH_DISTANCE,//
					Messages.Graph_Pref_color_statistic_distance,
					new RGB(255, 255, 255),
					new RGB(239, 167, 16),
					new RGB(203, 141, 14),
					null),
			new ColorDefinition(PREF_GRAPH_TIME,//
					Messages.Graph_Pref_color_statistic_time,
					new RGB(255, 255, 255),
					new RGB(187, 187, 140),
					new RGB(170, 170, 127),
					null)										};

	private static GraphColorDefaults	instance;

	public GraphColorDefaults() {}

	public static GraphColorDefaults getInstance() {
		if (instance == null) {
			instance = new GraphColorDefaults();
		}
		return instance;
	}

	public ColorDefinition[] getGraphDefinitionList() {

		if (fGraphDefinitionMap != null) {
			return fGraphDefinitionMap;
		}

		List<ColorDefinition> list = new ArrayList<ColorDefinition>();

		// sort list by name
		Collections.addAll(list, fGraphDefinitions);
//		Collections.sort(list, new Comparator<ColorDefinition>() {
//			public int compare(ColorDefinition def1, ColorDefinition def2) {
//				return def1.getVisibleName().compareTo(def2.getVisibleName());
//			}
//		});

		return fGraphDefinitionMap = list.toArray(new ColorDefinition[list.size()]);
	}

}
