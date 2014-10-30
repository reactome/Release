var account = getAccount(document.location.hostname);

if (account !== null) {
	var _gaq = _gaq || [];  
	_gaq.push(['_setAccount', account]);
	_gaq.push(['_setDomainName', getDomain(document.location.hostname)]);
	_gaq.push(['_trackPageview']);

	(function() {
		var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
		ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
		var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
	})();
}

function getAccount(hostname) {
	var account = "UA-42985898";

	if (hostname.search("reactome.org") !== -1 || hostname === "reactomeprd1.oicr.on.ca") {
		return account + "-1";
	} else if (hostname === "reactomedev.oicr.on.ca") {		
		return account + "-2";
	} else if (hostname === "reactomecurator.oicr.on.ca") {
		return account + "-3";
	} else {
		return null;
	}
}

function getDomain(hostname) {
	if (hostname === "www.reactome.org") {
		return "reactome.org";
	}
	
	return hostname;
}