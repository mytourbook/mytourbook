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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.map.TileFactoryInfo;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.IMapListener;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.MapEvent;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.preferences.PrefPageMapProviders;
import de.byteholder.geoclipse.ui.ViewerDetailForm;
import de.byteholder.geoclipse.util.PixelConverter;
import de.byteholder.gpx.GeoPosition;

public class DialogMPWms extends DialogMP implements ITileListener, IMapDefaultActions {

	private static final String				DIALOG_SETTINGS_VIEWER_WIDTH			= "viewerWidth";							//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_SHOW_TILE_INFO		= "isShowTileInfo";						//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG	= "IsShowTileImageLogging";				//$NON-NLS-1$

	/*
	 * UI components
	 */
	private Display							fDisplay;
	private Composite						fLeftContainer;
	private ViewerDetailForm				fDetailForm;

	private CheckboxTableViewer				fLayerViewer;
	private Composite						fViewerContainer;

	private Combo							fComboImageSize;
	private Label							fLblMapInfo;
	private Label							fLblTileInfo;

	private Combo							fComboImageFormat;
	private Button							fBtnOk;

	/*
	 * next/prev buttons are disabled because the offline folder is wront
	 */
//	private Button							fBtnPrevMapProvider;
//	private Button							fBtnNextMapProvider;
	private Image							fUpImage;
	private Image							fDownImage;

	private ToolBar							fToolbar;
	private Button							fBtnShowMap;
	private Button							fBtnShowOsmMap;

	private Button							fChkLoadTransparentImages;
	private Button							fChkShowTileInfo;
	private Button							fChkShowTileImageLog;

	private Combo							fCboTileImageLog;
	private Text							fTxtLogDetail;

	private FormToolkit						fFormTk									= new FormToolkit(Display
																							.getCurrent());
	private ExpandableComposite				fLogContainer;

	/*
	 * none UI fields
	 */
	private final IDialogSettings			fDialogSettings;

	private PrefPageMapProviders			fPrefPageMapFactory;

	/**
	 * all visible {@link MtLayer}'s
	 */
	private ArrayList<MtLayer>				fAllMtLayers							= new ArrayList<MtLayer>();
	private ArrayList<MtLayer>				fDisplayedLayers						= new ArrayList<MtLayer>();

	private int								fStatIsQueued;
	private int								fStatStartLoading;
	private int								fStatEndLoading;
	private int								fStatErrorLoading;

	private String							fDefaultMessage;

	private String							fTileUrl;
	private long							fDragStartViewerLeft;

	private NumberFormat					fNfLatLon								= NumberFormat.getNumberInstance();
	{
		// initialize lat/lon formatter
		fNfLatLon.setMinimumFractionDigits(6);
		fNfLatLon.setMaximumFractionDigits(6);
	}

	private int								fStatUpdateCounter						= 0;

	private MPWms							fMpWms;

	private MPPlugin						fDefaultMapProvider;
	private TileFactory						fDefaultTileFactory;

	// load tile image logging
	private boolean							fIsTileImageLogging;
	private ConcurrentLinkedQueue<LogEntry>	fLogEntries								= new ConcurrentLinkedQueue<LogEntry>();

	public DialogMPWms(final Shell parentShell, final PrefPageMapProviders mapFactory, final MPWms wmsMapProvider) {

		super(parentShell, wmsMapProvider);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		fDialogSettings = Activator.getDefault().getDialogSettingsSection("DialogWmsConfiguration");//$NON-NLS-1$

		fPrefPageMapFactory = mapFactory;
		fMpWms = wmsMapProvider;

		fDefaultMapProvider = MapProviderManager.getInstance().getDefaultMapProvider();
		fDefaultTileFactory = fDefaultMapProvider.getTileFactory(true);
	}

	void actionSetZoomToShowEntireLayer() {

		// get rectangle from first layer

		if (fDisplayedLayers.size() == 0) {
			return;
		}

		final MtLayer mtLayer = fDisplayedLayers.get(0);

		final HashSet<GeoPosition> layerBounds = new HashSet<GeoPosition>();
		layerBounds.add(mtLayer.getUpperGeoPosition());
		layerBounds.add(mtLayer.getLowerGeoPosition());

		setZoomFromBounds(layerBounds);

		updateMapPosition();

		fMap.queueMapRedraw();
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
		fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
		fMap.queueMapRedraw();
	}

	/**
	 * @return Returns <code>true</code> when the data are valid and set's the error message
	 *         accordingly
	 */
	private boolean checkValidation() {

		final boolean isValid = true;

//		final WMSCapabilities wmsCaps = fMapProvider.getWmsCaps();
//
//		if (wmsCaps == null) {
//			setErrorMessage(Messages.pref_map_validationError_capsIsInvalid);
//			isValid = false;
////		} else if (fIsCapsUrlChecked == false) {
////			setErrorMessage(Messages.pref_map_validationError_capsUrlIsNotChecked);
////			isValid = false;
//		} else {
//
//			try {
//				new URL(fTxtMapUrl.getText());
//			} catch (final MalformedURLException e) {
//				setErrorMessage(Messages.pref_map_validationError_invalidUrl);
//				fTxtMapUrl.setFocus();
//			}
//		}
//
//		if (isValid) {
//			setErrorMessage(null);
//		}
//
//		enableControls(isValid);

		return isValid;
	}

	@Override
	public boolean close() {

		// clear loading queue
		fMpWms.getTileFactory(true).resetAll(false);

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_WmsConfig_DialogTitle);

		fDownImage = Activator.getImageDescriptor(Messages.App_Image_Up).createImage();
		fUpImage = Activator.getImageDescriptor(Messages.App_Image_Down).createImage();

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

		setTitle(Messages.Dialog_WmsConfig_DialogArea_Title);

		TileFactory.addTileListener(this);

		restoreState();

		// initialize after the shell size is set
		initializeUIFromModel(fMpWms);

		// force the viewer to do the layout to remove horizontal scollbar
		fViewerContainer.layout();

		enableControls();
	}

	private void createActions() {

		final ToolBarManager tbm = new ToolBarManager(fToolbar);

		tbm.add(new ActionZoomIn(this));
		tbm.add(new ActionZoomOut(this));
		tbm.add(new ActionZoomOutToMinZoom(this));

		tbm.add(new Separator());

		tbm.add(new ActionZoomShowEntireLayer(this));
		tbm.add(new ActionShowFavoritePos(this));
		tbm.add(new ActionSetFavoritePosition(this));

		tbm.update(true);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		fBtnOk = getButton(IDialogConstants.OK_ID);
		fBtnOk.setText(Messages.Dialog_MapConfig_Button_Save);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Control contents = super.createContents(parent);

		return contents;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);
		createActions();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUIContainer(container, pixelConverter);
			createUILog(container, pixelConverter);
		}
	}

	private void createUIContainer(final Composite parent, final PixelConverter pixelConverter) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			// left part (layer selection)
			fLeftContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 5, 0, 0).applyTo(fLeftContainer);
			createUILayer(fLeftContainer, pixelConverter);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);
			UI.addSashColorHandler(sash);

			// right part (map)
			final Composite mapContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(mapContainer);
			createUIMap(mapContainer, pixelConverter);

			fDetailForm = new ViewerDetailForm(container, fLeftContainer, sash, mapContainer, 30);
		}
	}

	private void createUILayer(final Composite parent, final PixelConverter pixelConverter) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Composite viewerContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(viewerContainer);
			GridLayoutFactory.fillDefaults().spacing(0, 2).applyTo(viewerContainer);
			{
				// header
				createUILayer10Header(viewerContainer);

				// table: layers
				createUILayer20Viewer(viewerContainer, pixelConverter);

				// label: hint
				label = new Label(viewerContainer, SWT.NONE);
				label.setText(Messages.Dialog_WmsConfig_Label_DndHint);
			}

			// ############################################################

			createUILayer30Details(container, pixelConverter);
		}
	}

	private void createUILayer10Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			// label: wms layer
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
//					.indent(-3, 0)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Dialog_WmsConfig_Label_Layers);

			// ############################################################

			/*
			 * next/prev buttons are disabled because the offline folder is wront
			 */
//			// button: previous map provider
//			fBtnPrevMapProvider = new Button(container, SWT.NONE);
//			fBtnPrevMapProvider.setToolTipText(Messages.Dialog_WmsConfig_Button_PreviousMapProvider_tooltip);
//			fBtnPrevMapProvider.setImage(fUpImage);
//			fBtnPrevMapProvider.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onSelectPreviousMapProvider();
//				}
//			});
//			setButtonLayoutData(fBtnPrevMapProvider);
//
//			// ############################################################
//
//			// button: next map provider
//			fBtnNextMapProvider = new Button(container, SWT.NONE);
//			fBtnNextMapProvider.setToolTipText(Messages.Dialog_WmsConfig_Button_NextMapProvider_tooltip);
//			fBtnNextMapProvider.setImage(fDownImage);
//			fBtnNextMapProvider.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onSelectNextMapProvider();
//				}
//			});
//			setButtonLayoutData(fBtnNextMapProvider);
		}
	}

	private void createUILayer20Viewer(final Composite parent, final PixelConverter pixelConverter) {

		final TableColumnLayout tableLayout = new TableColumnLayout();
		fViewerContainer = new Composite(parent, SWT.NONE);
		fViewerContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fViewerContainer);

		/*
		 * create table
		 */
		final Table table = new Table(fViewerContainer, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fLayerViewer = new CheckboxTableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: layer title
		tvc = new TableViewerColumn(fLayerViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_WmsConfig_Column_LayerName);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MtLayer mtLayer = (MtLayer) cell.getElement();

				cell.setText(mtLayer.getGeoLayer().getTitle());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(20, true));

		// column: layer id
		tvc = new TableViewerColumn(fLayerViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_WmsConfig_Column_LayerId);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MtLayer mtLayer = (MtLayer) cell.getElement();

				cell.setText(mtLayer.getGeoLayer().getName());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(10, true));

		// column: bbox
		tvc = new TableViewerColumn(fLayerViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_WmsConfig_Column_Bbox);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MtLayer mtLayer = (MtLayer) cell.getElement();

				final GeoPosition lowerPosition = mtLayer.getLowerGeoPosition();
				final GeoPosition upperPosition = mtLayer.getUpperGeoPosition();

				final StringBuilder sb = new StringBuilder();

				sb.append(lowerPosition.getLatitude());
				sb.append(", "); //$NON-NLS-1$
				sb.append(lowerPosition.getLongitude());
				sb.append(" / "); //$NON-NLS-1$
				sb.append(upperPosition.getLatitude());
				sb.append(", "); //$NON-NLS-1$
				sb.append(upperPosition.getLongitude());

				cell.setText(sb.toString());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(20, true));

		fLayerViewer.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				final int mtLayerSize = fAllMtLayers.size();
				if (mtLayerSize == 0) {
					return null;
				} else {
					return fAllMtLayers.toArray(new MtLayer[mtLayerSize]);
				}
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		fLayerViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// select the checked item
				fLayerViewer.setSelection(new StructuredSelection(event.getElement()));

				// set focus to selected layer
				table.setSelection(table.getSelectionIndex());

				onCheckLayer(event.getElement());
			}
		});

		fLayerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectLayer();
			}
		});

		/*
		 * set drag adapter
		 */
		fLayerViewer.addDragSupport(
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
						final ISelection selection = fLayerViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(fDragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(fLayerViewer) {

			private Widget	fTableItem;

			@Override
			public void dragOver(final DropTargetEvent event) {

				// keep table item
				fTableItem = event.item;

				super.dragOver(event);
			}

			@Override
			public boolean performDrop(final Object droppedData) {

				if (droppedData instanceof StructuredSelection) {

					final Object firstElement = ((StructuredSelection) droppedData).getFirstElement();
					if (firstElement instanceof MtLayer) {

						final MtLayer mtLayer = (MtLayer) firstElement;

						final int location = getCurrentLocation();
						final Table layerTable = fLayerViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == fDragStartViewerLeft) {
							fLayerViewer.remove(mtLayer);
						}

						int tableIndex;

						if (fTableItem == null) {

							fLayerViewer.add(mtLayer);
							tableIndex = layerTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							tableIndex = layerTable.indexOf((TableItem) fTableItem);
							if (tableIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								fLayerViewer.insert(mtLayer, tableIndex);
							} else if (location == LOCATION_AFTER) {
								fLayerViewer.insert(mtLayer, ++tableIndex);
							}
						}

						// set check state
						fLayerViewer.setChecked(mtLayer, mtLayer.isDisplayedInMap());

						// reselect filter item
						fLayerViewer.setSelection(new StructuredSelection(mtLayer));

						// set focus to selection
						layerTable.setSelection(tableIndex);
						layerTable.setFocus();

						// update new layer topology in the map
						updateMap(false);

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection instanceof StructuredSelection) {
					final Object draggedItem = ((StructuredSelection) selection).getFirstElement();
					if (target == draggedItem) {
						return false;
					}
				}

				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
					return false;
				}

				// check drop location
				final int location = getCurrentLocation();
				if ((location == LOCATION_AFTER || location == LOCATION_BEFORE) == false) {
					return false;
				}

				return true;
			}

		};

		fLayerViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUILayer30Details(final Composite parent, final PixelConverter pixelConverter) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 10).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			final Composite leftContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(leftContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(leftContainer);
			{
				// label: image size
				label = new Label(leftContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(Messages.Dialog_WmsConfig_Label_ImageSize);

				// combo: image size
				fComboImageSize = new Combo(leftContainer, SWT.BORDER | SWT.READ_ONLY);
				fComboImageSize.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectImageSize();
					}
				});
				// set content
				for (final String imageSize : MapProviderManager.IMAGE_SIZE) {
					fComboImageSize.add(imageSize);
				}

				// ############################################################

				// label: image format
				label = new Label(leftContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(Messages.Dialog_WmsConfig_Label_ImageFormat);

				// combo: image format
				fComboImageFormat = new Combo(leftContainer, SWT.BORDER | SWT.READ_ONLY);
				GridDataFactory
						.fillDefaults()
						.hint(pixelConverter.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(fComboImageFormat);
				fComboImageFormat.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectImageFormat();
					}
				});
			}

			// ############################################################

			final Composite rightContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().indent(10, 0).applyTo(rightContainer);
			GridLayoutFactory.fillDefaults().applyTo(rightContainer);
			{
				// check: get transparent tiles
				fChkLoadTransparentImages = new Button(rightContainer, SWT.CHECK);
				GridDataFactory.fillDefaults().applyTo(fChkLoadTransparentImages);
				fChkLoadTransparentImages.setText(Messages.Dialog_WmsConfig_Button_GetTransparentMap);
				fChkLoadTransparentImages.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectTransparentImage();
					}
				});

				// ############################################################

				// check: show tile info
				fChkShowTileInfo = new Button(rightContainer, SWT.CHECK);
				GridDataFactory.fillDefaults().applyTo(fChkShowTileInfo);
				fChkShowTileInfo.setText(Messages.Dialog_MapConfig_Button_ShowTileInfo);
				fChkShowTileInfo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						fMap.setShowDebugInfo(fChkShowTileInfo.getSelection());
					}
				});

				// ############################################################

				// check: show tile image loading log
				fChkShowTileImageLog = new Button(rightContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
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
	}

	private void createUILog(final Composite parent, final PixelConverter pixelConverter) {

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

	private void createUIMap(final Composite parent, final PixelConverter pixelConverter) {

		final Composite toolbarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbarContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(toolbarContainer);
		{
			// button: update map
			fBtnShowMap = new Button(toolbarContainer, SWT.NONE);
			fBtnShowMap.setText(Messages.Dialog_WmsConfig_Button_UpdateMap);
			fBtnShowMap.setToolTipText(Messages.Dialog_WmsConfig_Button_UpdateMap_Tooltip);
			fBtnShowMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectWmsMap();
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
					onSelectOsmMap();
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
		 * !!! don't do any map initialization until the tile factory is set !!!
		 */
	}

	private void enableControls() {

		fBtnShowOsmMap.setEnabled(fMpWms.getImageSize() == MapProviderManager.OSM_IMAGE_SIZE);

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

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;

		// disable bounds
		// return null;
	}

	private Rectangle getPositionBounds(final Set<GeoPosition> positions, final int zoom) {

		final TileFactory tileFactory = fMap.getTileFactory();

		final GeoPosition pos1 = positions.iterator().next();
		final java.awt.Point point1 = tileFactory.geoToPixel(pos1, zoom);

		final Rectangle rect = new Rectangle(point1.x, point1.y, 0, 0);

		for (final GeoPosition pos : positions) {

			final java.awt.Point point = tileFactory.geoToPixel(pos, zoom);

			rect.add(new Rectangle(point.x, point.y, 0, 0));
		}
		return rect;
	}

	private void initializeUIFromModel(final MPWms mapProvider) {

		fMpWms = mapProvider;

		/*
		 * get layers
		 */
		fAllMtLayers.clear();
		final ArrayList<MtLayer> allMtLayers = fMpWms.getMtLayers();
		fAllMtLayers.addAll(allMtLayers);

		// check layers
		if (fAllMtLayers.size() == 0) {
			StatusUtil.showStatus(
					NLS.bind(Messages.DBG034_Wms_Error_LayersAreNotAvailable, fMpWms.getName()),
					new Exception());
			return;
		}

		// sort layers by position or name
		Collections.sort(fAllMtLayers);

		/*
		 * set fields
		 */
		fChkLoadTransparentImages.setSelection(fMpWms.isTransparent());

		/*
		 * url: set initially the caps url, this will be overwritten from the tile url when a tile
		 * is loaded
		 */
		fTileUrl = fMpWms.getCapabilitiesUrl();
		fCboTileImageLog.add(fTileUrl);

		/*
		 * image size
		 */
		final String imageSize = Integer.toString(fMpWms.getImageSize());
		int listIndex = 0;
		int sizeIndex = -1;
		for (final String listImageSize : MapProviderManager.IMAGE_SIZE) {
			if (listImageSize.equals(imageSize)) {
				sizeIndex = listIndex;
				break;
			}
			listIndex++;
		}
		if (sizeIndex == -1) {
			sizeIndex = 0;
			// correct map provider value
			fMpWms.setImageSize(Integer.parseInt(MapProviderManager.IMAGE_SIZE[0]));
		}
		fComboImageSize.select(sizeIndex);

		/*
		 * set image format
		 */
		final String currentImageFormat = fMpWms.getImageFormat();
		int formatIndex = 0;
		int selectedFormatIndex = -1;
		fComboImageFormat.removeAll();
		for (final String imageFormat : fMpWms.getImageFormats()) {

			fComboImageFormat.add(imageFormat);

			if (currentImageFormat.equalsIgnoreCase(imageFormat)) {
				selectedFormatIndex = formatIndex;
			}

			formatIndex++;
		}
		// select current format
		if (selectedFormatIndex == -1) {
			fComboImageFormat.select(0);
		} else {
			fComboImageFormat.select(selectedFormatIndex);
		}

		// show map provider in the message area
		fDefaultMessage = NLS.bind(Messages.Dialog_MapConfig_DialogArea_Message, fMpWms.getName());
		setMessage(fDefaultMessage);

		// show layers into the viewer
		fLayerViewer.setInput(this);

		/*
		 * check layers
		 */
		final ArrayList<MtLayer> checkedLayers = new ArrayList<MtLayer>();
		for (final MtLayer mtLayer : allMtLayers) {
			if (mtLayer.isDisplayedInMap()) {
				checkedLayers.add(mtLayer);
			}
		}
		if (checkedLayers.size() > 0) {
			fLayerViewer.setCheckedElements(checkedLayers.toArray());
		}

		enableControls();

		updateMap(true);
	}

	@Override
	protected void okPressed() {

		if (checkValidation() == false) {
			return;
		}

		super.okPressed();
	}

	/**
	 * a layer is checked
	 * 
	 * @param checkedLayer
	 *            layer which is checked/unchecked
	 */
	private void onCheckLayer(final Object checkedLayer) {

		/*
		 * check if a layer is checked, if not, check currently unchecked layer
		 */
		final TableItem[] tableItems = fLayerViewer.getTable().getItems();
		int checkedLayers = 0;

		for (final TableItem tableItem : tableItems) {
			if (tableItem.getChecked()) {
				checkedLayers++;
			}
		}

		if (checkedLayers == 0) {

			// recheck current layer when no layer is checked

			fLayerViewer.setChecked(checkedLayer, true);
		}

		updateMap(false);
	}

	private void onDispose() {

		// dispose images
		if (fUpImage != null && fUpImage.isDisposed() == false) {
			fUpImage.dispose();
		}
		if (fDownImage != null && fDownImage.isDisposed() == false) {
			fDownImage.dispose();
		}
		if (fFormTk != null) {
			fFormTk.dispose();
		}

		TileFactory.removeTileListener(DialogMPWms.this);
	}

	private void onSelectImageFormat() {

		final String oldValue = fMpWms.getImageFormat();
		final String newValue = fComboImageFormat.getItem(fComboImageFormat.getSelectionIndex());

		if (oldValue.equals(newValue)) {
			return;
		}

		// keep current position and zoom-level
		final GeoPosition center = fMap.getCenterPosition();
		final int zoom = fMap.getZoom();

		// set image format
		fMpWms.setImageFormat(newValue);

		resetMap(center, zoom);
	}

	private void onSelectImageSize() {

		final int oldValue = fMpWms.getImageSize();
		final int newValue = Integer.parseInt(MapProviderManager.IMAGE_SIZE[fComboImageSize.getSelectionIndex()]);

		if (oldValue == newValue) {
			return;
		}

		// keep current position and zoom-level
		final GeoPosition center = fMap.getCenterPosition();
		final int zoom = fMap.getZoom();

		// set image size and initialize tile factory
		fMpWms.setImageSize(newValue);

		enableControls();

		resetMap(center, zoom);
	}

	private void onSelectLayer() {

		final int oldValue = fMpWms.getImageSize();
		final int newValue = Integer.parseInt(MapProviderManager.IMAGE_SIZE[fComboImageSize.getSelectionIndex()]);

		if (oldValue == newValue) {
			return;
		}

		// set image size and initialize tile factory
		fMpWms.setImageSize(newValue);
	}

//	private void onSelectNextMapProvider() {
//
//		final MapProviderNavigator mapProviderNavigator = fPrefPageMapFactory.getNextMapProvider();
//
//		if (mapProviderNavigator == null) {
//			// there is no next map provider
//			fBtnNextMapProvider.setEnabled(false);
//			return;
//		}
//
//		// keep layer data for the current map provider
//		updateModelFromUI();
//
//		fBtnPrevMapProvider.setEnabled(true);
//		fBtnNextMapProvider.setEnabled(mapProviderNavigator.canMoveFurther);
//
//		initializeUIFromModel(mapProviderNavigator.mapProvider);
//	}

	private void onSelectOsmMap() {

		if (fMap.getTileFactory() == fDefaultTileFactory) {

			// toggle map, display wms

			onSelectWmsMap();

		} else {

			// display OSM

			fDefaultMapProvider.setStateToReloadOfflineCounter();

			// ensure the map is using the correct zoom levels
			setMapZoomLevelFromInfo(fDefaultTileFactory.getInfo());

			fMap.resetTileFactory(fDefaultTileFactory);
		}
	}

//	private void onSelectPreviousMapProvider() {
//
//		final MapProviderNavigator mapProviderNavigator = fPrefPageMapFactory.getPreviousMapProvider();
//
//		if (mapProviderNavigator == null) {
//			// there is no previous map provider
//			fBtnPrevMapProvider.setEnabled(false);
//			return;
//		}
//
//		// keep layer data for the current map provider
//		updateModelFromUI();
//
//		fBtnNextMapProvider.setEnabled(true);
//		fBtnPrevMapProvider.setEnabled(mapProviderNavigator.canMoveFurther);
//
//		initializeUIFromModel(mapProviderNavigator.mapProvider);
//	}

	private void onSelectTransparentImage() {

		fMpWms.setTransparent(fChkLoadTransparentImages.getSelection());

		// reset all images
		fMpWms.getTileFactory(true).resetAll(true);

		// delete offline images because they are invalid for the new image size
		fPrefPageMapFactory.deleteOfflineMap(fMpWms);

		// display map with new image size
		fMap.queueMapRedraw();
	}

	private void onSelectWmsMap() {

		// check if the tile factory has changed
		final TileFactory customTileFactory = fMpWms.getTileFactory(true);
		if (fMap.getTileFactory() != customTileFactory) {

			/*
			 * select wms map provider
			 */
			/**
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
			 * <br>
			 * ensure the map is using the correct zoom levels before other map actions are done<br>
			 * <br>
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
			 */
			setMapZoomLevelFromInfo(customTileFactory.getInfo());

			fMap.resetTileFactory(customTileFactory);
		}

		updateModelFromUI();

		// reset all images
		fMpWms.getTileFactory(true).resetAll(false);

		// display map 
		fMap.queueMapRedraw();
	}

	private void resetMap(final GeoPosition center, final int zoom) {

		// reset all images
		fMpWms.getTileFactory(true).resetAll(false);

		// delete offline images because they are invalid for the new image size
		fPrefPageMapFactory.deleteOfflineMap(fMpWms);

		fMap.setZoom(zoom);
		fMap.setGeoCenterPosition(center);

		// display map 
		fMap.queueMapRedraw();
	}

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			fDetailForm.setViewerWidth(fDialogSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}

		// debug tile info
		final boolean isShowDebugInfo = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_INFO);
		fChkShowTileInfo.setSelection(isShowDebugInfo);
		fMap.setShowDebugInfo(isShowDebugInfo);

		// tile image logging
		final boolean isShowImageLogging = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG);
		fChkShowTileImageLog.setSelection(isShowImageLogging);
	}

	private void saveState() {

		fDialogSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, fLeftContainer.getSize().x);
		fDialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_INFO, fChkShowTileInfo.getSelection());
		fDialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG, fChkShowTileImageLog.getSelection());

		updateModelFromUI();
	}

	/**
	 * ensure the map is using the correct zoom levels
	 */
	private void setMapZoomLevelFromInfo(final TileFactoryInfo factoryInfo) {

		final int factoryMinZoom = factoryInfo.getMinimumZoomLevel();
		final int factoryMaxZoom = factoryInfo.getMaximumZoomLevel();

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

	/**
	 * Calculates a zoom level so that all points in the specified set will be visible on screen.
	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
	 * so that the entire city and it's points are visible without panning.
	 * 
	 * @param positions
	 *            A set of GeoPositions to calculate the new zoom from
	 * @param adjustZoomLevel
	 *            when <code>true</code> the zoom level will be adjusted to user settings
	 */
	private void setZoomFromBounds(final Set<GeoPosition> positions) {

		if (positions == null || positions.size() < 2) {
			return;
		}

		final TileFactory tileFactory = fMap.getTileFactory();
		final TileFactoryInfo tileInfo = tileFactory.getInfo();

		final int maximumZoomLevel = tileInfo.getMaximumZoomLevel();
		int zoom = tileInfo.getMinimumZoomLevel();

		Rectangle2D positionRect = getPositionBounds(positions, zoom);

//		if (positionRect.getY() < 0.0) {
//			positionRect.setRect(positionRect.getX(), 0.0d, positionRect.getWidth(), positionRect.getHeight());
//		}

		java.awt.Rectangle viewport = fMap.getViewport();

//		System.out.println();
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		for (final GeoPosition geoPosition : positions) {
//			System.out.println(geoPosition.getLongitude() + ", " + geoPosition.getLatitude()); //$NON-NLS-1$
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}
//
//		System.out.println();
//		dump("vp0:", viewport, positionRect);
//		System.out.println();
		// TODO remove SYSTEM.OUT.PRINTLN

//		// zoom IN until the tour is visible in the map
//		while (!viewport.contains(positionRect)) {
//
//			System.out.println();
//			dump("vp1:", viewport, positionRect);
//			System.out.println();
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			// center map
//			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
//					positionRect.getY() + positionRect.getHeight() / 2);
//
//			final GeoPosition devCenter = tileFactory.pixelToGeo(center, zoom);
//			fMap.setGeoCenterPosition(devCenter);
//
//			// check zoom level
//			if (++zoom >= maximumZoomLevel) {
//				break;
//			}
//			fMap.setZoom(zoom);
//
//			positionRect = getBoundingRect(positions, zoom);
//			viewport = fMap.getViewport();
//		}
//
//		System.out.println();
//		// TODO remove SYSTEM.OUT.PRINTLN

		// zoom IN until the tour is larger than the viewport
		while (positionRect.getWidth() < viewport.width && positionRect.getHeight() < viewport.height) {

//			System.out.println();
//			dump("vp2:", viewport, positionRect);
//			System.out.println();
//			// TODO remove SYSTEM.OUT.PRINTLN

			// center position in the map
			final Point2D center = new Point2D.Double(//
					positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);

			final GeoPosition devCenter = tileFactory.pixelToGeo(center, zoom);
			fMap.setGeoCenterPosition(devCenter);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}

			fMap.setZoom(zoom);

			positionRect = getPositionBounds(positions, zoom);
			viewport = fMap.getViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		fMap.setZoom(zoom);
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

		// when stat is cleared, que can get negative, prevent this
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
				fTxtLogDetail.setText(logEntry);
			}
		};

		fDisplay.asyncExec(infoRunnable);
	}

	/**
	 * update position and check state in all layers
	 * 
	 * @return Returns the number of layers which are displayed in the map
	 */
	private int updateLayerState() {

		fDisplayedLayers.clear();

		int itemIndex = 0;
		int visibleLayers = 0;
		for (final TableItem tableItem : fLayerViewer.getTable().getItems()) {

			final MtLayer mtLayer = (MtLayer) tableItem.getData();
			final boolean isChecked = tableItem.getChecked();

			mtLayer.setIsDisplayedInMap(isChecked);
			mtLayer.setPositionIndex(itemIndex++);

			if (isChecked) {
				fDisplayedLayers.add(mtLayer);
				visibleLayers++;
			}
		}

		return visibleLayers;
	}

	/**
	 * sets the tile factory in the map
	 */
	private void updateMap(final boolean isUpdatePosition) {

		final int visibleLayers = updateLayerState();

		if (visibleLayers == 0) {
			// there is nothing which can be displayed
			return;
		}

		// update layers BEFORE the tile factory is set
		fMpWms.initializeLayers();

		// set factory and display map
		final TileFactory tileFactory = fMpWms.getTileFactory(true);
		fMap.resetTileFactory(tileFactory);

		if (isUpdatePosition) {
			// set position to previous position
			fMap.setZoom(fMpWms.getLastUsedZoom());
			fMap.setGeoCenterPosition(fMpWms.getLastUsedPosition());
		}

	}

	/*
	 * keep zoom+position
	 */
	private void updateMapPosition() {
		fMpWms.setLastUsedZoom(fMap.getZoom());
		fMpWms.setLastUsedPosition(fMap.getCenterPosition());
	}

	private void updateModelFromUI() {

		updateLayerState();
		updateMapPosition();
	}

}
