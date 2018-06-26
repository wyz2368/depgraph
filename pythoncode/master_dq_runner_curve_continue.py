import sys
import datetime
import subprocess
from os import chdir
import os.path
import pythoncode.check_if_beneficial as check
from add_new_data import get_add_data_result_file_name

def run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
    new_epoch, env_short_name_payoffs, def_model_to_add, att_model_to_add, graph_name):
    cmd_list = ["python3", "generate_new_cols_curve.py", env_name_def_net, \
        env_name_att_net, env_name_both, str(new_col_count), str(new_epoch), \
        env_short_name_payoffs, str(def_model_to_add), str(att_model_to_add), graph_name]
    subprocess.call(cmd_list)

def run_append_net_names(env_short_name_payoffs, new_epoch, def_pkl_prefix, \
    att_pkl_prefix, def_model_to_add, att_model_to_add):
    cmd_list = ["python3", "append_net_names_curve.py", env_short_name_payoffs, \
        str(new_epoch), def_pkl_prefix, att_pkl_prefix, str(def_model_to_add), \
        str(att_model_to_add)]
    subprocess.call(cmd_list)

def run_add_new_data(game_number, env_short_name_payoffs, new_epoch):
    cmd_list = ["python3", "add_new_data.py", str(game_number), env_short_name_payoffs, \
        str(new_epoch)]
    subprocess.call(cmd_list)

def run_gambit(game_number, cur_epoch):
    cmd_list = ["python3", "gambit_analyze.py", str(game_number), str(cur_epoch)]
    subprocess.call(cmd_list)

def run_create_tsv(game_number, cur_epoch, env_short_name_tsv):
    cmd_list = ["python3", "create_tsv_files.py", str(game_number), str(cur_epoch), \
        env_short_name_tsv]
    subprocess.call(cmd_list)

def run_update_strats(env_short_name_tsv, env_short_name_payoffs, new_epoch):
    cmd_list = ["python3", "update_opponent_strats.py", env_short_name_payoffs, \
        env_short_name_tsv, str(new_epoch)]
    subprocess.call(cmd_list)

def run_gen_def_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
        def_payoff_count, env_short_name_payoffs, graph_name, new_epoch):
    cmd_list = ["python3", "gen_def_payoffs.py", env_name_def_net, env_name_att_net, \
        env_name_both, str(def_payoff_count), env_short_name_payoffs, graph_name, \
        str(new_epoch)]
    subprocess.call(cmd_list)

def run_gen_att_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
        att_payoff_count, env_short_name_payoffs, graph_name, new_epoch):
    cmd_list = ["python3", "gen_att_payoffs.py", env_name_def_net, env_name_att_net, \
        env_name_both, str(att_payoff_count), env_short_name_payoffs, graph_name, \
        str(new_epoch)]
    subprocess.call(cmd_list)

def run_train_test_def(graph_name, env_short_name_payoffs, new_epoch, \
    env_name_vs_mixed_att):
    cmd_list = ["python3", "train_test_def.py", graph_name, env_short_name_payoffs, \
        str(new_epoch), env_name_vs_mixed_att]
    subprocess.call(cmd_list)

def run_train_test_att(graph_name, env_short_name_payoffs, new_epoch, \
    env_name_vs_mixed_def):
    cmd_list = ["python3", "train_test_att.py", graph_name, env_short_name_payoffs, \
        str(new_epoch), env_name_vs_mixed_def]
    subprocess.call(cmd_list)

def get_check_if_beneficial(env_short_name_payoffs, new_epoch, is_def):
    return check.check_for_cli(env_short_name_payoffs, new_epoch, is_def)

def run_epoch_continue(game_number, cur_epoch, env_short_name_tsv, \
    env_short_name_payoffs, env_name_def_net, \
    env_name_att_net, env_name_both, def_payoff_count, att_payoff_count, graph_name, \
    env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, def_pkl_prefix, \
    att_pkl_prefix, def_model_to_add, att_model_to_add):
    new_epoch = cur_epoch + 1
    result_file_name = get_add_data_result_file_name(game_number, new_epoch)
    if os.path.isfile(result_file_name):
        raise ValueError("Cannot run epoch " + str(cur_epoch) + ": " + \
            result_file_name + " already exists.")

    print("\tWill generate new columns, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample payoff of each new network (if it beneficially deviates) against all opponent
    # strategies
    chdir("pythoncode")
    run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
        new_epoch, env_short_name_payoffs, def_model_to_add, att_model_to_add, graph_name)
    # append name of each new network (if it beneficially deviates) to list of networks to
    # use in equilibrium search
    run_append_net_names(env_short_name_payoffs, new_epoch, def_pkl_prefix, \
        att_pkl_prefix, def_model_to_add, att_model_to_add)

    chdir("..")
    # add new payoff data to game object (from new beneficially deviating network(s))
    run_add_new_data(game_number, env_short_name_payoffs, new_epoch)

    print("\tShould continue after round: " + str(new_epoch), flush=True)
    return True

def main(game_number, cur_epoch, env_short_name_tsv, env_short_name_payoffs, \
        env_name_def_net, env_name_att_net, \
        env_name_both, def_payoff_count, att_payoff_count, graph_name, \
        env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, def_pkl_prefix, \
        att_pkl_prefix, def_model_to_add, att_model_to_add):
    should_continue = True
    if cur_epoch < 1:
        raise ValueError("cur_epoch must be at least 1: " + str(cur_epoch))
    if def_model_to_add is None and att_model_to_add is None:
        raise ValueError("Both models to add cannot be None.")
    my_epoch = cur_epoch
    print("\tStarting from epoch: " + str(my_epoch), flush=True)
    print("\tWill run epoch: " + str(my_epoch) + ", time: " + \
        str(datetime.datetime.now()), flush=True)
    should_continue = run_epoch_continue(game_number, my_epoch, env_short_name_tsv, \
        env_short_name_payoffs, env_name_def_net, env_name_att_net, env_name_both, \
        def_payoff_count, \
        att_payoff_count, graph_name, env_name_vs_mixed_def, env_name_vs_mixed_att, \
        new_col_count, def_pkl_prefix, att_pkl_prefix, def_model_to_add, att_model_to_add)
    if should_continue:
        my_epoch += 1
        print("\tShould continue with epoch: " + str(my_epoch) + ", time: " + \
            str(datetime.datetime.now()), flush=True)
    else:
        print("\tConverged at epoch: " + str(my_epoch) + ", time: " + \
            str(datetime.datetime.now()), flush=True)

'''
example: python3 master_dq_runner_curve_continue.py 3013 17 sl29_randNoAndB sl29 \
    DepgraphJava29N-v0 DepgraphJavaEnvAtt29N-v0 DepgraphJavaEnvBoth29N-v0 100 400 \
    SepLayerGraph0_noAnd_B.json DepgraphJavaEnvVsMixedDef29N-v0 \
    DepgraphJavaEnvVsMixedAtt29N-v0 400 dg_sl29_dq_mlp_rand_epoch \
    dg_sl29_dq_mlp_rand_epoch dg_s29_dq_mlp_rand_epoch17_afterRetrain_r1.pkl \
    None
'''
if __name__ == '__main__':
    if len(sys.argv) != 16:
        raise ValueError("Need 15 args: game_number, cur_epoch, env_short_name_tsv, " + \
            "env_short_name_payoffs, " + \
            "env_name_def_net, env_name_att_net, env_name_both, def_payoff_count, " + \
            "att_payoff_count, graph_name, env_name_vs_mixed_def, "  + \
            "env_name_vs_mixed_att, new_col_count, def_pkl_prefix, att_pkl_prefix, " + \
            "def_model_to_add, att_model_to_add")
    GAME_NUMBER = int(sys.argv[1])
    CUR_EPOCH = int(sys.argv[2])
    ENV_SHORT_NAME_TSV = sys.argv[3]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[4]
    ENV_NAME_DEF_NET = sys.argv[5]
    ENV_NAME_ATT_NET = sys.argv[6]
    ENV_NAME_BOTH = sys.argv[7]
    DEF_PAYOFF_COUNT = sys.argv[8]
    ATT_PAYOFF_COUNT = sys.argv[9]
    GRAPH_NAME = sys.argv[10]
    ENV_NAME_VS_MIXED_DEF = sys.argv[11]
    ENV_NAME_VS_MIXED_ATT = sys.argv[12]
    NEW_COL_COUNT = int(sys.argv[13])
    DEF_PKL_PREFIX = sys.argv[14]
    ATT_PKL_PREFIX = sys.argv[15]
    DEF_MODEL_TO_ADD = sys.argv[16]
    if DEF_MODEL_TO_ADD == "None":
        DEF_MODEL_TO_ADD = None
    ATT_MODEL_TO_ADD = sys.argv[17]
    if ATT_MODEL_TO_ADD == "None":
        ATT_MODEL_TO_ADD = None
    main(GAME_NUMBER, CUR_EPOCH, ENV_SHORT_NAME_TSV, ENV_SHORT_NAME_PAYOFFS, \
        ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, \
        ENV_NAME_BOTH, DEF_PAYOFF_COUNT, ATT_PAYOFF_COUNT, GRAPH_NAME, \
        ENV_NAME_VS_MIXED_DEF, ENV_NAME_VS_MIXED_ATT, NEW_COL_COUNT, \
        DEF_PKL_PREFIX, ATT_PKL_PREFIX, DEF_MODEL_TO_ADD, ATT_MODEL_TO_ADD)
