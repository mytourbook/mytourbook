define(
[
		"dojo/_base/declare",
],
//
function(declare) {

	/**
	 * @name icn-services.StateMixin
	 * @class This class can be mixed in to provide ...
	 */
	var StateMixin = declare("icnExtensions.StateMixin", null,
	{

		/**
		 * Contains the state object, it is stored in the localStorage and contains the dialog size/position.
		 */
		_state : undefined,

		/**
		 * Unique name for the state object.
		 */
		_stateName : undefined,

		/**
		 * Setup the state object.
		 */
		_setupState : function() {

			// get a unique name for the state object, this.id is unique for each instance.
			this._stateName = this.declaredClass;

			var rawState = localStorage.getItem(this._stateName);

			this._state = rawState ? JSON.parse(rawState) : {};
		},

		// load item from local storage
		getStateValue : function(name) {
			return this._state[name];
		},

		/**
		 * Save item(s) in the state.
		 * 
		 * @param {string}
		 *            name - Name for the value.
		 * @param {number}
		 *            value - Value.
		 * @param {string}
		 *            [name2] - Name for the 2nd value.
		 * @param {number}
		 *            [value2] - 2nd value.
		 */
		setStateValue : function(name, value, name2, value2) {

			var state = this._state;

			if (name) {
				state[name] = value;
			}

			if (name2) {
				state[name2] = value2;
			}

			// Put the object into storage
			localStorage.setItem(this._stateName, JSON.stringify(state));
		},

		postCreate : function() {

			this.inherited(arguments);

			this._setupState();
		},
	});

	return StateMixin;
});
