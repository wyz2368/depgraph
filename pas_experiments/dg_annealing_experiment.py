import csv
import time
import os.path
from pas_experiments import get_n
from gambit_analyze_pas import do_gambit_analyze
from create_tsv_files_pas import create_tsv, get_tsv_strat_name
from get_payoff_from_game_pas import get_def_payoff_eq, get_eq_from_file
from enjoy_def_pas import get_def_payoff
from dg_annealing import run_depgraph_annealing
from generate_new_cols_pas import gen_new_cols
from add_new_data_pas import add_data
from depgraph_connect import convert_params_from_0_1, get_def_name
from utility import get_game_file_name, get_result_name, get_deviations_name

def get_att_eq(run_name, test_round, cur_step):
    att_mixed_strat_name = get_tsv_strat_name(run_name, test_round, cur_step, False)
    if not os.path.isfile(att_mixed_strat_name):
        raise ValueError(att_mixed_strat_name + " missing.")
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
    return att_mixed_strat

def convert_deviating_strat_to_def_name(deviating_strat_0_1):
    deviating_strat_java = convert_params_from_0_1(deviating_strat_0_1)
    def_name = get_def_name(deviating_strat_java)
    return def_name

def record_deviations(deviations_name, deviation_sequences):
    with open(deviations_name, 'w') as txt_file:
        for sequence in deviation_sequences:
            txt_file.write(str(sequence) + "\n")

def record_result_tuples(result_name, result_tuples):
    with open(result_name, 'w') as tsv_file:
        csv_writer = csv.writer(tsv_file, delimiter='\t')
        for cur_tuple in result_tuples:
            csv_writer.writerow(cur_tuple)

def get_results(max_p, alpha_list, test_count, max_steps, max_samples, samples_per_param, \
    neighbor_variance, should_print, run_name, samples_new_column):
    results = []
    deviation_sequences = []
    start_time = time.time()
    for test_round in range(test_count):
        cur_step = 0
        was_confirmed = False
        deviation_sequence = []
        while cur_step < max_steps:
            print("new round: " + str(test_round) + "\t" + str(cur_step))
            do_gambit_analyze(run_name, test_round, cur_step)
            create_tsv(run_name, test_round, cur_step)

            error_prob_one_side = alpha_list[cur_step]
            cur_n = get_n(max_p, error_prob_one_side)
            if should_print:
                print("cur_n: " + str(cur_n))

            def_payoff_old = get_def_payoff_eq(run_name, test_round, cur_step)
            found_dev = False
            att_mixed_strat = get_att_eq(run_name, test_round, cur_step)
            for _ in range(cur_n):
                output_name = get_game_file_name(run_name, test_round, cur_step + 1)
                if os.path.isfile(output_name):
                    print("Skipping, file already exists: " + output_name)
                    break
                # deviating_strat is in [0, 1]^3
                deviating_strat, _ = run_depgraph_annealing(max_samples, \
                    samples_per_param, neighbor_variance, should_print, None, \
                    att_mixed_strat)
                def_payoff_cur = get_def_payoff(deviating_strat, run_name, test_round, \
                    cur_step, samples_new_column, att_mixed_strat)
                if def_payoff_cur > def_payoff_old:
                    found_dev = True
                    deviation_sequence.append(deviating_strat)
                    if cur_step + 1 < max_steps:
                        def_name = convert_deviating_strat_to_def_name(deviating_strat)
                        gen_new_cols(def_name, run_name, test_round, cur_step, \
                            samples_new_column)
                        add_data(run_name, test_round, cur_step)
                    break
            if not found_dev:
                if should_print:
                    print("confirmed after step: " + str(cur_step) + ", round " + \
                        str(test_round))
                was_confirmed = True
                break
            cur_step += 1
        cur_result = (was_confirmed, test_round)
        if test_round % 10 == 0:
            print(str(test_round) + "\t" + str(cur_result), flush=True)
        results.append(cur_result)
        deviation_sequences.append(deviation_sequence)
    seconds_taken = time.time() - start_time
    print("Seconds taken for all rounds: " + str(seconds_taken), flush=True)
    return results, deviation_sequences

def main(max_p, error_tolerance, test_count, max_rounds, max_steps, samples_per_param, \
    neighbor_variance, should_print, run_name, samples_new_column):
    result_name = get_result_name(run_name)
    if os.path.exists(result_name):
        raise ValueError("File exists: " + result_name)
    deviations_name = get_deviations_name(run_name)
    if os.path.exists(deviations_name):
        raise ValueError("File exists: " + deviations_name)

    alpha_list = [error_tolerance * 1.0 / max_rounds] * max_rounds
    print("alpha_list: " + str(alpha_list))
    result_tuples, deviation_sequences = get_results(max_p, alpha_list, test_count, \
        max_rounds, max_steps, samples_per_param, neighbor_variance, should_print, \
        run_name, samples_new_column)
    record_result_tuples(result_name, result_tuples)
    record_deviations(deviations_name, deviation_sequences)

# example: python3 dg_annealing_experiment.py
if __name__ == "__main__":
    MAX_P = 0.1
    ERROR_TOLERANCE = 0.1
    TEST_COUNT = 3
    MAX_STEPS = 5
    MAX_SAMPLES = 3
    SAMPLES_PER_PARAM = 3
    NEIGHBOR_VARIANCE = 0.05
    SHOULD_PRINT = True
    RUN_NAME = "dg1"
    SAMPLES_NEW_COLUMN = 3
    main(MAX_P, ERROR_TOLERANCE, TEST_COUNT, MAX_STEPS, MAX_SAMPLES, SAMPLES_PER_PARAM, \
        NEIGHBOR_VARIANCE, SHOULD_PRINT, RUN_NAME, SAMPLES_NEW_COLUMN)
