import sys
import subprocess
import time
import os.path

def get_game_file_name(game_number, tsv_epoch):
    if tsv_epoch is None:
        return "game_" + str(game_number) + ".json"
    return "game_" + str(game_number) + "_" + str(tsv_epoch) + ".json"

def get_gambit_input_name(game_number, tsv_epoch):
    if tsv_epoch is None:
        return "game_" + str(game_number) + "_gambit.nfg"
    return "game_" + str(game_number) + "_" + str(tsv_epoch) + "_gambit.nfg"

def get_gambit_result_name(game_number, tsv_epoch):
    if tsv_epoch is None:
        return "gambit_result_" + str(game_number) + "_lcp.txt"
    return "gambit_result_" + str(game_number) + "_" + str(tsv_epoch) + "_lcp.txt"

def get_decoded_result_name(game_number, tsv_epoch):
    if tsv_epoch is None:
        return "gambit_result_" + str(game_number) + "_lcp_decode.txt"
    return "gambit_result_" + str(game_number) + "_" + str(tsv_epoch) + "_lcp_decode.txt"

def call_and_wait(command_str):
    print("Will run:\n" + command_str)
    my_process = subprocess.Popen(command_str, shell=True)
    my_process.wait()
    sleep_sec = 5
    time.sleep(sleep_sec)
    my_process.kill()

def make_gambit_file(game_number, tsv_epoch):
    game_file_name = get_game_file_name(game_number, tsv_epoch)
    gambit_input_name = get_gambit_input_name(game_number, tsv_epoch)
    if os.path.isfile(gambit_input_name):
        print("Skipping: " + gambit_input_name + " already exists.")
        return
    if not os.path.isfile(game_file_name):
        raise ValueError(game_file_name + " missing.")
    command_str = "ga conv -i " + game_file_name + " gambit > " + gambit_input_name
    call_and_wait(command_str)

def gambit_analyze(game_number, tsv_epoch):
    gambit_input_name = get_gambit_input_name(game_number, tsv_epoch)
    gambit_result_name = get_gambit_result_name(game_number, tsv_epoch)
    if os.path.isfile(gambit_result_name):
        print("Skipping: " + gambit_result_name + " already exists.")
        return
    if not os.path.isfile(gambit_input_name):
        raise ValueError(gambit_input_name + " missing.")
    command_str = "gambit-lcp < " + gambit_input_name + " -d 8 > " + gambit_result_name
    call_and_wait(command_str)

def call_decode_gambit_solution(game_number, tsv_epoch):
    gambit_input_name = get_gambit_input_name(game_number, tsv_epoch)
    gambit_result_name = get_gambit_result_name(game_number, tsv_epoch)
    gambit_decoded_name = get_decoded_result_name(game_number, tsv_epoch)
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

def main(game_number, tsv_epoch):
    make_gambit_file(game_number, tsv_epoch)
    gambit_analyze(game_number, tsv_epoch)
    call_decode_gambit_solution(game_number, tsv_epoch)

# example: python3 gambit_analyze.py 3013 None
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 arg: game_number, current_tsv_epoch")
    GAME_NUM = int(sys.argv[1])
    TSV_EPOCH = sys.argv[2]
    if TSV_EPOCH == "None":
        TSV_EPOCH = None
    if TSV_EPOCH is not None:
        TSV_EPOCH = int(TSV_EPOCH)
    main(GAME_NUM, TSV_EPOCH)
