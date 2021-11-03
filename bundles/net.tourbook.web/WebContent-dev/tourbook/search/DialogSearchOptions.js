define(
[
   'dojo/_base/declare',
   "dojo/_base/lang",
   'dojo/dom',
   'dojo/on',
   "dojo/request/xhr",

   'dijit/form/NumberSpinner',
   'dijit/form/RadioButton',
   'dijit/TitlePane',

   '../widget/BaseDialog',
   './SearchMgr',

   'dojo/text!./DialogSearchOptions.html',
   'dojo/i18n!./nls/Messages'

], function(

declare, 
lang, 
dom, 
on, 
xhr, 

// these widgets MUST be preloaded when used in the template
NumberSpinner, 
RadioButton, 
TitlePane,

BaseDialog, 
SearchMgr, 

template, 
Messages 

) {

   var dlgSearchOptions = declare('tourbook.search.DialogSearchOptions',
   [ BaseDialog
   ], {

      templateString : template,

      // create messages field which is needed that messages can be accessed in the template
      messages : Messages,

      constructor : function(args) {
         this._searchApp = args.searchApp
      },

      postCreate : function() {

         var dlg = this

         this.inherited(arguments)

         /*
          * Tooltips with html tags must be defined in the js code, otherwise the TAGs do not work
          */

         this.apChk_EaseSearching_Tooltip.label             = Messages.Search_Options_Checkbox_EaseSearching_Tooltip

         this.apChkSearch_Tour_Tooltip.label                = Messages.Search_Options_Checkbox_Search_Tour_Tooltip
         this.apChkSearch_Tour_LocationStart_Tooltip.label  = Messages.Search_Options_Checkbox_Search_Tour_LocationStart_Tooltip
         this.apChkSearch_Tour_LocationEnd_Tooltip.label    = Messages.Search_Options_Checkbox_Search_Tour_LocationEnd_Tooltip
         this.apChkSearch_Tour_Weather_Tooltip.label        = Messages.Search_Options_Checkbox_Search_Tour_Weather_Tooltip
         
         this.apChkSearch_Marker_Tooltip.label              = Messages.Search_Options_Checkbox_Search_Marker_Tooltip
         this.apChkSearch_Waypoint_Tooltip.label            = Messages.Search_Options_Checkbox_Search_Waypoint_Tooltip
      },
      
      /**
       * Overwrite BaseDialog.showDialog and restore the state of the UI.
       */
      showDialog : function showDialog(args) {
 
         this.inherited(arguments)

         this._restoreState()
      },

      apActionRestoreDefaults : function apActionRestoreDefaults() {

         this._setSearchOptions(
         {
            isRestoreDefaults : true
         });
      },

      /**
       * Search all checkbox
       */
      apSearchAll : function apSearchAll() {

         this._enableControls()

         // fire selection
         this.apSelection()
      },

      /**
       * Selection is from an attach point.
       */
      apSelection : function apSelection() {

         if (this._isValid()) {

            var searchOptions = 
            {
               isEaseSearching               : this.apChkEaseSearching.get('checked'),

               isSearch_All                  : this.apChkSearch_All.get('checked'),

               isSearch_Tour                 : this.apChkSearch_Tour.get('checked'),
               isSearch_Tour_LocationStart   : this.apChkSearch_Tour_LocationStart.get('checked'),
               isSearch_Tour_LocationEnd     : this.apChkSearch_Tour_LocationEnd.get('checked'),
               isSearch_Tour_Weather         : this.apChkSearch_Tour_Weather.get('checked'),

               isSearch_Marker               : this.apChkSearch_Marker.get('checked'),
               isSearch_Waypoint             : this.apChkSearch_Waypoint.get('checked'),
               
               isSortByDateAscending         : this.apSortByDateAscending.get('checked'),
               
               isShowDate                    : this.apChkShowDate.get('checked'),
               isShowTime                    : this.apChkShowTime.get('checked'),
               isShowDescription             : this.apChkShowDescription.get('checked'),
               isShowItemNumber              : this.apChkShowItemNumber.get('checked'),
               isShowLuceneID                : this.apChkShowLuceneID.get('checked')
            };
            
            this._setSearchOptions(searchOptions)
         }
      },
      
      _isValid : function _isValid() {
         
         var 
         statusText                    = '', 
         isValid                       = true, 
         
         isSearch_All                  = this.apChkSearch_All.get('checked'), 

         isSearch_Tour                 = this.apChkSearch_Tour.get('checked'), 
         isSearch_Tour_LocationStart   = this.apChkSearch_Tour_LocationStart.get('checked'),
         isSearch_Tour_LocationEnd     = this.apChkSearch_Tour_LocationEnd.get('checked'),
         isSearch_Tour_Weather         = this.apChkSearch_Tour_Weather.get('checked')

         isSearch_Marker               = this.apChkSearch_Marker.get('checked'), 
         isSearch_Waypoint             = this.apChkSearch_Waypoint.get('checked')
         
         if (isSearch_All) {
            
            // content is valid
            
         } else {
            
            // at least one content must be checked
            
            if (isSearch_Tour == false 
               && isSearch_Tour_LocationStart == false
               && isSearch_Tour_LocationEnd == false
               && isSearch_Tour_Weather == false
               && isSearch_Marker == false 
               && isSearch_Waypoint == false
               ) {
                  
                  statusText = Messages.Search_Validation_SearchFilter
                  isValid = false
               }
            }
            
            // update status
            dom.byId('domSearchStatus').innerHTML = statusText
            
            // resize dialog because status text has changed and can be too long 
            this._dialog.resize();
            
            return isValid;
         },
         
         _enableControls : function _enableControls() {
            
            var isSearch_All = this.apChkSearch_All.get('checked')

            var labelClass = isSearch_All ? 'disableLabel' : ''
            
            this.apChkSearch_Tour                     .set('disabled', isSearch_All)
            this.apChkSearch_Marker                   .set('disabled', isSearch_All)
            this.apChkSearch_Waypoint                 .set('disabled', isSearch_All)
                                                      
            this.apChkSearch_Tour_LocationStart       .set('disabled', isSearch_All)
            this.apChkSearch_Tour_LocationEnd         .set('disabled', isSearch_All)
            this.apChkSearch_Tour_Weather             .set('disabled', isSearch_All)
                                                      
            this.apChkSearch_Tour_Label               .className = labelClass
            this.apChkSearch_Marker_Label             .className = labelClass
            this.apChkSearch_Waypoint_Label           .className = labelClass
            
            this.apChkSearch_Tour_LocationStart_Label .className = labelClass
            this.apChkSearch_Tour_LocationEnd_Label   .className = labelClass
            this.apChkSearch_Tour_Weather_Label       .className = labelClass
      },

      /**
       * 
       */
      _restoreState : function _restoreState(callBack) {

         var _this = this;

         var xhrQuery = {}
         xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_GET_SEARCH_OPTIONS

         xhr(SearchMgr.XHR_SEARCH_HANDLER, {

            handleAs       : 'json',
            preventCache   : true,
            timeout        : SearchMgr.XHR_TIMEOUT,

            query          : xhrQuery

         }).then(function(xhrData) {

            _this._updateUI_FromState(_this, xhrData)
         });
      },

      /**
       * Set search options in the backend and reload current search with new search options.
       */
      _setSearchOptions : function _setSearchOptions(searchOptions) {

         var _this = this
 
         var jsonSearchOptions = JSON.stringify(searchOptions)

         var xhrQuery = {}
         xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SET_SEARCH_OPTIONS
         xhrQuery[SearchMgr.XHR_PARAM_SEARCH_OPTIONS] = encodeURIComponent(jsonSearchOptions)

         xhr(SearchMgr.XHR_SEARCH_HANDLER, {

            handleAs       : 'json',
            preventCache   : true,
            timeout        : SearchMgr.XHR_TIMEOUT,

            query          : xhrQuery

         }).then(function(xhrData) {

            if (xhrData.isSearchOptionsDefault) {

               // set defaults in the UI
               _this._updateUI_FromState(_this, xhrData)
            }

            // repeat previous search

            _this._searchApp._searchInput.startSearch(true)
         });
      },

      _updateUI_FromState : function _updateUI_FromState(dialog, xhrData) {

         dialog.apChkEaseSearching              .set('checked', xhrData.isEaseSearching)

         dialog.apChkSearch_All                 .set('checked', xhrData.isSearch_All)

         dialog.apChkSearch_Tour                .set('checked', xhrData.isSearch_Tour)
         dialog.apChkSearch_Tour_LocationStart  .set('checked', xhrData.isSearch_Tour_LocationStart)
         dialog.apChkSearch_Tour_LocationEnd    .set('checked', xhrData.isSearch_Tour_LocationEnd)
         dialog.apChkSearch_Tour_Weather        .set('checked', xhrData.isSearch_Tour_Weather)

         dialog.apChkSearch_Marker              .set('checked', xhrData.isSearch_Marker)
         dialog.apChkSearch_Waypoint            .set('checked', xhrData.isSearch_Waypoint)

         dialog.apSortByDateAscending           .set('checked', xhrData.isSortByDateAscending)
         dialog.apSortByDateDescending          .set('checked', !xhrData.isSortByDateAscending)

         dialog.apChkShowDate                   .set('checked', xhrData.isShowDate)
         dialog.apChkShowTime                   .set('checked', xhrData.isShowTime)
         dialog.apChkShowDescription            .set('checked', xhrData.isShowDescription)
         dialog.apChkShowItemNumber             .set('checked', xhrData.isShowItemNumber)
         dialog.apChkShowLuceneID               .set('checked', xhrData.isShowLuceneID)

         dialog._enableControls()
         dialog._isValid()
      }

   });

   return dlgSearchOptions

});