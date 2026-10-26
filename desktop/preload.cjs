'use strict';
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('ajdIntro', Object.freeze({
  finish: (remember) => {
    if (location.protocol === 'file:') ipcRenderer.send('ajd:intro-done', remember === true);
  }
}));

// The methods are visible to the hosted renderer but every IPC call is
// independently checked in main.cjs against the trusted main frame origin.
if (location.origin === 'https://tracker.ajdapp.org') {
  const sendState = (channel, state) => {
    if (!state || typeof state !== 'object') return;
    ipcRenderer.send(channel, {
      end: Number(state.end) || 0,
      totalSeconds: Number(state.totalSeconds) || 900,
      label: String(state.label || '').slice(0, 120)
    });
  };
  contextBridge.exposeInMainWorld('ajdDesktopPiP', Object.freeze({
    open: state => sendState('ajd:pip-open', state),
    sync: state => sendState('ajd:pip-sync', state),
    close: () => ipcRenderer.send('ajd:pip-close')
  }));
}
