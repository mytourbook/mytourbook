/**
 * This file is referenced by the `dojoBuild` key in `package.json` and provides extra hinting specific to the Dojo
 * build system about how certain files in the package need to be handled at build time. Build profiles for the
 * application itself are stored in the `profiles` directory.
 */

var profile = (function() {

	var miniExcludes = {
		"tourbook/build-profile" : 1
	},

	amdExcludes = {}, //
	copyOnly = {},

	isJsResource = /\.js$/, //
	isTestResource = /\/test\//;

	return {

		resourceTags : {

			test : function(filename, mid) {
				return isTestResource.test(filename);
			},

			miniExclude : function(filename, mid) {
				return isTestResource.test(filename) || mid in miniExcludes;
			},

			amd : function(filename, mid) {
				return isJsResource.test(filename) && !(mid in amdExcludes);
			},

			copyOnly : function(filename, mid) {
				return mid in copyOnly;
			}
		}
	};
})();
