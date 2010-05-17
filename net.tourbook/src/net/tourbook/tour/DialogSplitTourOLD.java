/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

/**
 * Split tour at a time slice position and save extracted time slices as a new tour
 */
public class DialogSplitTourOLD extends TitleAreaDialog {

	private static final String		STATE_SPLIT_METHOD							= "SplitMethod";					//$NON-NLS-1$
	private static final String		STATE_SPLIT_METHOD_REMOVE					= "remove";						//$NON-NLS-1$
	private static final String		STATE_SPLIT_METHOD_KEEP						= "keep";							//$NON-NLS-1$

	private static final String		STATE_TYPE_SELECTED_ID						= "TourTypeId";					//$NON-NLS-1$
	private static final String		STATE_TYPE_SOURCE							= "TourTypeSource";				//$NON-NLS-1$
	private static final String		STATE_TYPE_SOURCE_FROM_TOUR					= "fromTour";						//$NON-NLS-1$
	private static final String		STATE_TYPE_SOURCE_PREVIOUS_SPLITTED_TOUR	= "previous";						//$NON-NLS-1$
	private static final String		STATE_TYPE_SOURCE_CUSTOM					= "custom";						//$NON-NLS-1$

	/**
	 * split method states
	 */
	private static final String[]	ALL_STATES_SPLIT_METHOD						= new String[] {
			STATE_SPLIT_METHOD_REMOVE,
			STATE_SPLIT_METHOD_KEEP											//
																				};
	private static final String[]	STATE_TEXT_SPLIT_METHOD						= new String[] {
			Messages.Dialog_SplitTour_ComboText_RemoveSlices,
			Messages.Dialog_SplitTour_ComboText_KeepSlices						//
																				};

	/**
	 * tour type states
	 */
	private static final String[]	ALL_STATES_TOUR_TYPE						= new String[] {
			STATE_TYPE_SOURCE_FROM_TOUR,
			STATE_TYPE_SOURCE_PREVIOUS_SPLITTED_TOUR,
			STATE_TYPE_SOURCE_CUSTOM											//
																				};
	private static final String[]	STATE_TEXT_TOUR_TYPE_SOURCE					= new String[] {
			Messages.Dialog_SplitTour_ComboText_TourTypeFromTour,
			Messages.Dialog_SplitTour_ComboText_TourTypePrevious,
			Messages.Dialog_SplitTour_ComboText_TourTypeCustom					//
																				};

	private TourData				_tourDataSource;
	private TourData				_tourDataTarget;

	private int						_timeSliceSplitIndex;

	private final IDialogSettings	_state										= TourbookPlugin.getDefault() //
																						.getDialogSettingsSection(
																								"DialogSplitTour"); //$NON-NLS-1$

	private long					_tourTypeIdFromSelectedTours				= TourDatabase.ENTITY_IS_NOT_SAVED;
	private long					_tourTypeIdPreviousJoinedTour				= TourDatabase.ENTITY_IS_NOT_SAVED;
	private long					_tourTypeIdCustom							= TourDatabase.ENTITY_IS_NOT_SAVED;

	/*
	 * UI controls
	 */
	private Combo					_cboSplitMethod;
	private Combo					_cboTourTypeSource;
	private Link					_linkTourType;
	private CLabel					_lblTourType;

	public DialogSplitTourOLD(final Shell parentShell, final TourData tourData, final int timeSliceSplitIndex) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		_tourDataSource = tourData;

		_timeSliceSplitIndex = timeSliceSplitIndex;
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_SplitTour_DlgArea_Title);
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_SplitTour_DlgArea_Title);
		setMessage(Messages.Dialog_SplitTour_DlgArea_Message);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);

		restoreState();

		return dlgContainer;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */

	private void createUI(final Composite parent) {

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.swtDefaults().numColumns(3).spacing(10, 8).applyTo(dlgContainer);
//		dlgContainer(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI20SplitMethod(dlgContainer);
			createUI30TourType(parent);
		}
	}

	/**
	 * split method
	 */
	private void createUI20SplitMethod(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_SplitTour_Label_SplitMethod);

		_cboSplitMethod = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_cboSplitMethod);

		// fill combo
		for (final String timeText : STATE_TEXT_SPLIT_METHOD) {
			_cboSplitMethod.add(timeText);
		}

	}

	/**
	 * tour type & tags
	 * 
	 * @param defaultSelectionAdapter
	 */
	private void createUI30TourType(final Composite parent) {

		/*
		 * tour type
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.Dialog_JoinTours_Label_TourType);

		_cboTourTypeSource = new Combo(parent, SWT.READ_ONLY);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(_cboTourTypeSource);
		_cboTourTypeSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourTypeSource();
			}
		});

		// fill combo
		for (final String tourTypeText : STATE_TEXT_TOUR_TYPE_SOURCE) {
			_cboTourTypeSource.add(tourTypeText);
		}

		// spacer
		new Label(parent, SWT.NONE);

		final Composite tourTypeContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false)//
				.indent(0, -8)
				.applyTo(tourTypeContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(tourTypeContainer);
		{
			_linkTourType = new Link(tourTypeContainer, SWT.NONE);
			_linkTourType.setText(Messages.Dialog_JoinTours_Link_TourType);
			_linkTourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(_linkTourType);
				}
			});

			_lblTourType = new CLabel(tourTypeContainer, SWT.NONE);
			GridDataFactory.swtDefaults().grab(true, false).applyTo(_lblTourType);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
//		return _state;
		return null;
	}

	private String getStateSplitMethod() {
		return Util.getStateFromCombo(_cboSplitMethod, ALL_STATES_SPLIT_METHOD, STATE_SPLIT_METHOD_REMOVE);
	}

	private String getStateTourTypeSource() {
		return Util.getStateFromCombo(_cboTourTypeSource, ALL_STATES_TOUR_TYPE, STATE_TYPE_SOURCE_FROM_TOUR);
	}

	@Override
	protected void okPressed() {

		saveState();

		super.okPressed();
	}

	private void onSelectTourTypeSource() {

		final String stateTourTypeSource = getStateTourTypeSource();

		long joinedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_TOUR)) {
			joinedTourTypeId = _tourTypeIdFromSelectedTours;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_PREVIOUS_SPLITTED_TOUR)) {
			joinedTourTypeId = _tourTypeIdPreviousJoinedTour;
		} else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
			joinedTourTypeId = _tourTypeIdCustom;
		}

		_tourDataTarget.setTourType(TourDatabase.getTourType(joinedTourTypeId));

		// update UI
		UI.updateUITourType(_tourDataTarget, _lblTourType, true);

//		enableControls();
	}

	private void restoreState() {

		// split method
		Util.selectStateInCombo(
				_state,
				STATE_SPLIT_METHOD,
				ALL_STATES_SPLIT_METHOD,
				STATE_SPLIT_METHOD_REMOVE,
				_cboSplitMethod);

	}

	private void saveState() {

		// split method
		_state.put(STATE_SPLIT_METHOD, getStateSplitMethod());

	}

}
