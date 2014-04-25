/***********************************************
 * Cool DHTML tooltip script- %/1ŽÂ€ŽÂŒiso8859-15ŽÂŽ© Dynamic Drive DHTML code library (www.dynamicdrive.com)
 * This notice MUST stay intact for legal use
 * Visit Dynamic Drive at http://www.dynamicdrive.com/ for full source code
 ***********************************************/
    
var offsetxpoint=-60; //Customize x offset of tooltip
var offsetypoint=20; //Customize y offset of tooltip
var ie=document.all;
var ns6=document.getElementById && !document.all;
var enabletip=false;
var tipobj;
if (ie||ns6) {
    tipobj=document.all? document.all["dhtmltooltip"] : document.getElementById? document.getElementById("dhtmltooltip") : "";
}
document.onmousemove=positiontip;
	
function getTipObj() {
    return document.all? document.all["dhtmltooltip"] : document.getElementById? document.getElementById("dhtmltooltip") : null;
}

function ietruebody(){
    return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body;
}
	     
function ddrivetip(thetext, thecolor, thewidth){
    if (ns6||ie){
	if (tipobj == null) {
	    tipobj = getTipObj();
	}
        if (typeof thecolor!="undefined" && thecolor!="") {
            tipobj.style.backgroundColor=thecolor;
        }
	if ((thewidth != null) && (typeof thewidth!="undefined")) {
            tipobj.style.width=thewidth+"px";
	}
        tipobj.innerHTML=thetext;
	//thewidth = thewidth ? thewidth : 150;
	//var str = "<table width=\"" + thewidth + "px\"><tr><td>" + thetext + "</td></tr></table>";
	//tipobj.innerHTML = str;
	//tipobj.innerHTML="<table width=\"" + thewidth + "px\"><tr><td>" + thetext + "</td></tr></table>";
	enabletip=true;
	return false;
    }
}



function ddrivetip2(thetext, thecolor, thewidth){
    if (ns6||ie){
	hideSelects();
        if (typeof thecolor!="undefined" && thecolor!="") {
            tipobj.style.backgroundColor=thecolor;
        }
	if (typeof thewidth!="undefined") {
            tipobj.style.width=thewidth+"px";
	}
        tipobj.innerHTML=thetext;
	enabletip=true;
	return false;
    }
}

function hideSelects() {
    if (ie) {
        var el = document.getElementById("popup_1");
        if (el != null) {
	    document.getElementById("popup_1").style.visibility="hidden";
	    document.getElementById("popup_2").style.visibility="hidden";
	    document.getElementById("popup_3").style.visibility="hidden";
	}
    }
}

function showSelects() {
    if (ie) {
	var el = document.getElementById("popup_1");
        if (el != null) {
	    document.getElementById("popup_1").style.visibility="visible";
	    document.getElementById("popup_2").style.visibility="visible";
	    document.getElementById("popup_3").style.visibility="visible";
	}
    }
}

function positiontip(e){
    if (enabletip){
	e = formatEvent(e);
	if (e.X + offsetxpoint + tipobj.offsetWidth > window.trueInnerWidth) {
	    tipobj.style.left = window.trueInnerWidth - tipobj.offsetWidth - 5 + "px";
	} else if (e.X + offsetxpoint < 0) {
	    tipobj.style.left = "5px";
	} else {
	    tipobj.style.left = e.pageX + offsetxpoint + "px";
	}
        if (e.Y + offsetypoint + tipobj.offsetHeight > window.trueInnerHeight) {
	     tipobj.style.top = e.pageY - offsetypoint - tipobj.offsetHeight + "px";
	} else {
	    tipobj.style.top = e.pageY + offsetypoint + "px";
	}
	tipobj.style.visibility="visible";
/*	tipobj.innerHTML= 
	//"left = " + tipobj.style.left + "<BR />offsetWidth = " + tipobj.offsetWidth + 
	//"<BR />innerWidth = " + window.innerWidth + "<BR />e.X = " + e.X + 
	//"<BR />document.body.clientWidth = " + document.body.clientWidth + 
	//"<BR />window.trueInnerWidth = " + window.trueInnerWidth + "<BR />document.width = " + document.width + 
	"<BR />top = " + tipobj.style.top + "<BR />offsetHeight = " + tipobj.offsetHeight + 
	"<BR />e.clientY = " + e.clientY + "<BR />window.innerHeight = " + window.innerHeight + 
	"<BR />e.pageY = " + e.pageY + "<BR />Y = " + e.Y + "<BR />document.height = " + document.height;
*/
    }
}
 
function hideddrivetip(){
    if (ns6||ie){
	if (tipobj == null) {
	    tipobj = getTipObj();
	}
	enabletip=false;
	tipobj.style.visibility="hidden";
	tipobj.style.backgroundColor='';
	tipobj.style.width='';
    }
}
	   
function hideddrivetip2(){
    if (ns6||ie){
	enabletip=false;
	tipobj.style.visibility="hidden";
	tipobj.style.backgroundColor='';
	tipobj.style.width='';
	showSelects();
    }
}  
	    
// format events to sync IE and Safari events with W3C DOM events
formatEvent = function (oEvent) {
    if (oEvent == null) {
	oEvent = window.event;
    }
    if (oEvent.srcElement && !oEvent.target) {
        oEvent.charCode = (oEvent.type == "keypress")?oEvent.keyCode:0;      // match key codes 
        oEvent.eventPhase = 2;                                               // enable bubbling phase
        oEvent.isChar = (oEvent.charCode > 0);                               // if keycode is true is value !=0
	//IE 6/7 BackCompat hackery
	if (document.documentElement) {
            oEvent.pageX = oEvent.clientX + document.documentElement.scrollLeft;            // creates pageX
            oEvent.pageY = oEvent.clientY + document.documentElement.scrollTop;             // creates pageY
	} else {
            oEvent.pageX = oEvent.clientX + document.body.scrollLeft;            // creates pageX
            oEvent.pageY = oEvent.clientY + document.body.scrollTop;             // creates pageY
	}
        if (!oEvent.preventDefault) {                                        // prevent event from default action
            oEvent.preventDefault = function () {
                this.returnValue = false;
            };
        }
        if (!oEvent.stopPropagation) {                                       // prevent bubbling
            oEvent.stopPropagation = function () {
                this.cancelBubble = true;
            };
        }
        if (oEvent.type == "mouseout") {                                     // create relatedTarget
            oEvent.relatedTarget = oEvent.toElement;
        } else if (oEvent.type == "mouseover") {
            oEvent.relatedTarget = oEvent.fromElement;
        }
	oEvent.target = oEvent.srcElement;                                   // create target
        oEvent.time = (new Date).getTime();                                  // create time
	oEvent.X = oEvent.clientX;
	oEvent.Y = oEvent.clientY;
	window.innerWidth = document.body.offsetWidth;
	window.innerHeight = document.body.offsetHeight;
	window.trueInnerWidth = document.body.offsetWidth - 20;
	window.trueInnerHeight = document.body.offsetHeight - 20;
    } else if (isSafari) {
	oEvent.X = oEvent.clientX - document.body.scrollLeft;
        oEvent.Y = oEvent.clientY - document.body.scrollTop;
	oEvent.target = oEvent.toElement;
	//	oEvent.pageX = 
	window.trueInnerWidth = window.innerWidth;
	window.trueInnerHeight = window.innerHeight;
    } else {
	oEvent.X = oEvent.clientX;
	oEvent.Y = oEvent.clientY;
	window.trueInnerWidth = window.innerWidth - 16;
	window.trueInnerHeight = window.innerHeight - 16;
    }
    return oEvent;
}

var BrowserDetect = {
	init: function () {
		this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
		this.version = this.searchVersion(navigator.userAgent)
			|| this.searchVersion(navigator.appVersion)
			|| "an unknown version";
		this.OS = this.searchString(this.dataOS) || "an unknown OS";
	},
	searchString: function (data) {
		for (var i=0;i<data.length;i++)	{
			var dataString = data[i].string;
			var dataProp = data[i].prop;
			this.versionSearchString = data[i].versionSearch || data[i].identity;
			if (dataString) {
				if (dataString.indexOf(data[i].subString) != -1)
					return data[i].identity;
			}
			else if (dataProp)
				return data[i].identity;
		}
	},
	searchVersion: function (dataString) {
		var index = dataString.indexOf(this.versionSearchString);
		if (index == -1) return;
		return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
	},
	dataBrowser: [
		{
			string: navigator.vendor,
			subString: "Apple",
			identity: "Safari"
		},
		{
			prop: window.opera,
			identity: "Opera"
		},
		{
			string: navigator.vendor,
			subString: "iCab",
			identity: "iCab"
		},
		{
			string: navigator.vendor,
			subString: "KDE",
			identity: "Konqueror"
		},
		{
			string: navigator.userAgent,
			subString: "Firefox",
			identity: "Firefox"
		},
		{	// for newer Netscapes (6+)
			string: navigator.userAgent,
			subString: "Netscape",
			identity: "Netscape"
		},
		{
			string: navigator.userAgent,
			subString: "MSIE",
			identity: "Explorer",
			versionSearch: "MSIE"
		},
		{
			string: navigator.userAgent,
			subString: "Gecko",
			identity: "Mozilla",
			versionSearch: "rv"
		},
		{ 	// for older Netscapes (4-)
			string: navigator.userAgent,
			subString: "Mozilla",
			identity: "Netscape",
			versionSearch: "Mozilla"
		}
	],
	dataOS : [
		{
			string: navigator.platform,
			subString: "Win",
			identity: "Windows"
		},
		{
			string: navigator.platform,
			subString: "Mac",
			identity: "Mac"
		},
		{
			string: navigator.platform,
			subString: "Linux",
			identity: "Linux"
		}
	]

};
BrowserDetect.init();
var isSafari = (BrowserDetect.browser == "Safari") ? true : false;
