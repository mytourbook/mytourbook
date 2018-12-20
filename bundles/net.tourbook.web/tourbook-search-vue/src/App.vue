<template>

  <v-app>
   <table style="width: 100%">
      <tr>
         <td>
            <div id="domInfo" class="actionIcon iconPhotoTooltip" tabindex="0">&nbsp;</div>
         </td>
         <td style="width: 99%; padding-left: 0.2em; padding-right: 0.4em;">
            <v-autocomplete
               label="label"
               :items="items"
               v-model="value"
            ></v-autocomplete>
            <div id="domSearchInput"></div>
         </td>
         <td style="margin-right: 0.2em">
            <div id="domAction_StartSearch"></div>
         </td>
         <td>
            <div id="domAppStatus" style="padding-left: 0.2em;">&nbsp;</div>
         </td>
         <td>
            <div id="domAction_SearchOptions"></div>
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
