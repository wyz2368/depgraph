import time
from fpsb_annealing import run_fpsb_annealing, sample_mean_def_payoff_fpsb
from ground_truth_annealing import should_continue_anneal

def get_ground_truth_dev_prob_fpsb(max_steps, neighbor_variance, should_print, \
                                   initial_params, att_mixed_strat, def_payoff_old, \
                                   anneal_ground_truth_max, anneal_ground_truth_min, \
                                   early_stop_level, epsilon_tolerance):
    if anneal_ground_truth_max < anneal_ground_truth_min or \
        anneal_ground_truth_min < 1 or early_stop_level <= 0.0 or early_stop_level > 1.0 \
        or epsilon_tolerance < 0.0:
        raise ValueError("invalid inputs")
    start_time = time.time()
    print("Will run simulated annealing, up to " + str(anneal_ground_truth_max) + \
        " times.")
    print_freq = 1
    beneficial_count = 0
    count_so_far = 0
    fmt = "{0:.6f}"
    while should_continue_anneal(count_so_far, beneficial_count, anneal_ground_truth_max, \
            anneal_ground_truth_min, early_stop_level, should_print):
        deviating_strat, _ = run_fpsb_annealing(max_steps, \
            neighbor_variance, should_print, initial_params, att_mixed_strat)
        def_payoff_cur = sample_mean_def_payoff_fpsb(deviating_strat, att_mixed_strat)
        count_so_far += 1
        if should_print:
            print("Cur payoff " + fmt.format(def_payoff_cur) + ", vs. goal " + \
                fmt.format(def_payoff_old) + " plus epsilon_tolerance " + \
                fmt.format(epsilon_tolerance))
        if def_payoff_cur > def_payoff_old + epsilon_tolerance:
            beneficial_count += 1
        if (count_so_far) % print_freq == 0:
            print("Finished round: " + str(count_so_far) + ", of max: " + \
                str(anneal_ground_truth_max))
            print("Current beneficial dev prob estimate: " + \
                str(beneficial_count * 1. / count_so_far))
    duration = time.time() - start_time
    print("Minutes taken for getting ground truth dev prob: " + str(int(duration // 60)))
    print("Rounds used simulated annealing: " + str(count_so_far) + ", of possible " + \
        str(anneal_ground_truth_max))
    return beneficial_count * 1. / count_so_far
