/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.map2.action.ActionMap2_PhotoFilter;
import net.tourbook.map2.view.Map2View;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoRatingStarOperator;
import net.tourbook.photo.RatingStars;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Photo properties dialog.
 */
public class Slideout_Map2_PhotoFilter extends AdvancedSlideout {

   public static final int                        OPERATOR_IS_LESS_OR_EQUAL  = 0;
   public static final int                        OPERATOR_IS_EQUAL          = 1;
   public static final int                        OPERATOR_IS_MORE_OR_EQUAL  = 2;

   private static final String[]                  _ratingStarOperatorsText   = {

         Messages.Photo_Filter_Operator_IsLess,
         Messages.Photo_Filter_Operator_IsEqual,
         Messages.Photo_Filter_Operator_IsMore,

   };

   /**
    * <b>THEY MUST BE IN SYNC WITH </b> {@link #_ratingStarOperatorsText}
    */
   private static final PhotoRatingStarOperator[] _ratingStarOperatorsValues = {

         PhotoRatingStarOperator.IS_LESS_OR_EQUAL,
         PhotoRatingStarOperator.IS_EQUAL,
         PhotoRatingStarOperator.IS_MORE_OR_EQUAL,

   };

   private IDialogSettings                        _state;

   /**
    * Filter operator
    */
   private int                                    _filterRatingStarOperatorIndex;
   private int                                    _filterRatingStars         = RatingStars.MAX_RATING_STARS;

   private PixelConverter                         _pc;

   private ActionMap2_PhotoFilter                 _actionMap2_PhotoFilter;
   private Map2View                               _map2View;
   private ToolItem                               _toolItem;

   /*
    * UI controls
    */
   private Composite   _shellContainer;
   private Composite   _containerNumbers;

   private Label       _lblAllPhotos;
   private Label       _lblFilteredPhotos;
   private Combo       _comboRatingStarOperators;

   private RatingStars _ratingStars;

   public Slideout_Map2_PhotoFilter(final ActionMap2_PhotoFilter actionMap2_PhotoFilter,
                                    final ToolItem toolItem,
                                    final Map2View map2View,
                                    final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 220, 100, 220, 100 });

      _actionMap2_PhotoFilter = actionMap2_PhotoFilter;
      _toolItem = toolItem;
      _map2View = map2View;
      _state = state;

      setTitleText(Messages.Photo_Filter_Label_PhotoFilter);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      UI.setChildColors(parent,
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));

      updateUI();

      enableActions();
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .margins(0, 0)
            .applyTo(_shellContainer);
      {
         createUI_10_Filter(_shellContainer);
      }

      return _shellContainer;
   }

   private void createUI_10_Filter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NO_FOCUS);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Combo: > = <
             */
            _comboRatingStarOperators = new Combo(container, SWT.READ_ONLY);
            _comboRatingStarOperators.setVisibleItemCount(10);
            _comboRatingStarOperators.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_RatingStarOperands();
               }
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_comboRatingStarOperators);
         }
         {
            /*
             * Rating stars
             */
            _ratingStars = new RatingStars(container);
            _ratingStars.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_RatingStars();
               }
            });
         }
         {
            /*
             * Number of filtered photos
             */
            _containerNumbers = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_pc.convertWidthInCharsToPixels(12), SWT.DEFAULT)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_containerNumbers);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(_containerNumbers);
            {
               /*
                * value: number of all photos
                */
               _lblAllPhotos = new Label(_containerNumbers, SWT.NO_FOCUS);
               _lblAllPhotos.setText(UI.EMPTY_STRING);
               _lblAllPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfAllPhotos_Tooltip);
               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .align(SWT.END, SWT.FILL).applyTo(_lblAllPhotos);

               /*
                * label: number of filtered photos
                */
               final Label label = new Label(_containerNumbers, SWT.NO_FOCUS);
               label.setText(UI.DASH);

               /*
                * value: number of filtered photos
                */
               _lblFilteredPhotos = new Label(_containerNumbers, SWT.NO_FOCUS);
               _lblFilteredPhotos.setText(UI.EMPTY_STRING);
               _lblFilteredPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfFilteredPhotos_Tooltip);
            }
         }
      }
   }

   private void enableActions() {

   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   protected void onDispose() {

   }

   @Override
   protected void onFocus() {

      _comboRatingStarOperators.setFocus();
   }

   private void onSelect_RatingStarOperands() {

      _filterRatingStarOperatorIndex = _comboRatingStarOperators.getSelectionIndex();

      updateMapPhotoFilter();
   }

   private void onSelect_RatingStars() {

      final int selectedStars = _ratingStars.getSelection();

      _filterRatingStars = selectedStars;

      enableActions();

      updateMapPhotoFilter();
   }

   public void restoreState() {

      // set photo filter into the map
//      updateMapPhotoFilter();
   }

   @Override
   public void saveState() {

      // save slideout position/size
      super.saveState();
   }

   private void updateMapPhotoFilter() {

      _map2View.photoFilter_UpdateFromSlideout(_filterRatingStars, _ratingStarOperatorsValues[_filterRatingStarOperatorIndex]);
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
   }

   /**
    * This is called after the filter is run to update depending UI controls, e.g. number of
    * filtered photos
    *
    * @param numFilteredPhotos
    * @param numAllPhotos
    * @param data
    */
   public void updateUI_NumberOfPhotos() {

      if (_lblAllPhotos == null || _lblAllPhotos.isDisposed()) {

         // UI is not initialized

         return;
      }

      _lblAllPhotos.setText(Integer.toString(_map2View.getPhotos().size()));
      _lblFilteredPhotos.setText(Integer.toString(_map2View.getFilteredPhotos().size()));

      _containerNumbers.layout();
   }

}
