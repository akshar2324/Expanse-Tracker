// AkSpend site: scroll reveals, the app style switch, cards that lean towards the pointer,
// and the "try it" demos (bank SMS, quick add, month-end forecast, the hero phone).
// Everything here is optional: without it the page shows each demo in its starting state.

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

  // ---------- shared helpers for the demos ----------
  const $ = (id) => document.getElementById(id);
  const rupees = (n) => "₹" + n.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const whole = (n) => "₹" + n.toLocaleString("en-IN", { maximumFractionDigits: 0 });
  const ICONS = { Food: "🍴", Grocery: "🛒", Transportation: "🚌", Shopping: "🛍️", Other: "🛒" };
  function pressOne(buttons, chosen) { buttons.forEach((b) => b.setAttribute("aria-pressed", String(b === chosen))); }
  function flash(el) { el.classList.remove("flash"); void el.offsetWidth; el.classList.add("flash"); }
  function row(name, sub, amount, income) {
    const li = document.createElement("li");
    li.className = "new";
    const ic = document.createElement("span"); ic.className = "tx-ic"; ic.textContent = income ? "↙" : (ICONS[sub.split(" · ")[0]] || "🛒");
    const nm = document.createElement("span"); nm.className = "tx-name"; nm.textContent = name;
    const sm = document.createElement("small"); sm.textContent = sub; nm.appendChild(sm);
    const b = document.createElement("b"); b.textContent = (income ? "+" : "−") + rupees(amount); if (income) b.className = "pos";
    li.append(ic, nm, b);
    return li;
  }

  // ---------- demo 1: bank SMS ----------
  // Sample messages from the app's own tests, with the result the app gives for each.
  const SMS = [
    { text: "Sent Rs.232.00 From HDFC Bank A/C *4821 To KUMAR GENERAL STORE On 14/10/26 Ref 629100004321", amount: 232, name: "KUMAR GENERAL STORE", meta: "14 Oct 2026 · Other · A/c ending 4821" },
    { text: "Sent Rs.99 From HDFC Bank A/C *1234 To SWIGGY On 05/01/26 Ref 401234567890", amount: 99, name: "SWIGGY", meta: "5 Jan 2026 · Food · A/c ending 1234" },
    { text: "Rs.1,500.50 credited to HDFC Bank A/c XX4821 on 05-01-26 from VPA friend@okaxis (UPI 401234567890)", amount: 1500.5, income: true, name: "friend@okaxis", meta: "5 Jan 2026 · Other · A/c ending 4821" },
    { text: "123456 is your OTP for a transaction of Rs.500 at AMAZON. Do not share.", skip: "This looks like a one-time password, not a payment." },
    { text: "Your UPI payment of Rs.250.00 to SWIGGY has failed. Money if debited will be refunded.", skip: "This payment failed or was declined, so no money moved." },
  ];
  const smsButtons = document.querySelectorAll("[data-sms]");
  if (smsButtons.length) {
    const result = $("sms-result"), save = $("sms-save");
    const showSms = (i) => {
      const m = SMS[i];
      $("sms-text").textContent = m.text;
      result.classList.toggle("skip", !!m.skip);
      $("sms-kind").textContent = m.skip ? "Nothing to add" : m.income ? "Income found" : "Expense found";
      $("sms-amount").textContent = m.skip ? "" : (m.income ? "+" : "−") + rupees(m.amount);
      $("sms-amount").className = "app-amount " + (m.income ? "pos" : "neg");
      $("sms-name").textContent = m.name || "";
      $("sms-meta").textContent = m.skip || m.meta;
      save.textContent = "Review and save"; save.disabled = false;
      flash(result);
    };
    smsButtons.forEach((b) => b.addEventListener("click", () => { pressOne(smsButtons, b); showSms(+b.dataset.sms); }));
    save.addEventListener("click", () => { save.textContent = "Saved to Activity ✓"; save.disabled = true; });
  }

  // ---------- demo 2: sums in the amount field ----------
  // A small calculator for + - * / and brackets. Returns null for anything else.
  function sum(text) {
    const t = text.replace(/\s+/g, "").replace(/[x×]/gi, "*").replace(/÷/g, "/");
    if (!t || !/^[\d.+\-*/()]+$/.test(t)) return null;
    let i = 0;
    const num = () => {
      if (t[i] === "(") { i++; const v = expr(); if (t[i] !== ")") throw 0; i++; return v; }
      if (t[i] === "-") { i++; return -num(); }
      const m = /^\d*\.?\d+|^\d+\.?/.exec(t.slice(i));
      if (!m) throw 0;
      i += m[0].length; return parseFloat(m[0]);
    };
    const term = () => { let v = num(); while (t[i] === "*" || t[i] === "/") { const op = t[i++], r = num(); v = op === "*" ? v * r : v / r; } return v; };
    const expr = () => { let v = term(); while (t[i] === "+" || t[i] === "-") { const op = t[i++], r = term(); v = op === "+" ? v + r : v - r; } return v; };
    try { const v = expr(); return i === t.length && isFinite(v) ? Math.round(v * 100) / 100 : null; } catch (e) { return null; }
  }
  const addInput = $("add-input");
  if (addInput) {
    const out = $("add-out"), list = $("add-list"), saveAdd = $("add-save"), cats = document.querySelectorAll("[data-cat]");
    let category = "Food", value = 45;
    const refresh = () => {
      value = sum(addInput.value);
      const ok = value !== null && value > 0;
      out.innerHTML = ok ? "= <strong></strong>" : "Type an amount or a sum, like <code>120/3+5</code>";
      if (ok) out.querySelector("strong").textContent = rupees(value);
      saveAdd.disabled = !ok;
    };
    addInput.addEventListener("input", refresh);
    cats.forEach((b) => b.addEventListener("click", () => { pressOne(cats, b); category = b.dataset.cat; }));
    saveAdd.addEventListener("click", () => {
      if (value === null || value <= 0) return;
      list.prepend(row(category === "Transportation" ? "Transport" : category, category + " · just now", value, false));
      while (list.children.length > 3) list.lastElementChild.remove();
    });
    addInput.addEventListener("keydown", (e) => { if (e.key === "Enter") saveAdd.click(); });
    refresh();
  }

  // ---------- demo 3: month-end forecast ----------
  // The app's sum: balance now, minus bills due, minus the daily rate for each day left.
  const fcDaily = $("fc-daily");
  if (fcDaily) {
    const fcBills = $("fc-bills-in"), card = document.querySelector(".forecast");
    const today = new Date(), end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const daysLeft = end.getDate() - today.getDate() + 1;
    const endLabel = end.getDate() + " " + end.toLocaleString("en-GB", { month: "short" });
    const BALANCE = 42000;
    const refresh = () => {
      const daily = +fcDaily.value, bills = +fcBills.value, variable = daily * daysLeft, total = BALANCE - bills - variable;
      $("fc-daily-out").textContent = whole(daily) + " a day";
      $("fc-bills-out").textContent = whole(bills);
      $("fc-bills").textContent = (bills ? "−" : "") + rupees(bills);
      $("fc-days").textContent = "Everyday spending, " + daysLeft + (daysLeft === 1 ? " day" : " days") + " left";
      $("fc-var").textContent = "−" + rupees(variable);
      $("fc-on").textContent = "Projected on " + endLabel;
      $("fc-total").textContent = rupees(total);
      $("fc-head").textContent = "About " + whole(Math.round(total / 10) * 10) + " left on " + endLabel;
    };
    [fcDaily, fcBills].forEach((el) => el.addEventListener("input", refresh));
    [fcDaily, fcBills].forEach((el) => el.addEventListener("change", () => flash(card)));
    refresh();
  }

  // ---------- airplane mode ----------
  const airplane = $("airplane");
  if (airplane) {
    const text = $("airplane-text"), off = text.textContent;
    airplane.addEventListener("change", () => {
      root.classList.toggle("airplane-on", airplane.checked);
      text.textContent = airplane.checked
        ? "Airplane mode on. The demos on this page still work, and so does the app: logging, bank messages, forecasts and backups all happen on the phone."
        : off;
    });
  }

  // ---------- hero phone: leans to the pointer and plays the auto-import story ----------
  const phone = $("hero-phone");
  if (!phone) return;
  const art = phone.closest(".hero-art");
  if (!reduced.matches && window.matchMedia("(hover: hover) and (pointer: fine)").matches) {
    let raf = 0;
    art.addEventListener("pointermove", (e) => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        const r = art.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
        phone.classList.add("is-tilting");
        phone.style.setProperty("--ry", (-14 + x * 22).toFixed(2) + "deg");
        phone.style.setProperty("--rx", (4 - y * 12).toFixed(2) + "deg");
      });
    });
    art.addEventListener("pointerleave", () => {
      cancelAnimationFrame(raf); phone.classList.remove("is-tilting");
      phone.style.removeProperty("--rx"); phone.style.removeProperty("--ry");
    });
  }
  if (reduced.matches) return; // the phone keeps its finished state

  const note = $("hp-note"), review = $("hp-review"), list = $("hp-list"), btn = $("hp-review-btn");
  const STORY = [
    { from: "HDFC Bank", text: "Sent Rs.232.00 From HDFC Bank A/C *4821 To KUMAR GENERAL STORE", amount: 232, name: "KUMAR GENERAL STORE", sub: "Other · A/c 4821" },
    { from: "HDFC Bank", text: "Sent Rs.99 From HDFC Bank A/C *1234 To SWIGGY", amount: 99, name: "SWIGGY", sub: "Food · A/c 1234" },
    { from: "HDFC Bank", text: "123456 is your OTP for a transaction of Rs.500 at AMAZON. Do not share.", skip: "One-time password. Ignored." },
  ];
  let run = 0, balance = 24000, spent = 0;
  const paint = () => { $("hp-balance").textContent = rupees(balance); $("hp-spent").textContent = rupees(spent); };
  function count(by) {
    const from = balance, t0 = performance.now(), mine = run;
    balance -= by; spent += by;
    const startSpent = spent - by;
    const step = (now) => {
      if (mine !== run) return;
      const k = Math.min(1, (now - t0) / 600), e = 1 - Math.pow(1 - k, 3);
      $("hp-balance").textContent = rupees(from - by * e); $("hp-spent").textContent = rupees(startSpent + by * e);
      if (k < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }
  async function play() {
    const mine = ++run;
    const wait = (ms) => new Promise((ok, stop) => setTimeout(() => (mine === run ? ok() : stop()), ms));
    try {
      for (;;) {
        balance = 24000; spent = 0; paint(); list.replaceChildren(); review.hidden = true; note.classList.remove("show");
        await wait(900);
        for (const m of STORY) {
          $("hp-note-from").textContent = m.from; $("hp-note-text").textContent = m.text;
          note.classList.add("show");
          await wait(1500);
          review.hidden = false; review.classList.toggle("skip", !!m.skip); btn.classList.remove("down");
          $("hp-review-label").textContent = m.skip ? "Nothing to add" : "Expense found";
          $("hp-review-amt").textContent = m.skip ? "" : "−" + rupees(m.amount);
          $("hp-review-name").textContent = m.skip || m.name;
          await wait(m.skip ? 1900 : 1300);
          note.classList.remove("show");
          if (!m.skip) {
            btn.classList.add("down");
            await wait(320);
            list.prepend(row(m.name, m.sub, m.amount, false));
            count(m.amount);
          }
          review.hidden = true;
          await wait(1500);
        }
        await wait(2600);
      }
    } catch (e) { /* stopped: off screen or tab hidden */ }
  }
  const stop = () => { run++; };
  let onScreen = false;
  const sync = () => (onScreen && !document.hidden ? play() : stop());
  if ("IntersectionObserver" in window) {
    new IntersectionObserver(([e]) => { onScreen = e.isIntersecting; sync(); }, { threshold: 0.3 }).observe(phone);
    document.addEventListener("visibilitychange", sync);
  }
})();
