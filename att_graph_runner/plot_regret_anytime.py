import sys
import os
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_payoffs_auto import get_eq_name
from get_both_payoffs_from_game import get_eq_from_file, get_json_data, \
    get_att_and_def_eq_payoffs, get_att_and_def_payoffs
from union_games import get_attacker_networks, get_defender_networks

def get_all_eqs(env_short_name_tsv, is_defender):
    result = []
    while True:
        file_name = get_eq_name(env_short_name_tsv, len(result), is_defender)
        if not os.path.isfile(file_name):
            return result
        cur_eq = get_eq_from_file(file_name)
        result.append(cur_eq)
    return result

def check_net_lists(def_nets, att_nets):
    if len(def_nets) != len(att_nets):
        raise ValueError("Wrong length: " + str(len(def_nets)) + ", " + str(len(att_nets)))
    for i in range(len(def_nets)):
        if def_nets[i] is None and att_nets[i] is None:
            raise ValueError("Both cannot be None: " + str(i))

def get_networks_by_round(game_data, is_defender, net_count):
    if is_defender:
        all_nets = get_defender_networks(game_data)
    else:
        all_nets = get_attacker_networks(game_data)
    print(all_nets)
    cur_index = 0
    result = []
    for cur_net in range(1, net_count):
        if cur_index < len(all_nets) and \
            ((str(cur_net) in all_nets[cur_index]) or \
                all_nets[cur_index] in ["dg_rand_30n_noAnd_B_eq_2.pkl", \
                "dg_dqmlp_rand30NoAnd_B_att_fixed.pkl"]):
            result.append(all_nets[cur_index])
            cur_index += 1
        else:
            result.append(None)
    print(result)
    return result

def get_all_def_eq_payoffs(def_eqs, final_att_eq, game_data):
    result = []
    for def_eq in def_eqs:
        _, def_payoff = get_att_and_def_eq_payoffs(game_data, final_att_eq, def_eq)
        result.append(def_payoff)
    return result

def get_all_att_eq_payoffs(att_eqs, final_def_eq, game_data):
    result = []
    for att_eq in att_eqs:
        att_payoff, _ = get_att_and_def_eq_payoffs(game_data, att_eq, final_def_eq)
        result.append(att_payoff)
    return result

def get_final_payoffs(final_def_eq, final_att_eq, game_data):
    att_payoff, def_payoff = get_att_and_def_eq_payoffs(game_data, final_att_eq, \
        final_def_eq)
    return att_payoff, def_payoff

def get_all_def_eq_regrets(def_eqs, final_att_eq, game_data):
    _, final_def_payoff = get_final_payoffs(def_eqs[-1], final_att_eq, game_data)
    def_eq_payoffs = get_all_def_eq_payoffs(def_eqs, final_att_eq, game_data)
    result = [round(final_def_payoff - x, 2) for x in def_eq_payoffs]
    return result

def get_all_att_eq_regrets(att_eqs, final_def_eq, game_data):
    final_att_payoff, _ = get_final_payoffs(final_def_eq, att_eqs[-1], game_data)
    att_eq_payoffs = get_all_att_eq_payoffs(att_eqs, final_def_eq, game_data)
    result = [round(final_att_payoff - x, 2) for x in att_eq_payoffs]
    return result

def get_def_net_payoff(game_data, attacker_eq, def_net):
    def_result = 0
    for attacker, att_weight in attacker_eq.items():
        _, def_payoff = get_att_and_def_payoffs(game_data, attacker, def_net)
        def_result += att_weight * def_payoff
    return def_result

def get_att_net_payoff(game_data, defender_eq, att_net):
    att_result = 0
    for defender, def_weight in defender_eq.items():
        att_payoff, _ = get_att_and_def_payoffs(game_data, att_net, defender)
        att_result += def_weight * att_payoff
    return att_result

def get_all_def_net_payoffs(def_nets, final_att_eq, game_data):
    result = []
    for def_net in def_nets:
        if def_net is None:
            result.append(None)
        else:
            def_payoff = get_def_net_payoff(game_data, final_att_eq, def_net)
            result.append(def_payoff)
    return result

def get_all_att_net_payoffs(att_nets, final_def_eq, game_data):
    result = []
    for att_net in att_nets:
        if att_net is None:
            result.append(None)
        else:
            att_payoff = get_att_net_payoff(game_data, final_def_eq, att_net)
            result.append(att_payoff)
    return result

def get_all_def_net_regrets(def_nets, final_def_eq, final_att_eq, game_data):
    _, final_def_payoff = get_final_payoffs(final_def_eq, final_att_eq, game_data)
    def_net_payoffs = get_all_def_net_payoffs(def_nets, final_att_eq, game_data)
    result = []
    for cur_payoff in def_net_payoffs:
        if cur_payoff is None:
            result.append(None)
        else:
            result.append(round(final_def_payoff - cur_payoff, 2))
    return result

def get_all_att_net_regrets(att_nets, final_def_eq, final_att_eq, game_data):
    final_att_payoff, _ = get_final_payoffs(final_def_eq, final_att_eq, game_data)
    att_net_payoffs = get_all_att_net_payoffs(att_nets, final_def_eq, game_data)
    result = []
    for cur_payoff in att_net_payoffs:
        if cur_payoff is None:
            result.append(None)
        else:
            result.append(round(final_att_payoff - cur_payoff, 2))
    return result

def plot_results(def_regrets, att_regrets, env_short_name_payoffs, is_network):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    my_len = max(len(def_regrets), len(att_regrets))
    if is_network:
        plt.scatter(range(my_len), def_regrets, label='Defender')
        plt.scatter(range(my_len), att_regrets, label='Attacker', \
            marker='x')
    else:
        plt.plot(range(my_len), def_regrets, lw=my_lw, label='Defender')
        plt.plot(range(my_len), att_regrets, lw=my_lw, label='Attacker', \
            linestyle='--')

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Regret', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 2
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
    save_name = env_short_name_payoffs + "_eq_regrets.pdf"
    if is_network:
        save_name = env_short_name_payoffs + "_net_regrets.pdf"
    savefig(save_name)

def main(game_file, env_short_name_payoffs, env_short_name_tsv):
    game_data = get_json_data(game_file)
    def_eqs = get_all_eqs(env_short_name_tsv, True)
    att_eqs = get_all_eqs(env_short_name_tsv, False)

    def_eq_regrets = get_all_def_eq_regrets(def_eqs, att_eqs[-1], game_data)
    att_eq_regrets = get_all_att_eq_regrets(att_eqs, def_eqs[-1], game_data)
    print("Defender eq. regrets:")
    print(def_eq_regrets)
    print("Attacker eq. regrets:")
    print(att_eq_regrets)

    net_count = len(def_eqs) - 1
    if env_short_name_tsv == "s29n1":
        net_count -= 1
    def_nets = get_networks_by_round(game_data, True, net_count)
    att_nets = get_networks_by_round(game_data, False, net_count)
    check_net_lists(def_nets, att_nets)

    def_net_regrets = get_all_def_net_regrets(def_nets, def_eqs[-1], att_eqs[-1], game_data)
    att_net_regrets = get_all_att_net_regrets(att_nets, def_eqs[-1], att_eqs[-1], game_data)
    print("Defender net. regrets:")
    print(def_net_regrets)
    print("Attacker net. regrets:")
    print(att_net_regrets)

    plot_results(def_eq_regrets, att_eq_regrets, env_short_name_payoffs, False)
    plot_results(def_net_regrets, att_net_regrets, env_short_name_payoffs, True)

'''
example: python3 plot_regret_anytime.py game_outputs2/game_3014_20_d30cd1.json \
    d30cd1 d30cd1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_file, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_FILE = sys.argv[1]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_FILE, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
