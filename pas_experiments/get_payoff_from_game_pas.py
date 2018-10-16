import sys
import json
import os.path
from utility import get_game_file_name, get_tsv_strat_name

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
    eqs_dir = ""
    lines = get_lines(eqs_dir + file_name)
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
            att_payoff, def_payoff = get_att_and_def_payoffs(game_data, attacker, defender)
            att_result += def_weight * att_weight * att_payoff
            def_result += def_weight * att_weight * def_payoff
    return att_result, def_result

def get_def_payoff_eq(run_name, test_round, cur_step):
    game_file_name = get_game_file_name(run_name, test_round, cur_step)
    if not os.path.isfile(game_file_name):
        raise ValueError(game_file_name + " missing.")
    game_data = get_json_data(game_file_name)

    att_mixed_strat_name = get_tsv_strat_name(run_name, test_round, cur_step, False)
    if not os.path.isfile(att_mixed_strat_name):
        raise ValueError(att_mixed_strat_name + " missing.")
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
    def_mixed_strat_name = get_tsv_strat_name(run_name, test_round, cur_step, True)
    if not os.path.isfile(def_mixed_strat_name):
        raise ValueError(def_mixed_strat_name + " missing.")
    def_mixed_strat = get_eq_from_file(def_mixed_strat_name)

    _, def_payoff = \
        get_att_and_def_eq_payoffs(game_data, att_mixed_strat, def_mixed_strat)
    return def_payoff

# example: python3 get_payoff_from_game_pas.py dg1 0 1
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: run_name, test_round, cur_step")
    RUN_NAME = sys.argv[1]
    TEST_ROUND = int(sys.argv[2])
    CUR_STEP = int(sys.argv[3])
    print(get_def_payoff_eq(RUN_NAME, RUN_NAME, CUR_STEP))
