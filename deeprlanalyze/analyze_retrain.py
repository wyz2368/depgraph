import sys
import os
import numpy as np
from plot_curves import has_values, get_values_even_if_ineligible

def get_fraction_final_won(data_lists, save_count):
    final_won = 0
    total = 0
    for data_list in data_lists:
        if data_list[2]: # is eligible (i.e., any result was a beneficial deviation)
            total += 1
            winner_index = data_list[1]
            if winner_index == save_count - 1:
                final_won += 1
    return final_won * 1.0 / total

def get_mean_final_gain_vs_eq(data_lists):
    gains = []
    for data_list in data_lists:
        if data_list[2]: # is eligible
            final_vs_eq = data_list[0][-1]
            pretrain_vs_eq = data_list[0][0]
            gains.append(final_vs_eq - pretrain_vs_eq)
    return np.mean(gains)

def get_mean_chosen_gain_vs_final(data_lists):
    gains = []
    for data_list in data_lists:
        if data_list[2]: # is eligible
            final_vs_eq = data_list[0][-1]
            winner_index = data_list[1]
            chosen_vs_eq = data_list[0][winner_index]
            gains.append(chosen_vs_eq - final_vs_eq)
    return np.mean(gains)

def main(env_short_name_payoffs, save_count):
    os.chdir("for_plot_curve")
    new_epoch = 2
    def_list = []
    att_list = []
    while has_values(env_short_name_payoffs, new_epoch, save_count):
        vs_eq_gains_def, _, winner_index_def, is_eligible = \
            get_values_even_if_ineligible( \
            env_short_name_payoffs, new_epoch, True, save_count)
        def_list.append([vs_eq_gains_def, winner_index_def, is_eligible])
        vs_eq_gains_att, _, winner_index_att, is_eligible = \
            get_values_even_if_ineligible( \
                env_short_name_payoffs, new_epoch, False, save_count)
        att_list.append([vs_eq_gains_att, winner_index_att, is_eligible])
        new_epoch += 1

    def_fraction_final_won = get_fraction_final_won(def_list, save_count)
    att_fraction_final_won = get_fraction_final_won(att_list, save_count)
    fmt = "{0:.2f}"
    print("Defender fraction final won: " + fmt.format(def_fraction_final_won))
    print("Attacker fraction final won: " + fmt.format(att_fraction_final_won))

    def_mean_final_gain_vs_eq = get_mean_final_gain_vs_eq(def_list)
    att_mean_final_gain_vs_eq = get_mean_final_gain_vs_eq(att_list)
    print("Defender final gain vs. eq: " + fmt.format(def_mean_final_gain_vs_eq))
    print("Attacker final gain vs. eq: " + fmt.format(att_mean_final_gain_vs_eq))

    def_mean_chosen_gain_vs_final = get_mean_chosen_gain_vs_final(def_list)
    att_mean_chosen_gain_vs_final = get_mean_chosen_gain_vs_final(att_list)
    print("Defender chosen gain vs. final: " + fmt.format(def_mean_chosen_gain_vs_final))
    print("Attacker chosen gain vs. final: " + fmt.format(att_mean_chosen_gain_vs_final))

'''
example: python3 analyze_retrain.py s29cs1 4
'''
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: env_short_name_payoffs, save_count")
    ENV_SHORT_NAME_PAYOFFS = sys.argv[1]
    SAVE_COUNT = int(sys.argv[2])
    main(ENV_SHORT_NAME_PAYOFFS, SAVE_COUNT)
