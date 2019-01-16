<template>
   <v-app>
  
      <table style="width: 100%; xborder:1px solid;">
         <tr>
            <td style="width:24px;">
               
               <VuePopper
                  class="app-tooltip"
                  trigger="hover"
                  :delay-on-mouse-out=10
                  :visible-arrow=false
                  :options="{
                  
                     placement: 'bottom',
                     modifiers: 
                     { 
                        offset: { offset: '0, -5px' } ,
                        flip: { enabled: false }
                     }
                  }">

                  <div class="popper" style="width:350px; padding:10px;">
                     <div v-html="$t('message.Search_App_Tooltip', [SearchMgr.Search_App_Tooltip_Url])"></div>
                  </div>

                  <button slot="reference">
                     <div slot="activator" id="domInfo" class="actionIcon iconPhotoTooltip" tabindex="1"></div>
                  </button>

               </VuePopper>
            </td>
            <td style="">

               <v-autocomplete 
                  :label               ="$t('message.Search_App_Text_Search_PlaceHolder')"
                  :items               ="searchItems"

                  :search-input.sync   ="loadProposals"

                  item-text   ="name"
                  ref         ="ref_SearchInput"
                  autofocus

                  class="no-transition"
                  tabindex="2"

                  :menu-props =" {
                                    maxHeight:'70vh'
                                 }"
               />
               <div id="domSearchInput"></div>
            </td>
            <td style="width:24px;">
               <div slot="activator" id="domActionSearch" class="actionIcon iconSearch" tabindex="3">&nbsp;</div>
            </td>
            <td style="width:80px;">
               <div id="domAppStatus" style="padding-left: 0.2em;">&nbsp;</div>
               <!-- <div>{{ $t('message.ftState') }}</div> -->
            </td>
            
            <td style="width:24px;">
               <SlideoutSearchOptions/>
            </td>
         </tr>
      </table>

      <v-container> 
      </v-container>

   </v-app>
</template>

<script src="./App.vue.js"></script>

<style>
@import './assets/search.css';
</style>
