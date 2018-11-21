import json
from os import remove
from graphviz import Digraph

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def add_metadata(graph, time_step, attacker_payoff, defender_payoff):
    fmt = "{0:.1f}"
    title = "t: " + str(time_step) + "\ndef_payoff: " + fmt.format(defender_payoff) + \
        "\natt_payoff: " + fmt.format(attacker_payoff)
    graph.attr(label=str(title))
    font_size = "20"
    graph.attr(fontsize=font_size)

def add_nodes_fancy(graph, json_data, attacker_nodes_after, nodes_attacked, nodes_defended):
    nodes = json_data["nodes"]
    for node in nodes:
        node_id = node["id"]
        node_name = str(node["id"])

        node_color = "green4"
        if node["nodeType"] != "NONTARGET":
            node_color = "red"

        line_style = "solid"
        if node_id in nodes_defended:
            line_style = "dashed"

        width = "1.0"
        if node_id in nodes_attacked:
            width = "5.0"

        my_style = line_style
        fill = "white"
        if node_id in attacker_nodes_after:
            my_style += ",filled"
            fill = "gray50"

        graph.node(name=node_name, label=node_name, color=node_color, penwidth=width, \
            style=my_style, fillcolor=fill)

def add_edges_fancy(graph, json_data, edges_attacked):
    edges = json_data["edges"]
    for edge in edges:
        edge_id = edge["id"]

        edge_color = "gray40"
        width = "1.0"
        if edge_id in edges_attacked:
            width = "4.0"
            edge_color = "black"

        tail = str(edge["srcID"])
        head = str(edge["desID"])
        graph.edge(tail_name=tail, head_name=head, color=edge_color, penwidth=width)

def get_game_data(status_data, game_index):
    games = status_data["games"]
    for game in games:
        cur_index = game["game_index"]
        if cur_index == game_index:
            return game["time_steps"]
    raise ValueError("Game not found: " + str(game_index))

def get_max_time(game_data):
    times = [x["time"] for x in game_data]
    if not times:
        return -1
    return max(times)

def get_time_step_data(game_data, time_step):
    for time_data in game_data:
        if time_data["time"] == time_step:
            return time_data
    raise ValueError("Time data not found: " + str(time_step))

def make_game_time_graph(status_data, json_data, graph_name, game_index, time_step, \
    should_view):
    game_data = get_game_data(status_data, game_index)
    time_step_data = get_time_step_data(game_data, time_step)
    edges_attacked = time_step_data["edges_attacked"]
    nodes_attacked = time_step_data["nodes_attacked"]
    nodes_defended = time_step_data["nodes_defended"]
    attacker_nodes_after = time_step_data["attacker_nodes_after"]

    short_graph_name = graph_name[:-1 * len(".json")]
    graph = Digraph(comment=short_graph_name)
    add_nodes_fancy(graph, json_data, attacker_nodes_after, nodes_attacked, nodes_defended)
    add_edges_fancy(graph, json_data, edges_attacked)
    attacker_payoff = time_step_data["attacker_score"]
    defender_payoff = time_step_data["defender_score"]
    add_metadata(graph, time_step, attacker_payoff, defender_payoff)

    # print(graph.source)
    output_name = "out_" + short_graph_name + "_g" + str(game_index) + \
        "_t" + str(time_step) + ".gv"
    graph.render(output_name, view=should_view)
    remove(output_name)

def make_all_game_graphs(status_data, json_data, graph_name, game_index):
    game_data = get_game_data(status_data, game_index)
    max_time = get_max_time(game_data)
    if max_time == -1:
        print("Skipping: no game data")
        return
    for time_step in range(max_time + 1):
        time_step_data = get_time_step_data(game_data, time_step)
        edges_attacked = time_step_data["edges_attacked"]
        nodes_attacked = time_step_data["nodes_attacked"]
        nodes_defended = time_step_data["nodes_defended"]
        attacker_nodes_after = time_step_data["attacker_nodes_after"]

        short_graph_name = graph_name[:-1 * len(".json")]
        graph = Digraph(comment=short_graph_name)
        add_nodes_fancy(graph, json_data, attacker_nodes_after, nodes_attacked, \
            nodes_defended)
        add_edges_fancy(graph, json_data, edges_attacked)
        attacker_payoff = time_step_data["attacker_score"]
        defender_payoff = time_step_data["defender_score"]
        add_metadata(graph, time_step, attacker_payoff, defender_payoff)

        # print(graph.source)
        output_name = "out_" + short_graph_name + "_g" + str(game_index) + \
            "_t" + str(time_step) + ".gv"
        graph.render(output_name, view=False)
        remove(output_name)

def run_make_graph(status_file_name, game_index, time_step):
    status_data = get_json_data(status_file_name)
    graph_name = status_data["graph_name"]
    json_data = get_json_data(graph_name)
    make_game_time_graph(status_data, json_data, graph_name, game_index, time_step, True)

def run_make_all_graphs(status_file_name, game_index):
    status_data = get_json_data(status_file_name)
    graph_name = status_data["graph_name"]
    json_data = get_json_data(graph_name)
    make_all_game_graphs(status_data, json_data, graph_name, game_index)

def main():
    status_file_name = "test_game_result.json"
    game_index = 1
    # time_step = 5
    # run_make_graph(status_file_name, game_index, time_step)
    run_make_all_graphs(status_file_name, game_index)

'''
python3 vis_net_plus.py
'''
if __name__ == "__main__":
    main()
