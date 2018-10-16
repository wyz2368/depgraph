import subprocess
import time

def print_to_file(lines, file_name):
    with open(file_name, 'w') as file:
        for line in lines:
            file.write(line + "\n")

def call_and_wait(command_str):
    print("Will run:\n" + command_str)
    my_process = subprocess.Popen(command_str, shell=True)
    my_process.wait()
    sleep_sec = 5
    time.sleep(sleep_sec)
    my_process.kill()

def call_and_wait_with_timeout(command_str):
    print("Will run:\n" + command_str)
    my_process = subprocess.Popen(command_str, shell=True)
    timeout_seconds = 300
    try:
        my_process.wait(timeout=timeout_seconds)
    except subprocess.TimeoutExpired:
        print("Process ran more seconds than: " + str(timeout_seconds))
    sleep_sec = 5
    time.sleep(sleep_sec)
    my_process.kill()

def get_game_file_name(run_name, test_round, cur_step):
    if cur_step == 0:
        if "fpsb" in run_name:
            return "game_fpsb.json"
        return "game.json"
    return "game_" + run_name + "_r" + str(test_round) + "_s" + str(cur_step) + ".json"

def get_result_name(run_name):
    return run_name + "_results.tsv"

def get_deviations_name(run_name):
    return run_name + "_deviations.txt"

def get_tsv_strat_name(run_name, test_round, cur_step, is_defender):
    if is_defender:
        return run_name + "_r" + str(test_round) +  "_s" + str(cur_step + 1) + \
        "_def.tsv"
    return run_name + "_r" + str(test_round) +  "_s" + str(cur_step + 1) + \
        "_att.tsv"

def get_gambit_input_name(run_name, test_round, cur_step):
    if cur_step == 0:
        if "fpsb" in run_name:
            return "game_fpsb_gambit.nfg"
        return "game_gambit.nfg"
    return "game_" + run_name + "_r" + str(test_round) + "_s" + str(cur_step) + \
        "_gambit.nfg"

def get_gambit_result_name(run_name, test_round, cur_step):
    if cur_step == 0:
        if "fpsb" in run_name:
            return "gambit_fpsb_result_lcp.txt"
        return "gambit_result_lcp.txt"
    return "gambit_result_" + run_name + "_r" + str(test_round) + "_s" + str(cur_step) + \
        "_lcp.txt"

def get_decoded_result_name(run_name, test_round, cur_step):
    if cur_step == 0:
        if "fpsb" in run_name:
            return "gambit_fpsb_result_lcp_decode.txt"
        return "gambit_result_lcp_decode.txt"
    return "gambit_result_" + run_name + "_r" + str(test_round) + "_s" + str(cur_step) + \
        "_lcp_decode.txt"

def get_new_payoff_file_name(run_name, test_round, cur_step):
    return "newPayoffs_" + run_name + "_r" + str(test_round) + "_s" + str(cur_step) + \
        ".json"
