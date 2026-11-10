// AkSpend site: scroll reveals, the app style switch, cards that lean towards the pointer,
// and the loader for the WebGL phone scenes (stage.js). Everything here is optional; the
// page reads the same without it.

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

  // ---------- cards lean towards the pointer ----------
  if (!reduced.matches && window.matchMedia("(hover: hover) and (pointer: fine)").matches) {
    document.querySelectorAll(".tilt").forEach((el) => {
      let raf = 0;
      el.addEventListener("pointermove", (e) => {
        cancelAnimationFrame(raf);
        raf = requestAnimationFrame(() => {
          const r = el.getBoundingClientRect();
          const x = Math.min(1, Math.max(0, (e.clientX - r.left) / r.width));
          const y = Math.min(1, Math.max(0, (e.clientY - r.top) / r.height));
          el.classList.add("is-tilting");
          el.style.setProperty("--ry", ((x - 0.5) * 9).toFixed(2) + "deg");
          el.style.setProperty("--rx", ((0.5 - y) * 9).toFixed(2) + "deg");
          el.style.setProperty("--mx", (x * 100).toFixed(1) + "%");
          el.style.setProperty("--my", (y * 100).toFixed(1) + "%");
        });
      });
      el.addEventListener("pointerleave", () => {
        cancelAnimationFrame(raf);
        el.classList.remove("is-tilting");
        el.style.removeProperty("--rx"); el.style.removeProperty("--ry");
      });
    });
  }

  // ---------- WebGL phone scenes ----------
  // The inline script in <head> adds "gl" when WebGL is there and motion is allowed. Three.js
  // is only fetched in that case; if anything fails the page drops back to the CSS phones.
  if (root.classList.contains("gl")) {
    const fallBack = () => root.classList.remove("gl", "gl-ready");
    import("./stage.js").then((stage) => {
      const stop = stage.start({ onFail: fallBack });
      styleListeners.push(stage.restyle);
      reduced.addEventListener?.("change", () => { if (reduced.matches) { stop(); fallBack(); } });
    }).catch(fallBack);
  }
})();
