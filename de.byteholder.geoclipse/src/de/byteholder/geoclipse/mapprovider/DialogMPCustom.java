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
	private Display							fDisplay;

	private Button							fBtnOk;
	private Composite						fLeftContainer;

	private ViewerDetailForm				fDetailForm;
	private ToolBar							fToolbar;

	private Label							fLblMapInfo;

	private Label							fLblTileInfo;
	private Button							fChkShowTileInfo;
	private Text							fTxtExampleUrl;
	private Text							fTxtCustomUrl;
	private Combo							fCboTileImageLog;
	private Button							fBtnShowMap;

	private Spinner							fSpinMinZoom;
	private Spinner							fSpinMaxZoom;
	private Button							fBtnShowOsmMap;
	private Composite						fPartContainer;
	private Button							fChkShowTileImageLog;

	private NumberFormat					fNfLatLon								= NumberFormat.getNumberInstance();

	{
		// initialize lat/lon formatter
		fNfLatLon.setMinimumFractionDigits(6);
		fNfLatLon.setMaximumFractionDigits(6);
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

	private IDialogSettings					fDialogSettings;

	private PrefPageMapProviders			fPrefPageMapFactory;

	private String							fDefaultMessage;

	/**
	 * contains the custom url with all url parts which are converted to a string
	 */
	private String							fCustomUrl;
	private String							fPreviousCustomUrl;

	private MPCustom						fMpCustom;
	private MP								fDefaultMapProvider;

	private boolean							fIsInitUI								= false;

	private boolean							fIsEnableTileImageLogging;
	private ConcurrentLinkedQueue<LogEntry>	fLogEntries								= new ConcurrentLinkedQueue<LogEntry>();

	private int								fStatUpdateCounter						= 0;
	private int								fStatIsQueued;
	private int								fStatStartLoading;
	private int								fStatEndLoading;
	private int								fStatErrorLoading;

	private int								fPreviousMinZoom;
	private int								fPreviousMaxZoom;

	private Label							fLblLog;
	private Text							fTxtImageFormat;

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

		fDialogSettings = Activator.getDefault().getDialogSettingsSection("DialogCustomConfiguration");//$NON-NLS-1$

		fPrefPageMapFactory = mapFactory;
		fMpCustom = customMapProvider;

		fDefaultMapProvider = MapProviderManager.getInstance().getDefaultMapProvider();
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

		fDisplay = Display.getCurrent();

		setTitle(Messages.Dialog_CustomConfig_DialogArea_Title);

		MP.addTileListener(this);

		restoreState();

		// initialize after the shell size is set
		initializeUIFromModel(fMpCustom);

		enableControls();
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
			fLeftContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 5, 0, 0).applyTo(fLeftContainer);
			createUI200LeftPart(fLeftContainer, pixelConverter);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);
			UI.addSashColorHandler(sash);

			// right part (map)
			final Composite mapContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(mapContainer);
			createUI300Map(mapContainer, pixelConverter);

			fDetailForm = new ViewerDetailForm(container, fLeftContainer, sash, mapContainer, 30);
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
			fPartContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).span(1, 1).applyTo(fPartContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(fPartContainer);
			{
				PART_ROWS.add(createUI210PartRow(fPartContainer, 0, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 1, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 2, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 3, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 4, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 5, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 6, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 7, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 8, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 9, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 10, pixelConverter));
				PART_ROWS.add(createUI210PartRow(fPartContainer, 11, pixelConverter));
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

				if (fIsInitUI) {
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
						if (fIsInitUI) {
							return;
						}
						onSelectRandomSpinnerMin(paraWidgets);
					}
				});
				fromSpinner.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (fIsInitUI) {
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
						if (fIsInitUI) {
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
				if (event.widget == fSpinMinZoom) {
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
			fTxtImageFormat = new Text(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtImageFormat);
			fTxtImageFormat.setToolTipText(Messages.Dialog_CustomConfig_Text_ImageFormat_Tooltip);

			// ################################################

			// label: min zoomlevel
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dialog_CustomConfig_Label_ZoomLevel);

			// container: zoom
			final Composite zoomContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(zoomContainer);
			{
				// spinner: min zoom level
				fSpinMinZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(fSpinMinZoom);
				fSpinMinZoom.setMinimum(UI_MIN_ZOOM_LEVEL);
				fSpinMinZoom.setMaximum(UI_MAX_ZOOM_LEVEL);
				fSpinMinZoom.setSelection(1);
				fSpinMinZoom.addMouseWheelListener(mouseWheelListener);
				fSpinMinZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (fIsInitUI) {
							return;
						}
						onSelectZoomSpinnerMin();
					}
				});
				fSpinMinZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (fIsInitUI) {
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
				fSpinMaxZoom = new Spinner(zoomContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.CENTER).applyTo(fSpinMaxZoom);
				fSpinMaxZoom.setMinimum(UI_MIN_ZOOM_LEVEL);
				fSpinMaxZoom.setMaximum(UI_MAX_ZOOM_LEVEL);
				fSpinMaxZoom.setSelection(1);
				fSpinMaxZoom.addMouseWheelListener(mouseWheelListener);
				fSpinMaxZoom.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (fIsInitUI) {
							return;
						}
						onSelectZoomSpinnerMax();
					}
				});
				fSpinMaxZoom.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						if (fIsInitUI) {
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
			fChkShowTileInfo = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//

					// display at the bottom of the dialog
					.grab(false, true)
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

	private void createUI300Map(final Composite parent, final PixelConverter pixelConverter) {

		final Composite toolbarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbarContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(toolbarContainer);
		{
			// button: update map
			fBtnShowMap = new Button(toolbarContainer, SWT.NONE);
			fBtnShowMap.setText(Messages.Dialog_CustomConfig_Button_UpdateMap);
			fBtnShowMap.setToolTipText(Messages.Dialog_CustomConfig_Button_UpdateMap_Tooltip);
			fBtnShowMap.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectCustomMap();
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
			fTxtCustomUrl = new Text(container, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtCustomUrl);
			fTxtCustomUrl.setToolTipText(Messages.Dialog_MapConfig_Label_CustomUrl_Tooltip);

			// ############################################################

			// label: example url
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Dialog_MapConfig_Label_ExampleUrl);
			label.setToolTipText(Messages.Dialog_MapConfig_Label_ExampleUrl_Tooltip);

			// text: custom url
			fTxtExampleUrl = new Text(container, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtExampleUrl);
			fTxtExampleUrl.setToolTipText(Messages.Dialog_MapConfig_Label_ExampleUrl_Tooltip);

			// ############################################################

			// label: url log
			fLblLog = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(fLblLog);
			fLblLog.setText(Messages.Dialog_MapConfig_Label_LoadedImageUrl);
			fLblLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);

			// combo: url log
			fCboTileImageLog = new Combo(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fCboTileImageLog);
			fCboTileImageLog.setToolTipText(Messages.Dialog_MapConfig_Button_ShowTileLog_Tooltip);
			fCboTileImageLog.setVisibleItemCount(40);
			fCboTileImageLog.setFont(getMonoFont());
		}
	}

	private void enableControls() {

		fIsEnableTileImageLogging = fChkShowTileImageLog.getSelection();

		if (fIsEnableTileImageLogging == false) {
			// remove old log entries
			fStatUpdateCounter = 0;
			fCboTileImageLog.removeAll();
		}

		fLblLog.setEnabled(fIsEnableTileImageLogging);
		fCboTileImageLog.setEnabled(fIsEnableTileImageLogging);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;

		// disable bounds
//		return null;
	}

	private void initializeUIFromModel(final MPCustom mp) {

		fMpCustom = mp;

		fIsInitUI = true;
		{
			updateUIUrlParts();
			updateUICustomUrl();

			/*
			 * set zoom level
			 */
			final int minZoomLevel = fMpCustom.getMinZoomLevel();
			final int maxZoomLevel = fMpCustom.getMaxZoomLevel();
			fSpinMinZoom.setSelection(minZoomLevel + UI_MIN_ZOOM_LEVEL);
			fSpinMaxZoom.setSelection(maxZoomLevel + UI_MIN_ZOOM_LEVEL);

			fPreviousCustomUrl = mp.getCustomUrl();
			fPreviousMinZoom = minZoomLevel;
			fPreviousMaxZoom = maxZoomLevel;

			fTxtImageFormat.setText(fMpCustom.getImageFormat());
		}
		fIsInitUI = false;

		// show map provider in the message area
		fDefaultMessage = NLS.bind(Messages.Dialog_MapConfig_DialogArea_Message, fMpCustom.getName());
		setMessage(fDefaultMessage);

		// set factory and display map
		fMap.setMapProviderWithReset(mp, true);
 
		// set position to previous position
		fMap.setZoom(fMpCustom.getLastUsedZoom());
		fMap.setGeoCenterPosition(fMpCustom.getLastUsedPosition());
	}

	@Override
	protected void okPressed() {

		// model is saved in the dialog opening code
		updateModelFromUI();

		super.okPressed();
	}

	private void onSelectCustomMap() {

		final int minZoom = fSpinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int maxZoom = fSpinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		// check if the custom url or zoom level has changed
		if (fCustomUrl.equals(fPreviousCustomUrl) && fPreviousMinZoom == minZoom && fPreviousMaxZoom == maxZoom) {
			// do nothing to optimize performace
			return;
		}

		// keep values for the next check
		fPreviousCustomUrl = fCustomUrl;
		fPreviousMinZoom = minZoom;
		fPreviousMaxZoom = maxZoom;

		updateModelFromUI();

		// reset all images
		fMpCustom.resetAll(false);

		// delete offline images to force the reload and to test the modified url
		fPrefPageMapFactory.deleteOfflineMap(fMpCustom);

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * <br>
		 * ensure the map is using the correct zoom levels before other map actions are done<br>
		 * <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 */
		updateMapZoomLevel(fMpCustom);

		fMap.setMapProviderWithReset(fMpCustom, true);
	}

	private void onSelectOsmMap() {

		if (fMap.getMapProvider() == fDefaultMapProvider) {

			// toggle map

			// update layers BEFORE the tile factory is set in the map
			updateModelFromUI();

			updateMapZoomLevel(fMpCustom);

			fMap.setMapProviderWithReset(fMpCustom, true);

		} else {

			// display OSM

			// ensure the map is using the correct zoom levels
			updateMapZoomLevel(fDefaultMapProvider);

			fDefaultMapProvider.setStateToReloadOfflineCounter();

			fMap.setMapProviderWithReset(fDefaultMapProvider, true);
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

		fPartContainer.layout();

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

		final int mapMinValue = fSpinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = fSpinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		if (mapMaxValue > MAP_MAX_ZOOM_LEVEL) {
			fSpinMaxZoom.setSelection(UI_MAX_ZOOM_LEVEL);
		}

		if (mapMaxValue < mapMinValue) {
			fSpinMinZoom.setSelection(mapMinValue + 1);
		}
	}

	private void onSelectZoomSpinnerMin() {

		final int mapMinValue = fSpinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int mapMaxValue = fSpinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		if (mapMinValue > mapMaxValue) {
			fSpinMinZoom.setSelection(mapMaxValue + 1);
		}
	}

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			fDetailForm.setViewerWidth(fDialogSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}

		final String exampleUrl = fDialogSettings.get(DIALOG_SETTINGS_EXAMPLE_URL);
		if (exampleUrl == null) {
			fTxtExampleUrl.setText(DEFAULT_EXAMPLE);
		} else {
			fTxtExampleUrl.setText(exampleUrl);
		}

		// debug tile info
		final boolean isShowDebugInfo = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_INFO);
		fChkShowTileInfo.setSelection(isShowDebugInfo);
		fMap.setShowDebugInfo(isShowDebugInfo);

		// tile image loading
		final boolean isShowImageLogging = fDialogSettings.getBoolean(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG);
		fChkShowTileImageLog.setSelection(isShowImageLogging);
	}

	private void saveState() {

		fDialogSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, fLeftContainer.getSize().x);
		fDialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_INFO, fChkShowTileInfo.getSelection());
		fDialogSettings.put(DIALOG_SETTINGS_IS_SHOW_TILE_IMAGE_LOG, fChkShowTileImageLog.getSelection());

		final String exampleUrl = fTxtExampleUrl.getText();
		if (exampleUrl.length() > 0) {
			fDialogSettings.put(DIALOG_SETTINGS_EXAMPLE_URL, exampleUrl);
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
		if (fIsEnableTileImageLogging == false) {
			return;
		}

		Runnable infoRunnable;

		LOG_LOCK.lock();
		try {

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

			infoRunnable = new Runnable() {

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

					displayLogEntries(fLogEntries, fCboTileImageLog);

					// select new entry
					fCboTileImageLog.select(fCboTileImageLog.getItemCount() - 1);
				}
			};
		} finally {
			LOG_LOCK.unlock();
		}

		fDisplay.asyncExec(infoRunnable);
	}

	/**
	 * ensure the map is using the correct zoom levels from the tile factory
	 */
	private void updateMapZoomLevel(final MP mp) {

		final int factoryMinZoom = mp.getMinimumZoomLevel();
		final int factoryMaxZoom = mp.getMaximumZoomLevel();

		final int mapZoom = fMap.getZoom();
		final GeoPosition mapCenter = fMap.getGeoCenter();

		if (mapZoom < factoryMinZoom) {
			fMap.setZoom(factoryMinZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}

		if (mapZoom > factoryMaxZoom) {
			fMap.setZoom(factoryMaxZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}
	}

	private void updateModelFromUI() {

		updateModelUrlParts();

		/*
		 * !!!! zoom level must be set before any other map methods are called because it
		 * initialized the map with new zoom levels !!!
		 */
		final int oldZoomLevel = fMap.getZoom();
		final GeoPosition mapCenter = fMap.getGeoCenter();

		final int newFactoryMinZoom = fSpinMinZoom.getSelection() - UI_MIN_ZOOM_LEVEL;
		final int newFactoryMaxZoom = fSpinMaxZoom.getSelection() - UI_MIN_ZOOM_LEVEL;

		// set new zoom level before other map actions are done
		fMpCustom.setZoomLevel(newFactoryMinZoom, newFactoryMaxZoom);

		// ensure the zoom level is in the valid range
		if (oldZoomLevel < newFactoryMinZoom) {
			fMap.setZoom(newFactoryMinZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}

		if (oldZoomLevel > newFactoryMaxZoom) {
			fMap.setZoom(newFactoryMaxZoom);
			fMap.setGeoCenterPosition(mapCenter);
		}

		/*
		 * keep position
		 */
		fMpCustom.setLastUsedZoom(fMap.getZoom());
		fMpCustom.setLastUsedPosition(fMap.getGeoCenter());

		if (fCustomUrl != null) {
			fMpCustom.setCustomUrl(fCustomUrl);
		}

		// update image format from the mp, the image format is set in the mp when images are loaded
		fTxtImageFormat.setText(fMpCustom.getImageFormat());
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

		fMpCustom.setUrlParts(urlParts);
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

		fBtnShowMap.setEnabled(sb.length() > 5);
		fCustomUrl = sb.toString();

		fTxtCustomUrl.setText(fCustomUrl);
	}

	/**
	 * display all url parts
	 */
	private void updateUIUrlParts() {

		int rowIndex = 0;
		final ArrayList<UrlPart> urlParts = fMpCustom.getUrlParts();

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
