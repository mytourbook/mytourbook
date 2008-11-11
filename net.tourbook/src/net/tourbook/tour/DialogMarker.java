/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.eclipse.swt.widgets.Text;

public class DialogMarker extends TitleAreaDialog {

	private static final String	DIALOG_SETTINGS_POSITION		= "marker_position";				//$NON-NLS-1$
	private static final String	DIALOG_SETTINGS_VIEWER_WIDTH	= "viewer_width";					//$NON-NLS-1$

	private static final int	COLUMN_DISTANCE					= 0;
	private static final int	COLUMN_MARKER_LABEL				= 1;
	private static final int	COLUMN_X_OFFSET					= 2;
	private static final int	COLUMN_Y_OFFSET					= 3;

	private static final int	OFFSET_PAGE_INCREMENT			= 20;

	private static final int	OFFSET_MAX						= 198;
	private static final int	OFFSET_0						= OFFSET_MAX / 2;

	private TourChart			fTourChart;
	private TourData			fTourData;

	/**
	 * marker which is currently selected
	 */
	private TourMarker			fSelectedTourMarker;

	/**
	 * backup for the selected tour marker
	 */
	private TourMarker			fBackupMarker					= new TourMarker();

	/**
	 * initial tour marker
	 */
	private TourMarker			fInitialTourMarker;

	private TableViewer			fMarkerViewer;

	private Text				fTextMarkerName;
	private Combo				fComboMarkerPosition;

	private Scale				fScaleX;
	private Label				fLabelXValue;

	private Scale				fScaleY;
	private Label				fLabelYValue;

	private Composite			fOffsetContainer;
	private ViewerDetailForm	fViewerDetailForm;

	private Composite			fMarkerListContainer;

	private Button				fBtnDelete;
	private Button				fBtnUndo;
	private Button				fBtnReset;

	private NumberFormat		fNF								= NumberFormat.getNumberInstance();

	private HashSet<TourMarker>	fTourMarkersBackup;

	private boolean				fIsOkPressed					= false;

	private class MarkerViewerContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			if (fTourData == null) {
				return new Object[0];
			} else {
				return fTourData.getTourMarkers().toArray();
			}
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class MarkerViewerLabelProvider extends CellLabelProvider {

		@Override
		public void update(final ViewerCell cell) {

			final TourMarker tourMarker = (TourMarker) cell.getElement();

			switch (cell.getColumnIndex()) {

			case COLUMN_DISTANCE:
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format((tourMarker.getDistance()) / (1000 * UI.UNIT_VALUE_DISTANCE)));

				if (tourMarker.getType() == ChartMarker.MARKER_TYPE_DEVICE) {
					cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
				break;

			case COLUMN_MARKER_LABEL:
				cell.setText(tourMarker.getLabel());
				break;

			case COLUMN_X_OFFSET:
				cell.setText(Integer.toString(tourMarker.getLabelXOffset()));
				break;

			case COLUMN_Y_OFFSET:
				cell.setText(Integer.toString(tourMarker.getLabelYOffset()));
				break;

			default:
				break;
			}
		}
	}

	/**
	 * Sort the markers by time
	 */
	private class MarkerViewerSorter extends ViewerSorter {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
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

		fTourData = tourData;

		/*
		 * make a backup copy of the tour markers, modify the original data so that the tour chart
		 * displays the modifications
		 */
		fTourMarkersBackup = new HashSet<TourMarker>();
		for (final TourMarker tourMarker : fTourData.getTourMarkers()) {
			fTourMarkersBackup.add(tourMarker.clone());
		}

		fInitialTourMarker = initialTourMarker;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	public void addTourMarker(final TourMarker newTourMarker) {

		if (newTourMarker == null) {
			return;
		}
		
		// update data model, add new marker to the marker list
		fTourData.getTourMarkers().add(newTourMarker);

		// update the viewer and select the new marker
		fMarkerViewer.refresh();
		fMarkerViewer.setSelection(new StructuredSelection(newTourMarker), true);

		fTextMarkerName.selectAll();
		fTextMarkerName.setFocus();

		// update chart
		fTourChart.updateMarkerLayer(true);
	}

	@Override
	public boolean close() {

		if (fIsOkPressed) {

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
			fTourData.setTourMarkers(fTourMarkersBackup);
		}

		saveDialogSettings();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);
		shell.setText(Messages.Dlg_TourMarker_Dlg_title);

		/*
		 * don't close the dialog when the enter key is pressed, except when the close button has
		 * the focus
		 */
//		shell.addTraverseListener(new TraverseListener() {
//			public void keyTraversed(final TraverseEvent e) {
//				if (e.detail == SWT.TRAVERSE_RETURN) {
//					e.detail = SWT.TRAVERSE_TAB_NEXT;
//					e.doit = true;
//				}
//			}
//		});
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		restoreDialogSettings();

		setTitle(Messages.Dlg_TourMarker_Dlg_title);
		setMessage(Messages.Dlg_TourMarker_Dlg_Message);

		// update marker viewer
		fMarkerViewer.setInput(this);

		if (fInitialTourMarker == null) {
			// select first marker if any are available
			final Object firstElement = fMarkerViewer.getElementAt(0);
			if (firstElement != null) {
				fMarkerViewer.setSelection(new StructuredSelection(firstElement), true);
			}
		} else {
			// select initial tour marker
			fMarkerViewer.setSelection(new StructuredSelection(fInitialTourMarker), true);
		}

		fTextMarkerName.selectAll();
		fTextMarkerName.setFocus();

		enableControls();

		return dlgAreaContainer;
	}

	private Composite createMarkerViewer(final Composite parent) {

		final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		tableLayouter.setLayoutData(gd);

		/*
		 * create table
		 */
		final Table table = new Table(tableLayouter, SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fMarkerViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		final PixelConverter pixelConverter = new PixelConverter(table);

		final MarkerViewerLabelProvider labelProvider = new MarkerViewerLabelProvider();

		// column: distance km/mi
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(UI.UNIT_LABEL_DISTANCE);
		tvc.getColumn().setToolTipText(Messages.Tour_Marker_Column_km_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(8), false));

		// column: marker
		tvc = new TableViewerColumn(fMarkerViewer, SWT.LEAD);
		tvc.getColumn().setText(Messages.Tour_Marker_Column_remark);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnWeightData(1, true));

		// column: horizontal offset
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(Messages.Tour_Marker_Column_horizontal_offset);
		tvc.getColumn().setToolTipText(Messages.Tour_Marker_Column_horizontal_offset_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(6), false));

		// column: vertical offset
		tvc = new TableViewerColumn(fMarkerViewer, SWT.TRAIL);
		tvc.getColumn().setText(Messages.Tour_Marker_Column_vertical_offset);
		tvc.getColumn().setToolTipText(Messages.Tour_Marker_Column_vertical_offset_tooltip);
		tvc.setLabelProvider(labelProvider);
		tableLayouter.addColumnData(new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(6), false));

		/*
		 * create table viewer
		 */

		fMarkerViewer.setContentProvider(new MarkerViewerContentProvicer());
		fMarkerViewer.setLabelProvider(labelProvider);
		fMarkerViewer.setSorter(new MarkerViewerSorter());

		fMarkerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					onSelectMarker((TourMarker) selection.getFirstElement());
				}
			}
		});

		fMarkerViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				fTextMarkerName.setFocus();
				fTextMarkerName.selectAll();
			}
		});

		return tableLayouter;
	}

	/**
	 * create tour chart with new marker
	 */
	private void createTourChart(final Composite parent) {

		fTourChart = new TourChart(parent, SWT.BORDER, true);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setContextProvider(new DialogMarkerTourChartContextProvicer(this));

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		gd.heightHint = 400;
		gd.verticalIndent = 5;
		fTourChart.setLayoutData(gd);

		// set title
		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

//		TourChartConfiguration chartConfig = new TourChartConfiguration(false);
//		chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

		final TourChartConfiguration chartConfig = TourManager.createTourChartConfiguration();
		fTourChart.updateTourChart(fTourData, chartConfig, false);
	}

	private void createUI(final Composite parent) {

		Label label;
		GridLayout gl;
		GridData gd;

		final Composite dlgMarginContainer = new Composite(parent, SWT.NONE);
		dlgMarginContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dlgMarginContainer.setLayout(new GridLayout());

		final Composite dlgContainer = new Composite(dlgMarginContainer, SWT.NONE);
		dlgContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		fMarkerListContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout(3, false);
		fMarkerListContainer.setLayout(gl);

		final Sash sash = new Sash(dlgContainer, SWT.VERTICAL);

		final Composite markerDetailContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout();
		markerDetailContainer.setLayout(gl);
		// markerDetailContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		fViewerDetailForm = new ViewerDetailForm(dlgContainer, fMarkerListContainer, sash, markerDetailContainer, 30);

		/*
		 * container: marker list
		 */
		label = new Label(fMarkerListContainer, SWT.NONE);
		label.setText(Messages.Dlg_TourMarker_Label_markers);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		createMarkerViewer(fMarkerListContainer);

		// button: delete
		fBtnDelete = new Button(fMarkerListContainer, SWT.NONE);
		fBtnDelete.setText(Messages.Dlg_TourMarker_Button_delete);
		fBtnDelete.setToolTipText(Messages.Dlg_TourMarker_Button_delete_tooltip);
		fBtnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onDeleteMarker();
			}
		});
		setButtonLayoutData(fBtnDelete);

		// button: undo
		fBtnUndo = new Button(fMarkerListContainer, SWT.NONE);
		fBtnUndo.getLayoutData();
		fBtnUndo.setText(Messages.Dlg_TourMarker_Button_undo);
		fBtnUndo.setToolTipText(Messages.Dlg_TourMarker_Button_undo_tooltip);
		fBtnUndo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fSelectedTourMarker.restoreMarkerFromBackup(fBackupMarker);
				updateMarkerUI();
				onChangeMarkerUI();
			}
		});
		setButtonLayoutData(fBtnUndo);

		/*
		 * container: marker details
		 */
		final Composite markerSettingsContainer = new Composite(markerDetailContainer, SWT.NONE);
		gl = new GridLayout(4, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		markerSettingsContainer.setLayout(gl);
		markerSettingsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		// markerSettingsContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		/*
		 * marker name
		 */
		label = new Label(markerSettingsContainer, SWT.NONE);
		label.setText(Messages.Dlg_TourMarker_Label_marker_name);

		fTextMarkerName = new Text(markerSettingsContainer, SWT.BORDER);

		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.minimumWidth = convertWidthInCharsToPixels(10);
		gd.widthHint = convertWidthInCharsToPixels(30);
		fTextMarkerName.setLayoutData(gd);

		fTextMarkerName.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				onChangeMarkerUI();
			}
		});

//		fTextMarkerName.addTraverseListener(new TraverseListener() {
//			public void keyTraversed(final TraverseEvent e) {
//				if (e.detail == SWT.TRAVERSE_RETURN) {
//					fMarkerViewer.getTable().setFocus();
//				}
//			}
//		});

		/*
		 * marker position
		 */
		label = new Label(markerSettingsContainer, SWT.NONE);
		label.setText(Messages.Dlg_TourMarker_Label_position);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.horizontalIndent = 10;
		label.setLayoutData(gd);

		fComboMarkerPosition = new Combo(markerSettingsContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboMarkerPosition.setVisibleItemCount(20);

		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.minimumWidth = convertWidthInCharsToPixels(10);
		gd.widthHint = convertWidthInCharsToPixels(10);
		fComboMarkerPosition.setLayoutData(gd);

		fComboMarkerPosition.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeMarkerUI();
			}
		});

		// fill position combo
		for (final String position : TourMarker.visualPositionLabels) {
			fComboMarkerPosition.add(position);
		}

		createTourChart(markerSettingsContainer);

		/*
		 * offset container
		 */
		fOffsetContainer = new Composite(markerDetailContainer, SWT.NONE);
		fOffsetContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		// fOffsetContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		gl = new GridLayout(7, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 0;
		fOffsetContainer.setLayout(gl);

		/*
		 * x-offset
		 */
		label = new Label(fOffsetContainer, SWT.NONE);
		label.setText(Messages.Dlg_TourMarker_Label_horizontal_offset);

		fScaleX = new Scale(fOffsetContainer, SWT.NONE);
		fScaleX.setMinimum(0);
		fScaleX.setMaximum(OFFSET_MAX);
		fScaleX.setPageIncrement(OFFSET_PAGE_INCREMENT);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 100;
		fScaleX.setLayoutData(gd);
		fScaleX.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeMarkerUI();
			}
		});
		fScaleX.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent e) {
				onChangeMarkerUI();
			}
		});

		fLabelXValue = new Label(fOffsetContainer, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.widthHint = convertWidthInCharsToPixels(5);
		fLabelXValue.setLayoutData(gd);

		/*
		 * y-offset
		 */
		label = new Label(fOffsetContainer, SWT.NONE);
		label.setText(Messages.Dlg_TourMarker_Label_vertical_offset);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.horizontalIndent = 5;
		label.setLayoutData(gd);

		fScaleY = new Scale(fOffsetContainer, SWT.NONE);
		fScaleY.setMinimum(0);
		fScaleY.setMaximum(OFFSET_MAX);
		fScaleY.setPageIncrement(OFFSET_PAGE_INCREMENT);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.widthHint = 100;
		fScaleY.setLayoutData(gd);
		fScaleY.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeMarkerUI();
			}
		});
		fScaleY.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent e) {
				onChangeMarkerUI();
			}
		});

		fLabelYValue = new Label(fOffsetContainer, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.widthHint = convertWidthInCharsToPixels(5);
		fLabelYValue.setLayoutData(gd);

		/*
		 * button: reset offset
		 */
		fBtnReset = new Button(fOffsetContainer, SWT.NONE);
		fBtnReset.setText(Messages.Dlg_TourMarker_Button_reset_offset);
		fBtnReset.setToolTipText(Messages.Dlg_TourMarker_Button_reset_offset_tooltip);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.horizontalIndent = 5;
		fBtnReset.setLayoutData(gd);

		fBtnReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				fScaleX.setSelection(OFFSET_0);
				fScaleY.setSelection(OFFSET_0);

				onChangeMarkerUI();
			}
		});
		setButtonLayoutData(fBtnReset);
	}

	@SuppressWarnings("unchecked")
	private void enableControls() {

		if (fSelectedTourMarker != null) {
			fBtnUndo.setEnabled(fSelectedTourMarker.compareTo(fBackupMarker, true) == false);
		} else {
			fBtnUndo.setEnabled(false);
		}

		/*
		 * button: delete marker
		 */
		boolean isCustomMarker = false;
		final IStructuredSelection markerSelection = (IStructuredSelection) fMarkerViewer.getSelection();

		// check if custom markers are selected
		for (final Iterator<TourMarker> iter = markerSelection.iterator(); iter.hasNext();) {
			final TourMarker tourMarker = iter.next();
			if (tourMarker.getType() != ChartMarker.MARKER_TYPE_DEVICE) {
				isCustomMarker = true;
				break;
			}
		}
		fBtnDelete.setEnabled(isCustomMarker);

		final boolean isMarkerAvailable = fMarkerViewer.getTable().getItemCount() != 0;

		if (isMarkerAvailable) {
			final boolean isScale0 = (fScaleX.getSelection() - OFFSET_0) == 0
					&& (fScaleY.getSelection() - OFFSET_0) == 0;
			fBtnReset.setEnabled(!isScale0);
		} else {
			fBtnReset.setEnabled(false);
		}

		fTextMarkerName.setEnabled(isMarkerAvailable);
		fComboMarkerPosition.setEnabled(isMarkerAvailable);
		fScaleX.setEnabled(isMarkerAvailable);
		fScaleY.setEnabled(isMarkerAvailable);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	/**
	 * @return Returns the dialog settings for this dialog
	 */
	private IDialogSettings getDialogSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	TourChart getTourChart() {
		return fTourChart;
	}

	@Override
	protected void okPressed() {

		fIsOkPressed = true;

		super.okPressed();
	}

	/**
	 * save marker modifications and update chart and viewer
	 */
	private void onChangeMarkerUI() {

		updateMarkerFromUI(fSelectedTourMarker);

		fLabelXValue.setText(Integer.toString(fScaleX.getSelection() - OFFSET_0));
		fLabelYValue.setText(Integer.toString(fScaleY.getSelection() - OFFSET_0));

		fTourChart.updateMarkerLayer(true);

		fMarkerViewer.update(fSelectedTourMarker, null);

		enableControls();
	}

	/**
	 * remove selected markers from the view and update dependened structures
	 */
	private void onDeleteMarker() {

		final IStructuredSelection markerSelection = (IStructuredSelection) fMarkerViewer.getSelection();
		final TourMarker selectedMarker = (TourMarker) markerSelection.getFirstElement();

		// confirm to save the changes
		final MessageBox msgBox = new MessageBox(fTourChart.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		msgBox.setText(Messages.Dlg_TourMarker_MsgBox_delete_marker_title);
		msgBox.setMessage(NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (selectedMarker).getLabel()));

		if (msgBox.open() == SWT.YES) {

			// get index for selected marker
			final int lastMarkerIndex = fMarkerViewer.getTable().getSelectionIndex();

			// update data model
			fTourData.getTourMarkers().remove(selectedMarker);

			// update the viewer
			fMarkerViewer.remove(selectedMarker);

			// update chart
			fTourChart.updateMarkerLayer(true);

			// select next marker
			TourMarker nextMarker = (TourMarker) fMarkerViewer.getElementAt(lastMarkerIndex);
			if (nextMarker == null) {
				nextMarker = (TourMarker) fMarkerViewer.getElementAt(lastMarkerIndex - 1);
			}

			if (nextMarker == null) {
				// disable controls when no marker is available
				enableControls();
			} else {
				fMarkerViewer.setSelection(new StructuredSelection(nextMarker), true);
			}

		}
	}

	private void onSelectMarker(final TourMarker newSelectedMarker) {

		if (newSelectedMarker == null) {
			return;
		}

		// save values for previous marker
		if (fSelectedTourMarker != null && newSelectedMarker != fSelectedTourMarker) {
			updateMarkerFromUI(fSelectedTourMarker);
			restoreVisibleType();
		}

		// set new selected marker
		fSelectedTourMarker = newSelectedMarker;

		// make a backup of the marker to undo modifications
		fSelectedTourMarker.setMarkerBackup(fBackupMarker);

		updateMarkerUI();
		onChangeMarkerUI();

		// set slider position
		fTourChart.setXSliderPosition(new SelectionChartXSliderPosition(fTourChart,
				newSelectedMarker.getSerieIndex(),
				SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
	}

	private void restoreDialogSettings() {

		final IDialogSettings dlgSettings = getDialogSettings();

		// restore width for the marker list when the width is available
		try {
			fViewerDetailForm.setViewerWidth(dlgSettings.getInt(DIALOG_SETTINGS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {}
	}

	/**
	 * restore type from the backup for the currently selected tour marker
	 */
	private void restoreVisibleType() {

		if (fSelectedTourMarker == null) {
			return;
		}

		fSelectedTourMarker.setVisibleType(ChartMarker.VISIBLE_TYPE_DEFAULT);
	}

	private void saveDialogSettings() {

		final IDialogSettings dlgSettings = getDialogSettings();

		dlgSettings.put(DIALOG_SETTINGS_POSITION, fComboMarkerPosition.getSelectionIndex());
		dlgSettings.put(DIALOG_SETTINGS_VIEWER_WIDTH, fMarkerListContainer.getSize().x);
	}

	private void updateMarkerFromUI(final TourMarker tourMarker) {

		if (tourMarker == null) {
			return;
		}

		tourMarker.setLabel(fTextMarkerName.getText().trim());
		tourMarker.setVisualPosition(fComboMarkerPosition.getSelectionIndex());

		tourMarker.setLabelXOffset(fScaleX.getSelection() - OFFSET_0);
		tourMarker.setLabelYOffset(fScaleY.getSelection() - OFFSET_0);
	}

	/**
	 * update marker ui from the selected marker
	 */
	private void updateMarkerUI() {

		// make the marker more visible by setting another type
		fSelectedTourMarker.setVisibleType(ChartMarker.VISIBLE_TYPE_TYPE_EDIT);

		fTextMarkerName.setText(fSelectedTourMarker.getLabel());
		fComboMarkerPosition.select(fSelectedTourMarker.getVisualPosition());

		fScaleX.setSelection(fSelectedTourMarker.getLabelXOffset() + OFFSET_0);
		fScaleY.setSelection(fSelectedTourMarker.getLabelYOffset() + OFFSET_0);
	}

}
