import sys
from plot_payoffs_auto import get_eq_name
from get_both_payoffs_from_game import get_eq_from_file, get_json_data, \
    get_att_and_def_eq_payoffs
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
    cur_index = 0
    result = []
    for cur_net in range(1, net_count):
        if str(cur_net) in all_nets[cur_index]:
            result.append(all_nets[cur_index])
            cur_index += 1
        else:
            result.append(None)
    return result

def get_run_to_def_regrets(game_data, attacker_mixed_strat, def_eq_payoff):
    def_net_names = get_network_names(game_data, run_name, True)
    def_payoffs = [get_def_payoff_vs_eq(game_data, attacker_mixed_strat, defender) for \
        defender in def_net_names]
    def_regrets = [max(def_eq_payoff - cur_def_payoff, 0) for cur_def_payoff in \
        def_payoffs]
    run_to_def_regrets[run_name] = def_regrets

def get_run_to_att_regrets(game_data, defender_mixed_strat, att_eq_payoff):
    att_net_names = get_network_names(game_data, run_name, False)
    att_payoffs = [get_att_payoff_vs_eq(game_data, defender_mixed_strat, attacker) for \
        attacker in att_net_names]
    att_regrets = [max(att_eq_payoff - cur_att_payoff, 0) for cur_att_payoff in \
        att_payoffs]
    run_to_att_regrets[run_name] = att_regrets

def get_all_def_eq_payoffs(def_eqs, final_att_eq, game_data):
    result = []
    for def_eq in def_eqs:
        _, def_payoff = get_att_and_def_eq_payoffs(game_data, final_att_eq, def_eq)
        result.append(def_payoff)
    return result

def get_all_att_eq_payoffs(att_eqs, final_def_eq, game_data):
    result = []
    for att_eq in def_eqs:
        att_payoff, _ = get_att_and_def_eq_payoffs(game_data, att_eq, final_def_eq)
        result.append(att_payoff)
    return result

def get_final_payoffs(final_def_eq, final_att_eq, game_data):
    att_payoff, def_payoff = get_att_and_def_eq_payoffs(game_data, final_att_eq, \
        final_def_eq)

def get_all_def_eq_regrets(def_eqs, final_att_eq, game_data):
    _, final_def_payoff = get_final_payoffs(def_eqs[-1], final_att_eq, game_data)
    def_eq_payoffs = get_all_def_eq_payoffs(def_eqs, final_att_eq, game_data)
    result = [x - final_def_payoff for x in def_eq_payoffs]
    return result

def get_all_att_eq_regrets(att_eqs, final_def_eq, game_data):
    final_att_payoff, _ = get_final_payoffs(final_def_eq, att_eqs[-1], game_data)
    att_eq_payoffs = get_all_att_eq_payoffs(att_eqs, final_def_eq, game_data)
    result = [x - final_att_payoff for x in att_eq_payoffs]
    return result

def main(game_file, env_short_name_payoffs, env_short_name_tsv):
    game_data = get_json_data(game_file)
    def_eqs = get_all_eqs(env_short_name_tsv, True)
    att_eqs = get_all_eqs(env_short_name_tsv, False)

    net_count = len(def_eqs) - 1
    def_nets = get_networks_by_round(game_data, True, net_count)
    att_nets = get_networks_by_round(game_data, False, net_count)
    check_net_lists(def_nets, att_nets)

    def_eq_regrets = get_all_def_eq_regrets(def_eqs, att_eqs[-1], game_data)
    att_eq_regrets = get_all_att_eq_regrets(att_eqs, def_eqs[-1], game_data)

'''
example: python3 plot_regret_anytime.py game_3014_20_d30cd1.json d30cd1 d30cd1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_file, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_FILE = sys.argv[1]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_FILE, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
