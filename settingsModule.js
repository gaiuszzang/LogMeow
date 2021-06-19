const { remote, ipcRenderer } = require('electron')
let fs = require('fs');
let path = require('path');

const settingDir = (process.platform == "darwin") ? 
    path.join(process.env.HOME, "Library", "LogMeow", "settings") :
    "settings";
const settingFile = path.join(settingDir, "settings.json")

let defaultSetting = {
    version: 1,
    logBufferSize: 10000,
    filterSearchYieldCount: 1000,
    scrollToBottomTimerMilles: 10
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


function migrationSetting() {
    let isUpdated = false
    /*
    if (setting.version == 1) {
        setting.version = 2
        //TODO Update to 2
        isUpdate = true
    }
    */

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
        migrationSetting()
    }
}

function saveConfig() {
    fs.writeFileSync(settingFile, JSON.stringify(setting), 'utf8');
}
