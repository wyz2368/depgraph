import random
import json
import os
from fpsb_annealing import get_def_payoff

def get_strats(strat_count):
    fmt = "{0:.6f}"
    result = sorted([fmt.format(random.random()) for _ in range(strat_count)])
    while len(result) != len(set(result)):
        result = sorted([fmt.format(random.random()) for _ in range(strat_count)])
    return result

def get_def_to_att_to_payoffs(strats):
    result = {}
    for def_strat in strats:
        result[def_strat] = {}
        for att_strat in strats:
            def_payoff = get_def_payoff(def_strat, att_strat)
            att_payoff = get_def_payoff(att_strat, def_strat)
            result[def_strat][att_strat] = (def_payoff, att_payoff)
    return result

def print_json(my_dict):
    with open('game_fpsb.json', 'w') as outfile:
        json.dump(my_dict, outfile)

    try:
        os.remove("game_fpsb_gambit.nfg")
    except OSError:
        pass
    try:
        os.remove("gambit_fpsb_result_lcp_decode.txt")
    except OSError:
        pass

def get_dict_result(strats):
    def_to_att_to_payoffs = get_def_to_att_to_payoffs(strats)

    result = {}
    result["roles"] = []
    result["roles"].append({"count": 1, "name": "attacker", "strategies": strats})
    result["roles"].append({"count": 1, "name": "defender", "strategies": strats})

    result["profiles"] = []
    cur_id = 1
    for def_strat in strats:
        for att_strat in strats:
            cur_profile = {"observations_count": 1, "id": cur_id}
            symmetry_groups = []
            def_payoff, att_payoff = def_to_att_to_payoffs[def_strat][att_strat]
            symmetry_groups.append({"role": "attacker", "strategy": att_strat, "count": 1, \
                "id": cur_id + 1, "payoff": att_payoff})
            symmetry_groups.append({"role": "defender", "strategy": def_strat, "count": 1, \
                "id": cur_id + 2, "payoff": def_payoff})
            cur_profile["symmetry_groups"] = symmetry_groups
            result["profiles"].append(cur_profile)
            cur_id += 3
    return result

def make_game(strat_count):
    strats = get_strats(strat_count)
    dict_result = get_dict_result(strats)
    print_json(dict_result)

if __name__ == "__main__":
    STRAT_COUNT = 10
    make_game(STRAT_COUNT)