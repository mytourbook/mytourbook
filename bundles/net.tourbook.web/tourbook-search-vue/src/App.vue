<template>

   <v-app>
  
      <table style="width: 100%; xborder:1px solid;">
         <tr>
            <td style="width:24px;">
               <v-tooltip bottom>
                  <div slot="activator" id="domInfo" class="actionIcon iconPhotoTooltip" tabindex="0">&nbsp;</div>
                  <span>Tooltip</span>
               </v-tooltip>
            </td>
            <td style="">
               <v-autocomplete
                  label="Search Tours, Marker and Waypoints"
                  :items="items"
                  v-model="value"
               ></v-autocomplete>
               <div id="domSearchInput"></div>
            </td>
            <td style="width:24px;">
               <div slot="activator" id="domActionSearch" class="actionIcon iconSearch" tabindex="10">&nbsp;</div>
            </td>
            <td style="width:80px;">
               <div id="domAppStatus" style="padding-left: 0.2em;">&nbsp;</div>
            </td>
            <td style="width:24px;">
               <div slot="activator" id="domAction_Options" class="actionIcon iconOptions" tabindex="20">&nbsp;</div>
            </td>
         </tr>
      </table>
      <v-container>
      </v-container>
   </v-app>
</template>

<script>

import HelloWorld from "./components/HelloWorld";

export default {
	
  name: "App",
  components: {
    HelloWorld
  },
  
  data() {
    return {
      //
    };
  }
};
</script>

<style> 

@import './assets/search.css'; 

</style> 
