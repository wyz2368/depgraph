import sys
from simulated_annealing import find_params_simulated_annealing
from depgraph_connect import setup_default, sample_mean_def_payoff, close_process, \
    close_gateway, convert_params_from_0_1, get_def_name, convert_params_to_0_1, \
    ATTACKER_MIXED

def run_depgraph_annealing(max_steps, samples_per_param, neighbor_variance, should_print, \
                           initial_params_java, att_mixed_strat):
    my_process = setup_default()
    param_count = 3
    max_temp = 15
    if initial_params_java is not None:
        initial_params_0_1 = convert_params_to_0_1(initial_params_java)
    else:
        initial_params_0_1 = None
    best_params, best_value = find_params_simulated_annealing(param_count, max_steps, \
        max_temp, samples_per_param, neighbor_variance, sample_mean_def_payoff, \
        should_print, initial_params_0_1, att_mixed_strat)
    close_gateway()
    close_process(my_process)
    return best_params, best_value

def main(max_steps, samples_per_param, neighbor_variance, should_print):
    # initial_params_java = None
    initial_params_java = [0.5, 0.5, 3.0]
    best_params, _ = run_depgraph_annealing(max_steps, samples_per_param, \
        neighbor_variance, should_print, initial_params_java, ATTACKER_MIXED)
    best_params_java = convert_params_from_0_1(best_params)
    best_def_strat = get_def_name(best_params_java)
    print("Best def strat: " + best_def_strat)

def get_truth_value(input_str):
    return input_str.strip() == "True"

'''
example: python3 dg_annealing.py 10 5 0.05 True

see also:
    ps -A | grep "java"
    kill -9 foo
'''
if __name__ == "__main__":
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: max_steps, samples_per_param, " + \
            "neighbor_variance, should_print")
    MAX_STEPS = int(sys.argv[1])
    SAMPLES_PER_PARAM = int(sys.argv[2])
    NEIGHBOR_VARIANCE = float(sys.argv[3])
    SHOULD_PRINT = get_truth_value(sys.argv[4])
    main(MAX_STEPS, SAMPLES_PER_PARAM, NEIGHBOR_VARIANCE, SHOULD_PRINT)
