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
	private static final int	SASH_SLIDER_WIDTH	= 8;

	private static int			MINIMUM_PART_WIDTH	= 100;

	private boolean				_isFirstResize		= true;

	/**
	 * Width in pixel for the left fixed part
	 */
	private int					_pxFixedWidth		= -1;

	/**
	 * Relative size of the left fixed part in %
	 */
	private int					_relFixedDefaultSize;

	private FormData			_sashLayoutData;

	private ControlAdapter		_resizeListener;

	/*
	 * UI controls
	 */
	private Composite			_parent;

	private Control				_maximizedPart;

	private Control				_fixedLeftPart;
	private Control				_sash;
	private Control				_flexRightPart;

	/**
	 * @param parent
	 * @param leftFixedPart
	 *            Left fixed part
	 * @param sash
	 * @param rightFlexPart
	 *            Right flex part
	 * @param state
	 *            State where the left fixed width is saved
	 * @param stateSashWidth
	 *            State key for the left fixed width
	 * @param relDefaultWidth
	 *            Relative width of the left fixed part in %
	 */
	public SashLeftFixedForm(	final Composite parent,
								final Composite leftFixedPart,
								final Sash sash,
								final Composite rightFlexPart,
								final IDialogSettings state,
								final String stateSashWidth,
								final int relDefaultWidth) {

		this(parent, leftFixedPart, sash, rightFlexPart, relDefaultWidth);

		// restore width
		final int restoredWidth = Util.getStateInt(state, stateSashWidth, -1);
		if (restoredWidth != -1) {
			setViewerWidth(restoredWidth);
		}

		UI.addSashColorHandler(sash);

		// save sash width
		sash.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(final MouseEvent e) {
				state.put(stateSashWidth, leftFixedPart.getSize().x);
			}
		});

	}

	public SashLeftFixedForm(final Composite parent, final Control viewer, final Control sash, final Control detail) {

		this(parent, viewer, sash, detail, 50);
	}

	/**
	 * @param parent
	 * @param fixedLeftPart
	 * @param sash
	 * @param flexRightPart
	 * @param relFixedDefaultSize
	 *            The initial size of the fixed (left) part is relative in %.
	 */
	public SashLeftFixedForm(	final Composite parent,
								final Control fixedLeftPart,
								final Control sash,
								final Control flexRightPart,
								final int relFixedDefaultSize) {

		initUI(parent);

		_parent = parent;

		_fixedLeftPart = fixedLeftPart;
		_sash = sash;
		_flexRightPart = flexRightPart;

		_relFixedDefaultSize = relFixedDefaultSize;

		/*
		 * Setup layout
		 */
		parent.setLayout(new FormLayout());

		final FormAttachment topAttachment = new FormAttachment(0, 0);
		final FormAttachment bottomAttachment = new FormAttachment(100, 0);

		/*
		 * Left fixed part
		 */
		final FormData fixedLayoutData = new FormData();
		fixedLayoutData.left = new FormAttachment(0, 0);
		fixedLayoutData.right = new FormAttachment(sash, 0);
		fixedLayoutData.top = topAttachment;
		fixedLayoutData.bottom = bottomAttachment;
		_fixedLeftPart.setLayoutData(fixedLayoutData);
		_fixedLeftPart.addControlListener(_resizeListener);

		/*
		 * Sash
		 */
		_sashLayoutData = new FormData();
		_sashLayoutData.left = null; // this is set later - new FormAttachment(relFixedDefaultSize, 0);
		_sashLayoutData.top = topAttachment;
		_sashLayoutData.bottom = bottomAttachment;
		_sashLayoutData.width = SASH_SLIDER_WIDTH;
		_sash.setLayoutData(_sashLayoutData);
		_sash.addListener(SWT.Selection, onSelectSash(parent, sash));

		/*
		 * Right flex part
		 */
		final FormData flexLayoutData = new FormData();
		flexLayoutData.left = new FormAttachment(sash, 0);
		flexLayoutData.right = new FormAttachment(100, 0);
		flexLayoutData.top = topAttachment;
		flexLayoutData.bottom = bottomAttachment;
		_flexRightPart.setLayoutData(flexLayoutData);
		_flexRightPart.addControlListener(_resizeListener);
	}

	/**
	 * @return Returns maximized control or <code>null</code> when nothing is maximazied.
	 */
	public Control getMaximizedControl() {
		return _maximizedPart;
	}

	public int getViewerWidth() {

		return _pxFixedWidth < 0 ? MINIMUM_PART_WIDTH : _pxFixedWidth;
	}

	private void initUI(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		MINIMUM_PART_WIDTH = pc.convertWidthInCharsToPixels(15);

		_resizeListener = new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		};
	}

	private void onResize() {

		final Rectangle parentRect = _parent.getClientArea();

		if (_isFirstResize) {

			final int parentWidth = parentRect.width;

			// ignore resize when shell is not yet setup
			if (parentWidth == 0) {
				return;
			}

			// run only the first time
			_isFirstResize = false;

			/*
			 * Set the initial width for the viewer sash, this must be done lately because the
			 * client area size is not set before
			 */
			int pxFixedWidth;

			if (_pxFixedWidth < 0) {

				// get absolute width of the fixed part from relative width
				pxFixedWidth = parentWidth * _relFixedDefaultSize / 100;

			} else {

				pxFixedWidth = _pxFixedWidth;
			}

			// adjust width when too large
			if (pxFixedWidth + MINIMUM_PART_WIDTH >= parentWidth) {
				pxFixedWidth = Math.max(parentWidth - MINIMUM_PART_WIDTH, MINIMUM_PART_WIDTH / 2);
			}

			_pxFixedWidth = pxFixedWidth;
			_sashLayoutData.left = new FormAttachment(0, pxFixedWidth);

		} else {

			if (_maximizedPart != null) {

				if (_maximizedPart == _fixedLeftPart) {

					_sashLayoutData.left = new FormAttachment(100, 0);

				} else if (_maximizedPart == _flexRightPart) {

					_sashLayoutData.left = new FormAttachment(0, -_sash.getSize().x);
				}

			} else {

				if (_pxFixedWidth < 0) {

					_sashLayoutData.left = new FormAttachment(50, 0);

				} else {

					// set the minimum width

					int pxViewerWidth = 0;

					if (_pxFixedWidth + MINIMUM_PART_WIDTH >= parentRect.width) {

						pxViewerWidth = Math.max(parentRect.width - MINIMUM_PART_WIDTH, MINIMUM_PART_WIDTH / 2);

					} else {
						pxViewerWidth = _pxFixedWidth;
					}

					_sashLayoutData.left = new FormAttachment(0, pxViewerWidth);
				}
			}

			_parent.layout();
		}
	}

	/**
	 * Setup listener when the sash slider is moved
	 * 
	 * @param parent
	 * @param sash
	 * @return
	 */
	private Listener onSelectSash(final Composite parent, final Control sash) {

		return new Listener() {
			@Override
			public void handleEvent(final Event e) {

				final Rectangle sashRect = sash.getBounds();
				final Rectangle parentRect = parent.getClientArea();

				final int draggedWidth = e.x;
				final int minWidth = parentRect.width - sashRect.width - MINIMUM_PART_WIDTH;

				// set new fixed width
				final int minFixedWidth = Math.min(draggedWidth, minWidth);
				final int newFixedWidth = Math.max(minFixedWidth, MINIMUM_PART_WIDTH);

				final int oldFixedWidth = sashRect.x;

				if (newFixedWidth != oldFixedWidth) {

					_pxFixedWidth = newFixedWidth;

					_sashLayoutData.left = new FormAttachment(0, newFixedWidth);

					parent.layout();
				}
			}
		};
	}

	/**
	 * Set the control which is maximized, set <code>null</code> to reset the maximized control
	 * 
	 * @param control
	 */
	public void setMaximizedControl(final Control control) {

		_maximizedPart = control;

		onResize();
	}

	/**
	 * Set width for the left fix part, this is mainly used to restore the width
	 * 
	 * @param pxViewerWidth
	 */
	public void setViewerWidth(final int pxViewerWidth) {

		_pxFixedWidth = Math.max(MINIMUM_PART_WIDTH, pxViewerWidth);

		onResize();
	}
}
