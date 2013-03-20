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
import net.tourbook.common.action.SmallImageButton;
import net.tourbook.common.tooltip.ToolTip3;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class PropertyViewerToolTip extends ToolTip3 {

	private static final int			SHELL_MARGIN			= 5;
	private static final int			WINDOW_TITLE_HEIGHT		= 12;

	/**
	 * Relative start of the sensitive area in a hovered row.
	 */
	private static final double			HOVERED_SENSITIVE_AREA	= 0.66;

	/**
	 * Column index for the info/tooltip column
	 */

	private ContainerCheckedTreeViewer	_propViewer;
	private Tree						_tree;

	private ViewerRow					_viewerRow;
	private ViewerRow					_sensitiveRowArea;

	private TVIMap3Layer				_mapLayer;

	private boolean						_isTTCloseHovered;
	private boolean						_isTTPinnedHovered;
	private boolean						_isTTDragged;
	private int							_devXTTMouseDown;
	private int							_devYTTMouseDown;

	private int							_hoverLeftBorder;
	private int							_columnWidth;

	/*
	 * UI resources
	 */
	private Composite					_shellContainer;
	private Color						_bgColor;
	private Color						_fgColor;
	private Font						_boldFont;

	private Cursor						_cursorDragged;
	private Cursor						_cursorHand;

	private Image						_ttCloseImage;
	private Image						_ttCloseImageHovered;
	private Image						_ttPinnedImage;
	private Image						_ttPinnedImageHovered;
	private Image						_ttPinnedImageDisabled;
	private Canvas						_canvasCloseTT;
	private SmallImageButton			_buttonPin;

	public PropertyViewerToolTip(final ContainerCheckedTreeViewer propViewer) {

		super(propViewer.getTree());

		_propViewer = propViewer;

		_tree = propViewer.getTree();
		_tree.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		final Device display = _tree.getDisplay();

		_cursorDragged = new Cursor(display, SWT.CURSOR_SIZEALL);
		_cursorHand = new Cursor(display, SWT.CURSOR_HAND);

		_ttCloseImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_APP_CLOSE_SMALL);
		_ttCloseImageHovered = UI.IMAGE_REGISTRY.get(UI.IMAGE_APP_CLOSE_SMALL_HOVERED);
		_ttPinnedImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_APP_PINNED_SMALL);
		_ttPinnedImageHovered = UI.IMAGE_REGISTRY.get(UI.IMAGE_APP_PINNED_SMALL_HOVERED);
		_ttPinnedImageDisabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_APP_PINNED_SMALL_DISABLED);

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private void addCloseTTListener(final Canvas canvasCloseTT) {

		canvasCloseTT.addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {

				if (_ttCloseImage == null || _ttCloseImage.isDisposed()) {
					return;
				}

				final GC gc = e.gc;

				// draw header background
				gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				gc.fillRectangle(e.x, e.y, e.width, e.height);

				final Image image = _isTTCloseHovered ? _ttCloseImageHovered : _ttCloseImage;
				gc.drawImage(image, 0, 0);
			}
		});

		canvasCloseTT.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				// hide tooltip
				canvasCloseTT.getShell().close();
			}
		});

		canvasCloseTT.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseEnter(final MouseEvent e) {
				_isTTCloseHovered = true;
				canvasCloseTT.redraw();
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				_isTTCloseHovered = false;
				canvasCloseTT.redraw();
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		});
	}

	private void addTTHeaderListener(final Composite header) {

		header.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseEnter(final MouseEvent e) {

				header.setCursor(_cursorHand);
			}

			@Override
			public void mouseExit(final MouseEvent e) {

				_isTTDragged = false;

				header.setCursor(null);
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		});

		header.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent event) {

//				System.out.println(UI.timeStampNano() + " mouseMove\t");
//				// TODO remove SYSTEM.OUT.PRINTLN

				if (_isTTDragged) {

					final int xDiff = event.x - _devXTTMouseDown;
					final int yDiff = event.y - _devYTTMouseDown;

					setDraggedLocation(header.getShell(), xDiff, yDiff);
				}
			}
		});

		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent event) {

				System.out.println(UI.timeStampNano() + " mouseDown\t" + _cursorDragged); //$NON-NLS-1$
				// TODO remove SYSTEM.OUT.PRINTLN

				_isTTDragged = true;

				_devXTTMouseDown = event.x;
				_devYTTMouseDown = event.y;

				header.setCursor(_cursorDragged);
			}

			@Override
			public void mouseUp(final MouseEvent e) {

				if (_isTTDragged) {

					_isTTDragged = false;

					_buttonPin.setEnabled(true);

					toolTipIsMoved(_shellContainer.getShell());
				}

				header.setCursor(_cursorHand);
			}
		});
	}

	@Override
	protected void createToolTipContentArea(final Composite parent) {

		createUI(parent);
	}

	private Composite createUI(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			if (_mapLayer.layerConfigProvider == null || _mapLayer.isLayerVisible == false) {
				createUI_10_Default(_shellContainer);
			} else {
				createUI_50_Custom(_shellContainer);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Default(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_20_Info(container);
			UI.setColorForAllChildren(container, _fgColor, _bgColor);
		}
	}

	private void createUI_20_Info(final Composite parent) {

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

	private void createUI_50_Custom(final Composite parent) {

//		System.out.println(UI.timeStampNano() + " _appCloseImageHovered\t" + _appCloseImageHovered);
//		// TODO remove SYSTEM.OUT.PRINTLN
		final ILayerConfigProvider layerConfigProvider = _mapLayer.layerConfigProvider;

		createUI_60_ToolTipHeader(parent, layerConfigProvider);

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults() //
				.extendedMargins(SHELL_MARGIN, SHELL_MARGIN, 0, SHELL_MARGIN)
				.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			layerConfigProvider.createConfigUI(this, container);
		}
	}

	private void createUI_60_ToolTipHeader(final Composite parent, final ILayerConfigProvider layerConfigProvider) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, WINDOW_TITLE_HEIGHT).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// show layer title
		container.setToolTipText(layerConfigProvider.getTitle());

		addTTHeaderListener(container);

		{
			/*
			 * button: pin
			 */
			final Rectangle imagePinBounds = _ttCloseImage.getBounds();

			_buttonPin = new SmallImageButton(container, _ttPinnedImage, _ttPinnedImageDisabled, _ttPinnedImageHovered);

			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.grab(false, true)
					.hint(imagePinBounds.width, imagePinBounds.height)
					.applyTo(_buttonPin);

			_buttonPin.setToolTipText(Messages.Map3_Tooltip_UnPin);
			_buttonPin.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));

			_buttonPin.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectPin();
				}
			});
//			_buttonPin.setVisible(false);


			/*
			 * button: close
			 */
			final Rectangle imageBounds = _ttCloseImage.getBounds();

			_canvasCloseTT = new Canvas(container, SWT.DOUBLE_BUFFERED);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.CENTER)
					.grab(true, true)
					.hint(imageBounds.width, imageBounds.height)
					.applyTo(_canvasCloseTT);

			_canvasCloseTT.setToolTipText(Messages.Map3_Tooltip_Close);
//			_canvasCloseTT.setVisible(false);

			addCloseTTListener(_canvasCloseTT);
		}
	}

	/**
	 * @return Returns row which is hovered with the mouse or <code>null</code> when no row is
	 *         hovered or when mouse is not hoverering the sensitive area.
	 *         <p>
	 *         Sensitive area is 1/3 of the right side of the row which is set in
	 *         {@link #HOVERED_SENSITIVE_AREA} = {@value #HOVERED_SENSITIVE_AREA}
	 */
	ViewerRow getHoveredRow() {

		return _sensitiveRowArea;
	}

	@Override
	protected Object getToolTipArea(final Point ownerHoverPosition) {

		// set default values
		_mapLayer = null;
		_viewerRow = null;
		_sensitiveRowArea = null;

		ViewerRow ttArea = null;

		final ViewerCell viewerCell = _propViewer.getCell(ownerHoverPosition);

		if (viewerCell != null) {

			/*
			 * use the whole row as content area that when mouse is hovering other cells in the same
			 * row, the tooltip keeps open
			 */
			_viewerRow = viewerCell.getViewerRow();

			_columnWidth = _tree.getColumn(0).getWidth();

			_hoverLeftBorder = (int) (_columnWidth * HOVERED_SENSITIVE_AREA);

			if (ownerHoverPosition.x > _hoverLeftBorder) {

				/*
				 * the info tooltip is opened when the mouse is in the last 1/3 part of the row
				 */

				_sensitiveRowArea = _viewerRow;

				final Object element = _viewerRow.getElement();

				if (element instanceof TVIMap3Layer) {

					ttArea = _viewerRow;

					final TVIMap3Layer mapLayer = (TVIMap3Layer) element;
					_mapLayer = mapLayer;
				}
			}
		}

		/*
		 * show user that sensitive row area is hovered and actions can be done
		 */
		_propViewer.getTree().setCursor(_sensitiveRowArea == null ? null : _cursorHand);

		return ttArea;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize, final Point mouseOwnerPosition) {

		if (_viewerRow == null) {
			return null;
		}

		// try to position the tooltip at the bottom of the cell

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
			 * adjust vertical position, it is outside of the display, prevent default repositioning
			 */

			ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - rowHeight;
		}

		resetUI();

		return ttDisplayLocation;
	}

	private void onDispose() {

		_cursorDragged = UI.disposeResource(_cursorDragged);
		_cursorHand = UI.disposeResource(_cursorHand);
	}

	private void onSelectPin() {
		// TODO Auto-generated method stub
		
	}

	private void resetUI() {

		_isTTCloseHovered = false;
	}

	void setLayerVisibility(final TVIMap3Layer mapLayer) {

		if (_mapLayer != null) {

			// tooltip is displayed

			if (_mapLayer.layerConfigProvider != null) {

				// update UI when a custom config provider is set

				update();
			}
		}
	}

}
