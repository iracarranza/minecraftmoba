
(function () {
  const KEY = "minecraft-moba-design-imports-v1";
  const PAGE_TYPES = {
    "objectives.html": ["objective"],
    "map.html": ["map_archetype"],
    "mechanics.html": ["mechanic"],
    "progression.html": ["item", "progression"],
    "balance.html": ["balance_note"],
    "inbox.html": ["idea"]
  };

  const page = document.body.dataset.page || "";
  const types = PAGE_TYPES[page];
  if (!types) return;

  let store = {};
  try { store = JSON.parse(localStorage.getItem(KEY) || "{}"); } catch (_) {}

  const records = [];
  types.forEach(type => {
    Object.values(store[type] || {}).forEach(r => records.push({ ...r, _type: type }));
  });
  if (!records.length) return;

  records.sort((a,b) => String(a.name || "").localeCompare(String(b.name || "")));

  const section = document.createElement("section");
  section.className = "imported-designs-section";
  section.innerHTML = `
    <div class="page-title-row imported-title-row">
      <div>
        <div class="kicker">Local imports</div>
        <h2>Imported designs</h2>
        <p class="muted">Added through Import Design and stored locally in this browser.</p>
      </div>
      <a class="button-link" href="import.html">Import another</a>
    </div>
    <div class="imported-design-list"></div>`;

  const list = section.querySelector(".imported-design-list");

  records.forEach(record => {
    const card = document.createElement("article");
    card.className = "panel imported-design-card";
    card.innerHTML = `
      <div class="reader-actions imported-record-actions">
        <span class="badge">${String(record._type).replaceAll("_"," ")}</span>
        <button type="button" data-remove>Remove local import</button>
      </div>
      <div>${record.html || ""}</div>`;
    card.querySelector("[data-remove]").addEventListener("click", () => {
      if (!confirm(`Remove “${record.name}” from this browser?`)) return;
      try {
        const current = JSON.parse(localStorage.getItem(KEY) || "{}");
        if (current[record._type]) {
          delete current[record._type][record.id];
          localStorage.setItem(KEY, JSON.stringify(current));
        }
      } catch (_) {}
      card.remove();
      if (!list.children.length) section.remove();
    });
    list.appendChild(card);
  });

  const main = document.querySelector("main");
  const footer = document.querySelector(".footer-note");
  if (footer) footer.before(section);
  else main.appendChild(section);
})();
