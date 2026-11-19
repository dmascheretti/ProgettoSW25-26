import numpy as np, wave, sys
SR = 48000; DUR = 30.0; N = int(SR * DUR)
L = np.zeros(N); R = np.zeros(N)
rng = np.random.default_rng(3)
t_all = np.arange(N) / SR

def note(n):  # midi -> Hz
    return 440.0 * 2 ** ((n - 69) / 12)

def env(n, a, d, s, r, sus_len):
    a, d, r = int(a*SR), int(d*SR), int(r*SR); sl = max(0, int(sus_len*SR))
    e = np.concatenate([np.linspace(0, 1, max(a,1)), np.linspace(1, s, max(d,1)), np.full(sl, s), np.linspace(s, 0, max(r,1))])
    return e[:n] if len(e) >= n else np.pad(e, (0, n-len(e)))

def add(sig, t0, gain=1.0, pan=0.0):
    i = int(t0*SR)
    if i >= N: return
    sig = sig[:N-i]
    l = np.cos((pan+1)*np.pi/4); r = np.sin((pan+1)*np.pi/4)
    L[i:i+len(sig)] += sig*gain*l*1.41; R[i:i+len(sig)] += sig*gain*r*1.41

def saw(f, n, detune=(0,)):
    tt = np.arange(n)/SR; out = np.zeros(n)
    for d in detune:
        ff = f*2**(d/1200); ph = rng.random()
        out += 2*((tt*ff+ph) % 1)-1
    return out/len(detune)

def lowpass(x, cutoff):
    # one-pole lowpass (cutoff may be array)
    a = np.exp(-2*np.pi*np.asarray(cutoff)/SR) * np.ones(len(x))
    y = np.zeros_like(x); acc = 0.0
    for i in range(len(x)):
        acc = (1-a[i])*x[i] + a[i]*acc; y[i] = acc
    return y

def lp_fast(x, cutoff):  # FFT brickwall-ish smooth lowpass
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1/SR)
    X *= 1/(1+(f/cutoff)**4); return np.fft.irfft(X, len(x))

def hp_fast(x, cutoff):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1/SR)
    X *= 1-1/(1+(f/cutoff)**4); return np.fft.irfft(X, len(x))

# ---------- tense intro 0-6 ----------
n = int(6.4*SR)
drone = sum(np.sin(2*np.pi*note(m)*np.arange(n)/SR + rng.random()*6) for m in (33, 45, 52))  # A1 A2 E3
drone += 0.4*lp_fast(saw(note(57), n, (-8, 0, 8)), 500)   # A3 dark
e = np.minimum(1, np.arange(n)/SR/1.0) * np.clip((6.3-np.arange(n)/SR)/0.4, 0, 1)
add(drone*e*0.10, 0.0)
# minor 2nd tension swell 3-6
n2 = int(3.2*SR); sw = lp_fast(saw(note(58), n2, (-10, 10)), 900)*np.linspace(0, 1, n2)**2
add(sw*0.05, 3.0, pan=0.3)
# ticking clock + heartbeat
for k in range(12):
    tt = 0.25 + k*0.5
    tick = rng.standard_normal(int(0.02*SR)) * np.exp(-np.arange(int(0.02*SR))/SR*300)
    add(hp_fast(np.pad(tick, (0, 2000)), 3000)*0.18, tt, pan=(-0.4 if k % 2 else 0.4))
for k in range(6):
    for off, g in ((0, 1.0), (0.18, 0.6)):
        m = int(0.25*SR); x = np.arange(m)/SR
        hb = np.sin(2*np.pi*(55-20*x)*x)*np.exp(-x*18)
        add(hb*0.35*g, 0.5+k*0.95+off)
# low battery warning beeps 3.2-5.2
for k in range(3):
    m = int(0.12*SR); x = np.arange(m)/SR
    b = np.sin(2*np.pi*880*x)*env(m, .005, .02, .7, .05, .05)
    add(b*0.06, 3.3+k*0.6, pan=0.0)

# ---------- main groove 6-27 (120 BPM) ----------
BPM = 120; beat = 60/BPM; T0 = 6.0; DROP = 12.5
# chord progression per bar (2s): F  G  Am  C  (C major uplifting): roots
prog = [(53, [65, 69, 72]), (55, [67, 71, 74]), (57, [69, 72, 76]), (48, [64, 67, 72])]
def bar_chord(tb):
    k = int((tb - T0) // (4*beat)) % 4; return prog[k]

# pads 6-27
t = T0
while t < 27.0:
    root, ch = bar_chord(t + 0.01); n = int(4*beat*SR)+int(0.5*SR)
    pad = sum(saw(note(m), n, (-12, -4, 5, 12)) for m in ch)/3
    pad = lp_fast(pad, 1800 if t >= DROP else 1100)
    add(pad*env(n, .3, .2, .9, .5, 4*beat-.5)*0.07, t, pan=0)
    t += 4*beat
# pluck arpeggio 16ths from 6.5
t = T0 + 0.0; i = 0
while t < 27.0:
    root, ch = bar_chord(t + 0.01)
    seq = [ch[0]+12, ch[1]+12, ch[2]+12, ch[1]+12, ch[0]+24, ch[2]+12, ch[1]+12, ch[2]+12]
    m = seq[i % 8]; n = int(0.22*SR); x = np.arange(n)/SR
    pl = (np.sign(np.sin(2*np.pi*note(m)*x))*0.5 + np.sin(2*np.pi*note(m)*x))*np.exp(-x*22)
    g = 0.045 if t < DROP else 0.06
    if t > 6.3: add(lp_fast(pl, 4000)*g, t, pan=(0.35 if i % 2 else -0.35))
    t += beat/2; i += 1
# kick, bass, hats, clap after DROP (and light hats before)
t = T0; k = 0
while t < 26.9:
    if t >= DROP - 1e-6:
        n = int(0.35*SR); x = np.arange(n)/SR
        kick = np.sin(2*np.pi*(45 + 120*np.exp(-x*30))*x)*np.exp(-x*9)
        add(kick*0.55, t)
        root, ch = bar_chord(t+0.01)
        # offbeat bass
        nb = int(0.24*SR); xb = np.arange(nb)/SR
        bs = lp_fast(saw(note(root-12), nb, (0, 7)), 700)*env(nb, .005, .05, .8, .08, .1)
        add(bs*0.22, t+beat/2)
        if k % 2 == 1:
            nc = int(0.2*SR); cl = hp_fast(rng.standard_normal(nc), 1200)*np.exp(-np.arange(nc)/SR*25)
            add(cl*0.08, t, pan=0.1)
    if t >= 8.0:
        nh = int(0.05*SR); hh = hp_fast(rng.standard_normal(nh+500), 7000)[:nh]*np.exp(-np.arange(nh)/SR*90)
        add(hh*(0.05 if t >= DROP else 0.03), t+beat/2, pan=-0.2)
    t += beat; k += 1

# riser before drop
n = int(1.5*SR); x = np.arange(n)/SR
rs = hp_fast(rng.standard_normal(n), 800)*(x/1.5)**2
add(lp_fast(rs, 6000)*0.10, DROP-1.5)
# ---------- SFX ----------
def whoosh(t0, dur=0.6, g=0.12):
    n = int(dur*SR); x = np.arange(n)/SR
    w = rng.standard_normal(n); w = lp_fast(w, 2500)*np.sin(np.pi*x/dur)**2
    add(w*g, t0-dur*0.6)
for tw in (3.0, 6.0, 14.0, 16.5, 22.5, 24.5, 27.0): whoosh(tw)
def tap(t0, g=0.25):
    n = int(0.03*SR); x = np.arange(n)/SR
    add(np.sin(2*np.pi*1800*x)*np.exp(-x*250)*g, t0)
tap(6+2.05); tap(6+4.4); tap(6+6.3)
def chime(t0, notes, g=0.12, step=0.09):
    for j, m in enumerate(notes):
        n = int(1.2*SR); x = np.arange(n)/SR
        c = (np.sin(2*np.pi*note(m)*x)+0.3*np.sin(2*np.pi*note(m)*2*x))*np.exp(-x*4)
        add(c*g, t0+j*step, pan=(j-1)*0.3)
chime(12.55, [76, 79, 84])                 # booking confirmed
chime(16.5+1.72, [84, 88], g=0.10, step=0.07)  # QR check-in beep
# plug click + charging hum (shot4 frame 30 -> 23.5)
n = int(0.06*SR); x = np.arange(n)/SR
clk = (rng.standard_normal(n)*0.6+np.sin(2*np.pi*2400*x))*np.exp(-x*120)
add(clk*0.5, 23.5)
n = int(0.08*SR); x = np.arange(n)/SR
add(np.sin(2*np.pi*90*x)*np.exp(-x*40)*0.4, 23.5)
chime(23.55, [72, 79, 84], g=0.09, step=0.06)
n = int(3.0*SR); x = np.arange(n)/SR
hum = (np.sin(2*np.pi*note(43)*x)+0.5*np.sin(2*np.pi*note(55)*x*(1+0.002*np.sin(x*3))))*np.minimum(1, x/0.6)*np.clip((3-x)/1, 0, 1)
add(lp_fast(hum, 400)*0.06, 23.6)
# rating stars ticks
for j in range(5): tap(24.5+1.6+j*0.1, g=0.12)
# ---------- end sonic logo 27-30 ----------
for j, m in enumerate([72, 76, 79, 84]):
    n = int(2.6*SR); x = np.arange(n)/SR
    c = (np.sin(2*np.pi*note(m)*x)+0.25*np.sin(2*np.pi*note(m)*3*x)*np.exp(-x*6))*np.exp(-x*1.4)
    add(c*0.12, 27.15+j*0.12, pan=(j-1.5)*0.25)
n = int(3.0*SR); final = sum(saw(note(m), n, (-10, 0, 10)) for m in (48, 60, 64, 67, 72))/5
add(lp_fast(final, 1600)*env(n, .1, .3, .7, 1.5, 1.0)*0.10, 27.0)
n = int(0.6*SR); x = np.arange(n)/SR
add(np.sin(2*np.pi*(40+60*np.exp(-x*20))*x)*np.exp(-x*5)*0.5, 27.05)

# ---------- reverb + master ----------
def reverb(x, secs=1.8, mix=0.22):
    n = int(secs*SR); ir = rng.standard_normal(n)*np.exp(-np.arange(n)/SR*3.5)
    ir = lp_fast(ir, 5000); ir /= np.sqrt((ir**2).sum())
    m = len(x)+n; F = 1 << (m-1).bit_length()
    y = np.fft.irfft(np.fft.rfft(x, F)*np.fft.rfft(ir, F), F)[:len(x)]
    return x*(1-mix)+y*mix*1.5
L = reverb(L); R = reverb(R)
fade = np.clip((DUR-t_all)/0.5, 0, 1)*np.minimum(1, t_all/0.05)
L *= fade; R *= fade
pk = max(np.abs(L).max(), np.abs(R).max()); L /= pk/0.89; R /= pk/0.89
L = np.tanh(L*1.3)/np.tanh(1.3); R = np.tanh(R*1.3)/np.tanh(1.3)
data = (np.stack([L, R], 1)*32767*0.92).astype(np.int16)
with wave.open(sys.argv[1], 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data.tobytes())
print('ok')
