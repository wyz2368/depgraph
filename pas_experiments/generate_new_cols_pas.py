import json
import time
import sys
from utility import get_game_file_name, get_new_payoff_file_name
from get_payoff_from_game_pas import get_json_data
from depgraph_connect import setup_default, get_mean_payoffs, close_process, close_gateway

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

def get_result_dict(deviating_strat, samples_new_column, attacker_strats):
    result = {}
    result[deviating_strat] = {}
    result["num_sims"] = samples_new_column

    start_time = time.time()
    my_process = setup_default()
    for att_strat in attacker_strats:
        att_mixed_strat = {att_strat: 1.0}
        def_payoff, att_payoff, _, _ = get_mean_payoffs(deviating_strat, \
            att_mixed_strat, samples_new_column)
        result[deviating_strat][att_strat] = [def_payoff, att_payoff]
        print([def_payoff, att_payoff])
    close_gateway()
    close_process(my_process)
    duration = time.time() - start_time
    print("Minutes taken: " + str(duration // 60))
    return result

def gen_new_cols(deviating_strat, run_name, test_round, cur_step, samples_new_column):
    game_file_name = get_game_file_name(run_name, test_round, cur_step)
    game_json = get_json_data(game_file_name)
    attacker_strats = get_attacker_strats(game_json)

    result_dict = get_result_dict(deviating_strat, samples_new_column, attacker_strats)
    out_file_name = get_new_payoff_file_name(run_name, test_round, cur_step)
    print_json(out_file_name, result_dict)

'''
example: python3 generate_new_cols_pas.py \
    MINCUT:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_stdev_0.0 dg1 0 1 5
'''
if __name__ == "__main__":
    if len(sys.argv) != 6:
        raise ValueError("Needs 5 arguments: deviating_strat, run_name, test_round, " + \
            "cur_step, samples_new_column")
    DEVIATING_STRAT = sys.argv[1]
    RUN_NAME = sys.argv[2]
    TEST_ROUND = int(sys.argv[3])
    CUR_STEP = int(sys.argv[4])
    SAMPLES_NEW_COLUMN = int(sys.argv[5])
    gen_new_cols(DEVIATING_STRAT, RUN_NAME, TEST_ROUND, CUR_STEP, SAMPLES_NEW_COLUMN)
