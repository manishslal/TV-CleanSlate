/* Debug builds only: reports errors and page state to the computer that built them. */
(function () {
  var log = [];
  function send() {
    var x = new XMLHttpRequest();
    var tiles = document.querySelectorAll('.tile'), v0 = document.getElementById('v0'), v1 = document.getElementById('v1');
    x.open('POST', 'http://' + DEBUG_HOST + ':8799/r');
    x.send(JSON.stringify({ log: log, tiles: tiles.length, first: tiles[0] && [tiles[0].offsetLeft, tiles[0].offsetTop, tiles[0].offsetWidth], seventh: tiles[6] && [tiles[6].offsetLeft, tiles[6].offsetTop],
      titles: Array.prototype.slice.call(tiles, 0, 40).map(function (t) { return t.textContent + '|' + (t.querySelector('img') ? t.querySelector('img').naturalWidth : 'noimg'); }),
      clock: (document.getElementById('clock') || {}).textContent, line: (document.getElementById('line') || {}).textContent, pill: (document.getElementById('pill') || {}).textContent,
      video: [v0 && [v0.currentSrc, v0.readyState, v0.networkState, v0.paused, v0.error && v0.error.code], v1 && [v1.currentSrc, v1.readyState, v1.networkState, v1.paused, v1.error && v1.error.code]],
      mem: tizen.systeminfo.getAvailableMemory(), body: document.body.className, hidden: document.hidden }));
  }
  window.addEventListener('error', function (e) { log.push((e.message || 'resource') + ' @' + (e.filename || (e.target && e.target.src) || '') + ':' + e.lineno); }, true);
  window.addEventListener('unhandledrejection', function (e) { log.push('rejection ' + e.reason); });
  document.addEventListener('keydown', function (e) { log.push('key ' + e.keyCode); });
  document.addEventListener('keyup', function (e) { log.push('keyup ' + e.keyCode); send(); });
  window.dbg = function (m) { log.push(m); send(); };
  ['loadstart', 'loadedmetadata', 'canplay', 'playing', 'timeupdate', 'waiting', 'stalled', 'abort', 'emptied', 'error'].forEach(function (name) {
    document.addEventListener(name, function (e) {
      if (e.target.tagName !== 'VIDEO' || (name === 'timeupdate' && e.target.dataset.ticked)) return;
      if (name === 'timeupdate') e.target.dataset.ticked = 1;
      log.push(e.target.id + ' ' + name + ' ' + e.target.readyState + '/' + e.target.networkState + (e.target.error ? ' err' + e.target.error.code : ''));
    }, true);
  });
  setInterval(function () {
    var x = new XMLHttpRequest();
    x.open('GET', 'http://' + DEBUG_HOST + ':8799/cmd?' + Date.now());
    x.onload = function () { if (!x.responseText) return; try { log.push('cmd -> ' + (0, eval)(x.responseText)); } catch (e) { log.push('cmd failed ' + e.message); } send(); };
    x.send();
  }, 1500);
  setInterval(send, 5000);
})();
