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
/**
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogSelectSRTMColors extends TitleAreaDialog {

	private static final int		maxColor		= 100;

	private final IDialogSettings	fDialogSettings;

	private Text					fTxtProfileName;
	private Text					fTxtTilePath;
	private Composite				fContainerColorButtons;
	private ColorChooser			fColorChooser;

	private static Label[]			colorLabel		= new Label[maxColor];
	private static Text[]			elevFields		= new Text[maxColor];
	private static Button[]			checkButtons	= new Button[maxColor];

	private SRTMProfile				fProfile;

	public DialogSelectSRTMColors(final Shell parentShell, final SRTMProfile profile) {

		super(parentShell);

		this.fProfile = profile;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fDialogSettings = Activator.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_adjust_srtm_colors_dialog_title);
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_adjust_srtm_colors_dialog_area_title);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		Button button;

		/*
		 * button: add
		 */
		button = createButton(parent, 42, Messages.dialog_adjust_srtm_colors_button_add, false);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final int rgbVertexListSize = fProfile.getVertexList().size();
				disposeFields(rgbVertexListSize);
				final RGBVertex rgbVertex = new RGBVertex();
				rgbVertex.setRGB(fColorChooser.getRGB());
				fProfile.getVertexList().add(rgbVertexListSize, rgbVertex);
				setFields();
			}

		});

		/*
		 * button: remove
		 */
		button = createButton(parent, 43, Messages.dialog_adjust_srtm_colors_button_remove, false);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ArrayList<RGBVertex> vertexList = fProfile.getVertexList();
				final int rgbVertexListSize = vertexList.size();
				for (int ix = rgbVertexListSize - 1; ix >= 0; ix--) {
					if (checkButtons[ix].getSelection()) {
						vertexList.remove(ix);
					}
				}
				disposeFields(rgbVertexListSize);
				setFields();
			}
		});

		/*
		 * button: sort
		 */
		button = createButton(parent, 44, Messages.dialog_adjust_srtm_colors_button_sort, false);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				sortColors();
			}
		});

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.dialog_adjust_srtm_colors_button_update);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);
		updateUI();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite propertyContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(propertyContainer);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(propertyContainer);
		{
			/*
			 * lable: profile name
			 */
			Label label = new Label(propertyContainer, SWT.NONE);
			label.setText(Messages.dialog_adjust_srtm_colors_label_profile_name);

			/*
			 * text: profile name
			 */
			fTxtProfileName = new Text(propertyContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtProfileName);

			/*
			 * lable: tile path
			 */
			label = new Label(propertyContainer, SWT.NONE);
			label.setText(Messages.dialog_adjust_srtm_colors_label_tile_path);

			/*
			 * text: tile path
			 */
			fTxtTilePath = new Text(propertyContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtTilePath);
		}

		final Composite colorContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(colorContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(colorContainer);
		{
			fContainerColorButtons = new Composite(colorContainer, SWT.NONE);

			final Composite containerColorChooser = new Composite(colorContainer, SWT.NONE);
			fColorChooser = new ColorChooser(containerColorChooser);
		}

		setFields();

		fColorChooser.createUI();
	}

	private void disposeFields(final int length) {
		for (int ix = 0; ix < length; ix++) {
			colorLabel[ix].dispose();
			elevFields[ix].dispose();
			checkButtons[ix].dispose();
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	public SRTMProfile getRgbVertexList() {
		return fProfile;
	}

	@Override
	protected void okPressed() {

		sortColors();

		fProfile.setProfileName(fTxtProfileName.getText());
		fProfile.setProfilePath(fTxtTilePath.getText());

		super.okPressed();
	}

	private void setFields() {

		fContainerColorButtons.pack(); // necessary if # of fields doesn't change!
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.TOP).applyTo(fContainerColorButtons);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(fContainerColorButtons);
		final GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);
		if (fProfile.getVertexList().size() == 0)
			return;
		for (int ix = fProfile.getVertexList().size() - 1; ix >= 0; ix--) {
			elevFields[ix] = new Text(fContainerColorButtons, SWT.SINGLE | SWT.TRAIL);
			elevFields[ix].setText("" + (fProfile.getVertexList().get(ix)).getElevation()); //$NON-NLS-1$
			elevFields[ix].setEditable(true);
			GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).grab(true, false).applyTo(elevFields[ix]);
			colorLabel[ix] = new Label(fContainerColorButtons, SWT.CENTER);
			colorLabel[ix].setBackground(new Color(fContainerColorButtons.getDisplay(),
					(fProfile.getVertexList().get(ix)).getRGB()));
			final GridData gridDataCL = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);
			gridDataCL.widthHint = 70;
			gridDataCL.heightHint = 20;
			colorLabel[ix].setLayoutData(gridDataCL);
			colorLabel[ix].setToolTipText("" //$NON-NLS-1$
					+ (fProfile.getVertexList().get(ix)).getRGB().red
					+ "/" //$NON-NLS-1$
					+ (fProfile.getVertexList().get(ix)).getRGB().green
					+ "/" //$NON-NLS-1$
					+ (fProfile.getVertexList().get(ix)).getRGB().blue);
			colorLabel[ix].addMouseListener(new MouseListener() {
				public void mouseDoubleClick(final MouseEvent e) {}

				public void mouseDown(final MouseEvent e) {
					final Color labelColor = new Color(fContainerColorButtons.getDisplay(), fColorChooser.getRGB());
					((Label) (e.widget)).setBackground(labelColor);
					sortColors();
				}

				public void mouseUp(final MouseEvent e) {}
			});

			checkButtons[ix] = new Button(fContainerColorButtons, SWT.CHECK);
			checkButtons[ix].setLayoutData(gridData);
			checkButtons[ix].setToolTipText(Messages.dialog_adjust_srtm_colors_checkbutton_ttt);
			checkButtons[ix].addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					switch (e.type) {
					case SWT.Selection:
						int checked = 0;
						for (int ix = 0; ix < fProfile.getVertexList().size(); ix++)
							if (checkButtons[ix].getSelection())
								checked++;
						if (checked == fProfile.getVertexList().size() - 2) {
							for (int ix = 0; ix < fProfile.getVertexList().size(); ix++)
								if (!checkButtons[ix].getSelection())
									checkButtons[ix].setEnabled(false);
						} else {
							for (int ix = 0; ix < fProfile.getVertexList().size(); ix++)
								checkButtons[ix].setEnabled(true);
						}
						break;
					}
				}
			});
		} // end for

		if (fProfile.getVertexList().size() <= 2)
			for (int ix = 0; ix < fProfile.getVertexList().size(); ix++)
				checkButtons[ix].setEnabled(false);

		fContainerColorButtons.pack();
	}

	@SuppressWarnings("unchecked")
	private void sortColors() {
		final int rgbVertexListSize = fProfile.getVertexList().size();
		fProfile.getVertexList().clear();
		for (int ix = 0, ixn = 0; ix < rgbVertexListSize; ix++) {
			if (elevFields[ix].getText().equals("")) //$NON-NLS-1$
				continue; // remove empty fields
			final Long elev = new Long(elevFields[ix].getText());
			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setElev(elev.longValue());
			rgbVertex.setRGB(colorLabel[ix].getBackground().getRGB());
			fProfile.getVertexList().add(ixn, rgbVertex);
			ixn++;
		}
		Collections.sort(fProfile.getVertexList());
		disposeFields(rgbVertexListSize);
		setFields();
	}

	private void updateUI() {

		fTxtProfileName.setText(fProfile.getProfileName());
		fTxtTilePath.setText(fProfile.getProfilePath());
	}
}
