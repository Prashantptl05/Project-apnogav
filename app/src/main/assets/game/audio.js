// Synthesized Web Audio API engine: car engines, desi horns, drift screech, sparks, taali clap, buffalo moo, and dhol celebration!

export class DesiAudioEngine {
  constructor() {
    this.ctx = null;
    this.enabled = true;
    this.engineOsc = null;
    this.engineSubOsc = null;
    this.engineGain = null;
    this.engineFilter = null;
    this.driftGain = null;
    this.hornActive = false;
    this.hornOsc1 = null;
    this.hornOsc2 = null;
    this.hornGain = null;
    this.initialized = false;
  }

  unlock() {
    if (!this.enabled) return;
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      this.ctx = new AudioCtx();
      this._setupContinuousNodes();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume().catch(() => {});
    }
  }

  setEnabled(flag) {
    this.enabled = !!flag;
    if (!this.enabled && this.engineGain && this.ctx) {
      this.engineGain.gain.setTargetAtTime(0, this.ctx.currentTime, 0.05);
      if (this.driftGain) this.driftGain.gain.setTargetAtTime(0, this.ctx.currentTime, 0.05);
      this.stopHorn();
    }
  }

  _setupContinuousNodes() {
    if (!this.ctx || this.initialized) return;
    this.initialized = true;

    // Engine synth
    this.engineOsc = this.ctx.createOscillator();
    this.engineSubOsc = this.ctx.createOscillator();
    this.engineFilter = this.ctx.createBiquadFilter();
    this.engineGain = this.ctx.createGain();

    this.engineOsc.type = 'sawtooth';
    this.engineSubOsc.type = 'triangle';
    this.engineOsc.frequency.value = 55;
    this.engineSubOsc.frequency.value = 28;

    this.engineFilter.type = 'lowpass';
    this.engineFilter.frequency.value = 220;

    this.engineGain.gain.value = 0;

    this.engineOsc.connect(this.engineFilter);
    this.engineSubOsc.connect(this.engineFilter);
    this.engineFilter.connect(this.engineGain);
    this.engineGain.connect(this.ctx.destination);

    this.engineOsc.start();
    this.engineSubOsc.start();

    // Drift noise synth
    const bufferSize = this.ctx.sampleRate * 1.5;
    const noiseBuffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
    const data = noiseBuffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = Math.random() * 2 - 1;
    }
    const noiseSrc = this.ctx.createBufferSource();
    noiseSrc.buffer = noiseBuffer;
    noiseSrc.loop = true;

    const band = this.ctx.createBiquadFilter();
    band.type = 'bandpass';
    band.frequency.value = 1350;
    band.Q.value = 4.2;

    this.driftGain = this.ctx.createGain();
    this.driftGain.gain.value = 0;

    noiseSrc.connect(band);
    band.connect(this.driftGain);
    this.driftGain.connect(this.ctx.destination);
    noiseSrc.start();
  }

  updateEngine(speedKmh, vehicleId, isDriving, isDrifting, isPaused) {
    if (!this.ctx || !this.engineGain) return;
    const now = this.ctx.currentTime;
    if (!this.enabled || !isDriving || isPaused) {
      this.engineGain.gain.setTargetAtTime(0, now, 0.08);
      if (this.driftGain) this.driftGain.gain.setTargetAtTime(0, now, 0.05);
      return;
    }

    const absSpeed = Math.abs(speedKmh);
    const baseFreq =
      vehicleId === 'lambo' ? 88 :
      vehicleId === 'rolls' ? 46 :
      vehicleId === 'gwagon' ? 56 : 52;
    const scale =
      vehicleId === 'lambo' ? 1.45 :
      vehicleId === 'rolls' ? 0.68 : 0.95;

    const targetFreq = baseFreq + absSpeed * scale;
    this.engineOsc.frequency.setTargetAtTime(targetFreq, now, 0.06);
    this.engineSubOsc.frequency.setTargetAtTime(targetFreq * 0.5, now, 0.06);
    this.engineFilter.frequency.setTargetAtTime(200 + absSpeed * 7.5, now, 0.08);

    const vol = Math.min(0.11, 0.028 + (absSpeed / 220) * 0.08);
    this.engineGain.gain.setTargetAtTime(vol, now, 0.08);

    if (this.driftGain) {
      const driftVol = isDrifting && absSpeed > 18 ? 0.075 : 0;
      this.driftGain.gain.setTargetAtTime(driftVol, now, 0.05);
    }
  }

  startHorn(vehicleId = 'scorpio') {
    this.unlock();
    if (!this.enabled || !this.ctx || this.hornActive) return;
    this.hornActive = true;
    const now = this.ctx.currentTime;

    this.hornOsc1 = this.ctx.createOscillator();
    this.hornOsc2 = this.ctx.createOscillator();
    this.hornGain = this.ctx.createGain();
    const filter = this.ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 1800;

    // Distinct desi luxury dual-tone horn frequencies
    const freqs =
      vehicleId === 'gwagon' ? [349, 440] :
      vehicleId === 'rolls' ? [293, 370] :
      vehicleId === 'lambo' ? [466, 587] : [330, 415];

    this.hornOsc1.type = 'sawtooth';
    this.hornOsc2.type = 'sawtooth';
    this.hornOsc1.frequency.setValueAtTime(freqs[0], now);
    this.hornOsc2.frequency.setValueAtTime(freqs[1], now);

    this.hornGain.gain.setValueAtTime(0.001, now);
    this.hornGain.gain.exponentialRampToValueAtTime(0.14, now + 0.03);

    this.hornOsc1.connect(filter);
    this.hornOsc2.connect(filter);
    filter.connect(this.hornGain);
    this.hornGain.connect(this.ctx.destination);

    this.hornOsc1.start(now);
    this.hornOsc2.start(now);
  }

  stopHorn() {
    if (!this.hornActive || !this.ctx) return;
    this.hornActive = false;
    const now = this.ctx.currentTime;
    try {
      if (this.hornGain) {
        this.hornGain.gain.setTargetAtTime(0.0001, now, 0.03);
      }
      const o1 = this.hornOsc1;
      const o2 = this.hornOsc2;
      setTimeout(() => {
        try { o1?.stop(); o2?.stop(); } catch (_) {}
      }, 90);
    } catch (_) {}
    this.hornOsc1 = null;
    this.hornOsc2 = null;
    this.hornGain = null;
  }

  playHornBurst(vehicleId = 'scorpio', durationMs = 280) {
    this.startHorn(vehicleId);
    setTimeout(() => this.stopHorn(), durationMs);
  }

  playTaaliClap() {
    this.unlock();
    if (!this.enabled || !this.ctx) return;
    const now = this.ctx.currentTime;
    for (let c = 0; c < 2; c++) {
      const t = now + c * 0.14;
      const buf = this.ctx.createBuffer(1, Math.floor(this.ctx.sampleRate * 0.09), this.ctx.sampleRate);
      const ch = buf.getChannelData(0);
      for (let i = 0; i < ch.length; i++) {
        ch[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.022));
      }
      const src = this.ctx.createBufferSource();
      src.buffer = buf;
      const bp = this.ctx.createBiquadFilter();
      bp.type = 'bandpass';
      bp.frequency.value = 1150;
      const g = this.ctx.createGain();
      g.gain.setValueAtTime(0.28, t);
      src.connect(bp);
      bp.connect(g);
      g.connect(this.ctx.destination);
      src.start(t);
    }
  }

  playBuffaloMoo() {
    this.unlock();
    if (!this.enabled || !this.ctx) return;
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    const f = this.ctx.createBiquadFilter();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(145, now);
    osc.frequency.exponentialRampToValueAtTime(98, now + 0.55);
    f.type = 'lowpass';
    f.frequency.value = 340;
    g.gain.setValueAtTime(0.001, now);
    g.gain.linearRampToValueAtTime(0.16, now + 0.08);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.65);
    osc.connect(f);
    f.connect(g);
    g.connect(this.ctx.destination);
    osc.start(now);
    osc.stop(now + 0.68);
  }

  playSparksScrape() {
    this.unlock();
    if (!this.enabled || !this.ctx) return;
    const now = this.ctx.currentTime;
    const buf = this.ctx.createBuffer(1, Math.floor(this.ctx.sampleRate * 0.28), this.ctx.sampleRate);
    const ch = buf.getChannelData(0);
    for (let i = 0; i < ch.length; i++) {
      ch[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.09));
    }
    const src = this.ctx.createBufferSource();
    src.buffer = buf;
    const hp = this.ctx.createBiquadFilter();
    hp.type = 'highpass';
    hp.frequency.value = 2600;
    const g = this.ctx.createGain();
    g.gain.setValueAtTime(0.22, now);
    src.connect(hp);
    hp.connect(g);
    g.connect(this.ctx.destination);
    src.start(now);
  }

  playGeofenceBounce() {
    this.unlock();
    if (!this.enabled || !this.ctx) return;
    this.playHornBurst('scorpio', 360);
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(240, now);
    osc.frequency.exponentialRampToValueAtTime(80, now + 0.32);
    g.gain.setValueAtTime(0.26, now);
    g.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
    osc.connect(g);
    g.connect(this.ctx.destination);
    osc.start(now);
    osc.stop(now + 0.36);
  }

  playCheckpointDing() {
    this.unlock();
    if (!this.enabled || !this.ctx) return;
    const now = this.ctx.currentTime;
    [523.25, 659.25, 783.99].forEach((freq, idx) => {
      const osc = this.ctx.createOscillator();
      const g = this.ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, now + idx * 0.06);
      g.gain.setValueAtTime(0.14, now + idx * 0.06);
      g.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.06 + 0.25);
      osc.connect(g);
      g.connect(this.ctx.destination);
      osc.start(now + idx * 0.06);
      osc.stop(now + idx * 0.06 + 0.26);
    });
  }

  // Celebratory Desi Dhol Rhythm on Mission Victory!
  playDholCelebration() {
    this.unlock();
    if (!this.enabled || !this.ctx) return;
    const now = this.ctx.currentTime;
    // Bhangra / Baraat dhol pattern: Dha-ge-na-ti-na-ka-dhin-na
    const pattern = [
      { t: 0.00, bass: true, high: false },
      { t: 0.16, bass: false, high: true },
      { t: 0.30, bass: true, high: true },
      { t: 0.48, bass: false, high: true },
      { t: 0.62, bass: true, high: false },
      { t: 0.78, bass: true, high: true },
      { t: 0.94, bass: false, high: true },
      { t: 1.08, bass: true, high: true },
    ];
    for (let loop = 0; loop < 3; loop++) {
      const offset = loop * 1.24;
      pattern.forEach((hit) => {
        const at = now + offset + hit.t;
        if (hit.bass) {
          const osc = this.ctx.createOscillator();
          const g = this.ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(140, at);
          osc.frequency.exponentialRampToValueAtTime(52, at + 0.16);
          g.gain.setValueAtTime(0.32, at);
          g.gain.exponentialRampToValueAtTime(0.001, at + 0.2);
          osc.connect(g);
          g.connect(this.ctx.destination);
          osc.start(at);
          osc.stop(at + 0.22);
        }
        if (hit.high) {
          const osc = this.ctx.createOscillator();
          const g = this.ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(340, at);
          osc.frequency.exponentialRampToValueAtTime(190, at + 0.08);
          g.gain.setValueAtTime(0.2, at);
          g.gain.exponentialRampToValueAtTime(0.001, at + 0.1);
          osc.connect(g);
          g.connect(this.ctx.destination);
          osc.start(at);
          osc.stop(at + 0.11);
        }
      });
    }
  }
}
