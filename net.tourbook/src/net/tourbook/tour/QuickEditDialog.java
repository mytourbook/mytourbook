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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class QuickEditDialog extends TitleAreaDialog {

	private Text					fTextTitle;
	private Text					fTextStartLocation;
	private Text					fTextEndLocation;
	private Text					fTextDescription;

	private TourData				fTourData;

	private final IDialogSettings	fDialogSettings;

	private static final Calendar	fCalendar		= GregorianCalendar.getInstance();
	private static final DateFormat	fDateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);
	private static final DateFormat	fTimeFormatter	= DateFormat.getTimeInstance(DateFormat.SHORT);

	public QuickEditDialog(Shell parentShell, TourData tourData) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fTourData = tourData;
		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	public void create() {

		super.create();

		getShell().setText(Messages.dialog_quick_edit_dialog_title);
		setTitle(Messages.dialog_quick_edit_dialog_area_title);

		fCalendar.set(fTourData.getStartYear(),
				fTourData.getStartMonth() - 1,
				fTourData.getStartDay(),
				fTourData.getStartHour(),
				fTourData.getStartMinute());

		setMessage(fDateFormatter.format(fCalendar.getTime()) + "  " + fTimeFormatter.format(fCalendar.getTime())); //$NON-NLS-1$

	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		// set field content
		fTextTitle.setText(fTourData.getTourTitle());
		fTextStartLocation.setText(fTourData.getTourStartPlace());
		fTextEndLocation.setText(fTourData.getTourEndPlace());
		fTextDescription.setText(fTourData.getTourDescription());

		return dlgAreaContainer;
	}

	private void createUI(Composite parent) {

		Label label;
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);

		final PixelConverter pixelConverter = new PixelConverter(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

		// title
		label = new Label(container, SWT.NONE);
		label.setText(Messages.Tour_Properties_Label_tour_title);
		fTextTitle = new Text(container, SWT.BORDER);
		fTextTitle.setLayoutData(gd);

		// description
		label = new Label(container, SWT.NONE);
		label.setText(Messages.Tour_Properties_Label_description);
		label.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
		fTextDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory//
		.fillDefaults()
				.grab(true, true)
				.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(4))
				.applyTo(fTextDescription);

		// start location
		label = new Label(container, SWT.NONE);
		label.setText(Messages.Tour_Properties_Label_start_location);
		fTextStartLocation = new Text(container, SWT.BORDER);
		fTextStartLocation.setLayoutData(gd);

		// end location
		label = new Label(container, SWT.NONE);
		label.setText(Messages.Tour_Properties_Label_end_location);
		fTextEndLocation = new Text(container, SWT.BORDER);
		fTextEndLocation.setLayoutData(gd);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	@Override
	protected void okPressed() {

		/*
		 * update tourdata from the fields
		 */
		fTourData.setTourTitle(fTextTitle.getText().trim());
		fTourData.setTourDescription(fTextDescription.getText().trim());
		fTourData.setTourStartPlace(fTextStartLocation.getText().trim());
		fTourData.setTourEndPlace(fTextEndLocation.getText().trim());

		super.okPressed();
	}
}
