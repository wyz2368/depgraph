import sys
import math
import numpy as np
from plot_payoffs_auto import get_all_payoffs, get_round_count, \
    get_cutoff_dev_payoffs, plot_payoffs

def get_means(payoff_lists):
    result = []
    max_len = 0
    for cur_list in payoff_lists:
        if cur_list:
            if cur_list[-1] is None:
                max_len = max(max_len, len(cur_list) - 1)
            else:
                max_len = max(max_len, len(cur_list))
    for i in range(max_len):
        sum_i = sum([x[i] if len(x) > i and x[i] is not None \
            else 0 for x in payoff_lists])
        count_i = sum([1 if len(x) > i and x[i] is not None \
            else 0 for x in payoff_lists])
        if count_i == 0:
            result.append(None)
        else:
            result.append(sum_i * 1.0 / count_i)
    return result

def get_standard_errors(payoff_lists):
    result = []
    max_len = 0
    for cur_list in payoff_lists:
        if cur_list:
            if cur_list[-1] is None:
                max_len = max(max_len, len(cur_list) - 1)
            else:
                max_len = max(max_len, len(cur_list))
    for i in range(max_len):
        cur_values = [x[i] for x in payoff_lists if len(x) > i and x[i] is not None]
        if not cur_values:
            result.append(0.0)
        else:
            cur_stdev = np.std(cur_values)
            cur_count = len(cur_values)
            cur_standard_error = cur_stdev * 1.0 / math.sqrt(cur_count)
            result.append(cur_standard_error)
    #for i in range(len(payoff_lists[0])):
    #    print([x[i] for x in payoff_lists])
    #    print(result[i])
    return result

def get_gain(net_payoff, eq_payoff):
    if net_payoff is None or eq_payoff is None:
        return 0
    return max(net_payoff - eq_payoff, 0)

def get_termination_rounds(game_number, env_short_name_payoffs_list, \
    env_short_name_tsv_list):
    stopped_rounds = []
    for i in range(len(env_short_name_payoffs_list)):
        env_short_name_payoffs = env_short_name_payoffs_list[i]
        env_short_name_tsv = env_short_name_tsv_list[i]
        round_count = get_round_count(env_short_name_tsv)
        att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
            get_all_payoffs(round_count, env_short_name_tsv, \
            env_short_name_payoffs, game_number)
        cur_att_gains = [get_gain(att_net_payoffs[j], att_eq_payoffs[j]) for j in \
            range(len(att_eq_payoffs))]
        cur_def_gains = [get_gain(def_net_payoffs[j], def_eq_payoffs[j]) for j in \
            range(len(def_eq_payoffs))]
        stopped_rounds.append(round_count - 1)
    return stopped_rounds

def get_all_mean_gains_with_standard_errors(game_number, env_short_name_payoffs_list, \
    env_short_name_tsv_list):
    all_att_gains = []
    all_def_gains = []
    for i in range(len(env_short_name_payoffs_list)):
        env_short_name_payoffs = env_short_name_payoffs_list[i]
        env_short_name_tsv = env_short_name_tsv_list[i]
        round_count = get_round_count(env_short_name_tsv)
        att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
            get_all_payoffs(round_count, env_short_name_tsv, \
            env_short_name_payoffs, game_number)
        cur_att_gains = [get_gain(att_net_payoffs[j], att_eq_payoffs[j]) for j in \
            range(len(att_eq_payoffs))]
        cur_def_gains = [get_gain(def_net_payoffs[j], def_eq_payoffs[j]) for j in \
            range(len(def_eq_payoffs))]
        all_att_gains.append(cur_att_gains)
        all_def_gains.append(cur_def_gains)
    mean_att_gain = get_means(all_att_gains)
    mean_def_gain = get_means(all_def_gains)
    sem_att_gain = get_standard_errors(all_att_gains)
    sem_def_gain = get_standard_errors(all_def_gains)
    return mean_att_gain, mean_def_gain, sem_att_gain, sem_def_gain

def get_all_mean_payoffs_with_standard_errors(game_number, env_short_name_payoffs_list, \
    env_short_name_tsv_list):
    all_att_eq = []
    all_def_eq = []
    all_att_net = []
    all_def_net = []
    for i in range(len(env_short_name_payoffs_list)):
        env_short_name_payoffs = env_short_name_payoffs_list[i]
        env_short_name_tsv = env_short_name_tsv_list[i]
        round_count = get_round_count(env_short_name_tsv)
        att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
            get_all_payoffs(round_count, env_short_name_tsv, \
            env_short_name_payoffs, game_number)
        all_att_eq.append(att_eq_payoffs)
        all_def_eq.append(def_eq_payoffs)
        all_att_net.append(att_net_payoffs)
        all_def_net.append(def_net_payoffs)
    mean_att_eq = get_means(all_att_eq)
    mean_def_eq = get_means(all_def_eq)
    mean_att_net = get_means(all_att_net)
    mean_def_net = get_means(all_def_net)
    sem_att_eq = get_standard_errors(all_att_eq)
    sem_def_eq = get_standard_errors(all_def_eq)
    sem_att_net = get_standard_errors(all_att_net)
    sem_def_net = get_standard_errors(all_def_net)
    return mean_att_eq, mean_def_eq, mean_att_net, mean_def_net, \
        sem_att_eq, sem_def_eq, sem_att_net, sem_def_net

def get_all_mean_payoffs(game_number, env_short_name_payoffs_list, \
    env_short_name_tsv_list):
    all_att_eq = []
    all_def_eq = []
    all_att_net = []
    all_def_net = []
    for i in range(len(env_short_name_payoffs_list)):
        env_short_name_payoffs = env_short_name_payoffs_list[i]
        env_short_name_tsv = env_short_name_tsv_list[i]
        round_count = get_round_count(env_short_name_tsv)
        att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
            get_all_payoffs(round_count, env_short_name_tsv, \
            env_short_name_payoffs, game_number)
        all_att_eq.append(att_eq_payoffs)
        all_def_eq.append(def_eq_payoffs)
        all_att_net.append(att_net_payoffs)
        all_def_net.append(def_net_payoffs)
    mean_att_eq = get_means(all_att_eq)
    mean_def_eq = get_means(all_def_eq)
    mean_att_net = get_means(all_att_net)
    mean_def_net = get_means(all_def_net)
    return mean_att_eq, mean_def_eq, mean_att_net, mean_def_net

def main(game_number, env_short_name_payoffs_list):
    print("Using environments: " + str(env_short_name_payoffs_list))
    env_short_name_tsv_list = [x + "_randNoAndB" for x in \
        env_short_name_payoffs_list]
    mean_att_eq, mean_def_eq, mean_att_net, mean_def_net = \
        get_all_mean_payoffs(game_number, env_short_name_payoffs_list, \
        env_short_name_tsv_list)
    mean_att_net = get_cutoff_dev_payoffs(mean_att_eq, mean_att_net)
    mean_def_net = get_cutoff_dev_payoffs(mean_def_eq, mean_def_net)
    print(mean_att_eq)
    print(mean_att_net)
    print(mean_def_eq)
    print(mean_def_net)
    save_env_name = env_short_name_payoffs_list[0] + "_mean"
    plot_payoffs(mean_def_eq, mean_def_net, mean_att_eq, mean_att_net, \
        save_env_name, True)

'''
example: python3 plot_mean_payoffs.py 3014 d30d1 d30m1
'''
if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise ValueError("Need 2+ args: game_number, env_short_name_payoffs+")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS_LIST = []
    for j in range(2, len(sys.argv)):
        ENV_SHORT_NAME_PAYOFFS_LIST.append(sys.argv[j])
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS_LIST)
