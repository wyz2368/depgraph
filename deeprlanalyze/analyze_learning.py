import sys
import numpy as np
from plot_learning_curves import get_learning_curves

LATE_FRAC = 0.1

def get_late_gains(learning_curves):
    result = []
    for learning_curve in learning_curves:
        learning_curve = learning_curve[1]
        start_of_late_part = int(len(learning_curve) * (1 - LATE_FRAC))
        late_gain = learning_curve[-1] - learning_curve[start_of_late_part]
        result.append(late_gain)
    return result

def get_end_regrets(learning_curves):
    result = []
    for learning_curve in learning_curves:
        learning_curve = learning_curve[1]
        best_payoff = max(learning_curve)
        final_payoff = learning_curve[-1]
        regret = best_payoff - final_payoff
        result.append(regret)
    return result

def print_regret_stats(regrets):
    mean_regrets = np.mean(regrets)
    fmt = "{0:.2f}"
    print("Mean regret vs. best: " + fmt.format(mean_regrets))

def print_gain_stats(gains):
    mean_gain = np.mean(gains)
    fmt = "{0:.2f}"
    print("Mean gain: " + fmt.format(mean_gain))

    gains_only = [x for x in gains if x > 0]
    fraction_pos = len(gains_only) * 1.0 / len(gains)
    print("Fraction positive gains: " + fmt.format(fraction_pos))

def main(env_short_name_payoffs):
    def_learning_curves = get_learning_curves(env_short_name_payoffs, True)
    att_learning_curves = get_learning_curves(env_short_name_payoffs, False)

    def_late_gains = get_late_gains(def_learning_curves)
    att_late_gains = get_late_gains(att_learning_curves)

    print("Defender late gains:")
    print_gain_stats(def_late_gains)
    print("Attacker late gains:")
    print_gain_stats(att_late_gains)

    def_end_regrets = get_end_regrets(def_learning_curves)
    att_end_regrets = get_end_regrets(att_learning_curves)

    print("Defender end regrets:")
    print_regret_stats(def_end_regrets)
    print("Attacker end regrets:")
    print_regret_stats(att_end_regrets)


'''
example: python3 analyze_learning.py s29m1
'''
if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: env_short_name_payoffs")
    ENV_SHORT_NAME_PAYOFFS = sys.argv[1]
    main(ENV_SHORT_NAME_PAYOFFS)
