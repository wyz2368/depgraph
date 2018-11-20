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

def plot_curves_merged_both(def_learning_curves, def_eq_payoffs, att_learning_curves, \
    att_eq_payoffs, env_short_name_payoffs):
    fig, ax = plt.subplots()
    fig.set_size_inches(10, 5)

    my_cmap = 'gnuplot'
    cmap = plt.cm.get_cmap(my_cmap)
    colors = []
    ax = plt.subplot(1, 2, 1)
    for i in range(len(def_learning_curves)):
        frac = i * 1.0 / (len(def_learning_curves) - 1)
        rgba = cmap(frac)
        colors.append(rgba)

    for i in range(len(def_learning_curves)):
        episodes = def_learning_curves[i][0]
        rewards = def_learning_curves[i][1]
        ax.plot(episodes, rewards, c=colors[i])

    my_lw = 2
    ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

    reward_lists = [x[1] for x in def_learning_curves]
    y_max = max([max(y) for y in reward_lists])
    y_max = max(y_max, max(def_eq_payoffs))

    y_min = min([min(y) for y in reward_lists])
    y_min = min(y_min, min(def_eq_payoffs))

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
    x_tick_spacing = 10000
    ax.xaxis.set_major_locator(tick.MultipleLocator(x_tick_spacing))
    y_tick_spacing = 100
    if "d30" in env_short_name_payoffs:
        y_tick_spacing = 50
    ax.yaxis.set_major_locator(tick.MultipleLocator(y_tick_spacing))
    ax.set_ylim((y_min - 5, y_max + 5))
    ax.tick_params(labelsize=14)
    plt.xlabel("Training episode", fontsize=16)
    plt.ylabel("Payoff gain", fontsize=16)
    plt.title("Defender", fontsize=20)
    # my_title = "Random 30-node graph"
    # if "s29" in env_short_name_payoffs:
    #     my_title = "Separate layers 29-node graph"
    # plt.title(my_title, fontsize=20)
    plt.tight_layout()

    ax = plt.subplot(1, 2, 2)
    for i in range(len(att_learning_curves)):
        frac = i * 1.0 / (len(att_learning_curves) - 1)
        rgba = cmap(frac)
        colors.append(rgba)

    for i in range(len(att_learning_curves)):
        episodes = att_learning_curves[i][0]
        rewards = att_learning_curves[i][1]
        ax.plot(episodes, rewards, c=colors[i])

    my_lw = 2
    ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

    reward_lists = [x[1] for x in att_learning_curves]
    y_max = max([max(y) for y in reward_lists])
    y_max = max(y_max, max(att_eq_payoffs))

    y_min = min([min(y) for y in reward_lists])
    y_min = min(y_min, min(att_eq_payoffs))

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
    ax.xaxis.set_major_locator(tick.MultipleLocator(x_tick_spacing))
    ax.yaxis.set_major_locator(tick.MultipleLocator(y_tick_spacing))
    ax.tick_params(labelsize=14)
    plt.xlabel("Training episode", fontsize=16)
    plt.ylabel("Payoff gain", fontsize=16)
    plt.title("Attacker", fontsize=20)
    # my_title = "Random 30-node graph"
    # if "s29" in env_short_name_payoffs:
    #     my_title = "Separate layers 29-node graph"
    # plt.title(my_title, fontsize=20)
    plt.tight_layout()


    save_name = "learning_curves_merged_both_"
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

    plot_curves_merged_both(normalized_def_learning_curves, def_eq_payoffs, \
        normalized_att_learning_curves, att_eq_payoffs, env_short_name_payoffs)

'''
example: python3 merged_learning_curves_both.py 3013 s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_number, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
