import sys
import time
from math import sqrt
from create_weighted_mixed_strat import get_strat_from_file
from get_both_payoffs_from_game import get_json_data
from generate_new_cols_curve import get_net_scope
from cli_enjoy_dg_att_net import get_payoffs_att_net_with_sd
from cli_enjoy_dg_def_net import get_payoffs_def_net_with_sd
from cli_enjoy_dg_two_sided import get_payoffs_both_with_sd
from append_net_names import get_truth_value

def is_old_network(strat_name):
    return "dg_dqmlp_rand30NoAnd_B_" in strat_name or \
        "dg_rand_30n_noAnd_B_" in strat_name or \
        "depgraph_dq_mlp_rand_epoch" in strat_name

def get_variance(counts, means, variances, overall_mean):
    # v = (1 / (n - 1)) * [\sum n_i (m_i - m)^2 + \sum (n_i - 1) v_i]
    total_count = sum(counts)
    term = 0
    for cur_index in range(len(means)):
        term += counts[cur_index] * ((means[cur_index] - overall_mean) ** 2) + \
            (counts[cur_index] - 1) * variances[cur_index]
    return term / (total_count - 1.)

def old_get_net_scope(net_name):
    # name is like:
    # *epochNUM_* or *epochNUM[a-z]* or *epochNUM.pkl, where NUM is an integer > 1,
    # unless "epoch" is absent, in which case return None.
    #
    # if "epoch" is absent: return None.
    # else if NUM is 2: return "deepq_train".
    # else: return "deepq_train_eNUM", inserting the integer for NUM
    if net_name in ["dg_rand_30n_noAnd_B_eq_2.pkl", "dg_dqmlp_rand30NoAnd_B_att_fixed.pkl"]:
        return None

    epoch_index = net_name.find('epoch')
    num_start_index = epoch_index + len("epoch")

    underbar_index = net_name.find('_', num_start_index + 1)
    dot_index = net_name.find('.', num_start_index + 1)
    e_index = net_name.find('e', num_start_index + 1)
    candidates = [x for x in [underbar_index, dot_index, e_index] if x > -1]
    num_end_index = min(candidates)
    net_num = net_name[num_start_index : num_end_index]
    if net_num == "2":
        return "deepq_train"
    return "deepq_train_e" + str(net_num)

def hybrid_get_net_scope(strat_name):
    if not is_old_network(strat_name):
        return get_net_scope(strat_name)
    return old_get_net_scope(strat_name)

def get_strat_tuple_counts(att_mixed_strat, def_mixed_strat, num_sims):
    '''
    *_mixed_strat is a dictionary mapping:
    -- strat name to probability of play
    Result will be like list of:
    (att_pure_strat, def_pure_strat, cur_sims),
    where cur_sims is in {1, . . ., num_sims} and sums to num_sims.
    '''
    if num_sims < 1:
        raise ValueError("num_sims must be positive")
    result = []
    for att_pure_strat, att_prob in att_mixed_strat.items():
        for def_pure_strat, def_prob in def_mixed_strat.items():
            expected_count = int(att_prob * def_prob * num_sims)
            if expected_count > 0:
                cur_tuple = (att_pure_strat, def_pure_strat, expected_count)
                result.append(cur_tuple)
    return result

def get_att_and_def_payoffs_with_stdev(game_data, attacker, defender):
    for profile in game_data["profiles"]:
        def_payoff = None
        att_payoff = None
        def_stdev = None
        att_stdev = None
        for group in profile["symmetry_groups"]:
            if group["strategy"] == defender and group["role"] == "defender":
                def_payoff = group["payoff"]
                def_stdev = group["payoff_sd"]
            if group["strategy"] == attacker and group["role"] == "attacker":
                att_payoff = group["payoff"]
                att_stdev = group["payoff_sd"]
        if def_payoff is not None and att_payoff is not None:
            return (att_payoff, def_payoff, att_stdev, def_stdev)
    raise ValueError("Missing payoffs: " + attacker + "\t" + defender)

def weighted_mean(values, weights):
    return sum([x * y for x, y in zip(values, weights)]) * 1. / sum(weights)

def get_payoffs_with_stderr(tuple_counts, env_name_def_net, env_name_att_net, \
    env_name_both, game_file, graph_name, should_print):
    def_means = []
    att_means = []
    def_stdevs = []
    att_stdevs = []
    counts = [x[2] for x in tuple_counts]

    game_data = get_json_data(game_file)

    fmt = "{0:.4f}"
    for att_strat, def_strat, cur_count in tuple_counts:
        if should_print:
            print("Will run: " + def_strat + " vs. " + att_strat + ", " + str(cur_count) + \
                " times.")
        att_payoff, def_payoff, att_stdev, def_stdev = None, None, None, None
        start_cur = time.time()
        if not is_network(att_strat) and not is_network(def_strat):
            # get expected payoff from game file
            att_payoff, def_payoff, att_stdev, def_stdev = \
                get_att_and_def_payoffs_with_stdev(game_data, att_strat, def_strat)
        elif is_network(att_strat) and not is_network(def_strat):
            # run att net vs. def heuristic
            att_scope = hybrid_get_net_scope(att_strat)
            def_payoff, att_payoff, def_stdev, att_stdev = get_payoffs_att_net_with_sd( \
                env_name_att_net, cur_count, def_strat, att_strat, graph_name, att_scope)
        elif not is_network(att_strat) and is_network(def_strat):
            # run att heuristic vs. def net
            def_scope = hybrid_get_net_scope(def_strat)
            def_payoff, att_payoff, def_stdev, att_stdev = get_payoffs_def_net_with_sd( \
                env_name_def_net, cur_count, def_strat, att_strat, graph_name, def_scope)
        else:
            # run att net vs. def net
            att_scope = hybrid_get_net_scope(att_strat)
            def_scope = hybrid_get_net_scope(def_strat)
            def_payoff, att_payoff, def_stdev, att_stdev = \
                get_payoffs_both_with_sd(env_name_both, cur_count, def_strat, \
                    att_strat, graph_name, def_scope, att_scope)
        duration_cur = time.time() - start_cur
        def_means.append(def_payoff)
        att_means.append(att_payoff)
        def_stdevs.append(def_stdev)
        att_stdevs.append(att_stdev)
        if should_print:
            print(fmt.format(def_payoff) + "\t" + fmt.format(att_payoff) + \
                fmt.format(def_stdev) + fmt.format(att_stdev))
            print("Seconds taken current: " + str(int(duration_cur)))
    def_mean = weighted_mean(def_means, counts)
    att_mean = weighted_mean(att_means, counts)

    def_vars = [x ** 2 for x in def_stdevs]
    att_vars = [x ** 2 for x in att_stdevs]
    def_variance = get_variance(counts, def_means, def_vars, def_mean)
    att_variance = get_variance(counts, att_means, att_vars, att_mean)
    def_stderr = sqrt(def_variance) / sqrt(sum(counts))
    att_stderr = sqrt(att_variance) / sqrt(sum(counts))
    return def_mean, att_mean, def_stderr, att_stderr

def is_network(strat_name):
    return ".pkl" in strat_name

def main(env_name_def_net, env_name_att_net, env_name_both, game_file, num_sims, \
    att_mixed_strat_file, def_mixed_strat_file, graph_name, should_print):
    # get mixed strategies from file
    overall_start_time = time.time()
    att_strat = get_strat_from_file(att_mixed_strat_file)
    def_strat = get_strat_from_file(def_mixed_strat_file)

    tuple_counts = get_strat_tuple_counts(att_strat, def_strat, num_sims)
    def_mean, att_mean, def_stderr, att_stderr = get_payoffs_with_stderr( \
        tuple_counts, env_name_def_net, env_name_att_net, env_name_both, game_file, \
        graph_name, should_print)
    print("def_mean, att_mean, def_stderr, att_stderr:")
    fmt = "{0:.4f}"
    print(fmt.format(def_mean))
    print(fmt.format(att_mean))
    print(fmt.format(def_stderr))
    print(fmt.format(att_stderr))

    overall_duration = time.time() - overall_start_time
    minutes_taken_overall = int(overall_duration * 1. / 60)
    print("Minutes taken overall: " + str(minutes_taken_overall))

'''
example: python3 eval_mixed_strats.py DepgraphJava-v0 DepgraphJavaEnvAtt-v0 \
    DepgraphJavaEnvBoth-v0 game_3014.json 1000 d30_epoch24_att.tsv \
    d30cd1_randNoAndB_epoch21_def.tsv RandomGraph30N100E6T1_B.json True
'''
if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: env_name_def_net, env_name_att_net, " + \
                         "env_name_both, game_file, num_sims, att_mixed_strat, " + \
                         "def_mixed_strat, graph_name, should_print")
    ENV_NAME_DEF_NET = sys.argv[1]
    ENV_NAME_ATT_NET = sys.argv[2]
    ENV_NAME_BOTH = sys.argv[3]
    GAME_FILE = sys.argv[4]
    NUM_SIMS = int(sys.argv[5])
    ATT_MIXED_STRAT = sys.argv[6]
    DEF_MIXED_STRAT = sys.argv[7]
    GRAPH_NAME = sys.argv[8]
    SHOULD_PRINT = get_truth_value(sys.argv[9])
    main(ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, \
        GAME_FILE, NUM_SIMS, ATT_MIXED_STRAT, DEF_MIXED_STRAT, GRAPH_NAME, SHOULD_PRINT)
