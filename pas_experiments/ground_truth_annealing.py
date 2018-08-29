import time
from dg_annealing import run_depgraph_annealing

def get_ground_truth_dev_prob(max_steps, samples_per_param, neighbor_variance, \
                              should_print, initial_params, att_mixed_strat, \
                              def_payoff_old, annealing_count_ground_truth):
    start_time = time.time()
    beneficial_count = 0
    print("Will run simulated annealing, times: " + str(annealing_count_ground_truth))
    print_freq = 25
    for cur_index in range(annealing_count_ground_truth):
        _, cur_value = run_depgraph_annealing(max_steps, samples_per_param, \
            neighbor_variance, should_print, initial_params, att_mixed_strat)
        if cur_value > def_payoff_old:
            beneficial_count += 1
        if (cur_index + 1) % print_freq == 0:
            print("Finished round: " + str(cur_index))
    duration = time.time() - start_time
    print("Minutes taken for getting ground truth dev prob: " + str(int(duration // 60)))
    return beneficial_count * 1. / annealing_count_ground_truth
