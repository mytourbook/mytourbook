/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.ITooltipUIProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

public class TaggingView_TooltipUIProvider implements ITooltipUIProvider {

   private static final String IMAGE_APP_CLOSE          = net.tourbook.common.Messages.Image__App_Close;
   private static final String APP_ACTION_CLOSE_TOOLTIP = net.tourbook.common.Messages.App_Action_Close_Tooltip;

   private static final int    SHELL_MARGIN             = 5;
   private static final int    MAX_DATA_WIDTH           = 300;

   private Object              _viewerCellData;

   private IToolTipProvider    _toolTipProvider;

   private ActionCloseTooltip  _actionCloseTooltip;
   private ActionEditTag       _actionEditTag;

   private boolean             _hasNotes;
   private String              _content_Notes;

   /*
    * UI resources
    */
   private Color _bgColor;
   private Color _fgColor;

   /*
    * UI controls
    */
   private Composite   _ttContainer;

   private Label       _lblTitle;
   private Text        _txtNotes;
   private TaggingView _taggingView;

   private class ActionCloseTooltip extends Action {

      public ActionCloseTooltip() {

         super(null, Action.AS_PUSH_BUTTON);

         setToolTipText(APP_ACTION_CLOSE_TOOLTIP);
         setImageDescriptor(CommonActivator.getImageDescriptor(IMAGE_APP_CLOSE));
      }

      @Override
      public void run() {
         _toolTipProvider.hideToolTip();
      }
   }

   private class ActionEditTag extends Action {

      public ActionEditTag() {

         super(null, Action.AS_PUSH_BUTTON);

         setToolTipText(Messages.Action_Tag_Edit_Tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_disabled));
      }

      @Override
      public void run() {

         _toolTipProvider.hideToolTip();

         _taggingView.editTag(_viewerCellData);
      }
   }

   public TaggingView_TooltipUIProvider(final TaggingView taggingView) {

      _taggingView = taggingView;
   }

   @Override
   public Composite createTooltipUI(final Composite parent, final Object viewerCellData, final IToolTipProvider toolTipProvider) {

      _viewerCellData = viewerCellData;
      _toolTipProvider = toolTipProvider;

      parseContent();

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      final Composite container = createUI(parent);

      updateUI();
      updateUI_Layout();

//      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(_fgColor);
      shellContainer.setBackground(_bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//      shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         _ttContainer = new Composite(shellContainer, SWT.NONE);
         _ttContainer.setForeground(_fgColor);
         _ttContainer.setBackground(_bgColor);
         GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_ttContainer);
//         _ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            createUI_10_UpperPart(_ttContainer);
            createUI_90_LowerPart(_ttContainer);
         }
      }

      return shellContainer;
   }

   private void createUI_10_UpperPart(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
            .numColumns(2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {

         {
            // Title

            _lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
            _lblTitle.setForeground(_fgColor);
            _lblTitle.setBackground(_bgColor);
            GridDataFactory.fillDefaults()
                  .hint(MAX_DATA_WIDTH, SWT.DEFAULT)
                  .grab(true, false)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblTitle);
            MTFont.setBannerFont(_lblTitle);
         }
         {
            // action toolbar in the top right corner
            createUI_12_Toolbar(container);
         }
      }
   }

   private void createUI_12_Toolbar(final Composite container) {

      /*
       * Create toolbar
       */
      final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
      GridDataFactory.fillDefaults().applyTo(toolbar);
      toolbar.setForeground(_fgColor);
      toolbar.setBackground(_bgColor);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      _actionEditTag = new ActionEditTag();
      tbm.add(_actionEditTag);

      /**
       * The close action is ALWAYS visible, sometimes there is a bug that the tooltip do not
       * automatically close when hovering out.
       */
      _actionCloseTooltip = new ActionCloseTooltip();
      tbm.add(_actionCloseTooltip);

      tbm.update(true);
   }

   private void createUI_90_LowerPart(final Composite parent) {

      if (_hasNotes == false) {
         return;
      }

//      final Label label;
      final PixelConverter pc = new PixelConverter(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(5, 0).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
      {
         /*
          * Notes
          */
         if (_hasNotes) {

            {
               // text field

               int style = SWT.WRAP | SWT.MULTI | SWT.READ_ONLY;//| SWT.BORDER;
               final int lineCount = Util.countCharacter(_content_Notes, '\n');

               if (lineCount > 10) {
                  style |= SWT.V_SCROLL;
               }

               _txtNotes = new Text(container, style);
               GridDataFactory
                     .fillDefaults()//
                     .span(2, 1)
                     .grab(true, false)
                     .hint(pc.convertWidthInCharsToPixels(80), SWT.DEFAULT)
                     .applyTo(_txtNotes);

               if (lineCount > 15) {
                  final GridData gd = (GridData) _txtNotes.getLayoutData();
                  gd.heightHint = pc.convertHeightInCharsToPixels(15);
               }

               _txtNotes.setForeground(_fgColor);
               _txtNotes.setBackground(_bgColor);
            }
         }
      }
   }

//   private Label createUI_Label(final Composite parent, final String labelText) {
//
//      final Label label = new Label(parent, SWT.NONE);
//      label.setForeground(_fgColor);
//      label.setBackground(_bgColor);
//
//      if (labelText != null) {
//         label.setText(labelText);
//      }
//
//      return label;
//   }

   private TourTagCategory getTagCategory() {

      final TVITagView_TagCategory tagCategoryItem = (TVITagView_TagCategory) _viewerCellData;

      final HashMap<Long, TourTagCategory> allTourTagCategories = TourDatabase.getAllTourTagCategories();
      final TourTagCategory tagCategory = allTourTagCategories.get(tagCategoryItem.getCategoryId());

      return tagCategory;
   }

   private TourTag getTourTag() {

      final TVITagView_Tag tagItem = (TVITagView_Tag) _viewerCellData;

      final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();
      final TourTag tourTag = allTourTags.get(tagItem.getTagId());

      return tourTag;
   }

   private void parseContent() {

      _content_Notes = null;

      if (_viewerCellData instanceof TVITagView_Tag) {

         final TourTag tourTag = getTourTag();

         _content_Notes = tourTag.getNotes();

      } else if (_viewerCellData instanceof TVITagView_TagCategory) {

         final TourTagCategory tagCategory = getTagCategory();

         _content_Notes = tagCategory.getNotes();
      }

      _hasNotes = _content_Notes != null && _content_Notes.length() > 0;
   }

   private void updateUI() {

      if (_viewerCellData instanceof TVITagView_Tag) {

         final TourTag tourTag = getTourTag();

         _lblTitle.setText(tourTag.getTagName());

         _actionEditTag.setToolTipText(Messages.Action_Tag_Edit_Tooltip);

      } else if (_viewerCellData instanceof TVITagView_TagCategory) {

         final TourTagCategory tagCategory = getTagCategory();

         _lblTitle.setText(tagCategory.getCategoryName());

         _actionEditTag.setToolTipText(Messages.Action_TagCategory_Edit_Tooltip);
      }

      if (_hasNotes) {

         _txtNotes.setText(_content_Notes);
      }

   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }
}
