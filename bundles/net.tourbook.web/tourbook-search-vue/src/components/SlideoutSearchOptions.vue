<template>
               
   <VuePopper
      class="app-tooltip"
      trigger="hover"
      :delay-on-mouse-out=10
      :visible-arrow=false
      :options="{
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

         <div id="${id}" style="white-space: nowrap;">
         	
         	<div class="dijitDialogPaneContentArea">
         	
         		<div id="domSearchStatus" style="padding-bottom:0.2em;"></div>
         	
         	
         		<!-- Group: Content -->
         		
         		<div 
         			data-dojo-type="dijit/TitlePane" 
         			data-dojo-props="toggleable:false, title: '$t('message.Search_Options_Group_Content}'">
         			
         			<!-- Show All (tour, marker, waypoint)-->
         			
         			<div>
                     <v-checkbox
                        :label="$t('message.Search_Options_Checkbox_ShowContentAll')"
                        v-model="apChkShowContentAll"
                        @click="apSearchAll"
                     ></v-checkbox>

         				<input 
         					type="checkbox" 
         					id="apChkShowContentAll"
         					data-dojo-type="dijit/form/CheckBox" 
         					data-dojo-attach-point="apChkShowContentAll"
         					data-dojo-attach-event="click:apSearchAll"/> 
         				<label for="apChkShowContentAll" v-html="$t('message.Search_Options_Checkbox_ShowContentAll')"></label>
         			</div>
         			
         			<div class="fieldVertSpaceTop" style="padding-left:1em;">
         			
         				<!-- Show tours -->
         				<input 
         					type="checkbox" 
         					id="apChkShowContentTour"
         					data-dojo-type="dijit/form/CheckBox" 
         					data-dojo-attach-point="apChkShowContentTour"
         					data-dojo-attach-event="click:apSelection"/> 
         				<label for="apChkShowContentTour" v-html="$t('message.Search_Options_Checkbox_ShowContentTour')"></label>
         
         
         				<!-- Show marker -->
         				<input 
         					type="checkbox" 
         					id="apChkShowContentMarker"
         					data-dojo-type="dijit/form/CheckBox" 
         					data-dojo-attach-point="apChkShowContentMarker"
         					data-dojo-attach-event="click:apSelection"/> 
         				<label for="apChkShowContentMarker" v-html="$t('message.Search_Options_Checkbox_ShowContentMarker')"></label>
         
         				<!-- Show waypoints -->
         				<input 
         					type="checkbox" 
         					id="apChkShowContentWaypoint"
         					data-dojo-type="dijit/form/CheckBox" 
         					data-dojo-attach-point="apChkShowContentWaypoint"
         					data-dojo-attach-event="click:apSelection"/> 
         				<label for="apChkShowContentWaypoint" v-html="$t('message.Search_Options_Checkbox_ShowContentWaypoint')"></label>
         				
         			</div>
         			
         			<!-- Do ease searching -->
         			<div class="fieldVertSpaceTop">
         				<input 
         					type="checkbox" 
         					id="apChkEaseSearching"
         					data-dojo-type="dijit/form/CheckBox" 
         					data-dojo-attach-point="apChkEaseSearching"
         					data-dojo-attach-event="click:apSelection"/> 
         				<label 
         					id="apChkEaseSearching_Label"
         					for="apChkEaseSearching" v-html="$t('message.Search_Options_Checkbox_EaseSearching')"></label>
         				<div 
         					data-dojo-type="dijit/Tooltip" 
         					data-dojo-attach-point="apChk_EaseSearching_Tooltip"
         					data-dojo-props="
         						connectId:['apChkEaseSearching_Label'],
         						position:['below']"></div>
         			</div>
         		</div>
         		
         		<br>
         		
         		
         		<!-- Group: Sorting -->
         		
         		<div 
         			data-dojo-type="dijit/TitlePane" 
         			data-dojo-props="toggleable:false, title: '$t('message.Search_Options_Group_Sorting')'">
         			
         			<table>
         			
         				<!-- Sort by date -->
         				<tr>
         					<td style="padding-right:2em;">
         						$t('message.Search_Options_Label_SortAscending')					
         					</td>
         					<td>
         					
         						<!-- sort ascending -->
         						<input 
         							type="radio" 
         					    	name="sorting" 
         							id="apSortByDateAscending"
         							data-dojo-type="dijit/form/RadioButton"
         							data-dojo-attach-point="apSortByDateAscending"
         							data-dojo-attach-event="change:apSelection"/>
         						<label for="apSortByDateAscending" v-html="$t('message.Search_Options_Radio_SortAscending')"></label>
         					
         						<!-- sort descending -->
         						<input 
         							type="radio" 
         					    	name="sorting" 
         							id="apSortByDateDescending"
         							data-dojo-type="dijit/form/RadioButton"
         							data-dojo-attach-point="apSortByDateDescending"
         							data-dojo-attach-event="change:apSelection"/>
         						<label for="apSortByDateDescending" v-html="$t('message.Search_Options_Radio_SortDescending')"></label>
         					</td>
         				</tr>
         			</table>
         		</div>
         		
         		<br>
         		
         		
         		<!-- Group: Result -->
         		
         		<div 
         			data-dojo-type	="dijit/TitlePane" 
         			data-dojo-props	="toggleable:false, title: '$t('message.Search_Options_Group_Result')'">
         			
         			<!-- Show description -->
         			<div>
         				<input 
         					type					="checkbox" 
         					id						="apChkShowDescription"
         					data-dojo-attach-point	="apChkShowDescription"
         					data-dojo-type			="dijit/form/CheckBox" 
         					data-dojo-attach-event	="click:apSelection"/> 
         				<label for="apChkShowDescription" v-html="$t('message.Search_Options_Checkbox_ShowDescription')"></label>
         				<br>
         			</div>
         			
         			<!-- Show date -->
         			<div class="fieldVertSpaceTop">
         				<input 
         					type					="checkbox" 
         					id						="apChkShowDate"
         					data-dojo-attach-point	="apChkShowDate"
         					data-dojo-type			="dijit/form/CheckBox" 
         					data-dojo-attach-event	="click:apSelection"/> 
         				<label for="apChkShowDate" v-html="$t('message.Search_Options_Checkbox_ShowDate')"></label>
         				<br>
         			</div>
         			
         			<!-- Show time -->
         			<div class="fieldVertSpaceTop">
         				<input 
         					type					="checkbox" 
         					id						="apChkShowTime"
         					data-dojo-attach-point	="apChkShowTime"
         					data-dojo-type			="dijit/form/CheckBox" 
         					data-dojo-attach-event	="click:apSelection"/> 
         				<label for="apChkShowTime" v-html="$t('message.Search_Options_Checkbox_ShowTime')"></label>
         				<br>
         			</div>
         			
         			<!-- Show item number -->
         			<div class="fieldVertSpaceTop">
         				<input 
         					type					="checkbox" 
         					id						="apChkShowItemNumber"
         					data-dojo-attach-point	="apChkShowItemNumber"
         					data-dojo-type			="dijit/form/CheckBox" 
         					data-dojo-attach-event	="click:apSelection"/> 
         				<label for="apChkShowItemNumber" v-html="$t('message.Search_Options_Checkbox_ShowItemNumber')"></label>
         			</div>
         			
         			<!-- Show Lucene doc ID -->
         			<div class="fieldVertSpaceTop">
         				<input 
         					type					="checkbox" 
         					id						="apChkShowLuceneID"
         					data-dojo-attach-point	="apChkShowLuceneID"
         					data-dojo-type			="dijit/form/CheckBox" 
         					data-dojo-attach-event	="click:apSelection"/> 
         				<label for="apChkShowLuceneID" v-html="$t('message.Search_Options_Checkbox_ShowLuceneDocId')"></label>
         			</div>
         		</div>
         		
         		<br>
         		
         		<!-- Actions -->
         		<div style="text-align:right;">
         			<span 
         				id						="apActionDefaults"
         				data-dojo-attach-point	="apActionDefaults"
         				data-dojo-type			="dijit/form/Button"
         				data-dojo-attach-event	="click:apActionRestoreDefaults"
         				 v-html="$t('message.Search_Options_Action_RestoreDefaults')"></span> 
         		</div>
         	</div>
         </div>

      </div>

   </VuePopper>
   
</template>

<script>
//
import VuePopper from 'vue-popperjs'

export default {
   //
   components: {
      VuePopper,
   },

   data: () => ({

      // prettier-ignore
   }),

   methods : {

		/**
		 * Search all checkbox
		 */
		apSearchAll : function apSearchAll() {
debugger
			this._enableControls();

			// fire selection
			this.apSelection();
      },
      
		_enableControls : function _enableControls() {

			// var isShowContentAll = this.apChkShowContentAll.get('checked');

			// this.apChkShowContentTour.set('disabled', isShowContentAll);
			// this.apChkShowContentMarker.set('disabled', isShowContentAll);
			// this.apChkShowContentWaypoint.set('disabled', isShowContentAll);
		},

   },
}
</script>

<style>
/* @import '../assets/search.css'; */
</style>
