#!/usr/bin/env python3
"""Validate engine recipes against the schema, the curriculum, and the engine specs.

Usage:
    python3 tools/recipes.py            # validate all recipe files and print coverage

Reads  specs/mechanics/recipes/recipe.schema.yaml and specs/mechanics/recipes/world-NN.yaml,
       plus the curriculum world files (via tools/curriculum.py).

Checks, per recipe:
  * conforms to the schema
  * the node exists and lists this engine
  * representations are a subset of the node's
  * kit / instrument / target / hint rungs are ones the engine spec defines
  * strategies map node strategy names to rule ids the engine spec defines
  * secret goal ids exist on the node
  * one recipe per (node, engine)
Then prints coverage: which (node, engine) pairs in the world still lack a recipe.
"""
from __future__ import annotations

import json
import sys
from collections import defaultdict
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
import curriculum as cur  # noqa: E402

try:
    import jsonschema
except ImportError:  # pragma: no cover
    jsonschema = None

ROOT = Path(__file__).resolve().parents[1]
RECIPES = ROOT / "specs" / "mechanics" / "recipes"
GENERATED = RECIPES / "generated"
SCHEMA = RECIPES / "recipe.schema.yaml"

# What each engine spec defines. Keep in step with engine-*.md.
ENGINES = {
    "build": {
        "binding": "kit",
        "kits": {"tiles", "blocks", "parts", "strokes", "marks", "gears", "units"},
        "targets": {"quantity", "product", "quotient", "factor-pairs", "constraint", "angle", "plot", "sequence", "table", "two-ways"},
        "rules": {
            "split-place-value", "split-one-dimension", "whole-array", "rows-of", "chunked-quotient", "all-pairs-ascending",
            "bundle", "unbundle", "unit-pieces", "grouped-copies", "regroup-whole", "edge-first", "mirror-pairs",
        },
        "hints": {"structure", "piece", "readout"},
    },
    "manipulate": {
        "binding": "instrument",
        "kits": {"number-line", "fraction-bar", "grid", "angle", "place-chart", "balance", "area-model", "graph"},
        "targets": {"value", "nearest", "equal", "partition", "unknown", "match", "symmetric"},
        "rules": {
            "midpoint-first", "zoom-then-place", "recut-only", "regroup-whole", "benchmark-compare", "shift-digits",
            "quarter-turns", "complement-first", "fold-test",
        },
        "hints": {"zoom", "nudge", "show"},
    },
}


def main() -> int:
    schema = yaml.safe_load(SCHEMA.read_text())
    nodes = {n["id"]: n for n in cur.flatten(cur.load_worlds())}
    problems: list[str] = []
    seen: dict[tuple[str, str], str] = {}
    files = sorted(RECIPES.glob("world-*.yaml"))
    if not files:
        print("no recipe files found")
        return 1

    for path in files:
        data = yaml.safe_load(path.read_text())
        if jsonschema is not None:
            errs = sorted(jsonschema.Draft202012Validator(schema).iter_errors(data), key=lambda e: list(e.path))
            for e in errs:
                where = "/".join(str(p) for p in e.path) or "<root>"
                problems.append(f"{path.name} schema at {where}: {e.message}")
            if errs:
                continue
        for r in data["recipes"]:
            tag = f"{path.name} {r['node']}/{r['engine']}"
            node = nodes.get(r["node"])
            if node is None:
                problems.append(f"{tag}: unknown node")
                continue
            if node["world"] != data["world"]:
                problems.append(f"{tag}: node is in world {node['world']}, file is world {data['world']}")
            if r["engine"] not in node["engines"]:
                problems.append(f"{tag}: node does not list engine {r['engine']} (has {node['engines']})")
            key = (r["node"], r["engine"])
            if key in seen:
                problems.append(f"{tag}: duplicate recipe (also in {seen[key]})")
            seen[key] = path.name
            extra_reps = set(r["representations"]) - set(node["representations"])
            if extra_reps:
                problems.append(f"{tag}: representations not on node: {sorted(extra_reps)}")
            spec = ENGINES.get(r["engine"])
            if spec is None:
                problems.append(f"{tag}: engine {r['engine']} has no spec yet")
                continue
            binding = r.get(spec["binding"])
            if binding is None:
                problems.append(f"{tag}: missing {spec['binding']}")
            elif binding not in spec["kits"]:
                problems.append(f"{tag}: unknown {spec['binding']} {binding}")
            other = "instrument" if spec["binding"] == "kit" else "kit"
            if other in r:
                problems.append(f"{tag}: {other} is not a {r['engine']} field")
            if r.get("instrument2") and r.get("instrument2") not in spec["kits"]:
                problems.append(f"{tag}: unknown instrument2 {r['instrument2']}")
            if r["target"] not in spec["targets"]:
                problems.append(f"{tag}: unknown target {r['target']}")
            if set(r["hints"]) != spec["hints"]:
                problems.append(f"{tag}: hint rungs must be {sorted(spec['hints'])}, got {sorted(r['hints'])}")
            for name, rule in (r.get("strategies") or {}).items():
                if name not in node["strategies"]:
                    problems.append(f"{tag}: strategy {name} is not on the node (has {node['strategies']})")
                if rule not in spec["rules"]:
                    problems.append(f"{tag}: unknown detection rule {rule}")
            goal_ids = {g["id"] for g in node["secret_goals"]}
            for g in r.get("secret_goals") or []:
                if g["id"] not in goal_ids:
                    problems.append(f"{tag}: secret goal {g['id']} is not on the node (has {sorted(goal_ids)})")
            gen = r["generator"]
            if set(gen) & {"low", "mid", "high"} and set(gen) != {"low", "mid", "high"}:
                problems.append(f"{tag}: generator bands must be all of low, mid, high")

    if problems:
        print("RECIPES INVALID", file=sys.stderr)
        print("\n".join(problems), file=sys.stderr)
        return 1

    # Emit JSON for the game: core reads JSON only (specs/build-plan.md M2).
    GENERATED.mkdir(parents=True, exist_ok=True)
    for path in files:
        data = yaml.safe_load(path.read_text())
        for r in data["recipes"]:
            r.setdefault("strategies", {})
            r.setdefault("secret_goals", [])
            r.setdefault("tolerance", {})
            r.setdefault("frame", "")
        out = GENERATED / (path.stem + ".json")
        out.write_text(json.dumps(data, indent=1) + "\n")
    print(f"wrote {GENERATED.relative_to(ROOT)}/world-NN.json")

    # Coverage per world present in the recipe files.
    worlds = sorted({int(p.stem.split("-")[1]) for p in files})
    for w in worlds:
        wnodes = [n for n in nodes.values() if n["world"] == w]
        have = defaultdict(set)
        for (nid, eng) in seen:
            have[nid].add(eng)
        specd = set(ENGINES)
        missing = [(n["id"], e) for n in wnodes for e in n["engines"] if e in specd and e not in have[n["id"]]]
        unspecd = [(n["id"], e) for n in wnodes for e in n["engines"] if e not in specd]
        covered_nodes = sum(1 for n in wnodes if have[n["id"]])
        print(f"world {w}: {len(seen)} recipes; {covered_nodes}/{len(wnodes)} nodes have at least one recipe")
        print(f"  missing recipes for specified engines: {len(missing)}")
        for nid, e in missing:
            print(f"    {nid} / {e}")
        print(f"  node/engine pairs waiting on unspecified engines: {len(unspecd)}")
    print("recipes valid")
    return 0


if __name__ == "__main__":
    sys.exit(main())
