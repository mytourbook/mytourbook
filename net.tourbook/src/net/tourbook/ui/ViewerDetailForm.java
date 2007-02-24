/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The ViewerDetailForm class provides the ability to keep the viewer width when
 * the parent is resized
 */
public class ViewerDetailForm {

	private static final int	MINIMUM_WIDTH	= 100;

	private Composite			fParent;

	private Control				fMaximizedControl;

	private Control				fViewer;
	private Control				fSash;
	private Control				fDetail;

	private Integer				fViewerWidth;
	private FormData			fSashData;

	private boolean				isInitialResize;

	public ViewerDetailForm(final Composite parent, final Control viewer, final Control sash,
			final Control detail) {

		fParent = parent;
		fViewer = viewer;
		fDetail = detail;
		fSash = sash;

		parent.setLayout(new FormLayout());

		FormAttachment topAttachment = new FormAttachment(0, 0);
		FormAttachment bottomAttachment = new FormAttachment(100, 0);

		final FormData fdViewer = new FormData();
		fdViewer.left = new FormAttachment(0, 0);
		fdViewer.right = new FormAttachment(sash, 0);
		fdViewer.top = topAttachment;
		fdViewer.bottom = bottomAttachment;
		viewer.setLayoutData(fdViewer);

		final int percent = 50;

		fSashData = new FormData();
		fSashData.left = new FormAttachment(percent, 0);
		fSashData.top = topAttachment;
		fSashData.bottom = bottomAttachment;
		sash.setLayoutData(fSashData);

		final FormData fdDetail = new FormData();
		fdDetail.left = new FormAttachment(sash, 0);
		fdDetail.right = new FormAttachment(100, 0);
		fdDetail.top = topAttachment;
		fdDetail.bottom = bottomAttachment;
		detail.setLayoutData(fdDetail);

		viewer.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				onResize();
			}
		});

		detail.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				onResize();
			}
		});

		sash.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {

				Rectangle sashRect = sash.getBounds();
				Rectangle parentRect = parent.getClientArea();

				int right = parentRect.width - sashRect.width - MINIMUM_WIDTH;
				e.x = Math.max(Math.min(e.x, right), MINIMUM_WIDTH);

				if (e.x != sashRect.x) {
					fSashData.left = new FormAttachment(0, e.x);
					parent.layout();
				}

				fViewerWidth = e.x;
			}
		});
	}

	public void setViewerWidth(Integer viewerWidth) {
		fViewerWidth = viewerWidth == null ? null : Math.max(MINIMUM_WIDTH, viewerWidth);
	}

	/**
	 * sets the control which is maximized, set <code>null</code> to reset the
	 * maximized control
	 * 
	 * @param control
	 */
	public void setMaximizedControl(Control control) {
		fMaximizedControl = control;
		onResize();
	}

	private void onResize() {

		if (isInitialResize == false) {

			/*
			 * set the initial width for the viewer sash, this is a bit of
			 * hacking but it works
			 */

			// execute only the first time
			isInitialResize = true;

			Integer viewerWidth = fViewerWidth;

			if (viewerWidth == null) {
				viewerWidth = fViewer.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			}

			fSashData.left = new FormAttachment(0, viewerWidth);
			
			fParent.layout();
			
//			System.out.println("isInit==false: "+viewerWidth);

		} else {

			if (fMaximizedControl != null) {

				if (fMaximizedControl == fViewer) {

					fSashData.left = new FormAttachment(100, 0);
					fParent.layout();

				} else if (fMaximizedControl == fDetail) {
					fSashData.left = new FormAttachment(0, -fSash.getSize().x);
					fParent.layout();
				}

			} else if (fMaximizedControl == null) {

				if (fViewerWidth == null) {
					fSashData.left = new FormAttachment(50, 0);
				} else {

					Rectangle parentRect = fParent.getClientArea();

					// set the minimum width
					int viewerWidth = fViewerWidth == null
							? MINIMUM_WIDTH
							: fViewerWidth >= parentRect.width ? Math.max(fViewerWidth
									- MINIMUM_WIDTH, 10) : fViewerWidth;

					fSashData.left = new FormAttachment(0, viewerWidth);
				}
				fParent.layout();
			}

//			System.out.println("isInit==true: "+fSashData.left);
		}
	}
}
