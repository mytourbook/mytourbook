
<template>

   <VuePopper
      class                ="app-tooltip"
      trigger              ="hover"
      :visible-arrow       =false
      :delay-on-mouse-out  =1000000
      :options             ="{
         //
         placement: 'bottom-end',
         modifiers: { 
            
            offset: { offset: '0, -5px' } ,
            flip: { enabled: false }
         }
      }">

      <button slot="reference">
         <div slot="activator" id="domAction_Options" class="actionIcon iconOptions" tabindex="4"></div>
      </button>

      <div class="popper">
      
         <v-toolbar dark color="primary"
            height="22"
            flat
         >
         <v-toolbar-title class="white--text">{{$t('message.Search_Options_Dialog_Header')}}</v-toolbar-title>
         </v-toolbar>


         <div style="padding:10px;">
            
            <!-- <div id="domSearchStatus" style="padding-bottom:0.2em;"></div> -->
            
            
            <!-- Group: What should be searched -->

            <v-subheader>{{$t('message.Search_Options_Group_Content')}}</v-subheader>

            <div>
               
               <!-- Show All (tour, marker, waypoint)-->

               <v-checkbox
                  :label      ="$t('message.Search_Options_Checkbox_ShowContentAll')"
                  v-model     ="apChkShowContentAll"
                  @click      ="apSearchAll"
                  hide-details
               />
               
               <v-layout row>
                  
                  <v-checkbox
                     :label      ="$t('message.Search_Options_Checkbox_ShowContentTour')"
                     style       ="padding-left:2em;"
                     hide-details
                     v-model     ="apChkShowContentTour"
                     @click      ="apSelection"
                     :disabled   ="apChkShowContentAll === true"
                     />
                     
                     <v-checkbox
                     :label      ="$t('message.Search_Options_Checkbox_ShowContentMarker')"
                     hide-details
                     v-model     ="apChkShowContentMarker"
                     @click      ="apSelection"
                     :disabled   ="apChkShowContentAll === true"
                     />
                     
                     
                     <v-checkbox
                     :label      ="$t('message.Search_Options_Checkbox_ShowContentWaypoint')"
                     hide-details
                     v-model     ="apChkShowContentWaypoint"
                     @click      ="apSelection"
                     :disabled   ="apChkShowContentAll === true"
                  />

               </v-layout>

               <!-- Ease searching -->
               <VuePopper
                  class                   ="app-tooltip"
                  trigger                 ="hover"
                  :visible-arrow          =false
                  :delay-on-mouse-over    =200
                  :delay-on-mouse-out     =0
                  >

                  <div class="popper" style="width:350px; padding:10px;">
                     <div v-html="$t('message.Search_Options_Checkbox_EaseSearching_Tooltip')"></div>
                  </div>

                  <v-checkbox
                     slot        ="reference"
                     :label      ="$t('message.Search_Options_Checkbox_EaseSearching')"
                     v-model     ="apChkEaseSearching"
                     @click=     "apSelection"
                     hide-details
                     />
                  </VuePopper>
            </div>

            <hr class="divider">


            <!-- Group: Sorting -->

            <div>

               <v-subheader>{{$t('message.Search_Options_Group_Sorting')}}</v-subheader>
               
               <v-radio-group 
                  :label      ="$t('message.Search_Options_Label_SortAscending')"
                  v-model     ="apSortByDate" 
                  :mandatory  ="false" 
                  :column     ="false" 
                  style       ="white-space: nowrap;"
                  @click      ="apSelection"
                  hide-details
               >
                  <v-radio :label="$t('message.Search_Options_Radio_SortAscending')"   value="ascending"    style="padding-left:2em;"/>
                  <v-radio :label="$t('message.Search_Options_Radio_SortDescending')"  value="descending"/>
               </v-radio-group>
            </div>         

            <hr class="divider">

            <!-- Group: Result -->

            <div>


               <v-subheader>{{$t('message.Search_Options_Group_Result')}}</v-subheader>

               <!-- Show description -->
               <v-checkbox
                  :label      ="$t('message.Search_Options_Checkbox_ShowDescription')"
                  v-model     ="apChkShowDescription"
                  @click      ="apSelection"
                  hide-details
               />
               
               <!-- Show date -->
               <v-checkbox
                  :label      ="$t('message.Search_Options_Checkbox_ShowDate')"
                  v-model     ="apChkShowDate"
                  @click      ="apSelection"
                  hide-details
               />
               
               <!-- Show time -->
               <v-checkbox
                  :label      ="$t('message.Search_Options_Checkbox_ShowTime')"
                  v-model     ="apChkShowTime"
                  @click      ="apSelection"
                  hide-details
               />
               
               <!-- Show item number -->
               <v-checkbox
                  :label      ="$t('message.Search_Options_Checkbox_ShowItemNumber')"
                  v-model     ="apChkShowItemNumber"
                  @click      ="apSelection"
                  hide-details
               />
               
               <!-- Show Lucene doc ID -->
               <v-checkbox
                  :label      ="$t('message.Search_Options_Checkbox_ShowLuceneDocId')"
                  v-model     ="apChkShowLuceneID"
                  @click      ="apSelection"
                  hide-details
                  />
                  
               </div>   
               
               <hr class="divider">
               
               <!-- Actions -->
               
               <div style="text-align:right;">
                  <v-btn
                     style    ="text-align:right;"
                     @click   ="apActionRestoreDefaults"
                     small
                  >{{$t('message.Search_Options_Action_RestoreDefaults')}}</v-btn>
               </div>
         </div>

      </div>

   </VuePopper>


</template>


<script>

import VuePopper from 'vue-popperjs'
import SearchMgr from '../SearchMgr'
import Axios from 'axios';

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
            });
      },

      /**
       * Search all checkbox
       */
      apSearchAll: function() {

         // this._enableControls();

         // // fire selection
         // this.apSelection();
      },

      /**
       * Selection is from an attach point.
       */
      apSelection: function() {

         if (this._isValid()) {

            var searchOptions = //
               {
                  // isEaseSearching: this.apChkEaseSearching.get('checked'),

                  // isShowContentAll: this.apChkShowContentAll.get('checked'),
                  // isShowContentTour: this.apChkShowContentTour.get('checked'),
                  // isShowContentMarker: this.apChkShowContentMarker.get('checked'),
                  // isShowContentWaypoint: this.apChkShowContentWaypoint.get('checked'),

                  // isSortByDateAscending: this.apSortByDateAscending.get('checked'),

                  // isShowDate: this.apChkShowDate.get('checked'),
                  // isShowTime: this.apChkShowTime.get('checked'),
                  // isShowDescription: this.apChkShowDescription.get('checked'),
                  // isShowItemNumber: this.apChkShowItemNumber.get('checked'),
                  // isShowLuceneID: this.apChkShowLuceneID.get('checked')
               };

            this._setSearchOptions(searchOptions);
         }
      },

      _isValid: function() {
         
            var statusText = ''
            var isValid = true

            // isShowContentAll = this.apChkShowContentAll.get('checked'), //
            // isShowContentTour = this.apChkShowContentTour.get('checked'), //
            // isShowContentMarker = this.apChkShowContentMarker.get('checked'), //
            // isShowContentWaypoint = this.apChkShowContentWaypoint.get('checked');

         if (isShowContentAll) {

            // content is valid

         } else {

            // at least one content must be checked

            if (isShowContentTour == false
               && isShowContentMarker == false
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

      _enableControls: function() {

         // var isShowContentAll = this.apChkShowContentAll.get('checked');

         // this.apChkShowContentTour.set('disabled', isShowContentAll);
         // this.apChkShowContentMarker.set('disabled', isShowContentAll);
         // this.apChkShowContentWaypoint.set('disabled', isShowContentAll);
      },

      /**
       * 
       */
      _restoreState: function(callBack) {

         var _this = this;

         var xhrData = {};
         xhrData[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_GET_SEARCH_OPTIONS;

         this.axios.request({

               url: SearchMgr.XHR_SEARCH_HANDLER,
               method: 'post',

               headers: { 'X-Requested-With': 'XMLHttpRequest' },
               timeout: SearchMgr.XHR_TIMEOUT, // default is `0` (no timeout)

               data: xhrData,
            }

         ).then(function(response) {

            // console.log('data:')
            // console.log(response.data);
            
            // console.log('status:');
            // console.log(response.status);
            
            // console.log('statusText:');
            // console.log(response.statusText);

            // console.log('headers:')
            // console.log(response.headers);

            // console.log('config:');
            // console.log(response.config);

         }).catch(function(error) {
// debugger
            console.log(error);
         })

         // xhr(SearchMgr.XHR_SEARCH_HANDLER, {

         //    handleAs: 'json',
         //    preventCache: true,
         //    timeout: SearchMgr.XHR_TIMEOUT,

         //    query: xhrQuery

         // }).then(function(xhrData) {

         //    _this._updateUI_FromState(_this, xhrData);
         // });
      },

      /**
       * Set search options in the backend and reload current search with new search options.
       */
      _setSearchOptions: function(searchOptions) {

         var _this = this;

         var jsonSearchOptions = JSON.stringify(searchOptions);

         var xhrQuery = {};
         xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SET_SEARCH_OPTIONS;
         xhrQuery[SearchMgr.XHR_PARAM_SEARCH_OPTIONS] = encodeURIComponent(jsonSearchOptions);

         xhr(SearchMgr.XHR_SEARCH_HANDLER, {

            handleAs: 'json',
            preventCache: true,
            timeout: SearchMgr.XHR_TIMEOUT,

            query: xhrQuery

         }).then(function(xhrData) {

            if (xhrData.isSearchOptionsDefault) {

               // set defaults in the UI
               _this._updateUI_FromState(_this, xhrData);
            }

            // repeat previous search

            _this._searchApp._searchInput.startSearch(true);
         });
      },

      _updateUI_FromState: function(dialog, xhrData) {

         // dialog.apChkEaseSearching.set('checked', xhrData.isEaseSearching);

         // dialog.apChkShowContentAll.set('checked', xhrData.isShowContentAll);
         // dialog.apChkShowContentTour.set('checked', xhrData.isShowContentTour);
         // dialog.apChkShowContentMarker.set('checked', xhrData.isShowContentMarker);
         // dialog.apChkShowContentWaypoint.set('checked', xhrData.isShowContentWaypoint);

         // dialog.apSortByDateAscending.set('checked', xhrData.isSortByDateAscending);
         // dialog.apSortByDateDescending.set('checked', !xhrData.isSortByDateAscending);

         // dialog.apChkShowDate.set('checked', xhrData.isShowDate);
         // dialog.apChkShowTime.set('checked', xhrData.isShowTime);
         // dialog.apChkShowDescription.set('checked', xhrData.isShowDescription);
         // dialog.apChkShowItemNumber.set('checked', xhrData.isShowItemNumber);
         // dialog.apChkShowLuceneID.set('checked', xhrData.isShowLuceneID);

         // dialog._enableControls();
         // dialog._isValid();
      }
   }
}


</script>


<style>/* @import '../assets/search.css'; */</style> 



