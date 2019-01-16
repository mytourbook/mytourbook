import VuePopper from 'vue-popperjs'
import SlideoutSearchOptions from './components/SlideoutSearchOptions'

import SearchMgr from './SearchMgr'

export default {
   //
   name: 'App',

   components: {
      SlideoutSearchOptions,
      VuePopper,
   },

   data: () => ({

      SearchMgr,

      searchItems: [],
      loadProposals: null,
   }),

   watch: {

      loadProposals: function(xhrSearchText) {

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

            _this.searchItems = xhrData.data.items

         }, function(err) {

            // Handle the error condition
            console.error('error: ' + err)
         })
      },
   }

}
