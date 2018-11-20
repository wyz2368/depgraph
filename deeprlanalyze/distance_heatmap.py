import sys
from copy import deepcopy
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
from matplotlib import cm
import matplotlib.ticker as tick
import numpy as np
from pylab import savefig
from plot_strat_rounds import get_all_eq_from_files

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_l1_distance(mixed_a, mixed_b):
    result = 0.0
    for pure_a, pure_a_weight in mixed_a.items():
        if pure_a not in mixed_b:
            result += pure_a_weight
        else:
            pure_b_weight = mixed_b[pure_a]
            result += abs(pure_a_weight - pure_b_weight)
    for pure_b, pure_b_weight in mixed_b.items():
        if pure_b not in mixed_a:
            result += pure_b_weight
    return result

def get_l1_distances(eq_list):
    result = []
    for i in range(len(eq_list)):
        mixed_a = eq_list[i]
        a_row = []
        for j in range(i):
            mixed_b = eq_list[j]
            a_row.append(get_l1_distance(mixed_a, mixed_b))
        result.append(a_row)
    return result

def similarities_from_distances(distance_matrix):
    result = deepcopy(distance_matrix)
    for i in range(len(result)):
        for j in range(len(result[i])):
            result[i][j] = (2.0 - result[i][j]) / 2.0
    return result

def plot_similarity_heatmap(similarities, is_defender, env_short_name_payoffs):
    sim = []
    for i in range(len(similarities)):
        cur = []
        for j in range(len(similarities)):
            if j < len(similarities[i]):
                cur.append(similarities[i][j])
            else:
                cur.append(-1)
        sim.append(cur)

    for_imshow = np.array(sim)
    mask = np.transpose(np.tri(for_imshow.shape[0], k=0))
    for_imshow = np.ma.array(for_imshow, mask=mask)

    fig, ax = plt.subplots()
    fig.set_size_inches(5, 5)

    cmap = cm.get_cmap('jet') # jet doesn't have white color
    cmap.set_bad('w')

    im = ax.imshow(for_imshow, interpolation="nearest", cmap=cmap)

    tick_spacing = 2
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.yaxis.set_major_locator(tick.MultipleLocator(tick_spacing))

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Training round', fontsize=16)
    plt.xlabel('Training round', fontsize=16)

    my_title = "Random 30-node graph"
    if "s29" in env_short_name_payoffs:
        my_title = "Separate layers 29-node graph"
    plt.title(my_title, fontsize=20)

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

    save_title = "sim_" + env_short_name_payoffs
    if is_defender:
        save_title += "_def.pdf"
    else:
        save_title += "_att.pdf"
    savefig(save_title)

def main(env_short_name_payoffs, env_short_name_tsv):
    def_eqs, att_eqs = get_all_eq_from_files(env_short_name_tsv)

    def_distances = get_l1_distances(def_eqs)
    def_similarities = similarities_from_distances(def_distances)
    att_distances = get_l1_distances(att_eqs)
    att_similarities = similarities_from_distances(att_distances)

    plot_similarity_heatmap(def_similarities, True, env_short_name_payoffs)
    plot_similarity_heatmap(att_similarities, False, env_short_name_payoffs)

'''
example: python3 distance_heatmap.py s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: env_short_name_payoffs, " + \
            "env_short_name_tsv")
    ENV_SHORT_NAME_PAYOFFS = sys.argv[1]
    ENV_SHORT_NAME_TSV = sys.argv[2]
    main(ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
