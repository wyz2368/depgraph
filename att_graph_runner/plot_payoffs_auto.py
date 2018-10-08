import sys
import re
import json
import os.path
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_lines(file_name):
    lines = None
    with open(file_name) as file:
        lines = file.readlines()
    lines = [line.strip() for line in lines]
    lines = [line for line in lines if line]
    return lines

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def get_eq_from_file(file_name):
    lines = get_lines(file_name)
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

def get_d30_early_eq_name(cur_epoch, is_defender):
    indexes = ["1", "2", "3", "4", "5", "6e", "7e", "8", "9", "10", "11", "12"]
    eqs_dir = "eqs2/"
    result = eqs_dir + "randNoAnd_B_epoch" + indexes[cur_epoch]
    if is_defender:
        result += "_def.tsv"
    else:
        result += "_att.tsv"
    return result

def get_old_eq_name(env_short_name_tsv, cur_epoch, is_defender):
    eqs_dir = "eqs2/"
    if env_short_name_tsv == "s29n1":
        cur_epoch -= 1
        if cur_epoch >= 14:
            cur_epoch += 1
        result = eqs_dir + "sl29_randNoAndB_epoch" + str(cur_epoch + 1)
        if is_defender:
            result += "_def.tsv"
        else:
            result += "_att.tsv"
        return result
    if env_short_name_tsv == "d30n1":
        if cur_epoch <= 11:
            return get_d30_early_eq_name(cur_epoch, is_defender)
        result = eqs_dir + "d30_epoch" + str(cur_epoch + 1)
        if is_defender:
            result += "_def.tsv"
        else:
            result += "_att.tsv"
        return result
    raise ValueError("Not an old run: " + env_short_name_tsv)

def get_eq_name(env_short_name_tsv, cur_epoch, is_defender):
    if env_short_name_tsv in ["s29n1", "d30n1"]:
        return get_old_eq_name(env_short_name_tsv, cur_epoch, is_defender)
    eqs_dir = "eqs2/"
    if is_defender:
        return eqs_dir + env_short_name_tsv + "_epoch" + \
            str(cur_epoch + 1) + "_def.tsv"
    return eqs_dir + env_short_name_tsv + "_epoch" + \
        str(cur_epoch + 1) + "_att.tsv"

def get_round_count(env_short_name_tsv):
    result = 0
    while True:
        file_name = get_eq_name(env_short_name_tsv, result, True)
        if not os.path.isfile(file_name):
            return result
        result += 1

def get_d30n1_game_name(cur_epoch):
    items = ["1", "2", "3", "4", "5_fixed_aug", "6e", "7e", "8c", "9_aug"]
    game_dir = "game_outputs2/"
    return game_dir + "game_3014_" + items[cur_epoch - 1] + ".json"

def get_game_name(game_number, env_short_name_payoffs, cur_epoch):
    game_dir = "game_outputs2/"
    if cur_epoch == 0:
        return game_dir + "game_" + str(game_number) + ".json"
    if env_short_name_payoffs == "d30n1":
        if cur_epoch <= 9:
            return get_d30n1_game_name(cur_epoch)
        return game_dir + "game_" + str(game_number) + "_" + str(cur_epoch) + ".json"
    elif env_short_name_payoffs == "s29n1":
        return game_dir + "game_" + str(game_number) + "_" + str(cur_epoch) + ".json"
    return game_dir + "game_" + str(game_number) + "_" + str(cur_epoch) + \
        "_" + env_short_name_payoffs + ".json"

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

def get_att_and_def_eq_payoffs(game_data, attacker_eq, defender_eq):
    att_result = 0
    def_result = 0
    for defender, def_weight in defender_eq.items():
        for attacker, att_weight in attacker_eq.items():
            att_payoff, def_payoff = get_att_and_def_payoffs( \
                game_data, attacker, defender)
            att_result += def_weight * att_weight * att_payoff
            def_result += def_weight * att_weight * def_payoff
    return att_result, def_result

def get_net_name(cur_epoch, env_short_name_payoffs, is_defender):
    # /net_strings2/<att or def>NetStrings_<env_short_name_payoffs>.txt
    net_dir = "net_strings2/"
    if is_defender:
        nets_file = net_dir + "defNetStrings"
    else:
        nets_file = net_dir + "attNetStrings"
    nets_file += "_" + env_short_name_payoffs + ".txt"
    lines = get_lines(nets_file)
    for net_name in lines:
        epoch_index = net_name.find('epoch')
        num_start_index = epoch_index + len("epoch")
        num_end_index = None
        retrain_pattern = re.compile("_r[0-9]+")
        if "_att.pkl" in net_name or retrain_pattern.search(net_name):
            # attacker network
            num_end_index = net_name.find("_", num_start_index)
        else:
            # defender network
            num_end_index = net_name.find(".pkl", num_start_index)
        if net_name in ["dg_dqmlp_rand30NoAnd_B_att_fixed.pkl", \
            "dg_rand_30n_noAnd_B_eq_2.pkl"]:
            my_epoch = 1
        elif net_name == "depgraph_dq_mlp_rand_epoch2_b.pkl":
            my_epoch = 2
        elif net_name == "dg_dqmlp_rand30NoAnd_B_epoch5_att_c.pkl":
            my_epoch = 5
        elif net_name in ["dg_dqmlp_rand30NoAnd_B_epoch6e_att.pkl", \
            "depgraph_dq_mlp_rand_epoch6e.pkl"]:
            my_epoch = 6
        elif net_name in ["dg_dqmlp_rand30NoAnd_B_epoch7e_att.pkl", \
            "depgraph_dq_mlp_rand_epoch7e.pkl"]:
            my_epoch = 7
        elif net_name in ["dg_dqmlp_rand30NoAnd_B_epoch8_att_fixed.pkl", \
            "depgraph_dq_mlp_rand_epoch8_fixed.pkl"]:
            my_epoch = 8
        elif net_name in ["depgraph_dq_mlp_rand_epoch9_fixed.pkl"]:
            my_epoch = 9
        elif net_name in ["depgraph_dq_mlp_rand_epoch10_fixed.pkl"]:
            my_epoch = 10
        elif net_name in ["depgraph_dq_mlp_rand_epoch11_fixed.pkl"]:
            my_epoch = 11
        else:
            my_epoch = int(net_name[num_start_index : num_end_index])
        if my_epoch == cur_epoch:
            return net_name
    return None

def get_payoff_net_vs_eq(cur_epoch, env_short_name_tsv, env_short_name_payoffs, \
    is_defender, game_number):
    net_name = get_net_name(cur_epoch + 1, env_short_name_payoffs, is_defender)
    print(net_name)
    if net_name is None:
        return None
    game_file_name = get_game_name(game_number, env_short_name_payoffs, \
        cur_epoch + 1)
    game_data = get_json_data(game_file_name)
    if is_defender:
        def_mixed_strat = {}
        def_mixed_strat[net_name] = 1.0
        att_mixed_strat_name = get_eq_name(env_short_name_tsv, cur_epoch, False)
        att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
        _, def_payoff = get_att_and_def_eq_payoffs(game_data, att_mixed_strat, \
            def_mixed_strat)
        return def_payoff
    att_mixed_strat = {}
    att_mixed_strat[net_name] = 1.0
    def_mixed_strat_name = get_eq_name(env_short_name_tsv, cur_epoch, True)
    def_mixed_strat = get_eq_from_file(def_mixed_strat_name)
    att_payoff, _ = get_att_and_def_eq_payoffs(game_data, att_mixed_strat, \
        def_mixed_strat)
    return att_payoff

def get_payoffs_eq_vs_eq(cur_epoch, env_short_name_tsv, env_short_name_payoffs, \
    game_number):
    # /game_outputs2/game_<game num>_<epoch num>_<env_short_name_payoffs>.json
    att_mixed_strat_name = get_eq_name(env_short_name_tsv, cur_epoch, False)
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
    def_mixed_strat_name = get_eq_name(env_short_name_tsv, cur_epoch, True)
    def_mixed_strat = get_eq_from_file(def_mixed_strat_name)
    game_file_name = get_game_name(game_number, env_short_name_payoffs, cur_epoch)
    game_data = get_json_data(game_file_name)
    att_payoff, def_payoff = \
        get_att_and_def_eq_payoffs(game_data, att_mixed_strat, def_mixed_strat)
    return att_payoff, def_payoff

def get_all_payoffs_eq_vs_eq(round_count, env_short_name_tsv, \
    env_short_name_payoffs, game_number):
    att_payoffs = []
    def_payoffs = []
    for cur_round in range(round_count):
        att_payoff, def_payoff = get_payoffs_eq_vs_eq(\
            cur_round, env_short_name_tsv, env_short_name_payoffs, game_number)
        att_payoffs.append(att_payoff)
        def_payoffs.append(def_payoff)
    return att_payoffs, def_payoffs

def get_all_net_payoffs_vs_eq(round_count, env_short_name_tsv, \
    env_short_name_payoffs, \
    game_number):
    att_payoffs = []
    def_payoffs = []
    for cur_round in range(round_count):
        att_payoff = get_payoff_net_vs_eq(cur_round, env_short_name_tsv, \
            env_short_name_payoffs, False, game_number)
        def_payoff = get_payoff_net_vs_eq(cur_round, env_short_name_tsv, \
            env_short_name_payoffs, True, game_number)
        att_payoffs.append(att_payoff)
        def_payoffs.append(def_payoff)
    return att_payoffs, def_payoffs

def get_all_payoffs(round_count, env_short_name_tsv, env_short_name_payoffs, \
    game_number):
    att_eq_payoffs, def_eq_payoffs = get_all_payoffs_eq_vs_eq(round_count, \
        env_short_name_tsv, env_short_name_payoffs, game_number)
    att_net_payoffs, def_net_payoffs = get_all_net_payoffs_vs_eq(round_count, \
        env_short_name_tsv, env_short_name_payoffs, game_number)
    return att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs

def get_cutoff_dev_payoffs(old_payoffs, dev_payoffs):
    result = []
    for i in range(len(dev_payoffs)):
        if dev_payoffs[i] is None:
            if i + 1 == len(dev_payoffs):
                pass
            else:
                result.append(old_payoffs[i])
        elif old_payoffs[i] > dev_payoffs[i]:
            result.append(old_payoffs[i])
        else:
            result.append(dev_payoffs[i])
    return result

def plot_payoffs(def_eq_payoffs, def_dev_payoffs, \
    att_eq_payoffs, att_dev_payoffs, env_short_name_payoffs):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    plt.plot(range(len(def_eq_payoffs)), def_eq_payoffs, lw=my_lw, label='Def. eq.')
    plt.plot(range(len(def_dev_payoffs)), def_dev_payoffs, linestyle='--', \
        lw=my_lw, label='Def. dev.')

    plt.plot(range(len(att_eq_payoffs)), att_eq_payoffs, lw=my_lw, label='Att. eq.')
    plt.plot(range(len(att_dev_payoffs)), att_dev_payoffs, linestyle='--', \
        lw=my_lw, label='Att. dev.')

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean Payoff', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 1
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
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
    savefig(env_short_name_payoffs + "_payoffs_vs_round.pdf")

def main(game_number, env_short_name_payoffs, env_short_name_tsv):
    round_count = get_round_count(env_short_name_tsv)
    att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
        get_all_payoffs(round_count, env_short_name_tsv, env_short_name_payoffs, \
            game_number)
    print(att_eq_payoffs)
    print(att_net_payoffs)
    print(def_eq_payoffs)
    print(def_net_payoffs)
    att_net_payoffs = get_cutoff_dev_payoffs(att_eq_payoffs, att_net_payoffs)
    def_net_payoffs = get_cutoff_dev_payoffs(def_eq_payoffs, def_net_payoffs)
    print(att_eq_payoffs)
    print(att_net_payoffs)
    print(def_eq_payoffs)
    print(def_net_payoffs)
    plot_payoffs(def_eq_payoffs, def_net_payoffs, \
        att_eq_payoffs, att_net_payoffs, env_short_name_payoffs)

'''
example: python3 plot_payoffs_auto.py 3013 s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_number, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
