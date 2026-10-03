/**
 * Silouder GitHub Pages - Interactive Tactical Controller
 */

// GitHub Repository Configuration
const DEFAULT_REPO_OWNER = "soms3r";
const DEFAULT_REPO_NAME = "silouder";
const FALLBACK_VERSION = "v1.0.0";
const FALLBACK_APK_NAME = "silouder_release_v1.apk";
const FALLBACK_APK_SIZE = "11.3 MB";

function getRepoConfig() {
  const host = window.location.hostname;
  const pathParts = window.location.pathname.split("/").filter(Boolean);

  if (host.endsWith("github.io") && pathParts.length > 0) {
    const owner = host.split(".")[0];
    const repo = pathParts[0];
    return { owner, repo };
  }

  return { owner: DEFAULT_REPO_OWNER, repo: DEFAULT_REPO_NAME };
}

document.addEventListener("DOMContentLoaded", () => {
  const { owner, repo } = getRepoConfig();

  // 1. Initialize Dynamic & Automatic Release Resolution
  initReleaseController(owner, repo);

  // 2. Initialize Animated Tactical Mesh Canvas Texture
  initTacticalMeshCanvas();

  // 3. Initialize F-Droid Coming Soon Handlers
  initFdroidHandlers();

  // 4. Simulated Tactical Wire Packet Stream in HUD
  initSimulatedPacketStream();
});

/**
 * Automatically resolves the latest production release from GitHub Releases API.
 * Ensures the website always distributes the newest APK without manual HTML edits.
 */
function initReleaseController(owner, repo) {
  const repoBaseUrl = `https://github.com/${owner}/${repo}`;
  const releaseBaseUrl = `${repoBaseUrl}/releases`;
  const fallbackApkUrl = `${releaseBaseUrl}/download/${FALLBACK_VERSION}/${FALLBACK_APK_NAME}`;

  // Apply default repository URLs immediately
  document.querySelectorAll("[data-repo-link]").forEach((el) => {
    el.href = repoBaseUrl;
  });

  document.querySelectorAll("[data-release-link]").forEach((el) => {
    el.href = releaseBaseUrl;
  });

  // Set initial fallback download URLs
  document.querySelectorAll("[data-download-apk]").forEach((el) => {
    el.href = fallbackApkUrl;
  });

  // Query GitHub REST API for the latest published release
  const apiUrl = `https://api.github.com/repos/${owner}/${repo}/releases/latest`;

  fetch(apiUrl)
    .then((res) => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return res.json();
    })
    .then((release) => {
      if (!release || !release.assets) return;

      const tagName = release.tag_name || FALLBACK_VERSION;
      
      // Locate the primary APK asset in the release
      const apkAsset = release.assets.find((a) =>
        a.name && a.name.toLowerCase().endsWith(".apk")
      );

      if (apkAsset) {
        const downloadUrl = apkAsset.browser_download_url;
        const apkName = apkAsset.name;
        const sizeMb = (apkAsset.size / (1024 * 1024)).toFixed(1) + " MB";

        // Dynamically update all download links
        document.querySelectorAll("[data-download-apk]").forEach((el) => {
          el.href = downloadUrl;
        });

        // Update release version badges
        document.querySelectorAll("[data-release-version]").forEach((el) => {
          el.textContent = tagName;
        });

        // Update package sizes
        document.querySelectorAll("[data-apk-size]").forEach((el) => {
          el.textContent = `~${sizeMb}`;
        });

        // Update asset filenames in sideload guide
        document.querySelectorAll("[data-apk-filename]").forEach((el) => {
          el.textContent = apkName;
        });

        // Update hero buttons & badges
        document.querySelectorAll("[data-hero-apk-text]").forEach((el) => {
          el.textContent = `Download APK (${tagName})`;
        });

        document.querySelectorAll("[data-hero-apk-badge]").forEach((el) => {
          el.textContent = `✓ APK Size: ${sizeMb} (Signed)`;
        });

        // Update download section labels
        document.querySelectorAll("[data-apk-btn-text]").forEach((el) => {
          el.textContent = `Direct Download: ${apkName}`;
        });

        document.querySelectorAll("[data-apk-sub-text]").forEach((el) => {
          el.innerHTML = `Production Signed &bull; SHA-256 Verified &bull; ${sizeMb}`;
        });

        console.log(`[Silouder] Auto-resolved latest release: ${tagName} (${apkName}, ${sizeMb})`);
      }
    })
    .catch((err) => {
      console.warn("[Silouder] Using cached release fallback:", err.message);
    });
}

/**
 * Handles F-Droid "Coming Soon" interactions & tactical notification toast
 */
function initFdroidHandlers() {
  document.querySelectorAll("[data-fdroid-trigger]").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      
      const targetCard = document.getElementById("fdroid-info");
      if (targetCard) {
        targetCard.scrollIntoView({ behavior: "smooth", block: "center" });
        targetCard.classList.remove("highlight-pulse");
        // Trigger DOM reflow for CSS animation restart
        void targetCard.offsetWidth;
        targetCard.classList.add("highlight-pulse");
      }

      showTacticalToast(
        "F-DROID PACKAGING IN PROGRESS",
        "Silouder has been submitted to the official F-Droid catalog and is currently in build review. You can install the verified release APK right now above!"
      );
    });
  });
}

/**
 * Renders a tactical HUD toast notification
 */
function showTacticalToast(title, message) {
  const existing = document.querySelector(".tactical-toast");
  if (existing) existing.remove();

  const toast = document.createElement("div");
  toast.className = "tactical-toast";
  toast.innerHTML = `
    <svg class="tactical-toast-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="10"></circle>
      <line x1="12" y1="16" x2="12" y2="12"></line>
      <line x1="12" y1="8" x2="12.01" y2="8"></line>
    </svg>
    <div class="tactical-toast-content">
      <span class="tactical-toast-title">${title}</span>
      <span class="tactical-toast-msg">${message}</span>
    </div>
    <button class="tactical-toast-close" aria-label="Close">&times;</button>
  `;

  document.body.appendChild(toast);

  const closeBtn = toast.querySelector(".tactical-toast-close");
  closeBtn.addEventListener("click", () => toast.remove());

  setTimeout(() => {
    if (toast.parentNode) {
      toast.style.opacity = "0";
      toast.style.transform = "translateY(20px)";
      toast.style.transition = "all 0.3s ease";
      setTimeout(() => toast.remove(), 300);
    }
  }, 6500);
}

/**
 * High-Performance Tactical Mesh Canvas Background Texture
 * Renders floating mesh nodes, P2P packet burst pings, and interactive proximity threads.
 */
function initTacticalMeshCanvas() {
  const canvas = document.getElementById("tactical-mesh-canvas");
  if (!canvas) return;

  // Check user preference for reduced motion
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    return;
  }

  const ctx = canvas.getContext("2d");
  let width = 0;
  let height = 0;
  let dpr = window.devicePixelRatio || 1;
  let animationFrameId = null;
  let isTabVisible = true;

  // Tactical Mesh Nodes
  let nodes = [];
  const nodeCount = window.innerWidth < 768 ? 32 : 55;
  const connectionDistance = 135;

  // Active packet bursts traveling along mesh links
  let packets = [];

  // Mouse / Pointer interaction
  const pointer = { x: -1000, y: -1000, active: false };

  function resize() {
    dpr = window.devicePixelRatio || 1;
    width = window.innerWidth;
    height = window.innerHeight;

    canvas.width = width * dpr;
    canvas.height = height * dpr;
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;

    ctx.scale(dpr, dpr);
  }

  window.addEventListener("resize", resize, { passive: true });
  resize();

  // Create initial tactical peer nodes
  const colors = [
    "rgba(0, 240, 255, 0.45)",   // Cyber cyan
    "rgba(16, 185, 129, 0.45)",  // Tactical emerald
    "rgba(192, 132, 252, 0.40)", // Tor purple
    "rgba(56, 189, 248, 0.45)"   // LoRa blue
  ];

  for (let i = 0; i < nodeCount; i++) {
    nodes.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.45,
      vy: (Math.random() - 0.5) * 0.45,
      radius: Math.random() * 1.5 + 1.2,
      color: colors[Math.floor(Math.random() * colors.length)],
      isHub: Math.random() > 0.85
    });
  }

  // Pointer event listeners
  window.addEventListener("pointermove", (e) => {
    pointer.x = e.clientX;
    pointer.y = e.clientY;
    pointer.active = true;
  }, { passive: true });

  window.addEventListener("pointerleave", () => {
    pointer.active = false;
  }, { passive: true });

  // Spawn periodic mesh packet burst pings
  setInterval(() => {
    if (!isTabVisible || nodes.length < 2) return;

    // Pick a random node that has a neighbor within range
    const srcIndex = Math.floor(Math.random() * nodes.length);
    const srcNode = nodes[srcIndex];

    for (let j = 0; j < nodes.length; j++) {
      if (srcIndex === j) continue;
      const targetNode = nodes[j];
      const dx = targetNode.x - srcNode.x;
      const dy = targetNode.y - srcNode.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      if (dist < connectionDistance) {
        packets.push({
          x: srcNode.x,
          y: srcNode.y,
          tx: targetNode.x,
          ty: targetNode.y,
          progress: 0,
          speed: 0.02 + Math.random() * 0.02,
          color: srcNode.color
        });
        break;
      }
    }
  }, 900);

  // Animation Loop
  function draw() {
    if (!isTabVisible) return;

    ctx.clearRect(0, 0, width, height);

    // 1. Update and draw nodes
    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i];

      node.x += node.vx;
      node.y += node.vy;

      // Soft bounce on screen edges
      if (node.x < 0 || node.x > width) node.vx *= -1;
      if (node.y < 0 || node.y > height) node.vy *= -1;

      // Draw node circle
      ctx.beginPath();
      ctx.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
      ctx.fillStyle = node.color;
      ctx.fill();

      // Hub nodes get an extra soft pulse halo
      if (node.isHub) {
        ctx.beginPath();
        ctx.arc(node.x, node.y, node.radius * 2.5, 0, Math.PI * 2);
        ctx.fillStyle = "rgba(0, 240, 255, 0.08)";
        ctx.fill();
      }
    }

    // 2. Connect proximity peers with mesh lines
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const dx = nodes[i].x - nodes[j].x;
        const dy = nodes[i].y - nodes[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < connectionDistance) {
          const alpha = (1 - dist / connectionDistance) * 0.22;
          ctx.beginPath();
          ctx.moveTo(nodes[i].x, nodes[i].y);
          ctx.lineTo(nodes[j].x, nodes[j].y);
          ctx.strokeStyle = `rgba(0, 240, 255, ${alpha})`;
          ctx.lineWidth = 0.8;
          ctx.stroke();
        }
      }

      // Proximity to user pointer
      if (pointer.active) {
        const pdx = nodes[i].x - pointer.x;
        const pdy = nodes[i].y - pointer.y;
        const pdist = Math.sqrt(pdx * pdx + pdy * pdy);

        if (pdist < 150) {
          const palpha = (1 - pdist / 150) * 0.35;
          ctx.beginPath();
          ctx.moveTo(nodes[i].x, nodes[i].y);
          ctx.lineTo(pointer.x, pointer.y);
          ctx.strokeStyle = `rgba(56, 189, 248, ${palpha})`;
          ctx.lineWidth = 1;
          ctx.stroke();
        }
      }
    }

    // 3. Render traveling packet pings
    for (let p = packets.length - 1; p >= 0; p--) {
      const pkt = packets[p];
      pkt.progress += pkt.speed;

      if (pkt.progress >= 1) {
        packets.splice(p, 1);
        continue;
      }

      const currX = pkt.x + (pkt.tx - pkt.x) * pkt.progress;
      const currY = pkt.y + (pkt.ty - pkt.y) * pkt.progress;

      ctx.beginPath();
      ctx.arc(currX, currY, 2.2, 0, Math.PI * 2);
      ctx.fillStyle = "#FFFFFF";
      ctx.shadowColor = "#00F0FF";
      ctx.shadowBlur = 8;
      ctx.fill();
      ctx.shadowBlur = 0; // Reset
    }

    animationFrameId = requestAnimationFrame(draw);
  }

  draw();

  // Conserve battery when user switches tabs
  document.addEventListener("visibilitychange", () => {
    isTabVisible = !document.hidden;
    if (isTabVisible) {
      draw();
    } else if (animationFrameId) {
      cancelAnimationFrame(animationFrameId);
    }
  });
}

/**
 * Simulates real-time multi-transport packet inspection in the device mockup
 */
function initSimulatedPacketStream() {
  const wireList = document.getElementById("mockup-wire-list");
  if (!wireList) return;

  const sampleNodes = ["!7a9f", "!e4b2", "!f01c", "!33da", "!99e1", "!b447"];
  const sampleTransports = [
    { name: "BT_SYNC", color: "#00F0FF", snr: "12.4dB" },
    { name: "LORA_MESH", color: "#38BDF8", snr: "8.6dB" },
    { name: "LAN_SOCKET", color: "#4ADE80", snr: "99.0dB" },
    { name: "TOR_CIRCUIT", color: "#C084FC", snr: "SECURE" }
  ];

  const payloads = [
    "VECTOR_GOSSIP_REQ",
    "ENVELOPE_AES256",
    "PEER_DISCOVERY_PING",
    "OUTBOX_FLUSH_ACK",
    "CHANNEL_BROADCAST",
    "ROUTING_HEARTBEAT"
  ];

  function addPacket() {
    const from = sampleNodes[Math.floor(Math.random() * sampleNodes.length)];
    let to = sampleNodes[Math.floor(Math.random() * sampleNodes.length)];
    while (to === from) {
      to = sampleNodes[Math.floor(Math.random() * sampleNodes.length)];
    }

    const transport = sampleTransports[Math.floor(Math.random() * sampleTransports.length)];
    const payload = payloads[Math.floor(Math.random() * payloads.length)];
    const isTx = Math.random() > 0.5;

    const row = document.createElement("div");
    row.className = "wire-item";
    row.style.animation = "fadeIn 0.3s ease";
    row.innerHTML = `
      <div>
        <span class="wire-direction" style="background: ${isTx ? '#F59E0B' : '#00F0FF'}"></span>
        <span style="color: #F1F5F9; font-weight: bold;">${from} &rarr; ${to}</span>
        <span style="color: #94A3B8; margin-left: 6px;">${payload}</span>
      </div>
      <div style="color: ${transport.color}; font-weight: 700;">
        ${transport.snr}
      </div>
    `;

    wireList.insertBefore(row, wireList.firstChild);

    // Keep wire stream to max 4 items
    while (wireList.children.length > 4) {
      wireList.removeChild(wireList.lastChild);
    }
  }

  // Periodic updates
  setInterval(addPacket, 3200);
}

// Copy Helper for code snippets
function copyCode(elementId, btn) {
  const el = document.getElementById(elementId);
  if (!el) return;

  navigator.clipboard.writeText(el.innerText).then(() => {
    const originalText = btn.innerText;
    btn.innerText = "COPIED!";
    btn.style.color = "#10B981";
    setTimeout(() => {
      btn.innerText = originalText;
      btn.style.color = "";
    }, 2000);
  });
}
