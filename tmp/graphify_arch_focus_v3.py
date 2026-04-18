import json
from pathlib import Path

import networkx as nx
from graphify.analyze import god_nodes, surprising_connections
from graphify.build import build_from_json
from graphify.cluster import cluster
from graphify.export import to_html
from graphify.report import generate
from networkx.readwrite import json_graph

ROOT = Path(r"d:/JavaCode/Lovemaster")
OUT = ROOT / "graphify-out"

EXTRACT_PATH = OUT / ".graphify_extract.arch2.json"
DETECT_PATH = OUT / ".graphify_detect.json"

V3_EXTRACT_PATH = OUT / ".graphify_extract.arch3.json"
V3_ANALYSIS_PATH = OUT / ".graphify_analysis.arch3.json"
V3_GRAPH_PATH = OUT / "graph.arch3.json"
V3_REPORT_PATH = OUT / "GRAPH_REPORT.arch3.md"
V3_HTML_PATH = OUT / "graph.arch3.html"


def main() -> None:
    extraction = json.loads(EXTRACT_PATH.read_text(encoding="utf-8"))
    detect_result = json.loads(DETECT_PATH.read_text(encoding="utf-8"))

    nodes = extraction.get("nodes", [])
    edges = extraction.get("edges", [])
    hyperedges = extraction.get("hyperedges", [])

    node_ids = {n["id"] for n in nodes if n.get("id")}
    g = nx.Graph()
    g.add_nodes_from(node_ids)
    for e in edges:
        s = e.get("source")
        t = e.get("target")
        if s in node_ids and t in node_ids:
            g.add_edge(s, t)

    if g.number_of_nodes() == 0:
        raise RuntimeError("arch2 graph has no nodes")

    components = list(nx.connected_components(g))
    largest = max(components, key=len)

    kept_nodes = [n for n in nodes if n.get("id") in largest]
    kept_ids = {n["id"] for n in kept_nodes}
    kept_edges = [e for e in edges if e.get("source") in kept_ids and e.get("target") in kept_ids]

    kept_hyperedges = []
    for h in hyperedges:
        h2 = dict(h)
        members = [nid for nid in h2.get("nodes", []) if nid in kept_ids]
        if len(members) >= 2:
            h2["nodes"] = members
            kept_hyperedges.append(h2)

    arch3_extraction = {
        "nodes": kept_nodes,
        "edges": kept_edges,
        "hyperedges": kept_hyperedges,
        "input_tokens": extraction.get("input_tokens", 0),
        "output_tokens": extraction.get("output_tokens", 0),
    }
    V3_EXTRACT_PATH.write_text(
        json.dumps(arch3_extraction, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    graph = build_from_json(arch3_extraction)
    communities = cluster(graph)
    gods = god_nodes(graph)
    surprises = surprising_connections(graph, communities)

    analysis = {
        "communities": {str(k): v for k, v in communities.items()},
        "cohesion": {},
        "god_nodes": gods,
        "surprises": surprises,
        "component": {
            "components_before": len(components),
            "largest_component_nodes": len(largest),
            "dropped_nodes": len(node_ids) - len(largest),
        },
    }
    V3_ANALYSIS_PATH.write_text(
        json.dumps(analysis, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    graph_data = json_graph.node_link_data(graph, edges="links")
    V3_GRAPH_PATH.write_text(
        json.dumps(graph_data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    report = generate(
        graph,
        communities,
        {},
        {},
        gods,
        surprises,
        detect_result,
        {
            "input_tokens": arch3_extraction.get("input_tokens", 0),
            "output_tokens": arch3_extraction.get("output_tokens", 0),
        },
        ".",
    )
    V3_REPORT_PATH.write_text(report, encoding="utf-8")
    to_html(graph, communities, str(V3_HTML_PATH))

    print("arch_focus_v3_complete")
    print(
        "nodes=", graph.number_of_nodes(),
        "edges=", graph.number_of_edges(),
        "communities=", len(communities),
    )
    print(
        "components_before=", len(components),
        "largest_component_nodes=", len(largest),
        "dropped_nodes=", len(node_ids) - len(largest),
    )


if __name__ == "__main__":
    main()
