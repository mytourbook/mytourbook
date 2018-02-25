/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;

/**
 * The class provides the ability to keep the width of the viewer when the parent is resized
 */
public class SashLeftFixedForm {

	/**
	 * Width of the sash slider
	 */
	private static final int	SASH_WIDTH			= 8;

	private static int			MINIMUM_PART_WIDTH	= 100;

	private boolean				_isInitialResize	= true;

	/*
	 * UI controls
	 */
	private Composite			_parent;

	private Control				_maximizedPart;

	private Control				_fixed;
	private Control				_sash;
	private Control				_flex;

	private Integer				_viewerWidth;
	private Integer				_sashWidth;
	private FormData			_sashLayoutData;

	public SashLeftFixedForm(	final Composite parent,
								final Composite leftPart,
								final Sash sash,
								final Composite rightPart,
								final IDialogSettings state,
								final String stateSashWidth) {

		this(parent, leftPart, sash, rightPart, 50);

		// restore width
		setViewerWidth(Util.getStateInt(state, stateSashWidth, 100));

		UI.addSashColorHandler(sash);

		// save sash width
		sash.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(final MouseEvent e) {
				state.put(stateSashWidth, leftPart.getSize().x);
			}
		});

	}

	public SashLeftFixedForm(final Composite parent, final Control viewer, final Control sash, final Control detail) {

		this(parent, viewer, sash, detail, 50);
	}

	/**
	 * @param parent
	 * @param fixedPart
	 * @param sash
	 * @param flexPart
	 * @param defaultSize
	 *            Relative size of the fixed (left) part in %.
	 */
	public SashLeftFixedForm(	final Composite parent,
								final Control fixedPart,
								final Control sash,
								final Control flexPart,
								final int defaultSize) {

		setViewerWidth_Internal(defaultSize);

		final PixelConverter pc = new PixelConverter(parent);
		MINIMUM_PART_WIDTH = pc.convertWidthInCharsToPixels(15);

		final ControlAdapter resizeListener = new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		};

		_parent = parent;

		_fixed = fixedPart;
		_sash = sash;
		_flex = flexPart;

		parent.setLayout(new FormLayout());

		final FormAttachment topAttachment = new FormAttachment(0, 0);
		final FormAttachment bottomAttachment = new FormAttachment(100, 0);

		/*
		 * Fixed (left) part
		 */
		final FormData fixedLayoutData = new FormData();
		fixedLayoutData.left = new FormAttachment(0, 0);
		fixedLayoutData.right = new FormAttachment(sash, 0);
		fixedLayoutData.top = topAttachment;
		fixedLayoutData.bottom = bottomAttachment;
		fixedPart.setLayoutData(fixedLayoutData);
		fixedPart.addControlListener(resizeListener);

		/*
		 * Sash
		 */
		_sashLayoutData = new FormData();
		_sashLayoutData.left = new FormAttachment(defaultSize, 0);
		_sashLayoutData.top = topAttachment;
		_sashLayoutData.bottom = bottomAttachment;
		_sashLayoutData.width = SASH_WIDTH;
		sash.setLayoutData(_sashLayoutData);
		sash.addListener(SWT.Selection, onSelectSash(parent, sash));

		/*
		 * Flex (right) part
		 */
		final FormData flexLayoutData = new FormData();
		flexLayoutData.left = new FormAttachment(sash, 0);
		flexLayoutData.right = new FormAttachment(100, 0);
		flexLayoutData.top = topAttachment;
		flexLayoutData.bottom = bottomAttachment;
		flexPart.setLayoutData(flexLayoutData);
		flexPart.addControlListener(resizeListener);
	}

	/**
	 * @return Returns maximized control or <code>null</code> when nothing is maximazied.
	 */
	public Control getMaximizedControl() {
		return _maximizedPart;
	}

	public int getViewerWidth() {
		return _sashWidth == null ? MINIMUM_PART_WIDTH : _sashWidth;
	}

	private void onResize() {

		if (_isInitialResize) {

			/*
			 * set the initial width for the viewer sash, this is a bit of hacking but it works
			 */

			// execute only the first time
			_isInitialResize = false;

			Integer viewerWidth = _viewerWidth;

			if (viewerWidth == null) {
				_viewerWidth = viewerWidth = _fixed.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			}

			_sashLayoutData.left = new FormAttachment(0, viewerWidth);

		} else {

			if (_maximizedPart != null) {

				if (_maximizedPart == _fixed) {

					_sashLayoutData.left = new FormAttachment(100, 0);

				} else if (_maximizedPart == _flex) {

					_sashLayoutData.left = new FormAttachment(0, -_sash.getSize().x);
				}

			} else {

				if (_viewerWidth == null) {

					_sashLayoutData.left = new FormAttachment(50, 0);

				} else {

					final Rectangle parentRect = _parent.getClientArea();

					// set the minimum width

					int viewerWidth = 0;

					if (_viewerWidth + MINIMUM_PART_WIDTH >= parentRect.width) {

						viewerWidth = Math.max(parentRect.width - MINIMUM_PART_WIDTH, MINIMUM_PART_WIDTH / 2);

					} else {
						viewerWidth = _viewerWidth;
					}

					_sashLayoutData.left = new FormAttachment(0, viewerWidth);
				}
			}

			_parent.layout();
		}
	}

	private Listener onSelectSash(final Composite parent, final Control sash) {

		return new Listener() {
			@Override
			public void handleEvent(final Event e) {

				final Rectangle sashRect = sash.getBounds();
				final Rectangle parentRect = parent.getClientArea();

				final int right = parentRect.width - sashRect.width - MINIMUM_PART_WIDTH;
				_sashWidth = Math.max(Math.min(e.x, right), MINIMUM_PART_WIDTH);

				if (_sashWidth != sashRect.x) {
					_sashLayoutData.left = new FormAttachment(0, _sashWidth);
					parent.layout();
				}

				_viewerWidth = _sashWidth;
			}
		};
	}

	/**
	 * sets the control which is maximized, set <code>null</code> to reset the maximized control
	 * 
	 * @param control
	 */
	public void setMaximizedControl(final Control control) {

		_maximizedPart = control;

		onResize();
	}

	/**
	 * @param viewerWidth
	 */
	public void setViewerWidth(final Integer viewerWidth) {

		setViewerWidth_Internal(viewerWidth);

		onResize();
	}

	private void setViewerWidth_Internal(final Integer viewerWidth) {

		_viewerWidth = viewerWidth == null ? null : Math.max(MINIMUM_PART_WIDTH, viewerWidth);
		_sashWidth = _viewerWidth;
	}
}
