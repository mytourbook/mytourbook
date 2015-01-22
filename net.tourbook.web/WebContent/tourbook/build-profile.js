var profile = (function() {

	var miniExcludes = {
		"tourbook/build-profile" : 1
	},

	testResource = /\/tests\//, //
	jsResource = /\.js$/,

	copyOnly = function(filename, mid) {
		
		var list = {};

		return (mid in list) //
			|| (/^tourbook\/resources\//.test(mid) && !/\.css$/.test(filename))
			|| /(html|ico|png|jpg|jpeg|gif|tiff)$/.test(filename);
		// Check if it is one of the special files, if it is in
		// app/resource (but not CSS) or is an image
	};

	return {

		resourceTags : {

			test : function(filename, mid) {
				return testResource.test(mid) || mid == "app/tests";
				// Tag our test files
			},

			copyOnly : function(filename, mid) {
				return copyOnly(filename, mid);
				// Tag our copy only files
			},

			miniExclude : function(filename, mid) {
				return testResource.test(mid) || mid in miniExcludes;
			},

			amd : function(filename, mid) {
				return !testResource.test(mid) && !copyOnly(filename, mid) && jsResource.test(filename);
				// If it isn't a test resource, copy only, but is a .js file, tag it as AMD
			}
		}
	};
})();