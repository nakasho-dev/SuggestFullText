const keywordInput = document.getElementById("keyword");
const latInput = document.getElementById("lat");
const lonInput = document.getElementById("lon");
const resultsEl = document.getElementById("results");
const statusEl = document.getElementById("status");

let debounceTimer = null;

function setStatus(text) {
  statusEl.textContent = text;
}

function clearResults() {
  resultsEl.innerHTML = "";
}

function renderResults(items) {
  clearResults();
  if (!items.length) {
    const empty = document.createElement("li");
    empty.className = "empty";
    empty.textContent = "該当する駅が見つかりませんでした。";
    resultsEl.appendChild(empty);
    return;
  }
  items.forEach((item) => {
    const li = document.createElement("li");
    li.className = "result-item";
    const line = document.createElement("div");
    line.className = "result-line";
    line.textContent = `${item.stationName} / ${item.address}`;
    li.appendChild(line);
    resultsEl.appendChild(li);
  });
}

function parseNumber(value) {
  const v = value.trim();
  if (!v) return null;
  const num = Number(v);
  return Number.isFinite(num) ? num : null;
}

async function fetchSuggest() {
  const keyword = keywordInput.value.trim();
  if (!keyword) {
    setStatus("入力待ち");
    clearResults();
    return;
  }

  const lat = parseNumber(latInput.value);
  const lon = parseNumber(lonInput.value);
  const hasGeo = lat !== null && lon !== null;

  const params = new URLSearchParams();
  params.set("keyword", keyword);
  params.set("limit", "10");
  if (hasGeo) {
    params.set("lat", String(lat));
    params.set("lon", String(lon));
  }

  const url = hasGeo
    ? `/api/stations/search-geo?${params.toString()}`
    : `/api/stations/search?${params.toString()}`;

  try {
    setStatus("検索中...");
    const res = await fetch(url);
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || `HTTP ${res.status}`);
    }
    const data = await res.json();
    renderResults(data);
    setStatus(`結果: ${data.length} 件`);
  } catch (err) {
    clearResults();
    setStatus("検索に失敗しました");
    const error = document.createElement("li");
    error.className = "empty";
    error.textContent = err.message || "error";
    resultsEl.appendChild(error);
  }
}

function scheduleSearch() {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  debounceTimer = setTimeout(fetchSuggest, 300);
}

keywordInput.addEventListener("input", scheduleSearch);
latInput.addEventListener("input", scheduleSearch);
lonInput.addEventListener("input", scheduleSearch);
