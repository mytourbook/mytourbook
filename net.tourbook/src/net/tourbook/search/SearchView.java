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
import net.tourbook.preferences.ITourbookPreferences;
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
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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

	public static final String			ID									= "net.tourbook.search.SearchView"; //$NON-NLS-1$

	private static final String			SEARCH_RESULT_CSS_FILE				= "/html/search-result.css";		//$NON-NLS-1$

	static final String					STATE_IS_SHOW_DATE_TIME				= "STATE_IS_SHOW_DATE_TIME";		//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_DATE_TIME_DEFAULT		= false;
	static final String					STATE_IS_SHOW_ITEM_NUMBER			= "STATE_IS_SHOW_ITEM_NUMBER";		//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_ITEM_NUMBER_DEFAULT	= false;
	static final String					STATE_IS_SHOW_SCORE					= "STATE_IS_SHOW_SCORE";			//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_SCORE_DEFAULT			= false;
	static final String					STATE_IS_SHOW_TOP_NAVIGATOR			= "STATE_IS_SHOW_TOP_NAVIGATOR";	//$NON-NLS-1$
	static final boolean				STATE_IS_SHOW_TOP_NAVIGATOR_DEFAULT	= false;
	static final String					STATE_HITS_PER_PAGE					= "STATE_HITS_PER_PAGE";			//$NON-NLS-1$
	static final int					STATE_HITS_PER_PAGE_DEFAULT			= 10;
	static final String					STATE_NUMBER_OF_PAGES				= "STATE_NUMBER_OF_PAGES";			//$NON-NLS-1$
	static final int					STATE_NUMBER_OF_PAGES_DEFAULT		= 5;

	private static final String			STATE_POPUP_WIDTH					= "STATE_POPUP_WIDTH";				//$NON-NLS-1$
	private static final String			STATE_POPUP_HEIGHT					= "STATE_POPUP_HEIGHT";			//$NON-NLS-1$
	private static final String			STATE_SEARCH_TEXT					= "STATE_SEARCH_TEXT";				//$NON-NLS-1$

	/**
	 * This is necessary otherwise XULrunner in Linux do not fire a location change event.
	 */
	private static final String			HTTP_DUMMY							= "http://dummy";					//$NON-NLS-1$

	private static final String			HREF_TOKEN							= "#";								//$NON-NLS-1$
	private static final String			PAGE_ABOUT_BLANK					= "about:blank";					//$NON-NLS-1$

	private static final String			ACTION_NAVIGATE_PAGE				= "NavigatePage";					//$NON-NLS-1$
	private static final String			ACTION_SELECT_TOUR					= "SelectTour";					//$NON-NLS-1$
	private static final String			ACTION_SELECT_MARKER				= "SelectMarker";					//$NON-NLS-1$

	private static String				HREF_NAVIGATE_PAGE;
	private static String				HREF_SELECT_TOUR;
	private static String				HREF_SELECT_MARKER;

	static {

		HREF_NAVIGATE_PAGE = HREF_TOKEN + ACTION_NAVIGATE_PAGE + HREF_TOKEN;
		HREF_SELECT_TOUR = HREF_TOKEN + ACTION_SELECT_TOUR + HREF_TOKEN;
		HREF_SELECT_MARKER = HREF_TOKEN + ACTION_SELECT_MARKER + HREF_TOKEN;
	}

	private final DateTimeFormatter		_dateFormatter						= DateTimeFormat.mediumDate();

	private final IPreferenceStore		_prefStore							= TourbookPlugin.getPrefStore();
	private final IDialogSettings		_state								= TourbookPlugin.getState(ID);
	//
	private PostSelectionProvider		_postSelectionProvider;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;
	private IPartListener2				_partListener;
	//
	private String						_htmlCss;
	private String						_tourChartIconUrl;
	private String						_tourMarkerIconUrl;

	private MTContentProposalAdapter	_contentProposalAdapter;
	private TextContentAdapter			_controlContentAdapter				= new TextContentAdapter();
	private MTProposalProvider			_proposalProvider					= new MTProposalProvider();

	private ActionSearchOptions			_actionTourBlogMarker;

	private boolean						_isShowDateTime;
	private boolean						_isShowItemNumber;
	private boolean						_isShowScore;
	private boolean						_isShowTopNavigator;
	private int							_hitsPerPage;
	private int							_numberOfPages;

	private SearchResult				_searchResult;
	private long						_searchTime							= -1;

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

			final List<LookupResult> proposals = MTSearchManager.getProposals(contents, position);

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

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == SearchView.this) {
					saveState();
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

			if (_isShowTopNavigator) {
				sb.append("<div style='padding-top:0px;'></div>");
				sb.append(sbNavigator);
				sb.append("<div style='padding-top:20px;'></div>");
			}
		}

		if (searchResult != null) {

			final int itemBaseNumber = searchResult.pageNumber * searchResult.hitsPerPage;

			int itemIndex = 0;

			for (final SearchResultItem entry : searchResult.items) {

				final int itemNumber = ++itemIndex + itemBaseNumber;

				sb.append("<div class='result-item'>"); //$NON-NLS-1$
				sb.append("<div class='hover-container'>\n"); //$NON-NLS-1$
				{
					createHTML_30_Item(sb, entry, itemNumber);
				}
				sb.append("</div>\n"); //$NON-NLS-1$
				sb.append("</div>\n"); //$NON-NLS-1$

			}
		}

		if (searchResult != null) {
			sb.append("<div style='padding-top:20px;'></div>");
			sb.append(sbNavigator);
			sb.append("<div style='padding-top:20px;'></div>");
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

		final String tourId = resultItem.tourId;
		final String markerId = resultItem.markerId;

		final boolean isTour = tourId != null && markerId == null;
		final boolean isMarker = markerId != null && tourId != null;

		final String iconUrl = isTour ? _tourChartIconUrl : _tourMarkerIconUrl;

		String itemTitleText = null;
		String hrefOpenItem = null;

		if (isTour) {

			final String tourTitle = resultItem.tourTitle;

			if (tourTitle != null) {
				itemTitleText = tourTitle;
			}

			hrefOpenItem = HTTP_DUMMY + HREF_SELECT_TOUR + tourId;

		} else if (isMarker) {

			final String tourMarkerLabel = resultItem.markerLabel;

			if (tourMarkerLabel != null) {
				itemTitleText = tourMarkerLabel;
			}

			hrefOpenItem = HTTP_DUMMY + HREF_SELECT_MARKER + markerId + HREF_TOKEN + tourId;
		}

		if (itemTitleText == null) {
			itemTitleText = UI.EMPTY_STRING;
		}

		String itemTitle = itemTitleText;
		if (itemTitle.length() == 0) {
			// publish new line that the icon is not overwritten
			itemTitle = "</br>";
		}

		final boolean isShowInfo = _isShowDateTime || _isShowItemNumber || _isShowScore;

		sb.append("<table><tbody><tr>");
		{
			/*
			 * Item image
			 */
			sb.append("<td class='item-image'>");
			{
				sb.append("<img src='" + iconUrl + "'></img>");
			}
			sb.append("</td>");

			/*
			 * Item content
			 */
			sb.append("<td style='width:100%;'>");
			{
				sb.append("<a class='item'" //
						+ (" xstyle='background-image: url(" + iconUrl + ")'")
						+ (" href='" + hrefOpenItem + "'") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" title='" + itemTitle + "'") //$NON-NLS-1$ //$NON-NLS-2$
						+ ">"); // //$NON-NLS-1$
				{
					// title
					sb.append("<span class='item-title'>" + itemTitle + "</span>");

					// description
					{
						final String description = resultItem.description;
						if (description != null) {
							sb.append("<div class='item-description'>"); //$NON-NLS-1$
							{
								sb.append(description);
							}
							sb.append("</div>\n"); //$NON-NLS-1$
						}
					}

					// info
					if (isShowInfo) {

						sb.append("<div class='item-info'>"); //$NON-NLS-1$
						sb.append("<table><tbody><tr>");
						{
							if (_isShowDateTime) {

								final long tourStartTime = resultItem.tourStartTime;
								if (tourStartTime != 0) {

									final DateTime dt = new DateTime(tourStartTime);

									sb.append("<td>"
											+ String.format("%s", _dateFormatter.print(dt.getMillis()))
											+ "</td>");

								}
							}

							if (_isShowScore) {
								sb.append("<td>" //
//										+ String.format("%d . %10.5f", resultItem.docId, resultItem.score)
										+ String.format("%3.3f", resultItem.score)
										+ "<td>");
							}

							if (_isShowItemNumber) {
								sb.append("<td>" + Integer.toString(itemNumber) + "<td>");
							}
						}
						sb.append("</tr></tbody></table>");
						sb.append("</div>\n"); //$NON-NLS-1$
					}
				}
				sb.append("</a>");
			}
			sb.append("</td>");
		}
		sb.append("</tr></tbody></table>");
	}

	private void createHTML_40_PageNavigator(final StringBuilder sb, final SearchResult searchResult) {

		final int totalHits = searchResult.totalHits;
		final int hitsPerPage = searchResult.hitsPerPage;
		final int activePageNumber = searchResult.pageNumber;

		if (totalHits <= hitsPerPage) {
			// paging is not needed
			return;
		}

		final int visiblePages = _numberOfPages;
		int maxPage = (totalHits / hitsPerPage);

		if (totalHits % hitsPerPage != 0) {
			maxPage++;
		}

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

		sbNext.append("<td style='width:49%; text-align:left;'>");
		sbPrevious.append("<td style='width:49%; text-align:right;'>");

		for (int currentPage = firstPage; currentPage < lastPage; currentPage++) {

			final int visiblePageNo = currentPage + 1;
			final String hrefPage = HTTP_DUMMY + HREF_NAVIGATE_PAGE + currentPage;

			/*
			 * Previous page
			 */
			if (currentPage == activePageNumber - 1) {
				sbPrevious.append("<a class='page-number' href='" + hrefPage + "'><</a>");
			}

			/*
			 * Every page
			 */
			sbPages.append("<td>");
			{
				if (currentPage == activePageNumber) {
					sbPages.append("<div class='page-number page-selected'>" + visiblePageNo + "</div>");
				} else {
					sbPages.append("<a class='page-number' href='" + hrefPage + "'>" + visiblePageNo + "</a>");
				}
			}
			sbPages.append("</td>");

			/*
			 * Next page
			 */
			if (currentPage == activePageNumber + 1) {
				sbNext.append("<a class='page-number' href='" + hrefPage + "'>></a>");
			}
		}

		sbNext.append("</td>");
		sbPrevious.append("</td>");

		/*
		 * first page
		 */
		sbFirst.append("<td>");
		{
			if (firstPage > 0) {
				final String hrefPage = HTTP_DUMMY + HREF_NAVIGATE_PAGE + 0;
				sbFirst.append("<a class='page-number' href='" + hrefPage + "'>1</a>");
			}
		}
		sbFirst.append("</td>");
		if (firstPage > 0) {
			sbFirst.append("<td>..</td>");
		}

		/*
		 * last page
		 */
		if (lastPage < maxPage - 0) {
			sbLast.append("<td>..</td>");
		}
		sbLast.append("<td>");
		{
			if (lastPage < maxPage) {
				final String hrefPage = HTTP_DUMMY + HREF_NAVIGATE_PAGE + (maxPage - 1);
				sbLast.append("<a class='page-number' href='" + hrefPage + "'>" + maxPage + "</a>");
			}
		}
		sbLast.append("</td>");

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
			createUI_30_ResultBrowser(container);
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
			_txtSearch.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(final KeyEvent e) {
//					onSearchKey(e);
				}
			});

			_txtSearch.addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(final TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_RETURN) {
						onSearchSelect(0);
					}
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

					onSearchSelect(0);
				}
			});

			_contentProposalAdapter.addContentProposalListener(new IContentProposalListener2() {

				@Override
				public void proposalPopupClosed(final ContentProposalAdapter adapter) {}

				@Override
				public void proposalPopupOpened(final ContentProposalAdapter adapter) {}
			});
		}
	}

	private void createUI_30_ResultBrowser(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

				// DEBUG: force error in win7
//				_browser = new Browser(parent, SWT.MOZILLA);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
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
			_tourChartIconUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__TourChart);
			_tourMarkerIconUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__TourMarker);

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	private void onBrowserCompleted(final ProgressEvent event) {

	}

	private void onBrowserLocationChanging(final LocationEvent event) {

		final String location = event.location;
		final String[] locationParts = location.split(HREF_TOKEN);

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tlocation:" + location)
//				+ ("\t" + Arrays.toString(locationParts)));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (locationParts.length >= 3) {

			final String partValue3 = locationParts[2];

			switch (locationParts[1]) {
			case ACTION_SELECT_MARKER:

				// get tour by id
				final Long markerTourId = Long.parseLong(locationParts[3]);
				final TourData tourData = TourManager.getTour(markerTourId);
				if (tourData == null) {
					return;
				}

				final long selectedMarkerId = Long.parseLong(partValue3);
				final ArrayList<TourMarker> selectedTourMarkers = new ArrayList<TourMarker>();

				// get marker by id
				for (final TourMarker tourMarker : tourData.getTourMarkers()) {
					if (tourMarker.getMarkerId() == selectedMarkerId) {
						selectedTourMarkers.add(tourMarker);
						break;
					}
				}

				final SelectionTourMarker markerSelection = new SelectionTourMarker(tourData, selectedTourMarkers);

				// ensure that the selection provider do not contain the wrong data
				_postSelectionProvider.setSelectionNoFireEvent(markerSelection);

				TourManager.fireEvent(//
						TourEventId.MARKER_SELECTION,
						markerSelection,
						getSite().getPart());

				break;

			case ACTION_SELECT_TOUR:

				final Long tourId = Long.parseLong(partValue3);
				final SelectionTourId tourSelection = new SelectionTourId(tourId);

				_postSelectionProvider.setSelection(tourSelection);

				break;

			case ACTION_NAVIGATE_PAGE:

				// navigate to another page

				_browser.getDisplay().asyncExec(new Runnable() {
					public void run() {

						final int pageNumber = Integer.parseInt(partValue3);

						onSearchSelect(pageNumber);
					}
				});

				break;

			default:
				break;
			}

		}

		if (location.equals(PAGE_ABOUT_BLANK) == false) {

			// about:blank is the initial page

			event.doit = false;
		}
	}

	/**
	 * @param isStartSearch
	 *            When <code>true</code> the search is started again.
	 */
	void onChangeUI(final boolean isStartSearch) {

		restoreState_Options();

		if (isStartSearch) {

			onSearchSelect(0);

		} else {

			updateUI(_searchResult, null);
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

	/**
	 * @param pageNumber
	 *            <code>0</code> is the first page number.
	 */
	private void onSearchSelect(final int pageNumber) {

		if (_contentProposalAdapter.isProposalPopupOpen()) {
			return;
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

		final SearchResult searchResult = MTSearchManager.search(searchText, pageNumber, _hitsPerPage);

		if (searchResult.items.size() == 0) {
			updateUI(null, "No result");
			return;
		}

		_searchResult = searchResult;
		_searchTime = System.currentTimeMillis() - startTime;

		updateUI(searchResult, null);
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

		_isShowDateTime = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_DATE_TIME,
				STATE_IS_SHOW_DATE_TIME_DEFAULT);

		_isShowItemNumber = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_ITEM_NUMBER,
				STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);

		_isShowScore = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_SCORE,
				STATE_IS_SHOW_SCORE_DEFAULT);

		_isShowTopNavigator = Util.getStateBoolean(_state,//
				STATE_IS_SHOW_TOP_NAVIGATOR,
				STATE_IS_SHOW_TOP_NAVIGATOR_DEFAULT);
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

	private void showInvalidPage() {

		_pageBook.showPage(_browser == null ? _pageNoBrowser : _pageSearch);
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
			appendState(stateText, String.format("%d results - %d ms", searchResult.totalHits, _searchTime));
		}

		final String html = createHTML(searchResult, stateText);

		_browser.setRedraw(true);
		_browser.setText(html);
	}

}
