'use strict';

const { app, BrowserWindow, shell, session, ipcMain, screen } = require('electron');
const path = require('node:path');
const fs = require('node:fs');

const APP_URL = 'https://tracker.ajdapp.org';
const BASE = new URL(APP_URL);
const INTRO_MARKER = path.join(app.getPath('userData'), 'intro-seen-v2');
const NATIVE_PIP_HOOK = fs.readFileSync(path.join(__dirname, 'native-pip-hook.js'), 'utf8');

let win = null;
let pipWindow = null;
let currentTimer = { end: 0, totalSeconds: 900, label: 'No active break' };

function onlyExternalHttps(raw) {
  try {
    const u = new URL(raw);
    if (u.protocol === 'https:' && u.origin !== BASE.origin) shell.openExternal(u.toString());
  } catch (_) {}
}

function isTrustedRenderer(event) {
  if (!win || win.isDestroyed() || event.sender !== win.webContents ||
      event.senderFrame !== win.webContents.mainFrame) return false;
  try {
    const url = new URL(event.senderFrame.url);
    return url.protocol === 'https:' && url.origin === BASE.origin;
  } catch (_) { return false; }
}

function isPipRenderer(event) {
  if (!pipWindow || pipWindow.isDestroyed() ||
      event.sender !== pipWindow.webContents ||
      event.senderFrame !== pipWindow.webContents.mainFrame) return false;
  try {
    const url = new URL(event.senderFrame.url);
    return url.protocol === 'file:' && decodeURIComponent(url.pathname).endsWith('/pip.html');
  } catch (_) { return false; }
}

function validatedTimer(state) {
  if (!state || typeof state !== 'object' || Array.isArray(state)) return null;
  const rawEnd = Number(state.end);
  const rawTotal = Number(state.totalSeconds);
  if (!Number.isFinite(rawEnd) || !Number.isFinite(rawTotal)) return null;
  // Restrict data crossing the renderer boundary. Neither markup nor paths are passed.
  const end = rawEnd > 0 && rawEnd <= Date.now() + 43200 * 1000 ? Math.max(0, rawEnd) : 0;
  const totalSeconds = Math.max(1, Math.min(43200, rawTotal));
  const label = typeof state.label === 'string'
    ? state.label.slice(0, 120).replace(/[\r\n\t]/g, ' ') : 'No active break';
  return { end, totalSeconds, label };
}

function sendTimerToPip() {
  if (pipWindow && !pipWindow.isDestroyed() && !pipWindow.webContents.isLoading()) {
    pipWindow.webContents.send('ajd:pip-state', currentTimer);
  }
}

function floatingWindowBounds() {
  const display = win && !win.isDestroyed()
    ? screen.getDisplayMatching(win.getBounds()) : screen.getPrimaryDisplay();
  const area = display.workArea;
  const width = Math.min(366, area.width);
  const height = Math.min(232, area.height);
  return {
    width, height,
    x: Math.max(area.x, area.x + area.width - width - 22),
    y: Math.max(area.y, area.y + area.height - height - 26)
  };
}

function openNativePip() {
  if (pipWindow && !pipWindow.isDestroyed()) {
    sendTimerToPip();
    if (pipWindow.isMinimized()) pipWindow.restore();
    pipWindow.showInactive();
    pipWindow.moveTop();
    return;
  }

  const mini = new BrowserWindow({
    ...floatingWindowBounds(),
    minWidth: 286, minHeight: 185,
    show: false, frame: false, resizable: true,
    alwaysOnTop: true, skipTaskbar: true,
    minimizable: false, maximizable: false, fullscreenable: false,
    hasShadow: true, backgroundColor: '#111a31',
    title: 'Break Tracker — Floating Timer',
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'pip-preload.cjs'),
      nodeIntegration: false, contextIsolation: true, sandbox: true,
      webSecurity: true, backgroundThrottling: false
    }
  });

  pipWindow = mini;
  mini.setAlwaysOnTop(true, 'floating');
  if (process.platform === 'darwin') {
    try { mini.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true }); } catch (_) {}
  }

  mini.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  mini.webContents.on('will-navigate', event => event.preventDefault());
  mini.webContents.on('did-finish-load', () => {
    if (mini.isDestroyed()) return;
    mini.webContents.send('ajd:pip-state', currentTimer);
    mini.showInactive();
  });
  mini.on('closed', () => { if (pipWindow === mini) pipWindow = null; });
  mini.loadFile(path.join(__dirname, 'pip.html')).catch(error => {
    console.error('Failed to load native floating timer', error);
    if (!mini.isDestroyed()) mini.close();
  });
}

function installPiPHook() {
  if (!win || win.isDestroyed()) return;
  let trusted = false;
  try { trusted = new URL(win.webContents.getURL()).origin === BASE.origin; } catch (_) {}
  if (!trusted) return;
  win.webContents.executeJavaScript(NATIVE_PIP_HOOK).catch(error => {
    console.warn('Break Tracker PiP integration not yet available on this page', error);
  });
}

function startBrandedApp(target) {
  return target.loadFile(path.join(__dirname, 'intro.html'), {
    query: { first: fs.existsSync(INTRO_MARKER) ? '0' : '1' }
  });
}

ipcMain.on('ajd:intro-done', (event, remember) => {
  const source = event.senderFrame?.url || '';
  if (!source.startsWith('file://') || !source.includes('/intro.html')) return;
  const target = BrowserWindow.fromWebContents(event.sender);
  if (!target || target !== win || target.isDestroyed()) return;
  if (remember === true) {
    try {
      fs.mkdirSync(path.dirname(INTRO_MARKER), { recursive: true });
      fs.writeFileSync(INTRO_MARKER, '1');
    } catch (_) {}
  }
  target.loadURL(APP_URL);
});

ipcMain.on('ajd:pip-open', (event, timer) => {
  if (!isTrustedRenderer(event)) return;
  const value = validatedTimer(timer);
  if (!value) return;
  currentTimer = value;
  openNativePip();
});
ipcMain.on('ajd:pip-sync', (event, timer) => {
  if (!isTrustedRenderer(event)) return;
  const value = validatedTimer(timer);
  if (!value) return;
  currentTimer = value;
  sendTimerToPip();
});
ipcMain.on('ajd:pip-close', event => {
  if (isTrustedRenderer(event) && pipWindow && !pipWindow.isDestroyed()) pipWindow.close();
});
ipcMain.on('ajd:pip-ui-close', event => {
  if (isPipRenderer(event) && pipWindow && !pipWindow.isDestroyed()) pipWindow.close();
});

function createWindow() {
  win = new BrowserWindow({
    width: 1180, height: 840, minWidth: 930, minHeight: 700,
    show: false, icon: path.join(__dirname, 'icon.png'), autoHideMenuBar: true,
    title: 'Break Tracker', backgroundColor: '#0f2628',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      nodeIntegration: false, contextIsolation: true, sandbox: true,
      webSecurity: true, allowRunningInsecureContent: false
    }
  });

  win.once('ready-to-show', () => win.show());
  win.webContents.setWindowOpenHandler(({ url }) => {
    onlyExternalHttps(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (event, url) => {
    try {
      const u = new URL(url);
      if (u.origin === BASE.origin && u.protocol === 'https:') return;
    } catch (_) {}
    event.preventDefault();
    onlyExternalHttps(url);
  });
  win.webContents.on('will-redirect', (event, url) => {
    try { if (new URL(url).origin === BASE.origin) return; } catch (_) {}
    event.preventDefault();
    onlyExternalHttps(url);
  });
  win.webContents.on('did-finish-load', installPiPHook);
  win.webContents.on('did-navigate-in-page', installPiPHook);
  win.on('closed', () => {
    win = null;
    if (pipWindow && !pipWindow.isDestroyed()) pipWindow.close();
  });
  startBrandedApp(win);
}

app.whenReady().then(() => {
  // Hosted content remains sandboxed. Only native timer IPC calls from the
  // exact production origin in the main frame are honored above.
  session.defaultSession.setPermissionCheckHandler((_contents, permission) => {
    return ['notifications', 'fullscreen'].includes(permission);
  });
  session.defaultSession.setPermissionRequestHandler((contents, permission, callback, details) => {
    const requesting = details?.requestingUrl || contents.getURL();
    let trusted = false;
    try { trusted = new URL(requesting).origin === BASE.origin; } catch (_) {}
    callback(trusted && ['notifications', 'fullscreen'].includes(permission));
  });
  createWindow();
  app.on('activate', () => {
    if (!win || win.isDestroyed()) createWindow();
  });
});

app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
