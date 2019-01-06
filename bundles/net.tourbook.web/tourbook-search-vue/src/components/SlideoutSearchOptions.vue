
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
      apActionRestoreDefaults: function() {
         //
         // this._setSearchOptions(
         //    //
         //    {
         //       isRestoreDefaults: true
         //    }
         // )
      },

      /**
       * Search all checkbox
       */
      apSearchAll: function apSearchAll() {
         //
         this._enableControls()

         // fire selection
         this.apSelection()
      },

      /**
       * Selection is from an attach point.
       */
      apSelection: function() {
         //
         // if (this._isValid()) {
         // 	var searchOptions = //
         // 	{
         // 		isEaseSearching : this.apChkEaseSearching.get('checked'),
         // 		isShowContentAll : this.apChkShowContentAll.get('checked'),
         // 		isShowContentTour : this.apChkShowContentTour.get('checked'),
         // 		isShowContentMarker : this.apChkShowContentMarker.get('checked'),
         // 		isShowContentWaypoint : this.apChkShowContentWaypoint.get('checked'),
         // 		isSortByDateAscending : this.apSortByDateAscending.get('checked'),
         // 		isShowDate : this.apChkShowDate.get('checked'),
         // 		isShowTime : this.apChkShowTime.get('checked'),
         // 		isShowDescription : this.apChkShowDescription.get('checked'),
         // 		isShowItemNumber : this.apChkShowItemNumber.get('checked'),
         // 		isShowLuceneID : this.apChkShowLuceneID.get('checked')
         // 	};
         // 	this._setSearchOptions(searchOptions);
         // }
      },

      _enableControls: function() {
         //
         // var isShowContentAll = this.apChkShowContentAll.get('checked');
         // this.apChkShowContentTour.set('disabled', isShowContentAll);
         // this.apChkShowContentMarker.set('disabled', isShowContentAll);
         // this.apChkShowContentWaypoint.set('disabled', isShowContentAll);
      }
   }
}
</script>

