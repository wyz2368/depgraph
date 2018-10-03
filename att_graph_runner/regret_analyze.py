import sys
import numpy as np
from scipy import stats
from runs_analyze import get_unioned_decoded_result_name, get_all_defender_mixed_strats, \
    get_all_attacker_mixed_strats
from get_both_payoffs_from_game import get_json_data, get_att_and_def_eq_payoffs, \
    get_att_and_def_payoffs

def get_run_names(game_data):
    return list(game_data["network_source"].keys())

def get_network_names(game_data, run_name, is_defender):
    run_networks = game_data["network_source"][run_name]
    if is_defender:
        def_networks = [x for x in run_networks if "att" not in x]
        return def_networks
    att_networks = [x for x in run_networks if "att" in x]
    return att_networks

def get_def_payoff_vs_eq(game_data, attacker_eq, defender):
    def_result = 0
    for attacker, att_weight in attacker_eq.items():
        _, def_payoff = get_att_and_def_payoffs(game_data, attacker, defender)
        def_result += att_weight * def_payoff
    return def_result

def get_att_payoff_vs_eq(game_data, defender_eq, attacker):
    att_result = 0
    for defender, def_weight in defender_eq.items():
        att_payoff, _ = get_att_and_def_payoffs(game_data, attacker, defender)
        att_result += def_weight * att_payoff
    return att_result

def get_run_to_def_regrets(run_names, game_data, attacker_mixed_strat, def_eq_payoff):
    run_to_def_regrets = {}
    for run_name in run_names:
        def_net_names = get_network_names(game_data, run_name, True)
        def_payoffs = [get_def_payoff_vs_eq(game_data, attacker_mixed_strat, defender) for \
            defender in def_net_names]
        def_regrets = [max(def_eq_payoff - cur_def_payoff, 0) for cur_def_payoff in \
            def_payoffs]
        run_to_def_regrets[run_name] = def_regrets
    return run_to_def_regrets

def get_run_to_att_regrets(run_names, game_data, defender_mixed_strat, att_eq_payoff):
    run_to_att_regrets = {}
    for run_name in run_names:
        att_net_names = get_network_names(game_data, run_name, False)
        att_payoffs = [get_att_payoff_vs_eq(game_data, defender_mixed_strat, attacker) for \
            attacker in att_net_names]
        att_regrets = [max(att_eq_payoff - cur_att_payoff, 0) for cur_att_payoff in \
            att_payoffs]
        run_to_att_regrets[run_name] = att_regrets
    return run_to_att_regrets

def mean_nonzero(values):
    tol = 0.1
    values = [x for x in values if abs(x) > tol]
    return np.mean(values)

def analyze_mean_ranks(run_names, run_to_def_ranks, run_to_att_ranks):
    fmt = "{0:.3f}"
    for run_name in run_names:
        mean_def_regret = np.mean(run_to_def_ranks[run_name])
        mean_att_regret = np.mean(run_to_att_ranks[run_name])
        print(run_name + ", def mean rank: " + fmt.format(mean_def_regret) + \
            ", att mean rank: " + fmt.format(mean_att_regret))

def analyze_mean_nonzero_ranks(run_names, run_to_def_ranks, run_to_att_ranks):
    fmt = "{0:.3f}"
    for run_name in run_names:
        mean_def_regret = mean_nonzero(run_to_def_ranks[run_name])
        mean_att_regret = mean_nonzero(run_to_att_ranks[run_name])
        print(run_name + ", def mean nonzero rank: " + fmt.format(mean_def_regret) + \
            ", att mean nonzero rank: " + fmt.format(mean_att_regret))

def analyze_means(run_names, run_to_def_regrets, run_to_att_regrets):
    fmt = "{0:.3f}"
    for run_name in run_names:
        mean_def_regret = np.mean(run_to_def_regrets[run_name])
        mean_att_regret = np.mean(run_to_att_regrets[run_name])
        print(run_name + ", def mean regret: " + fmt.format(mean_def_regret) + \
            ", att mean regret: " + fmt.format(mean_att_regret))

def analyze_means_nonzero(run_names, run_to_def_regrets, run_to_att_regrets):
    fmt = "{0:.3f}"
    for run_name in run_names:
        mean_def_regret = mean_nonzero(run_to_def_regrets[run_name])
        mean_att_regret = mean_nonzero(run_to_att_regrets[run_name])
        print(run_name + ", def mean nonzero regret: " + fmt.format(mean_def_regret) + \
            ", att mean nonzero regret: " + fmt.format(mean_att_regret))

def get_run_to_ranks(run_to_regrets):
    to_sort = []
    run_to_ranks = {}
    for run_name, regrets in run_to_regrets.items():
        run_to_ranks[run_name] = []
        for regret in regrets:
            to_sort.append((regret, run_name))
    to_sort.sort(key=lambda tup: tup[0])

    cur_rank = 0
    for i in range(len(to_sort)):
        if i > 0 and to_sort[i][0] > to_sort[i - 1][0]:
            cur_rank += 1
        cur_run_name = to_sort[i][1]
        run_to_ranks[cur_run_name].append(cur_rank)
    return run_to_ranks

def welch_t_test_regrets(run_names, run_to_regrets):
    if len(run_names) != 2:
        raise ValueError("Expected two classes to compare: " + str(run_names))

    left_vals = run_to_regrets[run_names[0]]
    right_vals = run_to_regrets[run_names[1]]
    _, p_value = stats.ttest_ind(left_vals, right_vals, equal_var=False)
    return p_value

def analyze_eq(game_data, defender_mixed_strat, attacker_mixed_strat):
    att_eq_payoff, def_eq_payoff = get_att_and_def_eq_payoffs(game_data, \
        attacker_mixed_strat, defender_mixed_strat)
    run_names = get_run_names(game_data)

    run_to_def_regrets = get_run_to_def_regrets(run_names, game_data, \
        attacker_mixed_strat, def_eq_payoff)
    run_to_att_regrets = get_run_to_att_regrets(run_names, game_data, \
        defender_mixed_strat, att_eq_payoff)
    analyze_means(run_names, run_to_def_regrets, run_to_att_regrets)
    analyze_means_nonzero(run_names, run_to_def_regrets, run_to_att_regrets)
    fmt = "{0:.3f}"
    print("defender regrets two-sided p-value: " + fmt.format(welch_t_test_regrets(\
        run_names, run_to_def_regrets)))
    print("attacker regrets two-sided p-value: " + fmt.format(welch_t_test_regrets(\
        run_names, run_to_att_regrets)))

    run_to_def_ranks = get_run_to_ranks(run_to_def_regrets)
    run_to_att_ranks = get_run_to_ranks(run_to_att_regrets)
    analyze_mean_ranks(run_names, run_to_def_ranks, run_to_att_ranks)
    analyze_mean_nonzero_ranks(run_names, run_to_def_ranks, run_to_att_ranks)

def analyze_all_eqs(game_data, defender_mixed_strats, attacker_mixed_strats):
    for i in range(len(defender_mixed_strats)):
        analyze_eq(game_data, defender_mixed_strats[i], attacker_mixed_strats[i])

def main(unioned_game_file):
    unioned_game_data = get_json_data(unioned_game_file)

    decoded_result_name = get_unioned_decoded_result_name(unioned_game_file)
    defender_mixed_strats = get_all_defender_mixed_strats(decoded_result_name)
    attacker_mixed_strats = get_all_attacker_mixed_strats(decoded_result_name)
    analyze_all_eqs(unioned_game_data, defender_mixed_strats, attacker_mixed_strats)

'''
example: python3 regret_analyze.py combined_outputs/game_comb_d30cd1_d30n1.json
'''
if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: unioned_game_file")
    UNIONED_GAME_FILE = sys.argv[1]
    main(UNIONED_GAME_FILE)
