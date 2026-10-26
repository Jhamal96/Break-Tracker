'use strict';
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('ajdNativeMiniTimer', Object.freeze({
  onState: callback => {
    if (typeof callback === 'function')
      ipcRenderer.on('ajd:pip-state', (_event, state) => callback(state));
  },
  close: () => ipcRenderer.send('ajd:pip-ui-close')
}));
