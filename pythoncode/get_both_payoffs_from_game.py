import sys
import json
import os.path

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

def get_game_file_name(game_number, new_epoch, env_short_name):
    # return the game file name from the previous epoch.
    if new_epoch is None or new_epoch == 1:
        return "../game_" + str(game_number) + ".json"
    return "../game_" + str(game_number) + "_" + str(new_epoch - 1) + "_" + \
        env_short_name + ".json"

def print_to_file(best_payoffs, out_file):
    '''
    Write the object to file as Json.
    '''
    with open(out_file, 'w') as my_file:
        json.dump(best_payoffs, my_file)

def main(game_number, env_short_name, new_epoch):
    def_out_file_name = "out_defPayoffs_" + env_short_name + "_randNoAndB_epoch" + \
        str(new_epoch - 1) + ".txt"
    att_out_file_name = "out_attPayoffs_" + env_short_name + "_randNoAndB_epoch" + \
        str(new_epoch - 1) + ".txt"
    if os.path.isfile(def_out_file_name) and os.path.isfile(att_out_file_name):
        print("Skipping: " + def_out_file_name + "and " + \
            att_out_file_name + " already exist.")
        return

    att_mixed_strat_name = env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
        "_att.tsv"
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
    def_mixed_strat_name = env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
        "_def.tsv"
    def_mixed_strat = get_eq_from_file(def_mixed_strat_name)

    game_file_name = get_game_file_name(game_number, new_epoch, env_short_name)
    if not os.path.isfile(game_file_name):
        raise ValueError(game_file_name + " missing.")
    game_data = get_json_data(game_file_name)

    att_payoff, def_payoff = \
        get_att_and_def_eq_payoffs(game_data, att_mixed_strat, def_mixed_strat)

    att_result = {}
    att_result["mean_reward"] = att_payoff
    print_to_file(att_result, att_out_file_name)

    def_result = {}
    def_result["mean_reward"] = def_payoff
    print_to_file(def_result, def_out_file_name)

if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_number, env_short_name, new_epoch")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    main(GAME_NUMBER, ENV_SHORT_NAME, NEW_EPOCH)
