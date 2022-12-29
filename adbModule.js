const { remote, ipcRenderer } = require('electron')
const adb = require('adbkit');
let client = adb.createClient();
let activeLogcat = null
let logIndex = 0

exports.getDevices = async function() {
    return await getAdbDevices()
}

exports.trackDevices = function(callback) {
    trackAdbDevices(callback)
}

exports.startLogcat = function(serial, callback) {
    return startAdbLogcat(serial, callback)
}

exports.stopLogcat = function() {
    return stopAdbLogcat()
}

exports.executeScheme = function(serial, scheme) {
    //adb shell am start -a android.intent.action.VIEW -d $1
    const shellCommand = 'am start -a android.intent.action.VIEW -d ' + scheme
    console.log(shellCommand)
    return new Promise(function(resolve, reject) {
        client.shell(serial, shellCommand)
            .then(adb.util.readAll)
            .then(function(output) {
                console.log('%s', output.toString().trim())
                resolve(output.toString().trim())
            })
    })
}

function getAdbDevices() {
    return new Promise(function(resolve, reject) {
        client.listDevices()
        .then(function(devices) {
            resolve(devices)
        })
    })
}

function trackAdbDevices(callback) {
    console.log("trackAdbDevices")
    client.trackDevices().then(function(tracker) {
        tracker.on('add', function(device) {
            callback()
            setTimeout(function() { callback() }, 1000)
        })
        tracker.on('remove', function(device) {
            callback()
        })
        tracker.on('end', function() {
            callback()
        })
    })
}

function startAdbLogcat(serial, callback) {
    client.openLogcat(serial, {clear: true}).then(function(logcat) {
        activeLogcat = logcat
        console.log('begin')
        logcat.on('entry', function(entry) {
            entry.logIndex = logIndex++
            entry.message = convertSystemSourceToHtml(entry.message)
            callback(entry)
        })
    })
}

function stopAdbLogcat() {
    if (activeLogcat != null) {
        activeLogcat.end()
        activeLogcat = null
    }
}

function convertSystemSourceToHtml(str) {
    str = str.replace(/</g,"&lt;");
    str = str.replace(/>/g,"&gt;");
    str = str.replace(/\"/g,"&quot;");
    str = str.replace(/\'/g,"&#39;");
    return str;
}
