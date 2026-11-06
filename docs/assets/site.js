// Scroll choreography for the landing page. Works without the 3D scene: in calm mode
// (reduced motion, no WebGL, or after "Skip animation") the chapters are plain sections.

const root = document.documentElement;
const story = document.getElementById("story");
const chapters = [...document.querySelectorAll(".chapter")];
const railDots = [...document.querySelectorAll(".rail li")];
const counterNow = document.querySelector(".counter-now");
const skip = document.querySelector(".skip-anim");
const features = document.getElementById("features");
const canvas = document.getElementById("scene");
const download = document.getElementById("download");
const install = document.getElementById("install");

let scene = null;
let activeChapter = -1;
let activeStep = -1;

const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
const smooth = (a, b, x) => { const t = clamp((x - a) / (b - a)); return t * t * (3 - 2 * t); };

function setCalm() {
  root.classList.remove("motion");
  root.classList.add("calm");
  chapters.forEach((c) => c.classList.add("on"));
  if (skip) skip.hidden = true;
}

/** Map the scroll position to a shape index: 0 hero, 1–3 chapters, 4 starfield, 5 closing sphere at download. */
function measure() {
  const y = window.scrollY;
  const vh = window.innerHeight;
  const top = story.offsetTop;
  const length = Math.max(story.offsetHeight - vh, 1);

  let morph;
  let chapter = -1;
  if (y < top) {
    morph = clamp(y / Math.max(top, 1));
  } else if (y <= top + length) {
    const seg = clamp((y - top) / length) * 3;
    chapter = Math.min(2, Math.floor(seg));
    // Hold each shape for most of its chapter, then pour into the next one.
    morph = chapter < 2 ? 1 + chapter + smooth(0.7, 1, seg - chapter) : 3;
  } else {
    morph = 3 + clamp((y - top - length) / vh);
    // Bookend: the swarm gathers back into a sphere as the download section comes in.
    if (download) {
      const r = download.getBoundingClientRect();
      morph += smooth(vh * 1.1, vh * 0.25, r.top);
    }
  }
  return { morph, chapter };
}

function update() {
  const { morph, chapter } = measure();
  if (scene) scene.setMorph(morph);

  if (chapter !== activeChapter) {
    activeChapter = chapter;
    chapters.forEach((c, i) => c.classList.toggle("on", i === chapter));
    if (chapter >= 0) counterNow.textContent = String(chapter + 1).padStart(2, "0");
  }

  const step = chapter >= 0 ? chapter + 1 : morph < 0.5 ? 0 : morph >= 3.5 ? 4 : activeStep;
  if (step !== activeStep) {
    activeStep = step;
    railDots.forEach((d, i) => d.classList.toggle("on", i === step));
  }

  if (skip) skip.classList.toggle("gone", morph >= 3.2);
  // Past the download headline, dim the scene so the install steps stay readable.
  if (install) root.classList.toggle("outro", install.getBoundingClientRect().top < window.innerHeight * 0.55);
}

let ticking = false;
function onScroll() {
  if (ticking) return;
  ticking = true;
  requestAnimationFrame(() => { ticking = false; update(); });
}

function watchReveals() {
  const items = document.querySelectorAll(".reveal");
  if (!("IntersectionObserver" in window)) { items.forEach((el) => el.classList.add("in")); return; }
  const io = new IntersectionObserver((entries) => {
    entries.forEach((e) => {
      if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
    });
  }, { rootMargin: "0px 0px -12% 0px", threshold: 0.12 });
  items.forEach((el) => io.observe(el));
}

async function boot() {
  watchReveals();
  if (!root.classList.contains("motion")) {
    setCalm();
    return;
  }

  skip.hidden = false;
  skip.addEventListener("click", () => {
    try { sessionStorage.setItem("akspend-skip", "1"); } catch (e) { /* private mode */ }
    features.scrollIntoView({ behavior: "auto", block: "start" });
    if (scene) scene.jumpTo(4);
    features.setAttribute("tabindex", "-1");
    features.focus({ preventScroll: true });
  });

  window.addEventListener("scroll", onScroll, { passive: true });
  window.addEventListener("resize", onScroll);
  update();

  try {
    const { startScene } = await import("./scene.js");
    scene = startScene(canvas, { mobile: window.matchMedia("(max-width: 760px), (pointer: coarse)").matches });
    update();
    requestAnimationFrame(() => canvas.classList.add("ready"));
  } catch (err) {
    // Without the 3D scene the pinned chapters would be empty, so fall back to the calm layout.
    console.warn("3D scene unavailable, using the static layout.", err);
    setCalm();
  }

  // The OS setting can change while the page is open.
  window.matchMedia("(prefers-reduced-motion: reduce)").addEventListener?.("change", (e) => {
    if (e.matches) location.reload();
  });
}

boot();
