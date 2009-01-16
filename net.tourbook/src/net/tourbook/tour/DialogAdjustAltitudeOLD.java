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
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour chart or from the
 * tree viewer
 */
public class DialogAdjustAltitudeOLD extends TitleAreaDialog {

	private static final String	WIDGET_DATA_ALTI_ID				= "altiId";		//$NON-NLS-1$

	private static final String	WIDGET_DATA_METRIC_ALTITUDE		= "metricAltitude"; //$NON-NLS-1$

	private static final String	DIALOG_SETTINGS_ADJUST_TYPE		= "adjust_type";	//$NON-NLS-1$
	private static final String	DIALOG_SETTINGS_SCALE_YAXIS		= "scale_y-axis";	//$NON-NLS-1$
	private static final String	DIALOG_SETTINGS_KEEP_START		= "keep_start";	//$NON-NLS-1$

	private static final int	ALTI_ID_START					= 1;
	private static final int	ALTI_ID_END						= 2;
	private static final int	ALTI_ID_MAX						= 3;

	static final int			ADJUST_ALTITUDE_NONE			= 0;
	static final int			ADJUST_ALTITUDE_WHOLE_TOUR		= 1;
	static final int			ADJUST_ALTITUDE_START_AND_END	= 2;
	static final int			ADJUST_ALTITUDE_MAX_HEIGHT		= 3;
	static final int			ADJUST_ALTITUDE_END				= 4;

	static final String[]		adjustTypes						= new String[] {
			Messages.Dlg_AdjustAltitude_Type_Show_original,
			Messages.Dlg_AdjustAltitude_Type_adjust_whole_tour,
			Messages.Dlg_AdjustAltitude_Type_start_and_end,
			Messages.Dlg_AdjustAltitude_Type_adjust_height,
			Messages.Dlg_AdjustAltitude_Type_adjust_end		};

	private Image				fShellImage;

	private TourChart			fDialogTourChart;
	private TourData			fTourData;

	private Composite			fDialogArea;

	private Combo				fComboAdjustType;

	private Label				fLblOldStartAlti;
	private Label				fLblOldEndAlti;
	private Label				fLblOldMaxAlti;

	private Spinner				fSpinnerNewStartAlti;
	private Spinner				fSpinnerNewMaxAlti;
	private Spinner				fSpinnerNewEndAlti;

	private Button				fChkScaleYAxis;
	private Button				fBtnReset;
	private Button				fBtnCompare;

	private Button				fRadioKeepStart;
	private Button				fRadioKeepBottom;

	protected boolean			fIsInitialAltiDisplayed;

	private int[]				fAltitudeSerieOriginal;
	private int[]				fAltitudeSerieModified;

	private int					fInitialAltiStart;
	private int					fInitialAltiMax;

	private int					fAltiMaxDiff;
	private int					fAltiStartDiff;

	private int					fOldStartAlti;
	private int					fOldAltiInputMax;
	private int					fOldAltiInputStart;

	private boolean				fIsModifiedInternal				= false;

	public DialogAdjustAltitudeOLD(final Shell parentShell, final IStructuredSelection selection) {

		super(parentShell);

		setShellProperties();
	}

	public DialogAdjustAltitudeOLD(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		setShellProperties();

		fTourData = tourData;

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		final int[] originalAltitudeSerie = fTourData.altitudeSerie;
		if (originalAltitudeSerie != null) {
			final int serieLength = originalAltitudeSerie.length;
			fAltitudeSerieOriginal = new int[serieLength];
			System.arraycopy(originalAltitudeSerie, 0, fAltitudeSerieOriginal, 0, serieLength);
		}
	}

	private void adjustAltitude(final Integer altiFlag) {

		final int newAltiStart = (Integer) fSpinnerNewStartAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
		final int newAltiMax = (Integer) fSpinnerNewMaxAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
		final int newAltiEnd = (Integer) fSpinnerNewEndAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);

		final int[] altiDest = fTourData.altitudeSerie;

		final boolean isAltiSetByUser = altiFlag != null;

		// set adjustment type and enable the field(s) which can be modified
		switch (fComboAdjustType.getSelectionIndex()) {
		case ADJUST_ALTITUDE_NONE:
			resetAltitude();
			break;

		case ADJUST_ALTITUDE_START_AND_END:

			// adjust start, end and max

			// adjust end alti to start alti
			adjustEndAltitude(fAltitudeSerieOriginal, fTourData, fAltitudeSerieOriginal[0]);

			adjustStartAndMax(altiDest, altiDest, isAltiSetByUser, newAltiStart, newAltiMax);

			break;

		case ADJUST_ALTITUDE_WHOLE_TOUR:

			// adjust evenly
			adjustEvenly(fAltitudeSerieOriginal, altiDest, newAltiStart);
			break;

		case ADJUST_ALTITUDE_END:

			// adjust end
			adjustEndAltitude(fAltitudeSerieOriginal, fTourData, newAltiEnd);
			break;

		case ADJUST_ALTITUDE_MAX_HEIGHT:

			// adjust max

			adjustStartAndMax(fAltitudeSerieOriginal, altiDest, isAltiSetByUser, newAltiStart, newAltiMax);
			break;

		default:
			break;
		}

		/*
		 * make a backup of the current values
		 */
		final int[] altitudeSerie = fTourData.altitudeSerie;
		if (altitudeSerie != null) {
			fAltitudeSerieModified = new int[altitudeSerie.length];

			for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
				fAltitudeSerieModified[altiIndex] = altitudeSerie[altiIndex];
			}
		}

		// force the imperial altitude series to be recomputed
		fTourData.clearAltitudeSeries();
	}

	/**
	 * set button width
	 */
	private void adjustButtonWidth() {
		
		final int btnCompareWidth = fBtnCompare.getBounds().width;
		final int btnResetWidth = fBtnReset.getBounds().width;
		final int newWidth = Math.max(btnCompareWidth, btnResetWidth);

		GridData gd;
		gd = (GridData) fBtnCompare.getLayoutData();
		gd.widthHint = newWidth;

		gd = (GridData) fBtnReset.getLayoutData();
		gd.widthHint = newWidth;
	}

	/**
	 * adjust end altitude
	 * 
	 * @param altiSrc
	 * @param tourData
	 * @param newEndAlti
	 */
	private void adjustEndAltitude(final int[] altiSrc, final TourData tourData, final int newEndAlti) {

		final int[] altiDest = tourData.altitudeSerie;
		int[] endDataSerie = tourData.getDistanceSerie();

		if (endDataSerie == null) {
			endDataSerie = tourData.timeSerie;
		}

		final int altiEndDiff = newEndAlti - altiSrc[altiDest.length - 1];
		final float lastEndDataValue = endDataSerie[endDataSerie.length - 1];

		for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {
			final float endDataValue = endDataSerie[serieIndex];
			final float altiDiff = endDataValue / lastEndDataValue * altiEndDiff;
			altiDest[serieIndex] = altiSrc[serieIndex] + Math.round(altiDiff);
		}
	}

	/**
	 * adjust every altitude with the same difference
	 * 
	 * @param altiSrc
	 * @param altiDest
	 * @param newStartAlti
	 */
	private void adjustEvenly(final int[] altiSrc, final int[] altiDest, final int newStartAlti) {

		final int altiStartDiff = newStartAlti - altiSrc[0];

		for (int altIndex = 0; altIndex < altiSrc.length; altIndex++) {
			altiDest[altIndex] = altiSrc[altIndex] + altiStartDiff;
		}
	}

	/**
	 * Adjust max altitude, keep min value
	 * 
	 * @param altiSrc
	 * @param altiDest
	 * @param maxAltiNew
	 */
	private void adjustMaxAltitude(final int[] altiSrc, final int[] altiDest, final int maxAltiNew) {

		// calculate min/max altitude
		int maxAltiSrc = altiSrc[0];
		int minAltiSrc = altiSrc[0];
		for (final int altitude : altiSrc) {
			if (altitude > maxAltiSrc) {
				maxAltiSrc = altitude;
			}
			if (altitude < minAltiSrc) {
				minAltiSrc = altitude;
			}
		}

		// adjust altitude
		final int altiDiffSrc = maxAltiSrc - minAltiSrc;
		final int altiDiffNew = maxAltiNew - minAltiSrc;

		final float altiDiff = (float) altiDiffSrc / (float) altiDiffNew;

		for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {

			float alti0Based = altiSrc[serieIndex] - minAltiSrc;
			alti0Based = alti0Based / altiDiff;

			altiDest[serieIndex] = Math.round(alti0Based) + minAltiSrc;
		}
	}

	/**
	 * Adjust start and max at the same time
	 * <p>
	 * it took me several days to figure out this algorithim, 10.4.2007 Wolfgang
	 */
	private void adjustStartAndMax(	final int[] altiSrc,
									final int[] altiDest,
									final boolean isAltiSetByUser,
									final int newAltiStart,
									final int newAltiMax) {
		if (isAltiSetByUser) {

			// adjust max
			fAltiStartDiff -= fOldAltiInputStart - newAltiStart;
			fAltiMaxDiff -= fOldAltiInputMax - newAltiMax;

			final int oldStart = altiSrc[0];
			adjustMaxAltitude(altiSrc, altiDest, fInitialAltiMax + fAltiMaxDiff);
			final int newStart = altiDest[0];

			// adjust start
			int startDiff;
			if (fRadioKeepStart.getSelection()) {
				startDiff = 0;
			} else {
				startDiff = newStart - oldStart;
			}
			adjustEvenly(altiDest, altiDest, fInitialAltiStart + fAltiStartDiff + startDiff);

		} else {

			// set initial altitude values

			int altiMax = altiDest[0];
			for (final int altitude : altiDest) {
				if (altitude > altiMax) {
					altiMax = altitude;
				}
			}

			fInitialAltiStart = altiDest[0];
			fInitialAltiMax = altiMax;

			fAltiStartDiff = 0;
			fAltiMaxDiff = 0;
		}
	}

	@Override
	public boolean close() {

		saveDialogSettings();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dlg_AdjustAltitude_Title_window);
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				fShellImage.dispose();
			}
		});
	}

	@Override
	public void create() {

		super.create();

		/*
		 * initialize dialog by restoring dialog settings
		 */

		restoreDialogSettings();

		onChangeAdjustType();

		fIsModifiedInternal = true;
		{
			fSpinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(fOldStartAlti));
			fSpinnerNewStartAlti.setSelection((int) (fOldStartAlti / UI.UNIT_VALUE_ALTITUDE));
		}
		fIsModifiedInternal = false;

		// set focus to cancel button
		getButton(IDialogConstants.CANCEL_ID).setFocus();

		adjustButtonWidth();

		fDialogArea.layout(true, true);
	}

	/**
	 * Create altitude spinner field
	 * 
	 * @param startContainer
	 * @return Returns the field
	 */
	private Spinner createAltiField(final Composite startContainer) {

		final Spinner spinner = new Spinner(startContainer, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(99999);
		spinner.setIncrement(1);
		spinner.setPageIncrement(1);
		UI.setWidth(spinner, convertWidthInCharsToPixels(6));

		spinner.addModifyListener(new ModifyListener() {

			public void modifyText(final ModifyEvent e) {

				if (fIsModifiedInternal) {
					return;
				}

				final Spinner spinner = (Spinner) e.widget;

				if (UI.UNIT_VALUE_ALTITUDE == 1) {

					final int modifiedAlti = spinner.getSelection();
//					int metricAlti = (Integer) spinner.getData(WIDGET_DATA_METRIC_ALTITUDE);
//					
//					final float oldAlti = metricAlti / UI.UNIT_VALUE_ALTITUDE;
//					int newMetricAlti = (int) (modifiedAlti * UI.UNIT_VALUE_ALTITUDE);
//					
//					if (modifiedAlti > oldAlti) {
//						newMetricAlti++;
//					}

					spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, modifiedAlti);

				} else {

					/**
					 * adjust the non metric (imperial) value, this seems to be complicate and it is
					 * <p>
					 * the altitude data are always saved in the database with the metric system
					 * therefor the altitude must always match to the metric system, changing the
					 * altitude in the imperial system has always 3 or 4 value differences from one
					 * meter to the next meter
					 * <p>
					 * after many hours of investigation this seems to work
					 */

					final int modifiedAlti = spinner.getSelection();
					final int metricAlti = (Integer) spinner.getData(WIDGET_DATA_METRIC_ALTITUDE);

					final float oldAlti = metricAlti / UI.UNIT_VALUE_ALTITUDE;
					int newMetricAlti = (int) (modifiedAlti * UI.UNIT_VALUE_ALTITUDE);

					if (modifiedAlti > oldAlti) {
						newMetricAlti++;
					}

					spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, newMetricAlti);
				}

				onChangeAltitude((Integer) e.widget.getData(WIDGET_DATA_ALTI_ID));
			}
		});

		spinner.addMouseWheelListener(new MouseWheelListener() {

			public void mouseScrolled(final MouseEvent e) {

				if (fIsModifiedInternal) {
					return;
				}

				final Spinner spinner = (Spinner) e.widget;

				int accelerator = (e.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
				accelerator *= (e.stateMask & SWT.SHIFT) != 0 ? 5 : 1;
				accelerator *= e.count > 0 ? 1 : -1;

				int metricAltitude = (Integer) e.widget.getData(WIDGET_DATA_METRIC_ALTITUDE);
				metricAltitude = metricAltitude + accelerator;

				fIsModifiedInternal = true;
				{
					spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(metricAltitude));
					spinner.setSelection((int) (metricAltitude / UI.UNIT_VALUE_ALTITUDE));
				}
				fIsModifiedInternal = false;

				onChangeAltitude((Integer) e.widget.getData(WIDGET_DATA_ALTI_ID));
			}
		});

		spinner.addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {}

			public void focusLost(final FocusEvent e) {
				onChangeAltitude((Integer) e.widget.getData(WIDGET_DATA_ALTI_ID));
			}
		});

		return spinner;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		// fFieldContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		Label label;
		GridLayout gl;
		GridData gd;

		fDialogArea = (Composite) super.createDialogArea(parent);

		final Composite dlgContainer = new Composite(fDialogArea, SWT.NONE);
		gl = new GridLayout(1, false);
		gl.marginWidth = 10;
		gl.verticalSpacing = 0;
		dlgContainer.setLayout(gl);
		dlgContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		setTitle(Messages.Dlg_AdjustAltitude_Title_dlg);

		/*
		 * combo: adjust type
		 */
		final Composite typeContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		typeContainer.setLayout(gl);
		typeContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		label = new Label(typeContainer, SWT.NONE);
		label.setText(Messages.Dlg_AdjustAltitude_Label_adjustment_type);

		fComboAdjustType = new Combo(typeContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboAdjustType.setVisibleItemCount(20);
		fComboAdjustType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeAdjustType();
			}
		});

		// fill combo
		for (final String adjustType : adjustTypes) {
			fComboAdjustType.add(adjustType);
		}

		/*
		 * tour chart
		 */
		fDialogTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		gd = new GridData(GridData.FILL_BOTH);
		gd.minimumWidth = 500;
		gd.minimumHeight = 300;
		fDialogTourChart.setLayoutData(gd);

		fDialogTourChart.setShowZoomActions(true);
		fDialogTourChart.setShowSlider(true);

		// set altitude visible
		final TourChartConfiguration chartConfig = new TourChartConfiguration(true);
		chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

		fDialogTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		fDialogTourChart.updateTourChart(fTourData, chartConfig, true);

		/*
		 * container: altitude controls
		 */
		final Composite adjustContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		adjustContainer.setLayout(gl);
		adjustContainer.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

		/*
		 * field: start altitude
		 */
		final Composite startContainer = new Composite(adjustContainer, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		startContainer.setLayout(gl);
		startContainer.setLayoutData(new GridData(SWT.LEAD, SWT.FILL, true, true));

		label = new Label(startContainer, SWT.NONE);
		label.setText(Messages.Dlg_AdjustAltitude_Label_start_altitude);
		label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

		fSpinnerNewStartAlti = createAltiField(startContainer);
		fSpinnerNewStartAlti.setData(WIDGET_DATA_ALTI_ID, new Integer(ALTI_ID_START));
		fSpinnerNewStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

		fLblOldStartAlti = new Label(startContainer, SWT.NONE);
		fLblOldStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

		/*
		 * field: max altitude
		 */
		final Composite maxContainer = new Composite(adjustContainer, SWT.NONE);
		maxContainer.setLayout(gl);
		maxContainer.setLayoutData(new GridData(SWT.CENTER, SWT.DEFAULT, true, false));

		label = new Label(maxContainer, SWT.NONE);
		label.setText(Messages.Dlg_AdjustAltitude_Label_max_altitude);
		label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

		fSpinnerNewMaxAlti = createAltiField(maxContainer);
		fSpinnerNewMaxAlti.setData(WIDGET_DATA_ALTI_ID, new Integer(ALTI_ID_MAX));
		fSpinnerNewMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

		fLblOldMaxAlti = new Label(maxContainer, SWT.NONE);
		fLblOldMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

		/*
		 * group: keep start/bottom
		 */
		final Group keepContainer = new Group(maxContainer, SWT.NONE);
		keepContainer.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false, 3, 1));
		keepContainer.setLayout(new GridLayout(1, false));
		keepContainer.setText(Messages.Dlg_AdjustAltitude_Group_options);

		final SelectionAdapter keepButtonSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeAltitude(new Integer(ALTI_ID_MAX));
			}
		};

		fRadioKeepBottom = new Button(keepContainer, SWT.RADIO);
		fRadioKeepBottom.setText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude);
		fRadioKeepBottom.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude_tooltip);
		fRadioKeepBottom.setLayoutData(new GridData());
		fRadioKeepBottom.addSelectionListener(keepButtonSelectionAdapter);
		// fRadioKeepBottom.setSelection(true);

		fRadioKeepStart = new Button(keepContainer, SWT.RADIO);
		fRadioKeepStart.setText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude);
		fRadioKeepStart.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude_tooltip);
		fRadioKeepStart.setLayoutData(new GridData());
		fRadioKeepStart.addSelectionListener(keepButtonSelectionAdapter);

		/*
		 * field: end altitude
		 */
		final Composite endContainer = new Composite(adjustContainer, SWT.NONE);
		endContainer.setLayout(gl);
		endContainer.setLayoutData(new GridData(SWT.TRAIL, SWT.DEFAULT, true, false));

		label = new Label(endContainer, SWT.NONE);
		label.setText(Messages.Dlg_AdjustAltitude_Label_end_altitude);
		label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

		fSpinnerNewEndAlti = createAltiField(endContainer);
		fSpinnerNewEndAlti.setData(WIDGET_DATA_ALTI_ID, new Integer(ALTI_ID_END));
		fSpinnerNewEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

		fLblOldEndAlti = new Label(endContainer, SWT.NONE);
		fLblOldEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

		/*
		 * button container
		 */
		final Composite buttonContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		buttonContainer.setLayout(gl);
		gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		gd.verticalIndent = 30;
		buttonContainer.setLayoutData(gd);

		/*
		 * checkbox: adjust y-axis scale
		 */
		fChkScaleYAxis = new Button(buttonContainer, SWT.CHECK);
		fChkScaleYAxis.setText(Messages.Dlg_AdjustAltitude_Checkbox_autoscale_yaxis);
		fChkScaleYAxis.setToolTipText(Messages.Dlg_AdjustAltitude_Checkbox_autoscale_yaxis_tooltip);
		fChkScaleYAxis.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeAltitude(new Integer(ALTI_ID_START));
			}
		});
		/*
		 * this feature is disabled because it's not working, the reason is that the vertical
		 * borders are automatically computed since version 1.5.0
		 */
		fChkScaleYAxis.setVisible(false);

		/*
		 * button: reset
		 */
		fBtnReset = new Button(buttonContainer, SWT.NONE);
		fBtnReset.setText(Messages.Dlg_AdjustAltitude_Button_reset_altitudes);
		fBtnReset.setToolTipText(Messages.Dlg_AdjustAltitude_Button_reset_altitudes_tooltip);
		fBtnReset.setLayoutData(new GridData(SWT.TRAIL, SWT.DEFAULT, true, false));
		fBtnReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeAdjustType();
			}
		});

		/*
		 * button: compare values
		 */
		fBtnCompare = new Button(buttonContainer, SWT.NONE);
		fBtnCompare.setText(Messages.Dlg_AdjustAltitude_Button_show_original_values);
		fBtnCompare.setToolTipText(Messages.Dlg_AdjustAltitude_Button_show_original_values_tooltip);
		fBtnCompare.setLayoutData(new GridData(SWT.TRAIL, SWT.DEFAULT, false, false));
		fBtnCompare.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
				if (fIsInitialAltiDisplayed == false && e.character == ' ') {
					fIsInitialAltiDisplayed = true;
					setOriginalAltitudeValues();
					fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				setInitialAltitudeValues();
				fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
				fIsInitialAltiDisplayed = false;
			}
		});

		fBtnCompare.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				if (fIsInitialAltiDisplayed == false) {
					fIsInitialAltiDisplayed = true;
					setOriginalAltitudeValues();
					fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
				}
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				setInitialAltitudeValues();
				fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
				fIsInitialAltiDisplayed = false;
			}
		});

		return fDialogArea;
	}

	@SuppressWarnings("unused")
	private void dumpMinMax(final int[] altiSrc, final String place) {

		int minAltiSrc1 = altiSrc[0];
		int maxAltiSrc1 = altiSrc[0];
		for (final int altitude : altiSrc) {
			if (altitude < minAltiSrc1) {
				minAltiSrc1 = altitude;
			}
			if (altitude > maxAltiSrc1) {
				maxAltiSrc1 = altitude;
			}
		}

		System.out.println(place + (" start:" + altiSrc[0]) //$NON-NLS-1$
				+ (" min:" + minAltiSrc1) //$NON-NLS-1$
				+ (" max:" + maxAltiSrc1)); //$NON-NLS-1$
	}

	private void enableFields() {

		// disable all altitude edit fields
		fSpinnerNewStartAlti.setEnabled(false);
		fSpinnerNewEndAlti.setEnabled(false);
		fSpinnerNewMaxAlti.setEnabled(false);

		fRadioKeepStart.setEnabled(false);
		fRadioKeepBottom.setEnabled(false);

		fBtnReset.setEnabled(true);
		fBtnCompare.setEnabled(true);
		fChkScaleYAxis.setEnabled(true);

		// set adjustment type and enable the field(s) which can be modified
		switch (fComboAdjustType.getSelectionIndex()) {

		case ADJUST_ALTITUDE_NONE:

			// in this mode the altitude values are already reset
			fBtnReset.setEnabled(false);
			fBtnCompare.setEnabled(false);

			fChkScaleYAxis.setEnabled(false);

			setMessage(Messages.Dlg_AdjustAltitude_Message_select_type);
			break;

		case ADJUST_ALTITUDE_START_AND_END:

			fSpinnerNewStartAlti.setEnabled(true);
			fSpinnerNewMaxAlti.setEnabled(true);

			fRadioKeepStart.setEnabled(true);
			fRadioKeepBottom.setEnabled(true);

			setMessage(Messages.Dlg_AdjustAltitude_Message_adjust_start_and_end);
			break;

		case ADJUST_ALTITUDE_WHOLE_TOUR:

			fSpinnerNewStartAlti.setEnabled(true);

			setMessage(Messages.Dlg_AdjustAltitude_Message_adjust_whole_tour);
			break;

		case ADJUST_ALTITUDE_END:

			fSpinnerNewEndAlti.setEnabled(true);

			setMessage(Messages.Dlg_AdjustAltitude_Message_adjust_end);
			break;

		case ADJUST_ALTITUDE_MAX_HEIGHT:

			fSpinnerNewMaxAlti.setEnabled(true);

			fRadioKeepStart.setEnabled(true);
			fRadioKeepBottom.setEnabled(true);

			setMessage(Messages.Dlg_AdjustAltitude_Message_adjust_max);
			break;

		default:
			break;
		}
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

	private void onChangeAdjustType() {

		if (fTourData.altitudeSerie == null) {
			return;
		}

		resetAltitude();

		enableFields();

		onChangeAltitude(null);
	}

	private void onChangeAltitude(final Integer altiFlag) {

		// calcuate new altitude values
		adjustAltitude(altiFlag);

		// set new values into the fields which can change the altitude
		setAltiFieldValues();

		fDialogTourChart.updateTourChart(!fChkScaleYAxis.getSelection());
	}

	/**
	 * reset altitudes to it's original values
	 */
	private void resetAltitude() {

		restoreOriginalAltitudeValues();

		final int startAlti = fAltitudeSerieOriginal[0];
		final int endAlti = fAltitudeSerieOriginal[fAltitudeSerieOriginal.length - 1];

		// calculate max altitude
		int maxAlti = startAlti;
		for (final int altitude : fAltitudeSerieOriginal) {
			if (altitude > maxAlti) {
				maxAlti = altitude;
			}
		}

		fOldStartAlti = startAlti;

		fLblOldStartAlti.setText(Integer.toString((int) (startAlti / UI.UNIT_VALUE_ALTITUDE)));
		fLblOldStartAlti.pack(true);

		fLblOldEndAlti.setText(Integer.toString((int) (endAlti / UI.UNIT_VALUE_ALTITUDE)));
		fLblOldEndAlti.pack(true);

		fLblOldMaxAlti.setText(Integer.toString((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE)));
		fLblOldMaxAlti.pack(true);

		fIsModifiedInternal = true;
		{
			fSpinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(startAlti));
			fSpinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ALTITUDE));

			fSpinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(endAlti));
			fSpinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ALTITUDE));

			fSpinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(maxAlti));
			fSpinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE));
		}
		fIsModifiedInternal = false;

	}

	/**
	 * Restore settings from a previous opened dialog
	 */
	private void restoreDialogSettings() {

		final IDialogSettings dlgSettings = getDialogSettings();

		try {
			fComboAdjustType.select(dlgSettings.getInt(DIALOG_SETTINGS_ADJUST_TYPE));
		} catch (final Exception e) {
			fComboAdjustType.select(ADJUST_ALTITUDE_NONE);
		}

		fChkScaleYAxis.setSelection(dlgSettings.getBoolean(DIALOG_SETTINGS_SCALE_YAXIS));

		if (dlgSettings.getBoolean(DIALOG_SETTINGS_KEEP_START)) {
			fRadioKeepStart.setSelection(true);
		} else {
			fRadioKeepBottom.setSelection(true);
		}
	}

	/**
	 * copy the old altitude values back into the tourdata altitude serie
	 */
	public void restoreOriginalAltitudeValues() {

		final int[] altitudeSerie = fTourData.altitudeSerie;

		if (altitudeSerie == null | fAltitudeSerieOriginal == null) {
			return;
		}

		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fAltitudeSerieOriginal[altiIndex];
		}

		// force the imperial altitude series to be recomputed
		fTourData.clearAltitudeSeries();
	}

	private void saveDialogSettings() {

		final IDialogSettings dlgSettings = getDialogSettings();

		dlgSettings.put(DIALOG_SETTINGS_ADJUST_TYPE, fComboAdjustType.getSelectionIndex());
		dlgSettings.put(DIALOG_SETTINGS_SCALE_YAXIS, fChkScaleYAxis.getSelection());
		dlgSettings.put(DIALOG_SETTINGS_KEEP_START, fRadioKeepStart.getSelection());
	}

	/**
	 * set the altitude fields with the current altitude values
	 */
	private void setAltiFieldValues() {

		final int[] altiSerie = fTourData.altitudeSerie;

		final int startAlti = altiSerie[0];
		final int endAlti = altiSerie[altiSerie.length - 1];

		// get max altitude
		int maxAlti = altiSerie[0];
		for (final int altitude : altiSerie) {
			if (altitude > maxAlti) {
				maxAlti = altitude;
			}
		}

		// keep values
		fOldAltiInputStart = startAlti;
		fOldAltiInputMax = maxAlti;

		fSpinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(startAlti));
		fSpinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(endAlti));
		fSpinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(maxAlti));

		/*
		 * prevent to fire the selection event in the spinner when a selection is set, this would
		 * cause endless loops
		 */
		fIsModifiedInternal = true;
		{
			fSpinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ALTITUDE));
			fSpinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ALTITUDE));
			fSpinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE));
		}
		fIsModifiedInternal = false;

		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}

	private void setInitialAltitudeValues() {

		final int[] altitudeSerie = fTourData.altitudeSerie;

		if (altitudeSerie == null || fAltitudeSerieModified == null) {
			return;
		}

		// set altitude to the initial values
		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fAltitudeSerieModified[altiIndex];
		}

		// force the imperial altitude series to be recomputed
		fTourData.clearAltitudeSeries();
	}

	private void setOriginalAltitudeValues() {

		final int[] altitudeSerie = fTourData.altitudeSerie;

		if (altitudeSerie == null || fAltitudeSerieOriginal == null) {
			return;
		}

		// set original values
		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fAltitudeSerieOriginal[altiIndex];
		}

		// force the imperial altitude series to be recomputed
		fTourData.clearAltitudeSeries();
	}

	private void setShellProperties() {

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.Image__edit_adjust_altitude).createImage();
		setDefaultImage(fShellImage);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}
}
