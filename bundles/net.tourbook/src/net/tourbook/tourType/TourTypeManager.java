/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tourType;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.ui.Messages;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class TourTypeManager {

	private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.tourType.TourTypeManager");//$NON-NLS-1$

	static {}

	private static final String					TAG_ROOT						= "TourTypeImageLayout";			//$NON-NLS-1$
	private static final String					XML_STATE_TOUR_TYPE_MANAGER		= "XML_STATE_TOUR_TYPE_MANAGER";	//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_BORDER_COLOR		= "tourTypeBorderColor";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_BORDER_LAYOUT	= "tourTypeBorderLayout";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_BORDER_WIDTH		= "tourTypeBorderWidth";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_IMAGE_COLOR1		= "tourTypeImageColor1";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_IMAGE_COLOR2		= "tourTypeImageColor2";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_IMAGE_LAYOUT		= "tourTypeImageLayout";			//$NON-NLS-1$

	public static final TourTypeColor			DEFAULT_IMAGE_COLOR1			= TourTypeColor.COLOR_DARK;
	public static final TourTypeColor			DEFAULT_IMAGE_COLOR2			= TourTypeColor.COLOR_BRIGHT;
	public static final TourTypeLayout			DEFAULT_IMAGE_LAYOUT			= TourTypeLayout.RECTANGLE;
	public static final TourTypeColor			DEFAULT_BORDER_COLOR			= TourTypeColor.COLOR_LINE;
	public static final TourTypeBorder			DEFAULT_BORDER_LAYOUT			= TourTypeBorder.BORDER_LEFT_RIGHT;
	public static final int						DEFAULT_BORDER_WIDTH			= 1;

	private static final TourTypeImageConfig	_imageConfig					= new TourTypeImageConfig();

	private static final TourTypeColorData[]	_allTourTypeColorData			= new TourTypeColorData[] {

			new TourTypeColorData(
					TourTypeColor.COLOR_BRIGHT,
					Messages.TourType_Config_Color_Bright),

			new TourTypeColorData(
					TourTypeColor.COLOR_DARK,
					Messages.TourType_Config_Color_Dark),

			new TourTypeColorData(
					TourTypeColor.COLOR_LINE,
					Messages.TourType_Config_Color_Line),
	};

	private static final TourTypeLayoutData[]	_allTourTypeLayoutData			= new TourTypeLayoutData[] {

			new TourTypeLayoutData(
					TourTypeLayout.RECTANGLE,
					Messages.TourType_Config_Layout_Fill_Rectangle,
					true,
					false),

			new TourTypeLayoutData(
					TourTypeLayout.CIRCLE,
					Messages.TourType_Config_Layout_Fill_Circle,
					true,
					false),

			new TourTypeLayoutData(
					TourTypeLayout.GRADIENT_HORIZONTAL,
					Messages.TourType_Config_Layout_Gradient_Horizontal,
					true,
					true),

			new TourTypeLayoutData(
					TourTypeLayout.GRADIENT_VERTICAL,
					Messages.TourType_Config_Layout_Gradient_Vertical,
					true,
					true),

			new TourTypeLayoutData(
					TourTypeLayout.NOTHING,
					Messages.TourType_Config_Layout_Nothing,
					false,
					false),
	};

	private static final TourTypeBorderData[]	_allTourTypeBorderData			= new TourTypeBorderData[] {

			new TourTypeBorderData(
					TourTypeBorder.BORDER_RECTANGLE,
					Messages.TourType_Config_Border_Rectangle),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_CIRCLE,
					Messages.TourType_Config_Border_Circle),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_LEFT,
					Messages.TourType_Config_Border_Left),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_RIGHT,
					Messages.TourType_Config_Border_Right),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_LEFT_RIGHT,
					Messages.TourType_Config_Border_LeftRight),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_TOP,
					Messages.TourType_Config_Border_Top),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_BOTTOM,
					Messages.TourType_Config_Border_Bottom),

			new TourTypeBorderData(
					TourTypeBorder.BORDER_TOP_BOTTOM,
					Messages.TourType_Config_Border_TopBottom),

	};

	public static class TourTypeBorderData {

		public TourTypeBorder	tourTypeBorder;
		public String			label;

		public TourTypeBorderData(final TourTypeBorder tourTypeBorder, final String label) {

			this.tourTypeBorder = tourTypeBorder;
			this.label = label;
		}
	}

	public static class TourTypeColorData {

		public TourTypeColor	tourTypeColor;
		public String			label;

		public TourTypeColorData(final TourTypeColor tourTypeColor, final String label) {

			this.tourTypeColor = tourTypeColor;
			this.label = label;
		}
	}

	public static class TourTypeLayoutData {

		public TourTypeLayout	tourTypeLayout;
		public String			label;
		public boolean			isColor1;
		public boolean			isColor2;

		public TourTypeLayoutData(	final TourTypeLayout tourTypeLayout,
									final String label,
									final boolean isColor1,
									final boolean isColor2) {

			this.tourTypeLayout = tourTypeLayout;
			this.label = label;
			this.isColor1 = isColor1;
			this.isColor2 = isColor2;
		}
	}

	public static TourTypeBorderData[] getAllTourTypeBorderData() {
		return _allTourTypeBorderData;
	}

	public static TourTypeColorData[] getAllTourTypeColorData() {
		return _allTourTypeColorData;
	}

	public static TourTypeLayoutData[] getAllTourTypeLayoutData() {
		return _allTourTypeLayoutData;
	}

	public static TourTypeImageConfig getImageConfig() {
		return _imageConfig;
	}

	public static int getTourTypeBorderIndex(final TourTypeBorder requestedData) {

		final TourTypeBorderData[] allData = getAllTourTypeBorderData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourTypeBorderData data = allData[dataIndex];

			if (data.tourTypeBorder.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	public static int getTourTypeColorIndex(final TourTypeColor requestedData) {

		final TourTypeColorData[] allData = getAllTourTypeColorData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourTypeColorData data = allData[dataIndex];

			if (data.tourTypeColor.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	public static int getTourTypeLayoutIndex(final TourTypeLayout requestedData) {

		final TourTypeLayoutData[] allData = getAllTourTypeLayoutData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourTypeLayoutData data = allData[dataIndex];

			if (data.tourTypeLayout.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	public static void restoreState() {

		final String stateValue = Util.getStateString(_state, XML_STATE_TOUR_TYPE_MANAGER, null);
		if (stateValue != null) {

			try {

				final Reader reader = new StringReader(stateValue);
				final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);

				/*
				 * Image
				 */
				_imageConfig.imageLayout = (TourTypeLayout) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_IMAGE_LAYOUT,
						DEFAULT_IMAGE_LAYOUT);

				_imageConfig.imageColor1 = (TourTypeColor) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_IMAGE_COLOR1,
						DEFAULT_IMAGE_COLOR1);

				_imageConfig.imageColor2 = (TourTypeColor) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_IMAGE_COLOR2,
						DEFAULT_IMAGE_COLOR2);

				/*
				 * Border
				 */
				_imageConfig.borderColor = (TourTypeColor) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_BORDER_COLOR,
						DEFAULT_BORDER_COLOR);

				_imageConfig.borderLayout = (TourTypeBorder) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_BORDER_LAYOUT,
						DEFAULT_BORDER_LAYOUT);

				_imageConfig.borderWidth = Util.getXmlInteger(
						xmlRoot,
						ATTR_TOUR_TYPE_BORDER_WIDTH,
						DEFAULT_BORDER_WIDTH);

			} catch (final WorkbenchException e) {
				// ignore
			}
		}
	}

	public static void saveState() {

		// use xml to can use the enum tools
		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		/*
		 * Image
		 */
		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_IMAGE_LAYOUT, _imageConfig.imageLayout);
		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_IMAGE_COLOR1, _imageConfig.imageColor1);
		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_IMAGE_COLOR2, _imageConfig.imageColor2);

		/*
		 * Border
		 */
		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_BORDER_COLOR, _imageConfig.borderColor);
		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_BORDER_LAYOUT, _imageConfig.borderLayout);
		xmlRoot.putInteger(ATTR_TOUR_TYPE_BORDER_WIDTH, _imageConfig.borderWidth);

		// Write the XML block to the state store.
		try (final Writer writer = new StringWriter()) {

			xmlRoot.save(writer);
			_state.put(XML_STATE_TOUR_TYPE_MANAGER, writer.toString());

		} catch (final IOException e) {
			StatusUtil.log(e);
		}

	}

}
