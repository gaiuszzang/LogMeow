const { app, BrowserWindow, ipcMain, Tray, Menu, globalShortcut, dialog, powerMonitor } = require('electron');


const appVersion = "1.0.0";
const isDebug = false;

let mainWin;

function createWindow() {
    mainWin = new BrowserWindow({
        width: 1400,
        height: 1000,
        minWidth: 800,
        minHeight: 600,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            enableRemoteModule: true
        },
        frame: true,
        resizable: true,
        show: true,
    });
    mainWin.loadFile('ui/main.html');
    mainWin.setMenu(null);
    // Open the DevTools.
    if (isDebug) {
        mainWin.webContents.openDevTools();
    }
    mainWin.once('show', () => {
        console.log("win is shown(once)");
    })
}

async function onCreate() {
    createWindow();
}

function onDestroy() {
    //nothing to do
}

async function userExit() {
    app.quit();
}

//App LifeCycle
app.on('ready', onCreate);
app.on('will-quit', onDestroy);
powerMonitor.on('shutdown', userExit);
