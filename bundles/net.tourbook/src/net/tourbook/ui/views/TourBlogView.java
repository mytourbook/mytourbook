/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.CSS;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourBlogView extends ViewPart {

   public static final String  ID                                              = "net.tourbook.ui.views.TourBlogView";      //$NON-NLS-1$

   private static final String NL                                              = UI.NEW_LINE1;

   private static final String TOUR_BLOG_CSS                                   = "/tourbook/resources/tour-blog.css";       //$NON-NLS-1$

   static final String         STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR         = "STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR"; //$NON-NLS-1$
   static final boolean        STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT = false;
   static final String         STATE_IS_SHOW_HIDDEN_MARKER                     = "STATE_IS_SHOW_HIDDEN_MARKER";             //$NON-NLS-1$
   static final boolean        STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT             = true;

   private static final String EXTERNAL_LINK_URL                               = "http";                                    //$NON-NLS-1$
   private static final String HREF_TOKEN                                      = "#";                                       //$NON-NLS-1$
   private static final String PAGE_ABOUT_BLANK                                = "about:blank";                             //$NON-NLS-1$

   /**
    * This is necessary otherwise XULrunner in Linux do not fire a location change event.
    */
   private static final String HTTP_DUMMY                                      = "http://dummy";                            //$NON-NLS-1$

   private static final String ACTION_EDIT_TOUR                                = "EditTour";                                //$NON-NLS-1$
   private static final String ACTION_EDIT_MARKER                              = "EditMarker";                              //$NON-NLS-1$
   private static final String ACTION_HIDE_MARKER                              = "HideMarker";                              //$NON-NLS-1$
   private static final String ACTION_OPEN_MARKER                              = "OpenMarker";                              //$NON-NLS-1$
   private static final String ACTION_SHOW_MARKER                              = "ShowMarker";                              //$NON-NLS-1$

   private static String       HREF_EDIT_TOUR;
   private static String       HREF_EDIT_MARKER;
   private static String       HREF_HIDE_MARKER;
   private static String       HREF_OPEN_MARKER;
   private static String       HREF_SHOW_MARKER;

   static {

      HREF_EDIT_TOUR = HREF_TOKEN + ACTION_EDIT_TOUR;

      HREF_EDIT_MARKER = HREF_TOKEN + ACTION_EDIT_MARKER + HREF_TOKEN;
      HREF_HIDE_MARKER = HREF_TOKEN + ACTION_HIDE_MARKER + HREF_TOKEN;
      HREF_OPEN_MARKER = HREF_TOKEN + ACTION_OPEN_MARKER + HREF_TOKEN;
      HREF_SHOW_MARKER = HREF_TOKEN + ACTION_SHOW_MARKER + HREF_TOKEN;
   }

   private static final String           HREF_MARKER_ITEM = "#MarkerItem";                //$NON-NLS-1$

   private static final IPreferenceStore _prefStore       = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state           = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _state_WEB       = WEB.getState();

   private PostSelectionProvider         _postSelectionProvider;
   private ISelectionListener            _postSelectionListener;
   private IPropertyChangeListener       _prefChangeListener;
   private ITourEventListener            _tourEventListener;
   private IPartListener2                _partListener;

   private TourData                      _tourData;

   private String                        _htmlCss;

   private String                        _imageUrl_ActionEdit;
   private String                        _imageUrl_ActionHideMarker;
   private String                        _imageUrl_ActionShowMarker;

   private String                        _cssMarker_DefaultColor;
   private String                        _cssMarker_DeviceColor;
   private String                        _cssMarker_HiddenColor;

   private boolean                       _isDrawWithDefaultColor;
   private boolean                       _isShowHiddenMarker;

   private Long                          _reloadedTourMarkerId;

   private ActionTourBlogOptions         _actionTourBlogOptions;

   /*
    * UI controls
    */
   private PageBook  _pageBook;

   private Composite _pageNoBrowser;
   private Composite _pageNoData;
   private Composite _pageContent;
   private Composite _parent;

   private Browser   _browser;
   private TourChart _tourChart;
   private Text      _txtNoBrowser;

   private class ActionTourBlogOptions extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTourBlogOptions(_parent, toolbar, TourBlogView.this, _state);
      }
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

               updateUI();
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
            if (part == TourBlogView.this) {
               return;
            }
            onSelectionChanged(selection);
         }
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourBlogView.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {

                  // update modified tour

                  final long viewTourId = _tourData.getTourId();

                  for (final TourData tourData : modifiedTours) {
                     if (tourData.getTourId() == viewTourId) {

                        // get modified tour
                        _tourData = tourData;

                        // removed old tour data from the selection provider
                        _postSelectionProvider.clearSelection();

                        updateUI();

                        // nothing more to do, the view contains only one tour
                        return;
                     }
                  }
               }

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.MARKER_SELECTION) {

               if (eventData instanceof SelectionTourMarker) {

                  final TourData tourData = ((SelectionTourMarker) eventData).getTourData();

                  if (tourData != _tourData) {

                     _tourData = tourData;

                     updateUI();
                  }
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tourData = null;

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();

      showInvalidPage();
   }

   private String create_10_Head() {

      // set body size
      final int bodyFontSize = Util.getStateInt(_state_WEB, WEB.STATE_BODY_FONT_SIZE, WEB.STATE_BODY_FONT_SIZE_DEFAULT);
      String htmlCss = _htmlCss.replace(WEB.STATE_BODY_FONT_SIZE_CSS_REPLACEMENT_TAG, Integer.toString(bodyFontSize));

      /*
       * Replace theme tags
       */

// SET_FORMATTING_OFF

      htmlCss = htmlCss.replace(WEB.CSS_TAG__BODY__COLOR,                        UI.IS_DARK_THEME ? "ddd" : "333");        //$NON-NLS-1$ //$NON-NLS-2$
      htmlCss = htmlCss.replace(WEB.CSS_TAG__BODY__BACKGROUND_COLOR,             UI.IS_DARK_THEME ? "333" : "fff");        //$NON-NLS-1$ //$NON-NLS-2$

      htmlCss = htmlCss.replace(WEB.CSS_TAG__A_LINK__COLOR,                      UI.IS_DARK_THEME ? "D6FF6F" : "3B9529");  //$NON-NLS-1$ //$NON-NLS-2$
      htmlCss = htmlCss.replace(WEB.CSS_TAG__A_VISITED__COLOR,                   UI.IS_DARK_THEME ? "7E9543" : "DE559D");  //$NON-NLS-1$ //$NON-NLS-2$

      htmlCss = htmlCss.replace(WEB.CSS_TAG__ACTION_CONTAINER__BACKGROUND_COLOR, UI.IS_DARK_THEME ? "444" : "f8f8f8");     //$NON-NLS-1$ //$NON-NLS-2$

// SET_FORMATTING_ON

      if (UI.IS_DARK_THEME) {

         // show dark scrollbar
         htmlCss = htmlCss.replace(WEB.CSS_TAG__BODY_SCROLLBAR, WEB.CSS_CONTENT__BODY_SCROLLBAR__DARK);
      }

      final String html = UI.EMPTY_STRING

            + "   <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" + NL //$NON-NLS-1$
            + "   <meta http-equiv='X-UA-Compatible' content='IE=edge' />" + NL //$NON-NLS-1$
            + htmlCss
            + NL;

      return html;
   }

   private String create_20_Body() {

      final StringBuilder sb = new StringBuilder();

      final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();
      final ArrayList<TourMarker> allMarker = new ArrayList<>(tourMarkers);
      Collections.sort(allMarker);

      create_22_BlogHeader(sb);
      create_24_Tour(sb);

      for (final TourMarker tourMarker : allMarker) {

         // check if marker is hidden and should not be displayed
         if (tourMarker.isMarkerVisible() == false && _isShowHiddenMarker == false) {
            continue;
         }

         sb.append("<div class='blog-item'>"); //$NON-NLS-1$
         sb.append("<div class='action-hover-container'>" + NL); //$NON-NLS-1$
         {
            create_30_Marker(sb, tourMarker);
            create_32_MarkerUrl(sb, tourMarker);
         }
         sb.append("</div>" + NL); //$NON-NLS-1$
         sb.append("</div>" + NL); //$NON-NLS-1$
      }

      return sb.toString();
   }

   private void create_22_BlogHeader(final StringBuilder sb) {

      /*
       * Date/Time header
       */
      final long elapsedTime = _tourData.getTourDeviceTime_Elapsed();

      final ZonedDateTime dtTourStart = _tourData.getTourStartTime();
      final ZonedDateTime dtTourEnd = dtTourStart.plusSeconds(elapsedTime);

      final String date = dtTourStart.format(TimeTools.Formatter_Date_F);

      final String time = String.format("%s - %s", //$NON-NLS-1$
            dtTourStart.format(TimeTools.Formatter_Time_M),
            dtTourEnd.format(TimeTools.Formatter_Time_M));

      sb.append("<div class='date'>" + date + "</div>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("<div class='time'>" + time + "</div>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("<div style='clear: both;'></div>" + NL); //$NON-NLS-1$
   }

   private void create_24_Tour(final StringBuilder sb) {

      String tourTitle = _tourData.getTourTitle();
      String tourDescription = _tourData.getTourDescription();
      String tourWeather = _tourData.getWeather();

      final boolean isDescription = tourDescription.length() > 0;
      final boolean isTitle = tourTitle.length() > 0;
      final boolean isWeather = tourWeather.length() > 0;

      if (isDescription || isTitle || isWeather) {

         sb.append("<div class='action-hover-container' style='margin-top:30px; margin-bottom: 5px;'>" + NL); //$NON-NLS-1$
         {

            sb.append("<div class='blog-item'>"); //$NON-NLS-1$
            {
               /*
                * Tour title
                */
               if (isTitle == false) {

                  tourTitle = "&nbsp;"; //$NON-NLS-1$

               } else {

                  if (UI.IS_SCRAMBLE_DATA) {
                     tourTitle = UI.scrambleText(tourTitle);
                  }
               }

               final String hoverEdit = NLS.bind(Messages.Tour_Blog_Action_EditTour_Tooltip, tourTitle);

               final String hrefEditTour = HTTP_DUMMY + HREF_EDIT_TOUR;

               sb.append(UI.EMPTY_STRING +

                     ("<div class='action-container'>" //                           //$NON-NLS-1$
                           + ("<a class='action' style='background: url(" //        //$NON-NLS-1$
                                 + _imageUrl_ActionEdit
                                 + ") no-repeat;'" //                               //$NON-NLS-1$
                                 + " href='" + hrefEditTour + "'" //                //$NON-NLS-1$ //$NON-NLS-2$
                                 + " title='" + hoverEdit + "'" //                  //$NON-NLS-1$ //$NON-NLS-2$
                                 + ">" //                                           //$NON-NLS-1$
                                 + "</a>") //                                       //$NON-NLS-1$
                           + "   </div>" + NL) //                                   //$NON-NLS-1$
                     + ("<span class='blog-title'>" + tourTitle + "</span>" + NL)); //$NON-NLS-1$ //$NON-NLS-2$

               /*
                * Description
                */
               if (isDescription) {

                  if (UI.IS_SCRAMBLE_DATA) {
                     tourDescription = UI.scrambleText(tourDescription);
                  }

                  sb.append("<p class='description'>" + WEB.convertHTML_LineBreaks(tourDescription) + "</p>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
               }

               /*
                * Weather
                */
               if (isWeather) {

                  if (UI.IS_SCRAMBLE_DATA) {
                     tourWeather = UI.scrambleText(tourWeather);
                  }

                  if (isDescription) {
                     // write spacer
                     sb.append("<div>&nbsp;</div>");//$NON-NLS-1$
                  }

                  sb.append("<div class='title'>" + Messages.tour_editor_section_weather + "</div>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
                  sb.append("<p class='description'>" + WEB.convertHTML_LineBreaks(tourWeather) + "</p>" + NL); //$NON-NLS-1$ //$NON-NLS-2$
               }
            }
            sb.append("</div>" + NL); //$NON-NLS-1$
         }
         sb.append("</div>" + NL); //$NON-NLS-1$

      } else {

         // there is no tour header, set some spacing

         sb.append("<div style='margin-top:20px;'></div>" + NL); //$NON-NLS-1$
      }
   }

   /**
    * Label
    */
   private void create_30_Marker(final StringBuilder sb, final TourMarker tourMarker) {

      final long markerId = tourMarker.getMarkerId();
      String markerLabel = tourMarker.getLabel();

      if (UI.IS_SCRAMBLE_DATA) {
         markerLabel = UI.scrambleText(markerLabel);
      }

      final String hrefOpenMarker = HTTP_DUMMY + HREF_OPEN_MARKER + markerId;
      final String hrefEditMarker = HTTP_DUMMY + HREF_EDIT_MARKER + markerId;
      final String hrefHideMarker = HTTP_DUMMY + HREF_HIDE_MARKER + markerId;
      final String hrefShowMarker = HTTP_DUMMY + HREF_SHOW_MARKER + markerId;

      final String hoverEditMarker = NLS.bind(Messages.Tour_Blog_Action_EditMarker_Tooltip, markerLabel);
      final String hoverHideMarker = NLS.bind(Messages.Tour_Blog_Action_HideMarker_Tooltip, markerLabel);
      final String hoverOpenMarker = NLS.bind(Messages.Tour_Blog_Action_OpenMarker_Tooltip, markerLabel);
      final String hoverShowMarker = NLS.bind(Messages.Tour_Blog_Action_ShowMarker_Tooltip, markerLabel);

      /*
       * get color by priority
       */
      String cssMarkerColor;

      if (_isDrawWithDefaultColor) {

         // force default color
         cssMarkerColor = _cssMarker_DefaultColor;

      } else if (tourMarker.isMarkerVisible() == false) {

         // show hidden color
         cssMarkerColor = _cssMarker_HiddenColor;

      } else if (tourMarker.isDeviceMarker()) {

         // show with device color
         cssMarkerColor = _cssMarker_DeviceColor;

      } else {

         cssMarkerColor = _cssMarker_DefaultColor;
      }

      final String htmlMarkerStyle = " style='color:" + cssMarkerColor + "'"; //$NON-NLS-1$ //$NON-NLS-2$

      final String htmlActionShowHideMarker = tourMarker.isMarkerVisible() //
            ? createHtml_Action(hrefHideMarker, hoverHideMarker, _imageUrl_ActionHideMarker)
            : createHtml_Action(hrefShowMarker, hoverShowMarker, _imageUrl_ActionShowMarker);

      final String htmlActionContainer = UI.EMPTY_STRING //
            + "<div class='action-container'>" //$NON-NLS-1$
            + ("<table><tbody><tr>") //$NON-NLS-1$
            + ("<td>" + htmlActionShowHideMarker + "</td>") //$NON-NLS-1$ //$NON-NLS-2$
            + ("<td>" + createHtml_Action(hrefEditMarker, hoverEditMarker, _imageUrl_ActionEdit) + "</td>") //$NON-NLS-1$ //$NON-NLS-2$
            + "</tr></tbody></table>" // //$NON-NLS-1$
            + "</div>" + NL; //$NON-NLS-1$

      sb.append("<div class='title'>" + NL //$NON-NLS-1$

            + htmlActionContainer

            + ("<a class='label-text'" //$NON-NLS-1$
                  + htmlMarkerStyle
                  + (" href='" + hrefOpenMarker + "'") //$NON-NLS-1$ //$NON-NLS-2$
                  + (" name='" + createHtml_MarkerName(markerId) + "'") //$NON-NLS-1$ //$NON-NLS-2$
                  + (" title='" + hoverOpenMarker + "'") //$NON-NLS-1$ //$NON-NLS-2$
                  + ">" + markerLabel + "</a>" + NL) //$NON-NLS-1$ //$NON-NLS-2$

            + "</div>" + NL); //$NON-NLS-1$
      /*
       * Description
       */
      final String description = tourMarker.getDescription();
      String descriptionWithLineBreaks = WEB.convertHTML_LineBreaks(description);

      if (UI.IS_SCRAMBLE_DATA) {
         descriptionWithLineBreaks = UI.scrambleText(descriptionWithLineBreaks);
      }

      sb.append("<a class='label-text' href='" + hrefOpenMarker + "' title='" + hoverOpenMarker + "'>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      sb.append("   <p class='description'" + htmlMarkerStyle + ">" + descriptionWithLineBreaks + "</p>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      sb.append("</a>" + NL); //$NON-NLS-1$
   }

   /**
    * Url
    */
   private void create_32_MarkerUrl(final StringBuilder sb, final TourMarker tourMarker) {

      final String urlText = tourMarker.getUrlText();
      final String urlAddress = tourMarker.getUrlAddress();
      final boolean isText = urlText.length() > 0;
      final boolean isAddress = urlAddress.length() > 0;

      if (isText || isAddress) {

         String linkText;

         if (isAddress == false) {

            // only text is in the link -> this is not a internet address but create a link of it

            linkText = "<a href='" + urlText + "' title='" + urlText + "'>" + urlText + "</a>" + NL; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

         } else if (isText == false) {

            linkText = "<a href='" + urlAddress + "' title='" + urlAddress + "'>" + urlAddress + "</a>" + NL; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

         } else {

            linkText = "<a href='" + urlAddress + "' title='" + urlAddress + "'>" + urlText + "</a>" + NL; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
         }

         sb.append(linkText);
      }
   }

   private void createActions() {

      _actionTourBlogOptions = new ActionTourBlogOptions();

      fillActionBars();
   }

   private String createHtml_Action(final String hrefMarker, final String hoverMarker, final String backgroundImage) {

      return "<a class='action'" // //$NON-NLS-1$
            + " style='background-image: url(" + backgroundImage + ");'" //$NON-NLS-1$ //$NON-NLS-2$
            + " href='" + hrefMarker + "'" //$NON-NLS-1$ //$NON-NLS-2$
            + " title='" + hoverMarker + "'" //$NON-NLS-1$ //$NON-NLS-2$
            + ">" //$NON-NLS-1$
            + "</a>"; //$NON-NLS-1$
   }

   private String createHtml_MarkerName(final long markerId) {

      return HREF_MARKER_ITEM + markerId;
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      initUI();

      createUI(parent);
      createActions();

      addSelectionListener();
      addTourEventListener();
      addPrefListener();
      addPartListener();

      showInvalidPage();

      // this part is a selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      // show markers from last selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_tourData == null) {
         showTourFromTourProvider();
      }
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _pageNoBrowser = new Composite(_pageBook, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageNoBrowser);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageNoBrowser);
      _pageNoBrowser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
      {
         _txtNoBrowser = new Text(_pageNoBrowser, SWT.WRAP | SWT.READ_ONLY);
         GridDataFactory.fillDefaults()//
               .grab(true, true)
               .align(SWT.FILL, SWT.BEGINNING)
               .applyTo(_txtNoBrowser);
         _txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
      }

      _pageContent = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_pageContent);
      {
         createUI_10_Browser(_pageContent);
      }
   }

   private void createUI_10_Browser(final Composite parent) {

      try {

         try {

            // use default browser
            _browser = new Browser(parent, SWT.NONE);

         } catch (final Exception e) {

            // use WebKit browser for Linux when default browser fails
            _browser = new Browser(parent, SWT.WEBKIT);
         }

         GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

         _browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(final LocationEvent event) {
               onBrowserLocationChanging(event);
            }
         });

         _browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(final ProgressEvent event) {
               onBrowserCompleted(event);
            }
         });

      } catch (final SWTError e) {

         _txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
      }
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionTourBlogOptions);
   }

   private void fireMarkerPosition(final StructuredSelection selection) {

      final Object[] selectedMarker = selection.toArray();

      if (selectedMarker.length > 0) {

         final ArrayList<TourMarker> allTourMarker = new ArrayList<>();

         for (final Object object : selectedMarker) {
            allTourMarker.add((TourMarker) object);
         }

         _postSelectionProvider.setSelection(new SelectionTourMarker(_tourData, allTourMarker));
      }
   }

   private void hrefActionEditMarker(final TourMarker selectedTourMarker) {

      if (_tourData.isManualTour()) {
         // a manually created tour do not have time slices -> no markers
         return;
      }

      final DialogMarker markerDialog = new DialogMarker(
            Display.getCurrent().getActiveShell(),
            _tourData,
            selectedTourMarker);

      if (markerDialog.open() == Window.OK) {
         saveModifiedTour();
      }
   }

   private void hrefActionEditTour() {

      if (new DialogQuickEdit(//
            Display.getCurrent().getActiveShell(),
            _tourData).open() == Window.OK) {

         saveModifiedTour();
      }
   }

   private void hrefActionHideMarker(final TourMarker selectedTourMarker) {

      selectedTourMarker.setMarkerVisible(false);

      prepareBrowserReload(selectedTourMarker);

      saveModifiedTour();
   }

   /**
    * Fire a selection for the selected marker(s).
    */
   private void hrefActionOpenMarker(final StructuredSelection selection) {

      // a chart must be available
      if (_tourChart == null) {

         final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

         if ((tourChart == null) || tourChart.isDisposed()) {

            fireMarkerPosition(selection);

            return;

         } else {
            _tourChart = tourChart;
         }
      }

      final Object[] selectedMarker = selection.toArray();

      if (selectedMarker.length > 1) {

         // two or more markers are selected

         _postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
               _tourChart,
               ((TourMarker) selectedMarker[0]).getSerieIndex(),
               ((TourMarker) selectedMarker[selectedMarker.length - 1]).getSerieIndex()));

      } else if (selectedMarker.length > 0) {

         // one marker is selected

         _postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
               _tourChart,
               ((TourMarker) selectedMarker[0]).getSerieIndex(),
               SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
      }
   }

   private void hrefActionShowMarker(final TourMarker selectedTourMarker) {

      selectedTourMarker.setMarkerVisible(true);

      prepareBrowserReload(selectedTourMarker);

      saveModifiedTour();
   }

   private void initUI() {

      try {

         /*
          * load css from file
          */
         final File cssFile = WEB.getFile(TOUR_BLOG_CSS);
         final String cssContent = Util.readContentFromFile(cssFile.getAbsolutePath());

         _htmlCss = "<style>" + cssContent + "</style>"; //$NON-NLS-1$ //$NON-NLS-2$

         /*
          * set image urls
          */
         _imageUrl_ActionEdit = net.tourbook.ui.UI.getIconUrl(ThemeUtil.getThemedImageName(Images.App_Edit));
         _imageUrl_ActionHideMarker = net.tourbook.ui.UI.getIconUrl(ThemeUtil.getThemedImageName(Images.App_Hide));
         _imageUrl_ActionShowMarker = net.tourbook.ui.UI.getIconUrl(ThemeUtil.getThemedImageName(Images.App_Show));

      } catch (final IOException | URISyntaxException e) {
         StatusUtil.showStatus(e);
      }

   }

   private void onBrowserCompleted(final ProgressEvent event) {

      if (_reloadedTourMarkerId == null) {
         return;
      }

      // get local copy
      final long reloadedTourMarkerId = _reloadedTourMarkerId;

      /*
       * This must be run async otherwise an endless loop will happen
       */
      _browser.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            final String href = "location.href='" + createHtml_MarkerName(reloadedTourMarkerId) + "'"; //$NON-NLS-1$ //$NON-NLS-2$

            _browser.execute(href);
         }
      });

      _reloadedTourMarkerId = null;
   }

   private void onBrowserLocationChanging(final LocationEvent event) {

      final String location = event.location;

      if (location.contains(HREF_MARKER_ITEM)) {

         /*
          * Page is reloaded and is scrolled to the tour marker where the last tour marker action is
          * done.
          */

         _browser.setRedraw(true);

         return;
      }

      final String[] locationParts = location.split(HREF_TOKEN);

      if (locationParts.length == 3) {

         // a tour marker id is selected, fire tour marker selection

         try {

            /**
             * Split location<br>
             * Part 1: location, e.g. "about"<br>
             * Part 2: action<br>
             * Part 3: markerID
             */
            final String markerIdText = locationParts[2];
            final long markerId = Long.parseLong(markerIdText);

            // get tour marker by id
            TourMarker hrefTourMarker = null;
            for (final TourMarker tourMarker : _tourData.getTourMarkers()) {
               if (tourMarker.getMarkerId() == markerId) {
                  hrefTourMarker = tourMarker;
                  break;
               }
            }

            final String action = locationParts[1];

            if (hrefTourMarker != null) {

               switch (action) {
               case ACTION_EDIT_MARKER:
                  hrefActionEditMarker(hrefTourMarker);
                  break;

               case ACTION_HIDE_MARKER:
                  hrefActionHideMarker(hrefTourMarker);
                  break;

               case ACTION_OPEN_MARKER:
                  hrefActionOpenMarker(new StructuredSelection(hrefTourMarker));
                  break;

               case ACTION_SHOW_MARKER:
                  hrefActionShowMarker(hrefTourMarker);
                  break;
               }
            }

         } catch (final Exception e) {
            // ignore
         }

      } else if (locationParts.length == 2) {

         final String action = locationParts[1];

         switch (action) {
         case ACTION_EDIT_TOUR:
            hrefActionEditTour();
            break;
         }

      } else if (location.startsWith(HTTP_DUMMY) == false && location.startsWith(EXTERNAL_LINK_URL)) {

         // open link in the external browser

         // check if this is a valid web url and not any other protocol
         WEB.openUrl(location);
      }

      if (location.equals(PAGE_ABOUT_BLANK) == false) {

         // about:blank is the initial page

         event.doit = false;
      }
   }

   private void onSelectionChanged(final ISelection selection) {

      long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

      if (selection instanceof SelectionTourData) {

         // a tour was selected, get the chart and update the marker viewer

         final SelectionTourData tourDataSelection = (SelectionTourData) selection;
         _tourData = tourDataSelection.getTourData();

         if (_tourData == null) {
            _tourChart = null;
         } else {
            _tourChart = tourDataSelection.getTourChart();
            tourId = _tourData.getTourId();
         }

      } else if (selection instanceof SelectionTourId) {

         _tourChart = null;
         tourId = ((SelectionTourId) selection).getTourId();

      } else if (selection instanceof SelectionTourIds) {

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
         if ((tourIds != null) && (tourIds.size() > 0)) {
            _tourChart = null;
            tourId = tourIds.get(0);
         }

      } else if (selection instanceof SelectionTourCatalogView) {

         final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

         final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
         if (refItem != null) {
            _tourChart = null;
            tourId = refItem.getTourId();
         }

      } else if (selection instanceof StructuredSelection) {

         _tourChart = null;
         final Object firstElement = ((StructuredSelection) selection).getFirstElement();
         if (firstElement instanceof TVICatalogComparedTour) {
            tourId = ((TVICatalogComparedTour) firstElement).getTourId();
         } else if (firstElement instanceof TVICompareResultComparedTour) {
            tourId = ((TVICompareResultComparedTour) firstElement).getTourId();
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }

      if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

         final TourData tourData = TourManager.getInstance().getTourData(tourId);
         if (tourData != null) {
            _tourData = tourData;
         }
      }

      final boolean isTourAvailable = (tourId >= 0) && (_tourData != null);
      if (isTourAvailable && _browser != null) {
         updateUI();
      }
   }

   /**
    * Keeps the current browser scroll position.
    *
    * @param tourMarker
    */
   private void prepareBrowserReload(final TourMarker tourMarker) {

      _reloadedTourMarkerId = tourMarker.getMarkerId();

      _browser.setRedraw(false);
   }

   private void saveModifiedTour() {

      /*
       * Run async because a tour save will fire a tour change event.
       */
      _parent.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {
            TourManager.saveModifiedTour(_tourData);
         }
      });
   }

   @Override
   public void setFocus() {

   }

   private void showInvalidPage() {

      _pageBook.showPage(_browser == null ? _pageNoBrowser : _pageNoData);
   }

   private void showTourFromTourProvider() {

      showInvalidPage();

      // a tour is not displayed, find a tour provider which provides a tour
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            // validate widget
            if (_pageBook.isDisposed()) {
               return;
            }

            /*
             * check if tour was set from a selection provider
             */
            if (_tourData != null) {
               return;
            }

            final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

            if ((selectedTours != null) && (selectedTours.size() > 0)) {
               onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
            }
         }
      });
   }

   /**
    * Update the UI from {@link #_tourData}.
    */
   void updateUI() {

      if (_tourData == null || _browser == null) {
         return;
      }

      _pageBook.showPage(_pageContent);

      _isDrawWithDefaultColor = _state.getBoolean(STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR);
      _isShowHiddenMarker = _state.getBoolean(STATE_IS_SHOW_HIDDEN_MARKER);

      final String graphMarker_ColorDefault = UI.IS_DARK_THEME
            ? ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT_DARK
            : ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT;

      final String graphMarker_ColorHidden = UI.IS_DARK_THEME
            ? ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN_DARK
            : ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN;

      final String graphMarker_ColorDevice = UI.IS_DARK_THEME
            ? ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE_DARK
            : ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE;

      _cssMarker_DefaultColor = CSS.color(PreferenceConverter.getColor(_prefStore, graphMarker_ColorDefault));
      _cssMarker_HiddenColor = CSS.color(PreferenceConverter.getColor(_prefStore, graphMarker_ColorHidden));
      _cssMarker_DeviceColor = CSS.color(PreferenceConverter.getColor(_prefStore, graphMarker_ColorDevice));

//      Force Internet Explorer to not use compatibility mode. Internet Explorer believes that websites under
//      several domains (including "ibm.com") require compatibility mode. You may see your web application run
//      normally under "localhost", but then fail when hosted under another domain (e.g.: "ibm.com").
//      Setting "IE=Edge" will force the latest standards mode for the version of Internet Explorer being used.
//      This is supported for Internet Explorer 8 and later. You can also ease your testing efforts by forcing
//      specific versions of Internet Explorer to render using the standards mode of previous versions. This
//      prevents you from exploiting the latest features, but may offer you compatibility and stability. Lookup
//      the online documentation for the "X-UA-Compatible" META tag to find which value is right for you.

      final String html = UI.EMPTY_STRING
            + "<!DOCTYPE html>" + NL // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
            + "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>" + NL //$NON-NLS-1$
            + "<head>" + NL + create_10_Head() + NL + "</head>" + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "<body>" + NL + create_20_Body() + NL + "</body>" + NL //$NON-NLS-1$ //$NON-NLS-2$
            + "</html>"; //$NON-NLS-1$

      _browser.setRedraw(true);
      _browser.setText(html);
   }
}
