import time
from statsmodels.stats.proportion import proportion_confint
from dg_annealing import run_depgraph_annealing
from enjoy_def_pas import get_def_payoff

def should_continue_anneal(count_so_far, beneficial_count, \
    anneal_ground_truth_max, anneal_ground_truth_min, early_stop_level, should_print):
    if count_so_far < anneal_ground_truth_min:
        return True
    if count_so_far >= anneal_ground_truth_max:
        return False
    if beneficial_count * 1. / count_so_far <= early_stop_level:
        return True
    clopper_pearson_min = proportion_confint(beneficial_count, count_so_far, alpha=0.1, \
        method='beta')[0]
    # continue if we aren't sure that the beneficial deviation probability is "too high"
    result = clopper_pearson_min <= early_stop_level
    fmt = "{0:.2f}"
    if should_print:
        print("deviation prob lower bound: " + fmt.format(clopper_pearson_min))
        if result:
            print("will stop because above: " + fmt.format(early_stop_level))
        else:
            print("will not stop because not above: " + fmt.format(early_stop_level))
    return result

def get_ground_truth_dev_prob(max_steps, samples_per_param, neighbor_variance, \
                              should_print, initial_params, att_mixed_strat, \
                              def_payoff_old, anneal_ground_truth_max, \
                              anneal_ground_truth_min, early_stop_level, \
                              run_name, test_round, cur_step, epsilon_tolerance, \
                              samples_check_result, my_port):
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
    fmt = "{0:.2f}"
    while should_continue_anneal(count_so_far, beneficial_count, anneal_ground_truth_max, \
            anneal_ground_truth_min, early_stop_level, should_print):
        deviating_strat, _ = run_depgraph_annealing(max_steps, samples_per_param, \
            neighbor_variance, should_print, initial_params, att_mixed_strat, my_port)
        def_payoff_cur = get_def_payoff(deviating_strat, run_name, test_round, \
            cur_step, samples_check_result, att_mixed_strat, my_port)
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
