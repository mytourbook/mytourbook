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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogSelectSRTMColors extends Dialog {

	private static final int		maxColor		= 100;

	private final IDialogSettings	fDialogSettings;

	private Text					fTxtProfileName;
	private Text					fTxtTilePath;

	private ImageCanvas				fProfileImageCanvas;
	private ColorChooser			fColorChooser;
	private Composite				fContainerVertexFields;
	private Button					fBtnRemove;

	private static Label[]			colorLabel		= new Label[maxColor];
	private static Text[]			elevFields		= new Text[maxColor];
	private static Button[]			checkButtons	= new Button[maxColor];

	private SRTMProfile				fProfile;
	private ArrayList<RGBVertex>	fVertexList;

	public DialogSelectSRTMColors(final Shell parentShell, final SRTMProfile profile) {

		super(parentShell);

		fProfile = profile;
		fProfile.setVertical();
		fVertexList = profile.getVertexList();

		// make dialog resizable
		setShellStyle(getShellStyle() /* | SWT.RESIZE */);

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

		paintProfileImage();

		enableActions();
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

				final int rgbVertexListSize = fVertexList.size();

				final RGBVertex rgbVertex = new RGBVertex();
				rgbVertex.setRGB(fColorChooser.getRGB());
				fVertexList.add(rgbVertexListSize, rgbVertex);

				disposeVertexFields(rgbVertexListSize);
				createUIVertexFieds();

				paintProfileImage();
				
				enableActions();
			}

		});

		/*
		 * button: remove
		 */
		fBtnRemove = createButton(parent, 43, Messages.dialog_adjust_srtm_colors_button_remove, false);
		fBtnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				final int rgbVertexListSize = fVertexList.size();

				for (int ix = rgbVertexListSize - 1; ix >= 0; ix--) {
					if (checkButtons[ix].getSelection()) {
						fVertexList.remove(ix);
					}
				}

				disposeVertexFields(rgbVertexListSize);
				createUIVertexFieds();

				paintProfileImage();
				
				enableActions();
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
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(propertyContainer);
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
		GridDataFactory.fillDefaults().indent(0, 10).applyTo(colorContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(20, 0).applyTo(colorContainer);
		{
			/*
			 * profile image
			 */
			fProfileImageCanvas = new ImageCanvas(colorContainer, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().grab(false, true).hint(100, SWT.DEFAULT).applyTo(fProfileImageCanvas);

			/*
			 * vertex fields
			 */
			fContainerVertexFields = new Composite(colorContainer, SWT.NONE);
			createUIVertexFieds();

			/*
			 * color chooser
			 */
			fColorChooser = new ColorChooser(new Composite(colorContainer, SWT.NONE));
			fColorChooser.createUI();
		}
	}

	private void createUIVertexFieds() {

		fContainerVertexFields.pack(); // necessary if # of fields doesn't change!

		GridDataFactory.fillDefaults()/* .indent(50, 0) */.align(SWT.CENTER, SWT.TOP).applyTo(fContainerVertexFields);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(fContainerVertexFields);

		final int vertexSize = fVertexList.size();
		if (vertexSize == 0) {
			return;
		}

		final GridData gdCheckbox = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);

		final SelectionAdapter checkboxListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				int checked = 0;
				for (int ix = 0; ix < vertexSize; ix++) {
					if (checkButtons[ix].getSelection()) {
						checked++;
					}
				}

				if (checked == vertexSize - 2) {
					for (int ix = 0; ix < vertexSize; ix++) {
						if (!checkButtons[ix].getSelection()) {
							checkButtons[ix].setEnabled(false);
						}
					}
				} else {
					for (int ix = 0; ix < vertexSize; ix++) {
						checkButtons[ix].setEnabled(true);
					}
				}

				enableActions();
			}
		};

		final Display display = fContainerVertexFields.getDisplay();

		for (int vertexIndex = vertexSize - 1; vertexIndex >= 0; vertexIndex--) {

			final RGBVertex vertex = fVertexList.get(vertexIndex);

			/*
			 * text: elevation
			 */
			final Text txtElevation = elevFields[vertexIndex] = new Text(fContainerVertexFields, SWT.SINGLE | SWT.TRAIL);
			GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).grab(true, false).applyTo(txtElevation);
			txtElevation.setText(UI.EMPTY_STRING + vertex.getElevation());
			txtElevation.setEditable(true);

			/*
			 * color label
			 */
			final Label lblColor = colorLabel[vertexIndex] = new Label(fContainerVertexFields, SWT.CENTER);

			final GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
			gd.widthHint = 70;
			gd.heightHint = 20;
			lblColor.setLayoutData(gd);

			final RGB vertexRGB = vertex.getRGB();
			lblColor.setToolTipText("" + vertexRGB.red + "/" + vertexRGB.green + "/" + vertexRGB.blue);
			lblColor.setBackground(new Color(display, vertexRGB));
			lblColor.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(final MouseEvent e) {
					final Color labelColor = new Color(display, fColorChooser.getRGB());
					((Label) (e.widget)).setBackground(labelColor);
					sortColors();
				}
			});

			/*
			 * checkbox
			 */
			final Button checkbox = checkButtons[vertexIndex] = new Button(fContainerVertexFields, SWT.CHECK);
			checkbox.setLayoutData(gdCheckbox);
			checkbox.setToolTipText(Messages.dialog_adjust_srtm_colors_checkbutton_ttt);
			checkbox.addSelectionListener(checkboxListener);
		}

		/*
		 * disable checkboxes when only 2 colors are available
		 */
		if (vertexSize <= 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				checkButtons[ix].setEnabled(false);
			}
		}

		fContainerVertexFields.pack();
	}

	private void disposeVertexFields(final int length) {
		for (int ix = 0; ix < length; ix++) {
			colorLabel[ix].dispose();
			elevFields[ix].dispose();
			checkButtons[ix].dispose();
		}
	}

	private void enableActions() {

		final int vertexSize = fVertexList.size();

		int checked = 0;
		for (int ix = 0; ix < vertexSize; ix++) {
			if (checkButtons[ix].getSelection()) {
				checked++;
			}
		}

		fBtnRemove.setEnabled(checked > 0 && vertexSize > 2);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTLOCATION;
	}

	public SRTMProfile getSRTMProfile() {
		return fProfile;
	}

	@Override
	protected void okPressed() {

		sortColors();

		fProfile.setProfileName(fTxtProfileName.getText());
		fProfile.setProfilePath(fTxtTilePath.getText());

		super.okPressed();
	}

	private void paintProfileImage() {
		final Rectangle imageBounds = fProfileImageCanvas.getBounds();
		fProfile.createImage(Display.getCurrent(), imageBounds.width, imageBounds.height);
		fProfileImageCanvas.paintImage(fProfile.getImage());
	}

	@SuppressWarnings("unchecked")
	private void sortColors() {

		final int rgbVertexListSize = fVertexList.size();
		fVertexList.clear();

		for (int ix = 0, ixn = 0; ix < rgbVertexListSize; ix++) {
			if (elevFields[ix].getText().equals(UI.EMPTY_STRING)) {
				continue; // remove empty fields
			}

			final Long elev = new Long(elevFields[ix].getText());
			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setElev(elev.longValue());
			rgbVertex.setRGB(colorLabel[ix].getBackground().getRGB());
			fVertexList.add(ixn, rgbVertex);
			ixn++;
		}
		Collections.sort(fVertexList);

		disposeVertexFields(rgbVertexListSize);
		createUIVertexFieds();
		
		paintProfileImage();
	}

	private void updateUI() {

		fTxtProfileName.setText(fProfile.getProfileName());
		fTxtTilePath.setText(fProfile.getProfilePath());
	}
}
