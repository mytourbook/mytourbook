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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class PhotoDirectoryViewSimple extends ViewPart /* implements ITourViewer */{

//	static public final String				ID						= "net.tourbook.photo.photoDirectoryView";	//$NON-NLS-1$
//
//	private static final String				STATE_PHOTO_FILE_PATH	= "PhotoFilePath";							//$NON-NLS-1$
//
//	private static final IDialogSettings	_state					= TourbookPlugin.getDefault()//
//																			.getDialogSettingsSection(
//																					"PhotoDirectoryView");		//$NON-NLS-1$
//	private static final IPreferenceStore	_prefStore				= TourbookPlugin.getDefault()//
//																			.getPreferenceStore();
//
//	private static final DateTimeFormatter	_dateFormatter			= DateTimeFormat.shortDate();
//	private static final DateTimeFormatter	_timeFormatter			= DateTimeFormat.mediumTime();
//
//	private static final DateTimeFormatter	_dtParser				= DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss")// //$NON-NLS-1$
//																			.withZone(DateTimeZone.UTC);
//
//	private static final NumberFormat		_nf0					= NumberFormat.getNumberInstance();
//	private static final NumberFormat		_nf8					= NumberFormat.getNumberInstance();
//	{
//		_nf0.setMinimumFractionDigits(0);
//		_nf0.setMaximumFractionDigits(0);
//		_nf8.setMinimumFractionDigits(8);
//		_nf8.setMaximumFractionDigits(8);
//	}
//
//	private IPartListener2					_partListener;
//	private IPropertyChangeListener			_prefChangeListener;
//	private PostSelectionProvider			_postSelectionProvider;
//
//	private TableViewer						_photoViewer;
//	private ColumnManager					_columnManager;
//
//	private ArrayList<Photo>				_photos					= new ArrayList<Photo>();
//
//	private ActionRefreshViewer				_actionRefreshViewer;
//
//	/*
//	 * UI controls
//	 */
//	private PixelConverter					_pc;
//
//	private Combo							_comboPath;
//	private Button							_btnSelectPath;
//
//	private class ActionRefreshViewer extends Action {
//
//		public ActionRefreshViewer() {
//
////			setToolTipText(Messages.Pref_Map_Button_RefreshTileInfoSelected_Tooltip);
////
//			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__refresh));
////			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.App_Image_RefreshSelected_Disabled));
//		}
//
//		@Override
//		public void run() {
//			doRefresh();
//		}
//
//	}
//
//	private class PhotoContentProvider implements IStructuredContentProvider {
//
//		public PhotoContentProvider() {}
//
//		@Override
//		public void dispose() {}
//
//		@Override
//		public Object[] getElements(final Object inputElement) {
//			return _photos.toArray();
//		}
//
//		@Override
//		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
//	}
//
//	private void addPartListener() {
//
//		_partListener = new IPartListener2() {
//			@Override
//			public void partActivated(final IWorkbenchPartReference partRef) {}
//
//			@Override
//			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}
//
//			@Override
//			public void partClosed(final IWorkbenchPartReference partRef) {
//				if (partRef.getPart(false) == PhotoDirectoryViewSimple.this) {
//					saveState();
//				}
//			}
//
//			@Override
//			public void partDeactivated(final IWorkbenchPartReference partRef) {}
//
//			@Override
//			public void partHidden(final IWorkbenchPartReference partRef) {}
//
//			@Override
//			public void partInputChanged(final IWorkbenchPartReference partRef) {}
//
//			@Override
//			public void partOpened(final IWorkbenchPartReference partRef) {}
//
//			@Override
//			public void partVisible(final IWorkbenchPartReference partRef) {}
//		};
//		getViewSite().getPage().addPartListener(_partListener);
//	}
//
//	private void addPrefListener() {
//
//		_prefChangeListener = new IPropertyChangeListener() {
//			@Override
//			public void propertyChange(final PropertyChangeEvent event) {
//
//				final String property = event.getProperty();
//
//				if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {
//
//					_photoViewer.getTable().setLinesVisible(
//							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//
//					_photoViewer.refresh();
//
//					/*
//					 * the tree must be redrawn because the styled text does not show with the new
//					 * color
//					 */
//					_photoViewer.getTable().redraw();
//				}
//			}
//		};
//
//		_prefStore.addPropertyChangeListener(_prefChangeListener);
//	}
//
//	private void createActions() {
//
//		/*
//		 * create actions
//		 */
//		_actionRefreshViewer = new ActionRefreshViewer();
//		final ActionModifyColumns actionModifyColumns = new ActionModifyColumns(this);
//
//		/*
//		 * fill view menu
//		 */
//		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
//		menuMgr.add(actionModifyColumns);
//
//		/*
//		 * fill view toolbar
//		 */
//		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
//		tbm.add(_actionRefreshViewer);
//	}

	@Override
	public void createPartControl(final Composite parent) {

//		_pc = new PixelConverter(parent);
//
//		// define all columns
//		_columnManager = new ColumnManager(this, _state);
//		defineAllColumns();
//
//		createUI(parent);
//		createActions();
//
//		addPartListener();
//		addPrefListener();
//
//		// set selection provider
//		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());
//
//		restoreState();
//
////		updateViewer();
	}

//	private void createUI(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
//		{
//			createUI10Path(container);
//			createUI20PhotoViewer(container);
//		}
//	}
//
//	private void createUI10Path(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//		GridLayoutFactory.fillDefaults()//
//				.numColumns(3)
//				.margins(2, 2)
//				.applyTo(container);
////		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//		{
//			/*
//			 * label: path
//			 */
//			final Label label = new Label(container, SWT.NONE);
//			label.setText("Photo path:");
//
//			/*
//			 * combo: path
//			 */
//			_comboPath = new Combo(container, SWT.SINGLE | SWT.BORDER);
//			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(_comboPath);
//			_comboPath.setVisibleItemCount(20);
////			_comboPath.addModifyListener(filePathModifyListener);
//			_comboPath.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
////					validateFields();
//				}
//			});
//
//			/*
//			 * button: browse
//			 */
//			_btnSelectPath = new Button(container, SWT.PUSH);
//			_btnSelectPath.setText(Messages.app_btn_browse);
//			_btnSelectPath.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onSelectBrowseDirectory();
////					validateFields();
//				}
//			});
//		}
//	}
//
//	/**
//	 * @param parent
//	 */
//	private void createUI20PhotoViewer(final Composite parent) {
//
//		// table
//		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
//
//		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		table.setHeaderVisible(true);
//		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//
//		_photoViewer = new TableViewer(table);
//		_columnManager.createColumns(_photoViewer);
//
//		_photoViewer.setContentProvider(new PhotoContentProvider());
////		_tourViewer.setSorter(new DeviceImportSorter());
//
//		_photoViewer.addDoubleClickListener(new IDoubleClickListener() {
//			@Override
//			public void doubleClick(final DoubleClickEvent event) {
//				openPhoto(true);
//			}
//		});
//
//		_photoViewer.getTable().addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyPressed(final KeyEvent e) {
//				if (e.keyCode == SWT.CR) {
//					openPhoto(true);
//				}
//			}
//		});
//
//		_photoViewer.addSelectionChangedListener(new ISelectionChangedListener() {
//			@Override
//			public void selectionChanged(final SelectionChangedEvent event) {
//				openPhoto(false);
//			}
//		});
//
//		createUI500ContextMenu();
//	}
//
//	/**
//	 * create the viewer context menu
//	 */
//	private void createUI500ContextMenu() {
//
//		final Table table = (Table) _photoViewer.getControl();
//
//		_columnManager.createHeaderContextMenu(table, null);
//	}
//
//	private void defineAllColumns() {
//
//		defineColumnName();
//		defineColumnDate();
//		defineColumnTime();
//		defineColumnDimension();
//		defineColumnOrientation();
//		defineColumnImageDirectionText();
//		defineColumnImageDirectionDegree();
//		defineColumnAltitude();
//		defineColumnLatitude();
//		defineColumnLongitude();
//		defineColumnLocation();
////		defineColumnOtherTags();
//	}
//
//	/**
//	 * column: altitude
//	 */
//	private void defineColumnAltitude() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_ALTITUDE.createColumn(_columnManager, _pc);
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				final double altitude = photoFile.getAltitude();
//
//				if (altitude == Double.MIN_VALUE) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(Integer.toString((int) (altitude / UI.UNIT_VALUE_ALTITUDE)));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: date
//	 */
//	private void defineColumnDate() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_DATE.createColumn(_columnManager, _pc);
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//
//				final DateTime dt = photoFile.getDateTime();
//				if (dt == null) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(_dateFormatter.print(dt.getMillis()));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: width x height
//	 */
//	private void defineColumnDimension() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_DIMENSION.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				final int width = photoFile.getWidth();
//				final int height = photoFile.getHeight();
//				if (width == Integer.MIN_VALUE || height == Integer.MIN_VALUE) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(Integer.toString(width) + " x " + Integer.toString(height));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: image direction degree
//	 */
//	private void defineColumnImageDirectionDegree() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_IMAGE_DIRECTION_DEGREE//
//				.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				final double imageDirection = photoFile.getImageDirection();
//
//				if (imageDirection == Double.MIN_VALUE) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(Integer.toString((int) imageDirection));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: image direction degree
//	 */
//	private void defineColumnImageDirectionText() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_IMAGE_DIRECTION_TEXT//
//				.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				final double imageDirection = photoFile.getImageDirection();
//
//				if (imageDirection == Double.MIN_VALUE) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					final int imageDirectionInt = (int) imageDirection;
//					cell.setText(getDirectionText(imageDirectionInt));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: latitude
//	 */
//	private void defineColumnLatitude() {
//
//		final ColumnDefinition colDef = net.tourbook.ui.TableColumnFactory.LATITUDE.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final double latitude = ((Photo) cell.getElement()).getLatitude();
//
//				if (latitude == Double.MIN_VALUE) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(_nf8.format(latitude));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: location
//	 */
//	private void defineColumnLocation() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_LOCATION.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				cell.setText(photoFile.getGpsAreaInfo());
//			}
//		});
//	}
//
//	/**
//	 * column: longitude
//	 */
//	private void defineColumnLongitude() {
//
//		final ColumnDefinition colDef = net.tourbook.ui.TableColumnFactory.LONGITUDE.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final double longitude = ((Photo) cell.getElement()).getLongitude();
//
//				if (longitude == Double.MIN_VALUE) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(_nf8.format(longitude));
//				}
//			}
//		});
//	}
//
//	/**
//	 * column: name
//	 */
//	private void defineColumnName() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_NAME.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				cell.setText(photoFile.getFileName());
//			}
//		});
//	}
//
//	/**
//	 * column: orientation
//	 */
//	private void defineColumnOrientation() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_ORIENTATION.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//				cell.setText(Integer.toString(photoFile.getOrientation()));
//			}
//		});
//	}
//
//	/**
//	 * column: tags which are not used
//	 */
//	private void defineColumnOtherTags() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_OTHER_TAGS.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photo = (Photo) cell.getElement();
//
//				final String otherTags = //
//				("  -  orientation:" + photo.getOrientation())
//						+ ("  -  altitude:" + (photo.getAltitude() == Double.MIN_VALUE ? "?" : photo.getAltitude()));
//
//				cell.setText(otherTags);
//			}
//		});
//	}
//
//	/**
//	 * column: time
//	 */
//	private void defineColumnTime() {
//
//		final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_TIME.createColumn(_columnManager, _pc);
//
//		colDef.setIsDefaultColumn();
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//
//				final Photo photoFile = (Photo) cell.getElement();
//
//				final DateTime dt = photoFile.getDateTime();
//				if (dt == null) {
//					cell.setText(UI.EMPTY_STRING);
//				} else {
//					cell.setText(_timeFormatter.print(dt.getMillis()));
//				}
//			}
//		});
//	}
//
//	@Override
//	public void dispose() {
//
//		getViewSite().getPage().removePartListener(_partListener);
//
//		_prefStore.removePropertyChangeListener(_prefChangeListener);
//
//		super.dispose();
//	}
//
//	private void doRefresh() {
//
//		try {
//			updateViewer();
//		} catch (final Exception e) {
//			StatusUtil.log(e);
//		}
//	}
//
//	@Override
//	public ColumnManager getColumnManager() {
//		return _columnManager;
//	}
//
//	private String getDirectionText(final int degreeDirection) {
//
//		final float degree = (degreeDirection + 22.5f) / 45.0f;
//
//		final int directionIndex = ((int) degree) % 8;
//
//		return IWeather.windDirectionText[directionIndex];
//	}
//
//	/**
//	 * Date/Time
//	 *
//	 * @param file
//	 * @param jpegMetadata
//	 * @return
//	 */
//	private DateTime getExifDate(final Photo photoFile, final JpegImageMetadata jpegMetadata) {
//
////		/*
////		 * !!! time is not correct, maybe it is the time when the GPS signal was
////		 * received !!!
////		 */
////		printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_TIME_STAMP);
//
//		final TiffField date = jpegMetadata.findEXIFValueWithExactMatch(TiffConstants.TIFF_TAG_DATE_TIME);
//
//		if (date != null) {
//
//			try {
//				return _dtParser.parseDateTime(date.getStringValue());
//			} catch (final Exception e) {
//				// ignore
//			}
//		}
//
//		return null;
//	}
//
//	/**
//	 * GPS area info
//	 */
//	private String getExifGpsArea(final Photo photoFile, final JpegImageMetadata jpegMetadata) {
//		try {
//			final TiffField field = jpegMetadata
//					.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_AREA_INFORMATION);
//			if (field != null) {
//				final Object fieldValue = field.getValue();
//				if (fieldValue != null) {
//
//					/**
//					 * source: Exif 2.2 specification
//					 *
//					 * <pre>
//					 *
//					 * Table 6 Character Codes and their Designation
//					 *
//					 * Character Code	Code Designation (8 Bytes) 						References
//					 * ASCII  			41.H, 53.H, 43.H, 49.H, 49.H, 00.H, 00.H, 00.H  ITU-T T.50 IA5
//					 * JIS				A.H, 49.H, 53.H, 00.H, 00.H, 00.H, 00.H, 00.H   JIS X208-1990
//					 * Unicode			55.H, 4E.H, 49.H, 43.H, 4F.H, 44.H, 45.H, 00.H  Unicode Standard
//					 * Undefined		00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H  Undefined
//					 *
//					 * </pre>
//					 */
//					final byte[] byteArrayValue = field.getByteArrayValue();
//					final int fieldLength = byteArrayValue.length;
//
//					if (fieldLength > 0) {
//
//						/**
//						 * <pre>
//						 *
//						 * skipping 1 + 6 characters:
//						 *
//						 * 1      character code
//						 * 2...7  have no idea why these bytes are set to none valid characters
//						 *
//						 * </pre>
//						 */
//						final byte[] valueBytes = Arrays.copyOfRange(byteArrayValue, 7, fieldLength);
//
//						String valueString = null;
//
//						final byte firstByte = byteArrayValue[0];
//						if (firstByte == 0x55) {
//
//							valueString = new String(valueBytes, "UTF-16");
//
//						} else {
//
//							valueString = new String(valueBytes);
//						}
//
//						return valueString;
//					}
//				}
//			}
//		} catch (final Exception e) {
//			// ignore
//		}
//
//		return null;
//	}
//
//	private int getExifIntValue(final Photo photoFile,
//								final JpegImageMetadata jpegMetadata,
//								final TagInfo tiffTag,
//								final int defaultValue) {
//
//		try {
//			final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tiffTag);
//			if (field != null) {
//				return field.getIntValue();
//			}
//		} catch (final Exception e) {
//			// ignore
//		}
//
//		return defaultValue;
//	}
//
//	/**
//	 * Latitude + lLongitude
//	 *
//	 * @param file
//	 * @param jpegMetadata
//	 * @throws ImageReadException
//	 */
//	private void getExifLatLon(final Photo photoFile, final JpegImageMetadata jpegMetadata) {
//
//		final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
//		if (exifMetadata != null) {
//
//			try {
//				final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
//				if (gpsInfo != null) {
//
//					photoFile.setLatitude(gpsInfo.getLatitudeAsDegreesNorth());
//					photoFile.setLongitude(gpsInfo.getLongitudeAsDegreesEast());
//				}
//			} catch (final Exception e) {
//				// ignore
//			}
//		}
//	}
//
//	/**
//	 * Image direction
//	 *
//	 * @param tagInfo
//	 */
//	private double getExifValueDouble(final Photo photoFile, final JpegImageMetadata jpegMetadata, final TagInfo tagInfo) {
//		try {
//			final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
//			if (field != null) {
//				return field.getDoubleValue();
//			}
//		} catch (final Exception e) {
//			// ignore
//		}
//
//		return Double.MIN_VALUE;
//	}
//
//	private void getImageFiles() throws IOException, ImageReadException {
//
//		_photos.clear();
//
////		File imagesFolder = new File("C:\\TEST-images\\");
//		File imagesFolder = new File(getPathName());
//
//		imagesFolder = imagesFolder.getAbsoluteFile();
//
//		final FileSystemTraversal.Visitor visitor = new FileSystemTraversal.Visitor() {
//
//			public boolean visit(final File file, final double progressEstimate) {
//
//				if (!Sanselan.hasImageFileExtension(file)) {
//					return true;
//				}
//
//				_photos.add(new Photo(file));
//
//				return true;
//			}
//		};
//		new FileSystemTraversal().traverseFiles(imagesFolder, visitor);
//	}
//
//	private String getPathName() {
//		return _comboPath.getText().trim();
//	}
//
//	private DateTime getTiffDate(final Photo photo, final TiffImageMetadata tiffMetadata) {
//
//		try {
//
//			final TiffField date = tiffMetadata.findField(TiffConstants.TIFF_TAG_DATE_TIME, true);
//			if (date != null) {
//				return _dtParser.parseDateTime(date.getStringValue());
//			}
//
//		} catch (final Exception e) {
//			// ignore
//		}
//
//		return null;
//	}
//
//	private int getTiffIntValue(final Photo photo,
//								final TiffImageMetadata tiffMetadata,
//								final TagInfo tiffTag,
//								final int defaultValue) {
//
//		try {
//			final TiffField field = tiffMetadata.findField(tiffTag, true);
//			if (field != null) {
//				return field.getIntValue();
//			}
//		} catch (final Exception e) {
//			// ignore
//		}
//
//		return defaultValue;
//	}
//
//	@Override
//	public ColumnViewer getViewer() {
//		return _photoViewer;
//	}
//
//	private void onSelectBrowseDirectory() {
//
//		final DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
//		dialog.setText(Messages.dialog_export_dir_dialog_text);
//		dialog.setMessage(Messages.dialog_export_dir_dialog_message);
//
//		final String selectedDirectoryName = dialog.open();
//
//		if (selectedDirectoryName != null) {
//			_comboPath.setText(selectedDirectoryName);
//
//			updateViewer();
//		}
//	}
//
//	/**
//	 * Display photo in the photo view.
//	 *
//	 * @param isOpenView
//	 */
//	private void openPhoto(final boolean isOpenView) {
//
//		final IStructuredSelection structuredSelection = (IStructuredSelection) _photoViewer.getSelection();
//
//		final Object firstElement = structuredSelection.getFirstElement();
//
//		if ((firstElement != null) && (firstElement instanceof Photo)) {
//
//			if (isOpenView) {
//				Util.showViewNotActive(PhotoImageView.ID);
//			}
//
//			final Photo photo = (Photo) firstElement;
//			final int oldWidth = photo.getWidth();
//
//			_postSelectionProvider.setSelection(structuredSelection);
//
//			if (oldWidth != photo.getWidth()) {
//
//				// width & height has been updated in the selection listener -> update viewer
//
//				_photoViewer.update(photo, null);
//			}
//		}
//	}
//
//	@Override
//	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {
//
//		final Table viewerControl = _photoViewer.getTable();
//		final Composite viewerParent = viewerControl.getParent();
//
//		viewerParent.setRedraw(false);
//		{
//			viewerControl.dispose();
//			createUI20PhotoViewer(viewerParent);
//			viewerParent.layout();
//
//			// update the viewer
//			reloadViewer();
//		}
//		viewerParent.setRedraw(true);
//
//		return _photoViewer;
//	}
//
//	@Override
//	public void reloadViewer() {
//		_photoViewer.setInput(new Object[0]);
//	}
//
//	private void restoreState() {
//
//		// photo path
//		UI.restoreCombo(_comboPath, _state.getArray(STATE_PHOTO_FILE_PATH));
//	}
//
//	private void saveState() {
//
//		// path
//		if (validateFilePath()) {
//			_state.put(STATE_PHOTO_FILE_PATH, Util.getUniqueItems(_comboPath.getItems(), getPathName(), 20));
//		}
//
//		_columnManager.saveState(_state);
//	}

	@Override
	public void setFocus() {

	}

//	private void updateViewer() {
//
//		try {
//			getImageFiles();
//		} catch (final Exception e) {
//			StatusUtil.log(e);
//		}
//
//		for (final Photo photo : _photos) {
//
////			System.out.println(photo.getFileName() + "\t");
////			// TODO remove SYSTEM.OUT.PRINTLN
//
//			final File imageFile = photo.getImageFile();
//
//			try {
//				final Map<String, Boolean> params = new HashMap<String, Boolean>();
//				final boolean ignoreImageData = true;//isPhilHarveyTestImage(imageFile);
//				params.put(SanselanConstants.PARAM_KEY_READ_THUMBNAILS, new Boolean(!ignoreImageData));
//
//				final IImageMetadata metadata = Sanselan.getMetadata(imageFile, params);
//
//				/*
//				 * this will log all available meta data
//				 */
////				System.out.println(metadata.toString());
//
//				/*
//				 * read meta data for 1 photo
//				 */
//				if (metadata instanceof TiffImageMetadata) {
//
//					final TiffImageMetadata tiffMetadata = (TiffImageMetadata) metadata;
//
//					photo.setDateTime(getTiffDate(photo, tiffMetadata));
//					photo.setSize(
//							getTiffIntValue(
//									photo,
//									tiffMetadata,
//									TiffTagConstants.TIFF_TAG_IMAGE_WIDTH,
//									Integer.MIN_VALUE),
//							getTiffIntValue(
//									photo,
//									tiffMetadata,
//									TiffTagConstants.TIFF_TAG_IMAGE_LENGTH,
//									Integer.MIN_VALUE));
//
//				} else if (metadata instanceof JpegImageMetadata) {
//
//					final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
//
//					photo.setDateTime(getExifDate(photo, jpegMetadata));
//
//					photo.setSize(
//							getExifIntValue(
//									photo,
//									jpegMetadata,
//									ExifTagConstants.EXIF_TAG_EXIF_IMAGE_WIDTH,
//									Integer.MIN_VALUE),
//							getExifIntValue(
//									photo,
//									jpegMetadata,
//									ExifTagConstants.EXIF_TAG_EXIF_IMAGE_LENGTH,
//									Integer.MIN_VALUE));
//
//					photo.setOrientation(//
//							getExifIntValue(photo, jpegMetadata, ExifTagConstants.EXIF_TAG_ORIENTATION, 1));
//
//					photo.setImageDirection(//
//							getExifValueDouble(photo, jpegMetadata, GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION));
//
//					photo.setAltitude(getExifValueDouble(photo, jpegMetadata, GpsTagConstants.GPS_TAG_GPS_ALTITUDE));
//
//					getExifLatLon(photo, jpegMetadata);
//					photo.setGpsAreaInfo(getExifGpsArea(photo, jpegMetadata));
//				}
//
//				// ensure date is set
//				if (photo.getDateTime() == null) {
//					photo.setDateTime(new DateTime(imageFile.lastModified()));
//				}
//
//			} catch (final Exception e) {
//				StatusUtil.log(e);
//			}
//		}
//
//		_photoViewer.setInput(this);
//	}
//
//	private boolean validateFilePath() {
//
//		// check path
//		final IPath filePath = new Path(getPathName());
//		if (new File(filePath.toOSString()).exists() == false) {
//
//			// invalid path
//			return false;
//		}
//
//		return true;
//	}

}
