#!/usr/bin/env python3
"""Validate the curriculum graph and build its generated outputs.

Usage:
    python3 tools/curriculum.py            # validate + build
    python3 tools/curriculum.py --check    # validate only (CI)

Reads  specs/curriculum/skill.schema.yaml and specs/curriculum/worlds/world-NN.yaml.
Writes specs/curriculum/generated/curriculum.json and specs/curriculum/generated/graph.html.

Checks:
  * each world file conforms to the schema
  * node ids are unique and their world prefix matches the file
  * every prerequisite exists and lives in the same or an earlier world
  * the graph is acyclic
  * every node is reachable from an entry node (no orphans)
"""
from __future__ import annotations

import json
import re
import sys
from collections import defaultdict, deque
from datetime import date
from pathlib import Path

import yaml

try:
    import jsonschema
except ImportError:  # pragma: no cover
    jsonschema = None

ROOT = Path(__file__).resolve().parents[1]
CURRICULUM = ROOT / "specs" / "curriculum"
WORLDS = CURRICULUM / "worlds"
GENERATED = CURRICULUM / "generated"
SCHEMA = CURRICULUM / "skill.schema.yaml"
TEMPLATE = ROOT / "tools" / "graph-viewer.template.html"

ID_RE = re.compile(r"^w(0[1-9]|1[0-2])\.([a-z]+)\.([a-z0-9-]+)$")

TIER_ORDER = ["playground", "adventure", "workshop", "studio", "lab"]
DOMAIN_NAMES = {
    "oa": "Operations & Algebraic Thinking",
    "nbt": "Number & Operations in Base Ten",
    "nf": "Fractions",
    "md": "Measurement & Data",
    "g": "Geometry",
    "rp": "Ratios & Proportional Relationships",
    "ns": "The Number System",
    "ee": "Expressions & Equations",
    "f": "Functions",
    "sp": "Statistics & Probability",
    "n": "Number & Quantity",
    "a": "Algebra",
    "s": "Statistics & Probability",
}


class Problem(Exception):
    pass


def load_worlds() -> list[dict]:
    schema = yaml.safe_load(SCHEMA.read_text())
    worlds = []
    for path in sorted(WORLDS.glob("world-*.yaml")):
        data = yaml.safe_load(path.read_text())
        if jsonschema is not None:
            validator = jsonschema.Draft202012Validator(schema)
            errors = sorted(validator.iter_errors(data), key=lambda e: list(e.path))
            if errors:
                lines = [f"{path.name}: schema errors"]
                for e in errors:
                    where = "/".join(str(p) for p in e.path) or "<root>"
                    lines.append(f"  at {where}: {e.message}")
                raise Problem("\n".join(lines))
        expected = int(path.stem.split("-")[1])
        if data["world"] != expected:
            raise Problem(f"{path.name}: world is {data['world']}, file name says {expected}")
        data["_file"] = path.name
        worlds.append(data)
    if [w["world"] for w in worlds] != list(range(1, 13)):
        raise Problem("expected world-01 … world-12, found: " + ", ".join(w["_file"] for w in worlds))
    return worlds


def flatten(worlds: list[dict]) -> list[dict]:
    nodes = []
    for w in worlds:
        defaults = w["defaults"]["mastery"]
        for n in w["nodes"]:
            node = dict(n)
            node["world"] = w["world"]
            node["tier"] = w["tier"]
            node.setdefault("source", "ccss")
            node.setdefault("ccss", [])
            node.setdefault("strategies", [])
            node.setdefault("secret_goals", [])
            node["mastery"] = {**defaults, **n.get("mastery", {})}
            m = ID_RE.match(node["id"])
            node["domain"] = m.group(2)
            node["domain_name"] = DOMAIN_NAMES.get(node["domain"], node["domain"].upper())
            nodes.append(node)
    return nodes


def validate(nodes: list[dict]) -> dict:
    problems: list[str] = []
    by_id: dict[str, dict] = {}
    for n in nodes:
        if n["id"] in by_id:
            problems.append(f"duplicate id {n['id']}")
        by_id[n["id"]] = n
        id_world = int(ID_RE.match(n["id"]).group(1))
        if id_world != n["world"]:
            problems.append(f"{n['id']}: id says world {id_world} but it is in world {n['world']}")
        if n["source"] == "ccss" and not n["ccss"]:
            problems.append(f"{n['id']}: source is ccss but no standards are listed")
        if n["source"] == "original" and n["ccss"]:
            problems.append(f"{n['id']}: source is original but lists standards")

    for n in nodes:
        for p in n["prerequisites"]:
            if p == n["id"]:
                problems.append(f"{n['id']}: lists itself as a prerequisite")
            elif p not in by_id:
                problems.append(f"{n['id']}: unknown prerequisite {p}")
            elif by_id[p]["world"] > n["world"]:
                problems.append(f"{n['id']}: prerequisite {p} is in a later world")
        if len(set(n["prerequisites"])) != len(n["prerequisites"]):
            problems.append(f"{n['id']}: duplicate prerequisites")

    if problems:
        raise Problem("\n".join(problems))

    # Topological order (Kahn). Anything left over is in a cycle.
    indeg = {n["id"]: len(n["prerequisites"]) for n in nodes}
    dependents = defaultdict(list)
    for n in nodes:
        for p in n["prerequisites"]:
            dependents[p].append(n["id"])
    queue = deque(sorted(i for i, d in indeg.items() if d == 0))
    order = []
    while queue:
        i = queue.popleft()
        order.append(i)
        for d in dependents[i]:
            indeg[d] -= 1
            if indeg[d] == 0:
                queue.append(d)
    if len(order) != len(nodes):
        stuck = sorted(i for i, d in indeg.items() if d > 0)
        raise Problem("cycle detected involving: " + ", ".join(stuck[:20]))

    # Depth = longest path from an entry node.
    depth = {}
    for i in order:
        preds = by_id[i]["prerequisites"]
        depth[i] = 0 if not preds else 1 + max(depth[p] for p in preds)

    return {"order": order, "dependents": dependents, "depth": depth, "by_id": by_id}


def stats(nodes: list[dict], info: dict) -> dict:
    edges = sum(len(n["prerequisites"]) for n in nodes)
    cross = sum(1 for n in nodes for p in n["prerequisites"] if info["by_id"][p]["world"] != n["world"])
    per_world = defaultdict(lambda: {"nodes": 0, "ccss": 0, "original": 0, "edges_in": 0, "cross_world_in": 0})
    per_domain = defaultdict(int)
    per_engine = defaultdict(int)
    per_rep = defaultdict(int)
    for n in nodes:
        w = per_world[n["world"]]
        w["nodes"] += 1
        w[n["source"]] += 1
        w["edges_in"] += len(n["prerequisites"])
        w["cross_world_in"] += sum(1 for p in n["prerequisites"] if info["by_id"][p]["world"] != n["world"])
        per_domain[n["domain_name"]] += 1
        for e in n["engines"]:
            per_engine[e] += 1
        for r in n["representations"]:
            per_rep[r] += 1
    entry = [n["id"] for n in nodes if not n["prerequisites"]]
    leaves = [n["id"] for n in nodes if not info["dependents"].get(n["id"])]
    standards = sorted({s for n in nodes for s in n["ccss"]})
    return {
        "nodes": len(nodes),
        "edges": edges,
        "cross_world_edges": cross,
        "entry_nodes": entry,
        "leaf_nodes": leaves,
        "max_depth": max(info["depth"].values()),
        "standards_referenced": len(standards),
        "original_nodes": sum(1 for n in nodes if n["source"] == "original"),
        "per_world": {k: dict(v) for k, v in sorted(per_world.items())},
        "per_domain": dict(sorted(per_domain.items(), key=lambda kv: -kv[1])),
        "per_engine": dict(sorted(per_engine.items(), key=lambda kv: -kv[1])),
        "per_representation": dict(sorted(per_rep.items(), key=lambda kv: -kv[1])),
    }


def build(worlds: list[dict], nodes: list[dict], info: dict, summary: dict) -> None:
    GENERATED.mkdir(parents=True, exist_ok=True)
    out_nodes = []
    for n in nodes:
        out_nodes.append({
            "id": n["id"],
            "world": n["world"],
            "tier": n["tier"],
            "domain": n["domain"],
            "domain_name": n["domain_name"],
            "title": n["title"],
            "description": n["description"],
            "source": n["source"],
            "ccss": n["ccss"],
            "prerequisites": n["prerequisites"],
            "dependents": sorted(info["dependents"].get(n["id"], [])),
            "representations": n["representations"],
            "engines": n["engines"],
            "mastery": n["mastery"],
            "strategies": n["strategies"],
            "secret_goals": n["secret_goals"],
            "depth": info["depth"][n["id"]],
        })
    payload = {
        "generated": date.today().isoformat(),
        "worlds": [{"world": w["world"], "name": w["name"], "tier": w["tier"], "notes": w.get("notes", "").strip()} for w in worlds],
        "tiers": TIER_ORDER,
        "domains": DOMAIN_NAMES,
        "stats": summary,
        "nodes": out_nodes,
    }
    (GENERATED / "curriculum.json").write_text(json.dumps(payload, indent=1) + "\n")
    if TEMPLATE.exists():
        html = TEMPLATE.read_text().replace("/*__CURRICULUM_JSON__*/null", json.dumps(payload))
        (GENERATED / "graph.html").write_text(html)


def print_summary(summary: dict) -> None:
    print(f"nodes: {summary['nodes']}   edges: {summary['edges']} ({summary['cross_world_edges']} cross-world)")
    print(f"entry nodes: {len(summary['entry_nodes'])}   leaves: {len(summary['leaf_nodes'])}   max depth: {summary['max_depth']}")
    print(f"standards referenced: {summary['standards_referenced']}   original nodes: {summary['original_nodes']}")
    print("per world:")
    for w, s in summary["per_world"].items():
        print(f"  w{w:02d}: {s['nodes']:3d} nodes  {s['edges_in']:3d} edges in  ({s['cross_world_in']} from earlier worlds)")
    print("per engine:", ", ".join(f"{k} {v}" for k, v in summary["per_engine"].items()))
    print("per representation:", ", ".join(f"{k} {v}" for k, v in summary["per_representation"].items()))


def main(argv: list[str]) -> int:
    check_only = "--check" in argv
    try:
        worlds = load_worlds()
        nodes = flatten(worlds)
        info = validate(nodes)
    except Problem as p:
        print("CURRICULUM INVALID", file=sys.stderr)
        print(p, file=sys.stderr)
        return 1
    summary = stats(nodes, info)
    print_summary(summary)
    if not check_only:
        build(worlds, nodes, info, summary)
        print(f"wrote {GENERATED.relative_to(ROOT)}/curriculum.json" + (" and graph.html" if TEMPLATE.exists() else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
