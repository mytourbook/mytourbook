/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.XMLMemento;

/**
 * This is a template for a title area dialog
 */
public class DialogAutomatedImportConfig extends TitleAreaDialog {

	private static final String				DATA_KEY_TOUR_TYPE_ID	= "DATA_KEY_TOUR_TYPE_ID";							//$NON-NLS-1$
	private static final String				DATA_KEY_VERTEX_INDEX	= "DATA_KEY_VERTEX_INDEX";							//$NON-NLS-1$

	private final IPreferenceStore			_prefStore				= TourbookPlugin.getPrefStore();
	private final IDialogSettings			_state					= TourbookPlugin.getState(getClass().getName());

	private MouseWheelListener				_defaultMouseWheelListener;
	private SelectionAdapter				_defaultSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private SelectionAdapter				_vertexTourTypeListener;
	private FocusListener					_vertexValueFocusListener;
	private MouseWheelListener				_vertexValueMouseWheelListener;

	private ActionAddVertex					_actionAddVertex;
	private ActionDeleteVertex[]			_actionDeleteVertex;
	private ActionOpenPrefDialog			_actionOpenTourTypePrefs;

	private boolean							_isInUIUpdate;

	private PixelConverter					_pc;

	private static final TourTypeVertex[]	DEFAULT_VERTICES;

	static {

		DEFAULT_VERTICES = new TourTypeVertex[] {
			//
			new TourTypeVertex(10),
			new TourTypeVertex(30),
			new TourTypeVertex(150),
			new TourTypeVertex(300)
		//
		};
	}
	/**
	 * Contains all vertices.
	 */
	private ArrayList<TourTypeVertex>		_ttVertices				= new ArrayList<TourTypeVertex>(
																			Arrays.asList(DEFAULT_VERTICES));

	/*
	 * UI controls
	 */
	private Composite						_parent;
	private Composite						_vertexOuterContainer;
	private Composite						_vertexContainer;
	private ScrolledComposite				_vertexScrolledContainer;

	private Button							_btnSelectDeviceFolder;
	private Button							_chkTourType;
	private Combo							_comboDevicePath;
	private Label[]							_labelTourTypeIcon;
	private Label[]							_labelSpeedUnit;
	private Link[]							_linkTourType;
	private Spinner[]						_spinnerAvgSpeed;

	private class ActionAddVertex extends Action {

		private int	_vertexIndex;

		public ActionAddVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_AutoImportConfig_Action_AddSpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Add));
		}

		@Override
		public void run() {
			action_AddVertex(_vertexIndex);
		}

		public void setData(final String key, final int vertexIndex) {

			_vertexIndex = vertexIndex;
		}
	}

	private class ActionDeleteVertex extends Action {

		private int	_vertexIndex;

		public ActionDeleteVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_AutoImportConfig_Action_RemoveSpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash_Disabled));
		}

		@Override
		public void run() {
			action_RemoveVertex(_vertexIndex);
		}

		public void setData(final String key, final int vertexIndex) {

			_vertexIndex = vertexIndex;
		}
	}

	private class ActionSetTourType extends Action {

		private int			_vertexIndex;
		private TourType	_tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionSetTourType(final TourType tourType, final boolean isChecked, final int vertexIndex) {

			super(tourType.getName(), AS_CHECK_BOX);

			_vertexIndex = vertexIndex;

			if (isChecked == false) {

				// show image when tour type can be selected, disabled images look ugly on win
				final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
				setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
			}

			setChecked(isChecked);
			setEnabled(isChecked == false);

			_tourType = tourType;
		}

		@Override
		public void run() {
			onSelectTourType(_vertexIndex, _tourType);
		}
	}

	DialogAutomatedImportConfig(final Shell parentShell) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

	}

	private void action_AddVertex(final int vertexIndex) {

		// update model - duplicate current vertex
		final TourTypeVertex currentVertex = _ttVertices.get(vertexIndex);
		addVertex(0, currentVertex.clone());

		// update UI + model
		updateUI_FromModel();
		updateModel_FromUI();
	}

	private void action_RemoveVertex(final int vertexIndex) {

		// update model
		final TourTypeVertex removedVertex = _ttVertices.get(vertexIndex);

		_ttVertices.remove(removedVertex);

		// update UI + model
		updateUI_FromModel();
		updateModel_FromUI();
	}

	private void addPrefChangeListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					checkTourTypes();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addVertex(final int vertexPosition, final TourTypeVertex ttVertex) {

		_ttVertices.add(vertexPosition, ttVertex);

		// sort vertices by value
		Collections.sort(_ttVertices);
	}

	private void checkTourTypes() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean close() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_AutoImportConfig_Dialog_Title);
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_AutoImportConfig_Dialog_Title);
		setMessage(Messages.Dialog_AutoImportConfig_Dialog_Message);

		addPrefChangeListener();

		restoreState();
		updateUI_FromModel();

		enableControls();
	}

	private void createActions() {

		_actionAddVertex = new ActionAddVertex();

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_parent = parent;

		final Composite ui = (Composite) super.createDialogArea(parent);

		initUI(ui);
		createActions();

		createUI(ui);

		return ui;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults()//
//				.numColumns(2)
//				.spacing(10, 8)
				.applyTo(container);
		{
			createUI_10_DeviceFolder(container);
			createUI_20_TourType(container);
		}
	}

	private void createUI_10_DeviceFolder(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * Label: device folder
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Dialog_AutoImportConfig_Label_DeviceFolder);

			/*
			 * Combo: path
			 */
			_comboDevicePath = new Combo(container, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
					.applyTo(_comboDevicePath);
			_comboDevicePath.setVisibleItemCount(20);
			_comboDevicePath.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					validateFields();
				}
			});
			_comboDevicePath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * Button: browse...
			 */
			_btnSelectDeviceFolder = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnSelectDeviceFolder);
			_btnSelectDeviceFolder.setText(Messages.app_btn_browse);
			_btnSelectDeviceFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectDeviceFolder);
		}
	}

	private void createUI_20_TourType(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.spacing(_pc.convertHorizontalDLUsToPixels(4), _pc.convertVerticalDLUsToPixels(4))
				.applyTo(group);
		group.setText(Messages.Dialog_AutoImportConfig_Group_TourType);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_21_EnableTourType(group);
			createUI_22_Actions(group);
			createUI_40_VertexFields(group);
		}
	}

	private void createUI_21_EnableTourType(final Composite parent) {

		// ckeckbox: enable min/max
		_chkTourType = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults()//
//				.span(7, 1)
				.applyTo(_chkTourType);
		_chkTourType.setText(Messages.Dialog_AutoImportConfig_Checkbox_SetTourType);
		_chkTourType.addSelectionListener(_defaultSelectionListener);
	}

	private void createUI_22_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(_actionAddVertex);
		tbm.update(true);
	}

	private void createUI_40_VertexFields(final Composite parent) {

		/*
		 * vertex fields container
		 */
		_vertexOuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(_pc.convertHorizontalDLUsToPixels(10), 0)
				.applyTo(_vertexOuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_vertexOuterContainer);
//		_vertexOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUI_42_VertexFields();
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_42_VertexFields() {

		final int vertexSize = _ttVertices.size();

		if (vertexSize == 0) {
			// this case should not happen
			return;
		}

		// check if required vertex fields are already available
		if (_spinnerAvgSpeed != null && _spinnerAvgSpeed.length == vertexSize) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_vertexScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _vertexScrolledContainer.getOrigin();

			_vertexScrolledContainer.dispose();
		}

		_vertexContainer = createUI_44_VertexScrolledContainer(_vertexOuterContainer);
//		_vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//		final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

		/*
		 * fields
		 */
		_actionDeleteVertex = new ActionDeleteVertex[vertexSize];
		_labelTourTypeIcon = new Label[vertexSize];
		_labelSpeedUnit = new Label[vertexSize];
		_linkTourType = new Link[vertexSize];
		_spinnerAvgSpeed = new Spinner[vertexSize];

		_vertexContainer.setRedraw(false);
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				/*
				 * Spinner: Speed value
				 */
				final Spinner spinnerValue = new Spinner(_vertexContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(0);
				spinnerValue.setMaximum(3000);
				spinnerValue.setToolTipText(Messages.Dialog_AutoImportConfig_Spinner_Speed_Tooltip);
				spinnerValue.addMouseWheelListener(_vertexValueMouseWheelListener);
				spinnerValue.addFocusListener(_vertexValueFocusListener);

				/*
				 * Label: Speed unit
				 */
				final Label lblUnit = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblUnit);
				lblUnit.setText(UI.UNIT_LABEL_SPEED);

				/*
				 * Label with icon: Tour type (CLabel cannot be disabled !!!)
				 */
				final Label lblTourTypeIcon = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.hint(16, 16)
						.applyTo(lblTourTypeIcon);

				/*
				 * Link: Tour type
				 */
				final Link linkTourType = new Link(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(linkTourType);
				linkTourType.setText(Messages.tour_editor_label_tour_type);
				linkTourType.addSelectionListener(_vertexTourTypeListener);
//				linkTourType.setBackground(bgColor);

				/*
				 * Context menu: Tour type
				 */
				final MenuManager menuMgr = new MenuManager();
				menuMgr.setRemoveAllWhenShown(true);
				menuMgr.addMenuListener(new IMenuListener() {
					@Override
					public void menuAboutToShow(final IMenuManager menuMgr) {
						fillTourTypeMenu(menuMgr, linkTourType);
					}
				});
				final Menu ttContextMenu = menuMgr.createContextMenu(linkTourType);
				linkTourType.setMenu(ttContextMenu);

				/*
				 * Action: Delete vertex
				 */
				final ActionDeleteVertex actionDeleteVertex = new ActionDeleteVertex();
				createUI_ActionButton(_vertexContainer, actionDeleteVertex);

				/*
				 * Keep vertex controls
				 */
				_actionDeleteVertex[vertexIndex] = actionDeleteVertex;
				_labelTourTypeIcon[vertexIndex] = lblTourTypeIcon;
				_labelSpeedUnit[vertexIndex] = lblUnit;
				_linkTourType[vertexIndex] = linkTourType;
				_spinnerAvgSpeed[vertexIndex] = spinnerValue;
			}
		}
		_vertexContainer.setRedraw(true);

		_vertexOuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_vertexScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_44_VertexScrolledContainer(final Composite parent) {

		// scrolled container
		_vertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_vertexScrolledContainer);
		_vertexScrolledContainer.setExpandVertical(true);
		_vertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_vertexScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
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

	/**
	 * Creates an action in a toolbar.
	 * 
	 * @param parent
	 * @param action
	 * @return
	 */
	private ToolBarManager createUI_ActionButton(final Composite parent, final Action action) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(action);
		tbm.update(true);

		return tbm;
	}

	private void enableControls() {

		final boolean isTourType = _chkTourType.getSelection();

		for (final ActionDeleteVertex action : _actionDeleteVertex) {
			action.setEnabled(isTourType);
		}

		for (final Spinner spinner : _spinnerAvgSpeed) {
			spinner.setEnabled(isTourType);
		}

		for (final Link link : _linkTourType) {
			link.setEnabled(isTourType);
		}

		for (final Label label : _labelSpeedUnit) {
			label.setEnabled(isTourType);
		}

		for (final Label label : _labelTourTypeIcon) {

			if (isTourType) {

				final Integer vertexIndex = (Integer) label.getData(DATA_KEY_VERTEX_INDEX);

				final TourTypeVertex vertex = _ttVertices.get(vertexIndex);
				final long tourTypeId = vertex.tourTypeId;

				label.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId));

			} else {

				// disabled image looks ugly
				label.setImage(null);
			}
		}
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr, final Link linkTourType) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = null;

		final int vertexIndex = (int) linkTourType.getData(DATA_KEY_VERTEX_INDEX);

		final ArrayList<TourTypeVertex> vertices = _ttVertices;
//		if (selectedTours.size() == 1) {
//			checkedTourType = selectedTours.get(0).getTourType();
//		}

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetTourType action = new ActionSetTourType(tourType, isChecked, vertexIndex);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		/*
		 * Field listener
		 */
		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelection();
			}
		};

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelection();
			}
		};

		/*
		 * Vertex listener
		 */
		_vertexValueMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};

		_vertexValueFocusListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {}

			@Override
			public void focusLost(final FocusEvent event) {
				onModifyVertexValue(event.widget);
			}
		};

		_vertexTourTypeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				UI.openControlMenu((Link) event.widget);
			}
		};
	}

	@Override
	protected void okPressed() {

		saveState();

		super.okPressed();
	}

	private void onModifyVertexValue(final Widget widget) {

		if (_isInUIUpdate) {
			return;
		}

		final Spinner spinner = (Spinner) widget;
		final Integer vertexIndex = (Integer) spinner.getData(DATA_KEY_VERTEX_INDEX);
		final TourTypeVertex vertex = _ttVertices.get(vertexIndex);

		// update model
		vertex.avgSpeed = spinner.getSelection();

		updateModel_FromUI();

		// update UI
		updateUI_FromModel();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(_parent.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

//		dialog.setFilterPath(getExportPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			_comboDevicePath.setText(selectedDirectoryName);
		}
	}

	private void onSelection() {

		enableControls();
	}

	private void onSelectTourType(final int vertexIndex, final TourType tourType) {

		// update model
		_ttVertices.get(vertexIndex).tourTypeId = tourType.getTypeId();

		/*
		 * Update UI
		 */
		final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());

		final Label ttIcon = _labelTourTypeIcon[vertexIndex];
		final Link ttLink = _linkTourType[vertexIndex];

		ttIcon.setImage(image);
		ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);

		_vertexOuterContainer.layout();
	}

	private void restoreState() {

		_chkTourType.setSelection(_prefStore.getBoolean(ITourbookPreferences.AUTOMATED_IMPORT_IS_TOUR_TYPE));
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.AUTOMATED_IMPORT_IS_TOUR_TYPE, _chkTourType.getSelection());
	}

	private void saveState_CustomColors(final IDialogSettings state) {

//		// Build the XML block for writing the bindings and active scheme.
//		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS);
//
//		saveState_CustomColors_Colors(xmlMemento);
//
//		// Write the XML block to the state store.
//		final Writer writer = new StringWriter();
//		try {
//
//			xmlMemento.save(writer);
//			state.put(XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS, writer.toString());
//
//		} catch (final IOException e) {
//
//			StatusUtil.log(e);
//
//		} finally {
//
//			try {
//				writer.close();
//			} catch (final IOException e) {
//				StatusUtil.log(e);
//			}
//		}
	}

	private void saveState_CustomColors_Colors(final XMLMemento xmlMemento) {

//		xmlMemento.putInteger(ATTR_NUMBER_OF_HORIZONTAL_COLORS, NUMBER_OF_HORIZONTAL_COLORS);
//		xmlMemento.putInteger(ATTR_NUMBER_OF_VERTICAL_COLORS, NUMBER_OF_VERTICAL_COLORS);
//
//		for (int verticalIndex = 0; verticalIndex < NUMBER_OF_VERTICAL_COLORS; verticalIndex++) {
//
//			final Label[] _horizontalColors = _customColors[verticalIndex];
//
//			for (int horizontalIndex = 0; horizontalIndex < NUMBER_OF_HORIZONTAL_COLORS; horizontalIndex++) {
//
//				final Label colorLabel = _horizontalColors[horizontalIndex];
//
//				final Object labelData = colorLabel.getData();
//				if (labelData instanceof RGB) {
//
//					final RGB rgb = (RGB) labelData;
//
//					final IMemento xmlCustomColor = xmlMemento.createChild(TAG_CUSTOM_COLOR);
//
//					xmlCustomColor.putInteger(ATTR_RED, rgb.red);
//					xmlCustomColor.putInteger(ATTR_GREEN, rgb.green);
//					xmlCustomColor.putInteger(ATTR_BLUE, rgb.blue);
//
//					xmlCustomColor.putInteger(ATTR_POSITION_HORIZONTAL, horizontalIndex);
//					xmlCustomColor.putInteger(ATTR_POSITION_VERTICAL, verticalIndex);
//				}
//			}
//		}
	}

	/**
	 * Get vertices from UI and sort them.
	 */
	private void updateModel_FromUI() {

		final int ttVertexListSize = _ttVertices.size();

		final ArrayList<TourTypeVertex> newVertices = new ArrayList<TourTypeVertex>();

		for (int vertexIndex = 0; vertexIndex < ttVertexListSize; vertexIndex++) {

			/*
			 * create vertices from UI controls
			 */
			final Spinner spinnerAvgSpeed = _spinnerAvgSpeed[vertexIndex];
			final Link linkTourType = _linkTourType[vertexIndex];

			final TourTypeVertex ttVertex = new TourTypeVertex();

			ttVertex.avgSpeed = spinnerAvgSpeed.getSelection();
			ttVertex.tourTypeId = (long) linkTourType.getData(DATA_KEY_TOUR_TYPE_ID);

			newVertices.add(ttVertex);
		}

		// sort vertices by value
		Collections.sort(newVertices);

		// update model
		_ttVertices.clear();
		_ttVertices.addAll(newVertices);
	}

	private void updateUI_FromModel() {

		// check and create vertex fields
		createUI_42_VertexFields();

		final int vertexSize = _ttVertices.size();

		_isInUIUpdate = true;
		{
			final net.tourbook.ui.UI uiInstance = net.tourbook.ui.UI.getInstance();

			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				final TourTypeVertex vertex = _ttVertices.get(vertexIndex);
				final long tourTypeId = vertex.tourTypeId;

				final Spinner spinnerAvgSpeed = _spinnerAvgSpeed[vertexIndex];
				final Link linkTourType = _linkTourType[vertexIndex];
				final Label labelTourTypeIcon = _labelTourTypeIcon[vertexIndex];

				// update UI
				spinnerAvgSpeed.setSelection(vertex.avgSpeed);
				linkTourType.setText(UI.LINK_TAG_START + uiInstance.getTourTypeLabel(tourTypeId) + UI.LINK_TAG_END);
				labelTourTypeIcon.setImage(uiInstance.getTourTypeImage(tourTypeId));

				// keep vertex references
				labelTourTypeIcon.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
				linkTourType.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
				spinnerAvgSpeed.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
				_actionDeleteVertex[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, vertexIndex);

				linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourTypeId);
			}
		}
		_isInUIUpdate = false;

		/*
		 * Disable remove actions when only 1 vertex is available.
		 */
		if (vertexSize <= 1) {
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {
				_actionDeleteVertex[vertexIndex].setEnabled(false);
			}
		}
	}

	private void validateFields() {
		// TODO Auto-generated method stub

	}

}
