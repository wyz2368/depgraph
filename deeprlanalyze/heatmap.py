import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
from matplotlib import cm
import matplotlib.ticker as tick
import numpy as np
from pylab import savefig
from plot_strat_rounds import get_all_eq_from_files, get_network_round_list

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_round_distributions(round_dicts):
    result = []
    for round_dict in round_dicts:
        round_list = get_network_round_list(round_dict)
        result.append(round_list)
    return result

def plot_heatmap(round_fractions, is_defender, env_short_name_payoffs):
    max_round = len(round_fractions)
    self_choices = []
    for i in range(max_round):
        cur_choices = []
        cur_dict = round_fractions[i]
        for j in range(max_round):
            cur_str = str(j)
            if cur_str in cur_dict:
                cur_choices.append(1.0 * cur_dict[cur_str])
            else:
                cur_choices.append(0.0)
        self_choices.append(cur_choices)

    for_imshow = np.array(self_choices)

    mask = np.transpose(np.tri(for_imshow.shape[0], k=-1))

    for_imshow = np.ma.array(for_imshow, mask=mask)

    fig, ax = plt.subplots()
    fig.set_size_inches(5, 5)

    cmap = cm.get_cmap('jet') # jet doesn't have white color
    cmap.set_bad('w')

    im = ax.imshow(for_imshow, interpolation="nearest", cmap=cmap)

    tick_spacing = 3
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.yaxis.set_major_locator(tick.MultipleLocator(tick_spacing))

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Training round', fontsize=16)
    plt.xlabel('Network weight', fontsize=16)

    # my_title = "Random 30-node graph"
    # if "s29" in env_short_name_payoffs:
    #     my_title = "Separate layers 29-node graph"
    # plt.title(my_title, fontsize=20)

    cbar = fig.colorbar(im, fraction=0.046, pad=0.04)
    cbar.ax.tick_params(labelsize=12)

    ax.tick_params(labelsize=14)
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

    # plt.show()
    save_title = "heatmap_"
    if is_defender:
        save_title += "def_"
    else:
        save_title += "att_"
    save_title += env_short_name_payoffs + ".pdf"
    savefig(save_title)

def main(env_short_name_payoffs, env_short_name_tsv):
    def_eqs, att_eqs = get_all_eq_from_files(env_short_name_tsv)
    def_round_distributions = get_round_distributions(def_eqs)
    att_round_distributions = get_round_distributions(att_eqs)
    print(def_round_distributions)
    print(att_round_distributions)
    plot_heatmap(def_round_distributions, True, env_short_name_payoffs)
    plot_heatmap(att_round_distributions, False, env_short_name_payoffs)

'''
example: python3 heatmap.py s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: env_short_name_payoffs, " + \
            "env_short_name_tsv")
    ENV_SHORT_NAME_PAYOFFS = sys.argv[1]
    ENV_SHORT_NAME_TSV = sys.argv[2]
    main(ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
