/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.ui.MessageDialogPage;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour
 * chart or from the tree viewer
 */
public class AdjustAltitudeDialog extends TrayDialog {

	static final int				ALTITUDE_ADJUSTMENT_ALL			= 10;
	static final int				ALTITUDE_ADJUSTMENT_END			= 20;
	static final int				ALTITUDE_ADJUSTMENT_MAX_HEIGHT	= 30;

	private TourChart				fTourChart;
	private IStructuredSelection	fSelection;

	private Button					fAdjustAll;
	private Button					fAdjustEnd;
	private Button					fAdjustMaxHight;

	private Label					fNewAltitudeLabel;
	private IntegerFieldEditor		fNewAltitudeEditor;

	private Text					fOldStartAltitude;
	private Text					fOldEndAltitude;
	private Text					fOldMaxHeightAltitude;

	private Composite				fFieldContainer;

	// keep selected values which can be read from outside the dialog
	int								fSelectedAdjustment;
	int								fNewAltitude;

	public AdjustAltitudeDialog(Shell parentShell, TourChart tourChart) {
		super(parentShell);
		fTourChart = tourChart;
	}

	public AdjustAltitudeDialog(Shell parentShell, IStructuredSelection selection) {
		super(parentShell);
		fSelection = selection;
	}

	protected Control createContents(Composite parent) {
		Control container = super.createContents(parent);

		// initialize dialog
		fAdjustAll.setSelection(true);
		setupDialog();

		return container;
	}

	protected Control createDialogArea(Composite parent) {

		Composite container = (Composite) super.createDialogArea(parent);

		// group: zoom options
		Group groupAdjustOptions = new Group(container, SWT.NONE);
		groupAdjustOptions.setText("Adjust Tour Altitude");
		groupAdjustOptions.setLayout(new GridLayout());

		// radio: adjust the whole tour
		fAdjustAll = new Button(groupAdjustOptions, SWT.RADIO);
		fAdjustAll.setText("Adjust the altitude evenly distributed for the whole tour");
		fAdjustAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setupDialog();
			}
		});

		// radio: adjust end
		fAdjustEnd = new Button(groupAdjustOptions, SWT.RADIO);
		fAdjustEnd.setText("Adjust the END altitude and keep the start altitude");
		fAdjustEnd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setupDialog();
			}
		});

		// radio: adjust max hight
		fAdjustMaxHight = new Button(groupAdjustOptions, SWT.RADIO);
		fAdjustMaxHight
				.setText("Adjust MAX HEIGHT altitude and keep the start altitude");
		fAdjustMaxHight.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setupDialog();
			}
		});

		GridData gd;

		fFieldContainer = new Composite(container, SWT.NONE);
		fFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// fFieldContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		if (fTourChart != null) {
			// start altitude
			Label label = new Label(fFieldContainer, SWT.NONE);
			label.setText("Current Start Altitude:");

			fOldStartAltitude = new Text(fFieldContainer, SWT.BORDER);
			fOldStartAltitude.setEnabled(false);
			UI.setWidth(fOldStartAltitude, convertWidthInCharsToPixels(5));

			// end altitude
			label = new Label(fFieldContainer, SWT.NONE);
			label.setText("Current End Altitude:");

			fOldEndAltitude = new Text(fFieldContainer, SWT.BORDER);
			fOldEndAltitude.setEnabled(false);
			UI.setWidth(fOldEndAltitude, convertWidthInCharsToPixels(5));

			// max height altitude
			label = new Label(fFieldContainer, SWT.NONE);
			label.setText("Current Max Height Altitude:");

			fOldMaxHeightAltitude = new Text(fFieldContainer, SWT.BORDER);
			fOldMaxHeightAltitude.setEnabled(false);
			UI.setWidth(fOldMaxHeightAltitude, convertWidthInCharsToPixels(5));
		}

		// label: title
		fNewAltitudeLabel = new Label(fFieldContainer, SWT.NONE);
		fNewAltitudeLabel.setText("");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fNewAltitudeLabel.setLayoutData(gd);
		fNewAltitudeLabel.setFont(JFaceResources.getFontRegistry().getBold(
				JFaceResources.DIALOG_FONT));

		// input: altitude difference
		fNewAltitudeEditor = new IntegerFieldEditor("", "", fFieldContainer);
		fNewAltitudeEditor.setTextLimit(4);
		fNewAltitudeEditor.setValidRange(0, 6000);
		UI.setFieldWidth(
				fFieldContainer,
				fNewAltitudeEditor,
				convertWidthInCharsToPixels(5));
		fNewAltitudeEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				validateDialog();
			}
		});

		DialogPage dialogPage = new MessageDialogPage(container) {};

		fNewAltitudeEditor.setPage(dialogPage);

		Dialog.applyDialogFont(container);

		return container;
	}

	protected void setupDialog() {

		if (fTourChart != null) {

			ArrayList<ChartDataYSerie> yDataList = fTourChart
					.getChartDataModel()
					.getYData();

			int[] altitudeSerie = null;
			for (ChartDataYSerie yData : yDataList) {
				Integer yDataInfo = (Integer) yData
						.getCustomData(ChartDataYSerie.YDATA_INFO);
				if (yDataInfo == TourManager.GRAPH_ALTITUDE) {
					altitudeSerie = yData.getHighValues()[0];
				}
			}

			if (altitudeSerie == null) {
				return;
			}

			fOldStartAltitude.setText(Integer.toString(altitudeSerie[0]));
			fOldEndAltitude.setText(Integer
					.toString(altitudeSerie[altitudeSerie.length - 1]));

			// calculate max altitude
			int maxHeight = altitudeSerie[0];
			for (int altitude : altitudeSerie) {
				if (altitude > maxHeight) {
					maxHeight = altitude;
				}
			}
			fOldMaxHeightAltitude.setText(Integer.toString(maxHeight));
		}

		Label label = fNewAltitudeEditor.getLabelControl(fFieldContainer);
		if (fAdjustAll.getSelection()) {
			fNewAltitudeLabel.setText("Evenly distributed");
			label.setText("New Start Altitude:");
		} else if (fAdjustEnd.getSelection()) {
			fNewAltitudeLabel.setText("Adjust End");
			label.setText("New End Altitude:");
		} else if (fAdjustMaxHight.getSelection()) {
			fNewAltitudeLabel.setText("Adjust Max Height");
			label.setText("New Max Altitude:");
		}

		fFieldContainer.layout();
	}

	private void validateDialog() {

		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(fNewAltitudeEditor.isValid());

		if (fAdjustAll.getSelection()) {
			fSelectedAdjustment = ALTITUDE_ADJUSTMENT_ALL;
		} else if (fAdjustEnd.getSelection()) {
			fSelectedAdjustment = ALTITUDE_ADJUSTMENT_END;
		} else if (fAdjustMaxHight.getSelection()) {
			fSelectedAdjustment = ALTITUDE_ADJUSTMENT_MAX_HEIGHT;
		}

		fNewAltitude = fNewAltitudeEditor.getIntValue();
	}

}
