import sys
import os.path
from plot_payoffs_auto import get_lines, get_round_count, get_all_payoffs
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
from pylab import savefig

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

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

def get_learning_curve_names_d30n1(is_defender):
    learning_dir = "learning_curves2/"
    def_names = ["tdj_mlp_defVMixed_randNoAnd_B.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_2.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch3.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch4_fixed.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch5.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch6e.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch7e.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch8_fixed.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch9.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch10.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch11.txt", \
        "tdj_mlp_defVMixed_randNoAnd_B_epoch12.txt"]
    for i in range(13, 25):
        def_names.append("defVMixed_d30_epoch" + str(i) + ".txt")
    def_names = [learning_dir + x for x in def_names]

    att_names = ["tdj_dg_rand30NoAnd_B_att_fixed.txt", \
        "tdj_dq_randNoAnd_B_epoch2_vsDef.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch3.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch4.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch5_c2.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch6e.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch7e.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch8_fixed.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch9.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch10.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch11.txt", \
        "tdj_mlp_attVMixed_randNoAnd_B_epoch12.txt"]
    for i in range(13, 25):
        att_names.append("attVMixed_d30_epoch" + str(i) + ".txt")
    att_names = [learning_dir + x for x in att_names]

    if is_defender:
        return def_names
    return att_names

def get_learning_curve_names(env_short_name_payoffs, is_defender):
    if env_short_name_payoffs == "d30n1":
        return get_learning_curve_names_d30n1(is_defender)
    cur_epoch = 1
    result = []
    learning_dir = "learning_curves2/"
    while True:
        cur_name = learning_dir + "defVMixed_"
        if not is_defender:
            cur_name = learning_dir + "attVMixed_"
        if env_short_name_payoffs == "s29n1":
            if cur_epoch <= 16:
                cur_name += "sl29_randNoAndB_epoch" + str(cur_epoch) + ".txt"
            else:
                cur_name += "sl29_epoch" + str(cur_epoch) + ".txt"
        else:
            cur_name += env_short_name_payoffs + "_epoch" + str(cur_epoch) + ".txt"
        if not os.path.isfile(cur_name):
            break
        result.append(cur_name)
        cur_epoch += 1
    return result

def get_learning_curves(env_short_name_payoffs, is_defender):
    file_names = get_learning_curve_names(env_short_name_payoffs, is_defender)
    result = []
    for file_name in file_names:
        lines = get_lines(file_name)
        episodes_list = get_episodes_list(lines)
        rewards_list = get_rewards_list(lines)
        result.append([episodes_list, rewards_list])
    return result

def plot_curves(learning_curves, is_defender, env_short_name_payoffs, \
    eq_vs_eq_payoffs, net_vs_eq_payoffs):
    num_curves = len(net_vs_eq_payoffs)
    my_size = 1
    while my_size ** 2 < num_curves:
        my_size += 1
    rows = my_size
    cols = my_size
    _, axes = plt.subplots(nrows=rows, ncols=cols, sharex=True, sharey='row', \
                             figsize=(10, 10))

    episode_lists = [x[0] for x in learning_curves]
    reward_lists = [x[1] for x in learning_curves]
    y_max = max([max(y) for y in reward_lists if y])
    y_max = max(y_max, max([x for x in net_vs_eq_payoffs if x]))
    y_max = max(y_max, max([x for x in eq_vs_eq_payoffs if x]))

    y_min = min([min(y) for y in reward_lists if y])
    y_min = min(y_min, min([x for x in net_vs_eq_payoffs if x]))
    y_min = min(y_min, min([x for x in eq_vs_eq_payoffs if x]))

    my_lw = 2
    my_range = min([len(net_vs_eq_payoffs), len(eq_vs_eq_payoffs), len(episode_lists), \
        len(reward_lists)])
    for i in range(my_range):
        cur_x = i % cols
        cur_y = i // cols
        ax = axes[cur_y, cur_x]
        own_reward = net_vs_eq_payoffs[i]
        goal_reward = eq_vs_eq_payoffs[i]
        episodes = episode_lists[i]
        rewards = reward_lists[i]
        ax.plot(episodes, rewards, c='blue')
        if own_reward:
            ax.axhline(y=own_reward, lw=my_lw, c='blue', linestyle='--')
        if goal_reward:
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
        if is_defender:
            ax.set_ylim(y_min - 10, y_max + 10)
        else:
            ax.set_ylim((-10, y_max + 10))
        ax.tick_params(labelsize=12)

    for i in range(len(net_vs_eq_payoffs), rows * cols):
        cur_x = i % cols
        cur_y = i // cols
        ax = axes[cur_y, cur_x]
        ax.remove()

    save_name = "learning_curves_" + env_short_name_payoffs
    if is_defender:
        save_name += "_def.pdf"
    else:
        save_name += "_att.pdf"
    savefig(save_name)

def main(game_number, env_short_name_payoffs, env_short_name_tsv):
    def_learning_curves = get_learning_curves(env_short_name_payoffs, True)
    att_learning_curves = get_learning_curves(env_short_name_payoffs, False)

    round_count = get_round_count(env_short_name_tsv)
    att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
        get_all_payoffs(round_count, env_short_name_tsv, env_short_name_payoffs, \
            game_number)
    #att_net_payoffs = [x for x in att_net_payoffs if x]
    #def_net_payoffs = [x for x in def_net_payoffs if x]
    print(att_eq_payoffs)
    print(att_net_payoffs)
    print(def_eq_payoffs)
    print(def_net_payoffs)
    print(len(att_eq_payoffs))
    print(len(att_net_payoffs))
    print(len(def_eq_payoffs))
    print(len(def_net_payoffs))

    plot_curves(def_learning_curves, True, env_short_name_payoffs, \
        def_eq_payoffs, def_net_payoffs)
    plot_curves(att_learning_curves, False, env_short_name_payoffs, \
        att_eq_payoffs, att_net_payoffs)

'''
example: python3 plot_learning_curves.py 3013 s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_number, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
