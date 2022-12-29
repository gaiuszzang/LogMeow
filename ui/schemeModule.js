const { remote, ipcRenderer } = require('electron')
const adb = require('../adbModule.js')

let serial

ipcRenderer.on('init', (event, setting, targetSerial) => {
    setStyle(setting.useDarkTheme)
    serial = targetSerial
    console.log(serial)
})

exports.executeScheme = async function(scheme) {
    return await adb.executeScheme(serial, scheme)
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
