(function () {
  const STORAGE_KEY = "minecraft-moba-class-library-v2";
  const listEl = document.getElementById("class-list");
  const reader = document.getElementById("class-reader");
  const empty = document.getElementById("class-reader-empty");
  const content = document.getElementById("class-reader-content");
  const editBtn = document.getElementById("class-edit");
  const saveBtn = document.getElementById("class-save");
  const cancelBtn = document.getElementById("class-cancel");
  const resetBtn = document.getElementById("class-reset");
  const deleteBtn = document.getElementById("class-delete");
  const exportBtn = document.getElementById("class-export");
  const importInput = document.getElementById("class-import");
  const resetAllBtn = document.getElementById("class-reset-all");
  const form = document.getElementById("new-class-form");
  const status = document.getElementById("class-manager-status");

  if (!listEl || !reader || !content || !form) return;

  const seeds = {};
  document.querySelectorAll("template.class-seed").forEach((tpl) => {
    seeds[tpl.dataset.classId] = {
      id: tpl.dataset.classId,
      name: tpl.dataset.name,
      status: tpl.dataset.status || "Working draft",
      primary: tpl.dataset.primary || "",
      html: tpl.innerHTML.trim(),
      custom: false
    };
  });

  let state = loadState();
  let selectedId = state.selectedId && getRecord(state.selectedId)
    ? state.selectedId
    : Object.keys(seeds)[0];
  let editSnapshot = null;

  function message(text) {
    if (!status) return;
    status.textContent = text;
    clearTimeout(message.timer);
    message.timer = setTimeout(() => { status.textContent = ""; }, 2200);
  }

  function loadState() {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
      return {
        overrides: parsed.overrides || {},
        custom: parsed.custom || {},
        selectedId: parsed.selectedId || null
      };
    } catch (_) {
      return { overrides: {}, custom: {}, selectedId: null };
    }
  }

  function persist() {
    state.selectedId = selectedId;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }

  function getRecord(id) {
    if (seeds[id]) {
      return state.overrides[id]
        ? { ...seeds[id], ...state.overrides[id], custom: false }
        : seeds[id];
    }
    return state.custom[id] || null;
  }

  function allRecords() {
    const canonical = Object.values(seeds).map((seed) => getRecord(seed.id));
    return canonical.concat(Object.values(state.custom));
  }

  function deriveName(html, fallback) {
    const box = document.createElement("div");
    box.innerHTML = html;
    const h2 = box.querySelector("h2");
    return (h2 && h2.textContent.trim()) || fallback;
  }

  function renderList() {
    listEl.innerHTML = "";
    allRecords().forEach((record) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "library-item" + (record.id === selectedId ? " active" : "");

      const name = document.createElement("strong");
      name.textContent = record.name;
      btn.appendChild(name);

      const sub = document.createElement("small");
      sub.textContent = [record.primary, record.status].filter(Boolean).join(" · ");
      btn.appendChild(sub);

      btn.addEventListener("click", () => select(record.id));
      listEl.appendChild(btn);
    });
  }

  function render() {
    renderList();
    const record = getRecord(selectedId);
    if (!record) {
      reader.hidden = true;
      empty.hidden = false;
      return;
    }

    empty.hidden = true;
    reader.hidden = false;
    content.innerHTML = record.html;
    content.contentEditable = "false";
    editBtn.hidden = false;
    saveBtn.hidden = true;
    cancelBtn.hidden = true;
    deleteBtn.hidden = !record.custom;
    resetBtn.textContent = record.custom ? "Discard unsaved edit" : "Reset to source";
  }

  function select(id) {
    if (!getRecord(id)) return;
    leaveEditMode(false);
    selectedId = id;
    persist();
    render();
  }

  function enterEditMode() {
    const record = getRecord(selectedId);
    if (!record) return;
    editSnapshot = content.innerHTML;
    content.contentEditable = "true";
    editBtn.hidden = true;
    saveBtn.hidden = false;
    cancelBtn.hidden = false;
    content.focus();
    message("Editing selected class.");
  }

  function leaveEditMode(restore) {
    if (restore && editSnapshot !== null) content.innerHTML = editSnapshot;
    content.contentEditable = "false";
    editSnapshot = null;
    editBtn.hidden = false;
    saveBtn.hidden = true;
    cancelBtn.hidden = true;
  }

  function saveSelected() {
    const record = getRecord(selectedId);
    if (!record) return;
    const html = content.innerHTML.trim();
    const name = deriveName(html, record.name);

    if (record.custom) {
      state.custom[selectedId] = { ...record, name, html, custom: true };
    } else {
      state.overrides[selectedId] = {
        name,
        status: record.status,
        primary: record.primary,
        html
      };
    }

    persist();
    leaveEditMode(false);
    render();
    message("Class saved locally.");
  }

  function resetSelected() {
    const record = getRecord(selectedId);
    if (!record) return;

    if (record.custom) {
      if (editSnapshot !== null) {
        content.innerHTML = editSnapshot;
        leaveEditMode(false);
        render();
        message("Unsaved changes discarded.");
      } else {
        message("Custom classes have no embedded source version.");
      }
      return;
    }

    if (!confirm("Reset this class to the canonical version embedded in classes.html?")) return;
    delete state.overrides[selectedId];
    persist();
    render();
    message("Reset to source.");
  }

  function deleteSelected() {
    const record = getRecord(selectedId);
    if (!record || !record.custom) return;
    if (!confirm(`Delete “${record.name}” from this browser's class library?`)) return;

    delete state.custom[selectedId];
    const remaining = allRecords();
    selectedId = remaining.length ? remaining[0].id : null;
    persist();
    render();
    message("Class deleted.");
  }

  function safeId(name) {
    const base = name.toLowerCase().trim()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "") || "new-class";
    let id = base;
    let n = 2;
    while (getRecord(id)) id = `${base}-${n++}`;
    return id;
  }

  function escapeHtml(value) {
    return String(value || "").replace(/[&<>"']/g, (ch) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;"
    })[ch]);
  }

  function createDraft(data) {
    const id = safeId(data.name);
    const otherBadges = (data.other || "")
      .split(",")
      .map((x) => x.trim())
      .filter(Boolean)
      .map((x) => `<span class="badge">${escapeHtml(x)}</span>`)
      .join("");

    const html = `
      <h2>${escapeHtml(data.name)}</h2>
      <div class="class-meta-line">
        <span class="badge status-working">${escapeHtml(data.status)}</span>
        ${data.primary ? `<span class="badge">Primary: ${escapeHtml(data.primary)}</span>` : ""}
        ${otherBadges}
      </div>
      <h3>Core idea</h3>
      <p>${escapeHtml(data.core)}</p>
      <h3>Primary archetype</h3>
      <p>${escapeHtml(data.primary || "TBD")}</p>
      <h3>Other possible archetypes</h3>
      <p>${escapeHtml(data.other || "TBD")}</p>
      <h3>Passive — name TBD</h3>
      <p>Describe the passive.</p>
      <h3>Ability 1 — name TBD</h3>
      <p>Describe Ability 1.</p>
      <h4>Upgrade choices</h4>
      <p><strong>Upgrade A</strong> — </p>
      <p><strong>Upgrade B</strong> — </p>
      <p><strong>Upgrade C</strong> — </p>
      <h3>Ability 2 — name TBD</h3>
      <p>Describe Ability 2.</p>
      <h4>Upgrade choices</h4>
      <p><strong>Upgrade A</strong> — </p>
      <p><strong>Upgrade B</strong> — </p>
      <p><strong>Upgrade C</strong> — </p>
      <h3>Ultimate — name TBD</h3>
      <p>Describe the ultimate.</p>
      <h3>Possible build directions</h3>
      <ul><li>TBD</li></ul>
      <h3>Open questions</h3>
      <ul><li>TBD</li></ul>
    `.trim();

    state.custom[id] = {
      id,
      name: data.name,
      status: data.status,
      primary: data.primary,
      html,
      custom: true
    };

    selectedId = id;
    persist();
    render();
    form.reset();
    document.getElementById("existing-classes").scrollIntoView({ behavior: "smooth", block: "start" });
    setTimeout(enterEditMode, 250);
    message("New class created.");
  }

  function exportLibrary() {
    const payload = {
      project: "Minecraft MOBA",
      type: "class-library",
      version: 2,
      exportedAt: new Date().toISOString(),
      classes: allRecords().map((r) => ({
        id: r.id,
        name: r.name,
        status: r.status,
        primary: r.primary,
        html: r.html,
        custom: !!r.custom
      }))
    };

    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "minecraft-moba-classes.json";
    a.click();
    URL.revokeObjectURL(url);
    message("Class library exported.");
  }

  async function importLibrary(file) {
    try {
      const parsed = JSON.parse(await file.text());
      if (!parsed || !Array.isArray(parsed.classes)) throw new Error("No classes array");

      const next = { overrides: {}, custom: {}, selectedId: null };
      parsed.classes.forEach((r) => {
        if (!r || !r.id || !r.html) return;
        const item = {
          id: String(r.id),
          name: String(r.name || r.id),
          status: String(r.status || "Working draft"),
          primary: String(r.primary || ""),
          html: String(r.html),
          custom: !!r.custom
        };

        if (seeds[item.id] && !item.custom) next.overrides[item.id] = item;
        else {
          item.custom = true;
          next.custom[item.id] = item;
        }
      });

      state = next;
      const records = allRecords();
      selectedId = records.length ? records[0].id : null;
      persist();
      render();
      message("Class library imported.");
    } catch (_) {
      message("Import failed: invalid class-library JSON.");
    }
  }

  editBtn.addEventListener("click", enterEditMode);
  saveBtn.addEventListener("click", saveSelected);
  cancelBtn.addEventListener("click", () => {
    leaveEditMode(true);
    render();
    message("Edit cancelled.");
  });
  resetBtn.addEventListener("click", resetSelected);
  deleteBtn.addEventListener("click", deleteSelected);
  exportBtn.addEventListener("click", exportLibrary);

  importInput.addEventListener("change", () => {
    const file = importInput.files && importInput.files[0];
    if (file) importLibrary(file);
    importInput.value = "";
  });

  resetAllBtn.addEventListener("click", () => {
    if (!confirm("Reset all canonical classes and delete every locally created class?")) return;
    localStorage.removeItem(STORAGE_KEY);
    state = { overrides: {}, custom: {}, selectedId: null };
    selectedId = Object.keys(seeds)[0] || null;
    persist();
    render();
    message("Entire class library reset.");
  });

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const fd = new FormData(form);
    createDraft({
      name: String(fd.get("name") || "").trim(),
      status: String(fd.get("status") || "Working draft"),
      primary: String(fd.get("primary") || "").trim(),
      other: String(fd.get("other") || "").trim(),
      core: String(fd.get("core") || "").trim()
    });
  });

  render();
})();
