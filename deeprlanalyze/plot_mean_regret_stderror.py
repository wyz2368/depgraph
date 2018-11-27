import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_regret_anytime import get_all_eqs, get_all_def_eq_regrets, get_all_att_eq_regrets
from plot_mean_payoffs import get_means, get_standard_errors
from get_both_payoffs_from_game import get_json_data

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_env_short_name_tsv(env_short_name_payoffs):
    if env_short_name_payoffs in ["s29n1", "d30n1"]:
        return env_short_name_payoffs
    return env_short_name_payoffs + "_randNoAndB"

def plot_regrets_with_stderr(def_regrets, att_regrets, def_errs, att_errs, \
    env_short_name_payoffs, should_use_fill):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    def_errs = def_errs[:len(def_regrets)]
    att_errs = att_errs[:len(att_regrets)]
    if should_use_fill:
        fill_alpha = 0.25
        plt.plot(range(len(def_regrets)), def_regrets, lw=my_lw, label='Def. regret', \
            color='blue')
        def_mins = [def_regrets[i] - def_errs[i] for i in range(len(def_errs))]
        def_maxes = [def_regrets[i] + def_errs[i] for i in range(len(def_errs))]
        plt.fill_between(range(len(def_regrets)), def_mins, def_maxes,
                         alpha=fill_alpha, edgecolor='blue', facecolor='blue', linewidth=0)

        plt.plot(range(len(att_regrets)), att_regrets, lw=my_lw, label='Att. regret', \
            color='orange', linestyle='--')
        att_mins = [att_regrets[i] - att_errs[i] for i in range(len(att_errs))]
        att_maxes = [att_regrets[i] + att_errs[i] for i in range(len(att_errs))]
        plt.fill_between(range(len(att_regrets)), att_mins, att_maxes,
                         alpha=fill_alpha, edgecolor='orange', facecolor='orange', \
                         linewidth=0)
    else:
        plt.errorbar(range(len(def_regrets)), def_regrets, yerr=def_errs, lw=my_lw, \
            label='Def. gain', color='blue', elinewidth=1)
        plt.errorbar(range(len(att_regrets)), att_regrets, yerr=att_errs, lw=my_lw, \
            linestyle='--', label='Att. gain', color='orange', \
            elinewidth=1)

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
    def_highs = [x + y for x, y in zip(def_regrets, def_errs)]
    att_highs = [x + y for x, y in zip(att_regrets, att_errs)]
    y_max = max(max(def_highs), max(att_highs))
    ax.set_ylim(-1, y_max + 2)
    ax.set_xlim(-1, len(def_regrets))
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
    savefig(env_short_name_payoffs + "_mean_regret_vs_round.pdf")

def main(game_file_list, env_short_name_payoffs_list):
    if len(game_file_list) != len(env_short_name_payoffs_list):
        raise ValueError("Must have same number of games and envs: " + \
            str(game_file_list) + ", " + str(env_short_name_payoffs_list))
    print("Using games: " + str(game_file_list))
    print("Using environments: " + str(env_short_name_payoffs_list))
    all_def_eq_regrets = []
    all_att_eq_regrets = []
    for i in range(len(game_file_list)):
        game_file = "game_outputs2/" + game_file_list[i]
        game_data = get_json_data(game_file)
        env_short_name_payoffs = env_short_name_payoffs_list[i]
        env_short_name_tsv = get_env_short_name_tsv(env_short_name_payoffs)
        def_eqs = get_all_eqs(env_short_name_tsv, True)
        att_eqs = get_all_eqs(env_short_name_tsv, False)
        def_eq_regrets = get_all_def_eq_regrets(def_eqs, att_eqs[-1], game_data)
        att_eq_regrets = get_all_att_eq_regrets(att_eqs, def_eqs[-1], game_data)
        all_def_eq_regrets.append(def_eq_regrets)
        all_att_eq_regrets.append(att_eq_regrets)
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
        env_short_name_payoffs_list[0], True)

'''
example: python3 plot_mean_regret_stderror.py game_3014_23.json game_3014_22_d30f1.json \
    d30n1 d30f1
'''
if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise ValueError("Need 2+ args: game_file_list, env_short_name_payoffs+")
    GAME_FILE_LIST = []
    ARG_INDEX = 1
    while ".json" in sys.argv[ARG_INDEX]:
        GAME_FILE_LIST.append(sys.argv[ARG_INDEX])
        ARG_INDEX += 1
    ENV_SHORT_NAME_PAYOFFS_LIST = []
    for j in range(ARG_INDEX, len(sys.argv)):
        ENV_SHORT_NAME_PAYOFFS_LIST.append(sys.argv[j])
    main(GAME_FILE_LIST, ENV_SHORT_NAME_PAYOFFS_LIST)
