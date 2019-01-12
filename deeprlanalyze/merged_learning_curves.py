import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_payoffs_auto import get_round_count, get_all_payoffs
from plot_learning_curves import get_learning_curves

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def plot_curves_merged(learning_curves, eq_payoffs, is_defender, \
    env_short_name_payoffs):
    fig, (ax, ax_right) = plt.subplots(1, 2, gridspec_kw={'width_ratios':[20, 1]})
    fig.set_size_inches(6, 4)

    my_cmap = 'gnuplot'
    cmap = plt.cm.get_cmap(my_cmap)
    colors = []
    for i in range(len(learning_curves)):
        frac = i * 1.0 / (len(learning_curves) - 1)
        rgba = cmap(frac)
        colors.append(rgba)

    norm = matplotlib.colors.Normalize(vmin=0, vmax=1)
    cbar = matplotlib.colorbar.ColorbarBase(ax_right, cmap=cmap,
                                            norm=norm,
                                            orientation='vertical',
                                            ticks=[0, 0.5, 1])
    cbar.set_label('Round', fontsize=16)
    cbar.ax.set_yticklabels(["0", "10", "20"], fontsize=14)

    for i in range(len(learning_curves)):
        episodes = learning_curves[i][0]
        rewards = learning_curves[i][1]
        ax.plot(episodes, rewards, c=colors[i])

    my_lw = 2
    ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

    reward_lists = [x[1] for x in learning_curves]
    y_max = max([max(y) for y in reward_lists])
    y_max = max(y_max, max(eq_payoffs))

    y_min = min([min(y) for y in reward_lists])
    y_min = min(y_min, min(eq_payoffs))

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
    # ax.grid(linewidth=1, linestyle=':')
    ax.spines['left'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.set_ylim((y_min - 5, y_max + 5))
    ax.tick_params(labelsize=14)
    ax.set_xlabel("Training episode", fontsize=16)
    ax.set_ylabel("Payoff gain", fontsize=16)

    tick_spacing = 10000
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))

    # my_title = "Random 30-node graph"
    # if "s29" in env_short_name_payoffs:
    #     my_title = "Separate layers 29-node graph"
    # plt.title(my_title, fontsize=20)
    plt.tight_layout()

    save_name = "learning_curves_merged_"
    if is_defender:
        save_name += "def_"
    else:
        save_name += "att_"
    save_name += env_short_name_payoffs + ".pdf"
    savefig(save_name)

def get_learning_curves_normalized(learning_curves, eq_payoffs):
    result = []
    for i in range(len(learning_curves)):
        learning_curve = learning_curves[i]
        eq_payoff = eq_payoffs[i]
        episodes_copy = learning_curve[0].copy()
        rewards = learning_curve[1]
        normalized_rewards = [x - eq_payoff for x in rewards]
        result.append([episodes_copy, normalized_rewards])
    return result

def main(game_number, env_short_name_payoffs, env_short_name_tsv):
    # each learning curve has [episodes_list, rewards_list]
    def_learning_curves = get_learning_curves(env_short_name_payoffs, True)
    att_learning_curves = get_learning_curves(env_short_name_payoffs, False)

    round_count = get_round_count(env_short_name_tsv)
    att_eq_payoffs, def_eq_payoffs, _, _ = \
        get_all_payoffs(round_count, env_short_name_tsv, env_short_name_payoffs, \
            game_number)

    normalized_def_learning_curves = get_learning_curves_normalized(def_learning_curves, \
        def_eq_payoffs)
    normalized_att_learning_curves = get_learning_curves_normalized(att_learning_curves, \
        att_eq_payoffs)

    plot_curves_merged(normalized_def_learning_curves, def_eq_payoffs, True, \
        env_short_name_payoffs)
    plot_curves_merged(normalized_att_learning_curves, att_eq_payoffs, False, \
        env_short_name_payoffs)

'''
example: python3 merged_learning_curves.py 3013 s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_number, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
