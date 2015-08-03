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
package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor settings.
 */
public class SlideoutViewSettings extends AnimatedToolTipShell implements IColorSelectorListener {

	private final IDialogSettings	_state				= TourbookPlugin.getState(TourDataEditorView.ID);

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private TourDataEditorView		_tourEditorView;

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Spinner					_spinnerLatLonDigits;

	private Label					_lblLatLonDigits;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutViewSettings(final Control ownerControl,
								final ToolBar toolBar,
								final IDialogSettings state,
								final TourDataEditorView tourMarkerAllView) {

		super(ownerControl);

		_tourEditorView = tourMarkerAllView;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		final boolean isCanClose = _isAnotherDialogOpened == false;

		return isCanClose;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected boolean closeShellAfterHidden() {

		/*
		 * Close the tooltip that the state is saved.
		 */

		return true;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		initUI(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_10_LatLonDigits(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_LatLonDigits(final Composite parent) {

		final SelectionListener _selectionAdapterLatLonDigits = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelect_LatLonDigits();
			}
		};

		/*
		 * Lat/lon digits
		 */
		{
			// label: lat/lon digits
			_lblLatLonDigits = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblLatLonDigits);
			_lblLatLonDigits.setText(Messages.Slideout_TourEditor_Label_LatLonDigits);
			_lblLatLonDigits.setToolTipText(Messages.Slideout_TourEditor_Label_LatLonDigits_Tooltip);

			// spinner
			_spinnerLatLonDigits = new Spinner(parent, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.END, SWT.CENTER)
					.applyTo(_spinnerLatLonDigits);
			_spinnerLatLonDigits.setMinimum(0);
			_spinnerLatLonDigits.setMaximum(20);
			_spinnerLatLonDigits.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onSelect_LatLonDigits();
				}
			});
			_spinnerLatLonDigits.addSelectionListener(_selectionAdapterLatLonDigits);
		}
	}

	private void enableControls() {

	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int itemHeight = _toolTipItemBounds.height;

		final int devX = _toolTipItemBounds.x - tipSize.x / 2;
		final int devY = _toolTipItemBounds.y + itemHeight;

		return new Point(devX, devY);
	}

	private void initUI(final Composite parent) {

//		_pc = new PixelConverter(parent);
//		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onSelect_LatLonDigits() {

		final int latLonDigits = _spinnerLatLonDigits.getSelection();

		_state.put(TourDataEditorView.STATE_LAT_LON_DIGITS, latLonDigits);

		_tourEditorView.updateUI_LatLonDigits(latLonDigits);

		enableControls();
	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {

			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	private void restoreState() {

		/*
		 * Lat/lon digits
		 */
		_spinnerLatLonDigits.setSelection(Util.getStateInt(
				_state,
				TourDataEditorView.STATE_LAT_LON_DIGITS,
				TourDataEditorView.DEFAULT_LAT_LON_DIGITS));
	}

}
