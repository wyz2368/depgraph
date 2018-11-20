import json
from graphviz import Digraph

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def add_nodes_fancy(graph, json_data, attacker_nodes_after):
    nodes = json_data["nodes"]
    for node in nodes:
        node_id = node["id"]
        node_name = str(node["id"])

        node_color = "green4"
        if node["nodeType"] != "NONTARGET":
            node_color = "red"

        line_style = "solid"
        if node["nodeType"] != "NONTARGET":
            line_style = "dashed"

        width = "4.0"

        my_style = line_style
        fill = "white"
        if node_id in attacker_nodes_after:
            my_style += ",filled"
            fill = "gray50"

        graph.node(name=node_name, label="", color=node_color, penwidth=width, \
            style=my_style, fillcolor=fill)

def add_edges(graph, json_data):
    edges = json_data["edges"]
    for edge in edges:
        tail = str(edge["srcID"])
        head = str(edge["desID"])
        graph.edge(tail_name=tail, head_name=head, color="gray30", penwidth="1.7")

GRAPH_NAME = "RandomGraph30N100E6T1_B"
# GRAPH_NAME = "SepLayerGraph0_noAnd_B"

GRAPH_JSON_NAME = GRAPH_NAME + ".json"
MY_JSON_DATA = get_json_data(GRAPH_JSON_NAME)

MY_GRAPH = Digraph(comment=GRAPH_NAME, graph_attr={"margin": "0.05"})
ATTACKER_NODES_AFTER = []

add_nodes_fancy(MY_GRAPH, MY_JSON_DATA, ATTACKER_NODES_AFTER)
add_edges(MY_GRAPH, MY_JSON_DATA)

# print(graph.source)
OUTPUT_NAME = "out_" + GRAPH_NAME + ".gv"
MY_GRAPH.render(OUTPUT_NAME, view=True)

# dot -Tps2 forest.gv -o forest.ps | ps2pdf forest.ps

# usage:
# python3 vis_net_paper.py
