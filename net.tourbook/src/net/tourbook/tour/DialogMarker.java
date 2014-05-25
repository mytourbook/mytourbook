/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.Hack;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.form.ViewerDetailForm;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourSign;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoUI;
import net.tourbook.preferences.PrefPageSigns;
import net.tourbook.sign.SignManager;
import net.tourbook.sign.SignMenuManager;
import net.tourbook.ui.tourChart.ChartLabel;
import net.tourbook.ui.tourChart.ITourMarkerSelectionListener;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class DialogMarker extends TitleAreaDialog implements ITourMarkerSelectionListener {

	private static final String			DIALOG_SETTINGS_POSITION			= "marker_position";						//$NON-NLS-1$
	private static final String			DIALOG_SETTINGS_VIEWER_WIDTH		= "viewer_width";							//$NON-NLS-1$
	private static final String			STATE_IMAGE_COLUMN_WIDTH			= "STATE_IMAGE_COLUMN_WIDTH";				//$NON-NLS-1$
	private static final String			STATE_MARKER_CONTAINER_SASH_WEIGHTS	= "MarkerContainerSashWeights";			//$NON-NLS-1$

	private static final int			OFFSET_PAGE_INCREMENT				= 20;
	private static final int			OFFSET_MAX							= 200;

	private int							IMAGE_DEFAULT_WIDTH;
	private int							IMAGE_MIN_WIDTH;
	private int							ROW_DEFAULT_HEIGHT;
	private int							ROW_MAX_HEIGHT;
	private int							CONTENT_DEFAULT_WIDTH;

	private final IDialogSettings		_state								= TourbookPlugin.getState("DialogMarker");	//$NON-NLS-1$

	private TourChart					_tourChart;
	private TourData					_tourData;

	/**
	 * marker which is currently selected
	 */
	private TourMarker					_selectedTourMarker;

	/**
	 * backup for the selected tour marker
	 */
	private TourMarker					_backupMarker						= new TourMarker();

	private Set<TourMarker>				_tourMarkers;
	private HashSet<TourMarker>			_tourMarkersBackup;

	/**
	 * initial tour marker
	 */
	private TourMarker					_initialTourMarker;

	private ModifyListener				_defaultModifyListener;
	private MouseWheelListener			_defaultMouseWheelListener;
	private SelectionAdapter			_defaultSelectionAdapter;

	private SignMenuManager				_signMenuManager					= new SignMenuManager();
	private ActionOpenPrefDialog		_actionOpenTourSignPrefs;

	private boolean						_isOkPressed						= false;
	private boolean						_isUpdateUI;
	private boolean						_isSetXSlider						= true;

	private NumberFormat				_nf3								= NumberFormat.getNumberInstance();

	private int							_signImageColumn;
	private int							_imageColumnWidth;

	/**
	 * Contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width.
	 */
	private final ArrayList<Control>	_firstColumnControls				= new ArrayList<Control>();

	/*
	 * none UI
	 */
	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private ViewerDetailForm			_viewerDetailForm;
	private Composite					_markerContainer;
	private SashForm					_markerContainerSashForm;

	private TableViewer					_markerViewer;

	/**
	 * Contains the table column widget for the sign image.
	 */
	private TableColumn					_tcSignImage;

	private Button						_btnDelete;
	private Button						_btnHideAll;
	private Button						_btnShowAll;
	private Button						_btnUndo;
	private Button						_chkVisibility;

	private Combo						_comboLabelPosition;
	private Combo						_comboMarkerName;
	private Combo						_comboSignPosition;

	private Group						_groupLabel;
	private Group						_groupSign;

	private ImageCanvas					_imgTourSign;

	private Label						_lblDescription;
	private Label						_lblLabel;
	private Label						_lblLabelOffsetX;
	private Label						_lblLabelOffsetY;
	private Label						_lblLabelPosition;
	private Label						_lblSign;
	private Label						_lblSignName;
	private Label						_lblSignOffsetX;
	private Label						_lblSignOffsetY;
	private Label						_lblSignPosition;

	private Spinner						_spinLabelOffsetX;
	private Spinner						_spinLabelOffsetY;
	private Spinner						_spinSignOffsetX;
	private Spinner						_spinSignOffsetY;

	private Text						_txtDescription;

	{
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);

		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeMarkerUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeMarkerUI();
			}
		};

		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onChangeMarkerUI();
			}
		};
	}

	private class LoadImageCallbackSelectedMarker implements ILoadCallBack {

		private TourSign	__tourSign;

		public LoadImageCallbackSelectedMarker(final TourSign tourSign) {
			__tourSign = tourSign;
		}

		@Override
		public void callBackImageIsLoaded(final boolean isImageLoaded) {

			if (isImageLoaded == false) {
				return;
			}

			// run in UI thread
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {

					if (getShell().isDisposed()) {
						return;
					}

					final Image signImage = SignManager.getPhotoImage(__tourSign.getSignImagePhoto());

					if (signImage != null) {

						// update UI
						_imgTourSign.setImage(signImage, false);

						redrawViewer();
//						_tourChart.updateLayerMarker(true);
					}
				}
			});

		}
	}

	private class LoadImageCallbackViewer implements ILoadCallBack {

		public LoadImageCallbackViewer(final TourSign tourSign) {}

		@Override
		public void callBackImageIsLoaded(final boolean isImageLoaded) {

			if (isImageLoaded == false) {
				return;
			}

			// run in UI thread
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {

					if (getShell().isDisposed()) {
						return;
					}

					// update sign image

					redrawViewer();
				}
			});
		}
	}

	private final class MarkerEditingSupport extends EditingSupport {

		private final CheckboxCellEditor	_cellEditor;

		private MarkerEditingSupport(final TableViewer tableViewer) {

			super(tableViewer);

			_cellEditor = new CheckboxCellEditor(tableViewer.getTable());
		}

		@Override
		protected boolean canEdit(final Object element) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return _cellEditor;
		}

		@Override
		protected Object getValue(final Object element) {

			if (element instanceof TourMarker) {

				final TourMarker tourMarker = (TourMarker) element;

				return tourMarker.isMarkerVisible() ? Boolean.TRUE : Boolean.FALSE;
			}

			return Boolean.FALSE;
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (element instanceof TourMarker) {

				final TourMarker tourMarker = (TourMarker) element;
				final Boolean isVisible = (Boolean) value;

				updateUI_TourMarker(tourMarker, isVisible);
			}
		}
	}

	private class MarkerViewerContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			if (_tourData == null) {
				return new Object[0];
			} else {
				return _tourMarkers.toArray();
			}
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Sort the markers by time
	 */
	private static class MarkerViewerSorter extends ViewerSorter {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
//			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
// time is disabled because it's not always available in gpx files
			return ((TourMarker) (obj1)).getSerieIndex() - ((TourMarker) (obj2)).getSerieIndex();
		}
	}

	/**
	 * @param parentShell
	 * @param tourData
	 * @param initialTourMarker
	 *            TourMarker which is selected when the dialog is opened
	 */
	public DialogMarker(final Shell parentShell, final TourData tourData, final TourMarker initialTourMarker) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window
		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker).createImage());

		_tourData = tourData;
		_tourMarkers = _tourData.getTourMarkers();

		/*
		 * make a backup copy of the tour markers, modify the original data so that the tour chart
		 * displays the modifications
		 */
		_tourMarkersBackup = new HashSet<TourMarker>();

		for (final TourMarker tourMarker : _tourMarkers) {
			_tourMarkersBackup.add(tourMarker.clone());
		}

		_initialTourMarker = initialTourMarker;
	}

	/**
	 * remove selected markers from the view and update dependened structures
	 */
	private void actionDeleteMarker() {

		final IStructuredSelection markerSelection = (IStructuredSelection) _markerViewer.getSelection();
		final TourMarker selectedMarker = (TourMarker) markerSelection.getFirstElement();

		// confirm to save the changes
		final MessageBox msgBox = new MessageBox(_tourChart.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		msgBox.setText(Messages.Dlg_TourMarker_MsgBox_delete_marker_title);
		msgBox.setMessage(NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (selectedMarker).getLabel()));

		if (msgBox.open() == SWT.YES) {

			// get index for selected marker
			final int lastMarkerIndex = _markerViewer.getTable().getSelectionIndex();

			// update data model
			_tourMarkers.remove(selectedMarker);

			// update the viewer
			_markerViewer.remove(selectedMarker);

			// update chart
			_tourChart.updateUI_LayerMarker(true);

			// select next marker
			TourMarker nextMarker = (TourMarker) _markerViewer.getElementAt(lastMarkerIndex);
			if (nextMarker == null) {
				nextMarker = (TourMarker) _markerViewer.getElementAt(lastMarkerIndex - 1);
			}

			_selectedTourMarker = null;

			if (nextMarker == null) {
				// disable controls when no marker is available
				enableControls();
			} else {
				_markerViewer.setSelection(new StructuredSelection(nextMarker), true);
			}

			_markerViewer.getTable().setFocus();
		}
	}

	public void actionRemoveTourSign() {

		// update model
		_selectedTourMarker.setSign(null);

		// update UI
		_lblSignName.setText(UI.EMPTY_STRING);
		_imgTourSign.setImage(null);

		redrawViewer();
	}

	public void actionSetTourSign(final TourSign tourSign) {

		_selectedTourMarker.setSign(tourSign);

		updateUI_TourSign(_selectedTourMarker);

		redrawViewer();

//		_tourChart.updateLayerMarker(true);

		onChangeMarkerUI();
	}

	private void actionShowHideAll(final boolean isVisible) {

		for (final TourMarker tourMarker : _tourMarkers) {
			tourMarker.setMarkerVisible(isVisible);
		}

		/*
		 * Update UI
		 */
		// controls
		updateUI_FromModel();

		// viewer+chart
		final TourMarker[] allTourMarker = _tourMarkers.toArray(new TourMarker[_tourMarkers.size()]);
		_markerViewer.update(allTourMarker, null);
		_tourChart.updateUI_LayerMarker(true);

		enableControls();
	}

	public void addTourMarker(final TourMarker newTourMarker) {

		if (newTourMarker == null) {
			return;
		}

		// update data model, add new marker to the marker list
		_tourMarkers.add(newTourMarker);

		// update the viewer and select the new marker
		_markerViewer.refresh();
		_markerViewer.setSelection(new StructuredSelection(newTourMarker), true);

		_comboMarkerName.setFocus();

		// update chart
		_tourChart.updateUI_LayerMarker(true);
	}

	@Override
	public boolean close() {

		if (_isOkPressed) {

			/*
			 * the markers are already set into the tour data because the original values are
			 * modified
			 */

			restoreState_VisibleType();

		} else {

			/*
			 * when OK is not pressed, revert tour markers, this happens when the Cancel button is
			 * pressed or when the window is closed
			 */
			_tourData.setTourMarkers(_tourMarkersBackup);
		}

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dlg_TourMarker_Dlg_title);

		shell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				dispose();
			}
		});
	}

	private void createActions() {

		_actionOpenTourSignPrefs = new ActionOpenPrefDialog(
				Messages.Dlg_TourMarker_Action_SignPreferences,
				PrefPageSigns.ID);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		final String okText = net.tourbook.ui.UI.convertOKtoSaveUpdateButton(_tourData);

		getButton(IDialogConstants.OK_ID).setText(okText);
	}

	private void createContextMenu(final Control control) {

		// link menu
		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				_signMenuManager.fillSignMenu(menuMgr, DialogMarker.this);

				menuMgr.add(new Separator());
				menuMgr.add(_actionOpenTourSignPrefs);
			}
		});

		// set context menu for the link
		final Menu signContextMenu = menuMgr.createContextMenu(control);
		control.setMenu(signContextMenu);

	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createActions();

		initUI(parent);

		createUI(dlgContainer);

		setTitle(Messages.Dlg_TourMarker_Dlg_title);
		setMessage(Messages.Dlg_TourMarker_Dlg_Message);

		fillUI();

		// update marker viewer
		_markerViewer.setInput(this);

		if (_initialTourMarker == null) {
			// select first marker if any are available
			final Object firstElement = _markerViewer.getElementAt(0);
			if (firstElement != null) {
				_markerViewer.setSelection(new StructuredSelection(firstElement), true);
			}
		} else {
			// select initial tour marker
			_markerViewer.setSelection(new StructuredSelection(_initialTourMarker), true);
		}

		_comboMarkerName.setFocus();

		enableControls();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.hint(_pc.convertWidthInCharsToPixels(150), _pc.convertHeightInCharsToPixels(40))
				.applyTo(shellContainer);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite dlgContainer = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(dlgContainer);
			GridLayoutFactory.swtDefaults().applyTo(dlgContainer);
			{
				// left part
				_markerContainer = createUI_10_LeftPart(dlgContainer);

				// sash
				final Sash sash = new Sash(dlgContainer, SWT.VERTICAL);
				UI.addSashColorHandler(sash);

				// right part
				final Composite chartContainer = createUI_20_RightPart(dlgContainer);

				_viewerDetailForm = new ViewerDetailForm(//
						dlgContainer,
						_markerContainer,
						sash,
						chartContainer,
						50);
			}

			createUI_90_MarkerActions(shellContainer);

			/*
			 * !!! UI must be restored and column image size must be set before columns are
			 * equalized, otherwise the column height is wrong !!!
			 */
			restoreState();

			// set column image (row) height
			onResizeImageColumn();

			// compute width for all controls and equalize column width for the different sections
			dlgContainer.layout(true, true);
			UI.setEqualizeColumWidths(_firstColumnControls, _pc.convertWidthInCharsToPixels(2));
		}
	}

	private Composite createUI_10_LeftPart(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 5, 0, 0)
				.applyTo(container);
		{
			_markerContainerSashForm = new SashForm(container, SWT.SMOOTH | SWT.VERTICAL);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(_markerContainerSashForm);
			_markerContainerSashForm.setSashWidth(10);
			{
				createUI_40_MarkerList(_markerContainerSashForm);
				createUI_50_MarkerDetails(_markerContainerSashForm);
			}
		}

		return container;
	}

	private Composite createUI_20_RightPart(final Composite dlgContainer) {

		final Composite chartContainer = new Composite(dlgContainer, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(chartContainer);
		GridLayoutFactory.fillDefaults()//
				.margins(5, 0)
				.applyTo(chartContainer);
//		chartContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_80_TourChart(chartContainer);
		}

		return chartContainer;
	}

	/**
	 * container: marker list
	 */
	private void createUI_40_MarkerList(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.indent(0, 4)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			// label: markers
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Dlg_TourMarker_Label_markers);

			createUI_42_MarkerViewer(container);
		}
	}

	private Composite createUI_42_MarkerViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, //
				SWT.FULL_SELECTION //
//						| SWT.BORDER
						| SWT.CHECK);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
//		table.setLinesVisible(true);

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				if (e.character == ' ') {
					toggleMarkerVisibility();
				}
			}
		});

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				if (event.index == _signImageColumn //
						&& (event.type == SWT.MeasureItem || event.type == SWT.PaintItem)) {

					onViewerPaint(event);
				}
			}
		};
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem, paintListener);

		ROW_DEFAULT_HEIGHT = table.getItemHeight();

		_markerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		defineColumn_1stHidden(tableLayout);//				// 0
		defineColumn_Distance(tableLayout);//				// 1
		defineColumn_IsVisible(tableLayout);//				// 2
		_tcSignImage = defineColumn_Image(tableLayout);//	// 3
		defineColumn_Marker(tableLayout);//					// 4
		defineColumn_Description(tableLayout);//			// 5
		defineColumn_OffsetX(tableLayout);//				// 6
		defineColumn_OffsetY(tableLayout);//				// 7

		_signImageColumn = 3;

		/*
		 * create table viewer
		 */
		_markerViewer.setContentProvider(new MarkerViewerContentProvider());
		_markerViewer.setSorter(new MarkerViewerSorter());

		_markerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					onSelectMarker((TourMarker) selection.getFirstElement());
				}
			}
		});

		_markerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				_comboMarkerName.setFocus();
			}
		});

		return layoutContainer;
	}

	private void createUI_50_MarkerDetails(final Composite parent) {

		/*
		 * container: marker details
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * Label
			 */
			_groupLabel = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
//					.grab(true, false)
					.span(2, 1)
					.applyTo(_groupLabel);
			_groupLabel.setText(Messages.Dlg_TourMarker_Group_Label);
			GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupLabel);
			{
				createUI_52_Label_Name(_groupLabel);
				createUI_53_Label_Position(_groupLabel);
			}

			/*
			 * Sign
			 */
			_groupSign = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
//					.grab(true, false)
					.span(2, 1)
					.applyTo(_groupSign);
			_groupSign.setText(Messages.Dlg_TourMarker_Group_Sign);
			GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupSign);
			{
				createUI_56_Sign(_groupSign);
				createUI_57_Sign_Position(_groupSign);
			}

			createUI_58_Description(container);
			createUI_60_Visibility(container);
		}
	}

	private void createUI_52_Label_Name(final Composite parent) {

		/*
		 * Marker name
		 */
		{
			// Label
			_lblLabel = new Label(parent, SWT.NONE);
			_firstColumnControls.add(_lblLabel);
			_lblLabel.setText(Messages.Dlg_TourMarker_Label_Name);

			// Combo
			_comboMarkerName = new Combo(parent, SWT.BORDER | SWT.FLAT);
			GridDataFactory.fillDefaults()//
					// !!! hint must be set because the width is adjusted to the content
					.hint(CONTENT_DEFAULT_WIDTH, SWT.DEFAULT)
					.grab(true, true)
					.applyTo(_comboMarkerName);
			_comboMarkerName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent e) {
					onChangeMarkerUI();
				}
			});
		}
	}

	private void createUI_53_Label_Position(final Composite parent) {

		/*
		 * Position
		 */
		{
			// label
			_lblLabelPosition = new Label(parent, SWT.NONE);
			_firstColumnControls.add(_lblLabelPosition);
			_lblLabelPosition.setText(Messages.Dlg_TourMarker_Label_position);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				/*
				 * Combo
				 */
				{
					_comboLabelPosition = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboLabelPosition.setVisibleItemCount(20);
					_comboLabelPosition.addSelectionListener(_defaultSelectionAdapter);
				}

				/*
				 * Offset
				 */
				final Composite valueContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.FILL)
						.applyTo(valueContainer);
				GridLayoutFactory.fillDefaults().numColumns(4).applyTo(valueContainer);
//				valueContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				{
					/*
					 * Horizontal offset
					 */
					{
						// Label
						_lblLabelOffsetX = new Label(valueContainer, SWT.NONE);
						GridDataFactory.fillDefaults()//
								.align(SWT.FILL, SWT.CENTER)
//								.indent(_pc.convertWidthInCharsToPixels(2), 0)
								.applyTo(_lblLabelOffsetX);
						_lblLabelOffsetX.setText(Messages.Dlg_TourMarker_Label_OffsetHorizontal);
						_lblLabelOffsetX.setToolTipText(Messages.Tour_Marker_Column_horizontal_offset_tooltip);

						// Spinner
						_spinLabelOffsetX = new Spinner(valueContainer, SWT.BORDER);
						_spinLabelOffsetX.setMinimum(-OFFSET_MAX);
						_spinLabelOffsetX.setMaximum(OFFSET_MAX);
						_spinLabelOffsetX.setPageIncrement(OFFSET_PAGE_INCREMENT);
						_spinLabelOffsetX.addSelectionListener(_defaultSelectionAdapter);
						_spinLabelOffsetX.addMouseWheelListener(_defaultMouseWheelListener);
					}

					/*
					 * Vertical offset
					 */
					{
						// Label
						_lblLabelOffsetY = new Label(valueContainer, SWT.NONE);
						GridDataFactory.fillDefaults()//
								.align(SWT.FILL, SWT.CENTER)
								.applyTo(_lblLabelOffsetY);
						_lblLabelOffsetY.setText(Messages.Dlg_TourMarker_Label_OffsetVertical);
						_lblLabelOffsetY.setToolTipText(Messages.Tour_Marker_Column_vertical_offset_tooltip);

						// Spinner
						_spinLabelOffsetY = new Spinner(valueContainer, SWT.BORDER);
						_spinLabelOffsetY.setMinimum(-OFFSET_MAX);
						_spinLabelOffsetY.setMaximum(OFFSET_MAX);
						_spinLabelOffsetY.setPageIncrement(OFFSET_PAGE_INCREMENT);
						_spinLabelOffsetY.addSelectionListener(_defaultSelectionAdapter);
						_spinLabelOffsetY.addMouseWheelListener(_defaultMouseWheelListener);
					}
				}
			}
		}
	}

	private void createUI_56_Sign(final Composite parent) {

		/*
		 * Sign
		 */
		{
			// label
			_lblSign = new Label(parent, SWT.NONE);
			_firstColumnControls.add(_lblSign);
			_lblSign.setText(Messages.Dlg_TourMarker_Label_Sign);

			final Composite signContainer = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(signContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(signContainer);
//			signContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				/*
				 * Image: sign image
				 */
				_imgTourSign = new ImageCanvas(signContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.hint(_pc.convertWidthInCharsToPixels(15), _pc.convertHeightInCharsToPixels(4))
						.applyTo(_imgTourSign);
				_imgTourSign.setStyle(SWT.CENTER | SWT.LEAD);
				_imgTourSign.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						UI.openControlMenu(_imgTourSign);
					}
				});
				createContextMenu(_imgTourSign);

				/*
				 * Label: sign name
				 */
				_lblSignName = new Label(signContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblSignName);
			}
		}
	}

	private void createUI_57_Sign_Position(final Composite parent) {

		/*
		 * Position
		 */
		{
			// label
			_lblSignPosition = new Label(parent, SWT.NONE);
			_firstColumnControls.add(_lblSignPosition);
			_lblSignPosition.setText(Messages.Dlg_TourMarker_Label_SignPosition);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				/*
				 * Combo
				 */
				{
					_comboSignPosition = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboSignPosition.setVisibleItemCount(20);
					_comboSignPosition.addSelectionListener(_defaultSelectionAdapter);
				}

				/*
				 * Offset
				 */
				final Composite valueContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.FILL)
						.applyTo(valueContainer);
				GridLayoutFactory.fillDefaults().numColumns(4).applyTo(valueContainer);
//				valueContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				{
					/*
					 * Horizontal offset
					 */
					{
						// Label
						_lblSignOffsetX = new Label(valueContainer, SWT.NONE);
						GridDataFactory.fillDefaults()//
								.align(SWT.FILL, SWT.CENTER)
//								.indent(_pc.convertWidthInCharsToPixels(2), 0)
								.applyTo(_lblSignOffsetX);
						_lblSignOffsetX.setText(Messages.Dlg_TourMarker_Label_OffsetHorizontal);
						_lblSignOffsetX.setToolTipText(Messages.Tour_Marker_Column_horizontal_offset_tooltip);

						// Spinner
						_spinSignOffsetX = new Spinner(valueContainer, SWT.BORDER);
						_spinSignOffsetX.setMinimum(-OFFSET_MAX);
						_spinSignOffsetX.setMaximum(OFFSET_MAX);
						_spinSignOffsetX.setPageIncrement(OFFSET_PAGE_INCREMENT);
						_spinSignOffsetX.addSelectionListener(_defaultSelectionAdapter);
						_spinSignOffsetX.addMouseWheelListener(_defaultMouseWheelListener);
					}

					/*
					 * Vertical offset
					 */
					{
						// Label
						_lblSignOffsetY = new Label(valueContainer, SWT.NONE);
						GridDataFactory.fillDefaults()//
								.align(SWT.FILL, SWT.CENTER)
								.applyTo(_lblSignOffsetY);
						_lblSignOffsetY.setText(Messages.Dlg_TourMarker_Label_OffsetVertical);
						_lblSignOffsetY.setToolTipText(Messages.Tour_Marker_Column_vertical_offset_tooltip);

						// Spinner
						_spinSignOffsetY = new Spinner(valueContainer, SWT.BORDER);
						_spinSignOffsetY.setMinimum(-OFFSET_MAX);
						_spinSignOffsetY.setMaximum(OFFSET_MAX);
						_spinSignOffsetY.setPageIncrement(OFFSET_PAGE_INCREMENT);
						_spinSignOffsetY.addSelectionListener(_defaultSelectionAdapter);
						_spinSignOffsetY.addMouseWheelListener(_defaultMouseWheelListener);
					}
				}
			}
		}
	}

	private void createUI_58_Description(final Composite parent) {

		/*
		 * Description
		 */
		{
			// label
			_lblDescription = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.applyTo(_lblDescription);
			_lblDescription.setText(Messages.Dlg_TourMarker_Label_Description);

			// text
			_txtDescription = new Text(parent, SWT.BORDER //
					| SWT.WRAP
					| SWT.V_SCROLL
					| SWT.H_SCROLL);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(3))
					.applyTo(_txtDescription);
			_txtDescription.addModifyListener(_defaultModifyListener);
		}
	}

	private void createUI_60_Visibility(final Composite parent) {

		/*
		 * Visibility
		 */
		{
			_chkVisibility = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
//					.align(SWT.END, SWT.FILL)
					.applyTo(_chkVisibility);
			_chkVisibility.setText(Messages.Dlg_TourMarker_Checkbox_MarkerVisibility);
			_chkVisibility.setToolTipText(Messages.Tour_Marker_Column_IsVisible_Tooltip);
			_chkVisibility.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					toggleMarkerVisibility();
				}
			});
		}
	}

	/**
	 * create tour chart with new marker
	 */
	private void createUI_80_TourChart(final Composite parent) {

		_tourChart = new TourChart(parent, SWT.FLAT /* | SWT.BORDER */, true);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.hint(600, 350)
				.applyTo(_tourChart);
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setContextProvider(new DialogMarkerTourChartContextProvicer(this));

		_tourChart.setIsFireTourMarkerEvent(false);
		_tourChart.addTourMarkerSelectionListener(this);

		// set title
		_tourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
			}
		});

		final TourChartConfiguration chartConfig = TourManager.createDefaultTourChartConfig();
		_tourChart.updateTourChart(_tourData, chartConfig, false);
	}

	private void createUI_90_MarkerActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(0, 0, 10, 0)
				.numColumns(4)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: delete
			 */
			_btnDelete = new Button(container, SWT.NONE);
			_btnDelete.setText(Messages.Dlg_TourMarker_Button_delete);
			_btnDelete.setToolTipText(Messages.Dlg_TourMarker_Button_delete_tooltip);
			_btnDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionDeleteMarker();
				}
			});
			setButtonLayoutData(_btnDelete);

			/*
			 * button: undo
			 */
			_btnUndo = new Button(container, SWT.NONE);
			_btnUndo.getLayoutData();
			_btnUndo.setText(Messages.Dlg_TourMarker_Button_undo);
			_btnUndo.setToolTipText(Messages.Dlg_TourMarker_Button_undo_tooltip);
			_btnUndo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					_selectedTourMarker.restoreMarkerFromBackup(_backupMarker);
					updateUI_FromModel();
					onChangeMarkerUI();
				}
			});
			setButtonLayoutData(_btnUndo);

			/*
			 * button: show all
			 */
			_btnShowAll = new Button(container, SWT.NONE);
			_btnShowAll.getLayoutData();
			_btnShowAll.setText(Messages.Dlg_TourMarker_Button_ShowAllMarker);
			_btnShowAll.setToolTipText(Messages.Dlg_TourMarker_Button_ShowAllMarker_Tooltip);
			_btnShowAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionShowHideAll(true);
				}
			});
			setButtonLayoutData(_btnShowAll);

			/*
			 * button: hide all
			 */
			_btnHideAll = new Button(container, SWT.NONE);
			_btnHideAll.getLayoutData();
			_btnHideAll.setText(Messages.Dlg_TourMarker_Button_HideAllMarker);
			_btnHideAll.setToolTipText(Messages.Dlg_TourMarker_Button_HideAllMarker_Tooltip);
			_btnHideAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionShowHideAll(false);
				}
			});
			setButtonLayoutData(_btnHideAll);
		}
	}

	/**
	 * column: hidden column to show first visible column with right alignment
	 */
	private void defineColumn_1stHidden(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		final TableColumn tc = tvc.getColumn();

		tvc.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {
				cell.setText(UI.EMPTY_STRING);
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(0, false));
	}

	/**
	 * Column: Description
	 */
	private void defineColumn_Description(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.CENTER);
		final TableColumn tc = tvc.getColumn();

		tc.setText(Messages.Tour_Marker_Column_Description_ShortCut);
		tc.setToolTipText(Messages.Tour_Marker_Column_Description_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				final String description = tourMarker.getDescription();

				cell.setText(description.length() == 0 ? UI.EMPTY_STRING : UI.SYMBOL_STAR);
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(4), false));
	}

	/**
	 * column: distance km/mi
	 */
	private void defineColumn_Distance(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		final TableColumn tc = tvc.getColumn();

		tc.setText(UI.UNIT_LABEL_DISTANCE);
		tc.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				final float markerDistance = tourMarker.getDistance();

				if (markerDistance == -1) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf3.format(markerDistance / (1000 * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)));
				}

				if (tourMarker.getType() == ChartLabel.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(11), false));
	}

	/**
	 * Column: Sign image
	 * 
	 * @return
	 */
	private TableColumn defineColumn_Image(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		final TableColumn tc = tvc.getColumn();

//		tc.setText(UI.UNIT_LABEL_DISTANCE);
//		tc.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {

			/*
			 * !!! set dummy label provider, otherwise an error occures !!!
			 */
			@Override
			public void update(final ViewerCell cell) {}
		});

		tc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeImageColumn();
			}
		});

		tableLayout.setColumnData(tc, new ColumnPixelData(_imageColumnWidth, false));

		return tc;
	}

	/**
	 * column: marker
	 */
	private void defineColumn_IsVisible(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.LEAD);
		final TableColumn tc = tvc.getColumn();

		tc.setText(Messages.Tour_Marker_Column_IsVisible);
		tc.setToolTipText(Messages.Tour_Marker_Column_IsVisible_Tooltip);

		tvc.setEditingSupport(new MarkerEditingSupport(_markerViewer));

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				cell.setText(tourMarker.isMarkerVisible()
						? Messages.App_Label_BooleanYes
						: Messages.App_Label_BooleanNo);
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(8), false));
	}

	/**
	 * column: marker
	 */
	private void defineColumn_Marker(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.LEAD);
		final TableColumn tc = tvc.getColumn();

		tc.setText(Messages.Tour_Marker_Column_remark);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				cell.setText(tourMarker.getLabel());
			}
		});
		tableLayout.setColumnData(tc, new ColumnWeightData(1, true));
	}

	/**
	 * column: horizontal offset
	 */
	private void defineColumn_OffsetX(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		final TableColumn tc = tvc.getColumn();

		tc.setText(Messages.Tour_Marker_Column_horizontal_offset);
		tc.setToolTipText(Messages.Tour_Marker_Column_horizontal_offset_tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				cell.setText(Integer.toString(tourMarker.getLabelXOffset()));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(6), false));
	}

	/**
	 * column: vertical offset
	 */
	private void defineColumn_OffsetY(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		final TableColumn tc = tvc.getColumn();

		tc.setText(Messages.Tour_Marker_Column_vertical_offset);
		tc.setToolTipText(Messages.Tour_Marker_Column_vertical_offset_tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				cell.setText(Integer.toString(tourMarker.getLabelYOffset()));
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(6), false));
	}

	private void dispose() {

		_firstColumnControls.clear();
	}

	private void enableControls() {

		final boolean areMarkersAvailable = _markerViewer.getTable().getItemCount() != 0;
		final boolean isMarkerSelected = _selectedTourMarker != null;

		boolean isMarkerVisible = false;
		if (_selectedTourMarker != null) {
			isMarkerVisible = _selectedTourMarker.isMarkerVisible();
		}
		final boolean isMarkerEnabled = isMarkerVisible && areMarkersAvailable;

		if (isMarkerSelected) {
			_btnUndo.setEnabled(_selectedTourMarker.isEqual(_backupMarker, true) == false);
		} else {
			_btnUndo.setEnabled(false);
		}

		_chkVisibility.setEnabled(isMarkerSelected);

		_btnDelete.setEnabled(isMarkerSelected);
		_btnShowAll.setEnabled(areMarkersAvailable);
		_btnHideAll.setEnabled(areMarkersAvailable);

		_comboLabelPosition.setEnabled(isMarkerEnabled);
		_comboMarkerName.setEnabled(isMarkerEnabled);
		_comboSignPosition.setEnabled(isMarkerEnabled);

		// this do not work on win
		_groupLabel.setEnabled(isMarkerEnabled);
		_groupSign.setEnabled(isMarkerEnabled);

		_imgTourSign.setEnabled(isMarkerEnabled);

		_lblDescription.setEnabled(isMarkerEnabled);
		_lblLabel.setEnabled(isMarkerEnabled);
		_lblLabelOffsetX.setEnabled(isMarkerEnabled);
		_lblLabelOffsetY.setEnabled(isMarkerEnabled);
		_lblLabelPosition.setEnabled(isMarkerEnabled);
		_lblSign.setEnabled(isMarkerEnabled);
		_lblSignName.setEnabled(isMarkerEnabled);
		_lblSignOffsetX.setEnabled(isMarkerEnabled);
		_lblSignOffsetY.setEnabled(isMarkerEnabled);
		_lblSignPosition.setEnabled(isMarkerEnabled);

		_spinLabelOffsetX.setEnabled(isMarkerEnabled);
		_spinLabelOffsetY.setEnabled(isMarkerEnabled);
		_spinSignOffsetX.setEnabled(isMarkerEnabled);
		_spinSignOffsetY.setEnabled(isMarkerEnabled);

		_txtDescription.setEnabled(isMarkerEnabled);

	}

	private void fillUI() {

		/*
		 * Fill position combos
		 */
		for (final String position : TourMarker.LABEL_POSITIONS) {
			_comboLabelPosition.add(position);
		}

		for (final String position : TourMarker.SIGN_POSITIONS) {
			_comboSignPosition.add(position);
		}

		/*
		 * Marker names combo
		 */
		final TreeSet<String> dbTitles = TourDatabase.getAllTourMarkerNames();
		for (final String title : dbTitles) {
			_comboMarkerName.add(title);
		}

		new AutocompleteComboInput(_comboMarkerName);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		return _state;
//		return null;
	}

	private int getImageColumnWidth() {

		int width;

		if (_tcSignImage == null) {

			width = IMAGE_DEFAULT_WIDTH;

		} else {

			width = _tcSignImage.getWidth();

			if (width < IMAGE_MIN_WIDTH) {
				width = IMAGE_MIN_WIDTH;
			}
		}

		return width;
	}

	private int getRowHeight() {

		return Math.min(ROW_MAX_HEIGHT, Math.max(ROW_DEFAULT_HEIGHT, _imageColumnWidth));
	}

	TourChart getTourChart() {
		return _tourChart;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		IMAGE_DEFAULT_WIDTH = _pc.convertWidthInCharsToPixels(6);
		IMAGE_MIN_WIDTH = _pc.convertWidthInCharsToPixels(2);
		ROW_MAX_HEIGHT = _pc.convertVerticalDLUsToPixels(50);
		CONTENT_DEFAULT_WIDTH = _pc.convertWidthInCharsToPixels(30);

		restoreState_Viewer();
	}

	@Override
	protected void okPressed() {

		_isOkPressed = true;

		super.okPressed();
	}

	/**
	 * save marker modifications and update chart and viewer
	 */
	private void onChangeMarkerUI() {

		updateModel_FromUI(_selectedTourMarker);

		_tourChart.updateUI_LayerMarker(true);

		_markerViewer.update(_selectedTourMarker, null);

		enableControls();
	}

	private void onResizeImageColumn() {

		final int imageColumnWidth = getImageColumnWidth();

		// check if the width has changed
		if (imageColumnWidth == _imageColumnWidth) {
			return;
		}

		_imageColumnWidth = imageColumnWidth;

		// update images
		if (UI.IS_WIN) {
			Hack.setTableItemHeight(_markerViewer.getTable(), getRowHeight());
		}
	}

	private void onSelectMarker(final TourMarker newSelectedMarker) {

		if (newSelectedMarker == null) {
			return;
		}

		// save values for previous marker
		if (_selectedTourMarker != null && newSelectedMarker != _selectedTourMarker) {
			updateModel_FromUI(_selectedTourMarker);
			restoreState_VisibleType();
		}

		// set new selected marker
		_selectedTourMarker = newSelectedMarker;

		// make a backup of the marker to undo modifications
		_selectedTourMarker.setMarkerBackup(_backupMarker);

		updateUI_FromModel();
		onChangeMarkerUI();

		if (_isSetXSlider) {

			// set slider position
			_tourChart.setXSliderPosition(new SelectionChartXSliderPosition(_tourChart, newSelectedMarker
					.getSerieIndex(), SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	private void onViewerPaint(final Event event) {

		final TableItem item = (TableItem) event.item;
		final Object itemData = item.getData();
		if (itemData instanceof TourMarker) {

			final TourSign tourSign = ((TourMarker) itemData).getTourSign();

			if (tourSign != null) {

				final Photo signPhoto = tourSign.getSignImagePhoto();
				final ILoadCallBack imageLoadCallback = new LoadImageCallbackViewer(tourSign);
				final Image signImage = SignManager.getPhotoImage(signPhoto, imageLoadCallback);

				if (signImage != null) {

					final int photoPosX = event.x;
					final int photoPosY = event.y;

					switch (event.type) {
					case SWT.MeasureItem:

						if (UI.IS_WIN) {

							// this is done with Hack.setTableItemHeight()

						} else {

							event.height = getRowHeight();
						}

						break;

					case SWT.PaintItem:

						final GC gc = event.gc;

						final int imageCanvasWidth = Math.max(ROW_DEFAULT_HEIGHT, _imageColumnWidth);
						final int imageCanvasHeight = event.height;

						PhotoUI.paintPhotoImage(
								gc,
								signPhoto,
								signImage,
								photoPosX,
								photoPosY,
								imageCanvasWidth,
								imageCanvasHeight);

						break;
					}
				}
			}
		}
	}

	private void redrawViewer() {

		// !!! refresh() and update() do not repaint a loaded image but a redraw() do
		_markerViewer.getTable().redraw();
	}

	private void restoreState() {

		// restore width for the marker list
		final int leftPartWidth = Util.getStateInt(
				_state,
				DIALOG_SETTINGS_VIEWER_WIDTH,
				_pc.convertWidthInCharsToPixels(80));

		_viewerDetailForm.setViewerWidth(leftPartWidth);

		UI.restoreSashWeight(
				_markerContainerSashForm,
				_state,
				STATE_MARKER_CONTAINER_SASH_WEIGHTS,
				new int[] { 50, 50 });
	}

	private void restoreState_Viewer() {

		_imageColumnWidth = Util.getStateInt(_state, STATE_IMAGE_COLUMN_WIDTH, IMAGE_DEFAULT_WIDTH);
	}

	/**
	 * restore type from the backup for the currently selected tour marker
	 */
	private void restoreState_VisibleType() {

		if (_selectedTourMarker == null) {
			return;
		}

		_selectedTourMarker.setVisibleType(ChartLabel.VISIBLE_TYPE_DEFAULT);
	}

	private void saveState() {

		_state.put(DIALOG_SETTINGS_POSITION, _comboLabelPosition.getSelectionIndex());
		_state.put(DIALOG_SETTINGS_VIEWER_WIDTH, _markerContainer.getSize().x);

		_state.put(STATE_IMAGE_COLUMN_WIDTH, _imageColumnWidth);

		UI.saveSashWeight(_markerContainerSashForm, _state, STATE_MARKER_CONTAINER_SASH_WEIGHTS);
	}

	@Override
	public void selectionChanged(final SelectionTourMarker tourMarkerSelection) {

		final ArrayList<TourMarker> selectedTourMarker = tourMarkerSelection.getTourMarker();

		// prevent that the x-slider is positioned in the tour chart
		_isSetXSlider = false;
		{
			_markerViewer.setSelection(new StructuredSelection(selectedTourMarker), true);
		}
		_isSetXSlider = true;

		_comboMarkerName.setFocus();
	}

	private void toggleMarkerVisibility() {

		final ISelection selection = _markerViewer.getSelection();
		if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TourMarker) {

				final TourMarker tourMarker = (TourMarker) firstElement;
				final boolean isMarkerVisible = !tourMarker.isMarkerVisible();

				updateUI_TourMarker(tourMarker, isMarkerVisible);
				enableControls();
			}
		}
	}

	private void updateModel_FromUI(final TourMarker tourMarker) {

		if (tourMarker == null) {
			return;
		}

		tourMarker.setMarkerVisible(_chkVisibility.getSelection());

		tourMarker.setLabel(_comboMarkerName.getText());
		tourMarker.setSignPosition(_comboSignPosition.getSelectionIndex());
		tourMarker.setVisualPosition(_comboLabelPosition.getSelectionIndex());

		tourMarker.setLabelXOffset(_spinLabelOffsetX.getSelection());
		tourMarker.setLabelYOffset(_spinLabelOffsetY.getSelection());
		tourMarker.setSignXOffset(_spinSignOffsetX.getSelection());
		tourMarker.setSignYOffset(_spinSignOffsetY.getSelection());

		tourMarker.setDescription(_txtDescription.getText());
	}

	/**
	 * update marker ui from the selected marker
	 */
	private void updateUI_FromModel() {

		_isUpdateUI = true;
		{
			// make the marker more visible by setting another type
			_selectedTourMarker.setVisibleType(ChartLabel.VISIBLE_TYPE_TYPE_EDIT);

			_chkVisibility.setSelection(_selectedTourMarker.isMarkerVisible());

			_comboMarkerName.setText(_selectedTourMarker.getLabel());
			_comboLabelPosition.select(_selectedTourMarker.getLabelPosition());
			_comboSignPosition.select(_selectedTourMarker.getSignPosition());

			_spinLabelOffsetX.setSelection(_selectedTourMarker.getLabelXOffset());
			_spinLabelOffsetY.setSelection(_selectedTourMarker.getLabelYOffset());
			_spinSignOffsetX.setSelection(_selectedTourMarker.getSignXOffset());
			_spinSignOffsetY.setSelection(_selectedTourMarker.getSignYOffset());

			_txtDescription.setText(_selectedTourMarker.getDescription());

			updateUI_TourSign(_selectedTourMarker);
		}
		_isUpdateUI = false;
	}

	private void updateUI_TourMarker(final TourMarker tourMarker, final Boolean isVisible) {

		tourMarker.setMarkerVisible(isVisible);

		// update UI
		_markerViewer.update(tourMarker, null);
		_tourChart.updateUI_LayerMarker(true);
	}

	private void updateUI_TourSign(final TourMarker tourMarker) {

		final TourSign tourSign = tourMarker.getTourSign();

		if (tourSign == null) {

			_lblSignName.setText(UI.EMPTY_STRING);
			_imgTourSign.setImage(null, false);

		} else {

			_lblSignName.setText(tourSign.getSignName());

			final Photo signPhoto = tourSign.getSignImagePhoto();
			final ILoadCallBack imageLoadCallback = new LoadImageCallbackSelectedMarker(tourSign);
			final Image tourSignImage = SignManager.getPhotoImage(signPhoto, imageLoadCallback);

			_imgTourSign.setImage(tourSignImage, false);
		}
	}

}
