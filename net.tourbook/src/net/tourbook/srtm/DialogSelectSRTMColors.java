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
package net.tourbook.srtm;

import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.util.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogSelectSRTMColors extends TitleAreaDialog {

	private final IDialogSettings	fDialogSettings;

	private Text					fTxtProfileName;
	private Text					fTxtTilePath;

	private ImageCanvas				fProfileImageCanvas;
	private ColorChooser			fColorChooser;
	private Composite				fVertexOuterContainer;
	private ScrolledComposite		fVertexScrolledContainer;

	// vertex fields
	private Text[]					elevFields;
	private Label[]					colorLabel;
	private Button[]				checkButtons;

	private Button					fBtnApply;
	private Button					fBtnOK;
	private Button					fBtnRemove;

	private SRTMProfile				fDialogProfile;
	private ArrayList<RGBVertex>	fVertexList;
	private ArrayList<SRTMProfile>	fProfileList;

	/**
	 * keep colors which must be disposed when the dialog gets disposed
	 */
	private ArrayList<Color>		fColorList	= new ArrayList<Color>();

	private Button					fRdoResolutionVeryFine;
	private Button					fRdoResolutionFine;
	private Button					fRdoResolutionRough;
	private Button					fRdoResolutionVeryRough;

	private Button					fChkShadow;
	private Text					fTxtShadowValue;
	private Label					fLblShadowValue;

	private Shell					fShell;

	private PrefPageSRTMColors		fPrefPageSRTMColors;
	private boolean					fIsNewProfile;
	private SRTMProfile				fSelectedProfile;

	private Image					fProfileImage;

	/**
	 * @param parentShell
	 * @param originalProfile
	 * @param dialogProfile
	 * @param profileList
	 * @param prefPageSRTMColors
	 * @param isNewProfile
	 */
	public DialogSelectSRTMColors(	final Shell parentShell,
									final SRTMProfile originalProfile,
									final SRTMProfile dialogProfile,
									final ArrayList<SRTMProfile> profileList,
									final PrefPageSRTMColors prefPageSRTMColors,
									final boolean isNewProfile) {

		super(parentShell);

		fDialogProfile = dialogProfile;
		fVertexList = dialogProfile.getVertexList();
		fProfileList = profileList;

		fPrefPageSRTMColors = prefPageSRTMColors;
		fIsNewProfile = isNewProfile;
		fSelectedProfile = originalProfile;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		fShell = shell;

		shell.setText(Messages.dialog_adjust_srtm_colors_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {

				// allow resizing the height but not the width

				final Point defaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				final Point shellSize = shell.getSize();

				defaultSize.y = shellSize.y;

				shell.setSize(defaultSize);
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_adjust_srtm_colors_dialog_title);
		setMessage(Messages.dialog_adjust_srtm_colors_dialog_message);

		updateUI();

		paintProfileImage();

		enableActions();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		Button button;

		/*
		 * button: add vertex
		 */
		button = createButton(parent,
				IDialogConstants.CLIENT_ID + 1,
				Messages.dialog_adjust_srtm_colors_button_add,
				false);

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// ensure the field list is updated and not unsorted
				sortVertexsAndUpdateProfile();

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
		 * button: add multiple vertexes
		 */
		button = createButton(parent,
				IDialogConstants.CLIENT_ID + 2,
				Messages.dialog_adjust_srtm_colors_button_add_multiple,
				false);

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// ensure the field list is updated and not unsorted
				sortVertexsAndUpdateProfile();

				final DialogCreateMultipleVertexes dialog = new DialogCreateMultipleVertexes(Display.getCurrent()
						.getActiveShell());
				if (dialog.open() == Window.OK) {

					final int startEle = dialog.getStartElevation();
					final int endEle = dialog.getEndElevation();
					final int eleDiff = dialog.getElevationDifference();

					for (int elevation = startEle; elevation <= endEle; elevation += eleDiff) {

						boolean isNewEle = true;

						/*
						 * check if elevation is already available, they will be ignored
						 */
						for (final RGBVertex vertex : fVertexList) {
							if (vertex.elev == elevation) {
								isNewEle = false;
								break;
							}
						}

						if (isNewEle) {
							// create new vertex
							final RGBVertex vertex = new RGBVertex(fColorChooser.getRGB());
							fVertexList.add(vertex);
							vertex.elev = elevation;
						}
					}

					createUIVertexFieds(fVertexOuterContainer);

					// set focus to the new vertex
//					elevFields[elevFields.length - 1].setFocus();

					enableActions();
					
					sortVertexsAndUpdateProfile();
				}
			}
		});

		/*
		 * button: remove vertex
		 */
		fBtnRemove = createButton(parent,
				IDialogConstants.CLIENT_ID + 4,
				Messages.dialog_adjust_srtm_colors_button_remove,
				false);
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
		 * button: sort vertexes
		 */
		button = createButton(parent,
				IDialogConstants.CLIENT_ID + 5,
				Messages.dialog_adjust_srtm_colors_button_sort,
				false);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				sortVertexsAndUpdateProfile();
			}
		});

		/*
		 * button: apply
		 */
		fBtnApply = createButton(parent,
				IDialogConstants.CLIENT_ID + 6,
				Messages.dialog_adjust_srtm_colors_button_apply,
				false);
		fBtnApply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onApply();
			}
		});

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		fBtnOK = getButton(IDialogConstants.OK_ID);
		fBtnOK.setText(Messages.dialog_adjust_srtm_colors_button_update);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
		{
			createUINames(container);
			createUIResolution(container);
			createUIColorList(container);
			createUIColorChooser(container);
		}

	}

	/**
	 * color chooser
	 */
	private void createUIColorChooser(final Composite parent) {
		fColorChooser = new ColorChooser(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(fColorChooser);
	}

	private void createUIColorList(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).indent(0, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(container);
		{
			/*
			 * profile image
			 */
			fProfileImageCanvas = new ImageCanvas(container, SWT.NO_BACKGROUND);
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
			fVertexOuterContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(fVertexOuterContainer);
			GridLayoutFactory.fillDefaults().applyTo(fVertexOuterContainer);

			createUIVertexFieds(fVertexOuterContainer);
		}
	}

	private void createUINames(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			final Composite nameContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(nameContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(nameContainer);
			{
				/*
				 * lable: profile name
				 */
				Label label = new Label(nameContainer, SWT.NONE);
				label.setText(Messages.dialog_adjust_srtm_colors_label_profile_name);

				/*
				 * text: profile name
				 */
				fTxtProfileName = new Text(nameContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtProfileName);

				/*
				 * lable: tile path
				 */
				label = new Label(nameContainer, SWT.NONE);
				label.setText(Messages.dialog_adjust_srtm_colors_label_tile_path);

				/*
				 * text: tile path
				 */
				fTxtTilePath = new Text(nameContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtTilePath);
				fTxtTilePath.addVerifyListener(net.tourbook.util.UI.verifyFilenameInput());
				fTxtTilePath.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						validateFields();
					}
				});
			}

		}
	}

	private void createUIResolution(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(container);
		{
			// radio group: resolution
			final Group groupResolution = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(groupResolution);
			GridLayoutFactory.swtDefaults().numColumns(4).applyTo(groupResolution);
			groupResolution.setText(Messages.prefPage_srtm_resolution_title);
			{
				// radio: very fine
				fRdoResolutionVeryFine = new Button(groupResolution, SWT.RADIO);
				fRdoResolutionVeryFine.setText(Messages.prefPage_srtm_resolution_very_fine);

				// radio: fine
				fRdoResolutionFine = new Button(groupResolution, SWT.RADIO);
				fRdoResolutionFine.setText(Messages.prefPage_srtm_resolution_fine);

				// radio: rough
				fRdoResolutionRough = new Button(groupResolution, SWT.RADIO);
				fRdoResolutionRough.setText(Messages.prefPage_srtm_resolution_rough);

				// radio: very rough
				fRdoResolutionVeryRough = new Button(groupResolution, SWT.RADIO);
				fRdoResolutionVeryRough.setText(Messages.prefPage_srtm_resolution_very_rough);
			}

			/*
			 * checkbox: shadow
			 */
			fChkShadow = new Button(container, SWT.CHECK);
			fChkShadow.setText(Messages.prefPage_srtm_shadow_text);
			fChkShadow.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableActions();
				}
			});

			final Composite shadowContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(shadowContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(shadowContainer);
			{
				/*
				 * lable: shadow info
				 */
				fLblShadowValue = new Label(shadowContainer, SWT.NONE);
				GridDataFactory.swtDefaults().indent(20, 0).applyTo(fLblShadowValue);
				fLblShadowValue.setText(Messages.prefPage_srtm_shadow_value_text);

				/*
				 * input: shadow value
				 */
				fTxtShadowValue = new Text(shadowContainer, SWT.BORDER);
				fTxtShadowValue.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						validateFields();
					}
				});
			}
		}
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUIVertexFieds(final Composite parent) {

		final int vertexSize = fVertexList.size();
		if (vertexSize == 0) {
			return;
		}

		final Display display = parent.getDisplay();
		Point scrollOrigin = null;

		// dispose previous content
		if (fVertexScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = fVertexScrolledContainer.getOrigin();

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
					widget.setText(UI.STRING_0);
					return;
				}

				final RGBVertex vertex = (RGBVertex) widget.getData();

				try {
					vertex.elev = Long.parseLong(vertexText);
				} catch (final NumberFormatException e) {
					widget.setText(UI.STRING_0);
				}
			}
		};

		final MouseAdapter colorMouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {

				final Label label = (Label) (e.widget);
				final RGBVertex vertex = (RGBVertex) label.getData();

				if (e.button == 3) {

					// right button: update color chooser from vertex color

					fColorChooser.setRGB(vertex.getRGB());

				} else {

					// other buttons: update vertex color from color chooser

					final RGB rgb = fColorChooser.getRGB();
					final Color labelColor = new Color(display, rgb);
					fColorList.add(labelColor);

					label.setBackground(labelColor);

					vertex.setRGB(rgb);

					sortVertexsAndUpdateProfile();
				}
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

			final String toolTipRGB = UI.EMPTY_STRING + vertexRGB.red + "/" + vertexRGB.green + "/" + vertexRGB.blue; //$NON-NLS-1$ //$NON-NLS-2$
			final String toolTipText = NLS.bind(Messages.dialog_adjust_srtm_colors_color_tooltip, toolTipRGB);

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
			final Label lblColor = colorLabel[vertexIndex] = new Label(vertexContainer, SWT.CENTER
					| SWT.BORDER
					| SWT.SHADOW_NONE);
			lblColor.setLayoutData(gdColor);
			lblColor.setToolTipText(toolTipText);
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

		// set scroll position to previous position
		if (scrollOrigin != null) {
			fVertexScrolledContainer.setOrigin(scrollOrigin);
		}

	}

	private void disposeProfileImage() {
		if (fProfileImage != null && fProfileImage.isDisposed() == false) {
			fProfileImage.dispose();
		}
	}

	private void enableActions() {

		if (validateFields() == false) {
			return;
		}

		// shadow value
		final boolean isShadow = fChkShadow.getSelection();
		fTxtShadowValue.setEnabled(isShadow);
		fLblShadowValue.setEnabled(isShadow);

		// remove button
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

	@Override
	protected Point getInitialSize() {

		final Point initialSize = super.getInitialSize();
		final Point defaultSize = fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

		// enforce dialog is opened and all controls are visible
		if (initialSize.y < defaultSize.y) {
			initialSize.y = defaultSize.y;
		}

		return initialSize;
	}

	private String getResolutionFromUI() {

		if (fRdoResolutionVeryFine.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_VERY_FINE;
		} else if (fRdoResolutionFine.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_FINE;
		} else if (fRdoResolutionRough.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_ROUGH;
		} else if (fRdoResolutionVeryRough.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_VERY_ROUGH;
		} else {
			return IPreferences.SRTM_RESOLUTION_VERY_FINE;
		}
	}

	@Override
	protected void okPressed() {

		updateProfileFromUI();

		super.okPressed();
	}

	private void onApply() {
		updateProfileFromUI();
		fPrefPageSRTMColors.saveProfile(fSelectedProfile, fDialogProfile, fIsNewProfile);
	}

	private void onDispose() {
		for (final Color color : fColorList) {
			color.dispose();
		}

		disposeProfileImage();
	}

	private void paintProfileImage() {

		disposeProfileImage();

		final Rectangle imageBounds = fProfileImageCanvas.getBounds();
		fProfileImage = fDialogProfile.createImage(imageBounds.width, imageBounds.height, false);
		fProfileImageCanvas.setImage(fProfileImage);
	}

	private void setResolutionIntoUI(final String resolution) {

		final boolean isVeryFine = IPreferences.SRTM_RESOLUTION_VERY_FINE.equals(resolution);
		final boolean isFine = IPreferences.SRTM_RESOLUTION_FINE.equals(resolution);
		final boolean isRough = IPreferences.SRTM_RESOLUTION_ROUGH.equals(resolution);
		final boolean isVeryRough = IPreferences.SRTM_RESOLUTION_VERY_ROUGH.equals(resolution);

		fRdoResolutionVeryFine.setSelection(isVeryFine);
		fRdoResolutionFine.setSelection(isFine);
		fRdoResolutionRough.setSelection(isRough);
		fRdoResolutionVeryRough.setSelection(isVeryRough);

		// ensure one is selected
		if ((isVeryFine || isFine || isRough || isVeryRough) == false) {
			fRdoResolutionVeryFine.setSelection(true);
		}
	}

	/**
	 * sort's the vertexes, updates fields and profile image
	 */
	private void sortVertexsAndUpdateProfile() {

		final int rgbVertexListSize = fVertexList.size();
		fVertexList.clear();

		for (int ix = 0; ix < rgbVertexListSize; ix++) {

			final String elevation = elevFields[ix].getText();
			if (elevation.equals(UI.EMPTY_STRING)) {
				continue; // remove empty fields
			}

			final Long elev = new Long(elevation);
			final RGB rgb = colorLabel[ix].getBackground().getRGB();
			
			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setElev(elev.longValue());
			rgbVertex.setRGB(rgb);

			fVertexList.add(rgbVertex);
		}
		Collections.sort(fVertexList);

		createUIVertexFieds(fVertexOuterContainer);
		paintProfileImage();
	}

	private void updateProfileFromUI() {

		sortVertexsAndUpdateProfile();

		fDialogProfile.setProfileName(fTxtProfileName.getText());
		fDialogProfile.setTilePath(fTxtTilePath.getText());
		fDialogProfile.setShadowState(fChkShadow.getSelection());
		fDialogProfile.setResolution(getResolutionFromUI());
		fDialogProfile.setShadowValue(Float.parseFloat(fTxtShadowValue.getText()));
	}

	private void updateUI() {

		fTxtProfileName.setText(fDialogProfile.getProfileName());
		fTxtTilePath.setText(fDialogProfile.getTilePath());
		fChkShadow.setSelection(fDialogProfile.isShadowState());
		setResolutionIntoUI(fDialogProfile.getResolution());
		fTxtShadowValue.setText(Float.toString(fDialogProfile.getShadowValue()));
	}

	private boolean validateFields() {

		boolean isValid = true;

		/*
		 * check if the tile path is already used
		 */
		final int dialogProfileId = fDialogProfile.getProfileId();
		final String dialogTilePath = fTxtTilePath.getText().trim();

		for (final SRTMProfile profile : fProfileList) {

			// ignore the same profile
			if (profile.getProfileId() == dialogProfileId) {
				continue;
			}

			if (profile.getTilePath().trim().equalsIgnoreCase(dialogTilePath)) {
				/*
				 * another profile has the same profile path
				 */
				isValid = false;
				setErrorMessage(Messages.dialog_adjust_srtm_colors_error_invalid_tile_path);
				break;
			}
		}

		/*
		 * check shadow value
		 */
		try {
			final float shadowValue = Float.parseFloat(fTxtShadowValue.getText());
			if (shadowValue > 1 || shadowValue < 0) {
				isValid = false;
				setErrorMessage(Messages.dialog_adjust_srtm_colors_error_invalid_shadow_value);
			}
		} catch (final NumberFormatException e) {
			isValid = false;
			setErrorMessage(Messages.dialog_adjust_srtm_colors_error_invalid_shadow_value);
		}

		fBtnOK.setEnabled(isValid);
		fBtnApply.setEnabled(isValid);

		if (isValid) {
			setErrorMessage(null);
		}

		return isValid;

	}
}
