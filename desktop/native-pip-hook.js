/* Runs only in the trusted tracker.ajdapp.org renderer of our Electron app.
   Browser/PWA users continue to use the existing Picture-in-Picture implementation. */
(() => {
  if (window.__ajdNativePipHookReady || !window.ajdDesktopPiP?.open) return;
  window.__ajdNativePipHookReady = true;

  function snapshot() {
    let end = 0, label = '', totalSeconds = 900;
    try {
      if (/\/kitt(?:\.html)?\/?$/i.test(location.pathname)) {
        if (typeof data !== 'undefined' && Array.isArray(data)) {
          const car = data.find(v => v?.active && Number(v.active.end) > Date.now());
          if (car) {
            end = Number(car.active.end);
            label = 'Kitt_' + car.number + ' · ' + (car.active.label || 'Break');
            totalSeconds = car.active.key === 'meal' ? 1800 : 900;
          }
        }
      } else {
        if (typeof targetEndTime !== 'undefined' && targetEndTime) {
          const value = targetEndTime instanceof Date ? targetEndTime.getTime() : Number(targetEndTime);
          if (Number.isFinite(value) && value > Date.now()) end = value;
        }
        if (typeof activeBreakName !== 'undefined') label = String(activeBreakName || '');
        if (typeof activeBreakKey !== 'undefined' && activeBreakKey === 'lunch') totalSeconds = 1800;
        const custom = /\((\d+)m\)/i.exec(label);
        if (custom) totalSeconds = Math.min(43200, Math.max(60, Number(custom[1]) * 60));
      }
    } catch (_) { /* May be on an older version of the hosted page. */ }
    if (!end) {
      // Graceful fallback for versions of the site without the timer variables.
      const timer = document.getElementById('timer-display') ||
        [...document.querySelectorAll('[id^="timer-"]')].find(el => /^\d+:\d+$/.test(el.textContent?.trim() || ''));
      const parts = /^(\d+):(\d\d)$/.exec(timer?.textContent?.trim() || '');
      if (parts) {
        const seconds = Number(parts[1]) * 60 + Number(parts[2]);
        if (seconds > 0) end = Date.now() + seconds * 1000;
      }
      const text = document.getElementById('timer-label')?.textContent?.trim();
      if (!label && text) label = text;
    }
    if (!Number.isFinite(end) || end < Date.now() - 2000) end = 0;
    return { end, totalSeconds, label: label || 'No active break' };
  }

  let lastSignature = '';
  function updateIfChanged() {
    const value = snapshot();
    const signature = JSON.stringify(value);
    if (signature === lastSignature) return;
    lastSignature = signature;
    window.ajdDesktopPiP.sync(value);
  }

  // Capture before inline onclick and the site's regular click listener.
  // This opens a genuine native floating window instead of browser PiP.
  document.addEventListener('click', event => {
    const button = event.target?.closest?.('button');
    if (!button || !(button.id === 'float' ||
      button.getAttribute('onclick')?.includes('openFloatingTimer'))) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const value = snapshot();
    lastSignature = JSON.stringify(value);
    window.ajdDesktopPiP.open(value);
  }, true);

  updateIfChanged();
  window.setInterval(updateIfChanged, 1000);
})();
