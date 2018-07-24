'''
Runs an epoch after the first, and part of next epoch of training for robustified
double-oracle method, where each successful training session is followed by fine-tuning
against a mixture of previous opponent equilibria for robustness.
Begins the earlier epoch after a human has decided which fine-tuned version to use for
attacker and defender, proceeding through the end of that epoch.
Runs the next epoch until after fine-tuning, when a human must assist by deciding which
network (the trained one or a fine-tuned version) to proceed with for attacker and defender.
'''
import sys
import datetime
import subprocess
from os import chdir
import os.path
import pythoncode.check_if_beneficial as check
from pythoncode.train_test_def import wait_for_def_lock, lock_def, read_def_port, \
    PORTS_PER_ROUND, MAX_PORT, MIN_PORT
from add_new_data import get_add_data_result_file_name

def run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
    new_epoch, env_short_name_payoffs, def_model_to_add, att_model_to_add, graph_name):
    '''
    Evaluate the mean payoff for self and opponent of the new defender and attacker nets
    (whichever were beneficial deviations) against each opponent strategy, for augmenting
    the game's payoff matrix.
    '''
    cmd_list = ["python3", "generate_new_cols_curve.py", env_name_def_net, \
        env_name_att_net, env_name_both, str(new_col_count), str(new_epoch), \
        env_short_name_payoffs, str(def_model_to_add), str(att_model_to_add), graph_name]
    subprocess.call(cmd_list)

def run_append_net_names(env_short_name_payoffs, def_model_to_add, att_model_to_add):
    '''
    Add the names of the new attacker and defender nets (whichever were beneficial
    deviations) to the lists of network strategies in the game.
    '''
    cmd_list = ["python3", "append_net_names_curve.py", env_short_name_payoffs, \
        str(def_model_to_add), str(att_model_to_add)]
    subprocess.call(cmd_list)

def run_add_new_data(game_number, env_short_name_payoffs, new_epoch):
    '''
    Add the data on payoffs with the new beneficially deviating
    strategy/strategies to the game's Json file.
    '''
    cmd_list = ["python3", "add_new_data.py", str(game_number), env_short_name_payoffs, \
        str(new_epoch)]
    subprocess.call(cmd_list)

def run_gambit(game_number, cur_epoch, env_short_name_payoffs):
    '''
    Call a script to find a Nash equilibrium of the current game.
    '''
    cmd_list = ["python3", "gambit_analyze.py", str(game_number), str(cur_epoch), \
        env_short_name_payoffs]
    subprocess.call(cmd_list)

def run_create_tsv(game_number, cur_epoch, env_short_name_tsv, env_short_name_payoffs):
    '''
    Run a script to extract the current equilibrium strategies to TSV files.
    '''
    cmd_list = ["python3", "create_tsv_files.py", str(game_number), str(cur_epoch), \
        env_short_name_tsv, env_short_name_payoffs]
    subprocess.call(cmd_list)

def run_update_strats(env_short_name_tsv, env_short_name_payoffs, new_epoch):
    '''
    Run a script to update the config files for the Gym environments to reference the new
    equilibrium strategies' TSV files.
    '''
    cmd_list = ["python3", "update_opponent_strats.py", env_short_name_payoffs, \
        env_short_name_tsv, str(new_epoch)]
    subprocess.call(cmd_list)

def run_gen_both_payoffs(game_number, env_short_name_payoffs, new_epoch):
    cmd_list = ["python3", "gen_both_payoffs_from_game.py", str(game_number), \
        env_short_name_payoffs, str(new_epoch)]
    subprocess.call(cmd_list)

def run_retrain_all(graph_name, env_short_name_payoffs, new_epoch, \
    env_name_vs_mixed_att, env_name_vs_mixed_def, port_lock_name, env_short_name_tsv, \
    max_timesteps_def_init, max_timesteps_def_retrain, max_timesteps_att_init, \
    max_timesteps_att_retrain):
    is_train = True
    wait_for_def_lock(port_lock_name, is_train)
    lock_def(port_lock_name, is_train)
    def_port = read_def_port(port_lock_name, is_train)
    def_port += PORTS_PER_ROUND
    if def_port >= MAX_PORT:
        def_port = MIN_PORT

    cmd_list_train_def = ["python3", "train_retrain_def.py", graph_name, \
        env_short_name_payoffs, str(new_epoch), env_name_vs_mixed_att, port_lock_name, \
        str(def_port), env_short_name_tsv, str(max_timesteps_def_init), \
        str(max_timesteps_def_retrain)]
    cmd_list_train_att = ["python3", "train_retrain_att.py", graph_name, \
        env_short_name_payoffs, str(new_epoch), env_name_vs_mixed_def, str(def_port), \
        port_lock_name, env_short_name_tsv, str(max_timesteps_att_init), \
        str(max_timesteps_att_retrain)]
    process_train_def = subprocess.Popen(cmd_list_train_def)
    process_train_att = subprocess.Popen(cmd_list_train_att)

    process_train_def.wait()
    process_train_att.wait()

def get_check_if_beneficial(env_short_name_payoffs, new_epoch, is_def):
    '''
    Check if the newly-trained strategy net (defender or attacker) yielded a higher mean
    payoff against the current equilibrium opponent than the best previous strategy of the
    same role.
    '''
    return check.check_for_cli(env_short_name_payoffs, new_epoch, is_def)

def run_test_curve(env_short_name_tsv, env_short_name_payoffs, cur_epoch, \
    old_strat_disc_fact, save_count, graph_name, is_defender_net, runs_per_pair, \
    env_name_vs_mixed_def, env_name_vs_mixed_att):
    '''
    Fine-tune the recently-trained network against a weighted mean of previous opponent
    equilibrium strategies, and evaluate the result of the current and fine-tuned network
    versions.
    '''
    cmd_list = ["python3", "test_curve.py", env_short_name_tsv, env_short_name_payoffs, \
        str(cur_epoch), str(old_strat_disc_fact), str(save_count), graph_name, \
        str(is_defender_net), str(runs_per_pair), env_name_vs_mixed_def, \
        env_name_vs_mixed_att]
    subprocess.call(cmd_list)

def run_epoch_continue(game_number, cur_epoch, env_short_name_tsv, \
    env_short_name_payoffs, env_name_def_net, \
    env_name_att_net, env_name_both, graph_name, \
    env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, def_model_to_add, \
    att_model_to_add, old_strat_disc_fact, save_count, runs_per_pair, \
    port_lock_name, max_timesteps_def_init, \
    max_timesteps_def_retrain, max_timesteps_att_init, max_timesteps_att_retrain):
    '''
    Finish running cur_epoch of training, adding the selected strategy or strategies
    def_model_to_add and att_model_to_add if not None.
    Begin the next round, new_epoch (cur_epoch + 1), and stop after the attacker and
    defender nets have been trained, fine-tuned against the previous equilibrium, and the
    trained and fine-tuned nets have been evaluated against the current and previous
    equilibria. (The human can then choose which attacker and defender nets to add to the
    game.) If in new_epoch neither attacker nor defender net is beneficial, stop early.
    '''
    # pwd is ~/
    new_epoch = cur_epoch + 1
    result_file_name = get_add_data_result_file_name(game_number, new_epoch, \
        env_short_name_payoffs)
    if os.path.isfile(result_file_name):
        raise ValueError("Cannot run epoch " + str(cur_epoch) + ": " + \
            result_file_name + " already exists.")

    print("\tWill generate new columns, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)

    # sample payoff of each new network (if it beneficially deviates) against all opponent
    # strategies
    chdir("pythoncode")
    # pwd is ~/pythoncode
    run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
        new_epoch, env_short_name_payoffs, def_model_to_add, att_model_to_add, graph_name)
    # append name of each new network (if it beneficially deviates) to list of networks to
    # use in equilibrium search
    run_append_net_names(env_short_name_payoffs, def_model_to_add, att_model_to_add)

    chdir("..")
    # pwd is ~/
    # add new payoff data to game object (from new beneficially deviating network(s))
    run_add_new_data(game_number, env_short_name_payoffs, new_epoch)

    print("\tShould continue after round: " + str(new_epoch), flush=True)

    cur_epoch += 1
    new_epoch += 1

    result_file_name = get_add_data_result_file_name(game_number, new_epoch, \
        env_short_name_payoffs)
    if os.path.isfile(result_file_name):
        raise ValueError("Cannot run epoch " + str(cur_epoch) + ": " + \
            result_file_name + " already exists.")

    print("\tWill run gambit, epoch: " + str(cur_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # get Nash equilibrium of current strategies
    run_gambit(game_number, cur_epoch, env_short_name_payoffs)
    # create TSV file for current attacker and defender equilibrium strategies
    run_create_tsv(game_number, cur_epoch, env_short_name_tsv, env_short_name_payoffs)

    chdir("pythoncode")
    # pwd is ~/pythoncode
    # set the mixed-strategy opponents to use current TSV file strategies
    run_update_strats(env_short_name_tsv, env_short_name_payoffs, new_epoch)
    print("\tWill get def payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    print("\tWill get att payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each defender strategy vs. new attacker equilibrium
    # sample and estimate mean payoff of each attacker strategy vs. new defender equilibrium
    run_gen_both_payoffs(game_number, env_short_name_payoffs, new_epoch)
    print("\tWill train and test both, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train defender network against current attacker equilibrium, and sample its payoff
    # train attacker network against current defender equilibrium, and sample its payoff
    run_retrain_all(graph_name, env_short_name_payoffs, new_epoch, \
        env_name_vs_mixed_att, env_name_vs_mixed_def, port_lock_name, env_short_name_tsv, \
        max_timesteps_def_init, max_timesteps_def_retrain, max_timesteps_att_init, \
        max_timesteps_att_retrain)

    # check if new defender network is beneficial deviation from old equilibrium
    is_def_beneficial = get_check_if_beneficial(env_short_name_payoffs, new_epoch, True)
    # check if new attacker network is beneficial deviation from old equilibrium
    is_att_beneficial = get_check_if_beneficial(env_short_name_payoffs, new_epoch, False)

    if not is_def_beneficial and not is_att_beneficial:
        # neither network beneficially deviates, so stop
        print("\tConverged after round: " + str(new_epoch), flush=True)
        return False

    chdir("..")
    # pwd is ~/
    is_defender_net = True
    run_test_curve(env_short_name_tsv, env_short_name_payoffs, cur_epoch, \
        old_strat_disc_fact, save_count, graph_name, is_defender_net, runs_per_pair, \
        env_name_vs_mixed_def, env_name_vs_mixed_att)
    is_defender_net = False
    run_test_curve(env_short_name_tsv, env_short_name_payoffs, cur_epoch, \
        old_strat_disc_fact, save_count, graph_name, is_defender_net, runs_per_pair, \
        env_name_vs_mixed_def, env_name_vs_mixed_att)
    return True

def main(game_number, cur_epoch, env_short_name_tsv, env_short_name_payoffs, \
        env_name_def_net, env_name_att_net, env_name_both, graph_name, \
        env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, def_model_to_add, \
        att_model_to_add, old_strat_disc_fact, save_count, runs_per_pair, \
        port_lock_name, max_timesteps_def_init, \
        max_timesteps_def_retrain, max_timesteps_att_init, max_timesteps_att_retrain):
    '''
    Call method to run cur_epoch, and begin next epoch if not converged.
    '''
    if old_strat_disc_fact <= 0.0 or old_strat_disc_fact > 1.0:
        raise ValueError("old_strat_disc_fact must be in (0, 1]: " + \
            str(old_strat_disc_fact))
    if save_count < 1:
        raise ValueError("save_count must be >= 1: " + str(save_count))
    if runs_per_pair < 2:
        raise ValueError("runs_per_pair must be >= 2: " + str(runs_per_pair))
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
        graph_name, env_name_vs_mixed_def, env_name_vs_mixed_att, \
        new_col_count, def_model_to_add, att_model_to_add, \
        old_strat_disc_fact, save_count, runs_per_pair, port_lock_name, \
        max_timesteps_def_init, max_timesteps_def_retrain, max_timesteps_att_init, \
        max_timesteps_att_retrain)
    if should_continue:
        my_epoch += 1
        print("\tShould continue with epoch: " + str(my_epoch) + ", time: " + \
            str(datetime.datetime.now()), flush=True)
    else:
        print("\tConverged at epoch: " + str(my_epoch) + ", time: " + \
            str(datetime.datetime.now()), flush=True)

'''
example: python3 master_dq_runner_curve_continue.py 3013 17 sl29_randNoAndB sl29 \
    DepgraphJava29N-v0 DepgraphJavaEnvAtt29N-v0 DepgraphJavaEnvBoth29N-v0 \
    SepLayerGraph0_noAnd_B.json DepgraphJavaEnvVsMixedDef29N-v0 \
    DepgraphJavaEnvVsMixedAtt29N-v0 400 dg_s29_dq_mlp_rand_epoch17_afterRetrain_r1.pkl \
    None 0.5 4 1000 s29 700000 400000 700000 400000
'''
if __name__ == '__main__':
    if len(sys.argv) != 22:
        raise ValueError("Need 21 args: game_number, cur_epoch, env_short_name_tsv, " + \
            "env_short_name_payoffs, env_name_def_net, env_name_att_net, " + \
            "env_name_both, graph_name, env_name_vs_mixed_def, "  + \
            "env_name_vs_mixed_att, new_col_count, def_model_to_add, att_model_to_add, " + \
            "old_strat_disc_fact, save_count, runs_per_pair, " + \
            "port_lock_name, max_timesteps_def_init, max_timesteps_def_retrain, " + \
            "max_timesteps_att_init, max_timesteps_att_retrain")
    GAME_NUMBER = int(sys.argv[1])
    CUR_EPOCH = int(sys.argv[2])
    ENV_SHORT_NAME_TSV = sys.argv[3]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[4]
    ENV_NAME_DEF_NET = sys.argv[5]
    ENV_NAME_ATT_NET = sys.argv[6]
    ENV_NAME_BOTH = sys.argv[7]
    GRAPH_NAME = sys.argv[8]
    ENV_NAME_VS_MIXED_DEF = sys.argv[9]
    ENV_NAME_VS_MIXED_ATT = sys.argv[10]
    NEW_COL_COUNT = int(sys.argv[11])
    DEF_MODEL_TO_ADD = sys.argv[12]
    if DEF_MODEL_TO_ADD == "None":
        DEF_MODEL_TO_ADD = None
    ATT_MODEL_TO_ADD = sys.argv[13]
    if ATT_MODEL_TO_ADD == "None":
        ATT_MODEL_TO_ADD = None
    OLD_STRAT_DISC_FACT = float(sys.argv[14])
    SAVE_COUNT = int(sys.argv[15])
    RUNS_PER_PAIR = int(sys.argv[16])
    PORT_LOCK_NAME = sys.argv[17]
    MAX_TIMESTEPS_DEF_INIT = int(sys.argv[18])
    MAX_TIMESTEPS_DEF_RETRAIN = int(sys.argv[19])
    MAX_TIMESTEPS_ATT_INIT = int(sys.argv[20])
    MAX_TIMESTEPS_ATT_RETRAIN = int(sys.argv[21])
    main(GAME_NUMBER, CUR_EPOCH, ENV_SHORT_NAME_TSV, ENV_SHORT_NAME_PAYOFFS, \
        ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, GRAPH_NAME, \
        ENV_NAME_VS_MIXED_DEF, ENV_NAME_VS_MIXED_ATT, NEW_COL_COUNT, \
        DEF_MODEL_TO_ADD, ATT_MODEL_TO_ADD, OLD_STRAT_DISC_FACT, SAVE_COUNT, \
        RUNS_PER_PAIR, PORT_LOCK_NAME, MAX_TIMESTEPS_DEF_INIT, MAX_TIMESTEPS_DEF_RETRAIN, \
        MAX_TIMESTEPS_ATT_INIT, MAX_TIMESTEPS_ATT_RETRAIN)
