/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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

import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.form.SashBottomFixedForm;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.tourChart.ChartLabel;
import net.tourbook.ui.tourChart.ITourMarkerSelectionListener;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class DialogMarker extends TitleAreaDialog implements ITourMarkerSelectionListener, ITourMarkerModifyListener
/*
 * , ITourSignSetter
 */{

	private static final String			TOUR_MARKER_COLUMN_IS_VISIBLE			= net.tourbook.ui.Messages.Tour_Marker_Column_IsVisible;
	private static final String			TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP	= net.tourbook.ui.Messages.Tour_Marker_Column_IsVisible_Tooltip;

	private static final String			DIALOG_SETTINGS_POSITION				= "marker_position";												//$NON-NLS-1$
//	private static final String			STATE_IMAGE_COLUMN_WIDTH	= "STATE_IMAGE_COLUMN_WIDTH";				//$NON-NLS-1$
	private static final String			STATE_INNER_SASH_HEIGHT					= "STATE_INNER_SASH_HEIGHT";										//$NON-NLS-1$
	private static final String			STATE_OUTER_SASH_WIDTH					= "STATE_OUTER_SASH_WIDTH";										//$NON-NLS-1$

	private static final int			OFFSET_PAGE_INCREMENT					= 20;
	private static final int			OFFSET_MAX								= 200;

	private int							CONTENT_DEFAULT_WIDTH;
//	private int							IMAGE_DEFAULT_WIDTH;
//	private int							IMAGE_MIN_WIDTH;
//	private int							ROW_DEFAULT_HEIGHT;
//	private int							ROW_MAX_HEIGHT;

	private final IDialogSettings		_state									= TourbookPlugin
																						.getState("DialogMarker");									//$NON-NLS-1$

	private TourChart					_tourChart;
	private TourData					_tourData;

	/**
	 * marker which is currently selected
	 */
	private TourMarker					_selectedTourMarker;

	/**
	 * backup for the selected tour marker
	 */
	private TourMarker					_backupMarker							= new TourMarker();

	private Set<TourMarker>				_originalTourMarkers;
	private HashSet<TourMarker>			_dialogTourMarkers;

	/**
	 * initial tour marker
	 */
	private TourMarker					_initialTourMarker;

	private ModifyListener				_defaultModifyListener;
	private MouseWheelListener			_defaultMouseWheelListener;
	private SelectionAdapter			_defaultSelectionAdapter;

//	private SignMenuManager				_signMenuManager			= new SignMenuManager(this);

	private boolean						_isOkPressed							= false;
	private boolean						_isUpdateUI;
	private boolean						_isSetXSlider							= true;

	private NumberFormat				_nf3									= NumberFormat.getNumberInstance();

//	private int							_signImageColumn;
//	private int							_imageColumnWidth;

	/**
	 * Contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width.
	 */
	private final ArrayList<Control>	_firstColumnControls					= new ArrayList<Control>();

	/*
	 * none UI
	 */
	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private SashLeftFixedForm			_sashOuterForm;
	private SashBottomFixedForm			_sashInnerForm;
	private Composite					_outerFixedPart;
	private Composite					_innerFixedPart;

	private TableViewer					_markerViewer;

//	/**
//	 * Contains the table column widget for the sign image.
//	 */
//	private TableColumn					_tcSignImage;

	private Button						_btnDelete;
	private Button						_btnHideAll;
	private Button						_btnPasteText;
	private Button						_btnPasteUrl;
	private Button						_btnShowAll;
	private Button						_btnUndo;
	private Button						_chkVisibility;

	private Combo						_comboLabelPosition;
	private Combo						_comboMarkerName;

	private Group						_groupText;
	private Group						_groupUrl;
//	private Group						_groupImage;

//	private ImageCanvas					_imgTourImage;
	private Image						_imagePaste;

	private Label						_lblDescription;
//	private Label						_lblImageName;
	private Label						_lblLabel;
	private Label						_lblLabelOffsetX;
	private Label						_lblLabelOffsetY;
	private Label						_lblLabelPosition;
	private Label						_lblLinkText;
	private Label						_lblLinkUrl;

//	private Link						_linkImage;

	private Spinner						_spinLabelOffsetX;
	private Spinner						_spinLabelOffsetY;

	private Text						_txtDescription;
	private Text						_txtUrlAddress;
	private Text						_txtUrlText;

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

//	private class LoadImageCallbackSelectedMarker implements ILoadCallBack {
//
//		private TourSign	__tourSign;
//
//		public LoadImageCallbackSelectedMarker(final TourSign tourSign) {
//			__tourSign = tourSign;
//		}
//
//		@Override
//		public void callBackImageIsLoaded(final boolean isImageLoaded) {
//
//			if (isImageLoaded == false) {
//				return;
//			}
//
//			// run in UI thread
//			Display.getDefault().syncExec(new Runnable() {
//
//				@Override
//				public void run() {
//
//					if (getShell().isDisposed()) {
//						return;
//					}
//
//					final Image signImage = SignManager.getSignImage(__tourSign.getSignImagePhoto());
//
//					if (signImage != null) {
//
//						// draw sign image
//						_imgTourImage.setImage(signImage, false);
//
//						redrawViewer();
//						_tourChart.redrawLayer();
//					}
//				}
//			});
//
//		}
//	}
//
//	private class LoadImageCallbackViewer implements ILoadCallBack {
//
//		public LoadImageCallbackViewer(final TourSign tourSign) {}
//
//		@Override
//		public void callBackImageIsLoaded(final boolean isImageLoaded) {
//
//			if (isImageLoaded == false) {
//				return;
//			}
//
//			// run in UI thread
//			Display.getDefault().syncExec(new Runnable() {
//
//				@Override
//				public void run() {
//
//					if (getShell().isDisposed()) {
//						return;
//					}
//
//					// draw sign image
//
//					redrawViewer();
//					_tourChart.redrawLayer();
//				}
//			});
//		}
//	}

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
				return _dialogTourMarkers.toArray();
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

		// create a shallow copy
		_originalTourMarkers = new HashSet<TourMarker>();
		_originalTourMarkers.addAll(_tourData.getTourMarkers());

		/*
		 * make a backup copy of the tour markers, modify the original data so that the tour chart
		 * displays the modifications
		 */
		_dialogTourMarkers = new HashSet<TourMarker>();

		for (final TourMarker tourMarker : _originalTourMarkers) {
			_dialogTourMarkers.add(tourMarker.clone());
		}

		_tourData.setTourMarkers(_dialogTourMarkers);

		_initialTourMarker = initialTourMarker;
	}

	/**
	 * remove selected markers from the view and update dependened structures
	 */
	private void actionDeleteMarker() {

		final IStructuredSelection markerSelection = (IStructuredSelection) _markerViewer.getSelection();
		final TourMarker selectedMarker = (TourMarker) markerSelection.getFirstElement();

		deleteTourMarker(selectedMarker);
	}

	private void actionPastText(final Text textControl) {

		final Clipboard cb = new Clipboard(_groupUrl.getDisplay());
		try {

			final TextTransfer transfer = TextTransfer.getInstance();

			final String transferText = (String) cb.getContents(transfer);
			if (transferText != null) {
				try {

					/*
					 * !!! It needs 2 times to be converted to get the correct text string !!!
					 */
					final URI uri = new URI(transferText);
					final URI decodedURI = new URI(
							uri.getScheme(),
							uri.getUserInfo(),
							uri.getHost(),
							uri.getPort(),
							uri.getPath(),
							uri.getQuery(),
							uri.getFragment());

					textControl.setText(decodedURI.toString());

				} catch (final Exception e) {

					MessageDialog.openInformation(
							getShell(),
							Messages.Dlg_TourMarker_MsgBox_WrongFormat_Title,
							NLS.bind(Messages.Dlg_TourMarker_MsgBox_WrongFormat_Message, transferText));
				}
			}
		} finally {
			cb.dispose();
		}
	}

	private void actionShowHideAll(final boolean isVisible) {

		for (final TourMarker tourMarker : _dialogTourMarkers) {
			tourMarker.setMarkerVisible(isVisible);
		}

		/*
		 * Update UI
		 */
		// controls
		updateUI_FromModel();

		// viewer+chart
		final TourMarker[] allTourMarker = _dialogTourMarkers.toArray(new TourMarker[_dialogTourMarkers.size()]);
		_markerViewer.update(allTourMarker, null);
		_tourChart.updateUI_MarkerLayer(true);

		enableControls();
	}

	public void addTourMarker(final TourMarker newTourMarker) {

		if (newTourMarker == null) {
			return;
		}

		// update data model, add new marker to the marker list
		_dialogTourMarkers.add(newTourMarker);

		// update the viewer and select the new marker
		_markerViewer.refresh();
		_markerViewer.setSelection(new StructuredSelection(newTourMarker), true);

		_comboMarkerName.setFocus();

		// update chart
		_tourChart.updateUI_MarkerLayer(true);
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
			_tourData.setTourMarkers(_originalTourMarkers);
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
				fillContextMenu(menuMgr);
			}
		});

		// set context menu for the link
		final Menu signContextMenu = menuMgr.createContextMenu(control);
		control.setMenu(signContextMenu);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

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
			final Composite sashContainer = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(sashContainer);
			GridLayoutFactory.swtDefaults().applyTo(sashContainer);
			{
				// left part
				_outerFixedPart = createUI_10_LeftPart(sashContainer);

				// sash
				final Sash sash = new Sash(sashContainer, SWT.VERTICAL);
				UI.addSashColorHandler(sash);

				// right part
				final Composite chartContainer = createUI_20_RightPart(sashContainer);

				_sashOuterForm = new SashLeftFixedForm(//
						sashContainer,
						_outerFixedPart,
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

//			// set column image (row) height
//			onResizeImageColumn();

			// compute width for all controls and equalize column width for the different sections
			sashContainer.layout(true, true);
			UI.setEqualizeColumWidths(_firstColumnControls, _pc.convertWidthInCharsToPixels(2));
		}
	}

	private Composite createUI_10_LeftPart(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 5, 0, 0)
				.applyTo(container);
		{
			final Composite sashContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(sashContainer);

			{
				// top part
				final Composite flexiblePart = createUI_40_MarkerList(sashContainer);

				// sash
				final Sash sash = new Sash(sashContainer, SWT.HORIZONTAL);
				UI.addSashColorHandler(sash);

				// bottom part
				_innerFixedPart = createUI_50_MarkerDetails(sashContainer);

				_sashInnerForm = new SashBottomFixedForm(//
						sashContainer,
						flexiblePart,
						sash,
						_innerFixedPart);
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
				.extendedMargins(5, 5, 0, 0)
				.applyTo(chartContainer);
		{
			createUI_80_TourChart(chartContainer);
		}

		return chartContainer;
	}

	/**
	 * container: marker list
	 * 
	 * @return
	 */
	private Composite createUI_40_MarkerList(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(0, 0, 0, 5)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			// label: markers
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Dlg_TourMarker_Label_markers);

			createUI_42_MarkerViewer(container);
		}

		return container;
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

//		/*
//		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
//		 * critical for performance that these methods be as efficient as possible.
//		 */
//		final Listener paintListener = new Listener() {
//			@Override
//			public void handleEvent(final Event event) {
//
//				if (event.index == _signImageColumn //
//						&& (event.type == SWT.MeasureItem || event.type == SWT.PaintItem)) {
//
//					onViewerPaint(event);
//				}
//			}
//		};
//		table.addListener(SWT.MeasureItem, paintListener);
//		table.addListener(SWT.PaintItem, paintListener);
//
//		ROW_DEFAULT_HEIGHT = table.getItemHeight();

		_markerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		defineColumn_1stHidden(tableLayout);//				// 0
		defineColumn_Distance(tableLayout);//				// 1
		defineColumn_IsVisible(tableLayout);//				// 2
//		_tcSignImage = defineColumn_Image(tableLayout);//	// 3
		defineColumn_Marker(tableLayout);//					// 4
		defineColumn_Description(tableLayout);//			// 5
		defineColumn_Url(tableLayout);//					// 6
		defineColumn_OffsetX(tableLayout);//				// 7
		defineColumn_OffsetY(tableLayout);//				// 8

//		_signImageColumn = 3;

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

	private Composite createUI_50_MarkerDetails(final Composite parent) {

		/*
		 * container: marker details
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * Text
			 */
			_groupText = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					/*
					 * !!! This min size ensures that the upper part (description) is NOT hidden
					 * before the other parts (url, image) when the vertical splitter is moved. It
					 * took a while to find this solution :-(
					 */
					.minSize(SWT.DEFAULT, _pc.convertVerticalDLUsToPixels(65))
					.applyTo(_groupText);
			_groupText.setText(Messages.Dlg_TourMarker_Group_Label);
			GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupText);
			{
				createUI_52_Label(_groupText);
				createUI_54_Description(_groupText);
				createUI_56_Label_Position(_groupText);
			}

//			/*
//			 * Image
//			 */
//			_groupImage = new Group(container, SWT.NONE);
//			GridDataFactory.fillDefaults()//
//					.grab(true, false)
//					.applyTo(_groupImage);
//			_groupImage.setText(Messages.Dlg_TourMarker_Group_Image);
//			GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupImage);
//
//			{
//				createUI_62_Image(_groupImage);
//			}

			/*
			 * Url
			 */
			_groupUrl = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_groupUrl);
			_groupUrl.setText(Messages.Dlg_TourMarker_Group_Url);
			GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_groupUrl);
			{
				createUI_60_Url(_groupUrl);
			}

			createUI_70_Visibility(container);
		}

		return container;
	}

	private void createUI_52_Label(final Composite parent) {

		/*
		 * Marker name
		 */
		{
			// Label
			_lblLabel = new Label(parent, SWT.NONE);
			_firstColumnControls.add(_lblLabel);
			_lblLabel.setText(Messages.Dlg_TourMarker_Label_Label);

			// Combo
			_comboMarkerName = new Combo(parent, SWT.BORDER | SWT.FLAT);
			GridDataFactory.fillDefaults()//
					// !!! hint must be set because the width is adjusted to the content
					.hint(CONTENT_DEFAULT_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(_comboMarkerName);
			_comboMarkerName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent e) {
					onChangeMarkerUI();
				}
			});
		}
	}

	private void createUI_54_Description(final Composite parent) {

		{
			/*
			 * Description
			 */

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
					.hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
					.applyTo(_txtDescription);
			_txtDescription.addModifyListener(_defaultModifyListener);
		}
	}

	private void createUI_56_Label_Position(final Composite parent) {

		/*
		 * Position
		 */
		{
			// label
			_lblLabelPosition = new Label(parent, SWT.NONE);
			_firstColumnControls.add(_lblLabelPosition);
			_lblLabelPosition.setText(Messages.Dlg_TourMarker_Label_position);
			_lblLabelPosition.setToolTipText(Messages.Dlg_TourMarker_Label_Position_Tooltip);

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

	private void createUI_60_Url(final Composite parent) {

		final FocusAdapter focusAdapterSelectAllText = new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * !!! This feature is not working for all cases !!!
				 */
				((Text) e.widget).selectAll();
			}
		};

		/*
		 * Link Text
		 */
		{
			// label
			_lblLinkText = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblLinkText);
			_lblLinkText.setText(Messages.Dlg_TourMarker_Label_LinkText);
			_lblLinkText.setToolTipText(Messages.Dlg_TourMarker_Label_LinkText_Tooltip);

			// text
			_txtUrlText = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(_txtUrlText);
			_txtUrlText.addModifyListener(_defaultModifyListener);
			_txtUrlText.addFocusListener(focusAdapterSelectAllText);

			// paste
			_btnPasteText = new Button(parent, SWT.NONE);
			_btnPasteText.setImage(_imagePaste);
			_btnPasteText.setToolTipText(Messages.Dlg_TourMarker_Button_PasteFromClipboard_Tooltip);
			_btnPasteText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionPastText(_txtUrlText);
				}
			});
		}

		/*
		 * Link Url
		 */
		{
			// label
			_lblLinkUrl = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblLinkUrl);
			_lblLinkUrl.setText(Messages.Dlg_TourMarker_Label_LinkUrl);
			_lblLinkUrl.setToolTipText(Messages.Dlg_TourMarker_Label_LinkUrl_Tooltip);

			// text
			_txtUrlAddress = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(_txtUrlAddress);
			_txtUrlAddress.addFocusListener(focusAdapterSelectAllText);
			_txtUrlAddress.addModifyListener(_defaultModifyListener);

			// paste
			_btnPasteUrl = new Button(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_btnPasteUrl);
			_btnPasteUrl.setImage(_imagePaste);
			_btnPasteUrl.setToolTipText(Messages.Dlg_TourMarker_Button_PasteFromClipboard_Tooltip);
			_btnPasteUrl.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionPastText(_txtUrlAddress);
				}
			});
		}
	}

	private void createUI_70_Visibility(final Composite parent) {

		{
			/*
			 * Visibility
			 */
			_chkVisibility = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
//					.span(2, 1)
//					.align(SWT.END, SWT.FILL)
					.applyTo(_chkVisibility);
			_chkVisibility.setText(Messages.Dlg_TourMarker_Checkbox_MarkerVisibility);
			_chkVisibility.setToolTipText(TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP);
			_chkVisibility.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					toggleMarkerVisibility();
				}
			});
		}
	}

//	private void createUI_62_Image(final Composite parent) {
//
//		final SelectionAdapter imageSelectionListener = new SelectionAdapter() {
//			@Override
//			public void widgetSelected(final SelectionEvent e) {
//				UI.openControlMenu(_imgTourImage);
//			}
//		};
//
//		final int SIGN_IMAGE_MAX_SIZE = TourMarker.getSignImageMaxSize(_pc);
//
//		/*
//		 * Marker image
//		 */
//		{
//			// label
//			_linkImage = new Link(parent, SWT.NONE);
//			_firstColumnControls.add(_linkImage);
//			_linkImage.setText(Messages.Dlg_TourMarker_Link_Image);
//			_linkImage.addSelectionListener(imageSelectionListener);
//
//			final Composite signContainer = new Composite(parent, SWT.NONE);
//			GridDataFactory.fillDefaults()//
//					.grab(true, false)
//					.applyTo(signContainer);
//			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(signContainer);
////			signContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//			{
//				/*
//				 * Image: sign image
//				 */
//				_imgTourImage = new ImageCanvas(signContainer, SWT.NONE);
//				GridDataFactory.fillDefaults()//
//						.hint(SIGN_IMAGE_MAX_SIZE, SIGN_IMAGE_MAX_SIZE)
//						.applyTo(_imgTourImage);
//				_imgTourImage.setStyle(SWT.CENTER | SWT.LEAD);
//				_imgTourImage.addSelectionListener(imageSelectionListener);
//				createContextMenu(_imgTourImage);
//
//				/*
//				 * Label: sign name
//				 */
//				_lblImageName = new Label(signContainer, SWT.NONE);
//				GridDataFactory.fillDefaults()//
//						.grab(true, false)
//						.align(SWT.FILL, SWT.CENTER)
//						.applyTo(_lblImageName);
//			}
//		}
//	}

	/**
	 * create tour chart with new marker
	 */
	private void createUI_80_TourChart(final Composite parent) {

		_tourChart = new TourChart(parent, SWT.FLAT /* | SWT.BORDER */, null);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(_pc.convertWidthInCharsToPixels(90), _pc.convertHeightInCharsToPixels(40))
				.applyTo(_tourChart);
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setContextProvider(new DialogMarkerTourChartContextProvicer(this), true);

		_tourChart.setIsDisplayedInDialog(true);

		_tourChart.addTourMarkerSelectionListener(this);
		_tourChart.addTourMarkerModifyListener(this);
		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {

				TourManager.fireEventWithCustomData(//
						TourEventId.SLIDER_POSITION_CHANGED,
						chartInfoSelection,
						null);
			}
		});

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
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 0, 10, 0)
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
	 * column: marker
	 */
	private void defineColumn_IsVisible(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.LEAD);
		final TableColumn tc = tvc.getColumn();

		tc.setText(TOUR_MARKER_COLUMN_IS_VISIBLE);
		tc.setToolTipText(TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP);

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

//	/**
//	 * Column: Sign image
//	 *
//	 * @return
//	 */
//	private TableColumn defineColumn_Image(final TableColumnLayout tableLayout) {
//
//		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
//		final TableColumn tc = tvc.getColumn();
//
////		tc.setText(UI.UNIT_LABEL_DISTANCE);
////		tc.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);
//		tvc.setLabelProvider(new CellLabelProvider() {
//
//			/*
//			 * !!! set dummy label provider, otherwise an error occures !!!
//			 */
//			@Override
//			public void update(final ViewerCell cell) {}
//		});
//
//		tc.addControlListener(new ControlAdapter() {
//			@Override
//			public void controlResized(final ControlEvent e) {
//				onResizeImageColumn();
//			}
//		});
//
//		tableLayout.setColumnData(tc, new ColumnPixelData(_imageColumnWidth, false));
//
//		return tc;
//	}

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

	/**
	 * Column: Url
	 */
	private void defineColumn_Url(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.CENTER);
		final TableColumn tc = tvc.getColumn();

		tc.setText(Messages.Tour_Marker_Column_Url_ShortCut);
		tc.setToolTipText(Messages.Tour_Marker_Column_Url_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final TourMarker tourMarker = (TourMarker) cell.getElement();
				final String urlAddress = tourMarker.getUrlAddress();
				final String urlText = tourMarker.getUrlText();

				cell.setText(urlAddress.length() > 0 || urlText.length() > 0 ? //
						UI.SYMBOL_STAR
						: UI.EMPTY_STRING);
			}
		});
		tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(4), false));
	}

	private void deleteTourMarker(final TourMarker tourMarker) {

		if (MessageDialog.openQuestion(
				getShell(),
				Messages.Dlg_TourMarker_MsgBox_delete_marker_title,
				NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (tourMarker).getLabel())) == false) {
			return;
		}

		_selectedTourMarker = null;

		// get index for selected marker
		final int lastMarkerIndex = _markerViewer.getTable().getSelectionIndex();

		// update data model
		_dialogTourMarkers.remove(tourMarker);

		// update the viewer
		_markerViewer.remove(tourMarker);

		// update chart
		_tourChart.updateUI_MarkerLayer(true);

		updateUI_FromModel();

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

	private void dispose() {

		_firstColumnControls.clear();

		Util.disposeResource(_imagePaste);
	}

	private void enableControls() {

		final boolean isMarkerSelected = _selectedTourMarker != null;
		final boolean isMarkerEnabled = _markerViewer.getTable().getItemCount() != 0;

		if (isMarkerSelected) {
			_btnUndo.setEnabled(_selectedTourMarker.isEqual(_backupMarker, true) == false);
		} else {
			_btnUndo.setEnabled(false);
		}

		_chkVisibility.setEnabled(isMarkerSelected);

		_btnDelete.setEnabled(isMarkerSelected);
		_btnShowAll.setEnabled(isMarkerEnabled);
		_btnHideAll.setEnabled(isMarkerEnabled);
		_btnPasteText.setEnabled(isMarkerEnabled);
		_btnPasteUrl.setEnabled(isMarkerEnabled);

		_comboLabelPosition.setEnabled(isMarkerEnabled);
		_comboMarkerName.setEnabled(isMarkerEnabled);

		// this do not work on win
		_groupText.setEnabled(isMarkerEnabled);
//		_groupImage.setEnabled(isMarkerEnabled);
		_groupUrl.setEnabled(isMarkerEnabled);

//		_imgTourImage.setEnabled(isMarkerEnabled);

		_lblDescription.setEnabled(isMarkerEnabled);
		_lblLabel.setEnabled(isMarkerEnabled);
		_lblLabelOffsetX.setEnabled(isMarkerEnabled);
		_lblLabelOffsetY.setEnabled(isMarkerEnabled);
		_lblLabelPosition.setEnabled(isMarkerEnabled);
//		_lblImageName.setEnabled(isMarkerEnabled);
		_lblLinkText.setEnabled(isMarkerEnabled);
		_lblLinkUrl.setEnabled(isMarkerEnabled);
//		_linkImage.setEnabled(isMarkerEnabled);

		_spinLabelOffsetX.setEnabled(isMarkerEnabled);
		_spinLabelOffsetY.setEnabled(isMarkerEnabled);

		_txtDescription.setEnabled(isMarkerEnabled);
		_txtUrlAddress.setEnabled(isMarkerEnabled);
		_txtUrlText.setEnabled(isMarkerEnabled);

	}

	private void fillContextMenu(final IMenuManager menuMgr) {

//		/*
//		 * Set menu items
//		 */
//		_signMenuManager.fillSignMenu(menuMgr);
//
//		/*
//		 * Enable actions
//		 */
//		final boolean isSignAvailable = _selectedTourMarker.getTourSign() != null;
//
//		_signMenuManager.setEnabledRemoveTourSignAction(isSignAvailable);

	}

	private void fillUI() {

		/*
		 * Fill position combos
		 */
		for (final String position : TourMarker.LABEL_POSITIONS) {
			_comboLabelPosition.add(position);
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

	/**
	 * Fires a selection to all opened views.
	 * 
	 * @param selection
	 */
	private void fireGlobalSelection(final ISelection selection) {

		PostSelectionProvider.fireSelection(selection);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		return _state;
//		return null;
	}

	TourChart getTourChart() {
		return _tourChart;
	}

//	private int getImageColumnWidth() {
//
//		int width;
//
//		if (_tcSignImage == null) {
//
//			width = IMAGE_DEFAULT_WIDTH;
//
//		} else {
//
//			width = _tcSignImage.getWidth();
//
//			if (width < IMAGE_MIN_WIDTH) {
//				width = IMAGE_MIN_WIDTH;
//			}
//		}
//
//		return width;
//	}
//
//	private int getRowHeight() {
//
//		return Math.min(ROW_MAX_HEIGHT, Math.max(ROW_DEFAULT_HEIGHT, _imageColumnWidth));
//	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_imagePaste = TourbookPlugin.getImageDescriptor(Messages.Image__App_Edit_Paste).createImage();

//		final int signImageMaxSize = TourMarker.getSignImageMaxSize(_pc);

		CONTENT_DEFAULT_WIDTH = _pc.convertWidthInCharsToPixels(30);

//		IMAGE_MIN_WIDTH = signImageMaxSize / 3;
//		IMAGE_DEFAULT_WIDTH = signImageMaxSize / 2;
//		ROW_MAX_HEIGHT = signImageMaxSize;

		restoreState_Viewer();
	}

	@Override
	protected void okPressed() {

		if (_selectedTourMarker.isValidForSave() == false) {
			return;
		}

		_isOkPressed = true;

		super.okPressed();
	}

	/**
	 * save marker modifications and update chart and viewer
	 */
	private void onChangeMarkerUI() {

		updateModel_FromUI(_selectedTourMarker);

		_tourChart.updateUI_MarkerLayer(true);

		_markerViewer.update(_selectedTourMarker, null);

		enableControls();
	}

//	private void onResizeImageColumn() {
//
//		final int imageColumnWidth = getImageColumnWidth();
//
//		// check if the width has changed
//		if (imageColumnWidth == _imageColumnWidth) {
//			return;
//		}
//
//		_imageColumnWidth = imageColumnWidth;
//
//		// update images
//		if (UI.IS_WIN) {
//			Hack.setTableItemHeight(_markerViewer.getTable(), getRowHeight());
//		}
//	}

//	private void onViewerPaint(final Event event) {
//
//		final TableItem item = (TableItem) event.item;
//		final Object itemData = item.getData();
//		if (itemData instanceof TourMarker) {
//
//			final TourSign tourSign = ((TourMarker) itemData).getTourSign();
//
//			if (tourSign != null) {
//
//				final Photo signPhoto = tourSign.getSignImagePhoto();
//				final ILoadCallBack imageLoadCallback = new LoadImageCallbackViewer(tourSign);
//				final Image signImage = SignManager.getSignImage(signPhoto, imageLoadCallback);
//
//				if (signImage != null && signImage.isDisposed() == false) {
//
//					final int photoPosX = event.x;
//					final int photoPosY = event.y;
//
//					switch (event.type) {
//					case SWT.MeasureItem:
//
//						if (UI.IS_WIN) {
//
//							// this is done with Hack.setTableItemHeight()
//
//						} else {
//
//							event.height = getRowHeight();
//						}
//
//						break;
//
//					case SWT.PaintItem:
//
//						final GC gc = event.gc;
//
//						final int imageCanvasWidth = Math.max(ROW_DEFAULT_HEIGHT, _imageColumnWidth);
//						final int imageCanvasHeight = event.height;
//
//						PhotoUI.paintPhotoImage(
//								gc,
//								signPhoto,
//								signImage,
//								photoPosX,
//								photoPosY,
//								imageCanvasWidth,
//								imageCanvasHeight,
//								SWT.CENTER,
//								null);
//
//						break;
//					}
//				}
//			}
//		}
//	}
//
//	private void redrawViewer() {
//
//		// !!! refresh() and update() do not repaint a loaded image but a redraw() do
//		_markerViewer.getTable().redraw();
//	}
//
//	@Override
//	public void removeTourSign() {
//
//		// update model
//		_selectedTourMarker.setTourSign(null);
//
//		// update UI
//		_lblImageName.setText(UI.EMPTY_STRING);
//		_imgTourImage.setImage(null);
//
//		redrawViewer();
//	}

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

	private void restoreState() {

		// restore width for the marker list
		final int leftPartWidth = Util.getStateInt(_state, //
				STATE_OUTER_SASH_WIDTH,
				_pc.convertWidthInCharsToPixels(80));

		_sashOuterForm.setViewerWidth(leftPartWidth);

		final int bottomPartHeight = Util.getStateInt(_state,//
				STATE_INNER_SASH_HEIGHT,
				_pc.convertWidthInCharsToPixels(10));

		_sashInnerForm.setFixedHeight(bottomPartHeight);
	}

	private void restoreState_Viewer() {

//		_imageColumnWidth = Util.getStateInt(_state, STATE_IMAGE_COLUMN_WIDTH, IMAGE_DEFAULT_WIDTH);
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
		_state.put(STATE_OUTER_SASH_WIDTH, _outerFixedPart.getSize().x);
		_state.put(STATE_INNER_SASH_HEIGHT, _innerFixedPart.getSize().y);

//		_state.put(STATE_IMAGE_COLUMN_WIDTH, _imageColumnWidth);
	}

//	@Override
//	public void setTourSign(final TourSign tourSign) {
//
//		_selectedTourMarker.setTourSign(tourSign);
//
//		updateUI_TourSign(_selectedTourMarker);
//
//		redrawViewer();
//
//		onChangeMarkerUI();
//	}

	@Override
	public void selectionChanged(final SelectionTourMarker tourMarkerSelection) {

		final ArrayList<TourMarker> selectedTourMarker = tourMarkerSelection.getSelectedTourMarker();

		// prevent that the x-slider is positioned in the tour chart
		_isSetXSlider = false;
		{
			_markerViewer.setSelection(new StructuredSelection(selectedTourMarker), true);
		}
		_isSetXSlider = true;

		_comboMarkerName.setFocus();

		fireGlobalSelection(tourMarkerSelection);
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

	@Override
	public void tourMarkerIsModified(final TourMarker tourMarker, final boolean isDeleted) {

		if (isDeleted) {

			deleteTourMarker(tourMarker);

		} else {

			// a tour marker is modified in the tour chart which is located in this dialog, update UI

			// controls
			updateUI_FromModel();

			// viewer
			_markerViewer.update(tourMarker, null);

			// chart
			_tourChart.updateUI_MarkerLayer(true);

			enableControls();
		}
	}

	private void updateModel_FromUI(final TourMarker tourMarker) {

		if (tourMarker == null) {
			return;
		}

		tourMarker.setMarkerVisible(_chkVisibility.getSelection());

		tourMarker.setLabel(_comboMarkerName.getText());
		tourMarker.setLabelPosition(_comboLabelPosition.getSelectionIndex());

		tourMarker.setLabelXOffset(_spinLabelOffsetX.getSelection());
		tourMarker.setLabelYOffset(_spinLabelOffsetY.getSelection());

		tourMarker.setDescription(_txtDescription.getText());
		tourMarker.setUrlAddress(_txtUrlAddress.getText());
		tourMarker.setUrlText(_txtUrlText.getText());
	}

	/**
	 * update marker ui from the selected marker
	 */
	private void updateUI_FromModel() {

		_isUpdateUI = true;
		{
			final boolean isTourMarker = _selectedTourMarker != null;

			if (isTourMarker) {

				// make the marker more visible by setting another type
				_selectedTourMarker.setVisibleType(ChartLabel.VISIBLE_TYPE_TYPE_EDIT);
			}

			_chkVisibility.setSelection(isTourMarker ? _selectedTourMarker.isMarkerVisible() : false);

			_comboMarkerName.setText(isTourMarker ? _selectedTourMarker.getLabel() : UI.EMPTY_STRING);
			_comboLabelPosition.select(isTourMarker ? _selectedTourMarker.getLabelPosition() : 0);

			_spinLabelOffsetX.setSelection(isTourMarker ? _selectedTourMarker.getLabelXOffset() : 0);
			_spinLabelOffsetY.setSelection(isTourMarker ? _selectedTourMarker.getLabelYOffset() : 0);

			_txtDescription.setText(isTourMarker ? _selectedTourMarker.getDescription() : UI.EMPTY_STRING);
			_txtUrlAddress.setText(isTourMarker ? _selectedTourMarker.getUrlAddress() : UI.EMPTY_STRING);
			_txtUrlText.setText(isTourMarker ? _selectedTourMarker.getUrlText() : UI.EMPTY_STRING);

			updateUI_TourSign(_selectedTourMarker);
		}
		_isUpdateUI = false;
	}

	private void updateUI_TourMarker(final TourMarker tourMarker, final Boolean isVisible) {

		tourMarker.setMarkerVisible(isVisible);

		// update UI
		_markerViewer.update(tourMarker, null);
		_tourChart.updateUI_MarkerLayer(true);
	}

	private void updateUI_TourSign(final TourMarker tourMarker) {

//		if (tourMarker == null || tourMarker.getTourSign() == null) {
//
//			_lblImageName.setText(UI.EMPTY_STRING);
//			_imgTourImage.setImage(null, false);
//
//		} else {
//
//			final TourSign tourSign = tourMarker.getTourSign();
//
//			_lblImageName.setText(tourSign.getSignName());
//
//			final Photo signPhoto = tourSign.getSignImagePhoto();
//			final ILoadCallBack imageLoadCallback = new LoadImageCallbackSelectedMarker(tourSign);
//			final Image tourSignImage = SignManager.getSignImage(signPhoto, imageLoadCallback);
//
//			_imgTourImage.setImage(tourSignImage, false);
//		}
	}

}
