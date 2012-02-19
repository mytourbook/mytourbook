/*******************************************************************************
 * Copyright (c) 2006-2009 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *******************************************************************************/

package net.tourbook.photo.gallery;

import org.eclipse.nebula.animation.AnimationRunner;
import org.eclipse.nebula.animation.effects.AbstractEffect;
import org.eclipse.nebula.animation.movement.IMovement;

/**
 * <p>
 * Animation used internally on collapse / expand events. Should not be used directly.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @see AnimationRunner#runEffect(org.eclipse.nebula.animation.effects.IEffect)
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 */
public class GalleryGroupResizeEffect extends AbstractEffect {

	int				src, dest, diff;
	GalleryMTItem	item	= null;

	/**
	 * Set up a new resize effect on a gallery item.
	 * 
	 * @param item
	 * @param src
	 * @param dest
	 * @param lengthMilli
	 * @param movement
	 * @param onStop
	 * @param onCancel
	 */
	public GalleryGroupResizeEffect(final GalleryMTItem item,
									final int src,
									final int dest,
									final long lengthMilli,
									final IMovement movement,
									final Runnable onStop,
									final Runnable onCancel) {
		super(lengthMilli, movement, onStop, onCancel);

		this.src = src;
		this.dest = dest;
		this.diff = dest - src;

		easingFunction.init(0, 1, (int) lengthMilli);

		this.item = item;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sharemedia.ui.sat.AbstractEffect#applyEffect(long)
	 */
	@Override
	public void applyEffect(final long currentTime) {
		if (!item.isDisposed()) {
			final double value = src + diff * easingFunction.getValue((int) currentTime);

			item.setData(DefaultGalleryGroupRenderer.DATA_ANIMATION, new Double(value));

			item.getGallery().updateStructuralValues(null, false);
			item.getGallery().updateScrollBarsProperties();
			item.getGallery().redraw();

		}
	}
}
