
<template src="./SlideoutSearchOptions.html"></template>
<style>
/* @import '../assets/search.css'; */
</style>


<script>
//
import VuePopper from 'vue-popperjs'

export default {
   //
   components: {
      VuePopper
   },

   data: () => ({
      //
      apChkShowContentAll: '',
      apChkShowContentTour: '',
      apChkShowContentMarker: '',
      apChkShowContentWaypoint: '',
      apChkEaseSearching: '',
      apSortByDate: '',
      apChkShowDescription: '',
      apChkShowDate: '',
      apChkShowTime: '',
      apChkShowItemNumber: '',
      apChkShowLuceneID: '',

      // prettier-ignore
      tooltipOptions:{
                        placement: 'bottom',
                        modifiers: { 
                           offset: { offset: '0, -5px' } ,
                           flip: { enabled: false }
                        }
                     }
   }),

   methods: {
      //
		apActionRestoreDefaults : function apActionRestoreDefaults() {

			this._setSearchOptions(//
			{
				isRestoreDefaults : true
			});
		},

		/**
		 * Search all checkbox
		 */
		apSearchAll : function apSearchAll() {

			this._enableControls();

			// fire selection
			this.apSelection();
		},

		/**
		 * Selection is from an attach point.
		 */
		apSelection : function apSelection() {

			if (this._isValid()) {

				var searchOptions = //
				{
					isEaseSearching : this.apChkEaseSearching.get('checked'),

					isShowContentAll : this.apChkShowContentAll.get('checked'),
					isShowContentTour : this.apChkShowContentTour.get('checked'),
					isShowContentMarker : this.apChkShowContentMarker.get('checked'),
					isShowContentWaypoint : this.apChkShowContentWaypoint.get('checked'),

					isSortByDateAscending : this.apSortByDateAscending.get('checked'),

					isShowDate : this.apChkShowDate.get('checked'),
					isShowTime : this.apChkShowTime.get('checked'),
					isShowDescription : this.apChkShowDescription.get('checked'),
					isShowItemNumber : this.apChkShowItemNumber.get('checked'),
					isShowLuceneID : this.apChkShowLuceneID.get('checked')
				};

				this._setSearchOptions(searchOptions);
			}
		},

		_isValid : function _isValid() {

			var //
			statusText = '', //
			isValid = true, //

			isShowContentAll = this.apChkShowContentAll.get('checked'), //
			isShowContentTour = this.apChkShowContentTour.get('checked'), //
			isShowContentMarker = this.apChkShowContentMarker.get('checked'), //
			isShowContentWaypoint = this.apChkShowContentWaypoint.get('checked');

			if (isShowContentAll) {

				// content is valid

			} else {

				// at least one content must be checked

				if (isShowContentTour == false //
					&& isShowContentMarker == false //
					&& isShowContentWaypoint == false) {

					statusText = Messages.Search_Validation_SearchFilter;
					isValid = false;
				}
			}

			// update status
			dom.byId('domSearchStatus').innerHTML = statusText;

			// resize dialog because status text has changed and can be too long 
			this._dialog.resize();

			return isValid;
		},

		_enableControls : function _enableControls() {

			var isShowContentAll = this.apChkShowContentAll.get('checked');

			this.apChkShowContentTour.set('disabled', isShowContentAll);
			this.apChkShowContentMarker.set('disabled', isShowContentAll);
			this.apChkShowContentWaypoint.set('disabled', isShowContentAll);
		},

		/**
		 * 
		 */
		_restoreState : function _restoreState(callBack) {

			var _this = this;

			var xhrQuery = {};
			xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_GET_SEARCH_OPTIONS;

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				handleAs : 'json',
				preventCache : true,
				timeout : SearchMgr.XHR_TIMEOUT,

				query : xhrQuery

			}).then(function(xhrData) {

				_this._updateUI_FromState(_this, xhrData);
			});
		},

		/**
		 * Set search options in the backend and reload current search with new search options.
		 */
		_setSearchOptions : function _setSearchOptions(searchOptions) {

			var _this = this;

			var jsonSearchOptions = JSON.stringify(searchOptions);

			var xhrQuery = {};
			xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SET_SEARCH_OPTIONS;
			xhrQuery[SearchMgr.XHR_PARAM_SEARCH_OPTIONS] = encodeURIComponent(jsonSearchOptions);

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				handleAs : 'json',
				preventCache : true,
				timeout : SearchMgr.XHR_TIMEOUT,

				query : xhrQuery

			}).then(function(xhrData) {

				if (xhrData.isSearchOptionsDefault) {

					// set defaults in the UI
					_this._updateUI_FromState(_this, xhrData);
				}

				// repeat previous search

				_this._searchApp._searchInput.startSearch(true);
			});
		},

		_updateUI_FromState : function _updateUI_FromState(dialog, xhrData) {

			dialog.apChkEaseSearching.set('checked', xhrData.isEaseSearching);

			dialog.apChkShowContentAll.set('checked', xhrData.isShowContentAll);
			dialog.apChkShowContentTour.set('checked', xhrData.isShowContentTour);
			dialog.apChkShowContentMarker.set('checked', xhrData.isShowContentMarker);
			dialog.apChkShowContentWaypoint.set('checked', xhrData.isShowContentWaypoint);

			dialog.apSortByDateAscending.set('checked', xhrData.isSortByDateAscending);
			dialog.apSortByDateDescending.set('checked', !xhrData.isSortByDateAscending);

			dialog.apChkShowDate.set('checked', xhrData.isShowDate);
			dialog.apChkShowTime.set('checked', xhrData.isShowTime);
			dialog.apChkShowDescription.set('checked', xhrData.isShowDescription);
			dialog.apChkShowItemNumber.set('checked', xhrData.isShowItemNumber);
			dialog.apChkShowLuceneID.set('checked', xhrData.isShowLuceneID);

			dialog._enableControls();
			dialog._isValid();
		}

   }
}
</script>

