const { app, BrowserWindow, Menu, shell, session } = require('electron');
const fs = require('fs');
const path = require('path');

const HOME_URL = 'https://spelunkr.tv/';
const APP_HOSTS = new Set(['spelunkr.tv', 'www.spelunkr.tv']);
const ICON = path.join(__dirname, 'build', 'icon.png');

app.setName('SPELUNKR');
app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');

const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
}

let mainWindow = null;

function stateFile() {
  return path.join(app.getPath('userData'), 'window-state.json');
}

function loadWindowState() {
  try {
    const saved = JSON.parse(fs.readFileSync(stateFile(), 'utf8'));
    if (saved && Number.isFinite(saved.width) && Number.isFinite(saved.height)) {
      return saved;
    }
  } catch {
    /* first launch */
  }
  return { width: 1280, height: 800 };
}

function saveWindowState() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  const bounds = mainWindow.getNormalBounds();
  const state = {
    ...bounds,
    isFullScreen: mainWindow.isFullScreen(),
  };
  try {
    fs.mkdirSync(path.dirname(stateFile()), { recursive: true });
    fs.writeFileSync(stateFile(), JSON.stringify(state));
  } catch {
    /* ignore disk errors */
  }
}

function isAppUrl(url) {
  try {
    const { protocol, hostname } = new URL(url);
    return (protocol === 'https:' || protocol === 'http:') && APP_HOSTS.has(hostname);
  } catch {
    return false;
  }
}

function openExternal(url) {
  if (typeof url === 'string' && /^https?:/i.test(url)) {
    shell.openExternal(url);
  }
}

function createWindow() {
  const saved = loadWindowState();
  mainWindow = new BrowserWindow({
    width: saved.width,
    height: saved.height,
    x: saved.x,
    y: saved.y,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: '#1C0A57',
    title: 'SPELUNKR',
    icon: ICON,
    show: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      backgroundThrottling: false,
      autoplayPolicy: 'no-user-gesture-required',
    },
  });

  if (saved.isFullScreen) mainWindow.setFullScreen(true);

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (!isAppUrl(url)) {
      event.preventDefault();
      openExternal(url);
    }
  });

  mainWindow.webContents.on('did-fail-load', (_event, code, _desc, url, isMainFrame) => {
    if (!isMainFrame || code === -3) return;
    console.error(`Failed to load ${url} (${code})`);
  });

  mainWindow.webContents.on('did-finish-load', () => {
    const title = mainWindow.webContents.getTitle();
    console.log(`LOADED ${mainWindow.webContents.getURL()} — ${title}`);
  });

  mainWindow.on('close', saveWindowState);
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  mainWindow.loadURL(HOME_URL);
}

function goHome() {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.loadURL(HOME_URL);
  }
}

function buildMenu() {
  const template = [
    {
      label: 'SPELUNKR',
      submenu: [
        {
          label: 'About SPELUNKR',
          click: () => {
            if (mainWindow && !mainWindow.isDestroyed()) {
              mainWindow.loadURL('https://spelunkr.tv/about');
            }
          },
        },
        { type: 'separator' },
        { role: 'hide' },
        { role: 'hideOthers' },
        { role: 'unhide' },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'View',
      submenu: [
        {
          label: 'Tune In',
          accelerator: 'CmdOrCtrl+T',
          click: goHome,
        },
        {
          label: 'Back',
          accelerator: 'CmdOrCtrl+[',
          click: () => {
            if (mainWindow?.webContents.canGoBack()) mainWindow.webContents.goBack();
          },
        },
        { type: 'separator' },
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
        {
          label: 'Enter Full Screen',
          accelerator: 'F11',
          click: () => {
            if (!mainWindow || mainWindow.isDestroyed()) return;
            mainWindow.setFullScreen(!mainWindow.isFullScreen());
          },
        },
      ],
    },
    {
      label: 'Window',
      submenu: [{ role: 'minimize' }, { role: 'zoom' }, { role: 'close' }],
    },
  ];

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

app.on('second-instance', () => {
  if (!mainWindow) {
    createWindow();
    return;
  }
  if (mainWindow.isMinimized()) mainWindow.restore();
  mainWindow.focus();
});

app.whenReady().then(() => {
  if (process.platform === 'darwin' && app.dock && fs.existsSync(ICON)) {
    app.dock.setIcon(ICON);
  }

  session.defaultSession.setPermissionRequestHandler((_wc, permission, callback) => {
    callback(permission === 'media' || permission === 'fullscreen' || permission === 'pointerLock');
  });

  buildMenu();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
