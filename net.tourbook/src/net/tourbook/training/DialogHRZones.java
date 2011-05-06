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
package net.tourbook.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.Widget;

public class DialogHRZones extends TitleAreaDialog {

	private final IDialogSettings		_state		= TourbookPlugin.getDefault() //
															.getDialogSettingsSection(getClass().getName());
	private TourPerson					_person;

	private Image						_imageTrash	= TourbookPlugin
															.getImageDescriptor(Messages.Image__App_Trash)
															.createImage();

	private ArrayList<TourPersonHRZone>	_hrZones;

	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private Composite					_hrZoneOuterContainer;
	private ScrolledComposite			_hrZoneScrolledContainer;

	// these arrays has the same sequence like _hrZones
	private Text[]						_txtHRZoneName;
	private Spinner[]					_spinnerMinPulse;
	private Spinner[]					_spinnerMaxPulse;
	private Button[]					_btnTrash;
	private Label[]						_labelGtLt;															// Gt... greater than, Lt...Lower than

	private Button						_btnAddZone;
	private Button						_btnRemoveZone;
	private Button						_btnSortZones;

	public DialogHRZones(final Shell parentShell, final TourPerson tourPerson) {

		super(parentShell);

		_person = tourPerson;

		// clone hr zone's
		_hrZones = new ArrayList<TourPersonHRZone>();
		_hrZones.addAll(tourPerson.getHrZones());
		Collections.sort(_hrZones);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	private void actionAddZone() {

		updateModelFromUI();

		final TourPersonHRZone hrZone = new TourPersonHRZone(_person);
		_hrZones.add(hrZone);

		Collections.sort(_hrZones);

		updateUIFromModel();
	}

	private void actionRemoveZone() {

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

		updateUIFromModel();
	}

	private void actionSortZone() {

		updateModelFromUI();
		updateUIFromModel();
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

		_pc = new PixelConverter(parent);

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
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(hrZoneContainer);
//		hrZoneContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI23HrZoneHeader(hrZoneContainer);
			createUI24HrZoneFields(hrZoneContainer);
		}

		return hrZoneContainer;
	}

	private void createUI23HrZoneHeader(final Composite parent) {

		/*
		 * label: zone
		 */
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.hint(250, SWT.DEFAULT)
				.align(SWT.FILL, SWT.BOTTOM)
				.applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Zone);

		/*
		 * header label: min pulse
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BOTTOM).applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_Pulse);

		/*
		 * label: ...
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(2), SWT.DEFAULT).applyTo(label);
		label.setText(UI.EMPTY_STRING);

		/*
		 * header label: max pulse
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BOTTOM).applyTo(label);
		label.setText(Messages.Dialog_HRZone_Label_Header_MaxPulse);

		/*
		 * label: %
		 */
		label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(UI.EMPTY_STRING);

		/*
		 * header label: trash
		 */
		final CLabel iconImport = new CLabel(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(iconImport);
		iconImport.setImage(_imageTrash);
		iconImport.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
	}

	private void createUI24HrZoneFields(final Composite parent) {

		final int hrZoneSize = _hrZones.size();

		final SelectionAdapter minSelectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectMinPulse(e.widget);
			}
		};
		final MouseWheelListener minMouseListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelectMinPulse(event.widget);
			}
		};

		final SelectionAdapter maxSelectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectMaxPulse(e.widget);
			}
		};
		final MouseWheelListener maxMouseListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelectMaxPulse(event.widget);
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
		_txtHRZoneName = new Text[hrZoneSize];
		_spinnerMinPulse = new Spinner[hrZoneSize];
		_spinnerMaxPulse = new Spinner[hrZoneSize];
		_labelGtLt = new Label[hrZoneSize];
		_btnTrash = new Button[hrZoneSize];

		for (int zoneIndex = 0; zoneIndex < hrZoneSize; zoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(zoneIndex);

			/*
			 * text: hr zone name
			 */
			final Text txtHRZoneName = _txtHRZoneName[zoneIndex] = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(txtHRZoneName);
//				txtHRZoneName.addModifyListener(eleModifyListener);
//				txtHRZoneName.addVerifyListener(eleVerifyListener);

			// update UI
			final String zoneName = hrZone.getZoneName();
			txtHRZoneName.setText(zoneName == null ? UI.EMPTY_STRING : zoneName);

			// keep reference to hr zone
			txtHRZoneName.setData(hrZone);

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
			 * label: <, >
			 */
			Label label = _labelGtLt[zoneIndex] = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.EMPTY_STRING);

			/*
			 * spinner: max pulse
			 */
			final Spinner spinnerMaxPulse = _spinnerMaxPulse[zoneIndex] = new Spinner(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(spinnerMaxPulse);
			spinnerMaxPulse.setMinimum(0);
			spinnerMaxPulse.setMaximum(120);
			spinnerMaxPulse.addSelectionListener(maxSelectListener);
			spinnerMaxPulse.addMouseWheelListener(maxMouseListener);
			spinnerMaxPulse.setData(zoneIndex);

			/*
			 * label: %
			 */
			label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.SYMBOL_PERCENTAGE);

			/*
			 * checkbox
			 */
			final Button checkbox = _btnTrash[zoneIndex] = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(checkbox);
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
					actionAddZone();
				}
			});

			/*
			 * button: sort zones
			 */
			_btnSortZones = new Button(container, SWT.NONE);
			_btnSortZones.setText(Messages.Dialog_HRZone_Button_SortZone);
			_btnSortZones.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					actionSortZone();
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
					actionRemoveZone();
				}
			});
			setButtonLayoutData(_btnRemoveZone);
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

		_btnSortZones.setEnabled(hrZoneSize > 1);
		_btnRemoveZone.setEnabled(isRemoveAllowed);

//		/*
//		 * disable checkboxes when only 2 hr zones are available
//		 */
//		if (hrZoneSize <= 2) {
//			for (int ix = 0; ix < hrZoneSize; ix++) {
//				_btnTrash[ix].setEnabled(false);
//			}
//		}

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return _state;
	}

	@Override
	protected void okPressed() {

		_person.setHrZones(new HashSet<TourPersonHRZone>(_hrZones));

		super.okPressed();
	}

	private void onDispose() {

		if (_imageTrash != null) {
			_imageTrash.dispose();
		}
	}

	private void onSelectMaxPulse(final Widget widget) {

		final Object data = widget.getData();
		if (data instanceof Integer) {
			final Integer zoneIndex = (Integer) data;

			final Spinner spMinPulse = _spinnerMinPulse[zoneIndex];
			final Spinner spMaxPulse = _spinnerMaxPulse[zoneIndex];

			// ensure min <= max
			if (spMinPulse.getSelection() > spMaxPulse.getSelection()) {
				spMinPulse.setSelection(spMaxPulse.getSelection());
			}
		}
	}

	private void onSelectMinPulse(final Widget widget) {

		final Object data = widget.getData();
		if (data instanceof Integer) {
			final Integer zoneIndex = (Integer) data;

			final Spinner spMinPulse = _spinnerMinPulse[zoneIndex];
			final Spinner spMaxPulse = _spinnerMaxPulse[zoneIndex];

			// ensure max >= min
			if (spMaxPulse.getSelection() < spMinPulse.getSelection()) {
				spMaxPulse.setSelection(spMinPulse.getSelection());
			}
		}
	}

	private void updateModelFromUI() {

		for (int hrZoneIndex = 0; hrZoneIndex < _txtHRZoneName.length; hrZoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(hrZoneIndex);

			int minValue = _spinnerMinPulse[hrZoneIndex].getSelection();
			int maxValue = _spinnerMaxPulse[hrZoneIndex].getSelection();

			// keep zone border
			if (hrZone.getZoneMinValue() == Integer.MIN_VALUE) {
				minValue = Integer.MIN_VALUE;
			}
			if (hrZone.getZoneMaxValue() == Integer.MAX_VALUE) {
				maxValue = Integer.MAX_VALUE;
			}

			hrZone.setZoneMinValue(minValue);
			hrZone.setZoneMaxValue(maxValue);

			hrZone.setZoneName(_txtHRZoneName[hrZoneIndex].getText());
		}

		Collections.sort(_hrZones);
	}

	private void updateUIFromModel() {

		if (_hrZones.size() == 0) {

			// create default zones

			_hrZones.addAll(TrainingManager.createHrZones(_person, TrainingManager.HR_ZONE_TEMPLATE_01));
			Collections.sort(_hrZones);
		}

		createUI20HrZoneScrolledContainer(_hrZoneOuterContainer);

		for (int hrZoneIndex = 0; hrZoneIndex < _txtHRZoneName.length; hrZoneIndex++) {

			final TourPersonHRZone hrZone = _hrZones.get(hrZoneIndex);

			if (hrZone.getZoneMinValue() == Integer.MIN_VALUE) {

				_spinnerMinPulse[hrZoneIndex].setVisible(false);
				_spinnerMaxPulse[hrZoneIndex].setSelection(hrZone.getZoneMaxValue());

//				_labelGtLt[hrZoneIndex].setText(UI.SYMBOL_LESS_THAN);
				_btnTrash[hrZoneIndex].setVisible(false);

				continue;
			}

			if (hrZone.getZoneMaxValue() == Integer.MAX_VALUE) {

				_spinnerMinPulse[hrZoneIndex].setSelection(hrZone.getZoneMinValue());
				_spinnerMaxPulse[hrZoneIndex].setVisible(false);

//				_labelGtLt[hrZoneIndex].setText(UI.SYMBOL_GREATER_THAN);
				_btnTrash[hrZoneIndex].setVisible(false);

				continue;
			}

			_spinnerMinPulse[hrZoneIndex].setSelection(hrZone.getZoneMinValue());
			_spinnerMaxPulse[hrZoneIndex].setSelection(hrZone.getZoneMaxValue());

			_labelGtLt[hrZoneIndex].setText(UI.SYMBOL_DASH);
		}

		enableControls();
	}
}
