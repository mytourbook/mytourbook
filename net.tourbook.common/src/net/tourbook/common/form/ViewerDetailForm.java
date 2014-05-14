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
package net.tourbook.common.form;

import org.eclipse.jface.layout.PixelConverter;
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
 * The class provides the ability to keep the width of the viewer when the parent is resized
 */
public class ViewerDetailForm {

	private static int	MINIMUM_WIDTH	= 100;

	private Composite	_parent;

	private Control		_maximizedControl;

	private Control		_viewer;
	private Control		_sash;
	private Control		_detail;

	private Integer		_viewerWidth;
	private Integer		_sashWidth;
	private FormData	_sashLayoutData;

	private boolean		_isInitialResize;

	public ViewerDetailForm(final Composite parent, final Control viewer, final Control sash, final Control detail) {
		this(parent, viewer, sash, detail, 50);
	}

	/**
	 * @param parent
	 * @param viewer
	 * @param sash
	 * @param detail
	 * @param leftWidth
	 *            Relative width of the left part in %.
	 */
	public ViewerDetailForm(final Composite parent,
							final Control viewer,
							final Control sash,
							final Control detail,
							final int leftWidth) {

		final PixelConverter pc = new PixelConverter(parent);
		MINIMUM_WIDTH = pc.convertWidthInCharsToPixels(15);

		_parent = parent;
		_viewer = viewer;
		_detail = detail;
		_sash = sash;

		parent.setLayout(new FormLayout());

		final FormAttachment topAttachment = new FormAttachment(0, 0);
		final FormAttachment bottomAttachment = new FormAttachment(100, 0);

		final FormData viewerLayoutData = new FormData();
		viewerLayoutData.left = new FormAttachment(0, 0);
		viewerLayoutData.right = new FormAttachment(sash, 0);
		viewerLayoutData.top = topAttachment;
		viewerLayoutData.bottom = bottomAttachment;
		viewer.setLayoutData(viewerLayoutData);

		_sashLayoutData = new FormData();
		_sashLayoutData.left = new FormAttachment(leftWidth, 0);
		_sashLayoutData.top = topAttachment;
		_sashLayoutData.bottom = bottomAttachment;
		sash.setLayoutData(_sashLayoutData);

		final FormData detailLayoutData = new FormData();
		detailLayoutData.left = new FormAttachment(sash, 0);
		detailLayoutData.right = new FormAttachment(100, 0);
		detailLayoutData.top = topAttachment;
		detailLayoutData.bottom = bottomAttachment;
		detail.setLayoutData(detailLayoutData);

		viewer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});

		detail.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});

		sash.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event e) {

				final Rectangle sashRect = sash.getBounds();
				final Rectangle parentRect = parent.getClientArea();

				final int right = parentRect.width - sashRect.width - MINIMUM_WIDTH;
				_sashWidth = Math.max(Math.min(e.x, right), MINIMUM_WIDTH);

				if (_sashWidth != sashRect.x) {
					_sashLayoutData.left = new FormAttachment(0, _sashWidth);
					parent.layout();
				}

				_viewerWidth = _sashWidth;
			}
		});
	}

	/**
	 * @return Returns maximized control or <code>null</code> when nothing is maximazied.
	 */
	public Control getMaximizedControl() {
		return _maximizedControl;
	}

	public int getViewerWidth() {
		return _sashWidth == null ? MINIMUM_WIDTH : _sashWidth;
	}

	private void onResize() {

		if (_isInitialResize == false) {

			/*
			 * set the initial width for the viewer sash, this is a bit of hacking but it works
			 */

			// execute only the first time
			_isInitialResize = true;

			Integer viewerWidth = _viewerWidth;

			if (viewerWidth == null) {
				_viewerWidth = viewerWidth = _viewer.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			}

			_sashLayoutData.left = new FormAttachment(0, viewerWidth);

			_parent.layout();

			// System.out.println("isInit==false: "+viewerWidth);

		} else {

			if (_maximizedControl != null) {

				if (_maximizedControl == _viewer) {

					_sashLayoutData.left = new FormAttachment(100, 0);
					_parent.layout();

				} else if (_maximizedControl == _detail) {
					_sashLayoutData.left = new FormAttachment(0, -_sash.getSize().x);
					_parent.layout();
				}

			} else {

				if (_viewerWidth == null) {
					_sashLayoutData.left = new FormAttachment(50, 0);
				} else {

					final Rectangle parentRect = _parent.getClientArea();

					// set the minimum width

					int viewerWidth = 0;

					if (_viewerWidth + MINIMUM_WIDTH >= parentRect.width) {

						viewerWidth = Math.max(parentRect.width - MINIMUM_WIDTH, MINIMUM_WIDTH / 2);

					} else {
						viewerWidth = _viewerWidth;
					}

					_sashLayoutData.left = new FormAttachment(0, viewerWidth);
				}
				_parent.layout();
			}

			// System.out.println("isInit==true: "+fSashData.left);
		}
	}

	/**
	 * sets the control which is maximized, set <code>null</code> to reset the maximized control
	 * 
	 * @param control
	 */
	public void setMaximizedControl(final Control control) {
		_maximizedControl = control;
		onResize();
	}

	/**
	 * @param viewerWidth
	 */
	public void setViewerWidth(final Integer viewerWidth) {
		_viewerWidth = viewerWidth == null ? null : Math.max(MINIMUM_WIDTH, viewerWidth);
		_sashWidth = _viewerWidth;
		onResize();
	}
}
