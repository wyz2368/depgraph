import json
import time
import sys
from utility import get_game_file_name, get_new_payoff_file_name
from get_payoff_from_game_pas import get_json_data
from fpsb_annealing import get_def_payoff

def get_attacker_strats(game_data):
    '''
    Returns the list of strategies for the attacker role.
    '''
    for role in game_data["roles"]:
        if role["name"] == "attacker":
            return role["strategies"]
    raise ValueError("Attacker not found: " + str(game_data["roles"]))

def print_json(file_name, json_obj):
    '''
    Prints the given Json object to the given file name.
    '''
    with open(file_name, 'w') as my_file:
        json.dump(json_obj, my_file)

def get_result_dict(deviating_strat, attacker_strats):
    result = {}
    result[deviating_strat] = {}

    start_time = time.time()
    for att_strat in attacker_strats:
        def_payoff = get_def_payoff(deviating_strat, att_strat)
        att_payoff = get_def_payoff(att_strat, deviating_strat)
        result[deviating_strat][att_strat] = [def_payoff, att_payoff]
        print([def_payoff, att_payoff])
    duration = time.time() - start_time
    print("Minutes used for generating column: " + str(int(duration // 60)))
    return result

def gen_new_cols_fpsb(deviating_strat, run_name, test_round, cur_step):
    game_file_name = get_game_file_name(run_name, test_round, cur_step)
    game_json = get_json_data(game_file_name)
    attacker_strats = get_attacker_strats(game_json)

    result_dict = get_result_dict(deviating_strat, attacker_strats)
    out_file_name = get_new_payoff_file_name(run_name, test_round, cur_step)
    print_json(out_file_name, result_dict)

'''
example: python3 fpsb_gen_new_cols.py \
    MINCUT:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_stdev_0.0 dg1 0 1
'''
if __name__ == "__main__":
    if len(sys.argv) != 5:
        raise ValueError("Needs 4 arguments: deviating_strat, run_name, test_round, " + \
            "cur_step")
    DEVIATING_STRAT = sys.argv[1]
    RUN_NAME = sys.argv[2]
    TEST_ROUND = int(sys.argv[3])
    CUR_STEP = int(sys.argv[4])
    gen_new_cols_fpsb(DEVIATING_STRAT, RUN_NAME, TEST_ROUND, CUR_STEP)
