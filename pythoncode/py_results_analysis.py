# import os
import json
import matplotlib.pyplot as plt
from matplotlib import cm
from matplotlib import colorbar
import matplotlib.ticker as ticker
import numpy as np
from pylab import savefig

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_mean_network_round(network_round_list):
    result = 0.0
    for cur_round, frequency in network_round_list.items():
        result += int(cur_round) * frequency
    return result

def get_lines(file_name):
    lines = None
    with open(file_name) as f:
        lines = f.readlines()
    return lines

def get_values_list(lines, string_indicator):
    result = []
    for line in lines:
        if string_indicator in line:
            first_bar_index = line.find("|")
            second_bar_index = line.find("|", first_bar_index + 1)
            third_bar_index = line.find("|", second_bar_index + 1)
            inner_part = line[second_bar_index + 1:third_bar_index].strip()
            float_value = float(inner_part)
            result.append(float_value)
    return result

def get_rewards_list(lines):
    return get_values_list(lines, "episode re")

def get_episodes_list(lines):
    return get_values_list(lines, "episodes")

def get_json(file_name):
    with open(file_name) as f:
        return json.load(f)

def get_mean_payoffs_with_stderr(regrets_file_name, network_list):
    regrets_file = "regret_outputs/" + regrets_file_name
    regrets_data = get_json(regrets_file)
    mean_payoffs = []
    standard_errors = []

    for network in network_list:
        entry = regrets_data[network]
        mean_payoffs.append(entry[0])
        standard_errors.append(entry[2])

    return (mean_payoffs, standard_errors)

def get_is_in_support_list(network_names, round_fractions_list, round_number):
    round_fractions = round_fractions_list[round_number]
    result = []
    for index in range(1, len(network_names) + 1):
        index_str = str(index)
        result.append(index_str in round_fractions)
    return result

def plot_mean_rewards_and_errors(mean_payoffs, standard_errors, \
    eq_mean_payoff, eq_standard_error, is_in_support_list, out_file_name):
    fig, ax = plt.subplots()
    fig.set_size_inches(6, 4)
    for i in range(len(mean_payoffs)):
        is_in_support = is_in_support_list[i]
        marker_color = 'r' if is_in_support else 'b'
        plt.plot(i + 1, mean_payoffs[i] - standard_errors[i],
                 marker='x', color='k', markersize=8)
        plt.plot(i + 1, mean_payoffs[i],
                 marker='o', color=marker_color, markersize=8)
        plt.plot(i + 1, mean_payoffs[i] + standard_errors[i],
                 marker='x', color='k', markersize=8)
    plt.axhline(y=eq_mean_payoff, xmin=0, xmax=len(mean_payoffs), \
                color='g', linestyle='-.', lw=2)
    plt.axhline(y=eq_mean_payoff - eq_standard_error,
                xmin=0, xmax=len(mean_payoffs), \
                color='g', linestyle='-.', lw=1)
    plt.axhline(y=eq_mean_payoff + eq_standard_error,
                xmin=0, xmax=len(mean_payoffs), \
                color='g', linestyle='-.', lw=1)
    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean Payoff', fontsize=16)
    plt.xlabel('Learning round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    plt.xticks(range(1, len(mean_payoffs) + 1))
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
    v_buffer = 10
    my_min = min([mean_payoffs[i] - standard_errors[i] for i in range(len(mean_payoffs))])
    my_max = max([mean_payoffs[i] + standard_errors[i] for i in range(len(mean_payoffs))])
    my_max = max(my_max, eq_mean_payoff + eq_standard_error)
    plt.ylim(my_min - v_buffer, my_max + v_buffer)
    plt.xlim(0, len(mean_payoffs) + 1)
    ax.tick_params(labelsize=14)
    plt.tight_layout()
    savefig(out_file_name)

def plot_usage_heatmap(round_fractions, figure_title, save_title):
    max_round = len(round_fractions)
    def_choices = []
    for i in range(max_round):
        cur_choices = []
        cur_dict = round_fractions[i]
        for j in range(max_round):
            cur_str = str(j)
            if cur_str in cur_dict:
                cur_choices.append(1.0 * cur_dict[cur_str])
            else:
                cur_choices.append(0.0)
        def_choices.append(cur_choices)

    for_imshow = np.array(def_choices)

    mask = np.transpose(np.tri(for_imshow.shape[0], k=-1))

    for_imshow = np.ma.array(for_imshow, mask=mask)

    fig, ax = plt.subplots()
    fig.set_size_inches(5, 5)

    cmap = cm.get_cmap('jet') # jet doesn't have white color
    cmap.set_bad('w')

    im = ax.imshow(for_imshow, interpolation="nearest", cmap=cmap)

    tick_spacing = 3
    ax.xaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))
    ax.yaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Training round', fontsize=16)
    plt.xlabel('Network used', fontsize=16)
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

d30_def_eq_payoffs = [-98.92, -85.59, -92.52, -83.25,
                      -80.3, -87.03, -76.7, -73.95, -82.38, -88.2, -82.86, -82.86, -79.81,
                      -83.27, -85.15, -85.1, -89.65, -82.57, -85.37, -87.12, -78.23,
                      -77.75, -84.13, -87.10]
d30_def_dev_payoffs = [-90.11, -79.61, -82.9, -45.03, -65.44,
                       -77.32, -73.96, -36.94, -68.68, -85.93, -50.91, -64.19, -65.28,
                       -74.2, -82.49, -84.99, -93.95, -75.16, -87.40, -89.4, -64.16,
                       -70.34, -82.64, -88.10]
d30_att_eq_payoffs = [39.32, 40.81, 30.43, 40.31, 43.44,
                      35.34, 38.64, 38.67, 40.82, 35.07, 42.55, 41.45, 33.12, 39.81, \
                      38.85, 39.15, 38.85, 40.96, 33.29, 36.36, 44.04, 44.76, 38.48, \
                      37.39]
d30_att_dev_payoffs = [112.82, 53.38, 40.11, 42.84, 48.92, 
                       54.25, 48.44, 43.5, 28.82, 58.65, 62.20, 31.88, 49.75, 38.83, \
                       38.80, 49.37, 63.62, 45.54, 41.35, 38.73, 47.13, 34.98, 30.29, \
                       33.68]

d30_def_frac_learned = [0, 0.74, 1, 0.85, 0.81, 0.95, 0.89, 0.92, 0.92, 0.97, 1.0, 1.0, 1,
                        0.93, 1, 1, 0.84, 0.91, 0.94, 0.99, 0.93, 1, 0.96, 0.94]
d30_att_frac_learned = [0, 0.25, 0.37, 1, 0.58, 0.61, 1, 1, 0.72, 0.40, 0.94, 0.69, 0.92,
                        0.67, 0.71, 0.56, 0.29, 0.62, 0.53, 0.47, 1, 0.77, 0.63, 0.49]

d30_att_learning_curves = ["tdj_dg_rand30NoAnd_B_att_fixed.txt",
                           "tdj_dq_randNoAnd_B_epoch2_vsDef.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch3.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch4.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch5_c2.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch6e.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch7e.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch8_fixed.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch9.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch10.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch11.txt",
                           "tdj_mlp_attVMixed_randNoAnd_B_epoch12.txt",
                           "attVMixed_d30_epoch13.txt",
                           "attVMixed_d30_epoch14.txt",
                           "attVMixed_d30_epoch15.txt",
                           "attVMixed_d30_epoch16.txt",
                           "attVMixed_d30_epoch17.txt",
                           "attVMixed_d30_epoch18.txt",
                           "attVMixed_d30_epoch19.txt",
                           "attVMixed_d30_epoch20.txt",
                           "attVMixed_d30_epoch21.txt",
                           "attVMixed_d30_epoch22.txt",
                           "attVMixed_d30_epoch23.txt",
                           "attVMixed_d30_epoch24.txt"]
d30_def_learning_curves = ["tdj_mlp_defVMixed_randNoAnd_B.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_2.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch3.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch4_fixed.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch5.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch6e.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch7e.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch8_fixed.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch9.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch10.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch11.txt",
                           "tdj_mlp_defVMixed_randNoAnd_B_epoch12.txt",
                           "defVMixed_d30_epoch13.txt",
                           "defVMixed_d30_epoch14.txt",
                           "defVMixed_d30_epoch15.txt",
                           "defVMixed_d30_epoch16.txt",
                           "defVMixed_d30_epoch17.txt",
                           "defVMixed_d30_epoch18.txt",
                           "defVMixed_d30_epoch19.txt",
                           "defVMixed_d30_epoch20.txt",
                           "defVMixed_d30_epoch21.txt",
                           "defVMixed_d30_epoch22.txt",
                           "defVMixed_d30_epoch23.txt",
                           "defVMixed_d30_epoch24.txt"]

d30_def_round_fractions = [
    {"0": 1},
    {"0": 0.26, "1": 0.74},
    {"2": 1},
    {"0": 0.15, "2": 0.64, "3": 0.21},
    {"0": 0.19, "2": 0.61, "3": 0.09, "4": 0.11},
    {"0": 0.05, "1": 0.43, "3": 0.1, "5": 0.42},
    {"0": 0.11, "2": 0.62, "5": 0.07, "6": 0.2},
    {"0": 0.08, "2": 0.63, "3": 0.05, "5": 0.09, "6": 0.15},
    {"0": 0.08, "1": 0.18, "2": 0.47, "3": 0.11, "5": 0.08, "8": 0.08},
    {"0": 0.03, "1": 0.05, "2": 0.39, "5": 0.01, "6": 0.03, "7": 0.01, "9": 0.48},
    {"8": 0.14, "10": 0.86},
    {"2": 0.34, "9": 0.10, "10": 0.41, "11": 0.15},
    {"2": 0.31, "4": 0.03, "5": 0.02, "9": 0.03, "10": 0.42, "12": 0.19},
    {"0": 0.07, "1": 0.06, "2": 0.26, "9": 0.03, "12": 0.16, "13": 0.42},
    {"1": 0.06, "2": 0.09, "3": 0.04, "6": 0.03, "9": 0.03, "10": 0.35, "13": 0.07, 
     "14": 0.33},
    {"1": 0.01, "2": 0.14, "6": 0.05, "9": 0.01, "10": 0.39, "14": 0.32, "15": 0.08},
    {"0": 0.16, "16": 0.84},
    {"0": 0.09, "10": 0.21, "12": 0.07, "13": 0.28, "14": 0.09, "16": 0.26},
    {"0": 0.06, "1": 0.11, "10": 0.30, "12": 0.03, "16": 0.08, "18": 0.42},
    {"0": 0.01, "1": 0.22, "2": 0.05, "6": 0.10, "12": 0.19, "18": 0.43},
    {"0": 0.07, "4": 0.02, "6": 0.28, "12": 0.15, "13": 0.13, "16": 0.13, "18": 0.22},
    {"6": 0.09, "8": 0.04, "9": 0.09, "14": 0.12, "16": 0.34, "18": 0.32},
    {"0": 0.04, "6": 0.13, "10": 0.03, "12": 0.03, "16": 0.23, "18": 0.33, "22": 0.21},
    {"0": 0.06, "1": 0.03, "2": 0.07, "6": 0.03, "12": 0.05, "16": 0.28, "18": 0.27,
        "22": 0.11, "23": 0.10}
]

d30_att_round_fractions = [
    {"0": 1},
    {"0": 0.76, "1": 0.24},
    {"0": 0.63, "2": 0.37},
    {"1": 0.3, "2": 0.17, "3": 0.53},
    {"0": 0.41, "1": 0.01, "2": 0.16, "4": 0.42},
    {"0": 0.39, "2": 0.13, "3": 0.1, "5": 0.38},
    {"2": 0.02, "4": 0.34, "5": 0.09, "6": 0.55},
    {"3": 0.12, "4": 0.27, "5": 0.1, "6": 0.08, "7": 0.43},
    {"0": 0.28, "2": 0.10, "3": 0.20, "5": 0.05, "8": 0.37},
    {"0": 0.60, "1": 0.01, "2": 0.06, "4": 0.12, "5": 0.18, "6": 0.04},
    {"0": 0.06, "10": 0.94},
    {"0": 0.31, "8": 0.29, "10": 0.32, "11": 0.08},
    {"0": 0.08, "1": 0.37, "3": 0.15, "5": 0.07, "10": 0.28, "11": 0.05},
    {"0": 0.32, "1": 0.16, "4": 0.02, "5": 0.01, "8": 0.06, "13": 0.43},
    {"0": 0.31, "1": 0.11, "2": 0.16, "5": 0.12, "8": 0.12, "10": 0.05, "13": 0.13},
    {"0": 0.44, "1": 0.02, "2": 0.13, "3": 0.10, "5": 0.12, "13": 0.19},
    {"0": 0.70, "1": 0.27, "16": 0.03},
    {"0": 0.38, "1": 0.15, "8": 0.16, "13": 0.04, "16": 0.18, "17": 0.09},
    {"0": 0.46, "2": 0.03, "5": 0.21, "10": 0.18, "13": 0.12},
    {"0": 0.53, "1": 0.05, "5": 0.13, "16": 0.01, "19": 0.28},
    {"1": 0.05, "8": 0.02, "16": 0.04, "18": 0.10, "19": 0.49, "20": 0.30},
    {"0": 0.23, "10": 0.06, "16": 0.02, "18": 0.18, "19": 0.18, "21": 0.33},
    {"0": 0.37, "5": 0.21, "7": 0.08, "16": 0.02, "17": 0.02, "19": 0.18, "21": 0.12},
    {"0": 0.51, "1": 0.04, "5": 0.12, "16": 0.01, "18": 0.01, "19": 0.29, "21": 0.02}
]

fig, ax = plt.subplots()
fig.set_size_inches(8, 5)

my_lw = 2
dev_payoffs = [max(d30_def_eq_payoffs[i], d30_def_dev_payoffs[i]) \
    for i in range(len(d30_def_dev_payoffs))]
plt.plot(range(len(d30_def_eq_payoffs)), d30_def_eq_payoffs, lw=my_lw, label='Def. eq.')
plt.plot(range(len(dev_payoffs)), dev_payoffs, linestyle='--', \
    lw=my_lw, label='Def. dev.')

dev_payoffs = [max(d30_att_eq_payoffs[i], d30_att_dev_payoffs[i]) \
    for i in range(len(d30_att_dev_payoffs))]
plt.plot(range(len(d30_att_eq_payoffs)), d30_att_eq_payoffs, lw=my_lw, label='Att. eq.')
plt.plot(range(len(dev_payoffs)), dev_payoffs, linestyle='--', \
    lw=my_lw, label='Att. dev.')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Mean Payoff', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Random 30-node graph', fontsize=20)
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
savefig("d30_payoffs_vs_round.pdf")


def_gains = [max(0, d30_def_dev_payoffs[i] - d30_def_eq_payoffs[i]) for i in \
    range(len(d30_def_dev_payoffs))]
att_gains = [max(0, d30_att_dev_payoffs[i] - d30_att_eq_payoffs[i]) for i in \
    range(len(d30_att_dev_payoffs))]

fig, ax = plt.subplots()
fig.set_size_inches(7, 4)

my_lw = 2
plt.plot(range(len(def_gains)), def_gains, lw=my_lw, label='Defender')
plt.plot(range(len(att_gains)), att_gains, lw=my_lw, linestyle=':', label='Attacker')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Deviation Gain', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Random 30-node graph', fontsize=20)
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
savefig("d30_gains.pdf")

fig, ax = plt.subplots()
fig.set_size_inches(7, 4)

my_lw = 2
plt.plot(range(len(d30_def_frac_learned)), d30_def_frac_learned, lw=my_lw, label='Defender')
plt.plot(range(len(d30_att_frac_learned)), d30_att_frac_learned, lw=my_lw, \
    linestyle=':', label='Attacker')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Fraction learned', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Random 30-node graph', fontsize=20)
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

# plt.show()
savefig("d30_frac_learned.pdf")


mean_def_rounds = [get_mean_network_round(cur_list) for cur_list in d30_def_round_fractions]
mean_att_rounds = [get_mean_network_round(cur_list) for cur_list in d30_att_round_fractions]

fig, ax = plt.subplots()
fig.set_size_inches(7, 4)

my_lw = 2
plt.plot(range(len(mean_def_rounds)), mean_def_rounds, lw=my_lw, label='Defender')
plt.plot(range(len(mean_att_rounds)), mean_att_rounds, linestyle=':', lw=my_lw, \
    label='Attacker')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Mean Round in Eq. Strategy', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Random 30-node graph', fontsize=20)
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

# plt.show()
savefig("d30_mean_rounds.pdf")


iteration = 5

file_name = "learning_curves/" + d30_def_learning_curves[iteration]
own_reward = d30_def_dev_payoffs[iteration]
goal_reward = d30_def_eq_payoffs[iteration]

lines = get_lines(file_name)
episodes = get_episodes_list(lines)
rewards = get_rewards_list(lines)

fig, ax = plt.subplots()
fig.set_size_inches(6, 4)

my_lw = 2
plt.plot(episodes, rewards, lw=my_lw, c='blue', label="QL exploring")
plt.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--', label="QL final")
plt.axhline(y=goal_reward, lw=my_lw, c='red', linestyle='-.', label="Old performance")

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)

plt.legend(loc=4) # lower-right
ax.tick_params(labelsize=14)

plt.xlabel("Episode", fontsize=20)
plt.ylabel("Defender\nreward", fontsize=20)
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
# savefig("d30_def_learning_curve_example.pdf")


iteration = 0

file_name = "learning_curves/" + d30_att_learning_curves[iteration]
own_reward = d30_att_dev_payoffs[iteration]
goal_reward = d30_att_eq_payoffs[iteration]

lines = get_lines(file_name)
episodes = get_episodes_list(lines)
rewards = get_rewards_list(lines)

fig, ax = plt.subplots()
fig.set_size_inches(6, 4)

my_lw = 2
plt.plot(episodes, rewards, lw=my_lw, c='blue', label="QL exploring")
plt.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--', label="QL final")
plt.axhline(y=goal_reward, lw=my_lw, c='red', linestyle='-.', label="Old performance")

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
ax.tick_params(labelsize=14)

plt.legend(loc=4) # lower-right

plt.xlabel("Episode", fontsize=20)
plt.ylabel("Attacker\nreward", fontsize=20)
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
# savefig("d30_att_learning_curve_example.pdf")


rows = 5
cols = 5
fig, axes = plt.subplots(nrows=rows, ncols=cols, sharex=True, sharey='row', \
                         figsize=(10, 10))

episode_lists = []
reward_lists = []
for i in range(len(d30_att_learning_curves)):
    file_name = "learning_curves/" + d30_att_learning_curves[i]
    lines = get_lines(file_name)
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    reward_lists.append(rewards)

y_max = max([max(y) for y in reward_lists])

for i in range(len(reward_lists)):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    own_reward = d30_att_dev_payoffs[i]
    goal_reward = d30_att_eq_payoffs[i]
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c='blue')
    ax.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--')
    ax.axhline(y=goal_reward, lw=my_lw, c='red', linestyle='-.')
    ax.set_title("Round " + str(i), fontsize=16)
    ax.tick_params(
        which='both',
        bottom='off',
        left='off',
        right='off',
        top='off'
    )
    ax.grid(linewidth=1, linestyle=':')
    ax.spines['left'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.set_ylim((-5, y_max + 5))
    ax.tick_params(labelsize=12)

for i in range(len(d30_att_learning_curves), rows * cols):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    ax.remove()

savefig("d30_att_learning_curves.pdf")



rows = 5
cols = 5
fig, axes = plt.subplots(nrows=rows, ncols=cols, sharex=True, sharey='row', \
    figsize=(10, 10))

episode_lists = []
reward_lists = []
for i in range(len(d30_def_learning_curves)):
    file_name = "learning_curves/" + d30_def_learning_curves[i]
    lines = get_lines(file_name)
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    reward_lists.append(rewards)

y_min = min([min(y) for y in reward_lists])
y_max = max([max(y) for y in reward_lists])

for i in range(len(reward_lists)):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    own_reward = d30_def_dev_payoffs[i]
    goal_reward = d30_def_eq_payoffs[i]
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c='blue')
    ax.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--')
    ax.axhline(y=goal_reward, lw=my_lw, c='red', linestyle='-.')
    ax.set_title("Round " + str(i), fontsize=16)
    ax.tick_params(
        which='both',
        bottom='off',
        left='off',
        right='off',
        top='off'
    )
    ax.grid(linewidth=1, linestyle=':')
    ax.spines['left'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.set_ylim((y_min - 5, y_max + 5))
    ax.tick_params(labelsize=14)

for i in range(len(d30_def_learning_curves), rows * cols):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    ax.remove()

savefig("d30_def_learning_curves.pdf")


my_cmap = 'gnuplot'

fig, ax = plt.subplots()
fig.set_size_inches(6, 4)

episode_lists = []
reward_lists = []
cmap = plt.cm.get_cmap(my_cmap)
colors = []
for i in range(len(d30_def_learning_curves)):
    frac = i * 1.0 / (len(d30_def_learning_curves) - 1)
    rgba = cmap(frac)
    colors.append(rgba)

    file_name = "learning_curves/" + d30_def_learning_curves[i]
    lines = get_lines(file_name)

    goal_reward = d30_def_eq_payoffs[i]
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    rewards = [x - goal_reward for x in rewards]
    reward_lists.append(rewards)

for i in range(len(reward_lists)):
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c=colors[i])

ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

y_min = min([min(y) for y in reward_lists])
y_max = max([max(y) for y in reward_lists])

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
ax.grid(linewidth=1, linestyle=':')
ax.spines['left'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.spines['bottom'].set_visible(False)
ax.set_ylim((y_min - 5, y_max + 5))
ax.tick_params(labelsize=14)
plt.xlabel("Episode", fontsize=16)
plt.ylabel("Defender gain", fontsize=16)
plt.title("Random 30-node graph", fontsize=20)
plt.tight_layout()

savefig("d30_def_learning_curves_merged.pdf")


fig, ax = plt.subplots()
fig.set_size_inches(6, 4)

episode_lists = []
reward_lists = []
cmap = plt.cm.get_cmap(my_cmap)
colors = []
for i in range(len(d30_att_learning_curves)):
    frac = i * 1.0 / (len(d30_att_learning_curves) - 1)
    rgba = cmap(frac)
    colors.append(rgba)

    file_name = "learning_curves/" + d30_att_learning_curves[i]
    lines = get_lines(file_name)

    goal_reward = d30_att_eq_payoffs[i]
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    rewards = [x - goal_reward for x in rewards]
    reward_lists.append(rewards)

for i in range(len(reward_lists)):
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c=colors[i])

ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

y_min = min([min(y) for y in reward_lists])
y_max = max([max(y) for y in reward_lists])

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
ax.grid(linewidth=1, linestyle=':')
ax.spines['left'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.spines['bottom'].set_visible(False)
ax.set_ylim((y_min - 5, y_max + 5))
ax.tick_params(labelsize=14)
plt.xlabel("Episode", fontsize=16)
plt.ylabel("Attacker gain", fontsize=16)
plt.title("Random 30-node graph", fontsize=20)
plt.tight_layout()

savefig("d30_att_learning_curves_merged.pdf")


plot_usage_heatmap(d30_def_round_fractions, \
    "Random 30-node graph: Defender", "d30_def_choices.pdf")

plot_usage_heatmap(d30_att_round_fractions, \
    "Random 30-node graph: Attacker", "d30_att_choices.pdf")


s29_def_eq_payoffs = [-342.32, -299.64, -259.95, -311.71,
                      -252.11, -283.81, -307.51, -316.22, -295.01, -344.13, -280.96,
                      -305.32, -300.99, -291.09, -274.06, -295.48, -295.48, -319.55,
                      -325.48, -311.04, -282.95, -262.05, -267.58, -253.03, -273.07,
                      -278.79, -285.74, -262.80, -263.10, -266.28, -266.62, -266.1,
                      -291.04]
s29_def_dev_payoffs = [-327.56, -242.54, -233.63, -223.84, -137.24,
                       -260.49, -183.96, -289.11, -160.55, -212.81, -222.4,
                       -263.41, -242.48, -265.72, -210.53, -222.96, -283.34, -312.57,
                       -323.34, -293.78, -259.87, -166.76, -234.39, -196.45, -202.20,
                       -224.00, -259.02, -202.67, -236.20, -207.76, -234.83, -218.59,
                       -293.08]
s29_att_eq_payoffs = [44.51, 87.61, 110.72, 117.84, 154.79,
                      122.88, 140.19, 136.72, 127.67, 151.37, 134.29, 147.05, 144.86,
                      124.33, 123.68, 126.76, 109.75, 96.48, 110.84, 103.16, 109.82,
                      116.01, 112.91, 122.71, 120.21, 123.36, 126.91, 134.30, 122.17,
                      132.42, 138.15, 134.64, 129.52]
s29_att_dev_payoffs = [122.78, 106.37, 223.11, 130.61, 200.38,
                       161.13, 172.57, 137.11, 154.66, 156.99, 132.49, 180.04, 132.56,
                       148.01, 169.82, 126.66, 76.69, 120.85, 164.64, 110.68, 129.52,
                       157.14, 107.79, 144.74, 98.48, 95.31, 135.84, 142.50, 144.77,
                       109.96, 135.34, 125.05, 128.14]

s29_def_frac_learned = [0, 0.61, 0.34, 0.82, 0.8, 0.57, 0.2, 0.63,
                        0.56, 0.55, 0.38, 0.56, 0.35, 0.66, 0.36, 0.56, 0.79, 0.84, 1,
                        0.91, 0.88, 1, 0.70, 0.79, 0.69, 0.73, 0.70, 0.52, 0.84, 0.54,
                        0.62, 0.86, 0.86]
s29_att_frac_learned = [0, 0.47, 0.54, 0.68, 1, 0.64, 1, 1,
                        1, 0.93, 1, 1, 1, 0.7, 1, 1, 0.85, 0.7, 0.38, 0.76, 1, 0.87, 1,
                        0.99, 1, 1, 1, 1, 1, 1, 1, 0.95, 0.82]

s29_att_learning_curves = ["attVMixed_sl29_randNoAndB_epoch1.txt",
                           "attVMixed_sl29_randNoAndB_epoch2.txt",
                           "attVMixed_sl29_randNoAndB_epoch3.txt",
                           "attVMixed_sl29_randNoAndB_epoch4.txt",
                           "attVMixed_sl29_randNoAndB_epoch5.txt",
                           "attVMixed_sl29_randNoAndB_epoch6.txt",
                           "attVMixed_sl29_randNoAndB_epoch7.txt",
                           "attVMixed_sl29_randNoAndB_epoch8.txt",
                           "attVMixed_sl29_randNoAndB_epoch9.txt",
                           "attVMixed_sl29_randNoAndB_epoch10.txt",
                           "attVMixed_sl29_randNoAndB_epoch11.txt",
                           "attVMixed_sl29_randNoAndB_epoch12.txt",
                           "attVMixed_sl29_randNoAndB_epoch13.txt",
                           "attVMixed_sl29_randNoAndB_epoch14.txt",
                           "attVMixed_sl29_randNoAndB_epoch15.txt",
                           "attVMixed_sl29_randNoAndB_epoch16.txt",
                           "attVMixed_sl29_epoch17.txt",
                           "attVMixed_sl29_epoch18.txt",
                           "attVMixed_sl29_epoch19.txt",
                           "attVMixed_sl29_epoch20.txt",
                           "attVMixed_sl29_epoch21.txt",
                           "attVMixed_sl29_epoch22.txt",
                           "attVMixed_sl29_epoch23.txt",
                           "attVMixed_sl29_epoch24.txt",
                           "attVMixed_sl29_epoch25.txt",
                           "attVMixed_sl29_epoch26.txt",
                           "attVMixed_sl29_epoch27.txt",
                           "attVMixed_sl29_epoch28.txt",
                           "attVMixed_sl29_epoch29.txt",
                           "attVMixed_sl29_epoch30.txt",
                           "attVMixed_sl29_epoch31.txt",
                           "attVMixed_sl29_epoch32.txt",
                           "attVMixed_sl29_epoch33.txt"]
s29_def_learning_curves = ["defVMixed_sl29_randNoAndB_epoch1.txt",
                           "defVMixed_sl29_randNoAndB_epoch2.txt",
                           "defVMixed_sl29_randNoAndB_epoch3.txt",
                           "defVMixed_sl29_randNoAndB_epoch4.txt",
                           "defVMixed_sl29_randNoAndB_epoch5.txt",
                           "defVMixed_sl29_randNoAndB_epoch6.txt",
                           "defVMixed_sl29_randNoAndB_epoch7.txt",
                           "defVMixed_sl29_randNoAndB_epoch8.txt",
                           "defVMixed_sl29_randNoAndB_epoch9.txt",
                           "defVMixed_sl29_randNoAndB_epoch10.txt",
                           "defVMixed_sl29_randNoAndB_epoch11.txt",
                           "defVMixed_sl29_randNoAndB_epoch12.txt",
                           "defVMixed_sl29_randNoAndB_epoch13.txt",
                           "defVMixed_sl29_randNoAndB_epoch14.txt",
                           "defVMixed_sl29_randNoAndB_epoch15.txt",
                           "defVMixed_sl29_randNoAndB_epoch16.txt",
                           "defVMixed_sl29_epoch17.txt",
                           "defVMixed_sl29_epoch18.txt",
                           "defVMixed_sl29_epoch19.txt",
                           "defVMixed_sl29_epoch20.txt",
                           "defVMixed_sl29_epoch21.txt",
                           "defVMixed_sl29_epoch22.txt",
                           "defVMixed_sl29_epoch23.txt",
                           "defVMixed_sl29_epoch24.txt",
                           "defVMixed_sl29_epoch25.txt",
                           "defVMixed_sl29_epoch26.txt",
                           "defVMixed_sl29_epoch27.txt",
                           "defVMixed_sl29_epoch28.txt",
                           "defVMixed_sl29_epoch29.txt",
                           "defVMixed_sl29_epoch30.txt",
                           "defVMixed_sl29_epoch31.txt",
                           "defVMixed_sl29_epoch32.txt",
                           "defVMixed_sl29_epoch33.txt"]

s29_def_round_fractions = [
    {"0": 1},
    {"0": 0.39, "1": 0.61},
    {"0": 0.56, "2": 0.34},
    {"0": 0.18, "1": 0.82},
    {"0": 0.2, "2": 0.29, "4": 0.51},
    {"0": 0.43, "1": 0.36, "4": 0.18, "5": 0.03},
    {"0": 0.8, "6": 0.2},
    {"0": 0.37, "1": 0.57, "7": 0.06},
    {"0": 0.44, "3": 0.06, "8": 0.5},
    {"0": 0.45, "7": 0.27, "8": 0.1, "9": 0.18},
    {"0": 0.62, "7": 0.03, "8": 0.19, "9": 0.03, "10": 0.13},
    {"0": 0.44, "3": 0.02, "8": 0.15, "9": 0.03, "10": 0.07, "11": 0.29},
    {"0": 0.65, "9": 0.07, "12": 0.28},
    {"0": 0.34, "1": 0.10, "3": 0.02, "11": 0.04, "12": 0.27, "13": 0.23},
    {"0": 0.64, "11": 0.03, "13": 0.24, "14": 0.09},
    {"0": 0.34, "12": 0.18, "14": 0.35, "15": 0.13},
    {"0": 0.21, "8": 0.09, "9": 0.01, "13": 0.08, "14": 0.17, "15": 0.02, "16": 0.42},
    {"0": 0.16, "10": 0.03, "14": 0.12, "16": 0.40, "17": 0.29},
    {"14": 0.42, "16": 0.06, "17": 0.39, "18": 0.13},
    {"0": 0.09, "17": 0.39, "18": 0.23, "19": 0.29},
    {"0": 0.12, "13": 0.04, "16": 0.10, "17": 0.39, "18": 0.22, "20": 0.13},
    {"2": 0.02, "9": 0.07, "16": 0.21, "17": 0.47, "19": 0.03, "20": 0.20},
    {"0": 0.30, "11": 0.01, "13": 0.01, "14": 0.03, "16": 0.16, "17": 0.40, "21": 0.02,
        "22": 0.07},
    {"0": 0.21, "13": 0.01, "16": 0.23, "17": 0.31, "18": 0.07, "21": 0.01, "22": 0.07,
        "23": 0.09},
    {"0": 0.31, "12": 0.08, "13": 0.03, "14": 0.22, "17": 0.25, "20": 0.09, "22": 0.02},
    {"0": 0.27, "13": 0.01, "14": 0.15, "17": 0.4, "25": 0.17},
    {"0": 0.3, "12": 0.1, "13": 0.02, "14": 0.03, "17": 0.19, "18": 0.01, "25": 0.19,
        "26": 0.16},
    {"0": 0.48, "4": 0.06, "16": 0.04, "17": 0.12, "19": 0.03, "25": 0.08, "26": 0.14, 
        "27": 0.05},
    {"0": 0.16, "13": 0.02, "16": 0.11, "17": 0.06, "18": 0.01, "20": 0.13, "25": 0.11,
        "26": 0.07, "27": 0.26, "28": 0.07},
    {"0": 0.46, "17": 0.11, "20": 0.08, "23": 0.18, "25": 0.01, "28": 0.06, "29": 0.10},
    {"0": 0.38, "17": 0.16, "23": 0.18, "26": 0.05, "27": 0.06, "28": 0.05, "29": 0.04,
        "30": 0.07},
    {"0": 0.14, "13": 0.02, "16": 0.04, "17": 0.21, "20": 0.06, "21": 0.01, "23": 0.01,
        "25": 0.08, "27": 0.14, "28": 0.04, "29": 0.06, "30": 0.07, "31": 0.12},
    {"0": 0.14, "17": 0.24, "18": 0.12, "19": 0.04, "20": 0.01, "25": 0.06, "27": 0.12,
        "28": 0.02, "30": 0.09, "31": 0.08, "32": 0.08}
]

s29_att_round_fractions = [
    {"0": 1},
    {"0": 0.53, "1": 0.47},
    {"0": 0.45, "1": 0.33, "2": 0.22},
    {"0": 0.32, "2": 0.06, "3": 0.62},
    {"1": 0.46, "3": 0.26, "4": 0.28},
    {"0": 0.36, "1": 0.18, "3": 0.28, "4": 0.08, "5": 0.1},
    {"3": 0.57, "6": 0.43},
    {"3": 0.07, "6": 0.31, "7": 0.62},
    {"6": 0.08, "7": 0.56, "8": 0.36},
    {"0": 0.07, "3": 0.1, "7": 0.33, "9": 0.5},
    {"3": 0.09, "6": 0.15, "7": 0.05, "8": 0.14, "9": 0.23,
     "10": 0.34},
    {"2": 0.21, "3": 0.18, "6": 0.11, "7": 0.31, "8": 0.01, "9": 0.18},
    {"3": 0.13, "7": 0.39, "9": 0.03, "12": 0.45},
    {"0": 0.31, "7": 0.11, "9": 0.09, "10": 0.35, "12": 0.14},
    {"3": 0.16, "7": 0.19, "10": 0.20, "12": 0.12, "14": 0.33},
    {"6": 0.13, "9": 0.09, "12": 0.23, "14": 0.12, "15": 0.43},
    {"0": 0.15, "3": 0.12, "6": 0.16, "7": 0.36, "12": 0.10, "14": 0.10, "15": 0.01},
    {"0": 0.30, "6": 0.10, "7": 0.18, "9": 0.30, "12": 0.12},
    {"0": 0.63, "6": 0.13, "9": 0.24},
    {"0": 0.24, "3": 0.28, "9": 0.20, "12": 0.06, "18": 0.05, "19": 0.17},
    {"3": 0.21, "6": 0.18, "9": 0.18, "15": 0.02, "18": 0.20, "20": 0.21},
    {"0": 0.13, "6": 0.05, "9": 0.12, "18": 0.09, "20": 0.04, "21": 0.57},
    {"3": 0.19, "7": 0.11, "9": 0.09, "10": 0.13, "12": 0.02, "15": 0.01, "18": 0.10,
        "21": 0.17, "22": 0.18},
    {"0": 0.01, "3": 0.14, "7": 0.10, "9": 0.06, "10": 0.23, "18": 0.12, "19": 0.13,
        "21": 0.14, "22": 0.07},
    {"6": 0.01, "9": 0.16, "14": 0.04, "15": 0.13, "19": 0.03, "21": 0.28, "22": 0.03,
        "24": 0.32},
    {"3": 0.01, "7": 0.42, "15": 0.11, "22": 0.1, "24": 0.36},
    {"3": 0.06, "6": 0.23, "7": 0.14, "9": 0.16, "10": 0.09, "12": 0.06, "20": 0.05,
        "24": 0.21},
    {"1": 0.02, "6": 0.12, "12": 0.05, "15": 0.14, "18": 0.1, "24": 0.12, "27": 0.45},
    {"6": 0.08, "7": 0.17, "9": 0.09, "14": 0.04, "18": 0.09, "19": 0.12, "20": 0.05,
        "24": 0.04, "27": 0.04, "28": 0.28},
    {"3": 0.06, "9": 0.10, "14": 0.17, "21": 0.09, "24": 0.08, "27": 0.13, "28": 0.15,
        "29": 0.22},
    {"6": 0.01, "7": 0.25, "9": 0.01, "12": 0.18, "14": 0.07, "19": 0.03, "24": 0.01,
        "27": 0.12, "28": 0.07, "29": 0.25},
    {"0": 0.05, "3": 0.13, "7": 0.11, "15": 0.02, "18": 0.01, "19": 0.02, "20": 0.13,
        "21": 0.25, "22": 0.02, "24": 0.03, "27": 0.14, "29": 0.09},
    {"0": 0.18, "6": 0.07, "7": 0.17, "9": 0.08, "20": 0.01, "21": 0.12, "24": 0.10,
        "27": 0.15, "29": 0.12}
]

s29_def_networks = [
    "dg_sl29_dq_mlp_rand_epoch1.pkl",
    "dg_sl29_dq_mlp_rand_epoch2.pkl",
    "dg_sl29_dq_mlp_rand_epoch3.pkl",
    "dg_sl29_dq_mlp_rand_epoch4.pkl",
    "dg_sl29_dq_mlp_rand_epoch5.pkl",
    "dg_sl29_dq_mlp_rand_epoch6.pkl",
    "dg_sl29_dq_mlp_rand_epoch7.pkl",
    "dg_sl29_dq_mlp_rand_epoch8.pkl",
    "dg_sl29_dq_mlp_rand_epoch9.pkl",
    "dg_sl29_dq_mlp_rand_epoch10.pkl"]

s29_att_networks = [
    "dg_sl29_dq_mlp_rand_epoch1_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch2_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch3_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch4_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch5_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch6_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch7_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch8_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch9_att.pkl",
    "dg_sl29_dq_mlp_rand_epoch10_att.pkl"]


fig, ax = plt.subplots()
fig.set_size_inches(8, 5)

my_lw = 2
dev_payoffs = [max(s29_def_eq_payoffs[i], s29_def_dev_payoffs[i]) \
    for i in range(len(s29_def_dev_payoffs))]
plt.plot(range(len(s29_def_eq_payoffs)), s29_def_eq_payoffs, lw=my_lw, label='Def. eq.')
plt.plot(range(len(dev_payoffs)), dev_payoffs, linestyle='--', \
    lw=my_lw, label='Def. dev.')

dev_payoffs = [max(s29_att_eq_payoffs[i], s29_att_dev_payoffs[i]) \
    for i in range(len(s29_att_dev_payoffs))]
plt.plot(range(len(s29_att_eq_payoffs)), s29_att_eq_payoffs, lw=my_lw, label='Att. eq.')
plt.plot(range(len(dev_payoffs)), dev_payoffs, linestyle='--', \
    lw=my_lw, label='Att. dev.')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Mean Payoff', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Separate layers 29-node graph', fontsize=20)
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

# plt.show()
savefig("s29_payoffs_vs_round.pdf")



def_gains = [max(0, s29_def_dev_payoffs[i] - s29_def_eq_payoffs[i]) for i in \
    range(len(s29_def_dev_payoffs))]
att_gains = [max(0, s29_att_dev_payoffs[i] - s29_att_eq_payoffs[i]) for i in \
    range(len(s29_att_dev_payoffs))]

fig, ax = plt.subplots()
fig.set_size_inches(7, 4)

my_lw = 2
plt.plot(range(len(def_gains)), def_gains, lw=my_lw, label='Defender')
plt.plot(range(len(att_gains)), att_gains, lw=my_lw, linestyle=':', label='Attacker')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Deviation Gain', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Separate layers 29-node graph', fontsize=20)
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

# plt.show()
savefig("s29_gains.pdf")


fig, ax = plt.subplots()
fig.set_size_inches(7, 4)

my_lw = 2
plt.plot(range(len(s29_def_frac_learned)), s29_def_frac_learned, lw=my_lw, label='Defender')
plt.plot(range(len(s29_att_frac_learned)), s29_att_frac_learned, lw=my_lw, \
    linestyle=':', label='Attacker')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Fraction learned', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Separate layers 29-node graph', fontsize=20)
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

# plt.show()
savefig("s29_frac_learned.pdf")


mean_def_rounds = [get_mean_network_round(cur_list) for cur_list in s29_def_round_fractions]
mean_att_rounds = [get_mean_network_round(cur_list) for cur_list in s29_att_round_fractions]

fig, ax = plt.subplots()
fig.set_size_inches(7, 4)

my_lw = 2
plt.plot(range(len(mean_def_rounds)), mean_def_rounds, lw=my_lw, label='Defender')
plt.plot(range(len(mean_att_rounds)), mean_att_rounds, linestyle=':', lw=my_lw, \
    label='Attacker')

ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['left'].set_visible(False)
ax.spines['bottom'].set_visible(False)
plt.ylabel('Mean Round in Eq. Strategy', fontsize=16)
plt.xlabel('Training round', fontsize=16)
plt.title('Separate layers 29-node graph', fontsize=20)
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

# plt.show()
savefig("s29_mean_rounds.pdf")


rows = 6
cols = 6
fig, axes = plt.subplots(nrows=rows, ncols=cols, sharex=True, sharey='row', \
                         figsize=(10, 10))

episode_lists = []
reward_lists = []
for i in range(len(s29_att_learning_curves)):
    file_name = "learning_curves/" + s29_att_learning_curves[i]
    lines = get_lines(file_name)
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    reward_lists.append(rewards)

y_max = max([max(y) for y in reward_lists])

for i in range(len(reward_lists)):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    own_reward = s29_att_dev_payoffs[i]
    goal_reward = s29_att_eq_payoffs[i]
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c='blue')
    ax.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--')
    ax.axhline(y=goal_reward, lw=my_lw, c='red', linestyle='-.')
    ax.set_title("Round " + str(i), fontsize=16)
    ax.tick_params(
        which='both',
        bottom='off',
        left='off',
        right='off',
        top='off'
    )
    ax.grid(linewidth=1, linestyle=':')
    ax.spines['left'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.set_ylim((-10, y_max + 10))
    ax.tick_params(labelsize=14)

for i in range(len(s29_att_learning_curves), rows * cols):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    ax.remove()

savefig("s29_att_learning_curves.pdf")


rows = 6
cols = 6
fig, axes = plt.subplots(nrows=rows, ncols=cols, sharex=True, sharey='row', \
                         figsize=(10, 10))

episode_lists = []
reward_lists = []
for i in range(len(s29_def_learning_curves)):
    file_name = "learning_curves/" + s29_def_learning_curves[i]
    lines = get_lines(file_name)
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    reward_lists.append(rewards)

y_min = min([min(y) for y in reward_lists])
y_max = max([max(y) for y in reward_lists])

for i in range(len(reward_lists)):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    own_reward = s29_def_dev_payoffs[i]
    goal_reward = s29_def_eq_payoffs[i]
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c='blue')
    ax.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--')
    ax.axhline(y=goal_reward, lw=my_lw, c='red', linestyle='-.')
    ax.set_title("Round " + str(i), fontsize=16)
    ax.tick_params(
        which='both',
        bottom='off',
        left='off',
        right='off',
        top='off'
    )
    ax.grid(linewidth=1, linestyle=':')
    ax.spines['left'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.set_ylim((y_min - 10, y_max + 10))
    ax.tick_params(labelsize=14)

for i in range(len(s29_def_learning_curves), rows * cols):
    cur_x = i % cols
    cur_y = i // cols
    ax = axes[cur_y, cur_x]
    ax.remove()

savefig("s29_def_learning_curves.pdf")


fig, ax = plt.subplots()
fig.set_size_inches(6, 4)

episode_lists = []
reward_lists = []
cmap = plt.cm.get_cmap(my_cmap)
colors = []
for i in range(len(s29_def_learning_curves)):
    frac = i * 1.0 / (len(d30_def_learning_curves) - 1)
    rgba = cmap(frac)
    colors.append(rgba)

    file_name = "learning_curves/" + s29_def_learning_curves[i]
    lines = get_lines(file_name)

    goal_reward = s29_def_eq_payoffs[i]
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    rewards = [x - goal_reward for x in rewards]
    reward_lists.append(rewards)

for i in range(len(reward_lists)):
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c=colors[i])

ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

y_min = min([min(y) for y in reward_lists])
y_max = max([max(y) for y in reward_lists])

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
ax.grid(linewidth=1, linestyle=':')
ax.spines['left'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.spines['bottom'].set_visible(False)
ax.set_ylim((y_min - 5, y_max + 5))
ax.tick_params(labelsize=14)
plt.xlabel("Episode", fontsize=16)
plt.ylabel("Defender gain", fontsize=16)
plt.title("Separate layers 29-node graph", fontsize=20)
plt.tight_layout()

savefig("s29_def_learning_curves_merged.pdf")


fig, ax = plt.subplots()
fig.set_size_inches(6, 4)

episode_lists = []
reward_lists = []
cmap = plt.cm.get_cmap(my_cmap)
colors = []
for i in range(len(s29_att_learning_curves)):
    frac = i * 1.0 / (len(d30_att_learning_curves) - 1)
    rgba = cmap(frac)
    colors.append(rgba)

    file_name = "learning_curves/" + s29_att_learning_curves[i]
    lines = get_lines(file_name)

    goal_reward = s29_att_eq_payoffs[i]
    episodes = get_episodes_list(lines)
    episode_lists.append(episodes)
    rewards = get_rewards_list(lines)
    rewards = [x - goal_reward for x in rewards]
    reward_lists.append(rewards)

for i in range(len(reward_lists)):
    episodes = episode_lists[i]
    rewards = reward_lists[i]
    ax.plot(episodes, rewards, c=colors[i])

ax.axhline(y=0, lw=my_lw, c='red', linestyle='--')

y_min = min([min(y) for y in reward_lists])
y_max = max([max(y) for y in reward_lists])

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
ax.grid(linewidth=1, linestyle=':')
ax.spines['left'].set_visible(False)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.spines['bottom'].set_visible(False)
ax.set_ylim((y_min - 5, y_max + 5))
ax.tick_params(labelsize=14)
plt.xlabel("Episode", fontsize=16)
plt.ylabel("Attacker gain", fontsize=16)
plt.title("Separate layers 29-node graph", fontsize=20)
plt.tight_layout()

savefig("s29_att_learning_curves_merged.pdf")



regrets_file = "out_net_defPayoffs_sl29_randNoAndB_vs_epoch10.txt"

mean_payoffs, standard_errors = get_mean_payoffs_with_stderr(regrets_file, \
    s29_def_networks)


eq_mean_payoff = -280.96
eq_standard_error = 10.53
is_in_support_list = get_is_in_support_list(s29_def_networks, s29_def_round_fractions, 10)
out_file_name = "s29_def_mean_rewards_eq10.pdf"

plot_mean_rewards_and_errors(mean_payoffs, standard_errors, eq_mean_payoff, \
    eq_standard_error, is_in_support_list, out_file_name)


regrets_file = "out_net_attPayoffs_sl29_randNoAndB_vs_epoch10.txt"

mean_payoffs, standard_errors = get_mean_payoffs_with_stderr(regrets_file, \
    s29_att_networks)


eq_mean_payoff = 134.29
eq_standard_error = 4.84
is_in_support_list = get_is_in_support_list(s29_att_networks, s29_att_round_fractions, 10)
out_file_name = "s29_att_mean_rewards_eq10.pdf"

plot_mean_rewards_and_errors(mean_payoffs, standard_errors, eq_mean_payoff, \
    eq_standard_error, is_in_support_list, out_file_name)


plot_usage_heatmap(s29_def_round_fractions, \
    "Separate layers 29-node graph:\nDefender", "s29_def_choices.pdf")

plot_usage_heatmap(s29_att_round_fractions, \
    "Separate layers 29-node graph:\nAttacker", "s29_att_choices.pdf")
