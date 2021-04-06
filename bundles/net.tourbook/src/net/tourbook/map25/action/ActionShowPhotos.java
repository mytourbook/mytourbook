/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
 * Copyright (C) 2019 Thomas Theussing
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
package net.tourbook.map25.action;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.Action;

public class ActionShowPhotos extends Action {

   private Map25View   _map25View;

	public ActionShowPhotos(final Map25View map25View) {

		super(null, AS_CHECK_BOX);

		_map25View = map25View;

		setToolTipText(Messages.Map_Action_ShowPhotos_Tooltip);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ShowPhotos_InMap));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.ShowAllPhotos_InMap_Disabled));
	}

	@Override
	public void run() {

	   _map25View.actionShowPhotos(isChecked());

	}

}
