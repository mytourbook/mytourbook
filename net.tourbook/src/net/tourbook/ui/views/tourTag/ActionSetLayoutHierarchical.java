/**
 * 
 */
package net.tourbook.ui.views.tourTag;

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