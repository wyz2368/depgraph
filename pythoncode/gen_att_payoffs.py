'''
Run all attacker policies against a defender mixed strategy
and returns the mean and standard error of payoffs of the best attacker policy.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import time
import math
import random
import json
import os.path

from cli_enjoy_dg_two_sided import get_payoffs_both_with_sd
from cli_enjoy_dg_def_net import get_payoffs_def_net_with_sd
from cli_enjoy_dg_att_net import get_payoffs_att_net_with_sd
from cli_enjoy_dg_no_net import get_payoffs_no_net_with_sd

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

def sample_def_strat(defender_mixed_strat):
    '''
    Randomly sample a defender pure strategy from the mixed strategy,
    according to the strategies' proportions.
    '''
    value = random.random()
    prob_total = 0.0
    for strat, prob in defender_mixed_strat.items():
        prob_total += prob
        if value <= prob_total:
            return strat
    # should not get here
    return list(defender_mixed_strat.keys())[0]

def sample_def_strats(defender_mixed_strat, total_samples):
    '''
    Randomly draw a dict that holds the number of times to play each defender
    pure strategy, where each of total_samples plays is drawn according to the
    given proportions.
    '''
    result = {}
    for _ in range(total_samples):
        cur_def_strat = sample_def_strat(defender_mixed_strat)
        if cur_def_strat in result:
            result[cur_def_strat] += 1
        else:
            result[cur_def_strat] = 1
    return result

def is_network_strat(strategy_name):
    '''
    Return whether the given strategy is for a network policy, rather than a heuristic.
    '''
    return "pkl" in strategy_name

def get_total_variance(strat_counts, strat_means, strat_variances, overall_mean):
    '''
    Var(Y) = E(Var(Y|X)) + Var(E(Y|X)).
    Var(Y) = sum_x Pr(x) Var(Y|X) + sum_x Pr(x) [ E(Y|X) - E(Y) ]^2.
    '''
    num_sims = sum(strat_counts)
    result = 0.0
    for i in range(len(strat_counts)):
        pr_x = strat_counts[i] * 1.0 / num_sims
        var_y_given_x = strat_variances[i]
        result += pr_x * var_y_given_x

        e_y_given_x = strat_means[i]
        e_y = overall_mean
        squared_diff = (e_y_given_x - e_y) ** 2
        result += pr_x * squared_diff
    return result

def get_best_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, defender_mixed_strat, att_heuristics, att_networks, graph_name):
    '''
    Run each attacker network and heuristic against defender_mixed_strat,
    find the mean, standard deviation, and standard error of the payoffs,
    and return the payoff with the highest upper bound on mean payoff,
    including the attacker strategy name, number of simulations, mean payoff,
    standard deviation of payoff, and standard error of payoff.
    '''
    start_time = time.time()

    temp_results = {}
    for att_heuristic in att_heuristics:
        def_strats_dict = sample_def_strats(defender_mixed_strat, num_sims)
        total_rewards = 0.0
        strat_counts = []
        strat_means = []
        strat_variances = []
        for def_strat, count in def_strats_dict.items():
            strat_counts.append(count)
            mean_rewards_tuple = None
            if is_network_strat(def_strat):
                # run attacker heuristic vs. defender network, for count runs
                mean_rewards_tuple = get_payoffs_def_net_with_sd( \
                    env_name_def_net, count, def_strat, att_heuristic, graph_name, \
                    get_net_scope(def_strat))
            else:
                # run defender heuristic vs. attacker heuristic, for count runs
                mean_rewards_tuple = get_payoffs_no_net_with_sd( \
                    count, def_strat, att_heuristic, graph_name)
            _, att_mean, _, att_sd = mean_rewards_tuple
            total_rewards += count * att_mean
            strat_means.append(att_mean)
            strat_variances.append(att_sd ** 2)
        mean_reward = total_rewards * 1.0 / num_sims
        variance_reward = get_total_variance(strat_counts, strat_means, strat_variances, \
            mean_reward)
        std_reward = math.sqrt(variance_reward)
        se_mean = std_reward / math.sqrt(num_sims)
        print(att_heuristic + "\t" + str(mean_reward) + "\t" + \
            str(std_reward) + "\t" + str(se_mean))
        temp_results[att_heuristic] = [mean_reward, std_reward, se_mean]

    for att_network in att_networks:
        def_strats_dict = sample_def_strats(defender_mixed_strat, num_sims)
        total_rewards = 0.0
        strat_counts = []
        strat_means = []
        strat_variances = []
        for def_strat, count in def_strats_dict.items():
            strat_counts.append(count)
            mean_rewards_tuple = None
            if is_network_strat(def_strat):
                # run defender network against attacker network, for count runs
                mean_rewards_tuple = get_payoffs_both_with_sd( \
                    env_name_both, count, def_strat, att_network, graph_name, \
                    get_net_scope(def_strat), get_net_scope(att_network))
            else:
                # run attacker network against defender heuristic for count runs
                mean_rewards_tuple = get_payoffs_att_net_with_sd( \
                    env_name_att_net, count, def_strat, att_network, graph_name, \
                    get_net_scope(att_network))
            _, att_mean, _, att_sd = mean_rewards_tuple
            total_rewards += count * att_mean
            strat_means.append(att_mean)
            strat_variances.append(att_sd ** 2)
        mean_reward = total_rewards * 1.0 / num_sims
        variance_reward = get_total_variance(strat_counts, strat_means, strat_variances, \
            mean_reward)
        std_reward = math.sqrt(variance_reward)
        se_mean = std_reward / math.sqrt(num_sims)
        print(att_network + "\t" + str(mean_reward) + "\t" + \
            str(std_reward) + "\t" + str(se_mean))
        temp_results[att_network] = [mean_reward, std_reward, se_mean]

    best_strat = None
    best_reward = -1000000

    best_upper_bound_strat = None
    best_upper_bound_reward = -1000000
    for strat, cur_list in temp_results.items():
        mean_reward = cur_list[0]
        if mean_reward > best_reward:
            best_strat = strat
            best_reward = mean_reward

        se_reward = temp_results[strat][2]
        upper_bound_reward = mean_reward + se_reward
        if upper_bound_reward > best_upper_bound_reward:
            best_upper_bound_strat = strat
            best_upper_bound_reward = upper_bound_reward

    upper_bound_result = {}
    upper_bound_result["def_strategy"] = best_upper_bound_strat
    best_upper_bound_result = temp_results[best_upper_bound_strat]
    upper_bound_result["mean_reward"] = best_upper_bound_result[0]
    upper_bound_result["stdev_reward"] = best_upper_bound_result[1]
    upper_bound_result["stderr_reward"] = best_upper_bound_result[2]
    upper_bound_result["upper_bound"] = best_upper_bound_reward
    print("upper bound result: " + str(upper_bound_result))

    result = {}
    result["att_strategy"] = best_strat
    best_result = temp_results[best_strat]
    result["mean_reward"] = best_result[0]
    result["stdev_reward"] = best_result[1]
    result["stderr_reward"] = best_result[2]
    duration = time.time() - start_time
    print(result)
    print("Minutes taken: " + str(duration // 60))
    return result

def get_mixed_strat(defender_mixed_strat):
    '''
    Get the defender mixed strategy from a file, which should have one pure
    strategy per line, followed by tab, followed by the proportion playing that strategy.
    '''
    lines = get_lines(defender_mixed_strat)
    result = {}
    for line in lines:
        if not line:
            continue
        line = line.strip()
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
    '''
    Write the object to file as Json.
    '''
    with open(out_file, 'w') as my_file:
        json.dump(best_payoffs, my_file)

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

def main(env_name_def_net, env_name_att_net, env_name_both, \
    num_sims, env_short_name, graph_name, new_epoch):
    '''
    Main method: reads in the heuristic strategy names, network file names, calls for
    the game simulations to be run.
    '''
    if num_sims < 1:
        raise ValueError("num_sims must be positive: " + str(num_sims))
    out_file_name = "out_attPayoffs_" + env_short_name + "_epoch" + str(new_epoch) + ".txt"
    if os.path.isfile(out_file_name):
        print("Skipping: " + out_file_name + " already exists.")
        return

    defender_mixed_strat = env_short_name + "_epoch" + str(new_epoch) + "_def.tsv"
    def_mixed_strat = get_mixed_strat(defender_mixed_strat)

    attacker_heuristics = "attStratStrings_" + env_short_name + ".txt"
    att_heuristics = get_lines(attacker_heuristics)
    attacker_networks = "attNetStrings_" + env_short_name + ".txt"
    att_networks = get_lines(attacker_networks)
    best_payoffs = get_best_payoffs(env_name_def_net, env_name_att_net, \
        env_name_both, num_sims, def_mixed_strat, \
        att_heuristics, att_networks, graph_name)
    print_to_file(best_payoffs, out_file_name)

if __name__ == '__main__':
    if len(sys.argv) != 8:
        raise ValueError("Need 7 args: env_name_def_net, " + \
                         "env_name_att_net, env_name_both, " + \
                         "num_sims, env_short_name, graph_name, new_epoch")
    ENV_NAME_DEF_NET = sys.argv[1]
    ENV_NAME_ATT_NET = sys.argv[2]
    ENV_NAME_BOTH = sys.argv[3]
    NUM_SIMS = int(float(sys.argv[4]))
    ENV_SHORT_NAME = sys.argv[5]
    GRAPH_NAME = sys.argv[6]
    NEW_EPOCH = int(sys.argv[7])
    main(ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, \
        NUM_SIMS, ENV_SHORT_NAME, GRAPH_NAME, NEW_EPOCH)
