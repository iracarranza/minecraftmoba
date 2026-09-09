(function () {
  const page = document.body.dataset.page || location.pathname.split("/").pop() || "index.html";
  const storageKey = "minecraft-moba-design-wiki::" + page;
  const editables = Array.from(document.querySelectorAll("[data-edit]"));
  const status = document.querySelector("[data-status]");

  function setStatus(message) {
    if (!status) return;
    status.textContent = message;
    window.clearTimeout(setStatus.timer);
    setStatus.timer = window.setTimeout(() => {
      status.textContent = "Edits save locally in this browser.";
    }, 1800);
  }

  function collect() {
    const data = {};
    editables.forEach((el) => {
      data[el.dataset.edit] = el.innerHTML;
    });
    return data;
  }

  function apply(data) {
    editables.forEach((el) => {
      if (Object.prototype.hasOwnProperty.call(data, el.dataset.edit)) {
        el.innerHTML = data[el.dataset.edit];
      }
    });
  }

  function save() {
    try {
      localStorage.setItem(storageKey, JSON.stringify(collect()));
      setStatus("Saved.");
    } catch (err) {
      setStatus("Could not save locally.");
    }
  }

  function load() {
    try {
      const raw = localStorage.getItem(storageKey);
      if (raw) apply(JSON.parse(raw));
    } catch (err) {}
  }

  editables.forEach((el) => {
    el.setAttribute("contenteditable", "true");
    el.setAttribute("spellcheck", "true");
    el.addEventListener("input", () => {
      try {
        localStorage.setItem(storageKey, JSON.stringify(collect()));
        if (status) status.textContent = "Saving…";
        window.clearTimeout(el._saveTimer);
        el._saveTimer = window.setTimeout(() => setStatus("Saved."), 300);
      } catch (err) {}
    });
  });

  document.querySelectorAll("[data-save]").forEach((btn) => btn.addEventListener("click", save));

  document.querySelectorAll("[data-export]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const payload = {
        project: "Minecraft MOBA Design Wiki",
        page,
        exportedAt: new Date().toISOString(),
        content: collect()
      };
      const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = page.replace(".html", "") + "-notes.json";
      a.click();
      URL.revokeObjectURL(url);
      setStatus("Exported JSON.");
    });
  });

  const importInput = document.querySelector("[data-import]");
  if (importInput) {
    importInput.addEventListener("change", async () => {
      const file = importInput.files && importInput.files[0];
      if (!file) return;
      try {
        const parsed = JSON.parse(await file.text());
        const content = parsed.content || parsed;
        apply(content);
        localStorage.setItem(storageKey, JSON.stringify(collect()));
        setStatus("Imported.");
      } catch (err) {
        setStatus("Import failed.");
      } finally {
        importInput.value = "";
      }
    });
  }

  document.querySelectorAll("[data-reset]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (!confirm("Reset this page to the version in the HTML file?")) return;
      localStorage.removeItem(storageKey);
      location.reload();
    });
  });

  load();
})();
