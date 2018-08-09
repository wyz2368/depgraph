import json
import matplotlib.pyplot as plt
from matplotlib import cm
import matplotlib.ticker as ticker
import numpy as np
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_squared_error
from pylab import savefig

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_json(file_name):
    with open(file_name) as file:
        return json.load(file)

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

def get_def_payoff_vs_eq(game_data, defender, attacker_eq):
    result = 0
    for attacker, att_weight in attacker_eq.items():
        _, def_payoff = get_att_and_def_payoffs(game_data, attacker, defender)
        result += att_weight * def_payoff
    return result

def get_att_payoff_vs_eq(game_data, attacker, defender_eq):
    result = 0
    for defender, def_weight in defender_eq.items():
        att_payoff, _ = get_att_and_def_payoffs(game_data, attacker, defender)
        result += def_weight * att_payoff
    return result

def get_att_and_def_eq_payoffs(game_data, attacker_eq, defender_eq):
    att_result = 0
    def_result = 0
    for defender, def_weight in defender_eq.items():
        for attacker, att_weight in attacker_eq.items():
            att_payoff, def_payoff = get_att_and_def_payoffs(game_data, attacker, defender)
            att_result += def_weight * att_weight * att_payoff
            def_result += def_weight * att_weight * def_payoff
    return att_result, def_result

def get_all_payoffs_vs_eqs(game_data, att_eq_files, def_eq_files):
    def_eq_payoffs = []
    att_eq_payoffs = []
    for i in range(len(def_eq_files)):
        def_eq = get_eq_from_file(def_eq_files[i])
        att_eq = get_eq_from_file(att_eq_files[i])
        att_payoff, def_payoff = get_att_and_def_eq_payoffs(game_data, att_eq, def_eq)
        def_eq_payoffs.append(def_payoff)
        att_eq_payoffs.append(att_payoff)
    return def_eq_payoffs, att_eq_payoffs

def get_all_def_payoffs_vs_eqs(game_data, def_nets, att_eq_files):
    result = []
    for def_net in def_nets:
        def_row = []
        for att_eq_file in att_eq_files:
            if def_net is None:
                def_row.append(None)
            else:
                att_eq = get_eq_from_file(att_eq_file)
                def_payoff = get_def_payoff_vs_eq(game_data, def_net, att_eq)
                def_row.append(def_payoff)
        result.append(def_row)
    return result

def get_all_att_payoffs_vs_eqs(game_data, att_nets, def_eq_files):
    result = []
    for def_eq_file in def_eq_files:
        def_eq = get_eq_from_file(def_eq_file)
        def_row = []
        for att_net in att_nets:
            if att_net is None:
                def_row.append(None)
            else:
                att_payoff = get_att_payoff_vs_eq(game_data, att_net, def_eq)
                def_row.append(att_payoff)
        result.append(def_row)
    return result

def get_att_and_def_payoffs(game_data, attacker, defender):
    for profile in game_data["profiles"]:
        def_payoff = None
        att_payoff = None
        for group in profile["symmetry_groups"]:
            if group["strategy"] == defender and group["role"] == "defender":
                def_payoff = group["payoff"]
            if group["strategy"] == attacker and group["role"] == "attacker":
                att_payoff = group["payoff"]
        if def_payoff is not None and att_payoff is not None:
            return (att_payoff, def_payoff)
    raise ValueError("Missing payoffs: " + attacker + "\t" + defender)

def get_def_att_round_to_payoff(player_payoffs):
    x_train = []
    y_train = []
    for i in range(len(player_payoffs)):
        for j in range(len(player_payoffs[0])):
            payoff = player_payoffs[i][j]
            if payoff is not None:
                x_train.append([i, j])
                y_train.append(payoff)
    return x_train, y_train

def get_all_def_and_att_payoffs(game_data, att_nets, def_nets):
    def_result = []
    att_result = []
    for def_net in def_nets:
        def_row = []
        att_row = []
        for att_net in att_nets:
            att_payoff = None
            def_payoff = None
            if def_net is not None and att_net is not None:
                att_payoff, def_payoff = \
                    get_att_and_def_payoffs(game_data, att_net, def_net)
            def_row.append(def_payoff)
            att_row.append(att_payoff)
        def_result.append(def_row)
        att_result.append(att_row)
    return def_result, att_result

def plot_payoffs_by_round(att_eq_payoffs, att_dev_payoffs, def_eq_payoffs, \
    def_dev_payoffs, figure_title, save_title):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    dev_payoffs = [max(def_eq_payoffs[i], def_dev_payoffs[i]) \
        for i in range(len(def_dev_payoffs))]
    plt.plot(range(len(def_eq_payoffs)), def_eq_payoffs, lw=my_lw, label='Def. eq.')
    plt.plot(range(len(def_dev_payoffs)), dev_payoffs, linestyle='--', \
        lw=my_lw, label='Def. dev.')

    dev_payoffs = [max(att_eq_payoffs[i], att_dev_payoffs[i]) \
        for i in range(len(att_dev_payoffs))]
    plt.plot(range(len(att_eq_payoffs)), att_eq_payoffs, lw=my_lw, label='Att. eq.')
    plt.plot(range(len(att_dev_payoffs)), dev_payoffs, linestyle='--', \
        lw=my_lw, label='Att. dev.')

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean Payoff', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    plt.title(figure_title, fontsize=20)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    ax.legend()
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
    savefig(save_title)

def plot_payoff_heatmap(payoffs, figure_title, save_title):
    y_len = len(payoffs)
    x_len = len(payoffs[0])

    for_imshow = np.array(payoffs)

    mask = np.zeros(for_imshow.shape)
    for i in range(y_len):
        for j in range(x_len):
            if payoffs[i][j] is None:
                mask[i][j] = 1

    for_imshow = np.ma.array(for_imshow, mask=mask, dtype=float)

    fig, ax = plt.subplots()
    fig.set_size_inches(5, 5)

    cmap = cm.get_cmap('jet') # jet doesn't have white color
    cmap.set_bad('w')

    im = ax.imshow(for_imshow, interpolation="nearest", cmap=cmap)

    x_vals = ax.get_xlim()
    y_vals = x_vals
    ax.plot(x_vals, y_vals, ls="-", c="dimgray", lw=1.0)

    tick_spacing = 2
    ax.xaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))
    ax.yaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Defender iteration', fontsize=16)
    plt.xlabel('Attacker iteration', fontsize=16)
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

d30_def_nets = [
    "dg_rand_30n_noAnd_B_eq_2.pkl",
    "depgraph_dq_mlp_rand_epoch2_b.pkl",
    "depgraph_dq_mlp_rand_epoch3.pkl",
    "depgraph_dq_mlp_rand_epoch4.pkl",
    "depgraph_dq_mlp_rand_epoch5.pkl",
    "depgraph_dq_mlp_rand_epoch6e.pkl",
    "depgraph_dq_mlp_rand_epoch7e.pkl",
    "depgraph_dq_mlp_rand_epoch8_fixed.pkl",
    "depgraph_dq_mlp_rand_epoch9_fixed.pkl",
    "depgraph_dq_mlp_rand_epoch10_fixed.pkl",
    "depgraph_dq_mlp_rand_epoch11_fixed.pkl",
    "depgraph_dq_mlp_rand_epoch12.pkl",
    "depgraph_dq_mlp_rand_epoch13.pkl",
    "depgraph_dq_mlp_rand_epoch14.pkl",
    "depgraph_dq_mlp_rand_epoch15.pkl",
    "depgraph_dq_mlp_rand_epoch16.pkl",
    None,
    "depgraph_dq_mlp_rand_epoch18.pkl",
    None,
    None,
    "depgraph_dq_mlp_rand_epoch21.pkl",
    "depgraph_dq_mlp_rand_epoch22.pkl",
    "depgraph_dq_mlp_rand_epoch23.pkl"
]

d30_att_nets = [
    "dg_dqmlp_rand30NoAnd_B_att_fixed.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch2_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch3_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch4_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch5_att_c.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch6e_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch7e_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch8_att_fixed.pkl",
    None,
    "dg_dqmlp_rand30NoAnd_B_epoch10_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch11_att.pkl",
    None,
    "dg_dqmlp_rand30NoAnd_B_epoch13_att.pkl",
    None,
    None,
    "dg_dqmlp_rand30NoAnd_B_epoch16_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch17_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch18_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch19_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch20_att.pkl",
    "dg_dqmlp_rand30NoAnd_B_epoch21_att.pkl",
    None,
    None
]

s29_def_nets = [
    "dg_sl29_dq_mlp_rand_epoch1.pkl",
    "dg_sl29_dq_mlp_rand_epoch2.pkl",
    "dg_sl29_dq_mlp_rand_epoch3.pkl",
    "dg_sl29_dq_mlp_rand_epoch4.pkl",
    "dg_sl29_dq_mlp_rand_epoch5.pkl",
    "dg_sl29_dq_mlp_rand_epoch6.pkl",
    "dg_sl29_dq_mlp_rand_epoch7.pkl",
    "dg_sl29_dq_mlp_rand_epoch8.pkl",
    "dg_sl29_dq_mlp_rand_epoch9.pkl",
    "dg_sl29_dq_mlp_rand_epoch10.pkl",
    "dg_sl29_dq_mlp_rand_epoch11.pkl",
    "dg_sl29_dq_mlp_rand_epoch12.pkl",
    "dg_sl29_dq_mlp_rand_epoch13.pkl",
    "dg_sl29_dq_mlp_rand_epoch14.pkl",
    "dg_sl29_dq_mlp_rand_epoch15.pkl",
    "dg_sl29_dq_mlp_rand_epoch16.pkl",
    "dg_sl29_dq_mlp_rand_epoch17.pkl",
    "dg_sl29_dq_mlp_rand_epoch18.pkl",
    "dg_sl29_dq_mlp_rand_epoch19.pkl",
    "dg_sl29_dq_mlp_rand_epoch20.pkl",
    "dg_sl29_dq_mlp_rand_epoch21.pkl",
    "dg_sl29_dq_mlp_rand_epoch22.pkl",
    "dg_sl29_dq_mlp_rand_epoch23.pkl",
    "dg_sl29_dq_mlp_rand_epoch24.pkl",
    "dg_sl29_dq_mlp_rand_epoch25.pkl",
    "dg_sl29_dq_mlp_rand_epoch26.pkl",
    "dg_sl29_dq_mlp_rand_epoch27.pkl",
    "dg_sl29_dq_mlp_rand_epoch28.pkl",
    "dg_sl29_dq_mlp_rand_epoch29.pkl",
    "dg_sl29_dq_mlp_rand_epoch30.pkl",
    "dg_sl29_dq_mlp_rand_epoch31.pkl",
    "dg_sl29_dq_mlp_rand_epoch32.pkl"
]

s29_att_nets = [
    "dg_sl29_dq_mlp_rand_epoch1_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch2_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch3_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch4_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch5_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch6_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch7_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch8_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch9_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch10_att.pkl",
    None,
    "dg_sl29_dq_mlp_rand_epoch12_att.pkl",
    None,
    "dg_sl29_dq_mlp_rand_epoch14_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch15_att.pkl",
    None,
    None,
    "dg_sl29_dq_mlp_rand_epoch18_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch19_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch20_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch21_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch22_att.pkl",
    None,
    "dg_sl29_dq_mlp_rand_epoch24_att.pkl",
    None,
    None,
    "dg_sl29_dq_mlp_rand_epoch27_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch28_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch29_att.pkl",
    None,
    None,
    None
]

d30_def_dev_payoffs = [-90.11, -79.61, -82.9, -45.03, -65.44,
                       -77.32, -73.96, -36.94, -68.68, -85.93, -50.91, -64.19, -65.28,
                       -74.2, -82.49, -84.99, -93.95, -75.16, -87.40, -89.4, -64.16,
                       -70.34, -82.64, -88.10]
d30_att_dev_payoffs = [112.82, 53.38, 40.11, 42.84, 48.92,
                       54.25, 48.44, 43.5, 28.82, 58.65, 62.20, 31.88, 49.75, 38.83, \
                       38.80, 49.37, 63.62, 45.54, 41.35, 38.73, 47.13, 34.98, 30.29, \
                       33.68]

s29_def_dev_payoffs = [-327.56, -242.54, -233.63, -223.84, -137.24,
                       -260.49, -183.96, -289.11, -160.55, -212.81, -222.4,
                       -263.41, -242.48, -265.72, -210.53, -222.96, -283.34, -312.57,
                       -323.34, -293.78, -259.87, -166.76, -234.39, -196.45, -202.20,
                       -224.00, -259.02, -202.67, -236.20, -207.76, -234.83, -218.59,
                       -298.08]
s29_att_dev_payoffs = [122.78, 106.37, 223.11, 130.61, 200.38,
                       161.13, 172.57, 137.11, 154.66, 156.99, 132.49, 180.04, 132.56,
                       148.01, 169.82, 126.66, 76.69, 120.85, 164.64, 110.68, 129.52,
                       157.14, 107.79, 144.74, 98.48, 95.31, 135.84, 142.50, 144.77,
                       109.96, 135.34, 125.05, 128.14]

game_folder = "game_outputs/"

d30_game_name = game_folder + "game_3014_23.json"
s29_game_name = game_folder + "game_3013_32.json"

d30_game = get_json(d30_game_name)
s29_game = get_json(s29_game_name)

d30_def_payoffs, d30_att_payoffs = \
    get_all_def_and_att_payoffs(d30_game, d30_att_nets, d30_def_nets)
plot_payoff_heatmap(d30_def_payoffs, "$r_{30}$ defender payoffs", "d30_def_net_payoffs.pdf")
plot_payoff_heatmap(d30_att_payoffs, "$r_{30}$ attacker payoffs", "d30_att_net_payoffs.pdf")

s29_def_payoffs, s29_att_payoffs = \
    get_all_def_and_att_payoffs(s29_game, s29_att_nets, s29_def_nets)
plot_payoff_heatmap(s29_def_payoffs, "$s_{29}$ defender payoffs", "s29_def_net_payoffs.pdf")
plot_payoff_heatmap(s29_att_payoffs, "$s_{29}$ attacker payoffs", "s29_att_net_payoffs.pdf")

d30_def_eq_payoffs = get_all_def_payoffs_vs_eqs(d30_game, d30_def_nets, d30_att_eqs)
d30_att_eq_payoffs = get_all_att_payoffs_vs_eqs(d30_game, d30_att_nets, d30_def_eqs)
plot_payoff_heatmap(d30_def_eq_payoffs, "$r_{30}$ defender vs. eq payoffs", \
    "d30_def_eq_payoffs.pdf")
plot_payoff_heatmap(d30_att_eq_payoffs, "$r_{30}$ attacker vs. eq payoffs", \
    "d30_att_eq_payoffs.pdf")

s29_def_eq_payoffs = get_all_def_payoffs_vs_eqs(s29_game, s29_def_nets, s29_att_eqs)
s29_att_eq_payoffs = get_all_att_payoffs_vs_eqs(s29_game, s29_att_nets, s29_def_eqs)
plot_payoff_heatmap(s29_def_eq_payoffs, "$s_{29}$ defender vs. eq payoffs", \
    "s29_def_eq_payoffs.pdf")
plot_payoff_heatmap(s29_att_eq_payoffs, "$s_{29}$ attacker vs. eq payoffs", \
    "s29_att_eq_payoffs.pdf")

d30_def_eq_payoffs_list, d30_att_eq_payoffs_list = \
    get_all_payoffs_vs_eqs(d30_game, d30_att_eqs, d30_def_eqs)
plot_payoffs_by_round(d30_att_eq_payoffs_list, d30_att_dev_payoffs, \
    d30_def_eq_payoffs_list, d30_def_dev_payoffs, "$r_{30}$ vs. equilibrium", \
    "d30_vs_eq_from_game.pdf")

s29_def_eq_payoffs_list, s29_att_eq_payoffs_list = \
    get_all_payoffs_vs_eqs(s29_game, s29_att_eqs, s29_def_eqs)
plot_payoffs_by_round(s29_att_eq_payoffs_list, s29_att_dev_payoffs, \
    s29_def_eq_payoffs_list, s29_def_dev_payoffs, "$s_{29}$ vs. equilibrium", \
    "s29_vs_eq_from_game.pdf")

def run_ridge(ridge_alpha, x_train, y_train):
    ridge_base_ms = Ridge(alpha=ridge_alpha, fit_intercept=True, copy_X=True)
    ridge_base_ms.fit(x_train, y_train)
    yval_hat = ridge_base_ms.predict(x_train)
    mse_ridge_train = mean_squared_error(yval_hat, y_train)
    print('Train MSE ms: ' + str(mse_ridge_train))
    # print('Bias: ' + ridge_base_ms.bias_)
    print('Coefficients: ' + str(ridge_base_ms.coef_))
    print("Intercept: " + str(ridge_base_ms.intercept_))

my_ridge_alpha = 2000.0
d30_def_x_train, d30_def_y_train = get_def_att_round_to_payoff(d30_def_payoffs)
print("\nFor d30 defender vs. net payoffs:")
run_ridge(my_ridge_alpha, d30_def_x_train, d30_def_y_train)

d30_att_x_train, d30_att_y_train = get_def_att_round_to_payoff(d30_att_payoffs)
print("\nFor d30 attacker vs. net payoffs:")
run_ridge(my_ridge_alpha, d30_att_x_train, d30_att_y_train)

s29_def_x_train, s29_def_y_train = get_def_att_round_to_payoff(s29_def_payoffs)
print("\nFor s29 defender vs. net payoffs:")
run_ridge(my_ridge_alpha, s29_def_x_train, s29_def_y_train)

s29_att_x_train, s29_att_y_train = get_def_att_round_to_payoff(s29_att_payoffs)
print("\nFor s29 attacker vs. net payoffs:")
run_ridge(my_ridge_alpha, s29_att_x_train, s29_att_y_train)

d30_def_eq_x_train, d30_def_eq_y_train = get_def_att_round_to_payoff(d30_def_eq_payoffs)
print("\nFor d30 defender vs. eq payoffs:")
run_ridge(my_ridge_alpha, d30_def_eq_x_train, d30_def_eq_y_train)

d30_att_eq_x_train, d30_att_eq_y_train = get_def_att_round_to_payoff(d30_att_eq_payoffs)
print("\nFor d30 attacker vs. eq payoffs:")
run_ridge(my_ridge_alpha, d30_att_eq_x_train, d30_att_eq_y_train)

s29_def_eq_x_train, s29_def_eq_y_train = get_def_att_round_to_payoff(s29_def_eq_payoffs)
print("\nFor s29 defender vs. eq payoffs:")
run_ridge(my_ridge_alpha, s29_def_eq_x_train, s29_def_eq_y_train)

s29_att_eq_x_train, s29_att_eq_y_train = get_def_att_round_to_payoff(s29_att_eq_payoffs)
print("\nFor s29 attacker vs. eq payoffs:")
run_ridge(my_ridge_alpha, s29_att_eq_x_train, s29_att_eq_y_train)
