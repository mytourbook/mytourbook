// 'use strict';

define(
[//
"dojo/_base/declare"
],

function(declare) {

	var NLS = declare("tourbook.util.NLS", [], {});

	NLS.escapeRegExp = function escapeRegExp(string) {

		return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
	};

	NLS.regExp_0 = new RegExp(NLS.escapeRegExp('{0}'), 'g');

	NLS.bind0 = function bind0(string, replace) {

		return string.replace(NLS.regExp_0, replace);
	};

	return NLS;
});