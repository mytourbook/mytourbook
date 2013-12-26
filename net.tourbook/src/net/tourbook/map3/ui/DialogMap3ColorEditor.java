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
package net.tourbook.map3.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorManager;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapColorProfile;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.widgets.ColorChooser;
import net.tourbook.common.widgets.IProfileColors;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.Messages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/**
 * This color editor is editing a clone of the provided {@link Map3ColorProfile}.
 * <p>
 * The provided {@link IMap3ColorUpdater} is called after the color is modified.
 */
public class DialogMap3ColorEditor extends TitleAreaDialog implements IProfileColors {

	/*
	 * Map2 massages are defined here that externalizing strings work properly, it works only with 1
	 * properties file.
	 */
	private static final String			MAP2_MESSAGE_1		= net.tourbook.map2.Messages.legendcolor_dialog_group_minmax_value;
	private static final String			MAP2_MESSAGE_2		= net.tourbook.map2.Messages.legendcolor_dialog_chk_max_value_text;
	private static final String			MAP2_MESSAGE_3		= net.tourbook.map2.Messages.legendcolor_dialog_chk_max_value_tooltip;
	private static final String			MAP2_MESSAGE_4		= net.tourbook.map2.Messages.legendcolor_dialog_txt_max_value;
	private static final String			MAP2_MESSAGE_5		= net.tourbook.map2.Messages.legendcolor_dialog_chk_min_value_text;
	private static final String			MAP2_MESSAGE_6		= net.tourbook.map2.Messages.legendcolor_dialog_chk_min_value_tooltip;
	private static final String			MAP2_MESSAGE_7		= net.tourbook.map2.Messages.legendcolor_dialog_txt_min_value;
	private static final String			MAP2_MESSAGE_8		= net.tourbook.map2.Messages.legendcolor_dialog_group_minmax_brightness;
	private static final String			MAP2_MESSAGE_9		= net.tourbook.map2.Messages.legendcolor_dialog_max_brightness_label;
	private static final String			MAP2_MESSAGE_10		= net.tourbook.map2.Messages.legendcolor_dialog_max_brightness_tooltip;
	private static final String			MAP2_MESSAGE_11		= net.tourbook.map2.Messages.legendcolor_dialog_min_brightness_label;
	private static final String			MAP2_MESSAGE_12		= net.tourbook.map2.Messages.legendcolor_dialog_min_brightness_tooltip;
	private static final String			MAP2_MESSAGE_13		= net.tourbook.map2.Messages.LegendColor_Dialog_Check_LiveUpdate;
	private static final String			MAP2_MESSAGE_14		= net.tourbook.map2.Messages.LegendColor_Dialog_Check_LiveUpdate_Tooltip;
	private static final String			MAP2_MESSAGE_15		= net.tourbook.map2.Messages.legendcolor_dialog_error_max_greater_min;

	private static final int			SPINNER_MIN_VALUE	= -200;
	private static final int			SPINNER_MAX_VALUE	= 10000;

	private final IDialogSettings		_state				= TourbookPlugin.getDefault().getDialogSettingsSection(
																	getClass().getName());

	private Map3ColorProfile			_originalProfile;
	private Map3ColorProfile			_dialogProfile;
	private boolean						_isNewProfile;

	private boolean						_isUIUpdate;
	private boolean						_isUIValid;

	/*
	 * UI resources
	 */
	private ColorChooser				_colorChooser;

	private Map3GradientColorProvider	_colorProvider;
	private IMap3ColorUpdater			_mapColorUpdater;

	private MouseWheelListener			_minMaxMouseWheelListener;
	private SelectionAdapter			_minMaxSelectionAdapter;

	/*
	 * UI controls
	 */
	private Shell						_shell;
	private Composite					_vertexOuterContainer;
	private ScrolledComposite			_vertexScrolledContainer;

	private ImageCanvas					_canvasProfileImage;
	private Image						_profileImage;

	private Button						_btnApply;
	private Button						_btnSave;
	private Button						_btnRemove;
	private Button						_chkForceMinValue;
	private Button						_chkForceMaxValue;
	private Button						_chkLiveUpdate;

	private Combo						_cboGraphType;
	private Combo						_cboMinBrightness;
	private Combo						_cboMaxBrightness;

	private Label						_lblMinValue;
	private Label						_lblMaxValue;

	private Spinner						_spinMinBrightness;
	private Spinner						_spinMaxBrightness;
	private Spinner						_spinMinValue;
	private Spinner						_spinMaxValue;

	private Text						_txtProfileName;

	// vertex fields
	private Spinner[]					_spinnerVertexValue;
	private Label[]						_lblVertexColor;
	private Button[]					_chkDeleteVertex;

	{
		_minMaxSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectMinMax();
			}
		};

		_minMaxMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelectMinMax();
			}
		};
	}

	/**
	 * @param parentShell
	 * @param originalProfile
	 * @param mapColorUpdater
	 *            This updater is called when OK or Apply are pressed or Live Update is done.
	 * @param isNewProfile
	 */
	public DialogMap3ColorEditor(	final Shell parentShell,
									final Map3ColorProfile originalProfile,
									final IMap3ColorUpdater mapColorUpdater,
									final boolean isNewProfile) {

		super(parentShell);

		_mapColorUpdater = mapColorUpdater;

		// create a profile working copy
		_dialogProfile = originalProfile.clone();
		_originalProfile = originalProfile;

		_colorProvider = new Map3GradientColorProvider(_dialogProfile);

		_isNewProfile = isNewProfile;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	/**
	 * Configure color provider with the vertex data.
	 * 
	 * @param imageHeight
	 */
	private void configureColorProvider(final int imageHeight) {

		final String unitText = UI.EMPTY_STRING;

		/*
		 * Get min/max values from the values which are displayed
		 */
		float minValue = 0;
		float maxValue = 0;

		final ArrayList<RGBVertex> rgbVertices = getRgbVertices();

		for (int vertexIndex = 0; vertexIndex < rgbVertices.size(); vertexIndex++) {

			final long value = rgbVertices.get(vertexIndex).getValue();

			if (vertexIndex == 0) {

				// initialize min/max values

				minValue = maxValue = value;

			} else {

				if (value < minValue) {
					minValue = value;
				} else if (value > maxValue) {
					maxValue = value;
				}
			}
		}

		_colorProvider.configureColorProvider(//
				imageHeight,
				minValue,
				maxValue,
				unitText,
				LegendUnitFormat.Number);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		_shell = shell;

		shell.setText(Messages.Map3Color_Dialog_Title);

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

				}

				final int width = defaultSize.x;
				final int height = shellSize.y;

				shell.setSize(width, height);
			}
		});
	}

	@Override
	public void create() {

		// create UI
		super.create();

		setTitle(Messages.Map3Color_Dialog_Title);
		setMessage(Messages.Map3Color_Dialog_Message);

		restoreState();

		updateUI_FromModel();
		enableGraphType();

		// set UI default behaviour
		_txtProfileName.setFocus();

		if (_isNewProfile) {

			// select whole profile name that it can be easily overwritten
			_txtProfileName.selectAll();
		}
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		createUI_99_ButtonBar(parent);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);

		updateUI_Initialize();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite uiContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(uiContainer);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(uiContainer);
//		uiContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			final Composite configContainer = new Composite(uiContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(configContainer);
			GridLayoutFactory.swtDefaults().numColumns(1).spacing(0, 15).applyTo(configContainer);
//			configContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_10_Names(configContainer);

				final Composite configInnerContainer = new Composite(configContainer, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(configInnerContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(configInnerContainer);
//				configInnerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				{
					createUI_30_ProfileImage(configInnerContainer);

					final Composite vertexContainer = new Composite(configInnerContainer, SWT.NONE);
					GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
					GridLayoutFactory.fillDefaults().numColumns(1).applyTo(vertexContainer);
//					vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					{
						createUI_40_VertexFields(vertexContainer);
						createUI_60_MinMaxValue(vertexContainer);
						createUI_62_Brightness(vertexContainer);
					}
				}
			}

			createUI_80_ColorChooser(uiContainer);
			createUI_90_LiveUpdate(uiContainer);
		}
	}

	private void createUI_10_Names(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Graph type
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Color_Dialog_Button_Label_GraphType);
				label.setToolTipText(Messages.Map3Color_Dialog_Button_Label_GraphType_Tooltip);

				_cboGraphType = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
//				_cboGraphType.addSelectionListener(_defaultSelectionAdapter);
			}

			{
				/*
				 * Profile name
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Color_Dialog_Button_Label_ProfileName);

				_txtProfileName = new Text(container, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtProfileName);
			}
		}
	}

	private void createUI_30_ProfileImage(final Composite parent) {

		/*
		 * profile image
		 */
		_canvasProfileImage = new ImageCanvas(parent, SWT.DOUBLE_BUFFERED);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.minSize(SWT.DEFAULT, 20)
//				.hint(SWT.DEFAULT, 20)
				.applyTo(_canvasProfileImage);

		_canvasProfileImage.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				drawProfileImage();
			}
		});
	}

	private void createUI_40_VertexFields(final Composite parent) {

		/*
		 * vertex fields container
		 */
		_vertexOuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_vertexOuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_vertexOuterContainer);
//		_vertexOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		/*
		 * Create fields that they are being initially displayed, otherwise the will be created but
		 * NOT visible
		 */
		createUI_50_VertexFields();
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_50_VertexFields() {

		final ArrayList<RGBVertex> rgbVerticies = getRgbVertices();

		final int vertexSize = rgbVerticies.size();

		if (vertexSize == 0) {
			// this case should not happen
			return;
		}

		// check if required vertex fields are already available
		if (_lblVertexColor != null && _lblVertexColor.length == vertexSize) {
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

		final Composite vertexContainer = createUI_52_VertexScrolledContainer(parent);
//		vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		/*
		 * Field listener
		 */
		final SelectionAdapter deleteListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onFieldRemoveCheckbox(vertexSize);
			}
		};

		final MouseAdapter colorMouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				onFieldMouseDown(display, e);
			}
		};

		// value listener
		final SelectionListener valueSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onFieldSelectValue(event.widget);
			}
		};
		final MouseWheelListener valueMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onFieldSelectValue(event.widget);
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
		_lblVertexColor = new Label[vertexSize];
		_spinnerVertexValue = new Spinner[vertexSize];
		_chkDeleteVertex = new Button[vertexSize];

		for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

			/*
			 * Spinner: Vertex value
			 */
			final Spinner spinnerValue = _spinnerVertexValue[vertexIndex] = new Spinner(vertexContainer, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(spinnerValue);
			spinnerValue.setMinimum(Integer.MIN_VALUE);
			spinnerValue.setMaximum(Integer.MAX_VALUE);
			spinnerValue.addSelectionListener(valueSelectionListener);
			spinnerValue.addMouseWheelListener(valueMouseWheelListener);

			/*
			 * Label: Value color
			 */
			final Label lblColor = _lblVertexColor[vertexIndex] = new Label(vertexContainer, SWT.CENTER
					| SWT.BORDER
					| SWT.SHADOW_NONE);
			lblColor.setLayoutData(gdColor);
			lblColor.addMouseListener(colorMouseListener);

			/*
			 * Checkbox: Delete vertex
			 */
			final Button checkbox = _chkDeleteVertex[vertexIndex] = new Button(vertexContainer, SWT.CHECK);
			checkbox.setLayoutData(gdCheckbox);
			checkbox.setToolTipText(Messages.Map3Color_Dialog_Checkbox_Delete_Tooltip);
			checkbox.addSelectionListener(deleteListener);
		}

		_vertexOuterContainer.layout(true);
//		_vertexOuterContainer.pack(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_vertexScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_52_VertexScrolledContainer(final Composite parent) {

		// scrolled container
		_vertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_vertexScrolledContainer);
		_vertexScrolledContainer.setExpandVertical(true);
		_vertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_vertexScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
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

	private void createUI_60_MinMaxValue(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, 10)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		group.setText(MAP2_MESSAGE_1);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			{
				/*
				 * Overwrite max value
				 */
				_chkForceMaxValue = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.applyTo(_chkForceMaxValue);
				_chkForceMaxValue.setText(MAP2_MESSAGE_2);
				_chkForceMaxValue.setToolTipText(MAP2_MESSAGE_3);
				_chkForceMaxValue.addSelectionListener(_minMaxSelectionAdapter);

				_lblMaxValue = new Label(group, SWT.NONE);
				_lblMaxValue.setText(MAP2_MESSAGE_4);
				GridDataFactory.fillDefaults()//
//						.indent(20, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblMaxValue);

				_spinMaxValue = new Spinner(group, SWT.BORDER);
				_spinMaxValue.setMinimum(SPINNER_MIN_VALUE);
				_spinMaxValue.setMaximum(SPINNER_MAX_VALUE);
				_spinMaxValue.addSelectionListener(_minMaxSelectionAdapter);
				_spinMaxValue.addMouseWheelListener(_minMaxMouseWheelListener);
			}
			{
				/*
				 * Overwrite min value
				 */
				_chkForceMinValue = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.applyTo(_chkForceMinValue);
				_chkForceMinValue.setText(MAP2_MESSAGE_5);
				_chkForceMinValue.setToolTipText(MAP2_MESSAGE_6);
				_chkForceMinValue.addSelectionListener(_minMaxSelectionAdapter);

				_lblMinValue = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
//						.indent(20, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblMinValue);
				_lblMinValue.setText(MAP2_MESSAGE_7);

				_spinMinValue = new Spinner(group, SWT.BORDER);
				_spinMinValue.setMinimum(SPINNER_MIN_VALUE);
				_spinMinValue.setMaximum(SPINNER_MAX_VALUE);
				_spinMinValue.addSelectionListener(_minMaxSelectionAdapter);
				_spinMinValue.addMouseWheelListener(_minMaxMouseWheelListener);
			}
		}
	}

	private void createUI_62_Brightness(final Composite parent) {

		Label label;

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.indent(0, 40)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		group.setText(MAP2_MESSAGE_8);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Max brightness
				 */
				label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(MAP2_MESSAGE_9);
				label.setToolTipText(MAP2_MESSAGE_10);

				_cboMaxBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMaxBrightness.addSelectionListener(_minMaxSelectionAdapter);

				_spinMaxBrightness = new Spinner(group, SWT.BORDER);
				_spinMaxBrightness.setMinimum(0);
				_spinMaxBrightness.setMaximum(100);
				_spinMaxBrightness.setPageIncrement(10);
				_spinMaxBrightness.addSelectionListener(_minMaxSelectionAdapter);
				_spinMaxBrightness.addMouseWheelListener(_minMaxMouseWheelListener);
			}
			{
				/*
				 * Min brightness
				 */
				label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(MAP2_MESSAGE_11);
				label.setToolTipText(MAP2_MESSAGE_12);

				_cboMinBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMinBrightness.addSelectionListener(_minMaxSelectionAdapter);

				_spinMinBrightness = new Spinner(group, SWT.BORDER);
				_spinMinBrightness.setMinimum(0);
				_spinMinBrightness.setMaximum(100);
				_spinMinBrightness.setPageIncrement(10);
				_spinMinBrightness.addSelectionListener(_minMaxSelectionAdapter);
				_spinMinBrightness.addMouseWheelListener(_minMaxMouseWheelListener);
			}
		}
	}

	/**
	 * Color chooser
	 */
	private void createUI_80_ColorChooser(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(10, 5, 5, 5)
				.numColumns(1)
				.applyTo(container);
		{
			_colorChooser = new ColorChooser(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(false, true)
					.applyTo(_colorChooser);

			_colorChooser.setProfileColors(this);
		}
	}

	private void createUI_90_LiveUpdate(final Composite parent) {

		/*
		 * button: live update
		 */
		_chkLiveUpdate = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.indent(5, 15)
				.applyTo(_chkLiveUpdate);
		_chkLiveUpdate.setText(MAP2_MESSAGE_13);
		_chkLiveUpdate.setToolTipText(MAP2_MESSAGE_14);
		_chkLiveUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
			}
		});
	}

	private void createUI_99_ButtonBar(final Composite parent) {

		{
			/*
			 * Autton: Add
			 */
			final Button btnAdd = createButton(
					parent,
					IDialogConstants.CLIENT_ID + 1,
					Messages.Map3Color_Dialog_Button_Add,
					false);

			btnAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onVertexAdd();
				}
			});
		}

		{
			/*
			 * Button: Remove
			 */
			_btnRemove = createButton(
					parent,
					IDialogConstants.CLIENT_ID + 4,
					Messages.Map3Color_Dialog_Button_Remove,
					false);
			_btnRemove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onVertexRemove();
				}
			});
		}

		{
			/*
			 * Button: Apply
			 */
			_btnApply = createButton(
					parent,
					IDialogConstants.CLIENT_ID + 6,
					Messages.Map3Color_Dialog_Button_Apply,
					false);
			_btnApply.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onApply();
				}
			});
		}

		// create default buttons (OK, Cancel)
		super.createButtonsForButtonBar(parent);

		{
			/*
			 * Button: Save
			 */
			// set text for the OK button
			_btnSave = getButton(IDialogConstants.OK_ID);
			_btnSave.setText(Messages.Map3Color_Dialog_Button_Save);
		}
	}

	private void doLiveUpdate() {

		if (_chkLiveUpdate.isEnabled() && _chkLiveUpdate.getSelection()) {
			onApply();
		}
	}

	private void drawProfileImage() {

		UI.disposeResource(_profileImage);

		final Rectangle imageBounds = _canvasProfileImage.getBounds();

		final int imageWidth = imageBounds.width;
		final int imageHeight = imageBounds.height;

		configureColorProvider(imageHeight);

		_profileImage = TourMapPainter.createMapLegendImage(
				Display.getCurrent(),
				_colorProvider,
				imageWidth,
				imageHeight);

		_canvasProfileImage.setImage(_profileImage);
	}

	private void enableControls() {

		final ArrayList<RGBVertex> rgbVertices = getRgbVertices();
		final int verticesSize = rgbVertices.size();

		final boolean isValid = verticesSize > 0 && validateFields();

		// min brightness
		final int minBrightness = _cboMinBrightness.getSelectionIndex();
		_spinMinBrightness.setEnabled(minBrightness != 0);

		// max brightness
		final int maxBrightness = _cboMaxBrightness.getSelectionIndex();
		_spinMaxBrightness.setEnabled(maxBrightness != 0);

		// min value
		boolean isChecked = _chkForceMinValue.getSelection();
		_lblMinValue.setEnabled(isChecked);
		_spinMinValue.setEnabled(isChecked);

		// max value
		isChecked = _chkForceMaxValue.getSelection();
		_lblMaxValue.setEnabled(isChecked);
		_spinMaxValue.setEnabled(isChecked);

		/*
		 * Enable remove button
		 */

		int checkedVertices = 0;

		for (int ix = 0; ix < verticesSize; ix++) {
			final Button button = _chkDeleteVertex[ix];
			if (button != null) {
				if (button.getSelection()) {
					checkedVertices++;
				}
			}
		}
		_btnRemove.setEnabled(checkedVertices > 0 && verticesSize > 2);

		/*
		 * Save/Apply buttons
		 */
		final boolean isLiveUpdate = _chkLiveUpdate.getSelection();
		final boolean canSave = isValid && isLiveUpdate == false;

		_btnApply.setEnabled(canSave);
		_btnSave.setEnabled(canSave);

		_chkLiveUpdate.setEnabled(isValid);
	}

	/**
	 * A graph type can only be selected, when there are more than one profile available for the
	 * current graph type.
	 */
	private void enableGraphType() {

		boolean canEnableGraphType = false;

		if (_isNewProfile) {

			canEnableGraphType = true;

		} else {

			final MapGraphId profileColorId = _dialogProfile.getGraphId();

			canEnableGraphType = Map3ColorManager.getColorProfiles(profileColorId).size() > 1;
		}

		_cboGraphType.setEnabled(canEnableGraphType);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
	}

	private int getGraphIdIndex(final MapGraphId colorId) {

		final ArrayList<Map3ColorDefinition> colorDefinitions = Map3ColorManager.getSortedColorDefinitions();

		for (int devIndex = 0; devIndex < colorDefinitions.size(); devIndex++) {

			final Map3ColorDefinition colorDefinition = colorDefinitions.get(devIndex);

			if (colorDefinition.getGraphId().equals(colorId)) {
				return devIndex;
			}
		}

		return 0;
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

		for (final RGBVertex rgbVertex : getRgbVertices()) {
			profileColors.add(rgbVertex.getRGB());
		}

		return profileColors.toArray(new RGB[profileColors.size()]);
	}

	private ProfileImage getProfileImage() {
		return _dialogProfile.getProfileImage();
	}

	private ArrayList<RGBVertex> getRgbVertices() {
		return getProfileImage().getRgbVertices();
	}

	private MapGraphId getSelectedGraphId() {

		final ArrayList<Map3ColorDefinition> colorDefinitions = Map3ColorManager.getSortedColorDefinitions();
		final int selectionIndex = _cboGraphType.getSelectionIndex();

		final Map3ColorDefinition selectedColorDef = colorDefinitions.get(selectionIndex);

		return selectedColorDef.getGraphId();
	}

	@Override
	protected void okPressed() {

		onApply();

		super.okPressed();
	}

	private void onApply() {

		updateModel_FromUI();

		_mapColorUpdater.applyMapColors(_originalProfile, _dialogProfile, _isNewProfile);
	}

	private void onDispose() {

		UI.disposeResource(_profileImage);
	}

	/**
	 * Set/push color.
	 * 
	 * @param display
	 * @param event
	 */
	private void onFieldMouseDown(final Display display, final MouseEvent event) {

		final Label vertexLabel = (Label) (event.widget);
		final RGBVertex vertex = (RGBVertex) vertexLabel.getData();

		if (event.button == 3) {

			// right button: update color chooser from vertex color

			_colorChooser.setRGB(vertex.getRGB());

		} else {

			// other buttons: update vertex color from color chooser

			final RGB rgb = _colorChooser.getRGB();
			updateUI_LabelColor(display, vertexLabel, rgb);

			vertex.setRGB(rgb);

			updateUI_FromModel_Vertices();
		}
	}

	private void onFieldRemoveCheckbox(final int vertexSize) {

		int checked = 0;
		for (int ix = 0; ix < vertexSize; ix++) {
			if (_chkDeleteVertex[ix].getSelection()) {
				checked++;
			}
		}

		if (checked == vertexSize - 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				if (!_chkDeleteVertex[ix].getSelection()) {
					_chkDeleteVertex[ix].setEnabled(false);
				}
			}
		} else {
			for (int ix = 0; ix < vertexSize; ix++) {
				_chkDeleteVertex[ix].setEnabled(true);
			}
		}

		enableControls();
	}

	private void onFieldSelectValue(final Widget widget) {

		if (_isUIUpdate) {
			return;
		}

		final Spinner spinner = (Spinner) widget;
		final RGBVertex vertex = (RGBVertex) spinner.getData();

		// update model
		vertex.setValue(spinner.getSelection());

		updateModel_FromUI_Vertices();

		// update UI
		updateUI_FromModel_Vertices();
		doLiveUpdate();
	}

	private void onSelectMinMax() {

		updateModel_FromUI();
		updateUI_FromModel();
	}

	private void onVertexAdd() {

		// ensure the field list is updated and not unsorted
//		sortVertexsAndUpdateProfile();

		// create new vertex at the beginning of the list
		final ProfileImage vertexImage = getProfileImage();
		vertexImage.addVertex(0, new RGBVertex(_colorChooser.getRGB()));

		updateUI_FromModel_Vertices();

		// set focus to the new vertex
		_spinnerVertexValue[_spinnerVertexValue.length - 1].setFocus();

		enableControls();

		/*
		 * !!! the fields are not sorted here because this leads to confusion when the field is
		 * moved to another position
		 */
	}

	private void onVertexRemove() {

		final ProfileImage rgbVertexImage = getProfileImage();
		final ArrayList<RGBVertex> removedVertices = new ArrayList<RGBVertex>();

		// get all checked checkboxes
		for (final Button checkBox : _chkDeleteVertex) {

			if (checkBox.getSelection()) {

				final RGBVertex rgbVertex = (RGBVertex) checkBox.getData();

				removedVertices.add(rgbVertex);
			}
		}

		// update model
		rgbVertexImage.removeVertices(removedVertices);

		updateUI_FromModel_Vertices();

		enableControls();
	}

	private void restoreState() {

		_colorChooser.restoreState(_state);
	}

	private void saveState() {

		_colorChooser.saveState(_state);
	}

	private void updateModel_FromUI() {

		_dialogProfile.setProfileName(_txtProfileName.getText());

		_dialogProfile.setGraphId(getSelectedGraphId());

		// update min/max brightness
		_dialogProfile.setMinBrightness(_cboMinBrightness.getSelectionIndex());
		_dialogProfile.setMaxBrightness(_cboMaxBrightness.getSelectionIndex());
		_dialogProfile.setMinBrightnessFactor(_spinMinBrightness.getSelection());
		_dialogProfile.setMaxBrightnessFactor(_spinMaxBrightness.getSelection());

		// update min/max value
		_dialogProfile.setIsMinValueOverwrite(_chkForceMinValue.getSelection());
		_dialogProfile.setIsMaxValueOverwrite(_chkForceMaxValue.getSelection());
		_dialogProfile.setMinValueOverwrite(_spinMinValue.getSelection());
		_dialogProfile.setMaxValueOverwrite(_spinMaxValue.getSelection());

		updateModel_FromUI_Vertices();

		doLiveUpdate();
	}

	/**
	 * Get vertices from UI and sorts them.
	 */
	private void updateModel_FromUI_Vertices() {

		final ArrayList<RGBVertex> rgbVertices = getRgbVertices();
		final int rgbVertexListSize = rgbVertices.size();
		final ArrayList<RGBVertex> newRgbVerticies = new ArrayList<RGBVertex>();

		for (int vertexIndex = 0; vertexIndex < rgbVertexListSize; vertexIndex++) {

			/*
			 * create vertices from UI controls
			 */
			final int value = _spinnerVertexValue[vertexIndex].getSelection();
			final RGB rgb = _lblVertexColor[vertexIndex].getBackground().getRGB();

			final RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setValue(value);
			rgbVertex.setRGB(rgb);

			newRgbVerticies.add(rgbVertex);
		}

		// sort vertices by value
		Collections.sort(newRgbVerticies);

		// update model
		getProfileImage().setVertices(newRgbVerticies);
	}

	private void updateUI_FromModel() {

		_txtProfileName.setText(_dialogProfile.getProfileName());

		final MapGraphId graphId = _dialogProfile.getGraphId();

		_cboGraphType.select(getGraphIdIndex(graphId));

		// update min/max brightness
		_cboMinBrightness.select(_dialogProfile.getMinBrightness());
		_cboMaxBrightness.select(_dialogProfile.getMaxBrightness());
		_spinMinBrightness.setSelection(_dialogProfile.getMinBrightnessFactor());
		_spinMaxBrightness.setSelection(_dialogProfile.getMaxBrightnessFactor());

		// update min/max value
		_chkForceMinValue.setSelection(_dialogProfile.isMinValueOverwrite());
		_chkForceMaxValue.setSelection(_dialogProfile.isMaxValueOverwrite());
		_spinMinValue.setSelection(_dialogProfile.getMinValueOverwrite());
		_spinMaxValue.setSelection(_dialogProfile.getMaxValueOverwrite());

		updateUI_FromModel_Vertices();

		enableControls();
	}

	private void updateUI_FromModel_Vertices() {

		// check vertex fields
		createUI_50_VertexFields();

		final ArrayList<RGBVertex> rgbVerticies = getRgbVertices();

		final int vertexSize = rgbVerticies.size();

		_isUIUpdate = true;
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				// show highest value at the top accoringly to the displayed legend
				final RGBVertex vertex = rgbVerticies.get(vertexSize - 1 - vertexIndex);

				// update value
				final Spinner spinnerValue = _spinnerVertexValue[vertexIndex];
				spinnerValue.setData(vertex);
				spinnerValue.setSelection(vertex.getValue());

				// update color
				final Label lblColor = _lblVertexColor[vertexIndex];
				final RGB vertexRGB = vertex.getRGB();

				lblColor.setToolTipText(NLS.bind(//
						Messages.Map3Color_Dialog_ProfileColor_Tooltip,
						new Object[] { vertexRGB.red, vertexRGB.green, vertexRGB.blue }));

				lblColor.setData(vertex);
				updateUI_LabelColor(lblColor.getDisplay(), lblColor, vertexRGB);

				// keep vertex reference
				_chkDeleteVertex[vertexIndex].setData(vertex);
			}
		}
		_isUIUpdate = false;

		/*
		 * disable checkboxes when only 2 colors are available
		 */
		if (vertexSize <= 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				_chkDeleteVertex[ix].setEnabled(false);
			}
		}

		// update profile image
		drawProfileImage();
	}

	/**
	 * Initialize UI.
	 */
	private void updateUI_Initialize() {

		final Collection<Map3ColorDefinition> colorDefinitions = Map3ColorManager.getSortedColorDefinitions();

		for (final Map3ColorDefinition colorDef : colorDefinitions) {
			_cboGraphType.add(colorDef.getVisibleName());
		}

		for (final String comboLabel : MapColorProfile.BRIGHTNESS_LABELS) {
			_cboMinBrightness.add(comboLabel);
		}

		for (final String comboLabel : MapColorProfile.BRIGHTNESS_LABELS) {
			_cboMaxBrightness.add(comboLabel);
		}
	}

	private void updateUI_LabelColor(final Display display, final Label label, final RGB vertexRGB) {

		final Color bgColor = new Color(display, vertexRGB);
		{
			label.setBackground(bgColor);
		}
		bgColor.dispose();
	}

	private boolean validateFields() {

		_isUIValid = true;

		final boolean isMinEnabled = _chkForceMinValue.getSelection();
		final boolean isMaxEnabled = _chkForceMaxValue.getSelection();

		// check that max is larger than min
		if (isMinEnabled && isMaxEnabled && (_spinMaxValue.getSelection() <= _spinMinValue.getSelection())) {

			setErrorMessage(MAP2_MESSAGE_15);
			_isUIValid = false;
		}

		if (_isUIValid) {
			setErrorMessage(null);
		}

		return _isUIValid;
	}
}
