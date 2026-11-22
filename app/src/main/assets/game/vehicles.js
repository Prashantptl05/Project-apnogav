import * as THREE from 'three';

export const VEHICLE_TUNING = {
  scorpio: {
    id: 'scorpio',
    name: 'Scorpio-N / Thar',
    maxSpeed: 41,        // ~148 km/h
    accel: 24,
    brake: 36,
    steerRate: 2.35,
    grip: 6.2,
    driftGrip: 2.1,
    clearance: 0.72,
    width: 2.25,
    length: 4.85,
    hoodCamY: 1.85,
    hoodCamZ: 0.55,
    chaseDist: 8.6,
    chaseHeight: 3.6,
  },
  gwagon: {
    id: 'gwagon',
    name: 'G-Wagon G63',
    maxSpeed: 51,        // ~185 km/h
    accel: 30,
    brake: 42,
    steerRate: 2.3,
    grip: 6.8,
    driftGrip: 2.2,
    clearance: 0.66,
    width: 2.3,
    length: 4.95,
    hoodCamY: 1.92,
    hoodCamZ: 0.5,
    chaseDist: 8.8,
    chaseHeight: 3.7,
  },
  rolls: {
    id: 'rolls',
    name: 'Rolls-Royce Phantom',
    maxSpeed: 57,        // ~205 km/h
    accel: 31,
    brake: 44,
    steerRate: 2.1,
    grip: 7.4,
    driftGrip: 2.4,
    clearance: 0.44,
    width: 2.25,
    length: 5.8,
    hoodCamY: 1.48,
    hoodCamZ: 0.85,
    chaseDist: 9.6,
    chaseHeight: 3.3,
  },
  lambo: {
    id: 'lambo',
    name: 'Lamborghini Aventador',
    maxSpeed: 72,        // ~260 km/h
    accel: 44,
    brake: 56,
    steerRate: 2.65,
    grip: 8.6,
    driftGrip: 2.5,
    clearance: 0.19,     // Sparks on village speed breakers!
    width: 2.28,
    length: 4.9,
    hoodCamY: 1.12,
    hoodCamZ: 0.65,
    chaseDist: 8.3,
    chaseHeight: 2.85,
  },
};

function makeWheel(radius, width, rimColor = 0xd4d4d8, centerCapColor = 0xf59e0b) {
  const pivot = new THREE.Group();
  const wheelSpin = new THREE.Group();

  const tireMat = new THREE.MeshStandardMaterial({ color: 0x16181c, roughness: 0.88, metalness: 0.05 });
  const rimMat = new THREE.MeshStandardMaterial({ color: rimColor, roughness: 0.22, metalness: 0.9 });
  const capMat = new THREE.MeshStandardMaterial({ color: centerCapColor, roughness: 0.25, metalness: 0.85 });

  const tire = new THREE.Mesh(new THREE.CylinderGeometry(radius, radius, width, 18), tireMat);
  tire.rotation.z = Math.PI / 2;
  tire.castShadow = true;
  wheelSpin.add(tire);

  const rim = new THREE.Mesh(new THREE.CylinderGeometry(radius * 0.62, radius * 0.62, width + 0.03, 12), rimMat);
  rim.rotation.z = Math.PI / 2;
  wheelSpin.add(rim);

  // Spokes so rotation is visually crisp
  for (let i = 0; i < 5; i++) {
    const spoke = new THREE.Mesh(new THREE.BoxGeometry(width + 0.05, radius * 1.05, 0.065), capMat);
    spoke.rotation.x = (i * Math.PI) / 5;
    wheelSpin.add(spoke);
  }

  const hub = new THREE.Mesh(new THREE.CylinderGeometry(radius * 0.18, radius * 0.18, width + 0.06, 10), capMat);
  hub.rotation.z = Math.PI / 2;
  wheelSpin.add(hub);

  pivot.add(wheelSpin);
  return { pivot, wheelSpin };
}

function addHeadAndBrakeLights(root, frontZ, rearZ, widthX, yHead, yBrake, headlightMeshes, brakeLightMeshes) {
  const headMat = new THREE.MeshStandardMaterial({
    color: 0xfffbeb,
    emissive: 0xfef3c7,
    emissiveIntensity: 0.85,
    roughness: 0.15,
  });
  const brakeMat = new THREE.MeshStandardMaterial({
    color: 0x991b1b,
    emissive: 0xef4444,
    emissiveIntensity: 0.55,
    roughness: 0.2,
  });

  [-widthX, widthX].forEach((x) => {
    const hl = new THREE.Mesh(new THREE.BoxGeometry(0.42, 0.19, 0.08), headMat);
    hl.position.set(x, yHead, frontZ);
    root.add(hl);
    headlightMeshes.push(hl);

    const bl = new THREE.Mesh(new THREE.BoxGeometry(0.46, 0.18, 0.08), brakeMat);
    bl.position.set(x, yBrake, rearZ);
    root.add(bl);
    brakeLightMeshes.push(bl);
  });

  // Soft forward headlight glow cones (intensified in Golden Hour / Night)
  const beamMat = new THREE.MeshBasicMaterial({
    color: 0xfef08a,
    transparent: true,
    opacity: 0.0,
    depthWrite: false,
    side: THREE.DoubleSide,
  });
  const beams = [];
  [-widthX, widthX].forEach((x) => {
    const cone = new THREE.Mesh(new THREE.ConeGeometry(1.4, 9.5, 10, 1, true), beamMat);
    cone.rotation.x = -Math.PI / 2;
    cone.position.set(x, yHead - 0.15, frontZ + 4.8);
    root.add(cone);
    beams.push(cone);
  });
  return { headMat, brakeMat, beamMat, beams };
}

export function buildVehicleModel(vehicleId) {
  const root = new THREE.Group();
  const bodyGroup = new THREE.Group();
  root.add(bodyGroup);

  const frontPivots = [];
  const allSpins = [];
  const headlightMeshes = [];
  const brakeLightMeshes = [];
  let underglowMat = null;
  let trunkPropAnchor = new THREE.Group();
  bodyGroup.add(trunkPropAnchor);

  const glassMat = new THREE.MeshStandardMaterial({
    color: 0x111827,
    roughness: 0.08,
    metalness: 0.92,
  });
  const chromeMat = new THREE.MeshStandardMaterial({
    color: 0xe5e7eb,
    roughness: 0.12,
    metalness: 0.96,
  });
  const goldMat = new THREE.MeshStandardMaterial({
    color: 0xf59e0b,
    roughness: 0.22,
    metalness: 0.88,
  });
  const darkTrimMat = new THREE.MeshStandardMaterial({
    color: 0x181a1f,
    roughness: 0.72,
    metalness: 0.2,
  });

  let lightRig;

  if (vehicleId === 'scorpio') {
    // 1. Black Mahindra Scorpio-N / Thar (Desi Vidhayak Edition)
    const paint = new THREE.MeshStandardMaterial({ color: 0x0f1115, roughness: 0.2, metalness: 0.85 });

    // High-clearance chassis
    const lower = new THREE.Mesh(new THREE.BoxGeometry(2.15, 0.72, 4.65), paint);
    lower.position.set(0, 0.96, 0);
    lower.castShadow = true;
    lower.receiveShadow = true;
    bodyGroup.add(lower);

    // Tall SUV cabin
    const cabin = new THREE.Mesh(new THREE.BoxGeometry(1.98, 0.82, 2.75), paint);
    cabin.position.set(0, 1.72, -0.35);
    cabin.castShadow = true;
    bodyGroup.add(cabin);

    // Tinted windshield & windows
    const windshield = new THREE.Mesh(new THREE.BoxGeometry(1.88, 0.66, 2.8), glassMat);
    windshield.position.set(0, 1.72, -0.35);
    bodyGroup.add(windshield);

    const sideGlass = new THREE.Mesh(new THREE.BoxGeometry(2.02, 0.56, 2.35), glassMat);
    sideGlass.position.set(0, 1.72, -0.35);
    bodyGroup.add(sideGlass);

    // Signature 6-slat chrome grille & heavy bullbar
    for (let i = -2; i <= 2; i++) {
      const slat = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.34, 0.08), chromeMat);
      slat.position.set(i * 0.22, 1.0, 2.34);
      bodyGroup.add(slat);
    }
    const bullbar = new THREE.Mesh(new THREE.BoxGeometry(1.85, 0.14, 0.2), goldMat);
    bullbar.position.set(0, 0.66, 2.42);
    bodyGroup.add(bullbar);

    // Roof LED Vidhayak light bar + roof rails
    const roofBar = new THREE.Mesh(new THREE.BoxGeometry(1.42, 0.11, 0.14), darkTrimMat);
    roofBar.position.set(0, 2.18, 0.86);
    bodyGroup.add(roofBar);
    const roofLed = new THREE.Mesh(
      new THREE.BoxGeometry(1.3, 0.07, 0.06),
      new THREE.MeshStandardMaterial({ color: 0xfef08a, emissive: 0xf59e0b, emissiveIntensity: 1.5 }),
    );
    roofLed.position.set(0, 2.18, 0.93);
    bodyGroup.add(roofLed);

    // Tailgate spare wheel
    const spare = new THREE.Mesh(new THREE.CylinderGeometry(0.42, 0.42, 0.24, 14), darkTrimMat);
    spare.rotation.x = Math.PI / 2;
    spare.position.set(0, 1.12, -2.42);
    bodyGroup.add(spare);

    // Side footboards
    [-1.12, 1.12].forEach((x) => {
      const step = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.08, 2.3), chromeMat);
      step.position.set(x, 0.52, -0.1);
      bodyGroup.add(step);
    });

    lightRig = addHeadAndBrakeLights(bodyGroup, 2.35, -2.35, 0.74, 1.04, 1.12, headlightMeshes, brakeLightMeshes);
    trunkPropAnchor.position.set(0, 2.18, -0.4);

    // 4 chunky SUV wheels
    const wheelCoords = [
      [-1.08, 0.48, 1.48, true],
      [1.08, 0.48, 1.48, true],
      [-1.08, 0.48, -1.45, false],
      [1.08, 0.48, -1.45, false],
    ];
    wheelCoords.forEach(([x, y, z, isFront]) => {
      const w = makeWheel(0.48, 0.34, 0xd4d4d8, 0xf59e0b);
      w.pivot.position.set(x, y, z);
      root.add(w.pivot);
      if (isFront) frontPivots.push(w.pivot);
      allSpins.push(w.wheelSpin);
    });
  } else if (vehicleId === 'gwagon') {
    // 2. Mercedes-Benz G-Wagon (G63 AMG)
    const paint = new THREE.MeshStandardMaterial({ color: 0x1e242b, roughness: 0.16, metalness: 0.88 });

    const lower = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.78, 4.78), paint);
    lower.position.set(0, 0.98, 0);
    lower.castShadow = true;
    bodyGroup.add(lower);

    // Iconic ultra-boxy upright cabin
    const cabin = new THREE.Mesh(new THREE.BoxGeometry(2.06, 0.9, 3.05), paint);
    cabin.position.set(0, 1.8, -0.45);
    cabin.castShadow = true;
    bodyGroup.add(cabin);

    const glassWrap = new THREE.Mesh(new THREE.BoxGeometry(2.1, 0.62, 2.86), glassMat);
    glassWrap.position.set(0, 1.82, -0.44);
    bodyGroup.add(glassWrap);

    const frontGlass = new THREE.Mesh(new THREE.BoxGeometry(1.9, 0.66, 0.12), glassMat);
    frontGlass.position.set(0, 1.82, 1.06);
    frontGlass.rotation.x = -0.14;
    bodyGroup.add(frontGlass);

    // AMG Panamericana vertical grille + star ring
    for (let i = -4; i <= 4; i++) {
      const bar = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.36, 0.08), chromeMat);
      bar.position.set(i * 0.14, 1.05, 2.41);
      bodyGroup.add(bar);
    }
    const badge = new THREE.Mesh(new THREE.TorusGeometry(0.15, 0.03, 8, 16), chromeMat);
    badge.position.set(0, 1.05, 2.45);
    bodyGroup.add(badge);

    // Hood indicator pods
    [-0.95, 0.95].forEach((x) => {
      const pod = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.1, 0.24), goldMat);
      pod.position.set(x, 1.42, 1.95);
      bodyGroup.add(pod);
    });

    // Twin AMG Side Exhausts
    [-1.16, 1.16].forEach((x) => {
      for (const dz of [-0.65, -0.92]) {
        const pipe = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.25, 10), chromeMat);
        pipe.rotation.z = Math.PI / 2;
        pipe.position.set(x, 0.45, dz);
        bodyGroup.add(pipe);
      }
    });

    // Rear chrome wheel cover
    const cover = new THREE.Mesh(new THREE.CylinderGeometry(0.46, 0.46, 0.26, 18), chromeMat);
    cover.rotation.x = Math.PI / 2;
    cover.position.set(0, 1.18, -2.52);
    bodyGroup.add(cover);

    lightRig = addHeadAndBrakeLights(bodyGroup, 2.42, -2.4, 0.78, 1.05, 1.08, headlightMeshes, brakeLightMeshes);
    trunkPropAnchor.position.set(0, 2.28, -0.45);

    [
      [-1.12, 0.46, 1.54, true],
      [1.12, 0.46, 1.54, true],
      [-1.12, 0.46, -1.5, false],
      [1.12, 0.46, -1.5, false],
    ].forEach(([x, y, z, isFront]) => {
      const w = makeWheel(0.46, 0.34, 0x27272a, 0xe5e7eb);
      w.pivot.position.set(x, y, z);
      root.add(w.pivot);
      if (isFront) frontPivots.push(w.pivot);
      allSpins.push(w.wheelSpin);
    });
  } else if (vehicleId === 'rolls') {
    // 3. Rolls-Royce Phantom
    const bodyPaint = new THREE.MeshStandardMaterial({ color: 0x0b0f19, roughness: 0.14, metalness: 0.9 });
    const hoodPaint = new THREE.MeshStandardMaterial({ color: 0xd4af37, roughness: 0.18, metalness: 0.92 });

    // Long stately sedan hull
    const lower = new THREE.Mesh(new THREE.BoxGeometry(2.18, 0.66, 5.65), bodyPaint);
    lower.position.set(0, 0.76, 0);
    lower.castShadow = true;
    bodyGroup.add(lower);

    // Two-tone Champagne Gold upper hood & roof accent
    const hoodTop = new THREE.Mesh(new THREE.BoxGeometry(2.04, 0.1, 2.15), hoodPaint);
    hoodTop.position.set(0, 1.12, 1.7);
    bodyGroup.add(hoodTop);

    const cabin = new THREE.Mesh(new THREE.BoxGeometry(1.94, 0.68, 2.65), bodyPaint);
    cabin.position.set(0, 1.4, -0.42);
    cabin.castShadow = true;
    bodyGroup.add(cabin);

    const glass = new THREE.Mesh(new THREE.BoxGeometry(1.98, 0.48, 2.45), glassMat);
    glass.position.set(0, 1.4, -0.42);
    bodyGroup.add(glass);

    // Glowing Illuminated Pantheon Grille
    const grillGlow = new THREE.MeshStandardMaterial({
      color: 0xfef3c7,
      emissive: 0xf59e0b,
      emissiveIntensity: 0.95,
      metalness: 0.85,
      roughness: 0.15,
    });
    const grillFrame = new THREE.Mesh(new THREE.BoxGeometry(0.96, 0.56, 0.12), chromeMat);
    grillFrame.position.set(0, 0.82, 2.84);
    bodyGroup.add(grillFrame);
    for (let i = -4; i <= 4; i++) {
      const slat = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.48, 0.14), grillGlow);
      slat.position.set(i * 0.095, 0.82, 2.86);
      bodyGroup.add(slat);
    }

    // Spirit of Ecstasy Hood Ornament
    const ped = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.07, 0.08, 8), goldMat);
    ped.position.set(0, 1.2, 2.74);
    bodyGroup.add(ped);
    const spirit = new THREE.Mesh(new THREE.ConeGeometry(0.08, 0.26, 6), goldMat);
    spirit.position.set(0, 1.34, 2.74);
    spirit.rotation.x = 0.25;
    bodyGroup.add(spirit);
    const wings = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.06, 0.08), goldMat);
    wings.position.set(0, 1.32, 2.68);
    wings.rotation.x = -0.4;
    bodyGroup.add(wings);

    lightRig = addHeadAndBrakeLights(bodyGroup, 2.84, -2.84, 0.78, 0.86, 0.86, headlightMeshes, brakeLightMeshes);
    trunkPropAnchor.position.set(0, 1.15, -2.2);

    [
      [-1.08, 0.42, 1.85, true],
      [1.08, 0.42, 1.85, true],
      [-1.08, 0.42, -1.8, false],
      [1.08, 0.42, -1.8, false],
    ].forEach(([x, y, z, isFront]) => {
      const w = makeWheel(0.42, 0.3, 0xf8fafc, 0xd4af37);
      w.pivot.position.set(x, y, z);
      root.add(w.pivot);
      if (isFront) frontPivots.push(w.pivot);
      allSpins.push(w.wheelSpin);
    });
  } else {
    // 4. Lamborghini Aventador (Low-Slung Supercar + Neon Underglow!)
    const paint = new THREE.MeshStandardMaterial({ color: 0xea580c, roughness: 0.14, metalness: 0.82 });

    // Ultra-low wedge chassis
    const lower = new THREE.Mesh(new THREE.BoxGeometry(2.24, 0.46, 4.72), paint);
    lower.position.set(0, 0.48, 0);
    lower.castShadow = true;
    bodyGroup.add(lower);

    // Sloped wedge nose
    const nose = new THREE.Mesh(new THREE.BoxGeometry(2.05, 0.28, 1.5), paint);
    nose.position.set(0, 0.52, 1.75);
    nose.rotation.x = 0.18;
    bodyGroup.add(nose);

    // Fighter-jet angular canopy
    const canopy = new THREE.Mesh(new THREE.BoxGeometry(1.66, 0.44, 2.1), glassMat);
    canopy.position.set(0, 0.86, 0.05);
    bodyGroup.add(canopy);

    // Scissor-door side intake scoops
    [-1.14, 1.14].forEach((x) => {
      const scoop = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.34, 1.35), darkTrimMat);
      scoop.position.set(x, 0.56, -0.45);
      bodyGroup.add(scoop);
    });

    // Rear carbon spoiler wing
    const wing = new THREE.Mesh(new THREE.BoxGeometry(2.18, 0.08, 0.34), darkTrimMat);
    wing.position.set(0, 0.96, -2.18);
    bodyGroup.add(wing);
    [-0.78, 0.78].forEach((x) => {
      const strut = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.28, 0.14), darkTrimMat);
      strut.position.set(x, 0.8, -2.18);
      bodyGroup.add(strut);
    });

    // Neon Underglow plane!
    underglowMat = new THREE.MeshBasicMaterial({
      color: 0x00f5ff,
      transparent: true,
      opacity: 0.58,
      depthWrite: false,
      side: THREE.DoubleSide,
    });
    const glowPlane = new THREE.Mesh(new THREE.PlaneGeometry(2.8, 5.3), underglowMat);
    glowPlane.rotation.x = -Math.PI / 2;
    glowPlane.position.set(0, 0.06, 0);
    root.add(glowPlane);

    lightRig = addHeadAndBrakeLights(bodyGroup, 2.4, -2.38, 0.78, 0.56, 0.62, headlightMeshes, brakeLightMeshes);
    trunkPropAnchor.position.set(0, 0.86, -1.35);

    [
      [-1.08, 0.36, 1.48, true],
      [1.08, 0.36, 1.48, true],
      [-1.12, 0.38, -1.42, false],
      [1.12, 0.38, -1.42, false],
    ].forEach(([x, y, z, isFront]) => {
      const w = makeWheel(0.37, 0.32, 0x18181b, 0xf59e0b);
      w.pivot.position.set(x, y, z);
      root.add(w.pivot);
      if (isFront) frontPivots.push(w.pivot);
      allSpins.push(w.wheelSpin);
    });
  }

  return {
    root,
    bodyGroup,
    frontPivots,
    allSpins,
    headlightMeshes,
    brakeLightMeshes,
    lightRig,
    underglowMat,
    trunkPropAnchor,
    tuning: VEHICLE_TUNING[vehicleId] || VEHICLE_TUNING.scorpio,
  };
}

// 3D Desi Hero (Kurta-Gamcha & Aviator Sunglasses) for Paidal Mode
export function buildDesiHeroModel() {
  const root = new THREE.Group();

  const skinMat = new THREE.MeshStandardMaterial({ color: 0xc68642, roughness: 0.65 });
  const kurtaMat = new THREE.MeshStandardMaterial({ color: 0xfef3c7, roughness: 0.75 });
  const pajamaMat = new THREE.MeshStandardMaterial({ color: 0xf8fafc, roughness: 0.8 });
  const gamchaMat = new THREE.MeshStandardMaterial({ color: 0xdc2626, roughness: 0.7 });
  const hairMat = new THREE.MeshStandardMaterial({ color: 0x111827, roughness: 0.7 });
  const shadesMat = new THREE.MeshStandardMaterial({ color: 0x09090b, roughness: 0.1, metalness: 0.9 });
  const shoeMat = new THREE.MeshStandardMaterial({ color: 0x78350f, roughness: 0.6 });

  // Torso (Kurta)
  const torso = new THREE.Mesh(new THREE.BoxGeometry(0.56, 0.82, 0.32), kurtaMat);
  torso.position.set(0, 1.12, 0);
  torso.castShadow = true;
  root.add(torso);

  // Draped Desi Red Gamcha across shoulder
  const gamchaFront = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.76, 0.36), gamchaMat);
  gamchaFront.position.set(0.18, 1.15, 0);
  gamchaFront.rotation.z = 0.14;
  root.add(gamchaFront);

  // Head + Hair + Sunglasses + Mustache
  const head = new THREE.Mesh(new THREE.BoxGeometry(0.36, 0.38, 0.36), skinMat);
  head.position.set(0, 1.75, 0);
  head.castShadow = true;
  root.add(head);

  const hair = new THREE.Mesh(new THREE.BoxGeometry(0.38, 0.14, 0.38), hairMat);
  hair.position.set(0, 1.94, -0.02);
  root.add(hair);

  const shades = new THREE.Mesh(new THREE.BoxGeometry(0.34, 0.1, 0.08), shadesMat);
  shades.position.set(0, 1.78, 0.18);
  root.add(shades);

  const mustache = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.04, 0.05), hairMat);
  mustache.position.set(0, 1.67, 0.19);
  root.add(mustache);

  // Arms
  const leftArmPivot = new THREE.Group();
  leftArmPivot.position.set(-0.37, 1.46, 0);
  const leftArm = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.62, 0.16), kurtaMat);
  leftArm.position.set(0, -0.26, 0);
  leftArmPivot.add(leftArm);
  root.add(leftArmPivot);

  const rightArmPivot = new THREE.Group();
  rightArmPivot.position.set(0.37, 1.46, 0);
  const rightArm = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.62, 0.16), kurtaMat);
  rightArm.position.set(0, -0.26, 0);
  rightArmPivot.add(rightArm);
  root.add(rightArmPivot);

  // Legs
  const leftLegPivot = new THREE.Group();
  leftLegPivot.position.set(-0.14, 0.72, 0);
  const leftLeg = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.66, 0.2), pajamaMat);
  leftLeg.position.set(0, -0.32, 0);
  const leftShoe = new THREE.Mesh(new THREE.BoxGeometry(0.21, 0.1, 0.28), shoeMat);
  leftShoe.position.set(0, -0.66, 0.04);
  leftLegPivot.add(leftLeg, leftShoe);
  root.add(leftLegPivot);

  const rightLegPivot = new THREE.Group();
  rightLegPivot.position.set(0.14, 0.72, 0);
  const rightLeg = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.66, 0.2), pajamaMat);
  rightLeg.position.set(0, -0.32, 0);
  const rightShoe = new THREE.Mesh(new THREE.BoxGeometry(0.21, 0.1, 0.28), shoeMat);
  rightShoe.position.set(0, -0.66, 0.04);
  rightLegPivot.add(rightLeg, rightShoe);
  root.add(rightLegPivot);

  return {
    root,
    leftArmPivot,
    rightArmPivot,
    leftLegPivot,
    rightLegPivot,
    walkPhase: 0,
    clapTimer: 0,
  };
}
