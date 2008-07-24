/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

final class ActionSetLayoutHierarchical extends Action {

	private TagView	fTagView;

	ActionSetLayoutHierarchical(final TagView tagView) {

		super(Messages.action_tagView_flat_hierarchical, AS_RADIO_BUTTON);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__layout_hierarchical));

		fTagView = tagView;
	}

	@Override
	public void run() {
		fTagView.setViewLayout(TagView.TAG_VIEW_LAYOUT_HIERARCHICAL);
	}
}
