import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_mean_payoffs import get_all_mean_gains_with_standard_errors, \
    get_termination_rounds

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_env_short_name_tsv(env_short_name_payoffs):
    if env_short_name_payoffs in ["s29n1", "d30n1"]:
        return env_short_name_payoffs
    return env_short_name_payoffs + "_randNoAndB"

def is_converged(env_short_name_payoffs, stopped_round):
    print(env_short_name_payoffs)
    print(stopped_round)
    if env_short_name_payoffs in ["d30d1_mean_stderr", "r30_mean_stderr"]:
        return stopped_round in [20, 22, 23]
    if env_short_name_payoffs in ["s29n1_mean_stderr"]:
        return stopped_round in [32]
    if env_short_name_payoffs in ["s29_mean_stderr"]:
        return stopped_round in [32, 39]
    return False

def plot_gains_with_stderr(def_gains, att_gains, def_errs, att_errs, \
    env_short_name_payoffs, should_use_fill, stopped_rounds):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    def_errs = def_errs[:len(def_gains)]
    att_errs = att_errs[:len(att_gains)]
    if should_use_fill:
        fill_alpha = 0.25
        plt.plot(range(len(def_gains)), def_gains, lw=my_lw, label='Def. gain', \
            color='blue')
        def_mins = [def_gains[i] - def_errs[i] for i in range(len(def_errs))]
        def_maxes = [def_gains[i] + def_errs[i] for i in range(len(def_errs))]
        plt.fill_between(range(len(def_gains)), def_mins, def_maxes,
                         alpha=fill_alpha, edgecolor='blue', facecolor='blue', linewidth=0)

        plt.plot(range(len(att_gains)), att_gains, lw=my_lw, label='Att. gain', \
            color='orange', linestyle='--')
        att_mins = [att_gains[i] - att_errs[i] for i in range(len(att_errs))]
        att_maxes = [att_gains[i] + att_errs[i] for i in range(len(att_errs))]
        plt.fill_between(range(len(att_gains)), att_mins, att_maxes,
                         alpha=fill_alpha, edgecolor='orange', facecolor='orange', \
                         linewidth=0)
    else:
        plt.errorbar(range(len(def_gains)), def_gains, yerr=def_errs, lw=my_lw, \
            label='Def. gain', color='blue', elinewidth=1)
        plt.errorbar(range(len(att_gains)), att_gains, yerr=att_errs, lw=my_lw, \
            linestyle='--', label='Att. gain', color='orange', \
            elinewidth=1)

    for stopped_round in stopped_rounds:
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
    tick_spacing = 2
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.legend()
    def_highs = [x + y for x, y in zip(def_gains, def_errs)]
    att_highs = [x + y for x, y in zip(att_gains, att_errs)]
    y_max = max(max(def_highs), max(att_highs))
    ax.set_ylim(-10, y_max + 10)
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

def main(game_number, env_short_name_payoffs_list):
    print("Using environments: " + str(env_short_name_payoffs_list))
    env_short_name_tsv_list = [get_env_short_name_tsv(x) for x in \
        env_short_name_payoffs_list]
    mean_att_gain, mean_def_gain, sem_att_gain, sem_def_gain = \
        get_all_mean_gains_with_standard_errors(game_number, env_short_name_payoffs_list, \
        env_short_name_tsv_list)
    stopped_rounds = get_termination_rounds(game_number, env_short_name_payoffs_list, \
        env_short_name_tsv_list)
    print(mean_att_gain)
    print(mean_def_gain)
    print(sem_att_gain)
    print(sem_def_gain)
    print(stopped_rounds)
    # print(str(len(att_gains)) + "\t" + str(len(def_gains)))
    save_env_name = env_short_name_payoffs_list[0] + "_mean_stderr"
    plot_gains_with_stderr(mean_def_gain, mean_att_gain, sem_def_gain, sem_att_gain, \
        save_env_name, True, stopped_rounds)

'''
example: python3 plot_mean_gains_stderror.py 3014 d30d1 d30m1 d30n1 d30f1 d30f2
'''
if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise ValueError("Need 2+ args: game_number, env_short_name_payoffs+")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS_LIST = []
    for j in range(2, len(sys.argv)):
        ENV_SHORT_NAME_PAYOFFS_LIST.append(sys.argv[j])
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS_LIST)
