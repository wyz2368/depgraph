import sys
import os.path
from create_tsv_files_pas import get_tsv_strat_name
from get_payoff_from_game_pas import get_eq_from_file
from depgraph_connect import setup_default, sample_mean_def_payoff, close_process, \
    close_gateway

# deviating_strat is in [0, 1]^3
def get_def_payoff(deviating_strat, run_name, test_round, cur_step, samples_new_column,
                   att_mixed_strat):
    att_mixed_strat_name = get_tsv_strat_name(run_name, test_round, cur_step, False)
    if not os.path.isfile(att_mixed_strat_name):
        raise ValueError(att_mixed_strat_name + " missing.")
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)

    my_process = setup_default()
    def_payoff = sample_mean_def_payoff(deviating_strat, samples_new_column, att_mixed_strat)
    close_gateway()
    close_process(my_process)
    return def_payoff

'''
example: python3 enjoy_def_pas.py dg1 0 1 5 RANDOM_WALK:numRWSample_100_qrParam_1.0
'''
if __name__ == "__main__":
    if len(sys.argv) != 6:
        raise ValueError("Need 5 args: run_name, test_round, " + \
            "cur_step, samples_new_column, att_mixed_strat")
    DEVIATING_STRAT = [0.5, 0.5, 0.5]
    RUN_NAME = sys.argv[1]
    TEST_ROUND = int(sys.argv[2])
    CUR_STEP = int(sys.argv[3])
    SAMPLES_NEW_COLUMN = int(sys.argv[4])
    ATT_MIXED_STRAT = {sys.argv[5]: 1.0}
    print(get_def_payoff(DEVIATING_STRAT, RUN_NAME, TEST_ROUND, CUR_STEP, \
        SAMPLES_NEW_COLUMN, ATT_MIXED_STRAT))
