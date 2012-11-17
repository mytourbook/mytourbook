/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal.gallery.MT20;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoGalleryProvider;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.IPhotoProvider;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.PhotoWrapper;
import net.tourbook.photo.internal.Messages;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

public class FullScreenPhotoGallery implements IPhotoGalleryProvider {

	private static final int		GALLERY_DEFAULT_HEIGHT					= 150;
	private static final int		GALLERY_MIN_IMAGE_SIZE					= 40;
	private static final int		GALLERY_MAX_IMAGE_SIZE					= 1000;

	private static final String		STATE_FULL_SCREEN_PHOTO_GALLERY_HEIGHT	= "STATE_FULL_SCREEN_PHOTO_GALLERY_HEIGHT"; //$NON-NLS-1$

	private ControlAnimation		_photoGalleryAnimation;

	private IDialogSettings			_state;

	private AllControlsListener		_allControlsListener;

	private GalleryMT20				_sourceGallery;
	private PhotoGallery			_photoGallery;
	private FullScreenImageViewer	_fullScreenImageViewer;

	private int						_displayedPhotosHash;
	private int						_displayedItemIndex;

	private ToolBarManager			_galleryToolbarManager;

	/*
	 * UI controls
	 */
	private Shell					_fullScreenShell;
	private Shell					_galleryShell;

	private ToolBar					_galleryToolbarControl;
	private Spinner					_spinnerResizeImage;

	private Control					_galleryContainer;
	private Composite				_containerFooter;

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class AllControlsListener implements Listener {
		public void handleEvent(final Event event) {
			onAllControlsEvent(event);
		}

	}

	public class ControlAnimation implements Runnable {

		/**
		 * how long each tick is when fading in/out (in ms)
		 */
//		private static final int	FADE_TIME_INTERVAL		= UI.IS_OSX ? 10 : 10;
		private final int			FADE_TIME_INTERVAL	= UI.IS_OSX ? 10 : 10;

		/**
		 * Number of steps when fading in
		 */
		private static final int	FADE_IN_STEPS		= 20;

		/**
		 * Number of steps when fading out
		 */
		private static final int	FADE_OUT_STEPS		= 10;

		private static final int	ALPHA_OPAQUE		= 0xff;

		private Display				_display;
		private Shell				_shell;

		private boolean				_isFadeIn;
		private boolean				_isFadeOut;

		private int					_fadeAlpha;

		public ControlAnimation(final Shell shell, final Control control) {

			_shell = shell;
			_display = shell.getDisplay();
		}

		public void fadeIn() {

			if (_isFadeIn) {
				// fade in is already started
				return;
			}

			if (_isFadeOut) {

				// stop fade out and start with current alpha

				_fadeAlpha = _shell.getAlpha();
			} else {
				_fadeAlpha = 0;
			}

			_isFadeIn = true;
			_isFadeOut = false;

			run();
		}

		public void fadeOut() {

			if (_isFadeOut) {
				// fade out is already started
				return;
			}

			_fadeAlpha = _shell.getAlpha();

			_isFadeIn = false;
			_isFadeOut = true;

			run();
		}

		@Override
		public void run() {

			if (_shell == null || _shell.isDisposed()) {
				return;
			}

			final boolean isVisible = _shell.isVisible();

			if (_isFadeIn) {

				final int fadeInStep = ALPHA_OPAQUE / FADE_IN_STEPS;

				int newAlpha = _fadeAlpha + fadeInStep;
				if (newAlpha > ALPHA_OPAQUE) {
					newAlpha = ALPHA_OPAQUE;
				}

				// set alpha before shell is displayed
				_shell.setAlpha(newAlpha);

				if (isVisible == false) {
					_shell.setVisible(true);
					_shell.setActive();
				}

				final int currentAlpha = _shell.getAlpha();
				if (currentAlpha != newAlpha) {

					// platform do not support alpha (e.g. Ubuntu 12.04 in my test system)

					_shell.setAlpha(ALPHA_OPAQUE);
					_isFadeIn = false;

				} else {

					_fadeAlpha = currentAlpha;

					if (currentAlpha == ALPHA_OPAQUE) {

						// reached end of fade in
						_isFadeIn = false;

					} else {

						// start timer for a neww fade in
						_display.timerExec(FADE_TIME_INTERVAL, this);
					}
				}

			} else if (_isFadeOut) {

				if (isVisible == false) {
					_isFadeOut = false;
					return;
				}

				final int fadeOutStep = ALPHA_OPAQUE / FADE_OUT_STEPS;

				int newAlpha = _fadeAlpha - fadeOutStep;
				if (newAlpha < 0) {
					newAlpha = 0;
				}

				_shell.setAlpha(newAlpha);

				final int currentAlpha = _shell.getAlpha();
				if (currentAlpha != newAlpha) {

					// platform do not support alpha (e.g. Ubuntu 12.04 in my test system)

					_shell.setAlpha(0);

					_shell.setVisible(false);
					_isFadeOut = false;

				} else {

					_fadeAlpha = currentAlpha;

					if (currentAlpha == 0) {

						// reached end of fade in

						_shell.setVisible(false);
						_isFadeOut = false;

					} else {

						// start timer for a neww fade out
						_display.timerExec(FADE_TIME_INTERVAL, this);
					}
				}
			}
		}
	}

	public FullScreenPhotoGallery(	final Shell fullScreenShell,
									final GalleryMT20 sourceGallery,
									final FullScreenImageViewer fullScreenImageViewer) {

		_fullScreenShell = fullScreenShell;
		_sourceGallery = sourceGallery;
		_fullScreenImageViewer = fullScreenImageViewer;

		createUI();

		_photoGalleryAnimation = new ControlAnimation(_galleryShell, _photoGallery.getGallery());

		_allControlsListener = new AllControlsListener();
		addListenerToAllControls(_galleryShell);

		addFullScreenListener();
	}

	private void addFullScreenListener() {

		_fullScreenShell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	/**
	 * ########################### Recursive #########################################<br>
	 * <p>
	 * Add listener to all controls
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void addListenerToAllControls(final Control control) {

		control.addListener(SWT.KeyDown, _allControlsListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addListenerToAllControls(child);
			}
		}
	}

	private void addShellListener() {

		_galleryShell.addControlListener(new ControlListener() {

			@Override
			public void controlMoved(final ControlEvent e) {}

			@Override
			public void controlResized(final ControlEvent e) {
				onResize(e);
			}
		});

		_galleryShell.addShellListener(new ShellListener() {

			@Override
			public void shellActivated(final ShellEvent e) {}

			@Override
			public void shellClosed(final ShellEvent e) {}

			@Override
			public void shellDeactivated(final ShellEvent e) {
				hideGallery();
			}

			@Override
			public void shellDeiconified(final ShellEvent e) {}

			@Override
			public void shellIconified(final ShellEvent e) {
				hideGallery();
			}
		});
	}

	private void createUI() {

		_galleryShell = new Shell(SWT.NO_TRIM | SWT.ON_TOP);

		final Rectangle fsShellSize = _fullScreenShell.getBounds();

		_galleryShell.setBounds(fsShellSize.x, fsShellSize.y, fsShellSize.width, GALLERY_DEFAULT_HEIGHT);
//		_galleryShell.setLayout(new FillLayout());
		GridLayoutFactory.fillDefaults().spacing(0, 10).applyTo(_galleryShell);
//		_galleryShell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		addShellListener();

		createUI_10_Gallery(_galleryShell);
	}

	private void createUI_10_Gallery(final Shell parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{

			_photoGallery = new PhotoGallery();

			_photoGallery.hideActionSorting();
			_photoGallery.hideActionFiltering();
			_photoGallery.setShowCustomActionBar();

			_photoGallery.createPhotoGallery(container, SWT.H_SCROLL, this);

			/**
			 * Prevent to open pref dialog, when it's opened it would close this tooltip and the
			 * pref dialog is hidden -->> APP IS FREEZING !!!
			 */
			_photoGallery.setShowOtherShellActions(false);

			createUI_20_ActionBar(_photoGallery.getCustomActionBarContainer());

			createUI_30_Footer(container);
		}

		fillActionBar();

		/*
		 * set fullscreen image viewer in the photo gallery to the fullscreen image viewer in the
		 * source gallery, this is a bit a a hack
		 */
		_photoGallery.setFullScreenImageViewer(_sourceGallery.getFullScreenImageViewer());

		_galleryContainer = _photoGallery.getGalleryContainer();
	}

	private void createUI_20_ActionBar(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * create gallery toolbar
			 */
			_galleryToolbarControl = new ToolBar(container, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.FILL)
					.grab(true, false)
					.applyTo(_galleryToolbarControl);

			/*
			 * spinner: resize image
			 */
			_spinnerResizeImage = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.applyTo(_spinnerResizeImage);
			_spinnerResizeImage.setMinimum(GALLERY_MIN_IMAGE_SIZE);
			_spinnerResizeImage.setMaximum(GALLERY_MAX_IMAGE_SIZE);
			_spinnerResizeImage.setToolTipText(UI.IS_OSX
					? Messages.FullScreen_ImageViewer_Spinner_ResizeImage_Tooltip_OSX
					: Messages.FullScreen_ImageViewer_Spinner_ResizeImage_Tooltip);

			_spinnerResizeImage.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectResizeImage();
				}
			});
			_spinnerResizeImage.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSelectResizeImage();
				}
			});

		}
	}

	private void createUI_30_Footer(final Composite parent) {

		_containerFooter = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerFooter);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.spacing(0, 0)
				.extendedMargins(0, 0, 5, 0)
				.applyTo(_containerFooter);
//		_containerFooter.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
//			final Label label = new Label(_containerFooter, SWT.NONE);
//			GridDataFactory.fillDefaults().applyTo(label);
//			label.setText("Madlkfjasljdfasjdflasf");
		}

	}

	private void fillActionBar() {

		/*
		 * fill gallery toolbar
		 */
		_galleryToolbarManager = new ToolBarManager(_galleryToolbarControl);

		// must be called after the custom action bar is created
		_photoGallery.createActionBar();

		_galleryToolbarManager.update(false);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	@Override
	public IToolBarManager getToolBarManager() {
		return _galleryToolbarManager;
	}

	private void hideGallery() {

		if (_galleryShell == null || _galleryShell.isVisible() == false) {
			return;
		}

		_photoGallery.stopLoadingImages();

		_photoGalleryAnimation.fadeOut();

		_fullScreenImageViewer.activate();
	}

	private void onAllControlsEvent(final Event event) {

		final int keyCode = event.keyCode;

		if (keyCode == SWT.ESC) {

			// hide full screen gallery
			hideGallery();

		} else {

			final char keyCharacterShowPhotoGallery = Messages.FullScreenImageViewer_KeyCharacter_ShowPhotoGallery
					.charAt(0);

			if (event.character == keyCharacterShowPhotoGallery) {

				// hide with the same key as opening full screen gallery
				hideGallery();
			}
		}
	}

	private void onDispose() {

		if (_galleryShell != null) {

			saveState();

			_galleryShell.close();
		}
	}

	private void onResize(final ControlEvent e) {

//		final Point shellSize = _galleryShell.getSize();
//		final Point gallerySize = _photoGallery.getGallery().getSize();
//
//		final int trimX = shellSize.y - gallerySize.y;
//
//		System.out.println(UI.timeStampNano()
//				+ " onResize()\ttrim: "
//				+ trimX
//				+ "  shell: "
//				+ shellSize.y
//				+ "  gallery: "
//				+ gallerySize.y);
	}

	private void onSelectResizeImage() {

		setGallerySize(_spinnerResizeImage.getSelection());
	}

	@Override
	public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

	void restoreState(final IDialogSettings state) {

		_state = state;

		final int imageSize = Util.getStateInt(state, STATE_FULL_SCREEN_PHOTO_GALLERY_HEIGHT, GALLERY_DEFAULT_HEIGHT);
		_spinnerResizeImage.setSelection(imageSize);

		updateColors(true);

		_photoGallery.restoreState(state);
	}

	void saveState() {

		_state.put(STATE_FULL_SCREEN_PHOTO_GALLERY_HEIGHT, _spinnerResizeImage.getSelection());

		_photoGallery.saveState(_state);
	}

	/**
	 * Set shell size from gallery size.
	 * 
	 * @param imageSize
	 */
	private void setGallerySize(final int imageSize) {

		final Point shellSize = _galleryShell.getSize();
		final Point gallerySize = _photoGallery.getGallery().getSize();

		final int trimX = shellSize.y - gallerySize.y;

		_galleryShell.setSize(shellSize.x, trimX + imageSize);
	}

	@Override
	public void setSelection(final PhotoSelection photoSelection) {
		_fullScreenImageViewer.showImage(photoSelection);
	}

	boolean showImages(final int mouseY, final int displayedItemIndex) {

		if (mouseY == 0) {

			// show gallery

			_displayedItemIndex = displayedItemIndex;

			showImages_10_InGallery();

			return true;

		} else {

			// hide gallery

			hideGallery();

			return false;
		}
	}

	private void showImages_10_InGallery() {

		final IPhotoProvider photoProvider = _sourceGallery.getPhotoProvider();
		final PhotoWrapper[] photoWrapper = photoProvider.getSortedAndFilteredPhotoWrapper();

		final int photosHash = photoWrapper.hashCode();
		final String galleryPositionKey = photosHash + "_FullScreenPhotoGallery";//$NON-NLS-1$

		/**
		 * !!! gallery shell must be visible before any gallery methods are called, otherwise the
		 * gallery is hidden and not fully initialized !!!!
		 */

		final boolean isShellVisible = _galleryShell.isVisible();
		if (isShellVisible == false) {
			_photoGalleryAnimation.fadeIn();
		}

		/**
		 * check if new images should be displayed, this check is VERY IMPORTANT otherwise this can
		 * be a performance hog
		 */
		if (_displayedPhotosHash != photosHash) {

			_displayedPhotosHash = photosHash;

			final Control gallery = _photoGallery.getGallery();

			final Point gallerySize = gallery.getSize();

			if (gallerySize.x == 0) {

				/**
				 * setting size is a bit tricky, I found no other way
				 */

				// height must be set with the layout
				final GridData gd = (GridData) _galleryContainer.getLayoutData();
				gd.heightHint = _spinnerResizeImage.getSelection();

				// with must be set from the shell size
				gd.widthHint = _galleryShell.getSize().x;

				_galleryShell.pack(true);
			}

			_photoGallery.showImages(photoWrapper, galleryPositionKey);
		}

		// show photo in the gallery which is displayed in the full screen viewer
		_photoGallery.selectItem(_displayedItemIndex, galleryPositionKey);
	}

	private void updateColors(final boolean isRestore) {

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
		final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);

		final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

		_photoGallery.updateColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor, isRestore);

		_containerFooter.setBackground(bgColor);
	}

}
