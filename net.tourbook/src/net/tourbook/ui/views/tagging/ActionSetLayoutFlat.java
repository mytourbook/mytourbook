/**
 * 
 */
package net.tourbook.ui.views.tagging;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

final class ActionSetLayoutFlat extends Action {

	private TagView	fTagView;

	ActionSetLayoutFlat(final TagView tagView) {
		
		super(Messages.action_tagView_flat_layout, AS_RADIO_BUTTON);
		
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__layout_flat));
		
		fTagView = tagView;
	}

	@Override
	public void run() {
		fTagView.setViewLayout(TagView.TAG_VIEW_LAYOUT_FLAT);
	}
}