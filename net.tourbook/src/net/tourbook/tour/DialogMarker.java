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
import net.tourbook.common.UI;
import net.tourbook.common.form.ViewerDetailForm;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class DialogMarker extends TitleAreaDialog {

	private static final String		DIALOG_SETTINGS_POSITION		= "marker_position";								//$NON-NLS-1$
	private static final String		DIALOG_SETTINGS_VIEWER_WIDTH	= "viewer_width";									//$NON-NLS-1$

	private static final int		OFFSET_PAGE_INCREMENT			= 20;

	private static final int		OFFSET_MAX						= 198;
	private static final int		OFFSET_0						= OFFSET_MAX / 2;

	private final IDialogSettings	_state							= TourbookPlugin
																			.getDefault()
																			.getDialogSettingsSection("DialogMarker");	//$NON-NLS-1$

	private TourChart				_tourChart;
	private TourData				_tourData;

	/**
	 * marker which is currently selected
	 */
	private TourMarker				_selectedTourMarker;

	/**
	 * backup for the selected tour marker
	 */
	private TourMarker				_backupMarker					= new TourMarker();

	private Set<TourMarker>			_tourMarkers;
	private HashSet<TourMarker>		_tourMarkersBackup;

	/**
	 * initial tour marker
	 */
	private TourMarker				_initialTourMarker;

	/*
	 * none UI
	 */
	private boolean					_isOkPressed					= false;
	private PixelConverter			_pc;

	private NumberFormat			_nf3							= NumberFormat.getNumberInstance();
	{
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	/*
	 * UI controls
	 */
	private TableViewer				_markerViewer;

	private Combo					_comboMarkerName;
	private Combo					_comboMarkerPosition;

	private Scale					_scaleX;
	private Label					_lblXValue;

	private Scale					_scaleY;
	private Label					_lblYValue;

	private ViewerDetailForm		_viewerDetailForm;

	private Composite				_markerListContainer;

	private Button					_btnDelete;
	private Button					_btnHideAll;
	private Button					_btnReset;
	private Button					_btnShowAll;
	private Button					_btnUndo;

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

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

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
				_markerListContainer = createUI_10_MarkerList(dlgContainer);

				// sash
				final Sash sash = new Sash(dlgContainer, SWT.VERTICAL);
				net.tourbook.common.UI.addSashColorHandler(sash);

				// right part
				final Composite markerDetailContainer = new Composite(dlgContainer, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(markerDetailContainer);
				GridLayoutFactory.fillDefaults().applyTo(markerDetailContainer);
//				markerDetailContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				{
					createUI_30_MarkerDetails(markerDetailContainer);
					createUI_40_TourChart(markerDetailContainer);
					createUI_50_Offset(markerDetailContainer);
				}

				_viewerDetailForm = new ViewerDetailForm(
						dlgContainer,
						_markerListContainer,
						sash,
						markerDetailContainer,
						30);

			}
			createUI_80_Actions(marginContainer);
		}
	}

	/**
	 * container: marker list
	 */
	private Composite createUI_10_MarkerList(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//

				// set vertical position to the marker label position
				.margins(0, 4)
				.applyTo(container);
		{
			// label: markers
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dlg_TourMarker_Label_markers);
//			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			createUI_15_MarkerViewer(container);
		}

		return container;
	}

	private Composite createUI_15_MarkerViewer(final Composite parent) {

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		layoutContainer.setLayout(tableLayout);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);

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

		_markerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		defineColumn_1stHidden(tableLayout);
		defineColumn_Distance(tableLayout);
		defineColumn_IsVisible(tableLayout);
		defineColumn_Marker(tableLayout);
		defineColumn_OffsetX(tableLayout);
		defineColumn_OffsetY(tableLayout);

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

	private void createUI_30_MarkerDetails(final Composite parent) {

		Label label;
		/*
		 * container: marker details
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
		// markerSettingsContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dlg_TourMarker_Label_marker_name);

			/*
			 * marker name
			 */
			_comboMarkerName = new Combo(container, SWT.BORDER | SWT.FLAT);
			_comboMarkerName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent e) {
					onChangeMarkerUI();
				}
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboMarkerName);
//			_comboMarkerName.setText(UI.EMPTY_STRING);

			// fill combobox
			final TreeSet<String> dbTitles = TourDatabase.getAllTourMarkerNames();
			for (final String title : dbTitles) {
				_comboMarkerName.add(title);
			}

			new AutocompleteComboInput(_comboMarkerName);

			/*
			 * marker position
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Dlg_TourMarker_Label_position);

			_comboMarkerPosition = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboMarkerPosition.setVisibleItemCount(20);
			_comboMarkerPosition.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeMarkerUI();
				}
			});

			// fill position combo
			for (final String position : TourMarker.visualPositionLabels) {
				_comboMarkerPosition.add(position);
			}
		}
	}

	/**
	 * create tour chart with new marker
	 */
	private void createUI_40_TourChart(final Composite parent) {

		_tourChart = new TourChart(parent, SWT.BORDER, true);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(600, 350)
//				.minSize(600, 200)
				.indent(0, 4)
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

	/**
	 * offset container
	 */
	private void createUI_50_Offset(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
		{
			/*
			 * x-offset
			 */
			// label
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Dlg_TourMarker_Label_horizontal_offset);

			// scale
			_scaleX = new Scale(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(convertWidthInCharsToPixels(20), SWT.DEFAULT)
					.minSize(convertWidthInCharsToPixels(20), SWT.DEFAULT)
					.applyTo(_scaleX);
			_scaleX.setMinimum(0);
			_scaleX.setMaximum(OFFSET_MAX);
			_scaleX.setPageIncrement(OFFSET_PAGE_INCREMENT);
			_scaleX.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeMarkerUI();
				}
			});
			_scaleX.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent e) {
					onChangeMarkerUI();
				}
			});

			// label: x value
			_lblXValue = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.hint(convertWidthInCharsToPixels(5), SWT.DEFAULT)
					.applyTo(_lblXValue);

			/*
			 * y-offset
			 */
			// label
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).indent(5, 0).applyTo(label);
			label.setText(Messages.Dlg_TourMarker_Label_vertical_offset);

			// scale
			_scaleY = new Scale(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(convertWidthInCharsToPixels(20), SWT.DEFAULT)
					.minSize(convertWidthInCharsToPixels(20), SWT.DEFAULT)
					.applyTo(_scaleY);
			_scaleY.setMinimum(0);
			_scaleY.setMaximum(OFFSET_MAX);
			_scaleY.setPageIncrement(OFFSET_PAGE_INCREMENT);
			_scaleY.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeMarkerUI();
				}
			});
			_scaleY.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent e) {
					onChangeMarkerUI();
				}
			});

			// label: y value
			_lblYValue = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.CENTER)
					.hint(convertWidthInCharsToPixels(5), SWT.DEFAULT)
					.applyTo(_lblYValue);
		}
	}

	private void createUI_80_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI_90_MarkerListActions(container);
			createUI_92_MarkerActions(container);
		}
	}

	private void createUI_90_MarkerListActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
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
					updateMarkerUI();
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

	private void createUI_92_MarkerActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(false, false)
				.align(SWT.END, SWT.FILL)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{

			/*
			 * button: reset offset
			 */
			_btnReset = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnReset);
			_btnReset.setText(Messages.Dlg_TourMarker_Button_reset_offset);
			_btnReset.setToolTipText(Messages.Dlg_TourMarker_Button_reset_offset_tooltip);
			_btnReset.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					_scaleX.setSelection(OFFSET_0);
					_scaleY.setSelection(OFFSET_0);

					onChangeMarkerUI();
				}
			});
			setButtonLayoutData(_btnReset);
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
	 * column: distance km/mi
	 */
	private void defineColumn_Distance(final TableColumnLayout tableLayout) {

		final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
		final TableColumn tc = tvc.getColumn();

		tc.setText(net.tourbook.ui.UI.UNIT_LABEL_DISTANCE);
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

		if (isMarkerAvailable) {
			final boolean isScale0 = (_scaleX.getSelection() - OFFSET_0) == 0
					&& (_scaleY.getSelection() - OFFSET_0) == 0;
			_btnReset.setEnabled(!isScale0);
		} else {
			_btnReset.setEnabled(false);
		}

		_btnShowAll.setEnabled(isMarkerAvailable);
		_btnHideAll.setEnabled(isMarkerAvailable);
		_comboMarkerName.setEnabled(isMarkerAvailable);
		_comboMarkerPosition.setEnabled(isMarkerAvailable);
		_scaleX.setEnabled(isMarkerAvailable);
		_scaleY.setEnabled(isMarkerAvailable);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return _state;
//		return null;
	}

	TourChart getTourChart() {
		return _tourChart;
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

		updateMarkerFromUI(_selectedTourMarker);

		_lblXValue.setText(Integer.toString(_scaleX.getSelection() - OFFSET_0));
		_lblYValue.setText(Integer.toString(_scaleY.getSelection() - OFFSET_0));

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

	private void onSelectMarker(final TourMarker newSelectedMarker) {

		if (newSelectedMarker == null) {
			return;
		}

		// save values for previous marker
		if (_selectedTourMarker != null && newSelectedMarker != _selectedTourMarker) {
			updateMarkerFromUI(_selectedTourMarker);
			restoreVisibleType();
		}

		// set new selected marker
		_selectedTourMarker = newSelectedMarker;

		// make a backup of the marker to undo modifications
		_selectedTourMarker.setMarkerBackup(_backupMarker);

		updateMarkerUI();
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

	private void restoreState() {

		// restore width for the marker list when the width is available
		try {
			_viewerDetailForm.setViewerWidth(_state.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			//
		}
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
		_state.put(DIALOG_SETTINGS_VIEWER_WIDTH, _markerListContainer.getSize().x);
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

	private void updateMarkerFromUI(final TourMarker tourMarker) {

		if (tourMarker == null) {
			return;
		}

		tourMarker.setLabel(_comboMarkerName.getText());
		tourMarker.setVisualPosition(_comboMarkerPosition.getSelectionIndex());

		tourMarker.setLabelXOffset(_scaleX.getSelection() - OFFSET_0);
		tourMarker.setLabelYOffset(_scaleY.getSelection() - OFFSET_0);
	}

	/**
	 * update marker ui from the selected marker
	 */
	private void updateMarkerUI() {

		// make the marker more visible by setting another type
		_selectedTourMarker.setVisibleType(ChartLabel.VISIBLE_TYPE_TYPE_EDIT);

		_comboMarkerName.setText(_selectedTourMarker.getLabel());
		_comboMarkerPosition.select(_selectedTourMarker.getVisualPosition());

		_scaleX.setSelection(_selectedTourMarker.getLabelXOffset() + OFFSET_0);
		_scaleY.setSelection(_selectedTourMarker.getLabelYOffset() + OFFSET_0);
	}

}
