import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_mean_payoffs import get_means, get_standard_errors
from runs_analyze import get_unioned_decoded_result_name, get_all_defender_mixed_strats, \
    get_all_attacker_mixed_strats
from regret_analyze import get_att_payoff_vs_eq, get_def_payoff_vs_eq, get_network_names, \
    get_run_names
from get_both_payoffs_from_game import get_json_data, get_att_and_def_eq_payoffs
from plot_strat_rounds import get_round

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_env_short_name_tsv(env_short_name_payoffs):
    if env_short_name_payoffs in ["s29n1", "d30n1"]:
        return env_short_name_payoffs
    return env_short_name_payoffs + "_randNoAndB"

def get_mins(regrets, errs):
    result = []
    for i in range(len(regrets)):
        if regrets[i] is not None and errs[i] is not None:
            result.append(regrets[i] - errs[i])
        else:
            result.append(None)
    return result

def get_maxes(regrets, errs):
    result = []
    for i in range(len(regrets)):
        if regrets[i] is not None and errs[i] is not None:
            result.append(regrets[i] + errs[i])
        else:
            result.append(None)
    return result

def get_high(regrets, errs):
    values = get_maxes(regrets, errs)
    return max([x for x in values if x is not None])

def get_low(regrets, errs):
    values = get_mins(regrets, errs)
    return min([x for x in values if x is not None])

def plot_regrets_with_stderr(def_regrets, att_regrets, def_errs, att_errs, \
    run_name, iteration):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    def_errs = def_errs[:len(def_regrets)]
    att_errs = att_errs[:len(att_regrets)]
    plt.plot(range(len(def_regrets)), def_regrets, lw=my_lw, label='Def. regret', \
        color='blue', marker='x', markersize=8)
    def_mins = get_mins(def_regrets, def_errs)
    def_maxes = get_maxes(def_regrets, def_errs)

    for i in range(len(def_regrets)):
        if def_mins[i] is not None and def_maxes[i] is not None:
            plt.vlines(x=i, ymin=def_mins[i], ymax=def_maxes[i], color='blue', lw=my_lw)

    plt.plot(range(len(att_regrets)), att_regrets, lw=my_lw, label='Att. regret', \
        color='orange', linestyle='--', marker='*', markersize=12)
    att_mins = get_mins(att_regrets, att_errs)
    att_maxes = get_maxes(att_regrets, att_errs)

    for i in range(len(att_regrets)):
        if att_mins[i] is not None and att_maxes[i] is not None:
            plt.vlines(x=i, ymin=att_mins[i], ymax=att_maxes[i], color='orange', lw=my_lw)

    ax.axhline(0, color='black', lw=1)
    ax.axvline(0, color='black', lw=1)

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean regret', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 2
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.legend()
    y_max = max(get_high(def_regrets, def_errs), get_high(att_regrets, att_errs))
    ax.set_ylim(-1, y_max + 2)
    plt.tick_params(
        axis='x',          # changes apply to the x-axis
        which='both',      # both major and minor ticks are affected
        top='off'         # ticks along the top edge are off
    )
    plt.tick_params(
        axis='y',          # changes apply to the y-axis
        which='both',      # both major and minor ticks are affected
        right='off'         # ticks along the right edge are off
    )
    plt.tight_layout()

    #plt.show()
    savefig(run_name + "_mean_regret_vs_round_" + str(iteration) + ".pdf")

def get_selected_lists(run_to_regrets, runs_to_include):
    return [run_to_regrets[x] for x in runs_to_include]

def filter_file_run_names(file_run_names, run_names):
    result = []
    for file_run_name in file_run_names:
        for run_name in run_names:
            if run_name in file_run_name:
                result.append(file_run_name)
                break
    return result

def get_run_to_att_regrets_or_none(run_names, game_data, defender_mixed_strat, \
    att_eq_payoff):
    run_to_att_regrets = {}
    for run_name in run_names:
        att_net_names = get_network_names(game_data, run_name, False)
        att_payoffs = [get_att_payoff_vs_eq(game_data, defender_mixed_strat, attacker) for \
            attacker in att_net_names]
        att_regrets = [max(att_eq_payoff - cur_att_payoff, 0) for cur_att_payoff in \
            att_payoffs]
        regrets_or_none = []
        max_round = max([get_round(x) for x in att_net_names])
        net_index = 0
        for i in range(1, max_round + 1):
            if get_round(att_net_names[net_index]) == i:
                regrets_or_none.append(att_regrets[net_index])
                net_index += 1
            else:
                regrets_or_none.append(None)
        run_to_att_regrets[run_name] = regrets_or_none
    return run_to_att_regrets

def get_run_to_def_regrets_or_none(run_names, game_data, attacker_mixed_strat, \
    def_eq_payoff):
    run_to_def_regrets = {}
    for run_name in run_names:
        def_net_names = get_network_names(game_data, run_name, True)
        def_payoffs = [get_def_payoff_vs_eq(game_data, attacker_mixed_strat, defender) for \
            defender in def_net_names]
        def_regrets = [max(def_eq_payoff - cur_def_payoff, 0) for cur_def_payoff in \
            def_payoffs]
        regrets_or_none = []
        max_round = max([get_round(x) for x in def_net_names])
        net_index = 0
        for i in range(1, max_round + 1):
            if get_round(def_net_names[net_index]) == i:
                regrets_or_none.append(def_regrets[net_index])
                net_index += 1
            else:
                regrets_or_none.append(None)
        run_to_def_regrets[run_name] = regrets_or_none
    return run_to_def_regrets

def plot_eq(game_data, defender_mixed_strat, attacker_mixed_strat, iteration, run_names):
    att_eq_payoff, def_eq_payoff = get_att_and_def_eq_payoffs(game_data, \
        attacker_mixed_strat, defender_mixed_strat)
    save_name = run_names[0]
    file_run_names = get_run_names(game_data)
    run_names = filter_file_run_names(file_run_names, run_names)

    run_to_def_regrets = get_run_to_def_regrets_or_none(run_names, game_data, \
        attacker_mixed_strat, def_eq_payoff)
    run_to_att_regrets = get_run_to_att_regrets_or_none(run_names, game_data, \
        defender_mixed_strat, att_eq_payoff)

    all_def_eq_regrets = get_selected_lists(run_to_def_regrets, run_names)
    all_att_eq_regrets = get_selected_lists(run_to_att_regrets, run_names)

    def_eq_regret_means = get_means(all_def_eq_regrets)
    att_eq_regret_means = get_means(all_att_eq_regrets)
    def_eq_regret_stderrs = get_standard_errors(all_def_eq_regrets)
    att_eq_regret_stderrs = get_standard_errors(all_att_eq_regrets)
    print(def_eq_regret_means)
    print(att_eq_regret_means)
    print(def_eq_regret_stderrs)
    print(att_eq_regret_stderrs)
    plot_regrets_with_stderr(def_eq_regret_means, att_eq_regret_means, \
        def_eq_regret_stderrs, att_eq_regret_stderrs, \
        save_name, iteration)

def main(unioned_game_file, run_names):
    unioned_game_data = get_json_data(unioned_game_file)

    decoded_result_name = get_unioned_decoded_result_name(unioned_game_file)
    defender_mixed_strats = get_all_defender_mixed_strats(decoded_result_name)
    attacker_mixed_strats = get_all_attacker_mixed_strats(decoded_result_name)

    for i in range(len(defender_mixed_strats)):
        plot_eq(unioned_game_data, defender_mixed_strats[i], attacker_mixed_strats[i], \
            i, run_names)

'''
example: python3 plot_mean_regret_stderror_union.py \
    combined_outputs/game_comb_d30_cd1_cm35_n1_f1_2f25.json d30cd1 d30cm1
'''
if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise ValueError("Need 2+ args: unioned_game_file, run_names+")
    UNIONED_GAME_FILE = sys.argv[1]
    RUN_NAMES_LIST = []
    for j in range(2, len(sys.argv)):
        RUN_NAMES_LIST.append(sys.argv[j])
    main(UNIONED_GAME_FILE, RUN_NAMES_LIST)
