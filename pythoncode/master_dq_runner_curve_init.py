import sys
import datetime
import subprocess
from os import chdir
import os.path
import pythoncode.check_if_beneficial as check
from add_new_data import get_add_data_result_file_name

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

def run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
    new_epoch, env_short_name_payoffs, is_def_beneficial, is_att_beneficial, graph_name):
    cmd_list = ["python3", "generate_new_cols.py", env_name_def_net, env_name_att_net, \
        env_name_both, str(new_col_count), str(new_epoch), env_short_name_payoffs, \
        str(is_def_beneficial), str(is_att_beneficial), graph_name]
    subprocess.call(cmd_list)

def run_append_net_names(env_short_name_payoffs, new_epoch, def_pkl_prefix, \
    att_pkl_prefix, is_def_beneficial, is_att_beneficial):
    cmd_list = ["python3", "append_net_names.py", env_short_name_payoffs, str(new_epoch), \
        def_pkl_prefix, att_pkl_prefix, str(is_def_beneficial), \
        str(is_att_beneficial)]
    subprocess.call(cmd_list)

def run_add_new_data(game_number, env_short_name_payoffs, new_epoch):
    cmd_list = ["python3", "add_new_data.py", str(game_number), env_short_name_payoffs, \
        str(new_epoch)]
    subprocess.call(cmd_list)

def run_test_curve(env_short_name_tsv, env_short_name_payoffs, cur_epoch, \
    old_strat_disc_fact, save_count, graph_name, is_defender_net, runs_per_pair, \
    env_name_vs_mixed_def, env_name_vs_mixed_att):
    cmd_list = ["python3", "test_curve.py", env_short_name_tsv, env_short_name_payoffs, \
        str(cur_epoch), str(old_strat_disc_fact), str(save_count), graph_name, \
        str(is_defender_net), str(runs_per_pair), env_name_vs_mixed_def, \
        env_name_vs_mixed_att]
    subprocess.call(cmd_list)

def run_init_epoch(game_number, env_short_name_tsv, env_short_name_payoffs, \
    env_name_def_net, \
    env_name_att_net, env_name_both, def_payoff_count, att_payoff_count, graph_name, \
    env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, def_pkl_prefix, \
    att_pkl_prefix, old_strat_disc_fact, save_count, runs_per_pair):
    cur_epoch = 0
    new_epoch = 1
    result_file_name = get_add_data_result_file_name(game_number, new_epoch)
    if os.path.isfile(result_file_name):
        raise ValueError("Cannot run epoch " + str(cur_epoch) + ": " + \
            result_file_name + " already exists.")

    print("\tWill run gambit, epoch: " + str(cur_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # get Nash equilibrium of current strategies
    run_gambit(game_number, cur_epoch)
    # create TSV file for current attacker and defender equilibrium strategies
    run_create_tsv(game_number, cur_epoch, env_short_name_tsv)

    chdir("pythoncode")
    # set the mixed-strategy opponents to use current TSV file strategies
    run_update_strats(env_short_name_tsv, env_short_name_payoffs, new_epoch)
    print("\tWill get def payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each defender strategy vs. new attacker equilibrium
    run_gen_def_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
        def_payoff_count, env_short_name_payoffs, graph_name, new_epoch)
    print("\tWill get att payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each attacker strategy vs. new defender equilibrium
    run_gen_att_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
        att_payoff_count, env_short_name_payoffs, graph_name, new_epoch)
    print("\tWill train and test defender, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train defender network against current attacker equilibrium, and sample its payoff
    run_train_test_def(graph_name, env_short_name_payoffs, new_epoch, env_name_vs_mixed_att)
    print("\tWill train and test attacker, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train attacker network against current defender equilibrium, and sample its payoff
    run_train_test_att(graph_name, env_short_name_payoffs, new_epoch, env_name_vs_mixed_def)

    # check if new defender network is beneficial deviation from old equilibrium
    is_def_beneficial = get_check_if_beneficial(env_short_name_payoffs, new_epoch, True)
    # check if new attacker network is beneficial deviation from old equilibrium
    is_att_beneficial = get_check_if_beneficial(env_short_name_payoffs, new_epoch, False)

    if not is_def_beneficial and not is_att_beneficial:
        # neither network beneficially deviates, so stop
        print("\tConverged after round: " + str(new_epoch), flush=True)
        return False

    print("\tWill generate new columns, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample payoff of each new network (if it beneficially deviates) against all opponent
    # strategies
    run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
        new_epoch, env_short_name_payoffs, is_def_beneficial, is_att_beneficial, graph_name)
    # append name of each new network (if it beneficially deviates) to list of networks to
    # use in equilibrium search
    run_append_net_names(env_short_name_payoffs, new_epoch, def_pkl_prefix, \
        att_pkl_prefix, is_def_beneficial, is_att_beneficial)

    chdir("..")
    # add new payoff data to game object (from new beneficially deviating network(s))
    run_add_new_data(game_number, env_short_name_payoffs, new_epoch)

    print("\tWill continue after round: " + str(new_epoch), flush=True)

    cur_epoch += 1
    new_epoch += 1

    result_file_name = get_add_data_result_file_name(game_number, new_epoch)
    if os.path.isfile(result_file_name):
        raise ValueError("Cannot run epoch " + str(cur_epoch) + ": " + \
            result_file_name + " already exists.")

    print("\tWill run gambit, epoch: " + str(cur_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # get Nash equilibrium of current strategies
    run_gambit(game_number, cur_epoch)
    # create TSV file for current attacker and defender equilibrium strategies
    run_create_tsv(game_number, cur_epoch, env_short_name_tsv)

    chdir("pythoncode")
    # set the mixed-strategy opponents to use current TSV file strategies
    run_update_strats(env_short_name_tsv, env_short_name_payoffs, new_epoch)
    print("\tWill get def payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each defender strategy vs. new attacker equilibrium
    run_gen_def_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
        def_payoff_count, env_short_name_payoffs, graph_name, new_epoch)
    print("\tWill get att payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each attacker strategy vs. new defender equilibrium
    run_gen_att_payoffs(env_name_def_net, env_name_att_net, env_name_both, \
        att_payoff_count, env_short_name_payoffs, graph_name, new_epoch)
    print("\tWill train and test defender, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train defender network against current attacker equilibrium, and sample its payoff
    run_train_test_def(graph_name, env_short_name_payoffs, new_epoch, env_name_vs_mixed_att)
    print("\tWill train and test attacker, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train attacker network against current defender equilibrium, and sample its payoff
    run_train_test_att(graph_name, env_short_name_payoffs, new_epoch, env_name_vs_mixed_def)

    # check if new defender network is beneficial deviation from old equilibrium
    is_def_beneficial = get_check_if_beneficial(env_short_name_payoffs, new_epoch, True)
    # check if new attacker network is beneficial deviation from old equilibrium
    is_att_beneficial = get_check_if_beneficial(env_short_name_payoffs, new_epoch, False)

    if not is_def_beneficial and not is_att_beneficial:
        # neither network beneficially deviates, so stop
        print("\tConverged after round: " + str(new_epoch), flush=True)
        return False

    chdir("..")
    if is_def_beneficial:
        is_defender_net = True
        print("\tWill get def curve, epoch: " + str(new_epoch)+ ", time: " + \
            str(datetime.datetime.now()), flush=True)
        run_test_curve(env_short_name_tsv, env_short_name_payoffs, cur_epoch, \
            old_strat_disc_fact, save_count, graph_name, is_defender_net, runs_per_pair, \
            env_name_vs_mixed_def, env_name_vs_mixed_att)
    if is_att_beneficial:
        is_defender_net = False
        print("\tWill get att curve, epoch: " + str(new_epoch)+ ", time: " + \
            str(datetime.datetime.now()), flush=True)
        run_test_curve(env_short_name_tsv, env_short_name_payoffs, cur_epoch, \
            old_strat_disc_fact, save_count, graph_name, is_defender_net, runs_per_pair, \
            env_name_vs_mixed_def, env_name_vs_mixed_att)

    return True

def main(game_number, env_short_name_tsv, env_short_name_payoffs, \
        env_name_def_net, env_name_att_net, \
        env_name_both, def_payoff_count, att_payoff_count, graph_name, \
        env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, def_pkl_prefix, \
        att_pkl_prefix, old_strat_disc_fact, save_count, runs_per_pair):
    if old_strat_disc_fact <= 0.0 or old_strat_disc_fact > 1.0:
        raise ValueError("old_strat_disc_fact must be in (0, 1]: " + \
            str(old_strat_disc_fact))
    if save_count < 1:
        raise ValueError("save_count must be >= 1: " + str(save_count))
    if runs_per_pair < 2:
        raise ValueError("runs_per_pair must be >= 2: " + str(runs_per_pair))

    should_continue = True
    my_epoch = 0
    print("\tStarting from epoch 0", flush=True)
    print("\tWill run epoch: " + str(my_epoch) + ", time: " + \
        str(datetime.datetime.now()), flush=True)
    should_continue = run_init_epoch(game_number, env_short_name_tsv, \
        env_short_name_payoffs, env_name_def_net, env_name_att_net, env_name_both, \
        def_payoff_count, \
        att_payoff_count, graph_name, env_name_vs_mixed_def, env_name_vs_mixed_att, \
        new_col_count, def_pkl_prefix, att_pkl_prefix, old_strat_disc_fact, save_count, \
        runs_per_pair)
    if should_continue:
        print("\tShould continue from epoch: " + str(my_epoch + 1) + ", time: " + \
            str(datetime.datetime.now()), flush=True)
    else:
        print("\tConverged, time: " + str(datetime.datetime.now()), flush=True)

'''
example: python3 master_dq_runner_curve_init.py 3013 sl29_randNoAndB sl29 \
    DepgraphJava29N-v0 DepgraphJavaEnvAtt29N-v0 DepgraphJavaEnvBoth29N-v0 100 400 \
    SepLayerGraph0_noAnd_B.json DepgraphJavaEnvVsMixedDef29N-v0 \
    DepgraphJavaEnvVsMixedAtt29N-v0 400 dg_sl29_dq_mlp_rand_epoch \
    dg_sl29_dq_mlp_rand_epoch 0.5 4 1000

'''
if __name__ == '__main__':
    if len(sys.argv) != 18:
        raise ValueError("Need 17 args: game_number, env_short_name_tsv, " + \
            "env_short_name_payoffs, " + \
            "env_name_def_net, env_name_att_net, env_name_both, def_payoff_count, " + \
            "att_payoff_count, graph_name, env_name_vs_mixed_def, "  + \
            "env_name_vs_mixed_att, new_col_count, def_pkl_prefix, att_pkl_prefix, " + \
            "old_strat_disc_fact, save_count, runs_per_pair")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_TSV = sys.argv[2]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[3]
    ENV_NAME_DEF_NET = sys.argv[4]
    ENV_NAME_ATT_NET = sys.argv[5]
    ENV_NAME_BOTH = sys.argv[6]
    DEF_PAYOFF_COUNT = sys.argv[7]
    ATT_PAYOFF_COUNT = sys.argv[8]
    GRAPH_NAME = sys.argv[9]
    ENV_NAME_VS_MIXED_DEF = sys.argv[10]
    ENV_NAME_VS_MIXED_ATT = sys.argv[11]
    NEW_COL_COUNT = int(sys.argv[12])
    DEF_PKL_PREFIX = sys.argv[13]
    ATT_PKL_PREFIX = sys.argv[14]
    OLD_STRAT_DISC_FACT = float(sys.argv[15])
    SAVE_COUNT = int(sys.argv[16])
    RUNS_PER_PAIR = int(sys.argv[17])
    main(GAME_NUMBER, ENV_SHORT_NAME_TSV, ENV_SHORT_NAME_PAYOFFS, \
        ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, \
        ENV_NAME_BOTH, DEF_PAYOFF_COUNT, ATT_PAYOFF_COUNT, GRAPH_NAME, \
        ENV_NAME_VS_MIXED_DEF, ENV_NAME_VS_MIXED_ATT, NEW_COL_COUNT, \
        DEF_PKL_PREFIX, ATT_PKL_PREFIX, OLD_STRAT_DISC_FACT, SAVE_COUNT, RUNS_PER_PAIR)
