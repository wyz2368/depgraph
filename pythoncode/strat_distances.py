from copy import deepcopy
import matplotlib.pyplot as plt
from matplotlib import cm
import matplotlib.ticker as ticker
import numpy as np
from pylab import savefig
from sklearn.manifold import TSNE

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_lines(file_name):
    lines = None
    with open(file_name) as file:
        lines = file.readlines()
    lines = [line.strip() for line in lines]
    lines = [line for line in lines if line]
    return lines

def get_eq_from_file(file_name):
    lines = get_lines("eqs/" + file_name)
    result = {}
    for line in lines:
        line = line.strip()
        while "  " in line:
            line = line.replace("  ", " ")
        items = None
        if "\t" in line:
            items = line.split('\t')
        else:
            items = line.split(" ")
        strat = items[0].strip()
        weight = float(items[1].strip())
        result[strat] = weight
    return result

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

def get_square_l1_distances(eq_list):
    result = []
    for i in range(len(eq_list)):
        mixed_a = eq_list[i]
        a_row = []
        for j in range(len(eq_list)):
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

def get_eqs_from_files(file_list):
    return [get_eq_from_file(file) for file in file_list]

# suggested perplexities to try: {3, 5, 7, 8}
def plot_distances_tsne(square_distances, perplexity, figure_title, save_title):
    Y = TSNE(n_components=2, perplexity=perplexity).fit_transform(square_distances)

    fig, ax = plt.subplots()
    fig.set_size_inches(5, 5)

    ax.scatter(Y[:, 0], Y[:, 1])
    for i in range(len(square_distances)):
        ax.annotate(i, (Y[i, 0], Y[i, 1]), fontsize=14)

    #ax.spines['right'].set_visible(False)
    #ax.spines['top'].set_visible(False)
    #ax.spines['left'].set_visible(False)
    #ax.spines['bottom'].set_visible(False)
    plt.tick_params(
        axis='both',          # changes apply to the x-axis
        which='both',      # both major and minor ticks are affected
        bottom=False,      # ticks along the bottom edge are off
        top=False,         # ticks along the top edge are off
        left=False,
        labelbottom=False,
        labelleft=False)
    plt.title(figure_title, fontsize=20)

    plt.tight_layout()

    # plt.show()
    savefig(save_title)

def plot_similarity_heatmap(similarities, figure_title, save_title):
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
    ax.xaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))
    ax.yaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))
    # plt.yticks(range(1, len(similarities) + 1, 2))

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Training round', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    plt.title(figure_title, fontsize=20)

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
    savefig(save_title)

d30_def_eqs = [
    "randNoAnd_B_epoch1_def.tsv",
    "randNoAnd_B_epoch2_def.tsv",
    "randNoAnd_B_epoch3_def.tsv",
    "randNoAnd_B_epoch4_def.tsv",
    "randNoAnd_B_epoch5_def.tsv",
    "randNoAnd_B_epoch6e_def.tsv",
    "randNoAnd_B_epoch7e_def.tsv",
    "randNoAnd_B_epoch8_def.tsv",
    "randNoAnd_B_epoch9_def.tsv",
    "randNoAnd_B_epoch10_def.tsv",
    "randNoAnd_B_epoch11_def.tsv",
    "randNoAnd_B_epoch12_def.tsv",
    "d30_epoch13_def.tsv",
    "d30_epoch14_def.tsv",
    "d30_epoch15_def.tsv",
    "d30_epoch16_def.tsv",
    "d30_epoch17_def.tsv",
    "d30_epoch18_def.tsv",
    "d30_epoch19_def.tsv",
    "d30_epoch20_def.tsv",
    "d30_epoch21_def.tsv",
    "d30_epoch22_def.tsv",
    "d30_epoch23_def.tsv",
    "d30_epoch24_def.tsv"
]

d30_att_eqs = [
    "randNoAnd_B_epoch1_att.tsv",
    "randNoAnd_B_epoch2_att.tsv",
    "randNoAnd_B_epoch3_att.tsv",
    "randNoAnd_B_epoch4_att.tsv",
    "randNoAnd_B_epoch5_att.tsv",
    "randNoAnd_B_epoch6e_att.tsv",
    "randNoAnd_B_epoch7e_att.tsv",
    "randNoAnd_B_epoch8_att.tsv",
    "randNoAnd_B_epoch9_att.tsv",
    "randNoAnd_B_epoch10_att.tsv",
    "randNoAnd_B_epoch11_att.tsv",
    "randNoAnd_B_epoch12_att.tsv",
    "d30_epoch13_att.tsv",
    "d30_epoch14_att.tsv",
    "d30_epoch15_att.tsv",
    "d30_epoch16_att.tsv",
    "d30_epoch17_att.tsv",
    "d30_epoch18_att.tsv",
    "d30_epoch19_att.tsv",
    "d30_epoch20_att.tsv",
    "d30_epoch21_att.tsv",
    "d30_epoch22_att.tsv",
    "d30_epoch23_att.tsv",
    "d30_epoch24_att.tsv"
]

s29_def_eqs = [
    "sl29_randNoAndB_epoch0_def.tsv",
    "sl29_randNoAndB_epoch1_def.tsv",
    "sl29_randNoAndB_epoch2_def.tsv",
    "sl29_randNoAndB_epoch3_def.tsv",
    "sl29_randNoAndB_epoch4_def.tsv",
    "sl29_randNoAndB_epoch5_def.tsv",
    "sl29_randNoAndB_epoch6_def.tsv",
    "sl29_randNoAndB_epoch7_def.tsv",
    "sl29_randNoAndB_epoch8_def.tsv",
    "sl29_randNoAndB_epoch9_def.tsv",
    "sl29_randNoAndB_epoch10_def.tsv",
    "sl29_randNoAndB_epoch11_def.tsv",
    "sl29_randNoAndB_epoch12_def.tsv",
    "sl29_randNoAndB_epoch13_def.tsv",
    "sl29_randNoAndB_epoch14_def.tsv",
    "sl29_randNoAndB_epoch16_def.tsv",
    "sl29_randNoAndB_epoch17_def.tsv",
    "sl29_randNoAndB_epoch18_def.tsv",
    "sl29_randNoAndB_epoch19_def.tsv",
    "sl29_randNoAndB_epoch20_def.tsv",
    "sl29_randNoAndB_epoch21_def.tsv",
    "sl29_randNoAndB_epoch22_def.tsv",
    "sl29_randNoAndB_epoch23_def.tsv",
    "sl29_randNoAndB_epoch24_def.tsv",
    "sl29_randNoAndB_epoch25_def.tsv",
    "sl29_randNoAndB_epoch26_def.tsv",
    "sl29_randNoAndB_epoch27_def.tsv",
    "sl29_randNoAndB_epoch28_def.tsv",
    "sl29_randNoAndB_epoch29_def.tsv",
    "sl29_randNoAndB_epoch30_def.tsv",
    "sl29_randNoAndB_epoch31_def.tsv",
    "sl29_randNoAndB_epoch32_def.tsv",
    "sl29_randNoAndB_epoch33_def.tsv"
]

s29_att_eqs = [
    "sl29_randNoAndB_epoch0_att.tsv",
    "sl29_randNoAndB_epoch1_att.tsv",
    "sl29_randNoAndB_epoch2_att.tsv",
    "sl29_randNoAndB_epoch3_att.tsv",
    "sl29_randNoAndB_epoch4_att.tsv",
    "sl29_randNoAndB_epoch5_att.tsv",
    "sl29_randNoAndB_epoch6_att.tsv",
    "sl29_randNoAndB_epoch7_att.tsv",
    "sl29_randNoAndB_epoch8_att.tsv",
    "sl29_randNoAndB_epoch9_att.tsv",
    "sl29_randNoAndB_epoch10_att.tsv",
    "sl29_randNoAndB_epoch11_att.tsv",
    "sl29_randNoAndB_epoch12_att.tsv",
    "sl29_randNoAndB_epoch13_att.tsv",
    "sl29_randNoAndB_epoch14_att.tsv",
    "sl29_randNoAndB_epoch16_att.tsv",
    "sl29_randNoAndB_epoch17_att.tsv",
    "sl29_randNoAndB_epoch18_att.tsv",
    "sl29_randNoAndB_epoch19_att.tsv",
    "sl29_randNoAndB_epoch20_att.tsv",
    "sl29_randNoAndB_epoch21_att.tsv",
    "sl29_randNoAndB_epoch22_att.tsv",
    "sl29_randNoAndB_epoch23_att.tsv",
    "sl29_randNoAndB_epoch24_att.tsv",
    "sl29_randNoAndB_epoch25_att.tsv",
    "sl29_randNoAndB_epoch26_att.tsv",
    "sl29_randNoAndB_epoch27_att.tsv",
    "sl29_randNoAndB_epoch28_att.tsv",
    "sl29_randNoAndB_epoch29_att.tsv",
    "sl29_randNoAndB_epoch30_att.tsv",
    "sl29_randNoAndB_epoch31_att.tsv",
    "sl29_randNoAndB_epoch32_att.tsv",
    "sl29_randNoAndB_epoch33_att.tsv"
]

d30_def_eqs = get_eqs_from_files(d30_def_eqs)
d30_def_distances = get_l1_distances(d30_def_eqs)
d30_def_similarities = similarities_from_distances(d30_def_distances)
# print(d30_def_similarities)

plot_similarity_heatmap(d30_def_similarities, "$r_{30}$ def. eq. similarities", \
    "d30_def_similarities.pdf")

d30_att_eqs = get_eqs_from_files(d30_att_eqs)
d30_att_distances = get_l1_distances(d30_att_eqs)
d30_att_similarities = similarities_from_distances(d30_att_distances)
plot_similarity_heatmap(d30_att_similarities, "$r_{30}$ att. eq. similarities", \
    "d30_att_similarities.pdf")

s29_def_eqs = get_eqs_from_files(s29_def_eqs)
s29_def_distances = get_l1_distances(s29_def_eqs)
s29_def_similarities = similarities_from_distances(s29_def_distances)
plot_similarity_heatmap(s29_def_similarities, "$s_{29}$ def. eq. similarities", \
    "s29_def_similarities.pdf")

s29_att_eqs = get_eqs_from_files(s29_att_eqs)
s29_att_distances = get_l1_distances(s29_att_eqs)
s29_att_similarities = similarities_from_distances(s29_att_distances)
plot_similarity_heatmap(s29_att_similarities, "$s_{29}$ att. eq. similarities", \
    "s29_att_similarities.pdf")

d30_def_sq_distances = np.array(get_square_l1_distances(d30_def_eqs))
my_perplexity = 5
plot_distances_tsne(d30_def_sq_distances, my_perplexity, \
    "$r_{30}$ def. eq. t-SNE", "d30_def_eq_tsne.pdf")

d30_att_sq_distances = np.array(get_square_l1_distances(d30_att_eqs))
plot_distances_tsne(d30_att_sq_distances, my_perplexity, \
    "$r_{30}$ att. eq. t-SNE", "d30_att_eq_tsne.pdf")

s29_def_sq_distances = np.array(get_square_l1_distances(s29_def_eqs))
plot_distances_tsne(s29_def_sq_distances, my_perplexity, \
    "$s_{29}$ def. eq. t-SNE", "s29_def_eq_tsne.pdf")

s29_att_sq_distances = np.array(get_square_l1_distances(s29_att_eqs))
plot_distances_tsne(s29_att_sq_distances, my_perplexity, \
    "$s_{29}$ att. eq. t-SNE", "s29_att_eq_tsne.pdf")
