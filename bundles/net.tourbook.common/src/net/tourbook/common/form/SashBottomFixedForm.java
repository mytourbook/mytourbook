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
 * The class provides the ability to keep the height of the fixed part when the parent is resized.
 */
public class SashBottomFixedForm {

	private static int	MINIMUM_PART_HEIGHT	= 100;

	private Composite	_parent;
	private Control		_sash;

	private FormData	_fixedFD;
	private int			_fixedHeight;

	/**
	 * <code>
	 * 
	 * ============<br>
	 * | Flexpart |<br>
	 * ============<br>
	 * |   Sash   |<br>
	 * ============<br>
	 * | Fixpart  |<br>
	 * ============<br>
	 * 
	 * </code>
	 * 
	 * @param parent
	 * @param fixedPart
	 * @param sash
	 * @param flexPart
	 */
	public SashBottomFixedForm(	final Composite parent,
								final Control flexPart,
								final Control sash,
								final Control fixedPart) {

		final PixelConverter pc = new PixelConverter(parent);
		MINIMUM_PART_HEIGHT = pc.convertHeightInCharsToPixels(5);

		_parent = parent;
		_sash = sash;

		parent.setLayout(new FormLayout());

		final FormAttachment leftAttachment = new FormAttachment(0, 0);
		final FormAttachment rightAttachment = new FormAttachment(100, 0);

		// flex part
		final FormData flexFD = new FormData();
		flexFD.top = new FormAttachment(0, 0);
		flexFD.bottom = new FormAttachment(sash, 0);
		flexFD.left = leftAttachment;
		flexFD.right = rightAttachment;
		flexPart.setLayoutData(flexFD);

		// sash
		final FormData sashFD = new FormData();
		sashFD.bottom = new FormAttachment(fixedPart, 0);
		sashFD.left = leftAttachment;
		sashFD.right = rightAttachment;
		sash.setLayoutData(sashFD);

		// fixed part
		_fixedFD = new FormData();
//		_fixedFD.top = new FormAttachment(50, 0);
		_fixedFD.top = new FormAttachment(0, _fixedHeight);
		_fixedFD.bottom = new FormAttachment(100, 0);
		_fixedFD.left = leftAttachment;
		_fixedFD.right = rightAttachment;
		fixedPart.setLayoutData(_fixedFD);

		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});

		sash.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event e) {

				final Rectangle sashRect = _sash.getBounds();
				final Rectangle parentRect = _parent.getClientArea();

				final int visibleHeight = parentRect.height - sashRect.height;
				final int sashBottom = sashRect.y + sashRect.height;

				/*
				 * Ensure that the minimum height at the top and the bottom is preserved.
				 */
				final int requestedSashBottom = Math.min(
						visibleHeight - MINIMUM_PART_HEIGHT,
						Math.max(MINIMUM_PART_HEIGHT, e.y + sashRect.height));

				if (requestedSashBottom != sashBottom) {

					_fixedFD.top = new FormAttachment(0, requestedSashBottom);

					_parent.layout();

					_fixedHeight = visibleHeight - requestedSashBottom;
				}
			}
		});
	}

	/**
	 * @return Returns the height value of the fixed bottom part.
	 */
	public int getFixedHeight() {
		return _fixedHeight;
	}

	private void onResize() {

		final Rectangle sashRect = _sash.getBounds();
		final Rectangle parentRect = _parent.getClientArea();

		final int visibleHeight = parentRect.height - sashRect.height;
		final int sashBottom = sashRect.y + sashRect.height;

		// set default height
		if (_fixedHeight < MINIMUM_PART_HEIGHT) {
			_fixedHeight = parentRect.height / 2;
		}

		/*
		 * Ensure that the minimum height at the top and the bottom is preserved.
		 */
		final int requestedSashBottom = Math.max(MINIMUM_PART_HEIGHT, visibleHeight - _fixedHeight);

		if (requestedSashBottom != sashBottom) {

			_fixedFD.top = new FormAttachment(0, requestedSashBottom);
			_parent.layout();
		}
	}

	/**
	 * Set the height of the fixed bottom part.
	 * 
	 * @param fixedHeight
	 *            Height in pixel.
	 */
	public void setFixedHeight(final Integer fixedHeight) {

		_fixedHeight = fixedHeight;

		onResize();
	}

}
