define(
[
		"dojo/_base/declare",
		"dojo/_base/lang",
		"dojo/dom",
		"dojo/on",
		"dojo/request/xhr",
		"dojo/store/Memory",
		"dijit/form/FilteringSelect",
], //
function(declare,
		lang,
		dom,
		on,
		xhr,
		Memory,
		FilteringSelect,
		zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz) {

	var SearchUI = declare("tourbook.SearchUI",
	[
			FilteringSelect,
	], {

		XHR_SEARCH_HANDLER : "/xhrSearch",

		XHR_PARAM_NAME_ACTION : "ACTION",
		XHR_ACTION_SEARCH : "Search",
		XHR_ACTION_PROPOSALS : "Proposals",

		_loadData : function loadData(xhrSearchText) {

			if (xhrSearchText) {
				xhrSearchText = xhrSearchText.trim();
			}

			if (!xhrSearchText) {
				console.info("Search text is empty.");
				return;
			}

			console.info("_loadData: " + xhrSearchText);

			var self = this;

			var encodeSearch = encodeURIComponent(xhrSearchText);

			xhr(this.XHR_SEARCH_HANDLER, {

				handleAs : "json",
				preventCache : true,
				timeout : 60000,

				query : {
//					action : this.XHR_ACTION_SEARCH,
					ACTION : this.XHR_ACTION_PROPOSALS,
					searchText : encodeSearch,
				}

			}).then(function(xhrData) {

				var newStore = new Memory({
					data : xhrData
				});

				self.store = newStore;

				console.info("XHR " + (new Date()).getTime() + "<br> " + xhrData);

			}, function(err) {

				// Handle the error condition
				console.error("error: " + err);
			})
		},
		
		_onChange : function onChange(event) {
			
			// show selected item
			
			this.log("onChange: " + event);
		},

		_onKeyPress : function _onKeyPress(event) {

			this.canLoadData = true;
		},

		_onKeyUp : function _onKeyUp(evt) {

			// load suggestions for the entered value

			console.info("_onKeyUp");

			if (this.canLoadData) {
				
				this.canLoadData = false;

				// prevent that it is call TWICE
				evt.stopPropagation();
				evt.preventDefault();

				var enteredSearchText = this.get('displayedValue');
				this._loadData(enteredSearchText);
			}
		},

		_onKeyDown : function _onKeyDown(event) {

			// load suggestions for the entered value

			console.info("_onKeyDown");
		},

		// hide validation checker
		isValid : function() {
			return true;
		},

		log : function(logText) {

			dom.byId("domLog").innerHTML = logText;
		},

		postCreate : function() {

			this.inherited(arguments);

			on(this.domNode, "change", lang.hitch(this, "_onChange"));
			on(this.domNode, "keypress", lang.hitch(this, "_onKeyPress"));
			on(this.domNode, "keydown", lang.hitch(this, "_onKeyDown"));
			on(this.domNode, "keyup", lang.hitch(this, "_onKeyUp"));

			// set focus to the search field
			this.focus();
		},

		// overwrite dijit.form._AutoCompleterMixin
		doHighlight : function(/* String */label, /* String */find) {

			debugger;

			return find;

//				// summary:
//				//		Highlights the string entered by the user in the menu.  By default this
//				//		highlights the first occurrence found. Override this method
//				//		to implement your custom highlighting.
//				// tags:
//				//		protected
			//
//				// Add (g)lobal modifier when this.highlightMatch == "all" and (i)gnorecase when this.ignoreCase == true
//				var //
//				modifiers = (this.ignoreCase ? "i" : "") + (this.highlightMatch == "all" ? "g" : ""), //
//				i = this.queryExpr.indexOf("${0}");
			//
//				find = regexp.escapeString(find); // escape regexp special chars
//				//If < appears in label, and user presses t, we don't want to highlight the t in the escaped "&lt;"
//				//first find out every occurrences of "find", wrap each occurrence in a pair of "\uFFFF" characters (which
//				//should not appear in any string). then html escape the whole string, and replace '\uFFFF" with the
//				//HTML highlight markup.
//				return this._escapeHtml(
//					label.replace(new RegExp((i == 0 ? "^" : "") + "(" + find + ")"
//							+ (i == (this.queryExpr.length - 4) ? "$" : ""), modifiers), '\uFFFF$1\uFFFF')).replace(
//					/\uFFFF([^\uFFFF]+)\uFFFF/g,
//					'<span class="dijitComboBoxHighlightMatch">$1</span>'); // returns String, (almost) valid HTML (entities encoded)
		},

	});

	return SearchUI;
});
