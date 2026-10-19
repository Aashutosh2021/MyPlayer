const OWNER = "Aashutosh2021";
const REPO = "MyPlayer";
const LATEST_RELEASE_URL = "https://github.com/Aashutosh2021/MyPlayer/releases/tag/v4.3";
const RELEASE_API = `https://api.github.com/repos/${OWNER}/${REPO}/releases/latest`;
const ALL_RELEASES_API = `https://api.github.com/repos/${OWNER}/${REPO}/releases`;

const $ = (selector) => document.querySelector(selector);

function formatDate(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric"
  }).format(new Date(iso));
}

function showToast(message) {
  const toast = $("#toast");
  if (!toast) return;
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => toast.classList.remove("show"), 3500);
}

function setDownloadLink(url, version) {
  const btn = $("#downloadBtn");
  const hero = $("#heroDownload");

  const targetUrl = url || LATEST_RELEASE_URL;

  if (btn) {
    btn.href = targetUrl;
    btn.setAttribute("aria-disabled", "false");
    btn.target = "_blank";
    btn.rel = "noopener";
    btn.onclick = () => true;
  }

  if (hero) {
    hero.href = targetUrl;
    hero.target = "_blank";
    hero.rel = "noopener";
  }

  const versionTitle = $("#versionTitle");
  if (versionTitle) {
    versionTitle.textContent = `MyPlayer ${version}`;
  }
}

async function loadLatestRelease() {
  const releaseText = $("#releaseText");
  const releaseMeta = $("#releaseMeta");

  if (!releaseText && !releaseMeta) {
    return; // Not on the home page
  }

  try {
    const response = await fetch(RELEASE_API, {
      headers: { Accept: "application/vnd.github+json" }
    });

    if (!response.ok) {
      throw new Error(`GitHub API returned ${response.status}`);
    }

    const release = await response.json();

    const apkAssets = (release.assets || []).filter(asset =>
      asset.name.toLowerCase().endsWith(".apk")
    );

    const version = release.tag_name || release.name || "v4.3";
    const date = formatDate(release.published_at);

    if (!apkAssets.length) {
      const releaseUrl = release.html_url || LATEST_RELEASE_URL;
      setDownloadLink(releaseUrl, version);
      if (releaseText) {
        releaseText.textContent = `${version} • Available on GitHub`;
      }
      if (releaseMeta) {
        releaseMeta.textContent = `${date ? `Published ${date} • ` : ""}Official package available on GitHub Releases`;
      }
      const releaseBtn = $("#releaseBtn");
      if (releaseBtn) {
        releaseBtn.href = releaseUrl;
      }
      return;
    }

    const apk =
      apkAssets.find(a => /universal/i.test(a.name)) ||
      apkAssets.find(a => /release/i.test(a.name)) ||
      apkAssets[0];

    setDownloadLink(apk.browser_download_url, version);

    if (releaseText) {
      releaseText.textContent = `${version} • ${apk.name}`;
    }
    if (releaseMeta) {
      releaseMeta.textContent = `${date ? `Published ${date} • ` : ""}${apk.name} • ${release.body ? "Release notes available on GitHub" : "Latest release"}`;
    }

    const releaseBtn = $("#releaseBtn");
    if (releaseBtn) {
      releaseBtn.href = release.html_url || LATEST_RELEASE_URL;
    }
  } catch (error) {
    console.error(error);
    if (releaseText) {
      releaseText.textContent = "v4.3 • Available on GitHub";
    }
    if (releaseMeta) {
      releaseMeta.textContent = "Download the latest official v4.3 APK directly from GitHub Releases.";
    }
    const versionTitle = $("#versionTitle");
    if (versionTitle) {
      versionTitle.textContent = "MyPlayer v4.3";
    }
    const heroDownload = $("#heroDownload");
    if (heroDownload) {
      heroDownload.href = LATEST_RELEASE_URL;
      heroDownload.target = "_blank";
      heroDownload.rel = "noopener";
    }
    const downloadBtn = $("#downloadBtn");
    if (downloadBtn) {
      downloadBtn.href = LATEST_RELEASE_URL;
      downloadBtn.target = "_blank";
      downloadBtn.rel = "noopener";
      downloadBtn.removeAttribute("aria-disabled");
    }
    const releaseBtn = $("#releaseBtn");
    if (releaseBtn) {
      releaseBtn.href = LATEST_RELEASE_URL;
    }
  }
}

// Releases Page Filter Search
function initReleasesFilter() {
  const searchInput = $("#releaseSearchInput");
  const releaseCards = document.querySelectorAll(".release-card");
  const noFound = $("#noReleasesFound");
  const resetBtn = $("#resetSearchBtn");
  const countBadge = $("#releaseCountBadge");

  if (!searchInput || !releaseCards.length) return;

  function filterReleases() {
    const query = searchInput.value.trim().toLowerCase();
    let visibleCount = 0;

    releaseCards.forEach(card => {
      const text = card.textContent.toLowerCase();
      const tags = (card.getAttribute("data-tags") || "").toLowerCase();
      const version = (card.getAttribute("data-version") || "").toLowerCase();

      if (!query || text.includes(query) || tags.includes(query) || version.includes(query)) {
        card.style.display = "";
        visibleCount++;
      } else {
        card.style.display = "none";
      }
    });

    if (countBadge) {
      countBadge.textContent = `${visibleCount} of ${releaseCards.length} Releases`;
    }

    if (noFound) {
      noFound.style.display = visibleCount === 0 ? "block" : "none";
    }
  }

  searchInput.addEventListener("input", filterReleases);

  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      searchInput.value = "";
      filterReleases();
      searchInput.focus();
    });
  }
}

// Theme Toggle (Day & Night Mode matching app)
function initThemeToggle() {
  const toggleBtn = $("#themeToggleBtn");
  const storedTheme = localStorage.getItem("myplayer_theme");

  if (storedTheme === "light") {
    document.documentElement.setAttribute("data-theme", "light");
  } else {
    document.documentElement.removeAttribute("data-theme");
  }

  if (!toggleBtn) return;

  toggleBtn.addEventListener("click", () => {
    const isLight = document.documentElement.getAttribute("data-theme") === "light";
    if (isLight) {
      document.documentElement.removeAttribute("data-theme");
      localStorage.setItem("myplayer_theme", "dark");
      showToast("Switched to Night Theme");
    } else {
      document.documentElement.setAttribute("data-theme", "light");
      localStorage.setItem("myplayer_theme", "light");
      showToast("Switched to Day Theme");
    }
  });
}

// Mobile navigation toggle
const menuBtn = $(".menu-btn");
const nav = $(".nav");

if (menuBtn && nav) {
  menuBtn.addEventListener("click", () => {
    const open = nav.classList.toggle("open");
    menuBtn.setAttribute("aria-expanded", String(open));
  });

  document.querySelectorAll(".nav a").forEach(link => {
    link.addEventListener("click", () => nav.classList.remove("open"));
  });
}

initThemeToggle();
loadLatestRelease();
initReleasesFilter();