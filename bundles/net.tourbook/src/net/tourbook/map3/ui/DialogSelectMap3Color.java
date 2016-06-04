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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ColorProviderConfig;
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
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3View;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap3Color;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Map3 tour track layer properties dialog.
 */
public class DialogSelectMap3Color extends AnimatedToolTipShell implements IMap3ColorUpdater {

	private static final String							IMAGE_APP_ADD					= net.tourbook.Messages.Image__App_Add;
	private static final String							IMAGE_GRAPH_ALL					= net.tourbook.Messages.Image__Options_Bright;

	private static final int							SHELL_MARGIN					= 0;

	private static int									NUMBER_OF_VISIBLE_ROWS			= 6;
	private static final int							COLUMN_WITH_ABSOLUTE_RELATIVE	= 4;
	private static final int							COLUMN_WITH_COLOR_IMAGE			= 15;
	private static final int							COLUMN_WITH_NAME				= 15;
	private static final int							COLUMN_WITH_VALUE				= 8;

	private static int									PROFILE_IMAGE_HEIGHT			= -1;

	private final IPreferenceStore						_prefStore						= TourbookPlugin.getPrefStore();

	// initialize with default values which are (should) never be used
	private Rectangle									_toolTipItemBounds				= new Rectangle(0, 0, 50, 50);

	private final WaitTimer								_waitTimer						= new WaitTimer();

	private MapGraphId									_graphId;
	private Map3View									_map3View;

	private boolean										_canOpenToolTip;
	private boolean										_isWaitTimerStarted;

	private boolean										_isInUIUpdate;
	private boolean										_isInFireEvent;

	private int											_columnIndexProfileImage;

	private Action										_actionAddColor;
	private Action										_actionEditAllColors;
	private Action										_actionEditSelectedColor;

	/*
	 * UI resources
	 */
	private PixelConverter								_pc;
	private HashMap<Map3GradientColorProvider, Image>	_profileImages					= new HashMap<Map3GradientColorProvider, Image>();
	private CheckboxTableViewer							_colorViewer;

	/*
	 * UI controls
	 */
	private Composite									_shellContainer;
	private TableColumn									_tcProfileImage;

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
		_map3View = map3View;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void actionAddColor() {

		final Object selectedItem = ((IStructuredSelection) _colorViewer.getSelection()).getFirstElement();

		final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;
		final Map3GradientColorProvider duplicatedColorProvider = selectedColorProvider.clone();

		// create a new profile name by setting it to the profile id which is unique
		duplicatedColorProvider.getMap3ColorProfile().setDuplicatedName();

		close();

		new DialogMap3ColorEditor(//
				_map3View.getShell(),
				duplicatedColorProvider,
				this,
				true).open();

	}

	private void actionEditAllColors() {

		close();

		PreferencesUtil.createPreferenceDialogOn(//
				_map3View.getShell(),
				PrefPageMap3Color.ID,
				null,
				_graphId).open();
	}

	private void actionEditSelectedColor() {

		final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();

		final Object selectedItem = selection.getFirstElement();
		final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;

		close();

		new DialogMap3ColorEditor(//
				_map3View.getShell(),
				selectedColorProvider,
				this,
				false).open();
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});

//		ownerControl.addDisposeListener(new DisposeListener() {
//			@Override
//			public void widgetDisposed(final DisposeEvent e) {
//				onDispose();
//			}
//		});
	}

	@Override
	public void applyMapColors(	final Map3GradientColorProvider originalCP,
								final Map3GradientColorProvider modifiedCP,
								final boolean isNewColorProvider) {

		/*
		 * Update model
		 */
		if (isNewColorProvider) {

			// a new profile is edited
			Map3GradientColorManager.addColorProvider(modifiedCP);

		} else {

			// an existing profile is modified
			Map3GradientColorManager.replaceColorProvider(originalCP, modifiedCP);
		}

		// fire event that color has changed
		TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		return true;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	private void createActions() {

		/*
		 * Action: Add color
		 */
		_actionAddColor = new Action() {
			@Override
			public void run() {
				actionAddColor();
			}
		};
		_actionAddColor.setImageDescriptor(TourbookPlugin.getImageDescriptor(IMAGE_APP_ADD));
		_actionAddColor.setToolTipText(Messages.Map3SelectColor_Dialog_Action_AddColor_Tooltip);

		/*
		 * Action: Edit selected color
		 */
		_actionEditSelectedColor = new Action() {
			@Override
			public void run() {
				actionEditSelectedColor();
			}
		};
		_actionEditSelectedColor.setImageDescriptor(UI.getGraphImageDescriptor(_graphId));
		_actionEditSelectedColor.setToolTipText(Messages.Map3SelectColor_Dialog_Action_EditSelectedColors);

		/*
		 * Action: Edit all colors.
		 */
		_actionEditAllColors = new Action() {
			@Override
			public void run() {
				actionEditAllColors();
			}
		};
		_actionEditAllColors.setImageDescriptor(TourbookPlugin.getImageDescriptor(IMAGE_GRAPH_ALL));
		_actionEditAllColors.setToolTipText(Messages.Map3SelectColor_Dialog_Action_EditAllColors);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite container = createUI(parent);

		updateUI_colorViewer();

		return container;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.margins(SHELL_MARGIN, SHELL_MARGIN)
				.spacing(0, 0)
				.applyTo(_shellContainer);
//		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_ColorViewer(_shellContainer);
			createUI_20_Actions(_shellContainer);
		}

		// set color for all controls
		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

		net.tourbook.common.UI.setChildColors(_shellContainer, fgColor, bgColor);

		_shellContainer.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		return _shellContainer;
	}

	private void createUI_10_ColorViewer(final Composite parent) {

		final ArrayList<Map3GradientColorProvider> colorProviders = Map3GradientColorManager
				.getColorProviders(_graphId);

		int tableStyle;
		if (colorProviders.size() > NUMBER_OF_VISIBLE_ROWS) {

			tableStyle = SWT.CHECK //
					| SWT.FULL_SELECTION
//				| SWT.H_SCROLL
					| SWT.V_SCROLL
					| SWT.NO_SCROLL;
		} else {

			// table contains less than maximum entries, scroll is not necessary

			tableStyle = SWT.CHECK //
					| SWT.FULL_SELECTION
					| SWT.NO_SCROLL;
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);

		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * create table
			 */
			final Table table = new Table(container, tableStyle);

			GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
			table.setHeaderVisible(false);
			table.setLinesVisible(false);

			/*
			 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
			 * critical for performance that these methods be as efficient as possible.
			 */
			final Listener paintListener = new Listener() {
				@Override
				public void handleEvent(final Event event) {

					if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {
						onViewerPaint(event);
					}
				}
			};
			table.addListener(SWT.MeasureItem, paintListener);
			table.addListener(SWT.PaintItem, paintListener);

			/*
			 * Set maximum number of visible rows
			 */
			table.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {

					final int itemHeight = table.getItemHeight();
					final int maxHeight = itemHeight * NUMBER_OF_VISIBLE_ROWS;

					final int defaultHeight = table.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

					if (defaultHeight > maxHeight) {

						final GridData gd = (GridData) container.getLayoutData();
						gd.heightHint = maxHeight;

//						container.layout(true, true);
					}
				}
			});

			_colorViewer = new CheckboxTableViewer(table);

			/*
			 * create columns
			 */
			defineColumn_10_Checkbox();
			defineColumn_20_MinValue();
			defineColumn_30_ColorImage();
			defineColumn_40_MaxValue();
			defineColumn_50_RelativeAbsolute();
			defineColumn_52_OverwriteLegendMinMax();

			_colorViewer.setComparator(new Map3ProfileComparator());

			_colorViewer.setContentProvider(new IStructuredContentProvider() {

				@Override
				public void dispose() {}

				@Override
				public Object[] getElements(final Object inputElement) {

					return colorProviders.toArray(new Map3GradientColorProvider[colorProviders.size()]);
				}

				@Override
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
				@Override
				public void selectionChanged(final SelectionChangedEvent event) {
					onViewerSelectColor();
				}
			});

			_colorViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(final DoubleClickEvent event) {
					actionEditSelectedColor();
				}
			});
		}
	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.extendedMargins(2, 0, 3, 2)
				.applyTo(container);
		{

			final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

			final ToolBarManager tbm = new ToolBarManager(toolbar);

			tbm.add(_actionAddColor);
			tbm.add(_actionEditSelectedColor);
			tbm.add(_actionEditAllColors);

			tbm.update(true);
		}
	}

	/**
	 * Column: Show only the checkbox
	 */
	private void defineColumn_10_Checkbox() {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_NAME));

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
	}

	/**
	 * Column: Min value
	 */
	private void defineColumn_20_MinValue() {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

		final TableColumn tc = tvc.getColumn();
		tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_VALUE));

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					final ProfileImage profileImage = colorProfile.getProfileImage();

					final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
					final RGBVertex firstVertex = vertices.get(0);

					final String minValueText = Integer.toString(firstVertex.getValue());

					cell.setText(minValueText);

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Color image
	 */
	private void defineColumn_30_ColorImage() {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_COLOR_IMAGE));

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
	}

	/**
	 * Column: Max value
	 */
	private void defineColumn_40_MaxValue() {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

		final TableColumn tc = tvc.getColumn();
		tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_VALUE));

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final String maxValueText;
					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					final ProfileImage profileImage = colorProfile.getProfileImage();

					final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
					final RGBVertex lastVertex = vertices.get(vertices.size() - 1);

					maxValueText = Integer.toString(lastVertex.getValue());

					cell.setText(maxValueText);

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Relative/absolute values
	 */
	private void defineColumn_50_RelativeAbsolute() {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

		final TableColumn tc = tvc.getColumn();
		tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_ABSOLUTE_RELATIVE));

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isAbsoluteValues()) {
						cell.setText(Messages.Pref_Map3Color_Column_ValueMarker_Absolute);
					} else {
						cell.setText(Messages.Pref_Map3Color_Column_ValueMarker_Relative);
					}

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * Column: Legend overwrite marker
	 */
	private void defineColumn_52_OverwriteLegendMinMax() {

		final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

		final TableColumn tc = tvc.getColumn();
		tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_ABSOLUTE_RELATIVE));

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3GradientColorProvider) {

					final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

					if (colorProfile.isAbsoluteValues() && colorProfile.isOverwriteLegendValues()) {
						cell.setText(Messages.Pref_Map3Color_Column_Legend_Marker);
					} else {
						cell.setText(UI.EMPTY_STRING);
					}

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
	}

	/**
	 * @return Returns <code>true</code> when the colors are disposed, otherwise <code>false</code>.
	 */
	public boolean disposeColors() {

		if (_isInFireEvent) {

			// reload the viewer
			updateUI_colorViewer();

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

	private Image getProfileImage(final Map3GradientColorProvider colorProvider) {

		Image image = _profileImages.get(colorProvider);

		if (isProfileImageValid(image)) {

			// image is OK

		} else {

			final int imageWidth = _tcProfileImage.getWidth();
			final int imageHeight = PROFILE_IMAGE_HEIGHT - 1;

			final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();
			final ArrayList<RGBVertex> rgbVertices = colorProfile.getProfileImage().getRgbVertices();

			colorProvider.configureColorProvider(//
					ColorProviderConfig.MAP3_PROFILE,
					imageWidth,
					rgbVertices,
					false);

			image = TourMapPainter.createMapLegendImage(//
					colorProvider,
					ColorProviderConfig.MAP3_PROFILE,
					imageWidth,
					imageHeight,
					false,
					false,
					false);

			final Image oldImage = _profileImages.put(colorProvider, image);

			Util.disposeResource(oldImage);
		}

		return image;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
//
//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		final int devY = _toolTipItemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		PROFILE_IMAGE_HEIGHT = (int) (_pc.convertHeightInCharsToPixels(1) * 1.0);

		NUMBER_OF_VISIBLE_ROWS = _prefStore.getInt(ITourbookPreferences.MAP3_NUMBER_OF_COLOR_SELECTORS);
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

		return true;
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	public void onDispose() {

		disposeProfileImages();
	}

	private void onResizeImageColumn() {

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
		if (event.index == _columnIndexProfileImage) {

			switch (event.type) {
			case SWT.MeasureItem:

//			event.width += getImageColumnWidth();
//			event.height = PROFILE_IMAGE_HEIGHT;

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

//			System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\tisToolTipVisible: true"));
//			// TODO remove SYSTEM.OUT.PRINTLN

			return;
		}

//		System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tisToolTipVisible: false"));
//		// TODO remove SYSTEM.OUT.PRINTLN

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
				// also select the active (checked) color provider
				_colorViewer.setSelection(new StructuredSelection(selectedColorProvider));
			}
			_isInUIUpdate = false;

			fireModifyEvent();

			return true;
		}

		return false;
	}

	private void updateUI_colorViewer() {

		_colorViewer.setInput(this);

		/*
		 * Select checked color provider that the actions can always be enabled.
		 */
		for (final Map3GradientColorProvider colorProvider : Map3GradientColorManager.getColorProviders(_graphId)) {
			if (colorProvider.getMap3ColorProfile().isActiveColorProfile()) {

				/**
				 * !!! Reveal and table.showSelection() do NOT work !!!
				 */
				_colorViewer.setSelection(new StructuredSelection(colorProvider), true);

				_colorViewer.getTable().showSelection();

				break;
			}
		}
	}

}
