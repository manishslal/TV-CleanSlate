/* ---- Tizen: plays Apple's aerial .mov files with Media Source. See "Aerial video" in README.md ---- */
var AERIAL_CODEC = 'video/mp4; codecs="avc1.640028"', AERIAL_AHEAD = 15;
function bytes(text) { return text.split('').map(function (c) { return c.charCodeAt(0); }); }
function u32(n) { return [n >>> 24 & 255, n >>> 16 & 255, n >>> 8 & 255, n & 255]; }
function u64(n) { return u32(Math.floor(n / 4294967296)).concat(u32(n % 4294967296)); }
function zeros(n) { var a = []; while (n--) a.push(0); return a; }
function join(parts) {
  var size = 0, at = 0;
  parts.forEach(function (p) { size += p.length; });
  var out = new Uint8Array(size);
  parts.forEach(function (p) { out.set(p, at); at += p.length; });
  return out;
}
function box(name, parts) {
  var body = join(parts), head = new Uint8Array(u32(body.length + 8).concat(bytes(name)));
  return join([head, body]);
}
function moovChild(d, start, end, name) {
  for (var p = start; p + 8 <= end; p += d.getUint32(p)) {
    if (String.fromCharCode(d.getUint8(p + 4), d.getUint8(p + 5), d.getUint8(p + 6), d.getUint8(p + 7)) === name) return [p + 8, p + d.getUint32(p)];
    if (d.getUint32(p) < 8) break;
  }
  return null;
}
function moovPath(d, range, names) {
  return names.reduce(function (r, n) { return r && moovChild(d, r[0], r[1], n); }, range);
}
function videoTrack(buffer) {
  var d = new DataView(buffer);
  for (var p = 0; p + 8 <= buffer.byteLength; p += d.getUint32(p)) {
    var trak = [p + 8, p + d.getUint32(p)], name = String.fromCharCode(d.getUint8(p + 4), d.getUint8(p + 5), d.getUint8(p + 6), d.getUint8(p + 7));
    var hdlr = name === 'trak' && moovPath(d, trak, ['mdia', 'hdlr']);
    if (hdlr && d.getUint32(hdlr[0] + 8) === 0x76696465) return readTrack(buffer, d, trak);
  }
  throw new Error('no video track');
}
function readTrack(buffer, d, trak) {
  var stbl = moovPath(d, trak, ['mdia', 'minf', 'stbl']), tkhd = moovChild(d, trak[0], trak[1], 'tkhd'), mdhd = moovPath(d, trak, ['mdia', 'mdhd']);
  var part = function (n) { return moovChild(d, stbl[0], stbl[1], n); };
  var stts = part('stts'), ctts = part('ctts'), stsz = part('stsz'), stsc = part('stsc'), stss = part('stss'), stco = part('stco'), co64 = part('co64');
  var count = d.getUint32(stsz[0] + 8), fixed = d.getUint32(stsz[0] + 4), i, j, k, p = 0;
  var sizes = new Uint32Array(count), dur = new Uint32Array(count), cto = new Int32Array(count), at = new Float64Array(count), keys = [];
  for (i = 0; i < count; i++) sizes[i] = fixed || d.getUint32(stsz[0] + 12 + i * 4);
  for (i = 0; i < d.getUint32(stts[0] + 4); i++) {
    k = d.getUint32(stts[0] + 8 + i * 8);
    for (j = 0; j < k; j++) dur[p++] = d.getUint32(stts[0] + 12 + i * 8);
  }
  if (ctts) {
    p = 0;
    for (i = 0; i < d.getUint32(ctts[0] + 4); i++) {
      k = d.getUint32(ctts[0] + 8 + i * 8);
      for (j = 0; j < k; j++) cto[p++] = d.getInt32(ctts[0] + 12 + i * 8);
    }
    var low = cto[0];
    for (i = 1; i < count; i++) low = Math.min(low, cto[i]);
    for (i = 0; i < count; i++) cto[i] -= low;
  }
  var chunks = d.getUint32((stco || co64)[0] + 4), runs = d.getUint32(stsc[0] + 4), run = 0, c, offset;
  p = 0;
  for (c = 0; c < chunks; c++) {
    offset = stco ? d.getUint32(stco[0] + 8 + c * 4) : d.getUint32(co64[0] + 8 + c * 8) * 4294967296 + d.getUint32(co64[0] + 12 + c * 8);
    while (run + 1 < runs && d.getUint32(stsc[0] + 8 + (run + 1) * 12) <= c + 1) run++;
    k = d.getUint32(stsc[0] + 12 + run * 12);
    for (j = 0; j < k && p < count; j++) { at[p] = offset; offset += sizes[p++]; }
  }
  if (stss) for (i = 0; i < d.getUint32(stss[0] + 4); i++) keys.push(d.getUint32(stss[0] + 8 + i * 4) - 1);
  else keys.push(0);
  return { count: count, sizes: sizes, dur: dur, cto: cto, at: at, keys: keys, scale: d.getUint32(mdhd[0] + 12),
    width: d.getUint16(tkhd[0] + 76), height: d.getUint16(tkhd[0] + 80), stsd: new Uint8Array(buffer, part('stsd')[0] - 8, part('stsd')[1] - part('stsd')[0] + 8) };
}
/* QuickTime extras that the TV's parser rejects: version fields in the entry header and a 4-byte zero terminator */
function sampleEntry(raw) {
  var size = raw.length, end = new DataView(raw.buffer, raw.byteOffset + size - 4, 4).getUint32(0) ? size : size - 4;
  var out = new Uint8Array(raw.subarray(0, end));
  out.set(zeros(16), 32);
  out.set(u32(end), 0);
  out.set(u32(end - 16), 16);
  return out;
}
function initSegment(t) {
  var stsd = sampleEntry(t.stsd);
  var matrix = u32(0x10000).concat(zeros(12), u32(0x10000), zeros(12), u32(0x40000000));
  var empty = function (name, n) { return box(name, [new Uint8Array(zeros(n))]); };
  var stbl = box('stbl', [stsd, empty('stts', 8), empty('stsc', 8), empty('stsz', 12), empty('stco', 8)]);
  var dref = box('dref', [new Uint8Array(u32(0).concat(u32(1))), box('url ', [new Uint8Array(u32(1))])]);
  var minf = box('minf', [box('vmhd', [new Uint8Array(u32(1).concat(zeros(8)))]), box('dinf', [dref]), stbl]);
  var hdlr = box('hdlr', [new Uint8Array(zeros(8).concat(bytes('vide'), zeros(13)))]);
  var mdhd = box('mdhd', [new Uint8Array(zeros(12).concat(u32(t.scale), u32(0), [0x55, 0xc4, 0, 0]))]);
  var tkhd = box('tkhd', [new Uint8Array(u32(7).concat(zeros(8), u32(1), zeros(24), matrix, u32(t.width * 65536), u32(t.height * 65536)))]);
  var mvhd = box('mvhd', [new Uint8Array(zeros(12).concat(u32(t.scale), u32(0), u32(0x10000), [1, 0], zeros(10), matrix, zeros(24), u32(2)))]);
  var trex = box('trex', [new Uint8Array(u32(0).concat(u32(1), u32(1), zeros(12)))]);
  var trak = box('trak', [tkhd, box('mdia', [mdhd, hdlr, minf])]);
  var ftyp = box('ftyp', [new Uint8Array(bytes('isom').concat(u32(1), bytes('isomavc1')))]);
  return join([ftyp, box('moov', [mvhd, trak, box('mvex', [trex])])]);
}
function mediaSegment(t, first, last, data, from, sequence, start) {
  var rows = last - first, trun = new DataView(new ArrayBuffer(12 + rows * 16)), size = 0, i, at;
  trun.setUint32(0, 0x00000F01);
  trun.setUint32(4, rows);
  for (i = 0; i < rows; i++) {
    at = 12 + i * 16;
    trun.setUint32(at, t.dur[first + i]);
    trun.setUint32(at + 4, t.sizes[first + i]);
    trun.setUint32(at + 8, i ? 0x01010000 : 0x02000000);
    trun.setInt32(at + 12, t.cto[first + i]);
    size += t.sizes[first + i];
  }
  var tfhd = box('tfhd', [new Uint8Array(u32(0x020000).concat(u32(1)))]);
  var tfdt = box('tfdt', [new Uint8Array(u32(0x01000000).concat(u64(start)))]);
  var length = 8 + 16 + 8 + tfhd.length + tfdt.length + 8 + 12 + rows * 16 + 8;
  trun.setUint32(8, length);
  var moof = box('moof', [box('mfhd', [new Uint8Array(u32(0).concat(u32(sequence)))]), box('traf', [tfhd, tfdt, box('trun', [new Uint8Array(trun.buffer)])])]);
  var out = new Uint8Array(moof.length + 8 + size), p = moof.length + 8;
  out.set(moof);
  out.set(u32(size + 8), moof.length);
  out.set(bytes('mdat'), moof.length + 4);
  for (i = first; i < last; i++) {
    out.set(data.subarray(t.at[i] - from, t.at[i] - from + t.sizes[i]), p);
    p += t.sizes[i];
  }
  return out;
}
function getRange(url, from, to, done, fail, tries) {
  var x = new XMLHttpRequest();
  x.open('GET', url);
  x.setRequestHeader('Range', 'bytes=' + from + '-' + to);
  x.responseType = 'arraybuffer';
  x.timeout = 20000;
  x.onload = function () { if (x.status === 206) done(x.response, x); else x.onerror(); };
  x.onerror = x.ontimeout = function () {
    if ((tries || 0) < 2) setTimeout(function () { getRange(url, from, to, done, fail, (tries || 0) + 1); }, 1000);
    else fail();
  };
  x.send();
}
function findMoov(url, done, fail) {
  (function look(at) {
    getRange(url, at, at + 15, function (head) {
      var d = new DataView(head), size = d.getUint32(0), name = String.fromCharCode(d.getUint8(4), d.getUint8(5), d.getUint8(6), d.getUint8(7));
      if (size === 1) size = d.getUint32(8) * 4294967296 + d.getUint32(12);
      if (name === 'moov') getRange(url, at + 8, at + size - 1, done, fail);
      else if (size) look(at + size);
      else fail();
    }, fail);
  })(0);
}
function aerialStop(v) {
  if (v.aerial) { clearInterval(v.aerial.tick); v.aerial.stopped = true; URL.revokeObjectURL(v.aerial.url); v.aerial = null; }
}
function aerialPlay(v, url, fail) {
  var media = new MediaSource(), run = { stopped: false }, buffer, track, key = 0, sequence = 1, start = 0, busy = false;
  aerialStop(v);
  v.aerial = run;
  function give(data) { busy = true; buffer.appendBuffer(data); }
  function fill() {
    if (run.stopped || busy || !buffer || buffer.updating) return;
    var ranges = buffer.buffered, ahead = ranges.length ? ranges.end(ranges.length - 1) - v.currentTime : 0;
    if (ranges.length && v.currentTime - ranges.start(0) > 10) { busy = true; buffer.remove(0, v.currentTime - 5); return; }
    if (ahead > AERIAL_AHEAD) return;
    if (key >= track.keys.length) {
      clearInterval(run.tick);
      if (media.readyState === 'open') media.endOfStream();
      return;
    }
    var first = track.keys[key], last = key + 1 < track.keys.length ? track.keys[key + 1] : track.count, i, from = Infinity, to = 0;
    for (i = first; i < last; i++) { from = Math.min(from, track.at[i]); to = Math.max(to, track.at[i] + track.sizes[i]); }
    busy = true;
    getRange(url, from, to - 1, function (data) {
      if (run.stopped) return;
      var at = start;
      for (i = first; i < last; i++) at += track.dur[i];
      give(mediaSegment(track, first, last, new Uint8Array(data), from, sequence++, start));
      start = at;
      key++;
    }, fail);
  }
  media.addEventListener('sourceopen', function () {
    findMoov(url, function (moov) {
      if (run.stopped) return;
      try { track = videoTrack(moov); } catch (e) { return fail(); }
      buffer = media.addSourceBuffer(AERIAL_CODEC);
      buffer.addEventListener('updateend', function () { busy = false; fill(); });
      give(initSegment(track));
      /* the engine pauses video as the app returns to the foreground */
      run.tick = setInterval(function () { if (v.paused) v.play(); fill(); }, 1000);
    }, fail);
  });
  v.src = run.url = URL.createObjectURL(media);
}
