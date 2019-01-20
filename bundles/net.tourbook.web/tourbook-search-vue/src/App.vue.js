import VuePopper from 'vue-popperjs'

import SearchMgr from './SearchMgr'

import SearchResultList from './components/SearchResultList'
import SlideoutSearchOptions from './components/SlideoutSearchOptions'

export default {
   //
   name: 'App',

   components: {
      SearchResultList,
      SlideoutSearchOptions,
      VuePopper,
   },

   data: () => ({

      SearchMgr,

      searchInputValue: '',
      _oldSearchText: '',

      allSearchProposal: [],
      allSearchResultItems: [

         {id: 0, name:'asaaaaaaaaaaaaa asaaaaaaaaaaaaa asaaaaaaaaaaaaa asaaaaaaaaaaaaa asaaaaaaaaaaaaa '},
         {id: 1, name:' asdf asd f asd f asdf a sdf as df asdf a dsf asd f aew r qwer qw er qwe rwq er qewr q ewr qewr q er qewr qewr'},
         {id: 2, name:'b b b b b b'},
      ],

      searchInputSync: null,
   }),

   methods: {

      onChange_SearchInput: function(params) {
         this.startSearch()
      },

      /**
       * Get search results for the current search text.
       */
      startSearch: function (isForceRefresh) {

         // show selected item

         var newSearchText = this.searchInputValue;

         console.warn('startSearch \'' + newSearchText + '\'');

         // check if loading is needed
         if (isForceRefresh || this._oldSearchText !== newSearchText) {

            // keep current search
            this._oldSearchText = newSearchText;

            var _this = this

            var queryData = {}
            queryData[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SEARCH
            queryData[SearchMgr.XHR_PARAM_SEARCH_TEXT] = newSearchText
      
            this.axios.request({

               url: SearchMgr.XHR_SEARCH_HANDLER,
               method: 'post',
               headers: { 'X-Requested-With': 'XMLHttpRequest' },
               timeout: SearchMgr.XHR_TIMEOUT,

               data: queryData,

            }).then(function(xhrData) {

               _this.allSearchResultItems = xhrData.data.items

            }, function(err) {

               // Handle the error condition
               console.error('error: ' + err)
            })
         }
      },

   },

   watch: {

      /**
       * 
       * @param {partial search text} xhrSearchText 
       */
      searchInputSync: function(xhrSearchText) {

         console.log('searchInputSync')

         if (xhrSearchText) {
            xhrSearchText = xhrSearchText.trim()
         }

         if (!xhrSearchText) {
            // console.info('Search text is empty')
            return
         }

         var _this = this

         var queryData = {}
         queryData[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_PROPOSALS
         // xhrData[SearchMgr.XHR_PARAM_SEARCH_TEXT] = encodeURIComponent(xhrSearchText)
         queryData[SearchMgr.XHR_PARAM_SEARCH_TEXT] = xhrSearchText

         this.axios.request({

            url: SearchMgr.XHR_SEARCH_HANDLER,
            method: 'post',
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            timeout: SearchMgr.XHR_TIMEOUT,

            data: queryData,

         }).then(function(xhrData) {

            _this.allSearchProposal = xhrData.data.items

         }, function(err) {

            // Handle the error condition
            console.error('error: ' + err)
         })
      },
   }

}
