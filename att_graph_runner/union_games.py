import sys
import os.path
import copy
import time
from get_both_payoffs_from_game import get_json_data
from check_game_data import check_for_missing_payoffs, check_for_extra_payoffs, \
    check_for_duplicates, get_attacker_strats, get_defender_strats
from eval_mixed_strats import hybrid_get_net_scope, is_network
from cli_enjoy_dg_two_sided import get_payoffs_both
from generate_new_cols import print_json
from add_new_data import add_defender_strategy, add_attacker_strategy, get_max_profile_id, \
    get_max_symmetry_group_id, get_original_num_sims, add_profile

def is_game_data_valid(game_data):
    no_missing = check_for_missing_payoffs(game_data)
    no_extra = check_for_extra_payoffs(game_data)
    no_dups = check_for_duplicates(game_data)
    return no_dups and no_missing and no_extra

def get_attacker_networks(game_data):
    return [x for x in get_attacker_strats(game_data) if is_network(x)]

def get_defender_networks(game_data):
    return [x for x in get_defender_strats(game_data) if is_network(x)]

def get_attacker_heuristics(game_data):
    return [x for x in get_attacker_strats(game_data) if not is_network(x)]

def get_defender_heuristics(game_data):
    return [x for x in get_defender_strats(game_data) if not is_network(x)]

def generate_new_payoffs(att_nets_a, def_nets_a, att_nets_b, def_nets_b, num_sims, \
    env_name_both, graph_name, old_game_file_a, old_game_file_b):
    result = {}
    result["old_game_file_a"] = old_game_file_a
    result["old_game_file_b"] = old_game_file_b
    result["env_name_both"] = env_name_both
    result["num_sims"] = num_sims
    result["graph_name"] = graph_name
    start_time = time.time()

    a_attack_to_generate = len(att_nets_a) * len(def_nets_b)
    b_attack_to_generate = len(att_nets_b) * len(def_nets_a)
    count_to_generate = a_attack_to_generate + b_attack_to_generate
    print("Will generate: " + str(count_to_generate) + " pairs of payoffs.")
    print("Count with " + old_game_file_a + " attacker: " + str(a_attack_to_generate))
    print("Count with " + old_game_file_b + " attacker: " + str(b_attack_to_generate))
    print("a attacker nets: " + att_nets_a)
    print("a defender nets: " + def_nets_a)
    print("b attacker nets: " + att_nets_b)
    print("b defender nets: " + def_nets_b)

    cur_index = 0
    for att_net in att_nets_a:
        for def_net in def_nets_b:
            print("Payoffs " + str(cur_index) + " of " + str(count_to_generate) + ":", \
                flush=True)
            cur_start_time = time.time()
            mean_rewards_tuple = get_payoffs_both( \
                env_name_both, num_sims, def_net, att_net, graph_name, \
                hybrid_get_net_scope(def_net), hybrid_get_net_scope(att_net))
            print(str((def_net, att_net)) + "\n" + str(mean_rewards_tuple), flush=True)
            if def_net not in result:
                result[def_net] = {}
            result[def_net][att_net] = list(mean_rewards_tuple)
            cur_index += 1
            cur_duration = time.time() - cur_start_time
            print("Seconds cur pair: " + str(int(cur_duration)), flush=True)
    for att_net in att_nets_b:
        for def_net in def_nets_a:
            print("Payoffs " + str(cur_index) + " of " + str(count_to_generate) + ":", \
                flush=True)
            cur_start_time = time.time()
            mean_rewards_tuple = get_payoffs_both( \
                env_name_both, num_sims, def_net, att_net, graph_name, \
                hybrid_get_net_scope(def_net), hybrid_get_net_scope(att_net))
            print(str((def_net, att_net)) + "\n" + str(mean_rewards_tuple), flush=True)
            if def_net not in result:
                result[def_net] = {}
            result[def_net][att_net] = list(mean_rewards_tuple)
            cur_index += 1
            cur_duration = time.time() - cur_start_time
            print("Seconds cur pair: " + str(int(cur_duration)), flush=True)
    duration = time.time() - start_time
    print("Minutes taken: " + str(duration // 60))
    return result

def get_results_to_add(new_payoffs_dict, att_nets_a, def_nets_a, att_nets_b, def_nets_b):
    result = []
    for att_net in att_nets_a:
        for def_net in def_nets_b:
            (mean_def_reward, mean_att_reward) = new_payoffs_dict[def_net][att_net]
            result.append((def_net, att_net, mean_def_reward, mean_att_reward))
    for att_net in att_nets_b:
        for def_net in def_nets_a:
            (mean_def_reward, mean_att_reward) = new_payoffs_dict[def_net][att_net]
            result.append((def_net, att_net, mean_def_reward, mean_att_reward))
    return result

def get_payoffs(game_data, defender, attacker):
    '''
    Returns True iff there is a payoff entry for the defender-attacker pair.
    '''
    for profile in game_data["profiles"]:
        matches_def = False
        matches_att = False
        def_payoff = None
        att_payoff = None
        for group in profile["symmetry_groups"]:
            if group["strategy"] == defender and group["role"] == "defender":
                matches_def = True
                def_payoff = group["payoff"]
            if group["strategy"] == attacker and group["role"] == "attacker":
                matches_att = True
                att_payoff = group["payoff"]
        if matches_def and matches_att:
            return (def_payoff, att_payoff)
    raise ValueError("Payoff not found: " + defender + "\t" + attacker)

def get_game_b_results_to_add(game_data_b, att_nets_b, def_nets_b, att_heuristics, \
    def_heuristics):
    result = []
    for att_net in att_nets_b:
        for def_strat in def_heuristics:
            (mean_def_reward, mean_att_reward) = get_payoffs(game_data_b, def_strat, \
                att_net)
            result.append((def_strat, att_net, mean_def_reward, mean_att_reward))
    for att_strat in att_heuristics:
        for def_net in def_nets_b:
            (mean_def_reward, mean_att_reward) = get_payoffs(game_data_b, def_net, \
                att_strat)
            result.append((def_net, att_strat, mean_def_reward, mean_att_reward))
    return result

def union_game_data(game_data_a, game_data_b, att_nets_a, def_nets_a, att_nets_b, \
    def_nets_b, att_heuristics, def_heuristics, new_payoffs_dict, old_game_file_a, \
    old_game_file_b):
    result = copy.deepcopy(game_data_a)
    for def_net in def_nets_b:
        add_defender_strategy(result, def_net)
    for att_net in att_nets_b:
        add_attacker_strategy(result, att_net)

    original_num_sims = get_original_num_sims(result)
    new_num_sims = new_payoffs_dict["num_sims"]
    next_profile_id = get_max_profile_id(result) + 1
    next_symmetry_group_id = get_max_symmetry_group_id(result) + 1
    results_to_add = get_results_to_add(new_payoffs_dict, att_nets_a, def_nets_a, \
        att_nets_b, def_nets_b)
    results_to_add.extend(get_game_b_results_to_add(game_data_b, att_nets_b, def_nets_b, \
        att_heuristics, def_heuristics))
    for result_to_add in results_to_add:
        (def_strat, att_strat, def_payoff, att_payoff) = result_to_add
        if def_strat in result and att_strat in result[def_strat]:
            print("Danger: duplicate profile: " + def_strat + " " + att_strat)
            sys.exit(1)
        add_profile(result, def_strat, att_strat, def_payoff, att_payoff, \
                    next_profile_id, next_symmetry_group_id, \
                    original_num_sims, new_num_sims)
        next_profile_id += 1
        next_symmetry_group_id += 2

    network_source = "network_source"
    result[network_source] = {}
    result[network_source][old_game_file_a] = []
    result[network_source][old_game_file_a] = []
    result[network_source][old_game_file_a].extend(att_nets_a)
    result[network_source][old_game_file_a].extend(def_nets_a)
    result[network_source][old_game_file_b].extend(att_nets_b)
    result[network_source][old_game_file_b].extend(def_nets_b)
    return result

def main(old_game_file_a, old_game_file_b, runs_per_pair, env_name_both, graph_name, \
    result_payoffs_file, result_game_file):
    print("old_game_file_a: " + old_game_file_a)
    print("old_game_file_b: " + old_game_file_b)
    print("num_sims: " + str(runs_per_pair))
    print("result_payoffs_file: " + result_payoffs_file)
    print("result_game_file: " + result_game_file)
    sys.stdout.flush()

    game_data_a = get_json_data(old_game_file_a)
    game_data_b = get_json_data(old_game_file_b)
    if not is_game_data_valid(game_data_a) or not is_game_data_valid(game_data_b):
        raise ValueError("Invalid game data")

    if os.path.isfile(result_payoffs_file):
        raise ValueError("File already exists: " + result_payoffs_file)
    if os.path.isfile(result_game_file):
        raise ValueError("File already exists: " + result_game_file)

    att_nets_a = get_attacker_networks(game_data_a)
    def_nets_a = get_defender_networks(game_data_a)
    att_nets_b = get_attacker_networks(game_data_b)
    def_nets_b = get_defender_networks(game_data_b)

    new_payoffs_dict = generate_new_payoffs(att_nets_a, def_nets_a, att_nets_b, \
        def_nets_b, runs_per_pair, env_name_both, graph_name, old_game_file_a, \
        old_game_file_b)
    print_json(result_payoffs_file, new_payoffs_dict)

    if not os.path.isfile(result_payoffs_file):
        raise ValueError(result_payoffs_file + " missing.")
    new_payoffs_dict = get_json_data(result_payoffs_file)

    att_heuristics = get_attacker_heuristics(game_data_a)
    def_heuristics = get_defender_heuristics(game_data_a)

    combined_game_data = union_game_data(game_data_a, game_data_b, att_nets_a, def_nets_a, \
        att_nets_b, def_nets_b, att_heuristics, def_heuristics, new_payoffs_dict, \
        old_game_file_a, old_game_file_b)
    print_json(result_game_file, combined_game_data)

    new_game_dict = get_json_data(result_game_file)
    if not is_game_data_valid(new_game_dict):
        raise ValueError("Invalid result data")

'''
example: python3 union_games.py game_3014_20_d30cd1.json game_3014_23.json \
   400 DepgraphJavaEnvBoth-v0 RandomGraph30N100E6T1_B.json payoffs_d30cd1_d30n1.json \
   game_comb_d30cd1_d30n1.json
'''
if __name__ == '__main__':
    if len(sys.argv) != 8:
        raise ValueError("Need 7 args: old_game_file_a, old_game_file_b, " + \
                         "runs_per_pair, env_name_both, graph_name, " + \
                         "result_payoffs_file, result_game_file")
    OLD_GAME_FILE_A = sys.argv[1]
    OLD_GAME_FILE_B = sys.argv[2]
    RUNS_PER_PAIR = int(sys.argv[3])
    ENV_NAME_BOTH = sys.argv[4]
    GRAPH_NAME = sys.argv[5]
    RESULT_PAYOFFS_FILE = sys.argv[6]
    RESULT_GAME_FILE = sys.argv[7]
    main(OLD_GAME_FILE_A, OLD_GAME_FILE_B, RUNS_PER_PAIR, ENV_NAME_BOTH, GRAPH_NAME, \
        RESULT_PAYOFFS_FILE, RESULT_GAME_FILE)
