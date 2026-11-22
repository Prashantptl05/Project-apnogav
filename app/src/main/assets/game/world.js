import * as THREE from 'three';

export const CENTER_LAT = 23.0035;
export const CENTER_LNG = 78.2215;
export const GEOFENCE_RADIUS = 1500; // Strictly 1.5 km radius around Gram Johar Barha!

// Landmark coordinates (meters relative to Gram Johar Barha Panchayat Chowk at 0,0)
export const LANDMARKS = {
  panchayatChowk: { x: 0, z: 0, label: 'Gram Panchayat Chowk' },
  chaiTapri: { x: 52, z: -28, label: 'Raju Ki Chai Tapri' },
  chachaKhet: { x: -420, z: -310, label: 'Chacha Ka Khet' },
  sarpanchHouse: { x: 245, z: -165, label: 'Sarpanch Ji Ki Haveli' },
  chanaField: { x: -210, z: 240, label: 'Taaza Chana Khet' },
  gaonNukkad: { x: 460, z: 195, label: 'Gaon Ka Nukkad' },
  punctureShop: { x: -340, z: 120, label: 'Puncture Wala Jugaad' },
  bhainsRoadSpot: { x: 185, z: 0, label: 'Bareli Main Road' },
  gullyStart: { x: -115, z: -135, label: 'Patli Purani Gully' },
  gullyExit: { x: -115, z: -35, label: 'Gully Exit' },
};

// Procedural Johar Barha road network (modeled on Bareli -> Johar Barha -> Amrawad/Simrod roads)
export const PROCEDURAL_ROADS = [
  // Main Bareli–Johar Barha East-West Highway (paved)
  { type: 'paved', width: 14, points: [[-1380, 0], [-600, 0], [0, 0], [580, 0], [1380, 35]] },
  // Amrawad North-South Feeder Road (paved)
  { type: 'paved', width: 12, points: [[0, -1350], [0, -450], [0, 0], [15, 480], [60, 1320]] },
  // Village Ring Road for Doston Ke Saath Geedi (paved/semi-paved)
  {
    type: 'paved',
    width: 11,
    points: [
      [-450, -320], [0, -450], [440, -240], [490, 210],
      [15, 480], [-390, 260], [-450, -320],
    ],
  },
  // Kachha village lanes & field trails (dusty ochre)
  { type: 'kachha', width: 8, points: [[-210, 0], [-210, 240], [-390, 260]] },
  { type: 'kachha', width: 8, points: [[245, 0], [245, -165], [440, -240]] },
  { type: 'kachha', width: 7.5, points: [[-340, 0], [-340, 120], [-390, 260]] },
  { type: 'kachha', width: 7, points: [[52, 0], [52, -160], [245, -165]] },
  // Narrow walled gully for Mission 6 & Paidal walking
  { type: 'gully', width: 5.2, points: [[-115, -155], [-115, -30], [-115, 0]] },
];

export const SPEED_BREAKERS = [
  { x: 95, z: 0, rotY: Math.PI / 2 },
  { x: 295, z: 0, rotY: Math.PI / 2 },
  { x: -165, z: 0, rotY: Math.PI / 2 },
  { x: 0, z: -135, rotY: 0 },
  { x: 0, z: 160, rotY: 0 },
  { x: 465, z: 65, rotY: 0 },
  { x: -240, z: -380, rotY: Math.PI / 2 },
];

function makeSignTexture(line1, line2, bg = '#166534', border = '#f59e0b', fg = '#ffffff') {
  const c = document.createElement('canvas');
  c.width = 512;
  c.height = 192;
  const ctx = c.getContext('2d');
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, 512, 192);
  ctx.strokeStyle = border;
  ctx.lineWidth = 10;
  ctx.strokeRect(8, 8, 496, 176);
  ctx.fillStyle = fg;
  ctx.font = 'bold 38px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText(line1, 256, 82);
  ctx.fillStyle = '#fde68a';
  ctx.font = '600 24px sans-serif';
  ctx.fillText(line2, 256, 134);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

function addRoadRibbon(group, points, width, mat, yOffset = 0.04) {
  for (let i = 0; i < points.length - 1; i++) {
    const [x1, z1] = points[i];
    const [x2, z2] = points[i + 1];
    const dx = x2 - x1;
    const dz = z2 - z1;
    const len = Math.hypot(dx, dz);
    if (len < 0.5) continue;

    const seg = new THREE.Mesh(new THREE.PlaneGeometry(width, len + width * 0.45), mat);
    seg.rotation.x = -Math.PI / 2;
    seg.rotation.z = -Math.atan2(dx, dz);
    seg.position.set((x1 + x2) / 2, yOffset, (z1 + z2) / 2);
    seg.receiveShadow = true;
    group.add(seg);
  }
}

export function buildJoharBarhaWorld(scene) {
  const worldGroup = new THREE.Group();
  scene.add(worldGroup);

  const colliders = []; // { x, z, hw, hd, isGullyWall }
  const nightLights = [];

  // 1. Lighting & Sky (Default: Bright Sunny Rural Madhya Pradesh Afternoon)
  const hemiLight = new THREE.HemisphereLight(0x87cefa, 0x7c6a43, 1.05);
  scene.add(hemiLight);

  const sunLight = new THREE.DirectionalLight(0xfff5dc, 1.95);
  sunLight.position.set(140, 210, 95);
  sunLight.castShadow = true;
  sunLight.shadow.mapSize.width = 1024;
  sunLight.shadow.mapSize.height = 1024;
  sunLight.shadow.camera.near = 20;
  sunLight.shadow.camera.far = 480;
  const d = 95;
  sunLight.shadow.camera.left = -d;
  sunLight.shadow.camera.right = d;
  sunLight.shadow.camera.top = d;
  sunLight.shadow.camera.bottom = -d;
  sunLight.shadow.bias = -0.0008;
  scene.add(sunLight);
  scene.add(sunLight.target);

  // Sun disc in sky
  const sunDiscMat = new THREE.MeshBasicMaterial({ color: 0xfff7c2 });
  const sunDisc = new THREE.Mesh(new THREE.SphereGeometry(42, 16, 12), sunDiscMat);
  sunDisc.position.set(900, 1100, 580);
  scene.add(sunDisc);

  // Fluffy afternoon clouds (instanced)
  const cloudMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.85 });
  const cloudGeo = new THREE.SphereGeometry(28, 10, 8);
  const cloudInst = new THREE.InstancedMesh(cloudGeo, cloudMat, 45);
  const dummy = new THREE.Object3D();
  for (let i = 0; i < 45; i++) {
    const a = (i / 45) * Math.PI * 2 + (i % 3) * 0.2;
    const r = 350 + (i * 97) % 1400;
    dummy.position.set(Math.cos(a) * r, 230 + (i % 5) * 32, Math.sin(a) * r);
    dummy.scale.set(2.4 + (i % 3) * 0.7, 0.58, 1.5 + (i % 2) * 0.6);
    dummy.updateMatrix();
    cloudInst.setMatrixAt(i, dummy.matrix);
  }
  scene.add(cloudInst);

  // 2. Ground Plane (Fertile MP plains with subtle grid-free earthy texture)
  const groundCanvas = document.createElement('canvas');
  groundCanvas.width = 256;
  groundCanvas.height = 256;
  const gctx = groundCanvas.getContext('2d');
  gctx.fillStyle = '#5c7c32';
  gctx.fillRect(0, 0, 256, 256);
  for (let i = 0; i < 1200; i++) {
    gctx.fillStyle = i % 3 === 0 ? '#4d6c28' : i % 3 === 1 ? '#6e8c3b' : '#857144';
    gctx.fillRect((i * 47) % 256, (i * 83) % 256, 5, 5);
  }
  const groundTex = new THREE.CanvasTexture(groundCanvas);
  groundTex.wrapS = groundTex.wrapT = THREE.RepeatWrapping;
  groundTex.repeat.set(95, 95);
  groundTex.colorSpace = THREE.SRGBColorSpace;

  const ground = new THREE.Mesh(
    new THREE.CircleGeometry(GEOFENCE_RADIUS + 420, 64),
    new THREE.MeshStandardMaterial({ map: groundTex, roughness: 0.96, metalness: 0.02 }),
  );
  ground.rotation.x = -Math.PI / 2;
  ground.receiveShadow = true;
  worldGroup.add(ground);

  // 3. Roads (Paved Asphalt + Dusty Kachha Lanes)
  const asphaltCanvas = document.createElement('canvas');
  asphaltCanvas.width = 128;
  asphaltCanvas.height = 128;
  const actx = asphaltCanvas.getContext('2d');
  actx.fillStyle = '#333840';
  actx.fillRect(0, 0, 128, 128);
  actx.fillStyle = '#e5e7eb';
  actx.fillRect(61, 8, 6, 46);
  actx.fillRect(61, 74, 6, 46);
  actx.fillStyle = '#f59e0b';
  actx.fillRect(4, 0, 4, 128);
  actx.fillRect(120, 0, 4, 128);
  const asphaltTex = new THREE.CanvasTexture(asphaltCanvas);
  asphaltTex.wrapS = asphaltTex.wrapT = THREE.RepeatWrapping;
  asphaltTex.repeat.set(1, 24);
  asphaltTex.colorSpace = THREE.SRGBColorSpace;

  const pavedMat = new THREE.MeshStandardMaterial({ map: asphaltTex, roughness: 0.85 });
  const kachhaMat = new THREE.MeshStandardMaterial({ color: 0xb98d59, roughness: 0.98 });
  const gullyMat = new THREE.MeshStandardMaterial({ color: 0xa67c4e, roughness: 0.96 });

  const roadGroup = new THREE.Group();
  worldGroup.add(roadGroup);

  PROCEDURAL_ROADS.forEach((road) => {
    const mat = road.type === 'paved' ? pavedMat : road.type === 'gully' ? gullyMat : kachhaMat;
    addRoadRibbon(roadGroup, road.points, road.width, mat, road.type === 'paved' ? 0.05 : 0.035);
  });

  // Central Gram Panchayat Chowk Roundabout Plaza
  const chowkRoad = new THREE.Mesh(
    new THREE.CylinderGeometry(24, 24, 0.08, 32),
    new THREE.MeshStandardMaterial({ color: 0x3b4048, roughness: 0.88 }),
  );
  chowkRoad.position.set(0, 0.03, 0);
  chowkRoad.receiveShadow = true;
  worldGroup.add(chowkRoad);

  const chowkIsland = new THREE.Mesh(
    new THREE.CylinderGeometry(8.5, 9.2, 0.65, 24),
    new THREE.MeshStandardMaterial({ color: 0xd97706, roughness: 0.78 }),
  );
  chowkIsland.position.set(0, 0.34, 0);
  chowkIsland.castShadow = true;
  worldGroup.add(chowkIsland);
  colliders.push({ x: 0, z: 0, hw: 8.6, hd: 8.6, isGullyWall: false });

  // Central Banyan Tree + Flagpole + Village Signboard at Chowk
  const pole = new THREE.Mesh(
    new THREE.CylinderGeometry(0.12, 0.16, 10, 8),
    new THREE.MeshStandardMaterial({ color: 0xe5e7eb, metalness: 0.7, roughness: 0.3 }),
  );
  pole.position.set(0, 5.2, 0);
  worldGroup.add(pole);

  // Tricolor bunting / saffron banner
  const flag = new THREE.Mesh(
    new THREE.BoxGeometry(2.6, 1.2, 0.06),
    new THREE.MeshStandardMaterial({ color: 0xf97316, roughness: 0.6 }),
  );
  flag.position.set(1.3, 9.2, 0);
  worldGroup.add(flag);

  const villageSign = new THREE.Mesh(
    new THREE.BoxGeometry(8.6, 3.2, 0.25),
    new THREE.MeshStandardMaterial({
      map: makeSignTexture('GRAM JOHAR BARHA', 'TEHSIL BARELI • DIST. RAISEN (MP)'),
      roughness: 0.45,
    }),
  );
  villageSign.position.set(0, 3.4, -18);
  villageSign.castShadow = true;
  worldGroup.add(villageSign);
  [-3.8, 3.8].forEach((px) => {
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.14, 0.14, 3.5, 8), new THREE.MeshStandardMaterial({ color: 0x475569 }));
    leg.position.set(px, 1.75, -18);
    worldGroup.add(leg);
  });

  // 4. Speed Breakers (Yellow & Black Desi Rumble Strips)
  const breakerGeo = new THREE.BoxGeometry(13.2, 0.24, 1.35);
  const breakerCanvas = document.createElement('canvas');
  breakerCanvas.width = 128;
  breakerCanvas.height = 32;
  const bctx = breakerCanvas.getContext('2d');
  for (let i = 0; i < 8; i++) {
    bctx.fillStyle = i % 2 === 0 ? '#facc15' : '#18181b';
    bctx.fillRect(i * 16, 0, 16, 32);
  }
  const breakerTex = new THREE.CanvasTexture(breakerCanvas);
  const breakerMat = new THREE.MeshStandardMaterial({ map: breakerTex, roughness: 0.7 });
  SPEED_BREAKERS.forEach((sb) => {
    const mesh = new THREE.Mesh(breakerGeo, breakerMat);
    mesh.position.set(sb.x, 0.12, sb.z);
    mesh.rotation.y = sb.rotY;
    mesh.receiveShadow = true;
    worldGroup.add(mesh);
  });

  // 5. Key Village Landmarks
  // 5a. Raju Ki Chai Tapri
  const tapri = new THREE.Group();
  tapri.position.set(LANDMARKS.chaiTapri.x, 0, LANDMARKS.chaiTapri.z);
  const tapriCounter = new THREE.Mesh(
    new THREE.BoxGeometry(5.2, 1.3, 2.6),
    new THREE.MeshStandardMaterial({ color: 0x92400e, roughness: 0.85 }),
  );
  tapriCounter.position.set(0, 0.65, 0);
  tapriCounter.castShadow = true;
  tapri.add(tapriCounter);

  const tapriRoof = new THREE.Mesh(
    new THREE.BoxGeometry(6.4, 0.18, 4.2),
    new THREE.MeshStandardMaterial({ color: 0x2563eb, roughness: 0.5 }),
  );
  tapriRoof.position.set(0, 3.2, 0);
  tapriRoof.rotation.x = 0.12;
  tapriRoof.castShadow = true;
  tapri.add(tapriRoof);

  [[-2.5, -1.3], [2.5, -1.3], [-2.5, 1.3], [2.5, 1.3]].forEach(([px, pz]) => {
    const post = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.1, 3.2, 6), new THREE.MeshStandardMaterial({ color: 0x713f12 }));
    post.position.set(px, 1.6, pz);
    tapri.add(post);
  });

  const chaiSign = new THREE.Mesh(
    new THREE.BoxGeometry(4.4, 1.2, 0.15),
    new THREE.MeshStandardMaterial({ map: makeSignTexture('RAJU CHAI TAPRI', 'KULHAD ADRAK CHAI • ₹10', '#7c2d12', '#facc15') }),
  );
  chaiSign.position.set(0, 3.7, 1.8);
  tapri.add(chaiSign);

  const tapriBulb = new THREE.PointLight(0xfbbf24, 0, 26);
  tapriBulb.position.set(0, 2.9, 0.8);
  tapri.add(tapriBulb);
  nightLights.push(tapriBulb);

  worldGroup.add(tapri);
  colliders.push({ x: LANDMARKS.chaiTapri.x, z: LANDMARKS.chaiTapri.z, hw: 3.1, hd: 2.0, isGullyWall: false });

  // 5b. Sarpanch Ji Ki Haveli
  const haveli = new THREE.Group();
  haveli.position.set(LANDMARKS.sarpanchHouse.x, 0, LANDMARKS.sarpanchHouse.z - 14);
  const hBase = new THREE.Mesh(
    new THREE.BoxGeometry(16, 5.2, 11),
    new THREE.MeshStandardMaterial({ color: 0xfef3c7, roughness: 0.8 }),
  );
  hBase.position.set(0, 2.6, 0);
  hBase.castShadow = true;
  haveli.add(hBase);
  const hTop = new THREE.Mesh(
    new THREE.BoxGeometry(11, 3.8, 8.5),
    new THREE.MeshStandardMaterial({ color: 0xfde68a, roughness: 0.78 }),
  );
  hTop.position.set(0, 7.1, -0.5);
  hTop.castShadow = true;
  haveli.add(hTop);
  const hTrim = new THREE.Mesh(
    new THREE.BoxGeometry(16.6, 0.5, 11.6),
    new THREE.MeshStandardMaterial({ color: 0xc2410c, roughness: 0.7 }),
  );
  hTrim.position.set(0, 5.3, 0);
  haveli.add(hTrim);
  const hSign = new THREE.Mesh(
    new THREE.BoxGeometry(7.2, 1.8, 0.2),
    new THREE.MeshStandardMaterial({ map: makeSignTexture('SARPANCH HAVELI', 'JOHAR BARHA VIP', '#9a3412', '#facc15') }),
  );
  hSign.position.set(0, 4.4, 5.65);
  haveli.add(hSign);
  worldGroup.add(haveli);
  colliders.push({ x: LANDMARKS.sarpanchHouse.x, z: LANDMARKS.sarpanchHouse.z - 14, hw: 8.5, hd: 6.0, isGullyWall: false });

  // 5c. Puncture Wala Dukaan
  const puncShop = new THREE.Group();
  puncShop.position.set(LANDMARKS.punctureShop.x - 10, 0, LANDMARKS.punctureShop.z);
  const pBox = new THREE.Mesh(
    new THREE.BoxGeometry(6.5, 3.4, 5.0),
    new THREE.MeshStandardMaterial({ color: 0x1d4ed8, roughness: 0.75 }),
  );
  pBox.position.set(0, 1.7, 0);
  pBox.castShadow = true;
  puncShop.add(pBox);
  const pSign = new THREE.Mesh(
    new THREE.BoxGeometry(5.6, 1.5, 0.18),
    new THREE.MeshStandardMaterial({ map: makeSignTexture('PUNCTURE JUGAAD', '24x7 HAWA & TYRE REPAIR', '#1e3a8a', '#facc15') }),
  );
  pSign.position.set(0, 3.8, 2.55);
  puncShop.add(pSign);
  worldGroup.add(puncShop);
  colliders.push({ x: LANDMARKS.punctureShop.x - 10, z: LANDMARKS.punctureShop.z, hw: 3.5, hd: 2.8, isGullyWall: false });

  // 5d. Narrow Walled Gully (Mission 6: Bina Scratch Ke Gully Drive!)
  const wallMat = new THREE.MeshStandardMaterial({ color: 0xd6b588, roughness: 0.9 });
  const roofTrimMat = new THREE.MeshStandardMaterial({ color: 0xb45309, roughness: 0.85 });
  [-119.5, -110.5].forEach((wx) => {
    const wall = new THREE.Mesh(new THREE.BoxGeometry(1.6, 3.6, 118), wallMat);
    wall.position.set(wx, 1.8, -94);
    wall.castShadow = true;
    wall.receiveShadow = true;
    worldGroup.add(wall);
    const cap = new THREE.Mesh(new THREE.BoxGeometry(1.9, 0.35, 118), roofTrimMat);
    cap.position.set(wx, 3.7, -94);
    worldGroup.add(cap);
    colliders.push({ x: wx, z: -94, hw: 0.95, hd: 59, isGullyWall: true });
  });

  // 6. Desi Village Houses (Clustered near lanes, clear of roads)
  const housePalette = [0xfef08a, 0xe0f2fe, 0xfed7aa, 0xf5f5f4, 0xfecdd3, 0xd9f99d];
  const roofMat = new THREE.MeshStandardMaterial({ color: 0xb91c1c, roughness: 0.82 });
  const houseSpots = [
    [-55, -52], [-80, -52], [62, -62], [92, -65], [130, -45], [160, -52],
    [45, 48], [85, 52], [135, 48], [175, 58], [-55, 52], [-95, 56],
    [-145, 55], [-265, -45], [-310, -52], [295, -58], [345, -52], [385, -70],
    [290, 52], [340, 62], [395, 55], [430, 145], [495, 165], [495, 240],
    [-255, 165], [-275, 225], [-160, 175], [-65, 185], [65, 185], [95, 255],
    [65, -240], [115, -265], [-65, -230], [-255, -245], [-320, -280], [190, -255],
  ];

  houseSpots.forEach(([hx, hz], idx) => {
    const w = 8 + (idx % 3) * 2.5;
    const h = 3.6 + (idx % 2) * 1.4;
    const dLen = 7.5 + (idx % 2) * 2;
    const hMat = new THREE.MeshStandardMaterial({ color: housePalette[idx % housePalette.length], roughness: 0.85 });
    const body = new THREE.Mesh(new THREE.BoxGeometry(w, h, dLen), hMat);
    body.position.set(hx, h / 2, hz);
    body.castShadow = true;
    body.receiveShadow = true;
    worldGroup.add(body);

    const roof = new THREE.Mesh(new THREE.ConeGeometry(Math.max(w, dLen) * 0.72, 1.8, 4), roofMat);
    roof.position.set(hx, h + 0.9, hz);
    roof.rotation.y = Math.PI / 4;
    roof.castShadow = true;
    worldGroup.add(roof);

    colliders.push({ x: hx, z: hz, hw: w / 2 + 0.3, hd: dLen / 2 + 0.3, isGullyWall: false });
  });

  // 7. Instanced Banyan & Neem Trees (240 trees in 2 draw calls!)
  const treeCount = 240;
  const trunkInst = new THREE.InstancedMesh(
    new THREE.CylinderGeometry(0.45, 0.75, 4.8, 7),
    new THREE.MeshStandardMaterial({ color: 0x5c4033, roughness: 0.92 }),
    treeCount,
  );
  const canopyInst = new THREE.InstancedMesh(
    new THREE.DodecahedronGeometry(3.6, 1),
    new THREE.MeshStandardMaterial({ color: 0x15803d, roughness: 0.85 }),
    treeCount,
  );
  trunkInst.castShadow = true;
  canopyInst.castShadow = true;

  let placedTrees = 0;
  let seed = 1337;
  const rand = () => {
    seed = (seed * 16807) % 2147483647;
    return (seed - 1) / 2147483646;
  };
  while (placedTrees < treeCount) {
    const angle = rand() * Math.PI * 2;
    const dist = 42 + rand() * (GEOFENCE_RADIUS - 110);
    const tx = Math.cos(angle) * dist;
    const tz = Math.sin(angle) * dist;
    // Keep clear of main cross roads and narrow gully
    if (Math.abs(tx) < 22 || Math.abs(tz) < 22) continue;
    if (Math.abs(tx + 115) < 16 && tz < 10 && tz > -165) continue;

    const sc = 0.78 + rand() * 0.75;
    dummy.position.set(tx, 2.4 * sc, tz);
    dummy.scale.set(sc, sc, sc);
    dummy.rotation.set(0, rand() * Math.PI, 0);
    dummy.updateMatrix();
    trunkInst.setMatrixAt(placedTrees, dummy.matrix);

    dummy.position.set(tx, 5.6 * sc, tz);
    dummy.scale.set(sc * 1.15, sc * 0.9, sc * 1.15);
    dummy.updateMatrix();
    canopyInst.setMatrixAt(placedTrees, dummy.matrix);
    placedTrees++;
  }
  worldGroup.add(trunkInst, canopyInst);

  // 8. Instanced Green Chana & Golden Wheat Agricultural Fields (600 patches in 2 draw calls!)
  const chanaCount = 320;
  const wheatCount = 320;
  const chanaInst = new THREE.InstancedMesh(
    new THREE.BoxGeometry(18, 0.7, 14),
    new THREE.MeshStandardMaterial({ color: 0x22c55e, roughness: 0.95 }),
    chanaCount,
  );
  const wheatInst = new THREE.InstancedMesh(
    new THREE.BoxGeometry(20, 1.05, 16),
    new THREE.MeshStandardMaterial({ color: 0xca8a04, roughness: 0.95 }),
    wheatCount,
  );

  let ci = 0;
  let wi = 0;
  for (let gx = -1250; gx <= 1250; gx += 68) {
    for (let gz = -1250; gz <= 1250; gz += 68) {
      const r = Math.hypot(gx, gz);
      if (r < 170 || r > GEOFENCE_RADIUS - 70) continue;
      if (Math.abs(gx) < 35 || Math.abs(gz) < 35) continue;
      dummy.position.set(gx + (rand() - 0.5) * 12, 0.35, gz + (rand() - 0.5) * 12);
      dummy.scale.set(1, 0.9 + rand() * 0.4, 1);
      dummy.rotation.set(0, (rand() > 0.5 ? 0 : Math.PI / 2), 0);
      dummy.updateMatrix();
      if ((gx + gz) % 136 === 0 && ci < chanaCount) {
        chanaInst.setMatrixAt(ci++, dummy.matrix);
      } else if (wi < wheatCount) {
        wheatInst.setMatrixAt(wi++, dummy.matrix);
      }
    }
  }
  chanaInst.count = ci;
  wheatInst.count = wi;
  worldGroup.add(chanaInst, wheatInst);

  // 9. STRICT 1.5 KM GLOWING 3D RED LASER / ENERGY GEOFENCE WALL!
  const fenceGroup = new THREE.Group();
  const fenceGeo = new THREE.CylinderGeometry(GEOFENCE_RADIUS, GEOFENCE_RADIUS, 42, 96, 1, true);
  const fenceMat = new THREE.MeshBasicMaterial({
    color: 0xef4444,
    transparent: true,
    opacity: 0.24,
    side: THREE.DoubleSide,
    depthWrite: false,
  });
  const fenceWall = new THREE.Mesh(fenceGeo, fenceMat);
  fenceWall.position.y = 21;
  fenceGroup.add(fenceWall);

  // Bright red glowing laser rings along the 1.5 km perimeter
  const laserRingMat = new THREE.MeshBasicMaterial({ color: 0xff2222 });
  [4, 14, 26, 38].forEach((hy) => {
    const ring = new THREE.Mesh(new THREE.TorusGeometry(GEOFENCE_RADIUS, 0.55, 6, 96), laserRingMat);
    ring.rotation.x = Math.PI / 2;
    ring.position.y = hy;
    fenceGroup.add(ring);
  });

  // Red energy pylon pillars around the perimeter
  const pylonInst = new THREE.InstancedMesh(
    new THREE.CylinderGeometry(0.9, 1.2, 46, 6),
    new THREE.MeshBasicMaterial({ color: 0xdc2626 }),
    48,
  );
  for (let i = 0; i < 48; i++) {
    const a = (i / 48) * Math.PI * 2;
    dummy.position.set(Math.cos(a) * GEOFENCE_RADIUS, 23, Math.sin(a) * GEOFENCE_RADIUS);
    dummy.scale.set(1, 1, 1);
    dummy.rotation.set(0, 0, 0);
    dummy.updateMatrix();
    pylonInst.setMatrixAt(i, dummy.matrix);
  }
  fenceGroup.add(pylonInst);
  worldGroup.add(fenceGroup);

  // 10. Optional Background OpenStreetMap Road Splines for Gram Johar Barha (23.0035, 78.2215)
  const osmGroup = new THREE.Group();
  worldGroup.add(osmGroup);
  let osmRoadSegments = [];

  async function loadOsmRoads(onStatus) {
    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 7000);
      const query = `[out:json][timeout:6];way["highway"](around:1450,${CENTER_LAT},${CENTER_LNG});(._;>;);out body;`;
      const res = await fetch('https://overpass-api.de/api/interpreter', {
        method: 'POST',
        body: query,
        signal: controller.signal,
      });
      clearTimeout(timer);
      if (!res.ok) throw new Error('OSM unavailable');
      const data = await res.json();
      const nodes = new Map();
      const ways = [];
      for (const el of data.elements || []) {
        if (el.type === 'node') nodes.set(el.id, el);
        else if (el.type === 'way' && el.nodes) ways.push(el);
      }

      const metersPerLat = 111132;
      const metersPerLng = 111320 * Math.cos((CENTER_LAT * Math.PI) / 180);
      const osmMat = new THREE.MeshStandardMaterial({ color: 0x4b5563, roughness: 0.9 });
      const osmDirtMat = new THREE.MeshStandardMaterial({ color: 0xa1794c, roughness: 0.96 });

      let count = 0;
      for (const w of ways) {
        const pts = [];
        for (const nid of w.nodes) {
          const n = nodes.get(nid);
          if (!n) continue;
          const x = (n.lon - CENTER_LNG) * metersPerLng;
          const z = -(n.lat - CENTER_LAT) * metersPerLat;
          if (Math.hypot(x, z) <= GEOFENCE_RADIUS - 25) {
            pts.push([x, z]);
          }
        }
        if (pts.length >= 2) {
          const hw = w.tags?.highway || 'track';
          const isPaved = ['primary', 'secondary', 'tertiary', 'unclassified', 'residential'].includes(hw);
          addRoadRibbon(osmGroup, pts, isPaved ? 10 : 6.5, isPaved ? osmMat : osmDirtMat, 0.032);
          osmRoadSegments.push(pts);
          count++;
        }
      }
      onStatus?.(count > 0 ? `OSM Live (${count} Johar Barha roads)` : 'Procedural Johar Barha Roads');
    } catch (_) {
      onStatus?.('Offline Procedural Johar Barha Roads');
    }
  }

  // Time-of-Day atmosphere controller (Defaults to Bright Sunny Rural MP Afternoon)
  function applyTimeOfDay(mode) {
    if (mode === 'GOLDEN_HOUR') {
      scene.background = new THREE.Color(0xfdba74);
      scene.fog = new THREE.FogExp2(0xfdba74, 0.00145);
      hemiLight.color.setHex(0xfed7aa);
      hemiLight.groundColor.setHex(0x7c2d12);
      hemiLight.intensity = 0.9;
      sunLight.color.setHex(0xfb923c);
      sunLight.intensity = 1.75;
      sunLight.position.set(-220, 75, 160);
      sunDisc.position.set(-1200, 320, 820);
      sunDiscMat.color.setHex(0xf97316);
      nightLights.forEach((l) => { l.intensity = 1.6; });
    } else if (mode === 'NIGHT') {
      scene.background = new THREE.Color(0x0b1020);
      scene.fog = new THREE.FogExp2(0x0b1020, 0.0019);
      hemiLight.color.setHex(0x334155);
      hemiLight.groundColor.setHex(0x0f172a);
      hemiLight.intensity = 0.55;
      sunLight.color.setHex(0x93c5fd);
      sunLight.intensity = 0.55;
      sunLight.position.set(120, 180, -140);
      sunDisc.position.set(800, 920, -900);
      sunDiscMat.color.setHex(0xe2e8f0);
      nightLights.forEach((l) => { l.intensity = 3.2; });
    } else {
      // AFTERNOON — Bright Sunny Rural Madhya Pradesh Afternoon
      scene.background = new THREE.Color(0x6ec3f8);
      scene.fog = new THREE.FogExp2(0xbae0f8, 0.00085);
      hemiLight.color.setHex(0x93d5ff);
      hemiLight.groundColor.setHex(0x7a6a3f);
      hemiLight.intensity = 1.15;
      sunLight.color.setHex(0xfff6dd);
      sunLight.intensity = 2.05;
      sunLight.position.set(150, 225, 105);
      sunDisc.position.set(900, 1180, 620);
      sunDiscMat.color.setHex(0xfffbeb);
      nightLights.forEach((l) => { l.intensity = 0; });
    }
  }

  applyTimeOfDay('AFTERNOON');

  return {
    worldGroup,
    colliders,
    sunLight,
    fenceMat,
    applyTimeOfDay,
    applySatelliteGroundTexture,
    loadOsmRoads,
    getOsmRoads: () => osmRoadSegments,
  };
}
