function queryByIds(type) {
    url = "/ReactomeRESTfulAPI/RESTfulWS/queryByIds/Pathway";
    body = "ID=109607,109606,75153,169911";
    doPost(url, type, body);
}

function doPost(url, type, body) {
    newWindow = window.open("", url);
    ajax = new XMLHttpRequest();
    ajax.open("POST", url, true);
    ajax.setRequestHeader("Content-type", "text/plain");
    ajax.setRequestHeader("Accept", type);
    ajax.onreadystatechange = function() {
	if(ajax.readyState == 4 && ajax.status == 200) {
	    text = ajax.responseText.replace(/</g, "&lt;");
	    text = text.replace(/>/g, "&gt;");
	    newWindow.document.write("<pre>" + text + "</pre>");
	    newWindow.document.close();
	}
    };
    ajax.send(body);
}

function listByQuery(type) {
    url = "/ReactomeRESTfulAPI/RESTfulWS/listByQuery/Pathway";
    body = "name=Apoptosis";
    doPost(url, type, body);
}

function pathwaysForEntities(type) {
    url = "/ReactomeRESTfulAPI/RESTfulWS/pathwaysForEntities";
    body="ID=170075,176374,68557";
    doPost(url, type, body);
}

function queryHitPathways(type) {
    url = "/ReactomeRESTfulAPI/RESTfulWS/queryHitPathways";
    body="PPP2R1A,CEP192,AKAP9,CENPJ,CEP290,DYNC1H1";
    doPost(url, type, body);
}
