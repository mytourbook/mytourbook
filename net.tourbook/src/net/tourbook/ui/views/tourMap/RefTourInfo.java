/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMap;

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class RefTourInfo extends Composite {

	private Label		txtDistance;
	private Label		txtAltitudeUp;
	private Label		txtAltitudeDown;

	final NumberFormat	nf	= NumberFormat.getNumberInstance();

	public RefTourInfo(Composite parent, int style) {
		super(parent, style);
		createLayout();
	}

	/**
	 * This method initializes this
	 */
	private void createLayout() {

		GridLayout gl = new GridLayout(3, false);
		setLayout(gl);

		GridData gd = new GridData();
		gd.widthHint = 50;

		Label label;

		label = new Label(this, SWT.NONE);
		label.setText(Messages.Tour_Map_Label_distance);
		txtDistance = new Label(this, SWT.TRAIL);
		txtDistance.setLayoutData(gd);
		label = new Label(this, SWT.NONE);
		label.setText(Messages.Tour_Map_Label_km);

		label = new Label(this, SWT.NONE);
		label.setText(Messages.Tour_Map_Label_altitude_up);
		txtAltitudeUp = new Label(this, SWT.TRAIL);
		txtAltitudeUp.setLayoutData(gd);
		label = new Label(this, SWT.NONE);
		label.setText(Messages.Tour_Map_Label_m);

		label = new Label(this, SWT.NONE);
		label.setText(Messages.Tour_Map_Label_altitude_down);
		txtAltitudeDown = new Label(this, SWT.TRAIL);
		txtAltitudeDown.setLayoutData(gd);
		label = new Label(this, SWT.NONE);
		label.setText(Messages.Tour_Map_Label_m);

	}

	void updateInfo(TourCompareConfig refTourChartData) {

		TourReference refTour = refTourChartData.getRefTour();

		TourData refTourData = refTourChartData.getRefTourData();
		int startValueIndex = refTour.getStartValueIndex();
		int endValueIndex = refTour.getEndValueIndex();

		int distance = refTourData.distanceSerie[endValueIndex]
				- refTourData.distanceSerie[startValueIndex];

		nf.setMinimumFractionDigits(2);
		txtDistance.setText(nf.format((float) distance / 1000));

		/*
		 * calculate the altitude up/down values
		 */
		int altUp = 0;
		int altDown = 0;
		int[] altSerie = refTourData.altitudeSerie;
		int lastAltitude = altSerie[startValueIndex];

		for (int serieIndex = startValueIndex; serieIndex <= endValueIndex; serieIndex++) {
			int currentAltitude = altSerie[serieIndex];
			int altDiff = currentAltitude - lastAltitude;
			lastAltitude = currentAltitude;

			if (altDiff < 0) {
				altDown -= altDiff;
			} else {
				altUp += altDiff;
			}
		}

		nf.setMinimumFractionDigits(0);
		txtAltitudeUp.setText(nf.format(altUp));
		txtAltitudeDown.setText(nf.format(altDown));
	}
}
