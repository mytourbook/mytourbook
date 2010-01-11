/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import java.util.concurrent.locks.ReentrantLock;

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
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.IMapListener;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.IZoomListener;
import de.byteholder.geoclipse.map.event.MapEvent;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.map.event.ZoomEvent;
import de.byteholder.geoclipse.preferences.PrefPageMapProviders;
import de.byteholder.geoclipse.ui.ViewerDetailForm;
import de.byteholder.geoclipse.util.PixelConverter;
import de.byteholder.geoclipse.util.Util;
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

	private IDialogSettings					fDialogSettings;

	private MPProfile						fMpProfile;

	/*
	 * UI controls
	 */
	private Display							fDisplay;
	private Button							fBtnOk;

	private Composite						fLeftContainer;
	private Composite						fInnerContainer;
	private ViewerDetailForm				fDetailForm;

	private ExpandableComposite				fPropContainer;
	private Composite						fPropInnerContainer;

	private ContainerCheckedTreeViewer		fTreeViewer;
	private TVIMapProviderRoot				fRootItem;

	private ToolBar							fToolbar;
	private Button							fBtnShowProfileMap;
	private Button							fBtnShowOsmMap;

	private Label							fLblMapInfo;
	private Label							fLblTileInfo;
	private Button							fChkLiveView;
	private Button							fChkShowTileInfo;

	private Spinner							fSpinMinZoom;
	private Spinner							fSpinMaxZoom;

	private Spinner							fSpinAlpha;
	private Scale							fScaleAlpha;
	private Button							fChkTransparentPixel;
	private Button							fChkTransparentBlack;
	private Button							fChkBrightness;
	private Spinner							fSpinBright;
	private Scale							fScaleBright;
	private Label							fLblAlpha;

	private ExpandableComposite				fTransContainer;
	private ColorSelector					fColorSelectorTransparent0;
	private ColorSelector					fColorSelectorTransparent1;
	private ColorSelector					fColorSelectorTransparent2;
	private ColorSelector					fColorSelectorTransparent3;
	private ColorSelector					fColorSelectorTransparent4;
	private ColorSelector					fColorSelectorTransparent5;
	private ColorSelector					fColorSelectorTransparent6;
	private ColorSelector					fColorSelectorTransparent7;
	private ColorSelector					fColorSelectorTransparent8;
	private ColorSelector					fColorSelectorTransparent9;
	private ColorSelector					fColorSelectorTransparent10;
	private ColorSelector					fColorSelectorTransparent11;
	private ColorSelector					fColorSelectorTransparent12;
	private ColorSelector					fColorSelectorTransparent13;
	private ColorSelector					fColorSelectorTransparent14;
	private ColorSelector					fColorSelectorTransparent15;
	private ColorSelector					fColorSelectorTransparent16;
	private ColorSelector					fColorSelectorTransparent17;
	private ColorSelector					fColorSelectorTransparent18;
	private ColorSelector					fColorSelectorTransparent19;
	private ColorSelector					fColorSelectorTransparent20;

	private ColorSelector					fColorImageBackground;

	private FormToolkit						fFormTk									= new FormToolkit(Display
																							.getCurrent());
	private ExpandableComposite				fLogContainer;
	private Button							fChkShowTileImageLog;
	private Combo							fCboTileImageLog;
	private Text							fTxtLogDetail;

	// debugging
//	private Spinner							fSpinnerBboxTop;
//	private Spinner							fSpinnerBboxBottom;

	/*
	 * none UI items
	 */
	// image logging
	private boolean							fIsTileImageLogging;
	private ConcurrentLinkedQueue<LogEntry>	fLogEntries								= new ConcurrentLinkedQueue<LogEntry>();

	private NumberFormat					fNfLatLon								= NumberFormat.getNumberInstance();
	{
		// initialize lat/lon formatter
		fNfLatLon.setMinimumFractionDigits(6);
		fNfLatLon.setMaximumFractionDigits(6);
	}

	private int								fStatUpdateCounter						= 0;

	private String							fDefaultMessage;

	private int								fStatIsQueued;
	private int								fStatStartLoading;
	private int								fStatEndLoading;
	private int								fStatErrorLoading;

	private MPPlugin						fDefaultMP;

	private boolean							fIsInitUI								= false;

	private Image							fImageMap;
	private Image							fImagePlaceholder;
	private Image							fImageLayer;

//	private PrefPageMapProviders			fPrefPageMapProvider;

	private long							fDragStartTime;

	protected boolean						fIsLiveView;

//	public static float						fBboxTop								= 1.0f;
//	public static float						fBboxBottom								= 1.0f;

	private class MapContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildrenAsArray();
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

		fDialogSettings = Activator.getDefault().getDialogSettingsSection("DialogMapProfileConfiguration");//$NON-NLS-1$

		fMpProfile = dialogMapProfile;

		/*
		 * disable saving of the profile image (not the child images) when the map is displayed in
		 * this dialoag because this improves performance when the image parameters are modified
		 */
		fMpProfile.setIsSaveImage(false);

		fDefaultMP = MapProviderManager.getInstance().getDefaultMapProvider();
	}

	public void actionZoomIn() {
		fMap.setZoom(fMap.getZoom() + 1);
		fMap.queueMapRedraw();
	}

	public void actionZoomOut() {
		fMap.setZoom(fMap.getZoom() - 1);
		fMap.queueMapRedraw();
	}

	public void actionZoomOutToMinZoom() {
		fMap.setZoom(fMap.getMapProvider().getMinimumZoomLevel());
		fMap.queueMapRedraw();
	}

	@Override
	protected void cancelPressed() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				// stop downloading images
				fMpProfile.resetAll(false);
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
		fMpProfile.setIsSaveImage(true);

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

		fDisplay = Display.getCurrent();

		setTitle(Messages.Dialog_MapProfile_DialogArea_Title);

		MP.addTileListener(this);

		restoreState();

		// initialize after the shell size is set
		updateUIFromModel(fMpProfile);

		displayProfileMap(false);

		enableProfileMapButton();
		fTreeViewer.getTree().setFocus();
	}

	private void createActions() {

		final ToolBarManager tbm = new ToolBarManager(fToolbar);

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
		fBtnOk = getButton(IDialogConstants.OK_ID);
		fBtnOk.setText(Messages.Dialog_MapConfig_Button_Update);
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

		fImageMap = Activator.getImageDescriptor(Messages.Image__Visibility).createImage();
		fImagePlaceholder = Activator.getImageDescriptor(Messages.Image__Placeholder16).createImage();
		fImageLayer = Activator.getImageDescriptor(Messages.Image_Action_ZoomShowEntireLayer).createImage();
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
			fLeftContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 5, 0, 0).applyTo(fLeftContainer);
			createUI110LeftContainer(fLeftContainer, pixelConverter);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);
			UI.addSashColorHandler(sash);

			// right part (map)
			final Composite mapContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(mapContainer);
			createUI180Map(mapContainer, pixelConverter);

			fDetailForm = new ViewerDetailForm(container, fLeftContainer, sash, mapContainer, 30);
		}
	}

	private void createUI110LeftContainer(final Composite parent, final PixelConverter pixelConverter) {

		fInnerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fInnerContainer);
		GridLayoutFactory.fillDefaults().applyTo(fInnerContainer);
		{
			// label: map provider
			final Label label = new Label(fInnerContainer, SWT.NONE);
			label.setText(Messages.Dialog_MapConfig_Label_MapProvider);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_HintDragAndDrop);

			createUI114Viewer(fInnerContainer, pixelConverter);
			createUI140DialogProperties(fInnerContainer);

			/*
			 * section properties
			 */
			final Color parentBackground = parent.getBackground();

			fPropContainer = fFormTk.createExpandableComposite(fInnerContainer, ExpandableComposite.TWISTIE);

			GridDataFactory.fillDefaults().grab(true, false).applyTo(fPropContainer);
			GridLayoutFactory.fillDefaults().applyTo(fPropContainer);

			fPropContainer.setBackground(parentBackground);
			fPropContainer.setText(Messages.Dialog_MapConfig_Label_Properties);
			fPropContainer.addExpansionListener(new IExpansionListener() {

				public void expansionStateChanged(final ExpansionEvent e) {
					fPropContainer.getParent().layout(true);
				}

				public void expansionStateChanging(final ExpansionEvent e) {}
			});

			{
				fPropInnerContainer = fFormTk.createComposite(fPropContainer);
				fPropContainer.setClient(fPropInnerContainer);

				GridDataFactory.fillDefaults().grab(true, false).applyTo(fPropInnerContainer);
				GridLayoutFactory.fillDefaults().applyTo(fPropInnerContainer);

				fPropInnerContainer.setBackground(parentBackground);
				{
					createUI120MapProviderProperties(fPropInnerContainer);
					createUI130ProfileProperties(fPropInnerContainer);
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
			fTreeViewer = new ContainerCheckedTreeViewer(tree);

			fTreeViewer.setContentProvider(new MapContentProvider());
			fTreeViewer.setUseHashlookup(true);

			fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(final DoubleClickEvent event) {

					final Object selectedItem = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
					if (selectedItem != null) {

						if (selectedItem instanceof TVIMapProvider) {

							// expand/collapse current item

							final MP mapProvider = ((TVIMapProvider) selectedItem).getMapProviderWrapper().getMP();

							if ((mapProvider instanceof MPWms) == false) {

								// all none wms map provider can be toggled

								toggleMapVisibility(tree);

							} else {

								// expand/collapse item

								if (fTreeViewer.getExpandedState(selectedItem)) {
									fTreeViewer.collapseToLevel(selectedItem, 1);
								} else {
									fTreeViewer.expandToLevel(selectedItem, 1);
								}
							}

						} else if (selectedItem instanceof TVIWmsLayer) {
							toggleMapVisibility(tree);
						}
					}
				}
			});

			fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(final SelectionChangedEvent event) {
					onSelectMapProvider(event.getSelection());
				}
			});

			fTreeViewer.addDragSupport(
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
							final ITreeSelection selection = (ITreeSelection) fTreeViewer.getSelection();

							transfer.setSelection(selection);
							transfer.setSelectionSetTime(fDragStartTime = event.time & 0xFFFFFFFFL);

							// only ONE map provider/layer is allowed to be dragged
							final Object firstElement = selection.getFirstElement();
							event.doit = selection.size() == 1
									&& (firstElement instanceof TVIMapProvider || firstElement instanceof TVIWmsLayer);
						}
					});

			fTreeViewer.addDropSupport(
					DND.DROP_MOVE,
					new Transfer[] { LocalSelectionTransfer.getTransfer() },
					new ProfileDropAdapter(this, fTreeViewer));

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
					fTreeViewer,
					new FocusCellOwnerDrawHighlighter(fTreeViewer));

			final ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
					fTreeViewer) {

				@Override
				protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {
					return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
							|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
							|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.F2)
							|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
				}
			};

			TreeViewerEditor.create(fTreeViewer, //
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
		tvc = new TreeViewerColumn(fTreeViewer, SWT.LEAD);
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

					cell.setImage(mpWrapper.isDisplayedInMap() ? fImageMap : fImagePlaceholder);

				} else if (element instanceof TVIWmsLayer) {

					final MtLayer mtLayer = ((TVIWmsLayer) element).getMtLayer();

					styledString.append(mtLayer.getGeoLayer().getTitle());

					styledString.append("  (", StyledString.QUALIFIER_STYLER);//$NON-NLS-1$
					styledString.append(mtLayer.getGeoLayer().getName(), StyledString.QUALIFIER_STYLER);
					styledString.append(")", StyledString.QUALIFIER_STYLER);//$NON-NLS-1$

					cell.setImage(mtLayer.isDisplayedInMap() ? fImageLayer : fImagePlaceholder);

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
		tvc = new TreeViewerColumn(fTreeViewer, SWT.LEAD);
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

		tvc.setEditingSupport(new EditingSupport(fTreeViewer) {

			private CheckboxCellEditor	fCellEditor	= new CheckboxCellEditor(fTreeViewer.getTree());

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
						fMpProfile.resetParentTiles();
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
		tvc = new TreeViewerColumn(fTreeViewer, SWT.LEAD);
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
		tvc = new TreeViewerColumn(fTreeViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Dialog_MapProfile_Column_Brightness);
		tc.setToolTipText(Messages.Dialog_MapProfile_Column_Brightness_Tooltip);
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMapProvider) {

					final MPWrapper mpWrapper = ((TVIMapProvider) element).getMapProviderWrapper();

					cell.setText(mpWrapper.isBrightness()
							? Integer.toString(mpWrapper.getBrightness())
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
		tvc = new TreeViewerColumn(fTreeViewer, SWT.LEAD);
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
			fChkBrightness = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(fChkBrightness);
			fChkBrightness.setText(Messages.Dialog_MapProfile_Button_Brightness);
			fChkBrightness.setToolTipText(Messages.Dialog_MapProfile_Button_Brightness_Tooltip);
			fChkBrightness.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					updateMVBrightness();
				}
			});

			// scale: brightness
			fScaleBright = new Scale(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleBright);
			fScaleBright.setMinimum(0);
			fScaleBright.setMaximum(100);
			fScaleBright.setToolTipText(Messages.Dialog_MapProfile_Scale_Brightness_Tooltip);
			fScaleBright.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyBrightScale();
				}
			});

			// spinner: brightness
			fSpinBright = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults()//
//					.grab(false, true)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(fSpinBright);
			fSpinBright.setMinimum(0);
			fSpinBright.setMaximum(100);
			fSpinBright.setToolTipText(Messages.Dialog_MapProfile_Scale_Brightness_Tooltip);

			fSpinBright.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			fSpinBright.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyBrightSpinner();
				}
			});

			fSpinBright.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					onModifyBrightSpinner();
				}
			});

			// ################################################

			// label: alpha
			fLblAlpha = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(fLblAlpha);
			fLblAlpha.setText(Messages.Dialog_CustomConfig_Label_Alpha);
			fLblAlpha.setToolTipText(Messages.Dialog_CustomConfig_Label_Alpha_Tooltip);

			// scale: alpha
			fScaleAlpha = new Scale(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAlpha);
			fScaleAlpha.setMinimum(0);
			fScaleAlpha.setMaximum(100);
			fScaleAlpha.setToolTipText(Messages.Dialog_CustomConfig_Label_Alpha_Tooltip);
			fScaleAlpha.addSelectionListener(new SelectionAdapter() {
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
			fScaleAlpha.addListener(SWT.MouseDoubleClick, new Listener() {
				public void handleEvent(final Event event) {
					onScaleDoubleClick(event.widget);
				}
			});

			// spinner: alpha
			fSpinAlpha = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(fSpinAlpha);
			fSpinAlpha.setMinimum(0);
			fSpinAlpha.setMaximum(100);
			fSpinAlpha.setToolTipText(Messages.Dialog_CustomConfig_Label_Alpha_Tooltip);

			fSpinAlpha.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			fSpinAlpha.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onModifyAlphaSpinner();
				}
			});

			fSpinAlpha.addModifyListener(new ModifyListener() {
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
				fChkTransparentPixel = new Button(containerOptions, SWT.CHECK);
				fChkTransparentPixel.setText(Messages.Dialog_MapConfig_Button_TransparentPixel);
				fChkTransparentPixel.setToolTipText(Messages.Dialog_MapConfig_Button_TransparentPixel_Tooltip);
				fChkTransparentPixel.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyTransparentColor();
					}
				});

				// check: is black transparent 
				fChkTransparentBlack = new Button(containerOptions, SWT.CHECK);
				fChkTransparentBlack.setText(Messages.Dialog_MapConfig_Button_TransparentBlack);
				fChkTransparentBlack.setToolTipText(Messages.Dialog_MapConfig_Button_TransparentBlack_Tooltip);
				fChkTransparentBlack.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyTransparentColor();
					}
				});
			}

			// ################################################

			createUI122ColorSelector1(group);
		}
	}

	private void createUI122ColorSelector1(final Composite parent) {

		// transparent color selectors

		final Color parentBackground = parent.getBackground();

		fTransContainer = fFormTk.createExpandableComposite(parent, ExpandableComposite.NO_TITLE);

		// prevent flickering in the UI
		fTransContainer.setExpanded(false);

		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(fTransContainer);
		GridLayoutFactory.fillDefaults().applyTo(fTransContainer);

		fTransContainer.setBackground(parentBackground);
		fTransContainer.addExpansionListener(new IExpansionListener() {

			public void expansionStateChanged(final ExpansionEvent e) {
//				fPropInnerContainer.layout(true);
				fInnerContainer.layout(true);
			}

			public void expansionStateChanging(final ExpansionEvent e) {}
		});

		{
			final Composite clientContainer = fFormTk.createComposite(fTransContainer);
			fTransContainer.setClient(clientContainer);

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
				fColorSelectorTransparent0 = createUIColorSelector(colorContainer0, colorListener, gd);
				fColorSelectorTransparent1 = createUIColorSelector(colorContainer0, colorListener, gd);
				fColorSelectorTransparent2 = createUIColorSelector(colorContainer0, colorListener, gd);
				fColorSelectorTransparent3 = createUIColorSelector(colorContainer0, colorListener, gd);
				fColorSelectorTransparent4 = createUIColorSelector(colorContainer0, colorListener, gd);
				fColorSelectorTransparent5 = createUIColorSelector(colorContainer0, colorListener, gd);
				fColorSelectorTransparent6 = createUIColorSelector(colorContainer0, colorListener, gd);
			}

			final Composite colorContainer1 = new Composite(colorContainerParent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(colorContainer1);
			GridLayoutFactory.fillDefaults().numColumns(7).spacing(0, 0).applyTo(colorContainer1);
			{
				fColorSelectorTransparent7 = createUIColorSelector(colorContainer1, colorListener, gd);
				fColorSelectorTransparent8 = createUIColorSelector(colorContainer1, colorListener, gd);
				fColorSelectorTransparent9 = createUIColorSelector(colorContainer1, colorListener, gd);
				fColorSelectorTransparent10 = createUIColorSelector(colorContainer1, colorListener, gd);
				fColorSelectorTransparent11 = createUIColorSelector(colorContainer1, colorListener, gd);
				fColorSelectorTransparent12 = createUIColorSelector(colorContainer1, colorListener, gd);
				fColorSelectorTransparent13 = createUIColorSelector(colorContainer1, colorListener, gd);
			}

			final Composite colorContainer2 = new Composite(colorContainerParent, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(colorContainer2);
			GridLayoutFactory.fillDefaults().numColumns(7).spacing(0, 0).applyTo(colorContainer2);
			{
				fColorSelectorTransparent14 = createUIColorSelector(colorContainer2, colorListener, gd);
				fColorSelectorTransparent15 = createUIColorSelector(colorContainer2, colorListener, gd);
				fColorSelectorTransparent16 = createUIColorSelector(colorContainer2, colorListener, gd);
				fColorSelectorTransparent17 = createUIColorSelector(colorContainer2, colorListener, gd);
				fColorSelectorTransparent18 = createUIColorSelector(colorContainer2, colorListener, gd);
				fColorSelectorTransparent19 = createUIColorSelector(colorContainer2, colorListener, gd);
				fColorSelectorTransparent20 = createUIColorSelector(colorContainer2, colorListener, gd);
			}
		}
	}

	private void createUI130ProfileProperties(final Composite parent) {

		Label label;
		final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {

				Util.adjustSpinnerValueOnMouseScroll(event);

				// validate values
				if (event.widget == fSpinMinZoom) {
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
				fSpinMinZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(fSpinMinZoom);
				fSpinMinZoom.setMinimum(MP.UI_MIN_ZOOM_LEVEL);
				fSpinMinZoom.setMaximum(MP.UI_MAX_ZOOM_LEVEL);
				fSpinMinZoom.setSelection(MP.UI_MIN_ZOOM_LEVEL);
				fSpinMinZoom.addMouseWheelListener(mouseWheelListener);
				fSpinMinZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (fIsInitUI) {
							return;
						}
						onModifyZoomSpinnerMin();
					}
				});
				fSpinMinZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (fIsInitUI) {
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
				fSpinMaxZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(fSpinMaxZoom);
				fSpinMaxZoom.setMinimum(MP.UI_MIN_ZOOM_LEVEL);
				fSpinMaxZoom.setMaximum(MP.UI_MAX_ZOOM_LEVEL);
				fSpinMaxZoom.setSelection(MP.UI_MAX_ZOOM_LEVEL);
				fSpinMaxZoom.addMouseWheelListener(mouseWheelListener);
				fSpinMaxZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (fIsInitUI) {
							return;
						}
						onModifyZoomSpinnerMax();
					}
				});
				fSpinMaxZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (fIsInitUI) {
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
			fColorImageBackground = new ColorSelector(group);
			GridDataFactory.swtDefaults()//
					.grab(false, true)
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.applyTo(fColorImageBackground.getButton());

			fColorImageBackground.addListener(new IPropertyChangeListener() {
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
			fChkLiveView = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.END)
					.applyTo(fChkLiveView);
			fChkLiveView.setText(Messages.Dialog_MapConfig_Button_LiveView);
			fChkLiveView.setToolTipText(Messages.Dialog_MapConfig_Button_LiveView_Tooltip);
			fChkLiveView.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					fIsLiveView = fChkLiveView.getSelection();
					fMap.setLiveView(fIsLiveView);

					updateLiveView();
				}
			});

			// ################################################

			// check: show tile info
			fChkShowTileInfo = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.END)
					.applyTo(fChkShowTileInfo);
			fChkShowTileInfo.setText(Messages.Dialog_MapConfig_Button_ShowTileInfo);
			fChkShowTileInfo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					fMap.setShowDebugInfo(fChkShowTileInfo.getSelection());
				}
			});

			// ############################################################

			// check: show tile image loading log
			fChkShowTileImageLog = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.applyTo(fChkShowTileImageLog);
			fChkShowTileImageLog.setText(Messages.Dialog_MapConfig_Button_ShowTileLog);
			fChkShowTileImageLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
			fChkShowTileImageLog.addSelectionListener(new SelectionAdapter() {
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
			fBtnShowProfileMap = new Button(toolbarContainer, SWT.NONE);
			fBtnShowProfileMap.setText(Messages.Dialog_MapProfile_Button_UpdateMap);
			fBtnShowProfileMap.setToolTipText(Messages.Dialog_MapProfile_Button_UpdateMap_Tooltip);
			fBtnShowProfileMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					displayProfileMap(true);
				}
			});

			// ############################################################

			// button: osm map
			fBtnShowOsmMap = new Button(toolbarContainer, SWT.NONE);
			fBtnShowOsmMap.setText(Messages.Dialog_MapConfig_Button_ShowOsmMap);
			fBtnShowOsmMap.setToolTipText(Messages.Dialog_MapConfig_Button_ShowOsmMap_Tooltip);
			fBtnShowOsmMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					displayOsmMap();
				}
			});

			// ############################################################

			fToolbar = new ToolBar(toolbarContainer, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.END, SWT.FILL)
					.applyTo(fToolbar);
		}

		fMap = new Map(parent, SWT.BORDER | SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(fMap);

		super.setMap(fMap);

		fMap.setShowScale(true);

		fMap.addZoomListener(new IZoomListener() {
			public void zoomChanged(final ZoomEvent event) {
			// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG 
//				resetMapProfile();
			// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG 
			}
		});

		fMap.addMapListener(new IMapListener() {

			public void mapInfo(final MapEvent event) {

				final GeoPosition mapCenter = event.mapCenter;

				double lon = mapCenter.getLongitude() % 360;
				lon = lon > 180 ? //
						lon - 360
						: lon < -180 ? //
								lon + 360
								: lon;

				fLblMapInfo.setText(NLS.bind(Messages.Dialog_MapConfig_Label_MapInfo, new Object[] {
						fNfLatLon.format(mapCenter.getLatitude()),
						fNfLatLon.format(lon),
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
			fLblMapInfo = new Label(infoContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fLblMapInfo);

			// label: tile info
			fLblTileInfo = new Label(infoContainer, SWT.TRAIL);
			GridDataFactory.fillDefaults().hint(pixelConverter.convertWidthInCharsToPixels(25), SWT.DEFAULT).applyTo(
					fLblTileInfo);
			fLblTileInfo.setToolTipText(Messages.Dialog_MapConfig_TileInfo_Tooltip_Line1
					+ Messages.Dialog_MapConfig_TileInfo_Tooltip_Line2
					+ Messages.Dialog_MapConfig_TileInfo_Tooltip_Line3);
		}

		/*
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * !!! don't do any map initialization until the tile factory is set !!!
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
	}

	private void createUI200Log(final Composite parent, final PixelConverter pixelConverter) {

		final Font monoFont = getMonoFont();
		final Color parentBackground = parent.getBackground();

		fLogContainer = fFormTk.createExpandableComposite(parent, ExpandableComposite.TWISTIE);

		GridDataFactory.fillDefaults().grab(true, false).applyTo(fLogContainer);
		GridLayoutFactory.fillDefaults().applyTo(fLogContainer);

		fLogContainer.setBackground(parentBackground);
		fLogContainer.setText(Messages.Dialog_MapConfig_Label_LoadedImageUrl);
		fLogContainer.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
		fLogContainer.addExpansionListener(new IExpansionListener() {

			public void expansionStateChanged(final ExpansionEvent e) {
				fLogContainer.getParent().layout(true);
			}

			public void expansionStateChanging(final ExpansionEvent e) {}
		});

		{
			final Composite clientContainer = fFormTk.createComposite(fLogContainer);
			fLogContainer.setClient(clientContainer);

			GridDataFactory.fillDefaults().grab(true, false).applyTo(clientContainer);
			GridLayoutFactory.fillDefaults().applyTo(clientContainer);

			clientContainer.setBackground(parentBackground);

			{
				// combo: url log
				fCboTileImageLog = new Combo(clientContainer, SWT.READ_ONLY);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(fCboTileImageLog);
				fCboTileImageLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
				fCboTileImageLog.setVisibleItemCount(40);
				fFormTk.adapt(fCboTileImageLog, true, true);
				fCboTileImageLog.setFont(monoFont);

				fCboTileImageLog.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						// display selected item in the text field below
						final int selectionIndex = fCboTileImageLog.getSelectionIndex();
						if (selectionIndex != -1) {
							fTxtLogDetail.setText(fCboTileImageLog.getItem(selectionIndex));
						}
					}
				});

				// label: selected log entry
				fTxtLogDetail = new Text(clientContainer, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(5))
						.applyTo(fTxtLogDetail);
				fFormTk.adapt(fTxtLogDetail, false, false);
				fTxtLogDetail.setFont(monoFont);
				fTxtLogDetail.setBackground(parentBackground);
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

	private void displayOsmMap() {

		if (fMap.getMapProvider() == fDefaultMP) {

			// toggle tile factory

			// update layers BEFORE the tile factory is set in the map
			updateModelFromUI();

			setMapZoomLevelFromInfo(fMpProfile);

			fMap.resetTileFactory(fMpProfile);

		} else {

			// display OSM

			// ensure the map is using the correct zoom levels
			setMapZoomLevelFromInfo(fDefaultMP);

			fDefaultMP.setStateToReloadOfflineCounter();

			fMap.resetTileFactory(fDefaultMP);
		}
	}

	/**
	 * Display profile map provider
	 */
	private void displayProfileMap(final boolean isDeleteOffline) {

		// update layers BEFORE the tile factory is set in the map
		updateModelFromUI();

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * <br>
		 * ensure the map is using the correct zoom levels before other map actions are done<br>
		 * <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 */
		setMapZoomLevelFromInfo(fMpProfile);

		fMap.resetTileFactory(fMpProfile);
	}

	private void enableControls() {

		final ITreeSelection selection = (ITreeSelection) fTreeViewer.getSelection();
		final Object firstElement = selection.getFirstElement();

		boolean isMpSelected = true;

		if (firstElement instanceof TVIWmsLayer) {

			// wms layer is selected, disable map provider controls

			isMpSelected = false;
		}

		final boolean isBrightness = isMpSelected & fChkBrightness.getSelection();
		final boolean isNoBrightness = isMpSelected & !isBrightness;
		final boolean isTransparent = isMpSelected & fChkTransparentPixel.getSelection() & !isBrightness;

		fChkBrightness.setEnabled(isMpSelected);
		fSpinBright.setEnabled(isBrightness);
		fScaleBright.setEnabled(isBrightness);

		fLblAlpha.setEnabled(isNoBrightness);
		fSpinAlpha.setEnabled(isNoBrightness);
		fScaleAlpha.setEnabled(isNoBrightness);

		/*
		 * transparent pixel
		 */
		fChkTransparentPixel.setEnabled(isNoBrightness);

		fChkTransparentBlack.setEnabled(isTransparent);
		fColorSelectorTransparent0.setEnabled(isTransparent);
		fColorSelectorTransparent1.setEnabled(isTransparent);
		fColorSelectorTransparent2.setEnabled(isTransparent);
		fColorSelectorTransparent3.setEnabled(isTransparent);
		fColorSelectorTransparent4.setEnabled(isTransparent);
		fColorSelectorTransparent5.setEnabled(isTransparent);
		fColorSelectorTransparent6.setEnabled(isTransparent);
		fColorSelectorTransparent7.setEnabled(isTransparent);
		fColorSelectorTransparent8.setEnabled(isTransparent);
		fColorSelectorTransparent9.setEnabled(isTransparent);
		fColorSelectorTransparent10.setEnabled(isTransparent);
		fColorSelectorTransparent11.setEnabled(isTransparent);
		fColorSelectorTransparent12.setEnabled(isTransparent);
		fColorSelectorTransparent13.setEnabled(isTransparent);
		fColorSelectorTransparent14.setEnabled(isTransparent);
		fColorSelectorTransparent15.setEnabled(isTransparent);
		fColorSelectorTransparent16.setEnabled(isTransparent);
		fColorSelectorTransparent17.setEnabled(isTransparent);
		fColorSelectorTransparent18.setEnabled(isTransparent);
		fColorSelectorTransparent19.setEnabled(isTransparent);
		fColorSelectorTransparent20.setEnabled(isTransparent);

		// check if the container must be expanded/collapsed
		final boolean isTransExpanded = fTransContainer.isExpanded();

		if ((isTransExpanded == true && isTransparent == false) || //
				(isTransExpanded == false && isTransparent == true)) {

			// show/hide transparent color section
			fTransContainer.setExpanded(isTransparent);
//			fPropInnerContainer.layout(true);
			fInnerContainer.layout(true);
		}

		/*
		 * image logging
		 */
		fIsTileImageLogging = fChkShowTileImageLog.getSelection();

		if (fIsTileImageLogging == false) {
			// remove old log entries
			fStatUpdateCounter = 0;
			fCboTileImageLog.removeAll();
			fTxtLogDetail.setText(UI.EMPTY_STRING);
		}

		fCboTileImageLog.setEnabled(fIsTileImageLogging);
		fTxtLogDetail.setEnabled(fIsTileImageLogging);

		// check if the container must be expanded/collapsed
		final boolean isLogExpanded = fLogContainer.isExpanded();

		if ((isLogExpanded == true && fIsTileImageLogging == false)
				|| (isLogExpanded == false && fIsTileImageLogging == true)) {

			// show/hide log section
			fLogContainer.setExpanded(fIsTileImageLogging);
			fLogContainer.getParent().layout(true);
		}
	}

	/**
	 * Enable profile map button when at least one map provider is displayed
	 */
	private void enableProfileMapButton() {

		boolean isEnabled = false;

		// check if a wrapper is displayed and enabled
		for (final MPWrapper mpWrapper : fMpProfile.getAllWrappers()) {
			if (mpWrapper.isDisplayedInMap() && mpWrapper.isEnabled()) {
				isEnabled = true;
				break;
			}
		}

		fBtnShowProfileMap.setEnabled(isEnabled);

		if (isEnabled == false) {
			// hide profile map
			displayOsmMap();
		}
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
		return fDialogSettings;

		// disable bounds
//		return null;
	}

	long getDragStartTime() {
		return fDragStartTime;
	}

	MPProfile getMpProfile() {
		return fMpProfile;
	}

	private RGB getRGB(final int colorValue) {

		final int red = (colorValue & 0xFF) >>> 0;
		final int green = (colorValue & 0xFF00) >>> 8;
		final int blue = (colorValue & 0xFF0000) >>> 16;

		return new RGB(red, green, blue);
	}

	public TVIMapProviderRoot getRootItem() {
		return fRootItem;
	}

	@Override
	protected void okPressed() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				// stop downloading images
				fMpProfile.resetAll(false);

				// model is saved in the dialog opening code
				updateModelFromUI();
			}
		});

		super.okPressed();
	}

	private void onDispose() {

		MP.removeTileListener(DialogMPProfile.this);

		if (fImageMap != null) {
			fImageMap.dispose();
		}
		if (fImagePlaceholder != null) {
			fImagePlaceholder.dispose();
		}
		if (fImageLayer != null) {
			fImageLayer.dispose();
		}
		if (fFormTk != null) {
			fFormTk.dispose();
		}
	}

	private void onModifyAlphaScale() {

		if (fIsInitUI) {
			return;
		}

		fIsInitUI = true;
		{
			fSpinAlpha.setSelection(fScaleAlpha.getSelection());
		}
		fIsInitUI = false;

		updateMVAlpha();
		updateLiveView();
	}

	private void onModifyAlphaSpinner() {

		if (fIsInitUI) {
			return;
		}

		fIsInitUI = true;
		{
			fScaleAlpha.setSelection(fSpinAlpha.getSelection());
		}
		fIsInitUI = false;

		updateMVAlpha();
		updateLiveView();
	}

	private void onModifyBrightScale() {

		if (fIsInitUI) {
			return;
		}

		fIsInitUI = true;
		{
			fSpinBright.setSelection(fScaleBright.getSelection());
		}
		fIsInitUI = false;

		updateMVBrightness();
	}

	private void onModifyBrightSpinner() {

		if (fIsInitUI) {
			return;
		}

		fIsInitUI = true;
		{
			fScaleBright.setSelection(fSpinBright.getSelection());
		}
		fIsInitUI = false;

		updateMVBrightness();
	}

	private void onModifyImageBgColor() {

		fMpProfile.setBackgroundColor(fColorImageBackground.getColorValue());

		updateLiveView();
	}

	private void onModifyProperties() {

		onModifyAlphaScale();

	}

	private void onModifyTransparentColor() {

		final Object firstElement = ((StructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TVIMapProvider) {

			// map provider is selected

			final TVIMapProvider tviMapProvider = (TVIMapProvider) firstElement;

			// update alpha in the map provider 

			final MPWrapper mpWrapper = tviMapProvider.getMapProviderWrapper();
			final boolean isBlack = fChkTransparentBlack.getSelection();

			// update model

			final int[] colorValues = new int[22];

			colorValues[0] = getColorValue(fColorSelectorTransparent0);
			colorValues[1] = getColorValue(fColorSelectorTransparent1);
			colorValues[2] = getColorValue(fColorSelectorTransparent2);
			colorValues[3] = getColorValue(fColorSelectorTransparent3);
			colorValues[4] = getColorValue(fColorSelectorTransparent4);
			colorValues[5] = getColorValue(fColorSelectorTransparent5);
			colorValues[6] = getColorValue(fColorSelectorTransparent6);
			colorValues[7] = getColorValue(fColorSelectorTransparent7);
			colorValues[8] = getColorValue(fColorSelectorTransparent8);
			colorValues[9] = getColorValue(fColorSelectorTransparent9);
			colorValues[10] = getColorValue(fColorSelectorTransparent10);
			colorValues[11] = getColorValue(fColorSelectorTransparent11);
			colorValues[12] = getColorValue(fColorSelectorTransparent12);
			colorValues[13] = getColorValue(fColorSelectorTransparent13);
			colorValues[14] = getColorValue(fColorSelectorTransparent14);
			colorValues[15] = getColorValue(fColorSelectorTransparent15);
			colorValues[16] = getColorValue(fColorSelectorTransparent16);
			colorValues[17] = getColorValue(fColorSelectorTransparent17);
			colorValues[18] = getColorValue(fColorSelectorTransparent18);
			colorValues[19] = getColorValue(fColorSelectorTransparent19);
			colorValues[20] = getColorValue(fColorSelectorTransparent20);

			// set black color when it's checked
			colorValues[21] = isBlack ? 0 : -1;

			mpWrapper.setIsTransparentColors(fChkTransparentPixel.getSelection());
			mpWrapper.setIsTransparentBlack(isBlack);
			mpWrapper.setTransparentColors(colorValues);

			// update viewer
			fTreeViewer.update(tviMapProvider, null);

			updateLiveView();
			enableControls();
		}

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

	private void onModifyZoomSpinnerMax() {

		final int mapMinValue = fSpinMinZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = fSpinMaxZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;

		if (mapMaxValue > MAP_MAX_ZOOM_LEVEL) {
			fSpinMaxZoom.setSelection(MP.UI_MAX_ZOOM_LEVEL);
		}

		if (mapMaxValue < mapMinValue) {
			fSpinMinZoom.setSelection(mapMinValue + 1);
		}

		updateLiveView();
	}

	private void onModifyZoomSpinnerMin() {

		final int mapMinValue = fSpinMinZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = fSpinMaxZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;

		if (mapMinValue > mapMaxValue) {
			fSpinMinZoom.setSelection(mapMaxValue + 1);
		}

		updateLiveView();
	}

	private void onScaleDoubleClick(final Widget widget) {

		final Scale scale = (Scale) widget;
		final int max = scale.getMaximum();

		scale.setSelection(max / 2);

		onModifyProperties();
	}

	private void onSelectMapProvider(final ISelection selection) {

		Object firstElement = ((StructuredSelection) selection).getFirstElement();

		if (firstElement instanceof TVIWmsLayer) {
			// get map provider item
			final TVIWmsLayer tviItem = (TVIWmsLayer) firstElement;
			firstElement = tviItem.getParentItem();
		}

		if (firstElement instanceof TVIMapProvider) {

			final MPWrapper selectedMpWrapper = ((TVIMapProvider) firstElement).getMapProviderWrapper();

			fIsInitUI = true;
			{
				final int alpha = selectedMpWrapper.getAlpha();
				final int brightness = selectedMpWrapper.getBrightness();

				fSpinAlpha.setSelection(alpha);
				fScaleAlpha.setSelection(alpha);

				fChkBrightness.setSelection(selectedMpWrapper.isBrightness());
				fSpinBright.setSelection(brightness);
				fScaleBright.setSelection(brightness);

				fChkTransparentPixel.setSelection(selectedMpWrapper.isTransparentColors());
				fChkTransparentBlack.setSelection(selectedMpWrapper.isTransparentBlack());

				final int[] transColor = selectedMpWrapper.getTransparentColors();
				final int colorLength = transColor == null ? 0 : transColor.length;
				int colorIndex = 0;

				setColorValue(fColorSelectorTransparent0, colorLength > colorIndex++ ? transColor[0] : 0);
				setColorValue(fColorSelectorTransparent1, colorLength > colorIndex++ ? transColor[1] : 0);
				setColorValue(fColorSelectorTransparent2, colorLength > colorIndex++ ? transColor[2] : 0);
				setColorValue(fColorSelectorTransparent3, colorLength > colorIndex++ ? transColor[3] : 0);
				setColorValue(fColorSelectorTransparent4, colorLength > colorIndex++ ? transColor[4] : 0);
				setColorValue(fColorSelectorTransparent5, colorLength > colorIndex++ ? transColor[5] : 0);
				setColorValue(fColorSelectorTransparent6, colorLength > colorIndex++ ? transColor[6] : 0);
				setColorValue(fColorSelectorTransparent7, colorLength > colorIndex++ ? transColor[7] : 0);
				setColorValue(fColorSelectorTransparent8, colorLength > colorIndex++ ? transColor[8] : 0);
				setColorValue(fColorSelectorTransparent9, colorLength > colorIndex++ ? transColor[9] : 0);
				setColorValue(fColorSelectorTransparent10, colorLength > colorIndex++ ? transColor[10] : 0);
				setColorValue(fColorSelectorTransparent11, colorLength > colorIndex++ ? transColor[11] : 0);
				setColorValue(fColorSelectorTransparent12, colorLength > colorIndex++ ? transColor[12] : 0);
				setColorValue(fColorSelectorTransparent13, colorLength > colorIndex++ ? transColor[13] : 0);
				setColorValue(fColorSelectorTransparent14, colorLength > colorIndex++ ? transColor[14] : 0);
				setColorValue(fColorSelectorTransparent15, colorLength > colorIndex++ ? transColor[15] : 0);
				setColorValue(fColorSelectorTransparent16, colorLength > colorIndex++ ? transColor[16] : 0);
				setColorValue(fColorSelectorTransparent17, colorLength > colorIndex++ ? transColor[17] : 0);
				setColorValue(fColorSelectorTransparent18, colorLength > colorIndex++ ? transColor[18] : 0);
				setColorValue(fColorSelectorTransparent19, colorLength > colorIndex++ ? transColor[19] : 0);
				setColorValue(fColorSelectorTransparent20, colorLength > colorIndex++ ? transColor[20] : 0);
			}
			fIsInitUI = false;
		}

		enableControls();
	}

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			fDetailForm.setViewerWidth(fDialogSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}

		// show tile info
		final boolean isShowDebugInfo = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_INFO);
		fChkShowTileInfo.setSelection(isShowDebugInfo);
		fMap.setShowDebugInfo(isShowDebugInfo);

		// is live view
		final boolean isLiveView = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_LIVE_VIEW);
		fChkLiveView.setSelection(isLiveView);
		fIsLiveView = isLiveView;
		fMap.setLiveView(isLiveView);

		// tile image logging
		final boolean isTileImageLogging = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG);
		fChkShowTileImageLog.setSelection(isTileImageLogging);
		fIsTileImageLogging = isTileImageLogging;

		// property container
		final boolean isPropExpanded = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_PROPERTIES_EXPANDED);
		fPropContainer.setExpanded(isPropExpanded);
//		fPropInnerContainer.layout(true);
		fInnerContainer.layout(true);

	}

	private void saveState() {

		fDialogSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, fLeftContainer.getSize().x);
		fDialogSettings.put(DIALOG_SETTINGS_IS_LIVE_VIEW, fChkLiveView.getSelection());
		fDialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_INFO, fChkShowTileInfo.getSelection());
		fDialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG, fChkShowTileImageLog.getSelection());
		fDialogSettings.put(DIALOG_SETTINGS_IS_PROPERTIES_EXPANDED, fPropContainer.isExpanded());
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

		final int mapZoom = fMap.getZoom();
		final GeoPosition mapCenter = fMap.getCenterPosition();

		if (mapZoom < factoryMinZoom) {
			fMap.setZoom(factoryMinZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}

		if (mapZoom > factoryMaxZoom) {
			fMap.setZoom(factoryMaxZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}
	}

	public void tileEvent(final TileEventId tileEventId, final Tile tile) {

		// check if logging is enable
		if (fIsTileImageLogging == false) {
			return;
		}

		final long nanoTime = System.nanoTime();
		fStatUpdateCounter++;

		// update statistics
		if (tileEventId == TileEventId.TILE_RESET_QUEUES) {
			fStatIsQueued = 0;
			fStatStartLoading = 0;
			fStatEndLoading = 0;
		} else if (tileEventId == TileEventId.TILE_IS_QUEUED) {
			fStatIsQueued++;
			tile.setTimeIsQueued(nanoTime);
		} else if (tileEventId == TileEventId.TILE_START_LOADING) {
			fStatStartLoading++;
			tile.setTimeStartLoading(nanoTime);
		} else if (tileEventId == TileEventId.TILE_END_LOADING) {
			fStatEndLoading++;
			fStatIsQueued--;
			tile.setTimeEndLoading(nanoTime);
		} else if (tileEventId == TileEventId.TILE_ERROR_LOADING) {
			fStatErrorLoading++;
			fStatIsQueued--;
		}

		// when stat is cleared, queue can get negative, prevent this
		if (fStatIsQueued < 0) {
			fStatIsQueued = 0;
		}

		// create log entry
		fLogEntries.add(new LogEntry(//
				tileEventId,
				tile,
				nanoTime,
				Thread.currentThread().getName(),
				fStatUpdateCounter));

		/*
		 * create runnable which displays the log
		 */

		final Runnable infoRunnable = new Runnable() {

			final int	fRunnableCounter	= fStatUpdateCounter;

			public void run() {

				// check if this is the last created runnable
				if (fRunnableCounter != fStatUpdateCounter) {
					// a new update event occured
					return;
				}

				if (fLblTileInfo.isDisposed()) {
					// widgets are disposed
					return;
				}

				// show at most 3 decimals
				fLblTileInfo.setText(NLS.bind(Messages.Dialog_MapConfig_TileInfo_Statistics, new Object[] {
						Integer.toString(fStatIsQueued % 1000),
						Integer.toString(fStatEndLoading % 1000),
						Integer.toString(fStatStartLoading % 1000),
						Integer.toString(fStatErrorLoading % 1000), //
				}));

				final String logEntry = displayLogEntries(fLogEntries, fCboTileImageLog);

				// select new entry
				fCboTileImageLog.select(fCboTileImageLog.getItemCount() - 1);

				// display last log in the detail field
				if (logEntry.length() > 0) {
					fTxtLogDetail.setText(logEntry);
				}
			}
		};

		fDisplay.asyncExec(infoRunnable);
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
					fMpProfile.resetParentTiles();
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
			fTreeViewer.update(itemData, null);

			updateLiveView();
		}
	}

	void updateLiveView() {
		if (fIsLiveView) {
			displayProfileMap(false);
		}
	}

	private void updateModelFromUI() {

		/*
		 * !!!! zoom level must be set before any other map methods are called because it
		 * initialized the map with new zoom levels !!!
		 */
		final int oldZoomLevel = fMap.getZoom();
		final GeoPosition mapCenter = fMap.getCenterPosition();

		final int newFactoryMinZoom = fSpinMinZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;
		final int newFactoryMaxZoom = fSpinMaxZoom.getSelection() - MP.UI_MIN_ZOOM_LEVEL;

		// set new zoom level before other map actions are done
		fMpProfile.setZoomLevel(newFactoryMinZoom, newFactoryMaxZoom);

		// ensure the zoom level is in the valid range
		if (oldZoomLevel < newFactoryMinZoom) {
			fMap.setZoom(newFactoryMinZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}
		if (oldZoomLevel > newFactoryMaxZoom) {
			fMap.setZoom(newFactoryMaxZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}

		// keep map position
		fMpProfile.setLastUsedZoom(fMap.getZoom());
		fMpProfile.setLastUsedPosition(fMap.getCenterPosition());

		// set positions of map provider 
		int tblItemIndex = 0;
		for (final TreeItem treeItem : fTreeViewer.getTree().getItems()) {

			final MPWrapper mpWrapper = ((TVIMapProvider) treeItem.getData()).getMapProviderWrapper();

			mpWrapper.setPositionIndex(tblItemIndex++);

			// update wms layer
			final MP mapProvider = mpWrapper.getMP();
			if (mapProvider instanceof MPWms) {

				final MPWms mpWms = (MPWms) mapProvider;

				if (mpWrapper.isDisplayedInMap()) {

					// check if wms is locked, this should not happen but it did
					final ReentrantLock wmsLock = MapProviderManager.getInstance().getWmsLock();
					if (wmsLock.tryLock()) {
						wmsLock.unlock();
					} else {

						StatusUtil.showStatus(Messages.DBG044_Wms_Error_WmsIsLocked, new Exception());

						mpWrapper.setEnabled(false);
						continue;
					}

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

		MPProfile.sortMpWrapper(fMpProfile.getAllWrappers());
		MPProfile.updateWrapperTileFactory(fMpProfile.getAllWrappers());
	}

	private void updateMVAlpha() {

		TVIMapProvider tviMapProvider = null;

		final Object firstElement = ((StructuredSelection) fTreeViewer.getSelection()).getFirstElement();
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
			final int newAlpha = fSpinAlpha.getSelection();
			mpWrapper.setAlpha(newAlpha);

			// update viewer
			fTreeViewer.update(tviMapProvider, null);
		}
	}

	private void updateMVBrightness() {

		final Object firstElement = ((StructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		if (firstElement instanceof TVIMapProvider) {

			// map provider is selected

			final TVIMapProvider tviMapProvider = (TVIMapProvider) firstElement;

			// update brightness in the map provider 

			final MPWrapper mpWrapper = tviMapProvider.getMapProviderWrapper();

			mpWrapper.setIsBrightness(fChkBrightness.getSelection());
			mpWrapper.setBrightness(fScaleBright.getSelection());

			// update viewer
			fTreeViewer.update(tviMapProvider, null);

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
				fTreeViewer.update(tviParent, null);
			}
		}
	}

	/**
	 * Initialize UI from the model and display the viewer content
	 * 
	 * @param mapProfile
	 */
	private void updateUIFromModel(final MPProfile mapProfile) {

		fMpProfile = mapProfile;

		fIsInitUI = true;
		{
			// zoom level
			final int minZoomLevel = fMpProfile.getMinZoomLevel();
			final int maxZoomLevel = fMpProfile.getMaxZoomLevel();
			fSpinMinZoom.setSelection(minZoomLevel + MP.UI_MIN_ZOOM_LEVEL);
			fSpinMaxZoom.setSelection(maxZoomLevel + MP.UI_MIN_ZOOM_LEVEL);

			final int color = fMpProfile.getBackgroundColor();
			fColorImageBackground.setColorValue(new RGB(
					(color & 0xFF) >>> 0,
					(color & 0xFF00) >>> 8,
					(color & 0xFF0000) >>> 16));

		}
		fIsInitUI = false;

		// show map provider in the message area
		fDefaultMessage = NLS.bind(Messages.Dialog_MapConfig_DialogArea_Message, fMpProfile.getName());
		setMessage(fDefaultMessage);

		final ArrayList<MPWrapper> allMpWrappers = fMpProfile.getAllWrappers();

		MPProfile.sortMpWrapper(allMpWrappers);
		MPProfile.updateWrapperTileFactory(allMpWrappers);

		// set factory this is required when zoom and position is set
		fMap.resetTileFactory(fMpProfile);

		// set position to previous position
		fMap.setZoom(fMpProfile.getLastUsedZoom());
		fMap.setGeoCenterPosition(fMpProfile.getLastUsedPosition());

		// create root item and update viewer
		fRootItem = new TVIMapProviderRoot(fTreeViewer, allMpWrappers);
		fTreeViewer.setInput(fRootItem);
	}
}
