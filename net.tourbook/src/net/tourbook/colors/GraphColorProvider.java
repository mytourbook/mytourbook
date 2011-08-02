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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.mapping.LegendColor;
import net.tourbook.mapping.ValueColor;

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
	public static final String				PREF_GRAPH_GRADIENT			= "gradient";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_ALTIMETER		= "altimeter";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_CADENCE			= "cadence";			//$NON-NLS-1$
	public static final String				PREF_GRAPH_TOUR_COMPARE		= "tourCompare";		//$NON-NLS-1$
	public static final String				PREF_GRAPH_PACE				= "pace";				//$NON-NLS-1$

	public static final String				PREF_COLOR_BRIGHT			= "bright";			//$NON-NLS-1$
	public static final String				PREF_COLOR_DARK				= "dark";				//$NON-NLS-1$
	public static final String				PREF_COLOR_LINE				= "line";				//$NON-NLS-1$
	public static final String				PREF_COLOR_TEXT				= "text";				//$NON-NLS-1$
	public static final String				PREF_COLOR_MAPPING			= "mapping";			//$NON-NLS-1$

	private static final String				MEMENTO_LEGEND_COLOR_FILE	= "legendcolor.xml";	//$NON-NLS-1$
	private static final String				MEMENTO_ROOT				= "legendcolorlist";	//$NON-NLS-1$

	private static final String				MEMENTO_CHILD_LEGEND_COLOR	= "legendcolor";		//$NON-NLS-1$
	private static final String				TAG_LEGEND_COLOR_PREF_NAME	= "prefname";			//$NON-NLS-1$

	private static final String				MEMENTO_CHILD_VALUE_COLOR	= "valuecolor";		//$NON-NLS-1$
	private static final String				TAG_VALUE_COLOR_VALUE		= "value";				//$NON-NLS-1$
	private static final String				TAG_VALUE_COLOR_RED			= "red";				//$NON-NLS-1$
	private static final String				TAG_VALUE_COLOR_GREEN		= "green";				//$NON-NLS-1$
	private static final String				TAG_VALUE_COLOR_BLUE		= "blue";				//$NON-NLS-1$

	private static final String				MEMENTO_CHILD_BRIGHTNESS	= "brightness";		//$NON-NLS-1$
	private static final String				TAG_BRIGHTNESS_MIN			= "min";				//$NON-NLS-1$
	private static final String				TAG_BRIGHTNESS_MIN_FACTOR	= "minFactor";			//$NON-NLS-1$
	private static final String				TAG_BRIGHTNESS_MAX			= "max";				//$NON-NLS-1$
	private static final String				TAG_BRIGHTNESS_MAX_FACTOR	= "maxFactor";			//$NON-NLS-1$

	private static final String				MEMENTO_CHILD_MIN_MAX_VALUE	= "minmaxValue";		//$NON-NLS-1$
	private static final String				TAG_IS_MIN_VALUE_OVERWRITE	= "isMinOverwrite";	//$NON-NLS-1$
	private static final String				TAG_MIN_VALUE_OVERWRITE		= "minValueOverwrite";	//$NON-NLS-1$
	private static final String				TAG_IS_MAX_VALUE_OVERWRITE	= "isMaxOverwrite";	//$NON-NLS-1$
	private static final String				TAG_MAX_VALUE_OVERWRITE		= "maxValueOverwrite";	//$NON-NLS-1$

	public static String[][]				colorNames					= new String[][] {
			{ PREF_COLOR_BRIGHT, Messages.Graph_Pref_color_gradient_bright },
			{ PREF_COLOR_DARK, Messages.Graph_Pref_color_gradient_dark },
			{ PREF_COLOR_LINE, Messages.Graph_Pref_color_line },
			{ PREF_COLOR_TEXT, Messages.Graph_Pref_ColorText },
			{ PREF_COLOR_MAPPING, Messages.Graph_Pref_color_mapping }	};

	private static final LegendColor		LEGEND_COLOR_ALTITUDE		= new LegendColor(
																				new ValueColor[] {
			new ValueColor(10, 130, 0, 157),
			new ValueColor(50, 255, 85, 13),
			new ValueColor(100, 255, 255, 0),
			new ValueColor(150, 0, 170, 9),
			new ValueColor(190, 23, 163, 255)									},
																				LegendColor.BRIGHTNESS_DIMMING,
																				15,
																				LegendColor.BRIGHTNESS_LIGHTNING,
																				39);

	private static final LegendColor		LEGEND_COLOR_PULSE			= new LegendColor(
																				new ValueColor[] {
			new ValueColor(10, 0, 203, 0),
			new ValueColor(50, 57, 255, 0),
			new ValueColor(100, 255, 255, 0),
			new ValueColor(150, 255, 0, 0),
			new ValueColor(190, 255, 0, 247)									},
																				LegendColor.BRIGHTNESS_DIMMING,
																				11,
																				LegendColor.BRIGHTNESS_DIMMING,
																				10);

	private static final LegendColor		LEGEND_COLOR_GRADIENT		= new LegendColor(
																				new ValueColor[] {
			new ValueColor(10, 0, 0, 255),
			new ValueColor(50, 0, 255, 255),
			new ValueColor(100, 0, 237, 0),
			new ValueColor(150, 255, 255, 0),
			new ValueColor(190, 255, 0, 0)										},
																				LegendColor.BRIGHTNESS_DIMMING,
																				23,
																				LegendColor.BRIGHTNESS_DIMMING,
																				10,
																				true,
																				-20,
																				true,
																				20);

	private static final LegendColor		LEGEND_COLOR_PACE			= new LegendColor(
																				new ValueColor[] {
			new ValueColor(10, 255, 0, 0),
			new ValueColor(50, 255, 255, 0),
			new ValueColor(100, 0, 169, 0),
			new ValueColor(150, 0, 255, 255),
			new ValueColor(190, 0, 0, 255)										},
																				LegendColor.BRIGHTNESS_DIMMING,
																				17,
																				LegendColor.BRIGHTNESS_DIMMING,
																				8)

																		;

	private static final LegendColor		LEGEND_COLOR_SPEED			= new LegendColor(
																				new ValueColor[] {
			new ValueColor(10, 0, 0, 255),
			new ValueColor(50, 0, 255, 255),
			new ValueColor(100, 0, 169, 0),
			new ValueColor(150, 255, 255, 0),
			new ValueColor(190, 255, 0, 0)										},
																				LegendColor.BRIGHTNESS_DIMMING,
																				17,
																				LegendColor.BRIGHTNESS_DIMMING,
																				8);

	/**
	 * 
	 */
	private static final ColorDefinition[]	GRAPH_COLOR_DEFAULTS		= new ColorDefinition[] {

			new ColorDefinition(PREF_GRAPH_ALTITUDE,//
					Messages.Graph_Label_Altitude,
					new RGB(255, 255, 255),
					new RGB(0, 255, 0),
					new RGB(45, 188, 45),
					new RGB(44, 134, 33),
					LEGEND_COLOR_ALTITUDE),

			new ColorDefinition(PREF_GRAPH_HEARTBEAT, //
					Messages.Graph_Label_Heartbeat,
					new RGB(255, 255, 255),
					new RGB(253, 0, 0),
					new RGB(253, 0, 0),
					new RGB(183, 0, 0),
					LEGEND_COLOR_PULSE),

			new ColorDefinition(PREF_GRAPH_SPEED,//
					Messages.Graph_Label_Speed,
					new RGB(255, 255, 255),
					new RGB(0, 135, 211),
					new RGB(0, 132, 210),
					new RGB(0, 106, 167),
					LEGEND_COLOR_SPEED),

			new ColorDefinition(PREF_GRAPH_PACE,//
					Messages.Graph_Label_Pace,
					new RGB(255, 255, 255),
					new RGB(0x9C, 0x2F, 0xFF),
					new RGB(0x9C, 0x2F, 0xFF),
					new RGB(88, 26, 142),
					LEGEND_COLOR_PACE),

			new ColorDefinition(PREF_GRAPH_POWER,//
					Messages.Graph_Label_Power,
					new RGB(255, 255, 255),
					new RGB(240, 0, 150),
					new RGB(240, 0, 150),
					new RGB(106, 0, 67),
					null),

			new ColorDefinition(PREF_GRAPH_TEMPTERATURE, //
					Messages.Graph_Label_Temperature,
					new RGB(255, 255, 255),
					new RGB(0, 217, 240),
					new RGB(0, 216, 240),
					new RGB(0, 134, 147),
					null),

			new ColorDefinition(PREF_GRAPH_GRADIENT, //
					Messages.Graph_Label_Gradient,
					new RGB(255, 255, 255),
					new RGB(249, 231, 0),
					new RGB(236, 206, 0),
					new RGB(111, 98, 0),
					LEGEND_COLOR_GRADIENT),

			new ColorDefinition(PREF_GRAPH_ALTIMETER, //
					Messages.Graph_Label_Altimeter,
					new RGB(255, 255, 255),
					new RGB(255, 180, 0),
					new RGB(249, 174, 0),
					new RGB(144, 103, 0),
					null),

			new ColorDefinition(PREF_GRAPH_CADENCE,//
					Messages.Graph_Label_Cadence,
					new RGB(255, 255, 255),
					new RGB(228, 106, 16),
					new RGB(228, 106, 16),
					new RGB(139, 64, 10),
					null),

			new ColorDefinition(PREF_GRAPH_TOUR_COMPARE, //
					Messages.Graph_Label_Tour_Compare,
					new RGB(255, 255, 255),
					new RGB(255, 140, 26),
					new RGB(242, 135, 22),
					new RGB(139, 77, 15),
					null),

			new ColorDefinition(PREF_GRAPH_DISTANCE,//
					Messages.Graph_Pref_color_statistic_distance,
					new RGB(255, 255, 255),
					new RGB(239, 167, 16),
					new RGB(203, 141, 14),
					new RGB(139, 98, 10),
					null),

			new ColorDefinition(PREF_GRAPH_TIME,//
					Messages.Graph_Pref_color_statistic_time,
					new RGB(255, 255, 255),
					new RGB(187, 187, 140),
					new RGB(170, 170, 127),
					new RGB(88, 88, 67),
					null)												};

	private static GraphColorProvider		_instance;

	private ColorDefinition[]				_graphColorDefinitions;

	public GraphColorProvider() {}

	public static GraphColorProvider getInstance() {
		if (_instance == null) {
			_instance = new GraphColorProvider();
		}
		return _instance;
	}

	private static XMLMemento getXMLMementoRoot() {
		Document document;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			final Element element = document.createElement(MEMENTO_ROOT);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (final ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	/**
	 * write the legend color data into a xml file
	 */
	public static void saveLegendData() {

		BufferedWriter writer = null;

		try {

			final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			final File file = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlMemento = getXMLMementoRoot();

			for (final ColorDefinition graphDefinition : GraphColorProvider.getInstance().getGraphColorDefinitions()) {

				final LegendColor legendColor = graphDefinition.getNewLegendColor();

				// legendColor can be null when a legend color is not defined
				if (legendColor == null) {
					continue;
				}

				final IMemento mementoLegendColor = xmlMemento.createChild(MEMENTO_CHILD_LEGEND_COLOR);
				mementoLegendColor.putString(TAG_LEGEND_COLOR_PREF_NAME, graphDefinition.getPrefName());

				for (final ValueColor valueColor : legendColor.valueColors) {

					final IMemento mementoValueColor = mementoLegendColor.createChild(MEMENTO_CHILD_VALUE_COLOR);

					mementoValueColor.putInteger(TAG_VALUE_COLOR_VALUE, valueColor.value);
					mementoValueColor.putInteger(TAG_VALUE_COLOR_RED, valueColor.red);
					mementoValueColor.putInteger(TAG_VALUE_COLOR_GREEN, valueColor.green);
					mementoValueColor.putInteger(TAG_VALUE_COLOR_BLUE, valueColor.blue);
				}

				final IMemento mementoBrightness = mementoLegendColor.createChild(MEMENTO_CHILD_BRIGHTNESS);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MIN, legendColor.minBrightness);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MIN_FACTOR, legendColor.minBrightnessFactor);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MAX, legendColor.maxBrightness);
				mementoBrightness.putInteger(TAG_BRIGHTNESS_MAX_FACTOR, legendColor.maxBrightnessFactor);

				final IMemento mementoMinMaxValue = mementoLegendColor.createChild(MEMENTO_CHILD_MIN_MAX_VALUE);
				mementoMinMaxValue.putInteger(TAG_IS_MIN_VALUE_OVERWRITE, legendColor.isMinValueOverwrite ? 1 : 0);
				mementoMinMaxValue.putInteger(TAG_MIN_VALUE_OVERWRITE, legendColor.overwriteMinValue);
				mementoMinMaxValue.putInteger(TAG_IS_MAX_VALUE_OVERWRITE, legendColor.isMaxValueOverwrite ? 1 : 0);
				mementoMinMaxValue.putInteger(TAG_MAX_VALUE_OVERWRITE, legendColor.overwriteMaxValue);
			}

			xmlMemento.save(writer);

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param preferenceName
	 *            preference name PREF_GRAPH_...
	 * @return Returns the {@link ColorDefinition} for the preference name
	 */
	public ColorDefinition getGraphColorDefinition(final String preferenceName) {

		final ColorDefinition[] colorDefinitions = getGraphColorDefinitions();
		for (final ColorDefinition colorDefinition : colorDefinitions) {
			if (colorDefinition.getPrefName().equals(preferenceName)) {
				return colorDefinition;
			}
		}

		return null;
	}

	public ColorDefinition[] getGraphColorDefinitions() {

		if (_graphColorDefinitions != null) {
			return _graphColorDefinitions;
		}

		final List<ColorDefinition> list = new ArrayList<ColorDefinition>();

		Collections.addAll(list, GRAPH_COLOR_DEFAULTS);

		// sort list by name
//		Collections.sort(list, new Comparator<ColorDefinition>() {
//			public int compare(ColorDefinition def1, ColorDefinition def2) {
//				return def1.getVisibleName().compareTo(def2.getVisibleName());
//			}
//		});

		_graphColorDefinitions = list.toArray(new ColorDefinition[list.size()]);

		readLegendColors();
		setLegendColors();

		return _graphColorDefinitions;
	}

	/**
	 * Read legend data from a xml file
	 */
	private void readLegendColors() {

		final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		final File file = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile();

		// check if file is available
		if (file.exists() == false) {
			return;
		}

		InputStreamReader reader = null;

		try {
			reader = new InputStreamReader(new FileInputStream(file), "UTF-8"); //$NON-NLS-1$

			final XMLMemento mementoRoot = XMLMemento.createReadRoot(reader);
			final IMemento[] mementoLegendColors = mementoRoot.getChildren(MEMENTO_CHILD_LEGEND_COLOR);

			// loop: all legend colors
			for (final IMemento mementoLegendColor : mementoLegendColors) {

				// check pref name
				final String prefName = mementoLegendColor.getString(TAG_LEGEND_COLOR_PREF_NAME);
				if (prefName == null) {
					continue;
				}

				// check value colors
				final IMemento[] mementoValueColors = mementoLegendColor.getChildren(MEMENTO_CHILD_VALUE_COLOR);
				if (mementoValueColors == null) {
					continue;
				}

				final LegendColor legendColor = new LegendColor();

				/*
				 * value colors
				 */
				final ArrayList<ValueColor> valueColors = new ArrayList<ValueColor>();

				// loop: all value colors
				for (final IMemento mementoValueColor : mementoValueColors) {

					final Integer value = mementoValueColor.getInteger(TAG_VALUE_COLOR_VALUE);
					final Integer red = mementoValueColor.getInteger(TAG_VALUE_COLOR_RED);
					final Integer green = mementoValueColor.getInteger(TAG_VALUE_COLOR_GREEN);
					final Integer blue = mementoValueColor.getInteger(TAG_VALUE_COLOR_BLUE);

					if (value != null && red != null && green != null && blue != null) {
						valueColors.add(new ValueColor(value, red, green, blue));
					}
				}
				legendColor.valueColors = valueColors.toArray(new ValueColor[valueColors.size()]);

				/*
				 * min/max brightness
				 */
				final IMemento[] mementoBrightness = mementoLegendColor.getChildren(MEMENTO_CHILD_BRIGHTNESS);
				if (mementoBrightness.length > 0) {

					final IMemento mementoBrightness0 = mementoBrightness[0];

					final Integer minBrightness = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MIN);
					if (minBrightness != null) {
						legendColor.minBrightness = minBrightness;
					}
					final Integer minBrightnessFactor = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MIN_FACTOR);
					if (minBrightness != null) {
						legendColor.minBrightnessFactor = minBrightnessFactor;
					}
					final Integer maxBrightness = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MAX);
					if (maxBrightness != null) {
						legendColor.maxBrightness = maxBrightness;
					}
					final Integer maxBrightnessFactor = mementoBrightness0.getInteger(TAG_BRIGHTNESS_MAX_FACTOR);
					if (minBrightness != null) {
						legendColor.maxBrightnessFactor = maxBrightnessFactor;
					}
				}

				/*
				 * min/max overwrite
				 */
				final IMemento[] mementoMinMaxValue = mementoLegendColor.getChildren(MEMENTO_CHILD_MIN_MAX_VALUE);
				if (mementoMinMaxValue.length > 0) {

					final IMemento mementoMinMaxValue0 = mementoMinMaxValue[0];

					final Integer isMinOverwrite = mementoMinMaxValue0.getInteger(TAG_IS_MIN_VALUE_OVERWRITE);
					if (isMinOverwrite != null) {
						legendColor.isMinValueOverwrite = isMinOverwrite == 1;
					}
					final Integer minValue = mementoMinMaxValue0.getInteger(TAG_MIN_VALUE_OVERWRITE);
					if (minValue != null) {
						legendColor.overwriteMinValue = minValue;
					}

					final Integer isMaxOverwrite = mementoMinMaxValue0.getInteger(TAG_IS_MAX_VALUE_OVERWRITE);
					if (isMaxOverwrite != null) {
						legendColor.isMaxValueOverwrite = isMaxOverwrite == 1;
					}
					final Integer maxValue = mementoMinMaxValue0.getInteger(TAG_MAX_VALUE_OVERWRITE);
					if (maxValue != null) {
						legendColor.overwriteMaxValue = maxValue;
					}
				}

				/*
				 * update color definition with the read data
				 */
				for (final ColorDefinition colorDefinition : _graphColorDefinitions) {

					if (colorDefinition.getPrefName().equals(prefName)) {

						// color definition found

						colorDefinition.setLegendColor(legendColor);
						break;
					}
				}
			}

		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final WorkbenchException e) {
			e.printStackTrace();
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * set legend colors when available
	 */
	private void setLegendColors() {

		for (final ColorDefinition colorDefinition : _graphColorDefinitions) {

			// set legend color
			if (colorDefinition.getLegendColor() == null) {

				// legend color is not set, try to get default when available

				final LegendColor defaultLegendColor = colorDefinition.getDefaultLegendColor();
				if (defaultLegendColor != null) {
					colorDefinition.setLegendColor(defaultLegendColor.getCopy());
				}
			}

			// set new legend color
			final LegendColor legendColor = colorDefinition.getLegendColor();
			colorDefinition.setNewLegendColor(legendColor);
		}
	}
}
