import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from append_net_names import get_truth_value
from plot_mean_payoffs import get_all_mean_gains_with_standard_errors, \
    get_termination_rounds
from plot_mean_gains_stderror import is_converged

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_env_short_name_tsv(env_short_name_payoffs):
    if env_short_name_payoffs in ["s29n1", "d30n1"]:
        return env_short_name_payoffs
    return env_short_name_payoffs + "_randNoAndB"

def plot_both_gains_with_stderr(do_def_gains, do_att_gains, do_def_errs, do_att_errs, \
                           hado_def_gains, hado_att_gains, hado_def_errs, hado_att_errs, \
                           env_short_name_payoffs, should_use_fill, do_stopped_rounds, \
                           hado_stopped_rounds):
    fig, ax = plt.subplots()
    fig.set_size_inches(10, 5)

    my_lw = 2

    do_def_errs = do_def_errs[:len(do_def_gains)]
    do_att_errs = do_att_errs[:len(do_att_gains)]

    hado_def_errs = hado_def_errs[:len(hado_def_gains)]
    hado_att_errs = hado_att_errs[:len(hado_att_gains)]

    do_def_highs = [x + y for x, y in zip(do_def_gains, do_def_errs)]
    do_att_highs = [x + y for x, y in zip(do_att_gains, do_att_errs)]
    hado_def_highs = [x + y for x, y in zip(hado_def_gains, hado_def_errs)]
    hado_att_highs = [x + y for x, y in zip(hado_att_gains, hado_att_errs)]
    y_max = max(max(do_def_highs), max(do_att_highs), max(hado_def_highs), \
        max(hado_att_highs))
    x_max = max(len(do_def_gains), len(hado_att_gains))

    ax = plt.subplot(1, 2, 1)
    if should_use_fill:
        fill_alpha = 0.25
        plt.plot(range(len(do_def_gains)), do_def_gains, lw=my_lw, label='Def. gain', \
            color='blue')
        def_mins = [do_def_gains[i] - do_def_errs[i] for i in range(len(do_def_errs))]
        def_maxes = [do_def_gains[i] + do_def_errs[i] for i in range(len(do_def_errs))]
        plt.fill_between(range(len(do_def_gains)), def_mins, def_maxes,
                         alpha=fill_alpha, edgecolor='blue', facecolor='blue', linewidth=0)

        plt.plot(range(len(do_att_gains)), do_att_gains, lw=my_lw, label='Att. gain', \
            color='orange', linestyle='--')
        att_mins = [do_att_gains[i] - do_att_errs[i] for i in range(len(do_att_errs))]
        att_maxes = [do_att_gains[i] + do_att_errs[i] for i in range(len(do_att_errs))]
        plt.fill_between(range(len(do_att_gains)), att_mins, att_maxes,
                         alpha=fill_alpha, edgecolor='orange', facecolor='orange', \
                         linewidth=0)
    else:
        plt.errorbar(range(len(do_def_gains)), do_def_gains, yerr=do_def_errs, lw=my_lw, \
            label='Def. gain', color='blue', elinewidth=1)
        plt.errorbar(range(len(do_att_gains)), do_att_gains, yerr=do_def_errs, lw=my_lw, \
            linestyle='--', label='Att. gain', color='orange', \
            elinewidth=1)

    for stopped_round in do_stopped_rounds:
        if is_converged(env_short_name_payoffs, stopped_round):
            plt.scatter(x=stopped_round, y=0, marker='*', s=200, zorder=3, \
                color='green')
        else:
            plt.scatter(x=stopped_round, y=0, color='r', marker='x', s=120, zorder=3)

    plt.axhline(0, color='black', lw=1)
    plt.axvline(0, color='black', lw=1)

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean gain', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 5
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    plt.legend()
    ax.set_ylim(-5, y_max + 10)
    ax.set_xlim(0, x_max)
    plt.title("DO-EGTA", fontsize=16)
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

    ax = plt.subplot(1, 2, 2)
    if should_use_fill:
        fill_alpha = 0.25
        plt.plot(range(len(hado_def_gains)), hado_def_gains, lw=my_lw, label='Def. gain', \
            color='blue')
        def_mins = [hado_def_gains[i] - hado_def_errs[i] for i in range(len(hado_def_errs))]
        def_maxes = [hado_def_gains[i] + hado_def_errs[i] for i in \
            range(len(hado_def_errs))]
        plt.fill_between(range(len(hado_def_gains)), def_mins, def_maxes,
                         alpha=fill_alpha, edgecolor='blue', facecolor='blue', linewidth=0)

        plt.plot(range(len(hado_att_gains)), hado_att_gains, lw=my_lw, label='Att. gain', \
            color='orange', linestyle='--')
        att_mins = [hado_att_gains[i] - hado_att_errs[i] for i in range(len(hado_att_errs))]
        att_maxes = [hado_att_gains[i] + hado_att_errs[i] for i in \
            range(len(hado_att_errs))]
        plt.fill_between(range(len(hado_att_gains)), att_mins, att_maxes,
                         alpha=fill_alpha, edgecolor='orange', facecolor='orange', \
                         linewidth=0)
    else:
        plt.errorbar(range(len(hado_def_gains)), hado_def_gains, yerr=do_def_errs, \
            lw=my_lw, label='Def. gain', color='blue', elinewidth=1)
        plt.errorbar(range(len(hado_att_gains)), hado_att_gains, yerr=do_def_errs, \
            lw=my_lw, linestyle='--', label='Att. gain', color='orange', elinewidth=1)

    for stopped_round in hado_stopped_rounds:
        if is_converged(env_short_name_payoffs, stopped_round):
            plt.scatter(x=stopped_round, y=0, marker='*', s=200, zorder=3, \
                color='green')
        else:
            plt.scatter(x=stopped_round, y=0, color='r', marker='x', s=120, zorder=3)

    ax.axhline(0, color='black', lw=1)
    ax.axvline(0, color='black', lw=1)

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean gain', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 5
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    plt.legend()
    ax.set_ylim(-5, y_max + 10)
    ax.set_xlim(0, x_max)
    plt.title("HADO-EGTA", fontsize=16)
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
    savefig(env_short_name_payoffs + "_gain_vs_round.pdf")

def combine_stderrs(eq_errors, net_errors):
    result = []
    max_len = max(len(eq_errors), len(net_errors))
    for i in range(max_len):
        if i < len(eq_errors) and i < len(net_errors):
            result.append(max(eq_errors[i], net_errors[i]))
        elif i < len(eq_errors):
            result.append(eq_errors[i])
        else:
            result.append(net_errors[i])
    return result

def main(is_r30):
    print("Is r30: " + str(is_r30))
    if is_r30:
        doegta_env_short_name_payoffs_list = ["d30d1", "d30m1", "d30n1", "d30f1", "d30f2"]
        hadoegta_env_short_name_payoffs_list = ["d30cd1", "d30cm1"]
        game_number = 3014
    else:
        doegta_env_short_name_payoffs_list = ["s29n1", "s29m1", "s29f1", "s29f2"]
        hadoegta_env_short_name_payoffs_list = ["s29cs1", "s29ce1"]
        game_number = 3013

    doegta_env_short_name_tsv_list = [get_env_short_name_tsv(x) for x in \
        doegta_env_short_name_payoffs_list]
    hadoegta_env_short_name_tsv_list = [get_env_short_name_tsv(x) for x in \
        hadoegta_env_short_name_payoffs_list]
    do_mean_att_gain, do_mean_def_gain, do_sem_att_gain, do_sem_def_gain = \
        get_all_mean_gains_with_standard_errors(game_number, \
            doegta_env_short_name_payoffs_list, doegta_env_short_name_tsv_list)
    hado_mean_att_gain, hado_mean_def_gain, hado_sem_att_gain, hado_sem_def_gain = \
        get_all_mean_gains_with_standard_errors(game_number, \
            hadoegta_env_short_name_payoffs_list, hadoegta_env_short_name_tsv_list)
    do_stopped_rounds = get_termination_rounds(game_number, \
        doegta_env_short_name_payoffs_list, doegta_env_short_name_tsv_list)
    hado_stopped_rounds = get_termination_rounds(game_number, \
        hadoegta_env_short_name_payoffs_list, hadoegta_env_short_name_tsv_list)
    print(do_mean_att_gain)
    print(do_mean_def_gain)
    print(do_sem_att_gain)
    print(do_sem_def_gain)
    print(hado_mean_att_gain)
    print(hado_mean_def_gain)
    print(hado_sem_att_gain)
    print(hado_sem_def_gain)
    print(do_stopped_rounds)
    print(hado_stopped_rounds)
    # print(str(len(att_gains)) + "\t" + str(len(def_gains)))
    save_env_name = "r30_mean_stderr"
    if not is_r30:
        save_env_name = "s29_mean_stderr"
    plot_both_gains_with_stderr(do_mean_def_gain, do_mean_att_gain, do_sem_def_gain, \
        do_sem_att_gain, hado_mean_def_gain, hado_mean_att_gain, hado_sem_def_gain,  \
        hado_sem_att_gain, save_env_name, True, do_stopped_rounds, hado_stopped_rounds)

'''
example: python3 plot_mean_gains_stderror_both.py True
'''
if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ValueError("Need 1 args: is_r30")
    IS_R30 = get_truth_value(sys.argv[1])
    main(IS_R30)
