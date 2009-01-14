/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.data.TourData;
import net.tourbook.ext.srtm.ElevationSRTM3;
import net.tourbook.ext.srtm.GeoLat;
import net.tourbook.ext.srtm.GeoLon;
import net.tourbook.ext.srtm.NumberForm;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DialogAdjustAltitudeSRTM_OLD extends TitleAreaDialog {

	private Image					fShellImage;
	private final IDialogSettings	fDialogSettings;

	private TourData				fTourData;

	private Composite				fDlgContainer;

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;

	private int[]					fBackupAltitudeSerie;
	private boolean					fIsTourSaved;

	private int						fAltiMinValue;
	private int						fAltiMaxValue;

	boolean							isSRTMValid	= false;

	/**
	 * creates a int array backup
	 * 
	 * @param source
	 * @return the backup array or <code>null</code> when the source is <code>null</code>
	 */
	private static int[] createDataSerieBackup(final int[] source) {

		int[] backup = null;

		if (source != null) {
			final int serieLength = source.length;
			backup = new int[serieLength];
			System.arraycopy(source, 0, backup, 0, serieLength);
		}

		return backup;
	}

	/**
	 * @param parentShell
	 * @param tourData
	 *            {@link TourData} for the tour which altitude should be adjusted
	 */
	public DialogAdjustAltitudeSRTM_OLD(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		// make dialog resizable and display maximize button
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.image__adjust_altitude_srtm).createImage();
		setDefaultImage(fShellImage);

		fTourData = tourData;

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	public boolean close() {

		if (fIsTourSaved == false) {

			// tour is not saved, dialog is canceled, restore original values

			restoreDataBackup();
		}

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.adjust_alti_srtm_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	public void create() {

		createDataBackup();

		// create UI
		super.create();

		setTitle(Messages.adjust_alti_srtm_dialog_header_title);
		setMessage(Messages.adjust_alti_srtm_dialog_header_message);

		createSRTMData();
		createChartConfiguration();

//		fTourChart.updateSRTMLayer();
		fTourChart.updateTourChart(fTourData, fTourChartConfig, true);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// rename OK button
		final Button buttonOK = getButton(IDialogConstants.OK_ID);
		buttonOK.setText(Messages.adjust_alti_srtm_save_tour);

		setButtonLayoutData(buttonOK);
	}

	private void createChartConfiguration() {

		// set altitude visible
		fTourChartConfig = new TourChartConfiguration(true);

		// set one visible graph
		int visibleGraph = -1;
		if (fTourData.altitudeSerie != null) {
			visibleGraph = TourManager.GRAPH_ALTITUDE;
		} else if (fTourData.pulseSerie != null) {
			visibleGraph = TourManager.GRAPH_PULSE;
		} else if (fTourData.temperatureSerie != null) {
			visibleGraph = TourManager.GRAPH_TEMPERATURE;
		} else if (fTourData.cadenceSerie != null) {
			visibleGraph = TourManager.GRAPH_CADENCE;
		}
		if (visibleGraph != -1) {
			fTourChartConfig.addVisibleGraph(visibleGraph);
		}

		// overwrite x-axis from pref store
		fTourChartConfig.setIsShowTimeOnXAxis(TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getString(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS)
				.equals(TourManager.X_AXIS_TIME));

		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {
				onDataModelChanged(changedChartDataModel);
			}
		});
	}

	private void createDataBackup() {

		// keep a backup of the altitude data because these data are changed in this dialog
		fBackupAltitudeSerie = createDataSerieBackup(fTourData.altitudeSerie);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		fDlgContainer = (Composite) super.createDialogArea(parent);

		createUI(fDlgContainer);

		return fDlgContainer;
	}

	private void createSRTMData() {

		final int[] srtmDataSerie = new int[fTourData.timeSerie.length];
		final int[] altitudeSerie = fTourData.altitudeSerie;

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			public void run() {

				// initialize SRTM
				new NumberForm();
				final ElevationSRTM3 elevationSRTM3 = new ElevationSRTM3();

				final double[] latitudeSerie = fTourData.latitudeSerie;
				final double[] longitude = fTourData.longitudeSerie;

				int serieIndex = 0;
				short lastValidSRTM = 0;

				for (final double latitude : latitudeSerie) {

					short srtmValue = elevationSRTM3.getElevation(new GeoLat(latitude),
							new GeoLon(longitude[serieIndex]));

					/*
					 * set invalid values to the previous valid value
					 */
					if (srtmValue == Short.MIN_VALUE) {
						srtmValue = lastValidSRTM;
					} else {
						isSRTMValid = true;
						lastValidSRTM = srtmValue;
					}

					// adjust wrong values
					if (srtmValue < -1000) {
						srtmValue = 0;
					} else if (srtmValue > 10000) {
						srtmValue = 10000;
					}

					// get min/max values
					if (serieIndex == 0) {
						fAltiMinValue = fAltiMaxValue = srtmValue;
					} else {
						fAltiMinValue = srtmValue < fAltiMinValue ? srtmValue : fAltiMinValue;
						fAltiMaxValue = srtmValue > fAltiMaxValue ? srtmValue : fAltiMaxValue;
					}

					srtmDataSerie[serieIndex++] = srtmValue;
				}

				// adjust min/max values to existing altitude
				if (altitudeSerie != null) {
					for (final int altitude : altitudeSerie) {
						fAltiMinValue = altitude < fAltiMinValue ? altitude : fAltiMinValue;
						fAltiMaxValue = altitude > fAltiMaxValue ? altitude : fAltiMaxValue;
					}
				}

			}
		});

		if (isSRTMValid) {
			if (altitudeSerie == null) {
				fTourData.altitudeSerie = srtmDataSerie;
			} else {
//				fTourData.srtmDataSerie = srtmDataSerie;
			}
		}
	}

	private void createUI(final Composite parent) {

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.fillDefaults().margins(10, 0).numColumns(1).applyTo(dlgContainer);

		fTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fTourChart);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return null;
		return fDialogSettings;
	}

	@Override
	protected void okPressed() {

		TourManager.saveModifiedTour(fTourData);

		fIsTourSaved = true;

		super.okPressed();
	}

	private void onDataModelChanged(final ChartDataModel changedChartDataModel) {

		// set title
		changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));

		// adjust min/max values for the altitude to the SRTM min/max values
		if (isSRTMValid) {

			for (final ChartDataSerie chartData : changedChartDataModel.getYData()) {
				if (chartData instanceof ChartDataYSerie) {
					final ChartDataYSerie yData = (ChartDataYSerie) chartData;

					final Integer graphId = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
					if (graphId == TourManager.GRAPH_ALTITUDE) {

						yData.setVisibleMinValue(fAltiMinValue, true);
						yData.setVisibleMaxValue(fAltiMaxValue, true);

						// nothing more to to
						break;
					}
				}
			}
		}
	}

	private void onDispose() {

		fShellImage.dispose();
	}

	/**
	 * Restore values which have been modified in the dialog
	 */
	private void restoreDataBackup() {

		fTourData.altitudeSerie = fBackupAltitudeSerie;
	}

}
