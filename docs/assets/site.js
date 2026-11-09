// AkSpend landing page: scroll reveals and the particle orb behind the hero phone.
// Everything here is optional; the page reads the same without it.

(function () {
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");

  // ---------- scroll reveals ----------
  const items = document.querySelectorAll(".reveal");
  if (reduced.matches || !("IntersectionObserver" in window)) {
    items.forEach((el) => el.classList.add("in"));
  } else {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
      });
    }, { rootMargin: "0px 0px -10% 0px", threshold: 0.1 });
    items.forEach((el) => io.observe(el));
  }

  // ---------- app style switch ----------
  // The three styles from the app's More > Appearance, plus the light/dark choice.
  const G = "screenshots/liquid-glass/after/", A = "screenshots/atelier/", S = "screenshots/soft/";
  const SHOTS = {
    soft: {
      light: { home: S + "light-1-home.jpg", log: S + "light-2-activity.jpg", insights: S + "light-4-insights.jpg", budgets: S + "light-3-budgets.jpg" },
      dark: { home: S + "dark-1-home.jpg", log: S + "dark-2-activity.jpg", insights: S + "dark-4-insights.jpg", budgets: S + "dark-3-budgets.jpg" },
    },
    glass: {
      light: { home: G + "light-1-home.jpg", log: G + "light-8-add-transaction.jpg", insights: G + "light-4-insights.jpg", budgets: G + "light-3-budgets.jpg" },
      dark: { home: G + "dark-1-home.jpg", log: G + "dark-8-add-transaction.jpg", insights: G + "dark-4-insights.jpg", budgets: G + "dark-3-budgets.jpg" },
    },
    // Atelier has no dark home or budgets screenshot yet, so those two stay light in dark mode.
    atelier: {
      light: { home: A + "atelier-light-1-home.jpg", log: A + "atelier-light-2-activity.jpg", insights: A + "atelier-light-4-insights.jpg", budgets: A + "atelier-light-3-budgets.jpg" },
      dark: { home: A + "atelier-light-1-home.jpg", log: A + "atelier-dark-2-activity.jpg", insights: A + "atelier-dark-4-insights.jpg", budgets: A + "atelier-light-3-budgets.jpg" },
    },
  };
  const ALT = {
    soft: {
      home: "AkSpend home screen in the Soft style: net balance with money in and out, a monthly budget dial, and Add and Transfer buttons",
      log: "Activity in the Soft style: expenses and income grouped by day with category and payment method",
      insights: "Insights in the Soft style: spending forecast and spending by category",
      budgets: "Budgets in the Soft style: money left to spend and progress for each category budget",
    },
    glass: {
      home: "AkSpend home screen: net balance of ₹63,991.50, money in and out this month, and today's spending",
      log: "Adding an expense with amount, category and payment method",
      insights: "Insights: spending forecast and spending by category",
      budgets: "Budgets screen with category limits and progress bars",
    },
    atelier: {
      home: "AkSpend home screen in the Atelier style: net balance of ₹36,021.00 with Add and Transfer buttons",
      log: "Activity in the Atelier style: today's expenses and income with category and payment method",
      insights: "Insights in the Atelier style: spending forecast and spending by category",
      budgets: "Budgets in the Atelier style, before any budget is set, with a New budget button",
    },
  };
  const root = document.documentElement;
  const darkMq = window.matchMedia("(prefers-color-scheme: dark)");
  const shots = document.querySelectorAll("img[data-shot]");
  const pickers = document.querySelectorAll(".style-pick button[data-style]");
  const modePickers = document.querySelectorAll(".style-pick button[data-mode]");
  const themeMeta = document.querySelector('meta[name="theme-color"]');
  const styleListeners = [];
  let mode = "auto";
  try { const m = localStorage.getItem("akspend-mode"); if (m === "light" || m === "dark") mode = m; } catch (e) { /* storage blocked */ }
  const isDark = () => mode === "dark" || (mode === "auto" && darkMq.matches);

  function applyStyle(style, save) {
    if (!SHOTS[style]) style = "soft";
    root.setAttribute("data-style", style);
    root.setAttribute("data-mode", isDark() ? "dark" : "light");
    const set = SHOTS[style][isDark() ? "dark" : "light"];
    shots.forEach((img) => {
      const key = img.dataset.shot;
      if (img.getAttribute("src") !== set[key]) img.setAttribute("src", set[key]);
      img.alt = ALT[style][key];
    });
    pickers.forEach((b) => b.setAttribute("aria-checked", String(b.dataset.style === style)));
    modePickers.forEach((b) => b.setAttribute("aria-checked", String(b.dataset.mode === mode)));
    if (themeMeta) themeMeta.setAttribute("content", getComputedStyle(root).getPropertyValue("--bg").trim());
    if (save) { try { localStorage.setItem("akspend-style", style); } catch (e) { /* storage blocked */ } }
    styleListeners.forEach((fn) => fn());
  }
  function setMode(next) {
    mode = next;
    try { localStorage.setItem("akspend-mode", mode); } catch (e) { /* storage blocked */ }
    applyStyle(root.getAttribute("data-style"), false);
  }
  pickers.forEach((b) => b.addEventListener("click", () => applyStyle(b.dataset.style, true)));
  modePickers.forEach((b) => b.addEventListener("click", () => setMode(b.dataset.mode)));
  // Arrow keys move between options, as in a radio group.
  function arrowKeys(group, pick) {
    group.forEach((b, i) => b.addEventListener("keydown", (e) => {
      if (e.key !== "ArrowLeft" && e.key !== "ArrowRight") return;
      e.preventDefault();
      const next = group[(i + (e.key === "ArrowRight" ? 1 : group.length - 1)) % group.length];
      next.focus(); pick(next);
    }));
  }
  arrowKeys(pickers, (b) => applyStyle(b.dataset.style, true));
  arrowKeys(modePickers, (b) => setMode(b.dataset.mode));
  darkMq.addEventListener?.("change", () => applyStyle(root.getAttribute("data-style"), false));
  applyStyle(root.getAttribute("data-style"), false);

  // ---------- particle orb ----------
  const canvas = document.querySelector(".orb");
  if (!canvas || !canvas.getContext) return;
  const ctx = canvas.getContext("2d");

  const small = window.matchMedia("(max-width: 820px)").matches;
  const COUNT = small ? 650 : 1100;
  const pts = [];
  const golden = Math.PI * (3 - Math.sqrt(5));
  for (let i = 0; i < COUNT; i++) {
    // Even spread over a sphere, with a few points pulled inside for depth.
    const y = 1 - (i / (COUNT - 1)) * 2;
    const ring = Math.sqrt(1 - y * y);
    const shell = Math.random() < 0.82 ? 1 : 0.45 + Math.random() * 0.45;
    pts.push({
      x: Math.cos(golden * i) * ring * shell,
      y: y * shell,
      z: Math.sin(golden * i) * ring * shell,
      s: 0.6 + Math.random() * 1.3,
      tw: Math.random() * Math.PI * 2,
    });
  }

  let w = 0, h = 0, dpr = 1, rgb = "15, 95, 84";
  function readColor() {
    rgb = getComputedStyle(document.documentElement).getPropertyValue("--orb").trim() || rgb;
  }
  function resize() {
    const r = canvas.getBoundingClientRect();
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    w = r.width; h = r.height;
    canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  let angle = 0.6, tilt = 0.35, px = 0, py = 0, t = 0;
  function draw() {
    ctx.clearRect(0, 0, w, h);
    const R = Math.min(w, h) * 0.42;
    const cx = w / 2, cy = h / 2;
    const ca = Math.cos(angle), sa = Math.sin(angle);
    const tt = tilt + py * 0.15, ct = Math.cos(tt), st = Math.sin(tt);
    for (let i = 0; i < pts.length; i++) {
      const p = pts[i];
      const x1 = p.x * ca + p.z * sa;
      const z1 = -p.x * sa + p.z * ca;
      const y2 = p.y * ct - z1 * st;
      const z2 = p.y * st + z1 * ct;
      const depth = (z2 + 1) / 2; // 0 back, 1 front
      const a = (0.12 + 0.62 * depth) * (0.75 + 0.25 * Math.sin(t * 1.6 + p.tw));
      ctx.fillStyle = "rgba(" + rgb + "," + a.toFixed(3) + ")";
      ctx.beginPath();
      ctx.arc(cx + x1 * R, cy + y2 * R, p.s * (0.55 + 0.6 * depth), 0, Math.PI * 2);
      ctx.fill();
    }
  }

  let running = false, visible = true, last = 0;
  function frame(now) {
    if (!running) return;
    const dt = Math.min((now - (last || now)) / 1000, 0.05);
    last = now; t += dt;
    angle += dt * (0.12 + px * 0.05);
    draw();
    requestAnimationFrame(frame);
  }
  function start() {
    if (running || reduced.matches || !visible || document.hidden) return;
    running = true; last = 0; requestAnimationFrame(frame);
  }
  function stop() { running = false; }

  readColor(); resize(); draw();
  window.addEventListener("resize", () => { resize(); draw(); });
  styleListeners.push(() => { readColor(); draw(); });
  window.addEventListener("pointermove", (e) => {
    px = (e.clientX / window.innerWidth) * 2 - 1;
    py = (e.clientY / window.innerHeight) * 2 - 1;
  }, { passive: true });
  document.addEventListener("visibilitychange", () => (document.hidden ? stop() : start()));
  reduced.addEventListener?.("change", () => (reduced.matches ? stop() : start()));
  if ("IntersectionObserver" in window) {
    new IntersectionObserver(([e]) => { visible = e.isIntersecting; visible ? start() : stop(); }).observe(canvas);
  }
  start();
})();
