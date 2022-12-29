const { remote, ipcRenderer } = require('electron')
let moment = require('moment')

//Table for convert priority to Level
let logLevelTable = ['U', 'U', 'V', 'D', 'I', 'W', 'E', 'F', 'U']


let defaultOpt = document.createElement('option')
let defaultLogTab = document.createElement('logtab')
let defaultLogLine = document.createElement('logline')
let defaultLogTabSpace = document.createElement('logtabspace')

let loggingState = false
let logWrapText = false

//Scroll to Bottom Timer
let logScrollToBottom = true
let scrollToBottomTimerMilles = 25
let scrollToBottomTimer = null

//Element for use (lazy init)
let logList

// lazy init
let setting

exports.init = function(loadedSetting) {
    setting = loadedSetting
    scrollToBottomTimerMilles = setting.scrollToBottomTimerMilles

    logList = document.querySelector('#loglist')
    initLogLevelFilter()
    updateLoggingButton()
    updateLogWrapTextButton()
    updateScrollToBottomButton()
    setStyle(setting.useDarkTheme)
    //init defaultLogLine
    /*
    defaultLogLine.appendChild(createLogTab('logtab_date'))
    defaultLogLine.appendChild(defaultLogTabSpace.cloneNode())
    defaultLogLine.appendChild(createLogTab('logtab_pid'))
    defaultLogLine.appendChild(defaultLogTabSpace.cloneNode())
    defaultLogLine.appendChild(createLogTab('logtab_tid'))
    defaultLogLine.appendChild(defaultLogTabSpace.cloneNode())
    defaultLogLine.appendChild(createLogTab('logtab_level'))
    defaultLogLine.appendChild(defaultLogTabSpace.cloneNode())
    defaultLogLine.appendChild(createLogTab('logtab_tag'))
    defaultLogLine.appendChild(defaultLogTabSpace.cloneNode())*/
    defaultLogLine.appendChild(createLogTab('logtab_log'))
}

exports.updateLogBufferStatus = function(currerntLogSize, logBufferSize) {
    document.querySelector('#footerStatus').innerHTML = "LogBuffer : " + currerntLogSize + "/" + logBufferSize

}

function initLogLevelFilter() {
    let levelFilter = document.querySelector('#levelFilter')
    levelFilter.options.length = 0
    levelFilter.append(createOption(0, "All"))
    levelFilter.append(createOption(2, "Verbose"))
    levelFilter.append(createOption(3, "Debug"))
    levelFilter.append(createOption(4, "Info"))
    levelFilter.append(createOption(5, "Warning"))
    levelFilter.append(createOption(6, "Error"))
    levelFilter.append(createOption(7, "Fatal"))
}

exports.updateDeviceList = function(devices) {
    let deviceList = document.querySelector('#deviceList')
    deviceList.options.length = 0
    devices.forEach(device => {
        deviceList.append(createOption(device.id, '[' + device.id + "] " + device.type))
    })
}

exports.addLogLine = function(logEntry) {
    logList.appendChild(createLogLine(logEntry))
}

exports.scrollToBottom = function() {
    if (scrollToBottomTimer == null & logScrollToBottom === true) {
        scrollToBottomTimer = setTimeout(function() {
            logList.scrollTop = logList.scrollHeight
            scrollToBottomTimer = null
        }, scrollToBottomTimerMilles)
    }
}

exports.toggleLoggingState = function() {
    loggingState = !loggingState
    updateLoggingButton()
    return loggingState
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

exports.removeTopLogLine = function() {
    logList.removeChild(logList.firstChild)
}

exports.removeAllLogLine = function() {
    logList.innerHTML = ""
}

exports.setVisibilityLogLine = function(logIndex, visible) {
    document.querySelector("#log_" + logIndex).style.display = visible ? '' : 'none'
}

exports.openScheme = function(serial) {
    ipcRenderer.invoke('openScheme', setting, serial)
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
