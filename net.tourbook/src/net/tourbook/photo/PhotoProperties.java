/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import net.tourbook.Messages;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.MapFilterData;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewPart;

import de.byteholder.geoclipse.map.UI;

/**
 * Photo properties dialog.
 */
public class PhotoProperties extends AnimatedToolTipShell implements IPhotoEventListener {

	private static final int		SHELL_MARGIN							= 5;

	private static final int		MIN_IMAGE_WIDTH							= 10;

	/**
	 * This value is small because a map do not yet load large images !!!
	 */
	private static final int		MAX_IMAGE_WIDTH							= 200;

	private static final String		STATE_PHOTO_FILTER_RATING_STARS			= "STATE_PHOTO_FILTER_RATING_STARS";			//$NON-NLS-1$
	private static final String		STATE_PHOTO_FILTER_RATING_STAR_OPERATOR	= "STATE_PHOTO_FILTER_RATING_STAR_OPERATOR";	//$NON-NLS-1$
	private static final String		STATE_PHOTO_PROPERTIES_IMAGE_SIZE		= "STATE_PHOTO_PROPERTIES_IMAGE_SIZE";			//$NON-NLS-1$

	private IDialogSettings			_state;

	// initialize with default values which are (should) never be used
	private Rectangle				_itemBounds								= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer								= new WaitTimer();

	private boolean					_canOpenToolTip;

	private boolean					_isWaitTimerStarted;

	private int						_filterRatingStars						= RatingStars.MAX_RATING_STARS;

	private final ListenerList		_propertiesListeners					= new ListenerList(ListenerList.IDENTITY);

	private int						_imageSize;

	/*
	 * filter operator
	 */
	private int						_filterRatingStarOperatorIndex;

	/*
	 * UI resources
	 */
	private Color					_fgColor;
	private Color					_bgColor;

	private PixelConverter			_pc;
	private MapFilterData			_oldMapFilterData;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Composite				_containerFilterHeader;
	private Label					_lblAllPhotos;
	private Label					_lblFilteredPhotos;

	private Combo					_comboRatingStarOperators;
	private RatingStars				_ratingStars;

	private Spinner					_spinnerImageSize;

	public static final int			OPERATOR_IS_LESS_OR_EQUAL				= 0;
	public static final int			OPERATOR_IS_EQUAL						= 1;
	public static final int			OPERATOR_IS_MORE_OR_EQUAL				= 2;

	private static final String[]	_ratingStarOperatorsText				= {
			Messages.Photo_Filter_Operator_IsLess,
			Messages.Photo_Filter_Operator_IsEqual,
			Messages.Photo_Filter_Operator_IsMore,
																			//
																			};

	/**
	 * <b>THEY MUST BE IN SYNC WITH </b> {@link #_filterRatingStarOperatorsText}
	 */
	private static final int[]		_ratingStarOperatorsValues				= {
			OPERATOR_IS_LESS_OR_EQUAL,
			OPERATOR_IS_EQUAL,
			OPERATOR_IS_MORE_OR_EQUAL,
																			//
																			};

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public PhotoProperties(final Control ownerControl, final ToolBar toolBar, final IDialogSettings state) {

		super(ownerControl);

		_state = state;

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});

		ownerControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		PhotoManager.addPhotoEventListener(this);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
	}

	public void addPropertiesListener(final IPhotoPropertiesListener listener) {
		_propertiesListeners.add(listener);
	}

	@Override
	protected void beforeHideToolTip() {}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite container = createUI(parent);

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		_fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		_bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

		updateUI();

		updateUI_Colors(parent);

		if (_oldMapFilterData != null) {

			/*
			 * _oldMapFilterData can be set before the UI is created
			 */

			updateFilterUI(_oldMapFilterData);
		}

		enableActions();

		return container;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.margins(SHELL_MARGIN, SHELL_MARGIN)
				.numColumns(3)
				.applyTo(_shellContainer);
		{
			createUI_10_Filter(_shellContainer);

			// spacer
			final Label label = new Label(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().hint(20, 0).applyTo(label);

			createUI_20_ImageSize(_shellContainer);
		}

		return _shellContainer;
	}

	private void createUI_10_Filter(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NO_FOCUS);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * label: photo filter
			 */
			final Label label = new Label(container, SWT.NO_FOCUS);
			GridDataFactory.fillDefaults().applyTo(label);

			label.setText(Messages.Photo_Filter_Label_RatingStars);
			label.setToolTipText(Messages.Photo_Filter_Label_RatingStars_Tooltip);

			createUI_12_FilterHeader(container);

			/*
			 * combo: > = <
			 */
//			_comboRatingStarOperators = new Combo(container, SWT.READ_ONLY);
			_comboRatingStarOperators = new Combo(container, SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.FILL)
//					.hint(_pc.convertWidthInCharsToPixels(15), SWT.DEFAULT)
					.applyTo(_comboRatingStarOperators);
			_comboRatingStarOperators.setVisibleItemCount(10);
//			_comboRatingStarOperators.setToolTipText(Messages.Photos_PhotoFilter_Combo_RatingStarOperands_Tooltip);
			_comboRatingStarOperators.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectRatingStarOperands();
				}
			});

			/*
			 * rating stars
			 */
			_ratingStars = new RatingStars(container);
			_ratingStars.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectRatingStars();
				}
			});
		}
	}

	private void createUI_12_FilterHeader(final Composite parent) {

		_containerFilterHeader = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.applyTo(_containerFilterHeader);
		GridLayoutFactory.fillDefaults()//
				.applyTo(_containerFilterHeader);
//		_containerFilterHeader.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * inner container is used to center horizontally
			 */
			final Composite innerContainer = new Composite(_containerFilterHeader, SWT.NONE);
			GridDataFactory.fillDefaults().//
					grab(true, false)
					.align(SWT.CENTER, SWT.FILL)
					.applyTo(innerContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(innerContainer);
//			innerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			{
				/*
				 * value: number of all photos
				 */
				_lblAllPhotos = new Label(innerContainer, SWT.NO_FOCUS);
				_lblAllPhotos.setText(UI.EMPTY_STRING);
				_lblAllPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfAllPhotos_Tooltip);

				/*
				 * label: number of filtered photos
				 */
				final Label label = new Label(innerContainer, SWT.NO_FOCUS);
				label.setText(UI.DASH_WITH_SPACE);

				/*
				 * value: number of filtered photos
				 */
				_lblFilteredPhotos = new Label(innerContainer, SWT.NO_FOCUS);
				_lblFilteredPhotos.setText(UI.EMPTY_STRING);
				_lblFilteredPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfFilteredPhotos_Tooltip);
			}
		}
	}

	private void createUI_20_ImageSize(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * label: displayed photos
			 */
			final Label label = new Label(container, SWT.NO_FOCUS);
			GridDataFactory.fillDefaults()//
//					.align(SWT.CENTER, SWT.BEGINNING)
					.applyTo(label);

			label.setText(Messages.Photo_Properties_Label_Size);
			label.setToolTipText(Messages.Photo_Properties_Label_ThumbnailSize_Tooltip);

			/*
			 * spinner: size
			 */
			_spinnerImageSize = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerImageSize);
			_spinnerImageSize.setMinimum(MIN_IMAGE_WIDTH);
			_spinnerImageSize.setMaximum(MAX_IMAGE_WIDTH);
			_spinnerImageSize.setIncrement(1);
			_spinnerImageSize.setPageIncrement(10);
			_spinnerImageSize.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectImageSize();
				}
			});
			_spinnerImageSize.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSelectImageSize();
				}
			});

		}
	}

	private void enableActions() {

//		final boolean isRatingStars = _filterRatingStars > 0;
//
//		_comboRatingStarOperators.setEnabled(isRatingStars);
	}

	private void firePropertiesEvent() {

		final PhotoPropertiesEvent propertyEvent = new PhotoPropertiesEvent();

		propertyEvent.filterRatingStars = _filterRatingStars;
		propertyEvent.fiterRatingStarOperator = _ratingStarOperatorsValues[_filterRatingStarOperatorIndex];

		final Object[] listeners = _propertiesListeners.getListeners();
		for (final Object listener : listeners) {
			((IPhotoPropertiesListener) listener).photoPropertyEvent(propertyEvent);
		}
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int itemWidth = _itemBounds.width;
		final int itemHeight = _itemBounds.height;

		final int itemWidth2 = itemWidth / 2;
		final int tipWidth2 = tipWidth / 2;

		final int devX = _itemBounds.x + itemWidth2 - tipWidth2;
		final int devY = _itemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	@Override
	protected Rectangle noHideOnMouseMove() {
		return _itemBounds;
	}

	private void onDispose() {

		PhotoManager.removePhotoEventListener(this);
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {}

	private void onSelectImageSize() {

		final int oldImageSize = _imageSize;

		_imageSize = _spinnerImageSize.getSelection();

		// optimize fire event
		if (oldImageSize != _imageSize) {

			Photo.setPaintedMapImageWidth(_imageSize);

			firePropertiesEvent();
		}
	}

	private void onSelectRatingStarOperands() {

		_filterRatingStarOperatorIndex = _comboRatingStarOperators.getSelectionIndex();

		firePropertiesEvent();
	}

	private void onSelectRatingStars() {

		final int selectedStars = _ratingStars.getSelection();

		_filterRatingStars = selectedStars;

		enableActions();

		firePropertiesEvent();
	}

	/**
	 * @param itemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle itemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (itemBounds != null) {

				_itemBounds = itemBounds;

				showToolTip();
			}

		} else {

			if (itemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_itemBounds = itemBounds;
			_canOpenToolTip = true;

//		System.out.println(UI.timeStampNano()
//				+ " open\t2\t_isWaitTimerStarted="
//				+ _isWaitTimerStarted
//				+ ("\t_canOpenToolTip=" + _canOpenToolTip)
//				+ ("\t__itemBounds=" + _itemBounds)
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	@Override
	public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

		if (photoEventId == PhotoEventId.PHOTO_FILTER) {

			if (data instanceof MapFilterData) {

				updateFilterUI((MapFilterData) data);
			}
		}
	}

	public void restoreState() {

		_filterRatingStars = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STARS, RatingStars.MAX_RATING_STARS);
		_filterRatingStarOperatorIndex = Util.getStateInt(
				_state,
				STATE_PHOTO_FILTER_RATING_STAR_OPERATOR,
				OPERATOR_IS_EQUAL);

		_imageSize = Util.getStateInt(_state, STATE_PHOTO_PROPERTIES_IMAGE_SIZE, Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT);

		// ensure that an image is displayed, it happend that image size was 0
		if (_imageSize < 10) {
			_imageSize = Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT;
		}

		// set image size for the map photos
		Photo.setPaintedMapImageWidth(_imageSize);

		// set photo filter into the map
		firePropertiesEvent();
	}

	public void saveState() {

		_state.put(STATE_PHOTO_FILTER_RATING_STARS, _filterRatingStars);
		_state.put(STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _filterRatingStarOperatorIndex);

		_state.put(STATE_PHOTO_PROPERTIES_IMAGE_SIZE, _imageSize);
	}

	/**
	 * This is called when the filter is run and filter statistics are available.
	 * 
	 * @param data
	 */
	protected void updateFilterActionUI(final MapFilterData data) {
		// do nothing
	}

	private void updateFilterUI(final MapFilterData data) {

		if (_lblAllPhotos == null || _lblAllPhotos.isDisposed()) {

			// UI is not initialized

			_oldMapFilterData = data;

			return;
		}

		_lblAllPhotos.setText(Integer.toString(data.allPhotos));
		_lblFilteredPhotos.setText(Integer.toString(data.filteredPhotos));

		_containerFilterHeader.layout();

		// update action button
		updateFilterActionUI(data);
	}

	private void updateUI() {

		// select rating star
		_ratingStars.setSelection(_filterRatingStars);

		for (final String operator : _ratingStarOperatorsText) {
			_comboRatingStarOperators.add(operator);
		}

		// ensure array bounds
		if (_filterRatingStarOperatorIndex >= _ratingStarOperatorsText.length) {
			_filterRatingStarOperatorIndex = 0;
		}

		// select operator
		_comboRatingStarOperators.select(_filterRatingStarOperatorIndex);

		// image size
		_spinnerImageSize.setSelection(_imageSize);
	}

	private void updateUI_Colors(final Control child) {

		/*
		 * ignore these controls because they do not look very good on Linux & OSX
		 */
		if (child instanceof Spinner || child instanceof Combo) {
			return;
		}

		child.setForeground(_fgColor);
		child.setBackground(_bgColor);

		if (child instanceof Composite) {
			final Control[] children = ((Composite) child).getChildren();
			for (final Control element : children) {

				if (element != null && element.isDisposed() == false) {
					updateUI_Colors(element);
				}
			}
		}
	}

}
