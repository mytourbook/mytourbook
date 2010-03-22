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
package de.byteholder.geoclipse.map;
 
import java.util.ArrayList;
import java.util.List;

import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MPProfile;
import de.byteholder.geoclipse.mapprovider.MPWrapper;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.tileinfo.TileInfoContribution;

public class DialogManageOfflineImages extends TitleAreaDialog implements ITileListener {

	private IPreferenceStore	_prefStore			= Activator.getDefault().getPreferenceStore();
	private IDialogSettings		_state				= Activator.getDefault().getDialogSettingsSection(
															"DialogManageOfflineImages");			//$NON-NLS-1$

	private OfflineLoadManager	_offlineManager		= OfflineLoadManager.getInstance();

	private MP					_selectedMp;
	private int					_mapZoomLevel;

	private Point				_offlineWorldStart;
	private Point				_offlineWorldEnd;

	private Combo				_comboMapProvider;
	private Combo				_comboTargetZoom;
	private Text				_txtQueue;
	private Button				_btnStartLoading;
	private Button				_btnDeleteAll;
	private Button				_btnDeletePart;
	private Button				_btnStop;

	private TileInfo			_tileInfo;

	private int[]				_targetZoomLevels;
	private ArrayList<MP>		_allMapProviders;

	private TableViewer			_partViewer;
	private ArrayList<PartMP>	_partMapProvider	= new ArrayList<PartMP>();
	private int					_validMapZoomLevel;

	class PartMP {

		MP	partMp;
		int	missingImages;
		int	existingImages;

		public PartMP(final MP partMp) {
			this.partMp = partMp;
		}
	}

	class PartViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _partMapProvider.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	class TileInfo extends TileInfoContribution {
		Control createUI(final Composite parent) {
			return createControl(parent);
		}
	}

	public DialogManageOfflineImages(	final Shell parentShell,
										final MP mp,
										final Point offlineWorldStart,
										final Point offlineWorldEnd,
										final int mapZoomLevel) {

		super(parentShell);

		_selectedMp = mp;
		_mapZoomLevel = mapZoomLevel;

		_offlineWorldStart = offlineWorldStart;
		_offlineWorldEnd = offlineWorldEnd;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public boolean close() {

		_offlineManager.stopLoading();

		MP.removeTileListener(this);

		super.close();

		return true;
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_OfflineArea_Title);
	}

	@Override
	public void create() {

		getMapProviders();

		super.create();

		setTitle(Messages.Dialog_OfflineArea_Title);
		setMessage(Messages.Dialog_OfflineArea_Message);

		MP.addTileListener(this);

		// disable all controls
		enableControls(false);

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				initOfflineManager();
				_comboTargetZoom.setFocus();
			}
		});
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {

		// create close button
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite container = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(container);

		initUI();

		return container;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			final Composite innerContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(innerContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(innerContainer);
			{
				createUI10LeftPart(innerContainer);
				createUI30Actions(innerContainer);
			}

			/*
			 * tile info
			 */
			_tileInfo = new TileInfo();
			final Control tileInfoControl = _tileInfo.createUI(container);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(tileInfoControl);
		}

		// set download as default button
		parent.getShell().setDefaultButton(_btnStartLoading);
	}

	private void createUI10LeftPart(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * map provider
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_MapProvider);

			// combo: zoom level
			_comboMapProvider = new Combo(container, SWT.READ_ONLY);
			_comboMapProvider.setVisibleItemCount(30);
			_comboMapProvider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectMapProvider();
				}
			});

			/*
			 * profile parts
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_ProfileParts);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(label);

			createUI20PartViewer(container);

			/*
			 * max zoom level
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_ZoomLevel);

			// combo: zoom level
			_comboTargetZoom = new Combo(container, SWT.READ_ONLY);
			_comboTargetZoom.setVisibleItemCount(20);
			_comboTargetZoom.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					getOfflineImageState();

					// focus was disabled, reset focus
					_comboTargetZoom.setFocus();
				}
			});

			/*
			 * queue
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_Queue);

			_txtQueue = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtQueue);
		}
	}

	private void createUI20PartViewer(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);

		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(10))
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_partViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: map provider
		tvc = new TableViewerColumn(_partViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_OfflineArea_Column_MapProvider);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final PartMP partMp = (PartMP) cell.getElement();
				cell.setText(partMp.partMp.getName());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(100));

		// column: existing images
		tvc = new TableViewerColumn(_partViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_OfflineArea_Column_ExistingImages);
		tvcColumn.setToolTipText(Messages.Dialog_OfflineArea_Column_ExistingImages_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final PartMP partMp = (PartMP) cell.getElement();
				cell.setText(Integer.toString(partMp.existingImages));
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(12), true));

		// column: missing images
		tvc = new TableViewerColumn(_partViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_OfflineArea_Column_MissingImages);
		tvcColumn.setToolTipText(Messages.Dialog_OfflineArea_Column_MissingImages_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final PartMP partMp = (PartMP) cell.getElement();
				cell.setText(Integer.toString(partMp.missingImages));
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(12), true));

		/*
		 * create table viewer
		 */

		_partViewer.setContentProvider(new PartViewerContentProvicer());

		_partViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					enableControls(true);
				}
			}
		});
	}

	private void createUI30Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 5).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: start loading
			 */
			_btnStartLoading = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnStartLoading);
			_btnStartLoading.setText(Messages.Dialog_OfflineArea_Button_StartDownloading);
			_btnStartLoading.setToolTipText(Messages.Dialog_OfflineArea_Button_StartDownloading_Tooltip);
			_btnStartLoading.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					onSelectDownload();
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectDownload();
				}
			});

			/*
			 * button: delete all images
			 */
			_btnDeleteAll = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnDeleteAll);
			_btnDeleteAll.setText(Messages.Dialog_OfflineArea_Button_DeleteAll);
			_btnDeleteAll.setToolTipText(Messages.Dialog_OfflineArea_Button_DeleteAll_Tooltip);
			_btnDeleteAll.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectDelete(null);
				}
			});

			/*
			 * button: delete part images
			 */
			_btnDeletePart = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnDeletePart);
			_btnDeletePart.setText(Messages.Dialog_OfflineArea_Button_DeletePart);
			_btnDeletePart.setToolTipText(Messages.Dialog_OfflineArea_Button_DeletePart_Tooltip);
			_btnDeletePart.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectDelete(((StructuredSelection) _partViewer.getSelection()).getFirstElement());
				}
			});

			/*
			 * button: stop
			 */
			_btnStop = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnStop);
			_btnStop.setText(Messages.Dialog_OfflineArea_Button_StopDownloading);
			_btnStop.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					_offlineManager.stopLoading();
					enableControls(true);
				}
			});
		}
	}

	private void deleteOfflineImages(final MP selectedPartMp) {

		if (OfflineLoadManager.isLoading()) {
			return;
		}

		final int selectedZoomLevel = _targetZoomLevels[_comboTargetZoom.getSelectionIndex()];

		final int tileSize = _selectedMp.getTileSize();

		final int worldStartX = _offlineWorldStart.x;
		final int worldStartY = _offlineWorldStart.y;
		final int worldEndX = _offlineWorldEnd.x;
		final int worldEndY = _offlineWorldEnd.y;

		for (final PartMP partMp : _partMapProvider) {

			final MP offlineMp = partMp.partMp;

			/*
			 * check if only one part should be deleted, all will be deleted when the selectedPartMP
			 * is null
			 */
			if (selectedPartMp != null && offlineMp != selectedPartMp) {
				continue;
			}

			double worldX1 = Math.min(worldStartX, worldEndX);
			double worldX2 = Math.max(worldStartX, worldEndX);
			double worldY1 = Math.min(worldStartY, worldEndY);
			double worldY2 = Math.max(worldStartY, worldEndY);

			for (int zoomLevel = _validMapZoomLevel; zoomLevel <= selectedZoomLevel; zoomLevel++) {

				final int maxMapTileSize = _selectedMp.getMapTileSize(zoomLevel).width;

				final int areaPixelWidth = (int) (worldX2 - worldX1);
				final int areaPixelHeight = (int) (worldY2 - worldY1);

				final int numTileWidth = (int) Math.ceil((double) areaPixelWidth / (double) tileSize);
				final int numTileHeight = (int) Math.ceil((double) areaPixelHeight / (double) tileSize);

				int tilePosMinX = (int) Math.floor(worldX1 / tileSize);
				int tilePosMinY = (int) Math.floor(worldY1 / tileSize);
				int tilePosMaxX = tilePosMinX + numTileWidth;
				int tilePosMaxY = tilePosMinY + numTileHeight;

				// ensure tiles are within the map
				tilePosMinX = Math.max(0, tilePosMinX);
				tilePosMinY = Math.max(0, tilePosMinY);
				tilePosMaxX = Math.min(tilePosMaxX, maxMapTileSize);
				tilePosMaxY = Math.min(tilePosMaxY, maxMapTileSize);

				for (int tilePosX = tilePosMinX; tilePosX <= tilePosMaxX; tilePosX++) {
					for (int tilePosY = tilePosMinY; tilePosY <= tilePosMaxY; tilePosY++) {

						// create offline tile
						final Tile offlineTile = new Tile(offlineMp, zoomLevel, tilePosX, tilePosY, null);
						offlineTile.setBoundingBoxEPSG4326();
						offlineMp.doPostCreation(offlineTile);

						_offlineManager.deleteOfflineImage(offlineMp, offlineTile);
					}
				}

				// set next zoom level, zoom into the map
				worldX1 *= 2;
				worldX2 *= 2;
				worldY1 *= 2;
				worldY2 *= 2;
			}
		}

		getOfflineImageState();
	}

	private void enableControls(final boolean canBeEnabled) {

		final boolean isLoading = OfflineLoadManager.isLoading();
		final boolean isPartSelected = _partViewer.getSelection().isEmpty() == false;

		_comboTargetZoom.setEnabled(canBeEnabled && isLoading == false);

		_btnStartLoading.setEnabled(canBeEnabled && isLoading == false);
		_btnStop.setEnabled(canBeEnabled && isLoading);

		_btnDeleteAll.setEnabled(canBeEnabled && isLoading == false);
		_btnDeletePart.setEnabled(canBeEnabled && isLoading == false && isPartSelected);

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return _state;
		return null;
	}

	/**
	 * @return update the map providers with information from the pref store, the customized order
	 *         and the toggle status
	 */
	private void getMapProviders() {

		final List<MP> allMapProviders = MapProviderManager.getInstance().getAllMapProviders(true);

		// get sorted map providers from the pref store
		final String[] storedProviderIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

		final ArrayList<MP> mapProviders = new ArrayList<MP>();
		final ArrayList<String> validMpIds = new ArrayList<String>();

		// set all map provider which are in the pref store
		for (final String storeMpId : storedProviderIds) {

			/*
			 * ensure that a map provider is unique and not duplicated, this happend during
			 * debugging
			 */
			boolean ignoreMP = false;
			for (final MP mp : mapProviders) {
				if (mp.getId().equalsIgnoreCase(storeMpId)) {
					ignoreMP = true;
					break;
				}
			}
			if (ignoreMP) {
				continue;
			}

			// find the stored map provider in the available map providers
			for (final MP mp : allMapProviders) {
				if (mp.getId().equalsIgnoreCase(storeMpId)) {
					mapProviders.add(mp);
					validMpIds.add(mp.getId());
					break;
				}
			}
		}

		// make sure that all available map providers are in the list
		for (final MP mp : allMapProviders) {
			if (!mapProviders.contains(mp)) {
				mapProviders.add(mp);
			}
		}

		/*
		 * save valid mp id's
		 */
		_prefStore.setValue(IMappingPreferences.MAP_PROVIDER_SORT_ORDER, //
				StringToArrayConverter.convertArrayToString(//
						validMpIds.toArray(new String[validMpIds.size()])));

		/*
		 * set status if the map provider can be toggled with the map provider button
		 */
		final String[] toggleIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(IMappingPreferences.MAP_PROVIDER_TOGGLE_LIST));

		for (final MP mp : allMapProviders) {

			final String mpId = mp.getId();

			for (final String toggleId : toggleIds) {
				if (mpId.equals(toggleId)) {
					mp.setCanBeToggled(true);
					break;
				}
			}
		}

		_allMapProviders = mapProviders;
	}

	private void getOfflineImageState() {

		if (OfflineLoadManager.isLoading()) {
			return;
		}

		enableControls(false);

		getOfflineMapProviders();

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				final int selectedZoomLevel = _targetZoomLevels[_comboTargetZoom.getSelectionIndex()];

				final int tileSize = _selectedMp.getTileSize();

				final int worldStartX = _offlineWorldStart.x;
				final int worldStartY = _offlineWorldStart.y;
				final int worldEndX = _offlineWorldEnd.x;
				final int worldEndY = _offlineWorldEnd.y;

				for (final PartMP partMp : _partMapProvider) {

					final MP offlineMp = partMp.partMp;

					double worldX1 = Math.min(worldStartX, worldEndX);
					double worldX2 = Math.max(worldStartX, worldEndX);
					double worldY1 = Math.min(worldStartY, worldEndY);
					double worldY2 = Math.max(worldStartY, worldEndY);

					for (int zoomLevel = _validMapZoomLevel; zoomLevel <= selectedZoomLevel; zoomLevel++) {

						final int maxMapTileSize = offlineMp.getMapTileSize(zoomLevel).width;

						final int areaPixelWidth = (int) (worldX2 - worldX1);
						final int areaPixelHeight = (int) (worldY2 - worldY1);

						final int numTileWidth = (int) Math.ceil((double) areaPixelWidth / (double) tileSize);
						final int numTileHeight = (int) Math.ceil((double) areaPixelHeight / (double) tileSize);

						int tilePosMinX = (int) Math.floor(worldX1 / tileSize);
						int tilePosMinY = (int) Math.floor(worldY1 / tileSize);
						int tilePosMaxX = tilePosMinX + numTileWidth;
						int tilePosMaxY = tilePosMinY + numTileHeight;

						// ensure tiles are within the map
						tilePosMinX = Math.max(0, tilePosMinX);
						tilePosMinY = Math.max(0, tilePosMinY);
						tilePosMaxX = Math.min(tilePosMaxX, maxMapTileSize);
						tilePosMaxY = Math.min(tilePosMaxY, maxMapTileSize);

						for (int tilePosX = tilePosMinX; tilePosX <= tilePosMaxX; tilePosX++) {
							for (int tilePosY = tilePosMinY; tilePosY <= tilePosMaxY; tilePosY++) {

								// create offline tile
								final Tile offlineTile = new Tile(offlineMp, zoomLevel, tilePosX, tilePosY, null);
								offlineTile.setBoundingBoxEPSG4326();
								offlineMp.doPostCreation(offlineTile);

								final boolean isAvailable = _offlineManager.isOfflineImageAvailable(
										offlineMp,
										offlineTile);

								if (isAvailable) {
									partMp.existingImages++;
								} else {
									partMp.missingImages++;
								}
							}
						}

						// zoom into the map
						worldX1 *= 2;
						worldX2 *= 2;
						worldY1 *= 2;
						worldY2 *= 2;
					}
				}
			}
		});

		// update part viewer
		_partViewer.setInput(new Object());

		// deselect all
		_partViewer.setSelection(null);

		enableControls(true);
	}

	/**
	 * creates the list {@link #_partMapProvider} which contains all map providers which should be
	 * loaded/deleted/checked
	 */
	private void getOfflineMapProviders() {

		// get map providers including
		_partMapProvider.clear();
		_partMapProvider.add(new PartMP(_selectedMp));

		// add all visible mp's which a profile mp contains
		if (_selectedMp instanceof MPProfile) {
			final MPProfile mpProfile = (MPProfile) _selectedMp;
			for (final MPWrapper mpWrapper : mpProfile.getAllWrappers()) {
				if (mpWrapper.isDisplayedInMap()) {
					_partMapProvider.add(new PartMP(mpWrapper.getMP()));
				}
			}
		}
	}

	private void initOfflineManager() {

		if (_offlineManager.initialize(_selectedMp)) {

			getOfflineImageState();

			enableControls(true);

		} else {

			// disable controls

			enableControls(false);
		}
	}

	private void initUI() {

		updateZoomLevels();

		// select first zoom level which is the minimum zoom
		_comboTargetZoom.select(0);

		// fill mp combo
		int mpCounter = 0;
		int mpIndex = 0;
		for (final MP mp : _allMapProviders) {
			_comboMapProvider.add(mp.getName());

			if (mp.equals(_selectedMp)) {
				mpIndex = mpCounter;
			}

			mpCounter++;
		}
		// select map provider which is displayed in the map
		_comboMapProvider.select(mpIndex);

		enableControls(true);
	}

	private void loadOfflineImages() {

		if (OfflineLoadManager.isLoading()) {
			return;
		}

		final int selectedZoomLevel = _targetZoomLevels[_comboTargetZoom.getSelectionIndex()];

		final int tileSize = _selectedMp.getTileSize();

		final int worldStartX = _offlineWorldStart.x;
		final int worldStartY = _offlineWorldStart.y;
		final int worldEndX = _offlineWorldEnd.x;
		final int worldEndY = _offlineWorldEnd.y;

		double worldX1 = Math.min(worldStartX, worldEndX);
		double worldX2 = Math.max(worldStartX, worldEndX);
		double worldY1 = Math.min(worldStartY, worldEndY);
		double worldY2 = Math.max(worldStartY, worldEndY);

		for (int zoomLevel = _validMapZoomLevel; zoomLevel <= selectedZoomLevel; zoomLevel++) {

			final int maxMapTileSize = _selectedMp.getMapTileSize(zoomLevel).width;

			final int areaPixelWidth = (int) (worldX2 - worldX1);
			final int areaPixelHeight = (int) (worldY2 - worldY1);

			final int numTileWidth = (int) Math.ceil((double) areaPixelWidth / (double) tileSize);
			final int numTileHeight = (int) Math.ceil((double) areaPixelHeight / (double) tileSize);

			int tilePosMinX = (int) Math.floor(worldX1 / tileSize);
			int tilePosMinY = (int) Math.floor(worldY1 / tileSize);
			int tilePosMaxX = tilePosMinX + numTileWidth;
			int tilePosMaxY = tilePosMinY + numTileHeight;

			// ensure tiles are within the map
			tilePosMinX = Math.max(0, tilePosMinX);
			tilePosMinY = Math.max(0, tilePosMinY);
			tilePosMaxX = Math.min(tilePosMaxX, maxMapTileSize);
			tilePosMaxY = Math.min(tilePosMaxY, maxMapTileSize);

			for (int tilePosX = tilePosMinX; tilePosX <= tilePosMaxX; tilePosX++) {
				for (int tilePosY = tilePosMinY; tilePosY <= tilePosMaxY; tilePosY++) {

					// create offline tile
					final Tile offlineTile = new Tile(_selectedMp, zoomLevel, tilePosX, tilePosY, null);
					offlineTile.setBoundingBoxEPSG4326();
					_selectedMp.doPostCreation(offlineTile);

//					final boolean isLoading = _offlineManager.addOfflineTile(offlineMp, offlineTile);
//
//					if (isLoading) {
//						_missingImages++;
//						_txtMissingImages.setText(Integer.toString(_missingImages));
//					} else {
//						_availImages++;
//						_txtAvailImages.setText(Integer.toString(_availImages));
//					}
				}
			}

			// set next zoom level, zoom into the map
			worldX1 *= 2;
			worldX2 *= 2;
			worldY1 *= 2;
			worldY2 *= 2;
		}
	}

	@Override
	protected void okPressed() {

		super.okPressed();
	}

	private void onSelectDelete(final Object selectedPart) {

		if (selectedPart instanceof PartMP) {

			final MP selectedPartMp = ((PartMP) selectedPart).partMp;

			if (MessageDialog.openConfirm(
					Display.getCurrent().getActiveShell(),
					Messages.Dialog_OfflineArea_ConfirmDelete_Title,
					NLS.bind(Messages.Dialog_OfflineArea_ConfirmDeletePart_Message, selectedPartMp.getName()))) {

				deleteOfflineImages(selectedPartMp);
			}

		} else {

			if (MessageDialog.openConfirm(
					Display.getCurrent().getActiveShell(),
					Messages.Dialog_OfflineArea_ConfirmDelete_Title,
					Messages.Dialog_OfflineArea_ConfirmDeleteAll_Message)) {

				deleteOfflineImages(null);
			}
		}
	}

	private void onSelectDownload() {
		loadOfflineImages();
		enableControls(true);
	}

	private void onSelectMapProvider() {

		_selectedMp = _allMapProviders.get(_comboMapProvider.getSelectionIndex());

		updateZoomLevels();

		initOfflineManager();

		_comboMapProvider.setFocus();
	}

	@Override
	public void tileEvent(final TileEventId tileEventId, final Tile tile) {

		_tileInfo.updateInfo(tileEventId);

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				// check if UI is still available
				if (_comboTargetZoom.isDisposed()) {
					return;
				}

				updateUI();
				enableControls(true);
			}
		});
	}

	private void updateUI() {

		_txtQueue.setText(Integer.toString(MP.getTileWaitingQueue().size()));
	}

	/**
	 * update zoom levels for the selected mp
	 */
	private void updateZoomLevels() {

		// get selected zoom level
		final int selectedIndex = _comboTargetZoom.getSelectionIndex();
		int selectedZoomLevel = -1;
		if (selectedIndex != -1 && _targetZoomLevels != null) {
			selectedZoomLevel = _targetZoomLevels[selectedIndex];
		}

		/*
		 * get valid zoom levels
		 */
		_validMapZoomLevel = _mapZoomLevel;
		final int mapMaxZoom = _selectedMp.getMaxZoomLevel();

		// check if the map zoom level is higher than the available mp zoom levels
		if (_mapZoomLevel > mapMaxZoom) {
			_validMapZoomLevel = mapMaxZoom;
		} else {
			_validMapZoomLevel = _mapZoomLevel;
		}

		_targetZoomLevels = new int[mapMaxZoom - _validMapZoomLevel + 1];

		int zoomIndex = 0;
		for (int zoomLevel = _validMapZoomLevel; zoomLevel <= mapMaxZoom; zoomLevel++) {
			_targetZoomLevels[zoomIndex++] = zoomLevel;
		}

		// fill zoom combo
		_comboTargetZoom.removeAll();
		int reselectedZoomLevelIndex = -1;
		zoomIndex = 0;
		for (final int zoomLevel : _targetZoomLevels) {

			_comboTargetZoom.add(Integer.toString(zoomLevel + 1));

			if (selectedZoomLevel != -1 && zoomLevel == selectedZoomLevel) {
				reselectedZoomLevelIndex = zoomIndex;
			}

			zoomIndex++;
		}

		// reselect zoom level 
		if (reselectedZoomLevelIndex == -1) {
			// old zoom level is not available, select first zoom level
			_comboTargetZoom.select(0);
		} else {
			_comboTargetZoom.select(reselectedZoomLevelIndex);
		}
	}
}
