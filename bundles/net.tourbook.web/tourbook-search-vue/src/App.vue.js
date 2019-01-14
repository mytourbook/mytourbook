
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

      searchItems: 
      [
         { itemText: 'abc 1', id: 1 }, 
         { itemText: 'bcd 2', id: 2 }, 
         { itemText: 'cde 3', id: 3 }, 
         { itemText: 'def 4', id: 4 },
      ],
   })
}
