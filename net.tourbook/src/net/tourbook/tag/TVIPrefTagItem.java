package net.tourbook.tag;

import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIPrefTagItem extends TreeViewerItem {

	private TreeViewer	fTagViewer;

	public TVIPrefTagItem(final TreeViewer tagViewer) {
		this.fTagViewer = tagViewer;
	}

	public TreeViewer getTagViewer() {
		return fTagViewer;
	}

}
