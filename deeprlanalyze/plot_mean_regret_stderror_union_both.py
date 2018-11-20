import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_mean_payoffs import get_means, get_standard_errors
from runs_analyze import get_unioned_decoded_result_name, get_all_defender_mixed_strats, \
    get_all_attacker_mixed_strats
from get_both_payoffs_from_game import get_json_data, get_att_and_def_eq_payoffs
from regret_analyze import get_run_names
from append_net_names import get_truth_value
from plot_mean_regret_stderror_union import get_selected_lists, filter_file_run_names, \
    get_run_to_att_regrets_or_none, get_run_to_def_regrets_or_none, get_mins, get_maxes, \
    get_high

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def plot_regrets_with_stderr_both(hado_def_regrets, hado_att_regrets, hado_def_errs, \
    hado_att_errs, do_def_regrets, do_att_regrets, do_def_errs, do_att_errs, \
    run_name, iteration):
    fig, ax = plt.subplots()
    fig.set_size_inches(10, 5)

    ax = plt.subplot(1, 2, 2)
    my_lw = 2
    def_errs = hado_def_errs[:len(hado_def_regrets)]
    att_errs = hado_att_errs[:len(hado_att_regrets)]
    plt.plot(range(len(hado_def_regrets)), hado_def_regrets, lw=my_lw, \
        label='Def. regret', color='blue', marker='p', markersize=7)
    def_mins = get_mins(hado_def_regrets, def_errs)
    def_maxes = get_maxes(hado_def_regrets, def_errs)

    for i in range(len(hado_def_regrets)):
        if def_mins[i] is not None and def_maxes[i] is not None:
            plt.vlines(x=i, ymin=def_mins[i], ymax=def_maxes[i], color='blue', lw=my_lw)

    plt.plot(range(len(hado_att_regrets)), hado_att_regrets, lw=my_lw, \
        label='Att. regret', color='orange', linestyle='--', marker='o', markersize=7)
    att_mins = get_mins(hado_att_regrets, att_errs)
    att_maxes = get_maxes(hado_att_regrets, att_errs)

    for i in range(len(hado_att_regrets)):
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
    plt.title("HADO-EGTA", fontsize=20)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 4
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.legend()
    y_max = max(get_high(do_def_regrets, def_errs), get_high(do_att_regrets, att_errs))
    ax.set_ylim(-1, y_max + 5)
    x_max = max(len(do_def_regrets), len(hado_def_regrets))
    ax.set_xlim(-1, x_max + 1)
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

    ax = plt.subplot(1, 2, 1)

    def_errs = do_def_errs[:len(do_def_regrets)]
    att_errs = do_att_errs[:len(do_att_regrets)]
    plt.plot(range(len(do_def_regrets)), do_def_regrets, lw=my_lw, \
        label='Def. regret', color='blue', marker='p', markersize=7)
    def_mins = get_mins(do_def_regrets, def_errs)
    def_maxes = get_maxes(do_def_regrets, def_errs)

    for i in range(len(do_def_regrets)):
        if def_mins[i] is not None and def_maxes[i] is not None:
            plt.vlines(x=i, ymin=def_mins[i], ymax=def_maxes[i], color='blue', lw=my_lw)

    plt.plot(range(len(do_att_regrets)), do_att_regrets, lw=my_lw, \
        label='Att. regret', color='orange', linestyle='--', marker='o', markersize=7)
    att_mins = get_mins(do_att_regrets, att_errs)
    att_maxes = get_maxes(do_att_regrets, att_errs)

    for i in range(len(do_att_regrets)):
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
    plt.title("DO-EGTA", fontsize=20)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.legend()
    # y_max = max(get_high(do_def_regrets, def_errs), get_high(do_att_regrets, att_errs))
    ax.set_ylim(-1, y_max + 5)
    ax.set_xlim(-1, x_max + 1)
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
    savefig(run_name + "_mean_regret_vs_round_" + str(iteration) + "_both.pdf")

def plot_eq_both(game_data, defender_mixed_strat, attacker_mixed_strat, iteration, \
    hado_run_names, do_run_names):
    att_eq_payoff, def_eq_payoff = get_att_and_def_eq_payoffs(game_data, \
        attacker_mixed_strat, defender_mixed_strat)
    save_name = hado_run_names[0]
    file_run_names = get_run_names(game_data)
    hado_run_names = filter_file_run_names(file_run_names, hado_run_names)
    do_run_names = filter_file_run_names(file_run_names, do_run_names)

    hado_run_to_def_regrets = get_run_to_def_regrets_or_none(hado_run_names, game_data, \
        attacker_mixed_strat, def_eq_payoff)
    hado_run_to_att_regrets = get_run_to_att_regrets_or_none(hado_run_names, game_data, \
        defender_mixed_strat, att_eq_payoff)

    do_run_to_def_regrets = get_run_to_def_regrets_or_none(do_run_names, game_data, \
        attacker_mixed_strat, def_eq_payoff)
    do_run_to_att_regrets = get_run_to_att_regrets_or_none(do_run_names, game_data, \
        defender_mixed_strat, att_eq_payoff)

    hado_all_def_eq_regrets = get_selected_lists(hado_run_to_def_regrets, hado_run_names)
    hado_all_att_eq_regrets = get_selected_lists(hado_run_to_att_regrets, hado_run_names)

    do_all_def_eq_regrets = get_selected_lists(do_run_to_def_regrets, do_run_names)
    do_all_att_eq_regrets = get_selected_lists(do_run_to_att_regrets, do_run_names)

    hado_def_eq_regret_means = get_means(hado_all_def_eq_regrets)
    hado_att_eq_regret_means = get_means(hado_all_att_eq_regrets)
    hado_def_eq_regret_stderrs = get_standard_errors(hado_all_def_eq_regrets)
    hado_att_eq_regret_stderrs = get_standard_errors(hado_all_att_eq_regrets)

    do_def_eq_regret_means = get_means(do_all_def_eq_regrets)
    do_att_eq_regret_means = get_means(do_all_att_eq_regrets)
    do_def_eq_regret_stderrs = get_standard_errors(do_all_def_eq_regrets)
    do_att_eq_regret_stderrs = get_standard_errors(do_all_att_eq_regrets)
    print(hado_def_eq_regret_means)
    print(hado_att_eq_regret_means)
    print(hado_def_eq_regret_stderrs)
    print(hado_att_eq_regret_stderrs)
    plot_regrets_with_stderr_both(hado_def_eq_regret_means, hado_att_eq_regret_means, \
        hado_def_eq_regret_stderrs, hado_att_eq_regret_stderrs, \
        do_def_eq_regret_means, do_att_eq_regret_means, \
        do_def_eq_regret_stderrs, do_att_eq_regret_stderrs, \
        save_name, iteration)

def main(unioned_game_file, is_r30):
    hado_run_names = None
    do_run_names = None
    if is_r30:
        hado_run_names = ["d30cd1", "d30cm1"]
        do_run_names = ["d30n1", "d30f1", "d30f2"]
    else:
        hado_run_names = ["s29cs1", "s29ce1"]
        do_run_names = ["s29n1", "s29m1", "s29f1"]
    unioned_game_data = get_json_data(unioned_game_file)

    decoded_result_name = get_unioned_decoded_result_name(unioned_game_file)
    defender_mixed_strats = get_all_defender_mixed_strats(decoded_result_name)
    attacker_mixed_strats = get_all_attacker_mixed_strats(decoded_result_name)

    for i in range(len(defender_mixed_strats)):
        plot_eq_both(unioned_game_data, defender_mixed_strats[i], \
            attacker_mixed_strats[i], i, hado_run_names, do_run_names)

'''
example: python3 plot_mean_regret_stderror_union_both.py \
    combined_outputs/game_comb_d30_cd1_cm35_n1_f1_2f25.json True
'''
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: unioned_game_file, is_r30")
    UNIONED_GAME_FILE = sys.argv[1]

    IS_R30 = get_truth_value(sys.argv[2])
    main(UNIONED_GAME_FILE, IS_R30)
