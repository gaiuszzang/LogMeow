const { app, BrowserWindow, ipcMain, Menu, powerMonitor, nativeTheme } = require('electron')
const remote = require('@electron/remote/main');
remote.initialize();



const appVersion = "1.0.9"
const isDebug = false

let mainWin;

function createWindow() {
    mainWin = new BrowserWindow({
        width: 1400,
        height: 1000,
        minWidth: 800,
        minHeight: 600,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false
        },
        frame: true,
        resizable: true,
        show: true,
    });
    remote.enable(mainWin.webContents);
    mainWin.loadFile('ui/home.html')
    mainWin.setMenu(null)
    // Open the DevTools.
    if (isDebug) {
        mainWin.webContents.openDevTools()
    }
    mainWin.once('show', () => {
        console.log("win is shown(once)")
    })
    ipcMain.handle('isDarkMode', (event, isDarkMode) => {
        useDarkMode = isDarkMode
        if (isDarkMode) {
            nativeTheme.themeSource = 'dark'
        } else {
            nativeTheme.themeSource = 'light'
        }
    })
    ipcMain.handle('openScheme', (event, setting, serial) => {
        openSchemeWindow(setting, serial)
    })
    ipcMain.handle('showDialog', (event, message) => {
        showDialog(message)
    })
}

function openSchemeWindow(setting, serial) {
    let schemeWin = new BrowserWindow({
        parent: mainWin,
        modal: false,
        width: 600,
        height: 300,
        minWidth: 500,
        minHeight: 260,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            enableRemoteModule: true
        },
        frame: true,
        resizable: true,
        fullscreen: false,
        show: true,
    })
    schemeWin.loadFile('ui/scheme.html')

    if (isDebug) {
        schemeWin.webContents.openDevTools()
    }
    schemeWin.once('show', () => {
        schemeWin.webContents.send('init', setting, serial)
    })
}

async function onCreate() {
    createWindow()
}

function onDestroy() {
    //nothing to do
}

async function userExit() {
    app.quit();
}

//App LifeCycle
app.on('ready', async () => {
    const {default: fixPath} = await import('fix-path');
    fixPath();
    onCreate();
})
app.on('will-quit', onDestroy)
powerMonitor.on('shutdown', userExit)
