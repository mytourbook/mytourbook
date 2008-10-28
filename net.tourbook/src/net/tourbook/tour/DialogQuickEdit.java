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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.dialogs.IDialogConstants;
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

public class DialogQuickEdit extends TitleAreaDialog implements ITourProvider {

	private Text					fTextTitle;
	private Text					fTextDescription;

	private TourData				fTourData;

	private final IDialogSettings	fDialogSettings;

	private static final Calendar	fCalendar			= GregorianCalendar.getInstance();
	private static final DateFormat	fDateFormatter		= DateFormat.getDateInstance(DateFormat.FULL);
	private static final DateFormat	fTimeFormatter		= DateFormat.getTimeInstance(DateFormat.SHORT);

	/**
	 * this width is used as a hint for the width of the description field, this value also
	 * influences the width of the columns in this editor
	 */
	private int						fTextColumnWidth	= 150;

	public DialogQuickEdit(final Shell parentShell, final TourData tourData) {

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
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		String okText = null;

		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
		if (tourDataEditor != null && tourDataEditor.isDirty() && tourDataEditor.getTourData() == fTourData) {
			okText = Messages.dialog_quick_edit_dialog_update;
		}

		if (okText == null) {
			okText = Messages.dialog_quick_edit_dialog_save;
		}

		getButton(IDialogConstants.OK_ID).setText(okText);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(dlgAreaContainer);

		updateUI();

		return dlgAreaContainer;
	}

	private void createUI(final Composite parent) {

		Label label;
		final GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

		// title
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_editor_label_tour_title);
		fTextTitle = new Text(container, SWT.BORDER);
		fTextTitle.setLayoutData(gd);

		// description
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_editor_label_description);
		label.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
		fTextDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory//
		.fillDefaults()
				.grab(true, true)
				.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(4))
				.applyTo(fTextDescription);

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(fTourData);

		return selectedTours;
	}

	@Override
	protected void okPressed() {

		/*
		 * update tourdata from the fields
		 */
		fTourData.setTourTitle(fTextTitle.getText().trim());
		fTourData.setTourDescription(fTextDescription.getText().trim());

		super.okPressed();
	}

	private void updateUI() {

		// set field content
		fTextTitle.setText(fTourData.getTourTitle());
		fTextDescription.setText(fTourData.getTourDescription());

	}
}
