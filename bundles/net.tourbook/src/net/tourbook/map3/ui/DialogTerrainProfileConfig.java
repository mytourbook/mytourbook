/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map3.ui;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.TerrainProfileLayer;

import java.awt.Dimension;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class DialogTerrainProfileConfig {

	private static final String			STATE_PREFIX							= "STATE_TERRAIN_PROFILE_";					//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_FOLLOW			= "STATE_TERRAIN_PROFILE_FOLLOW";			//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_DIMENSION			= "STATE_TERRAIN_PROFILE_DIMENSION";		//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_KEEP_PROPORTIONS	= "STATE_TERRAIN_PROFILE_KEEP_PROPORTIONS";	//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_ZERO_BASED		= "STATE_TERRAIN_PROFILE_ZERO_BASED";		//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_SHOW_EYE			= "STATE_TERRAIN_PROFILE_SHOW_EYE";			//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_PROFILE_LENGTH	= "STATE_TERRAIN_PROFILE_PROFILE_LENGTH";	//$NON-NLS-1$
	private static final String			STATE_TERRAIN_PROFILE_IS_TOOLTIP_MOVED	= "STATE_TERRAIN_PROFILE_IS_TOOLTIP_MOVED";	//$NON-NLS-1$

	private static final String[]		DIMENSION_LABEL							= {

			Messages.Terrain_Profile_Dimension_Small,
			Messages.Terrain_Profile_Dimension_Medium,
			Messages.Terrain_Profile_Dimension_Large };

	private static final Dimension[]	DIMENSION_VALUE							= {

			new Dimension(250, 100),
			new Dimension(450, 140),
			new Dimension(655, 240) };

	private static final String[]		FOLLOW_LABEL							= {

			Messages.Terrain_Follow_View,
			Messages.Terrain_Follow_Cursor,
			Messages.Terrain_Follow_Eye,
			Messages.Terrain_Follow_None,
			Messages.Terrain_Follow_Object };

	private IDialogSettings				_state;

	private WorldWindowGLCanvas			_wwcanvas;

	private TerrainProfileLayer			_profileLayer;

	private boolean						_isUpdateUI;

	private SelectionAdapter			_selectionListener;

	private Point						_initialTTLocation;
	private Object						_toolTipArea;

	private IToolProvider				_toolProvider;

	// UI controls
	private Shell						_shell;

	private Combo						_comboDimension;
	private Combo						_comboFollow;

	private Button						_chkShowEye;
	private Button						_chkKeepProportions;
	private Button						_chkZeroBased;

	private Label						_lblProfileLength;

	private Spinner						_spinnerProfileLength;

	private class TerrainToolProvider implements IToolProvider {

		@Override
		public void createToolUI(final Composite parent) {
			createUI(parent);
		}

		@Override
		public Point getInitialLocation() {
			return _initialTTLocation;
		}

		@Override
		public Object getToolTipArea() {
			return _toolTipArea;
		}

		@Override
		public String getToolTitle() {
			return Messages.Custom_Layer_TerrainProfile;
		}

		@Override
		public boolean isFlexTool() {
			return true;
		}

		@Override
		public void resetInitialLocation() {
			resetInitialLocation2();
		}

		@Override
		public void setToolTipArea(final Object toolTipArea) {
			_toolTipArea = toolTipArea;
		}
	};

	public DialogTerrainProfileConfig(	final WorldWindowGLCanvas wwcanvas,
										final TerrainProfileLayer profileLayer,
										final IDialogSettings state) {

		_wwcanvas = wwcanvas;
		_profileLayer = profileLayer;
		_state = state;

		_toolProvider = new TerrainToolProvider();

		_selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModify();
			}
		};
	}

	private void createUI(final Composite parent) {

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				saveState();
			}
		});

		_shell = parent.getShell();

		createUI_10(parent);

		updateUI();

		restoreState();
	}

	private void createUI_10(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * label: dimension
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(Messages.Terrain_Profile_Label_Dimension);

				/*
				 * combo: dimension
				 */
				_comboDimension = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboDimension);
				_comboDimension.setVisibleItemCount(10);
				_comboDimension.addSelectionListener(_selectionListener);
			}

			{
				/*
				 * label: follow
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(Messages.Terrain_Profile_Label_Follow);

				/*
				 * combo: follow
				 */
				_comboFollow = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboFollow);
				_comboFollow.setVisibleItemCount(10);
				_comboFollow.addSelectionListener(_selectionListener);
			}

			/*
			 * checkbox: show eye
			 */
			_chkShowEye = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowEye);
			_chkShowEye.setText(Messages.Terrain_Profile_Checkbox_ShowEye);

			/*
			 * checkbox: keep proportions
			 */
			_chkKeepProportions = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkKeepProportions);
			_chkKeepProportions.setText(Messages.Terrain_Profile_Checkbox_KeepProportions);

			/*
			 * checkbox: zero based
			 */
			_chkZeroBased = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkZeroBased);
			_chkZeroBased.setText(Messages.Terrain_Profile_Checkbox_ZeroBased);

			{
				/*
				 * label: profile length
				 */
				_lblProfileLength = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblProfileLength);
				_lblProfileLength.setText(Messages.Terrain_Profile_Label_ProfileLength);

				/*
				 * spinner: profile length
				 */
				_spinnerProfileLength = new Spinner(container, SWT.BORDER);
//				GridDataFactory.fillDefaults().applyTo(_spinnerProfileLength);
				_spinnerProfileLength.setMinimum(1);
				_spinnerProfileLength.setMaximum(30);
				_spinnerProfileLength.setIncrement(1);
				_spinnerProfileLength.setPageIncrement(5);
//				_spinnerProfileLength.setToolTipText(Messages.Terrain_Profile_Label_ProfileLength);
				_spinnerProfileLength.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModify();
					}

				});
				_spinnerProfileLength.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onModify();
					}
				});
			}
		}
	}

	private void enableControls() {

		final int followIndex = _comboFollow.getSelectionIndex();

		switch (followIndex) {

		default:
		case 0:
			_chkShowEye.setEnabled(false);
			_lblProfileLength.setEnabled(true);
			_spinnerProfileLength.setEnabled(true);
			break;

		case 1:
			_chkShowEye.setEnabled(false);
			_lblProfileLength.setEnabled(true);
			_spinnerProfileLength.setEnabled(true);
			break;

		case 2:
			_chkShowEye.setEnabled(true);
			_lblProfileLength.setEnabled(true);
			_spinnerProfileLength.setEnabled(true);
			break;

		case 3:
			_chkShowEye.setEnabled(false);
			_lblProfileLength.setEnabled(false);
			_spinnerProfileLength.setEnabled(false);
			break;

		case 4:
			_chkShowEye.setEnabled(true);
			_lblProfileLength.setEnabled(true);
			_spinnerProfileLength.setEnabled(true);
			break;
		}
	}

	public IToolProvider getToolProvider() {
		return _toolProvider;
	}

	private void onModify() {

		final Dimension graphDimension = DIMENSION_VALUE[_comboDimension.getSelectionIndex()];

		final int followIndex = _comboFollow.getSelectionIndex();

		String follow = null;
		switch (followIndex) {

		default:
		case 0:
			follow = TerrainProfileLayer.FOLLOW_VIEW;
			break;

		case 1:
			follow = TerrainProfileLayer.FOLLOW_CURSOR;
			break;

		case 2:
			follow = TerrainProfileLayer.FOLLOW_EYE;
			break;

		case 3:
			follow = TerrainProfileLayer.FOLLOW_NONE;
			break;

		case 4:
			follow = TerrainProfileLayer.FOLLOW_OBJECT;

//			final OrbitView view = (OrbitView) getWwd().getView();
//			tpl.setObjectPosition(getWwd().getView().getEyePosition());
//			tpl.setObjectHeading(view.getHeading());
			break;
		}

		_profileLayer.setFollow(follow);
		_profileLayer.setSize(graphDimension);
		_profileLayer.setKeepProportions(_chkKeepProportions.getSelection());
		_profileLayer.setZeroBased(_chkZeroBased.getSelection());
		_profileLayer.setShowEyePosition(_chkShowEye.getSelection());
		_profileLayer.setProfileLengthFactor(_spinnerProfileLength.getSelection() / 10.0);

		enableControls();

		_wwcanvas.redraw();
	}

	private void resetInitialLocation2() {

		_state.put(STATE_TERRAIN_PROFILE_IS_TOOLTIP_MOVED, false);

		UI.resetInitialLocation(_state, STATE_PREFIX);
	}

	private void restoreState() {

		_comboFollow.select(Util.getStateCombo(_state, STATE_TERRAIN_PROFILE_FOLLOW, 0, _comboFollow, 0));
		_comboDimension.select(Util.getStateCombo(_state, STATE_TERRAIN_PROFILE_DIMENSION, 0, _comboDimension, 0));

		_chkKeepProportions.setSelection(Util.getStateBoolean(_state, STATE_TERRAIN_PROFILE_KEEP_PROPORTIONS, false));
		_chkZeroBased.setSelection(Util.getStateBoolean(_state, STATE_TERRAIN_PROFILE_ZERO_BASED, true));
		_chkShowEye.setSelection(Util.getStateBoolean(_state, STATE_TERRAIN_PROFILE_SHOW_EYE, false));

		_spinnerProfileLength.setSelection(Util.getStateInt(_state, STATE_TERRAIN_PROFILE_PROFILE_LENGTH, 10));

		/*
		 * tooltip location
		 */

		// reset location
		_initialTTLocation = null;

		final boolean isTTMoved = Util.getStateBoolean(_state, STATE_TERRAIN_PROFILE_IS_TOOLTIP_MOVED, false);
		if (isTTMoved) {

			final SlideoutMap3Layer map3LayerDialog = Map3Manager.getMap3LayerSlideout();
			if (map3LayerDialog != null) {

				if (_shell != null) {

					_initialTTLocation = UI.getInitialLocation(_state,
							STATE_PREFIX,
							_comboDimension.getShell(),
							_shell);
				}
			}
		}

		enableControls();
	}

	private void saveState() {

		final Shell ttShell = _comboDimension.getShell();

		_state.put(STATE_TERRAIN_PROFILE_FOLLOW, _comboFollow.getSelectionIndex());
		_state.put(STATE_TERRAIN_PROFILE_DIMENSION, _comboDimension.getSelectionIndex());

		_state.put(STATE_TERRAIN_PROFILE_KEEP_PROPORTIONS, _chkKeepProportions.getSelection());
		_state.put(STATE_TERRAIN_PROFILE_ZERO_BASED, _chkZeroBased.getSelection());
		_state.put(STATE_TERRAIN_PROFILE_SHOW_EYE, _chkShowEye.getSelection());

		_state.put(STATE_TERRAIN_PROFILE_PROFILE_LENGTH, _spinnerProfileLength.getSelection());

		/*
		 * save tooltip location
		 */
		final Object shellData = ttShell.getData(ToolTip3.SHELL_DATA_TOOL);
		if (shellData instanceof ToolTip3Tool) {

			final ToolTip3Tool ttTool = (ToolTip3Tool) shellData;

			// save move state
			final boolean isMoved = ttTool.isMoved();
			_state.put(STATE_TERRAIN_PROFILE_IS_TOOLTIP_MOVED, isMoved);

			if (isMoved) {

				// save dialog position ONLY when moved, when not moved the tooltip is displayed at the default location

				final SlideoutMap3Layer map3LayerDialog = Map3Manager.getMap3LayerSlideout();
				if (map3LayerDialog != null) {

					if (_shell != null) {
						UI.saveDialogBounds(_state, STATE_PREFIX, ttShell, _shell);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("\nTerrainProfileConfiguration\n   getToolTipArea()=") //$NON-NLS-1$
				.append(_toolProvider.getToolTipArea())
				.append(", \n   getToolTitle()=") //$NON-NLS-1$
				.append(_toolProvider.getToolTitle())
				.append(", \n   isToolMovable()=") //$NON-NLS-1$
				.append(_toolProvider.isFlexTool())
				.append("\n"); //$NON-NLS-1$
		return builder.toString();
	}

	private void updateUI() {

		for (final String value : DIMENSION_LABEL) {
			_comboDimension.add(value);
		}

		for (final String value : FOLLOW_LABEL) {
			_comboFollow.add(value);
		}
	}

}
