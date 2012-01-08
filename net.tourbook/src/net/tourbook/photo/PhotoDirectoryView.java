/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.util.ColumnDefinition;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.apache.commons.sanselan.ImageReadException;
import org.apache.commons.sanselan.Sanselan;
import org.apache.commons.sanselan.SanselanConstants;
import org.apache.commons.sanselan.common.bytesource.ByteSource;
import org.apache.commons.sanselan.common.bytesource.ByteSourceFile;
import org.apache.commons.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.commons.sanselan.formats.jpeg.JpegImageParser;
import org.apache.commons.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.commons.sanselan.test.util.FileSystemTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PhotoDirectoryView extends ViewPart implements ITourViewer {

	static public final String		ID					= "net.tourbook.photo.photoDirectoryView";				//$NON-NLS-1$

	private static final String		STATE_PHOTO_PATH	= "PhotoPath";											//$NON-NLS-1$

	private final IDialogSettings	_state				= TourbookPlugin.getDefault()//
																.getDialogSettingsSection("PhotoDirectory");	//$NON-NLS-1$
	private final IPreferenceStore	_prefStore			= TourbookPlugin.getDefault()//
																.getPreferenceStore();

	private IPartListener2			_partListener;
	private IPropertyChangeListener	_prefChangeListener;

	private TableViewer				_photoViewer;
	private ColumnManager			_columnManager;

	private ArrayList<PhotoFile>	_photoFiles			= new ArrayList<PhotoFile>();

	private ActionRefreshViewer		_actionRefreshViewer;

	/*
	 * UI controls
	 */
	private PixelConverter			_pc;

	private Combo					_comboPath;
	private Button					_btnSelectPath;

	private class ActionRefreshViewer extends Action {

		public ActionRefreshViewer() {

//			setToolTipText(Messages.Pref_Map_Button_RefreshTileInfoSelected_Tooltip);
//
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__refresh));
//			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.App_Image_RefreshSelected_Disabled));
		}

		@Override
		public void run() {
			doRefresh();
		}

	}

	private class PhotoContentProvider implements IStructuredContentProvider {

		public PhotoContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _photoFiles.toArray();
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	protected static boolean hasExifData(final File file) {
		//        Debug.debug("hasExifData file", file.getAbsoluteFile());

		if (!file.getName().toLowerCase().endsWith(".jpg")) {
			return false;
			//ImageFormat format = Sanselan.guessFormat(file);
			//if (format != ImageFormat.IMAGE_FORMAT_JPEG)
			//    return false;
		}

		//        Debug.debug("possible file", file);

		try {
			final ByteSource byteSource = new ByteSourceFile(file);
			return new JpegImageParser().hasExifSegment(byteSource);
		} catch (final Exception e) {
			//            Debug.debug("Error file", file.getAbsoluteFile());
			//            Debug.debug(e, 4);
			return false;
		}
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotoDirectoryView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_photoViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_photoViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_photoViewer.getTable().redraw();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createActions() {

		/*
		 * create actions
		 */
		_actionRefreshViewer = new ActionRefreshViewer();
		final ActionModifyColumns actionModifyColumns = new ActionModifyColumns(this);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(_actionRefreshViewer);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);
		createActions();

		addPartListener();
		addPrefListener();

		restoreState();

		_photoViewer.setInput(new Object[0]);
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
		{
			createUI10Path(container);
			createUI20PhotoViewer(container);
		}
	}

	private void createUI10Path(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.margins(2, 2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: path
			 */
			final Label label = new Label(container, SWT.NONE);
			label.setText("Photo path:");

			/*
			 * combo: path
			 */
			_comboPath = new Combo(container, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(_comboPath);
			_comboPath.setVisibleItemCount(20);
//			_comboPath.addModifyListener(filePathModifyListener);
			_comboPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectPath = new Button(container, SWT.PUSH);
			_btnSelectPath.setText(Messages.app_btn_browse);
			_btnSelectPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
//					validateFields();
				}
			});
		}
	}

	/**
	 * @param parent
	 */
	private void createUI20PhotoViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_photoViewer = new TableViewer(table);
		_columnManager.createColumns(_photoViewer);

		// table viewer
		_photoViewer.setContentProvider(new PhotoContentProvider());
//		_tourViewer.setSorter(new DeviceImportSorter());

		_photoViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object firstElement = ((IStructuredSelection) _photoViewer.getSelection()).getFirstElement();

				if ((firstElement != null) && (firstElement instanceof TourData)) {

				}
			}
		});

		_photoViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
//				fireSelectedTour();
			}
		});
	}

	private void defineAllColumns() {

		defineColumnName();
	}

	/**
	 * column: name
	 */
	private void defineColumnName() {

		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_NAME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final PhotoFile photoFile = (PhotoFile) cell.getElement();
				cell.setText(photoFile.fileName);
			}
		});
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void doRefresh() {

		try {
			refreshViewer();
		} catch (final Exception e) {
			StatusUtil.log(e);
		}
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	private void getImageFiles() throws IOException, ImageReadException {

		_photoFiles.clear();

//		File imagesFolder = new File("C:\\TEST-images\\");
		File imagesFolder = new File(getPathName());

		imagesFolder = imagesFolder.getAbsoluteFile();

		final FileSystemTraversal.Visitor visitor = new FileSystemTraversal.Visitor() {

			public boolean visit(final File file, final double progressEstimate) {

				if (!Sanselan.hasImageFileExtension(file)) {
					return true;
				}

				_photoFiles.add(new PhotoFile(file));

				return true;
			}
		};
		new FileSystemTraversal().traverseFiles(imagesFolder, visitor);
	}

	private String getPathName() {
		return _comboPath.getText().trim();
	}

	@Override
	public ColumnViewer getViewer() {
		return _photoViewer;
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			_comboPath.setText(selectedDirectoryName);
		}
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		final Table viewerControl = _photoViewer.getTable();
		final Composite viewerParent = viewerControl.getParent();

		viewerParent.setRedraw(false);
		{
			viewerControl.dispose();
			createUI20PhotoViewer(viewerParent);
			viewerParent.layout();

			// update the viewer
			reloadViewer();
		}
		viewerParent.setRedraw(true);

		return _photoViewer;
	}

	private void refreshViewer() throws Exception {

		getImageFiles();

		for (final PhotoFile photoFile : _photoFiles) {

			final File imageFile = photoFile.photoFile;

			try {
				final Map<String, Boolean> params = new HashMap<String, Boolean>();
				final boolean ignoreImageData = true;//isPhilHarveyTestImage(imageFile);
				params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, new Boolean(!ignoreImageData));

				final JpegImageMetadata metadata = (JpegImageMetadata) Sanselan.getMetadata(imageFile, params);
				if (null == metadata) {
					continue;
				}

				final TiffImageMetadata exifMetadata = metadata.getExif();
				if (null == exifMetadata) {
					continue;
				}

				final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
				if (null == gpsInfo) {
					continue;
				}

//				Debug.debug("imageFile", imageFile);
//				Debug.debug("gpsInfo", gpsInfo);
//				Debug.debug("gpsInfo longitude as degrees east", gpsInfo.getLongitudeAsDegreesEast());
//				Debug.debug("gpsInfo latitude as degrees north", gpsInfo.getLatitudeAsDegreesNorth());
//				Debug.debug();

			} catch (final Exception e) {

//				Debug.debug("imageFile", imageFile.getAbsoluteFile());
//				Debug.debug("imageFile", imageFile.length());
//				Debug.debug(e, 13);

				//                File brokenFolder = new File(imageFile.getParentFile(), "@Broken");
				//                if(!brokenFolder.exists())
				//                    brokenFolder.mkdirs();
				//                File movedFile = new File(brokenFolder, imageFile.getName());
				//                imageFile.renameTo(movedFile);

				throw e;
			}
		}

	}

	@Override
	public void reloadViewer() {
		// TODO Auto-generated method stub

	}

	private void restoreState() {

		// photo path
		UI.restoreCombo(_comboPath, _state.getArray(STATE_PHOTO_PATH));
	}

	private void saveState() {

		// path
		if (validateFilePath()) {
			_state.put(STATE_PHOTO_PATH, Util.getUniqueItems(_comboPath.getItems(), getPathName(), 20));
		}

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {

	}

	private boolean validateFilePath() {

		// check path
		final IPath filePath = new Path(getPathName());
		if (new File(filePath.toOSString()).exists() == false) {

			// invalid path
			return false;
		}

		return true;
	}

}
