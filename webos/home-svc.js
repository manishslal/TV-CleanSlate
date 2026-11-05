// Small local helper for the Home page: the things a web page cannot do itself. Listens on 127.0.0.1 only.
//   GET /vol?step=1|-1|0|mute   change or read the volume
//   GET /net                    network status
//   GET /news?u=<feed>          a Google News RSS feed, cached 30 minutes (the page itself gets 403)
//   GET /target?id=<app>        what opens when the TV turns on ('' is Home)
//   GET /bing                   Bing's last eight photos of the day (its list does not allow web pages), cached an hour
//   GET /saver?min=<n>          Home's own screensaver delay in minutes (0 is Never); see guardSaver
const http = require('http'), https = require('https'), cp = require('child_process'), fs = require('fs'), url = require('url');
const TARGET = '/media/developer/cleanyourtv/boot-target';
const cache = {};

function luna(uri, payload) {
  return new Promise(resolve => {
    const c = cp.spawn('luna-send', ['-n', '1', '-w', '4000', uri, JSON.stringify(payload || {})], { stdio: ['ignore', 'pipe', 'ignore'] });
    let out = '';
    c.stdout.on('data', d => { out += d; });
    c.on('close', () => { try { resolve(JSON.parse(out)); } catch (e) { resolve({ returnValue: false }); } });
  });
}
const A = 'luna://com.webos.service.audio/master/';
function volume(step) {
  const done = () => luna(A + 'getVolume');
  if (step === '1') return luna(A + 'volumeUp').then(done);
  if (step === '-1') return luna(A + 'volumeDown').then(done);
  if (step === 'mute') return done().then(v => luna(A + 'muteVolume', { muted: !(v.volumeStatus && v.volumeStatus.muteStatus) })).then(done);
  return done();
}
function news(u) {
  const hit = cache[u];
  if (hit && Date.now() - hit.at < 30 * 60000) return Promise.resolve(hit.xml);
  if (url.parse(u).hostname !== 'news.google.com') return Promise.resolve('');
  return new Promise(resolve => {
    https.get(Object.assign(url.parse(u), { headers: { 'User-Agent': 'curl/8' } }), r => {
      let b = '';
      r.setEncoding('utf8');
      r.on('data', d => { b += d; });
      r.on('end', () => { if (r.statusCode === 200) cache[u] = { at: Date.now(), xml: b }; resolve(r.statusCode === 200 ? b : (hit ? hit.xml : '')); });
    }).on('error', () => resolve(hit ? hit.xml : ''));
  });
}
function bing() {
  const hit = cache.bing;
  if (hit && Date.now() - hit.at < 60 * 60000) return Promise.resolve(hit.json);
  return new Promise(resolve => {
    https.get(Object.assign(url.parse('https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-US'), { headers: { 'User-Agent': 'curl/8' } }), r => {
      let b = '';
      r.setEncoding('utf8');
      r.on('data', d => { b += d; });
      r.on('end', () => {
        try { const json = JSON.parse(b); cache.bing = { at: Date.now(), json }; resolve(json); } catch (e) { resolve(hit ? hit.json : { images: [] }); }
      });
    }).on('error', () => resolve(hit ? hit.json : { images: [] }));
  });
}
// the sharper 192 px icon that store apps ship; the page falls back to the helper's 130 px one on a 404
function icon(id, res) {
  const dir = '/media/cryptofs/apps/usr/palm/applications/' + id.replace(/[^\w.-]/g, '');
  fs.readFile(dir + '/appinfo.json', 'utf8', (e, info) => {
    let name = '';
    try { name = JSON.parse(info).extraLargeIcon; } catch (x) {}
    if (e || !name) { res.writeHead(404, { 'access-control-allow-origin': '*' }); return res.end(); }
    fs.readFile(dir + '/' + name, (e2, png) => {
      res.writeHead(e2 ? 404 : 200, { 'content-type': 'image/png', 'access-control-allow-origin': '*', 'cache-control': 'max-age=86400' });
      res.end(e2 ? undefined : png);
    });
  });
}
// The TV's own screensaver (com.webos.app.screensaver, a clock on black) asks tvpower's clients before it
// starts. Home has its own, so while Home is in front and its screensaver is on, the answer is no; over any
// other app, or with Home's set to Never, the TV's starts as before.
const SAVER_CLIENT = 'cleanyourtv-home', P = 'luna://com.webos.service.tvpower/power/';
let saverMin = 5;
function answerSaver(msg) {
  // the last request and its answer, for checking on the TV
  const note = a => { try { fs.writeFileSync('/tmp/home-saver.log', new Date().toISOString() + ' ' + JSON.stringify(msg) + ' ' + a + '\n'); } catch (e) {} };
  if (msg.timestamp === undefined) return note('');
  luna('luna://com.webos.applicationManager/getForegroundAppInfo').then(fg => {
    const ack = !(fg.appId === 'com.webos.app.home' && saverMin > 0);
    luna(P + 'responseScreenSaverRequest', { clientName: SAVER_CLIENT, ack, timestamp: msg.timestamp });
    note('fg=' + fg.appId + ' ack=' + ack);
  });
}
function guardSaver() {
  const c = cp.spawn('luna-send', ['-i', P + 'registerScreenSaverRequest', JSON.stringify({ subscribe: true, clientName: SAVER_CLIENT })], { stdio: ['ignore', 'pipe', 'ignore'] });
  let buf = '';
  // one JSON object per answer, however the tool lays it out (an object cut across reads waits for the rest)
  c.stdout.on('data', d => {
    buf += d;
    let depth = 0, start = -1;
    for (let i = 0; i < buf.length; i++) {
      if (buf[i] === '{') { if (depth++ === 0) start = i; } else if (buf[i] === '}' && depth > 0 && --depth === 0) {
        try { answerSaver(JSON.parse(buf.slice(start, i + 1))); } catch (e) {}
      }
    }
    buf = depth > 0 ? buf.slice(start) : '';
  });
  c.on('close', () => setTimeout(guardSaver, 30000));
}
guardSaver();
http.createServer((req, res) => {
  const q = url.parse(req.url, true);
  const send = o => { res.writeHead(200, { 'content-type': 'application/json', 'access-control-allow-origin': '*' }); res.end(JSON.stringify(o)); };
  if (q.pathname === '/vol') return volume(String(q.query.step || '0')).then(send);
  if (q.pathname === '/net') return luna('luna://com.webos.service.connectionmanager/getStatus').then(send);
  if (q.pathname === '/news') return news(String(q.query.u || '')).then(xml => send({ xml }));
  if (q.pathname === '/icon') return icon(String(q.query.id || ''), res);
  if (q.pathname === '/bing') return bing().then(send);
  if (q.pathname === '/saver') { const n = parseInt(q.query.min, 10); if (n >= 0) saverMin = n; return send({ returnValue: true, min: saverMin }); }
  if (q.pathname === '/target') { fs.writeFileSync(TARGET, String(q.query.id || '').replace(/[^\w.-]/g, '')); return send({ returnValue: true }); }
  res.writeHead(404);
  res.end();
}).listen(8766, '127.0.0.1');
