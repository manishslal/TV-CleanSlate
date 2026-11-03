/* ---- Tizen: what home-svc.js and CleanYourTV's helper do on webOS ---- */
var LEFT_OUT = /^org\.tizen\.|^com\.samsung\.tv\.(?!aria-video$|store$)|^ug-|\.preview$|^org\.volt\.|^CSHome|service$|Service$|keyboard|AdSuite|addrmplayer|ADPlayer|PrivacyChoices|ThinExtra|MultiScreen|emanual|DownloadManager|SmartTV$/i;
function tizenApps() {
  return new Promise(function (done, fail) {
    tizen.application.getAppsInfo(function (list) {
      done({ apps: list.filter(function (a) { return a.show && !LEFT_OUT.test(a.id); }).map(function (a) { return { id: a.id, title: a.name, icon: a.iconPath }; }) });
    }, fail);
  });
}
/* An app can't catch the Home key, but Smart Hub takes focus without hiding this page, so Home reopens itself over it.
   Opening an app leaves focus too, and the limit stops a loop if something else keeps taking focus. */
var HOME_ID = 'CSHome00Te.Home', leftAt = 0, reopens = [];
function reopenHome() {
  var now = Date.now();
  if (!S.takeHome || document.hidden || document.hasFocus() || now - leftAt < 3000) return;
  reopens = reopens.filter(function (t) { return now - t < 30000; });
  if (reopens.length >= 3) return;
  reopens.push(now);
  launchTizen(HOME_ID).catch(function () {});
}
window.addEventListener('blur', function () { setTimeout(reopenHome, 500); });
/* Menu and Source also take focus, and nothing shows which key did it, so Home takes these keys.
   No app can open the Source panel, so the first press lets the key go, and the second reaches the TV. */
var SOURCE_ID = 'org.tizen.SMSourceAPP', SOURCE_WAIT = 6000;
function sourceAgain() {
  var hint = document.getElementById('hint'), text = hint.textContent;
  try { tizen.tvinputdevice.unregisterKey('Source'); } catch (e) {}
  leftAt = Date.now() + SOURCE_WAIT - 3000;
  hint.textContent = 'Press Source again';
  setTimeout(function () {
    hint.textContent = text;
    try { tizen.tvinputdevice.registerKey('Source'); } catch (e) {}
  }, SOURCE_WAIT);
}
function takeKey(name, act) {
  try {
    tizen.tvinputdevice.registerKey(name);
    var code = tizen.tvinputdevice.getKey(name).code;
    document.addEventListener('keydown', function (e) {
      if (e.keyCode !== code) return;
      e.stopImmediatePropagation();
      e.preventDefault();
      act();
    }, true);
  } catch (e) {}
}
takeKey('Menu', function () { launchApp('org.tizen.menu').catch(function () {}); });
takeKey('Source', sourceAgain);
function launchApp(id) {
  leftAt = Date.now();
  if (id === SOURCE_ID) { sourceAgain(); return Promise.resolve(); }
  return launchTizen(id);
}
/* On some TVs the store's Netflix ID opens a black screen, and the built-in app is the one Smart Hub opens */
var LAUNCH_AS = { 'RN1MCdNq8t.Netflix': 'org.tizen.netflix-app' };
function launchTizen(id) {
  return new Promise(function (done, fail) {
    if (window.dbg) dbg('launch ' + id);
    if (LAUNCH_AS[id]) return tizen.application.launch(LAUNCH_AS[id], done, function () { tizen.application.launch(id, done, fail); });
    tizen.application.launch(id, done, function (e) { if (window.dbg) dbg('launch failed ' + e.name + ' ' + e.message); fail(e); });
  });
}
function volumeState() {
  return { volumeStatus: { volume: tizen.tvaudiocontrol.getVolume(), muteStatus: tizen.tvaudiocontrol.isMute() } };
}
function setVolumeStep(step) {
  var a = tizen.tvaudiocontrol;
  if (step === '1') a.setVolumeUp();
  else if (step === '-1') a.setVolumeDown();
  else if (step === 'mute') a.setMute(!a.isMute());
  return volumeState();
}
function networkState() {
  return new Promise(function (done) {
    tizen.systeminfo.getPropertyValue('NETWORK', function (n) {
      var wired = n.networkType === 'ETHERNET', wifi = n.networkType === 'WIFI';
      var state = { wired: { state: wired ? 'connected' : 'off' }, wifi: { state: wifi ? 'connected' : 'off' } };
      if (!wired && !wifi) return done(state);
      tizen.systeminfo.getPropertyValue(wired ? 'ETHERNET_NETWORK' : 'WIFI_NETWORK', function (d) {
        var link = wired ? state.wired : state.wifi;
        link.ipAddress = d.ipAddress;
        if (d.ssid) link.ssid = d.ssid;
        done(state);
      }, function () { done(state); });
    }, function () { done({}); });
  });
}
api = function (path, body) {
  if (path === '/api/apps') return tizenApps();
  if (path === '/api/inputs') return Promise.resolve({});
  if (path === '/api/launch') return launchApp(body.id);
  return Promise.resolve({});
};
svc = function (path) {
  var q = path.split('?'), value = decodeURIComponent((q[1] || '').replace(/^\w+=/, ''));
  if (q[0] === '/news') return fetch(value).then(function (r) { return r.text(); }).then(function (xml) { return { xml: xml }; });
  if (q[0] === '/vol') return Promise.resolve(setVolumeStep(value));
  if (q[0] === '/net') return networkState();
  return Promise.resolve({});
};
/* This engine has no CSS variables, so the focus glow's app color goes in a rule of its own. */
function haloStyle(c) {
  var el = document.getElementById('halo');
  if (!el) { el = document.createElement('style'); el.id = 'halo'; document.head.appendChild(el); }
  var ring = '0 0 0 5px rgba(0,0,0,.75), 0 0 0 13px #f5f5f7', light = '0 0 0 5px rgba(0,0,0,.4), 0 0 0 13px #f5f5f7';
  el.textContent = '.tile.f .plate { box-shadow: ' + ring + ', 0 0 30px ' + c + '; }' +
    'body.light:not(.lite) .tile.f .plate { box-shadow: ' + light + ', 0 0 30px ' + c + '; }' +
    'body.glowlarge .tile.f .plate { box-shadow: ' + ring + ', 0 0 56px ' + c + '; }' +
    'body.fnoring .tile.f .plate { box-shadow: 0 0 30px ' + c + '; }' +
    'body.fnoring.glowlarge .tile.f .plate { box-shadow: 0 0 56px ' + c + '; }';
}
