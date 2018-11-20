import sys
from plot_strat_rounds import get_all_eq_from_files
from heatmap import get_round_distributions, plot_heatmap

def get_mean_round_distributions(round_distributions_list):
    '''
    each element of input round_distributions_list is a list of dicts. mapping:
    -- index of each list corresponds to the round number of the network, 0-based.
    -- each dict in each inner list maps:
        -- string version of integers in {0, . . ., cur_round_index}, to weight.

    the output is a list of dicts, of length equal to the max length in
    round_distributions_list.
    each dict maps string versions of integers in {0, cur_round_index}, where cur_round_index
    is the index of the dict in the result list, to a weight.
    the weight equals the mean weight of the corresponding string in the cur_round_index
    dicts in round_distributions_list, only over those outer lists where this index exists.
    '''
    result = []
    max_len = max([len(x) for x in round_distributions_list])
    for i in range(max_len):
        cur_output_dict = {}
        cur_count = 0
        for distribution_list in round_distributions_list:
            if len(distribution_list) > i:
                cur_input_dict = distribution_list[i]
                cur_count += 1
                for key, value in cur_input_dict.items():
                    if key in cur_output_dict:
                        cur_output_dict[key] += value
                    else:
                        cur_output_dict[key] = value
        for key, value in cur_output_dict.items():
            cur_output_dict[key] *= 1. / cur_count
        result.append(cur_output_dict)
    return result

def main(env_short_name_payoffs_list):
    print("Using environments: " + str(env_short_name_payoffs_list))
    def_round_distributions_list = []
    att_round_distributions_list = []
    for env_short_name_payoffs in env_short_name_payoffs_list:
        env_short_name_tsv = env_short_name_payoffs + "_randNoAndB"
        def_eqs, att_eqs = get_all_eq_from_files(env_short_name_tsv)
        def_round_distributions = get_round_distributions(def_eqs)
        att_round_distributions = get_round_distributions(att_eqs)
        def_round_distributions_list.append(def_round_distributions)
        att_round_distributions_list.append(att_round_distributions)
    all_def_round_dist = get_mean_round_distributions(def_round_distributions_list)
    all_att_round_dist = get_mean_round_distributions(att_round_distributions_list)
    print(all_def_round_dist)
    print(all_att_round_dist)
    out_prefix = env_short_name_payoffs_list[0] + "_mean"
    plot_heatmap(all_def_round_dist, True, out_prefix)
    plot_heatmap(all_att_round_dist, False, out_prefix)

'''
example: python3 heatmap_mean.py d30d1 d30m1
'''
if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ValueError("Need 1+ args: env_short_name_payoffs+")
    ENV_SHORT_NAME_PAYOFFS_LIST = []
    for j in range(1, len(sys.argv)):
        ENV_SHORT_NAME_PAYOFFS_LIST.append(sys.argv[j])
    main(ENV_SHORT_NAME_PAYOFFS_LIST)
