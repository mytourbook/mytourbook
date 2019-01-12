import VuePopper from 'vue-popperjs'
import SearchMgr from '../SearchMgr'
import Axios from 'axios'

export default {

   components: {
      VuePopper
   },

   data: () => ({

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

      vm_SearchStatus: '',

      tooltipOptions: {
         placement: 'bottom',
         modifiers: {
            offset: { offset: '0, -5px' },
            flip: { enabled: false }
         }
      }
   }),

   mounted: function() {

      this._restoreState()
   },

   methods: {

      apActionRestoreDefaults: function() {

         this._setSearchOptions( //
            {
               isRestoreDefaults: true
            })
      },

      /**
       * Selection is from an attach point.
       */
      apSelection: function() {

         if (this._isValid()) {

            var searchOptions = {

               isEaseSearching: this.apChkEaseSearching,

               isShowContentAll: this.apChkShowContentAll,
               isShowContentTour: this.apChkShowContentTour,
               isShowContentMarker: this.apChkShowContentMarker,
               isShowContentWaypoint: this.apChkShowContentWaypoint,

               isSortByDateAscending: this.apSortByDate === 'ascending',

               isShowDate: this.apChkShowDate,
               isShowTime: this.apChkShowTime,
               isShowDescription: this.apChkShowDescription,
               isShowItemNumber: this.apChkShowItemNumber,
               isShowLuceneID: this.apChkShowLuceneID
            }

            this._setSearchOptions(searchOptions)
         }
      },

      // VuePopper event when search options is displayed
      vm_SearchOptions_Show: function(data) {
         
         console.log(data)
      },
      
      // VuePopper event when search options is hidden
      vm_SearchOptions_Hide: function(data) {

         console.log(data)
      },

      _isValid: function() {

         var isValid = true
         var statusText = ''

         var isShowContentAll = this.apChkShowContentAll
         var isShowContentTour = this.apChkShowContentTour
         var isShowContentMarker = this.apChkShowContentMarker
         var isShowContentWaypoint = this.apChkShowContentWaypoint

         if (isShowContentAll) {

            // content is valid

         } else {

            // at least one content must be checked

            if (isShowContentTour == false
               && isShowContentMarker == false
               && isShowContentWaypoint == false) {

               statusText = this.$t('message.Search_Validation_SearchFilter')
               isValid = false
            }
         }

         // update status

         this.vm_searchStatus = statusText

         // resize dialog because status text has changed and can be too long 
         // this._dialog.resize()

         return isValid
      },

      _enableControls: function() {

         // var isShowContentAll = this.apChkShowContentAll.get('checked')

         // this.apChkShowContentTour.set('disabled', isShowContentAll)
         // this.apChkShowContentMarker.set('disabled', isShowContentAll)
         // this.apChkShowContentWaypoint.set('disabled', isShowContentAll)
      },

      /**
       * 
       */
      _restoreState: function(callBack) {

         var _this = this

         var xhrData = {}
         xhrData[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_GET_SEARCH_OPTIONS

         this.axios.request({

               url: SearchMgr.XHR_SEARCH_HANDLER,
               method: 'post',

               headers: { 'X-Requested-With': 'XMLHttpRequest' },
               timeout: SearchMgr.XHR_TIMEOUT,

               data: xhrData,
            }

         ).then(function(response) {

            _this._updateUI_FromState(_this, response.data)

         }).catch(function(error) {
            console.log(error)
         })
      },

      /**
       * Set search options in the backend and reload current search with new search options.
       */
      _setSearchOptions: function(searchOptions) {

         var _this = this

         var jsonSearchOptions = JSON.stringify(searchOptions)

         var xhrData = {}

         xhrData[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SET_SEARCH_OPTIONS
         xhrData[SearchMgr.XHR_PARAM_SEARCH_OPTIONS] = jsonSearchOptions

         this.axios.request({

               url: SearchMgr.XHR_SEARCH_HANDLER,
               method: 'post',

               headers: { 'X-Requested-With': 'XMLHttpRequest' },
               timeout: SearchMgr.XHR_TIMEOUT,

               data: xhrData,
            }

         ).then(function(response) {

            if (searchOptions.isRestoreDefaults) {

               // set defaults in the UI
               _this._updateUI_FromState(_this, response.data)
            }

            // repeat previous search

            // _this._searchApp._searchInput.startSearch(true)

         }).catch(function(error) {
            console.log(error)
         })
      },

      _updateUI_FromState: function(_this, xhrData) {

         _this.apChkEaseSearching = xhrData.isEaseSearching

         _this.apChkShowContentAll = xhrData.isShowContentAll
         _this.apChkShowContentTour = xhrData.isShowContentTour
         _this.apChkShowContentMarker = xhrData.isShowContentMarker
         _this.apChkShowContentWaypoint = xhrData.isShowContentWaypoint

         _this.apSortByDate = xhrData.isSortByDateAscending ? 'ascending' : 'descending'

         _this.apChkShowDate = xhrData.isShowDate
         _this.apChkShowTime = xhrData.isShowTime
         _this.apChkShowDescription = xhrData.isShowDescription
         _this.apChkShowItemNumber = xhrData.isShowItemNumber
         _this.apChkShowLuceneID = xhrData.isShowLuceneID

         _this._enableControls()
         _this._isValid()
      }
   }
}
