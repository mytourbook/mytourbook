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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartLabel;
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
import net.tourbook.sign.TVIPrefSign;
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

public class DialogMarker extends TitleAreaDialog {

	private static final String		DIALOG_SETTINGS_POSITION			= "marker_position";						//$NON-NLS-1$
	private static final String		STATE_MARKER_CONTAINER_SASH_WEIGHTS	= "MarkerContainerSashWeights";			//$NON-NLS-1$
	private static final String		DIALOG_SETTINGS_VIEWER_WIDTH		= "viewer_width";							//$NON-NLS-1$

	private static final int		OFFSET_PAGE_INCREMENT				= 20;
	private static final int		OFFSET_MAX							= 200;

	private int						DEFAULT_ROW_HEIGHT;
	private int						DEFAULT_IMAGE_WIDTH;
	private int						MAX_ROW_HEIGHT;

	private final IDialogSettings	_state								= TourbookPlugin.getState("DialogMarker");	//$NON-NLS-1$

	private TourChart				_tourChart;
	private TourData				_tourData;

	/**
	 * marker which is currently selected
	 */
	private TourMarker				_selectedTourMarker;

	/**
	 * backup for the selected tour marker
	 */
	private TourMarker				_backupMarker						= new TourMarker();

	private Set<TourMarker>			_tourMarkers;
	private HashSet<TourMarker>		_tourMarkersBackup;

	/**
	 * initial tour marker
	 */
	private TourMarker				_initialTourMarker;

	private ModifyListener			_defaultModifyListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_defaultSelectionAdapter;

	private SignMenuManager			_signMenuManager					= new SignMenuManager();
	private ActionOpenPrefDialog	_actionOpenTourSignPrefs;

	private boolean					_isOkPressed						= false;
	private boolean					_isUpdateUI;

	private NumberFormat			_nf3								= NumberFormat.getNumberInstance();
	{
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	private int						_signImageColumn;
	private int						_imageColumnWidth;

	/*
	 * none UI
	 */
	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private ViewerDetailForm		_viewerDetailForm;
	private Composite				_markerContainer;
	private SashForm				_markerContainerSashForm;

	private TableViewer				_markerViewer;

	/**
	 * Contains the table column widget for the sign image.
	 */
	private TableColumn				_tcSignImage;

	private Button					_btnDelete;
	private Button					_btnHideAll;
	private Button					_btnShowAll;
	private Button					_btnUndo;

	private Combo					_comboMarkerName;
	private Combo					_comboMarkerPosition;

	private ImageCanvas				_imgTourSign;
	private Label					_lblSignName;

	private Spinner					_spinOffsetX;
	private Spinner					_spinOffsetY;

// comment is disabled
//	private Text					_txtComment;
	private Text					_txtDescription;

	{
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

		private Photo	__signPhoto;

		public LoadImageCallbackSelectedMarker(final Photo signPhoto) {
			__signPhoto = signPhoto;
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

					// update sign image

					final Image signImage = SignManager.getPhotoImage(__signPhoto);

					if (signImage != null) {
						_imgTourSign.setImage(signImage);
					}
				}
			});

		}
	}

	private class LoadImageCallbackViewer implements ILoadCallBack {

		public LoadImageCallbackViewer() {}

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

					// !!! refresh() and update() do not repaint a loaded image but a redraw() do
					_markerViewer.getTable().redraw();
				}
			});

		}
	}

	private final class MarkerEditingSupport extends EditingSupport {

		private final TableViewer			_tableViewer;
		private final CheckboxCellEditor	_cellEditor;

		private MarkerEditingSupport(final TableViewer tableViewer) {

			super(tableViewer);

			_tableViewer = tableViewer;
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

				tourMarker.setMarkerVisible((Boolean) value);
			}

			// update UI
			_tableViewer.update(element, null);
			_tourChart.updateLayerMarker(true);
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

	public void actionRemoveTourSign() {

		_selectedTourMarker.setSign(null);
		_lblSignName.setText(UI.EMPTY_STRING);
		_imgTourSign.setImage(null);
	}

	public void actionSetTourSign(final TourSign tourSign) {

		_selectedTourMarker.setSign(tourSign);
		_lblSignName.setText(tourSign.getSignName());

		final Photo signPhoto = new Photo(tourSign.getImageFilePathName());
		final ILoadCallBack imageLoadCallback = new LoadImageCallbackSelectedMarker(signPhoto);

		final Image signImage = SignManager.getPhotoImage(signPhoto, imageLoadCallback);

		if (signImage != null) {
			_imgTourSign.setImage(signImage);
		}
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
		_tourChart.updateLayerMarker(true);
	}

	@Override
	public boolean close() {

		if (_isOkPressed) {

			/*
			 * the markers are already set into the tour data because the original values are
			 * modified
			 */

			restoreVisibleType();

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

		restoreState();

		setTitle(Messages.Dlg_TourMarker_Dlg_title);
		setMessage(Messages.Dlg_TourMarker_Dlg_Message);

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

		final Composite marginContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(marginContainer);
		GridLayoutFactory.swtDefaults().applyTo(marginContainer);
		{
			final Composite dlgContainer = new Composite(marginContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
			GridLayoutFactory.swtDefaults().applyTo(dlgContainer);
			{
				// left part
				_markerContainer = createUI_10_LeftPart(dlgContainer);

				// sash
				final Sash sash = new Sash(dlgContainer, SWT.VERTICAL);
				UI.addSashColorHandler(sash);

				// right part
				final Composite chartContainer = createUI_20_RightPart(dlgContainer);

				_viewerDetailForm = new ViewerDetailForm(dlgContainer, _markerContainer, sash, chartContainer, 30);
			}

			createUI_90_MarkerActions(marginContainer);
		}
	}

	private Composite createUI_10_LeftPart(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 5, 0, 0)
				.applyTo(container);
		{
			_markerContainerSashForm = new SashForm(container, SWT.SMOOTH | SWT.VERTICAL);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_markerContainerSashForm);
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
		GridDataFactory.fillDefaults().grab(true, false).applyTo(chartContainer);
		GridLayoutFactory.fillDefaults().margins(5, 0).applyTo(chartContainer);
//		markerDetailContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
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
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

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
		table.setLinesVisible(true);

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

		DEFAULT_ROW_HEIGHT = table.getItemHeight();

		_markerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		defineColumn_1stHidden(tableLayout);//		// 0
		defineColumn_Distance(tableLayout);//		// 1
		defineColumn_IsVisible(tableLayout);//		// 2
		_tcSignImage = defineColumn_Image(tableLayout);//			// 3
		defineColumn_Marker(tableLayout);//			// 4
		defineColumn_Description(tableLayout);//	// 5
		defineColumn_OffsetX(tableLayout);//		// 6
		defineColumn_OffsetY(tableLayout);//		// 7

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
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * Marker name
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dlg_TourMarker_Label_marker_name);

				// Combo
				_comboMarkerName = new Combo(container, SWT.BORDER | SWT.FLAT);
				_comboMarkerName.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(final KeyEvent e) {
						onChangeMarkerUI();
					}
				});
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboMarkerName);

				// fill combobox
				final TreeSet<String> dbTitles = TourDatabase.getAllTourMarkerNames();
				for (final String title : dbTitles) {
					_comboMarkerName.add(title);
				}

				new AutocompleteComboInput(_comboMarkerName);
			}

			/*
			 * Position
			 */
			{
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dlg_TourMarker_Label_position);

				// combo
				_comboMarkerPosition = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboMarkerPosition.setVisibleItemCount(20);
				_comboMarkerPosition.addSelectionListener(_defaultSelectionAdapter);

				// fill position combo
				for (final String position : TourMarker.visualPositionLabels) {
					_comboMarkerPosition.add(position);
				}
			}

			/*
			 * Sign
			 */
			{
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dlg_TourMarker_Label_Sign);

				final Composite signContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(signContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(signContainer);
//				signContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
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

			/*
			 * Description
			 */
			{
				// label
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(label);
				label.setText(Messages.Dlg_TourMarker_Label_Description);

				// text
				_txtDescription = new Text(container, SWT.BORDER //
						| SWT.WRAP
						| SWT.V_SCROLL
						| SWT.H_SCROLL);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
//						.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(3))
						.applyTo(_txtDescription);
				_txtDescription.addModifyListener(_defaultModifyListener);
			}

			/*
			 * Comment
			 */
//			{
//				// label
//				final Label label = new Label(container, SWT.NONE);
//				GridDataFactory.fillDefaults()//
//						.align(SWT.BEGINNING, SWT.BEGINNING)
//						.applyTo(label);
//				label.setText(Messages.Dlg_TourMarker_Label_Comment);
//
//				// text
//				_txtComment = new Text(container, SWT.BORDER //
//						| SWT.WRAP
//						| SWT.V_SCROLL
//						| SWT.H_SCROLL);
//				GridDataFactory.fillDefaults()//
//						.grab(true, true)
////						.hint(SWT.DEFAULT, _pc.convertHeightInCharsToPixels(3))
//						.applyTo(_txtComment);
//				_txtComment.addModifyListener(_defaultModifyListener);
//			}

			createUI_70_Offset(container);
		}
	}

	/**
	 * offset container
	 */
	private void createUI_70_Offset(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				// Label: Horizontal offset
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
				label.setText(Messages.Dlg_TourMarker_Label_horizontal_offset);

				// Spinner: Horizontal offset
				_spinOffsetX = new Spinner(container, SWT.BORDER);
				_spinOffsetX.setMinimum(-OFFSET_MAX);
				_spinOffsetX.setMaximum(OFFSET_MAX);
				_spinOffsetX.setPageIncrement(OFFSET_PAGE_INCREMENT);
				_spinOffsetX.addSelectionListener(_defaultSelectionAdapter);
				_spinOffsetX.addMouseWheelListener(_defaultMouseWheelListener);

			}
			{
				// Label: Vertical offset
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_pc.convertWidthInCharsToPixels(5), 0)
						.applyTo(label);
				label.setText(Messages.Dlg_TourMarker_Label_vertical_offset);

				// Spinner: Vertical offset
				_spinOffsetY = new Spinner(container, SWT.BORDER);
				_spinOffsetY.setMinimum(-OFFSET_MAX);
				_spinOffsetY.setMaximum(OFFSET_MAX);
				_spinOffsetY.setPageIncrement(OFFSET_PAGE_INCREMENT);
				_spinOffsetY.addSelectionListener(_defaultSelectionAdapter);
				_spinOffsetY.addMouseWheelListener(_defaultMouseWheelListener);

			}
		}
	}

	/**
	 * create tour chart with new marker
	 */
	private void createUI_80_TourChart(final Composite parent) {

		_tourChart = new TourChart(parent, SWT.FLAT /* | SWT.BORDER */, true);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(600, 350)
				.applyTo(_tourChart);
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setContextProvider(new DialogMarkerTourChartContextProvicer(this));

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
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
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
					onDeleteMarker();
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
					onShowHideAll(true);
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
					onShowHideAll(false);
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

		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(10), false));

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

	private void enableControls() {

		final boolean isMarkerSelected = _selectedTourMarker != null;

		if (isMarkerSelected) {
			_btnUndo.setEnabled(_selectedTourMarker.compareTo(_backupMarker, true) == false);
		} else {
			_btnUndo.setEnabled(false);
		}

		/*
		 * button: delete marker
		 */
		_btnDelete.setEnabled(isMarkerSelected);

		final boolean isMarkerAvailable = _markerViewer.getTable().getItemCount() != 0;

		_btnShowAll.setEnabled(isMarkerAvailable);
		_btnHideAll.setEnabled(isMarkerAvailable);
		_comboMarkerName.setEnabled(isMarkerAvailable);
		_comboMarkerPosition.setEnabled(isMarkerAvailable);
		_lblSignName.setEnabled(isMarkerSelected);
		_spinOffsetX.setEnabled(isMarkerAvailable);
		_spinOffsetY.setEnabled(isMarkerAvailable);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		return _state;
//		return null;
	}

	private int getImageColumnWidth() {

		int width;

		if (_tcSignImage == null) {
			width = DEFAULT_IMAGE_WIDTH;
		} else {
			width = _tcSignImage.getWidth();
		}

		return width;
	}

	private int getRowHeight() {

		return Math.min(MAX_ROW_HEIGHT, Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth));
	}

	TourChart getTourChart() {
		return _tourChart;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		DEFAULT_IMAGE_WIDTH = _pc.convertWidthInCharsToPixels(6);
		MAX_ROW_HEIGHT = _pc.convertVerticalDLUsToPixels(50);
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

		_tourChart.updateLayerMarker(true);

		_markerViewer.update(_selectedTourMarker, null);

		enableControls();
	}

	/**
	 * remove selected markers from the view and update dependened structures
	 */
	private void onDeleteMarker() {

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
			_tourChart.updateLayerMarker(true);

			// select next marker
			TourMarker nextMarker = (TourMarker) _markerViewer.getElementAt(lastMarkerIndex);
			if (nextMarker == null) {
				nextMarker = (TourMarker) _markerViewer.getElementAt(lastMarkerIndex - 1);
			}

			if (nextMarker == null) {
				// disable controls when no marker is available
				enableControls();
			} else {
				_markerViewer.setSelection(new StructuredSelection(nextMarker), true);
			}

			_markerViewer.getTable().setFocus();
		}
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
			restoreVisibleType();
		}

		// set new selected marker
		_selectedTourMarker = newSelectedMarker;

		// make a backup of the marker to undo modifications
		_selectedTourMarker.setMarkerBackup(_backupMarker);

		updateUI_FromModel();
		onChangeMarkerUI();

		// set slider position
		_tourChart.setXSliderPosition(new SelectionChartXSliderPosition(
				_tourChart,
				newSelectedMarker.getSerieIndex(),
				SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
	}

	private void onShowHideAll(final boolean isVisible) {

		for (final TourMarker tourMarker : _tourMarkers) {
			tourMarker.setMarkerVisible(isVisible);
		}

		// update UI
		final TourMarker[] allTourMarker = _tourMarkers.toArray(new TourMarker[_tourMarkers.size()]);
		_markerViewer.update(allTourMarker, null);
		_tourChart.updateLayerMarker(true);
	}

	private void onViewerPaint(final Event event) {

		final TableItem item = (TableItem) event.item;
		final Object itemData = item.getData();
		if (itemData instanceof TVIPrefSign) {

			final TourSign tourSign = ((TourMarker) itemData).getTourSign();

			final Photo signPhoto = new Photo(tourSign.getImageFilePathName());
			final ILoadCallBack imageLoadCallback = new LoadImageCallbackSelectedMarker(signPhoto);

			final Image signImage = SignManager.getPhotoImage(signPhoto, imageLoadCallback);

			if (signImage != null) {

				final int photoPosX = event.x;
				final int photoPosY = event.y;

				switch (event.type) {
				case SWT.MeasureItem:

					// this is replaced with Hack.setTreeItemHeight() for win

//					event.width += imageRect.width;
//					event.height = Math.max(event.height, imageRect.height + 2);

					if (UI.IS_WIN == false) {
						event.height = getRowHeight();
					}

					break;

				case SWT.PaintItem:

					final GC gc = event.gc;
					final Photo photo = signPhoto;

					final int imageCanvasWidth = Math.max(DEFAULT_ROW_HEIGHT, _imageColumnWidth);
					final int imageCanvasHeight = event.height;

					PhotoUI.paintPhotoImage(
							gc,
							photo,
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

	/**
	 * restore type from the backup for the currently selected tour marker
	 */
	private void restoreVisibleType() {

		if (_selectedTourMarker == null) {
			return;
		}

		_selectedTourMarker.setVisibleType(ChartLabel.VISIBLE_TYPE_DEFAULT);
	}

	private void saveState() {

		_state.put(DIALOG_SETTINGS_POSITION, _comboMarkerPosition.getSelectionIndex());
		_state.put(DIALOG_SETTINGS_VIEWER_WIDTH, _markerContainer.getSize().x);

		UI.saveSashWeight(_markerContainerSashForm, _state, STATE_MARKER_CONTAINER_SASH_WEIGHTS);
	}

	private void toggleMarkerVisibility() {

		final ISelection selection = _markerViewer.getSelection();
		if (selection instanceof StructuredSelection) {
			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TourMarker) {

				final TourMarker tourMarker = (TourMarker) firstElement;
				tourMarker.setMarkerVisible(!tourMarker.isMarkerVisible());

				// update UI
				_markerViewer.update(tourMarker, null);
				_tourChart.updateLayerMarker(true);
			}
		}
	}

	private void updateModel_FromUI(final TourMarker tourMarker) {

		if (tourMarker == null) {
			return;
		}

		tourMarker.setLabel(_comboMarkerName.getText());
		tourMarker.setVisualPosition(_comboMarkerPosition.getSelectionIndex());

//		tourMarker.setComment(_txtComment.getText());
		tourMarker.setDescription(_txtDescription.getText());

		tourMarker.setLabelXOffset(_spinOffsetX.getSelection());
		tourMarker.setLabelYOffset(_spinOffsetY.getSelection());
	}

	/**
	 * update marker ui from the selected marker
	 */
	private void updateUI_FromModel() {

		_isUpdateUI = true;
		{
			// make the marker more visible by setting another type
			_selectedTourMarker.setVisibleType(ChartLabel.VISIBLE_TYPE_TYPE_EDIT);

			_comboMarkerName.setText(_selectedTourMarker.getLabel());
			_comboMarkerPosition.select(_selectedTourMarker.getVisualPosition());

//			_txtComment.setText(_selectedTourMarker.getComment());
			_txtDescription.setText(_selectedTourMarker.getDescription());

			_spinOffsetX.setSelection(_selectedTourMarker.getLabelXOffset());
			_spinOffsetY.setSelection(_selectedTourMarker.getLabelYOffset());
		}
		_isUpdateUI = false;
	}

}
