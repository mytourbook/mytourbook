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
package de.byteholder.geoclipse.map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.widgets.Text;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MPProfile;
import de.byteholder.geoclipse.tileinfo.TileInfoContribution;

public class DialogManageOfflineImages extends TitleAreaDialog implements ITileListener {

	private IDialogSettings		_state;

	private MP					_mp;
	private int					_mapZoomLevel;

	private Point				_offlineWorldStart;
	private Point				_offlineWorldEnd;

	private OfflineLoadManager	_offlineManager	= OfflineLoadManager.getInstance();

	private Combo				_comboMaxZoom;
	private Text				_txtAvailImages;
	private Text				_txtMissingImages;
	private Text				_txtQueue;
	private Button				_btnStartLoading;
	private Button				_btnStartDeleting;
	private Button				_btnStop;

	private TileInfo			_tileInfo;

	private int					_availImages;
	private int					_missingImages;

	private int[]				_availZoomLevels;

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

		_mp = mp;
		_mapZoomLevel = mapZoomLevel;

		_offlineWorldStart = offlineWorldStart;
		_offlineWorldEnd = offlineWorldEnd;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		_state = Activator.getDefault().getDialogSettingsSection("DialogManageOfflineImages"); //$NON-NLS-1$
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

		createData();

		super.create();

		setTitle(Messages.Dialog_OfflineArea_Title);
		setMessage(NLS.bind(Messages.Dialog_OfflineArea_Message, _mp.getName()));

		MP.addTileListener(this);

		restoreState();

		if (_offlineManager.initialize(_mp)) {

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					getOfflineImageState();
					_comboMaxZoom.setFocus();
				}
			});

		} else {

			// disable OK button
			final Button okButton = getButton(IDialogConstants.OK_ID);
			okButton.setEnabled(false);
		}

	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {

		// create close button
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	private void createData() {

		/*
		 * create available zoom levels
		 */
		final int mapMaxZoom = _mp.getMaxZoomLevel();
		_availZoomLevels = new int[mapMaxZoom - _mapZoomLevel + 1];

		int zoomIndex = 0;

		for (int zoomLevel = _mapZoomLevel; zoomLevel <= mapMaxZoom; zoomLevel++) {
			_availZoomLevels[zoomIndex++] = zoomLevel;
		}

	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite container = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(container);

		updateUIInitial();

		return container;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI10LeftPart(container);
			createUI20Actions(container);

			/*
			 * tile info
			 */
			_tileInfo = new TileInfo();
			final Control tileInfoControl = _tileInfo.createUI(container);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(tileInfoControl);
		}
	}

	private void createUI10LeftPart(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * max zoom level
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_ZoomLevel);

			// combo: zoom level
			_comboMaxZoom = new Combo(container, SWT.READ_ONLY);
			_comboMaxZoom.setVisibleItemCount(20);
			_comboMaxZoom.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					getOfflineImageState();

					// focus was disabled, reset focus
					_comboMaxZoom.setFocus();
				}
			});

			// fill combo
			for (final int zoomLevel : _availZoomLevels) {
				_comboMaxZoom.add(Integer.toString(zoomLevel + 1));
			}

			// set download as default button
			parent.getShell().setDefaultButton(_btnStartLoading);

			/*
			 * available images
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_AvailableImages);

			_txtAvailImages = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtAvailImages);

			/*
			 * loading images
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_RequiredImages);

			_txtMissingImages = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtMissingImages);

			/*
			 * loading images
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_OfflineArea_Label_Queue);

			_txtQueue = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtQueue);
		}
	}

	private void createUI20Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 5).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: start loading
			 */
			_btnStartLoading = new Button(container, SWT.NONE);
			_btnStartLoading.setText(Messages.Dialog_OfflineArea_Button_StartDownloading);
			_btnStartLoading.setToolTipText(Messages.Dialog_OfflineArea_Button_StartDownloading_Tooltip);
			_btnStartLoading.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					onSelectStart(true);
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectStart(true);
				}
			});
			setButtonLayoutData(_btnStartLoading);

			/*
			 * button: start deleting
			 */
			_btnStartDeleting = new Button(container, SWT.NONE);
			_btnStartDeleting.setText(Messages.Dialog_OfflineArea_Button_StartDeleting);
			_btnStartDeleting.setToolTipText(Messages.Dialog_OfflineArea_Button_StartDeleting_Tooltip);
			_btnStartDeleting.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectStart(false);
				}
			});
			setButtonLayoutData(_btnStartLoading);

			/*
			 * button: stop
			 */
			_btnStop = new Button(container, SWT.NONE);
			_btnStop.setText(Messages.Dialog_OfflineArea_Button_StopDownloading);
			_btnStop.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					_offlineManager.stopLoading();
					enableControls(true);
				}
			});
			setButtonLayoutData(_btnStop);
		}
	}

	private void deleteOfflineImages() {

		if (OfflineLoadManager.isLoading()) {
			return;
		}

		final int selectedZoomLevel = _availZoomLevels[_comboMaxZoom.getSelectionIndex()];

		final int tileSize = _mp.getTileSize();

		final int worldStartX = _offlineWorldStart.x;
		final int worldStartY = _offlineWorldStart.y;
		final int worldEndX = _offlineWorldEnd.x;
		final int worldEndY = _offlineWorldEnd.y;

		double worldX1 = Math.min(worldStartX, worldEndX);
		double worldX2 = Math.max(worldStartX, worldEndX);
		double worldY1 = Math.min(worldStartY, worldEndY);
		double worldY2 = Math.max(worldStartY, worldEndY);

		for (int zoomLevel = _mapZoomLevel; zoomLevel <= selectedZoomLevel; zoomLevel++) {

			final int maxMapTileSize = _mp.getMapTileSize(zoomLevel).width;

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
					final Tile offlineTile = new Tile(_mp, zoomLevel, tilePosX, tilePosY, null);

					_offlineManager.deleteOfflineImage(offlineTile);
				}
			}

			// set next zoom level, zoom into the map
			worldX1 *= 2;
			worldX2 *= 2;
			worldY1 *= 2;
			worldY2 *= 2;
		}

		getOfflineImageState();
	}

	private void enableControls(final boolean canBeEnabled) {

		final boolean isLoading = OfflineLoadManager.isLoading();
		final boolean isMpProfile = _mp instanceof MPProfile;

		_comboMaxZoom.setEnabled(canBeEnabled && isLoading == false);

		_btnStartLoading.setEnabled(canBeEnabled && isLoading == false);
		_btnStartDeleting.setEnabled(canBeEnabled && isLoading == false && isMpProfile == false);
		_btnStop.setEnabled(canBeEnabled && isLoading);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return _state;
		return null;
	}

	private void getOfflineImageState() {

		if (OfflineLoadManager.isLoading()) {
			return;
		}

		enableControls(false);

		// reset statistics
		_availImages = 0;
		_missingImages = 0;

		_txtMissingImages.setText(Integer.toString(_missingImages));
		_txtAvailImages.setText(Integer.toString(_availImages));

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				final int selectedZoomLevel = _availZoomLevels[_comboMaxZoom.getSelectionIndex()];

				final int tileSize = _mp.getTileSize();

				final int worldStartX = _offlineWorldStart.x;
				final int worldStartY = _offlineWorldStart.y;
				final int worldEndX = _offlineWorldEnd.x;
				final int worldEndY = _offlineWorldEnd.y;

				double worldX1 = Math.min(worldStartX, worldEndX);
				double worldX2 = Math.max(worldStartX, worldEndX);
				double worldY1 = Math.min(worldStartY, worldEndY);
				double worldY2 = Math.max(worldStartY, worldEndY);

				for (int zoomLevel = _mapZoomLevel; zoomLevel <= selectedZoomLevel; zoomLevel++) {

					final int maxMapTileSize = _mp.getMapTileSize(zoomLevel).width;

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
							final Tile offlineTile = new Tile(_mp, zoomLevel, tilePosX, tilePosY, null);

							final boolean isAvailable = _offlineManager.isOfflineImageAvailable(offlineTile);

							if (isAvailable) {
								_availImages++;
								_txtAvailImages.setText(Integer.toString(_availImages));
							} else {
								_missingImages++;
								_txtMissingImages.setText(Integer.toString(_missingImages));
							}
						}
					}

					// set next zoom level, zoom into the map
					worldX1 *= 2;
					worldX2 *= 2;
					worldY1 *= 2;
					worldY2 *= 2;
				}
			}
		});

		enableControls(true);
	}

	private void loadOfflineImages() {

		if (OfflineLoadManager.isLoading()) {
			return;
		}

		// reset statistics
		_availImages = 0;
		_missingImages = 0;

		_txtMissingImages.setText(Integer.toString(_missingImages));
		_txtAvailImages.setText(Integer.toString(_availImages));

		final int selectedZoomLevel = _availZoomLevels[_comboMaxZoom.getSelectionIndex()];

		final int tileSize = _mp.getTileSize();

		final int worldStartX = _offlineWorldStart.x;
		final int worldStartY = _offlineWorldStart.y;
		final int worldEndX = _offlineWorldEnd.x;
		final int worldEndY = _offlineWorldEnd.y;

		double worldX1 = Math.min(worldStartX, worldEndX);
		double worldX2 = Math.max(worldStartX, worldEndX);
		double worldY1 = Math.min(worldStartY, worldEndY);
		double worldY2 = Math.max(worldStartY, worldEndY);

		for (int zoomLevel = _mapZoomLevel; zoomLevel <= selectedZoomLevel; zoomLevel++) {

			final int maxMapTileSize = _mp.getMapTileSize(zoomLevel).width;

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
					final Tile offlineTile = new Tile(_mp, zoomLevel, tilePosX, tilePosY, null);
					offlineTile.setBoundingBoxEPSG4326();
					_mp.doPostCreation(offlineTile);

					final boolean isLoading = _offlineManager.addOfflineTile(offlineTile);

					if (isLoading) {
						_missingImages++;
						_txtMissingImages.setText(Integer.toString(_missingImages));
					} else {
						_availImages++;
						_txtAvailImages.setText(Integer.toString(_availImages));
					}
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

	private void onSelectStart(final boolean isLoading) {

		if (isLoading) {
			loadOfflineImages();
		} else {
			deleteOfflineImages();
		}

		enableControls(true);
	}

	private void restoreState() {

	}

	@Override
	public void tileEvent(final TileEventId tileEventId, final Tile tile) {

		_tileInfo.updateInfo(tileEventId);

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				// check if UI is still available
				if (_comboMaxZoom.isDisposed()) {
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

	private void updateUIInitial() {

		_availImages = 0;
		_missingImages = 0;

		_txtMissingImages.setText(Integer.toString(_missingImages));
		_txtAvailImages.setText(Integer.toString(_availImages));

		// select first zoom level which is the minimum zoom
		_comboMaxZoom.select(0);

		enableControls(true);
 	}
}
