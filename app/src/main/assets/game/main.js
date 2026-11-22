import * as THREE from 'three';
import { DesiAudioEngine } from './audio.js';
import { buildVehicleModel, buildDesiHeroModel } from './vehicles.js';
import { buildJoharBarhaWorld, CENTER_LAT, CENTER_LNG, GEOFENCE_RADIUS, SPEED_BREAKERS } from './world.js';
import { MissionManager } from './missions.js';

function bridgeCall(method, ...args) {
  try {
    if (window.AndroidBridge && typeof window.AndroidBridge[method] === 'function') {
      window.AndroidBridge[method](...args);
    }
  } catch (_) {}
}

export async function bootJoharBarha3D() {
  const canvas = document.getElementById('game-canvas');
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.08;

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(62, window.innerWidth / window.innerHeight, 0.3, 2400);

  const audio = new DesiAudioEngine();
  const world = buildJoharBarhaWorld(scene);

  // State
  let isTitleScreen = true;
  let isPaused = false;
  let isWalking = false;
  let currentVehicleId = 'scorpio';
  let ownedVehicleId = 'scorpio';
  let cameraMode = 'CHASE'; // CHASE | DRIFT | HOOD | DARSHAN_360 | WALK
  let darshanOrbitYaw = 0;
  let darshanPitch = 0.25;

  // Input Controls
  const controls = {
    steer: 0,      // -1..1 (supports both Steer Buttons & Virtual Wheel)
    gas: false,
    brake: false,
    drift: false,
    horn: false,
  };

  // Car Physics State
  const carState = {
    x: 0,
    y: 0,
    z: 22,
    heading: Math.PI, // Facing Panchayat Chowk
    vx: 0,
    vz: 0,
    speed: 0,
    steerAngle: 0,
    bounceY: 0,
    bounceVel: 0,
    lastBreakerHit: 0,
    geofenceCooldown: 0,
  };

  // Hero Paidal Physics State
  const heroState = {
    x: 4,
    y: 0,
    z: 22,
    vy: 0,
    heading: 0,
    onGround: true,
  };

  let vehicleModel = buildVehicleModel(currentVehicleId);
  scene.add(vehicleModel.root);

  const heroModel = buildDesiHeroModel();
  heroModel.root.visible = false;
  scene.add(heroModel.root);

  // Dust, Sparks & 3D Celebration Confetti Particles
  const particles = [];
  const dustGeo = new THREE.SphereGeometry(0.32, 6, 5);
  const sparkGeo = new THREE.BoxGeometry(0.14, 0.14, 0.45);
  const confettiGeo = new THREE.BoxGeometry(0.32, 0.06, 0.22);
  const dustMat = new THREE.MeshBasicMaterial({ color: 0xd6b078, transparent: true, opacity: 0.55 });
  const sparkMat = new THREE.MeshBasicMaterial({ color: 0xffc83d });
  const confettiColors = [0xf59e0b, 0xef4444, 0x22c55e, 0x38bdf8, 0xec4899, 0xfacc15];

  function spawnParticle(type, x, y, z, vx, vy, vz, life) {
    if (particles.length > 140) return;
    let mesh;
    if (type === 'dust') {
      mesh = new THREE.Mesh(dustGeo, dustMat);
    } else if (type === 'spark') {
      mesh = new THREE.Mesh(sparkGeo, sparkMat);
    } else {
      const col = confettiColors[Math.floor(Math.random() * confettiColors.length)];
      mesh = new THREE.Mesh(confettiGeo, new THREE.MeshBasicMaterial({ color: col }));
    }
    mesh.position.set(x, y, z);
    scene.add(mesh);
    particles.push({ mesh, type, vx, vy, vz, life, maxLife: life });
  }

  function burstConfetti(cx, cy, cz) {
    for (let i = 0; i < 65; i++) {
      const a = Math.random() * Math.PI * 2;
      const spd = 4 + Math.random() * 11;
      spawnParticle(
        'confetti',
        cx,
        cy + 2.5,
        cz,
        Math.cos(a) * spd,
        8 + Math.random() * 12,
        Math.sin(a) * spd,
        2.6 + Math.random() * 1.4,
      );
    }
  }

  function swapVehicleMesh(vehicleId, isLoaner = false) {
    if (!isLoaner) ownedVehicleId = vehicleId;
    if (currentVehicleId === vehicleId && vehicleModel) return;
    currentVehicleId = vehicleId;
    scene.remove(vehicleModel.root);
    vehicleModel = buildVehicleModel(vehicleId);
    vehicleModel.root.position.set(carState.x, carState.y, carState.z);
    vehicleModel.root.rotation.y = carState.heading;
    scene.add(vehicleModel.root);
    updateVehicleNightLights(currentTimeOfDay);
  }

  let currentTimeOfDay = 'AFTERNOON';
  function updateVehicleNightLights(mode) {
    if (!vehicleModel?.lightRig) return;
    const isDark = mode === 'NIGHT' || mode === 'GOLDEN_HOUR';
    vehicleModel.lightRig.beamMat.opacity = mode === 'NIGHT' ? 0.2 : mode === 'GOLDEN_HOUR' ? 0.08 : 0.0;
    vehicleModel.lightRig.headMat.emissiveIntensity = isDark ? 2.4 : 0.7;
    if (vehicleModel.underglowMat) {
      vehicleModel.underglowMat.opacity = mode === 'NIGHT' ? 0.85 : 0.45;
    }
  }

  const missions = new MissionManager(scene, audio, {
    onComplete: (missionId, elapsedSec) => {
      const px = isWalking ? heroState.x : carState.x;
      const pz = isWalking ? heroState.z : carState.z;
      burstConfetti(px, 0.5, pz);
      if (currentVehicleId !== ownedVehicleId) {
        swapVehicleMesh(ownedVehicleId, false);
      }
      bridgeCall('onMissionComplete', missionId, elapsedSec);
    },
    onFail: (missionId, reason) => {
      if (currentVehicleId !== ownedVehicleId) {
        swapVehicleMesh(ownedVehicleId, false);
      }
      bridgeCall('onMissionFailed', missionId, reason);
    },
    onToast: (msg) => bridgeCall('onToast', msg),
  });

  const missionGameApi = {
    getVehicleModel: () => vehicleModel,
    setLoanerVehicle: (vid) => swapVehicleMesh(vid, true),
    forceDrivingMode: () => {
      isWalking = false;
      heroModel.root.visible = false;
      if (cameraMode === 'WALK') cameraMode = 'CHASE';
    },
    placeVehicle: (x, z, heading) => {
      carState.x = x;
      carState.z = z;
      carState.heading = heading;
      carState.vx = 0;
      carState.vz = 0;
      carState.speed = 0;
      vehicleModel.root.position.set(x, 0, z);
      vehicleModel.root.rotation.y = heading;
    },
  };

  // Drag-to-orbit on canvas during 360 Gaon Darshan mode
  let dragTouchId = null;
  let lastTouchX = 0;
  let lastTouchY = 0;
  canvas.addEventListener('pointerdown', (e) => {
    audio.unlock();
    if (cameraMode === 'DARSHAN_360') {
      dragTouchId = e.pointerId;
      lastTouchX = e.clientX;
      lastTouchY = e.clientY;
    }
  });
  canvas.addEventListener('pointermove', (e) => {
    if (cameraMode === 'DARSHAN_360' && e.pointerId === dragTouchId) {
      darshanOrbitYaw -= (e.clientX - lastTouchX) * 0.008;
      darshanPitch = Math.max(0.08, Math.min(0.85, darshanPitch + (e.clientY - lastTouchY) * 0.005));
      lastTouchX = e.clientX;
      lastTouchY = e.clientY;
    }
  });
  canvas.addEventListener('pointerup', (e) => {
    if (e.pointerId === dragTouchId) dragTouchId = null;
  });

  // Expose JS API for Kotlin Compose HUD
  window.JoharGame = {
    startPlaying: () => {
      audio.unlock();
      isTitleScreen = false;
      isPaused = false;
    },
    setPaused: (flag) => {
      isPaused = !!flag;
      if (isPaused) audio.stopHorn();
    },
    setControls: (steer, gas, brake, drift) => {
      audio.unlock();
      controls.steer = Math.max(-1, Math.min(1, Number(steer) || 0));
      controls.gas = !!gas;
      controls.brake = !!brake;
      controls.drift = !!drift;
    },
    setHorn: (pressed) => {
      audio.unlock();
      if (pressed && !controls.horn) {
        if (isWalking) {
          audio.playTaaliClap();
          heroModel.clapTimer = 0.45;
          missions.onHornOrTaali(heroState.x, heroState.z, true);
        } else {
          audio.startHorn(currentVehicleId);
          missions.onHornOrTaali(carState.x, carState.z, false);
        }
      } else if (!pressed && controls.horn) {
        audio.stopHorn();
      }
      controls.horn = !!pressed;
    },
    triggerJump: () => {
      audio.unlock();
      if (isWalking && heroState.onGround) {
        heroState.vy = 8.8;
        heroState.onGround = false;
      }
    },
    toggleWalkMode: () => {
      audio.unlock();
      if (!isWalking) {
        // Exit car -> spawn Desi Hero beside driver door
        isWalking = true;
        const sideX = Math.cos(carState.heading) * 2.4;
        const sideZ = -Math.sin(carState.heading) * 2.4;
        heroState.x = carState.x + sideX;
        heroState.z = carState.z + sideZ;
        heroState.y = 0;
        heroState.vy = 0;
        heroState.heading = carState.heading;
        heroModel.root.position.set(heroState.x, 0, heroState.z);
        heroModel.root.rotation.y = heroState.heading;
        heroModel.root.visible = true;
        carState.vx *= 0.2;
        carState.vz *= 0.2;
        cameraMode = 'WALK';
        bridgeCall('onToast', 'Paidal Mode ON! Kurta-Gamcha swag mein gully explore karo!');
      } else {
        // Re-enter car (if within 14m or teleport hero back to driver seat cleanly)
        const dist = Math.hypot(heroState.x - carState.x, heroState.z - carState.z);
        if (dist > 28) {
          // Bring car nearby smoothly so player is never stranded
          carState.x = heroState.x - 3;
          carState.z = heroState.z;
          vehicleModel.root.position.set(carState.x, 0, carState.z);
        }
        isWalking = false;
        heroModel.root.visible = false;
        cameraMode = 'CHASE';
      }
    },
    cycleCamera: () => {
      if (isWalking) {
        cameraMode = cameraMode === 'WALK' ? 'DARSHAN_360' : 'WALK';
      } else {
        const order = ['CHASE', 'DRIFT', 'HOOD', 'DARSHAN_360'];
        const idx = order.indexOf(cameraMode);
        cameraMode = order[(idx + 1) % order.length];
      }
      if (cameraMode === 'DARSHAN_360') {
        darshanOrbitYaw = (isWalking ? heroState.heading : carState.heading) + Math.PI;
      }
      return cameraMode;
    },
    setVehicle: (vehicleId) => {
      swapVehicleMesh(vehicleId, false);
    },
    startMission: (missionId) => {
      audio.unlock();
      isTitleScreen = false;
      isPaused = false;
      missions.startMission(missionId, missionGameApi);
    },
    abortMission: () => {
      missions.abortMission(vehicleModel);
      if (currentVehicleId !== ownedVehicleId) {
        swapVehicleMesh(ownedVehicleId, false);
      }
    },
    respawnAtChowk: () => {
      missionGameApi.forceDrivingMode();
      missionGameApi.placeVehicle(0, 26, Math.PI);
      bridgeCall('onToast', 'Respawned at Gram Panchayat Chowk, Johar Barha!');
    },
    setTimeOfDay: (mode) => {
      currentTimeOfDay = mode || 'AFTERNOON';
      world.applyTimeOfDay(currentTimeOfDay);
      updateVehicleNightLights(currentTimeOfDay);
    },
    setGraphicsQuality: (quality) => {
      const high = quality !== 'BALANCED';
      renderer.setPixelRatio(high ? Math.min(window.devicePixelRatio || 1, 2) : 1.0);
      renderer.shadowMap.enabled = high;
    },
    setSoundEnabled: (enabled) => {
      audio.setEnabled(enabled);
    },
    loadOsm: () => {
      world.loadOsmRoads((status) => bridgeCall('onOsmStatus', status));
    },
    setMapsApiKey: (apiKey) => {
      world.applySatelliteGroundTexture(apiKey || '');
    },
  };

  window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });

  // Hide boot screen and inform Kotlin bridge
  const bootEl = document.getElementById('boot-overlay');
  if (bootEl) bootEl.classList.add('hidden');
  bridgeCall('onEngineReady');

  // Physics & Render Loop
  let lastTime = performance.now();
  let telemetryAccum = 0;
  let distanceAccum = 0;
  const camPosSmooth = new THREE.Vector3(0, 6, 34);
  const camLookSmooth = new THREE.Vector3(0, 1.5, 18);

  function resolveCollisions(x, z, radius, isCar) {
    let outX = x;
    let outZ = z;
    let hitWall = false;
    let hitGullyWall = false;

    for (const c of world.colliders) {
      const dx = outX - c.x;
      const dz = outZ - c.z;
      const ex = c.hw + radius;
      const ez = c.hd + radius;
      if (Math.abs(dx) < ex && Math.abs(dz) < ez) {
        const ox = ex - Math.abs(dx);
        const oz = ez - Math.abs(dz);
        if (ox < oz) {
          outX += dx >= 0 ? ox : -ox;
        } else {
          outZ += dz >= 0 ? oz : -oz;
        }
        hitWall = true;
        if (c.isGullyWall) hitGullyWall = true;
      }
    }
    if (isCar && hitGullyWall) {
      missions.onGullyWallHit();
    }
    return { x: outX, z: outZ, hitWall };
  }

  function enforceGeofence(stateObj, isCar) {
    const r = Math.hypot(stateObj.x, stateObj.z);
    if (r >= GEOFENCE_RADIUS - 6) {
      const nx = stateObj.x / r;
      const nz = stateObj.z / r;
      // Physically bounce player inward!
      stateObj.x = nx * (GEOFENCE_RADIUS - 16);
      stateObj.z = nz * (GEOFENCE_RADIUS - 16);
      if (isCar) {
        stateObj.vx = -nx * 18;
        stateObj.vz = -nz * 18;
        stateObj.heading = Math.atan2(-nx, -nz);
      }
      if (carState.geofenceCooldown <= 0) {
        carState.geofenceCooldown = 3.2;
        audio.playGeofenceBounce();
        bridgeCall('onGeofenceHit');
      }
    }
  }

  function tick(now) {
    requestAnimationFrame(tick);
    const dt = Math.min(0.05, (now - lastTime) / 1000);
    lastTime = now;
    const tSec = now * 0.001;

    // Pulse the 1.5 km red laser geofence subtly
    world.fenceMat.opacity = 0.19 + Math.sin(tSec * 3.5) * 0.07;

    if (carState.geofenceCooldown > 0) carState.geofenceCooldown -= dt;

    if (!isPaused && !isTitleScreen) {
      if (!isWalking) {
        // --- VEHICLE ARCADE PHYSICS ---
        const tuning = vehicleModel.tuning;
        const fwdX = Math.sin(carState.heading);
        const fwdZ = Math.cos(carState.heading);
        const rightX = Math.cos(carState.heading);
        const rightZ = -Math.sin(carState.heading);

        let fwdSpeed = carState.vx * fwdX + carState.vz * fwdZ;
        let latSpeed = carState.vx * rightX + carState.vz * rightZ;

        if (controls.gas) {
          fwdSpeed += tuning.accel * dt;
        }
        if (controls.brake) {
          if (fwdSpeed > 1.5) {
            fwdSpeed = Math.max(0, fwdSpeed - tuning.brake * dt);
          } else {
            fwdSpeed = Math.max(-14, fwdSpeed - tuning.accel * 0.72 * dt);
          }
        }
        if (!controls.gas && !controls.brake) {
          fwdSpeed *= Math.pow(0.28, dt);
          if (Math.abs(fwdSpeed) < 0.08) fwdSpeed = 0;
        }

        // Clamp top speed (reduced if flat tyre in Mission 7)
        const maxSpd = missions.isPunctureActive() ? Math.min(28, tuning.maxSpeed) : tuning.maxSpeed;
        fwdSpeed = Math.max(-15, Math.min(maxSpd, fwdSpeed));

        // Steering (including flat-tire pull during Puncture Wala Jugaad!)
        let effectiveSteer = -controls.steer;
        if (missions.isPunctureActive() && Math.abs(fwdSpeed) > 3) {
          effectiveSteer += 0.28; // Pulls to one side!
        }

        const speedFactor = Math.min(1, Math.abs(fwdSpeed) / 7.5);
        const reverseSign = fwdSpeed >= -0.1 ? 1 : -1;
        const driftBoost = controls.drift ? 1.48 : 1.0;
        carState.heading += effectiveSteer * tuning.steerRate * driftBoost * speedFactor * reverseSign * dt;

        // Lateral grip (slippy when holding DRIFT handbrake!)
        const grip = controls.drift ? tuning.driftGrip : tuning.grip;
        latSpeed *= Math.exp(-grip * dt);
        if (controls.drift && Math.abs(fwdSpeed) > 6) {
          fwdSpeed *= Math.pow(0.75, dt);
        }

        // Reconstruct world velocity
        const nFwdX = Math.sin(carState.heading);
        const nFwdZ = Math.cos(carState.heading);
        const nRightX = Math.cos(carState.heading);
        const nRightZ = -Math.sin(carState.heading);
        carState.vx = nFwdX * fwdSpeed + nRightX * latSpeed;
        carState.vz = nFwdZ * fwdSpeed + nRightZ * latSpeed;
        carState.speed = fwdSpeed;

        const prevX = carState.x;
        const prevZ = carState.z;
        carState.x += carState.vx * dt;
        carState.z += carState.vz * dt;

        // Collisions
        const col = resolveCollisions(carState.x, carState.z, 1.35, true);
        if (col.hitWall) {
          carState.x = col.x;
          carState.z = col.z;
          carState.vx *= -0.25;
          carState.vz *= -0.25;
        }

        // Speed Breakers check (bounce + Lamborghini sparks + Tarbooz check!)
        const speedKmh = Math.abs(fwdSpeed) * 3.6;
        if (tSec - carState.lastBreakerHit > 0.65) {
          for (const sb of SPEED_BREAKERS) {
            if (Math.hypot(carState.x - sb.x, carState.z - sb.z) < 5.5 && speedKmh > 18) {
              carState.lastBreakerHit = tSec;
              carState.bounceVel = Math.min(6.2, speedKmh * 0.055);
              missions.onSpeedBreakerFastHit(speedKmh);
              if (currentVehicleId === 'lambo' && speedKmh > 48) {
                audio.playSparksScrape();
                bridgeCall('onToast', 'CHINGAARI! Lamborghini speed breaker par ragad gayi!');
                for (let s = 0; s < 16; s++) {
                  spawnParticle(
                    'spark',
                    carState.x + (Math.random() - 0.5) * 1.4,
                    0.25,
                    carState.z + (Math.random() - 0.5) * 1.4,
                    -carState.vx * 0.3 + (Math.random() - 0.5) * 8,
                    2 + Math.random() * 5,
                    -carState.vz * 0.3 + (Math.random() - 0.5) * 8,
                    0.55,
                  );
                }
              }
              break;
            }
          }
        }

        // Suspension spring bounce
        carState.bounceVel -= 26 * dt;
        carState.bounceY += carState.bounceVel * dt;
        if (carState.bounceY < 0) {
          carState.bounceY = 0;
          carState.bounceVel = 0;
        }

        // Geofence check
        enforceGeofence(carState, true);

        // Track driven distance
        distanceAccum += Math.hypot(carState.x - prevX, carState.z - prevZ);

        // Animate vehicle mesh, front steering wheels, spinning wheels, brake lights & dust
        vehicleModel.root.position.set(carState.x, carState.bounceY, carState.z);
        vehicleModel.root.rotation.y = carState.heading;
        // Subtle body roll on turns
        vehicleModel.bodyGroup.rotation.z = THREE.MathUtils.lerp(
          vehicleModel.bodyGroup.rotation.z,
          controls.steer * Math.min(1, speedKmh / 90) * 0.06,
          dt * 8,
        );

        carState.steerAngle = THREE.MathUtils.lerp(carState.steerAngle, -controls.steer * 0.45, dt * 10);
        vehicleModel.frontPivots.forEach((p) => { p.rotation.y = carState.steerAngle; });
        const spinDelta = (fwdSpeed / 0.45) * dt;
        vehicleModel.allSpins.forEach((w) => { w.rotation.x += spinDelta; });

        if (vehicleModel.lightRig) {
          vehicleModel.lightRig.brakeMat.emissiveIntensity = controls.brake || controls.drift ? 2.8 : 0.45;
        }

        // Tire dust particles on village lanes or drifting
        if ((controls.drift && speedKmh > 22) || (speedKmh > 30 && Math.random() < 0.35)) {
          const rearX = carState.x - nFwdX * 2.1;
          const rearZ = carState.z - nFwdZ * 2.1;
          spawnParticle(
            'dust',
            rearX + (Math.random() - 0.5) * 1.4,
            0.25,
            rearZ + (Math.random() - 0.5) * 1.4,
            (Math.random() - 0.5) * 2.5,
            1.1 + Math.random() * 1.2,
            (Math.random() - 0.5) * 2.5,
            0.75,
          );
        }

        audio.updateEngine(speedKmh, currentVehicleId, true, controls.drift, false);
      } else {
        // --- 3D PAIDAL (WALKING HERO) PHYSICS ---
        const turnRate = 2.6;
        heroState.heading -= controls.steer * turnRate * dt;
        let moveDir = 0;
        if (controls.gas) moveDir += 1;
        if (controls.brake) moveDir -= 0.7;
        const walkSpeed = controls.drift ? 11.5 : 7.2; // Hold drift pedal to sprint!
        const vx = Math.sin(heroState.heading) * moveDir * walkSpeed;
        const vz = Math.cos(heroState.heading) * moveDir * walkSpeed;

        heroState.x += vx * dt;
        heroState.z += vz * dt;

        // Jump gravity
        if (!heroState.onGround) {
          heroState.vy -= 24 * dt;
          heroState.y += heroState.vy * dt;
          if (heroState.y <= 0) {
            heroState.y = 0;
            heroState.vy = 0;
            heroState.onGround = true;
          }
        }

        const col = resolveCollisions(heroState.x, heroState.z, 0.48, false);
        heroState.x = col.x;
        heroState.z = col.z;
        enforceGeofence(heroState, false);

        heroModel.root.position.set(heroState.x, heroState.y, heroState.z);
        heroModel.root.rotation.y = heroState.heading;

        // Limb animation
        if (Math.abs(moveDir) > 0.05) {
          heroModel.walkPhase += dt * (controls.drift ? 14 : 9.5);
          const sw = Math.sin(heroModel.walkPhase) * 0.65;
          heroModel.leftLegPivot.rotation.x = sw;
          heroModel.rightLegPivot.rotation.x = -sw;
          if (heroModel.clapTimer <= 0) {
            heroModel.leftArmPivot.rotation.x = -sw * 0.8;
            heroModel.rightArmPivot.rotation.x = sw * 0.8;
          }
        } else {
          heroModel.leftLegPivot.rotation.x *= 0.85;
          heroModel.rightLegPivot.rotation.x *= 0.85;
          if (heroModel.clapTimer <= 0) {
            heroModel.leftArmPivot.rotation.x *= 0.85;
            heroModel.rightArmPivot.rotation.x *= 0.85;
          }
        }

        if (heroModel.clapTimer > 0) {
          heroModel.clapTimer -= dt;
          const clap = Math.sin(heroModel.clapTimer * 28) * 0.5;
          heroModel.leftArmPivot.rotation.x = -1.2;
          heroModel.rightArmPivot.rotation.x = -1.2;
          heroModel.leftArmPivot.rotation.z = -0.35 + clap * 0.3;
          heroModel.rightArmPivot.rotation.z = 0.35 - clap * 0.3;
        } else {
          heroModel.leftArmPivot.rotation.z = 0;
          heroModel.rightArmPivot.rotation.z = 0;
        }

        audio.updateEngine(0, currentVehicleId, false, false, false);
      }

      // Mission progress update
      const px = isWalking ? heroState.x : carState.x;
      const pz = isWalking ? heroState.z : carState.z;
      const py = isWalking ? heroState.y : carState.bounceY;
      missions.update(dt, px, pz, py, isWalking, tSec);
    } else {
      audio.updateEngine(0, currentVehicleId, false, false, true);
    }

    // Update particles
    for (let i = particles.length - 1; i >= 0; i--) {
      const p = particles[i];
      p.life -= dt;
      if (p.life <= 0) {
        scene.remove(p.mesh);
        particles.splice(i, 1);
        continue;
      }
      if (p.type === 'confetti') {
        p.vy -= 14 * dt;
        p.mesh.rotation.x += dt * 7;
        p.mesh.rotation.y += dt * 5;
      } else if (p.type === 'spark') {
        p.vy -= 18 * dt;
      }
      p.mesh.position.x += p.vx * dt;
      p.mesh.position.y = Math.max(0.05, p.mesh.position.y + p.vy * dt);
      p.mesh.position.z += p.vz * dt;
    }

    // Move dynamic sun shadow camera to follow player
    const focusX = isWalking ? heroState.x : carState.x;
    const focusZ = isWalking ? heroState.z : carState.z;
    world.sunLight.target.position.set(focusX, 0, focusZ);
    world.sunLight.position.set(focusX + 120, 195, focusZ + 85);

    // --- 3D CAMERA MODES ---
    const desiredPos = new THREE.Vector3();
    const desiredLook = new THREE.Vector3();

    if (isTitleScreen) {
      // Cinematic orbit around Scorpio-N & Gram Panchayat Chowk on title screen
      const orbitA = tSec * 0.28;
      desiredPos.set(carState.x + Math.sin(orbitA) * 11.5, 4.4, carState.z + Math.cos(orbitA) * 11.5);
      desiredLook.set(carState.x, 1.4, carState.z - 2);
    } else if (cameraMode === 'DARSHAN_360') {
      // 360° Gaon Darshan Panoramic Orbit
      darshanOrbitYaw += dt * 0.22;
      const dist = 13.5;
      desiredPos.set(
        focusX + Math.sin(darshanOrbitYaw) * dist * Math.cos(darshanPitch),
        2.2 + Math.sin(darshanPitch) * dist,
        focusZ + Math.cos(darshanOrbitYaw) * dist * Math.cos(darshanPitch),
      );
      desiredLook.set(focusX, 1.8, focusZ);
    } else if (isWalking || cameraMode === 'WALK') {
      const dist = 5.4;
      desiredPos.set(
        heroState.x - Math.sin(heroState.heading) * dist,
        heroState.y + 2.9,
        heroState.z - Math.cos(heroState.heading) * dist,
      );
      desiredLook.set(
        heroState.x + Math.sin(heroState.heading) * 4,
        heroState.y + 1.5,
        heroState.z + Math.cos(heroState.heading) * 4,
      );
    } else if (cameraMode === 'HOOD') {
      const t = vehicleModel.tuning;
      desiredPos.set(
        carState.x + Math.sin(carState.heading) * t.hoodCamZ,
        carState.bounceY + t.hoodCamY,
        carState.z + Math.cos(carState.heading) * t.hoodCamZ,
      );
      desiredLook.set(
        carState.x + Math.sin(carState.heading) * 28,
        carState.bounceY + 1.2,
        carState.z + Math.cos(carState.heading) * 28,
      );
    } else if (cameraMode === 'DRIFT') {
      const t = vehicleModel.tuning;
      const swing = controls.steer * 0.52;
      const a = carState.heading + swing;
      desiredPos.set(
        carState.x - Math.sin(a) * (t.chaseDist + 2.2),
        t.chaseHeight + 1.1,
        carState.z - Math.cos(a) * (t.chaseDist + 2.2),
      );
      desiredLook.set(
        carState.x + Math.sin(carState.heading) * 5.5,
        1.4,
        carState.z + Math.cos(carState.heading) * 5.5,
      );
    } else {
      // Smooth Third-Person Chase Camera
      const t = vehicleModel.tuning;
      const speedPull = Math.min(2.2, Math.abs(carState.speed) * 0.045);
      desiredPos.set(
        carState.x - Math.sin(carState.heading) * (t.chaseDist + speedPull),
        t.chaseHeight + carState.bounceY * 0.35,
        carState.z - Math.cos(carState.heading) * (t.chaseDist + speedPull),
      );
      desiredLook.set(
        carState.x + Math.sin(carState.heading) * 5.0,
        1.45,
        carState.z + Math.cos(carState.heading) * 5.0,
      );
    }

    const lerpRate = cameraMode === 'HOOD' ? 1.0 : Math.min(1, dt * 6.5);
    camPosSmooth.lerp(desiredPos, lerpRate);
    camLookSmooth.lerp(desiredLook, lerpRate);
    camera.position.copy(camPosSmooth);
    camera.lookAt(camLookSmooth);

    renderer.render(scene, camera);

    // Push Telemetry to Android Compose HUD at ~15 Hz
    telemetryAccum += dt;
    if (telemetryAccum >= 0.066) {
      telemetryAccum = 0;
      const px = isWalking ? heroState.x : carState.x;
      const pz = isWalking ? heroState.z : carState.z;
      const headingRad = isWalking ? heroState.heading : carState.heading;
      const metersPerLat = 111132;
      const metersPerLng = 111320 * Math.cos((CENTER_LAT * Math.PI) / 180);
      const lat = CENTER_LAT - pz / metersPerLat;
      const lng = CENTER_LNG + px / metersPerLng;
      const kmh = isWalking ? 0 : Math.round(Math.abs(carState.speed) * 3.6);
      const gear = isWalking ? 'W' : carState.speed < -0.5 ? 'R' : kmh < 2 ? 'N' : String(Math.min(6, 1 + Math.floor(kmh / 36)));

      const payload = {
        speedKmh: kmh,
        gear,
        isWalking,
        cameraMode,
        vehicleId: currentVehicleId,
        x: Math.round(px),
        z: Math.round(pz),
        headingDeg: Math.round(((headingRad * 180) / Math.PI + 360) % 360),
        distFromCenter: Math.round(Math.hypot(px, pz)),
        lat: Number(lat.toFixed(5)),
        lng: Number(lng.toFixed(5)),
        mission: missions.getHudSnapshot(px, pz),
        deltaDistMeters: Math.round(distanceAccum),
      };
      distanceAccum = 0;
      bridgeCall('onTelemetry', JSON.stringify(payload));
    }
  }

  requestAnimationFrame(tick);
}
