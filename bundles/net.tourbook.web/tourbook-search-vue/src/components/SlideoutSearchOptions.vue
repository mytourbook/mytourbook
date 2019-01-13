
<template>

   <VuePopper
      class                = "app-tooltip"
      trigger              = "hover"
      :visible-arrow       = false
      :delay-on-mouse-out  = 100
      :options             = "{
         
         placement:     'bottom-end',
         modifiers: 
         { 
            offset   : { offset: '0px, -5px' } ,
            flip     : { enabled: false },
         },
      }"

      v-on:show = "vm_SearchOptions_Show"
      v-on:hide = "vm_SearchOptions_Hide"
   >

      <button slot="reference">
         <div slot="activator" ref="ref_SearchOptions_Reference" class="actionIcon iconOptions" tabindex="4"></div>
      </button>

      <div class="popper">
      
         <v-toolbar dark color="primary"
            height="22"
            flat
         >
            <v-toolbar-title class="white--text">{{$t('message.Search_Options_Dialog_Header')}}</v-toolbar-title>
         </v-toolbar>


         <div style="padding:10px;">
            
            <p style="xpadding-bottom:1em; color:#f00; max-width:250px;"
               v-if="ap_SearchStatus !== ''"
            >{{ap_SearchStatus}}</p>
            
            
            <!-- Group: What should be searched -->

            <v-subheader>{{$t('message.Search_Options_Group_Content')}}</v-subheader>

            <div>
               
               <!-- Show All (tour, marker, waypoint)-->

               <v-checkbox
                  :label      = "$t('message.Search_Options_Checkbox_ShowContentAll')"
                  v-model     = "apChkShowContentAll"
                  @click      = "apSelection"
                  hide-details
               />
               
               <v-layout row>
                  
                  <v-checkbox
                     :label      = "$t('message.Search_Options_Checkbox_ShowContentTour')"
                     style       = "padding-left:2em;"
                     hide-details
                     v-model     = "apChkShowContentTour"
                     @click      = "apSelection"
                     :disabled   = "apChkShowContentAll === true"
                     />
                     
                     <v-checkbox
                     :label      = "$t('message.Search_Options_Checkbox_ShowContentMarker')"
                     hide-details
                     v-model     = "apChkShowContentMarker"
                     @click      = "apSelection"
                     :disabled   = "apChkShowContentAll === true"
                     />
                     
                     
                     <v-checkbox
                     :label      = "$t('message.Search_Options_Checkbox_ShowContentWaypoint')"
                     hide-details
                     v-model     = "apChkShowContentWaypoint"
                     @click      = "apSelection"
                     :disabled   = "apChkShowContentAll === true"
                  />

               </v-layout>

               <!-- Ease searching -->
               <VuePopper
                  class                   = "app-tooltip"
                  trigger                 = "hover"
                  :visible-arrow          = false
                  :delay-on-mouse-over    = 200
                  :delay-on-mouse-out     = 0
                  >

                  <div class="popper" style="width:350px; padding:10px;">
                     <div v-html="$t('message.Search_Options_Checkbox_EaseSearching_Tooltip')"></div>
                  </div>

                  <v-checkbox
                     slot        = "reference"
                     :label      = "$t('message.Search_Options_Checkbox_EaseSearching')"
                     v-model     = "apChkEaseSearching"
                     @click      = "apSelection"
                     hide-details
                     />
                  </VuePopper>
            </div>

            <hr class="divider">


            <!-- Group: Sorting -->

            <div>

               <v-subheader>{{$t('message.Search_Options_Group_Sorting')}}</v-subheader>
               
               <v-radio-group 
                  :label      = "$t('message.Search_Options_Label_SortAscending')"
                  v-model     = "apSortByDate" 
                  :mandatory  = "false" 
                  :column     = "false" 
                  style       = "white-space: nowrap;"
                  @click      = "apSelection"
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
                  :label      = "$t('message.Search_Options_Checkbox_ShowDescription')"
                  v-model     = "apChkShowDescription"
                  @click      = "apSelection"
                  hide-details
               />
               
               <!-- Show date -->
               <v-checkbox
                  :label      = "$t('message.Search_Options_Checkbox_ShowDate')"
                  v-model     = "apChkShowDate"
                  @click      = "apSelection"
                  hide-details
               />
               
               <!-- Show time -->
               <v-checkbox
                  :label      = "$t('message.Search_Options_Checkbox_ShowTime')"
                  v-model     = "apChkShowTime"
                  @click      = "apSelection"
                  hide-details
               />
               
               <!-- Show item number -->
               <v-checkbox
                  :label      = "$t('message.Search_Options_Checkbox_ShowItemNumber')"
                  v-model     = "apChkShowItemNumber"
                  @click      = "apSelection"
                  hide-details
               />
               
               <!-- Show Lucene doc ID -->
               <v-checkbox
                  :label      = "$t('message.Search_Options_Checkbox_ShowLuceneDocId')"
                  v-model     = "apChkShowLuceneID"
                  @click      = "apSelection"
                  hide-details
                  />
                  
               </div>   
               
               <hr class="divider">
               
               <!-- Actions -->
               
               <div style="text-align:right;">
                  <v-btn
                     style    = "text-align:right;"
                     @click   = "apActionRestoreDefaults"
                     small
                  >{{$t('message.Search_Options_Action_RestoreDefaults')}}</v-btn>
               </div>
         </div>

      </div>

   </VuePopper>


</template>


<script src="./SlideoutSearchOptions.vue.js"></script>

<style>/* @import '../assets/search.css'; */</style> 



