require([ //
//
'dojo/dom', //
'dojo/dom-construct'
//
],
//		
function(dom, domConstruct,
		zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz) {

	var greetingNode = dom.byId('greeting');

	domConstruct.place('<i> Dojo!</i>', greetingNode);
	
});
