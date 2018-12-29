<template>

   <v-app>
  
      <table style="width: 100%; xborder:1px solid;">
         <tr>
            <td style="width:24px;">
               <v-tooltip bottom>
                  <div slot="activator" id="domInfo" class="actionIcon iconPhotoTooltip" tabindex="1">&nbsp;</div>
                  <div>some text 2</div>
               </v-tooltip>
            </td>
            <td style="">
               <v-autocomplete 
                  :label="Messages.Search_App_Text_Search_PlaceHolder"
                  :items="searchItems"
                  item-text="itemText"
                  autofocus

                  class="no-transition"
                  tabindex="2"
               />
               <div id="domSearchInput"></div>
            </td>
            <td style="width:24px;">
               <div slot="activator" id="domActionSearch" class="actionIcon iconSearch" tabindex="3">&nbsp;</div>
            </td>
            <td style="width:80px;">
               <div id="domAppStatus" style="padding-left: 0.2em;">&nbsp;</div>
            </td>
            <td style="width:24px;">
               <div slot="activator" id="domAction_Options" class="actionIcon iconOptions" tabindex="4">&nbsp;</div>
            </td>
         </tr>
      </table>
      <v-container>
      </v-container>
   </v-app>
</template>

<script>


export default {
	
  name: "App",
  components: {

  },
  
  data: () => ({

       value:'this is a value',

       searchItems: [

          {itemText: 'abc 1', id: 1},
          {itemText: 'bcd 2', id: 2},
          {itemText: 'cde 3', id: 3},
          {itemText: 'def 4', id: 4},
       ],

       Messages: {
          Search_App_Text_Search_PlaceHolder : 'Search Tours, Marker and Waypoints'
       }
    })
};
</script>

<style>

@import './assets/search.css'; 

</style> 
