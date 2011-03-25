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
package de.byteholder.geoclipse.mapprovider;

import java.awt.Point;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.util.StatusUtil;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.IPositionListener;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.MapPositionEvent;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.preferences.PrefPageMapProviders;
import de.byteholder.geoclipse.ui.ViewerDetailForm;
import de.byteholder.gpx.GeoPosition;

public class DialogMPWms extends DialogMP implements ITileListener, IMapDefaultActions {

	private static final String						DIALOG_SETTINGS_VIEWER_WIDTH			= "viewerWidth";							//$NON-NLS-1$
	private static final String						DIALOG_SETTINGS_IS_SHOW_TILE_INFO		= "isShowTileInfo";						//$NON-NLS-1$
	private static final String						DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG	= "IsShowTileImageLogging";				//$NON-NLS-1$

	/*
	 * UI components
	 */
	private Display									_display;
	private Composite								_leftContainer;
	private ViewerDetailForm						_detailForm;

	private CheckboxTableViewer						_layerViewer;
	private Composite								_viewerContainer;

	private Combo									_comboImageSize;
	private Label									_lblMapInfo;
	private Label									_lblTileInfo;

	private Combo									_cboImageFormat;
	private Button									_btnOk;

	/*
	 * next/prev buttons are disabled because the offline folder is wront
	 */
//	private Button							fBtnPrevMapProvider;
//	private Button							fBtnNextMapProvider;

	private ToolBar									_toolbar;
	private Button									_btnShowMap;
	private Button									_btnShowOsmMap;

	private Button									_chkLoadTransparentImages;
	private Button									_chkShowTileInfo;
	private Button									_chkShowTileImageLog;

	private Combo									_cboTileImageLog;
	private Text									_txtLogDetail;

	private final FormToolkit						_formTk									= new FormToolkit(
																									Display
																											.getCurrent());
	private ExpandableComposite						_logContainer;

	/*
	 * none UI fields
	 */
	private final IDialogSettings					_dialogSettings;

	private final PrefPageMapProviders				_prefPageMapFactory;

	/**
	 * all visible {@link MtLayer}'s
	 */
	private final ArrayList<MtLayer>				_allMtLayers							= new ArrayList<MtLayer>();
	private final ArrayList<MtLayer>				_displayedLayers						= new ArrayList<MtLayer>();

	private int										_statIsQueued;
	private int										_statStartLoading;
	private int										_statEndLoading;
	private int										_statErrorLoading;

	private String									_defaultMessage;

	private String									_tileUrl;
	private long									_dragStartViewerLeft;

	private final NumberFormat						_nfLatLon								= NumberFormat
																									.getNumberInstance();
	{
		// initialize lat/lon formatter
		_nfLatLon.setMinimumFractionDigits(6);
		_nfLatLon.setMaximumFractionDigits(6);
	}

	private int										_statUpdateCounter						= 0;

	private MPWms									_mpWms;

	private final MPPlugin							_defaultMapProvider;

	// load tile image logging
	private boolean									_isTileImageLogging;
	private final ConcurrentLinkedQueue<LogEntry>	_logEntries								= new ConcurrentLinkedQueue<LogEntry>();

	private PixelConverter							_pc;

	public DialogMPWms(final Shell parentShell, final PrefPageMapProviders mapFactory, final MPWms wmsMapProvider) {

		super(parentShell, wmsMapProvider);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		_dialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection("DialogWmsConfiguration");//$NON-NLS-1$

		_prefPageMapFactory = mapFactory;
		_mpWms = wmsMapProvider;

		_defaultMapProvider = MapProviderManager.getInstance().getDefaultMapProvider();
	}

	void actionSetZoomToShowEntireLayer() {

		// get rectangle from first layer

		if (_displayedLayers.size() == 0) {
			return;
		}

		final MtLayer mtLayer = _displayedLayers.get(0);

		final HashSet<GeoPosition> layerBounds = new HashSet<GeoPosition>();
		layerBounds.add(mtLayer.getUpperGeoPosition());
		layerBounds.add(mtLayer.getLowerGeoPosition());

		setZoomFromBounds(layerBounds);

		updateMapPosition();

		_map.queueMapRedraw();
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
		_mpWms.resetAll(false);

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_WmsConfig_DialogTitle);

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

		setTitle(Messages.Dialog_WmsConfig_DialogArea_Title);

		MP.addTileListener(this);

		restoreState();

		// initialize after the shell size is set
		initializeUIFromModel(_mpWms);

		// force the viewer to do the layout to remove horizontal scollbar
		_viewerContainer.layout();

		enableControls();
	}

	private void createActions() {

		final ToolBarManager tbm = new ToolBarManager(_toolbar);

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
		_btnOk = getButton(IDialogConstants.OK_ID);
		_btnOk.setText(Messages.Dialog_MapConfig_Button_Save);
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

		_pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUIContainer(container);
			createUILog(container);
		}
	}

	private void createUIContainer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			// left part (layer selection)
			_leftContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 5, 0, 0).applyTo(_leftContainer);
			createUILayer(_leftContainer);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);
			net.tourbook.util.UI.addSashColorHandler(sash);

			// right part (map)
			final Composite mapContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(mapContainer);
			createUIMap(mapContainer);

			_detailForm = new ViewerDetailForm(container, _leftContainer, sash, mapContainer, 30);
		}
	}

	private void createUILayer(final Composite parent) {

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
				createUILayer20Viewer(viewerContainer);

				// label: hint
				label = new Label(viewerContainer, SWT.NONE);
				label.setText(Messages.Dialog_WmsConfig_Label_DndHint);
			}

			// ############################################################

			createUILayer30Details(container);
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

	private void createUILayer20Viewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();
		_viewerContainer = new Composite(parent, SWT.NONE);
		_viewerContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);

		/*
		 * create table
		 */
		final Table table = new Table(_viewerContainer, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_layerViewer = new CheckboxTableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: layer title
		tvc = new TableViewerColumn(_layerViewer, SWT.LEAD);
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
		tvc = new TableViewerColumn(_layerViewer, SWT.LEAD);
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
		tvc = new TableViewerColumn(_layerViewer, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_WmsConfig_Column_Bbox);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final MtLayer mtLayer = (MtLayer) cell.getElement();

				final GeoPosition lowerPosition = mtLayer.getLowerGeoPosition();
				final GeoPosition upperPosition = mtLayer.getUpperGeoPosition();

				final StringBuilder sb = new StringBuilder();

				sb.append(lowerPosition.latitude);
				sb.append(", "); //$NON-NLS-1$
				sb.append(lowerPosition.longitude);
				sb.append(" / "); //$NON-NLS-1$
				sb.append(upperPosition.latitude);
				sb.append(", "); //$NON-NLS-1$
				sb.append(upperPosition.longitude);

				cell.setText(sb.toString());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(20, true));

		_layerViewer.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				final int mtLayerSize = _allMtLayers.size();
				if (mtLayerSize == 0) {
					return null;
				} else {
					return _allMtLayers.toArray(new MtLayer[mtLayerSize]);
				}
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_layerViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// select the checked item
				_layerViewer.setSelection(new StructuredSelection(event.getElement()));

				// set focus to selected layer
				table.setSelection(table.getSelectionIndex());

				onCheckLayer(event.getElement());
			}
		});

		_layerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectLayer();
			}
		});

		/*
		 * set drag adapter
		 */
		_layerViewer.addDragSupport(
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
						final ISelection selection = _layerViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_layerViewer) {

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
						final Table layerTable = _layerViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStartViewerLeft) {
							_layerViewer.remove(mtLayer);
						}

						int tableIndex;

						if (fTableItem == null) {

							_layerViewer.add(mtLayer);
							tableIndex = layerTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							tableIndex = layerTable.indexOf((TableItem) fTableItem);
							if (tableIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_layerViewer.insert(mtLayer, tableIndex);
							} else if (location == LOCATION_AFTER) {
								_layerViewer.insert(mtLayer, ++tableIndex);
							}
						}

						// set check state
						_layerViewer.setChecked(mtLayer, mtLayer.isDisplayedInMap());

						// reselect filter item
						_layerViewer.setSelection(new StructuredSelection(mtLayer));

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
				if (((location == LOCATION_AFTER) || (location == LOCATION_BEFORE)) == false) {
					return false;
				}

				return true;
			}

		};

		_layerViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUILayer30Details(final Composite parent) {

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
				_comboImageSize = new Combo(leftContainer, SWT.BORDER | SWT.READ_ONLY);
				_comboImageSize.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectImageSize();
					}
				});
				// set content
				for (final String imageSize : MapProviderManager.IMAGE_SIZE) {
					_comboImageSize.add(imageSize);
				}

				// ############################################################

				// label: image format
				label = new Label(leftContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(Messages.Dialog_WmsConfig_Label_ImageFormat);

				// combo: image format
				_cboImageFormat = new Combo(leftContainer, SWT.BORDER | SWT.READ_ONLY);
				GridDataFactory
						.fillDefaults()
						.hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(_cboImageFormat);
				_cboImageFormat.addSelectionListener(new SelectionAdapter() {
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
				_chkLoadTransparentImages = new Button(rightContainer, SWT.CHECK);
				GridDataFactory.fillDefaults().applyTo(_chkLoadTransparentImages);
				_chkLoadTransparentImages.setText(Messages.Dialog_WmsConfig_Button_GetTransparentMap);
				_chkLoadTransparentImages.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectTransparentImage();
					}
				});

				// ############################################################

				// check: show tile info
				_chkShowTileInfo = new Button(rightContainer, SWT.CHECK);
				GridDataFactory.fillDefaults().applyTo(_chkShowTileInfo);
				_chkShowTileInfo.setText(Messages.Dialog_MapConfig_Button_ShowTileInfo);
				_chkShowTileInfo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						final boolean isTileInfo = _chkShowTileInfo.getSelection();
						_map.setShowDebugInfo(isTileInfo, isTileInfo);
					}
				});

				// ############################################################

				// check: show tile image loading log
				_chkShowTileImageLog = new Button(rightContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
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
	}

	private void createUILog(final Composite parent) {

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
						.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(5))
						.applyTo(_txtLogDetail);
				_formTk.adapt(_txtLogDetail, false, false);
				_txtLogDetail.setFont(monoFont);
				_txtLogDetail.setBackground(parentBackground);
			}
		}
	}

	private void createUIMap(final Composite parent) {

		final Composite toolbarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbarContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(toolbarContainer);
		{
			// button: update map
			_btnShowMap = new Button(toolbarContainer, SWT.NONE);
			_btnShowMap.setText(Messages.Dialog_WmsConfig_Button_UpdateMap);
			_btnShowMap.setToolTipText(Messages.Dialog_WmsConfig_Button_UpdateMap_Tooltip);
			_btnShowMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectWmsMap();
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
					onSelectOsmMap();
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

		_map.addMousePositionListener(new IPositionListener() {

			public void setPosition(final MapPositionEvent event) {

				final GeoPosition mousePosition = event.mapGeoPosition;

				double lon = mousePosition.longitude % 360;
				lon = lon > 180 ? //
						lon - 360
						: lon < -180 ? //
								lon + 360
								: lon;

				_lblMapInfo.setText(NLS.bind(
						Messages.Dialog_MapConfig_Label_MapInfo,
						new Object[] {
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
			GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(25), SWT.DEFAULT).applyTo(_lblTileInfo);

			_lblTileInfo.setToolTipText(Messages.Dialog_MapConfig_TileInfo_Tooltip_Line1
					+ Messages.Dialog_MapConfig_TileInfo_Tooltip_Line2
					+ Messages.Dialog_MapConfig_TileInfo_Tooltip_Line3);

		}

		/*
		 * !!! don't do any map initialization until the tile factory is set !!!
		 */
	}

	private void enableControls() {

		_btnShowOsmMap.setEnabled(_mpWms.getTileSize() == MapProviderManager.OSM_IMAGE_SIZE);

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

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position

		try {

			// Get the stored width
			_dialogSettings.getInt(UI.DIALOG_WIDTH);

		} catch (final NumberFormatException e) {

			// dialog width is not yet set, set default size

			_dialogSettings.put(UI.DIALOG_WIDTH, 1000);
			_dialogSettings.put(UI.DIALOG_HEIGHT, 800);
		}

		return _dialogSettings;

		// disable bounds
		// return null;
	}

	private Rectangle getPositionBounds(final Set<GeoPosition> positions, final int zoom) {

		final MP mp = _map.getMapProvider();

		final GeoPosition pos1 = positions.iterator().next();
		final Point point1 = mp.geoToPixel(pos1, zoom);

		final Rectangle rect = new Rectangle(point1.x, point1.y, 0, 0);

		for (final GeoPosition pos : positions) {

			final Point point = mp.geoToPixel(pos, zoom);

			rect.add(new Rectangle(point.x, point.y, 0, 0));
		}
		return rect;
	}

	private void initializeUIFromModel(final MPWms mapProvider) {

		_mpWms = mapProvider;

		/*
		 * get layers
		 */
		_allMtLayers.clear();
		final ArrayList<MtLayer> allMtLayers = _mpWms.getMtLayers();
		_allMtLayers.addAll(allMtLayers);

		// check layers
		if (_allMtLayers.size() == 0) {
			StatusUtil.showStatus(
					NLS.bind(Messages.DBG034_Wms_Error_LayersAreNotAvailable, _mpWms.getName()),
					new Exception());
			return;
		}

		// sort layers by position or name
		Collections.sort(_allMtLayers);

		/*
		 * set fields
		 */
		_chkLoadTransparentImages.setSelection(_mpWms.isTransparent());

		/*
		 * url: set initially the caps url, this will be overwritten from the tile url when a tile
		 * is loaded
		 */
		_tileUrl = _mpWms.getCapabilitiesUrl();
		_cboTileImageLog.add(_tileUrl);

		/*
		 * image size
		 */
		final String imageSize = Integer.toString(_mpWms.getTileSize());
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

			// update map provider value
			_mpWms.setTileSize(Integer.parseInt(MapProviderManager.IMAGE_SIZE[0]));
		}
		_comboImageSize.select(sizeIndex);

		/*
		 * set image format
		 */
		final String currentImageFormat = _mpWms.getImageFormat();
		int formatIndex = 0;
		int selectedFormatIndex = -1;
		_cboImageFormat.removeAll();
		for (final String imageFormat : _mpWms.getImageFormats()) {

			_cboImageFormat.add(imageFormat);

			if (currentImageFormat.equalsIgnoreCase(imageFormat)) {
				selectedFormatIndex = formatIndex;
			}

			formatIndex++;
		}
		// select current format
		if (selectedFormatIndex == -1) {
			_cboImageFormat.select(0);
		} else {
			_cboImageFormat.select(selectedFormatIndex);
		}

		// show map provider in the message area
		_defaultMessage = NLS.bind(Messages.Dialog_MapConfig_DialogArea_Message, _mpWms.getName());
		setMessage(_defaultMessage);

		// show layers into the viewer
		_layerViewer.setInput(this);

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
			_layerViewer.setCheckedElements(checkedLayers.toArray());
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
		final TableItem[] tableItems = _layerViewer.getTable().getItems();
		int checkedLayers = 0;

		for (final TableItem tableItem : tableItems) {
			if (tableItem.getChecked()) {
				checkedLayers++;
			}
		}

		if (checkedLayers == 0) {

			// recheck current layer when no layer is checked

			_layerViewer.setChecked(checkedLayer, true);
		}

		updateMap(false);
	}

	private void onDispose() {

		if (_formTk != null) {
			_formTk.dispose();
		}

		MP.removeTileListener(this);
	}

	private void onSelectImageFormat() {

		final String oldValue = _mpWms.getImageFormat();
		final String newValue = _cboImageFormat.getItem(_cboImageFormat.getSelectionIndex());

		if (oldValue.equals(newValue)) {
			return;
		}

		// keep current position and zoom-level
		final GeoPosition center = _map.getGeoCenter();
		final int zoom = _map.getZoom();

		// set image format
		_mpWms.setImageFormat(newValue);

		resetMap(center, zoom);
	}

	private void onSelectImageSize() {

		final int oldValue = _mpWms.getTileSize();
		final int newValue = Integer.parseInt(MapProviderManager.IMAGE_SIZE[_comboImageSize.getSelectionIndex()]);

		if (oldValue == newValue) {
			return;
		}

		// keep current position and zoom-level
		final GeoPosition center = _map.getGeoCenter();
		final int zoom = _map.getZoom();

		// set image size and initialize tile factory
		_mpWms.setTileSize(newValue);

		enableControls();

		resetMap(center, zoom);
	}

	private void onSelectLayer() {

		final int oldValue = _mpWms.getTileSize();
		final int newValue = Integer.parseInt(MapProviderManager.IMAGE_SIZE[_comboImageSize.getSelectionIndex()]);

		if (oldValue == newValue) {
			return;
		}

		// set image size and initialize tile factory
		_mpWms.setTileSize(newValue);
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

		if (_map.getMapProvider() == _defaultMapProvider) {

			// toggle map, display wms

			onSelectWmsMap();

		} else {

			// display OSM

			_defaultMapProvider.setStateToReloadOfflineCounter();

			// ensure the map is using the correct zoom levels
			updateMapZoomLevels(_defaultMapProvider);

			_map.setMapProviderWithReset(_defaultMapProvider);
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

		_mpWms.setTransparent(_chkLoadTransparentImages.getSelection());

		// reset all images
		_mpWms.resetAll(true);

		// delete offline images because they are invalid for the new image size
		_prefPageMapFactory.deleteOfflineMap(_mpWms);

		// display map with new image size
		_map.queueMapRedraw();
	}

	private void onSelectWmsMap() {

		// check if the tile factory has changed
		if (_map.getMapProvider() != _mpWms) {

			/*
			 * select wms map provider
			 */
		}

		updateModelFromUI();

		// reset all images
		_mpWms.resetAll(false);

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * <br>
		 * ensure the map is using the correct zoom levels before other map actions are done<br>
		 * <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 */
		updateMapZoomLevels(_mpWms);

		_map.setMapProviderWithReset(_mpWms);
	}

	private void resetMap(final GeoPosition center, final int zoom) {

		// reset all images
		_mpWms.resetAll(false);

		// delete offline images because they are invalid for the new image size
		_prefPageMapFactory.deleteOfflineMap(_mpWms);

		_map.setZoom(zoom);
		_map.setMapCenter(center);

		// display map
		_map.queueMapRedraw();
	}

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			_detailForm.setViewerWidth(_dialogSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}

		// debug tile info
		final boolean isShowDebugInfo = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_INFO);
		_chkShowTileInfo.setSelection(isShowDebugInfo);
		_map.setShowDebugInfo(isShowDebugInfo, isShowDebugInfo);

		// tile image logging
		final boolean isShowImageLogging = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG);
		_chkShowTileImageLog.setSelection(isShowImageLogging);
	}

	private void saveState() {

		_dialogSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, _leftContainer.getSize().x);
		_dialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_INFO, _chkShowTileInfo.getSelection());
		_dialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG, _chkShowTileImageLog.getSelection());

		updateModelFromUI();
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

		if ((positions == null) || (positions.size() < 2)) {
			return;
		}

		final MP mp = _map.getMapProvider();

		final int maximumZoomLevel = mp.getMaximumZoomLevel();

		int zoom = mp.getMinimumZoomLevel();

		Rectangle positionRect = getPositionBounds(positions, zoom);

//		if (positionRect.getY() < 0.0) {
//			positionRect.setRect(positionRect.getX(), 0.0d, positionRect.getWidth(), positionRect.getHeight());
//		}

		org.eclipse.swt.graphics.Rectangle viewport = _map.getWorldPixelViewport();

//		System.out.println();
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		for (final GeoPosition geoPosition : positions) {
//			System.out.println(geoPosition.longitude + ", " + geoPosition.latitude); //$NON-NLS-1$
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
		while ((positionRect.width < viewport.width) && (positionRect.height < viewport.height)) {

//			System.out.println();
//			dump("vp2:", viewport, positionRect);
//			System.out.println();
//			// TODO remove SYSTEM.OUT.PRINTLN

			// center position in the map
			final double centerX = positionRect.x + positionRect.width / 2;
			final double centerY = positionRect.y + positionRect.height / 2;
			final Point center = new Point((int) centerX, (int) centerY);

			final GeoPosition devCenter = mp.pixelToGeo(center, zoom);
			_map.setMapCenter(devCenter);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}

			_map.setZoom(zoom);

			positionRect = getPositionBounds(positions, zoom);
			viewport = _map.getWorldPixelViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		_map.setZoom(zoom);
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

		// when stat is cleared, que can get negative, prevent this
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
				_lblTileInfo.setText(NLS.bind(
						Messages.Dialog_MapConfig_TileInfo_Statistics,
						new Object[] {
								Integer.toString(_statIsQueued % 1000),
								Integer.toString(_statEndLoading % 1000),
								Integer.toString(_statStartLoading % 1000),
								Integer.toString(_statErrorLoading % 1000), //
						}));

				final String logEntry = displayLogEntries(_logEntries, _cboTileImageLog);

				// select new entry
				_cboTileImageLog.select(_cboTileImageLog.getItemCount() - 1);

				// display last log in the detail field
				_txtLogDetail.setText(logEntry);
			}
		};

		_display.asyncExec(infoRunnable);
	}

	/**
	 * update position and check state in all layers
	 * 
	 * @return Returns the number of layers which are displayed in the map
	 */
	private int updateLayerState() {

		_displayedLayers.clear();

		int itemIndex = 0;
		int visibleLayers = 0;
		for (final TableItem tableItem : _layerViewer.getTable().getItems()) {

			final MtLayer mtLayer = (MtLayer) tableItem.getData();
			final boolean isChecked = tableItem.getChecked();

			mtLayer.setIsDisplayedInMap(isChecked);
			mtLayer.setPositionIndex(itemIndex++);

			if (isChecked) {
				_displayedLayers.add(mtLayer);
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
		_mpWms.initializeLayers();

		// set factory and display map
		_map.setMapProviderWithReset(_mpWms);

		if (isUpdatePosition) {
			// set position to previous position
			_map.setZoom(_mpWms.getLastUsedZoom());
			_map.setMapCenter(_mpWms.getLastUsedPosition());
		}
	}

	/*
	 * keep zoom+position
	 */
	private void updateMapPosition() {
		_mpWms.setLastUsedZoom(_map.getZoom());
		_mpWms.setLastUsedPosition(_map.getGeoCenter());
	}

	/**
	 * ensure the map is using the correct zoom levels
	 */
	private void updateMapZoomLevels(final MP mp) {

		final int factoryMinZoom = mp.getMinimumZoomLevel();
		final int factoryMaxZoom = mp.getMaximumZoomLevel();

		final int mapZoom = _map.getZoom();
		final GeoPosition mapCenter = _map.getGeoCenter();

		if (mapZoom < factoryMinZoom) {
			_map.setZoom(factoryMinZoom);
			_map.setMapCenter(mapCenter);
		}

		if (mapZoom > factoryMaxZoom) {
			_map.setZoom(factoryMaxZoom);
			_map.setMapCenter(mapCenter);
		}
	}

	private void updateModelFromUI() {

		updateLayerState();
		updateMapPosition();
	}

}
