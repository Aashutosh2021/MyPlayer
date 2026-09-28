const OWNER = "Aashutosh2021";
const REPO = "MyPlayer";
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

  if (btn) {
    btn.href = url;
    btn.setAttribute("aria-disabled", "false");
    btn.onclick = () => {
      if (!url) {
        showToast("APK download is not available right now.");
        return false;
      }
      return true;
    };
  }

  if (hero) {
    hero.href = url;
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

    if (!apkAssets.length) {
      throw new Error("No APK attached to the latest release.");
    }

    const apk =
      apkAssets.find(a => /universal/i.test(a.name)) ||
      apkAssets.find(a => /release/i.test(a.name)) ||
      apkAssets[0];

    const version = release.tag_name || release.name || "Latest";
    const date = formatDate(release.published_at);

    setDownloadLink(apk.browser_download_url, version);

    if (releaseText) {
      releaseText.textContent = `${version} • ${apk.name}`;
    }
    if (releaseMeta) {
      releaseMeta.textContent = `${date ? `Published ${date} • ` : ""}${apk.name} • ${release.body ? "Release notes available on GitHub" : "Latest release"}`;
    }

    const releaseBtn = $("#releaseBtn");
    if (releaseBtn) {
      releaseBtn.href = release.html_url || `https://github.com/${OWNER}/${REPO}/releases`;
    }
  } catch (error) {
    console.error(error);
    if (releaseText) {
      releaseText.textContent = "Latest release could not be loaded";
    }
    if (releaseMeta) {
      releaseMeta.textContent = "Please open GitHub Releases to download the current APK.";
    }
    const versionTitle = $("#versionTitle");
    if (versionTitle) {
      versionTitle.textContent = "Latest MyPlayer Release";
    }
    const heroDownload = $("#heroDownload");
    if (heroDownload) {
      heroDownload.href = `https://github.com/${OWNER}/${REPO}/releases`;
    }
    const downloadBtn = $("#downloadBtn");
    if (downloadBtn) {
      downloadBtn.href = `https://github.com/${OWNER}/${REPO}/releases`;
      downloadBtn.removeAttribute("aria-disabled");
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

loadLatestRelease();
initReleasesFilter();