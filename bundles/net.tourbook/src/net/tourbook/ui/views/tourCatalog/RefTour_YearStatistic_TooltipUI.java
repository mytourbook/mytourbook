/******************************************************  *************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.OtherMessages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.Action_ToolTip_EditPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class RefTour_YearStatistic_TooltipUI {

   private static final int VERTICAL_LINE_SPACE = 8;
   private static final int SHELL_MARGIN        = 5;

   /*
    * Tooltip context
    */
   private IToolTipProvider _toolTipProvider;

   /*
    * Actions
    */
   private ActionCloseTooltip             _actionCloseTooltip;
   private Action_ToolTip_EditPreferences _actionPrefDialog;

   /*
    * UI resources
    */
   private Color  _bgColor;
   private Color  _fgColor;

   private Float  _avgPulse;
   private Float  _avgSpeed;
   private String _title;

   /*
    * UI controls
    */
   private Composite _ttContainer;

   private Label     _lblAvgPulse;
   private Label     _lblAvgPulseUnit;
   private Label     _lblAvgSpeed;
   private Label     _lblAvgSpeedUnit;
   private Label     _lblTitle;

   private class ActionCloseTooltip extends Action {

      public ActionCloseTooltip() {

         super(null, Action.AS_PUSH_BUTTON);

         setToolTipText(OtherMessages.APP_ACTION_CLOSE_TOOLTIP);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Close));
      }

      @Override
      public void run() {
         _toolTipProvider.hideToolTip();
      }
   }

   private void createActions() {

      _actionCloseTooltip = new ActionCloseTooltip();

      _actionPrefDialog = new Action_ToolTip_EditPreferences(_toolTipProvider,
            Messages.Tour_Tooltip_Action_EditFormatPreferences,
            PrefPageAppearanceDisplayFormat.ID,

            // set index for the tab folder which should be selected when dialog is opened and applied
            // in net.tourbook.preferences.PrefPageAppearanceDisplayFormat.applyData(Object)
            // -> select single tour formatting
            Integer.valueOf(0));
   }

   /**
    * @param parent
    * @param toolTipProvider
    * @param statisticData_Frequency
    * @param frequencyStatistic
    * @param serieIndex
    * @param valueIndex
    * @param uiText_Title
    * @param avgSpeed
    * @param avgPulse
    * @param tourTypeId
    * @param toolTip_Title
    * @param toolTip_SubTitle
    * @param summaryColumn_HeaderTitle
    * @param isShowSummary
    * @param isShowPercentage
    */
   void createContentArea(final Composite parent,
                          final IToolTipProvider toolTipProvider,
                          final String uiText_Title,
                          final Float avgPulse,
                          final Float avgSpeed) {

      _toolTipProvider = toolTipProvider;

      _title = uiText_Title;
      _avgPulse = avgPulse;
      _avgSpeed = avgSpeed;

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      initUI(parent);

      createActions();
      createUI(parent);

      updateUI();
      updateUI_Layout();

      enableControls();
   }

   private void createUI(final Composite parent) {

      final Point defaultSpacing = LayoutConstants.getSpacing();

      final int numColumns = 3;

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
         GridLayoutFactory
               .fillDefaults() //
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(_ttContainer);
//         _ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            createUI_10_Header(_ttContainer);

            final Composite container = new Composite(_ttContainer, SWT.NONE);
            container.setForeground(_fgColor);
            container.setBackground(_bgColor);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults()
                  .numColumns(numColumns)

                  // remove vertical spacing
                  .spacing(defaultSpacing.x, 0)
                  .applyTo(container);
//            container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            {
               createUI_Spacer_Row(container, numColumns);
               createUI_30_BarData(container);
            }
         }
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final int numColumns = 2;

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(numColumns)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * Title
          */
         _lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
         _lblTitle.setForeground(_fgColor);
         _lblTitle.setBackground(_bgColor);
         MTFont.setBannerFont(_lblTitle);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblTitle);

         /*
          * Action toolbar in the top right corner
          */
         createUI_12_Header_Toolbar(container);
      }
   }

   private void createUI_12_Header_Toolbar(final Composite container) {

      /*
       * Create toolbar
       */
      final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
      GridDataFactory.fillDefaults().applyTo(toolbar);
      toolbar.setForeground(_fgColor);
      toolbar.setBackground(_bgColor);
//      toolbar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      /*
       * Fill toolbar
       */

      tbm.add(_actionPrefDialog);

      /**
       * The close action is ALWAYS visible, sometimes there is a bug that the tooltip do not
       * automatically close when hovering out.
       */
      tbm.add(_actionCloseTooltip);

      tbm.update(true);
   }

   private void createUI_30_BarData(final Composite parent) {

      {
         /*
          * Avg Pulse
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPulse);

         _lblAvgPulse = createUI_LabelValue(parent, SWT.TRAIL);
         _lblAvgPulseUnit = createUI_LabelValue(parent, SWT.LEAD);
      }
      {
         /*
          * Avg Speed
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgSpeed);

         _lblAvgSpeed = createUI_LabelValue(parent, SWT.TRAIL);
         _lblAvgSpeedUnit = createUI_LabelValue(parent, SWT.LEAD);
      }
   }

   private Label createUI_Label(final Composite parent, final String labelText) {

      final Label label = new Label(parent, SWT.NONE);

      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      if (labelText != null) {
         label.setText(labelText);
      }

      return label;
   }

   private Label createUI_LabelValue(final Composite parent, final int style) {

      final Label label = new Label(parent, style);

      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      GridDataFactory.fillDefaults().applyTo(label);

      return label;
   }

   public Composite createUI_NoData(final Composite parent) {

      final Display display = parent.getDisplay();

      final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      final Color fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(fgColor);
      shellContainer.setBackground(bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {

         final Composite container = new Composite(shellContainer, SWT.NONE);
         container.setForeground(fgColor);
         container.setBackground(bgColor);
         GridLayoutFactory
               .fillDefaults()//
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(container);
         {
            final Label label = new Label(container, SWT.NONE);
//            label.setText(_noTourTooltip);
            label.setForeground(fgColor);
            label.setBackground(bgColor);
         }
      }

      return shellContainer;
   }

   /**
    * Spacer for 4 columns
    *
    * @param container
    * @param numColumns
    */
   private void createUI_Spacer_Row(final Composite container, final int numColumns) {

      // spacer
      final Label label = createUI_Label(container, null);
      GridDataFactory.fillDefaults()
            .span(numColumns, 1)
            .hint(1, VERTICAL_LINE_SPACE)
            .applyTo(label);
   }

   public void dispose() {

   }

   private void enableControls() {

   }

   private void initUI(final Composite parent) {

   }

   private void updateUI() {

      _lblTitle.setText(_title);

      // avg pulse
      _lblAvgPulse.setText(FormatManager.formatPulse(_avgPulse));
      _lblAvgPulseUnit.setText(Messages.Value_Unit_Pulse);

      _lblAvgSpeed.setText(FormatManager.formatSpeed(_avgSpeed));
      _lblAvgSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }

}
