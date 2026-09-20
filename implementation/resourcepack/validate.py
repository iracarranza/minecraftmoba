"""Check that the pack and the plugin agree about codepoints.

A mismatch between them does not degrade gracefully: the client renders a tofu
box, which looks like a broken font rather than like a missing feature. This is
the check that keeps the two sides honest, and it is entirely structural — it
never looks at what a glyph depicts.
"""
from __future__ import annotations
import argparse
import json
import re
from pathlib import Path

ESCAPE = re.compile(r"\\u(e[0-9a-f]{3})", re.IGNORECASE)


def pack_codepoints(pack: Path) -> dict[str, str]:
    manifest = json.loads((pack / "GLYPH_MANIFEST.json").read_text())
    return {g: info["codepoint"] for g, info in manifest["glyphs"].items()}


def declared_providers(pack: Path) -> set[str]:
    font = json.loads((pack / "assets/moba/font/glyphs.json").read_text())
    return {c for p in font["providers"] for c in p.get("chars", [])}


def referenced_in_source(roots: list[Path]) -> set[str]:
    """Private-use escapes appearing in plugin source."""
    found = set()
    for root in roots:
        if not root.exists(): continue
        for path in root.rglob("*.java"):
            for match in ESCAPE.finditer(path.read_text(errors="ignore")):
                found.add(chr(int(match.group(1), 16)))
    return found


def validate(pack: Path, sources: list[Path]):
    problems = []
    manifest = json.loads((pack / "GLYPH_MANIFEST.json").read_text())

    declared = declared_providers(pack)
    for glyph, cp in pack_codepoints(pack).items():
        ch = chr(int(cp.removeprefix("U+"), 16))
        if ch not in declared:
            problems.append(f"{glyph}: in manifest but no font provider declares {cp}")
        texture = pack / "assets/moba/textures/font" / f"{glyph}.png"
        if not texture.exists():
            problems.append(f"{glyph}: provider declares {cp} but {texture.name} is missing")

    # Duplicate codepoints would silently shadow one another.
    seen = {}
    for glyph, cp in pack_codepoints(pack).items():
        if cp in seen:
            problems.append(f"codepoint {cp} assigned to both {seen[cp]} and {glyph}")
        seen[cp] = glyph

    used = referenced_in_source(sources)
    unknown = sorted(c for c in used if c not in declared)
    for ch in unknown:
        problems.append(f"plugin emits U+{ord(ch):04X} which the pack does not provide "
                        f"(renders as tofu)")

    unused = sorted(c for c in declared if c not in used) if used else []

    result = {
        "schema": "moba_glyph_validation/1",
        "pack": str(pack),
        "glyphs_declared": len(declared),
        "glyphs_referenced_by_plugin": len(used),
        "referenced_but_missing": [f"U+{ord(c):04X}" for c in unknown],
        "declared_but_unused": [f"U+{ord(c):04X}" for c in unused],
        "placeholders": sorted(g for g, i in manifest["glyphs"].items() if i.get("placeholder")),
        "problems": problems,
        "pass": not problems,
        "not_covered": ["whether any glyph looks correct, which is a design question",
                        "runtime pack negotiation and client-side pack rejection",
                        "glyph alignment once real artwork replaces the placeholders"],
    }
    return result


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--pack", type=Path, required=True)
    p.add_argument("--source", type=Path, action="append", default=[])
    p.add_argument("--output", type=Path)
    a = p.parse_args()
    r = validate(a.pack.resolve(), [s.resolve() for s in a.source])
    if a.output:
        a.output.parent.mkdir(parents=True, exist_ok=True)
        a.output.write_text(json.dumps(r, indent=2) + "\n")
    for problem in r["problems"]:
        print("PROBLEM", problem)
    print(f"{r['glyphs_declared']} declared, {r['glyphs_referenced_by_plugin']} referenced, "
          f"{len(r['placeholders'])} still placeholders: {'PASS' if r['pass'] else 'FAIL'}")
    raise SystemExit(0 if r["pass"] else 1)
