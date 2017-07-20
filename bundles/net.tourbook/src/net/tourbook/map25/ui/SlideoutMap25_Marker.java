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
package net.tourbook.map25.ui;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25View;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutMap25_Marker extends ToolbarSlideout implements IColorSelectorListener {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionAdapter;
	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private FocusListener			_keepOpenListener;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeUI();
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};
	}

	private PixelConverter			_pc;

	private Action					_actionRestoreDefaults;

	private Map25View				_map25View;
	
	private Font				_boldFont;
	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}
	/*
	 * UI controls
	 */

	public SlideoutMap25_Marker(	final Control ownerControl,
									final ToolBar toolBar,
									final IDialogSettings state,
									final Map25View map25View) {

		super(ownerControl, toolBar);

		_map25View = map25View;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		setIsAnotherDialogOpened(isDialogOpened);
	}

	private void createActions() {

		/*
		 * Action: Restore default
		 */
		_actionRestoreDefaults = new Action() {
			@Override
			public void run() {
				resetToDefaults();
			}
		};

		_actionRestoreDefaults.setImageDescriptor(//
				TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		createActions();

		final Composite ui = createUI(parent);

		fillUI();
		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Header(container);
				createUI_20_Properties(container);

			}
		}

		return shellContainer;
	}

	private void createUI_10_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Label: Slideout title
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_Map25MarkerOptions_Label_Title);
				label.setFont(_boldFont);
				GridDataFactory.fillDefaults().applyTo(label);
			}
			{
				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.BEGINNING)
						.applyTo(toolbar);

				final ToolBarManager tbm = new ToolBarManager(toolbar);

//				tbm.add(_actionRestoreDefaults);

				tbm.update(true);
			}
		}
	}

	private void createUI_20_Properties(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				final Composite ttContainer = new Composite(container, SWT.NONE);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.applyTo(ttContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ttContainer);
				{
				}
			}

		}
	}


	private void enableControls() {

	}

	private void fillUI() {

	}

	private void onChangeUI() {


		// update chart with new settings
//		_map25View.updateUI_MarkerLayer();

		enableControls();

		// notify pref listener
		TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED, Math.random());
	}

	private void resetToDefaults() {

		/*
		 * Update UI with defaults from pref store
		 */

		onChangeUI();
	}

	private void restoreState() {

	}

}
