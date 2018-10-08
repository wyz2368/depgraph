import sys
from plot_mean_payoffs import get_all_mean_payoffs
from plot_gains_auto import get_gains, plot_gains

def get_env_short_name_tsv(env_short_name_payoffs):
    if env_short_name_payoffs in ["s29n1", "d30n1"]:
        return env_short_name_payoffs
    return env_short_name_payoffs + "_randNoAndB"

def main(game_number, env_short_name_payoffs_list):
    print("Using environments: " + str(env_short_name_payoffs_list))
    env_short_name_tsv_list = [get_env_short_name_tsv(x) for x in \
        env_short_name_payoffs_list]
    mean_att_eq, mean_def_eq, mean_att_net, mean_def_net = \
        get_all_mean_payoffs(game_number, env_short_name_payoffs_list, \
        env_short_name_tsv_list)
    print(mean_att_eq)
    print(mean_att_net)
    print(mean_def_eq)
    print(mean_def_net)
    #print(str(len(mean_att_eq)) + "\t" + str(len(mean_att_net)) + "\t" + \
    #    str(len(mean_def_eq)) + "\t" + str(len(mean_def_net)))
    att_gains = get_gains(mean_att_eq, mean_att_net)
    def_gains = get_gains(mean_def_eq, mean_def_net)
    # print(str(len(att_gains)) + "\t" + str(len(def_gains)))
    save_env_name = env_short_name_payoffs_list[0] + "_mean"
    plot_gains(def_gains, att_gains, save_env_name)

'''
example: python3 plot_mean_gains.py 3014 d30d1 d30m1
'''
if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise ValueError("Need 2+ args: game_number, env_short_name_payoffs+")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS_LIST = []
    for j in range(2, len(sys.argv)):
        ENV_SHORT_NAME_PAYOFFS_LIST.append(sys.argv[j])
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS_LIST)
