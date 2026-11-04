#!/usr/bin/env python3
"""Home's own interface sounds, made here so every build ships the same set and no licence applies.

Calm, not clicky: each sound is a soft mallet tone (a kalimba-like pluck of a few sine partials,
eased in over a few milliseconds so nothing clicks), in D major pentatonic so any two sounds that
follow each other agree. The error ("limit") is a low, muted two-note fall, never a buzz.

Writes android/app/src/main/res/raw/ui_{move,select,back,limit}.wav; the web builds copy them.
The clock's flap (ui_flap.wav) is a card's clack, not a tone, and is left as it is.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 48000
OUT = Path(__file__).resolve().parent.parent / 'android/app/src/main/res/raw'

D4, FS4, A4, D5, E5, FS5, A5, B5, D6 = 293.66, 369.99, 440.0, 587.33, 659.26, 739.99, 880.0, 987.77, 1174.66


def mallet(freq, dur, decay, attack=0.004, bright=0.18, start=0.0, length=None, gain=1.0):
    """One soft struck tone: the fundamental, a quiet octave and a faint inharmonic overtone that dies first."""
    n = int(RATE * (length or dur + start))
    out = [0.0] * n
    s0 = int(RATE * start)
    for i in range(s0, n):
        t = (i - s0) / RATE
        if t > dur:
            break
        env = (0.5 - 0.5 * math.cos(math.pi * min(t / attack, 1.0))) * math.exp(-t / decay)
        tail = 1.0 if t < dur - 0.012 else 0.5 + 0.5 * math.cos(math.pi * (t - dur + 0.012) / 0.012)
        v = math.sin(2 * math.pi * freq * t)
        v += bright * math.sin(2 * math.pi * freq * 2 * t) * math.exp(-t / (decay * 0.5))
        v += bright * 0.35 * math.sin(2 * math.pi * freq * 4.2 * t) * math.exp(-t / (decay * 0.15))
        out[i] += v * env * tail * gain
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    return [sum(p[i] for p in parts if i < len(p)) for i in range(n)]


def write(name, samples, peak):
    top = max(abs(s) for s in samples) or 1.0
    with wave.open(str(OUT / f'ui_{name}.wav'), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(s / top * peak * 32767)) for s in samples))


# Focus moving: a short, quiet, round tap. Lowest in level: it plays on every press.
write('move', mallet(A5, 0.07, 0.022, attack=0.003, bright=0.1), 0.22)
# OK: two notes rising a fifth, the second just after the first, like a soft "yes".
write('select', mix(mallet(D5, 0.24, 0.07, length=0.3), mallet(A5, 0.22, 0.07, start=0.055, length=0.3, gain=0.8)), 0.42)
# Back: the same two notes falling.
write('back', mix(mallet(A5, 0.2, 0.06, length=0.26, gain=0.8), mallet(D5, 0.2, 0.065, start=0.05, length=0.26)), 0.34)
# Nowhere to go: low and muted, a gentle fall of a third, no overtones to make it sharp.
write('limit', mix(mallet(FS4, 0.2, 0.06, attack=0.006, bright=0.04, length=0.3), mallet(D4, 0.22, 0.07, attack=0.006, bright=0.04, start=0.075, length=0.3, gain=0.9)), 0.34)
print('wrote', ', '.join(f'ui_{n}.wav' for n in ('move', 'select', 'back', 'limit')), 'to', OUT)
