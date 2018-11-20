import sys
from plot_strat_rounds import get_all_eq_from_files, get_all_mean_rounds, plot_mean_rounds

def get_overall_means(means_lists):
    result = []
    max_len = max([len(x) for x in means_lists])
    for i in range(max_len):
        total = 0
        number = 0
        for means_list in means_lists:
            if len(means_list) > i:
                total += means_list[i]
                number += 1
        result.append(total * 1. / number)
    return result

def main(env_short_name_payoffs_list):
    print("Using environments: " + str(env_short_name_payoffs_list))
    mean_def_rounds_list = []
    mean_att_rounds_list = []
    for env_short_name_payoffs in env_short_name_payoffs_list:
        env_short_name_tsv = env_short_name_payoffs + "_randNoAndB"
        def_eqs, att_eqs = get_all_eq_from_files(env_short_name_tsv)
        mean_def_rounds = get_all_mean_rounds(def_eqs)
        mean_att_rounds = get_all_mean_rounds(att_eqs)
        mean_def_rounds_list.append(mean_def_rounds)
        mean_att_rounds_list.append(mean_att_rounds)
    all_mean_def_rounds = get_overall_means(mean_def_rounds_list)
    all_mean_att_rounds = get_overall_means(mean_att_rounds_list)
    print(all_mean_def_rounds)
    print(all_mean_att_rounds)
    out_prefix = env_short_name_payoffs_list[0] + "_mean"
    plot_mean_rounds(all_mean_def_rounds, all_mean_att_rounds, out_prefix)

'''
example: python3 plot_mean_strat_rounds.py d30d1 d30m1
'''
if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ValueError("Need 1+ args: env_short_name_payoffs+")
    ENV_SHORT_NAME_PAYOFFS_LIST = []
    for j in range(1, len(sys.argv)):
        ENV_SHORT_NAME_PAYOFFS_LIST.append(sys.argv[j])
    main(ENV_SHORT_NAME_PAYOFFS_LIST)
