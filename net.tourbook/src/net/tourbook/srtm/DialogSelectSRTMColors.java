/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.common.UI;
import net.tourbook.ui.ImageCanvas;

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
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

public class DialogSelectSRTMColors extends TitleAreaDialog {

	private final IDialogSettings	_state;

	private ColorChooser			_colorChooser;
	private SRTMProfile				_dialogProfile;

	private ArrayList<RGBVertex>	_vertexes;
	private ArrayList<SRTMProfile>	_profiles;

	private PrefPageSRTMColors		_prefPageSRTMColors;
	private SRTMProfile				_selectedProfile;
	private boolean					_isNewProfile;

	private boolean					_isUpdateUI;

	/*
	 * UI resources
	 */
	/**
	 * keep colors which must be disposed when the dialog gets disposed
	 */
	private ArrayList<Color>		_colors	= new ArrayList<Color>();

	/*
	 * UI controls
	 */
	private Text					_txtProfileName;
	private Text					_txtTilePath;

	private ImageCanvas				_canvasProfileImage;
	private Composite				_containerVertexOuter;
	private ScrolledComposite		_containerVertexScrolled;

	// vertex fields
	private Spinner[]				_spinnerElevation;
	private Label[]					_lblColor;
	private Button[]				_chkDelete;

	private Button					_btnApply;
	private Button					_btnOK;
	private Button					_btnRemove;

	private Button					_rdoResolutionVeryFine;
	private Button					_rdoResolutionFine;
	private Button					_rdoResolutionRough;
	private Button					_rdoResolutionVeryRough;

	private Button					_chkShadow;
	private Text					_txtShadowValue;
	private Label					_lblShadowValue;

	private Shell					_shell;

	private Image					_profileImage;

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

		_dialogProfile = dialogProfile;
		_vertexes = dialogProfile.getVertexList();
		_profiles = profileList;

		_prefPageSRTMColors = prefPageSRTMColors;
		_isNewProfile = isNewProfile;
		_selectedProfile = originalProfile;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		_state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		_shell = shell;

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
		button = createButton(
				parent,
				IDialogConstants.CLIENT_ID + 1,
				Messages.dialog_adjust_srtm_colors_button_add,
				false);

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// ensure the field list is updated and not unsorted
				sortVertexsAndUpdateProfile();

				// create new vertex at the end of the list
				_vertexes.add(new RGBVertex(_colorChooser.getRGB()));

				createUI32VertexFieds(_containerVertexOuter);

				// set focus to the new vertex
				_spinnerElevation[_spinnerElevation.length - 1].setFocus();

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
		button = createButton(
				parent,
				IDialogConstants.CLIENT_ID + 2,
				Messages.dialog_adjust_srtm_colors_button_add_multiple,
				false);

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// ensure the field list is updated and not unsorted
				sortVertexsAndUpdateProfile();

				final DialogCreateMultipleVertexes dialog = new DialogCreateMultipleVertexes(Display
						.getCurrent()
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
						for (final RGBVertex vertex : _vertexes) {
							if (vertex.elev == elevation) {
								isNewEle = false;
								break;
							}
						}

						if (isNewEle) {
							// create new vertex
							final RGBVertex vertex = new RGBVertex(_colorChooser.getRGB());
							_vertexes.add(vertex);
							vertex.elev = elevation;
						}
					}

					createUI32VertexFieds(_containerVertexOuter);

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
		_btnRemove = createButton(
				parent,
				IDialogConstants.CLIENT_ID + 4,
				Messages.dialog_adjust_srtm_colors_button_remove,
				false);
		_btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				final int rgbVertexListSize = _vertexes.size();

				for (int ix = rgbVertexListSize - 1; ix >= 0; ix--) {
					if (_chkDelete[ix].getSelection()) {
						_vertexes.remove(ix);
					}
				}

				createUI32VertexFieds(_containerVertexOuter);
				paintProfileImage();
				enableActions();
			}
		});

		/*
		 * button: sort vertexes
		 */
		button = createButton(
				parent,
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
		_btnApply = createButton(
				parent,
				IDialogConstants.CLIENT_ID + 6,
				Messages.dialog_adjust_srtm_colors_button_apply,
				false);
		_btnApply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onApply();
			}
		});

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		_btnOK = getButton(IDialogConstants.OK_ID);
		_btnOK.setText(Messages.dialog_adjust_srtm_colors_button_update);
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
			createUI10Names(container);
			createUI20Resolution(container);
			createUI30ColorList(container);
			createUI40ColorChooser(container);
		}
	}

	private void createUI10Names(final Composite parent) {

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
				_txtProfileName = new Text(nameContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtProfileName);

				/*
				 * lable: tile path
				 */
				label = new Label(nameContainer, SWT.NONE);
				label.setText(Messages.dialog_adjust_srtm_colors_label_tile_path);

				/*
				 * text: tile path
				 */
				_txtTilePath = new Text(nameContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtTilePath);
				_txtTilePath.addVerifyListener(net.tourbook.common.UI.verifyFilenameInput());
				_txtTilePath.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						validateFields();
					}
				});
			}

		}
	}

	private void createUI20Resolution(final Composite parent) {

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
				_rdoResolutionVeryFine = new Button(groupResolution, SWT.RADIO);
				_rdoResolutionVeryFine.setText(Messages.prefPage_srtm_resolution_very_fine);

				// radio: fine
				_rdoResolutionFine = new Button(groupResolution, SWT.RADIO);
				_rdoResolutionFine.setText(Messages.prefPage_srtm_resolution_fine);

				// radio: rough
				_rdoResolutionRough = new Button(groupResolution, SWT.RADIO);
				_rdoResolutionRough.setText(Messages.prefPage_srtm_resolution_rough);

				// radio: very rough
				_rdoResolutionVeryRough = new Button(groupResolution, SWT.RADIO);
				_rdoResolutionVeryRough.setText(Messages.prefPage_srtm_resolution_very_rough);
			}

			/*
			 * checkbox: shadow
			 */
			_chkShadow = new Button(container, SWT.CHECK);
			_chkShadow.setText(Messages.prefPage_srtm_shadow_text);
			_chkShadow.addSelectionListener(new SelectionAdapter() {
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
				_lblShadowValue = new Label(shadowContainer, SWT.NONE);
				GridDataFactory.swtDefaults().indent(20, 0).applyTo(_lblShadowValue);
				_lblShadowValue.setText(Messages.prefPage_srtm_shadow_value_text);

				/*
				 * input: shadow value
				 */
				_txtShadowValue = new Text(shadowContainer, SWT.BORDER);
				_txtShadowValue.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent e) {
						validateFields();
					}
				});
			}
		}
	}

	private void createUI30ColorList(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).indent(0, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(container);
		{
			/*
			 * profile image
			 */
			_canvasProfileImage = new ImageCanvas(container, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().grab(false, true).hint(100, SWT.DEFAULT).applyTo(_canvasProfileImage);
			_canvasProfileImage.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					paintProfileImage();
				}
			});

			/*
			 * vertex fields
			 */
			_containerVertexOuter = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(_containerVertexOuter);
			GridLayoutFactory.fillDefaults().applyTo(_containerVertexOuter);

			createUI32VertexFieds(_containerVertexOuter);
		}
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI32VertexFieds(final Composite parent) {

		final int vertexSize = _vertexes.size();
		if (vertexSize == 0) {
			return;
		}

		final Display display = parent.getDisplay();
		Point scrollOrigin = null;

		// dispose previous content
		if (_containerVertexScrolled != null) {

			// get current scroll position
			scrollOrigin = _containerVertexScrolled.getOrigin();

			_containerVertexScrolled.dispose();
		}

		// scrolled container
		_containerVertexScrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(_containerVertexScrolled);
		_containerVertexScrolled.setExpandVertical(true);
		_containerVertexScrolled.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_containerVertexScrolled, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(vertexContainer);

		_containerVertexScrolled.setContent(vertexContainer);
		_containerVertexScrolled.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_containerVertexScrolled.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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
					if (_chkDelete[ix].getSelection()) {
						checked++;
					}
				}

				if (checked == vertexSize - 2) {
					for (int ix = 0; ix < vertexSize; ix++) {
						if (!_chkDelete[ix].getSelection()) {
							_chkDelete[ix].setEnabled(false);
						}
					}
				} else {
					for (int ix = 0; ix < vertexSize; ix++) {
						_chkDelete[ix].setEnabled(true);
					}
				}

				enableActions();
			}
		};

		final SelectionListener eleSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onSelectElevation(event.widget);
			}
		};

		final MouseAdapter colorMouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {

				final Label label = (Label) (e.widget);
				final RGBVertex vertex = (RGBVertex) label.getData();

				if (e.button == 3) {

					// right button: update color chooser from vertex color

					_colorChooser.setRGB(vertex.getRGB());

				} else {

					// other buttons: update vertex color from color chooser

					final RGB rgb = _colorChooser.getRGB();
					final Color labelColor = new Color(display, rgb);
					_colors.add(labelColor);

					label.setBackground(labelColor);

					vertex.setRGB(rgb);

					sortVertexsAndUpdateProfile();
				}
			}
		};

		_isUpdateUI = true;

		/*
		 * grid data
		 */
		final GridData gdColor = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gdColor.widthHint = 70;
		gdColor.heightHint = 20;

		final GridData gdCheckbox = new GridData(SWT.CENTER, SWT.CENTER, false, false);

		/*
		 * fields
		 */
		_lblColor = new Label[vertexSize];
		_spinnerElevation = new Spinner[vertexSize];
		_chkDelete = new Button[vertexSize];

		for (int vertexIndex = vertexSize - 1; vertexIndex >= 0; vertexIndex--) {

			final RGBVertex vertex = _vertexes.get(vertexIndex);
			final RGB vertexRGB = vertex.getRGB();

			final String toolTipRGB = UI.EMPTY_STRING + vertexRGB.red + "/" + vertexRGB.green + "/" + vertexRGB.blue; //$NON-NLS-1$ //$NON-NLS-2$
			final String toolTipText = NLS.bind(Messages.dialog_adjust_srtm_colors_color_tooltip, toolTipRGB);

			/*
			 * spinner: elevation
			 */
			final Spinner spinnerElevation = _spinnerElevation[vertexIndex] = new Spinner(vertexContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(spinnerElevation);
			spinnerElevation.setMinimum(Integer.MIN_VALUE);
			spinnerElevation.setMaximum(Integer.MAX_VALUE);
			spinnerElevation.addSelectionListener(eleSelectionListener);
			spinnerElevation.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onSelectElevation(event.widget);
				}
			});
			spinnerElevation.setData(vertex);
			spinnerElevation.setSelection((int) vertex.elev);

			/*
			 * color label
			 */
			final Label lblColor = _lblColor[vertexIndex] = new Label(vertexContainer, SWT.CENTER
					| SWT.BORDER
					| SWT.SHADOW_NONE);
			lblColor.setLayoutData(gdColor);
			lblColor.setToolTipText(toolTipText);
			final Color bgColor = new Color(display, vertexRGB);
			_colors.add(bgColor);
			lblColor.setBackground(bgColor);
			lblColor.addMouseListener(colorMouseListener);
			lblColor.setData(vertex);

			/*
			 * checkbox: delete
			 */
			final Button checkbox = _chkDelete[vertexIndex] = new Button(vertexContainer, SWT.CHECK);
			checkbox.setLayoutData(gdCheckbox);
			checkbox.setToolTipText(Messages.dialog_adjust_srtm_colors_checkbutton_ttt);
			checkbox.addSelectionListener(checkboxListener);

			/*
			 * spacer
			 */
			final Label label = new Label(vertexContainer, SWT.NONE);
			GridDataFactory.fillDefaults().minSize(10, 1).applyTo(label);
		}
		_isUpdateUI = false;

		/*
		 * disable checkboxes when only 2 colors are available
		 */
		if (vertexSize <= 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				_chkDelete[ix].setEnabled(false);
			}
		}

		_containerVertexOuter.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_containerVertexScrolled.setOrigin(scrollOrigin);
		}

	}

	/**
	 * color chooser
	 */
	private void createUI40ColorChooser(final Composite parent) {
		_colorChooser = new ColorChooser(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_colorChooser);
	}

	private void disposeProfileImage() {
		if (_profileImage != null && _profileImage.isDisposed() == false) {
			_profileImage.dispose();
		}
	}

	private void enableActions() {

		if (validateFields() == false) {
			return;
		}

		// shadow value
		final boolean isShadow = _chkShadow.getSelection();
		_txtShadowValue.setEnabled(isShadow);
		_lblShadowValue.setEnabled(isShadow);

		// remove button
		final int vertexSize = _vertexes.size();
		int checked = 0;
		for (int ix = 0; ix < vertexSize; ix++) {
			final Button button = _chkDelete[ix];
			if (button != null) {
				if (button.getSelection()) {
					checked++;
				}
			}
		}
		_btnRemove.setEnabled(checked > 0 && vertexSize > 2);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
	}

	@Override
	protected Point getInitialSize() {

		final Point initialSize = super.getInitialSize();
		final Point defaultSize = _shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

		// enforce dialog is opened and all controls are visible
		if (initialSize.y < defaultSize.y) {
			initialSize.y = defaultSize.y;
		}

		return initialSize;
	}

	private String getResolutionFromUI() {

		if (_rdoResolutionVeryFine.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_VERY_FINE;
		} else if (_rdoResolutionFine.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_FINE;
		} else if (_rdoResolutionRough.getSelection()) {
			return IPreferences.SRTM_RESOLUTION_ROUGH;
		} else if (_rdoResolutionVeryRough.getSelection()) {
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
		_prefPageSRTMColors.saveProfile(_selectedProfile, _dialogProfile, _isNewProfile);
	}

	private void onDispose() {
		for (final Color color : _colors) {
			color.dispose();
		}

		disposeProfileImage();
	}

	private void onSelectElevation(final Widget widget) {

		if (_isUpdateUI) {
			return;
		}

		final Spinner spinner = (Spinner) widget;
		final RGBVertex vertex = (RGBVertex) spinner.getData();

		vertex.elev = spinner.getSelection();
	}

	private void paintProfileImage() {

		disposeProfileImage();

		final Rectangle imageBounds = _canvasProfileImage.getBounds();
		_profileImage = _dialogProfile.createImage(imageBounds.width, imageBounds.height, false);
		_canvasProfileImage.setImage(_profileImage);
	}

	private void setResolutionIntoUI(final String resolution) {

		final boolean isVeryFine = IPreferences.SRTM_RESOLUTION_VERY_FINE.equals(resolution);
		final boolean isFine = IPreferences.SRTM_RESOLUTION_FINE.equals(resolution);
		final boolean isRough = IPreferences.SRTM_RESOLUTION_ROUGH.equals(resolution);
		final boolean isVeryRough = IPreferences.SRTM_RESOLUTION_VERY_ROUGH.equals(resolution);

		_rdoResolutionVeryFine.setSelection(isVeryFine);
		_rdoResolutionFine.setSelection(isFine);
		_rdoResolutionRough.setSelection(isRough);
		_rdoResolutionVeryRough.setSelection(isVeryRough);

		// ensure one is selected
		if ((isVeryFine || isFine || isRough || isVeryRough) == false) {
			_rdoResolutionVeryFine.setSelection(true);
		}
	}

	/**
	 * sort's the vertexes, updates fields and profile image
	 */
	private void sortVertexsAndUpdateProfile() {

		final int rgbVertexListSize = _vertexes.size();
		_vertexes.clear();

		for (int ix = 0; ix < rgbVertexListSize; ix++) {

			final int elevation = _spinnerElevation[ix].getSelection();
			final RGB rgb = _lblColor[ix].getBackground().getRGB();

			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setElev(elevation);
			rgbVertex.setRGB(rgb);

			_vertexes.add(rgbVertex);
		}
		Collections.sort(_vertexes);

		createUI32VertexFieds(_containerVertexOuter);
		paintProfileImage();
	}

	private void updateProfileFromUI() {

		sortVertexsAndUpdateProfile();

		_dialogProfile.setProfileName(_txtProfileName.getText());
		_dialogProfile.setTilePath(_txtTilePath.getText());
		_dialogProfile.setShadowState(_chkShadow.getSelection());
		_dialogProfile.setResolution(getResolutionFromUI());
		_dialogProfile.setShadowValue(Float.parseFloat(_txtShadowValue.getText()));
	}

	private void updateUI() {

		_txtProfileName.setText(_dialogProfile.getProfileName());
		_txtTilePath.setText(_dialogProfile.getTilePath());
		_chkShadow.setSelection(_dialogProfile.isShadowState());
		setResolutionIntoUI(_dialogProfile.getResolution());
		_txtShadowValue.setText(Float.toString(_dialogProfile.getShadowValue()));
	}

	private boolean validateFields() {

		boolean isValid = true;

		/*
		 * check if the tile path is already used
		 */
		final int dialogProfileId = _dialogProfile.getProfileId();
		final String dialogTilePath = _txtTilePath.getText().trim();

		for (final SRTMProfile profile : _profiles) {

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
			final float shadowValue = Float.parseFloat(_txtShadowValue.getText());
			if (shadowValue > 1 || shadowValue < 0) {
				isValid = false;
				setErrorMessage(Messages.dialog_adjust_srtm_colors_error_invalid_shadow_value);
			}
		} catch (final NumberFormatException e) {
			isValid = false;
			setErrorMessage(Messages.dialog_adjust_srtm_colors_error_invalid_shadow_value);
		}

		_btnOK.setEnabled(isValid);
		_btnApply.setEnabled(isValid);

		if (isValid) {
			setErrorMessage(null);
		}

		return isValid;

	}
}
