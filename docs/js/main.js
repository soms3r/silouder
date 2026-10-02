/**
 * Silouder GitHub Pages - Interactive Tactical Controller
 */

// GitHub Repository Configuration
const DEFAULT_REPO_OWNER = "soms3r";
const DEFAULT_REPO_NAME = "silouder";

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
  const repoBaseUrl = `https://github.com/${owner}/${repo}`;
  const releaseBaseUrl = `${repoBaseUrl}/releases`;
  const directApkUrl = `${releaseBaseUrl}/latest/download/app-debug.apk`;

  // Bind dynamic repository URLs
  document.querySelectorAll("[data-repo-link]").forEach((el) => {
    el.href = repoBaseUrl;
  });

  document.querySelectorAll("[data-release-link]").forEach((el) => {
    el.href = releaseBaseUrl;
  });

  document.querySelectorAll("[data-download-apk]").forEach((el) => {
    el.href = directApkUrl;
  });

  // Simulated Tactical Wire Packet Stream in HUD
  initSimulatedPacketStream();
});

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
