const { remote, ipcRenderer, shell } = require('electron')
let fs = require('fs');
let path = require('path');

const settingDir = (process.platform == "darwin") ? 
    path.join(process.env.HOME, "Library", "LogMeow", "settings") :
    "settings";
const settingFile = path.join(settingDir, "settings.json")

let defaultSetting = {
    version: 4,
    logBufferSize: 10000,
    filterSearchYieldCount: 1000,
    scrollToBottomTimerMilles: 10,
    useDarkTheme: false,
    useFilterIgnoreCase: true,
    useMessageFilterHighlight: true
}

let setting = null

exports.init = function() {
    if (!fs.existsSync(settingDir)) {
        fs.mkdirSync(settingDir, { recursive: true });
    }

    loadConfig()
}

exports.getSetting = function() {
    return setting
}

exports.updateSetting = function(newSetting) {
    setting = newSetting
    saveConfig()
}

exports.openSettingDirectory = function() {
    shell.openPath(settingDir)
}

function migrateSetting() {
    let isUpdated = false
    
    if (setting.version == 1) {
        setting.version = 2
        setting.useDarkTheme = false
        isUpdated = true
    }

    if (setting.version == 2) {
        setting.version = 3
        setting.useFilterIgnoreCase = true
        isUpdated = true
    }

    if (setting.version == 3) {
        setting.version = 4
        setting.useMessageFilterHighlight = true
        isUpdated = true
    }

    if (isUpdated == true) {
        saveConfig()
    }
}

function loadConfig() {
    if (!fs.existsSync(settingFile)) {
        setting = defaultSetting
        saveConfig()
    } else {
        setting = JSON.parse(fs.readFileSync(settingFile, 'utf8'));
        migrateSetting()
    }
}

function saveConfig() {
    fs.writeFileSync(settingFile, JSON.stringify(setting), 'utf8');
}
