import sys
from plot_strat_rounds import get_all_eq_from_files, get_round

def get_total_gain(prev_round_dict, cur_round_dict, cur_net_name):
    result = 0.0
    for strat, weight in cur_round_dict.items():
        if strat != cur_net_name:
            if strat in prev_round_dict:
                result += max(0, weight - prev_round_dict[strat])
            else:
                result += weight
    return result

def get_network_round_to_name(round_dict):
    result = {}
    for strat in round_dict.keys():
        net_round = str(get_round(strat))
        if net_round not in result:
            result[net_round] = strat
    return result

def get_all_increases(round_dicts):
    result = []
    for i in range(1, len(round_dicts)):
        prev_round_dict = round_dicts[i - 1]
        cur_round_dict = round_dicts[i]
        cur_rounds_to_names = get_network_round_to_name(cur_round_dict)
        cur_net_name = cur_rounds_to_names.get(str(i), None)
        total_gain = get_total_gain(prev_round_dict, cur_round_dict, cur_net_name)
        result.append(total_gain)
    return result

def main(env_short_name_tsv):
    def_eqs, att_eqs = get_all_eq_from_files(env_short_name_tsv)
    def_increases = get_all_increases(def_eqs)
    att_increases = get_all_increases(att_eqs)

    print("def increases: " + str(def_increases))
    print("att increases: " + str(att_increases))

'''
example: python3 plot_increases.py s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: env_short_name_tsv")
    ENV_SHORT_NAME_TSV = sys.argv[1]
    main(ENV_SHORT_NAME_TSV)
