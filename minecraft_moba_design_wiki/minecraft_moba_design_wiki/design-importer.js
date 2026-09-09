
(function () {
  const input = document.getElementById("design-import-input");
  const parseBtn = document.getElementById("design-parse");
  const applyBtn = document.getElementById("apply-import");
  const preview = document.getElementById("import-preview");
  const message = document.getElementById("import-message");
  const applyRow = document.getElementById("import-apply-row");
  const destinationLink = document.getElementById("destination-link");
  const clearBtn = document.getElementById("clear-import");
  const exampleBtn = document.getElementById("load-class-example");
  if (!input || !parseBtn || !applyBtn || !preview) return;

  const CLASS_KEY = "minecraft-moba-class-library-v2";
  const IMPORT_KEY = "minecraft-moba-design-imports-v1";
  const CANONICAL_CLASS_IDS = new Set(["mole", "gardener", "golem-master"]);
  const ROUTES = {
    class: "classes.html",
    objective: "objectives.html",
    map_archetype: "map.html",
    mechanic: "mechanics.html",
    item: "progression.html",
    progression: "progression.html",
    balance_note: "balance.html",
    idea: "inbox.html"
  };
  const LABELS = {
    class: "Classes",
    objective: "Objectives",
    map_archetype: "Map / World",
    mechanic: "Unique Mechanics",
    item: "Items / Progression",
    progression: "Items / Progression",
    balance_note: "Balance Notes",
    idea: "Inbox / Ideas"
  };

  let candidate = null;

  function escapeHtml(v) {
    return String(v ?? "").replace(/[&<>"']/g, c => ({
      "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"
    })[c]);
  }

  function safeId(v) {
    return String(v || "").toLowerCase().trim()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "");
  }

  function toArray(v) {
    if (!v) return [];
    if (Array.isArray(v)) return v.map(String).map(x => x.trim()).filter(Boolean);
    return [String(v).trim()].filter(Boolean);
  }

  function stripFence(raw) {
    let text = String(raw || "").trim();
    const match = text.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i);
    return match ? match[1].trim() : text;
  }

  function normalizeAbility(raw, fallback) {
    if (!raw) return { name: `${fallback} — TBD`, description: "", upgrades: [] };
    if (typeof raw === "string") return { name: fallback, description: raw, upgrades: [] };
    return {
      name: String(raw.name || fallback),
      description: String(raw.description || raw.body || ""),
      upgrades: Array.isArray(raw.upgrades) ? raw.upgrades.map((u, i) => {
        if (typeof u === "string") return { name: `Upgrade ${i + 1}`, description: u };
        return {
          name: String((u && u.name) || `Upgrade ${i + 1}`),
          description: String((u && (u.description || u.body)) || "")
        };
      }) : []
    };
  }

  function normalize(raw) {
    if (!raw || typeof raw !== "object" || Array.isArray(raw)) {
      throw new Error("The import must be one JSON object.");
    }

    const type = String(raw.type || "").trim().toLowerCase();
    if (!ROUTES[type]) throw new Error("Unsupported design type.");

    const name = String(raw.name || raw.title || "").trim();
    if (!name) throw new Error("A name is required.");

    const id = safeId(raw.id || name);
    if (!id) throw new Error("A valid id is required.");

    if (type === "class") {
      const archetypes = raw.archetypes && typeof raw.archetypes === "object" ? raw.archetypes : {};
      const secondaryRaw = archetypes.secondary ?? raw.secondary ?? [];
      return {
        type, version: Number(raw.version || 1), id, name,
        status: String(raw.status || "Working draft"),
        archetypes: {
          primary: String(archetypes.primary || raw.primary || "").trim(),
          secondary: Array.isArray(secondaryRaw)
            ? secondaryRaw.map(String).map(x => x.trim()).filter(Boolean)
            : String(secondaryRaw || "").split(",").map(x => x.trim()).filter(Boolean)
        },
        coreIdea: String(raw.coreIdea || raw.core_idea || raw.summary || "").trim(),
        passive: normalizeAbility(raw.passive, "Passive"),
        abilities: Array.isArray(raw.abilities)
          ? raw.abilities.map((a, i) => normalizeAbility(a, `Ability ${i + 1}`))
          : [],
        ultimate: normalizeAbility(raw.ultimate, "Ultimate"),
        buildDirections: toArray(raw.buildDirections || raw.build_directions),
        openQuestions: toArray(raw.openQuestions || raw.open_questions),
        notes: toArray(raw.notes)
      };
    }

    return {
      type, version: Number(raw.version || 1), id, name,
      status: String(raw.status || "Working draft"),
      summary: String(raw.summary || raw.description || "").trim(),
      tags: toArray(raw.tags),
      sections: Array.isArray(raw.sections) ? raw.sections.map(s => {
        if (typeof s === "string") return { heading: "", body: s };
        return {
          heading: String((s && (s.heading || s.title)) || "").trim(),
          body: s ? (s.body ?? s.content ?? "") : ""
        };
      }) : [],
      openQuestions: toArray(raw.openQuestions || raw.open_questions)
    };
  }

  function abilityHtml(a, heading) {
    if (!a) return "";
    const upgrades = a.upgrades && a.upgrades.length
      ? `<h4>Upgrade choices</h4>${a.upgrades.map(u =>
          `<p><strong>${escapeHtml(u.name)}</strong>${u.description ? ` — ${escapeHtml(u.description)}` : ""}</p>`
        ).join("")}`
      : "";
    return `<h3>${escapeHtml(heading)} — ${escapeHtml(a.name)}</h3>
      <p>${escapeHtml(a.description || "TBD")}</p>${upgrades}`;
  }

  function classToHtml(c) {
    const badges = [
      `<span class="badge status-working">${escapeHtml(c.status)}</span>`,
      c.archetypes.primary ? `<span class="badge">Primary: ${escapeHtml(c.archetypes.primary)}</span>` : "",
      ...c.archetypes.secondary.map(x => `<span class="badge">${escapeHtml(x)}</span>`)
    ].join("");

    const builds = c.buildDirections.length
      ? `<h3>Possible build directions</h3><ul>${c.buildDirections.map(x => `<li>${escapeHtml(x)}</li>`).join("")}</ul>`
      : "";
    const questions = c.openQuestions.length
      ? `<h3>Open questions</h3><ul>${c.openQuestions.map(x => `<li>${escapeHtml(x)}</li>`).join("")}</ul>`
      : "";
    const notes = c.notes.length
      ? `<h3>Notes</h3><ul>${c.notes.map(x => `<li>${escapeHtml(x)}</li>`).join("")}</ul>`
      : "";

    return `<h2>${escapeHtml(c.name)}</h2>
      <div class="class-meta-line">${badges}</div>
      <h3>Core idea</h3><p>${escapeHtml(c.coreIdea || "TBD")}</p>
      ${abilityHtml(c.passive, "Passive")}
      ${c.abilities.map((a,i) => abilityHtml(a, `Ability ${i+1}`)).join("")}
      ${abilityHtml(c.ultimate, "Ultimate")}
      ${builds}${questions}${notes}`.trim();
  }

  function genericToHtml(d) {
    const sectionHtml = d.sections.map(s => {
      const heading = s.heading ? `<h3>${escapeHtml(s.heading)}</h3>` : "";
      const body = Array.isArray(s.body)
        ? `<ul>${s.body.map(x => `<li>${escapeHtml(x)}</li>`).join("")}</ul>`
        : `<p>${escapeHtml(String(s.body || ""))}</p>`;
      return heading + body;
    }).join("");

    const questions = d.openQuestions.length
      ? `<h3>Open questions</h3><ul>${d.openQuestions.map(x => `<li>${escapeHtml(x)}</li>`).join("")}</ul>`
      : "";

    return `<h2>${escapeHtml(d.name)}</h2>
      <div class="meta-row">
        <span class="badge status-working">${escapeHtml(d.status)}</span>
        ${d.tags.map(x => `<span class="badge">${escapeHtml(x)}</span>`).join("")}
      </div>
      ${d.summary ? `<p>${escapeHtml(d.summary)}</p>` : ""}
      ${sectionHtml}${questions}`.trim();
  }

  function loadClassState() {
    try {
      const p = JSON.parse(localStorage.getItem(CLASS_KEY) || "{}");
      return { overrides: p.overrides || {}, custom: p.custom || {}, selectedId: p.selectedId || null };
    } catch (_) {
      return { overrides: {}, custom: {}, selectedId: null };
    }
  }

  function loadImports() {
    try {
      const p = JSON.parse(localStorage.getItem(IMPORT_KEY) || "{}");
      return p && typeof p === "object" ? p : {};
    } catch (_) {
      return {};
    }
  }

  function actionFor(d) {
    if (d.type === "class") {
      const s = loadClassState();
      if (CANONICAL_CLASS_IDS.has(d.id)) return "Update canonical class override";
      if (s.custom[d.id]) return "Update existing custom class";
      return "Create new class";
    }
    const store = loadImports();
    return store[d.type] && store[d.type][d.id]
      ? "Update existing imported design"
      : "Create imported design";
  }

  function renderPreview(d) {
    const isClass = d.type === "class";
    const details = isClass
      ? `<dt>Primary archetype</dt><dd>${escapeHtml(d.archetypes.primary || "TBD")}</dd>
         <dt>Abilities</dt><dd>${d.abilities.length} standard abilities + passive + ultimate</dd>
         <dt>Open questions</dt><dd>${d.openQuestions.length}</dd>`
      : `<dt>Sections</dt><dd>${d.sections.length}</dd>
         <dt>Open questions</dt><dd>${d.openQuestions.length}</dd>`;

    preview.className = "import-preview";
    preview.innerHTML = `
      <dl class="preview-dl">
        <dt>Type</dt><dd>${escapeHtml(d.type)}</dd>
        <dt>ID</dt><dd><code>${escapeHtml(d.id)}</code></dd>
        <dt>Name</dt><dd>${escapeHtml(d.name)}</dd>
        <dt>Destination</dt><dd>${escapeHtml(LABELS[d.type])}</dd>
        <dt>Action</dt><dd><strong>${escapeHtml(actionFor(d))}</strong></dd>
        ${details}
      </dl>
      <div class="preview-render">${isClass ? classToHtml(d) : genericToHtml(d)}</div>`;
    destinationLink.href = ROUTES[d.type];
    destinationLink.textContent = `Open ${LABELS[d.type]}`;
    applyRow.hidden = false;
  }

  function parse() {
    try {
      const text = stripFence(input.value);
      if (!text) throw new Error("Paste a design block first.");
      candidate = normalize(JSON.parse(text));
      renderPreview(candidate);
      message.textContent = "Valid design block. Review the preview, then apply it.";
      message.className = "import-message success";
    } catch (err) {
      candidate = null;
      applyRow.hidden = true;
      preview.className = "import-preview empty-state";
      preview.textContent = err.message || "Could not parse the design block.";
      message.textContent = "Nothing was changed.";
      message.className = "import-message error";
    }
  }

  function apply() {
    if (!candidate) return;

    if (candidate.type === "class") {
      const state = loadClassState();
      const record = {
        id: candidate.id,
        name: candidate.name,
        status: candidate.status,
        primary: candidate.archetypes.primary,
        html: classToHtml(candidate)
      };
      if (CANONICAL_CLASS_IDS.has(candidate.id)) {
        state.overrides[candidate.id] = record;
      } else {
        state.custom[candidate.id] = { ...record, custom: true };
      }
      state.selectedId = candidate.id;
      localStorage.setItem(CLASS_KEY, JSON.stringify(state));
    } else {
      const store = loadImports();
      if (!store[candidate.type]) store[candidate.type] = {};
      store[candidate.type][candidate.id] = {
        ...candidate,
        html: genericToHtml(candidate),
        importedAt: new Date().toISOString()
      };
      localStorage.setItem(IMPORT_KEY, JSON.stringify(store));
    }

    message.textContent = `Applied. “${candidate.name}” is now stored for ${LABELS[candidate.type]}.`;
    message.className = "import-message success";
    renderPreview(candidate);
  }

  parseBtn.addEventListener("click", parse);
  applyBtn.addEventListener("click", apply);

  clearBtn.addEventListener("click", () => {
    input.value = "";
    candidate = null;
    preview.className = "import-preview empty-state";
    preview.innerHTML = 'Paste a design block and choose <strong>Parse &amp; Preview</strong>.';
    applyRow.hidden = true;
    message.textContent = "";
    input.focus();
  });

  exampleBtn.addEventListener("click", () => {
    input.value = JSON.stringify({
      type: "class",
      version: 1,
      id: "redstoner",
      name: "Redstoner",
      status: "Working draft",
      archetypes: {
        primary: "Production",
        secondary: ["Construction", "Combat"]
      },
      coreIdea: "Uses redstone machinery to automate local production and create temporary infrastructure.",
      passive: {
        name: "Conductive",
        description: "TBD."
      },
      abilities: [
        {
          name: "Deploy Mechanism",
          description: "TBD.",
          upgrades: [
            {"name": "Upgrade A", "description": "TBD."},
            {"name": "Upgrade B", "description": "TBD."},
            {"name": "Upgrade C", "description": "TBD."}
          ]
        }
      ],
      ultimate: {
        name: "Ultimate — TBD",
        description: "TBD."
      },
      buildDirections: ["Automation", "Fortification"],
      openQuestions: ["Exact resource costs", "Machine persistence"]
    }, null, 2);
    parse();
  });
})();
