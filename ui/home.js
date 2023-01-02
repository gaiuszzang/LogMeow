const { remote, ipcRenderer, shell } = require('electron')
const fs = require('fs')
const path = require('path')
const setting = require('../settingsModule.js')
const adb = require('../adbModule.js')
const log = require('../logModule.js')
const moment = require('moment')

const mediaDir = (process.platform == "darwin") ? 
    path.join(process.env.HOME, "LogMeow") : "LogMeow";

function initMediaDirectory() {
    if (!fs.existsSync(mediaDir)) {
        fs.mkdirSync(mediaDir, { recursive: true });
    }
}

function openMediaDirectory() {
    shell.openPath(mediaDir)
}

//Table for convert priority to Level
let logLevelTable = ['U', 'U', 'V', 'D', 'I', 'W', 'E', 'F', 'U']


let defaultOpt = document.createElement('option')
let defaultLogTab = document.createElement('logtab')
let defaultLogLine = document.createElement('logline')
let defaultLogTabSpace = document.createElement('logtabspace')

let loggingState = false
let logWrapText = false
let videoRecordingState = false

//Scroll to Bottom Timer
let logScrollToBottom = true
let scrollToBottomTimerMilles = 25
let scrollToBottomTimer = null

//Element for use (lazy init)
let logList

exports.onCreate = function() {
    setting.init()
    const loadedSetting = setting.getSetting()

    log.init(loadedSetting)
    scrollToBottomTimerMilles = loadedSetting.scrollToBottomTimerMilles
    initUI(loadedSetting)

    adb.trackDevices(updateDeviceList)

    log.setLogBufferStatusCallback(updateLogBufferStatus)

    document.querySelector("#pidFilter").addEventListener('input', function() {
        log.updatePidFilter(this.value, setVisibilityLogLine)
    });
    document.querySelector("#tagFilter").addEventListener('input', function() {
        log.updateTagFilter(this.value, setVisibilityLogLine)
    });
    document.querySelector("#messageFilter").addEventListener('input', function() {
        log.updateMessageFilter(this.value, setVisibilityLogLine)
    });
}

exports.updateLevelFilter = function() {
    let levelFilter = document.querySelector('#levelFilter')
    log.updateLevelFilter(levelFilter.options[levelFilter.selectedIndex].value, setVisibilityLogLine)
}

exports.doLogging = function() {
    let serial = getSelectedSerial()
    if (serial != null) {
        loggingState = !loggingState
        updateLoggingButton()
        if (loggingState === true) {
            adb.startLogcat(serial, addLog)
        } else {
            adb.stopLogcat()
        }
    } else {
        alert("There is no selected device.")
        loggingState = false
        updateLoggingButton()
    }
}

exports.recordVideo = function() {
    let serial = getSelectedSerial()
    if (serial != null) {
        videoRecordingState = !videoRecordingState
        updateVideoRecordingButton()
        if (videoRecordingState == true) {
            initMediaDirectory()
            const outputFilePath = path.join(mediaDir, "ScreenRecord_" + moment().format('YYYYMMDD-hhmmss') + ".mp4")
            adb.startVideoRecord(serial, outputFilePath, (output) => {
                videoRecordingState = false
                updateVideoRecordingButton()
                openMediaDirectory()
            })
        } else {
            adb.stopVideoRecord()
        }
    } else {
        alert("There is no selected device.")
        videoRecordingState = false
        updateVideoRecordingButton()
    }
}

exports.openSetting = function() {
    setting.openSettingDirectory()
}

exports.openScheme = function() {
    let serial = getSelectedSerial()
    if (serial != null) {
        ipcRenderer.invoke('openScheme', setting.getSetting(), serial)
    } else {
        alert("There is no selected device.")
    }
}

exports.openScrcpy = async function() {
    let serial = getSelectedSerial()
    if (serial != null) {
        let result = await adb.scrcpy(serial)
        if (result == false) {
            alert("scrcpy cannot found or execute.")
        }
    } else {
        alert("There is no selected device.")
    }
}

exports.takePicture = async function() {
    console.log("takePicture")
    let serial = getSelectedSerial()
    if (serial != null) {
        initMediaDirectory()
        let pictureStream = await adb.takePicture(serial)
        let screenshotFile = path.join(mediaDir, "Screenshot_" + moment().format('YYYYMMDD-hhmmss') + ".png")
        await savePictureStream(pictureStream, screenshotFile)
        openMediaDirectory()
    } else {
        alert("There is no selected device.")
    }
}

exports.clearAll = function() {
    log.removeAllLog()
    logList.innerHTML = ""
}

exports.updateDeviceList = async function() {
    updateDeviceList()
}

exports.toggleScrollToBottom = function() {
    logScrollToBottom = !logScrollToBottom
    updateScrollToBottomButton()
}

exports.toggleLogWrapText = function() {
    logWrapText = !logWrapText
    if (logWrapText) {
        updateProperty('--logtab-log-whitespace', 'pre-wrap')
    } else {
        updateProperty('--logtab-log-whitespace', 'pre')
    }
    updateLogWrapTextButton()
}

function addLog(logEntry) {
    let addResult = log.addLog(logEntry)
    logList.appendChild(createLogLine(logEntry))
    scrollToBottom()
    if (addResult.isExceed) {
        logList.removeChild(logList.firstChild)
    }
    if (!addResult.isFilterSatisfied) {
        setVisibilityLogLine(logEntry.logIndex, false)
    }
}

function updateLogBufferStatus(currerntLogSize, logBufferSize) {
    document.querySelector('#footerStatus').innerHTML = "LogBuffer : " + currerntLogSize + "/" + logBufferSize
}

function initUI(loadedSetting) {
    logList = document.querySelector('#loglist')
    let levelFilter = document.querySelector('#levelFilter')
    levelFilter.options.length = 0
    levelFilter.append(createOption(0, "All"))
    levelFilter.append(createOption(2, "Verbose"))
    levelFilter.append(createOption(3, "Debug"))
    levelFilter.append(createOption(4, "Info"))
    levelFilter.append(createOption(5, "Warning"))
    levelFilter.append(createOption(6, "Error"))
    levelFilter.append(createOption(7, "Fatal"))

    updateLoggingButton()
    updateLogWrapTextButton()
    updateScrollToBottomButton()
    setStyle(loadedSetting.useDarkTheme)
    defaultLogLine.appendChild(createLogTab('logtab_log'))
}

function scrollToBottom() {
    if (scrollToBottomTimer == null & logScrollToBottom === true) {
        scrollToBottomTimer = setTimeout(function() {
            logList.scrollTop = logList.scrollHeight
            scrollToBottomTimer = null
        }, scrollToBottomTimerMilles)
    }
}

function setVisibilityLogLine(logIndex, visible) {
    document.querySelector("#log_" + logIndex).style.display = visible ? '' : 'none'
}

async function updateDeviceList() {
    let devices = await adb.getDevices()
    let deviceList = document.querySelector('#deviceList')
    deviceList.options.length = 0
    devices.forEach(device => {
        deviceList.append(createOption(device.id, '[' + device.id + "] " + device.type))
    })
}

function updateLoggingButton() {
    let button = document.querySelector('#loggingButton')
    if (loggingState === true) {
        button.src = "stop.svg"
        button.alt = "Stop"
    } else {
        button.src = "play.svg"
        button.alt = "Start"
    }
}

function updateLogWrapTextButton() {
    let button = document.querySelector('#logWrapTextButton')
    if (logWrapText === true) {
        button.classList.remove("c_footer_image_button")
        button.classList.add("c_footer_image_button_clicked")
    } else {
        button.classList.remove("c_footer_image_button_clicked")
        button.classList.add("c_footer_image_button")
    }
}

function updateScrollToBottomButton() {
    let button = document.querySelector('#logScrollToBottom')
    if (logScrollToBottom === true) {
        button.classList.remove("c_footer_image_button")
        button.classList.add("c_footer_image_button_clicked")
    } else {
        button.classList.remove("c_footer_image_button_clicked")
        button.classList.add("c_footer_image_button")
    }
}

function updateVideoRecordingButton() {
    let button = document.querySelector('#videoButton')
    if (videoRecordingState === true) {
        button.style.backgroundColor = "#ff000060"
    } else {
        button.style.backgroundColor = null
    }
}


function createLogTab(className) {
    let logTab = defaultLogTab.cloneNode()
    logTab.className = className
    return logTab
}

function createLogLine(logEntry) {
    let dateString = moment(logEntry.date).format('YYYY-MM-DD hh:mm:ss') + '.' + moment(logEntry.date).millisecond()
    let level = logLevelTable[logEntry.priority];

    let logLine = defaultLogLine.cloneNode(true)
    logLine.className = 'logtab_' + level
    logLine.id = 'log_' + logEntry.logIndex

    /* logLine.querySelector('.logtab_date').innerHTML = dateString
    logLine.querySelector('.logtab_pid').innerHTML = logEntry.pid
    logLine.querySelector('.logtab_tid').innerHTML = logEntry.tid
    logLine.querySelector('.logtab_level').innerHTML = level
    logLine.querySelector('.logtab_tag').innerHTML = logEntry.tag
    logLine.querySelector('.logtab_log').innerHTML = logEntry.message */
    logLine.querySelector('.logtab_log').innerHTML = dateString.padEnd(23, ' ') + ' ' +
                                                     logEntry.pid.toString().padStart(5, ' ') + ' ' +
                                                     logEntry.tid.toString().padStart(5, ' ') + ' ' +
                                                     level + ' ' + logEntry.tag + ':  ' + logEntry.message

    logLine.onclick = function() {
        updateFooterDetailMessage(logEntry.message)
    }
    return logLine
}

function updateFooterDetailMessage(message) {
    document.querySelector('#footerDetail').innerHTML = message
}

function createOption(value, text) {
    let opt = defaultOpt.cloneNode()
    opt.value = value
    opt.innerHTML = text
    return opt
}

function updateProperty(propertyName, value) {
    document.documentElement.style.setProperty(propertyName, value);
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

    ipcRenderer.invoke('isDarkMode', isDarkTheme)
}


function getSelectedSerial() {
    const deviceList = document.querySelector('#deviceList')
    if (deviceList.selectedIndex < 0) return null
    const serial = deviceList.options[deviceList.selectedIndex].value
    if (serial == undefined || serial == null || serial == '') {
        return null
    }
    return serial
}

function savePictureStream(pictureStream, filepath) {
    return new Promise(function(resolve, reject) {
        let fileWriteStream = fs.createWriteStream(filepath)
        
        pictureStream.on('data', (data) => {
            fileWriteStream.write(data)
        })
        pictureStream.on('end', () => {
            fileWriteStream.end()
            resolve()
        })
        pictureStream.on('error', (e) => {
            console.log(e)
            resolve()
        })
        fileWriteStream.on('error', (e) => {
            console.log(e)
            resolve()
        })
    })
}