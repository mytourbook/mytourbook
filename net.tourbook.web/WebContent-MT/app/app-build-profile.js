/**
 * This file is referenced by the `dojoBuild` key in `package.json` and provides extra hinting specific to the Dojo
 * build system about how certain files in the package need to be handled at build time. Build profiles for the
 * application itself are stored in the `profiles` directory.
 */

var profileOLD = {
	// Resource tags are functions that provide hints to the build system about the way files should be processed.
	// Each of these functions is called once for every file in the package directory. The first argument passed to
	// the function is the filename of the file, and the second argument is the computed AMD module ID of the file.
	resourceTags : {
		// Files that contain test code and should be excluded when the `copyTests` build flag exists and is `false`.
		// It is strongly recommended that the `mini` build flag be used instead of `copyTests`. Therefore, no files
		// are marked with the `test` tag here.
		test : function(filename, mid) {
			return false;
		},

		// Files that should be copied as-is without being modified by the build system.
		// All files in the `app/resources` directory that are not CSS files are marked as copy-only, since these files
		// are typically binaries (images, etc.) and may be corrupted by the build system if it attempts to process
		// them and naively assumes they are scripts.
		copyOnly : function(filename, mid) {
			return (/^app\/resources\//.test(mid) && !/\.css$/.test(filename));
		},

		// Files that are AMD modules.
		// All JavaScript in this package should be AMD modules if you are starting a new project. If you are copying
		// any legacy scripts from an existing project, those legacy scripts should not be given the `amd` tag.
		amd : function(filename, mid) {
//			return !this.copyOnly(filename, mid) && /\.js$/.test(filename);
			return /\.js$/.test(filename);
		},

		// Files that should not be copied when the `mini` build flag is set to true.
		// In this case, we are excluding this package configuration file which is not necessary in a built copy of
		// the application.
		miniExclude : function(filename, mid) {
			return mid in {
				'app/package' : 1
			};
		}
	}
};

/**
 * dgrid profile
 */
var miniExcludes = {
	'dgrid/CHANGES.md' : 1,
	'dgrid/LICENSE' : 1,
	'dgrid/README.md' : 1,
	'dgrid/package' : 1
}, isTestRe = /\/test\//;

var profileDGRID = {

	resourceTags : {
		test : function(filename) {
			return isTestRe.test(filename);
		},
		miniExclude : function(filename, mid) {
			return (/\/(?:test|demos)\//).test(filename) || mid in miniExcludes;
		},
		amd : function(filename) {
			return (/\.js$/).test(filename);
		}
	}
};

/**
 * profile from http://dojotoolkit.org/documentation/tutorials/1.7/build/
 */
var profile = (function() {

	// regex to check if mid is in app/tests directory
	var regTestResource = /^app\/tests\//;

	var copyOnly = function(filename, mid) {

		var list = {

			// we shouldn't touch our profile
			"app/app-build-.profile" : true,

			// we shouldn't touch our package.json
			"app/package.json" : true
		};

		return (mid in list) //
			|| (/^app\/resources\//.test(mid) && !/\.css$/.test(filename)) //
			|| /(png|jpg|jpeg|gif|tiff)$/.test(filename);
		// Check if it is one of the special files, if it is in
		// app/resource (but not CSS) or is an image
	};

	return {

		resourceTags : {

			test : function(filename, mid) {
				return regTestResource.test(mid) || mid == "app/tests";
				// Tag our test files
			},

			copyOnly : function(filename, mid) {
				return copyOnly(filename, mid);
				// Tag our copy only files
			},

			amd : function(filename, mid) {
				return !regTestResource.test(mid) && !copyOnly(filename, mid) && /\.js$/.test(filename);
				// If it isn't a test resource, copy only, but is a .js file, tag it as AMD
			}
		}
	};
})();