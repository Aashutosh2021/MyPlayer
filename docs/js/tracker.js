// ─────────────────────────────────────────────────────────────────────────────
// MyPlayer — Site Tracker (shared by index.html, releases.html)
//
// What it does:
//   1. Logs ONE 'visit' event per browser session (page, geo, device)
//   2. Logs a 'download' event every time a visitor clicks any
//      GitHub releases link (`/releases/tag/...`) — never blocks the click
//
// Geo comes from the visitor's IP via free HTTPS lookups (no permission
// popup, no API key). If every lookup fails the event is still logged,
// just without location fields.
//
// Data lives in Firestore collection `events` — see docs/firestore.rules.
// The super-admin dashboard is docs/admin.html.
// ─────────────────────────────────────────────────────────────────────────────

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getFirestore, collection, addDoc, serverTimestamp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { firebaseConfig } from "./firebase-config.js";

let _db = null;
const GEO_CACHE_KEY = "mp_geo_cache";
const VISIT_KEY = "mp_visit_logged";

function db() {
  if (!_db) {
    const app = initializeApp(firebaseConfig);
    _db = getFirestore(app);
  }
  return _db;
}

// ── IP geolocation (free, keyless, HTTPS) ───────────────────────────────────
async function lookupGeo() {
  // Same-session cache: the IP won't change mid-visit.
  try {
    const cached = sessionStorage.getItem(GEO_CACHE_KEY);
    if (cached) return JSON.parse(cached);
  } catch (_) { /* storage can be blocked — ignore */ }

  const providers = [
    {
      url: "https://ipwho.is/",
      parse: (d) => d && d.success !== false ? {
        ip: d.ip, city: d.city, region: d.region,
        country: d.country, countryCode: d.country_code,
        lat: d.latitude, lon: d.longitude
      } : null
    },
    {
      url: "https://ipapi.co/json/",
      parse: (d) => !d.error ? {
        ip: d.ip, city: d.city, region: d.region,
        country: d.country_name, countryCode: d.country_code,
        lat: d.latitude, lon: d.longitude
      } : null
    }
  ];

  for (const p of providers) {
    try {
      const ctrl = new AbortController();
      const kill = setTimeout(() => ctrl.abort(), 4000);
      const res = await fetch(p.url, { signal: ctrl.signal });
      clearTimeout(kill);
      if (!res.ok) continue;
      const geo = p.parse(await res.json());
      if (geo && geo.ip) {
        try { sessionStorage.setItem(GEO_CACHE_KEY, JSON.stringify(geo)); } catch (_) {}
        return geo;
      }
    } catch (_) { /* try next provider */ }
  }
  return null; // all lookups failed — log without geo
}

// ── Small device hints (no heavy UA parser) ─────────────────────────────────
function deviceHints() {
  const ua = navigator.userAgent || "";
  const platform = (navigator.userAgentData && navigator.userAgentData.platform) || "";
  const os = /Android/i.test(ua) ? "Android"
    : /iPhone|iPad|iPod/i.test(ua) ? "iOS"
    : /Windows/i.test(ua) ? "Windows"
    : /Mac OS X/i.test(ua) ? "macOS"
    : /Linux/i.test(ua) ? "Linux"
    : (platform || "Unknown");
  return {
    device: /Mobi|Android|iPhone|iPad/i.test(ua) ? "Mobile" : "Desktop",
    os,
    ua
  };
}

// ── Core event writer ───────────────────────────────────────────────────────
async function track(type, extra = {}) {
  try {
    const geo = await lookupGeo();
    const payload = {
      type,
      ts: serverTimestamp(),
      page: location.pathname.split("/").pop() || "index.html",
      referrer: document.referrer || "direct",
      lang: navigator.language || "",
      screen: `${screen.width}x${screen.height}`,
      ...deviceHints(),
      ...(geo ? { ...geo } : { ip: null, city: null, region: null, country: null, countryCode: null, lat: null, lon: null }),
      ...extra
    };
    await addDoc(collection(db(), "events"), payload);
  } catch (err) {
    // Tracking must NEVER break the site.
    console.warn("[tracker] event skipped:", err && err.code || err);
  }
}

// ── Download click interception (event delegation) ──────────────────────────
function wireDownloadTracking() {
  document.addEventListener("click", (e) => {
    const a = e.target.closest && e.target.closest('a[href*="/releases/tag/"]');
    if (!a) return;
    const href = a.getAttribute("href") || "";
    const tag = (href.match(/releases\/tag\/([^\/?#]+)/) || [])[1] || "unknown";
    const kind = a.classList.contains("nav-download") ? "navbar"
      : a.classList.contains("primary-btn") ? "hero"
      : a.classList.contains("secondary-btn") ? "card-secondary"
      : "card-primary";
    track("download", {
      version: decodeURIComponent(tag),
      button: kind,
      href
    }); // fire & forget — navigation is never blocked
  }, { passive: true });
}

// ── Public API ──────────────────────────────────────────────────────────────
export function initTracker({ logVisit = true, trackDownloads = true } = {}) {
  if (trackDownloads) wireDownloadTracking();

  if (logVisit) {
    try {
      if (sessionStorage.getItem(VISIT_KEY)) return; // 1 visit per browser session
      sessionStorage.setItem(VISIT_KEY, "1");
    } catch (_) { /* private mode — still log */ }
    track("visit");
  }
}

export { track };
