/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class DialogHRZones extends TitleAreaDialog {

	private final IDialogSettings		_state		= TourbookPlugin.getDefault() //
															.getDialogSettingsSection(getClass().getName());
	private TourPerson					_person;

	private Image						_imageTrash	= TourbookPlugin
															.getImageDescriptor(Messages.Image__App_Trash)
															.createImage();

	private ArrayList<TourPersonHRZone>	_hrZones;
	private boolean						_isUpdateUI;

	/*
	 * UI controls
	 */
	private Composite					_hrZoneOuterContainer;
	private ScrolledComposite			_hrZoneScrolledContainer;

	// these arrays has the same sequence like _hrZones
	private Text[]						_txtZoneName;
	private Text[]						_txtNameShortcut;
	private Spinner[]					_spinnerMinPulse;
	private Label[]						_labelMaxPulse;
	private ColorSelector[]				_colorSelector;
	private Button[]					_btnTrash;

	private Button						_btnAddZone;
	private Button						_btnRemoveZone;
	private Button						_btnSortZones;

//	private Button						_btnApply;

	public DialogHRZones(final Shell parentShell, final TourPerson tourPerson) {

		super(parentShell);

		_person = tourPerson;

		// clone hr zone's
		_hrZones = new ArrayList<TourPersonHRZone>();
		_hrZones.addAll(tourPerson.getHrZonesSorted());

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_HRZone_DialogTitle);

		shell.addDisposeListener(new DisposeListener() {
			@Override
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

		setTitle(Messages.Dialog_HRZone_DialogTitle);
		setMessage(Messages.Dialog_HRZone_DialogMessage);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
//		final Button btnOK = getButton(IDialogConstants.OK_ID);
//		btnOK.setText(Messages.App_Action_Save);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite ui = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(ui);

		updateUIFromModel();

		return ui;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).spacing(20, 0).applyTo(container);
		{
			createUI10HRZone(container);
			createUI50Actions(container);
		}
	}

	private void createUI10HRZone(final Composite parent) {

		/*
		 * hr zone fields
		 */
		_hrZoneOuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneOuterContainer);
		GridLayoutFactory.fillDefaults().applyTo(_hrZoneOuterContainer);

		createUI20HrZoneScrolledContainer(_hrZoneOuterContainer);
	}

	private void createUI20HrZoneScrolledContainer(final Composite parent) {

		Point scrollBackup = null;

		// dispose previous ui
		if (_hrZoneScrolledContainer != null) {

			// get current scroll position
			scrollBackup = _hrZoneScrolledContainer.getOrigin();

			// dispose previous fields
			_hrZoneScrolledContainer.dispose();
		}

		// scrolled container
		_hrZoneScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_hrZoneScrolledContainer);

		final Composite hrZoneInnerContainer = createUI22HrZoneInnerContainer(_hrZoneScrolledContainer);

		_hrZoneScrolledContainer.setContent(hrZoneInnerContainer);
		_hrZoneScrolledContainer.setExpandVertical(true);
		_hrZoneScrolledContainer.setExpandHorizontal(true);
		_hrZoneScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_hrZoneScrolledContainer.setMinSize(hrZoneInnerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		_hrZoneOuterContainer.layout(true, true);

		// set scroll position to previous position
		if (scrollBackup != null) {
			_hrZoneScrolledContainer.setOrigin(scrollBackup);
		}
	}

	private Composite createUI22HrZoneInnerContainer(final Composite parent) {

		// hr zone container
		final Composite hrZoneContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(8).applyTo(hrZoneContainer);
//		hrZoneContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI23HrZoneHeader(hrZoneContainer);
			createUI24HrZoneFields(hrZoneContainer);
		}

		return hrZoneContainer;
	}

	private void createUI23HrZoneHeader(final Composite parent) {

		/*
		 * label: color
		 */
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.BOTTOM)
				.applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Color);

		/*
		 * label: zone name
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.hint(250, SWT.DEFAULT)
				.align(SWT.FILL, SWT.BOTTOM)
				.applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Zone);

		/*
		 * label: zone shortcut
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BOTTOM).applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_ZoneShortcut);

		/*
		 * header label: pulse
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BOTTOM).span(3, 1).applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Pulse);

		/*
		 * label: %
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(UI.EMPTY_STRING);

		/*
		 * label: trash
		 */
		final CLabel iconImport = new CLabel(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).indent(10, 0).applyTo(iconImport);
		iconImport.setImage(_imageTrash);
		iconImport.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
	}

	private void createUI24HrZoneFields(final Composite parent) {

		final int hrZoneSize = _hrZones.size();

		final SelectionAdapter minSelectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (_isUpdateUI) {
					return;
				}
				enableControls();
			}
		};
		final MouseWheelListener minMouseListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {

				if (_isUpdateUI) {
					return;
				}
				UI.adjustSpinnerValueOnMouseScroll(event);

				enableControls();
			}
		};
		final IPropertyChangeListener colorListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				if (_isUpdateUI) {
					return;
				}
				enableControls();
			}
		};
		final ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {

				if (_isUpdateUI) {
					return;
				}
				enableControls();
			}
		};
		final SelectionAdapter trashSelectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
			}
		};

		/*
		 * fields
		 */
		_txtZoneName = new Text[hrZoneSize];
		_txtNameShortcut = new Text[hrZoneSize];
		_spinnerMinPulse = new Spinner[hrZoneSize];
		_labelMaxPulse = new Label[hrZoneSize];
		_colorSelector = new ColorSelector[hrZoneSize];
		_btnTrash = new Button[hrZoneSize];

		for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

			/*
			 * color: hr zone
			 */
			final ColorSelector colorSelector = _colorSelector[zoneIndex] = new ColorSelector(parent);
			colorSelector.addListener(colorListener);

			/*
			 * text: hr zone name
			 */
			final Text txtHRZoneName = _txtZoneName[zoneIndex] = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(txtHRZoneName);
			txtHRZoneName.addModifyListener(modifyListener);

			/*
			 * text: name shortcut
			 */
			final Text txtNameShortcut = _txtNameShortcut[zoneIndex] = new Text(parent, SWT.SINGLE
					| SWT.LEAD
					| SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(txtNameShortcut);
			txtNameShortcut.addModifyListener(modifyListener);

			/*
			 * spinner: min pulse
			 */
			final Spinner spinnerMinPulse = _spinnerMinPulse[zoneIndex] = new Spinner(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(spinnerMinPulse);
			spinnerMinPulse.setMinimum(0);
			spinnerMinPulse.setMaximum(120);
			spinnerMinPulse.addSelectionListener(minSelectListener);
			spinnerMinPulse.addMouseWheelListener(minMouseListener);
			spinnerMinPulse.setData(zoneIndex);

			/*
			 * label: ...
			 */
			Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.SYMBOL_DASH);

			/*
			 * label: max pulse
			 */
			final Label labelMaxPulse = _labelMaxPulse[zoneIndex] = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(labelMaxPulse);
			labelMaxPulse.setData(zoneIndex);

			/*
			 * label: %
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.SYMBOL_PERCENTAGE);

			/*
			 * checkbox: trash
			 */
			final Button checkbox = _btnTrash[zoneIndex] = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).indent(10, 0).applyTo(checkbox);
			checkbox.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
			checkbox.addSelectionListener(trashSelectListener);
		}
	}

	private void createUI50Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: add zone
			 */
			_btnAddZone = new Button(container, SWT.NONE);
			_btnAddZone.setText(Messages.Dialog_HRZone_Button_AddZone);
			setButtonLayoutData(_btnAddZone);
			_btnAddZone.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddZone();
				}
			});

			/*
			 * button: sort zones
			 */
			_btnSortZones = new Button(container, SWT.NONE);
			_btnSortZones.setText(Messages.Dialog_HRZone_Button_SortZone);
			_btnSortZones.setToolTipText(Messages.Dialog_HRZone_Button_SortZone_Tooltip);
			_btnSortZones.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSortZone();
				}
			});
			setButtonLayoutData(_btnSortZones);

			/*
			 * button: remove zone
			 */
			_btnRemoveZone = new Button(container, SWT.NONE);
			_btnRemoveZone.setText(Messages.Dialog_HRZone_Button_RemoveZone);
			_btnRemoveZone.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onRemoveZone();
				}
			});
			setButtonLayoutData(_btnRemoveZone);

// this button was disabled because	it's a complex task and time consuming task (tours must be updated)
//			/*
//			 * button: remove zone
//			 */
//			_btnApply = new Button(container, SWT.NONE);
//			_btnApply.setText(Messages.Dialog_HRZone_Button_Apply);
//			_btnApply.setToolTipText(Messages.Dialog_HRZone_Button_Apply_Tooltip);
//			_btnApply.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onApply();
//				}
//			});
//			setButtonLayoutData(_btnApply);
		}
	}

	private void enableControls() {

		final int hrZoneSize = _hrZones.size();
		boolean isRemoveAllowed = false;

		if (hrZoneSize > 2 && _btnTrash != null) {

			for (final Button btnTrash : _btnTrash) {
				if (btnTrash.getSelection()) {
					isRemoveAllowed = true;
					break;
				}
			}
		}

		_btnAddZone.setEnabled(hrZoneSize < TourData.MAX_HR_ZONES);
		_btnSortZones.setEnabled(hrZoneSize > 1);
		_btnRemoveZone.setEnabled(isRemoveAllowed);
//		_btnApply.setEnabled(_isPersonModified);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return _state;
	}

	@Override
	protected void okPressed() {

		okPressedActions();

		super.okPressed();
	}

	private void okPressedActions() {

		updateModelFromUI();

		_person.setHrZones(new HashSet<TourPersonHRZone>(_hrZones));
	}

	private void onAddZone() {

		updateModelFromUI();

		final TourPersonHRZone hrZone = new TourPersonHRZone(_person);
		_hrZones.add(hrZone);

		Collections.sort(_hrZones);

		updateUIFromModel();
	}

//	private void onApply() {
//
//		okPressedActions();
//
//		_prefStore.setValue(ITourbookPreferences.HR_ZONES_ARE_MODIFIED, Math.random());
//	}

	private void onDispose() {

		if (_imageTrash != null) {
			_imageTrash.dispose();
		}
	}

	private void onRemoveZone() {

		updateModelFromUI();

		final ArrayList<TourPersonHRZone> removedZones = new ArrayList<TourPersonHRZone>();
		int zoneIndex = 0;

		// collect all hr zones which should be removed
		for (final Button btnTrash : _btnTrash) {

			if (btnTrash.getSelection()) {
				removedZones.add(_hrZones.get(zoneIndex));
			}

			zoneIndex++;
		}

		_hrZones.removeAll(removedZones);

		updateModelMaxValues();
		updateUIFromModel();
	}

	private void onSortZone() {

		updateModelFromUI();
		updateUIFromModel();
	}

//	private void onSelectMinPulse(final Widget widget) {
//
//		final Object data = widget.getData();
//		if (data instanceof Integer) {
//			final Integer zoneIndex = (Integer) data;
//
//			final Spinner spMinPulse = _spinnerMinPulse[zoneIndex];
//
//			// ensure max >= min
////			if (spMaxPulse.getSelection() < spMinPulse.getSelection()) {
////				spMaxPulse.setSelection(spMinPulse.getSelection());
////			}
//		}
//	}

	private void updateModelFromUI() {

		final int zoneLength = _txtZoneName.length;

		for (int zoneIndex = 0; zoneIndex < zoneLength; zoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(zoneIndex);

			hrZone.setZoneMinValue(_spinnerMinPulse[zoneIndex].getSelection());

			hrZone.setZoneName(_txtZoneName[zoneIndex].getText());
			hrZone.setNameShortcut(_txtNameShortcut[zoneIndex].getText());

			hrZone.setColor(_colorSelector[zoneIndex].getColorValue());
		}

		Collections.sort(_hrZones);

		updateModelMaxValues();
	}

	/**
	 * set max value from the previous min value
	 */
	private void updateModelMaxValues() {

		int maxValue = 0;
		final int zoneLength = _hrZones.size();

		for (int zoneIndex = 0; zoneIndex < zoneLength; zoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(zoneIndex);

			if (zoneIndex < zoneLength - 1) {

				final int prevZoneMinValue = _hrZones.get(zoneIndex + 1).getZoneMinValue();

				maxValue = prevZoneMinValue - 1;

			} else if (zoneIndex == zoneLength - 1) {

				// set last zone to infinity

				maxValue = Integer.MAX_VALUE;
			}

			final int zoneMinValue = hrZone.getZoneMinValue();
			if (maxValue < zoneMinValue) {
				maxValue = zoneMinValue;
			}

			hrZone.setZoneMaxValue(maxValue);
		}
	}

	private void updateUIFromModel() {

		_isUpdateUI = true;
		{
			if (_hrZones.size() == 0) {

				// create default zones

				_hrZones.addAll(TrainingManager.createHrZones(_person, TrainingManager.HR_ZONE_TEMPLATE_01));
			}

			Collections.sort(_hrZones);

			createUI20HrZoneScrolledContainer(_hrZoneOuterContainer);

			final int zoneLength = _txtZoneName.length;

			for (int hrIndex = 0; hrIndex < zoneLength; hrIndex++) {

				final TourPersonHRZone hrZone = _hrZones.get(hrIndex);

				// update UI
				final String zoneName = hrZone.getZoneName();
				_txtZoneName[hrIndex].setText(zoneName == null ? UI.EMPTY_STRING : zoneName);

				final String nameShortcut = hrZone.getNameShortcut();
				_txtNameShortcut[hrIndex].setText(nameShortcut == null ? UI.EMPTY_STRING : nameShortcut);

				_colorSelector[hrIndex].setColorValue(hrZone.getColor());

				if (hrIndex == 0 || hrIndex == zoneLength - 1) {

					_btnTrash[hrIndex].setVisible(false);
				}

				final int zoneMinValue = hrZone.getZoneMinValue();
				final int zoneMaxValue = hrZone.getZoneMaxValue();

				final String zoneMaxValueText = zoneMaxValue == Integer.MAX_VALUE //
						? Messages.App_Label_max
						: Integer.toString(zoneMaxValue);

				_spinnerMinPulse[hrIndex].setSelection(zoneMinValue);
				_labelMaxPulse[hrIndex].setText(zoneMaxValueText);
			}

			_hrZoneOuterContainer.layout(true, true);

			enableControls();
		}
		_isUpdateUI = false;
	}
}
