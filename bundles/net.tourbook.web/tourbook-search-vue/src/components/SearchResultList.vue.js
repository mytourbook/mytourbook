import SearchMgr from '../SearchMgr'

import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

export default {

   props: {
      allSearchResultItems: Array
   },

   components: {

   },

   data: () => ({

   }),

   methods: {

      onUpdate_SearchResultList: function(startIndex, endIndex) {

         console.log('onUpdate_SearchResultList: ' + startIndex + ' - ' + endIndex)
      }

   }
}
