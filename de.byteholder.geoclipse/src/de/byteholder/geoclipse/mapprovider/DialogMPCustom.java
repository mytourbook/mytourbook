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
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.IMapListener;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.MapEvent;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.preferences.PrefPageMapProviders;
import de.byteholder.geoclipse.ui.ViewerDetailForm;
import de.byteholder.geoclipse.util.PixelConverter;
import de.byteholder.geoclipse.util.Util;
import de.byteholder.gpx.GeoPosition;

public class DialogMPCustom extends DialogMP implements ITileListener, IMapDefaultActions {

	private static final String				PARAMETER_TRAILING_CHAR					= "}";													//$NON-NLS-1$
	private static final String				PARAMETER_LEADING_CHAR					= "{";													//$NON-NLS-1$

	private static final int				MAX_RANDOM								= 9999;

	private static final int				UI_MAX_ZOOM_LEVEL						= 20;
	private static final int				UI_MIN_ZOOM_LEVEL						= 1;
	private static final int				MAP_MAX_ZOOM_LEVEL						= UI_MAX_ZOOM_LEVEL
																							- UI_MIN_ZOOM_LEVEL;

	public static final String				DEFAULT_URL								= "http://";											//$NON-NLS-1$

	private static final String				DIALOG_SETTINGS_VIEWER_WIDTH			= "ViewerWidth";										//$NON-NLS-1$

	private static final String				DIALOG_SETTINGS_IS_SHOW_TILE_INFO		= "IsShowTileInfo";									//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG	= "IsShowTileImageLogging";							//$NON-NLS-1$
	private static final String				DIALOG_SETTINGS_EXAMPLE_URL				= "ExampleUrl";										//$NON-NLS-1$

	// feldberg
//	private static final String				DEFAULT_EXAMPLE							= "http://tile.openstreetmap.org/16/34225/22815.png";	//$NON-NLS-1$

	// tremola
	private static final String				DEFAULT_EXAMPLE							= "http://tile.openstreetmap.org/15/17165/11587.png";	//$NON-NLS-1$

	/*
	 * UI controls
	 */
	private Display							_display;

	private Button							_btnOk;
	private Composite						_leftContainer;

	private ViewerDetailForm				_detailForm;
	private ToolBar							_toolbar;

	private Label							_lblMapInfo;

	private Label							_lblTileInfo;
	private Button							_chkShowTileInfo;
	private Text							_txtExampleUrl;
	private Text							_txtCustomUrl;
	private Combo							_cboTileImageLog;
	private Button							_btnShowMap;

	private Spinner							_spinMinZoom;
	private Spinner							_spinMaxZoom;
	private Button							_btnShowOsmMap;
	private Composite						_partContainer;
	private Button							_chkShowTileImageLog;

	private Label							_lblLog;
	private Text							_txtImageFormat;

	/*
	 * NON-UI fields
	 */
	private NumberFormat					_nfLatLon								= NumberFormat.getNumberInstance();

	{
		// initialize lat/lon formatter
		_nfLatLon.setMinimumFractionDigits(6);
		_nfLatLon.setMaximumFractionDigits(6);
	}

	/**
	 * url parameter items which can be selected in the combobox for each parameter
	 */
	private ArrayList<PartUIItem>			PART_ITEMS								= new ArrayList<PartUIItem>();

	{
		PART_ITEMS.add(new PartUIItem(//
				PART_TYPE.NONE,
				WIDGET_KEY.PAGE_NONE,
				UI.EMPTY_STRING,
				UI.EMPTY_STRING));

		PART_ITEMS.add(new PartUIItem(
				PART_TYPE.HTML,
				WIDGET_KEY.PAGE_HTML,
				Messages.Url_Parameter_Text,
				Messages.Url_Parameter_Text_Abbr));

		PART_ITEMS.add(new PartUIItem(
				PART_TYPE.ZOOM,
				WIDGET_KEY.PAGE_ZOOM,
				Messages.Url_Parameter_Zoom,
				Messages.Url_Parameter_Zoom_Abbr));

		PART_ITEMS.add(new PartUIItem(
				PART_TYPE.X,
				WIDGET_KEY.PAGE_X,
				Messages.Url_Parameter_X,
				Messages.Url_Parameter_X_Abbr));

		PART_ITEMS.add(new PartUIItem(
				PART_TYPE.Y,
				WIDGET_KEY.PAGE_Y,
				Messages.Url_Parameter_Y,
				Messages.Url_Parameter_Y_Abbr));

		PART_ITEMS.add(new PartUIItem(
				PART_TYPE.RANDOM_INTEGER,
				WIDGET_KEY.PAGE_RANDOM,
				Messages.Url_Parameter_Random,
				Messages.Url_Parameter_Random_Abbr));
	}

	/**
	 * contains all rows with url parts which are displayed in the UI
	 */
	private ArrayList<PartRow>				PART_ROWS								= new ArrayList<PartRow>();

	private IDialogSettings					_dialogSettings;

	private PrefPageMapProviders			_prefPageMapFactory;

	private String							_defaultMessage;

	/**
	 * contains the custom url with all url parts which are converted to a string
	 */
	private String							_customUrl;
	private String							_previousCustomUrl;

	private MPCustom						_mpCustom;
	private MP								_defaultMapProvider;

	private boolean							_isInitUI								= false;

	private boolean							_isEnableTileImageLogging;
	private ConcurrentLinkedQueue<LogEntry>	_logEntries								= new ConcurrentLinkedQueue<LogEntry>();

	private int								_statUpdateCounter						= 0;
	private int								_statIsQueued;
	private int								_statStartLoading;
	private int								_statEndLoading;
	private int								_statErrorLoading;

	private int								_previousMinZoom;
	private int								_previousMaxZoom;

	private static final ReentrantLock		LOG_LOCK								= new ReentrantLock();

	enum PART_TYPE {
		NONE, //
		HTML, //
		//
		ZOOM, //
		X, //
		Y, //
		//
		RANDOM_INTEGER, //
		RANDOM_ALPHA, //
		//
	}

	private class PartRow {

		private Combo						rowCombo;

		/**
		 * The hashmap contains all widgets for one row
		 */
		private HashMap<WIDGET_KEY, Widget>	rowWidgets;

		public PartRow(final Combo combo, final HashMap<WIDGET_KEY, Widget> widgets) {
			rowCombo = combo;
			rowWidgets = widgets;
		}
	}

	private class PartUIItem {

		PART_TYPE	partKey;
		WIDGET_KEY	widgetKey;

		String		text;
		String		abbreviation;

		public PartUIItem(	final PART_TYPE partItemKey,
							final WIDGET_KEY partWidgetKey,
							final String partText,
							final String partAbbr) {

			partKey = partItemKey;
			widgetKey = partWidgetKey;
			text = partText;
			abbreviation = partAbbr;
		}
	}

	private enum WIDGET_KEY {
		//
		PAGEBOOK, //
		//
		PAGE_NONE, //
		PAGE_HTML, //
		PAGE_X, //
		PAGE_Y, //
		PAGE_ZOOM, //
		PAGE_RANDOM, //
		//
//		PAGE_LAT_TOP, //
//		PAGE_LAT_BOTTOM, //
//		PAGE_LON_LEFT, //
//		PAGE_LON_RIGHT, //
		//
		SPINNER_RANDOM_START, //
		SPINNER_RANDOM_END, //
		//
		INPUT_ZOOM,
		//
		TEXT_HTML,
		//
		LABEL_X_SIGN, //
		SPINNER_X_VALUE,
		//
		INPUT_Y_SIGN, //
		SPINNER_Y_VALUE, //
	}

	public DialogMPCustom(	final Shell parentShell,
							final PrefPageMapProviders mapFactory,
							final MPCustom customMapProvider) {

		super(parentShell, customMapProvider);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		_dialogSettings = Activator.getDefault().getDialogSettingsSection("DialogCustomConfiguration");//$NON-NLS-1$

		_prefPageMapFactory = mapFactory;
		_mpCustom = customMapProvider;

		_defaultMapProvider = MapProviderManager.getInstance().getDefaultMapProvider();
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
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_CustomConfig_DialogTitle);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {

				MP.removeTileListener(DialogMPCustom.this);
			}
		});
	}

	@Override
	public void create() {

		super.create();

		_display = Display.getCurrent();

		setTitle(Messages.Dialog_CustomConfig_DialogArea_Title);

		MP.addTileListener(this);

		restoreState();

		// initialize after the shell size is set
		initializeUIFromModel(_mpCustom);

		enableControls();
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

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);
		{
			createUI100Container(container, pixelConverter);
			createUI400UrlLogInfo(container, pixelConverter);
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
			createUI200LeftPart(_leftContainer, pixelConverter);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);
			UI.addSashColorHandler(sash);

			// right part (map)
			final Composite mapContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(mapContainer);
			createUI300Map(mapContainer, pixelConverter);

			_detailForm = new ViewerDetailForm(container, _leftContainer, sash, mapContainer, 30);
		}
	}

	private void createUI200LeftPart(final Composite parent, final PixelConverter pixelConverter) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			// label: url parameter
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			label.setText(Messages.Dialog_CustomConfig_Label_UrlParts);

			// url parts
			_partContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).span(1, 1).applyTo(_partContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_partContainer);
			{
				PART_ROWS.add(createUI210PartRow(_partContainer, 0, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 1, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 2, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 3, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 4, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 5, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 6, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 7, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 8, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 9, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 10, pixelConverter));
				PART_ROWS.add(createUI210PartRow(_partContainer, 11, pixelConverter));
			}

			createUI220Detail(container, pixelConverter);
			createUI240DebugInfo(container);
		}
	}

	private PartRow createUI210PartRow(final Composite container, final int row, final PixelConverter pixelConverter) {

		// combo: parameter item type
		final Combo combo = new Combo(container, SWT.READ_ONLY);
		combo.setVisibleItemCount(10);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isInitUI) {
					return;
				}

				final Combo combo = (Combo) e.widget;

				/*
				 * show page according to the selected item in the combobox
				 */
				final HashMap<WIDGET_KEY, Widget> rowWidgets = PART_ROWS.get(row).rowWidgets;

				onSelectPart(combo, rowWidgets);
			}
		});

		// fill combo
		for (final PartUIItem paraItem : PART_ITEMS) {
			combo.add(paraItem.text);
		}

		// select default
		combo.select(0);

		/*
		 * pagebook: parameter widgets
		 */
		final HashMap<WIDGET_KEY, Widget> paraWidgets = createUI212ParaWidgets(container, pixelConverter);

		return new PartRow(combo, paraWidgets);
	}

	private HashMap<WIDGET_KEY, Widget> createUI212ParaWidgets(	final Composite parent,
																final PixelConverter pixelConverter) {

		final HashMap<WIDGET_KEY, Widget> paraWidgets = new HashMap<WIDGET_KEY, Widget>();

		final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {

				Util.adjustSpinnerValueOnMouseScroll(event);

				// validate values
				if (event.widget == (Spinner) paraWidgets.get(WIDGET_KEY.SPINNER_RANDOM_START)) {
					onSelectRandomSpinnerMin(paraWidgets);
				} else {
					onSelectRandomSpinnerMax(paraWidgets);
				}
			}
		};

		final ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				updateUICustomUrl();
			}
		};

		final PageBook bookParameter = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(bookParameter);
		paraWidgets.put(WIDGET_KEY.PAGEBOOK, bookParameter);
		{
			// page: none
			Label label = new Label(bookParameter, SWT.NONE);
			paraWidgets.put(WIDGET_KEY.PAGE_NONE, label);

			/*
			 * page: html text
			 */
			final Composite textContainer = new Composite(bookParameter, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(textContainer);
			{
				final Text txtHtml = new Text(textContainer, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(txtHtml);
				txtHtml.addModifyListener(modifyListener);

				paraWidgets.put(WIDGET_KEY.TEXT_HTML, txtHtml);
			}
			paraWidgets.put(WIDGET_KEY.PAGE_HTML, textContainer);

			/*
			 * page: x
			 */
			final Composite xContainer = new Composite(bookParameter, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(xContainer);
			{
				label = new Label(xContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(createUI214Parameter(PART_TYPE.X));
			}
			paraWidgets.put(WIDGET_KEY.PAGE_X, xContainer);

			/*
			 * page: y
			 */
			final Composite yContainer = new Composite(bookParameter, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(yContainer);
			{
				label = new Label(yContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(createUI214Parameter(PART_TYPE.Y));
			}
			paraWidgets.put(WIDGET_KEY.PAGE_Y, yContainer);

			/*
			 * page: zoom
			 */
			final Composite zoomContainer = new Composite(bookParameter, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(zoomContainer);
			{
				label = new Label(zoomContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(createUI214Parameter(PART_TYPE.ZOOM));
			}
			paraWidgets.put(WIDGET_KEY.PAGE_ZOOM, zoomContainer);

			/*
			 * page: random
			 */
			final Composite randContainer = new Composite(bookParameter, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(randContainer);
			{
				final Spinner fromSpinner = new Spinner(randContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(fromSpinner);
				fromSpinner.setMinimum(0);
				fromSpinner.setMaximum(MAX_RANDOM);
				fromSpinner.setSelection(0);
				fromSpinner.addMouseWheelListener(mouseWheelListener);
				fromSpinner.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectRandomSpinnerMin(paraWidgets);
					}
				});
				fromSpinner.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectRandomSpinnerMin(paraWidgets);
					}
				});

				paraWidgets.put(WIDGET_KEY.SPINNER_RANDOM_START, fromSpinner);

				label = new Label(randContainer, SWT.NONE);
				label.setText("..."); //$NON-NLS-1$

				final Spinner toSpinner = new Spinner(randContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(toSpinner);
				toSpinner.setMinimum(1);
				toSpinner.setMaximum(MAX_RANDOM);
				toSpinner.setSelection(1);
				toSpinner.addMouseWheelListener(mouseWheelListener);
				toSpinner.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectRandomSpinnerMax(paraWidgets);
					}
				});
				toSpinner.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						onSelectRandomSpinnerMax(paraWidgets);
					}
				});

				paraWidgets.put(WIDGET_KEY.SPINNER_RANDOM_END, toSpinner);
			}
			paraWidgets.put(WIDGET_KEY.PAGE_RANDOM, randContainer);
		}

		// show hide page
		bookParameter.showPage((Control) paraWidgets.get(WIDGET_KEY.PAGE_NONE));

		return paraWidgets;
	}

	private String createUI214Parameter(final PART_TYPE itemKey) {

		for (final PartUIItem paraItem : PART_ITEMS) {
			if (paraItem.partKey == itemKey) {
				return PARAMETER_LEADING_CHAR + paraItem.abbreviation + PARAMETER_TRAILING_CHAR; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		StatusUtil.showStatus("invalid itemKey '" + itemKey + "'", new Exception()); //$NON-NLS-1$ //$NON-NLS-2$

		return UI.EMPTY_STRING;
	}

	private void createUI220Detail(final Composite parent, final PixelConverter pixelConverter) {

		Label label;
		final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {

				Util.adjustSpinnerValueOnMouseScroll(event);

				// validate values
				if (event.widget == _spinMinZoom) {
					onSelectZoomSpinnerMin();
				} else {
					onSelectZoomSpinnerMax();
				}
			}
		};

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label: image format
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.Dialog_CustomConfig_Label_ImageFormat);

			// label: image format value
			_txtImageFormat = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtImageFormat);
			_txtImageFormat.setToolTipText(Messages.Dialog_CustomConfig_Text_ImageFormat_Tooltip);

			// ################################################

			// label: min zoomlevel
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_CustomConfig_Label_ZoomLevel);

			// container: zoom
			final Composite zoomContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(zoomContainer);
			{
				// spinner: min zoom level
				_spinMinZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(_spinMinZoom);
				_spinMinZoom.setMinimum(UI_MIN_ZOOM_LEVEL);
				_spinMinZoom.setMaximum(UI_MAX_ZOOM_LEVEL);
				_spinMinZoom.setSelection(1);
				_spinMinZoom.addMouseWheelListener(mouseWheelListener);
				_spinMinZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectZoomSpinnerMin();
					}
				});
				_spinMinZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectZoomSpinnerMin();
					}
				});

				// ################################################

				label = new Label(zoomContainer, SWT.NONE);
				label.setText("..."); //$NON-NLS-1$

				// ################################################

				// spinner: min zoom level
				_spinMaxZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(_spinMaxZoom);
				_spinMaxZoom.setMinimum(UI_MIN_ZOOM_LEVEL);
				_spinMaxZoom.setMaximum(UI_MAX_ZOOM_LEVEL);
				_spinMaxZoom.setSelection(1);
				_spinMaxZoom.addMouseWheelListener(mouseWheelListener);
				_spinMaxZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectZoomSpinnerMax();
					}
				});
				_spinMaxZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (_isInitUI) {
							return;
						}
						onSelectZoomSpinnerMax();
					}
				});
			}
		}
	}

	private void createUI240DebugInfo(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			// check: show tile info
			_chkShowTileInfo = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//

					// display at the bottom of the dialog
					.grab(false, true)
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

	private void createUI300Map(final Composite parent, final PixelConverter pixelConverter) {

		final Composite toolbarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbarContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(toolbarContainer);
		{
			// button: update map
			_btnShowMap = new Button(toolbarContainer, SWT.NONE);
			_btnShowMap.setText(Messages.Dialog_CustomConfig_Button_UpdateMap);
			_btnShowMap.setToolTipText(Messages.Dialog_CustomConfig_Button_UpdateMap_Tooltip);
			_btnShowMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectCustomMap();
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

		_map.addMapListener(new IMapListener() {

			public void mapInfo(final MapEvent event) {

				final GeoPosition mapCenter = event.mapCenter;

				double lon = mapCenter.longitude % 360;
				lon = lon > 180 ? //
						lon - 360
						: lon < -180 ? //
								lon + 360
								: lon;

				_lblMapInfo.setText(NLS.bind(Messages.Dialog_MapConfig_Label_MapInfo, new Object[] {
						_nfLatLon.format(mapCenter.latitude),
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
		 * !!! don't do any map initialization until the tile factory is set !!!
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
	}

	private void createUI400UrlLogInfo(final Composite parent, final PixelConverter pixelConverter) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label: custom url
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Dialog_MapConfig_Label_CustomUrl);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_CustomUrl_Tooltip);

			// text: custom url
			_txtCustomUrl = new Text(container, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtCustomUrl);
			_txtCustomUrl.setToolTipText(Messages.Dialog_MapConfig_Label_CustomUrl_Tooltip);

			// ############################################################

			// label: example url
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Dialog_MapConfig_Label_ExampleUrl);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_ExampleUrl_Tooltip);

			// text: custom url
			_txtExampleUrl = new Text(container, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtExampleUrl);
			_txtExampleUrl.setToolTipText(Messages.Dialog_MapConfig_Label_ExampleUrl_Tooltip);

			// ############################################################

			// label: url log
			_lblLog = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblLog);
			_lblLog.setText(Messages.Dialog_MapConfig_Label_LoadedImageUrl);
			_lblLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);

			// combo: url log
			_cboTileImageLog = new Combo(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_cboTileImageLog);
			_cboTileImageLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
			_cboTileImageLog.setVisibleItemCount(40);
			_cboTileImageLog.setFont(getMonoFont());
		}
	}

	private void enableControls() {

		_isEnableTileImageLogging = _chkShowTileImageLog.getSelection();

		if (_isEnableTileImageLogging == false) {
			// remove old log entries
			_statUpdateCounter = 0;
			_cboTileImageLog.removeAll();
		}

		_lblLog.setEnabled(_isEnableTileImageLogging);
		_cboTileImageLog.setEnabled(_isEnableTileImageLogging);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _dialogSettings;

		// disable bounds
//		return null;
	}

	private void initializeUIFromModel(final MPCustom mp) {

		_mpCustom = mp;

		_isInitUI = true;
		{
			updateUIUrlParts();
			updateUICustomUrl();

			/*
			 * set zoom level
			 */
			final int minZoomLevel = _mpCustom.getMinZoomLevel();
			final int maxZoomLevel = _mpCustom.getMaxZoomLevel();
			_spinMinZoom.setSelection(minZoomLevel + UI_MIN_ZOOM_LEVEL);
			_spinMaxZoom.setSelection(maxZoomLevel + UI_MIN_ZOOM_LEVEL);

			_previousCustomUrl = mp.getCustomUrl();
			_previousMinZoom = minZoomLevel;
			_previousMaxZoom = maxZoomLevel;

			_txtImageFormat.setText(_mpCustom.getImageFormat());
		}
		_isInitUI = false;

		// show map provider in the message area
		_defaultMessage = NLS.bind(Messages.Dialog_MapConfig_DialogArea_Message, _mpCustom.getName());
		setMessage(_defaultMessage);

		// set factory and display map
		_map.setMapProviderWithReset(mp, true);
 
		// set position to previous position
		_map.setZoom(_mpCustom.getLastUsedZoom());
		_map.setGeoCenterPosition(_mpCustom.getLastUsedPosition());
	}

	@Override
	protected void okPressed() {

		// model is saved in the dialog opening code
		updateModelFromUI();

		super.okPressed();
	}

	private void onSelectCustomMap() {

		final int minZoom = _spinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int maxZoom = _spinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		// check if the custom url or zoom level has changed
		if (_customUrl.equals(_previousCustomUrl) && _previousMinZoom == minZoom && _previousMaxZoom == maxZoom) {
			// do nothing to optimize performace
			return;
		}

		// keep values for the next check
		_previousCustomUrl = _customUrl;
		_previousMinZoom = minZoom;
		_previousMaxZoom = maxZoom;

		updateModelFromUI();

		// reset all images
		_mpCustom.resetAll(false);

		// delete offline images to force the reload and to test the modified url
		_prefPageMapFactory.deleteOfflineMap(_mpCustom);

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * <br>
		 * ensure the map is using the correct zoom levels before other map actions are done<br>
		 * <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 */
		updateMapZoomLevel(_mpCustom);

		_map.setMapProviderWithReset(_mpCustom, true);
	}

	private void onSelectOsmMap() {

		if (_map.getMapProvider() == _defaultMapProvider) {

			// toggle map

			// update layers BEFORE the tile factory is set in the map
			updateModelFromUI();

			updateMapZoomLevel(_mpCustom);

			_map.setMapProviderWithReset(_mpCustom, true);

		} else {

			// display OSM

			// ensure the map is using the correct zoom levels
			updateMapZoomLevel(_defaultMapProvider);

			_defaultMapProvider.setStateToReloadOfflineCounter();

			_map.setMapProviderWithReset(_defaultMapProvider, true);
		}
	}

	/**
	 * Shows the part page which is selected in the combo
	 * 
	 * @param combo
	 * @param rowWidgets
	 */
	private void onSelectPart(final Combo combo, final HashMap<WIDGET_KEY, Widget> rowWidgets) {

		final PartUIItem selectedPartItem = PART_ITEMS.get(combo.getSelectionIndex());

		final PageBook pagebook = (PageBook) rowWidgets.get(WIDGET_KEY.PAGEBOOK);
		final Widget page = rowWidgets.get(selectedPartItem.widgetKey);

		pagebook.showPage((Control) page);

		_partContainer.layout();

		updateUICustomUrl();
	}

	private void onSelectRandomSpinnerMax(final HashMap<WIDGET_KEY, Widget> paraWidgets) {

		/*
		 * ensure the to value is larger than the from value
		 */
		final Spinner minSpinner = (Spinner) paraWidgets.get(WIDGET_KEY.SPINNER_RANDOM_START);
		final Spinner maxSpinner = (Spinner) paraWidgets.get(WIDGET_KEY.SPINNER_RANDOM_END);

		final int fromRand = minSpinner.getSelection();
		final int toRand = maxSpinner.getSelection();

		if (toRand <= fromRand) {

			if (toRand < 1) {
				minSpinner.setSelection(0);
				maxSpinner.setSelection(1);
			} else {
				minSpinner.setSelection(toRand - 1);
			}
		}

		updateUICustomUrl();
	}

	private void onSelectRandomSpinnerMin(final HashMap<WIDGET_KEY, Widget> paraWidgets) {

		/*
		 * ensure the from value is smaller than the to value
		 */
		final Spinner minSpinner = (Spinner) paraWidgets.get(WIDGET_KEY.SPINNER_RANDOM_START);
		final Spinner maxSpinner = (Spinner) paraWidgets.get(WIDGET_KEY.SPINNER_RANDOM_END);

		final int fromRand = minSpinner.getSelection();
		final int toRand = maxSpinner.getSelection();

		if (fromRand >= toRand) {

			if (toRand < MAX_RANDOM) {
				maxSpinner.setSelection(fromRand + 1);
			} else {
				minSpinner.setSelection(toRand - 1);
			}
		}

		updateUICustomUrl();
	}

	private void onSelectZoomSpinnerMax() {

		final int mapMinValue = _spinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = _spinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		if (mapMaxValue > MAP_MAX_ZOOM_LEVEL) {
			_spinMaxZoom.setSelection(UI_MAX_ZOOM_LEVEL);
		}

		if (mapMaxValue < mapMinValue) {
			_spinMinZoom.setSelection(mapMinValue + 1);
		}
	}

	private void onSelectZoomSpinnerMin() {

		final int mapMinValue = _spinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = _spinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		if (mapMinValue > mapMaxValue) {
			_spinMinZoom.setSelection(mapMaxValue + 1);
		}
	}

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			_detailForm.setViewerWidth(_dialogSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}

		final String exampleUrl = _dialogSettings.get(DIALOG_SETTINGS_EXAMPLE_URL);
		if (exampleUrl == null) {
			_txtExampleUrl.setText(DEFAULT_EXAMPLE);
		} else {
			_txtExampleUrl.setText(exampleUrl);
		}

		// debug tile info
		final boolean isShowDebugInfo = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_INFO);
		_chkShowTileInfo.setSelection(isShowDebugInfo);
		_map.setShowDebugInfo(isShowDebugInfo);

		// tile image loading
		final boolean isShowImageLogging = _dialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG);
		_chkShowTileImageLog.setSelection(isShowImageLogging);
	}

	private void saveState() {

		_dialogSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, _leftContainer.getSize().x);
		_dialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_INFO, _chkShowTileInfo.getSelection());
		_dialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG, _chkShowTileImageLog.getSelection());

		final String exampleUrl = _txtExampleUrl.getText();
		if (exampleUrl.length() > 0) {
			_dialogSettings.put(DIALOG_SETTINGS_EXAMPLE_URL, exampleUrl);
		}
	}

	/**
	 * select part type in the row combo
	 */
	private int selectPartType(final PartRow partRow, final PART_TYPE partType) {

		int partTypeIndex = 0;
		for (final PartUIItem partItem : PART_ITEMS) {
			if (partItem.partKey == partType) {
				break;
			}
			partTypeIndex++;
		}

		final Combo rowCombo = partRow.rowCombo;
		rowCombo.select(partTypeIndex);

		onSelectPart(rowCombo, partRow.rowWidgets);

		return partTypeIndex;
	}

	public void tileEvent(final TileEventId tileEventId, final Tile tile) {

		// check if logging is enable
		if (_isEnableTileImageLogging == false) {
			return;
		}

		Runnable infoRunnable;

		LOG_LOCK.lock();
		try {

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

			infoRunnable = new Runnable() {

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

					displayLogEntries(_logEntries, _cboTileImageLog);

					// select new entry
					_cboTileImageLog.select(_cboTileImageLog.getItemCount() - 1);
				}
			};
		} finally {
			LOG_LOCK.unlock();
		}

		_display.asyncExec(infoRunnable);
	}

	/**
	 * ensure the map is using the correct zoom levels from the tile factory
	 */
	private void updateMapZoomLevel(final MP mp) {

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

	private void updateModelFromUI() {

		updateModelUrlParts();

		/*
		 * !!!! zoom level must be set before any other map methods are called because it
		 * initialized the map with new zoom levels !!!
		 */
		final int oldZoomLevel = _map.getZoom();
		final GeoPosition mapCenter = _map.getGeoCenter();

		final int newFactoryMinZoom = _spinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int newFactoryMaxZoom = _spinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		// set new zoom level before other map actions are done
		_mpCustom.setZoomLevel(newFactoryMinZoom, newFactoryMaxZoom);

		// ensure the zoom level is in the valid range
		if (oldZoomLevel < newFactoryMinZoom) {
			_map.setZoom(newFactoryMinZoom);
			_map.setGeoCenterPosition(mapCenter);
		}

		if (oldZoomLevel > newFactoryMaxZoom) {
			_map.setZoom(newFactoryMaxZoom);
			_map.setGeoCenterPosition(mapCenter);
		}

		/*
		 * keep position
		 */
		_mpCustom.setLastUsedZoom(_map.getZoom());
		_mpCustom.setLastUsedPosition(_map.getGeoCenter());

		if (_customUrl != null) {
			_mpCustom.setCustomUrl(_customUrl);
		}

		// update image format from the mp, the image format is set in the mp when images are loaded
		_txtImageFormat.setText(_mpCustom.getImageFormat());
	}

	/**
	 * Save all url parts from the UI into the map provider
	 */
	private void updateModelUrlParts() {

		final ArrayList<UrlPart> urlParts = new ArrayList<UrlPart>();
		int rowIndex = 0;

		for (final PartRow row : PART_ROWS) {

			final HashMap<WIDGET_KEY, Widget> rowWidgets = row.rowWidgets;
			final PartUIItem partItem = PART_ITEMS.get(row.rowCombo.getSelectionIndex());

			// skip empty parts
			if (partItem.partKey == PART_TYPE.NONE) {
				continue;
			}

			final UrlPart urlPart = new UrlPart();
			urlParts.add(urlPart);

			urlPart.setPosition(rowIndex);

			switch (partItem.partKey) {

			case HTML:

				final Text txtHtml = (Text) rowWidgets.get(WIDGET_KEY.TEXT_HTML);

				urlPart.setPartType(PART_TYPE.HTML);
				urlPart.setHtml(txtHtml.getText());

				break;

			case RANDOM_INTEGER:

				final Spinner spinnerStart = (Spinner) rowWidgets.get(WIDGET_KEY.SPINNER_RANDOM_START);
				final Spinner spinnerEnd = (Spinner) rowWidgets.get(WIDGET_KEY.SPINNER_RANDOM_END);

				urlPart.setPartType(PART_TYPE.RANDOM_INTEGER);
				urlPart.setRandomIntegerStart(spinnerStart.getSelection());
				urlPart.setRandomIntegerEnd(spinnerEnd.getSelection());

				break;

			case X:

				urlPart.setPartType(PART_TYPE.X);

				break;

			case Y:

				urlPart.setPartType(PART_TYPE.Y);

				break;

//			case LAT_TOP:
//				urlPart.setPartType(PART_TYPE.LAT_TOP);
//				break;
//
//			case LAT_BOTTOM:
//				urlPart.setPartType(PART_TYPE.LAT_BOTTOM);
//				break;
//
//			case LON_LEFT:
//				urlPart.setPartType(PART_TYPE.LON_LEFT);
//				break;
//
//			case LON_RIGHT:
//				urlPart.setPartType(PART_TYPE.LON_RIGHT);
//				break;

			case ZOOM:
				urlPart.setPartType(PART_TYPE.ZOOM);
				break;

			default:
				break;
			}

			rowIndex++;
		}

		_mpCustom.setUrlParts(urlParts);
	}

	/**
	 * update custom url from url parts
	 */
	private void updateUICustomUrl() {

		final StringBuilder sb = new StringBuilder();

		for (final PartRow row : PART_ROWS) {

			final HashMap<WIDGET_KEY, Widget> rowWidgets = row.rowWidgets;
			final PartUIItem selectedParaItem = PART_ITEMS.get(row.rowCombo.getSelectionIndex());

			switch (selectedParaItem.partKey) {

			case HTML:

				final Text txtHtml = (Text) rowWidgets.get(WIDGET_KEY.TEXT_HTML);
				sb.append(txtHtml.getText());

				break;

			case RANDOM_INTEGER:

				final Spinner fromSpinner = (Spinner) rowWidgets.get(WIDGET_KEY.SPINNER_RANDOM_START);
				final Spinner toSpinner = (Spinner) rowWidgets.get(WIDGET_KEY.SPINNER_RANDOM_END);

				final int fromValue = fromSpinner.getSelection();
				final int toValue = toSpinner.getSelection();

				sb.append(PARAMETER_LEADING_CHAR);
				sb.append(Integer.toString(fromValue));
				sb.append("..."); //$NON-NLS-1$ 
				sb.append(Integer.toString(toValue));
				sb.append(PARAMETER_TRAILING_CHAR);

				break;

			case X:
				sb.append(createUI214Parameter(PART_TYPE.X));
				break;

			case Y:
				sb.append(createUI214Parameter(PART_TYPE.Y));
				break;

			case ZOOM:
				sb.append(createUI214Parameter(PART_TYPE.ZOOM));
				break;

			default:
				break;
			}
		}

		_btnShowMap.setEnabled(sb.length() > 5);
		_customUrl = sb.toString();

		_txtCustomUrl.setText(_customUrl);
	}

	/**
	 * display all url parts
	 */
	private void updateUIUrlParts() {

		int rowIndex = 0;
		final ArrayList<UrlPart> urlParts = _mpCustom.getUrlParts();

		for (final UrlPart urlPart : urlParts) {

			// check bounds
			if (rowIndex >= PART_ROWS.size()) {
				StatusUtil.log("there are too few part rows", new Exception()); //$NON-NLS-1$
				break;
			}

			final PartRow partRow = PART_ROWS.get(rowIndex++);
			final PART_TYPE partType = urlPart.getPartType();

			// display part widget (page/input widget)
			selectPartType(partRow, partType);

			// display part content
			switch (partType) {

			case HTML:

				final Text txtHtml = (Text) partRow.rowWidgets.get(WIDGET_KEY.TEXT_HTML);
				txtHtml.setText(urlPart.getHtml());

				break;

			case RANDOM_INTEGER:

				final Spinner txtRandomStart = (Spinner) partRow.rowWidgets.get(WIDGET_KEY.SPINNER_RANDOM_START);
				final Spinner txtRandomEnd = (Spinner) partRow.rowWidgets.get(WIDGET_KEY.SPINNER_RANDOM_END);

				txtRandomStart.setSelection(urlPart.getRandomIntegerStart());
				txtRandomEnd.setSelection(urlPart.getRandomIntegerEnd());

				break;

			default:
				break;
			}
		}

		// hide part rows which are not used
		rowIndex = rowIndex < 0 ? 0 : rowIndex;
		for (int partRowIndex = rowIndex; partRowIndex < PART_ROWS.size(); partRowIndex++) {
			final PartRow partRow = PART_ROWS.get(partRowIndex);
			selectPartType(partRow, PART_TYPE.NONE);
		}
	}
}
