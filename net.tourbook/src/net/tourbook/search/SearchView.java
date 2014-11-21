/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SearchView extends ViewPart {

	public static final String			ID										= "net.tourbook.search.SearchView";					//$NON-NLS-1$

	private static final String			IMAGE_ACTION_TOUR_WAY_POINT				= net.tourbook.map2.Messages.Image_Action_TourWayPoint;

	private static final String			SEARCH_RESULT_CSS_FILE					= "/html/search-result.css";							//$NON-NLS-1$

	static final String					STATE_IS_SHOW_DATE_TIME					= "STATE_IS_SHOW_DATE_TIME";							//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_DATE_TIME_DEFAULT			= false;
	static final String					STATE_IS_SHOW_ITEM_NUMBER				= "STATE_IS_SHOW_ITEM_NUMBER";							//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_ITEM_NUMBER_DEFAULT		= false;
	static final String					STATE_IS_SHOW_LUCENE_DOC_ID				= "STATE_IS_SHOW_LUCENE_DOC_ID";						//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT		= false;
//	static final String					STATE_IS_SHOW_SCORE						= "STATE_IS_SHOW_SCORE";			//$NON-NLS-1$
//	static final boolean				STATE_IS_SHOW_SCORE_DEFAULT				= false;
	static final String					STATE_IS_SHOW_TOP_NAVIGATOR				= "STATE_IS_SHOW_TOP_NAVIGATOR";						//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_TOP_NAVIGATOR_DEFAULT		= false;
	static final String					STATE_IS_SORT_DATE_ASCENDING			= "STATE_IS_SORT_DATE_ASCENDING";						//$NON-NLS-1$
	static final boolean				STATE_IS_SORT_DATE_ASCENDING_DEFAULT	= false;
	static final String					STATE_HITS_PER_PAGE						= "STATE_HITS_PER_PAGE";								//$NON-NLS-1$
	static final int					STATE_HITS_PER_PAGE_DEFAULT				= 10;
	static final String					STATE_NUMBER_OF_PAGES					= "STATE_NUMBER_OF_PAGES";								//$NON-NLS-1$
	static final int					STATE_NUMBER_OF_PAGES_DEFAULT			= 3;

	private static final String			STATE_POPUP_WIDTH						= "STATE_POPUP_WIDTH";									//$NON-NLS-1$
	private static final String			STATE_POPUP_HEIGHT						= "STATE_POPUP_HEIGHT";								//$NON-NLS-1$
	private static final String			STATE_SEARCH_TEXT						= "STATE_SEARCH_TEXT";									//$NON-NLS-1$

	private static final String			TAG_TD									= "<td>";
	private static final String			TAG_TD_END								= "</td>";

	private static final String			CSS_HOVER_CONTAINER						= "hover-container";									//$NON-NLS-1$
	private static final String			CSS_SELECTED							= "selected";											//$NON-NLS-1$

	private static final String			PAGE_ABOUT_BLANK						= "about:blank";										//$NON-NLS-1$

	/**
	 * This is necessary otherwise XULrunner in Linux do not fire a location change event.
	 */
	private static final String			HTTP_PROTOCOL							= "http://dummy/a?";									//$NON-NLS-1$

	private static final String			HREF_TOKEN								= "&";													//$NON-NLS-1$
	private static final String			HREF_VALUE_SEP							= "=";													//$NON-NLS-1$

	private static final String			PARAM_ACTION							= "action";											//$NON-NLS-1$
	private static final String			PARAM_DOC_ID							= "docId";												//$NON-NLS-1$
	private static final String			PARAM_MARKER_ID							= "markerId";											//$NON-NLS-1$
	private static final String			PARAM_PAGE								= "page";												//$NON-NLS-1$
	private static final String			PARAM_TOUR_ID							= "tourId";											//$NON-NLS-1$

	private static final String			ACTION_EDIT_MARKER						= "EditMarker";										//$NON-NLS-1$
	private static final String			ACTION_EDIT_TOUR						= "EditTour";											//$NON-NLS-1$
	private static final String			ACTION_NAVIGATE_PAGE					= "NavigatePage";										//$NON-NLS-1$
	private static final String			ACTION_SELECT_TOUR						= "SelectTour";										//$NON-NLS-1$
	private static final String			ACTION_SELECT_MARKER					= "SelectMarker";										//$NON-NLS-1$
	private static final String			ACTION_SELECT_WAY_POINT					= "SelectWayPoint";									//$NON-NLS-1$

	private static String				HREF_ACTION_EDIT_MARKER;
	private static String				HREF_ACTION_EDIT_TOUR;
	private static String				HREF_ACTION_NAVIGATE_PAGE;
	private static String				HREF_ACTION_SELECT_TOUR;
	private static String				HREF_ACTION_SELECT_MARKER;
	private static final String			HREF_ACTION_SELECT_WAY_POINT;

	private static String				HREF_PARAM_DOC_ID;
	private static String				HREF_PARAM_MARKER_ID;
	private static String				HREF_PARAM_PAGE;
	private static String				HREF_PARAM_TOUR_ID;

	static {

		// e.g. ...&action=EditMarker...

		final String HREF_ACTION = HREF_TOKEN + PARAM_ACTION + HREF_VALUE_SEP;

		HREF_ACTION_EDIT_MARKER = HREF_ACTION + ACTION_EDIT_MARKER;
		HREF_ACTION_EDIT_TOUR = HREF_ACTION + ACTION_EDIT_TOUR;
		HREF_ACTION_NAVIGATE_PAGE = HREF_ACTION + ACTION_NAVIGATE_PAGE;
		HREF_ACTION_SELECT_TOUR = HREF_ACTION + ACTION_SELECT_TOUR;
		HREF_ACTION_SELECT_MARKER = HREF_ACTION + ACTION_SELECT_MARKER;
		HREF_ACTION_SELECT_WAY_POINT = HREF_ACTION + ACTION_SELECT_WAY_POINT;

		HREF_PARAM_DOC_ID = HREF_TOKEN + PARAM_DOC_ID + HREF_VALUE_SEP;
		HREF_PARAM_MARKER_ID = HREF_TOKEN + PARAM_MARKER_ID + HREF_VALUE_SEP;
		HREF_PARAM_TOUR_ID = HREF_TOKEN + PARAM_TOUR_ID + HREF_VALUE_SEP;
		HREF_PARAM_PAGE = HREF_TOKEN + PARAM_PAGE + HREF_VALUE_SEP;
	}

	private final DateTimeFormatter		_dateFormatter							= DateTimeFormat.mediumDate();

	private final IPreferenceStore		_prefStore								= TourbookPlugin.getPrefStore();
	private final IDialogSettings		_state									= TourbookPlugin.getState(ID);
	//
	private PostSelectionProvider		_postSelectionProvider;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;
	private IPartListener2				_partListener;
	//
	private String						_actionUrl_EditImage;
	private String						_htmlCss;
	private String						_iconUrl_Tour;
	private String						_iconUrl_Marker;
	private String						_iconUrl_WayPoint;

	private MTContentProposalAdapter	_contentProposalAdapter;
	private TextContentAdapter			_controlContentAdapter					= new TextContentAdapter();
	private MTProposalProvider			_proposalProvider						= new MTProposalProvider();

	private ActionSearchOptions			_actionTourBlogMarker;

	private boolean						_isBrowserLoadingCompleted;
	private boolean						_isUIShowDateTime;
	private boolean						_isUIShowItemNumber;
	private boolean						_isUIShowLuceneDocId;
//	private boolean						_isUIShowScore;
	private boolean						_isUIShowTopNavigator;

	private int							_hitsPerPage;
	private int							_numberOfPages;

	private SearchResult				_searchResult;
	private long						_searchTime								= -1;

	/**
	 * Lucene doc id for a selected document in the UI, otherwise it's <code>-1</code>.
	 */
	private int							_selectedDocId							= -1;
	private int							_previousDocId							= -1;

	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private Browser						_browser;

	private Composite					_pageNoBrowser;
	private Composite					_pageSearch;
	private Composite					_uiParent;

	private PageBook					_pageBook;

	private Text						_txtNoBrowser;
	private Text						_txtSearch;

	public class MTContentProposalAdapter extends ContentProposalAdapter {

		/**
		 * @param control
		 * @param controlContentAdapter
		 * @param proposalProvider
		 * @param keyStroke
		 * @param autoActivationCharacters
		 */
		public MTContentProposalAdapter(final Control control,
										final IControlContentAdapter controlContentAdapter,
										final IContentProposalProvider proposalProvider,
										final KeyStroke keyStroke,
										final char[] autoActivationCharacters) {

			super(control, controlContentAdapter, proposalProvider, keyStroke, autoActivationCharacters);
		}

		/**
		 * Open popup with {@link SWT#ARROW_DOWN} key.
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		protected void openProposalPopup() {
			super.openProposalPopup();
		}
	}

	/**
	 * Copied from {@link org.eclipse.jdt.internal.ui.dialogs.GenerateToStringDialog} and adjusted.
	 */
	private class MTProposalProvider implements IContentProposalProvider {

		private String	latestContents;
		private int		latestPosition;

		private class Proposal implements IContentProposal {

			final private String	proposal;
			private int				position;

			public Proposal(final String proposal) {
				this.proposal = proposal;
				this.position = proposal.length();
			}

			public String getContent() {

				final int overlap = stringOverlap(latestContents.substring(0, latestPosition), proposal);

				position = proposal.length() - overlap;

				return proposal.substring(overlap);
			}

			public int getCursorPosition() {
				return position;
			}

			public String getDescription() {
//				return parser.getVariableDescriptions().get(proposal);
				return null;
			}

			public String getLabel() {
				return proposal;
			}
		}

		public IContentProposal[] getProposals(final String contents, final int position) {

			final List<Proposal> primaryProposals = new ArrayList<Proposal>();
			final List<Proposal> secondaryProposals = new ArrayList<Proposal>();

			final List<LookupResult> proposals = FTSearchManager.getProposals(contents, position);

			final String contentToCursor = contents.substring(0, position);

			for (final LookupResult lookupResult : proposals) {

				final String proposalString = lookupResult.key.toString();

				if (stringOverlap(contentToCursor, proposalString) > 0) {
					primaryProposals.add(new Proposal(proposalString));
				} else {
					secondaryProposals.add(new Proposal(proposalString));
				}
			}

			this.latestContents = contents;
			this.latestPosition = position;

			primaryProposals.addAll(secondaryProposals);

			return primaryProposals.toArray(new IContentProposal[0]);
		}

		/**
		 * Checks if the end of the first string is equal to the beginning of of the second string.
		 * 
		 * @param s1
		 *            first String
		 * @param s2
		 *            second String
		 * @return length of overlapping segment (0 if strings don't overlap)
		 */
		private int stringOverlap(final String s1, final String s2) {

			final int l1 = s1.length();

			for (int l = 1; l <= Math.min(s1.length(), s2.length()); l++) {

				boolean ok = true;

				for (int i = 0; i < l; i++) {
					if (s1.charAt(l1 - l + i) != s2.charAt(i)) {
						ok = false;
						break;
					}
				}

				if (ok) {
					return l;
				}
			}

			return 0;
		}
	}

	void actionOpenSearchView() {

		// force focus, but there are still possibilities, that the focus is not set into the search control.
		setFocus();

		if (_txtSearch.getSelectionCount() == 0) {

			// nothing is selected -> select all
			_txtSearch.selectAll();

		} else {

			// something is selected -> select nothing and move cursor to the end

			_txtSearch.setSelection(_txtSearch.getText().length());
		}

	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == SearchView.this) {

					saveState();

					/**
					 * Close ft index that it will be created each time when the index is opened.
					 */
					FTSearchManager.close();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == SearchView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						for (final TourData tourData : modifiedTours) {

						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void appendState(final StringBuilder state, final String text) {

		if (state.length() > 0) {
			state.append(UI.DASH_WITH_SPACE);
		}

		state.append(text);
	}

	private void clearView() {

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();
	}

	private void createActions() {

		_actionTourBlogMarker = new ActionSearchOptions(this, _uiParent);

		fillActionBars();
	}

	private String createHTML(final SearchResult searchResult, final StringBuilder stateText) {

		final String html = "" // //$NON-NLS-1$
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
				+ "<html>\n" //$NON-NLS-1$
				+ ("<head>\n" + createHTML_10_Head() + "\n</head>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("<body>\n" + createHTML_20_Body(searchResult, stateText.toString()) + "\n</body>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</html>"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _htmlCss
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	/**
	 * @param searchResult
	 *            Can be <code>null</code> when search result is not available.
	 * @param stateText
	 * @return
	 */
	private String createHTML_20_Body(final SearchResult searchResult, final String stateText) {

		final StringBuilder sb = new StringBuilder();
		final StringBuilder sbNavigator = new StringBuilder();

		if (searchResult != null) {

			createHTML_40_PageNavigator(sbNavigator, searchResult);

			if (_isUIShowTopNavigator) {
				sb.append("<div style='padding-top:3px;'></div>");
				sb.append(sbNavigator);
				sb.append("<div style='padding-top:3px;'></div>");
			}
		}

		if (searchResult != null) {

			final int itemBaseNumber = searchResult.pageNumber * searchResult.hitsPerPage;

			int itemIndex = 0;

			for (final SearchResultItem entry : searchResult.items) {

				final int itemNumber = ++itemIndex + itemBaseNumber;

				String selectedItemClass = UI.EMPTY_STRING;

				if (_selectedDocId != -1 && _selectedDocId == entry.docId) {
					selectedItemClass = UI.SPACE1 + CSS_SELECTED;
				}

				sb.append("<div"
						+ (" class='" + CSS_HOVER_CONTAINER + selectedItemClass + "'")
						+ " id='" + entry.docId + "'>\n"); //$NON-NLS-1$
				{
					createHTML_30_Item(sb, entry, itemNumber);
				}
				sb.append("</div>\n"); //$NON-NLS-1$

			}
		}

		if (searchResult != null) {
			sb.append("<div style='padding-top:3px;'></div>");
			sb.append(sbNavigator);
			sb.append("<div style='padding-top:3px;'></div>");
		}

		// state
		{
			sb.append("<div class='result-state'>"); //$NON-NLS-1$
			{
				sb.append(stateText);
			}
			sb.append("</div>\n"); //$NON-NLS-1$
		}

		return sb.toString();
	}

	private void createHTML_30_Item(final StringBuilder sb, final SearchResultItem resultItem, final int itemNumber) {

		final int docId = resultItem.docId;
		final int docSource = resultItem.docSource;
		final String markerId = resultItem.markerId;
		final String tourId = resultItem.tourId;

		final boolean isTour = docSource == FTSearchManager.DOC_SOURCE_TOUR;
		final boolean isMarker = docSource == FTSearchManager.DOC_SOURCE_TOUR_MARKER;
		final boolean isWayPoint = docSource == FTSearchManager.DOC_SOURCE_WAY_POINT;

		String iconUrl = null;

		String hoverMessage = null;
		String hrefEditItem = null;
		String hrefSelectItem = null;
		String itemTitleText = null;

		if (isTour) {

			final String tourTitle = resultItem.title;
			if (tourTitle != null) {
				itemTitleText = tourTitle;
			}

			hrefSelectItem = HTTP_PROTOCOL
					+ HREF_ACTION_SELECT_TOUR
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_DOC_ID + docId);

			hrefEditItem = HTTP_PROTOCOL
					+ HREF_ACTION_EDIT_TOUR
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_DOC_ID + docId);

			iconUrl = _iconUrl_Tour;
			hoverMessage = Messages.Search_View_Action_EditTour_Tooltip;

		} else if (isMarker) {

			final String tourMarkerLabel = resultItem.title;
			if (tourMarkerLabel != null) {
				itemTitleText = tourMarkerLabel;
			}

			hrefSelectItem = HTTP_PROTOCOL
					+ HREF_ACTION_SELECT_MARKER
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_MARKER_ID + markerId)
					+ (HREF_PARAM_DOC_ID + docId);

			hrefEditItem = HTTP_PROTOCOL
					+ HREF_ACTION_EDIT_MARKER
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_MARKER_ID + markerId)
					+ (HREF_PARAM_DOC_ID + docId);

			iconUrl = _iconUrl_Marker;
			hoverMessage = Messages.Search_View_Action_EditMarker_Tooltip;

		} else if (isWayPoint) {

			final String tourMarkerLabel = resultItem.title;
			if (tourMarkerLabel != null) {
				itemTitleText = tourMarkerLabel;
			}

			hrefSelectItem = HTTP_PROTOCOL
					+ HREF_ACTION_SELECT_WAY_POINT
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_MARKER_ID + markerId)
					+ (HREF_PARAM_DOC_ID + docId);

			iconUrl = _iconUrl_WayPoint;
			hoverMessage = Messages.Search_View_Action_EditMarker_Tooltip;
		}

		if (itemTitleText == null) {
			itemTitleText = UI.EMPTY_STRING;
		}

		String itemTitle = itemTitleText;
		if (itemTitle.length() == 0) {
			// show new line that the icon is not overwritten
			itemTitle = "</br>";
		}

		final String description = resultItem.description;
		final boolean isDescription = description != null;

		// hovered actions
		if (hrefEditItem != null) {

			sb.append("<div class='action-container'>" //$NON-NLS-1$
					+ ("<table><tbody><tr>") //$NON-NLS-1$
					+ (TAG_TD + createHtml_Action(hrefEditItem, hoverMessage, _actionUrl_EditImage) + TAG_TD_END)
					+ "</tr></tbody></table>" // //$NON-NLS-1$
					+ "</div>\n"); //$NON-NLS-1$
		}

		sb.append("<a class='item'" //
				+ (" href='" + hrefSelectItem + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" title='" + itemTitle + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ ">"); // //$NON-NLS-1$
		{
			sb.append("<table><tbody><tr>");
			{
				/*
				 * Item image
				 */
				sb.append("<td class='item-image'>");
				sb.append("<img src='" + iconUrl + "'></img>");
				sb.append(TAG_TD_END);

				/*
				 * Item content
				 */
				sb.append("<td style='width:100%;'>");
				{
					// title
					if (isDescription) {
						sb.append("<span class='item-title'>" + itemTitle + "</span>");
					} else {
						sb.append("<span class='item-title-no-description'>" + itemTitle + "</span>");
					}

					// description
					if (isDescription) {
						sb.append("<div class='item-description'>"); //$NON-NLS-1$
						sb.append(description);
						sb.append("</div>\n"); //$NON-NLS-1$
					}

					// info
					if (_isUIShowDateTime || _isUIShowItemNumber || /* _isUIShowScore || */_isUIShowLuceneDocId) {

						sb.append("<div class='item-info'>"); //$NON-NLS-1$
						sb.append("<table><tbody><tr>");
						{
							if (_isUIShowDateTime) {

								final long tourStartTime = resultItem.tourStartTime;
								if (tourStartTime != 0) {

									final DateTime dt = new DateTime(tourStartTime);

									sb.append(TAG_TD
											+ String.format("%s", _dateFormatter.print(dt.getMillis()))
											+ TAG_TD_END);

								}
							}

//							if (_isUIShowScore) {
//								sb.append(TAG_TD + String.format("%1.3f", resultItem.score) + TAG_TD_END);
//							}

							if (_isUIShowItemNumber) {
								sb.append(TAG_TD + Integer.toString(itemNumber) + TAG_TD_END);
							}

							if (_isUIShowLuceneDocId) {
								sb.append(TAG_TD + String.format("%d", docId) + TAG_TD_END);
							}
						}
						sb.append("</tr></tbody></table>");
						sb.append("</div>\n"); //$NON-NLS-1$
					}
				}
				sb.append(TAG_TD_END);
			}
			sb.append("</tr></tbody></table>");
		}
		sb.append("</a>");
	}

	private void createHTML_40_PageNavigator(final StringBuilder sb, final SearchResult searchResult) {

		final int totalHits = searchResult.totalHits;
		final int hitsPerPage = searchResult.hitsPerPage;
		final int activePageNumber = searchResult.pageNumber;

		if (totalHits <= hitsPerPage) {
			// paging is not needed
			return;
		}

		final int maxPage = getMaxPage(totalHits, hitsPerPage);

		final int visiblePages = _numberOfPages;
		int pagesBefore = visiblePages / 2;
		int pagesAfter = visiblePages - pagesBefore;

		if (activePageNumber - pagesBefore < 0) {

			final int pagesDiff = activePageNumber - pagesBefore;

			pagesBefore = activePageNumber;
			pagesAfter += -pagesDiff;
		}

		if (activePageNumber + pagesAfter > maxPage) {

			pagesAfter = maxPage - activePageNumber;
			pagesBefore = visiblePages - pagesAfter;
		}

		int firstPage = activePageNumber - pagesBefore;
		int lastPage = activePageNumber + pagesAfter;
		if (firstPage < 0) {
			firstPage = 0;
		}
		if (lastPage > maxPage) {
			lastPage = maxPage;
		}

		final StringBuilder sbPages = new StringBuilder();

		final StringBuilder sbFirst = new StringBuilder();
		final StringBuilder sbLast = new StringBuilder();
		final StringBuilder sbNext = new StringBuilder();
		final StringBuilder sbPrevious = new StringBuilder();

		sbPrevious.append("<td style='width:49%; text-align:center; padding-left:3px;'>");
		sbNext.append("<td style='width:49%; text-align:center; padding-right:3px;'>");

		for (int currentPage = firstPage; currentPage < lastPage; currentPage++) {

			final int visiblePageNo = currentPage + 1;
			final String hrefPage = HTTP_PROTOCOL + HREF_ACTION_NAVIGATE_PAGE + HREF_PARAM_PAGE + currentPage;

			/*
			 * Previous page
			 */
			if (currentPage == activePageNumber - 1) {
				sbPrevious.append("<a class='page-number' href='" + hrefPage + "'><</a>");
			}

			/*
			 * Every page
			 */
			sbPages.append(TAG_TD);
			{
				if (currentPage == activePageNumber) {
					sbPages.append("<div class='page-number page-selected'>" + visiblePageNo + "</div>");
				} else {
					sbPages.append("<a class='page-number' href='" + hrefPage + "'>" + visiblePageNo + "</a>");
				}
			}
			sbPages.append(TAG_TD_END);

			/*
			 * Next page
			 */
			if (currentPage == activePageNumber + 1) {
				sbNext.append("<a class='page-number' href='" + hrefPage + "'>></a>");
			}
		}

		sbNext.append(TAG_TD_END);
		sbPrevious.append(TAG_TD_END);

		/*
		 * first page
		 */
		sbFirst.append(TAG_TD);
		{
			if (firstPage > 0) {
				final String hrefPage = HTTP_PROTOCOL + HREF_ACTION_NAVIGATE_PAGE + HREF_PARAM_PAGE + 0;
				sbFirst.append("<a" + " class='page-number' href='" + hrefPage + "'>1</a>");
			}
		}
		sbFirst.append(TAG_TD_END);
		if (firstPage > 0) {
			sbFirst.append("<td>..</td>");
		}

		/*
		 * last page
		 */
		if (lastPage < maxPage - 0) {
			sbLast.append("<td>..</td>");
		}
		sbLast.append(TAG_TD);
		{
			if (lastPage < maxPage) {
				final String hrefPage = HTTP_PROTOCOL + HREF_ACTION_NAVIGATE_PAGE + HREF_PARAM_PAGE + (maxPage - 1);
				sbLast.append("<a class='page-number' href='" + hrefPage + "'>" + maxPage + "</a>");
			}
		}
		sbLast.append(TAG_TD_END);

		/*
		 * put all together
		 */
		sb.append("<div class='page-navigator'>"); //$NON-NLS-1$
		{
			sb.append("<table><tbody><tr>");
			{
				sb.append(sbPrevious);
				sb.append(sbFirst);
				sb.append(sbPages);
				sb.append(sbLast);
				sb.append(sbNext);
			}
			sb.append("</tr></tbody></table>");
		}
		sb.append("</div>\n"); //$NON-NLS-1$
	}

	private String createHtml_Action(final String hrefMarker, final String hoverMessage, final String backgroundImage) {

		return "<a class='action'" // //$NON-NLS-1$
				+ (" style='background-image: url(" + backgroundImage + ");'") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" href='" + hrefMarker + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" title='" + hoverMessage + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ ">" // //$NON-NLS-1$
				+ "</a>"; //$NON-NLS-1$
	}

	@Override
	public void createPartControl(final Composite parent) {

		_uiParent = parent;

		initUI(parent);

		createUI(parent);
		createActions();

		addTourEventListener();
		addPrefListener();
		addPartListener();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		restoreState();

		showInvalidPage();
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoBrowser = new Composite(_pageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageNoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageNoBrowser);
		{
			_txtNoBrowser = new Text(_pageNoBrowser, SWT.WRAP | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_txtNoBrowser);
		}

		_pageSearch = new Composite(_pageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageSearch);
		GridLayoutFactory.fillDefaults().applyTo(_pageSearch);
		{
			createUI_10_Search(_pageSearch);
		}
	}

	private void createUI_10_Search(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.spacing(0, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		{
			createUI_20_QueryField(container);
			createUI_30_Browser(container);
		}
	}

	private void createUI_20_QueryField(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(0, 2, 2, 2)
//				.margins(0, 0)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		{
// ALTERNATIVE TEXT CONTROL
//tasktop.TextSearchControl
			/*
			 * Text: Search field
			 */
			_txtSearch = new Text(container, SWT.NONE /* SWT.BORDER */);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtSearch);
			_txtSearch.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			_txtSearch.addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(final TraverseEvent e) {

					switch (e.detail) {
					case SWT.TRAVERSE_RETURN:

						startSearch(true, 0);

						// text field can loose the focus
						setFocus();

						break;
					}
				}
			});

			_txtSearch.addListener(SWT.KeyDown, new Listener() {
				public void handleEvent(final Event event) {
					onKeyDown(event);
				}
			});

			_contentProposalAdapter = new MTContentProposalAdapter(//
					_txtSearch,
					_controlContentAdapter,
					_proposalProvider,
					null,
					null);

			_contentProposalAdapter.addContentProposalListener(new IContentProposalListener() {

				@Override
				public void proposalAccepted(final IContentProposal proposal) {

//					System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//							+ ("\tContent: " + proposal.getContent())
//							+ ("\tLabel: " + proposal.getLabel())
//							+ ("\tCursorPosition: " + proposal.getCursorPosition())
//					//
//							);
//					// TODO remove SYSTEM.OUT.PRINTLN

					startSearch(true, 0);

					// text field can loose the focus
					setFocus();
				}
			});

		}
	}

	private void createUI_30_Browser(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

				// DEBUG: force error in win7
//				_browser = new Browser(parent, SWT.MOZILLA);

			} catch (final Exception e) {

				/**
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 * <p>
				 * e.g. for Eclipse 3.8.2
				 * 
				 * <pre>
				 * 
				 * XURL=https://ftp.mozilla.org/pub/mozilla.org/xulrunner/releases/10.0.2/runtimes/xulrunner-10.0.2.en-US.linux-x86_64.tar.bz2
				 * cd /opt
				 * sudo sh -c "wget -O- $XURL | tar -xj"
				 * sudo ln -s /opt/xulrunner/xulrunner /usr/bin/xulrunner
				 * sudo ln -s /opt/xulrunner/xpcshell /usr/bin/xpcshell
				 * 
				 * </pre>
				 */
				_browser = new Browser(parent, SWT.MOZILLA);

//	-Dorg.eclipse.swt.browser.XULRunnerPath=c:\xxx\xulrunner

			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
					onBrowserLocation(event);
				}
			});

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {
					onBrowserCompleted(event);
				}
			});

		} catch (final SWTError e) {

			_txtSearch.setEnabled(false);

			_txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));

			showInvalidPage();

			StatusUtil.log(e);
		}
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionTourBlogMarker);
	}

	private int getMaxPage(final int totalHits, final int hitsPerPage) {

		int maxPage = (totalHits / hitsPerPage);

		if (totalHits % hitsPerPage != 0) {
			maxPage++;
		}

		return maxPage;
	}

	private void hrefActionEditTour(final Long tourId) {

		// get tour by id
		final TourData tourData = TourManager.getTour(tourId);
		if (tourData == null) {
			return;
		}

		if (new DialogQuickEdit(//
				Display.getCurrent().getActiveShell(),
				tourData).open() == Window.OK) {

			saveModifiedTour(tourData);
		}
	}

	private void hrefActionMarker(final String action, final long tourId, final long markerId) {

		// get tour by id
		final TourData tourData = TourManager.getTour(tourId);
		if (tourData == null) {
			return;
		}

		TourMarker selectedTourMarker = null;

		// get marker by id
		for (final TourMarker tourMarker : tourData.getTourMarkers()) {
			if (tourMarker.getMarkerId() == markerId) {
				selectedTourMarker = tourMarker;
				break;
			}
		}

		if (selectedTourMarker == null) {
			return;
		}

		switch (action) {
		case ACTION_EDIT_MARKER:
			hrefActionMarker_Edit(tourData, selectedTourMarker);
			break;

		case ACTION_SELECT_MARKER:
			hrefActionMarker_Select(tourData, selectedTourMarker);
			break;
		}
	}

	private void hrefActionMarker_Edit(final TourData tourData, final TourMarker tourMarker) {

		if (tourData.isManualTour()) {
			// a manually created tour do not have time slices -> no markers
			return;
		}

		final DialogMarker markerDialog = new DialogMarker(//
				_uiParent.getShell(),
				tourData,
				tourMarker);

		if (markerDialog.open() == Window.OK) {
			saveModifiedTour(tourData);
		}
	}

	private void hrefActionMarker_Select(final TourData tourData, final TourMarker selectedTourMarker) {

		final ArrayList<TourMarker> selectedTourMarkers = new ArrayList<TourMarker>();
		selectedTourMarkers.add(selectedTourMarker);

		final SelectionTourMarker markerSelection = new SelectionTourMarker(tourData, selectedTourMarkers);

		// ensure that the selection provider contain the correct data
		_postSelectionProvider.setSelectionNoFireEvent(markerSelection);

		TourManager.fireEvent(//
				TourEventId.MARKER_SELECTION,
				markerSelection,
				getSite().getPart());
	}

	private void hrefActionWayPoint(final String action, final long tourId, final long markerId) {

		// get tour by id
		final TourData tourData = TourManager.getTour(tourId);
		if (tourData == null) {
			return;
		}

		TourWayPoint selectedWayPoint = null;

		// get marker by id
		for (final TourWayPoint wayPoint : tourData.getTourWayPoints()) {
			if (wayPoint.getWayPointId() == markerId) {
				selectedWayPoint = wayPoint;
				break;
			}
		}

		if (selectedWayPoint == null) {
			return;
		}

		// fire selection
		final ISelection selection = new StructuredSelection(selectedWayPoint);
		_postSelectionProvider.setSelection(selection);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		try {

			/*
			 * load css from file
			 */
			final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(SEARCH_RESULT_CSS_FILE);
			final URL fileUrl = FileLocator.toFileURL(bundleUrl);
			final URI fileUri = fileUrl.toURI();
			final File file = new File(fileUri);

			final String cssContent = Util.readContentFromFile(file.getAbsolutePath());

			_htmlCss = "<style>" + cssContent + "</style>"; //$NON-NLS-1$ //$NON-NLS-2$

			/*
			 * set image urls
			 */
			_iconUrl_Tour = net.tourbook.ui.UI.getIconUrl(Messages.Image__TourChart);
			_iconUrl_Marker = net.tourbook.ui.UI.getIconUrl(Messages.Image__TourMarker);
			_iconUrl_WayPoint = net.tourbook.ui.UI.getIconUrl(IMAGE_ACTION_TOUR_WAY_POINT);

			_actionUrl_EditImage = net.tourbook.ui.UI.getIconUrl(Messages.Image__quick_edit);

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	private void onBrowserCompleted(final ProgressEvent event) {

		_isBrowserLoadingCompleted = true;

//		System.out
//				.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tonBrowserCompleted()"));
//// TODO remove SYSTEM.OUT.PRINTLN

	}

	private void onBrowserLocation(final LocationEvent event) {

		final String location = event.location;

		final String[] locationParts = location.split(HREF_TOKEN);

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tlocation:" + location)
//				+ ("\t" + Arrays.toString(locationParts)));
//		// TODO remove SYSTEM.OUT.PRINTLN

		String action = null;
		long tourId = -1;
		long markerId = -1;
		int docId = -1;
		int page = -1;

		for (final String part : locationParts) {

			final int valueSepPos = part.indexOf(HREF_VALUE_SEP);

			String key;
			String value = null;

			if (valueSepPos == -1) {
				key = part;
			} else {
				key = part.substring(0, valueSepPos);
				value = part.substring(valueSepPos + 1, part.length());
			}

			if (key == null) {
				// this should not happen
				return;
			}

			switch (key) {
			case PARAM_ACTION:
				action = value;
				break;

			case PARAM_DOC_ID:
				docId = Integer.parseInt(value);
				break;

			case PARAM_PAGE:
				page = Integer.parseInt(value);
				break;

			case PARAM_MARKER_ID:
				markerId = Long.parseLong(value);
				break;

			case PARAM_TOUR_ID:
				tourId = Long.parseLong(value);
				break;

			default:
				break;
			}
		}

		if (location.equals(PAGE_ABOUT_BLANK) && action == null) {

			// about:blank is the initial page

		} else {

			// keep current page when an action is performed, OTHERWISE the current page will disappear :-(

			event.doit = false;
		}

		if (action == null) {
			return;
		}

		switch (action) {

		case ACTION_EDIT_TOUR:

			setSelectedDocId(docId);

			hrefActionEditTour(tourId);

			break;

		case ACTION_SELECT_TOUR:

			setSelectedDocId(docId);

			_postSelectionProvider.setSelection(new SelectionTourId(tourId));

			break;

		case ACTION_EDIT_MARKER:
		case ACTION_SELECT_MARKER:

			setSelectedDocId(docId);

			hrefActionMarker(action, tourId, markerId);

			break;

		case ACTION_SELECT_WAY_POINT:

			setSelectedDocId(docId);

			hrefActionWayPoint(action, tourId, markerId);

			break;

		case ACTION_NAVIGATE_PAGE:

			// navigate to another page

			final int pageNumber = page;

			_browser.getDisplay().asyncExec(new Runnable() {
				public void run() {
					startSearch(false, pageNumber);
				}
			});

			break;
		}
	}

	/**
	 * @param isStartSearch
	 *            When <code>true</code> the search is started again.
	 */
	void onChangeUI(final boolean isStartSearch) {

		restoreState_Options();

		if (isStartSearch) {

			startSearch(true, 0);

		} else {

			updateUI(_searchResult, null);
		}
	}

	private void onKeyDown(final Event event) {

		if (_searchResult == null) {
			return;
		}

		final int totalHits = _searchResult.totalHits;
		final int hitsPerPage = _searchResult.hitsPerPage;
		final int activePageNumber = _searchResult.pageNumber;

		if (totalHits <= hitsPerPage) {
			// paging is not needed
			return;
		}

		int navigateToPage = -1;

		switch (event.keyCode) {
		case SWT.PAGE_DOWN:

			final int maxPage = getMaxPage(totalHits, hitsPerPage);

			if (activePageNumber < maxPage - 1) {
				navigateToPage = activePageNumber + 1;
			}

			break;

		case SWT.PAGE_UP:

			if (activePageNumber > 0) {
				navigateToPage = activePageNumber - 1;
			}

			break;
		}

		if (navigateToPage != -1) {

			startSearch(false, navigateToPage);

			// text field can loose the focus
			setFocus();
		}
	}

	/**
	 * Open popup with {@link SWT#ARROW_DOWN} key.
	 * 
	 * @param e
	 */
	private void onSearchKey(final KeyEvent e) {

		if (_contentProposalAdapter.isProposalPopupOpen()) {
			return;
		}

		if (e.keyCode == SWT.ARROW_DOWN) {
//		if (e.keyCode == SWT.ARROW_UP) {
//			e.doit = false;

			e.keyCode = SWT.ARROW_UP;
			_contentProposalAdapter.openProposalPopup();
		}
	}

	private void restoreState() {

		final int popupWidth = Util.getStateInt(_state, STATE_POPUP_WIDTH, _pc.convertWidthInCharsToPixels(40));
		final int popupHeight = Util.getStateInt(_state, STATE_POPUP_HEIGHT, _pc.convertHeightInCharsToPixels(20));
		final Point popupSize = new Point(popupWidth, popupHeight);

		_contentProposalAdapter.setPopupSize(popupSize);

		final String searchText = Util.getStateString(_state, STATE_SEARCH_TEXT, UI.EMPTY_STRING);
		_txtSearch.setText(searchText);
		// move cursor to the end of the text
		_txtSearch.setSelection(searchText.length());

		restoreState_Options();

		onChangeUI(false);
	}

	private void restoreState_Options() {

		_hitsPerPage = Util.getStateInt(_state, STATE_HITS_PER_PAGE, STATE_HITS_PER_PAGE_DEFAULT);
		_numberOfPages = Util.getStateInt(_state, STATE_NUMBER_OF_PAGES, STATE_NUMBER_OF_PAGES_DEFAULT);

		_isUIShowDateTime = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_DATE_TIME,
				STATE_IS_SHOW_DATE_TIME_DEFAULT);

		_isUIShowLuceneDocId = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_LUCENE_DOC_ID,
				STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

		_isUIShowItemNumber = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_ITEM_NUMBER,
				STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);

//		_isUIShowScore = Util.getStateBoolean(_state, //
//				STATE_IS_SHOW_SCORE,
//				STATE_IS_SHOW_SCORE_DEFAULT);

		_isUIShowTopNavigator = Util.getStateBoolean(_state,//
				STATE_IS_SHOW_TOP_NAVIGATOR,
				STATE_IS_SHOW_TOP_NAVIGATOR_DEFAULT);

		final boolean isSortDateAscending = Util.getStateBoolean(_state,//
				STATE_IS_SORT_DATE_ASCENDING,
				STATE_IS_SORT_DATE_ASCENDING_DEFAULT);

		FTSearchManager.setResultSorting(isSortDateAscending);
	}

	private void saveModifiedTour(final TourData tourData) {

		/*
		 * Run async because a tour save will fire a tour change event.
		 */
		_uiParent.getDisplay().asyncExec(new Runnable() {
			public void run() {
				TourManager.saveModifiedTour(tourData);
			}
		});
	}

	private void saveState() {

		final Point popupSize = _contentProposalAdapter.getPopupSize();
		_state.put(STATE_POPUP_WIDTH, popupSize.x);
		_state.put(STATE_POPUP_HEIGHT, popupSize.y);

		_state.put(STATE_SEARCH_TEXT, _txtSearch.getText());
	}

	@Override
	public void setFocus() {

		_txtSearch.setFocus();
	}

	/**
	 * Highlight selected item by changing the background color.
	 * 
	 * @param docId
	 */
	private void setSelectedDocId(final int docId) {

		_previousDocId = _selectedDocId;
		_selectedDocId = docId;

		final StringBuilder sb = new StringBuilder();
		sb.append("var oldId = " + _previousDocId + ";");
		sb.append("var selectedId = " + _selectedDocId + ";");

		sb.append("if (oldId >= 0) {");
		sb.append("	var divOldSelection = document.getElementById(oldId.toString());");
		sb.append("	if (divOldSelection) {");
		sb.append("		divOldSelection.setAttribute('class', '" + CSS_HOVER_CONTAINER + "');");
		sb.append("	}");
		sb.append("}");

		sb.append("if (selectedId >= 0) {");
		sb.append("	var divSelection = document.getElementById(selectedId.toString());");
		sb.append("	if (divSelection) {");
		sb.append("		divSelection.setAttribute('class', '" + CSS_HOVER_CONTAINER + UI.SPACE + CSS_SELECTED + "');");
		sb.append("	}");
		sb.append("}");

		_browser.execute(sb.toString());
	}

	private void showInvalidPage() {

		_pageBook.showPage(_browser == null ? _pageNoBrowser : _pageSearch);
	}

	/**
	 * @param isNewSearch
	 * @param pageNumber
	 *            <code>0</code> is the first page number.
	 */
	private void startSearch(final boolean isNewSearch, final int pageNumber) {

		if (_contentProposalAdapter.isProposalPopupOpen()) {
			return;
		}

		if (isNewSearch) {

			// reset selected doc id

			_selectedDocId = -1;
			_previousDocId = -1;
		}

		_searchResult = null;

		String searchText = _txtSearch.getText().trim();

		// check if search text is valid
		if (searchText.length() == 0) {
			updateUI(null, "No result");
			return;
		}
		if (searchText.startsWith(UI.SYMBOL_STAR) || searchText.startsWith("?")) {
			updateUI(null, "* or ? is not allowed as first character");
			return;
		}

		if (searchText.endsWith(UI.SYMBOL_STAR) == false) {

			// Append a * otherwise nothing is found
			searchText += UI.SYMBOL_STAR;
		}

		_searchTime = -1;
		final long startTime = System.currentTimeMillis();

		final SearchResult searchResult = FTSearchManager.search(searchText, pageNumber, _hitsPerPage);

		if (searchResult.items.size() == 0 && searchResult.error == null) {
			updateUI(null, "No result");
			return;
		}

		_searchResult = searchResult;
		_searchTime = System.currentTimeMillis() - startTime;

		updateUI(searchResult, null);
	}

	/**
	 * @param searchResult
	 *            Can be <code>null</code> when a search result is not available.
	 * @param statusText
	 *            Can be <code>null</code> when a status text is not available.
	 */
	private void updateUI(final SearchResult searchResult, final String statusText) {

		final StringBuilder stateText = new StringBuilder();

		if (statusText != null) {
			appendState(stateText, statusText);
		}

		if (searchResult != null) {

			if (searchResult.error != null) {
				appendState(stateText, searchResult.error);
			}

			appendState(stateText, String.format("%d results - %d ms", searchResult.totalHits, _searchTime));
		}

		final String html = createHTML(searchResult, stateText);

		_browser.setRedraw(true);
		_browser.setText(html);
	}

}
