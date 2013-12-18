/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import java.util.LinkedHashSet;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.widgets.ColorChooser;
import net.tourbook.common.widgets.IProfileColors;
import net.tourbook.common.widgets.ImageCanvas;

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

public class DialogSelectSRTMColors extends TitleAreaDialog implements IProfileColors {

	private final IDialogSettings	_state;

	private ColorChooser			_colorChooser;
	private SRTMProfile				_dialogProfile;

	private ArrayList<SRTMProfile>	_srtmProfiles;

	private PrefPageSRTMColors		_prefPageSRTMColors;
	private SRTMProfile				_originalProfile;
	private boolean					_isNewProfile;

	private boolean					_isUIUpdated;

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
	private Composite				_vertexOuterContainer;
	private ScrolledComposite		_vertexScrolledContainer;

	private Text					_txtProfileName;
	private Text					_txtTilePath;

	private ImageCanvas				_canvasProfileImage;

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
	 * @param srtmProfiles
	 * @param prefPageSRTMColors
	 * @param isNewProfile
	 */
	public DialogSelectSRTMColors(	final Shell parentShell,
									final SRTMProfile originalProfile,
									final SRTMProfile dialogProfile,
									final ArrayList<SRTMProfile> srtmProfiles,
									final PrefPageSRTMColors prefPageSRTMColors,
									final boolean isNewProfile) {

		super(parentShell);

		_srtmProfiles = srtmProfiles;

		_originalProfile = originalProfile;
		_dialogProfile = dialogProfile;

		_prefPageSRTMColors = prefPageSRTMColors;
		_isNewProfile = isNewProfile;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		_state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
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

				// allow resizing the height, preserve minimum width

				final Point defaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				final Point shellSize = shell.getSize();

				if (shellSize.x < defaultSize.x) {

//					defaultSize.x = shellSize.x;
					defaultSize.y = shellSize.y;

					shell.setSize(defaultSize);
				}

			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_adjust_srtm_colors_dialog_title);
		setMessage(Messages.dialog_adjust_srtm_colors_dialog_message);

		restoreState();
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
				onVertexAdd();
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
				onVertexAddMultiple();
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
				onVertexRemove();
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

		final Composite configContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(configContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(configContainer);
		{
			createUI_10_Names(configContainer);
			createUI_20_Resolution(configContainer);
		}

		final Composite colorContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(colorContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(colorContainer);
//		colorContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_30_VertexImage(colorContainer);
			createUI_40_ColorChooser(colorContainer);
		}
	}

	private void createUI_10_Names(final Composite parent) {

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

	private void createUI_20_Resolution(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
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

	private void createUI_30_VertexImage(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).indent(0, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 0).applyTo(container);
		{
			/*
			 * profile image
			 */
			_canvasProfileImage = new ImageCanvas(container, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().grab(true, true).hint(100, SWT.DEFAULT).applyTo(_canvasProfileImage);
			_canvasProfileImage.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					paintProfileImage();
				}
			});

			/*
			 * vertex fields
			 */
			_vertexOuterContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(_vertexOuterContainer);
			GridLayoutFactory.fillDefaults().applyTo(_vertexOuterContainer);

			createUI_70_VertexFieds();
		}
	}

	/**
	 * color chooser
	 */
	private void createUI_40_ColorChooser(final Composite parent) {

		_colorChooser = new ColorChooser(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_colorChooser);

		_colorChooser.setProfileColors(this);
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_70_VertexFieds() {

		final ArrayList<RGBVertex> rgbVerticies = getVertexImage().getRgbVerticies();

		final int vertexSize = rgbVerticies.size();
		if (vertexSize == 0) {
			return;
		}

		final Composite parent = _vertexOuterContainer;
		final Display display = parent.getDisplay();

		Point scrollOrigin = null;

		// dispose previous content
		if (_vertexScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _vertexScrolledContainer.getOrigin();

			_vertexScrolledContainer.dispose();
		}

		final Composite vertexContainer = createUI_72_VertexScrolledContainer(parent);

		/*
		 * field listener
		 */
		final SelectionAdapter checkboxListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onFieldRemoveCheckbox(vertexSize);
			}
		};

		final SelectionListener eleSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onFieldSelectElevation(event.widget);
			}
		};

		final MouseAdapter colorMouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				onFieldMouseDown(display, e);
			}
		};

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

		_isUIUpdated = true;
		{
			for (int vertexIndex = vertexSize - 1; vertexIndex >= 0; vertexIndex--) {

				final RGBVertex vertex = rgbVerticies.get(vertexIndex);
				final RGB vertexRGB = vertex.getRGB();
				final Color bgColor = new Color(display, vertexRGB);
				_colors.add(bgColor);

				/*
				 * spinner: elevation
				 */
				final Spinner spinnerElevation = _spinnerElevation[vertexIndex] = new Spinner(
						vertexContainer,
						SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(spinnerElevation);
				spinnerElevation.setMinimum(Integer.MIN_VALUE);
				spinnerElevation.setMaximum(Integer.MAX_VALUE);
				spinnerElevation.addSelectionListener(eleSelectionListener);
				spinnerElevation.setData(vertex);
				spinnerElevation.setSelection((int) vertex.getValue());
				spinnerElevation.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onFieldSelectElevation(event.widget);
					}
				});

				/*
				 * Label: altitude color
				 */
				final Label lblColor = _lblColor[vertexIndex] = new Label(vertexContainer, SWT.CENTER
						| SWT.BORDER
						| SWT.SHADOW_NONE);
				lblColor.setLayoutData(gdColor);
				lblColor.setBackground(bgColor);
				lblColor.addMouseListener(colorMouseListener);
				lblColor.setData(vertex);
				lblColor.setToolTipText(NLS.bind(//
						Messages.Dialog_Adjust_SRTMColors_Color_Tooltip,
						new Object[] { vertexRGB.red, vertexRGB.green, vertexRGB.blue }));

				/*
				 * checkbox: delete
				 */
				final Button checkbox = _chkDelete[vertexIndex] = new Button(vertexContainer, SWT.CHECK);
				checkbox.setLayoutData(gdCheckbox);
				checkbox.setToolTipText(Messages.dialog_adjust_srtm_colors_checkbutton_ttt);
				checkbox.addSelectionListener(checkboxListener);
			}
		}
		_isUIUpdated = false;

		/*
		 * disable checkboxes when only 2 colors are available
		 */
		if (vertexSize <= 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				_chkDelete[ix].setEnabled(false);
			}
		}

		_vertexOuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_vertexScrolledContainer.setOrigin(scrollOrigin);
		}

	}

	private Composite createUI_72_VertexScrolledContainer(final Composite parent) {

		// scrolled container
		_vertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(_vertexScrolledContainer);
		_vertexScrolledContainer.setExpandVertical(true);
		_vertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_vertexScrolledContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(vertexContainer);

		_vertexScrolledContainer.setContent(vertexContainer);
		_vertexScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_vertexScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return vertexContainer;
	}

//	private void disposeProfileImage() {
//
//		if (_profileImage != null && _profileImage.isDisposed() == false) {
//			_profileImage.dispose();
//		}
//	}

	private void enableActions() {

		if (validateFields() == false) {
			return;
		}

		// shadow value
		final boolean isShadow = _chkShadow.getSelection();
		_txtShadowValue.setEnabled(isShadow);
		_lblShadowValue.setEnabled(isShadow);

		// remove buttons

		final ArrayList<RGBVertex> rgbVerticies = getVertexImage().getRgbVerticies();
		final int vertexSize = rgbVerticies.size();

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

	@Override
	public RGB[] getProfileColors() {

		/*
		 * create a set with all profile colors
		 */
		final LinkedHashSet<RGB> profileColors = new LinkedHashSet<RGB>();

		for (final RGBVertex rgbVertex : getVertexImage().getRgbVerticies()) {
			profileColors.add(rgbVertex.getRGB());
		}

		return profileColors.toArray(new RGB[profileColors.size()]);
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

	private ProfileImage getVertexImage() {
		return _dialogProfile.getRgbVertexImage();
	}

	@Override
	protected void okPressed() {

		updateProfileFromUI();

		super.okPressed();
	}

	private void onApply() {
		updateProfileFromUI();
		_prefPageSRTMColors.saveProfile(_originalProfile, _dialogProfile, _isNewProfile);
	}

	private void onDispose() {

		for (final Color color : _colors) {
			color.dispose();
		}

		UI.disposeResource(_profileImage);
	}

	private void onFieldMouseDown(final Display display, final MouseEvent e) {

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

	private void onFieldRemoveCheckbox(final int vertexSize) {

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

	private void onFieldSelectElevation(final Widget widget) {

		if (_isUIUpdated) {
			return;
		}

		final Spinner spinner = (Spinner) widget;
		final RGBVertex vertex = (RGBVertex) spinner.getData();

		vertex.setValue(spinner.getSelection());
	}

	private void onVertexAdd() {

		// ensure the field list is updated and not unsorted
		sortVertexsAndUpdateProfile();

		// create new vertex at the end of the list
		final ProfileImage vertexImage = getVertexImage();
		vertexImage.addVertex(new RGBVertex(_colorChooser.getRGB()));

		createUI_70_VertexFieds();

		// set focus to the new vertex
		_spinnerElevation[_spinnerElevation.length - 1].setFocus();

		enableActions();

		/*
		 * !!! the fields are not sorted here because this leads to confusion when the field is
		 * moved to another position
		 */
	}

	private void onVertexAddMultiple() {
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

				final ProfileImage rgbVertexImage = getVertexImage();

				/*
				 * check if elevation is already available, they will be ignored
				 */
				for (final RGBVertex vertex : rgbVertexImage.getRgbVerticies()) {
					if (vertex.getValue() == elevation) {
						isNewEle = false;
						break;
					}
				}

				if (isNewEle) {

					// create new vertex

					final RGBVertex vertex = new RGBVertex(_colorChooser.getRGB());
					vertex.setValue(elevation);

					rgbVertexImage.addVertex(vertex);
				}
			}

			createUI_70_VertexFieds();

			// set focus to the new vertex
//					elevFields[elevFields.length - 1].setFocus();

			enableActions();

			sortVertexsAndUpdateProfile();
		}
	}

	private void onVertexRemove() {

		final ProfileImage rgbVertexImage = getVertexImage();
		final ArrayList<Integer> vertexRemoveIndex = new ArrayList<Integer>();

		// get all checked checkedboxes
		for (int verticesIndex = 0; verticesIndex < _chkDelete.length; verticesIndex++) {
			if (_chkDelete[verticesIndex].getSelection()) {
				vertexRemoveIndex.add(verticesIndex);
			}
		}

		rgbVertexImage.removeVertices(vertexRemoveIndex);

		createUI_70_VertexFieds();
		paintProfileImage();
		enableActions();
	}

	private void paintProfileImage() {

		// force a redraw
		UI.disposeResource(_profileImage);

		final Rectangle imageBounds = _canvasProfileImage.getBounds();
		_profileImage = _dialogProfile.createImage(imageBounds.width, imageBounds.height, false);

		_canvasProfileImage.setImage(_profileImage);
	}

	private void restoreState() {

		_colorChooser.restoreState(_state);
	}

	private void saveState() {

		_colorChooser.saveState(_state);
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

//		final ArrayList<RGBVertex> rgbVerticies = _dialogProfile.getRgbVertexImage().getRgbVerticies();

		final int rgbVertexListSize = getVertexImage().getRgbVerticies().size();

		final ArrayList<RGBVertex> newRgbVerticies = new ArrayList<RGBVertex>();

		for (int vertexIndex = 0; vertexIndex < rgbVertexListSize; vertexIndex++) {

			/*
			 * create vertices from UI controls
			 */
			final int elevation = _spinnerElevation[vertexIndex].getSelection();
			final RGB rgb = _lblColor[vertexIndex].getBackground().getRGB();

			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setValue(elevation);
			rgbVertex.setRGB(rgb);

			newRgbVerticies.add(rgbVertex);
		}

		// sort vertices by elevation/value
		Collections.sort(newRgbVerticies);

		// update model
		getVertexImage().setVertices(newRgbVerticies);

		createUI_70_VertexFieds();

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

		for (final SRTMProfile profile : _srtmProfiles) {

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
