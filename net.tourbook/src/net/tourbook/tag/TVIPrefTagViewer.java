package net.tourbook.tag;

import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIPrefTagViewer extends TreeViewerItem {

	private TreeViewer	fTagViewer;

	public TVIPrefTagViewer(final TreeViewer tagViewer) {
		this.fTagViewer = tagViewer;
	}

	public TreeViewer getTagViewer() {
		return fTagViewer;
	}

}
