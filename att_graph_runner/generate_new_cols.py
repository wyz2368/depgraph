'''
Run the new attacker and defender networks against all previous networks and
heuristic strategies, and return the mean payoff tuples in a Json object.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import json
import time
import os.path

from cli_enjoy_dg_two_sided import get_payoffs_both
from cli_enjoy_dg_def_net import get_payoffs_def_net
from cli_enjoy_dg_att_net import get_payoffs_att_net

def get_net_scope(net_name):
    # defender name is like:
    # *_epochNUM.pkl, where NUM is an integer >= 1.
    #
    # attacker name is like:
    # *_epochNUM_att.pkl, where NUM is an integer >= 1.
    #
    # if NUM == 1: return "deepq_train"
    # else: return "deepq_train_eNUM", inserting the integer for NUM
    if "epoch1.pkl" in net_name or "epoch1_att.pkl" in net_name:
        # first round is special case: don't add _e1
        return "deepq_train"

    epoch_index = net_name.find('epoch')
    num_start_index = epoch_index + len("epoch")
    num_end_index = None
    if "_att.pkl" in net_name:
        # attacker network
        num_end_index = net_name.find("_att.pkl", num_start_index)
    else:
        # defender network
        num_end_index = net_name.find(".pkl", num_start_index)
    return "deepq_train_e" + net_name[num_start_index : num_end_index]

def get_result_dict(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, new_defender_model, new_attacker_model, def_heuristics, \
    att_heuristics, def_networks, att_networks, graph_name):
    '''
    Run each of new_defender_model and new_attacker_model against every model for the
    opponent, num_sims times each, and return a Json object that stores the sample mean
    payoff for every resulting pair of strategies.
    '''
    if new_defender_model is None and new_attacker_model is None:
        raise ValueError("Both new models cannot be None.")

    result = {}
    if new_defender_model is not None:
        result[new_defender_model] = {}
    if new_attacker_model is not None:
        result[new_attacker_model] = {}
    result["env_name_def_net"] = env_name_def_net
    result["env_name_att_net"] = env_name_att_net
    result["env_name_both"] = env_name_both
    result["num_sims"] = num_sims
    result["graph_name"] = graph_name
    start_time = time.time()

    if new_defender_model is not None:
        # run new_defender_model against all att_heuristics
        start_time_att_heuristics = time.time()
        for att_heuristic in att_heuristics:
            mean_rewards_tuple = get_payoffs_def_net( \
                env_name_def_net, num_sims, new_defender_model, att_heuristic, graph_name, \
                get_net_scope(new_defender_model))
            print(str((new_defender_model, att_heuristic)) + "\n" + str(mean_rewards_tuple))
            result[new_defender_model][att_heuristic] = list(mean_rewards_tuple)
        if att_heuristics:
            duration_att_heuristics = time.time() - start_time_att_heuristics
            time_per_att_heuristic = int(duration_att_heuristics * 1.0 / len(att_heuristics))
            print("Seconds per attacker heuristic: " + str(time_per_att_heuristic))

        # run new_defender_model against all att_networks
        start_time_att_nets = time.time()
        for att_network in att_networks:
            mean_rewards_tuple = get_payoffs_both( \
                env_name_both, num_sims, new_defender_model, att_network, graph_name, \
                get_net_scope(new_defender_model), get_net_scope(att_network))
            print(str((new_defender_model, att_network)) + "\n" + str(mean_rewards_tuple))
            result[new_defender_model][att_network] = list(mean_rewards_tuple)
        if att_networks:
            duration_att_nets = time.time() - start_time_att_nets
            time_per_att_net = int(duration_att_nets * 1.0 / len(att_networks))
            print("Seconds per attacker network: " + str(time_per_att_net))

    if new_attacker_model is not None:
        # run new_attacker_model against all def_heuristics
        start_time_def_heuristics = time.time()
        for def_heuristic in def_heuristics:
            mean_rewards_tuple = get_payoffs_att_net( \
                env_name_att_net, num_sims, def_heuristic, new_attacker_model, graph_name, \
                get_net_scope(new_attacker_model))
            print(str((def_heuristic, new_attacker_model)) + "\n" + str(mean_rewards_tuple))
            result[new_attacker_model][def_heuristic] = list(mean_rewards_tuple)
        if def_heuristics:
            duration_def_heuristics = time.time() - start_time_def_heuristics
            time_per_def_heuristic = int(duration_def_heuristics * 1.0 / len(def_heuristics))
            print("Seconds per defender heuristic: " + str(time_per_def_heuristic))

        # run new_attacker_model against all def_networks
        start_time_both_nets = time.time()
        for def_network in def_networks:
            mean_rewards_tuple = get_payoffs_both( \
                env_name_both, num_sims, def_network, new_attacker_model, graph_name, \
                get_net_scope(def_network), get_net_scope(new_attacker_model))
            print(str((def_network, new_attacker_model)) + "\n" + str(mean_rewards_tuple))
            result[new_attacker_model][def_network] = list(mean_rewards_tuple)
        duration_both_nets = int(time.time() - start_time_both_nets)
        print("Seconds playing both new networks: " + str(duration_both_nets))

    if new_defender_model is not None and new_attacker_model is not None:
        # run new_defender_model against new_attacker_model
        mean_rewards_tuple = get_payoffs_both( \
            env_name_both, num_sims, new_defender_model, new_attacker_model, graph_name, \
            get_net_scope(new_defender_model), get_net_scope(new_attacker_model))
        print(str((new_defender_model, new_attacker_model)) + "\n" + str(mean_rewards_tuple))
        result[new_defender_model][new_attacker_model] = list(mean_rewards_tuple)
    duration = time.time() - start_time
    print("Minutes taken: " + str(duration // 60))
    return result

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

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def main(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, new_epoch, env_short_name, is_def_beneficial, \
    is_att_beneficial, graph_name):
    '''
    Main method: reads in the heuristic strategy names, network file names, calls for
    the game simulations to be run, and prints the resulting Json result to file.
    '''
    if num_sims < 1:
        raise ValueError("num_sims must be positive: " + str(num_sims))

    out_file_name = "out_newPayoffs_" + env_short_name + "_epoch" + str(new_epoch) + \
        ".json"
    if os.path.isfile(out_file_name):
        print("Skipping: " + out_file_name + " already exists.")
        return

    if not is_def_beneficial and not is_att_beneficial:
        print("Skipping: neither deviation is beneficial.")
        return

    defender_heuristics = "defStratStrings_" + env_short_name + ".txt"
    def_heuristics = get_lines(defender_heuristics)

    attacker_heuristics = "attStratStrings_" + env_short_name + ".txt"
    att_heuristics = get_lines(attacker_heuristics)

    defender_networks = "oldDefNetNames_" + env_short_name + ".txt"
    def_networks = get_lines(defender_networks)

    attacker_networks = "oldAttNetNames_" + env_short_name + ".txt"
    att_networks = get_lines(attacker_networks)

    new_defender = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + ".pkl"
    if not is_def_beneficial:
        new_defender = None

    new_attacker = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + \
        "_att.pkl"
    if not is_att_beneficial:
        new_attacker = None

    result_dict = get_result_dict(env_name_def_net, env_name_att_net, env_name_both, \
        num_sims, new_defender, new_attacker, \
        def_heuristics, att_heuristics, def_networks, att_networks, graph_name)
    print_json(out_file_name, result_dict)

'''
example: python3 generate_new_cols.py DepgraphJava29N-v0 DepgraphJavaEnvAtt29N-v0 \
    DepgraphJavaEnvBoth29N-v0 400 1 sl29 True False SepLayerGraph0_noAnd_B.json
'''
if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: env_name_def_net, env_name_att_net, env_name_both, " +
                         "num_sims, new_epoch, env_short_name, is_def_beneficial, " + \
                         "is_att_beneficial, graph_name")
    ENV_NAME_DEF_NET = sys.argv[1]
    ENV_NAME_ATT_NET = sys.argv[2]
    ENV_NAME_BOTH = sys.argv[3]
    NUM_SIMS = int(float(sys.argv[4]))
    NEW_EPOCH = int(float(sys.argv[5]))
    ENV_SHORT_NAME = sys.argv[6]
    IS_DEF_BENEFICIAL = get_truth_value(sys.argv[7])
    IS_ATT_BENEFICIAL = get_truth_value(sys.argv[8])
    GRAPH_NAME = sys.argv[9]
    main(ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, \
        NUM_SIMS, NEW_EPOCH, ENV_SHORT_NAME, IS_DEF_BENEFICIAL, \
        IS_ATT_BENEFICIAL, GRAPH_NAME)
