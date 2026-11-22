import * as THREE from 'three';
import { LANDMARKS } from './world.js';

// Helper to build a glowing 3D checkpoint ring
function createCheckpointBeacon(color = 0xf59e0b) {
  const g = new THREE.Group();
  const ring = new THREE.Mesh(
    new THREE.TorusGeometry(4.6, 0.34, 10, 32),
    new THREE.MeshBasicMaterial({ color }),
  );
  ring.rotation.x = Math.PI / 2;
  ring.position.y = 0.55;
  g.add(ring);

  const pillar = new THREE.Mesh(
    new THREE.CylinderGeometry(4.3, 4.3, 18, 24, 1, true),
    new THREE.MeshBasicMaterial({
      color,
      transparent: true,
      opacity: 0.16,
      side: THREE.DoubleSide,
      depthWrite: false,
    }),
  );
  pillar.position.y = 9;
  g.add(pillar);

  g.visible = false;
  return { group: g, ring, pillar };
}

// Helper to build a 3D Buffalo (Bhains) for Mission 5
function createBuffalo(x, z, rotY) {
  const g = new THREE.Group();
  const bodyMat = new THREE.MeshStandardMaterial({ color: 0x1c1917, roughness: 0.85 });
  const hornMat = new THREE.MeshStandardMaterial({ color: 0x57534e, roughness: 0.6 });

  const body = new THREE.Mesh(new THREE.BoxGeometry(1.15, 0.95, 2.2), bodyMat);
  body.position.set(0, 1.15, 0);
  body.castShadow = true;
  g.add(body);

  const head = new THREE.Mesh(new THREE.BoxGeometry(0.68, 0.62, 0.85), bodyMat);
  head.position.set(0, 1.35, 1.32);
  g.add(head);

  [-0.45, 0.45].forEach((hx) => {
    const horn = new THREE.Mesh(new THREE.ConeGeometry(0.08, 0.58, 6), hornMat);
    horn.position.set(hx, 1.65, 1.25);
    horn.rotation.z = hx > 0 ? -0.9 : 0.9;
    g.add(horn);
  });

  [[-0.4, 0.72], [0.4, 0.72], [-0.4, -0.75], [0.4, -0.75]].forEach(([lx, lz]) => {
    const leg = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.8, 0.22), bodyMat);
    leg.position.set(lx, 0.4, lz);
    g.add(leg);
  });

  g.position.set(x, 0, z);
  g.rotation.y = rotY;
  return { group: g, cleared: false, vx: 0, vz: 0 };
}

// Helper to build a 3D Village NPC (Chacha / Sarpanch / Baraat)
function createDesiNpc(shirtColor = 0xfef3c7, turbanColor = 0xf59e0b) {
  const g = new THREE.Group();
  const body = new THREE.Mesh(
    new THREE.BoxGeometry(0.6, 1.15, 0.36),
    new THREE.MeshStandardMaterial({ color: shirtColor, roughness: 0.8 }),
  );
  body.position.y = 0.85;
  body.castShadow = true;
  g.add(body);

  const head = new THREE.Mesh(
    new THREE.BoxGeometry(0.36, 0.36, 0.36),
    new THREE.MeshStandardMaterial({ color: 0xb97a3d, roughness: 0.7 }),
  );
  head.position.y = 1.64;
  g.add(head);

  const turban = new THREE.Mesh(
    new THREE.CylinderGeometry(0.24, 0.22, 0.22, 10),
    new THREE.MeshStandardMaterial({ color: turbanColor, roughness: 0.7 }),
  );
  turban.position.y = 1.9;
  g.add(turban);
  return g;
}

// Helper to build 10 stacked 3D Tarbooz (Watermelons) for Lamborghini trunk
function createTarboozStack() {
  const g = new THREE.Group();
  const mat = new THREE.MeshStandardMaterial({ color: 0x15803d, roughness: 0.55 });
  const stripeMat = new THREE.MeshStandardMaterial({ color: 0x052e16, roughness: 0.65 });
  const melons = [];
  for (let i = 0; i < 10; i++) {
    const m = new THREE.Group();
    const ball = new THREE.Mesh(new THREE.SphereGeometry(0.24, 10, 8), mat);
    const band = new THREE.Mesh(new THREE.TorusGeometry(0.23, 0.03, 6, 10), stripeMat);
    m.add(ball, band);
    const row = i < 6 ? 0 : 1;
    const col = row === 0 ? i % 3 : (i - 6) % 2;
    const depth = row === 0 ? Math.floor(i / 3) : Math.floor((i - 6) / 2);
    m.position.set((col - (row === 0 ? 1 : 0.5)) * 0.44, row * 0.36, (depth - 0.5) * 0.44);
    g.add(m);
    melons.push(m);
  }
  return { group: g, melons };
}

export class MissionManager {
  constructor(scene, audio, callbacks) {
    this.scene = scene;
    this.audio = audio;
    this.callbacks = callbacks;

    this.activeMission = null;
    this.stage = 0;
    this.timeLeft = 0;
    this.elapsed = 0;
    this.hornCount = 0;
    this.watermelonsLeft = 10;
    this.gullyScratches = 0;
    this.target = null; // { x, z, radius, label }

    this.beacon = createCheckpointBeacon(0xf59e0b);
    this.scene.add(this.beacon.group);

    // Floating 3D Golden Directional Arrow above Car / Hero
    this.arrowGroup = new THREE.Group();
    const arrowMat = new THREE.MeshStandardMaterial({
      color: 0xfacc15,
      emissive: 0xf59e0b,
      emissiveIntensity: 0.85,
      roughness: 0.25,
      metalness: 0.6,
    });
    const shaft = new THREE.Mesh(new THREE.BoxGeometry(0.32, 0.18, 1.3), arrowMat);
    const tip = new THREE.Mesh(new THREE.ConeGeometry(0.54, 0.95, 4), arrowMat);
    tip.rotation.x = Math.PI / 2;
    tip.position.z = 1.05;
    this.arrowGroup.add(shaft, tip);
    this.arrowGroup.visible = false;
    this.scene.add(this.arrowGroup);

    // Dynamic props group cleared between missions
    this.propGroup = new THREE.Group();
    this.scene.add(this.propGroup);

    this.buffaloes = [];
    this.baraatNpcs = [];
    this.tarboozRig = null;
    this.pickupProp = null;
    this.geediCheckpoints = [
      { x: 0, z: -450, label: 'Amrawad Road Mod' },
      { x: 440, z: -240, label: 'Sarpanch Khet Corner' },
      { x: 490, z: 210, label: 'Gaon Nukkad' },
      { x: 15, z: 480, label: 'Simrod Link Road' },
      { x: -390, z: 260, label: 'Chana Maidan' },
      { x: 0, z: 0, label: 'Panchayat Chowk Finish!' },
    ];
  }

  clearProps(vehicleRef) {
    while (this.propGroup.children.length > 0) {
      this.propGroup.remove(this.propGroup.children[0]);
    }
    if (this.tarboozRig && vehicleRef?.trunkPropAnchor) {
      vehicleRef.trunkPropAnchor.remove(this.tarboozRig.group);
    }
    this.tarboozRig = null;
    this.buffaloes = [];
    this.baraatNpcs = [];
    this.pickupProp = null;
    this.beacon.group.visible = false;
    this.arrowGroup.visible = false;
  }

  startMission(missionId, gameApi) {
    this.clearProps(gameApi.getVehicleModel());
    this.activeMission = missionId;
    this.stage = 0;
    this.elapsed = 0;
    this.hornCount = 0;
    this.watermelonsLeft = 10;
    this.gullyScratches = 0;

    // Ensure player is in car at mission start
    gameApi.forceDrivingMode();

    switch (missionId) {
      case 'chacha_chai': {
        gameApi.setLoanerVehicle('rolls');
        gameApi.placeVehicle(22, -16, Math.PI / 2);
        this.timeLeft = 90;
        this._setTarget(LANDMARKS.chaiTapri.x, LANDMARKS.chaiTapri.z + 8, 9, 'Pick up Garam Chai at Raju Tapri');
        const chacha = createDesiNpc(0xfef9c3, 0xf97316);
        chacha.position.set(LANDMARKS.chachaKhet.x, 0, LANDMARKS.chachaKhet.z);
        this.propGroup.add(chacha);
        break;
      }
      case 'baraat_entry': {
        gameApi.setLoanerVehicle('gwagon');
        gameApi.placeVehicle(-160, 0, Math.PI / 2);
        this.timeLeft = 0;
        this._setTarget(0, 14, 15, 'Drive to Baraat at Chowk & Blast HORN 3x!');
        // Spawn festive Baraat dancers around Chowk
        const colors = [0xf59e0b, 0xef4444, 0x22c55e, 0xec4899, 0x3b82f6, 0xa855f7];
        for (let i = 0; i < 8; i++) {
          const a = (i / 8) * Math.PI * 2;
          const npc = createDesiNpc(colors[i % colors.length], 0xfacc15);
          npc.position.set(Math.cos(a) * 14, 0, Math.sin(a) * 14);
          this.propGroup.add(npc);
          this.baraatNpcs.push({ npc, phase: i * 0.8 });
        }
        break;
      }
      case 'chana_bhaji': {
        gameApi.placeVehicle(0, 28, 0);
        this.timeLeft = 0;
        this._setTarget(LANDMARKS.chanaField.x, LANDMARKS.chanaField.z, 11, 'Pickup Taaza 3D Chana Ki Bhaji');
        const crate = new THREE.Mesh(
          new THREE.BoxGeometry(2.2, 1.4, 2.2),
          new THREE.MeshStandardMaterial({ color: 0x16a34a, emissive: 0x15803d, emissiveIntensity: 0.45 }),
        );
        crate.position.set(LANDMARKS.chanaField.x, 1.0, LANDMARKS.chanaField.z);
        this.propGroup.add(crate);
        this.pickupProp = crate;
        const sarpanch = createDesiNpc(0xffffff, 0xdc2626);
        sarpanch.position.set(LANDMARKS.sarpanchHouse.x, 0, LANDMARKS.sarpanchHouse.z);
        this.propGroup.add(sarpanch);
        break;
      }
      case 'tarbooz_express': {
        gameApi.setLoanerVehicle('lambo');
        gameApi.placeVehicle(-60, 0, Math.PI / 2);
        this.timeLeft = 110;
        this.watermelonsLeft = 10;
        this.tarboozRig = createTarboozStack();
        gameApi.getVehicleModel().trunkPropAnchor.add(this.tarboozRig.group);
        this._setTarget(LANDMARKS.gaonNukkad.x, LANDMARKS.gaonNukkad.z, 13, 'Deliver 10 Tarbooz to Nukkad (Slow on Breakers!)');
        break;
      }
      case 'bhains_jam': {
        gameApi.placeVehicle(115, 0, Math.PI / 2);
        this.timeLeft = 0;
        // Spawn 4 buffaloes blocking the road at x = 185, z = 0
        for (let i = 0; i < 4; i++) {
          const b = createBuffalo(185 + (i % 2) * 3.5, -4.5 + i * 3.0, Math.PI / 2 + (i - 1.5) * 0.2);
          this.propGroup.add(b.group);
          this.buffaloes.push(b);
        }
        this._setTarget(185, 0, 14, 'Get Out (Walk) & Tap TAALI near Buffaloes!');
        break;
      }
      case 'gully_drive': {
        gameApi.setLoanerVehicle('rolls');
        gameApi.placeVehicle(LANDMARKS.gullyStart.x, LANDMARKS.gullyStart.z, 0);
        this.timeLeft = 0;
        this.gullyScratches = 0;
        this._setTarget(LANDMARKS.gullyExit.x, LANDMARKS.gullyExit.z + 18, 8, 'Exit Narrow Gully with ZERO Wall Scratches!');
        break;
      }
      case 'puncture_jugaad': {
        gameApi.placeVehicle(25, 22, -Math.PI / 2);
        this.timeLeft = 60;
        this._setTarget(LANDMARKS.punctureShop.x, LANDMARKS.punctureShop.z, 11, 'Flat Tyre Pull! Reach Puncture Shop in 60s!');
        break;
      }
      case 'doston_geedi': {
        gameApi.placeVehicle(0, -35, Math.PI);
        this.timeLeft = 0;
        const first = this.geediCheckpoints[0];
        this._setTarget(first.x, first.z, 16, `Geedi 1/6: ${first.label}`);
        break;
      }
    }
  }

  abortMission(vehicleRef) {
    this.clearProps(vehicleRef);
    this.activeMission = null;
    this.target = null;
  }

  _setTarget(x, z, radius, label) {
    this.target = { x, z, radius, label };
    this.beacon.group.position.set(x, 0, z);
    this.beacon.group.visible = true;
    this.arrowGroup.visible = true;
  }

  isPunctureActive() {
    return this.activeMission === 'puncture_jugaad';
  }

  onSpeedBreakerFastHit(speedKmh) {
    if (this.activeMission === 'tarbooz_express' && speedKmh > 52) {
      const lost = speedKmh > 95 ? 3 : 1;
      this.watermelonsLeft = Math.max(0, this.watermelonsLeft - lost);
      if (this.tarboozRig) {
        this.tarboozRig.melons.forEach((m, idx) => {
          m.visible = idx < this.watermelonsLeft;
        });
      }
      this.callbacks.onToast?.(`Arre! Speed breaker par Tarbooz phoot gaya! (${this.watermelonsLeft}/10 bache)`);
      if (this.watermelonsLeft <= 0) {
        this._fail('Saare Tarbooz phoot gaye! Speed breakers par dheere chalo!');
      }
    }
  }

  onGullyWallHit() {
    if (this.activeMission === 'gully_drive') {
      this.gullyScratches++;
      this._fail('Scratch lag gaya! Rolls-Royce ki paint kharab ho gayi, phir se try karo!');
    }
  }

  onHornOrTaali(px, pz, isWalking) {
    if (!this.activeMission) return;

    if (this.activeMission === 'baraat_entry' && this.target) {
      const dist = Math.hypot(px - this.target.x, pz - this.target.z);
      if (dist < 26) {
        this.hornCount++;
        this.audio.playCheckpointDing();
        this.callbacks.onToast?.(`Baraat Swag Horn ${this.hornCount}/3! Full Volume!`);
        if (this.hornCount >= 3) {
          this._complete();
        }
      }
    }

    if (this.activeMission === 'bhains_jam' && this.stage === 0) {
      if (!isWalking) {
        this.callbacks.onToast?.('Bhainsein horn se nahi hatengi! Gaadi se utar kar (Walk) TAALI bajao!');
        return;
      }
      let scaredAny = false;
      this.buffaloes.forEach((b) => {
        const d = Math.hypot(px - b.group.position.x, pz - b.group.position.z);
        if (!b.cleared && d < 22) {
          b.cleared = true;
          scaredAny = true;
          const side = b.group.position.z >= 0 ? 1 : -1;
          b.vx = (Math.random() - 0.5) * 4;
          b.vz = side * (9 + Math.random() * 4);
        }
      });
      if (scaredAny) {
        this.audio.playBuffaloMoo();
        const remaining = this.buffaloes.filter((b) => !b.cleared).length;
        if (remaining === 0) {
          this.stage = 1;
          this.audio.playCheckpointDing();
          this._setTarget(245, 0, 12, 'Rasta Clear! Wapas Gaadi mein baitho aur aage badho!');
          this.callbacks.onToast?.('Shaabash! Bhainsein hat gayi, ab gaadi aage badhao!');
        }
      }
    }
  }

  update(dt, px, pz, py, isWalking, timeSec) {
    // Animate Baraat dancers
    this.baraatNpcs.forEach((item) => {
      item.npc.position.y = Math.abs(Math.sin(timeSec * 6.5 + item.phase)) * 0.7;
      item.npc.rotation.y += dt * 1.4;
    });

    // Move scattered buffaloes
    this.buffaloes.forEach((b) => {
      if (b.cleared) {
        b.group.position.x += b.vx * dt;
        b.group.position.z += b.vz * dt;
        b.group.rotation.y = Math.atan2(b.vx, b.vz);
        b.vx *= 0.985;
        b.vz *= 0.985;
      }
    });

    if (this.pickupProp) {
      this.pickupProp.rotation.y += dt * 1.8;
      this.pickupProp.position.y = 1.1 + Math.sin(timeSec * 3.5) * 0.25;
    }

    if (!this.activeMission || !this.target) return;

    // Beacon pulse
    const s = 1 + Math.sin(timeSec * 5) * 0.08;
    this.beacon.ring.scale.set(s, s, 1);

    // Floating 3D Directional Arrow above player/car
    this.arrowGroup.position.set(px, py + (isWalking ? 3.2 : 3.9) + Math.sin(timeSec * 4) * 0.18, pz);
    const angleToTarget = Math.atan2(this.target.x - px, this.target.z - pz);
    this.arrowGroup.rotation.y = angleToTarget;

    // Timer update
    this.elapsed += dt;
    if (this.timeLeft > 0) {
      this.timeLeft -= dt;
      if (this.timeLeft <= 0) {
        this.timeLeft = 0;
        const msg = this.activeMission === 'chacha_chai'
          ? 'Chai thandi ho gayi! Chacha ne daant laga di!'
          : this.activeMission === 'puncture_jugaad'
            ? 'Tyre poora baith gaya! Phir se try karo!'
            : 'Time khatam ho gaya! Phir se try karo!';
        this._fail(msg);
        return;
      }
    }

    const dist = Math.hypot(px - this.target.x, pz - this.target.z);
    if (dist <= this.target.radius) {
      if (this.activeMission === 'chacha_chai') {
        if (this.stage === 0) {
          this.stage = 1;
          this.audio.playCheckpointDing();
          this._setTarget(LANDMARKS.chachaKhet.x, LANDMARKS.chachaKhet.z, 13, 'Garam Chai mil gayi! Chacha ke Khet bhagao!');
          this.callbacks.onToast?.('Kulhad Garam Chai pickup done! Seedha Chacha ke khet!');
        } else {
          this._complete();
        }
      } else if (this.activeMission === 'chana_bhaji') {
        if (this.stage === 0) {
          this.stage = 1;
          if (this.pickupProp) this.pickupProp.visible = false;
          this.audio.playCheckpointDing();
          this._setTarget(LANDMARKS.sarpanchHouse.x, LANDMARKS.sarpanchHouse.z, 12, 'Deliver Chana Ki Bhaji to Sarpanch Haveli!');
          this.callbacks.onToast?.('Taaza Chana Ki Bhaji load ho gayi! Sarpanch ji ke ghar chalo!');
        } else {
          this._complete();
        }
      } else if (this.activeMission === 'tarbooz_express' || this.activeMission === 'gully_drive' || this.activeMission === 'puncture_jugaad') {
        this._complete();
      } else if (this.activeMission === 'bhains_jam' && this.stage === 1 && !isWalking) {
        this._complete();
      } else if (this.activeMission === 'doston_geedi') {
        this.stage++;
        this.audio.playCheckpointDing();
        if (this.stage >= this.geediCheckpoints.length) {
          this._complete();
        } else {
          const next = this.geediCheckpoints[this.stage];
          this._setTarget(next.x, next.z, 16, `Geedi ${this.stage + 1}/6: ${next.label}`);
          this.callbacks.onToast?.(`Checkpoint ${this.stage}/6 cleared! Agla: ${next.label}`);
        }
      }
    }
  }

  getHudSnapshot(px, pz) {
    if (!this.activeMission) return null;
    const dist = this.target ? Math.round(Math.hypot(this.target.x - px, this.target.z - pz)) : 0;
    return {
      missionId: this.activeMission,
      objective: this.target ? this.target.label : '',
      timeLeftSec: Math.ceil(this.timeLeft),
      distanceMeters: dist,
      targetX: this.target ? this.target.x : 0,
      targetZ: this.target ? this.target.z : 0,
      extraStatus:
        this.activeMission === 'tarbooz_express' ? `Tarbooz: ${this.watermelonsLeft}/10` :
        this.activeMission === 'baraat_entry' ? `Horns: ${this.hornCount}/3` :
        this.activeMission === 'doston_geedi' ? `Lap: ${this.stage + 1}/6` : '',
    };
  }

  _complete() {
    const id = this.activeMission;
    const elapsedSec = Math.round(this.elapsed);
    this.activeMission = null;
    this.target = null;
    this.beacon.group.visible = false;
    this.arrowGroup.visible = false;
    this.audio.playDholCelebration();
    this.callbacks.onComplete?.(id, elapsedSec);
  }

  _fail(reason) {
    const id = this.activeMission;
    this.activeMission = null;
    this.target = null;
    this.beacon.group.visible = false;
    this.arrowGroup.visible = false;
    this.callbacks.onFail?.(id, reason);
  }
}
