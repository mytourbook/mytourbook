/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation; either version 2 of the License, or (at your option) any later 
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartMarker;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class MarkerDialog extends Dialog {

	private static final int				OFFSET_PAGE_INCREMENT		= 10;

	private static final int				OFFSET_MAX					= 200;
	private static final int				OFFSET_0					= OFFSET_MAX / 2;

	private static final String				DIALOG_SETTINGS_POSITION	= "marker_position";

	private final TourChartContextProvider	fTourChartContextProvider;

	public Text								fTextMarkerName;
	public Combo							fComboMarkerPosition;

	private TourChart						fTourChart;
	private TourData						fTourData;
	private TourMarker						fTempTourMarker;

	private boolean							fSaveNewMarker;

	private Scale							fScaleX;
	private Label							fLabelXValue;

	private Scale							fScaleY;

	private Label							fLabelYValue;

	private Composite						fOffsetContainer;

	protected MarkerDialog(TourChartContextProvider tourChartContextProvider, Shell parentShell) {

		super(parentShell);

		fTourChartContextProvider = tourChartContextProvider;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	public boolean close() {

		if (fSaveNewMarker == false) {
			// remove temp marker
			fTourData.getTourMarkers().remove(fTempTourMarker);
		}

		saveDialogSettings();

		return super.close();
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.TourMap_Dlg_add_marker_title);
	}

	private void createContent(Composite dlgArea) {

		Label label;
		GridLayout gl;
		GridData gd;

		Composite dlgContainer = new Composite(dlgArea, SWT.NONE);
		gl = new GridLayout(4, false);
		dlgContainer.setLayout(gl);
		dlgContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		/*
		 * marker name
		 */
		label = new Label(dlgContainer, SWT.NONE);
		label.setText(Messages.TourMap_Dlg_add_marker_label);

		fTextMarkerName = new Text(dlgContainer, SWT.BORDER);

		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.widthHint = convertWidthInCharsToPixels(30);
		fTextMarkerName.setLayoutData(gd);

		fTextMarkerName.setText("<new marker>");
		fTextMarkerName.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				updateMarker();
			}
		});

		/*
		 * marker position
		 */
		label = new Label(dlgContainer, SWT.NONE);
		label.setText("&Position:");
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.horizontalIndent = 10;
		label.setLayoutData(gd);

		fComboMarkerPosition = new Combo(dlgContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboMarkerPosition.setVisibleItemCount(20);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.widthHint = convertWidthInCharsToPixels(30);
		fComboMarkerPosition.setLayoutData(gd);
		fComboMarkerPosition.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateMarker();
			}
		});

		// fill marker combo
		for (String position : TourMarker.visualPositionLabels) {
			fComboMarkerPosition.add(position);
		}

		createTourChart(dlgContainer);

		/*
		 * offset container
		 */
		fOffsetContainer = new Composite(dlgArea, SWT.NONE);
		gl = new GridLayout(7, false);
		fOffsetContainer.setLayout(gl);
		// fOffsetContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		/*
		 * x-offset
		 */
		label = new Label(fOffsetContainer, SWT.NONE);
		label.setText("&Horizontal Offset:");

		fScaleX = new Scale(fOffsetContainer, SWT.NONE);
		fScaleX.setMinimum(0);
		fScaleX.setMaximum(OFFSET_MAX);
		fScaleX.setPageIncrement(OFFSET_PAGE_INCREMENT);
		fScaleX.setSelection(OFFSET_0);
		fScaleX.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateXOffset();
				updateMarker();
			}
		});
		fScaleX.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent e) {
				updateXOffset();
				updateMarker();
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
		label.setText("&Vertical Offset:");
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.horizontalIndent = 5;
		label.setLayoutData(gd);

		fScaleY = new Scale(fOffsetContainer, SWT.NONE);
		fScaleY.setMinimum(0);
		fScaleY.setMaximum(OFFSET_MAX);
		fScaleY.setPageIncrement(OFFSET_PAGE_INCREMENT);
		fScaleY.setSelection(OFFSET_0);
		fScaleY.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateYOffset();
				updateMarker();
			}
		});
		fScaleY.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent e) {
				updateYOffset();
				updateMarker();
			}
		});

		fLabelYValue = new Label(fOffsetContainer, SWT.NONE);
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.widthHint = convertWidthInCharsToPixels(5);
		fLabelYValue.setLayoutData(gd);

		/*
		 * button: reset offset
		 */
		Button btnReset = new Button(fOffsetContainer, SWT.NONE);
		btnReset.setText("Reset Offset");
		btnReset.setToolTipText("Set horizontal and vertical position to 0");
		gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
		gd.horizontalIndent = 20;
		btnReset.setLayoutData(gd);
		btnReset.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onResetOffset();
			}
		});
	}

	private void onResetOffset() {

		fScaleX.setSelection(OFFSET_0);
		updateXOffset();

		fScaleY.setSelection(OFFSET_0);
		updateYOffset();

		updateMarker();
	}

	/**
	 * create tour chart with new marker
	 */
	private void createTourChart(Composite parent) {

		fTourChart = new TourChart(parent, SWT.BORDER, false);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1);
		gd.minimumHeight = 200;
		gd.minimumWidth = 400;
		gd.verticalIndent = 5;
		fTourChart.setLayoutData(gd);

		// set title
		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(ChartDataModel changedChartDataModel) {
				changedChartDataModel.setTitle(TourManager.getTourDate(fTourData));
			}
		});

		TourChartConfiguration chartConfig = new TourChartConfiguration();
		chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

		fTourData = fTourChartContextProvider.fView.getTourChart().getTourData();
		int serieIndex = fTourChartContextProvider.fSlider.getValuesIndex();

		// create temp marker
		fTempTourMarker = new TourMarker(fTourData, ChartMarker.MARKER_TYPE_TEMP);
		fTempTourMarker.setSerieIndex(serieIndex);
		fTempTourMarker.setDistance(fTourData.distanceSerie[serieIndex]);
		fTempTourMarker.setTime(fTourData.timeSerie[serieIndex]);
		fTempTourMarker.setLabel(fTextMarkerName.getText().trim());
		fTempTourMarker.setVisualPosition(fComboMarkerPosition.getSelectionIndex());

		// add new marker to the marker list
		fTourData.getTourMarkers().add(fTempTourMarker);

		fTourChart.updateChart(fTourData, chartConfig);
	}

	protected Control createDialogArea(Composite parent) {

		Composite dlgArea = (Composite) super.createDialogArea(parent);

		createContent(dlgArea);

		restoreDialogSettings();

		fTextMarkerName.selectAll();

		updateXOffset();
		updateYOffset();
		updateMarker();

		return dlgArea;
	}

	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(
				fTourChartContextProvider.getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	/**
	 * @return Returns the dialog settings for this dialog
	 */
	private IDialogSettings getDialogSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(
				fTourChartContextProvider.getClass().getName());
	}

	protected void okPressed() {

		TourChart tourChart = fTourChartContextProvider.fView.getTourChart();

		fSaveNewMarker = true;

		fTempTourMarker.setType(ChartMarker.MARKER_TYPE_CUSTOM);
		tourChart.setTourDirty();
		tourChart.updateMarkerLayer(true);

		// update marker list and other listener
		tourChart.fireSelectionTourChart();

		super.okPressed();
	}

	private void restoreDialogSettings() {

		IDialogSettings dlgSettings = getDialogSettings();

		try {
			fComboMarkerPosition.select(dlgSettings.getInt(DIALOG_SETTINGS_POSITION));
		} catch (Exception e) {
			fComboMarkerPosition.select(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
		}

		updateMarker();
	}

	private void saveDialogSettings() {

		IDialogSettings dlgSettings = getDialogSettings();

		dlgSettings.put(DIALOG_SETTINGS_POSITION, fComboMarkerPosition.getSelectionIndex());
	}

	private void updateMarker() {

		fTempTourMarker.setLabel(fTextMarkerName.getText().trim());
		fTempTourMarker.setVisualPosition(fComboMarkerPosition.getSelectionIndex());

		fTourChart.updateMarkerLayer(true);
	}

	private void updateXOffset() {

		final int xOffset = fScaleX.getSelection() - OFFSET_0;

		fTempTourMarker.setLabelXOffset(xOffset);

		fLabelXValue.setText(Integer.toString(xOffset));
	}

	private void updateYOffset() {

		final int yOffset = fScaleY.getSelection() - OFFSET_0;

		fTempTourMarker.setLabelYOffset(-yOffset);

		fLabelYValue.setText(Integer.toString(yOffset));
	}

}
