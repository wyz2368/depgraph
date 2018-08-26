import sys
import os.path
from utility import get_game_file_name, call_and_wait, call_and_wait_with_timeout, \
    get_gambit_input_name, get_gambit_result_name, get_decoded_result_name
from decode_gambit_solution_pas import decode

def make_gambit_file(run_name, test_round, cur_step):
    game_file_name = get_game_file_name(run_name, test_round, cur_step)
    gambit_input_name = get_gambit_input_name(run_name, test_round, cur_step)
    if os.path.isfile(gambit_input_name):
        print("Skipping: " + gambit_input_name + " already exists.")
        return
    if not os.path.isfile(game_file_name):
        raise ValueError(game_file_name + " missing.")
    command_str = "ga conv -i " + game_file_name + " gambit > " + gambit_input_name
    call_and_wait(command_str)

def gambit_analyze(run_name, test_round, cur_step):
    gambit_input_name = get_gambit_input_name(run_name, test_round, cur_step)
    gambit_result_name = get_gambit_result_name(run_name, test_round, cur_step)
    if os.path.isfile(gambit_result_name):
        print("Skipping: " + gambit_result_name + " already exists.")
        return
    if not os.path.isfile(gambit_input_name):
        raise ValueError(gambit_input_name + " missing.")
    command_str = "gambit-lcp < " + gambit_input_name + " -d 8 > " + gambit_result_name
    call_and_wait_with_timeout(command_str)

def decode_gambit_solution(run_name, test_round, cur_step):
    gambit_input_name = get_gambit_input_name(run_name, test_round, cur_step)
    gambit_result_name = get_gambit_result_name(run_name, test_round, cur_step)
    gambit_decoded_name = get_decoded_result_name(run_name, test_round, cur_step)
    if os.path.isfile(gambit_decoded_name):
        print("Skipping: " + gambit_decoded_name + " already exists.")
        return
    if not os.path.isfile(gambit_input_name):
        raise ValueError(gambit_input_name + " missing.")
    if not os.path.isfile(gambit_result_name):
        raise ValueError(gambit_result_name + " missing.")
    decode(gambit_input_name, gambit_result_name, gambit_decoded_name)

def do_gambit_analyze(run_name, test_round, cur_step):
    make_gambit_file(run_name, test_round, cur_step)
    gambit_analyze(run_name, test_round, cur_step)
    decode_gambit_solution(run_name, test_round, cur_step)

# should use game_3014.json as game.json
# example: python3 gambit_analyze_pas.py dg1 0 1
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: run_name, test_round, cur_step")
    RUN_NAME = sys.argv[1]
    TEST_ROUND = int(sys.argv[2])
    CUR_STEP = int(sys.argv[3])
    do_gambit_analyze(RUN_NAME, TEST_ROUND, CUR_STEP)
