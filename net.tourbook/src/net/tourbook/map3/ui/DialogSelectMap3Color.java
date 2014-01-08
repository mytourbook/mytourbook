/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.ui;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.Map3ProfileComparator;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.view.Map3View;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map3 tour track layer properties dialog.
 */
public class DialogSelectMap3Color extends AnimatedToolTipShell {

	private static int									PROFILE_IMAGE_HEIGHT	= 5;
	private static final int							PROFILE_IMAGE_MIN_SIZE	= 30;

	private static final int							SHELL_MARGIN			= 0;

	// initialize with default values which are (should) never be used
	private Rectangle									_toolTipItemBounds		= new Rectangle(0, 0, 50, 50);

	private final WaitTimer								_waitTimer				= new WaitTimer();

	private MapGraphId									_graphId;
//	private Map3View			_map3View;

	private boolean										_canOpenToolTip;
	private boolean										_isWaitTimerStarted;

	private HashMap<Map3GradientColorProvider, Image>	_profileImages			= new HashMap<Map3GradientColorProvider, Image>();

	private boolean										_isInUIUpdate;

	private CheckboxTableViewer							_colorViewer;

	private int											_columnIndexProfileImage;
	private int											_oldImageWidth			= -1;

	private PixelConverter								_pc;

	/*
	 * UI resources
	 */
	private Color										_fgColor;
	private Color										_bgColor;

	/*
	 * UI controls
	 */
	private Composite									_shellContainer;

	private TableColumn									_tcProfileImage;
	private boolean										_isInFireEvent;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public DialogSelectMap3Color(	final Control ownerControl,
									final ToolBar toolBar,
									final Map3View map3View,
									final MapGraphId graphId) {

		super(ownerControl);

		_graphId = graphId;
//		_map3View = map3View;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});

		ownerControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canShowToolTip() {

		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		_pc = new PixelConverter(parent);

		PROFILE_IMAGE_HEIGHT = (int) (_pc.convertHeightInCharsToPixels(1) * 1.0);

		final Composite container = createUI(parent);

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		_fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		_bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

		net.tourbook.common.UI.updateChildColors(parent, _fgColor, _bgColor);

		_colorViewer.setInput(this);

//		final Point size = _colorViewer.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);
//		System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tsize: " + size));
//		// TODO remove SYSTEM.OUT.PRINTLN

		return container;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_shellContainer);
		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_ColorViewer(_shellContainer);
		}

		return _shellContainer;
	}

	private void createUI_10_ColorViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.hint(SWT.DEFAULT, SWT.DEFAULT)
				.hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT)
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, //
				SWT.CHECK //
//						| SWT.FULL_SELECTION
//						| SWT.BORDER
		//
		);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			public void handleEvent(final Event event) {

				if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {
					onViewerPaint(event);
				}
			}
		};
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem, paintListener);

		_colorViewer = new CheckboxTableViewer(table);

		/*
		 * create columns
		 */

		defineColumn_10_Checkbox(tableLayout);

//		defineColumn_20_MinValue(tableLayout);
//		defineColumn_22_MinValueOverwrite(tableLayout);

		defineColumn_30_Spacer(tableLayout);
		defineColumn_32_ColorImage(tableLayout);
		defineColumn_30_Spacer(tableLayout);

//		defineColumn_40_MaxValueOverwrite(tableLayout);
//		defineColumn_42_MaxValue(tableLayout);

		_colorViewer.setComparator(new Map3ProfileComparator());

		_colorViewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {}

			public Object[] getElements(final Object inputElement) {

				final ArrayList<Map3GradientColorProvider> colorProviders = Map3GradientColorManager
						.getColorProviders(_graphId);

				return colorProviders.toArray(//
						new Map3GradientColorProvider[colorProviders.size()]);
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_colorViewer.setCheckStateProvider(new ICheckStateProvider() {

			@Override
			public boolean isChecked(final Object element) {
				return onViewerIsChecked(element);
			}

			@Override
			public boolean isGrayed(final Object element) {
				return onViewerIsGrayed(element);
			}
		});

		_colorViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				onViewerCheckStateChange(event);
			}
		});

		_colorViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onViewerSelectColor();
			}
		});
	}

	/**
	 * Column: Show only the checkbox
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_10_Checkbox(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setMoveable(false);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					cell.setText(colorProfile.getProfileName());
				}
			}
		});

//		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(5), false, true));
		tableLayout.setColumnData(tc, new ColumnWeightData(20));
	}

	/**
	 * Column: Min value
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_20_MinValue(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

		final TableColumn tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map3Color_Column_MinValue_Header);
		tc.setToolTipText(Messages.Pref_Map3Color_Column_MinValue_Label);
		tc.setMoveable(false);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final String valueText;
					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isMinValueOverwrite()) {

						valueText = Integer.toString(colorProfile.getMinValueOverwrite());

					} else {

						final ProfileImage profileImage = colorProfile.getProfileImage();

						final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
						final RGBVertex firstVertex = vertices.get(0);

						valueText = Integer.toString(firstVertex.getValue());
					}

					cell.setText(valueText);

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

//		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(8), false, true));
		tableLayout.setColumnData(tc, new ColumnWeightData(8));
	}

	/**
	 * Column: Min value overwrite
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_22_MinValueOverwrite(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.CENTER);

		final TableColumn tc = tvc.getColumn();
		tc.setToolTipText(Messages.Pref_Map3Color_Column_MinValueOverwrite_Tooltip);
		tc.setMoveable(false);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isMinValueOverwrite()) {
						cell.setText(UI.SYMBOL_EXCLAMATION_POINT);
					} else {
						cell.setText(UI.EMPTY_STRING);
					}

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

//		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(2), false, true));
		tableLayout.setColumnData(tc, new ColumnWeightData(2));
	}

	/**
	 * Column: spacer
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_30_Spacer(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

//				cell.setText(UI.EMPTY_STRING);
			}
		});

		tableLayout.setColumnData(tc, new ColumnWeightData(1, 3));
	}

	/**
	 * Column: Color image
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_32_ColorImage(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map3Color_Column_Colors);
		tc.setMoveable(false);

		_tcProfileImage = tc;
		_columnIndexProfileImage = _colorViewer.getTable().getColumnCount() - 1;

		tc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeImageColumn();
			}
		});

		tvc.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});

//		tableLayout.setColumnData(tc, new ColumnPixelData(_defaultProfileImageWidth, false, true));
		tableLayout.setColumnData(tc, new ColumnWeightData(50, false));
	}

	/**
	 * Column: Max value overwrite
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_40_MaxValueOverwrite(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.CENTER);

		final TableColumn tc = tvc.getColumn();
		tc.setToolTipText(Messages.Pref_Map3Color_Column_MaxValueOverwrite_Tooltip);
		tc.setMoveable(false);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isMaxValueOverwrite()) {
						cell.setText(UI.SYMBOL_EXCLAMATION_POINT);
					} else {
						cell.setText(UI.EMPTY_STRING);
					}

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

//		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(2), false, true));
		tableLayout.setColumnData(tc, new ColumnWeightData(2));
	}

	/**
	 * Column: Max value
	 * 
	 * @param tableLayout
	 */
	private void defineColumn_42_MaxValue(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setText(Messages.Pref_Map3Color_Column_MaxValue_Header);
		tc.setToolTipText(Messages.Pref_Map3Color_Column_MaxValue_Label);
		tc.setMoveable(false);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final String valueText;
					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isMaxValueOverwrite()) {

						valueText = Integer.toString(colorProfile.getMaxValueOverwrite());

					} else {

						final ProfileImage profileImage = colorProfile.getProfileImage();

						final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
						final RGBVertex lastVertex = vertices.get(vertices.size() - 1);

						valueText = Integer.toString(lastVertex.getValue());
					}

					cell.setText(valueText);

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

//		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(8), false, true));
		tableLayout.setColumnData(tc, new ColumnWeightData(8));
	}

	/**
	 * @return Returns <code>true</code> when the colors are disposed, otherwise <code>false</code>.
	 */
	public boolean disposeColors() {

		if (_isInFireEvent) {
			return false;
		}

		disposeProfileImages();

		return true;
	}

	private void disposeProfileImages() {

		for (final Image profileImage : _profileImages.values()) {
			profileImage.dispose();
		}

		_profileImages.clear();
	}

	/**
	 * Fire event that 3D map colors have changed.
	 */
	private void fireModifyEvent() {

		_isInFireEvent = true;
		{
			TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
		}
		_isInFireEvent = false;
	}

	private int getImageColumnWidth() {

		int width;

		if (_tcProfileImage == null) {
			width = 20;
		} else {
			width = _tcProfileImage.getWidth();
		}

		// ensure min size
		if (width < PROFILE_IMAGE_MIN_SIZE) {
			width = PROFILE_IMAGE_MIN_SIZE;
		}

		return width;
	}

	private Image getProfileImage(final Map3GradientColorProvider colorProvider) {

		Image image = _profileImages.get(colorProvider);

		if (isProfileImageValid(image)) {

			// image is OK

		} else {

			final int imageWidth = getImageColumnWidth();

			final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();
			final ArrayList<RGBVertex> rgbVertices = colorProfile.getProfileImage().getRgbVertices();

			colorProvider.configureColorProvider(imageWidth, rgbVertices);

			image = TourMapPainter.createMapLegendImage(//
					Display.getCurrent(),
					colorProvider,
					imageWidth,
					PROFILE_IMAGE_HEIGHT - 1,
					false,
					false);

			final Image oldImage = _profileImages.put(colorProvider, image);

			Util.disposeResource(oldImage);

			_oldImageWidth = imageWidth;
		}

		return image;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int tipWidth = tipSize.x;

		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		final int devY = _toolTipItemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	/**
	 * @param image
	 * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
	 *         the profile image must be created,
	 */
	private boolean isProfileImageValid(final Image image) {

		if (image == null || image.isDisposed()) {

			return false;

		}

		if (image.getBounds().width != getImageColumnWidth()) {

			image.dispose();

			return false;
		}

		return true;
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	public void onDispose() {

		disposeProfileImages();
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

	}

	private void onResizeImageColumn() {

		final int newImageWidth = getImageColumnWidth();

		// check if the width has changed
		if (newImageWidth == _oldImageWidth) {
			return;
		}

		// recreate images
		disposeProfileImages();
	}

	private void onViewerCheckStateChange(final CheckStateChangedEvent event) {

		final Object viewerItem = event.getElement();

		if (viewerItem instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) viewerItem;

			if (event.getChecked()) {

				// set as active color provider

				setActiveColorProvider(colorProvider);

			} else {

				// a color provider cannot be unchecked, to be unchecked, another color provider must be checked

				_colorViewer.setChecked(colorProvider, true);
			}
		}

	}

	private boolean onViewerIsChecked(final Object element) {

		if (element instanceof Map3GradientColorProvider) {

			// set checked only active color providers

			final Map3GradientColorProvider mgrColorProvider = (Map3GradientColorProvider) element;
			final boolean isActiveColorProfile = mgrColorProvider.getMap3ColorProfile().isActiveColorProfile();

			return isActiveColorProfile;
		}

		return false;
	}

	private boolean onViewerIsGrayed(final Object element) {

		if (element instanceof Map3ColorDefinition) {
			return true;
		}

		return false;
	}

	private void onViewerPaint(final Event event) {

		// paint images at the correct column

		final int columnIndex = event.index;

		if (columnIndex == _columnIndexProfileImage) {

			onViewerPaint_ProfileImage(event);
		}
	}

	private void onViewerPaint_ProfileImage(final Event event) {

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

//			event.width += getImageColumnWidth();
			event.height = PROFILE_IMAGE_HEIGHT;

			break;

		case SWT.PaintItem:

			final TableItem item = (TableItem) event.item;
			final Object itemData = item.getData();

			if (itemData instanceof Map3GradientColorProvider) {

				final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) itemData;

				final Image image = getProfileImage(colorProvider);

				if (image != null) {

					final Rectangle rect = image.getBounds();

					final int x = event.x + event.width;
					final int yOffset = Math.max(0, (event.height - rect.height) / 2);

					event.gc.drawImage(image, x, event.y + yOffset);
				}
			}

			break;
		}
	}

	/**
	 * Is called when a color in the color viewer is selected.
	 */
	private void onViewerSelectColor() {

		if (_isInUIUpdate) {
			return;
		}

		final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();

		final Object selectedItem = selection.getFirstElement();

		if (selectedItem instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;

			setActiveColorProvider(selectedColorProvider);
		}

	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	/**
	 * @param selectedColorProvider
	 * @return Returns <code>true</code> when a new color provider is set, otherwise
	 *         <code>false</code>.
	 */
	private boolean setActiveColorProvider(final Map3GradientColorProvider selectedColorProvider) {

		final Map3ColorProfile selectedColorProfile = selectedColorProvider.getMap3ColorProfile();

		// check if the selected color provider is already the active color provider
		if (selectedColorProfile.isActiveColorProfile()) {
			return false;
		}

		final MapGraphId graphId = selectedColorProvider.getGraphId();
		final Map3ColorDefinition colorDefinition = Map3GradientColorManager.getColorDefinition(graphId);

		final ArrayList<Map3GradientColorProvider> allGraphIdColorProvider = colorDefinition.getColorProviders();

		if (allGraphIdColorProvider.size() < 2) {

			// this case should need no attention

		} else {

			// set selected color provider as active color provider

			// reset state for previous color provider
			final Map3GradientColorProvider oldActiveColorProvider = Map3GradientColorManager
					.getActiveMap3ColorProvider(graphId);
			_colorViewer.setChecked(oldActiveColorProvider, false);

			// set state for selected color provider
			_colorViewer.setChecked(selectedColorProvider, true);

			// set new active color provider
			Map3GradientColorManager.setActiveColorProvider(selectedColorProvider);

			_isInUIUpdate = true;
			{
				// also select a checked color provider
				_colorViewer.setSelection(new StructuredSelection(selectedColorProvider));
			}
			_isInUIUpdate = false;

			fireModifyEvent();

			return true;
		}

		return false;
	}

}
