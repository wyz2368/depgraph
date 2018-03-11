'''
Run all defender policies against an attacker mixed strategy
and returns the mean and standard error of payoffs of the best defender policy.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import time
import random

from cli_enjoy_dg_two_sided import get_payoffs_both_with_sd
from cli_enjoy_dg_def_net import get_payoffs_def_net_with_sd
from cli_enjoy_dg_att_net import get_payoffs_att_net_with_sd

def sample_att_strat(attacker_mixed_strat):
    value = random.random()
    prob_total = 0.0
    for strat, prob in attacker_mixed_strat.items():
        prob_total += prob
        if value <= prob_total:
            return strat
    # should not get here
    return list(attacker_mixed_strat.keys())[0]

def sample_att_strats(attacker_mixed_strat, total_samples):
    result = {}
    for _ in range(total_samples):
        cur_att_strat = sample_att_strat(attacker_mixed_strat)
        if cur_att_strat in result:
            result[cur_att_strat] += 1
        else:
            result[cur_att_strat] = 1
    return result

def is_network_strat(strategy_name):
    return "pkl" in strategy_name

def get_best_payoffs(env_name_neither, env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, attacker_mixed_strat, def_heuristics, def_networks, graph_name):
    '''
    Run each defender network and heuristic against attacker_mixed_strat,
    find the mean, standard deviation, and standard error of the payoffs,
    and return the payoff with the highest upper bound on mean payoff,
    including the defender strategy name, number of simulations, mean payoff,
    standard deviation of payoff, and standard error of payoff.
    '''
    result = {}
    start_time = time.time()

    for def_heuristic in def_heuristics:
        att_strats_dict = sample_att_strats(attacker_mixed_strat, num_sims)
        total_rewards = 0.0
        stdev_rewards = 0.0
        for att_strat, count in att_strats_dict.items():
            if is_network_strat(att_strat):
                # run defender heuristic vs. attacker network, for count runs
                mean_rewards_tuple = get_payoffs_att_net_with_sd( \
                    env_name_att_net, count, def_heuristic, att_strat, graph_name)
            else:
                # run defender heuristic vs. attacker heuristic, for count runs
                # TODO: make get_payoffs_no_net that returns stdev of payoffs
                pass

    for def_network in def_networks:
        att_strats_dict = sample_att_strats(attacker_mixed_strat, num_sims)
        for att_strat, count in att_strats_dict.items():
            if is_network_strat(att_strat):
                # run defender network against attacker network, for count runs
                mean_rewards_tuple = get_payoffs_both_with_sd( \
                    env_name_both, count, def_network, att_strat, graph_name)
            else:
                # run defender betwork against attack heuristic for count runs
                mean_rewards_tuple = get_payoffs_def_net_with_sd( \
                    env_name_def_net, count, def_network, att_strat, graph_name)

    duration = time.time() - start_time
    print("Minutes taken: " + str(duration // 60))
    return result

def main(env_name_neither, env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, attacker_mixed_strat, defender_heuristics, \
    defender_networks, graph_name, out_file_name):
    '''
    Main method: reads in the heuristic strategy names, network file names, calls for
    the game simulations to be run.
    '''
    if num_sims < 1:
        raise ValueError("num_sims must be positive: " + str(num_sims))
    att_mixed_strat = get_mixed_strat(attacker_mixed_strat)
    def_heuristics = get_lines(defender_heuristics)
    def_networks = get_lines(defender_networks)
    best_payoffs = get_best_payoffs(env_name_neither, env_name_def_net, env_name_att_net, \
        env_name_both, num_sims, att_mixed_strat, \
        def_heuristics, def_networks, graph_name)
    print_to_file(best_payoffs, out_file_name)

def get_mixed_strat(attacker_mixed_strat):
    lines = get_lines(attacker_mixed_strat)
    result = {}
    for line in lines:
        if not line:
            continue
        line = line.trim()
        parts = line.split("\t")
        if len(parts) != 2:
            raise ValueError("Must have 2 items per line")
        strat_name = parts[0]
        prob = float(parts[1])
        if prob < 0.0:
            raise ValueError("Can't have negative probability")
        result[strat_name] = prob
    total_prob = sum(result.values())
    tol = 0.001
    if abs(total_prob - 1.0) > tol:
        raise ValueError("Probabilities must sum to 1.0")
    return result

def print_to_file(best_payoffs, out_file):
    with open(out_file, "w") as text_file:
        text_file.write(best_payoffs)

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
        raise ValueError("Need 12 args: env_name_neither, env_name_def_net, " + \
                         "env_name_att_net, env_name_both, " + \
                         "num_sims, attacker_mixed_strat, defender_heuristics, " + \
                         "defender_networks, graph_name, out_file_name")
    ENV_NAME_NEITHER = sys.argv[1]
    ENV_NAME_DEF_NET = sys.argv[2]
    ENV_NAME_ATT_NET = sys.argv[3]
    ENV_NAME_BOTH = sys.argv[4]
    NUM_SIMS = int(float(sys.argv[5]))
    ATTACKER_MIXED_STRAT = sys.argv[6]
    DEFENDER_HEURISTICS = sys.argv[7]
    DEFENDER_NETWORKS = sys.argv[8]
    GRAPH_NAME = sys.argv[9]
    OUT_FILE_NAME = sys.argv[10]
    main(ENV_NAME_NEITHER, ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, \
        NUM_SIMS, ATTACKER_MIXED_STRAT, \
        DEFENDER_HEURISTICS, DEFENDER_NETWORKS, GRAPH_NAME, OUT_FILE_NAME)
