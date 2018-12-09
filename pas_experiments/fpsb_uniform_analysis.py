from os import listdir
import time
from analyze_new_file import get_results
from get_payoff_from_game_pas import get_eq_from_file
from fpsb_annealing import sample_mean_def_payoff_fpsb, get_def_payoff
from fpsb_anneal_experiment import record_result_tuples

def get_att_eq(cur_round):
    prefix = "fpsb4_r" + str(cur_round) + "_s"
    suffix = "_att.tsv"
    att_mixed_strat_name = get_max_file(prefix, suffix)
    att_mixed_strat = get_eq_from_file(att_mixed_strat_name)
    return att_mixed_strat

def get_def_eq(cur_round):
    prefix = "fpsb4_r" + str(cur_round) + "_s"
    suffix = "_def.tsv"
    def_mixed_strat_name = get_max_file(prefix, suffix)
    def_mixed_strat = get_eq_from_file(def_mixed_strat_name)
    return def_mixed_strat

def get_max_file(prefix, suffix):
    max_index = -1
    result = ""
    for file_name in listdir():
        if file_name.startswith(prefix) and file_name.endswith(suffix):
            inner_part = file_name[len(prefix):len(file_name) - len(suffix)]
            cur_index = int(inner_part)
            if cur_index > max_index:
                result = file_name
                max_index = cur_index
    if result == "":
        raise ValueError("No file name found: " + prefix + ", " + suffix)
    return result

def get_uniform_random_dev_prob(att_mixed_strat, def_payoff_old, deviation_attempts, \
    epsilon_tolerance):
    beneficial_count = 0
    for def_index in range(deviation_attempts):
        deviating_strat = def_index * 1.0 / deviation_attempts
        def_payoff_cur = sample_mean_def_payoff_fpsb(deviating_strat, \
            att_mixed_strat)
        if def_payoff_cur > def_payoff_old + epsilon_tolerance:
            beneficial_count += 1
    return beneficial_count * 1.0 / deviation_attempts

def get_uniform_rand_ground_truth(cur_round, deviation_attempts, cur_def_payoff):
    cur_att_eq = get_att_eq(cur_round)
    cur_def_eq = get_def_eq(cur_round)
    cur_def_payoff = get_def_payoff(cur_att_eq, cur_def_eq)
    return get_uniform_random_dev_prob(cur_att_eq, cur_def_eq, cur_def_payoff, \
        deviation_attempts)

def main(max_rounds, deviation_attempts, epsilon_tolerance):
    print("max_rounds: " + str(max_rounds) + ", deviation_attempts: " + \
        str(deviation_attempts) + ", epsilon_tolerance: " + str(epsilon_tolerance))

    start_time = time.time()
    results = get_results("fpsb4_results.tsv")
    is_accepted_list = [x[0] for x in results]
    simulated_anneal_ground_truth_est_list = [x[1] for x in results]

    uniform_rand_ground_truth_est_list = []
    for cur_round in range(max_rounds):
        uniform_rand_ground_truth_est_list.append(get_uniform_rand_ground_truth( \
            cur_round, deviation_attempts, epsilon_tolerance))

    result_tuples = zip(is_accepted_list, simulated_anneal_ground_truth_est_list, \
        uniform_rand_ground_truth_est_list)
    record_result_tuples("fpsb4_uniform_results.tsv", result_tuples)
    duration = time.time() - start_time
    print("Minutes taken overall: " + str(int(duration // 60)))

if __name__ == "__main__":
    MAX_ROUNDS = 400
    DEVIATION_ATTEMPTS = 1000
    EPSILON_TOLERANCE = 0.0001
    main(MAX_ROUNDS, DEVIATION_ATTEMPTS, EPSILON_TOLERANCE)
