/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import gov.nasa.worldwind.layers.Layer;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolTip3;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class PropertyViewerToolTip extends ToolTip3 {

	private static final double			HOVER_RELATIVE_LEFT_BORDER	= 0.66;

	private static final int			SHELL_MARGIN				= 5;

	/**
	 * Column index for the info/tooltip column
	 */
	private static final int			INFO_COLUMN_INDEX			= 0;

	private ContainerCheckedTreeViewer	_propViewer;
	private Tree						_tree;

	private Object						_ttContentArea;
	private ViewerRow					_viewerRow;

	private TVIMap3Layer				_mapLayer;

	/*
	 * UI resources
	 */
	private Color						_bgColor;
	private Color						_fgColor;
	private Font						_boldFont;

	private Composite					_ttContainer;

	private int							_hoverLeftBorder;
//	private int							_rowWidth;
	private int							_columnWidth;

	public PropertyViewerToolTip(final ContainerCheckedTreeViewer propViewer) {

		super(propViewer.getTree());

		_propViewer = propViewer;
		_tree = propViewer.getTree();
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		final Composite container = createUI(parent);

		UI.setColorForAllChildren(container, _fgColor, _bgColor);

		return container;
	}

	private Composite createUI(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			_ttContainer = new Composite(shellContainer, SWT.NONE);
			GridLayoutFactory.fillDefaults() //
					.numColumns(1)
					.equalWidth(true)
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_10_Info(_ttContainer);
			}
		}

		return shellContainer;
	}

	private void createUI_10_Info(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * layer name
			 */
			Label label = new Label(container, SWT.NONE);
			label.setFont(_boldFont);
			label.setText(_mapLayer.name);

			final Layer wwLayer = _mapLayer.wwLayer;

			final double minActiveAltitude = wwLayer.getMinActiveAltitude();
			final double maxActiveAltitude = wwLayer.getMaxActiveAltitude();

			/*
			 * min/max altitude
			 */
			label = new Label(container, SWT.NONE);

			label.setText(UI.FormatDoubleMinMaxElevationMeter(minActiveAltitude)
					+ UI.ELLIPSIS_WITH_SPACE
					+ UI.FormatDoubleMinMaxElevationMeter(maxActiveAltitude));
		}
	}

	@Override
	protected Object getToolTipArea(final Point ownerHoverPosition) {

		_ttContentArea = null;
		_mapLayer = null;

		final ViewerCell viewerCell = _propViewer.getCell(ownerHoverPosition);

		if (viewerCell != null) {

			/*
			 * use the whole row as content area that when mouse is hovering other cells in the same
			 * row, the tooltip keeps open
			 */
			_viewerRow = viewerCell.getViewerRow();

			_columnWidth = _tree.getColumn(0).getWidth();

			_hoverLeftBorder = (int) (_columnWidth * HOVER_RELATIVE_LEFT_BORDER);

			if (ownerHoverPosition.x > _hoverLeftBorder) {

				/*
				 * the info tooltip is opened when the mouse is in the last 1/3 part of the row
				 */

				final Object element = _viewerRow.getElement();

				if (element instanceof TVIMap3Layer) {

					_ttContentArea = _viewerRow;

					final TVIMap3Layer mapLayer = (TVIMap3Layer) element;
					_mapLayer = mapLayer;
				}
			}
		}

		return _ttContentArea;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize, final Point mouseOwnerPosition) {

		// try to position the tooltip at the bottom of the cell

		if (_ttContentArea != null) {

			final Rectangle displayBounds = _tree.getDisplay().getBounds();

			final Rectangle rowBounds = _viewerRow.getBounds();
			final int rowWidth = rowBounds.width;
			final int rowHeight = rowBounds.height;

			final int devX = _columnWidth;
			final int devY = rowBounds.y;// + cellHeight;

			final Point ttDisplayLocation = _tree.toDisplay(devX, devY);
			final int tipSizeWidth = tipSize.x;
			final int tipSizeHeight = tipSize.y;

			if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

				ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth - rowWidth;
			}

			if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

				/*
				 * adjust vertical position, it is outside of the display, prevent default
				 * repositioning
				 */

				ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - rowHeight;
			}

			return fixupDisplayBounds(tipSize, ttDisplayLocation);
		}

		return null;
	}

}
