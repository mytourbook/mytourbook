/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

public class ActionHandlerPhotoFilter extends AbstractHandler {

	static final String				COMMAND_ID	= "command.net.tourbook.PhotoFilter";				//$NON-NLS-1$

	private static IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isAppPhotoFilterInitialized;

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final boolean isPhotoFilterActive = !HandlerUtil.toggleCommandState(event.getCommand());

		TourbookPlugin.setActivePhotoFilter(isPhotoFilterActive);

		// fire event that photo filter has changed
		_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());

		return null;
	}

	@Override
	public void setEnabled(final Object evaluationContext) {

		super.setEnabled(evaluationContext);

		if (_isAppPhotoFilterInitialized == false) {

			_isAppPhotoFilterInitialized = true;

			/*
			 * initialize app photo filter, this is a hack because the whole app startup should be
			 * sometimes be streamlined, it's more and more confusing
			 */
			final Command command = ((ICommandService) PlatformUI//
					.getWorkbench()
					.getService(ICommandService.class))//
					.getCommand(ActionHandlerPhotoFilter.COMMAND_ID);

			final State state = command.getState(RegistryToggleState.STATE_ID);
			final Boolean isPhotoFilterActive = (Boolean) state.getValue();

			TourbookPlugin.setActivePhotoFilter(isPhotoFilterActive);
		}
	}

}
