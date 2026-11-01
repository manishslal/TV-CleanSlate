/* ---- Tizen: what home-svc.js and CleanYourTV's helper do on webOS ---- */
var LEFT_OUT = /^org\.tizen\.|^ug-|\.preview$|^org\.volt\.|^CSHome|service$|Service$|keyboard|AdSuite|addrmplayer|ADPlayer|PrivacyChoices|ThinExtra|MultiScreen|emanual|DownloadManager|SmartTV$/i;
function tizenApps() {
  return new Promise(function (done, fail) {
    tizen.application.getAppsInfo(function (list) {
      done({ apps: list.filter(function (a) { return a.show && !LEFT_OUT.test(a.id); }).map(function (a) { return { id: a.id, title: a.name, icon: a.iconPath }; }) });
    }, fail);
  });
}
function launchApp(id) {
  return new Promise(function (done, fail) {
    if (window.dbg) dbg('launch ' + id);
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
      done({ wired: { state: wired ? 'connected' : 'off' }, wifi: { state: wifi ? 'connected' : 'off' } });
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
