import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_regret_anytime import get_all_eqs, get_all_def_eq_regrets, get_all_att_eq_regrets
from plot_mean_payoffs import get_means, get_standard_errors
from get_both_payoffs_from_game import get_json_data
from append_net_names import get_truth_value

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_env_short_name_tsv(env_short_name_payoffs):
    if env_short_name_payoffs in ["s29n1", "d30n1"]:
        return env_short_name_payoffs
    return env_short_name_payoffs + "_randNoAndB"

def plot_both_regrets_with_stderr(do_def_regrets, do_att_regrets, do_def_errs, \
    do_att_errs, hado_def_regrets, hado_att_regrets, hado_def_errs, hado_att_errs, \
    is_r30, should_use_fill):
    fig, ax = plt.subplots()
    fig.set_size_inches(10, 5)

    my_lw = 2

    do_def_errs = do_def_errs[:len(do_def_regrets)]
    do_att_errs = do_att_errs[:len(do_att_regrets)]

    hado_def_errs = hado_def_errs[:len(hado_def_regrets)]
    hado_att_errs = hado_att_errs[:len(hado_att_regrets)]

    do_def_highs = [x + y for x, y in zip(do_def_regrets, do_def_errs)]
    do_att_highs = [x + y for x, y in zip(do_att_regrets, do_att_errs)]
    hado_def_highs = [x + y for x, y in zip(hado_def_regrets, hado_def_errs)]
    hado_att_highs = [x + y for x, y in zip(hado_att_regrets, hado_att_errs)]
    y_max = max(max(do_def_highs), max(do_att_highs), max(hado_def_highs), \
        max(hado_att_highs))
    x_max = max(len(do_def_regrets), len(do_att_regrets), len(hado_def_regrets), \
        len(hado_att_regrets))

    ax = plt.subplot(1, 2, 1)
    if should_use_fill:
        fill_alpha = 0.25
        plt.plot(range(len(do_def_regrets)), do_def_regrets, lw=my_lw, \
            label='Def. regret', color='blue')
        def_mins = [do_def_regrets[i] - do_def_errs[i] for i in range(len(do_def_errs))]
        def_maxes = [do_def_regrets[i] + do_def_errs[i] for i in range(len(do_def_errs))]
        plt.fill_between(range(len(do_def_regrets)), def_mins, def_maxes,
                         alpha=fill_alpha, edgecolor='blue', facecolor='blue', linewidth=0)

        plt.plot(range(len(do_att_regrets)), do_att_regrets, lw=my_lw, \
            label='Att. regret', color='orange', linestyle='--')
        att_mins = [do_att_regrets[i] - do_att_errs[i] for i in range(len(do_att_errs))]
        att_maxes = [do_att_regrets[i] + do_att_errs[i] for i in range(len(do_att_errs))]
        plt.fill_between(range(len(do_att_regrets)), att_mins, att_maxes,
                         alpha=fill_alpha, edgecolor='orange', facecolor='orange', \
                         linewidth=0)
    else:
        plt.errorbar(range(len(do_def_regrets)), do_def_regrets, yerr=do_def_errs, \
            lw=my_lw, label='Def. gain', color='blue', elinewidth=1)
        plt.errorbar(range(len(do_att_regrets)), do_att_regrets, yerr=do_att_errs, \
            lw=my_lw, linestyle='--', label='Att. gain', color='orange', elinewidth=1)

    plt.axhline(0, color='black', lw=1)
    plt.axvline(0, color='black', lw=1)

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean regret', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 5
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    plt.legend()
    ax.set_ylim(-1, y_max + 2)
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
        plt.plot(range(len(hado_def_regrets)), hado_def_regrets, lw=my_lw, \
            label='Def. regret', color='blue')
        def_mins = [hado_def_regrets[i] - hado_def_errs[i] for i in \
            range(len(hado_def_errs))]
        def_maxes = [hado_def_regrets[i] + hado_def_errs[i] for i in \
            range(len(hado_def_errs))]
        plt.fill_between(range(len(hado_def_regrets)), def_mins, def_maxes,
                         alpha=fill_alpha, edgecolor='blue', facecolor='blue', linewidth=0)

        plt.plot(range(len(hado_att_regrets)), hado_att_regrets, lw=my_lw, \
            label='Att. regret', color='orange', linestyle='--')
        att_mins = [hado_att_regrets[i] - hado_att_errs[i] for i in \
            range(len(hado_att_errs))]
        att_maxes = [hado_att_regrets[i] + hado_att_errs[i] for i in \
            range(len(hado_att_errs))]
        plt.fill_between(range(len(hado_att_regrets)), att_mins, att_maxes,
                         alpha=fill_alpha, edgecolor='orange', facecolor='orange', \
                         linewidth=0)
    else:
        plt.errorbar(range(len(hado_def_regrets)), hado_def_regrets, yerr=hado_def_errs, \
            lw=my_lw, label='Def. gain', color='blue', elinewidth=1)
        plt.errorbar(range(len(hado_att_regrets)), hado_att_regrets, yerr=hado_att_errs, \
            lw=my_lw, linestyle='--', label='Att. gain', color='orange', elinewidth=1)

    plt.axhline(0, color='black', lw=1)
    plt.axvline(0, color='black', lw=1)

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean regret', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 5
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    plt.legend()
    ax.set_ylim(-1, y_max + 2)
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
    save_name = "r30_mean_regret_vs_round_stderr.pdf"
    if not is_r30:
        save_name = "s29_mean_regret_vs_round_stderr.pdf"
    savefig(save_name)

def main(is_r30):
    print("Is r30: " + str(is_r30))
    if is_r30:
        do_game_file_list = ["game_3014_23.json", "game_3014_22_d30f1.json"]
        hado_game_file_list = ["game_3014_20_d30cd1.json"]
        do_env_short_name_payoffs_list = ["d30n1", "d30f1"]
        hado_env_short_name_payoffs_list = ["d30cd1"]
    else:
        do_game_file_list = ["game_3013_32.json"]
        hado_game_file_list = ["game_3013_39_s29cs1.json"]
        do_env_short_name_payoffs_list = ["s29n1"]
        hado_env_short_name_payoffs_list = ["s29cs1"]
    do_def_eq_regrets = []
    do_att_eq_regrets = []
    for i in range(len(do_game_file_list)):
        game_file = "game_outputs2/" + do_game_file_list[i]
        game_data = get_json_data(game_file)
        env_short_name_payoffs = do_env_short_name_payoffs_list[i]
        env_short_name_tsv = get_env_short_name_tsv(env_short_name_payoffs)
        def_eqs = get_all_eqs(env_short_name_tsv, True)
        att_eqs = get_all_eqs(env_short_name_tsv, False)
        def_eq_regrets = get_all_def_eq_regrets(def_eqs, att_eqs[-1], game_data)
        att_eq_regrets = get_all_att_eq_regrets(att_eqs, def_eqs[-1], game_data)
        do_def_eq_regrets.append(def_eq_regrets)
        do_att_eq_regrets.append(att_eq_regrets)
    do_def_eq_regret_means = get_means(do_def_eq_regrets)
    do_att_eq_regret_means = get_means(do_att_eq_regrets)
    do_def_eq_regret_stderrs = get_standard_errors(do_def_eq_regrets)
    do_att_eq_regret_stderrs = get_standard_errors(do_att_eq_regrets)
    print(do_def_eq_regret_means)
    print(do_att_eq_regret_means)
    print(do_def_eq_regret_stderrs)
    print(do_att_eq_regret_stderrs)

    hado_def_eq_regrets = []
    hado_att_eq_regrets = []
    for i in range(len(hado_game_file_list)):
        game_file = "game_outputs2/" + hado_game_file_list[i]
        game_data = get_json_data(game_file)
        env_short_name_payoffs = hado_env_short_name_payoffs_list[i]
        env_short_name_tsv = get_env_short_name_tsv(env_short_name_payoffs)
        def_eqs = get_all_eqs(env_short_name_tsv, True)
        att_eqs = get_all_eqs(env_short_name_tsv, False)
        def_eq_regrets = get_all_def_eq_regrets(def_eqs, att_eqs[-1], game_data)
        att_eq_regrets = get_all_att_eq_regrets(att_eqs, def_eqs[-1], game_data)
        hado_def_eq_regrets.append(def_eq_regrets)
        hado_att_eq_regrets.append(att_eq_regrets)
    hado_def_eq_regret_means = get_means(hado_def_eq_regrets)
    hado_att_eq_regret_means = get_means(hado_att_eq_regrets)
    hado_def_eq_regret_stderrs = get_standard_errors(hado_def_eq_regrets)
    hado_att_eq_regret_stderrs = get_standard_errors(hado_att_eq_regrets)
    print(hado_def_eq_regret_means)
    print(hado_att_eq_regret_means)
    print(hado_def_eq_regret_stderrs)
    print(hado_att_eq_regret_stderrs)

    plot_both_regrets_with_stderr(do_def_eq_regret_means, do_att_eq_regret_means, \
        do_def_eq_regret_stderrs, do_att_eq_regret_stderrs, \
        hado_def_eq_regret_means, hado_att_eq_regret_means, hado_def_eq_regret_stderrs, \
        hado_att_eq_regret_stderrs, is_r30, True)

'''
example: python3 plot_mean_regret_stderror_both.py True
'''
if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ValueError("Need 1 args: is_r30")
    IS_R30 = get_truth_value(sys.argv[1])
    main(IS_R30)
