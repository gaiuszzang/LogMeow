const { remote, ipcRenderer } = require('electron')

let isMouseDown = false
let beginX = 0
let originWidth = 0
let tabId, tabVarName, tabMinWidth, tabMaxWidth

exports.initTabDrag = function(doc) {
    //Setting Horizontal ScrollBar
    doc.getElementById("barDate").onmousedown = function(e) {
        onMouseDown(e, "tabDate", "--logtab-date-width", 100, 260);
    };
    doc.getElementById("barPID").onmousedown = function(e) {
        onMouseDown(e, "tabPID", "--logtab-pid-width", 30, 90);
    };
    doc.getElementById("barTID").onmousedown = function(e) {
        onMouseDown(e, "tabTID", "--logtab-tid-width", 30, 90);
    };
    doc.getElementById("barLevel").onmousedown = function(e) {
        onMouseDown(e, "tabLevel", "--logtab-level-width", 30, 60);
    };
    doc.getElementById("barTag").onmousedown = function(e) {
        onMouseDown(e, "tabTag", "--logtab-tag-width", 100, 300);
    };
    doc.onmouseup = onMouseUp;
    doc.onmousemove = onMouseMove;
}

function onMouseDown(e, elementId, propertyName, minWidth, maxWidth) {
    beginX = event.clientX;
    tabId = elementId;
    originWidth = document.getElementById(tabId).offsetWidth
    tabVarName = propertyName;
    tabMinWidth = minWidth;
    tabMaxWidth = maxWidth;
    isMouseDown = true;
    return false;
}

function onMouseUp(e) {
    isMouseDown = false;
    return false;
}

function onMouseMove(e) {
    if (isMouseDown) {
        let newWidth = originWidth - (beginX - event.clientX);
        if (newWidth < tabMinWidth) newWidth = tabMinWidth;
        if (newWidth > tabMaxWidth) newWidth = tabMaxWidth;
        updateProperty(tabVarName, newWidth + "px");
    }
}

function updateProperty(propertyName, width) {
    document.documentElement.style.setProperty(propertyName, width);
}