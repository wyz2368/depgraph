'''
Run the new attacker and defender networks against all previous networks and
heuristic strategies, and return the mean payoff tuples in a Json object.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import json

from cli_enjoy_dg_two_sided import get_payoffs_both
from cli_enjoy_dg_def_net import get_payoffs_def_net
from cli_enjoy_dg_att_net import get_payoffs_att_net

def get_result_json(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, new_defender_model, new_attacker_model, def_heuristics, \
    att_heuristics, def_networks, att_networks, graph_name):
    '''
    Run each of new_defender_model and new_attacker_model against every model for the
    opponent, num_sims times each, and return a Json object that stores the sample mean
    payoff for every resulting pair of strategies.
    '''
    result = {}

    # run new_defender_model against all att_heuristics
    for att_heuristic in att_heuristics:
        mean_rewards_tuple = get_payoffs_def_net( \
            env_name_def_net, num_sims, new_defender_model, att_heuristic, graph_name)
        cur_input_tuple = (new_defender_model, att_heuristic)
        result[cur_input_tuple] = mean_rewards_tuple
        print(str(cur_input_tuple) + "\n" + str(mean_rewards_tuple))

    # run new_defender_model against all att_networks
    for att_network in att_networks:
        mean_rewards_tuple = get_payoffs_both( \
            env_name_both, num_sims, new_defender_model, att_network, graph_name)
        cur_input_tuple = (new_defender_model, att_network)
        result[cur_input_tuple] = mean_rewards_tuple
        print(str(cur_input_tuple) + "\n" + str(mean_rewards_tuple))

    # run new_attacker_model against all def_heuristics
    for def_heuristic in def_heuristics:
        mean_rewards_tuple = get_payoffs_att_net( \
            env_name_att_net, num_sims, def_heuristic, new_attacker_model, graph_name)
        cur_input_tuple = (def_heuristic, new_attacker_model)
        result[cur_input_tuple] = mean_rewards_tuple
        print(str(cur_input_tuple) + "\n" + str(mean_rewards_tuple))

    # run new_attacker_model against all def_networks
    for def_network in def_networks:
        mean_rewards_tuple = get_payoffs_both( \
            env_name_both, num_sims, def_network, new_attacker_model, graph_name)
        cur_input_tuple = (def_network, new_attacker_model)
        result[cur_input_tuple] = mean_rewards_tuple
        print(str(cur_input_tuple) + "\n" + str(mean_rewards_tuple))

    # run new_defender_model against new_attacker_model
    mean_rewards_tuple = get_payoffs_both( \
        env_name_both, num_sims, new_defender_model, new_attacker_model, graph_name)
    cur_input_tuple = (new_defender_model, new_attacker_model)
    result[cur_input_tuple] = mean_rewards_tuple
    print(str(cur_input_tuple) + "\n" + str(mean_rewards_tuple))
    return result

def main(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, new_defender, new_attacker, defender_heuristics, \
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
    result_json = get_result_json(env_name_def_net, env_name_att_net, env_name_both, \
        num_sims, new_defender, new_attacker, \
        def_heuristics, att_heuristics, def_networks, att_networks, graph_name)
    print_json(out_file_name, result_json)

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
    if len(sys.argv) != 13:
        raise ValueError("Need 12 args: env_name_def_net, env_name_att_net, env_name_both, " +
                         "num_sims, new_defender, new_attacker, " + \
                         "defender_heuristics, attacker_heuristics, defender_networks, " + \
                         "attacker_networks, graph_name, out_file_name")
    ENV_NAME_DEF_NET = sys.argv[1]
    ENV_NAME_ATT_NET = sys.argv[2]
    ENV_NAME_BOTH = sys.argv[3]
    NUM_SIMS = int(float(sys.argv[4]))
    NEW_DEFENDER = sys.argv[5]
    NEW_ATTACKER = sys.argv[6]
    DEFENDER_HEURISTICS = sys.argv[7]
    ATTACKER_HEURISTICS = sys.argv[8]
    DEFENDER_NETWORKS = sys.argv[9]
    ATTACKER_NETWORKS = sys.argv[10]
    GRAPH_NAME = sys.argv[11]
    OUT_FILE_NAME = sys.argv[12]
    main(ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, \
        NUM_SIMS, NEW_DEFENDER, NEW_ATTACKER, DEFENDER_HEURISTICS, \
        ATTACKER_HEURISTICS, DEFENDER_NETWORKS, ATTACKER_NETWORKS, GRAPH_NAME, OUT_FILE_NAME)
