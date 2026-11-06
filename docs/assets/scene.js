// AkSpend landing page: one particle swarm that morphs between shapes as the page scrolls.
//   0 vault sphere (hero) → 1 phone with a padlock → 2 a week of spending bars
//   → 3 budget rings → 4 a calm starfield (features, privacy) → 5 the vault again (download).
// The page drives it through setMorph(); this file owns rendering, camera and post-processing.

import * as THREE from "three";
import { EffectComposer } from "../vendor/jsm/postprocessing/EffectComposer.js";
import { RenderPass } from "../vendor/jsm/postprocessing/RenderPass.js";
import { UnrealBloomPass } from "../vendor/jsm/postprocessing/UnrealBloomPass.js";
import { OutputPass } from "../vendor/jsm/postprocessing/OutputPass.js";

const SHAPES = 6;
const TAU = Math.PI * 2;

// Camera distance per shape: it pushes in on the chapters and pulls back for the starfield.
const CAMERA_Z = [8.6, 7.0, 7.4, 6.9, 9.4, 8.2];

export function startScene(canvas, { mobile }) {
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: false, powerPreference: "high-performance" });
  const pixelRatio = Math.min(window.devicePixelRatio || 1, mobile ? 1.25 : 2);
  renderer.setPixelRatio(pixelRatio);
  // Opaque black: the canvas is composited with `mix-blend-mode: screen`, so black reads as
  // transparent and the CSS backdrop shows through, while bloom still has a clean buffer.
  renderer.setClearColor(0x000000, 1);

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
  camera.position.set(0, 0, CAMERA_Z[0]);

  // ---- main swarm ----
  const count = mobile ? 7000 : 16000;
  const geometry = new THREE.BufferGeometry();
  const shapes = buildShapes(count);
  for (let s = 0; s < SHAPES; s++) geometry.setAttribute(`p${s}`, new THREE.BufferAttribute(shapes[s], 3));
  geometry.setAttribute("position", new THREE.BufferAttribute(shapes[0], 3)); // bounds only
  geometry.setAttribute("aSeed", new THREE.BufferAttribute(seeds(count), 1));
  geometry.setAttribute("aColor", new THREE.BufferAttribute(palette(count), 3));

  const uniforms = {
    uTime: { value: 0 },
    uMorph: { value: 0 },
    uSize: { value: mobile ? 40 : 36 },
    uPixelRatio: { value: pixelRatio },
    uFade: { value: 1 },
  };
  const swarm = new THREE.Points(geometry, new THREE.ShaderMaterial({
    uniforms, vertexShader: SWARM_VERTEX, fragmentShader: SWARM_FRAGMENT,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
  }));
  swarm.frustumCulled = false;

  const group = new THREE.Group();
  group.add(swarm);
  scene.add(group);

  // ---- dust: a slow, out-of-focus layer that gives the camera moves real parallax ----
  const dustCount = mobile ? 500 : 1400;
  const dustGeo = new THREE.BufferGeometry();
  const dustPos = new Float32Array(dustCount * 3);
  for (let i = 0; i < dustCount; i++) put(dustPos, i, jitter(14), jitter(9), -12 + Math.random() * 16);
  dustGeo.setAttribute("position", new THREE.BufferAttribute(dustPos, 3));
  dustGeo.setAttribute("aSeed", new THREE.BufferAttribute(seeds(dustCount), 1));
  const dustUniforms = { uTime: uniforms.uTime, uPixelRatio: uniforms.uPixelRatio };
  const dust = new THREE.Points(dustGeo, new THREE.ShaderMaterial({
    uniforms: dustUniforms, vertexShader: DUST_VERTEX, fragmentShader: DUST_FRAGMENT,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
  }));
  dust.frustumCulled = false;
  scene.add(dust);

  // ---- post-processing: bloom turns the dots into light ----
  let composer = null;
  let bloom = null;
  try {
    composer = new EffectComposer(renderer);
    composer.addPass(new RenderPass(scene, camera));
    bloom = new UnrealBloomPass(new THREE.Vector2(1, 1), mobile ? 0.85 : 1.05, 0.6, 0.0);
    composer.addPass(bloom);
    composer.addPass(new OutputPass());
  } catch (err) {
    console.warn("Bloom unavailable, rendering without it.", err);
    composer = null;
  }

  let morphTarget = 0;
  let morph = 0;
  let pointerX = 0;
  let pointerY = 0;
  let width = 0;
  let height = 0;
  let running = true;
  const clock = new THREE.Clock();

  function resize() {
    width = canvas.clientWidth;
    height = canvas.clientHeight;
    renderer.setSize(width, height, false);
    if (composer) {
      composer.setPixelRatio(pixelRatio);
      composer.setSize(width, height);
      // Bloom at half resolution: it is blurred anyway, and this halves its cost.
      bloom.resolution.set(width / 2, height / 2);
    }
    camera.aspect = width / Math.max(height, 1);
    camera.updateProjectionMatrix();
  }
  resize();
  window.addEventListener("resize", resize);

  window.addEventListener("pointermove", (e) => {
    pointerX = (e.clientX / window.innerWidth) * 2 - 1;
    pointerY = (e.clientY / window.innerHeight) * 2 - 1;
  }, { passive: true });

  function frame() {
    if (!running) return;
    const dt = Math.min(clock.getDelta(), 0.05);
    const t = (uniforms.uTime.value += dt);

    // Ease toward the scroll target so fast scrolling still looks like a flow.
    morph += (morphTarget - morph) * (1 - Math.pow(0.0015, dt));
    uniforms.uMorph.value = morph;

    const heroW = 1 - smooth(0, 1, morph);
    const storyW = smooth(0.2, 1, morph) * (1 - smooth(3, 3.8, morph));
    const starsW = smooth(3, 4, morph);
    const endW = smooth(4, 5, morph);
    uniforms.uFade.value = 1 - 0.6 * starsW + 0.12 * endW;

    // Framing: hero sphere above the headline, chapter shapes beside (wide) or above (narrow) their text.
    const wide = width >= 900;
    const ox = wide ? -1.9 * storyW : 0;
    const oy = wide ? 0.55 * heroW : (1.0 * heroW + 1.75 * storyW) * (1 - starsW);
    const scale = (wide ? 1 : 0.62) * (1 + 0.6 * starsW - 0.95 * endW);
    group.position.x += (ox - group.position.x) * 0.08;
    group.position.y += (oy - group.position.y) * 0.08;
    group.scale.setScalar(group.scale.x + (scale - group.scale.x) * 0.08);
    group.rotation.y += ((Math.sin(t * 0.25) * 0.22 + pointerX * 0.22) - group.rotation.y) * 0.05;
    group.rotation.x += ((pointerY * 0.12) - group.rotation.x) * 0.05;

    // Camera: dolly per shape, swing around the swarm while it is between shapes, and drift.
    const seg = Math.min(Math.floor(morph), SHAPES - 2);
    const frac = morph - seg;
    const z = CAMERA_Z[seg] + (CAMERA_Z[seg + 1] - CAMERA_Z[seg]) * smooth(0, 1, frac);
    const swing = Math.sin(Math.PI * smooth(0, 1, frac)) * 0.42 * (seg % 2 === 0 ? 1 : -1);
    const angle = swing + Math.sin(t * 0.11) * 0.05;
    camera.position.set(Math.sin(angle) * z, Math.cos(t * 0.13) * 0.18 - pointerY * 0.15, Math.cos(angle) * z);
    camera.lookAt(group.position.x * 0.35, group.position.y * 0.35, 0);

    if (composer) composer.render(dt);
    else renderer.render(scene, camera);
    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);

  // Stop drawing while the tab is hidden.
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) {
      running = false;
    } else if (!running) {
      running = true;
      clock.getDelta();
      requestAnimationFrame(frame);
    }
  });

  return {
    setMorph(value) { morphTarget = Math.max(0, Math.min(SHAPES - 1, value)); },
    jumpTo(value) { morphTarget = morph = Math.max(0, Math.min(SHAPES - 1, value)); },
  };
}

// ---------- shapes ----------

function buildShapes(n) {
  return [sphere(n), phoneLock(n), weekBars(n), budgetRings(n), starfield(n), sphere(n)].map(shuffleInPlace);
}

function sphere(n) {
  const out = new Float32Array(n * 3);
  const golden = Math.PI * (3 - Math.sqrt(5));
  for (let i = 0; i < n; i++) {
    const shell = Math.random() < 0.78;
    const r = shell ? 2.05 + jitter(0.03) : 0.4 + Math.random() * 1.55;
    const y = 1 - (i / (n - 1)) * 2;
    const ring = Math.sqrt(1 - y * y);
    const theta = golden * i;
    put(out, i, Math.cos(theta) * ring * r, y * r, Math.sin(theta) * ring * r);
  }
  return out;
}

function phoneLock(n) {
  const out = new Float32Array(n * 3);
  const w = 1.55, h = 3.1, rad = 0.28;
  for (let i = 0; i < n; i++) {
    const k = Math.random();
    if (k < 0.42) {
      // phone outline, with a little thickness
      const [x, y] = roundedRectEdge(w, h, rad, Math.random());
      put(out, i, x + jitter(0.025), y + jitter(0.025), jitter(0.08));
    } else if (k < 0.58) {
      // faint screen surface
      put(out, i, (Math.random() - 0.5) * (w - 0.3), (Math.random() - 0.5) * (h - 0.4), -0.05 + jitter(0.02));
    } else if (k < 0.74) {
      // padlock body: mostly outline, some fill
      const [ex, ey] = Math.random() < 0.6
        ? roundedRectEdge(0.78, 0.62, 0.1, Math.random())
        : [(Math.random() - 0.5) * 0.78, (Math.random() - 0.5) * 0.62];
      put(out, i, ex, ey - 0.25, 0.12 + jitter(0.04));
    } else if (k < 0.84) {
      // padlock shackle: half ring above the body
      const a = Math.random() * Math.PI;
      put(out, i, Math.cos(a) * 0.27 + jitter(0.02), 0.06 + Math.sin(a) * 0.36 + jitter(0.02), 0.12 + jitter(0.03));
    } else {
      // the "network" it refuses to talk to: a loose ring drifting far outside
      const a = Math.random() * TAU;
      const r = 3.3 + Math.random() * 1.2;
      put(out, i, Math.cos(a) * r, Math.sin(a) * r * 0.85, -1 + jitter(1.2));
    }
  }
  return out;
}

function weekBars(n) {
  const out = new Float32Array(n * 3);
  const heights = [0.95, 1.6, 0.7, 2.2, 1.3, 2.7, 1.75];
  const gap = 0.5;
  const base = -1.35;
  const total = heights.reduce((a, b) => a + b, 0);
  for (let i = 0; i < n; i++) {
    if (Math.random() < 0.08) {
      put(out, i, (Math.random() - 0.5) * gap * 7.6, base - 0.12 + jitter(0.02), jitter(0.2)); // baseline
      continue;
    }
    let pick = Math.random() * total;
    let col = 0;
    while (pick > heights[col]) { pick -= heights[col]; col++; }
    // dense edges, lighter inside, so each bar reads as a solid column
    let px = (Math.random() - 0.5) * 0.3;
    let pz = (Math.random() - 0.5) * 0.3;
    if (Math.random() < 0.55) {
      if (Math.random() < 0.5) px = Math.sign(px || 1) * 0.15; else pz = Math.sign(pz || 1) * 0.15;
    }
    put(out, i, (col - 3) * gap + px, base + Math.random() * heights[col], pz);
  }
  return out;
}

function budgetRings(n) {
  const out = new Float32Array(n * 3);
  const rings = [{ r: 0.8, fill: 0.86 }, { r: 1.28, fill: 0.62 }, { r: 1.76, fill: 0.38 }];
  for (let i = 0; i < n; i++) {
    const ring = rings[Math.floor(Math.random() * rings.length)];
    const track = Math.random() < 0.18;
    // filled arc starts at 12 o'clock and runs clockwise; the track is a sparse full circle
    const a = Math.PI / 2 - (track ? Math.random() : Math.random() * ring.fill) * TAU;
    const rr = ring.r + jitter(track ? 0.015 : 0.085);
    put(out, i, Math.cos(a) * rr, Math.sin(a) * rr, jitter(track ? 0.02 : 0.09));
  }
  return out;
}

function starfield(n) {
  const out = new Float32Array(n * 3);
  for (let i = 0; i < n; i++) {
    const u = Math.random() * 2 - 1;
    const a = Math.random() * TAU;
    const r = 4.5 + Math.random() * 7;
    const ring = Math.sqrt(1 - u * u);
    put(out, i, Math.cos(a) * ring * r, u * r * 0.7, Math.sin(a) * ring * r - 3);
  }
  return out;
}

// ---------- helpers ----------

function roundedRectEdge(w, h, r, t) {
  // Walk the perimeter of a rounded rectangle; t in [0,1).
  const sw = w - 2 * r, sh = h - 2 * r, arc = (Math.PI / 2) * r;
  let d = t * (2 * sw + 2 * sh + 4 * arc);
  const segs = [
    [sw, (s) => [-w / 2 + r + s, h / 2]],
    [arc, (s) => corner(w / 2 - r, h / 2 - r, r, Math.PI / 2 - s / r)],
    [sh, (s) => [w / 2, h / 2 - r - s]],
    [arc, (s) => corner(w / 2 - r, -h / 2 + r, r, -s / r)],
    [sw, (s) => [w / 2 - r - s, -h / 2]],
    [arc, (s) => corner(-w / 2 + r, -h / 2 + r, r, -Math.PI / 2 - s / r)],
    [sh, (s) => [-w / 2, -h / 2 + r + s]],
    [arc, (s) => corner(-w / 2 + r, h / 2 - r, r, Math.PI - s / r)],
  ];
  for (const [len, fn] of segs) {
    if (d <= len) return fn(d);
    d -= len;
  }
  return [-w / 2 + r, h / 2];
}

function corner(cx, cy, r, a) { return [cx + Math.cos(a) * r, cy + Math.sin(a) * r]; }
function put(arr, i, x, y, z) { arr[i * 3] = x; arr[i * 3 + 1] = y; arr[i * 3 + 2] = z; }
function jitter(amount) { return (Math.random() - 0.5) * 2 * amount; }

function shuffleInPlace(arr) {
  // Shuffle whole points, so particle i lands somewhere random in every shape and morphs look like a flow.
  const n = arr.length / 3;
  for (let i = n - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    for (let k = 0; k < 3; k++) {
      const tmp = arr[i * 3 + k];
      arr[i * 3 + k] = arr[j * 3 + k];
      arr[j * 3 + k] = tmp;
    }
  }
  return arr;
}

function seeds(n) {
  const out = new Float32Array(n);
  for (let i = 0; i < n; i++) out[i] = Math.random();
  return out;
}

function palette(n) {
  // The app's dark palette: teal primary, warm ivory surface text, a little amber.
  const teal = [0x8e / 255, 0xd4 / 255, 0xc6 / 255];
  const deep = [0x3f / 255, 0x9c / 255, 0x8c / 255];
  const ivory = [0xf0 / 255, 0xe9 / 255, 0xe2 / 255];
  const amber = [0xf0 / 255, 0xbd / 255, 0x98 / 255];
  const out = new Float32Array(n * 3);
  for (let i = 0; i < n; i++) {
    const k = Math.random();
    const c = k < 0.5 ? teal : k < 0.72 ? deep : k < 0.92 ? ivory : amber;
    out.set(c, i * 3);
  }
  return out;
}

function smooth(a, b, x) {
  const t = Math.max(0, Math.min(1, (x - a) / (b - a)));
  return t * t * (3 - 2 * t);
}

// ---------- shaders ----------

const SWARM_VERTEX = /* glsl */ `
  attribute vec3 p0;
  attribute vec3 p1;
  attribute vec3 p2;
  attribute vec3 p3;
  attribute vec3 p4;
  attribute vec3 p5;
  attribute float aSeed;
  attribute vec3 aColor;

  uniform float uTime;
  uniform float uMorph;
  uniform float uSize;
  uniform float uPixelRatio;

  varying vec3 vColor;
  varying float vAlpha;

  // Each particle starts its move a little later than the last, so shapes pour into each other.
  float stage(float m, float seed) {
    float t = clamp((m - seed * 0.35) / 0.65, 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
  }

  vec3 rotY(vec3 p, float a) {
    float c = cos(a), s = sin(a);
    return vec3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
  }

  void main() {
    float s1 = stage(uMorph, aSeed);
    float s2 = stage(uMorph - 1.0, aSeed);
    float s3 = stage(uMorph - 2.0, aSeed);
    float s4 = stage(uMorph - 3.0, aSeed);
    float s5 = stage(uMorph - 4.0, aSeed);

    // Both spheres spin on their own; the chapter shapes face the reader.
    vec3 pos = mix(rotY(p0, uTime * 0.18), p1, s1);
    pos = mix(pos, p2, s2);
    pos = mix(pos, p3, s3);
    pos = mix(pos, p4, s4);
    pos = mix(pos, rotY(p5, uTime * 0.18), s5);

    // Mid-flight swirl: strongest halfway through a morph, zero once a shape has settled.
    float flight = sin(3.14159 * s1) + sin(3.14159 * s2) + sin(3.14159 * s3) + sin(3.14159 * s4) + sin(3.14159 * s5);
    vec3 swirl = normalize(vec3(-pos.y, pos.x, 0.35) + 0.0001);
    pos += swirl * flight * (0.35 + aSeed * 0.5);

    // Gentle breathing so a settled shape still feels alive.
    pos += 0.025 * vec3(sin(uTime * 0.9 + aSeed * 40.0), cos(uTime * 0.7 + aSeed * 31.0), sin(uTime * 0.8 + aSeed * 17.0));

    vec4 mv = modelViewMatrix * vec4(pos, 1.0);
    gl_Position = projectionMatrix * mv;

    // A few bright "sparks" among many fine particles reads as depth and catches the bloom.
    float spark = step(0.985, aSeed);
    float size = mix(0.3 + aSeed * 0.8, 2.4, spark);
    gl_PointSize = uSize * size * uPixelRatio / -mv.z;

    // Depth fog: far particles fade, near ones stay crisp.
    float depth = -mv.z;
    float fog = smoothstep(17.0, 6.5, depth);
    float twinkle = 0.65 + 0.35 * sin(uTime * (1.5 + aSeed * 2.0) + aSeed * 60.0);

    vColor = mix(aColor, vec3(1.0), spark * 0.6);
    vAlpha = fog * twinkle * mix(1.0, 1.4, spark);
  }
`;

const SWARM_FRAGMENT = /* glsl */ `
  uniform float uFade;
  varying vec3 vColor;
  varying float vAlpha;

  void main() {
    float d = length(gl_PointCoord - 0.5);
    if (d > 0.5) discard;
    float core = smoothstep(0.5, 0.0, d);
    float glow = core * core;
    gl_FragColor = vec4(vColor * (0.5 + 0.7 * glow), glow * vAlpha * uFade * 0.75);
  }
`;

const DUST_VERTEX = /* glsl */ `
  attribute float aSeed;
  uniform float uTime;
  uniform float uPixelRatio;
  varying float vAlpha;

  void main() {
    vec3 p = position;
    p.y += mod(uTime * (0.04 + aSeed * 0.06) + aSeed * 9.0, 9.0) - 4.5;
    p.x += sin(uTime * 0.1 + aSeed * 20.0) * 0.3;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    gl_Position = projectionMatrix * mv;
    gl_PointSize = (14.0 + aSeed * 30.0) * uPixelRatio / -mv.z;
    vAlpha = 0.18 * smoothstep(20.0, 4.0, -mv.z);
  }
`;

const DUST_FRAGMENT = /* glsl */ `
  varying float vAlpha;
  void main() {
    float d = length(gl_PointCoord - 0.5);
    if (d > 0.5) discard;
    gl_FragColor = vec4(vec3(0.62, 0.85, 0.8), smoothstep(0.5, 0.1, d) * vAlpha);
  }
`;
