const { remote, ipcRenderer } = require('electron')
const { exec, execSync, spawn } = require('child_process')
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
    const shellCommand = 'am start -a android.intent.action.VIEW -d "' + scheme + '"'
    return new Promise(function(resolve, reject) {
        client.shell(serial, shellCommand)
            .then(adb.util.readAll)
            .then(function(output) {
                resolve(output.toString().trim())
            })
    })
}

exports.takePicture = async function(serial) {
    return await client.screencap(serial)
}

exports.scrcpy = async function(serial) {
    return new Promise(function(resolve, reject) {
        const command = 'scrcpy -s ' + serial + ' --legacy-paste'
        exec(command, (error, stdout, stderr) => {
            if (error != null) {
                resolve(false)
            } else {
                resolve(true)
            }
        })
    })
}

let videoFileName = '/sdcard/logmeow.mp4'
let videoRecordProcess = null

exports.startVideoRecord = function(serial, outputPath, callback) {
    videoRecordProcess = spawn('adb', ['-s', serial, 'shell', 'screenrecord', videoFileName])
    videoRecordProcess.on('close', (code) => {
        //delay 1000ms for saving file internally
        setTimeout(() => {
            const pullCommand = 'adb -s ' + serial + ' pull ' + videoFileName + ' ' + outputPath
            const removeCommand = 'rm ' + videoFileName
            execSync(pullCommand)
            client.shell(serial, removeCommand)
            callback(outputPath)
            videoRecordProcess = null
        }, 1000)
        
    })
}

exports.stopVideoRecord = function() {
    if (videoRecordProcess != null) {
        videoRecordProcess.kill()
    }
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
