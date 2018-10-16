import csv
import time
import os.path
from pas_experiments import get_n
from gambit_analyze_pas import do_gambit_analyze
from create_tsv_files_pas import create_tsv, get_tsv_strat_name
from get_payoff_from_game_pas import get_def_payoff_eq, get_eq_from_file
from fpsb_annealing import run_fpsb_annealing, sample_mean_def_payoff_fpsb
from fpsb_gen_new_cols import gen_new_cols_fpsb
from add_new_data_pas import add_data
from utility import get_game_file_name, get_result_name, get_deviations_name
from fpsb_ground_truth import get_ground_truth_dev_prob_fpsb

def get_att_eq(run_name, test_round, cur_step):
    att_mixed_strat_name = get_tsv_strat_name(run_name, test_round, cur_step, False)
    if not os.path.isfile(att_mixed_strat_name):
        raise ValueError(att_mixed_strat_name + " missing.")
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
    return att_mixed_strat

def record_deviations(deviations_name, deviation_sequences):
    with open(deviations_name, 'w') as txt_file:
        for sequence in deviation_sequences:
            txt_file.write(str(sequence) + "\n")

def record_result_tuples(result_name, result_tuples):
    with open(result_name, 'w') as tsv_file:
        csv_writer = csv.writer(tsv_file, delimiter='\t')
        for cur_tuple in result_tuples:
            csv_writer.writerow(cur_tuple)

def get_results(max_p, alpha_list, test_count, max_steps, max_samples, \
    neighbor_variance, should_print, run_name, \
    anneal_ground_truth_max, anneal_ground_truth_min, early_stop_level, epsilon_tolerance):
    results = []
    deviation_sequences = []
    start_time_all = time.time()
    fmt = "{0:.6f}"
    for test_round in range(test_count):
        start_time_round = time.time()
        cur_step = 0
        was_confirmed = False
        deviation_sequence = []
        while cur_step < max_steps:
            print("new round: test round " + str(test_round) + ", cur step: " + \
                str(cur_step), flush=True)
            do_gambit_analyze(run_name, test_round, cur_step)
            create_tsv(run_name, test_round, cur_step)

            error_prob_one_side = alpha_list[cur_step]
            cur_n = get_n(max_p, error_prob_one_side)
            if should_print:
                print("cur_n: " + str(cur_n), flush=True)

            def_payoff_old = get_def_payoff_eq(run_name, test_round, cur_step)
            found_dev = False
            att_mixed_strat = get_att_eq(run_name, test_round, cur_step)
            start_time_sim_annealing = time.time()
            for cur_annealing_step in range(cur_n):
                output_name = get_game_file_name(run_name, test_round, cur_step + 1)
                if os.path.isfile(output_name):
                    print("Skipping, file already exists: " + output_name, flush=True)
                    break
                # deviating_strat is in [0, 1]
                deviating_strat, cur_best_value = run_fpsb_annealing(max_samples, \
                    neighbor_variance, should_print, None, att_mixed_strat)
                if should_print:
                    print("Finished annealing round " + str(cur_annealing_step) + " of " + \
                        str(cur_n), flush=True)
                    print("Mean def payoff to beat was: " + fmt.format(def_payoff_old),\
                        flush=True)
                    print("Estimated best value was: " + fmt.format(cur_best_value), \
                        flush=True)
                def_payoff_cur = sample_mean_def_payoff_fpsb(deviating_strat, \
                    att_mixed_strat)
                if def_payoff_cur > def_payoff_old + epsilon_tolerance:
                    found_dev = True
                    deviation_sequence.append(deviating_strat)
                    if should_print:
                        print("found deviation after annealing step " + \
                            str(cur_annealing_step) + ", strategy step " + str(cur_step) + \
                            ", round " + str(test_round), flush=True)
                        print("New estimate beats old value by epsilon_tolerance of " + \
                            fmt.format(epsilon_tolerance) + ": " + \
                            fmt.format(def_payoff_cur), flush=True)
                    if cur_step + 1 < max_steps:
                        def_name = deviating_strat[0]
                        gen_new_cols_fpsb(def_name, run_name, test_round, cur_step)
                        add_data(run_name, test_round, cur_step)
                    break
                else:
                    if should_print:
                        print("New estimate fails to beat old value by " + \
                            "epsilon_tolerance of " + fmt.format(epsilon_tolerance) + \
                            ": " + fmt.format(def_payoff_cur), flush=True)
            seconds_taken_simulated_annealing = time.time() - start_time_sim_annealing
            print("Minutes used for all simulated annealing: " + \
                str(int(seconds_taken_simulated_annealing // 60)), flush=True)
            if not found_dev:
                if should_print:
                    print("confirmed after step: " + str(cur_step) + ", round " + \
                        str(test_round), flush=True)
                was_confirmed = True
                break
            cur_step += 1
        if cur_step == max_steps:
            cur_step -= 1 # needed to find ground truth deviation probability
        ground_truth_dev_prob = get_ground_truth_dev_prob_fpsb(max_samples, \
            neighbor_variance, should_print, None, att_mixed_strat, def_payoff_old, \
            anneal_ground_truth_max, anneal_ground_truth_min, early_stop_level, \
            epsilon_tolerance)
        cur_result = (was_confirmed, test_round, ground_truth_dev_prob)
        if test_round % 10 == 0:
            print("round " + str(test_round) + " result: " + str(cur_result), flush=True)
        results.append(cur_result)
        deviation_sequences.append(deviation_sequence)
        seconds_taken_round = time.time() - start_time_round
        print("Minutes taken for round " + str(test_round) + " of " + str(test_count) + \
            ": " + str(int(seconds_taken_round // 60)), flush=True)
    seconds_taken_all = time.time() - start_time_all
    print("Minutes taken for all rounds: " + str(int(seconds_taken_all // 60)) + "\n", \
        flush=True)
    return results, deviation_sequences

def main(max_p, error_tolerance, test_count, max_rounds, max_steps, \
    neighbor_variance, should_print, run_name, \
    anneal_ground_truth_max, anneal_ground_truth_min, early_stop_level, epsilon_tolerance):
    result_name = get_result_name(run_name)
    if os.path.exists(result_name):
        raise ValueError("File exists: " + result_name)
    deviations_name = get_deviations_name(run_name)
    if os.path.exists(deviations_name):
        raise ValueError("File exists: " + deviations_name)
    if epsilon_tolerance < 0.0:
        raise ValueError("epsilon_tolerance must be >= 0.0: " + str(epsilon_tolerance))

    fmt = "{0:.6f}"
    print("Will run fpsb_annealing_experiment.py:")
    print("max_p: " + fmt.format(max_p))
    print("error_tolerance: " + fmt.format(error_tolerance))
    print("test_count: " + str(test_count))
    print("max_rounds: " + str(max_rounds))
    print("max_steps: " + str(max_steps))
    print("neighbor_variance: " + fmt.format(neighbor_variance))
    print("run_name: " + run_name)
    print("anneal_ground_truth_max: " + str(anneal_ground_truth_max))
    print("anneal_ground_truth_min: " + str(anneal_ground_truth_min))
    print("early_stop_level: " + fmt.format(early_stop_level))
    print("epsilon_tolerance: " + fmt.format(epsilon_tolerance) + "\n", flush=True)

    alpha_list = [error_tolerance * 1.0 / max_rounds] * max_rounds
    print("alpha_list: " + str(alpha_list), flush=True)
    result_tuples, deviation_sequences = get_results(max_p, alpha_list, test_count, \
        max_rounds, max_steps, neighbor_variance, should_print, \
        run_name, anneal_ground_truth_max, anneal_ground_truth_min, \
        early_stop_level, epsilon_tolerance)
    record_result_tuples(result_name, result_tuples)
    record_deviations(deviations_name, deviation_sequences)

'''
example: python3 fpsb_anneal_experiment.py
or: stdbuf -i0 -o0 -e0 python3 fpsb_anneal_experiment.py > out_fpsb1.txt

good debugging values:
(0.2, 0.2, 2, 3, 3, 0.05, True, "fpsb1", 3, 10, 0.4, 0.01)

good final values:
(0.05, 0.1, 700, 10, 20, 0.03, True, fpsb1, 400, 20, 0.1, 0.01)

compromise final values:
(0.05, 0.1, 1, 10, 5, 0.03, True, fpsb1, 100, 10, 0.1, 0.01)
'''
if __name__ == "__main__":
    MAX_P = 0.05
    ERROR_TOLERANCE = 0.1
    TEST_COUNT = 1
    MAX_STEPS = 10
    MAX_SAMPLES = 50
    NEIGHBOR_VARIANCE = 0.003
    SHOULD_PRINT = True
    RUN_NAME = "fpsb1"
    ANNEAL_GROUND_TRUTH_MAX = 200
    ANNEAL_GROUND_TRUTH_MIN = 10
    EARLY_STOP_LEVEL = MAX_P * 2
    EPSILON_TOLERANCE = 0.0001
    main(MAX_P, ERROR_TOLERANCE, TEST_COUNT, MAX_STEPS, MAX_SAMPLES, \
        NEIGHBOR_VARIANCE, SHOULD_PRINT, RUN_NAME, \
        ANNEAL_GROUND_TRUTH_MAX, ANNEAL_GROUND_TRUTH_MIN, EARLY_STOP_LEVEL, \
        EPSILON_TOLERANCE)
