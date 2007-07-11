/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour chart or from the
 * tree viewer
 */
public class AdjustAltitudeDialog extends TitleAreaDialog {

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
			Messages.Dlg_Adjust_Altitude_Type_Show_original,
			Messages.Dlg_Adjust_Altitude_Type_adjust_whole_tour,
			Messages.Dlg_Adjust_Altitude_Type_start_and_end,
			Messages.Dlg_Adjust_Altitude_Type_adjust_height,
			Messages.Dlg_Adjust_Altitude_Type_adjust_end		};

	private TourChart			fSelectedTourChart;
	private TourChart			fTourChart;

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

//	private boolean				fIsSpinnerChangedByUser;
	protected boolean			fIsInitialAltiDisplayed;

	private int[]				fOriginalAltitudes;
	private int					fInitialAltiStart;

	private int					fInitialAltiMax;
	private int[]				fInitialAltitude;

	private int					fAltiMaxDiff;
	private int					fAltiStartDiff;

	private int					fOldStartAlti;
	private int					fOldAltiInputMax;
	private int					fOldAltiInputStart;

	public AdjustAltitudeDialog(Shell parentShell, IStructuredSelection selection) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	public AdjustAltitudeDialog(Shell parentShell, TourChart tourChart) {

		super(parentShell);

		// make the dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		fSelectedTourChart = tourChart;

		/*
		 * keep a backup for the altitude data because they will be changed, they are saved when the
		 * user presses the OK button
		 */
		int[] altitudeSerie = getAltitudeSerie();
		if (altitudeSerie != null) {
			final int serieLength = altitudeSerie.length;
			fOriginalAltitudes = new int[serieLength];
			System.arraycopy(altitudeSerie, 0, fOriginalAltitudes, 0, serieLength);
		}
	}

	private void adjustAltitude(TourData tourData, Integer altiFlag) {

		final int newAltiStart = fSpinnerNewStartAlti.getSelection();
		final int newAltiMax = fSpinnerNewMaxAlti.getSelection();
		final int[] altiDest = tourData.altitudeSerie;

		boolean isAltiSetByUser = altiFlag != null;

		// set adjustment type and enable the field(s) which can be modified
		switch (fComboAdjustType.getSelectionIndex()) {
		case ADJUST_ALTITUDE_NONE:
			resetAltitude();
			break;

		case ADJUST_ALTITUDE_START_AND_END:

			// adjust start, end and max

			// adjust end alti to start alti
			adjustEndAltitude(fOriginalAltitudes, tourData, fOriginalAltitudes[0]);

			adjustStartAndMax(altiDest, altiDest, isAltiSetByUser, newAltiStart, newAltiMax);

			break;

		case ADJUST_ALTITUDE_WHOLE_TOUR:

			// adjust evenly
			adjustEvenly(fOriginalAltitudes, altiDest, newAltiStart);
			break;

		case ADJUST_ALTITUDE_END:

			// adjust end
			adjustEndAltitude(fOriginalAltitudes, tourData, fSpinnerNewEndAlti.getSelection());
			break;

		case ADJUST_ALTITUDE_MAX_HEIGHT:

			// adjust max

			adjustStartAndMax(
					fOriginalAltitudes,
					altiDest,
					isAltiSetByUser,
					newAltiStart,
					newAltiMax);
			break;

		default:
			break;
		}

		/*
		 * make a backup of the current values
		 */
		int[] altitudeSerie = getAltitudeSerie();
		if (altitudeSerie != null) {
			fInitialAltitude = new int[altitudeSerie.length];

			for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
				fInitialAltitude[altiIndex] = altitudeSerie[altiIndex];
			}
		}
	}

	/**
	 * adjust end altitude
	 * 
	 * @param altiSrc
	 * @param tourData
	 * @param newEndAlti
	 */
	private void adjustEndAltitude(int[] altiSrc, TourData tourData, int newEndAlti) {

		int[] altiDest = tourData.altitudeSerie;
		int[] distanceSerie = tourData.distanceSerie;

		int altiEndDiff = newEndAlti - altiSrc[altiDest.length - 1];
		float tourDistance = distanceSerie[distanceSerie.length - 1];

		for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {
			float distance = distanceSerie[serieIndex];
			float altiDiff = distance / tourDistance * altiEndDiff;
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
	private void adjustEvenly(int[] altiSrc, int[] altiDest, int newStartAlti) {

		int altiStartDiff = newStartAlti - altiSrc[0];

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
	private void adjustMaxAltitude(int[] altiSrc, int[] altiDest, int maxAltiNew) {

		// calculate min/max altitude
		int maxAltiSrc = altiSrc[0];
		int minAltiSrc = altiSrc[0];
		for (int altitude : altiSrc) {
			if (altitude > maxAltiSrc) {
				maxAltiSrc = altitude;
			}
			if (altitude < minAltiSrc) {
				minAltiSrc = altitude;
			}
		}

		// adjust altitude
		int altiDiffSrc = maxAltiSrc - minAltiSrc;
		int altiDiffNew = maxAltiNew - minAltiSrc;

		float altiDiff = (float) altiDiffSrc / (float) altiDiffNew;

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
									boolean isAltiSetByUser,
									final int newAltiStart,
									final int newAltiMax) {
		if (isAltiSetByUser) {

			// adjust max
			fAltiStartDiff -= fOldAltiInputStart - newAltiStart;
			fAltiMaxDiff -= fOldAltiInputMax - newAltiMax;

			int oldStart = altiSrc[0];
			adjustMaxAltitude(altiSrc, altiDest, fInitialAltiMax + fAltiMaxDiff);
			int newStart = altiDest[0];

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
			for (int altitude : altiDest) {
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

	public boolean close() {

		saveDialogSettings();

		return super.close();
	}

	protected void configureShell(Shell shell) {

		super.configureShell(shell);

		// set window title
		shell.setText(Messages.Dlg_Adjust_Altitude_Title_window);
	}

	/**
	 * Create altitude spinner field
	 * 
	 * @param startContainer
	 * @return Returns the field
	 */
	private Spinner createAltiField(Composite startContainer) {

		Spinner spinner = new Spinner(startContainer, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(99999);
		spinner.setIncrement(1);
		spinner.setPageIncrement(1);
		UI.setWidth(spinner, convertWidthInCharsToPixels(6));

		spinner.addMouseWheelListener(new MouseWheelListener() {

			public void mouseScrolled(MouseEvent e) {
				Spinner widget = (Spinner) e.widget;

				int accelerator = (e.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
				accelerator *= (e.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

				widget.setSelection(widget.getSelection() + ((e.count > 0 ? 1 : -1) * accelerator));

				onChangeAltitude((Integer) e.widget.getData());
			}
		});

		spinner.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent e) {}

			public void focusLost(FocusEvent e) {
				onChangeAltitude((Integer) e.widget.getData());
			}
		});

		return spinner;
	}

	protected Control createDialogArea(Composite parent) {

		// fFieldContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		Label label;
		GridLayout gl;
		GridData gd;

		fDialogArea = (Composite) super.createDialogArea(parent);

		Composite dlgContainer = new Composite(fDialogArea, SWT.NONE);
		gl = new GridLayout(1, false);
		gl.marginWidth = 10;
		gl.verticalSpacing = 0;
		dlgContainer.setLayout(gl);
		dlgContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		setTitle(Messages.Dlg_Adjust_Altitude_Title_dlg);

		/*
		 * create adjust type combo
		 */
		Composite typeContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		typeContainer.setLayout(gl);
		typeContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		label = new Label(typeContainer, SWT.NONE);
		label.setText(Messages.Dlg_Adjust_Altitude_Label_adjustment_type);

		fComboAdjustType = new Combo(typeContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboAdjustType.setVisibleItemCount(20);
		fComboAdjustType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onChangeAdjustType();
			}
		});

		// fill combo
		for (String adjustType : adjustTypes) {
			fComboAdjustType.add(adjustType);
		}

		/*
		 * create tour chart
		 */
		fTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		gd = new GridData(GridData.FILL_BOTH);
		gd.minimumWidth = 500;
		gd.minimumHeight = 300;
		fTourChart.setLayoutData(gd);

		// set altitude visible
		TourChartConfiguration chartConfig = new TourChartConfiguration(true);
		chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

		final TourData tourData = fSelectedTourChart.getTourData();

		fTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(ChartDataModel changedChartDataModel) {

				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(tourData));
			}
		});

		fTourChart.updateChart(tourData, chartConfig, true);

		/*
		 * container: altitude controls
		 */
		Composite adjustContainer = new Composite(dlgContainer, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		adjustContainer.setLayout(gl);
		adjustContainer.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

		/*
		 * field: start altitude
		 */
		Composite startContainer = new Composite(adjustContainer, SWT.NONE);
		gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		startContainer.setLayout(gl);
		startContainer.setLayoutData(new GridData(SWT.LEAD, SWT.FILL, true, true));

		label = new Label(startContainer, SWT.NONE);
		label.setText(Messages.Dlg_Adjust_Altitude_Label_start_altitude);
		label.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_start_altitude_tooltip);

		fSpinnerNewStartAlti = createAltiField(startContainer);
		fSpinnerNewStartAlti.setData(new Integer(ALTI_ID_START));
		fSpinnerNewStartAlti
				.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_start_altitude_tooltip);

		fLblOldStartAlti = new Label(startContainer, SWT.NONE);
		fLblOldStartAlti.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_original_values);

		/*
		 * field: max altitude
		 */
		Composite maxContainer = new Composite(adjustContainer, SWT.NONE);
		maxContainer.setLayout(gl);
		maxContainer.setLayoutData(new GridData(SWT.CENTER, SWT.DEFAULT, true, false));

		label = new Label(maxContainer, SWT.NONE);
		label.setText(Messages.Dlg_Adjust_Altitude_Label_max_altitude);
		label.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_max_altitude_tooltip);

		fSpinnerNewMaxAlti = createAltiField(maxContainer);
		fSpinnerNewMaxAlti.setData(new Integer(ALTI_ID_MAX));
		fSpinnerNewMaxAlti.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_max_altitude_tooltip);

		fLblOldMaxAlti = new Label(maxContainer, SWT.NONE);
		fLblOldMaxAlti.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_original_values);

		/*
		 * group: keep start/bottom
		 */
		Group keepContainer = new Group(maxContainer, SWT.NONE);
		keepContainer.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false, 3, 1));
		keepContainer.setLayout(new GridLayout(1, false));
		keepContainer.setText(Messages.Dlg_Adjust_Altitude_Group_options);

		final SelectionAdapter keepButtonSelectionAdapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onChangeAltitude(new Integer(ALTI_ID_MAX));
			}
		};

		fRadioKeepBottom = new Button(keepContainer, SWT.RADIO);
		fRadioKeepBottom.setText(Messages.Dlg_Adjust_Altitude_Radio_keep_bottom_altitude);
		fRadioKeepBottom
				.setToolTipText(Messages.Dlg_Adjust_Altitude_Radio_keep_bottom_altitude_tooltip);
		fRadioKeepBottom.setLayoutData(new GridData());
		fRadioKeepBottom.addSelectionListener(keepButtonSelectionAdapter);
		// fRadioKeepBottom.setSelection(true);

		fRadioKeepStart = new Button(keepContainer, SWT.RADIO);
		fRadioKeepStart.setText(Messages.Dlg_Adjust_Altitude_Radio_keep_start_altitude);
		fRadioKeepStart
				.setToolTipText(Messages.Dlg_Adjust_Altitude_Radio_keep_start_altitude_tooltip);
		fRadioKeepStart.setLayoutData(new GridData());
		fRadioKeepStart.addSelectionListener(keepButtonSelectionAdapter);

		/*
		 * field: end altitude
		 */
		Composite endContainer = new Composite(adjustContainer, SWT.NONE);
		endContainer.setLayout(gl);
		endContainer.setLayoutData(new GridData(SWT.TRAIL, SWT.DEFAULT, true, false));

		label = new Label(endContainer, SWT.NONE);
		label.setText(Messages.Dlg_Adjust_Altitude_Label_end_altitude);
		label.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_end_altitude_tooltip);

		fSpinnerNewEndAlti = createAltiField(endContainer);
		fSpinnerNewEndAlti.setData(new Integer(ALTI_ID_END));
		fSpinnerNewEndAlti.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_end_altitude_tooltip);

		fLblOldEndAlti = new Label(endContainer, SWT.NONE);
		fLblOldEndAlti.setToolTipText(Messages.Dlg_Adjust_Altitude_Label_original_values);

		/*
		 * button container
		 */
		Composite buttonContainer = new Composite(dlgContainer, SWT.NONE);
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
		fChkScaleYAxis.setText(Messages.Dlg_Adjust_Altitude_Checkbox_autoscale_yaxis);
		fChkScaleYAxis
				.setToolTipText(Messages.Dlg_Adjust_Altitude_Checkbox_autoscale_yaxis_tooltip);
		fChkScaleYAxis.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onChangeAltitude(new Integer(ALTI_ID_START));
			}
		});

		/*
		 * button: reset
		 */
		fBtnReset = new Button(buttonContainer, SWT.NONE);
		fBtnReset.setText(Messages.Dlg_Adjust_Altitude_Button_reset_altitudes);
		fBtnReset.setToolTipText(Messages.Dlg_Adjust_Altitude_Button_reset_altitudes_tooltip);
		fBtnReset.setLayoutData(new GridData(SWT.TRAIL, SWT.DEFAULT, true, false));
		fBtnReset.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onChangeAdjustType();
			}
		});

		/*
		 * button: compare values
		 */
		fBtnCompare = new Button(buttonContainer, SWT.NONE);
		fBtnCompare.setText(Messages.Dlg_Adjust_Altitude_Button_show_original_values);
		fBtnCompare
				.setToolTipText(Messages.Dlg_Adjust_Altitude_Button_show_original_values_tooltip);
		fBtnCompare.setLayoutData(new GridData(SWT.TRAIL, SWT.DEFAULT, false, false));
		fBtnCompare.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent e) {
				if (fIsInitialAltiDisplayed == false && e.character == ' ') {
					fIsInitialAltiDisplayed = true;
					setOriginalAltitudeValues();
					fTourChart.updateChart(!fChkScaleYAxis.getSelection());
				}
			}

			public void keyReleased(KeyEvent e) {
				setInitialAltitudeValues();
				fTourChart.updateChart(!fChkScaleYAxis.getSelection());
				fIsInitialAltiDisplayed = false;
			}
		});

		fBtnCompare.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent e) {
				if (fIsInitialAltiDisplayed == false) {
					fIsInitialAltiDisplayed = true;
					setOriginalAltitudeValues();
					fTourChart.updateChart(!fChkScaleYAxis.getSelection());
				}
			}

			public void mouseUp(MouseEvent e) {
				setInitialAltitudeValues();
				fTourChart.updateChart(!fChkScaleYAxis.getSelection());
				fIsInitialAltiDisplayed = false;
			}
		});

		return fDialogArea;
	}

	@SuppressWarnings("unused")//$NON-NLS-1$
	private void dumpMinMax(int[] altiSrc, String place) {

		int minAltiSrc1 = altiSrc[0];
		int maxAltiSrc1 = altiSrc[0];
		for (int altitude : altiSrc) {
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

			setMessage(Messages.Dlg_Adjust_Altitude_Message_select_type);
			break;

		case ADJUST_ALTITUDE_START_AND_END:

			fSpinnerNewStartAlti.setEnabled(true);
			fSpinnerNewMaxAlti.setEnabled(true);

			fRadioKeepStart.setEnabled(true);
			fRadioKeepBottom.setEnabled(true);

			setMessage(Messages.Dlg_Adjust_Altitude_Message_adjust_start_and_end);
			break;

		case ADJUST_ALTITUDE_WHOLE_TOUR:

			fSpinnerNewStartAlti.setEnabled(true);

			setMessage(Messages.Dlg_Adjust_Altitude_Message_adjust_whole_tour);
			break;

		case ADJUST_ALTITUDE_END:

			fSpinnerNewEndAlti.setEnabled(true);

			setMessage(Messages.Dlg_Adjust_Altitude_Message_adjust_end);
			break;

		case ADJUST_ALTITUDE_MAX_HEIGHT:

			fSpinnerNewMaxAlti.setEnabled(true);

			fRadioKeepStart.setEnabled(true);
			fRadioKeepBottom.setEnabled(true);

			setMessage(Messages.Dlg_Adjust_Altitude_Message_adjust_max);
			break;

		default:
			break;
		}
	}

	/**
	 * @return Returns the altitude serie from the chart
	 */
	private int[] getAltitudeSerie() {

		// get altitude data from the data model
		ArrayList<ChartDataYSerie> yDataList = fSelectedTourChart.getChartDataModel().getYData();
		int[] altitudeSerie = null;
		for (ChartDataYSerie yData : yDataList) {
			Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
			if (yDataInfo == TourManager.GRAPH_ALTITUDE) {
				altitudeSerie = yData.getHighValues()[0];
				break;
			}
		}
		return altitudeSerie;
	}

	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(
				getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	/**
	 * @return Returns the dialog settings for this dialog
	 */
	private IDialogSettings getDialogSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	/**
	 * initialize dialog
	 */
	public void init() {

		restoreDialogSettings();

		onChangeAdjustType();

		fSpinnerNewStartAlti.setSelection(fOldStartAlti);

		// set focus to cancel button
		getButton(IDialogConstants.CANCEL_ID).setFocus();

		// set button width
		int btnCompareWidth = fBtnCompare.getBounds().width;
		int btnResetWidth = fBtnReset.getBounds().width;
		int newWidth = Math.max(btnCompareWidth, btnResetWidth);

		GridData gd;
		gd = (GridData) fBtnCompare.getLayoutData();
		gd.widthHint = newWidth;

		gd = (GridData) fBtnReset.getLayoutData();
		gd.widthHint = newWidth;

		fDialogArea.layout(true, true);
	}

	protected void okPressed() {

		// confirm to save the changes
		MessageBox msgBox = new MessageBox(fTourChart.getShell(), SWT.ICON_WORKING
				| SWT.YES
				| SWT.NO);
		msgBox.setText(Messages.Dlg_Adjust_Altitude_Dlg_save_tour_title);
		msgBox.setMessage(Messages.Dlg_Adjust_Altitude_Dlg_save_tour_message);

		if (msgBox.open() == SWT.YES) {
			fTourChart.fTourData.computeMaxAltitude();
			TourDatabase.saveTour(fTourChart.fTourData);
			fSelectedTourChart.updateChart(true);

			super.okPressed();
		}

	}

	private void onChangeAdjustType() {

		int[] altitudeSerie = getAltitudeSerie();

		if (altitudeSerie == null) {
			return;
		}

		resetAltitude();

		enableFields();

		onChangeAltitude(null);
	}

	private void onChangeAltitude(Integer altiFlag) {

		// calcuate new altitude values
		adjustAltitude(fTourChart.fTourData, altiFlag);

		// set new values into the fields which can change the altitude
		setAltiFieldValues();

		fTourChart.updateChart(!fChkScaleYAxis.getSelection());
	}

	/**
	 * reset altitudes to it's original values
	 */
	private void resetAltitude() {

		restoreOriginalAltitudeValues();

		final int startAlti = fOriginalAltitudes[0];
		final int endAlti = fOriginalAltitudes[fOriginalAltitudes.length - 1];

		// calculate max altitude
		int maxAlti = startAlti;
		for (int altitude : fOriginalAltitudes) {
			if (altitude > maxAlti) {
				maxAlti = altitude;
			}
		}

		fOldStartAlti = startAlti;

		fLblOldStartAlti.setText(Integer.toString(startAlti));
		fLblOldStartAlti.pack(true);

		fLblOldEndAlti.setText(Integer.toString(endAlti));
		fLblOldEndAlti.pack(true);

		fLblOldMaxAlti.setText(Integer.toString(maxAlti));
		fLblOldMaxAlti.pack(true);

//		fIsSpinnerChangedByUser = false;
		{
			fSpinnerNewStartAlti.setSelection(startAlti);
			fSpinnerNewEndAlti.setSelection(endAlti);
			fSpinnerNewMaxAlti.setSelection(maxAlti);
		}
//		fIsSpinnerChangedByUser = true;
	}

	private void restoreDialogSettings() {

		IDialogSettings dlgSettings = getDialogSettings();

		try {
			fComboAdjustType.select(dlgSettings.getInt(DIALOG_SETTINGS_ADJUST_TYPE));
		} catch (Exception e) {
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

		int[] altitudeSerie = getAltitudeSerie();

		if (altitudeSerie == null | fOriginalAltitudes == null) {
			return;
		}

		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fOriginalAltitudes[altiIndex];
		}
	}

	private void saveDialogSettings() {

		IDialogSettings dlgSettings = getDialogSettings();

		dlgSettings.put(DIALOG_SETTINGS_ADJUST_TYPE, fComboAdjustType.getSelectionIndex());
		dlgSettings.put(DIALOG_SETTINGS_SCALE_YAXIS, fChkScaleYAxis.getSelection());
		dlgSettings.put(DIALOG_SETTINGS_KEEP_START, fRadioKeepStart.getSelection());
	}

	/**
	 * set the altitude fields with the current altitude values
	 */
	private void setAltiFieldValues() {

		int[] altiSerie = fTourChart.fTourData.altitudeSerie;
		int altiStart = altiSerie[0];
		int altiEnd = altiSerie[altiSerie.length - 1];

		// get max altitude
		int altiMax = altiSerie[0];
		for (int altitude : altiSerie) {
			if (altitude > altiMax) {
				altiMax = altitude;
			}
		}

		// keep values
		fOldAltiInputStart = altiStart;
		fOldAltiInputMax = altiMax;

		/*
		 * prevent to fire the selection event in the spinner when a selection is set, this would
		 * cause endless loops
		 */
//		fIsSpinnerChangedByUser = false;
		{
			fSpinnerNewStartAlti.setSelection(altiStart);
			fSpinnerNewEndAlti.setSelection(altiEnd);
			fSpinnerNewMaxAlti.setSelection(altiMax);
		}
//		fIsSpinnerChangedByUser = true;

		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}

	private void setInitialAltitudeValues() {

		int[] altitudeSerie = getAltitudeSerie();

		if (altitudeSerie == null || fInitialAltitude == null) {
			return;
		}

		// set altitude to the initial values
		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fInitialAltitude[altiIndex];
		}
	}

	private void setOriginalAltitudeValues() {

		int[] altitudeSerie = getAltitudeSerie();

		if (altitudeSerie == null || fOriginalAltitudes == null) {
			return;
		}

		// set original values
		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
			altitudeSerie[altiIndex] = fOriginalAltitudes[altiIndex];
		}
	}
}
