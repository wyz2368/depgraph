import sys
import os.path
from gambit_analyze import call_and_wait, call_and_wait_with_timeout
from get_both_payoffs_from_game import get_json_data
from create_tsv_files import get_defender_lines, get_attacker_lines, \
    get_rounded_strategy_lines
from eval_mixed_strats import is_network

def get_unioned_gambit_input_name(unioned_game_file):
    result = unioned_game_file[:-5] + "_gambit.nfg"
    return result

def get_unioned_gambit_result_name(unioned_game_file):
    result = unioned_game_file[:-5] + "_lcp.txt"
    return result

def get_unioned_decoded_result_name(unioned_game_file):
    result = unioned_game_file[:-5] + "_lcp_decode.txt"
    return result

def make_union_gambit_file(unioned_game_file):
    gambit_input_name = get_unioned_gambit_input_name(unioned_game_file)
    if os.path.isfile(gambit_input_name):
        print("Skipping: " + gambit_input_name + " already exists.")
        return
    if not os.path.isfile(unioned_game_file):
        raise ValueError(unioned_game_file + " missing.")
    command_str = "ga conv -i " + unioned_game_file + " gambit > " + gambit_input_name
    call_and_wait(command_str)

def gambit_analyze_unioned(unioned_game_file):
    gambit_input_name = get_unioned_gambit_input_name(unioned_game_file)
    gambit_result_name = get_unioned_gambit_result_name(unioned_game_file)
    if os.path.isfile(gambit_result_name):
        print("Skipping: " + gambit_result_name + " already exists.")
        return
    if not os.path.isfile(gambit_input_name):
        raise ValueError(gambit_input_name + " missing.")
    command_str = "gambit-lcp < " + gambit_input_name + " -d 8 > " + gambit_result_name
    call_and_wait_with_timeout(command_str)

def call_decode_gambit_solution_unioned(unioned_game_file):
    gambit_input_name = get_unioned_gambit_input_name(unioned_game_file)
    gambit_result_name = get_unioned_gambit_result_name(unioned_game_file)
    gambit_decoded_name = get_unioned_decoded_result_name(unioned_game_file)
    if os.path.isfile(gambit_decoded_name):
        print("Skipping: " + gambit_decoded_name + " already exists.")
        return
    if not os.path.isfile(gambit_input_name):
        raise ValueError(gambit_input_name + " missing.")
    if not os.path.isfile(gambit_result_name):
        raise ValueError(gambit_result_name + " missing.")
    command_str = "python3 decode_gambit_solution.py " + gambit_input_name + " " + \
        gambit_result_name + " > " + gambit_decoded_name
    call_and_wait(command_str)

def get_defender_mixed_strat(decoded_result_name):
    lines = get_defender_lines(decoded_result_name)
    lines = get_rounded_strategy_lines(lines)
    result = {}
    for line in lines:
        parts = line.split("\t")
        result[parts[0]] = float(parts[1])
    return result

def get_attacker_mixed_strat(decoded_result_name):
    lines = get_attacker_lines(decoded_result_name)
    lines = get_rounded_strategy_lines(lines)
    result = {}
    for line in lines:
        parts = line.split("\t")
        result[parts[0]] = float(parts[1])
    return result

def find_old_game_file_name(strat, unioned_game_data):
    if not is_network(strat):
        return None
    for old_game_file_name, cur_strats in unioned_game_data["network_source"].items():
        if strat in cur_strats:
            return old_game_file_name
    raise ValueError("Strategy not found: " + strat)

def get_run_fractions(mixed_strat, unioned_game_data):
    result = {}
    for strat, weight in mixed_strat.items():
        old_game_file_name = find_old_game_file_name(strat, unioned_game_data)
        if old_game_file_name is not None:
            if old_game_file_name in result:
                result[old_game_file_name] += weight
            else:
                result[old_game_file_name] = weight
    return result

def print_results(unioned_game_data, defender_mixed_strat, attacker_mixed_strat):
    defender_run_to_fraction = get_run_fractions(defender_mixed_strat, unioned_game_data)
    attacker_run_to_fraction = get_run_fractions(attacker_mixed_strat, unioned_game_data)

    print("Defender run to eq fraction:")
    print(defender_run_to_fraction)

    print("Attacker run to eq fraction:")
    print(attacker_run_to_fraction)

def main(unioned_game_file):
    make_union_gambit_file(unioned_game_file)
    gambit_analyze_unioned(unioned_game_file)
    call_decode_gambit_solution_unioned(unioned_game_file)

    unioned_game_data = get_json_data(unioned_game_file)
    decoded_result_name = get_unioned_decoded_result_name(unioned_game_file)
    defender_mixed_strat = get_defender_mixed_strat(decoded_result_name)
    attacker_mixed_strat = get_attacker_mixed_strat(decoded_result_name)
    print_results(unioned_game_data, defender_mixed_strat, attacker_mixed_strat)

'''
example: python3 runs_analyze.py game_comb_d30cd1_d30n1.json
'''
if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: unioned_game_file")
    UNIONED_GAME_FILE = sys.argv[1]
    main(UNIONED_GAME_FILE)
