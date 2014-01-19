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
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapColorProfile;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ColorChooser;
import net.tourbook.common.widgets.IProfileColors;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.Messages;
import net.tourbook.map3.action.ActionAddVertex;
import net.tourbook.map3.action.ActionAddVertices;
import net.tourbook.map3.action.ActionDeleteVertex;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;

/**
 * This color editor is editing a clone of the provided {@link Map3ColorProfile}.
 * <p>
 * The provided {@link IMap3ColorUpdater} is called after the color is modified.
 */
public class DialogMap3ColorEditor extends TitleAreaDialog implements IProfileColors {

	/*
	 * Map2 massages are defined here that externalizing strings can be done easily by disabling
	 * temporarily the messages.
	 */
	private static final String			LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE			= net.tourbook.map2.Messages.LegendColor_Dialog_Check_LiveUpdate;
	private static final String			LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE_TOOLTIP	= net.tourbook.map2.Messages.LegendColor_Dialog_Check_LiveUpdate_Tooltip;
	private static final String			LEGENDCOLOR_DIALOG_MIN_BRIGHTNESS_LABEL			= net.tourbook.map2.Messages.legendcolor_dialog_min_brightness_label;
	private static final String			LEGENDCOLOR_DIALOG_MIN_BRIGHTNESS_TOOLTIP		= net.tourbook.map2.Messages.legendcolor_dialog_min_brightness_tooltip;
	private static final String			LEGENDCOLOR_DIALOG_MAX_BRIGHTNESS_LABEL			= net.tourbook.map2.Messages.legendcolor_dialog_max_brightness_label;
	private static final String			LEGENDCOLOR_DIALOG_MAX_BRIGHTNESS_TOOLTIP		= net.tourbook.map2.Messages.legendcolor_dialog_max_brightness_tooltip;

	private static final String			STATE_IS_LIVE_UPDATE							= "STATE_IS_LIVE_UPDATE";													//$NON-NLS-1$
	private static final String			STATE_IS_PRONTO_COLOR							= "STATE_IS_PRONTO_COLOR";													//$NON-NLS-1$

	private static final String			DATA_KEY_VERTEX_INDEX							= "DATA_KEY_VERTEX_INDEX";													//$NON-NLS-1$
	private static final String			DATA_KEY_SORT_ID								= "DATA_KEY_SORT_ID";														//$NON-NLS-1$

	private final IDialogSettings		_state											= TourbookPlugin
																								.getState(getClass()
																										.getName());

	/**
	 * Contains a clone from {@link #_originalColorProvider}.
	 */
	private Map3GradientColorProvider	_dialogColorProvider;
	private Map3GradientColorProvider	_originalColorProvider;

	private boolean						_isNewColorProvider;
	private boolean						_isInProntoUpdate;
	private boolean						_isInUIUpdate;
	private boolean						_isProntoColorEnabled;

	private Integer						_prontoColorVertexIndex;

	private IMap3ColorUpdater			_mapColorUpdater;

	private MouseWheelListener			_defaultMouseWheelListener;
	private SelectionAdapter			_defaultSelectionAdapter;

	/*
	 * UI resources
	 */
	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private Shell						_shell;
	private Composite					_vertexOuterContainer;
	private ScrolledComposite			_vertexScrolledContainer;

	private ColorChooser				_colorChooser;
	private ImageCanvas					_canvasProfileImage;
	private Image						_profileImage;

	private Button						_btnApply;
	private Button						_btnSave;
	private Button						_chkLiveUpdate;
	private Button						_chkOverwriteLegendValues;

	private Combo						_cboGraphType;
	private Combo						_cboMinBrightness;
	private Combo						_cboMaxBrightness;

	private Label						_lblMaxBrightness;
	private Label						_lblMinBrightness;

	private Button						_rdoAbsoluteValues;
	private Button						_rdoRelativeValues;

	private Spinner						_spinMinBrightness;
	private Spinner						_spinMaxBrightness;

	private Text						_txtProfileName;

	// vertex fields
	private ActionAddVertex[]			_actionAddVertex;
	private ActionDeleteVertex[]		_actionDeleteVertex;
	private Button						_chkProntoColor;
	private Label[]						_lblVertexColor;
	private Button[]					_rdoProntoColors;
	private Spinner[]					_spinnerOpacity;
	private Spinner[]					_spinnerVertexValue;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectControl();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelectControl();
			}
		};
	}

	/**
	 * @param parentShell
	 * @param originalColorProvider
	 * @param mapColorUpdater
	 *            This updater is called when OK or Apply is pressed or when a Live Update is done.
	 * @param isNewProfile
	 */
	public DialogMap3ColorEditor(	final Shell parentShell,
									final Map3GradientColorProvider originalColorProvider,
									final IMap3ColorUpdater mapColorUpdater,
									final boolean isNewProfile) {

		super(parentShell);

		_mapColorUpdater = mapColorUpdater;

		// create a profile working copy
		_dialogColorProvider = originalColorProvider.clone();
		_originalColorProvider = originalColorProvider;

		_isNewColorProvider = isNewProfile;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	public void actionAddVertex(final int vertexIndex) {

		// update model - duplicate current vertex
		final RGBVertex currentVertex = getRgbVertices().get(vertexIndex);
		getProfileImage().addVertex(0, currentVertex.clone());

		// update UI
		updateUI_FromModel_Vertices();

		onApply(false);
	}

	public void actionAddVertices() {

		final DialogCreateMultipleVertices dialog = new DialogCreateMultipleVertices(_shell);
		if (dialog.open() == Window.OK) {

			// update model
			getProfileImage().addVertices(
					dialog.getStartValue(),
					dialog.getEndValue(),
					dialog.getValueDifference(),
					_colorChooser.getRGB());

			updateUI_FromModel_Vertices();

			onApply(false);
		}
	}

	public void actionRemoveVertex(final int vertexIndex) {

		// update model
		final RGBVertex removedVertex = getRgbVertices().get(vertexIndex);

		getProfileImage().removeVertex(removedVertex);

		// update UI
		updateUI_FromModel_Vertices();

		onApply(false);
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

		if (_isNewColorProvider) {

			// select whole profile name that it can be easily overwritten
			_txtProfileName.selectAll();
		}
	}

	/**
	 * Creates an action in a toolbar.
	 * 
	 * @param parent
	 * @param action
	 * @return
	 */
	private ToolBarManager createActionButton(final Composite parent, final Action action) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(action);
		tbm.update(true);

		return tbm;
	}

	@Override
	protected Control createButtonBar(final Composite parent) {

		return createUI_98_ButtonBar(parent);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		createUI_99_ButtonsForButtonBar(parent);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_pc = new PixelConverter(parent);

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
						createUI_60_AdjustValues(vertexContainer);
					}
				}
			}

			createUI_80_ColorChooser(uiContainer);
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
				_cboGraphType.addSelectionListener(_defaultSelectionAdapter);
			}

			{
				/*
				 * Profile name
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Color_Dialog_Button_Label_ProfileName);

				_txtProfileName = new Text(container, SWT.BORDER);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtProfileName);

				_txtProfileName.addTraverseListener(new TraverseListener() {

					@Override
					public void keyTraversed(final TraverseEvent e) {

						// do live update
						onModifyProfileName(e);
					}
				});
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
				.hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
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

		/*
		 * Field listener
		 */
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

		final SelectionListener prontoListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onFieldSelectPronto(event.widget);
			}
		};

		/*
		 * fields
		 */
		_actionAddVertex = new ActionAddVertex[vertexSize];
		_actionDeleteVertex = new ActionDeleteVertex[vertexSize];
		_lblVertexColor = new Label[vertexSize];
		_rdoProntoColors = new Button[vertexSize];
		_spinnerOpacity = new Spinner[vertexSize];
		_spinnerVertexValue = new Spinner[vertexSize];

		vertexContainer.setRedraw(false);
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				/*
				 * Action: Add vertex
				 */
				final ActionAddVertex actionAddVertex = new ActionAddVertex(this);
				createActionButton(vertexContainer, actionAddVertex);

				/*
				 * Spinner: Vertex value
				 */
				final Spinner spinnerValue = new Spinner(vertexContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(-10000);
				spinnerValue.setMaximum(10000);
				spinnerValue.addSelectionListener(valueSelectionListener);
				spinnerValue.addMouseWheelListener(valueMouseWheelListener);
				spinnerValue.setToolTipText(Messages.Map3Color_Dialog_Spinner_ColorValue_Tooltip);

				/*
				 * Action: Delete vertex
				 */
				final ActionDeleteVertex actionDeleteVertex = new ActionDeleteVertex(this);
				createActionButton(vertexContainer, actionDeleteVertex);

				/*
				 * Label: Value color
				 */
				final Label lblColor = new Label(vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.hint(70, 10)
						.applyTo(lblColor);
				lblColor.addMouseListener(colorMouseListener);

				/*
				 * Spinner: Opacity
				 */
				final Spinner spinnerOpacity = new Spinner(vertexContainer, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(spinnerOpacity);

				spinnerOpacity.setMinimum(Map3GradientColorManager.OPACITY_MIN);
				spinnerOpacity.setMaximum(Map3GradientColorManager.OPACITY_MAX);
				spinnerOpacity.setDigits(Map3GradientColorManager.OPACITY_DIGITS);
				spinnerOpacity.setIncrement(1);
				spinnerOpacity.setPageIncrement(10);
				spinnerOpacity.addSelectionListener(_defaultSelectionAdapter);
				spinnerOpacity.addMouseWheelListener(_defaultMouseWheelListener);
				spinnerOpacity.setToolTipText(Messages.Map3Color_Dialog_Spinner_ColorOpacity_Tooltip);

				/*
				 * Radio: Pronto Color
				 */
				final Button prontoColor = new Button(vertexContainer, SWT.RADIO);
				prontoColor.setToolTipText(Messages.Map3Color_Dialog_Radio_ProntoColor);
				prontoColor.addSelectionListener(prontoListener);

				/*
				 * Keep vertex controls
				 */
				_actionAddVertex[vertexIndex] = actionAddVertex;
				_actionDeleteVertex[vertexIndex] = actionDeleteVertex;
				_spinnerOpacity[vertexIndex] = spinnerOpacity;
				_spinnerVertexValue[vertexIndex] = spinnerValue;
				_lblVertexColor[vertexIndex] = lblColor;
				_rdoProntoColors[vertexIndex] = prontoColor;
			}

			createUI_54_VertexActionBar(vertexContainer);
		}
		vertexContainer.setRedraw(true);

		_vertexOuterContainer.layout(true);

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
		GridLayoutFactory.fillDefaults()//
				.numColumns(6)
				.applyTo(vertexContainer);
//		vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

		_vertexScrolledContainer.setContent(vertexContainer);
		_vertexScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_vertexScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return vertexContainer;
	}

	private void createUI_54_VertexActionBar(final Composite parent) {

		/*
		 * Action bar
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(6, 1)
//				.align(SWT.END, SWT.FILL)
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
				final ToolBarManager tbm = new ToolBarManager(toolbar);

//				tbm.add(new ActionAddVertex(this));
				tbm.add(new ActionAddVertices(this));

				tbm.update(true);
			}

			{
				_chkProntoColor = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.FILL)
						.applyTo(_chkProntoColor);
				_chkProntoColor.setText(Messages.Map3Color_Dialog_Checkbox_EnableProntoColor);
				_chkProntoColor.setToolTipText(Messages.Map3Color_Dialog_Checkbox_EnableProntoColor_Tooltip);
				_chkProntoColor.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						enableControls_Pronto();
					}
				});

				// invalidate pronto color, initially a pronto color is not checked
				_prontoColorVertexIndex = null;
			}
		}
	}

	private void createUI_60_AdjustValues(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI_62_AbsoluteRelativeValue(container);
			createUI_64_Brightness(container);
			createUI_66_OverwriteLegendValues(container);
		}
	}

	private void createUI_62_AbsoluteRelativeValue(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Map3Color_Dialog_Label_Values);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * Radio: Relative
			 */
			_rdoRelativeValues = new Button(container, SWT.RADIO);
			_rdoRelativeValues.setText(Messages.Map3Color_Dialog_Radio_RelativeValues);
			_rdoRelativeValues.setToolTipText(Messages.Map3Color_Dialog_Radio_RelativeValues_Tooltip);
			_rdoRelativeValues.addSelectionListener(_defaultSelectionAdapter);
		}

		{
			/*
			 * Radio: Absolute
			 */
			_rdoAbsoluteValues = new Button(container, SWT.RADIO);
			_rdoAbsoluteValues.setText(Messages.Map3Color_Dialog_Radio_AbsoluteValues);
			_rdoAbsoluteValues.setToolTipText(Messages.Map3Color_Dialog_Radio_AbsoluteValues_Tooltip);
			_rdoAbsoluteValues.addSelectionListener(_defaultSelectionAdapter);
		}
	}

	private void createUI_64_Brightness(final Composite parent) {

		{
			/*
			 * Max brightness
			 */
			_lblMaxBrightness = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblMaxBrightness);
			_lblMaxBrightness.setText(LEGENDCOLOR_DIALOG_MAX_BRIGHTNESS_LABEL);
			_lblMaxBrightness.setToolTipText(LEGENDCOLOR_DIALOG_MAX_BRIGHTNESS_TOOLTIP);

			final Composite containerMax = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerMax);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerMax);
			{
				_cboMaxBrightness = new Combo(containerMax, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMaxBrightness.addSelectionListener(_defaultSelectionAdapter);

				_spinMaxBrightness = new Spinner(containerMax, SWT.BORDER);
				_spinMaxBrightness.setMinimum(0);
				_spinMaxBrightness.setMaximum(100);
				_spinMaxBrightness.setPageIncrement(10);
				_spinMaxBrightness.addSelectionListener(_defaultSelectionAdapter);
				_spinMaxBrightness.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}

		{
			/*
			 * Min brightness
			 */
			_lblMinBrightness = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblMinBrightness);
			_lblMinBrightness.setText(LEGENDCOLOR_DIALOG_MIN_BRIGHTNESS_LABEL);
			_lblMinBrightness.setToolTipText(LEGENDCOLOR_DIALOG_MIN_BRIGHTNESS_TOOLTIP);

			final Composite containerMin = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerMin);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerMin);
			{

				_cboMinBrightness = new Combo(containerMin, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMinBrightness.addSelectionListener(_defaultSelectionAdapter);

				_spinMinBrightness = new Spinner(containerMin, SWT.BORDER);
				_spinMinBrightness.setMinimum(0);
				_spinMinBrightness.setMaximum(100);
				_spinMinBrightness.setPageIncrement(10);
				_spinMinBrightness.addSelectionListener(_defaultSelectionAdapter);
				_spinMinBrightness.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_66_OverwriteLegendValues(final Composite parent) {

		_chkOverwriteLegendValues = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkOverwriteLegendValues);
		_chkOverwriteLegendValues.setText(Messages.Map3Color_Dialog_Checkbox_OverwriteLegendValues);
		_chkOverwriteLegendValues.setToolTipText(Messages.Map3Color_Dialog_Checkbox_OverwriteLegendValues_Tooltip);
		_chkOverwriteLegendValues.addSelectionListener(_defaultSelectionAdapter);

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

	private Control createUI_98_ButtonBar(final Composite parent) {

		Control containerButtonBar;

		/*
		 * Live update checkbox is created here that it can be left aligned in a separate container.
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Checkbox: live update
				 */
				_chkLiveUpdate = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.indent(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), 0)
						.applyTo(_chkLiveUpdate);
				_chkLiveUpdate.setText(LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE);
				_chkLiveUpdate.setToolTipText(LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE_TOOLTIP);
				_chkLiveUpdate.addSelectionListener(_defaultSelectionAdapter);
			}

			containerButtonBar = super.createButtonBar(container);
		}

		return containerButtonBar;
	}

	private void createUI_99_ButtonsForButtonBar(final Composite parent) {

//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

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
					onApply(true);
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

	private void drawProfileImage() {

		UI.disposeResource(_profileImage);

		final Rectangle imageBounds = _canvasProfileImage.getBounds();

		final int imageWidth = imageBounds.width;
		final int imageHeight = imageBounds.height;
		final boolean isDrawUnits = _dialogColorProvider.getMap3ColorProfile().isAbsoluteValues();

		_dialogColorProvider.configureColorProvider(//
				imageHeight,
				getRgbVertices(),
				isDrawUnits);

		_profileImage = TourMapPainter.createMapLegendImage(//
				_dialogColorProvider,
				imageWidth,
				imageHeight,
				true,
				true,
				true);

		_canvasProfileImage.setImage(_profileImage);
	}

	private void enableControls() {

		final ArrayList<RGBVertex> rgbVertices = getRgbVertices();
		final int verticesSize = rgbVertices.size();

		final boolean isValidVertices = verticesSize > 0;
		final boolean isAbsolute = _rdoAbsoluteValues.getSelection();
		final int minBrightnessValue = _cboMinBrightness.getSelectionIndex();
		final int maxBrightnessValue = _cboMaxBrightness.getSelectionIndex();

		_spinMinBrightness.setEnabled(minBrightnessValue != 0);
		_spinMaxBrightness.setEnabled(maxBrightnessValue != 0);

		_chkOverwriteLegendValues.setEnabled(isAbsolute);

		// Vertex delete actions
		final boolean canRemoveVertices = verticesSize > 2;
		for (final ActionDeleteVertex actionDeletevertex : _actionDeleteVertex) {
			actionDeletevertex.setEnabled(canRemoveVertices);
		}

		/*
		 * Save/Apply buttons
		 */
		final boolean isLiveUpdate = _chkLiveUpdate.getSelection();
		final boolean canSave = isValidVertices && isLiveUpdate == false;

		_btnApply.setEnabled(canSave);
		_btnSave.setEnabled(canSave);

		_chkLiveUpdate.setEnabled(isValidVertices);

		enableControls_Pronto();
	}

	private void enableControls_Pronto() {

		_isProntoColorEnabled = _chkProntoColor.getSelection();

		for (final Button prontoColor : _rdoProntoColors) {
			prontoColor.setEnabled(_isProntoColorEnabled);
		}
	}

	/**
	 * A graph type can only be selected, when more than one color providers are available for the
	 * current graph type.
	 */
	private void enableGraphType() {

		boolean canEnableGraphType = false;

		if (_isNewColorProvider) {

			canEnableGraphType = true;

		} else {

			final MapGraphId graphId = _dialogColorProvider.getGraphId();

			canEnableGraphType = Map3GradientColorManager.getColorProviders(graphId).size() > 1;
		}

		_cboGraphType.setEnabled(canEnableGraphType);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
	}

	private int getGraphIdIndex(final MapGraphId colorId) {

		final ArrayList<Map3ColorDefinition> colorDefinitions = Map3GradientColorManager.getSortedColorDefinitions();

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

		return _dialogColorProvider.getMap3ColorProfile().getProfileImage();
	}

	private ArrayList<RGBVertex> getRgbVertices() {

		return getProfileImage().getRgbVertices();
	}

	private MapGraphId getSelectedGraphId() {

		final ArrayList<Map3ColorDefinition> colorDefinitions = Map3GradientColorManager.getSortedColorDefinitions();
		final int selectionIndex = _cboGraphType.getSelectionIndex();

		final Map3ColorDefinition selectedColorDef = colorDefinitions.get(selectionIndex);

		return selectedColorDef.getGraphId();
	}

	@Override
	public void modifiedColor(final RGB modifiedRGB) {

		if (_isInProntoUpdate) {

			/*
			 * !!! prevent slow down when setting pronto color into the color chooser, it's very
			 * complicated and this is the easiest way to solve it !!!
			 */

			return;
		}

		if (_prontoColorVertexIndex == null || _isProntoColorEnabled == false) {

			// a pronto color is noch selected or is disabled

			return;
		}

		// update model
		final RGBVertex rgbVertex = getRgbVertices().get(_prontoColorVertexIndex);
		rgbVertex.setRGB(modifiedRGB);

		// update UI
		final Label lblColor = _lblVertexColor[_prontoColorVertexIndex];
		updateUI_LabelColor(lblColor.getDisplay(), lblColor, modifiedRGB);

		updateUI_FromVertexColor();
	}

	/**
	 * Save button is pressed.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	protected void okPressed() {

		onApply(true);

		super.okPressed();
	}

	/**
	 * Updates model from the UI and do a live update when selected.
	 * 
	 * @param isForceLiveUpdate
	 * @return Returns <code>true</code> when live update and UI update from the model is done.
	 */
	private boolean onApply(final boolean isForceLiveUpdate) {

		updateModel_FromUI();

		if (isForceLiveUpdate || (_chkLiveUpdate.isEnabled() && _chkLiveUpdate.getSelection())) {

			_mapColorUpdater.applyMapColors(_originalColorProvider, _dialogColorProvider, _isNewColorProvider);

			// after an update, a color provider is not new any more otherwise each update creates a new profile
			_isNewColorProvider = false;

			// set model and UI as when the dialog has been opened
			_originalColorProvider = _dialogColorProvider;
			_dialogColorProvider = _originalColorProvider.clone();

			updateUI_FromModel();

			enableGraphType();

			return true;
		}

		return false;
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
		final Integer vertexIndex = (Integer) vertexLabel.getData(DATA_KEY_VERTEX_INDEX);
		final RGBVertex vertex = getRgbVertices().get(vertexIndex);

		if (event.button == 3) {

			// right button: update color chooser from vertex color

			_colorChooser.setRGB(vertex.getRGB());

		} else {

			// other buttons: update vertex color from color chooser

			final RGB rgb = _colorChooser.getRGB();

			// update model
			vertex.setRGB(rgb);

			// update UI
			updateUI_LabelColor(display, vertexLabel, rgb);

			updateUI_FromVertexColor();
		}
	}

	private void onFieldSelectPronto(final Widget widget) {

		final Integer vertexIndex = (Integer) widget.getData(DATA_KEY_VERTEX_INDEX);

		// keep pronto index
		_prontoColorVertexIndex = vertexIndex;

		// set color into the color chooser
		_isInProntoUpdate = true;
		{
			final RGBVertex vertex = getRgbVertices().get(vertexIndex);
			_colorChooser.setRGB(vertex.getRGB());
		}
		_isInProntoUpdate = false;
	}

	private void onFieldSelectValue(final Widget widget) {

		if (_isInUIUpdate) {
			return;
		}

		final Spinner spinner = (Spinner) widget;
		final Integer vertexIndex = (Integer) spinner.getData(DATA_KEY_VERTEX_INDEX);
		final RGBVertex vertex = getRgbVertices().get(vertexIndex);

		// update model
		vertex.setValue(spinner.getSelection());

		updateModel_FromUI_Vertices();

		// update UI
		updateUI_FromModel_Vertices();

		onApply(false);
	}

	private void onModifyProfileName(final TraverseEvent event) {

		if (_isInUIUpdate) {
			return;
		}

		if (

		// Ignore arrow left/right that within the text it can be navigated
		event.keyCode == SWT.ARROW_LEFT || event.keyCode == SWT.ARROW_RIGHT

		// Ignore Escape key, otherwise a new profile can be created when immediatedly closing a new created profile without doing anything.
				|| event.keyCode == SWT.ESC) {

			return;
		}

		onApply(false);
	}

	private void onSelectControl() {

		updateModel_FromUI();

		if (onApply(false) == false) {

			// UI is not yet updated from the model
			updateUI_FromModel();
		}
	}

	private void restoreState() {

		_colorChooser.restoreState(_state);

		_chkLiveUpdate.setSelection(Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false));

		_isProntoColorEnabled = Util.getStateBoolean(_state, STATE_IS_PRONTO_COLOR, false);
		_chkProntoColor.setSelection(_isProntoColorEnabled);
	}

	private void saveState() {

		_colorChooser.saveState(_state);

		_state.put(STATE_IS_LIVE_UPDATE, _chkLiveUpdate.getSelection());
		_state.put(STATE_IS_PRONTO_COLOR, _chkProntoColor.getSelection());
	}

	private void updateModel_FromUI() {

		// update color provider, set graph id
		final MapGraphId selectedGraphId = getSelectedGraphId();
		_dialogColorProvider.setGraphId(selectedGraphId);

		/*
		 * Update color profile
		 */
		final Map3ColorProfile colorProfile = _dialogColorProvider.getMap3ColorProfile();

		colorProfile.setProfileName(_txtProfileName.getText());
		colorProfile.setIsAbsoluteValues(_rdoAbsoluteValues.getSelection());
		colorProfile.setIsOverwriteLegendValues(_chkOverwriteLegendValues.getSelection());

		// update min/max brightness
		colorProfile.setMinBrightness(_cboMinBrightness.getSelectionIndex());
		colorProfile.setMaxBrightness(_cboMaxBrightness.getSelectionIndex());
		colorProfile.setMinBrightnessFactor(_spinMinBrightness.getSelection());
		colorProfile.setMaxBrightnessFactor(_spinMaxBrightness.getSelection());

		updateModel_FromUI_Vertices();
	}

	/**
	 * Get vertices from UI and sorts them.
	 */
	private void updateModel_FromUI_Vertices() {

		final ArrayList<RGBVertex> rgbVertices = getRgbVertices();
		final int rgbVertexListSize = rgbVertices.size();
		final ArrayList<RGBVertex> newRgbVertices = new ArrayList<RGBVertex>();

		for (int vertexIndex = 0; vertexIndex < rgbVertexListSize; vertexIndex++) {

			/*
			 * create vertices from UI controls
			 */
			final Spinner spinnerOpacity = _spinnerOpacity[vertexIndex];
			final Spinner spinnerVertexValue = _spinnerVertexValue[vertexIndex];

			final int value = spinnerVertexValue.getSelection();
			final Integer sortId = (Integer) spinnerVertexValue.getData(DATA_KEY_SORT_ID);

			final RGB rgb = _lblVertexColor[vertexIndex].getBackground().getRGB();
			final float opacity = (float) (spinnerOpacity.getSelection() / Map3GradientColorManager.OPACITY_DIGITS_FACTOR);

			final RGBVertex rgbVertex = new RGBVertex(sortId);
			rgbVertex.setValue(value);
			rgbVertex.setRGB(rgb);
			rgbVertex.setOpacity(opacity);

			newRgbVertices.add(rgbVertex);
		}

		// sort vertices by value
		Collections.sort(newRgbVertices);

		// update model
		getProfileImage().setVertices(newRgbVertices);

		_isProntoColorEnabled = _chkProntoColor.getSelection();
	}

	private void updateUI_FromModel() {

		_isInUIUpdate = true;
		{
			final Map3ColorProfile colorProfile = _dialogColorProvider.getMap3ColorProfile();

			_txtProfileName.setText(colorProfile.getProfileName());
			_chkOverwriteLegendValues.setSelection(colorProfile.isOverwriteLegendValues());

			final boolean isAbsoluteValues = colorProfile.isAbsoluteValues();
			_rdoAbsoluteValues.setSelection(isAbsoluteValues);
			_rdoRelativeValues.setSelection(!isAbsoluteValues);

			final MapGraphId graphId = _dialogColorProvider.getGraphId();
			_cboGraphType.select(getGraphIdIndex(graphId));

			// update min/max brightness
			_cboMinBrightness.select(colorProfile.getMinBrightness());
			_cboMaxBrightness.select(colorProfile.getMaxBrightness());
			_spinMinBrightness.setSelection(colorProfile.getMinBrightnessFactor());
			_spinMaxBrightness.setSelection(colorProfile.getMaxBrightnessFactor());
		}
		_isInUIUpdate = false;

		updateUI_FromModel_Vertices();

		enableControls();
	}

	private void updateUI_FromModel_Vertices() {

		// check and create vertex fields
		createUI_50_VertexFields();

		final ArrayList<RGBVertex> rgbVerticies = getRgbVertices();

		final int vertexSize = rgbVerticies.size();

		_isInUIUpdate = true;
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				// show highest value at the top accoringly to the displayed legend
				final int keyIndex = vertexSize - 1 - vertexIndex;
				final RGBVertex vertex = rgbVerticies.get(keyIndex);

				// update opacity
				final Spinner spinnerOpacity = _spinnerOpacity[vertexIndex];
				final double opacity = vertex.getOpacity() * Map3GradientColorManager.OPACITY_DIGITS_FACTOR
				// must be rounded otherwise it can be wrong
						+ 0.0001;
				spinnerOpacity.setSelection((int) opacity);

				// update value
				final Spinner spinnerValue = _spinnerVertexValue[vertexIndex];
				spinnerValue.setSelection(vertex.getValue());

				// update color
				final Label lblColor = _lblVertexColor[vertexIndex];
				final RGB vertexRGB = vertex.getRGB();

				lblColor.setToolTipText(NLS.bind(//
						Messages.Map3Color_Dialog_ProfileColor_Tooltip,
						new Object[] { vertexRGB.red, vertexRGB.green, vertexRGB.blue }));

				updateUI_LabelColor(lblColor.getDisplay(), lblColor, vertexRGB);

				// keep vertex references
				spinnerValue.setData(DATA_KEY_VERTEX_INDEX, keyIndex);
				spinnerValue.setData(DATA_KEY_SORT_ID, vertex.getSortId());

				lblColor.setData(DATA_KEY_VERTEX_INDEX, keyIndex);
				_rdoProntoColors[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, keyIndex);
				_actionAddVertex[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, keyIndex);
				_actionDeleteVertex[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, keyIndex);
			}

			_chkProntoColor.setSelection(_isProntoColorEnabled);
		}
		_isInUIUpdate = false;

		/*
		 * Disable remove actions when only 2 colors are available.
		 */
		if (vertexSize <= 2) {
			for (int ix = 0; ix < vertexSize; ix++) {
				_actionDeleteVertex[ix].setEnabled(false);
			}
		}

		// update profile image
		drawProfileImage();
	}

	private void updateUI_FromVertexColor() {

		// invalidate cached colors
		getProfileImage().invalidateCachedColors();

		updateUI_FromModel_Vertices();

		onApply(false);
	}

	/**
	 * Initialize UI.
	 */
	private void updateUI_Initialize() {

		final Collection<Map3ColorDefinition> colorDefinitions = Map3GradientColorManager.getSortedColorDefinitions();

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

	private void updateUI_LabelColor(final Display display, final Label label, final RGB rgb) {

		final Color bgColor = new Color(display, rgb);
		{
			label.setBackground(bgColor);
		}
		bgColor.dispose();
	}

}
