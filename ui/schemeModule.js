const { remote, ipcRenderer } = require('electron')
let fs = require('fs');
let path = require('path');
const adb = require('../adbModule.js')

const schemeConfigDir = (process.platform == "darwin") ? 
    path.join(process.env.HOME, "Library", "LogMeow", "settings") :
    "settings";
const schemeConfigFile = path.join(schemeConfigDir, "schemeConfig.json")

let defaultSchemeConfig = {
    version: 1,
    history: []
}
let schemeConfig = null

//lazy init
let serial

//Element for use (lazy init)
let historyList

ipcRenderer.on('init', (event, setting, targetSerial) => {
    setStyle(setting.useDarkTheme)
    serial = targetSerial
    historyList = document.querySelector('#historylist')
    loadSchemeConfig()
    initHistoryList()
})

exports.executeScheme = async function(scheme) {
    return await adb.executeScheme(serial, scheme)
}

exports.addSchemeToHistory = function(scheme) {
    addSchemeToHistory(scheme)
}

exports.removeSchemeToHistory = function(scheme) {
    removeSchemeToHistory(scheme)
}

function setStyle(isDarkTheme) {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    if (isDarkTheme) {
        link.href = 'style-darktheme.css';
    } else {
        link.href = 'style.css';
    }
    document.head.appendChild(link);
}

function initHistoryList() {
    historyList.innerHTML = ""
    schemeConfig.history.forEach((scheme) => {
        historyList.appendChild(createSchemeHistoryItem(scheme), historyList.firstChild)
    })
}

function createSchemeHistoryItem(scheme) {
    let historyItemText = document.createElement('div')
    historyItemText.style.flexGrow = 1
    historyItemText.style.marginTop = "auto"
    historyItemText.style.marginBottom = "auto"
    historyItemText.innerHTML = scheme

    let historyItemButtonGroup = document.createElement('div')
    historyItemButtonGroup.style.float = "right"
    historyItemButtonGroup.style.visibility = 'hidden'
    historyItemButtonGroup.innerHTML = getUseSchemeButtonHTML(scheme) + getRemoveSchemeButtonHTML(scheme)

    let historyItem = document.createElement('historyitem')
    historyItem.appendChild(historyItemText)
    historyItem.appendChild(historyItemButtonGroup)
    historyItem.addEventListener('mouseover', function handleMouseOver() {
        historyItemButtonGroup.style.visibility = 'visible'
    })
    historyItem.addEventListener('mouseout', function handleMouseOver() {
        historyItemButtonGroup.style.visibility = 'hidden'
    })
    return historyItem
}


function getUseSchemeButtonHTML(scheme) {
    return '<div class="c_header_image_button" style="display: inline-block;" alt="Use" onclick="document.querySelector(\'#scheme\').value = \'' + scheme + '\'"> \
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"> \
                    <g data-name="Layer 2"> \
                        <g data-name="corner-right-up"> \
                            <rect width="24" height="24" transform="rotate(180 12 12)" opacity="0"/> \
                            <path d="M18.62 8.22l-5-4a1 1 0 0 0-1.24 0l-5 4a1 1 0 0 0 1.24 1.56L12 7.08V16a1 1 0 0 1-1 1H6a1 1 0 0 0 0 2h5a3 3 0 0 0 3-3V7.08l3.38 2.7A1 1 0 0 0 18 10a1 1 0 0 0 .78-.38 1 1 0 0 0-.16-1.4z" transform="scale(0.7, 0.7) translate(5, 5)"/> \
                        </g> \
                    </g> \
                </svg> \
            </div>'
}

function getRemoveSchemeButtonHTML(scheme) {
    return '<div class="c_header_image_button" style="display: inline-block;" alt="Remove" onclick="removeSchemeToHistory(\'' + scheme + '\')"> \
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"> \
                    <g data-name="Layer 2"> \
                        <g data-name="trash"> \
                            <rect width="24" height="24" opacity="0"/> \
                            <path d="M21 6h-5V4.33A2.42 2.42 0 0 0 13.5 2h-3A2.42 2.42 0 0 0 8 4.33V6H3a1 1 0 0 0 0 2h1v11a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3V8h1a1 1 0 0 0 0-2zM10 4.33c0-.16.21-.33.5-.33h3c.29 0 .5.17.5.33V6h-4zM18 19a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V8h12z" transform="scale(0.7, 0.7) translate(5, 5)"/> \
                        </g> \
                    </g> \
                </svg> \
            </div>'
}

// ----- schemeConfig functions -----

function migrateSchemeConfig() {
    let isUpdated = false

    /* TODO next version
    if (schemeConfig.version == 1) {
        schemeConfig.version = 2
        isUpdated = true
    }*/

    if (isUpdated == true) {
        saveSchemeConfig()
    }
}

function loadSchemeConfig() {
    if (!fs.existsSync(schemeConfigFile)) {
        schemeConfig = defaultSchemeConfig
        saveSchemeConfig()
    } else {
        schemeConfig = JSON.parse(fs.readFileSync(schemeConfigFile, 'utf8'));
        migrateSchemeConfig()
    }
}

function saveSchemeConfig() {
    fs.writeFileSync(schemeConfigFile, JSON.stringify(schemeConfig), 'utf8');
}

function addSchemeToHistory(scheme) {
    removeSchemeToHistory(scheme) //remove previous same scheme
    schemeConfig.history.unshift(scheme)
    historyList.insertBefore(createSchemeHistoryItem(scheme), historyList.firstChild)
    saveSchemeConfig()
}

function removeSchemeToHistory(scheme) {
    console.log("removeSchemeHistory(" + scheme + ")")
    const index = schemeConfig.history.indexOf(scheme)
    console.log(historyList.childNodes)
    if (index > -1) {
        console.log("removeSchemeHistory(" + scheme + ") index = " + index)
        schemeConfig.history.splice(index, 1)
        historyList.removeChild(historyList.childNodes.item(index))
    }
    console.log("removeSchemeHistory(" + scheme + ") historyList Result = ")
    console.log(historyList.childNodes)
    saveSchemeConfig()
}