import sys
import re
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
from pylab import savefig
from plot_payoffs_auto import get_round_count, get_eq_from_file, get_eq_name

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_mean_network_round(network_round_list):
    result = 0.0
    for cur_round, frequency in network_round_list.items():
        result += int(cur_round) * frequency
    return result

def get_round(net_name):
    if ".pkl" not in net_name:
        return 0

    if "epoch1.pkl" in net_name or "epoch1_att.pkl" in net_name or "att_fixed.pkl" in \
        net_name or net_name == "dg_rand_30n_noAnd_B_eq_2.pkl":
        # first round is special case: don't add _e1
        return 1

    if "epoch2_b" in net_name:
        return 2
    if "5_att_c" in net_name:
        return 5
    if "6e" in net_name:
        return 6
    if "7e" in net_name:
        return 7
    if "8_fixed" in net_name:
        return 8
    if "9_fixed" in net_name:
        return 9
    if "10_fixed" in net_name:
        return 10
    if "11_fixed" in net_name:
        return 11

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
    return int(net_name[num_start_index : num_end_index])

def get_network_round_list(round_dict):
    result = {}
    for strat, weight in round_dict.items():
        net_round = str(get_round(strat))
        if net_round not in result:
            result[net_round] = weight
        else:
            result[net_round] += weight
    return result

def get_all_mean_rounds(round_dicts):
    result = []
    for round_dict in round_dicts:
        round_list = get_network_round_list(round_dict)
        mean_round = get_mean_network_round(round_list)
        result.append(mean_round)
    return result

def get_all_eq_from_files(env_short_name_tsv):
    round_count = get_round_count(env_short_name_tsv)
    def_eqs = []
    att_eqs = []
    for i in range(round_count):
        def_mixed_strat_name = get_eq_name(env_short_name_tsv, i, True)
        att_mixed_strat_name = get_eq_name(env_short_name_tsv, i, False)
        def_eq = get_eq_from_file(def_mixed_strat_name)
        att_eq = get_eq_from_file(att_mixed_strat_name)
        def_eqs.append(def_eq)
        att_eqs.append(att_eq)
    return (def_eqs, att_eqs)

def plot_mean_rounds(mean_def_rounds, mean_att_rounds, env_short_name_payoffs):
    fig, ax = plt.subplots()
    fig.set_size_inches(7, 4)

    my_lw = 2
    plt.plot(range(len(mean_def_rounds)), mean_def_rounds, lw=my_lw, label="Defender")
    plt.plot(range(len(mean_att_rounds)), mean_att_rounds, linestyle=':', lw=my_lw, \
        label='Attacker')

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Mean Round in Eq. Strategy', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    my_title = "Random 30-node graph"
    if "s29" in env_short_name_payoffs:
        my_title = "Separate layers 29-node graph"
    plt.title(my_title, fontsize=20)
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

    savefig(env_short_name_payoffs + "_strat_rounds.pdf")

def main(env_short_name_payoffs, env_short_name_tsv):
    def_eqs, att_eqs = get_all_eq_from_files(env_short_name_tsv)
    mean_def_rounds = get_all_mean_rounds(def_eqs)
    mean_att_rounds = get_all_mean_rounds(att_eqs)
    plot_mean_rounds(mean_def_rounds, mean_att_rounds, env_short_name_payoffs)

'''
example: python3 plot_mean_rounds.py s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: env_short_name_payoffs, " + \
            "env_short_name_tsv")
    ENV_SHORT_NAME_PAYOFFS = sys.argv[1]
    ENV_SHORT_NAME_TSV = sys.argv[2]
    main(ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
