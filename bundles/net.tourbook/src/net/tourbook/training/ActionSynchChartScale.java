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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;

public class ActionSynchChartScale extends Action {

	private TrainingView	_trainingView;

	public ActionSynchChartScale(final TrainingView trainingView) {

		super(UI.EMPTY_STRING, AS_CHECK_BOX);

		_trainingView = trainingView;

		setToolTipText(Messages.Training_View_Action_SynchChartScale);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_statistics));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_statistics_Disabled));
	}

	@Override
	public void run() {
		_trainingView.actionSynchChartScale();
	}
}
