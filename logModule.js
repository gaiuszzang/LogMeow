const { remote, ipcRenderer } = require('electron')

//Log Buffer
let logBufferSize = 10000
let logBuffer = []
let logBufferStatusCallback = null

//Log Filter
let filterSearchYieldCount = 1000
let useFilterIgnoreCase = true
let levelFilter = 0
let pidFilter = 0
let tagFilter = ""
let messageFilter = ""


let previousFilterJob = null


exports.init = function(setting) {
    logBuffer.length = 0 //clear buffer
    logBufferSize = setting.logBufferSize
    filterSearchYieldCount = setting.filterSearchYieldCount
    useFilterIgnoreCase = setting.useFilterIgnoreCase
}

exports.setLogBufferStatusCallback = function(callback) {
    logBufferStatusCallback = callback
    requestLogBufferStatusCallback()
}

exports.addLog = function(logEntry) {
    let result = {
        isExceed: false,
        isFilterSatisfied: isFilterSatisfied(logEntry)
    }
    logBuffer.push(logEntry)
    if (logBuffer.length > logBufferSize) {
        logBuffer.shift()
        result.isExceed = true
    }
    requestLogBufferStatusCallback()
    return result
}

exports.removeAllLog = function() {
    logBuffer.length = 0
    requestLogBufferStatusCallback()
}

exports.updateLevelFilter = function(level, callback) {
    levelFilter = level
    runFilter(callback)
}

exports.updatePidFilter = function(pid, callback) {
    pidFilter = parseInt(pid)
    if (isNaN(pidFilter)) {
        pidFilter = 0
    }
    runFilter(callback)
}


exports.updateTagFilter = function(tag, callback) {
    tagFilter = tag.trim()
    runFilter(callback)
}

exports.updateMessageFilter = function(keyword, callback) {
    messageFilter = keyword.trim()
    runFilter(callback)
}

function isFilterSatisfied(logEntry) {
    if (levelFilter > 0 && logEntry.priority != levelFilter) {
        return false
    }
    if (pidFilter > 0 && logEntry.pid != pidFilter && logEntry.tid != pidFilter) {
        return false
    }
    if (tagFilter != "") {
        if (useFilterIgnoreCase == true) {
            if (!logEntry.tag.toLowerCase().includes(tagFilter.toLowerCase())) {
                return false
            }
        } else {
            if (!logEntry.tag.includes(tagFilter)) {
                return false
            }
        }
    }
    if (messageFilter != "") {
        if (useFilterIgnoreCase == true) {
            if (!logEntry.message.toLowerCase().includes(messageFilter.toLowerCase())) {
                return false
            }
        } else {
            if (!logEntry.message.includes(messageFilter)) {
                return false
            }
        }
    }
    return true
}

function runFilter(callback) {
    if (previousFilterJob != null && previousFilterJob.done == false) {
        previousFilterJob.done = true
    }
    previousFilterJob = {
        done: false
    }
    asyncRunFilter(previousFilterJob, callback)
}

async function asyncRunFilter(job, callback) {
    for (let i = 0; i < logBuffer.length && job.done != true; i++) {
        let logEntry = logBuffer[i]
        callback(logEntry.logIndex, isFilterSatisfied(logEntry))
        if (i % filterSearchYieldCount == 0) {
            await delay(0)
        }
    }
    job.done = true
}

function delay(millies) {
    return new Promise(function(resolve, reject) {
        setTimeout(function() {
            resolve()
        }, millies)
    })
}

function requestLogBufferStatusCallback() {
    if (logBufferStatusCallback != null) {
        logBufferStatusCallback(logBuffer.length, logBufferSize)
    }
}