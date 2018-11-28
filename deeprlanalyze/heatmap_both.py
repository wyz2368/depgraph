import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
from matplotlib import cm
import matplotlib.ticker as tick
import numpy as np
from pylab import savefig
from append_net_names import get_truth_value
from plot_strat_rounds import get_all_eq_from_files, get_network_round_list

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_round_distributions(round_dicts):
    result = []
    for round_dict in round_dicts:
        round_list = get_network_round_list(round_dict)
        result.append(round_list)
    return result

def plot_heatmap_both(do_round_fractions, hado_round_fractions, is_defender, is_r30):
    fig, ax = plt.subplots()
    fig.set_size_inches(10, 5)

    ax = plt.subplot(1, 2, 1)
    do_max_round = len(do_round_fractions)
    self_choices = []
    for i in range(do_max_round):
        cur_choices = []
        cur_dict = do_round_fractions[i]
        for j in range(do_max_round):
            cur_str = str(j)
            if cur_str in cur_dict:
                cur_choices.append(1.0 * cur_dict[cur_str])
            else:
                cur_choices.append(0.0)
        self_choices.append(cur_choices)
    for_imshow = np.array(self_choices)

    mask = np.transpose(np.tri(for_imshow.shape[0], k=-1))

    for_imshow = np.ma.array(for_imshow, mask=mask)
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
    plt.ylabel('Equilibrium round', fontsize=16)
    plt.xlabel('DQN index', fontsize=16)
    plt.title("DO-EGTA", fontsize=20)

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

    ax = plt.subplot(1, 2, 2)
    hado_max_round = len(hado_round_fractions)
    self_choices = []
    for i in range(hado_max_round):
        cur_choices = []
        cur_dict = hado_round_fractions[i]
        for j in range(hado_max_round):
            cur_str = str(j)
            if cur_str in cur_dict:
                cur_choices.append(1.0 * cur_dict[cur_str])
            else:
                cur_choices.append(0.0)
        self_choices.append(cur_choices)
    for_imshow = np.array(self_choices)

    mask = np.transpose(np.tri(for_imshow.shape[0], k=-1))

    for_imshow = np.ma.array(for_imshow, mask=mask)
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
    plt.ylabel('Equilibrium round', fontsize=16)
    plt.xlabel('DQN index', fontsize=16)
    plt.title("HADO-EGTA", fontsize=20)

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
    save_title = "heatmap_both_"
    if is_defender:
        save_title += "def_"
    else:
        save_title += "att_"
    if is_r30:
        save_title += "r30"
    else:
        save_title += "s29"
    save_title += ".pdf"
    savefig(save_title)

def main(is_r30):
    do_env_short_name_tsv = "d30n1"
    hado_env_short_name_tsv = "d30cd1_randNoAndB"
    if not is_r30:
        do_env_short_name_tsv = "s29n1"
        hado_env_short_name_tsv = "s29cs1_randNoAndB"
    do_def_eqs, do_att_eqs = get_all_eq_from_files(do_env_short_name_tsv)
    hado_def_eqs, hado_att_eqs = get_all_eq_from_files(hado_env_short_name_tsv)
    do_def_round_distributions = get_round_distributions(do_def_eqs)
    do_att_round_distributions = get_round_distributions(do_att_eqs)
    hado_def_round_distributions = get_round_distributions(hado_def_eqs)
    hado_att_round_distributions = get_round_distributions(hado_att_eqs)
    print(do_def_round_distributions)
    print(do_att_round_distributions)
    print(hado_def_round_distributions)
    print(hado_att_round_distributions)
    plot_heatmap_both(do_def_round_distributions, hado_def_round_distributions, True, \
        is_r30)
    plot_heatmap_both(do_att_round_distributions, hado_att_round_distributions, False, \
        is_r30)

'''
example: python3 heatmap_both.py True
'''
if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ValueError("Need 1 args: is_r30")
    IS_R30 = get_truth_value(sys.argv[1])
    main(IS_R30)
