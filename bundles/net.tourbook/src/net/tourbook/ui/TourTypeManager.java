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
package net.tourbook.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class TourTypeManager {

	private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.ui.TourTypeManager");//$NON-NLS-1$

	static {}

	private static final String					TAG_ROOT						= "TourTypeImageLayout";			//$NON-NLS-1$
	private static final String					XML_STATE_TOUR_TYPE_MANAGER		= "XML_STATE_TOUR_TYPE_MANAGER";	//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_IMAGE_LAYOUT		= "tourTypeImageLayout";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_BORDER_LAYOUT	= "tourTypeBorderLayout";			//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_BORDER_WIDTH		= "tourTypeBorderWidth";			//$NON-NLS-1$

	public static final TourTypeLayout			DEFAULT_IMAGE_LAYOUT			= TourTypeLayout.FILL_RECT_DARK;
	public static final TourTypeBorder			DEFAULT_BORDER_LAYOUT			= TourTypeBorder.BORDER_LEFT_RIGHT;
	public static final int						DEFAULT_BORDER_WIDTH			= 1;

	private static TourTypeLayout				_currentTourTypeImageLayout		= DEFAULT_IMAGE_LAYOUT;
	private static TourTypeBorder				_currentTourTypeBorderLayout	= DEFAULT_BORDER_LAYOUT;
	private static int							_currentTourTypeBorderWidth		= DEFAULT_BORDER_WIDTH;

	private static final TourTypeLayoutData[]	_allTourTypeLayoutData			= new TourTypeLayoutData[] {

			new TourTypeLayoutData(
					TourTypeLayout.FILL_RECT_DARK,
					Messages.TourType_Config_Layout_Fill_Rect_Dark),

			new TourTypeLayoutData(
					TourTypeLayout.FILL_RECT_BRIGHT,
					Messages.TourType_Config_Layout_Fill_Rect_Bright),

			new TourTypeLayoutData(
					TourTypeLayout.FILL_CIRCLE_DARK,
					Messages.TourType_Config_Layout_Fill_Circle_Dark),

			new TourTypeLayoutData(
					TourTypeLayout.FILL_CIRCLE_BRIGHT,
					Messages.TourType_Config_Layout_Fill_Circle_Bright),

			new TourTypeLayoutData(
					TourTypeLayout.GRADIENT_LEFT_RIGHT,
					Messages.TourType_Config_Layout_Gradient_LeftRight),

			new TourTypeLayoutData(
					TourTypeLayout.GRADIENT_RIGHT_LEFT,
					Messages.TourType_Config_Layout_Gradient_RightLeft),

			new TourTypeLayoutData(
					TourTypeLayout.GRADIENT_TOP_BOTTOM,
					Messages.TourType_Config_Layout_Gradient_TopBottom),

			new TourTypeLayoutData(
					TourTypeLayout.GRADIENT_BOTTOM_TOP,
					Messages.TourType_Config_Layout_Gradient_BottomTop),

			new TourTypeLayoutData(
					TourTypeLayout.NOTHING,
					Messages.TourType_Config_Layout_Nothing),
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

	public static class TourTypeLayoutData {

		public TourTypeLayout	tourTypeLayout;
		public String			label;

		public TourTypeLayoutData(final TourTypeLayout tourTypeLayout, final String label) {

			this.tourTypeLayout = tourTypeLayout;
			this.label = label;
		}
	}

	public static TourTypeBorderData[] getAllTourTypeBorderData() {
		return _allTourTypeBorderData;
	}

	public static TourTypeLayoutData[] getAllTourTypeLayoutData() {
		return _allTourTypeLayoutData;
	}

	public static TourTypeBorder getCurrentBorderLayout() {
		return _currentTourTypeBorderLayout;
	}

	public static int getCurrentBorderWidth() {
		return _currentTourTypeBorderWidth;
	}

	public static TourTypeLayout getCurrentImageLayout() {
		return _currentTourTypeImageLayout;
	}

	public static int getTourTypeBorderIndex() {

		return getTourTypeBorderIndex(_currentTourTypeBorderLayout);
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

	public static int getTourTypeLayoutIndex() {

		return getTourTypeLayoutIndex(_currentTourTypeImageLayout);
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

				_currentTourTypeImageLayout = (TourTypeLayout) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_IMAGE_LAYOUT,
						DEFAULT_IMAGE_LAYOUT);

				_currentTourTypeBorderLayout = (TourTypeBorder) Util.getXmlEnum(
						xmlRoot,
						ATTR_TOUR_TYPE_BORDER_LAYOUT,
						DEFAULT_BORDER_LAYOUT);

				_currentTourTypeBorderWidth = Util.getXmlInteger(
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

		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_IMAGE_LAYOUT, _currentTourTypeImageLayout);
		Util.setXmlEnum(xmlRoot, ATTR_TOUR_TYPE_BORDER_LAYOUT, _currentTourTypeBorderLayout);
		xmlRoot.putInteger(ATTR_TOUR_TYPE_BORDER_WIDTH, _currentTourTypeBorderWidth);

		// Write the XML block to the state store.
		try (final Writer writer = new StringWriter()) {

			xmlRoot.save(writer);
			_state.put(XML_STATE_TOUR_TYPE_MANAGER, writer.toString());

		} catch (final IOException e) {
			StatusUtil.log(e);
		}

	}

	public static void setTourTypeLayout(	final TourTypeLayout newTourTypeLayout,
											final TourTypeBorder newTourTypeBorderLayout,
											final int newBorderWidth) {

		_currentTourTypeImageLayout = newTourTypeLayout;
		_currentTourTypeBorderLayout = newTourTypeBorderLayout;
		_currentTourTypeBorderWidth = newBorderWidth;
	}

}
