/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.mapprovider;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.tourbook.util.Util;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.IPositionListener;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.IZoomListener;
import de.byteholder.geoclipse.map.event.MapPositionEvent;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.map.event.ZoomEvent;
import de.byteholder.geoclipse.preferences.PrefPageMapProviders;
import de.byteholder.geoclipse.ui.ViewerDetailForm;
import de.byteholder.geoclipse.util.PixelConverter;
import de.byteholder.gpx.GeoPosition;

public class DialogMPProfile extends DialogMP implements ITileListener, IMapDefaultActions {

	private static final int				MAP_MAX_ZOOM_LEVEL						= MP.UI_MAX_ZOOM_LEVEL
																							- MP.UI_MIN_ZOOM_LEVEL;

	public static final String				DEFAULT_URL								= "http://";								//$NON-NLS-1$

	private static final String				DIALOG_SETTINGS_VIEWER_WIDTH			= "ViewerWidth";							//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_SHOW_TILE_INFO		= "IsShowTileInfo";						//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_LIVE_VIEW			= "IsLiveView";							//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG	= "IsShowTileImageLogging";				//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_PROPERTIES_EXPANDED	= "IsPropertiesExpanded";					//$NON-NLS-1$


	/*
	 * UI controls
	 */
	private Display							_display;
	private Button							_btnOk;

	private Composite						_leftContainer;
	private Composite						_innerContainer;
	private ViewerDetailForm				_detailForm;

	private ExpandableComposite				_propContainer;
	private Composite						_propInnerContainer;

	private ContainerCheckedTreeViewer		_treeViewer;
	private TVIMapProviderRoot				_rootItem;

	private ToolBar							_toolbar;
	private Button							_btnShowProfileMap;
	private Button							_btnShowOsmMap;

	private Label							_lblMapInfo;
	private Label							_lblTileInfo;
	private Text							_txtMpUrl;
	private Button							_chkLiveView;
	private Button							_chkShowTileInfo;

	private Spinner							_spinMinZoom;
	private Spinner							_spinMaxZoom;

	private Spinner							_spinAlpha;
	private Scale							_scaleAlpha;
	private Button							_chkTransparentPixel;
	private Button							_chkTransparentBlack;
	private Button							_chkBrightness;
	private Spinner							_spinBright;
	private Scale							_scaleBright;
	private Label							_lblAlpha;

	private ExpandableComposite				_transparentContainer;
	private ColorSelector					_colorSelectorTransparent0;
	private ColorSelector					_colorSelectorTransparent1;
	private ColorSelector					_colorSelectorTransparent2;
	private ColorSelector					_colorSelectorTransparent3;
	private ColorSelector					_colorSelectorTransparent4;
	private ColorSelector					_colorSelectorTransparent5;
	private ColorSelector					_colorSelectorTransparent6;
	private ColorSelector					_colorSelectorTransparent7;
	private ColorSelector					_colorSelectorTransparent8;
	private ColorSelector					_colorSelectorTransparent9;
	private ColorSelector					_colorSelectorTransparent10;
	private ColorSelector					_colorSelectorTransparent11;
	private ColorSelector					_colorSelectorTransparent12;
	private ColorSelector					_colorSelectorTransparent13;
	private ColorSelector					_colorSelectorTransparent14;
	private ColorSelector					_colorSelectorTransparent15;
	private ColorSelector					_colorSelectorTransparent16;
	private ColorSelector					_colorSelectorTransparent17;
	private ColorSelector					_colorSelectorTransparent18;
	private ColorSelector					_colorSelectorTransparent19;
	private ColorSelector					_colorSelectorTransparent20;

	private ColorSelector					_colorImageBackground;

	private final FormToolkit						_formTk									= new FormToolkit(Display
																							.getCurrent());
	private ExpandableComposite				_logContainer;
	private Button							_chkShowTileImageLog;
	private Combo							_cboTileImageLog;
	private Text							_txtLogDetail;

	private Image							_imageMap;
	private Image							_imagePlaceholder;
	private Image							_imageLayer;

	/*
	 * none UI items
	 */

	private final IDialogSettings					_dialogSettings;

	private final MPPlugin						_mpDefault;
	private MPProfile						_mpProfile;

	private String							_defaultMessage;

	// image logging
	private boolean							_isTileImageLogging;
	private final ConcurrentLinkedQueue<LogEntry>	_logEntries								= new ConcurrentLinkedQueue<LogEntry>();

	private int								_statUpdateCounter						= 0;

	private int								_statIsQueued;
	private int								_statStartLoading;
	private int								_statEndLoading;
	private int								_statErrorLoading;

	private long							_dragStartTime;

	private boolean							_isInitUI								= false;
	private boolean							_isLiveView;

	private final NumberFormat					_nfLatLon								= NumberFormat.getNumberInstance();

	{
		// initialize lat/lon formatter
		_nfLatLon.setMinimumFractionDigits(6);
		_nfLatLon.setMaximumFractionDigits(6);
	}

	private class MapContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public DialogMPProfile(	final Shell parentShell,
							final PrefPageMapProviders prefPageMapProvider,
							final MPProfile dialogMapProfile) {

		super(parentShell, dialogMapProfile);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		_dialogSettings = Activator.getDefault().getDialogSettingsSection("DialogMapProfileConfiguration");//$NON-NLS-1$

		_mpProfile = dialogMapProfile;

		/*
		 * disable saving of the profile image (not the child images) when the map is displayed in
		 * this dialoag because this improves performance when the image parameters are modified
		 */
		_mpProfile.setIsSaveImage(false);

		_mpDefault = MapProviderManager.getInstance().getDefaultMapProvider();
	}

	public void actionZoomIn() {
		_map.setZoom(_map.getZoom() + 1);
		_map.queueMapRedraw();
	}

	public void actionZoomOut() {
		_map.setZoom(_map.getZoom() - 1);
		_map.queueMapRedraw();
	}

	public void actionZoomOutToMinZoom() {
		_map.setZoom(_map.getMapProvider().getMinimumZoomLevel());
		_map.queueMapRedraw();
	}

	@Override
	protected void cancelPressed() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				// stop downloading images
				_mpProfile.resetAll(false);
			}
		});

		super.cancelPressed();
	}

	/**
	 * @param mapProvider
	 *            {@link MPWms}
	 * @return Returns <code>true</code> when the wms map provider can be displayed, this is
	 *         possible when a layer is displayed
	 */
	private boolean canWmsBeDisplayed(final MPWms mapProvider) {

		final ArrayList<MtLayer> mtLayers = mapProvider.getMtLayers();
		if (mtLayers == null) {

			// wms is not loaded

			final ArrayList<LayerOfflineData> offlineLayers = mapProvider.getOfflineLayers();
			if (offlineLayers != null) {

				// offline info is available

				for (final LayerOfflineData offlineLayer : offlineLayers) {
					if (offlineLayer.isDisplayedInMap) {
						return true;
					}
				}
			}

		} else {

			// wms is loaded

			for (final MtLayer mtLayer : mtLayers) {
				if (mtLayer.isDisplayedInMap()) {

					// at least one layer is displayed
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean close() {

		// restore default behaviour
		_mpProfile.setIsSaveImage(true);

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_MapProfile_DialogTitle);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	public void create() {

		super.create();

		_display = Display.getCurrent();

		setTitle(Messages.Dialog_MapProfile_DialogArea_Title);

		MP.addTileListener(this);

		restoreState();

		// initialize after the shell size is set
		updateUIFromModel(_mpProfile);

		enableProfileMapButton();
		_treeViewer.getTree().setFocus();
	}

	private void createActions() {

		final ToolBarManager tbm = new ToolBarManager(_toolbar);

		tbm.add(new ActionZoomIn(this));
		tbm.add(new ActionZoomOut(this));
		tbm.add(new ActionZoomOutToMinZoom(this));

		tbm.add(new Separator());

		tbm.add(new ActionShowFavoritePos(this));
		tbm.add(new ActionSetFavoritePosition(this));

		tbm.update(true);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		_btnOk = getButton(IDialogConstants.OK_ID);
		_btnOk.setText(Messages.Dialog_MapConfig_Button_Update);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Control contents = super.createContents(parent);

		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		initializeDialogUnits(parent);
		createResources();

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);
		createActions();

		return dlgContainer;
	}

	private void createResources() {

		_imageMap = Activator.getImageDescriptor(Messages.Image_ViewIcon_Map).createImage();
		_imagePlaceholder = Activator.getImageDescriptor(Messages.Image_ViewIcon_Placeholder16).createImage();
		_imageLayer = Activator.getImageDescriptor(Messages.Image_Action_ZoomShowEntireLayer).createImage();
	}

	private void createUI(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);
		{
			createUI100Container(container, pixelConverter);
			createUI200Log(container, pixelConverter);
		}
	}

	private void createUI100Container(final Composite parent, final PixelConverter pixelConverter) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		{
			// left part (layer selection)
			_leftContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 5, 0, 0).applyTo(_leftContainer);
			createUI110LeftContainer(_leftContainer, pixelConverter);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);
			UI.addSashColorHandler(sash);

			// right part (map)
			final Composite mapContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(mapContainer);
			createUI180Map(mapContainer, pixelConverter);

			_detailForm = new ViewerDetailForm(container, _leftContainer, sash, mapContainer, 30);
		}
	}

	private void createUI110LeftContainer(final Composite parent, final PixelConverter pixelConverter) {

		_innerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_innerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_innerContainer);
		{
			// label: map provider
			final Label label = new Label(_innerContainer, SWT.NONE);
			label.setText(Messages.Dialog_MapConfig_Label_MapProvider);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_HintDragAndDrop);

			createUI114Viewer(_innerContainer, pixelConverter);
			createUI140DialogProperties(_innerContainer);

			/*
			 * section properties
			 */
			final Color parentBackground = parent.getBackground();

			_propContainer = _formTk.createExpandableComposite(_innerContainer, ExpandableComposite.TWISTIE);

			GridDataFactory.fillDefaults().grab(true, false).applyTo(_propContainer);
			GridLayoutFactory.fillDefaults().applyTo(_propContainer);

			_propContainer.setBackground(parentBackground);
			_propContainer.setText(Messages.Dialog_MapConfig_Label_Properties);
			_propContainer.addExpansionListener(new IExpansionListener() {

				public void expansionStateChanged(final ExpansionEvent e) {
					_propContainer.getParent().layout(true);
				}

				public void expansionStateChanging(final ExpansionEvent e) {}
			});

			{
				_propInnerContainer = _formTk.createComposite(_propContainer);
				_propContainer.setClient(_propInnerContainer);

				GridDataFactory.fillDefaults().grab(true, false).applyTo(_propInnerContainer);
				GridLayoutFactory.fillDefaults().applyTo(_propInnerContainer);

				_propInnerContainer.setBackground(parentBackground);
				{
					createUI120MapProviderProperties(_propInnerContainer);
					createUI130ProfileProperties(_propInnerContainer);
				}
			}
		}
	}

	private Control createUI114Viewer(final Composite parent, final PixelConverter pixelConverter) {

		final TreeColumnLayout treeLayout = new TreeColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(treeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);
		{

			final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);

			tree.setHeaderVisible(true);
			tree.setLinesVisible(true);

			/*
			 * tree viewer
	 		 */
			_treeViewer = new ContainerCheckedTreeViewer(tree);

			_treeViewer.setContentProvider(new MapContentProvider());
			_treeViewer.setUseHashlookup(true);

			_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(final DoubleClickEvent event) {

					final Object selectedItem = ((IStructuredSelection) _treeViewer.getSelection()).getFirstElement();
					if (selectedItem != null) {

						if (selectedItem instanceof TVIMapProvider) {

							// expand/collapse current item

							final MP mapProvider = ((TVIMapProvider) selectedItem).getMapProviderWrapper().getMP();

							if ((mapProvider instanceof MPWms) == false) {

								// all none wms map provider can be toggled

								toggleMapVisibility(tree);

							} else {

								// expand/collapse item

								if (_treeViewer.getExpandedState(selectedItem)) {
									_treeViewer.collapseToLevel(selectedItem, 1);
								} else {
									_treeViewer.expandToLevel(selectedItem, 1);
								}
							}

						} else if (selectedItem instanceof TVIWmsLayer) {
							toggleMapVisibility(tree);
						}
					}
				}
			});

			_treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(final SelectionChangedEvent event) {
					onSelectMP(event.getSelection());
				}
			});

			_treeViewer.addDragSupport(
					DND.DROP_MOVE,
					new Transfer[] { LocalSelectionTransfer.getTransfer() },
					new DragSourceListener() {

						public void dragFinished(final DragSourceEvent event) {

							final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

							if (event.doit == false) {
								return;
							}

							transfer.setSelection(null);
							transfer.setSelectionSetTime(0);
						}

						public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
						}

						public void dragStart(final DragSourceEvent event) {

							final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
							final ITreeSelection selection = (ITreeSelection) _treeViewer.getSelection();

							transfer.setSelection(selection);
							transfer.setSelectionSetTime(_dragStartTime = event.time & 0xFFFFFFFFL);

							// only ONE map provider/layer is allowed to be dragged
							final Object firstElement = selection.getFirstElement();
							event.doit = (selection.size() == 1)
									&& ((firstElement instanceof TVIMapProvider) || (firstElement instanceof TVIWmsLayer));
						}
					});

			_treeViewer.addDropSupport(
					DND.DROP_MOVE,
					new Transfer[] { LocalSelectionTransfer.getTransfer() },
					new ProfileDropAdapter(this, _treeViewer));

			tree.addKeyListener(new KeyListener() {

				public void keyPressed(final KeyEvent e) {

					/*
					 * toggle the visibility with the space key
					 */
					if (e.keyCode == ' ') {
						toggleMapVisibility(tree);
					}
				}

				public void keyReleased(final KeyEvent e) {}
			});

			/*
			 * add editing support for the tree
			 */
			final TreeViewerFocusCellManager focusCellManager = new TreeViewerFocusCellManager(
					_treeViewer,
					new FocusCellOwnerDrawHighlighter(_treeViewer));

			final ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
					_treeViewer) {

				@Override
				protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {
					return (event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL)
							|| (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION)
							|| ((event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED) && (event.keyCode == SWT.F2))
							|| (event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC);
				}
			};

			TreeViewerEditor.create(_treeViewer, //
					focusCellManager,
					actSupport,
					ColumnViewerEditor.TABBING_HORIZONTAL //
							| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR //
							| ColumnViewerEditor.TABBING_VERTICAL
							| ColumnViewerEditor.KEYBOARD_ACTIVATION);

		}

		createUI116ViewerColumns(treeLayout, pixelConverter);

		return layoutContainer;
	}

	/**
	 * create columns for the tree viewer
	 * 
	 * @param pixelConverter
	 */
	private void createUI116ViewerColumns(final TreeColumnLayout treeLayout, final PixelConverter pixelConverter) {

		TreeViewerColumn tvc;
		TreeColumn tc;

		/*
		 * column: map provider
		 */
		tvc = new TreeViewerColumn(_treeViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Dialog_MapProfile_Column_MapProvider);
		tc.setToolTipText(Messages.Dialog_MapProfile_Column_MapProvider_Tooltip);
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final StyledString styledString = new StyledString();
				final Object element = cell.getElement();

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();
					final MP mapProvider = mpWrapper.getMP();

					styledString.append(mapProvider.getName());

					cell.setImage(mpWrapper.isDisplayedInMap() ? _imageMap : _imagePlaceholder);

				} else if (element instanceof TVIWmsLayer) {

					final MtLayer mtLayer = ((TVIWmsLayer) element).getMtLayer();

					styledString.append(mtLayer.getGeoLayer().getTitle());

					styledString.append("  (", StyledString.QUALIFIER_STYLER);//$NON-NLS-1$
					styledString.append(mtLayer.getGeoLayer().getName(), StyledString.QUALIFIER_STYLER);
					styledString.append(")", StyledString.QUALIFIER_STYLER);//$NON-NLS-1$

					cell.setImage(mtLayer.isDisplayedInMap() ? _imageLayer : _imagePlaceholder);

				} else {
					styledString.append(element.toString());
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		});
		treeLayout.setColumnData(tc, new ColumnWeightData(100, true));

		/*
		 * column: is visible
		 */
		tvc = new TreeViewerColumn(_treeViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Dialog_MapProfile_Column_IsVisible);
		tc.setToolTipText(Messages.Dialog_MapProfile_Column_IsVisible_Tooltip);
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();

					cell.setText(Boolean.toString(mpWrapper.isDisplayedInMap()));

				} else if (element instanceof TVIWmsLayer) {

					final MtLayer mtLayer = ((TVIWmsLayer) element).getMtLayer();

					cell.setText(Boolean.toString(mtLayer.isDisplayedInMap()));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		tvc.setEditingSupport(new EditingSupport(_treeViewer) {

			private final CheckboxCellEditor	fCellEditor	= new CheckboxCellEditor(_treeViewer.getTree());

			@Override
			protected boolean canEdit(final Object element) {

				if (element instanceof TVIMapProvider) {

					final TVIMapProvider tvi = (TVIMapProvider) element;
					final MP mapProvider = tvi.getMapProviderWrapper().getMP();

					if (mapProvider instanceof MPWms) {

						// wms can be toggled when at least one layer is displayed

						return canWmsBeDisplayed((MPWms) mapProvider);
					}
				}

				return true;
			}

			@Override
			protected CellEditor getCellEditor(final Object element) {
				return fCellEditor;
			}

			@Override
			protected Object getValue(final Object element) {

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();

					return mpWrapper.isDisplayedInMap();

				} else if (element instanceof TVIWmsLayer) {

					final MtLayer mtLayer = ((TVIWmsLayer) element).getMtLayer();

					return mtLayer.isDisplayedInMap();
				}

				return null;
			}

			@Override
			protected void setValue(final Object element, final Object value) {

				final boolean isChecked = ((Boolean) value);

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();

					mpWrapper.setIsDisplayedInMap(isChecked);

					if (isChecked) {

						/*
						 * remove parent tiles from loading cache because they can have loading
						 * errors (from their children) which prevents them to be loaded again
						 */
						_mpProfile.resetParentTiles();
					}

					enableProfileMapButton();

				} else if (element instanceof TVIWmsLayer) {

					final TVIWmsLayer tviLayer = (TVIWmsLayer) element;
					final MtLayer mtLayer = tviLayer.getMtLayer();

					mtLayer.setIsDisplayedInMap(isChecked);

					updateMVMapProvider(tviLayer);
				}

				// update viewer
				getViewer().update(element, null);

				updateLiveView();
			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10)));

		/*
		 * column: alpha
		 */
		tvc = new TreeViewerColumn(_treeViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Dialog_MapProfile_Column_Alpha);
		tc.setToolTipText(Messages.Dialog_MapProfile_Column_Alpha_Tooltip);
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();

					cell.setText(Integer.toString(mpWrapper.getAlpha()));

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10)));

		/*
		 * column: brightness
		 */
		tvc = new TreeViewerColumn(_treeViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Dialog_MapProfile_Column_Brightness);
		tc.setToolTipText(Messages.Dialog_MapProfile_Column_Brightness_Tooltip);
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();

					cell.setText(mpWrapper.isBrightnessForNextMp()
							? Integer.toString(mpWrapper.getBrightnessValueForNextMp())
							: UI.EMPTY_STRING);

				} else {

					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10)));

		/*
		 * column: empty to prevent scrolling to the right when the right column is selected
		 */
		tvc = new TreeViewerColumn(_treeViewer, SWT.LEAD);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
			/*
			 * !!! label provider is necessary to prevent a NPE !!!
			 */
			}
		});
		tc = tvc.getColumn();
		treeLayout.setColumnData(tc, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(4)));

	}

	private void createUI120MapProviderProperties(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_MapConfig_Group_MapProvider);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			// check: brightness
			_chkBrightness = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkBrightness);
			_chkBrightness.setText(Messages.Dialog_MapProfile_Button_Brightness);
			_chkBrightness.setToolTipText(Messages.Dialog_MapProfile_Button_Brightness_Tooltip);
			_chkBrightness.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateMVBrightness();
				}
			});

			// scale: brightness
			_scaleBright = new Scale(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleBright);
			_scaleBright.setMinimum(0);
			_scaleBright.setMaximum(100);
			_scaleBright.setToolTipText(Messages.Dialog_MapProfile_Scale_Brightness_Tooltip);
			_scaleBright.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyBrightScale();
				}
			});

			// spinner: brightness
			_spinBright = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults()//
//					.grab(false, true)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinBright);
			_spinBright.setMinimum(0);
			_spinBright.setMaximum(100);
			_spinBright.setToolTipText(Messages.Dialog_MapProfile_Scale_Brightness_Tooltip);

			_spinBright.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			_spinBright.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyBrightSpinner();
				}
			});

			_spinBright.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					onModifyBrightSpinner();
				}
			});

			// ################################################

			// label: alpha
			_lblAlpha = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_lblAlpha);
			_lblAlpha.setText(Messages.Dialog_CustomConfig_Label_Alpha);
			_lblAlpha.setToolTipText(Messages.Dialog_CustomConfig_Label_Alpha_Tooltip);

			// scale: alpha
			_scaleAlpha = new Scale(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleAlpha);
			_scaleAlpha.setMinimum(0);
			_scaleAlpha.setMaximum(100);
			_scaleAlpha.setToolTipText(Messages.Dialog_CustomConfig_Label_Alpha_Tooltip);
			_scaleAlpha.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyAlphaScale();
				}
			});

			/*
			 * this do not work !!!
			 */
//			fScaleAlpha.addMouseWheelListener(new MouseWheelListener() {
//				public void mouseScrolled(final MouseEvent event) {
//
//					final int newValue = Util.adjustScaleValueOnMouseScroll(event);
//
//					fIsInitUI = true;
////					{
////						fScaleAlpha.setSelection(newValue);
////					}
//					fIsInitUI = false;
//
////					onModifyAlphaScale();
//				}
//			});

			// center scale on mouse double click
			_scaleAlpha.addListener(SWT.MouseDoubleClick, new Listener() {
				public void handleEvent(final Event event) {
					onScaleDoubleClick(event.widget);
				}
			});

			// spinner: alpha
			_spinAlpha = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_spinAlpha);
			_spinAlpha.setMinimum(0);
			_spinAlpha.setMaximum(100);
			_spinAlpha.setToolTipText(Messages.Dialog_CustomConfig_Label_Alpha_Tooltip);

			_spinAlpha.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			_spinAlpha.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyAlphaSpinner();
				}
			});

			_spinAlpha.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					onModifyAlphaSpinner();
				}
			});

			// ################################################

			final Composite containerOptions = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(containerOptions);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerOptions);
			{
				// check: set transparent pixel
				_chkTransparentPixel = new Button(containerOptions, SWT.CHECK);
				_chkTransparentPixel.setText(Messages.Dialog_MapConfig_Button_TransparentPixel);
				_chkTransparentPixel.setToolTipText(Messages.Dialog_MapConfig_Button_TransparentPixel_Tooltip);
				_chkTransparentPixel.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyTransparentColor();
					}
				});

				// check: is black transparent
				_chkTransparentBlack = new Button(containerOptions, SWT.CHECK);
				_chkTransparentBlack.setText(Messages.Dialog_MapConfig_Button_TransparentBlack);
				_chkTransparentBlack.setToolTipText(Messages.Dialog_MapConfig_Button_TransparentBlack_Tooltip);
				_chkTransparentBlack.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyTransparentColor();
					}
				});
			}

			// ################################################

			createUI122ColorSelector1(group);

			// ################################################

			// text: map provider url
			_txtMpUrl = new Text(group, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_txtMpUrl);
			_txtMpUrl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}

	private void createUI122ColorSelector1(final Composite parent) {

		// transparent color selectors

		final Color parentBackground = parent.getBackground();

		_transparentContainer = _formTk.createExpandableComposite(parent, ExpandableComposite.NO_TITLE);

		// prevent flickering in the UI
		_transparentContainer.setExpanded(false);

		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_transparentContainer);
		GridLayoutFactory.fillDefaults().applyTo(_transparentContainer);

		_transparentContainer.setBackground(parentBackground);
		_transparentContainer.addExpansionListener(new IExpansionListener() {

			public void expansionStateChanged(final ExpansionEvent e) {
//				fPropInnerContainer.layout(true);
				_innerContainer.layout(true);
			}

			public void expansionStateChanging(final ExpansionEvent e) {}
		});

		{
			final Composite clientContainer = _formTk.createComposite(_transparentContainer);
			_transparentContainer.setClient(clientContainer);

			GridDataFactory.fillDefaults().grab(true, false).applyTo(clientContainer);
			GridLayoutFactory.fillDefaults().applyTo(clientContainer);

			clientContainer.setBackground(parentBackground);

			{
				createUI124ColorSelector2(clientContainer);
			}
		}

	}

	private void createUI124ColorSelector2(final Composite parent) {

		final IPropertyChangeListener colorListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onModifyTransparentColor();
			}
		};

		final GridDataFactory gd = GridDataFactory.swtDefaults().grab(false, true).align(SWT.BEGINNING, SWT.BEGINNING);

		final Composite colorContainerParent = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(colorContainerParent);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(colorContainerParent);
		{
			final Composite colorContainer0 = new Composite(colorContainerParent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(7).spacing(0, 0).applyTo(colorContainer0);
			{
				_colorSelectorTransparent0 = createUIColorSelector(colorContainer0, colorListener, gd);
				_colorSelectorTransparent1 = createUIColorSelector(colorContainer0, colorListener, gd);
				_colorSelectorTransparent2 = createUIColorSelector(colorContainer0, colorListener, gd);
				_colorSelectorTransparent3 = createUIColorSelector(colorContainer0, colorListener, gd);
				_colorSelectorTransparent4 = createUIColorSelector(colorContainer0, colorListener, gd);
				_colorSelectorTransparent5 = createUIColorSelector(colorContainer0, colorListener, gd);
				_colorSelectorTransparent6 = createUIColorSelector(colorContainer0, colorListener, gd);
			}

			final Composite colorContainer1 = new Composite(colorContainerParent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(colorContainer1);
			GridLayoutFactory.fillDefaults().numColumns(7).spacing(0, 0).applyTo(colorContainer1);
			{
				_colorSelectorTransparent7 = createUIColorSelector(colorContainer1, colorListener, gd);
				_colorSelectorTransparent8 = createUIColorSelector(colorContainer1, colorListener, gd);
				_colorSelectorTransparent9 = createUIColorSelector(colorContainer1, colorListener, gd);
				_colorSelectorTransparent10 = createUIColorSelector(colorContainer1, colorListener, gd);
				_colorSelectorTransparent11 = createUIColorSelector(colorContainer1, colorListener, gd);
				_colorSelectorTransparent12 = createUIColorSelector(colorContainer1, colorListener, gd);
				_colorSelectorTransparent13 = createUIColorSelector(colorContainer1, colorListener, gd);
			}

			final Composite colorContainer2 = new Composite(colorContainerParent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(colorContainer2);
			GridLayoutFactory.fillDefaults().numColumns(7).spacing(0, 0).applyTo(colorContainer2);
			{
				_colorSelectorTransparent14 = createUIColorSelector(colorContainer2, colorListener, gd);
				_colorSelectorTransparent15 = createUIColorSelector(colorContainer2, colorListener, gd);
				_colorSelectorTransparent16 = createUIColorSelector(colorContainer2, colorListener, gd);
				_colorSelectorTransparent17 = createUIColorSelector(colorContainer2, colorListener, gd);
				_colorSelectorTransparent18 = createUIColorSelector(colorContainer2, colorListener, gd);
				_colorSelectorTransparent19 = createUIColorSelector(colorContainer2, colorListener, gd);
				_colorSelectorTransparent20 = createUIColorSelector(colorContainer2, colorListener, gd);
			}
		}
	}

	private void createUI130ProfileProperties(final Composite parent) {

		Label label;
		final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {

				Util.adjustSpinnerValueOnMouseScroll(event);

				// validate values
				if (event.widget == _spinMinZoom) {
					onModifyZoomSpinnerMin();
				} else {
					onModifyZoomSpinnerMax();
				}
			}
		};

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_MapConfig_Group_Profile);
		GridDataFactory.fillDefaults().applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			// label: min zoomlevel
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_CustomConfig_Label_ZoomLevel);

			// ------------------------------------------------

			final Composite zoomContainer = new Composite(group, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(zoomContainer);
			{
				// spinner: min zoom level
				_spinMinZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(_spinMinZoom);
				_spinMinZoom.setMinimum(MP.UI_MIN_ZOOM_LEVEL);
				_spinMinZoom.setMaximum(MP.UI_MAX_ZOOM_LEVEL);
				_spinMinZoom.setSelection(MP.UI_MIN_ZOOM_LEVEL);
				_spinMinZoom.addMouseWheelListener(mouseWheelListener);
				_spinMinZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (_isInitUI) {
							return;
						}
						onModifyZoomSpinnerMin();
					}
				});
				_spinMinZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (_isInitUI) {
							return;
						}
						onModifyZoomSpinnerMin();
					}
				});

				// ------------------------------------------------

				label = new Label(zoomContainer, SWT.NONE);
				label.setText("..."); //$NON-NLS-1$

				// ------------------------------------------------

				// spinner: min zoom level
				_spinMaxZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(_spinMaxZoom);
				_spinMaxZoom.setMinimum(MP.UI_MIN_ZOOM_LEVEL);
				_spinMaxZoom.setMaximum(MP.UI_MAX_ZOOM_LEVEL);
				_spinMaxZoom.setSelection(MP.UI_MAX_ZOOM_LEVEL);
				_spinMaxZoom.addMouseWheelListener(mouseWheelListener);
				_spinMaxZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (_isInitUI) {
							return;
						}
						onModifyZoomSpinnerMax();
					}
				});
				_spinMaxZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (_isInitUI) {
							return;
						}
						onModifyZoomSpinnerMax();
					}
				});
			}

			// ################################################

			// label: image background color
			label = new Label(group, SWT.CHECK);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.Dialog_MapConfig_Label_ImageBackgroundColor);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_ImageBackgroundColor_Tooltip);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_ImageBackgroundColor_Tooltip);

			// ------------------------------------------------

			// color: image bg color
			_colorImageBackground = new ColorSelector(group);
			GridDataFactory.swtDefaults()//
					.grab(false, true)
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.applyTo(_colorImageBackground.getButton());

			_colorImageBackground.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onModifyImageBgColor();
				}
			});
		}
	}

	private void createUI140DialogProperties(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			// check: live view
			_chkLiveView = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.END)
					.applyTo(_chkLiveView);
			_chkLiveView.setText(Messages.Dialog_MapConfig_Button_LiveView);
			_chkLiveView.setToolTipText(Messages.Dialog_MapConfig_Button_LiveView_Tooltip);
			_chkLiveView.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					_isLiveView = _chkLiveView.getSelection();
					_map.setLiveView(_isLiveView);

					updateLiveView();
				}
			});

			// ################################################

			// check: show tile info
			_chkShowTileInfo = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.END)
					.applyTo(_chkShowTileInfo);
			_chkShowTileInfo.setText(Messages.Dialog_MapConfig_Button_ShowTileInfo);
			_chkShowTileInfo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					_map.setShowDebugInfo(_chkShowTileInfo.getSelection());
				}
			});

			// ############################################################

			// check: show tile image loading log
			_chkShowTileImageLog = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.applyTo(_chkShowTileImageLog);
			_chkShowTileImageLog.setText(Messages.Dialog_MapConfig_Button_ShowTileLog);
			_chkShowTileImageLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
			_chkShowTileImageLog.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableControls();
				}
			});
		}
	}

	private void createUI180Map(final Composite parent, final PixelConverter pixelConverter) {

		final Composite toolbarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbarContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(toolbarContainer);
		{
			// button: update map
			_btnShowProfileMap = new Button(toolbarContainer, SWT.NONE);
			_btnShowProfileMap.setText(Messages.Dialog_MapProfile_Button_UpdateMap);
			_btnShowProfileMap.setToolTipText(Messages.Dialog_MapProfile_Button_UpdateMap_Tooltip);
			_btnShowProfileMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectMapProfile(true);
				}
			});

			// ############################################################

			// button: osm map
			_btnShowOsmMap = new Button(toolbarContainer, SWT.NONE);
			_btnShowOsmMap.setText(Messages.Dialog_MapConfig_Button_ShowOsmMap);
			_btnShowOsmMap.setToolTipText(Messages.Dialog_MapConfig_Button_ShowOsmMap_Tooltip);
			_btnShowOsmMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectMapOSM();
				}
			});

			// ############################################################

			_toolbar = new ToolBar(toolbarContainer, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.END, SWT.FILL)
					.applyTo(_toolbar);
		}

		_map = new Map(parent, SWT.BORDER | SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_map);

		_map.setShowScale(true);

		_map.addZoomListener(new IZoomListener() {
			public void zoomChanged(final ZoomEvent event) {
			// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
//				resetMapProfile();
			// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
			}
		});

		_map.addMousePositionListener(new IPositionListener() {

			public void setPosition(final MapPositionEvent event) {

				final GeoPosition mousePosition = event.mapGeoPosition;

				double lon = mousePosition.longitude % 360;
				lon = lon > 180 ? //
						lon - 360
						: lon < -180 ? //
								lon + 360
								: lon;

				_lblMapInfo.setText(NLS.bind(Messages.Dialog_MapConfig_Label_MapInfo, new Object[] {
						_nfLatLon.format(mousePosition.latitude),
						_nfLatLon.format(lon),
						Integer.toString(event.mapZoomLevel + 1) }));
			}
		});

		/*
		 * tile and map info
		 */
		final Composite infoContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(infoContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(infoContainer);
		{
			// label: map info
			_lblMapInfo = new Label(infoContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblMapInfo);

			// label: tile info
			_lblTileInfo = new Label(infoContainer, SWT.TRAIL);
			GridDataFactory.fillDefaults().hint(pixelConverter.convertWidthInCharsToPixels(25), SWT.DEFAULT).applyTo(
					_lblTileInfo);
			_lblTileInfo.setToolTipText(Messages.Dialog_MapConfig_TileInfo_Tooltip_Line1
					+ Messages.Dialog_MapConfig_TileInfo_Tooltip_Line2
					+ Messages.Dialog_MapConfig_TileInfo_Tooltip_Line3);
		}

		/*
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * !!! don't do any map initialization until the map provider is set !!!
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
	}

	private void createUI200Log(final Composite parent, final PixelConverter pixelConverter) {

		final Font monoFont = getMonoFont();
		final Color parentBackground = parent.getBackground();

		_logContainer = _formTk.createExpandableComposite(parent, ExpandableComposite.TWISTIE);

		GridDataFactory.fillDefaults().grab(true, false).applyTo(_logContainer);
		GridLayoutFactory.fillDefaults().applyTo(_logContainer);

		_logContainer.setBackground(parentBackground);
		_logContainer.setText(Messages.Dialog_MapConfig_Label_LoadedImageUrl);
		_logContainer.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
		_logContainer.addExpansionListener(new IExpansionListener() {

			public void expansionStateChanged(final ExpansionEvent e) {
				_logContainer.getParent().layout(true);
			}

			public void expansionStateChanging(final ExpansionEvent e) {}
		});

		{
			final Composite clientContainer = _formTk.createComposite(_logContainer);
			_logContainer.setClient(clientContainer);

			GridDataFactory.fillDefaults().grab(true, false).applyTo(clientContainer);
			GridLayoutFactory.fillDefaults().applyTo(clientContainer);

			clientContainer.setBackground(parentBackground);

			{
				// combo: url log
				_cboTileImageLog = new Combo(clientContainer, SWT.READ_ONLY);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_cboTileImageLog);
				_cboTileImageLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
				_cboTileImageLog.setVisibleItemCount(40);
				_formTk.adapt(_cboTileImageLog, true, true);
				_cboTileImageLog.setFont(monoFont);

				_cboTileImageLog.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						// display selected item in the text field below
						final int selectionIndex = _cboTileImageLog.getSelectionIndex();
						if (selectionIndex != -1) {
							_txtLogDetail.setText(_cboTileImageLog.getItem(selectionIndex));
						}
					}
				});

				// label: selected log entry
				_txtLogDetail = new Text(clientContainer, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(5))
						.applyTo(_txtLogDetail);
				_formTk.adapt(_txtLogDetail, false, false);
				_txtLogDetail.setFont(monoFont);
				_txtLogDetail.setBackground(parentBackground);
			}
		}
	}

	private ColorSelector createUIColorSelector(final Composite parent,
												final IPropertyChangeListener colorListener,
												final GridDataFactory gd) {

		final ColorSelector colorSelector = new ColorSelector(parent);

		colorSelector.addListener(colorListener);
		gd.applyTo(colorSelector.getButton());

		return colorSelector;
	}

	private void enableControls() {

		final ITreeSelection selection = (ITreeSelection) _treeViewer.getSelection();
		final Object firstElement = selection.getFirstElement();

		boolean isMpSelected = true;

		if (firstElement instanceof TVIWmsLayer) {

			// wms layer is selected, disable map provider controls

			isMpSelected = false;
		}

		final boolean isBrightness = isMpSelected & _chkBrightness.getSelection();
		final boolean isNoBrightness = isMpSelected & !isBrightness;
		final boolean isTransparent = isMpSelected & _chkTransparentPixel.getSelection() & !isBrightness;

		_chkBrightness.setEnabled(isMpSelected);
		_spinBright.setEnabled(isBrightness);
		_scaleBright.setEnabled(isBrightness);

		_lblAlpha.setEnabled(isNoBrightness);
		_spinAlpha.setEnabled(isNoBrightness);
		_scaleAlpha.setEnabled(isNoBrightness);

		/*
		 * transparent pixel
		 */
		_chkTransparentPixel.setEnabled(isNoBrightness);

		_chkTransparentBlack.setEnabled(isTransparent);
		_colorSelectorTransparent0.setEnabled(isTransparent);
		_colorSelectorTransparent1.setEnabled(isTransparent);
		_colorSelectorTransparent2.setEnabled(isTransparent);
		_colorSelectorTransparent3.setEnabled(isTransparent);
		_colorSelectorTransparent4.setEnabled(isTransparent);
		_colorSelectorTransparent5.setEnabled(isTransparent);
		_colorSelectorTransparent6.setEnabled(isTransparent);
		_colorSelectorTransparent7.setEnabled(isTransparent);
		_colorSelectorTransparent8.setEnabled(isTransparent);
		_colorSelectorTransparent9.setEnabled(isTransparent);
		_colorSelectorTransparent10.setEnabled(isTransparent);
		_colorSelectorTransparent11.setEnabled(isTransparent);
		_colorSelectorTransparent12.setEnabled(isTransparent);
		_colorSelectorTransparent13.setEnabled(isTransparent);
		_colorSelectorTransparent14.setEnabled(isTransparent);
		_colorSelectorTransparent15.setEnabled(isTransparent);
		_colorSelectorTransparent16.setEnabled(isTransparent);
		_colorSelectorTransparent17.setEnabled(isTransparent);
		_colorSelectorTransparent18.setEnabled(isTransparent);
		_colorSelectorTransparent19.setEnabled(isTransparent);
		_colorSelectorTransparent20.setEnabled(isTransparent);

		// check if the container must be expanded/collapsed
		final boolean isTransExpanded = _transparentContainer.isExpanded();

		if (((isTransExpanded == true) && (isTransparent == false)) || //
				((isTransExpanded == false) && (isTransparent == true))) {

			// show/hide transparent color section
			_transparentContainer.setExpanded(isTransparent);
//			fPropInnerContainer.layout(true);
			_innerContainer.layout(true);
		}

		/*
		 * image logging
		 */
		_isTileImageLogging = _chkShowTileImageLog.getSelection();

		if (_isTileImageLogging == false) {
			// remove old log entries
			_statUpdateCounter = 0;
			_cboTileImageLog.removeAll();
			_txtLogDetail.setText(UI.EMPTY_STRING);
		}

		_cboTileImageLog.setEnabled(_isTileImageLogging);
		_txtLogDetail.setEnabled(_isTileImageLogging);

		// check if the container must be expanded/collapsed
		final boolean isLogExpanded = _logContainer.isExpanded();

		if (((isLogExpanded == true) && (_isTileImageLogging == false))
				|| ((isLogExpanded == false) && (_isTileImageLogging == true))) {

			// show/hide log section
			_logContainer.setExpanded(_isTileImageLogging);
			_logContainer.getParent().layout(true);
		}
	}

	/**
	 * Enable profile map button when at least one map provider is displayed
	 */
	private void enableProfileMapButton() {

		boolean isEnabled = false;

		// check if a wrapper is displayed and enabled
		for (final MPWrapper mpWrapper : _mpProfile.getAllWrappers()) {
			if (mpWrapper.isDisplayedInMap() && mpWrapper.isEnabled()) {
				isEnabled = true;
				break;
			}
		}

		_btnShowProfileMap.setEnabled(isEnabled);
	}

	/**
	 * @param colorSelector
	 * @return Returns the color value of the {@link ColorSelector} or -1 when the color is black
	 */
	private int getColorValue(final ColorSelector colorSelector) {

		final RGB rgb = colorSelector.getColorValue();

		colorSelector.getButton().setToolTipText(rgb.toString());

		final int colorValue = ((rgb.red & 0xFF) << 0) | ((rgb.green & 0xFF) << 8) | ((rgb.blue & 0xFF) << 16);

		return colorValue == 0 ? -1 : colorValue;
	}


	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position

		try {

			// Get the stored width
			_dialogSettings.getInt(UI.DIALOG_WIDTH);

		} catch (final NumberFormatException e) {

			// dialog width is not yet set, set default size

			_dialogSettings.put(UI.DIALOG_WIDTH, 850);
			_dialogSettings.put(UI.DIALOG_HEIGHT, 700);
		}

		return _dialogSettings;
 
		// disable bounds
//		return null;
	}

//	private void createUIBBoxTest(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		{
//			Label label = new Label(container, SWT.NONE);
//			label.setText("top:");
//
//			fSpinnerBboxTop = new Spinner(container, SWT.BORDER);
//			GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(fSpinnerBboxTop);
//			fSpinnerBboxTop.setMinimum(100000);
//			fSpinnerBboxTop.setMaximum(10000000);
//			fSpinnerBboxTop.setSelection(100000);
//			fSpinnerBboxTop.addMouseWheelListener(new MouseWheelListener() {
//				public void mouseScrolled(final MouseEvent event) {
//					Util.adjustSpinnerValueOnMouseScroll(event);
//				}
//			});
//
//			fSpinnerBboxTop.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onModifyBbox();
//				}
//			});
//
//			fSpinnerBboxTop.addModifyListener(new ModifyListener() {
//				public void modifyText(final ModifyEvent e) {
//					onModifyBbox();
//				}
//			});
//
//			label = new Label(container, SWT.NONE);
//			label.setText("bottom:");
//
//			fSpinnerBboxBottom = new Spinner(container, SWT.BORDER);
//			GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(fSpinnerBboxBottom);
//			fSpinnerBboxBottom.setMinimum(100000);
//			fSpinnerBboxBottom.setMaximum(10000000);
//			fSpinnerBboxBottom.setSelection(100000);
//			fSpinnerBboxBottom.addMouseWheelListener(new MouseWheelListener() {
//				public void mouseScrolled(final MouseEvent event) {
//					Util.adjustSpinnerValueOnMouseScroll(event);
//				}
//			});
//
//			fSpinnerBboxBottom.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onModifyBbox();
//				}
//			});
//
//			fSpinnerBboxBottom.addModifyListener(new ModifyListener() {
//				public void modifyText(final ModifyEvent e) {
//					onModifyBbox();
//				}
//			});
//		}
//	}

	long getDragStartTime() {
		return _dragStartTime;
	}

	MPProfile getMpProfile() {
		return _mpProfile;
	}

	private String getMpUrl(final MP mp) {

		if (mp instanceof MPWms) {

			// wms map provider

			return ((MPWms) mp).getCapabilitiesUrl();

		} else if (mp instanceof MPCustom) {

			// custom map provider

			return ((MPCustom) mp).getCustomUrl();

		} else if (mp instanceof MPPlugin) {

			// plugin map provider

			final MPPlugin pluginMapProvider = (MPPlugin) mp;

			final String baseURL = pluginMapProvider.getBaseURL();
			return baseURL == null ? UI.EMPTY_STRING : baseURL;
		}

		return UI.EMPTY_STRING;
	}

	private RGB getRGB(final int colorValue) {

		final int red = (colorValue & 0xFF) >>> 0;
		final int green = (colorValue & 0xFF00) >>> 8;
		final int blue = (colorValue & 0xFF0000) >>> 16;

		return new RGB(red, green, blue);
	}

	public TVIMapProviderRoot getRootItem() {
		return _rootItem;
	}

	@Override
	protected void okPressed() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				// stop downloading images
				_mpProfile.resetAll(false);

				// model is saved in the dialog opening code
				updateModelFromUI();
			}
		});

		super.okPressed();
	}

	private void onDispose() {

		MP.removeTileListener(DialogMPProfile.this);

		if (_imageMap != null) {
			_imageMap.dispose();
		}
		if (_imagePlaceholder != null) {
			_imagePlaceholder.dispose();
		}
		if (_imageLayer != null) {
			_imageLayer.dispose();
		}
		if (_formTk != null) {
			_formTk.dispose();
		}
	}

	private void onModifyAlphaScale() {

		if (_isInitUI) {
			return;
		}

		_isInitUI = true;
		{
			_spinAlpha.setSelection(_scaleAlpha.getSelection());
		}
		_isInitUI = false;

		updateMVAlpha();
		updateLiveView();
	}

	private void onModifyAlphaSpinner() {

		if (_isInitUI) {
			return;
		}

		_isInitUI = true;
		{
			_scaleAlpha.setSelection(_spinAlpha.getSelection());
		}
		_isInitUI = false;

		updateMVAlpha();
		updateLiveView();
	}

	private void onModifyBrightScale() {

		if (_isInitUI) {
			return;
		}

		_isInitUI = true;
		{
			_spinBright.setSelection(_scaleBright.getSelection());
		}
		_isInitUI = false;

		updateMVBrightness();
	}

	private void onModifyBrightSpinner() {

		if (_isInitUI) {
			return;
		}

		_isInitUI = true;
		{
			_scaleBright.setSelection(_spinBright.getSelection());
		}
		_isInitUI = false;

		updateMVBrightness();
	}

	private void onModifyImageBgColor() {

		_mpProfile.setBackgroundColor(_colorImageBackground.getColorValue());

		updateLiveView();
	}

	private void onModifyProperties() {

		onModifyAlphaScale();

	}

	private void onModifyTransparentColor() {

		final Object firstElement = ((StructuredSelection) _treeViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TVIMapProvider) {

			// map provider is selected

			final TVIMapProvider tviMapProvider = (TVIMapProvider) firstElement;

			// update alpha in the map provider

			final MPWrapper mpWrapper = tviMapProvider.getMapProviderWrapper();
			final boolean isBlack = _chkTransparentBlack.getSelection();

			// update model

			final int[] colorValues = new int[22];

			colorValues[0] = getColorValue(_colorSelectorTransparent0);
			colorValues[1] = getColorValue(_colorSelectorTransparent1);
			colorValues[2] = getColorValue(_colorSelectorTransparent2);
			colorValues[3] = getColorValue(_colorSelectorTransparent3);
			colorValues[4] = getColorValue(_colorSelectorTransparent4);
			colorValues[5] = getColorValue(_colorSelectorTransparent5);
			colorValues[6] = getColorValue(_colorSelectorTransparent6);
			colorValues[7] = getColorValue(_colorSelectorTransparent7);
			colorValues[8] = getColorValue(_colorSelectorTransparent8);
			colorValues[9] = getColorValue(_colorSelectorTransparent9);
			colorValues[10] = getColorValue(_colorSelectorTransparent10);
			colorValues[11] = getColorValue(_colorSelectorTransparent11);
			colorValues[12] = getColorValue(_colorSelectorTransparent12);
			colorValues[13] = getColorValue(_colorSelectorTransparent13);
			colorValues[14] = getColorValue(_colorSelectorTransparent14);
			colorValues[15] = getColorValue(_colorSelectorTransparent15);
			colorValues[16] = getColorValue(_colorSelectorTransparent16);
			colorValues[17] = getColorValue(_colorSelectorTransparent17);
			colorValues[18] = getColorValue(_colorSelectorTransparent18);
			colorValues[19] = getColorValue(_colorSelectorTransparent19);
			colorValues[20] = getColorValue(_colorSelectorTransparent20);

			// set black color when it's checked
			colorValues[21] = isBlack ? 0 : -1;

			mpWrapper.setIsTransparentColors(_chkTransparentPixel.getSelection());
			mpWrapper.setIsTransparentBlack(isBlack);
			mpWrapper.setTransparentColors(colorValues);

			// update viewer
			_treeViewer.update(tviMapProvider, null);

			updateLiveView();
			enableControls();
		}

	}

	private void onModifyZoomSpinnerMax() {

		final int mapMinValue = _spinMinZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = _spinMaxZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;

		if (mapMaxValue > MAP_MAX_ZOOM_LEVEL) {
			_spinMaxZoom.setSelection(MP.UI_MAX_ZOOM_LEVEL);
		}

		if (mapMaxValue < mapMinValue) {
			_spinMinZoom.setSelection(mapMinValue + 1);
		}

		updateLiveView();
	}

//	private void onModifyBbox() {
//
//		fBboxTop = (float) fSpinnerBboxTop.getSelection() / 100000;
//		fBboxBottom = (float) fSpinnerBboxBottom.getSelection() / 100000;
//
//		// delete offline images to force the reload to test the modified url
//		for (final MapProviderWrapper mpWrapper : fMpProfile.getAllWrappers()) {
//
//			if (mpWrapper.isDisplayedInMap()) {
//				fPrefPageMapProvider.deleteOfflineMap(mpWrapper.getMapProvider());
//			}
//		}
////
////		System.out.println();
////		System.out.println();
////		System.out.println("top: " + fBboxTop);
////		System.out.println("bot: " + fBboxBottom);
////		System.out.println();
//
////		displayProfileMap(false);
//	}

	private void onModifyZoomSpinnerMin() {

		final int mapMinValue = _spinMinZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = _spinMaxZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;

		if (mapMinValue > mapMaxValue) {
			_spinMinZoom.setSelection(mapMaxValue + 1);
		}

		updateLiveView();
	}

	private void onScaleDoubleClick(final Widget widget) {

		final Scale scale = (Scale) widget;
		final int max = scale.getMaximum();

		scale.setSelection(max / 2);

		onModifyProperties();
	}

	/**
	 * Toggle OSM/Profile map
	 */
	private void onSelectMapOSM() {

		// toggle tile factory
		if (_map.getMapProvider() == _mpDefault) {

			// display profile map provider

			// update layers BEFORE the tile factory is set in the map
			updateModelFromUI();

			setMapZoomLevelFromInfo(_mpProfile);

			_map.setMapProviderWithReset(_mpProfile, true);

		} else {

			// display OSM

			_mpDefault.setStateToReloadOfflineCounter();

			// ensure the map is using the correct zoom levels
			setMapZoomLevelFromInfo(_mpDefault);

			_map.setMapProviderWithReset(_mpDefault, true);
		}
	}

	/**
	 * Display map with the profile map provider
	 */
	private void onSelectMapProfile(final boolean isDeleteOffline) {

		// update layers BEFORE the tile factory is set in the map
		updateModelFromUI();

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * <br>
		 * ensure the map is using the correct zoom levels before other map actions are done<br>
		 * <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 */
		setMapZoomLevelFromInfo(_mpProfile);

		_map.setMapProviderWithReset(_mpProfile, true);
	}

	/**
	 * A map provider is selected in the mp list
	 * 
	 * @param selection
	 */
	private void onSelectMP(final ISelection selection) {

		Object firstElement = ((StructuredSelection) selection).getFirstElement();

		if (firstElement instanceof TVIWmsLayer) {
			// get map provider item
			final TVIWmsLayer tviItem = (TVIWmsLayer) firstElement;
			firstElement = tviItem.getParentItem();
		}

		if (firstElement instanceof TVIMapProvider) {

			final MPWrapper selectedMpWrapper = ((TVIMapProvider) firstElement).getMapProviderWrapper();

			_isInitUI = true;
			{
				final int alpha = selectedMpWrapper.getAlpha();
				final int brightness = selectedMpWrapper.getBrightnessValueForNextMp();
				final String mpUrl = getMpUrl(selectedMpWrapper.getMP());

				_spinAlpha.setSelection(alpha);
				_scaleAlpha.setSelection(alpha);

				_txtMpUrl.setText(mpUrl);
				_txtMpUrl.setToolTipText(mpUrl);
				_chkBrightness.setSelection(selectedMpWrapper.isBrightnessForNextMp());
				_spinBright.setSelection(brightness);
				_scaleBright.setSelection(brightness);

				_chkTransparentPixel.setSelection(selectedMpWrapper.isTransparentColors());
				_chkTransparentBlack.setSelection(selectedMpWrapper.isTransparentBlack());

				final int[] transColor = selectedMpWrapper.getTransparentColors();
				final int colorLength = transColor == null ? 0 : transColor.length;
				int colorIndex = 0;

				setColorValue(_colorSelectorTransparent0, colorLength > colorIndex++ ? transColor[0] : 0);
				setColorValue(_colorSelectorTransparent1, colorLength > colorIndex++ ? transColor[1] : 0);
				setColorValue(_colorSelectorTransparent2, colorLength > colorIndex++ ? transColor[2] : 0);
				setColorValue(_colorSelectorTransparent3, colorLength > colorIndex++ ? transColor[3] : 0);
				setColorValue(_colorSelectorTransparent4, colorLength > colorIndex++ ? transColor[4] : 0);
				setColorValue(_colorSelectorTransparent5, colorLength > colorIndex++ ? transColor[5] : 0);
				setColorValue(_colorSelectorTransparent6, colorLength > colorIndex++ ? transColor[6] : 0);
				setColorValue(_colorSelectorTransparent7, colorLength > colorIndex++ ? transColor[7] : 0);
				setColorValue(_colorSelectorTransparent8, colorLength > colorIndex++ ? transColor[8] : 0);
				setColorValue(_colorSelectorTransparent9, colorLength > colorIndex++ ? transColor[9] : 0);
				setColorValue(_colorSelectorTransparent10, colorLength > colorIndex++ ? transColor[10] : 0);
				setColorValue(_colorSelectorTransparent11, colorLength > colorIndex++ ? transColor[11] : 0);
				setColorValue(_colorSelectorTransparent12, colorLength > colorIndex++ ? transColor[12] : 0);
				setColorValue(_colorSelectorTransparent13, colorLength > colorIndex++ ? transColor[13] : 0);
				setColorValue(_colorSelectorTransparent14, colorLength > colorIndex++ ? transColor[14] : 0);
				setColorValue(_colorSelectorTransparent15, colorLength > colorIndex++ ? transColor[15] : 0);
				setColorValue(_colorSelectorTransparent16, colorLength > colorIndex++ ? transColor[16] : 0);
				setColorValue(_colorSelectorTransparent17, colorLength > colorIndex++ ? transColor[17] : 0);
				setColorValue(_colorSelectorTransparent18, colorLength > colorIndex++ ? transColor[18] : 0);
				setColorValue(_colorSelectorTransparent19, colorLength > colorIndex++ ? transColor[19] : 0);
				setColorValue(_colorSelectorTransparent20, colorLength > colorIndex++ ? transColor[20] : 0);
			}
			_isInitUI = false;
		}

		enableControls();
	}

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			_detailForm.setViewerWidth(_dialogSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}

		// show tile info
		final boolean isShowDebugInfo = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_INFO);
		_chkShowTileInfo.setSelection(isShowDebugInfo);
		_map.setShowDebugInfo(isShowDebugInfo);

		// is live view
		final boolean isLiveView = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_LIVE_VIEW);
		_chkLiveView.setSelection(isLiveView);
		_isLiveView = isLiveView;
		_map.setLiveView(isLiveView);

		// tile image logging
		final boolean isTileImageLogging = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG);
		_chkShowTileImageLog.setSelection(isTileImageLogging);
		_isTileImageLogging = isTileImageLogging;

		// property container
		final boolean isPropExpanded = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_PROPERTIES_EXPANDED);
		_propContainer.setExpanded(isPropExpanded);
		_innerContainer.layout(true);

	}

	private void saveState() {

		_dialogSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, _leftContainer.getSize().x);
		_dialogSettings.put(DIALOG_SETTINGS_IS_LIVE_VIEW, _chkLiveView.getSelection());
		_dialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_INFO, _chkShowTileInfo.getSelection());
		_dialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG, _chkShowTileImageLog.getSelection());
		_dialogSettings.put(DIALOG_SETTINGS_IS_PROPERTIES_EXPANDED, _propContainer.isExpanded());
	}

	private void setColorValue(final ColorSelector colorSelector, int colorValue) {

		if (colorValue == -1) {
			colorValue = 0;
		}

		final RGB rgb = getRGB(colorValue);

		colorSelector.setColorValue(rgb);
		colorSelector.getButton().setToolTipText(rgb.toString());
	}

	/**
	 * ensure the map is using the correct zoom levels from the tile factory
	 */
	private void setMapZoomLevelFromInfo(final MP mp) {

		final int factoryMinZoom = mp.getMinimumZoomLevel();
		final int factoryMaxZoom = mp.getMaximumZoomLevel();

		final int mapZoom = _map.getZoom();
		final GeoPosition mapCenter = _map.getGeoCenter();

		if (mapZoom < factoryMinZoom) {
			_map.setZoom(factoryMinZoom);
			_map.setGeoCenterPosition(mapCenter);
		}

		if (mapZoom > factoryMaxZoom) {
			_map.setZoom(factoryMaxZoom);
			_map.setGeoCenterPosition(mapCenter);
		}
	}

	public void tileEvent(final TileEventId tileEventId, final Tile tile) {

		// check if logging is enable
		if (_isTileImageLogging == false) {
			return;
		}

		final long nanoTime = System.nanoTime();
		_statUpdateCounter++;

		// update statistics
		if (tileEventId == TileEventId.TILE_RESET_QUEUES) {
			_statIsQueued = 0;
			_statStartLoading = 0;
			_statEndLoading = 0;
		} else if (tileEventId == TileEventId.TILE_IS_QUEUED) {
			_statIsQueued++;
			tile.setTimeIsQueued(nanoTime);
		} else if (tileEventId == TileEventId.TILE_START_LOADING) {
			_statStartLoading++;
			tile.setTimeStartLoading(nanoTime);
		} else if (tileEventId == TileEventId.TILE_END_LOADING) {
			_statEndLoading++;
			_statIsQueued--;
			tile.setTimeEndLoading(nanoTime);
		} else if (tileEventId == TileEventId.TILE_ERROR_LOADING) {
			_statErrorLoading++;
			_statIsQueued--;
		}

		// when stat is cleared, queue can get negative, prevent this
		if (_statIsQueued < 0) {
			_statIsQueued = 0;
		}

		// create log entry
		_logEntries.add(new LogEntry(//
				tileEventId,
				tile,
				nanoTime,
				Thread.currentThread().getName(),
				_statUpdateCounter));

		/*
		 * create runnable which displays the log
		 */

		final Runnable infoRunnable = new Runnable() {

			final int	fRunnableCounter	= _statUpdateCounter;

			public void run() {

				// check if this is the last created runnable
				if (fRunnableCounter != _statUpdateCounter) {
					// a new update event occured
					return;
				}

				if (_lblTileInfo.isDisposed()) {
					// widgets are disposed
					return;
				}

				// show at most 3 decimals
				_lblTileInfo.setText(NLS.bind(Messages.Dialog_MapConfig_TileInfo_Statistics, new Object[] {
						Integer.toString(_statIsQueued % 1000),
						Integer.toString(_statEndLoading % 1000),
						Integer.toString(_statStartLoading % 1000),
						Integer.toString(_statErrorLoading % 1000), //
				}));

				final String logEntry = displayLogEntries(_logEntries, _cboTileImageLog);

				// select new entry
				_cboTileImageLog.select(_cboTileImageLog.getItemCount() - 1);

				// display last log in the detail field
				if (logEntry.length() > 0) {
					_txtLogDetail.setText(logEntry);
				}
			}
		};

		_display.asyncExec(infoRunnable);
	}

	/**
	 * toggle visibility of the map provider or wms layer
	 */
	private void toggleMapVisibility(final Tree tree) {

		final TreeItem[] items = tree.getSelection();
		if (items.length > 0) {

			// get tree custom item
			final Object itemData = items[0].getData();

			if (itemData instanceof TVIMapProvider) {

				final MPWrapper mpWrapper = ((TVIMapProvider) itemData).getMapProviderWrapper();
				boolean isWmsDisplayed = mpWrapper.isDisplayedInMap();

				final MP mapProvider = mpWrapper.getMP();
				if (mapProvider instanceof MPWms) {

					// visibility for a wms map provider can be toggled only a layer

					if (isWmsDisplayed) {
						// hide wms
						isWmsDisplayed = false;
					} else {
						// show wms only when one layer is displayed
						isWmsDisplayed = canWmsBeDisplayed((MPWms) mapProvider);
					}

				} else {

					// toggle state

					isWmsDisplayed = !isWmsDisplayed;
				}

				if (isWmsDisplayed) {

					/*
					 * remove parent tiles from loading cache because they can have loading
					 * errors (from their children) which prevents them to be loaded again
					 */
					_mpProfile.resetParentTiles();
				}

				mpWrapper.setIsDisplayedInMap(isWmsDisplayed);
				enableProfileMapButton();

			} else if (itemData instanceof TVIWmsLayer) {

				final TVIWmsLayer tviLayer = (TVIWmsLayer) itemData;
				final MtLayer mtLayer = tviLayer.getMtLayer();

				// toggle layer state
				mtLayer.setIsDisplayedInMap(!mtLayer.isDisplayedInMap());

				// update parent state
				updateMVMapProvider(tviLayer);
			}

			// update viewer
			_treeViewer.update(itemData, null);

			updateLiveView();
		}
	}

	void updateLiveView() {
		if (_isLiveView) {
			onSelectMapProfile(false);
		}
	}

	private void updateModelFromUI() {

		/*
		 * !!!! zoom level must be set before any other map methods are called because it
		 * initialized the map with new zoom levels !!!
		 */
		final int oldZoomLevel = _map.getZoom();
		final GeoPosition mapCenter = _map.getGeoCenter();

		final int newFactoryMinZoom = _spinMinZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;
		final int newFactoryMaxZoom = _spinMaxZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;

		// set new zoom level before other map actions are done
		_mpProfile.setZoomLevel(newFactoryMinZoom, newFactoryMaxZoom);

		// ensure the zoom level is in the valid range
		if (oldZoomLevel < newFactoryMinZoom) {
			_map.setZoom(newFactoryMinZoom);
			_map.setGeoCenterPosition(mapCenter);
		}
		if (oldZoomLevel > newFactoryMaxZoom) {
			_map.setZoom(newFactoryMaxZoom);
			_map.setGeoCenterPosition(mapCenter);
		}

		// keep map position
		_mpProfile.setLastUsedZoom(_map.getZoom());
		_mpProfile.setLastUsedPosition(_map.getGeoCenter());

		// set positions of map provider
		int tblItemIndex = 0;
		for (final TreeItem treeItem : _treeViewer.getTree().getItems()) {

			final MPWrapper mpWrapper = ((TVIMapProvider) treeItem.getData()).getMapProviderWrapper();

			mpWrapper.setPositionIndex(tblItemIndex++);

			// update wms layer
			final MP mapProvider = mpWrapper.getMP();
			if (mapProvider instanceof MPWms) {

				final MPWms mpWms = (MPWms) mapProvider;

				if (mpWrapper.isDisplayedInMap()) {

//					// check if wms is locked, this should not happen but it did
//					final ReentrantLock wmsLock = MapProviderManager.getInstance().getWmsLock();
//					if (wmsLock.tryLock()) {
//						wmsLock.unlock();
//					} else {
//
//						StatusUtil.showStatus(Messages.DBG044_Wms_Error_WmsIsLocked, new Exception());
//
//						mpWrapper.setEnabled(false);
//						continue;
//					}

					// ensure that the wms layers are loaded from the wms server
					if (MapProviderManager.checkWms(mpWms, null) == null) {

						mpWrapper.setEnabled(false);
						continue;
					}

					/*
					 * update layer position, in windows it was possible that the tree hasn't been
					 * expanded until now but it returned one child without data
					 */
					final TreeItem[] treeChildren = treeItem.getItems();
					int itemIndex = 0;
					int visibleLayers = 0;

					for (final TreeItem childItem : treeChildren) {

						final Object childData = childItem.getData();
						if (childData instanceof TVIWmsLayer) {

							final MtLayer mtLayer = ((TVIWmsLayer) childData).getMtLayer();

							mtLayer.setPositionIndex(itemIndex++);

							if (mtLayer.isDisplayedInMap()) {
								visibleLayers++;
							}
						}
					}

					mpWms.initializeLayers();
				}
			}
		}

		MPProfile.sortMpWrapper(_mpProfile.getAllWrappers());
		MPProfile.updateMpFromWrapper(_mpProfile.getAllWrappers());
	}

	private void updateMVAlpha() {

		TVIMapProvider tviMapProvider = null;

		final Object firstElement = ((StructuredSelection) _treeViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TVIMapProvider) {

			// map provider is selected

			tviMapProvider = (TVIMapProvider) firstElement;

		} else if (firstElement instanceof TVIWmsLayer) {

			// wms layer is selected, get parent

			final TVIWmsLayer tviWmsLayer = (TVIWmsLayer) firstElement;

			final TreeViewerItem parentItem = tviWmsLayer.getParentItem();
			if (parentItem instanceof TVIMapProvider) {
				tviMapProvider = (TVIMapProvider) parentItem;
			}
		}

		if (tviMapProvider != null) {

			// update alpha in the map provider

			final MPWrapper mpWrapper = tviMapProvider.getMapProviderWrapper();

			// update model
			final int newAlpha = _spinAlpha.getSelection();
			mpWrapper.setAlpha(newAlpha);

			// update viewer
			_treeViewer.update(tviMapProvider, null);
		}
	}

	private void updateMVBrightness() {

		final Object firstElement = ((StructuredSelection) _treeViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TVIMapProvider) {

			// map provider is selected

			final TVIMapProvider tviMapProvider = (TVIMapProvider) firstElement;

			// update brightness in the map provider

			final MPWrapper mpWrapper = tviMapProvider.getMapProviderWrapper();

			mpWrapper.setIsBrightnessForNextMp(_chkBrightness.getSelection());
			mpWrapper.setBrightnessForNextMp(_scaleBright.getSelection());

			// update viewer
			_treeViewer.update(tviMapProvider, null);

			updateLiveView();
			enableControls();
		}
	}

	/**
	 * Update visibility of the wms map provider according to the visible layers
	 * 
	 * @param tviLayer
	 */
	private void updateMVMapProvider(final TVIWmsLayer tviLayer) {

		final TreeViewerItem tviParent = tviLayer.getParentItem();
		if (tviParent instanceof TVIMapProvider) {

			final MPWrapper parentMpWrapper = ((TVIMapProvider) tviParent).getMapProviderWrapper();
			final MP mapProvider = parentMpWrapper.getMP();

			if (mapProvider instanceof MPWms) {

				// check if a layer is visible

				parentMpWrapper.setIsDisplayedInMap(canWmsBeDisplayed((MPWms) mapProvider));
				enableProfileMapButton();

				// update parent item in the viewer
				_treeViewer.update(tviParent, null);
			}
		}
	}

	/**
	 * Initialize UI from the model and display the viewer content
	 * 
	 * @param mapProfile
	 */
	private void updateUIFromModel(final MPProfile mapProfile) {

		_mpProfile = mapProfile;

		_isInitUI = true;
		{
			// zoom level
			final int minZoomLevel = _mpProfile.getMinZoomLevel();
			final int maxZoomLevel = _mpProfile.getMaxZoomLevel();
			_spinMinZoom.setSelection(minZoomLevel + MP.UI_MIN_ZOOM_LEVEL);
			_spinMaxZoom.setSelection(maxZoomLevel + MP.UI_MIN_ZOOM_LEVEL);

			final int color = _mpProfile.getBackgroundColor();
			_colorImageBackground.setColorValue(new RGB(
					(color & 0xFF) >>> 0,
					(color & 0xFF00) >>> 8,
					(color & 0xFF0000) >>> 16));

		}
		_isInitUI = false;

		// show map provider in the message area
		_defaultMessage = NLS.bind(Messages.Dialog_MapConfig_DialogArea_Message, _mpProfile.getName());
		setMessage(_defaultMessage);

		final ArrayList<MPWrapper> allMpWrappers = _mpProfile.getAllWrappers();

		MPProfile.sortMpWrapper(allMpWrappers);
		MPProfile.updateMpFromWrapper(allMpWrappers);

		// set factory this is required when zoom and position is set
		_map.setMapProviderWithReset(_mpProfile, true);

		// set position to previous position
		_map.setZoom(_mpProfile.getLastUsedZoom());
		_map.setGeoCenterPosition(_mpProfile.getLastUsedPosition());

		// create root item and update viewer
		_rootItem = new TVIMapProviderRoot(_treeViewer, allMpWrappers);
		_treeViewer.setInput(_rootItem);
	}
}
