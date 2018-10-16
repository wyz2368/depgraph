import sys
from fpsb_simulated_annealing import find_params_simulated_annealing_fpsb

def get_def_payoff(def_strat, att_strat):
    def_strat = float(def_strat)
    att_strat = float(att_strat)
    if def_strat == 0.0:
        return 0.0
    if att_strat == 0.0:
        return (1 - def_strat) * 1.0 / 2
    if def_strat <= att_strat:
        return ((1 - def_strat) * def_strat) * 1.0 / (3 * att_strat)
    return ((1 - def_strat) * (3 * def_strat ** 2 - att_strat ** 2)) / (6.0 * def_strat ** 2)

def sample_mean_def_payoff_fpsb(def_strat, att_mixed_strat):
    result = 0.0
    for att_strat, weight in att_mixed_strat.items():
        result += weight * get_def_payoff(def_strat[0], att_strat)
    return result

def run_fpsb_annealing(max_steps, neighbor_variance, should_print, initial_params, \
                       att_mixed_strat):
    param_count = 1
    max_temp = 15
    best_params, best_value = find_params_simulated_annealing_fpsb(param_count, max_steps, \
        max_temp, neighbor_variance, sample_mean_def_payoff_fpsb, \
        should_print, initial_params, att_mixed_strat)
    return best_params, best_value

def main(max_steps, neighbor_variance, should_print):
    # initial_params = None
    initial_params = [0.5]
    att_mixed_strat = {"0.5": 0.1, "0.2": 0.9}
    best_params, _ = run_fpsb_annealing(max_steps, \
        neighbor_variance, should_print, initial_params, att_mixed_strat)
    print("Best def strat: " + best_params)

def get_truth_value(input_str):
    return input_str.strip() == "True"

'''
example: python3 fpsb_annealing.py 5 0.05 True

see also:
    ps -A | grep "java"
    kill -9 foo
'''
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: max_steps, neighbor_variance, should_print")
    MAX_STEPS = int(sys.argv[1])
    NEIGHBOR_VARIANCE = float(sys.argv[2])
    SHOULD_PRINT = get_truth_value(sys.argv[3])
    main(MAX_STEPS, NEIGHBOR_VARIANCE, SHOULD_PRINT)
