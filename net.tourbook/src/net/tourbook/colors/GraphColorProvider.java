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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.Messages;
import net.tourbook.mapping.LegendColor;
import net.tourbook.mapping.ValueColor;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GraphColorProvider {

	public static final String				PREF_GRAPH_ALTITUDE			= "altitude";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_DISTANCE			= "distance";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_TIME				= "duration";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_SPEED			= "speed";				//$NON-NLS-1$
	public static final String				PREF_GRAPH_HEARTBEAT		= "heartbeat";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_TEMPTERATURE		= "tempterature";		//$NON-NLS-1$
	public static final String				PREF_GRAPH_POWER			= "power";				//$NON-NLS-1$
	public static final String				PREF_GRAPH_GRADIEND			= "gradiend";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_ALTIMETER		= "altimeter";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_CADENCE			= "cadence";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_TOUR_COMPARE		= "tourCompare";		//$NON-NLS-1$
	public static final String				PREF_GRAPH_PACE				= "pace";				//$NON-NLS-1$

	public static final String				PREF_COLOR_BRIGHT			= "bright";			//$NON-NLS-1$
	public static final String				PREF_COLOR_DARK				= "dark";				//$NON-NLS-1$ 
	public static final String				PREF_COLOR_LINE				= "line";				//$NON-NLS-1$
	public static final String				PREF_COLOR_MAPPING			= "mapping";			//$NON-NLS-1$

	private static final String				MEMENTO_LEGEND_COLOR_FILE	= "legendcolor.xml";	//$NON-NLS-1$

	private static final String				MEMENTO_ROOT				= "legendcolorlist";
	private static final String				MEMENTO_CHILD_LEGEND_COLOR	= "legendcolor";
	private static final String				MEMENTO_CHILD_VALUE_COLOR	= "valuecolor";
	private static final String				MEMENTO_CHILD_BRIGHTNESS	= "brightness";

	private static final String				TAG_LEGEND_COLOR_PREF_NAME	= "prefname";
	private static final String				TAG_VALUE_COLOR_VALUE		= "value";
	private static final String				TAG_VALUE_COLOR_RED			= "red";
	private static final String				TAG_VALUE_COLOR_GREEN		= "green";
	private static final String				TAG_VALUE_COLOR_BLUE		= "blue";

	private static final String				TAG_BRIGHTNESS_MIN			= "min";
	private static final String				TAG_BRIGHTNESS_MIN_FACTOR	= "minFactor";
	private static final String				TAG_BRIGHTNESS_MAX			= "max";
	private static final String				TAG_BRIGHTNESS_MAX_FACTOR	= "maxFactor";

	public static String[][]				colorNames					= new String[][] {
			{ PREF_COLOR_BRIGHT, Messages.Graph_Pref_color_gradient_bright },
			{ PREF_COLOR_DARK, Messages.Graph_Pref_color_gradient_dark },
			{ PREF_COLOR_LINE, Messages.Graph_Pref_color_line },
			{ PREF_COLOR_MAPPING, Messages.Graph_Pref_color_mapping }	};

	private static final LegendColor		LEGEND_COLOR_ALTITUDE		= new LegendColor(new ValueColor[] {
			new ValueColor(10, 161, 85, 0),
			new ValueColor(50, 232, 169, 0),
			new ValueColor(100, 0, 241, 0),
			new ValueColor(150, 0, 158, 255),
			new ValueColor(190, 205, 234, 255)							},
																				LegendColor.BRIGHTNESS_DIMMING,
																				15,
																				LegendColor.BRIGHTNESS_LIGHTNING,
																				100);

	private static final LegendColor		LEGEND_COLOR_PULSE			= new LegendColor(new ValueColor[] {
			new ValueColor(10, 0, 203, 0),
			new ValueColor(50, 57, 255, 0),
			new ValueColor(100, 255, 255, 0),
			new ValueColor(150, 255, 0, 0),
			new ValueColor(190, 255, 0, 247)							},
																				LegendColor.BRIGHTNESS_DIMMING,
																				34,
																				LegendColor.BRIGHTNESS_DIMMING,
																				38);

	private static final LegendColor		LEGEND_COLOR_GRADIENT		= new LegendColor(new ValueColor[] {
			new ValueColor(10, 0, 0, 255),
			new ValueColor(50, 0, 255, 255),
			new ValueColor(100, 0, 237, 0),
			new ValueColor(150, 255, 255, 0),
			new ValueColor(190, 255, 0, 0)								},
																				LegendColor.BRIGHTNESS_DIMMING,
																				59,
																				LegendColor.BRIGHTNESS_DIMMING,
																				27);

	private static final LegendColor		LEGEND_COLOR_PACE			= new LegendColor(new ValueColor[] {
			new ValueColor(10, 255, 0, 0),
			new ValueColor(50, 255, 0, 128),
			new ValueColor(100, 255, 0, 255),
			new ValueColor(150, 126, 0, 255),
			new ValueColor(190, 0, 0, 255)								},
																				LegendColor.BRIGHTNESS_DIMMING,
																				54,
																				LegendColor.BRIGHTNESS_DIMMING,
																				18)

																		;

	private static final LegendColor		LEGEND_COLOR_SPEED			= new LegendColor(new ValueColor[] {
			new ValueColor(10, 0, 0, 255),
			new ValueColor(50, 135, 0, 255),
			new ValueColor(100, 255, 0, 255),
			new ValueColor(150, 255, 0, 124),
			new ValueColor(190, 255, 0, 0)								},
																				LegendColor.BRIGHTNESS_DIMMING,
																				54,
																				LegendColor.BRIGHTNESS_DIMMING,
																				18);

	/**
	 * 
	 */
	private static final ColorDefinition[]	GRAPH_COLOR_DEFAULTS		= new ColorDefinition[] {

			new ColorDefinition(PREF_GRAPH_ALTITUDE,//
					Messages.Graph_Label_Altitude,
					new RGB(255, 255, 255),
					new RGB(0, 255, 0),
					new RGB(45, 188, 45),
					LEGEND_COLOR_ALTITUDE),

			new ColorDefinition(PREF_GRAPH_HEARTBEAT,
					Messages.Graph_Label_Heartbeat,
					new RGB(255, 255, 255),
					new RGB(253, 0, 0),
					new RGB(253, 0, 0),
					LEGEND_COLOR_PULSE),

			new ColorDefinition(PREF_GRAPH_SPEED,//
					Messages.Graph_Label_Speed,
					new RGB(255, 255, 255),
					new RGB(0, 135, 211),
					new RGB(0, 132, 210),
					LEGEND_COLOR_SPEED),

			new ColorDefinition(PREF_GRAPH_PACE,//
					Messages.Graph_Label_Pace,
					new RGB(255, 255, 255),
					new RGB(0, 47, 211),
					new RGB(0, 43, 210),
					LEGEND_COLOR_PACE),

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
					LEGEND_COLOR_GRADIENT),

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
					null)												};

	private static GraphColorProvider		instance;

	private ColorDefinition[]				fGraphColorDefinitions;

	public GraphColorProvider() {}

	public static GraphColorProvider getInstance() {
		if (instance == null) {
			instance = new GraphColorProvider();
		}
		return instance;
	}

	private static XMLMemento getXMLMementoRoot() {
		Document document;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element element = document.createElement(MEMENTO_ROOT);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	/**
	 * write the legend data into a xml file
	 */
	public static void saveLegendData() {

		BufferedWriter writer = null;

		try {

			IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			File file = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			XMLMemento xmlMemento = getXMLMementoRoot();

			for (ColorDefinition graphDefinition : GraphColorProvider.getInstance().getGraphColorDefinitions()) {

				final LegendColor legendColor = graphDefinition.getNewLegendColor();

				// legendColor can be null when a legend color is not defined
				if (legendColor == null) {
					continue;
				}

				IMemento mementoLegendColor = xmlMemento.createChild(MEMENTO_CHILD_LEGEND_COLOR);
				mementoLegendColor.putString(TAG_LEGEND_COLOR_PREF_NAME, graphDefinition.getPrefName());

				for (ValueColor valueColor : legendColor.valueColors) {

					IMemento mementoValueColor = mementoLegendColor.createChild(MEMENTO_CHILD_VALUE_COLOR);

					mementoValueColor.putInteger(TAG_VALUE_COLOR_VALUE, valueColor.value);
					mementoValueColor.putInteger(TAG_VALUE_COLOR_RED, valueColor.red);
					mementoValueColor.putInteger(TAG_VALUE_COLOR_GREEN, valueColor.green);
					mementoValueColor.putInteger(TAG_VALUE_COLOR_BLUE, valueColor.blue);
				}

				IMemento mementoBrightness = mementoLegendColor.createChild(MEMENTO_CHILD_BRIGHTNESS);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MIN, legendColor.minBrightness);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MIN_FACTOR, legendColor.minBrightnessFactor);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MAX, legendColor.maxBrightness);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MAX_FACTOR, legendColor.maxBrightnessFactor);
			}

			xmlMemento.save(writer);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ColorDefinition[] getGraphColorDefinitions() {

		if (fGraphColorDefinitions != null) {
			return fGraphColorDefinitions;
		}

		List<ColorDefinition> list = new ArrayList<ColorDefinition>();

		Collections.addAll(list, GRAPH_COLOR_DEFAULTS);

		// sort list by name
//		Collections.sort(list, new Comparator<ColorDefinition>() {
//			public int compare(ColorDefinition def1, ColorDefinition def2) {
//				return def1.getVisibleName().compareTo(def2.getVisibleName());
//			}
//		});

		fGraphColorDefinitions = list.toArray(new ColorDefinition[list.size()]);

		readLegendColors();
		setLegendColors();

		return fGraphColorDefinitions;
	}

	/**
	 * Read legend data from a xml file
	 */
	private void readLegendColors() {

		IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		File file = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile();

		// check if file is available
		if (file.exists() == false) {
			return;
		}

		InputStreamReader reader = null;

		try {
			reader = new InputStreamReader(new FileInputStream(file), "UTF-8"); //$NON-NLS-1$

			XMLMemento mementoRoot = XMLMemento.createReadRoot(reader);
			IMemento[] mementoLegendColors = mementoRoot.getChildren(MEMENTO_CHILD_LEGEND_COLOR);

			// loop: all legend colors
			for (IMemento mementoLegendColor : mementoLegendColors) {

				String prefName = mementoLegendColor.getString(TAG_LEGEND_COLOR_PREF_NAME);

				if (prefName == null) {
					continue;
				}

				IMemento[] mementoValueColors = mementoLegendColor.getChildren(MEMENTO_CHILD_VALUE_COLOR);

				if (mementoLegendColors == null) {
					continue;
				}

				LegendColor legendColor = new LegendColor();

				/*
				 * read value colors
				 */
				ArrayList<ValueColor> valueColors = new ArrayList<ValueColor>();

				// loop: all value colors
				for (IMemento mementoValueColor : mementoValueColors) {

					Integer value = mementoValueColor.getInteger(TAG_VALUE_COLOR_VALUE);
					Integer red = mementoValueColor.getInteger(TAG_VALUE_COLOR_RED);
					Integer green = mementoValueColor.getInteger(TAG_VALUE_COLOR_GREEN);
					Integer blue = mementoValueColor.getInteger(TAG_VALUE_COLOR_BLUE);

					if (value != null && red != null && green != null && blue != null) {
						valueColors.add(new ValueColor(value, red, green, blue));
					}
				}

				legendColor.valueColors = valueColors.toArray(new ValueColor[valueColors.size()]);

				/*
				 * read brightness
				 */
				IMemento[] mementoBrightness = mementoLegendColor.getChildren(MEMENTO_CHILD_BRIGHTNESS);

				if (mementoBrightness.length == 0) {
					continue;
				}

				IMemento mementoBrightness0 = mementoBrightness[0];

				Integer minBrightness = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MIN);
				if (minBrightness != null) {
					legendColor.minBrightness = minBrightness;
				}
				Integer minBrightnessFactor = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MIN_FACTOR);
				if (minBrightness != null) {
					legendColor.minBrightnessFactor = minBrightnessFactor;
				}
				Integer maxBrightness = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MAX);
				if (maxBrightness != null) {
					legendColor.maxBrightness = maxBrightness;
				}
				Integer maxBrightnessFactor = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MAX_FACTOR);
				if (minBrightness != null) {
					legendColor.maxBrightnessFactor = maxBrightnessFactor;
				}

				/*
				 * put the legend color into the color definition
				 */

				for (ColorDefinition colorDefinition : fGraphColorDefinitions) {

					if (colorDefinition.getPrefName().equals(prefName)) {

						// color definition found

						colorDefinition.setLegendColor(legendColor);
						break;
					}
				}
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * set legend colors when available
	 */
	private void setLegendColors() {

		for (ColorDefinition colorDefinition : fGraphColorDefinitions) {

			// set legend color
			if (colorDefinition.getLegendColor() == null) {

				// legend color is not set, try to get default when available

				final LegendColor defaultLegendColor = colorDefinition.getDefaultLegendColor();
				if (defaultLegendColor != null) {
					colorDefinition.setLegendColor(defaultLegendColor.getCopy());
				}
			}

			// set new legend color
			LegendColor legendColor = colorDefinition.getLegendColor();
			colorDefinition.setNewLegendColor(legendColor);
		}
	}
}
