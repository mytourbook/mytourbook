// See http://dojotoolkit.org/reference-guide/build/index.html#build-index
var profile = {
	basePath: "temp/dojo",

	releaseDir: "dojo/../release",
	action: "release",
	layerOptimize: "comments",
	optimize: "comments",
	stripConsole: "warn",
	localeList: "ar,ca,cs,da,de,el,en,en-ca,en-gb,es,fi,fr,fr-ca,he,hr,hu,it,iw,ja,ko,nb,nb-no,nl,nn,no,pl,pt,pt-br,ru,ro,sk,sl,sv,th,tr,zh,zh-cn,zh-tw",


	packages: [
		{
			name: "dojo",
			location: "./dojo"
		},
		{
			name: "dijit",
			location: "./dijit"
		},
		{
			name: "dojox",
			location: "./dojox"
		},
		{
			name: "gridx",
			location: "./gridx"
		},
		{
			name: "idx",
			location: "./idx"
		},
		{
			name: "ecm",
			location: "./ecm"
		},
		{
			name: "samplePluginDojo",
			location: "./samplePluginDojo"
		}
	],

	layers: {
		"samplePluginDojo/SamplePlugin": {
			include: [
				"samplePluginDojo/SamplePlugin"
			],
			exclude: [
				"dojo/dojo",
				"dijit/dijit",
				"ecm/ecm"
			]
		}
	}

};
