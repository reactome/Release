var account = getAccount(document.location.hostname);

if (account !== null) {
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', account, 'auto');
  ga('send', 'pageview');
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