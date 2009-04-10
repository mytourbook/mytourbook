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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
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

	private final IDialogSettings	fDialogSettings;

	private Text					fTxtProfileName;
	private Text					fTxtTilePath;

	private ImageCanvas				fProfileImageCanvas;
	private Composite				fColorContainer;
	private ColorChooser			fColorChooser;
	private Composite				fVertexOuterContainer;
	private ScrolledComposite		fVertexScrolledContainer;

	private Text[]					elevFields;
	private Label[]					colorLabel;
	private Button[]				checkButtons;

	private Button					fBtnRemove;

	private SRTMProfile				fProfile;
	private ArrayList<RGBVertex>	fVertexList;

	/**
	 * keep colors which must be disposed when the dialog gets disposed
	 */
	private ArrayList<Color>		fColorList	= new ArrayList<Color>();

	public DialogSelectSRTMColors(final Shell parentShell, final SRTMProfile profile) {

		super(parentShell);

		fProfile = profile;
		fProfile.setVertical();
		fVertexList = profile.getVertexList();

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fDialogSettings = Activator.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_adjust_srtm_colors_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				disposeColors();
			}
		});

		shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {

				// allow resizing the height but not the width

				final Point defaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				final Point shellSize = shell.getSize();

//				defaultSize.y = shellSize.y < defaultSize.y ? defaultSize.y : shellSize.y;
				defaultSize.y = shellSize.y;

				shell.setSize(defaultSize);
			}
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

				// ensure the field list is updated and not unsorted
				updateProfile();

				// create new vertex at the end of the list
				fVertexList.add(new RGBVertex(fColorChooser.getRGB()));

				createUIVertexFieds(fVertexOuterContainer);

				// set focus to the new vertex
				elevFields[elevFields.length - 1].setFocus();

				enableActions();

				/*
				 * !!! the fields are not sorted here because this leads to confusion when the field
				 * is moved to another position
				 */
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

				createUIVertexFieds(fVertexOuterContainer);
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
				updateProfile();
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

		fColorContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).indent(0, 10).applyTo(fColorContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(10, 0).applyTo(fColorContainer);
		{
			/*
			 * profile image
			 */
			fProfileImageCanvas = new ImageCanvas(fColorContainer, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().grab(false, true).hint(100, SWT.DEFAULT).applyTo(fProfileImageCanvas);
			fProfileImageCanvas.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					paintProfileImage();
				}
			});

			/*
			 * vertex fields
			 */
			fVertexOuterContainer = new Composite(fColorContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(fVertexOuterContainer);
			GridLayoutFactory.fillDefaults().applyTo(fVertexOuterContainer);

			createUIVertexFieds(fVertexOuterContainer);

			/*
			 * color chooser
			 */
			fColorChooser = new ColorChooser(new Composite(fColorContainer, SWT.NONE));
			fColorChooser.createUI();

		}
	}

	private void createUIVertexFieds(final Composite parent) {

		final int vertexSize = fVertexList.size();
		if (vertexSize == 0) {
			return;
		}

		final Display display = parent.getDisplay();

		// dispose previous content
		if (fVertexScrolledContainer != null) {
			fVertexScrolledContainer.dispose();
		}

		// scrolled container
		fVertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(fVertexScrolledContainer);
		fVertexScrolledContainer.setExpandVertical(true);
		fVertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(fVertexScrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(vertexContainer);

		fVertexScrolledContainer.setContent(vertexContainer);
		fVertexScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				fVertexScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		/*
		 * field listener
		 */
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

		final ModifyListener eleModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent event) {

				final Text widget = (Text) event.widget;
				final String vertexText = widget.getText();

				if (vertexText.trim().length() == 0) {
					widget.setText(UI.STRING_0);//$NON-NLS-1$
					return;
				}

				final RGBVertex vertex = (RGBVertex) widget.getData();

				try {
					vertex.elev = Long.parseLong(vertexText);
				} catch (final NumberFormatException e) {
					widget.setText(UI.STRING_0);//$NON-NLS-1$
				}
			}
		};

		final MouseAdapter colorMouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {

				final RGB rgb = fColorChooser.getRGB();
				final Color labelColor = new Color(display, rgb);
				fColorList.add(labelColor);

				final Label label = (Label) (e.widget);
				label.setBackground(labelColor);

				final RGBVertex vertex = (RGBVertex) label.getData();
				vertex.setRGB(rgb);

				updateProfile();
			}
		};

		final VerifyListener eleVerifyListener = UI.verifyListenerInteger(true);

		/*
		 * grid data
		 */
		final GridData gdEle = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gdEle.widthHint = 50;

		final GridData gdColor = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gdColor.widthHint = 70;
		gdColor.heightHint = 20;

		final GridData gdCheckbox = new GridData(SWT.CENTER, SWT.CENTER, false, false);

		/*
		 * fields
		 */
		colorLabel = new Label[vertexSize];
		elevFields = new Text[vertexSize];
		checkButtons = new Button[vertexSize];

		for (int vertexIndex = vertexSize - 1; vertexIndex >= 0; vertexIndex--) {

			final RGBVertex vertex = fVertexList.get(vertexIndex);
			final RGB vertexRGB = vertex.getRGB();

			/*
			 * text: elevation
			 */
			final Text txtElevation = elevFields[vertexIndex] = new Text(vertexContainer, SWT.SINGLE
					| SWT.TRAIL
					| SWT.BORDER);
			txtElevation.setLayoutData(gdEle);
			txtElevation.setText(UI.EMPTY_STRING + vertex.getElevation());
			txtElevation.addModifyListener(eleModifyListener);
			txtElevation.addVerifyListener(eleVerifyListener);
			txtElevation.setData(vertex);

			/*
			 * color label
			 */
			final Label lblColor = colorLabel[vertexIndex] = new Label(vertexContainer, SWT.CENTER);
			lblColor.setLayoutData(gdColor);
			lblColor.setToolTipText(UI.EMPTY_STRING + vertexRGB.red + "/" + vertexRGB.green + "/" + vertexRGB.blue); //$NON-NLS-1$ //$NON-NLS-2$
			final Color bgColor = new Color(display, vertexRGB);
			fColorList.add(bgColor);
			lblColor.setBackground(bgColor);
			lblColor.addMouseListener(colorMouseListener);
			lblColor.setData(vertex);

			/*
			 * checkbox
			 */
			final Button checkbox = checkButtons[vertexIndex] = new Button(vertexContainer, SWT.CHECK);
			checkbox.setLayoutData(gdCheckbox);
			checkbox.setToolTipText(Messages.dialog_adjust_srtm_colors_checkbutton_ttt);
			checkbox.addSelectionListener(checkboxListener);

			/*
			 * spacer
			 */
			final Label label = new Label(vertexContainer, SWT.NONE);
			GridDataFactory.fillDefaults().minSize(10, 1).applyTo(label);
		}

		/*
		 * disable checkboxes when only 2 colors are available
		 */
		if (vertexSize <= 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				checkButtons[ix].setEnabled(false);
			}
		}

		fVertexOuterContainer.layout(true);
	}

	private void disposeColors() {
		for (final Color color : fColorList) {
			color.dispose();
		}
	}

	private void enableActions() {

		final int vertexSize = fVertexList.size();

		int checked = 0;
		for (int ix = 0; ix < vertexSize; ix++) {
			final Button button = checkButtons[ix];
			if (button != null) {
				if (button.getSelection()) {
					checked++;
				}
			}
		}

		fBtnRemove.setEnabled(checked > 0 && vertexSize > 2);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

//	@Override
//	protected int getDialogBoundsStrategy() {
//		return DIALOG_PERSISTLOCATION;
//	}

	public SRTMProfile getSRTMProfile() {
		return fProfile;
	}

	@Override
	protected void okPressed() {

		updateProfile();

		fProfile.setProfileName(fTxtProfileName.getText());
		fProfile.setProfilePath(fTxtTilePath.getText());

		super.okPressed();
	}

	private void paintProfileImage() {
		final Rectangle imageBounds = fProfileImageCanvas.getBounds();
		fProfile.createImage(Display.getCurrent(), imageBounds.width, imageBounds.height);
		fProfileImageCanvas.paintImage(fProfile.getImage());
	}

	/**
	 * sort's the vertexes, updates fields and profile image
	 */
	private void updateProfile() {

		final int rgbVertexListSize = fVertexList.size();
		fVertexList.clear();

		for (int ix = 0; ix < rgbVertexListSize; ix++) {

			final String elevation = elevFields[ix].getText();
			if (elevation.equals(UI.EMPTY_STRING)) {
				continue; // remove empty fields
			}

			final Long elev = new Long(elevation);
			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setElev(elev.longValue());
			rgbVertex.setRGB(colorLabel[ix].getBackground().getRGB());

			fVertexList.add(rgbVertex);
		}
		Collections.sort(fVertexList);

		createUIVertexFieds(fVertexOuterContainer);
		paintProfileImage();
	}

	private void updateUI() {

		fTxtProfileName.setText(fProfile.getProfileName());
		fTxtTilePath.setText(fProfile.getProfilePath());
	}
}
