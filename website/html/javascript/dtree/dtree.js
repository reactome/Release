/*--------------------------------------------------|
| dTree 2.05 | www.destroydrop.com/javascript/tree/ |
|---------------------------------------------------|
| Copyright (c) 2002-2003 Geir Landrö               |
|                                                   |
| This script can be used freely as long as all     |
| copyright messages are intact.                    |
|                                                   |
| Updated: 17.04.2003                               |
|--------------------------------------------------*/

// Node object
function Node(id, pid, dbId, name, url, title, target, icon, iconOpen, open, typeIcon, extraStr) {
	this.id = id;
	this.pid = pid;
	this.dbId = dbId;
	this.name = name;
	this.url = url;
	this.title = title;
	this.target = target;
	this.icon = icon;
	this.iconOpen = iconOpen;
// IV: 
	this.typeIcon = typeIcon;
	this.extraStr = extraStr;
	this._io = open || false;
	this._is = false;
	this._ls = false;
	this._hc = false;
	this._ai = 0;
	this._p;
};

// Tree object
function dTree(objName) {
	this.config = {
		target					: null,
		folderLinks			: true,
		useSelection		: true,
		useCookies			: true,
		useLines				: true,
		useIcons				: true,
		useStatusText		: false,
		closeSameLevel	: false,
		inOrder					: false
	}
	this.icon = {
		root				: '/icons/dtree/base.gif',
		folder			: '/icons/dtree/folder.gif',
		folderOpen	: '/icons/dtree/folderopen.gif',
		node				: '/icons/dtree/page.gif',
		empty				: '/icons/dtree/empty.gif',
		line				: '/icons/dtree/line.gif',
		join				: '/icons/dtree/join.gif',
		joinBottom	: '/icons/dtree/joinbottom.gif',
		plus				: '/icons/dtree/plus.gif',
		plusBottom	: '/icons/dtree/plusbottom.gif',
		minus				: '/icons/dtree/minus.gif',
		minusBottom	: '/icons/dtree/minusbottom.gif',
		nlPlus			: '/icons/dtree/nolines_plus.gif',
		nlMinus			: '/icons/dtree/nolines_minus.gif',
		plusRoot			: '/icons/dtree/plusroot.gif',
		minusRoot			: '/icons/dtree/minusroot.gif',
		childlessroot			: '/icons/dtree/childlessroot.gif'
	};
	this.obj = objName;
	this.aNodes = [];
	this.aIndent = [];
	this.root = new Node(-1);
	this.selectedNode = null;
	this.selectedFound = false;
	this.completed = false;
	this.hierarchyTypeStyleClassName = "hierarchyType_" + this.obj;
};

// Adds a new node to the node array
dTree.prototype.add = function(id, pid, dbId, name, url, title, target, icon, iconOpen, open, typeIcon, extraStr) {
    this.aNodes[this.aNodes.length] = new Node(id, pid, dbId, name, url, title, target, icon, iconOpen, open, typeIcon, extraStr);
};

// Open/close all nodes
dTree.prototype.openAll = function() {
	this.oAll(true);
};
dTree.prototype.closeAll = function() {
	this.oAll(false);
};

// Outputs the tree to the page
dTree.prototype.toString = function() {
//	var str = '<div class="dtree">\n';
// IV: give this element an id so that it can be retrieved and changed, e.g. re-drawn
	var str = this.createHierarchyTypeStyle() + '<div id="dtree' + this.obj + '" class="dtree">\n';
	if (document.getElementById) {
		if (this.config.useCookies) this.selectedNode = this.getSelected();
		str += this.addNode(this.root);
	} else str += 'Browser not supported.';
	str += '</div>';
	if (!this.selectedFound) this.selectedNode = null;
	this.completed = true;
	return str;
};

// Creates the tree structure
dTree.prototype.addNode = function(pNode) {
	var str = '';
	var n=0;
	if (this.config.inOrder) n = pNode._ai;
	for (n; n<this.aNodes.length; n++) {
		if (this.aNodes[n].pid == pNode.id) {
			var cn = this.aNodes[n];
			cn._p = pNode;
			cn._ai = n;
			this.setCS(cn);
			if (!cn.target && this.config.target) cn.target = this.config.target;
//			if (cn._hc && !cn._io && this.config.useCookies) cn._io = this.isOpen(cn.id);
			if (cn._hc && !cn._io && this.config.useCookies) cn._io = this.isOpen(cn.dbId);
			if (!this.config.folderLinks && cn._hc) cn.url = null;
// IV: this code is commented out since otherwise selecting multiple nodes wouldn't work
//			if (this.config.useSelection && cn.id == this.selectedNode && !this.selectedFound) {
//					cn._is = true;
//					this.selectedNode = n;
//					this.selectedFound = true;
//			}
			str += this.node(cn, n);
			if (cn._ls) break;
		}
	}
	return str;
};

// Creates the node icon, url and text
dTree.prototype.node = function(node, nodeId) {
	var str = '<div class="dtree">' + this.indent(node, nodeId);
	if (node.typeIcon) {
//		str += '<span class="hierarchyType">' + node.typeIcon + '</span>';
		str += '<span class="' + this.hierarchyTypeStyleClassName + '">' + node.typeIcon + '</span>';
	}
	if (this.config.useIcons) {
// IV: Commented out since I want to use only explicitly specified icons
//		if (!node.icon) node.icon = (this.root.id == node.pid) ? this.icon.root : ((node._hc) ? this.icon.folder : this.icon.node);
//		if (!node.iconOpen) node.iconOpen = (node._hc) ? this.icon.folderOpen : this.icon.node;
// IV: Commented out since otherwise root icon would always be the same.
//		if (this.root.id == node.pid) {
//			node.icon = this.icon.root;
//			node.iconOpen = this.icon.root;
//		}
		if (node.icon || node.iconOpen) {
			str += '<img id="i' + this.obj + nodeId + '" src="' + ((node._io) ? node.iconOpen : node.icon) + '" alt="" />';
		}
	}
	if (node.url) {
		str += '<a id="s' + this.obj + nodeId + '" class="' + ((this.config.useSelection) ? ((node._is ? 'nodeSel' : 'node')) : 'node') + '" href="' + node.url + '"';
		if (node.title) str += ' title="' + node.title + '"';
		if (node.target) str += ' target="' + node.target + '"';
		if (this.config.useStatusText) str += ' onmouseover="window.status=\'' + node.name + '\';return true;" onmouseout="window.status=\'\';return true;" ';
// IV: Commented out since don't want/need it.
//		if (this.config.useSelection && ((node._hc && this.config.folderLinks) || !node._hc))
//			str += ' onclick="javascript: ' + this.obj + '.s1(' + nodeId + ');"';
		str += '>';
	}
// IV: replaces this with 'else' below to get also nodes w/o url (say, where it is contained in the name) to have id.
//	else if ((!this.config.folderLinks || !node.url) && node._hc && node.pid != this.root.id) {
//		str += '<a href="javascript: ' + this.obj + '.o(' + nodeId + ');" class="node">';
//	}
	else {
	        str += '<span id="s' + this.obj + nodeId + '" class="' + ((this.config.useSelection) ? ((node._is ? 'nodeSel' : 'node')) : 'node') + '"';
		if (node.title) str += ' title="' + node.title + '"';
		if (node.target) str += ' target="' + node.target + '"';
		if (this.config.useStatusText) str += ' onmouseover="window.status=\'' + node.name + '\';return true;" onmouseout="window.status=\'\';return true;" ';
		str += '>';
	}
	str += node.name;
//	if (node.url || ((!this.config.folderLinks || !node.url) && node._hc && node.pid != this.root.id)) {
//	        str += '</a>';
	if (node.url) {
	        str += '</a>';
	} else {
	        str += '</span>';
	}
	if (node.extraStr) {
	        str += node.extraStr;
	}
	str += '</div>';
	if (node._hc) {
		str += '<div id="d' + this.obj + nodeId + '" class="clip" style="display:' + ((this.root.id == node.pid || node._io) ? 'block' : 'none') + ';">';
		str += this.addNode(node);
		str += '</div>';
	}
	this.aIndent.pop();
//	alert(str);
	return str;
};

// Adds the empty and line icons
dTree.prototype.indent = function(node, nodeId) {
	var str = '';
	if (this.root.id != node.pid) {
		for (var n=0; n<this.aIndent.length; n++)
			str += '<img src="' + ( (this.aIndent[n] == 1 && this.config.useLines) ? this.icon.line : this.icon.empty ) + '" alt="" />';
		// IV: handling for apparent roots
		(node._ls) ? this.aIndent.push(0) : ((node.pid==0) ? this.aIndent.push(0) : this.aIndent.push(1));
		if (node._hc) {
			str += '<a href="javascript: ' + this.obj + '.o(' + nodeId + ');"><img id="j' + this.obj + nodeId + '" src="';
			if (!this.config.useLines) {
			    str += (node._io) ? this.icon.nlMinus : this.icon.nlPlus;
			}
			else if (node.pid==0) {
			    str += (node._io) ? this.icon.minusRoot : this.icon.plusRoot;
			}
			else {
			    str += ( (node._io) ? ((node._ls && this.config.useLines) ? this.icon.minusBottom : this.icon.minus) : ((node._ls && this.config.useLines) ? this.icon.plusBottom : this.icon.plus ) );
			}
			str += '" alt="" /></a>';
		} else {
		    // IV: handling for childless apparent roots
		    str += '<img src="' + ((this.config.useLines) ?
					   ((node.pid==0) ?
					    (this.icon.childlessroot) : 
					    ((node._ls) ? this.icon.joinBottom : this.icon.join )) : 
					   this.icon.empty) + '" alt="" />';
		}
	}
	return str;
};

// Adds the empty and line icons
dTree.prototype.indentOriginal = function(node, nodeId) {
	var str = '';
	if (this.root.id != node.pid) {
		for (var n=0; n<this.aIndent.length; n++)
			str += '<img src="' + ( (this.aIndent[n] == 1 && this.config.useLines) ? this.icon.line : this.icon.empty ) + '" alt="" />';
		(node._ls) ? this.aIndent.push(0) : this.aIndent.push(1);
		if (node._hc) {
			str += '<a href="javascript: ' + this.obj + '.o(' + nodeId + ');"><img id="j' + this.obj + nodeId + '" src="';
			if (!this.config.useLines) str += (node._io) ? this.icon.nlMinus : this.icon.nlPlus;
			else str += ( (node._io) ? ((node._ls && this.config.useLines) ? this.icon.minusBottom : this.icon.minus) : ((node._ls && this.config.useLines) ? this.icon.plusBottom : this.icon.plus ) );
			str += '" alt="" /></a>';
		} else str += '<img src="' + ( (this.config.useLines) ? ((node._ls) ? this.icon.joinBottom : this.icon.join ) : this.icon.empty) + '" alt="" />';
	}
	return str;
};

// Checks if a node has any children and if it is the last sibling
dTree.prototype.setCS = function(node) {
	var lastId;
	for (var n=0; n<this.aNodes.length; n++) {
		if (this.aNodes[n].pid == node.id) node._hc = true;
		if (this.aNodes[n].pid == node.pid) lastId = this.aNodes[n].id;
	}
	if (lastId==node.id) node._ls = true;
};

// Returns the selected node
dTree.prototype.getSelected = function() {
//	var sn = this.getCookie('cs' + this.obj);
	var sn = this.getCookie('csd');
	return (sn) ? sn : null;
};

// Highlights the selected node
// IV: changed so that highliting multople nodes is possible.
dTree.prototype.s = function(id) {
	if (!this.config.useSelection) return;
	var cn = this.aNodes[id];
	if (cn._hc && !this.config.folderLinks) return;
	eNew = document.getElementById("s" + this.obj + id);
        if (eNew) {
  	         eNew.className = "nodeSel";
//	         if (this.config.useCookies) this.setCookie('cs' + this.obj, cn.id);
//	         if (this.config.useCookies) this.setCookie('csd', cn.id);
	         if (this.config.useCookies) this.setCookie('csd', cn.dbId);
        } else {
	    //alert("No eNew for id: " + id + " " + "s" + this.obj + id);
        }
	return;
};

// Highlights the selected node
dTree.prototype.s1 = function(id) {
	if (!this.config.useSelection) return;
	var cn = this.aNodes[id];
	if (cn._hc && !this.config.folderLinks) return;
	if (this.selectedNode != id) {
		if (this.selectedNode || this.selectedNode==0) {
			eOld = document.getElementById("s" + this.obj + this.selectedNode);
			eOld.className = "node";
		}
		eNew = document.getElementById("s" + this.obj + id);
		eNew.className = "nodeSel";
		this.selectedNode = id;
//		if (this.config.useCookies) this.setCookie('cs' + this.obj, cn.id);
		if (this.config.useCookies) this.setCookie('csd', cn.id);
	}
};

// Toggle Open or close
dTree.prototype.o = function(id) {
	var cn = this.aNodes[id];
	this.nodeStatus(!cn._io, id, cn._ls);
	cn._io = !cn._io;
	if (this.config.closeSameLevel) this.closeLevel(cn);
	if (this.config.useCookies) this.updateCookie();
};

// Open or close all nodes
dTree.prototype.oAll = function(status) {
	for (var n=0; n<this.aNodes.length; n++) {
	        if (this.aNodes[n]._hc && this.aNodes[n].pid != this.root.id) {
			this.nodeStatus(status, n, this.aNodes[n]._ls)
			this.aNodes[n]._io = status;
		}
	}
	if (this.config.useCookies) this.updateCookie();
};

// Opens the tree to a specific node
// IV: capable of opening to multiple nodes with same id.
dTree.prototype.openTo = function(nId, bSelect, bFirst) {
	if (!bFirst) {
		for (var n=0; n<this.aNodes.length; n++) {
		    	var cn=this.aNodes[n];
			if (cn.id == nId) {
			       if (cn.pid==this.root.id || !cn._p) return;
			       cn._io = true;
			       cn._is = bSelect;
			       if (this.completed && cn._hc) this.nodeStatus(true, cn._ai, cn._ls);
			       if (this.completed && bSelect) this.s(cn._ai);
			       else if (bSelect) this._sn=cn._ai;
			       this.openTo(cn._p._ai, false, true);
			}
		}
	}
	var cn=this.aNodes[nId];
	if (!cn) return;
	if (cn.pid==this.root.id || !cn._p) return;
	cn._io = true;
	cn._is = bSelect;
	if (this.completed && cn._hc) this.nodeStatus(true, cn._ai, cn._ls);
	if (this.completed && bSelect) this.s(cn._ai);
	else if (bSelect) this._sn=cn._ai;
	this.openTo(cn._p._ai, false, true);
	// IV: updateCookie to remember the situation
        if (this.config.useCookies) this.updateCookie();
};

// Opens the tree to a specific node
dTree.prototype.openTo1 = function(nId, bSelect, bFirst) {
	if (!bFirst) {
		for (var n=0; n<this.aNodes.length; n++) {
			if (this.aNodes[n].id == nId) {
				nId=n;
				break;
			}
		}
	}
	var cn=this.aNodes[nId];
	if (cn.pid==this.root.id || !cn._p) return;
	cn._io = true;
	cn._is = bSelect;
	if (this.completed && cn._hc) this.nodeStatus(true, cn._ai, cn._ls);
	if (this.completed && bSelect) this.s(cn._ai);
	else if (bSelect) this._sn=cn._ai;
	this.openTo(cn._p._ai, false, true);
};

// Closes all nodes on the same level as certain node
dTree.prototype.closeLevel = function(node) {
	for (var n=0; n<this.aNodes.length; n++) {
		if (this.aNodes[n].pid == node.pid && this.aNodes[n].id != node.id && this.aNodes[n]._hc) {
			this.nodeStatus(false, n, this.aNodes[n]._ls);
			this.aNodes[n]._io = false;
			this.closeAllChildren(this.aNodes[n]);
		}
	}
}

// Closes all children of a node
dTree.prototype.closeAllChildren = function(node) {
	for (var n=0; n<this.aNodes.length; n++) {
		if (this.aNodes[n].pid == node.id && this.aNodes[n]._hc) {
			if (this.aNodes[n]._io) this.nodeStatus(false, n, this.aNodes[n]._ls);
			this.aNodes[n]._io = false;
			this.closeAllChildren(this.aNodes[n]);		
		}
	}
}

// Change the status of a node(open or closed)
dTree.prototype.nodeStatus = function(status, id, bottom) {
	eDiv	= document.getElementById('d' + this.obj + id);
	eJoin	= document.getElementById('j' + this.obj + id);
	var isApparentRoot = false;
	if (this.config.useIcons) {
		eIcon	= document.getElementById('i' + this.obj + id);
		if (eIcon) {
		    eIcon.src = (status) ? this.aNodes[id].iconOpen : this.aNodes[id].icon;
		}
		isApparentRoot = (this.aNodes[id].pid==0) ? true : false;
	}
	//	eJoin.src = (this.config.useLines && !isApparentRoot)?
	//	((status)?((bottom)?this.icon.minusBottom:this.icon.minus):((bottom)?this.icon.plusBottom:this.icon.plus)):
	//	((status)?this.icon.nlMinus:this.icon.nlPlus);
	eJoin.src = (this.config.useLines)?
	((isApparentRoot)?((status)?this.icon.minusRoot:this.icon.plusRoot)
	 :((status)?((bottom)?this.icon.minusBottom:this.icon.minus):((bottom)?this.icon.plusBottom:this.icon.plus))):
	((status)?this.icon.nlMinus:this.icon.nlPlus);
	eDiv.style.display = (status) ? 'block': 'none';
};


// [Cookie] Clears a cookie
dTree.prototype.clearCookie = function() {
	var now = new Date();
	var yesterday = new Date(now.getTime() - 1000 * 60 * 60 * 24);
//	this.setCookie('co'+this.obj, 'cookieValue', yesterday);
//	this.setCookie('cs'+this.obj, 'cookieValue', yesterday);
	this.setCookie('cod', 'cookieValue', yesterday);
	this.setCookie('csd', 'cookieValue', yesterday);
};

// [Cookie] Sets value in a cookie
dTree.prototype.setCookie = function(cookieName, cookieValue, expires, path, domain, secure) {
	document.cookie =
		escape(cookieName) + '=' + escape(cookieValue)
		+ (expires ? '; expires=' + expires.toGMTString() : '')
		+ (path ? '; path=' + path : '')
		+ (domain ? '; domain=' + domain : '')
		+ (secure ? '; secure' : '');
};

// [Cookie] Gets a value from a cookie
dTree.prototype.getCookie = function(cookieName) {
	var cookieValue = '';
	var posName = document.cookie.indexOf(escape(cookieName) + '=');
	if (posName != -1) {
		var posValue = posName + (escape(cookieName) + '=').length;
		var endPos = document.cookie.indexOf(';', posValue);
		if (endPos != -1) cookieValue = unescape(document.cookie.substring(posValue, endPos));
		else cookieValue = unescape(document.cookie.substring(posValue));
	}
	return (cookieValue);
};

// [Cookie] Returns ids of open nodes as a string
dTree.prototype.updateCookie = function() {
	var str = '';
	for (var n=0; n<this.aNodes.length; n++) {
		if (this.aNodes[n]._io && this.aNodes[n].pid != this.root.id) {
			if (str) str += '.';
//			str += this.aNodes[n].id;
			str += this.aNodes[n].dbId;
		}
	}
//	this.setCookie('co' + this.obj, str);
	this.setCookie('cod', str);
};

// [Cookie] Checks if a node id is in a cookie
dTree.prototype.isOpen = function(dbId) {
//	var aOpen = this.getCookie('co' + this.obj).split('.');
	var aOpen = this.getCookie('cod').split('.');
	for (var n=0; n<aOpen.length; n++)
		if (aOpen[n] == dbId) return true;
	return false;
};

// If Push and pop is not implemented by the browser
if (!Array.prototype.push) {
	Array.prototype.push = function array_push() {
		for(var i=0;i<arguments.length;i++)
			this[this.length]=arguments[i];
		return this.length;
	}
};
if (!Array.prototype.pop) {
	Array.prototype.pop = function array_pop() {
		lastElement = this[this.length-1];
		this.length = Math.max(this.length-1,0);
		return lastElement;
	}
};

// IV: "re-draws" the tree
dTree.prototype.reDraw = function() {
	var str = 'dtree' + this.obj;
	var treeDiv = document.all? document.all[str] : document.getElementById ? document.getElementById(str) : "";
	treeDiv.innerHTML = this;
}

// IV: switch icons on/off
dTree.prototype.toggleIcons = function() {
	this.config.useIcons = ! this.config.useIcons;
	this.reDraw();
}

// IV: switch hierarchyType on/off
dTree.prototype.toggleTypeIcon = function() {
//    var styleClassName = "hierarchyType_" + this.obj;
    var styleClassName = this.hierarchyTypeStyleClassName;
    if (getStyleClass(styleClassName).style.display == 'none') {
        getStyleClass(styleClassName).style.display='inline';
	this.setCookie("hierarchyType","on");
    } else {
	getStyleClass(styleClassName).style.display='none';
	this.setCookie("hierarchyType","off");
    }
}

// IV:
dTree.prototype.setHierarchyTypeVisibility = function() {
    var cv = this.getCookie("hierarchyType");
//    var styleClassName = "hierarchyType_" + this.obj;
    var styleClassName = this.hierarchyTypeStyleClassName;
    if (cv == "on") {
	getStyleClass(styleClassName).style.display='inline';
    } else {
	getStyleClass(styleClassName).style.display='none';
    }
    //    if ((cv == null) || (cv == "off")) {
    //	getStyleClass(styleClassName).style.display='none';
    //    } else {
    //	getStyleClass(styleClassName).style.display='inline';
    //    }
}

// IV: doesn't really belong here but anyway
function getStyleClass (className) {
    var regex = new RegExp('\\.' + className + '\$', "i");  
    if (document.all) {
	for (var s = 0; s < document.styleSheets.length; s++) {
            for (var r = 0; r < document.styleSheets[s].rules.length; r++) {
                if (document.styleSheets[s].rules[r].selectorText &&
		    document.styleSheets[s].rules[r].selectorText.match(regex)) {
                    return document.styleSheets[s].rules[r];
                }
            }
        }
    }
    else if (document.getElementById) {
        for (var s = 0; s < document.styleSheets.length; s++) {
            for (var r = 0; r < document.styleSheets[s].cssRules.length; r++) {
                if (document.styleSheets[s].cssRules[r].selectorText &&
		    document.styleSheets[s].cssRules[r].selectorText.match(regex)) {
                    return document.styleSheets[s].cssRules[r];
                }
            }
        }
    }
    return null;
}

dTree.prototype.createHierarchyTypeStyle = function() {
    return '<style type="text/css">.' + this.hierarchyTypeStyleClassName +
    '{display:none;}</style>';
}

dTree.prototype.changeNodeClass = function(dbId,className) {
    var oldClassName;
//    alert(nId + " " + className + " " +  this.aNodes.length);
    for (var n=0; n<this.aNodes.length; n++) {
	var cn=this.aNodes[n];
	if (cn.dbId == dbId) {
	    eNew = document.getElementById("s" + this.obj + n);
	    //alert(nId + " " + className + " " + n + " " + eNew);
	    if (eNew != null) {
	        if (oldClassName == null) {
		   oldClassName = eNew.className;
                } 
	        eNew.className = className;
	    }
	}
    }
    return oldClassName;
};

// Opens the tree to node(s) with given dbId
// IV: capable of opening to multiple nodes with same dbId.
dTree.prototype.openToDbId = function(dbId, bSelect) {
	for (var n=0; n<this.aNodes.length; n++) {
	    	var cn=this.aNodes[n];
		if (cn.dbId == dbId) {
		       if (cn.pid==this.root.id || !cn._p) return;
		       cn._io = true;
		       cn._is = bSelect;
		       if (this.completed && cn._hc) this.nodeStatus(true, cn._ai, cn._ls);
		       if (this.completed && bSelect) this.s(cn._ai);
		       else if (bSelect) this._sn=cn._ai;
		       this.openTo(cn._p._ai, false, true);
		}
	}
	// IV: updateCookie to remember the situation
        if (this.config.useCookies) this.updateCookie();
};

// Opens the tree to nodes with given dbIds
// IV: capable of opening to multiple nodes with same dbId.
dTree.prototype.openToDbIds = function(dbIds, bSelect) {
	for (var n=0; n<this.aNodes.length; n++) {
	    	var cn=this.aNodes[n];
		//'include' is from Prototype's Enumerable
		if (dbIds.include(cn.dbId)) {
		       if (cn.pid==this.root.id || !cn._p) return;
		       cn._io = true;
		       cn._is = bSelect;
		       if (this.completed && cn._hc) this.nodeStatus(true, cn._ai, cn._ls);
		       if (this.completed && bSelect) this.s(cn._ai);
		       else if (bSelect) this._sn=cn._ai;
		       this.openTo(cn._p._ai, false, true);
		}
	}
	// IV: updateCookie to remember the situation
        if (this.config.useCookies) this.updateCookie();
};

// Highlights (and selects) node(s) with given dbId
dTree.prototype.selectNode = function(dbId) {
	for (var n=0; n<this.aNodes.length; n++) {
	    	var cn=this.aNodes[n];
		if (cn.dbId == dbId) {
		       this.s(cn._ai);
		}
	}
};

dTree.prototype.containsNodeWithDbID = function(dbId) {
        for (var n=0, l=this.aNodes.length; n<l; ++n) {
	    	var cn=this.aNodes[n];
		if (cn.dbId == dbId) {
		       return true;
		}
	}
	return false;
};

dTree.prototype.containsNodesWithDbIDs = function(dbIds) {
        for (var n=0, l=dbIds.length; n<l; ++n) {
		if (! this.containsNodeWithDbID(dbIds[n])) {
		    return false;
		}
	}
	return true;
};

// This requires prototype.js
dTree.prototype.deselectAllNodes = function() {
    document.getElementsByClassName('nodeSel').each(function(n){n.className = 'node';});
};