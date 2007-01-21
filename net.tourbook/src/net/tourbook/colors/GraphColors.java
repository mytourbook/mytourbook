/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
import java.util.Comparator;
import java.util.List;


import net.tourbook.Messages;

import org.eclipse.swt.graphics.RGB;

public class GraphColors {

	public static final String	PREF_GRAPH_ALTITUDE		= "altitude"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_DISTANCE		= "distance"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_TIME			= "duration"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_SPEED		= "speed"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_HEARTBEAT	= "heartbeat"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_TEMPTERATURE	= "tempterature"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_POWER		= "power"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_GRADIEND		= "gradiend"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_ALTIMETER	= "altimeter"; //$NON-NLS-1$
	public static final String	PREF_GRAPH_CADENCE		= "cadence"; //$NON-NLS-1$

	public static final String	PREF_COLOR_BRIGHT		= "bright"; //$NON-NLS-1$
	public static final String	PREF_COLOR_DARK			= "dark"; //$NON-NLS-1$
	public static final String	PREF_COLOR_LINE			= "line"; //$NON-NLS-1$

	private ColorDefinition[]	graphDefinitionMap;

	public static String[][]	colorNames				= new String[][] {
			{ PREF_COLOR_BRIGHT, Messages.Graph_Pref_color_gradient_bright },
			{ PREF_COLOR_DARK, Messages.Graph_Pref_color_gradient_dark },
			{ PREF_COLOR_LINE, Messages.Graph_Pref_color_line }			};

	private ColorDefinition[]	fGraphDefinitions		= new ColorDefinition[] {

			new ColorDefinition(
					PREF_GRAPH_ALTITUDE,
					Messages.Graph_Label_Altitude,
					new RGB(255, 255, 255),
					new RGB(0, 255, 0),
					new RGB(45, 188, 45)),

			new ColorDefinition(PREF_GRAPH_DISTANCE, Messages.Graph_Pref_color_statistic_distance, new RGB(
					255,
					255,
					255), new RGB(239, 167, 16), new RGB(203, 141, 14)),

			new ColorDefinition(
					PREF_GRAPH_TIME,
					Messages.Graph_Pref_color_statistic_time,
					new RGB(255, 255, 255),
					new RGB(187, 187, 140),
					new RGB(170, 170, 127)),

			new ColorDefinition(
					PREF_GRAPH_SPEED,
					Messages.Graph_Label_Speed,
					new RGB(255, 255, 255),
					new RGB(0, 113, 229),
					new RGB(0, 94, 187)),

			new ColorDefinition(
					PREF_GRAPH_HEARTBEAT,
					Messages.Graph_Label_Heartbeat,
					new RGB(255, 255, 255),
					new RGB(253, 0, 0),
					new RGB(253, 0, 0)),

			new ColorDefinition(PREF_GRAPH_TEMPTERATURE, Messages.Graph_Label_Temperature, new RGB(
					255,
					255,
					255), new RGB(0, 195, 221), new RGB(0, 169, 193)),

			new ColorDefinition(
					PREF_GRAPH_POWER,
					Messages.Graph_Label_Power,
					new RGB(255, 255, 255),
					new RGB(83, 91, 172),
					new RGB(70, 75, 145)),

			new ColorDefinition(
					PREF_GRAPH_CADENCE,
					Messages.Graph_Label_Cadence,
					new RGB(255, 255, 255),
					new RGB(186, 0, 255),
					new RGB(144, 0, 193)),

			new ColorDefinition(
					PREF_GRAPH_ALTIMETER,
					Messages.Graph_Label_Altimeter,
					new RGB(255, 255, 255),
					new RGB(255, 180, 0),
					new RGB(244, 132, 0)),

			new ColorDefinition(
					PREF_GRAPH_GRADIEND,
					Messages.Graph_Label_Gradiend,
					new RGB(255, 255, 255),
					new RGB(255, 239, 0),
					new RGB(198, 188, 0))				};

	private static GraphColors	instance;

	public GraphColors() {}

	public static GraphColors getInstance() {
		if (instance == null) {
			instance = new GraphColors();
		}
		return instance;
	}

	public ColorDefinition[] getGraphDefinitionList() {

		ColorDefinition[] fGraphDefinitionList = graphDefinitionMap;
		if (fGraphDefinitionList == null) {

			List<ColorDefinition> list = new ArrayList<ColorDefinition>();

			Collections.addAll(list, fGraphDefinitions);
			Collections.sort(list, new Comparator<ColorDefinition>() {
				public int compare(ColorDefinition def1, ColorDefinition def2) {
					return def1.getVisibleName().compareTo(def2.getVisibleName());
				}
			});
			return fGraphDefinitionList = list.toArray(new ColorDefinition[list.size()]);
		}
		return fGraphDefinitionList;
	}

}
