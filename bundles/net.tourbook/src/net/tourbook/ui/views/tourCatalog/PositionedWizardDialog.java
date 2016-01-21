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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

/**
 * A wizard dialog where the positon and size is stored in dialog settings
 */
public class PositionedWizardDialog extends WizardDialog {

	private static final String	STATE_WIZARD_X		= "STATE_WIZARD_X";		//$NON-NLS-1$
	private static final String	STATE_WIZARD_Y		= "STATE_WIZARD_Y";		//$NON-NLS-1$
	private static final String	STATE_WIZARD_WIDTH	= "STATE_WIZARD_WIDTH";	//$NON-NLS-1$
	private static final String	STATE_WIZARD_HEIGHT	= "STATE_WIZARD_HEIGHT";	//$NON-NLS-1$

	private IDialogSettings		_state;

	private int					_defaultWidth;
	private int					_defaultHeight;

	/**
	 * @param parent
	 * @param wizard
	 * @param stateSection
	 * @param defaultWidth
	 * @param defaultHeight
	 */
	public PositionedWizardDialog(	final Shell parent,
									final Wizard wizard,
									final String stateSection,
									final int defaultWidth,
									final int defaultHeight) {

		super(parent, wizard);

		_state = TourbookPlugin.getState(stateSection);

		_defaultWidth = defaultWidth;
		_defaultHeight = defaultHeight;
	}

	@Override
	public boolean close() {

		saveBounds();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		// set icon for the window
		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__view_compare_wizard).createImage());

		super.configureShell(shell);

		shell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	protected Point getInitialLocation(final Point initialSize) {

		// get initial location as default
		final Point initialLocation = super.getInitialLocation(initialSize);

		final Point location = new Point(//
				Util.getStateInt(_state, STATE_WIZARD_X, initialLocation.x),
				Util.getStateInt(_state, STATE_WIZARD_Y, initialLocation.y));

		return location;
	}

	@Override
	protected Point getInitialSize() {

		final int height = Util.getStateInt(_state, STATE_WIZARD_HEIGHT, _defaultHeight);
		final int width = Util.getStateInt(_state, STATE_WIZARD_WIDTH, _defaultWidth);

		return new Point(width, height);

	}

	private void onDispose() {

	}

	private void saveBounds() {

		final Rectangle bounds = getShell().getBounds();

		_state.put(STATE_WIZARD_X, bounds.x);
		_state.put(STATE_WIZARD_Y, bounds.y);
		_state.put(STATE_WIZARD_WIDTH, bounds.width);
		_state.put(STATE_WIZARD_HEIGHT, bounds.height);
	}

}
