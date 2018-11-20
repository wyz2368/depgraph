import json
from graphviz import Digraph

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def add_nodes(graph, json_data):
    nodes = json_data["nodes"]
    for node in nodes:
        node_name = str(node["id"])
        node_color = None
        if node["nodeType"] == "NONTARGET":
            if node["state"] == "INACTIVE":
                node_color = "green"
            else:
                node_color = "red"
        else:
            if node["state"] == "INACTIVE":
                node_color = "blue"
            else:
                node_color = "gray"
        graph.node(name=node_name, label=node_name, color=node_color)

def add_edges(graph, json_data):
    edges = json_data["edges"]
    for edge in edges:
        tail = str(edge["srcID"])
        head = str(edge["desID"])
        graph.edge(tail_name=tail, head_name=head)

# graph_name = "RandomGraph30N100E6T1_B"
graph_name = "SepLayerGraph0_noAnd_B"

graph_json_name = graph_name + ".json"
json_data = get_json_data(graph_json_name)

graph = Digraph(comment=graph_name)
add_nodes(graph, json_data)
add_edges(graph, json_data)

# print(graph.source)
output_name = "out_" + graph_name + ".gv"
graph.render(output_name, view=True)  

# usage:
# python3 vis_net.py
