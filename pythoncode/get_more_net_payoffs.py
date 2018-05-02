'''
Run each attacker and defender network against all other networks and
heuristic strategies, and return the mean payoff tuples in a Json object.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import json
import time

from cli_enjoy_dg_two_sided import get_payoffs_both
from cli_enjoy_dg_def_net import get_payoffs_def_net
from cli_enjoy_dg_att_net import get_payoffs_att_net

def get_net_scope(net_name):
    '''
    Return the scope in which to load a defender network, based on its name.
    '''
    if "epoch2" in net_name:
        return "deepq_train"
    if "epoch3" in net_name:
        return "deepq_train_e3"
    if "epoch4" in net_name:
        return "deepq_train_e4"
    if "epoch5" in net_name:
        return "deepq_train_e5"
    return None

def get_result_dict(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, def_heuristics, \
    att_heuristics, def_networks, att_networks, graph_name):
    '''
    Run each of def_networks and att_networks against every opponent network or heuristic,
    num_sims times each, and return a Json object that stores the sample mean
    payoff for every resulting pair of strategies.
    '''
    result = {}
    result["env_name_def_net"] = env_name_def_net
    result["env_name_att_net"] = env_name_att_net
    result["env_name_both"] = env_name_both
    result["num_sims"] = num_sims
    result["graph_name"] = graph_name
    for def_network in def_networks:
        result[def_network] = {}
    for def_heuristic in def_heuristics:
        result[def_heuristic] = {}
    start_time = time.time()

    for def_network in def_networks:
        # run def_network against all att_heuristics
        for att_heuristic in att_heuristics:
            mean_rewards_tuple = get_payoffs_def_net( \
                env_name_def_net, num_sims, def_network, att_heuristic, graph_name, \
                get_net_scope(def_network))
            print(str((def_network, att_heuristic)) + "\n" + str(mean_rewards_tuple))
            result[def_network][att_heuristic] = list(mean_rewards_tuple)

        # run def_network against all att_networks
        for att_network in att_networks:
            mean_rewards_tuple = get_payoffs_both( \
                env_name_both, num_sims, def_network, att_network, graph_name, \
                get_net_scope(def_network), get_net_scope(att_network))
            print(str((def_network, att_network)) + "\n" + str(mean_rewards_tuple))
            result[def_network][att_network] = list(mean_rewards_tuple)

    for att_network in att_networks:
        # run att_network against all def_heuristics
        for def_heuristic in def_heuristics:
            mean_rewards_tuple = get_payoffs_att_net( \
                env_name_att_net, num_sims, def_heuristic, att_network, graph_name, \
                get_net_scope(att_network))
            print(str((def_heuristic, att_network)) + "\n" + str(mean_rewards_tuple))
            result[def_heuristic][att_network] = list(mean_rewards_tuple)

    duration = time.time() - start_time
    print("Minutes taken: " + str(duration // 60))
    return result

def main(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, defender_heuristics, \
    attacker_heuristics, defender_networks, attacker_networks, graph_name, out_file_name):
    '''
    Main method: reads in the heuristic strategy names, network file names, calls for
    the game simulations to be run, and prints the resulting Json result to file.
    '''
    if num_sims < 1:
        raise ValueError("num_sims must be positive: " + str(num_sims))
    def_heuristics = get_lines(defender_heuristics)
    att_heuristics = get_lines(attacker_heuristics)
    def_networks = get_lines(defender_networks)
    att_networks = get_lines(attacker_networks)
    entry_count = len(def_networks) * (len(att_networks) + len(att_heuristics)) + \
        len(att_networks) * len(def_heuristics)
    print("Entries to generate: " + str(entry_count))
    result_dict = get_result_dict(env_name_def_net, env_name_att_net, env_name_both, \
        num_sims, \
        def_heuristics, att_heuristics, def_networks, att_networks, graph_name)
    print_json(out_file_name, result_dict)

def print_json(file_name, json_obj):
    '''
    Prints the given Json object to the given file name.
    '''
    with open(file_name, 'w') as my_file:
        json.dump(json_obj, my_file)

def get_lines(file_name):
    '''
    Returns the lines from the given file name as a list of strings, after stripping
    leading and trailing whitespace, and dropping any empty strings.
    '''
    result = None
    with open(file_name) as my_file:
        result = my_file.readlines()
    result = [x.strip() for x in result]
    result = [x for x in result if x]
    return result

if __name__ == '__main__':
    if len(sys.argv) != 11:
        raise ValueError("Need 10 args: env_name_def_net, env_name_att_net, env_name_both, " +
                         "num_sims, " + \
                         "defender_heuristics, attacker_heuristics, defender_networks, " + \
                         "attacker_networks, graph_name, out_file_name")
    ENV_NAME_DEF_NET = sys.argv[1]
    ENV_NAME_ATT_NET = sys.argv[2]
    ENV_NAME_BOTH = sys.argv[3]
    NUM_SIMS = int(float(sys.argv[4]))
    DEFENDER_HEURISTICS = sys.argv[5]
    ATTACKER_HEURISTICS = sys.argv[6]
    DEFENDER_NETWORKS = sys.argv[7]
    ATTACKER_NETWORKS = sys.argv[8]
    GRAPH_NAME = sys.argv[9]
    OUT_FILE_NAME = sys.argv[10]
    main(ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, \
        NUM_SIMS, DEFENDER_HEURISTICS, \
        ATTACKER_HEURISTICS, DEFENDER_NETWORKS, ATTACKER_NETWORKS, GRAPH_NAME, OUT_FILE_NAME)
