// AkSpend site: the WebGL phone. Two small scenes share this file: the hero phone with a few
// soft clay shapes, and the "How it works" stage where one phone turns to show each step.
// The screens are the real screenshots, read from the <img> elements already on the page, so
// the style switch and alt text keep working. Loaded only when WebGL is there and motion is
// allowed (see site.js); nothing on the page depends on it.

import * as THREE from "./vendor/three-0.170.0.module.min.js";

const root = document.documentElement;
const TAU = Math.PI * 2;
const clamp01 = (v) => Math.min(1, Math.max(0, v));
const smooth = (v) => { v = clamp01(v); return v * v * (3 - 2 * v); };
const ease = (dt, rate) => 1 - Math.exp(-dt * rate);
const cssVar = (name) => getComputedStyle(root).getPropertyValue(name).trim();

// ---------- shared geometry (phone height is 1) ----------
const PW = 0.468, PD = 0.052, PR = 0.078, BEZEL = 0.014, BEVEL = 0.012;
const SW = PW - 2 * BEZEL, SH = 1 - 2 * BEZEL;

function roundedRect(w, h, r) {
  const s = new THREE.Shape(), x = -w / 2, y = -h / 2;
  s.moveTo(x + r, y);
  s.lineTo(x + w - r, y); s.absarc(x + w - r, y + r, r, -Math.PI / 2, 0);
  s.lineTo(x + w, y + h - r); s.absarc(x + w - r, y + h - r, r, 0, Math.PI / 2);
  s.lineTo(x + r, y + h); s.absarc(x + r, y + h - r, r, Math.PI / 2, Math.PI);
  s.lineTo(x, y + r); s.absarc(x + r, y + r, r, Math.PI, Math.PI * 1.5);
  return s;
}
function extruded(w, h, r, depth, bevel) {
  const g = new THREE.ExtrudeGeometry(roundedRect(w - 2 * bevel, h - 2 * bevel, r - bevel), {
    depth: depth - 2 * bevel, bevelEnabled: true, bevelThickness: bevel, bevelSize: bevel, bevelSegments: 5, curveSegments: 18,
  });
  g.center();
  return g;
}
let GEO = null;
function geometry() {
  if (GEO) return GEO;
  const screen = new THREE.ShapeGeometry(roundedRect(SW, SH, PR - BEZEL), 18);
  const pos = screen.attributes.position, uv = screen.attributes.uv;
  for (let i = 0; i < pos.count; i++) uv.setXY(i, pos.getX(i) / SW + 0.5, pos.getY(i) / SH + 0.5);
  GEO = {
    body: extruded(PW, 1, PR, PD, BEVEL),
    screen,
    island: extruded(0.19, 0.2, 0.05, 0.016, 0.005),
    lens: new THREE.CylinderGeometry(0.034, 0.034, 0.008, 40).rotateX(Math.PI / 2),
    button: new THREE.BoxGeometry(0.008, 1, 0.016),
    plane: new THREE.PlaneGeometry(1, 1),
    torus: new THREE.TorusGeometry(1, 0.4, 28, 72),
    sphere: new THREE.SphereGeometry(1, 48, 32),
    capsule: new THREE.CapsuleGeometry(0.42, 0.95, 10, 28),
  };
  return GEO;
}

// Blurred white stamps, tinted per style, used as the soft shadows and highlights. The shape is
// drawn off-canvas and only its canvas shadow lands in view (works where ctx.filter does not).
function blob(w, h, draw) {
  const c = document.createElement("canvas"); c.width = w; c.height = h;
  const g = c.getContext("2d");
  g.shadowColor = "#fff"; g.shadowBlur = 26; g.shadowOffsetX = 2000; g.fillStyle = "#fff";
  g.translate(-2000, 0); g.beginPath(); draw(g); g.fill();
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace;
  return t;
}
function sheen() {
  const c = document.createElement("canvas"); c.width = 256; c.height = 4;
  const g = c.getContext("2d"), grad = g.createLinearGradient(0, 0, 256, 0);
  grad.addColorStop(0.2, "rgba(255,255,255,0)"); grad.addColorStop(0.5, "rgba(255,255,255,1)"); grad.addColorStop(0.8, "rgba(255,255,255,0)");
  g.fillStyle = grad; g.fillRect(0, 0, 256, 4);
  const t = new THREE.CanvasTexture(c); t.wrapS = THREE.RepeatWrapping; t.rotation = 0.5; t.repeat.set(0.6, 1);
  return t;
}

// ---------- one scene on one canvas ----------
class Scene {
  constructor(canvas, anchor) {
    this.canvas = canvas; this.anchor = anchor; this.visible = false; this.drawn = false;
    this.renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    this.renderer.setClearColor(0x000000, 0);
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(24, 1, 1, 10);
    this.sun = new THREE.DirectionalLight(0xffffff, 2.3); this.sun.position.set(-1, 1.25, 1.5);
    this.sky = new THREE.HemisphereLight(0xffffff, 0x999999, 1.7);
    this.scene.add(this.sun, this.sky);

    const G = geometry();
    this.mat = {
      body: new THREE.MeshStandardMaterial({ roughness: 0.36, metalness: 0.12 }),
      trim: new THREE.MeshStandardMaterial({ roughness: 0.3, metalness: 0.2 }),
      lens: new THREE.MeshStandardMaterial({ color: 0x08080c, roughness: 0.08, metalness: 0.5 }),
      screen: new THREE.MeshBasicMaterial({ color: 0x111118, toneMapped: false, polygonOffset: true, polygonOffsetFactor: -2, polygonOffsetUnits: -2 }),
      sheen: new THREE.MeshBasicMaterial({ map: sheen(), transparent: true, opacity: 0.13, blending: THREE.AdditiveBlending, depthWrite: false, polygonOffset: true, polygonOffsetFactor: -4, polygonOffsetUnits: -4 }),
      clay: new THREE.MeshStandardMaterial({ roughness: 0.85, metalness: 0 }),
      pop: new THREE.MeshStandardMaterial({ roughness: 0.55, metalness: 0.05 }),
    };
    // Phone: a group scaled to the anchor's height in CSS pixels.
    this.phone = new THREE.Group();
    const body = new THREE.Mesh(G.body, this.mat.body);
    const screen = new THREE.Mesh(G.screen, this.mat.screen); screen.position.z = PD / 2 + 0.0004;
    const gloss = new THREE.Mesh(G.screen, this.mat.sheen); gloss.position.z = PD / 2 + 0.0008;
    const island = new THREE.Mesh(G.island, this.mat.trim); island.position.set(0.105, 0.36, -PD / 2 - 0.004);
    const lensA = new THREE.Mesh(G.lens, this.mat.lens); lensA.position.set(0.105, 0.405, -PD / 2 - 0.012);
    const lensB = new THREE.Mesh(G.lens, this.mat.lens); lensB.position.set(0.105, 0.315, -PD / 2 - 0.012);
    const power = new THREE.Mesh(G.button, this.mat.trim); power.scale.y = 0.07; power.position.set(PW / 2 + 0.002, 0.13, 0);
    const volume = new THREE.Mesh(G.button, this.mat.trim); volume.scale.y = 0.13; volume.position.set(PW / 2 + 0.002, 0.27, 0);
    this.phone.add(body, screen, gloss, island, lensA, lensB, power, volume);
    this.scene.add(this.phone);

    // Soft shadow pair behind the phone: shade down-right, highlight up-left.
    const rectBlob = blob(256, 512, (g) => (g.roundRect ? g.roundRect(78, 116, 100, 280, 24) : g.rect(78, 116, 100, 280)));
    this.shade = this.stamp(rectBlob); this.glow = this.stamp(rectBlob);
    this.placeholder = null; this.textures = {}; this.shapes = [];
    this.size = { w: 0, h: 0, ph: 300, cx: 0, cy: 0 };
    this.rx = 0; this.ry = 0; this.rz = 0;
  }
  stamp(map) {
    const m = new THREE.Mesh(geometry().plane, new THREE.MeshBasicMaterial({ map, transparent: true, depthWrite: false, toneMapped: false }));
    m.renderOrder = -1; this.scene.add(m);
    return m;
  }
  // A screenshot texture that follows its <img>: it re-uploads whenever the image (re)loads.
  watch(key, img) {
    // Drawn through a 2D canvas at the image's real size, so a change of screenshot size (the
    // styles differ) just means a fresh upload.
    const copy = document.createElement("canvas"), tex = new THREE.CanvasTexture(copy);
    tex.colorSpace = THREE.SRGBColorSpace; tex.anisotropy = this.renderer.capabilities.getMaxAnisotropy();
    const ready = { ok: false };
    const upload = () => {
      if (!img.naturalWidth) return;
      const shot = img.naturalWidth / img.naturalHeight, face = SW / SH;
      if (shot > face) { tex.repeat.set(face / shot, 1); tex.offset.set((1 - face / shot) / 2, 0); }
      else { tex.repeat.set(1, shot / face); tex.offset.set(0, (1 - shot / face) / 2); }
      copy.width = img.naturalWidth; copy.height = img.naturalHeight;
      copy.getContext("2d").drawImage(img, 0, 0);
      tex.dispose(); tex.needsUpdate = true; ready.ok = true;
    };
    img.loading = "eager";
    img.addEventListener("load", upload);
    if (img.complete) upload();
    this.textures[key] = { tex, ready };
  }
  show(key) {
    const t = this.textures[key];
    if (!t || !t.ready.ok || this.shown === key) return;
    this.shown = key; this.mat.screen.map = t.tex; this.mat.screen.color.set(0xffffff); this.mat.screen.needsUpdate = true;
  }
  restyle() {
    const soft = !!cssVar("--lo"), dark = root.getAttribute("data-mode") === "dark";
    const bg = cssVar("--bg") || "#e6e3ef", bezel = cssVar("--bezel") || "#26243a";
    this.mat.body.color.set(bezel);
    this.mat.trim.color.set(bezel).lerp(new THREE.Color(0xffffff), dark ? 0.1 : 0.16);
    this.mat.clay.color.set(bg);
    this.mat.pop.color.set(cssVar("--accent-text") || "#5e49b0");
    this.sky.groundColor.set(soft ? cssVar("--lo") : dark ? "#3a3a3a" : "#9a9a9a");
    this.sky.intensity = dark ? 2.1 : 1.7;
    const lo = soft ? cssVar("--lo") : "#000000", hi = soft ? cssVar("--hi") : "#ffffff";
    const loA = soft ? 0.95 : dark ? 0.5 : 0.2, hiA = soft ? 0.95 : 0;
    [this.shade, ...this.shapes.map((s) => s.shade)].forEach((m) => { m.material.color.set(lo); m.material.opacity = loA; });
    [this.glow, ...this.shapes.map((s) => s.glow)].forEach((m) => { m.material.color.set(hi); m.material.opacity = hiA; });
    this.softStyle = soft;
  }
  resize() {
    const r = this.canvas.getBoundingClientRect(), a = this.anchor.getBoundingClientRect();
    if (!r.width || !r.height) return;
    const s = this.size; s.w = r.width; s.h = r.height;
    // Use the anchor's layout size, so a CSS transform on it never changes the phone's size.
    s.ph = this.anchor.offsetHeight || a.height;
    s.cx = a.left + a.width / 2 - (r.left + r.width / 2);
    s.cy = -(a.top + a.height / 2 - (r.top + r.height / 2));
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    this.renderer.setSize(r.width, r.height, false);
    // One world unit is one CSS pixel at the phone's depth.
    const dist = r.height / 2 / Math.tan((this.camera.fov * Math.PI) / 360);
    this.camera.aspect = r.width / r.height; this.camera.position.z = dist;
    this.camera.near = dist / 8; this.camera.far = dist * 3; this.camera.updateProjectionMatrix();
  }
  place(x, y, scale) {
    const s = this.size, ph = s.ph * scale;
    this.phone.position.set(s.cx + x, s.cy + y, 0);
    this.phone.scale.setScalar(ph);
    this.phone.rotation.set(this.rx, this.ry, this.rz);
    this.mat.sheen.map.offset.x = this.ry * 0.5 + this.rx;
    // The shadow pair narrows as the phone turns edge-on.
    const turn = Math.max(0.3, Math.abs(Math.cos(this.ry))), off = ph * (this.softStyle ? 0.055 : 0.03);
    const bw = ph * PW * (256 / 100) * turn, bh = ph * (512 / 280);
    this.shade.scale.set(bw, bh, 1); this.shade.position.set(s.cx + x + off, s.cy + y - off * 1.2, -ph * 0.2);
    this.glow.scale.set(bw, bh, 1); this.glow.position.set(s.cx + x - off, s.cy + y + off, -ph * 0.2);
  }
  render() { this.renderer.render(this.scene, this.camera); this.drawn = true; }
  dispose() { this.renderer.dispose(); }
}

// ---------- hero: phone, clay shapes, pointer and drag ----------
function hero(canvas, pointer) {
  const wrap = document.querySelector(".phone-wrap"), figure = wrap.querySelector(".phone-hero");
  const section = document.querySelector(".hero");
  const view = new Scene(canvas, wrap);
  view.watch("home", figure.querySelector("img"));

  const G = geometry();
  const round = blob(256, 256, (g) => g.arc(128, 128, 54, 0, TAU));
  // x, y, z in phone heights from the phone's centre; r is the radius in phone heights.
  [
    { geo: G.torus, mat: "clay", x: 0.43, y: 0.33, z: -0.3, r: 0.085, spin: 0.25, tiltX: 0.9, tiltY: 0.5 },
    { geo: G.sphere, mat: "pop", x: -0.42, y: -0.33, z: 0.25, r: 0.062, spin: 0, tiltX: 0, tiltY: 0 },
    { geo: G.capsule, mat: "clay", x: -0.47, y: 0.1, z: -0.2, r: 0.058, spin: -0.2, tiltX: 0.3, tiltY: 0.2 },
    { geo: G.sphere, mat: "clay", x: 0.46, y: -0.02, z: 0.2, r: 0.034, spin: 0, tiltX: 0, tiltY: 0 },
  ].forEach((d, i) => {
    const mesh = new THREE.Mesh(d.geo, view.mat[d.mat]);
    mesh.rotation.set(d.tiltX, d.tiltY, 0);
    view.scene.add(mesh);
    view.shapes.push({ ...d, mesh, shade: view.stamp(round), glow: view.stamp(round), phase: i * 1.7 });
  });

  // Drag (mouse or a sideways swipe) spins the phone; it coasts, then settles facing front again.
  let yaw = 0, vel = 0, dragging = false, lastX = 0, lastT = 0;
  figure.addEventListener("pointerdown", (e) => {
    dragging = true; lastX = e.clientX; lastT = e.timeStamp; vel = 0;
    try { figure.setPointerCapture(e.pointerId); } catch (err) { /* pointer already gone */ }
  });
  figure.addEventListener("pointermove", (e) => {
    if (!dragging) return;
    const dx = (e.clientX - lastX) * 0.011, dt = Math.max(1, e.timeStamp - lastT) / 1000;
    yaw += dx; vel = dx / dt; lastX = e.clientX; lastT = e.timeStamp;
  });
  const release = () => { dragging = false; };
  figure.addEventListener("pointerup", release); figure.addEventListener("pointercancel", release);
  figure.addEventListener("dragstart", (e) => e.preventDefault());

  let first = true, heroH = 1;
  view.measure = () => { view.resize(); heroH = section.offsetHeight || 1; };
  view.update = (t, dt) => {
    view.show("home");
    if (!dragging) {
      vel *= Math.exp(-dt * 2.6); yaw += vel * dt;
      if (Math.abs(vel) < 1.5) yaw += (Math.round(yaw / TAU) * TAU - yaw) * ease(dt, 2.4);
    }
    // p runs 0 to 1 as the hero scrolls away: the phone turns and leans back as it leaves.
    const p = clamp01(window.scrollY / (heroH * 0.9));
    const ty = -0.42 + pointer.x * 0.2 + p * 1.5 + yaw + Math.sin(t * 0.5) * 0.04;
    const tx = 0.09 + pointer.y * 0.12 + p * 0.3;
    if (first) { view.ry = ty - 2.2; view.rx = tx + 0.3; first = false; }
    view.ry += (ty - view.ry) * ease(dt, 5); view.rx += (tx - view.rx) * ease(dt, 5);
    view.rz = -0.045 * (1 - p);
    const ph = view.size.ph;
    view.place(0, Math.sin(t * 0.8) * ph * 0.012, 1);
    view.shapes.forEach((s) => {
      const depth = 1 + s.z * 1.6;
      // On narrow screens the shapes move in so none is cut off by the edge of the page.
      const reach = Math.min(view.size.w, window.innerWidth) / 2 / ph - s.r * 1.15;
      const sx = Math.max(-reach, Math.min(reach, s.x));
      const x = view.size.cx + (sx + pointer.x * 0.03 * depth) * ph;
      const y = view.size.cy + (s.y - pointer.y * 0.03 * depth + Math.sin(t * 0.7 + s.phase) * 0.014 + p * 0.22 * depth) * ph;
      const r = s.r * ph;
      s.mesh.position.set(x, y, s.z * ph); s.mesh.scale.setScalar(r);
      s.mesh.rotation.z = t * s.spin + s.phase; s.mesh.rotation.x = s.tiltX + Math.sin(t * 0.4 + s.phase) * 0.25;
      const span = r * 2 * (s.geo === G.torus ? 1.4 : s.geo === G.capsule ? 1.15 : 1) * (256 / 108), off = r * 0.42;
      s.shade.scale.set(span, span, 1); s.shade.position.set(x + off, y - off, -ph * 0.45);
      s.glow.scale.set(span, span, 1); s.glow.position.set(x - off, y + off, -ph * 0.45);
    });
  };
  return view;
}

// ---------- how it works: one phone, three screens ----------
function stage(canvas, pointer) {
  const box = canvas.closest(".how-stage");
  const steps = [...document.querySelectorAll(".how-steps li")];
  const dots = [...box.querySelectorAll(".stage-dots i")];
  const keys = [];
  const view = new Scene(canvas, box.querySelector(".stage-slot"));
  steps.forEach((li) => { const img = li.querySelector("img[data-shot]"); keys.push(img.dataset.shot); view.watch(img.dataset.shot, img); });

  let active = -1, first = true;
  view.measure = () => view.resize();
  view.update = (t, dt) => {
    const stack = window.matchMedia("(max-width: 820px)").matches;
    const boxR = box.getBoundingClientRect(), vh = window.innerHeight;
    // The reading line: mid-screen beside the stage, or the middle of the space under it.
    const line = stack ? (boxR.bottom + vh) / 2 : vh / 2;
    const mids = steps.map((li) => { const r = li.getBoundingClientRect(); return stack ? r.top + Math.min(r.height, 150) / 2 : r.top + r.height / 2; });
    // s is the step position as a float: 0 at the first step, 2 at the last.
    let s = 0;
    if (line >= mids[mids.length - 1]) s = mids.length - 1;
    else for (let k = 0; k < mids.length - 1; k++) if (line >= mids[k] && line < mids[k + 1]) s = k + (line - mids[k]) / (mids[k + 1] - mids[k]);
    const k = Math.min(Math.floor(s), steps.length - 2), spin = smooth((s - k - 0.22) / 0.56);
    // Each change of step is one full turn; the screen swaps while the back faces the camera.
    view.show(keys[spin < 0.5 ? k : k + 1]);
    const now = Math.round(s);
    if (now !== active) {
      active = now;
      steps.forEach((li, i) => li.classList.toggle("is-active", i === now));
      dots.forEach((d, i) => d.classList.toggle("on", i === now));
    }
    // The phone turns in as the stage scrolls into place.
    const stick = parseFloat(getComputedStyle(box).top) || 0;
    const arrive = smooth(1 - (boxR.top - stick) / (vh * 0.7));
    const ty = 0.34 + (k + spin) * TAU - (1 - arrive) * 1.5 + pointer.x * 0.12 + Math.sin(t * 0.5) * 0.03;
    const tx = 0.07 + pointer.y * 0.08 + (1 - arrive) * 0.25;
    if (first) { view.ry = ty; view.rx = tx; first = false; }
    view.ry += (ty - view.ry) * ease(dt, 9); view.rx += (tx - view.rx) * ease(dt, 6);
    view.rz = 0.03;
    view.place(0, Math.sin(t * 0.8) * view.size.ph * 0.008, 0.96);
  };
  return view;
}

// ---------- run ----------
let views = [];
export function restyle() { views.forEach((v) => v.restyle()); }

export function start({ onFail }) {
  const pointer = { x: 0, y: 0, tx: 0, ty: 0 };
  let raf = 0, last = 0, clock = 0, stopped = false;
  const heroCanvas = document.querySelector('.scene[data-scene="hero"]');
  const stageCanvas = document.querySelector('.scene[data-scene="stage"]');
  views = [hero(heroCanvas, pointer), stage(stageCanvas, pointer)];
  restyle();

  function frame(now) {
    raf = 0;
    if (stopped) return;
    const dt = Math.min((now - (last || now)) / 1000, 0.05); last = now; clock += dt;
    pointer.x += (pointer.tx - pointer.x) * ease(dt, 4); pointer.y += (pointer.ty - pointer.y) * ease(dt, 4);
    let any = false;
    views.forEach((v) => { if (v.visible) { any = true; v.update(clock, dt); v.render(); } });
    if (views[0].drawn && views[0].shown) root.classList.add("gl-ready");
    if (any && !document.hidden) raf = requestAnimationFrame(frame);
  }
  // Frames run only while a scene is on screen and the tab is showing.
  const wake = () => { if (!raf && !stopped && !document.hidden) { last = 0; raf = requestAnimationFrame(frame); } };
  const measure = () => { views.forEach((v) => v.measure()); wake(); };
  function stop() {
    if (stopped) return;
    stopped = true; cancelAnimationFrame(raf); io.disconnect(); ro.disconnect();
    window.removeEventListener("resize", measure); window.removeEventListener("pointermove", move);
    document.removeEventListener("visibilitychange", wake);
    views.forEach((v) => v.dispose()); views = [];
  }
  const move = (e) => {
    if (e.pointerType !== "mouse") return;
    pointer.tx = (e.clientX / window.innerWidth) * 2 - 1; pointer.ty = (e.clientY / window.innerHeight) * 2 - 1;
  };
  const io = new IntersectionObserver((entries) => {
    entries.forEach((e) => { const v = views.find((x) => x.canvas === e.target); if (v) v.visible = e.isIntersecting; });
    wake();
  }, { rootMargin: "80px 0px" });
  const ro = new ResizeObserver(measure);
  views.forEach((v) => {
    io.observe(v.canvas); ro.observe(v.canvas); ro.observe(v.anchor);
    v.canvas.addEventListener("webglcontextlost", () => { stop(); onFail(); });
  });
  window.addEventListener("resize", measure);
  window.addEventListener("pointermove", move, { passive: true });
  document.addEventListener("visibilitychange", wake);
  // If the first screenshot never arrives, show the scene anyway rather than hold the fade-in.
  setTimeout(() => { if (!stopped && views[0] && views[0].drawn) root.classList.add("gl-ready"); }, 2500);
  measure();
  return stop;
}
