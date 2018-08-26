import random
from math import sin, pi
from simulated_annealing import find_params_simulated_annealing, get_beta_params, get_temp, \
    should_switch_params
from depgraph_connect import ATTACKER_MIXED

def sample_noise(samples):
    return (random.random() - 0.5) / samples

def quadratic_sample_function(params, samples):
    '''
    f() = - (params[0] - 0.7) ** 2 - (params[1] - 0.03) ** 2
    best should be (0.7, 0.03) with value 0
    '''
    return -1 * (params[0] - 0.7) ** 2 - (params[1] - 0.03) ** 2 + sample_noise(samples)

def sin_sample_function(params, samples):
    '''
    f() = sin(pi * (params[0] - 0.2)) + 0.5 * sin(29 * pi * (params[0] - 0.2))
    best should be (0.7), with value 1.5
    '''
    return sin(pi * (params[0] - 0.2)) + 0.5 * sin(29 * pi * (params[0] - 0.2)) + \
        sample_noise(samples)

def test_beta_params():
    low_var = 0.02
    high_var = 0.03
    for frac in range(1, 20):
        cur_mean = frac * 1. / 20
        print(get_beta_params(cur_mean, low_var))
        print(get_beta_params(cur_mean, high_var))

def test_get_temp():
    max_steps = 100
    for cur_step in range(max_steps):
        print(get_temp(cur_step, max_steps, 10.0))

def test_should_switch():
    max_steps = 100
    trials_each = 50
    max_temp = 10.0
    for cur_step in range(max_steps):
        cur_temp = get_temp(cur_step, max_steps, max_temp)
        count_switch = 0
        for _ in range(trials_each):
            should_switch = should_switch_params(max_temp, 0, cur_temp)
            if should_switch:
                count_switch += 1
        print(count_switch * 1. / trials_each)

def test_annealing_sin():
    param_count = 1
    max_steps = 100
    max_temp = 0.75
    samples_per_param = 1000
    neighbor_variance = 0.03
    sample_function = sin_sample_function
    should_print = True
    find_params_simulated_annealing(param_count, max_steps, max_temp, samples_per_param,
                                    neighbor_variance, sample_function, should_print, None, \
                                    ATTACKER_MIXED)

def test_annealing_quadratic():
    param_count = 2
    max_steps = 100
    max_temp = 1
    samples_per_param = 1000
    neighbor_variance = 0.03
    sample_function = quadratic_sample_function
    should_print = True
    find_params_simulated_annealing(param_count, max_steps, max_temp, samples_per_param,
                                    neighbor_variance, sample_function, should_print, None, \
                                    ATTACKER_MIXED)

def main():
    # test_get_temp()
    # test_should_switch()
    # test_annealing_quadratic()
    test_annealing_sin()

if __name__ == "__main__":
    main()
