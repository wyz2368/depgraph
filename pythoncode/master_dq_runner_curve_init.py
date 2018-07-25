'''
Run the first epoch and part of second epoch of training for robustified double-oracle
method, where each successful training session is followed by fine-tuning against a mixture
of previous opponent equilibria for robustness.
Runs the second epoch until after fine-tuning, when a human must assist by deciding which
network (the trained one or a fine-tuned version) to proceed with for attacker and defender.
'''
import sys
import datetime
import subprocess
from os import chdir
import os.path
import pythoncode.check_if_beneficial as check
import pythoncode.select_best_curve as select
from pythoncode.train_test_def import wait_for_def_lock, lock_def, read_def_port, \
    PORTS_PER_ROUND, MAX_PORT, MIN_PORT
from add_new_data import get_add_data_result_file_name

RETRAIN_MIN_WEIGHT = 0.001

def run_gambit(game_number, cur_epoch, env_short_name_payoffs):
    '''
    Call a script to find a Nash equilibrium of the current game.
    '''
    cmd_list = ["python3", "gambit_analyze.py", str(game_number), str(cur_epoch), \
        env_short_name_payoffs]
    subprocess.call(cmd_list)

def run_create_tsv_curve(game_number, cur_epoch, env_short_name_tsv, \
    env_short_name_payoffs):
    '''
    Run a script to extract the current equilibrium strategies to TSV files.
    '''
    cmd_list = ["python3", "create_tsv_files_curve.py", str(game_number), \
        str(cur_epoch), env_short_name_tsv, env_short_name_payoffs]
    subprocess.call(cmd_list)

def run_create_retrain_strat(old_strat_disc_fact, env_short_name_payoffs):
    is_defender_items = [True, False]
    for is_defender in is_defender_items:
        tsv_names_file = "def_strat_files_" + env_short_name_payoffs + ".txt"
        if not is_defender:
            tsv_names_file = "att_strat_files_" + env_short_name_payoffs + ".txt"
        cmd_list = ["python3", "create_weighted_mixed_strat.py", \
            str(old_strat_disc_fact), str(RETRAIN_MIN_WEIGHT), str(is_defender), \
            tsv_names_file, env_short_name_payoffs]
        subprocess.call(cmd_list)

def run_update_strats(env_short_name_tsv, port_lock_name, new_epoch):
    '''
    Run a script to update the config files for the Gym environments to reference the new
    equilibrium strategies' TSV files.
    '''
    cmd_list = ["python3", "update_opponent_strats.py", port_lock_name, \
        env_short_name_tsv, str(new_epoch)]
    subprocess.call(cmd_list)

def run_gen_both_payoffs(game_number, env_short_name_payoffs, new_epoch):
    cmd_list = ["python3", "gen_both_payoffs_from_game.py", str(game_number), \
        env_short_name_payoffs, str(new_epoch)]
    subprocess.call(cmd_list)

def run_train_test_all(graph_name, env_short_name_payoffs, new_epoch, \
    env_name_vs_mixed_att, env_name_vs_mixed_def, port_lock_name, env_short_name_tsv, \
    max_timesteps_def, max_timesteps_att):
    is_train = True
    wait_for_def_lock(port_lock_name, is_train)
    lock_def(port_lock_name, is_train)
    def_port = read_def_port(port_lock_name, is_train)
    def_port += PORTS_PER_ROUND
    if def_port >= MAX_PORT:
        def_port = MIN_PORT

    cmd_list_train_def = ["python3", "train_test_def.py", graph_name, \
        env_short_name_payoffs, str(new_epoch), env_name_vs_mixed_att, port_lock_name, \
        str(def_port), env_short_name_tsv, str(max_timesteps_def)]
    cmd_list_train_att = ["python3", "train_test_att.py", graph_name, \
        env_short_name_payoffs, str(new_epoch), env_name_vs_mixed_def, str(def_port), \
        port_lock_name, env_short_name_tsv, str(max_timesteps_att)]
    process_train_def = subprocess.Popen(cmd_list_train_def)
    process_train_att = subprocess.Popen(cmd_list_train_att)

    process_train_def.wait()
    process_train_att.wait()

def run_train_retrain_all(graph_name, env_short_name_payoffs, new_epoch, \
    env_name_vs_mixed_att, env_name_vs_mixed_def, port_lock_name, env_short_name_tsv, \
    max_timesteps_def_init, max_timesteps_def_retrain, max_timesteps_att_init, \
    max_timesteps_att_retrain, save_count):
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
        str(max_timesteps_def_retrain), str(save_count)]
    cmd_list_train_att = ["python3", "train_retrain_att.py", graph_name, \
        env_short_name_payoffs, str(new_epoch), env_name_vs_mixed_def, str(def_port), \
        port_lock_name, env_short_name_tsv, str(max_timesteps_att_init), \
        str(max_timesteps_att_retrain), str(save_count)]
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

def get_select_best_curve(env_short_name_payoffs, new_epoch, is_def, save_count):
    '''
    Get the best of the trained or retrained strategy numbers, or None if none is a
    beneficial deviation from current equilibrium.
    '''
    return select.get_best_retrain_number(env_short_name_payoffs, new_epoch, is_def, \
        save_count)

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

def get_def_model_name(env_short_name_payoffs, new_epoch, retrain_index):
    ''' Get name of the defender network to generate. '''
    if new_epoch == 1 or retrain_index == 0:
        return "dg_" + env_short_name_payoffs + "_dq_mlp_rand_epoch" + str(new_epoch) + \
            ".pkl"
    prefix_for_save = "dg_" + env_short_name_payoffs + "_dq_mlp_retrain_epoch" + \
        str(new_epoch)
    return prefix_for_save + "_r" + str(retrain_index) + ".pkl"

def get_att_model_name(env_short_name_payoffs, new_epoch, retrain_index):
    ''' Get name of the attacker network to generate. '''
    if new_epoch == 1 or retrain_index == 0:
        return "dg_" + env_short_name_payoffs + "_dq_mlp_rand_epoch" + str(new_epoch) + \
            "_att.pkl"
    prefix_for_save = "dg_" + env_short_name_payoffs + "_dq_mlp_retrain_epoch" + \
        str(new_epoch) + "_att"
    return prefix_for_save + "_r" + str(retrain_index) + ".pkl"

def run_init_epoch(game_number, env_short_name_tsv, env_short_name_payoffs, \
    env_name_def_net, env_name_att_net, env_name_both, graph_name, \
    env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, old_strat_disc_fact, \
    save_count, port_lock_name, max_timesteps_def_init, \
    max_timesteps_def_retrain, max_timesteps_att_init, max_timesteps_att_retrain):
    '''
    Run the first epoch (epoch 0) of training. If no beneficial deviation is found, stop.
    Otherwise, begin the next epoch (epoch 1) of training, and stop after the attacker and
    defender nets have been trained, fine-tuned against the previous equilibrium, and the
    trained and fine-tuned nets have been evaluated against the current and previous
    equilibria. (The human can then choose which attacker and defender nets to add to the
    game.)
    '''
    cur_epoch = 0
    new_epoch = 1
    # pwd is ~/
    result_file_name = get_add_data_result_file_name(game_number, new_epoch + 1, \
        env_short_name_payoffs)
    if os.path.isfile(result_file_name):
        raise ValueError("Cannot run init: " + result_file_name + " already exists.")

    print("\tWill run gambit, epoch: " + str(cur_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # get Nash equilibrium of current strategies
    run_gambit(game_number, cur_epoch, env_short_name_payoffs)
    # create TSV file for current attacker and defender equilibrium strategies
    run_create_tsv_curve(game_number, cur_epoch, env_short_name_tsv, \
        env_short_name_payoffs)

    chdir("pythoncode")
    # pwd is ~/pythoncode
    # set the mixed-strategy opponents to use current TSV file strategies
    run_update_strats(env_short_name_tsv, port_lock_name, new_epoch)
    print("\tWill get def payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    print("\tWill get att payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each defender strategy
    # vs. new attacker equilibrium
    # sample and estimate mean payoff of each attacker strategy
    # vs. new defender equilibrium
    run_gen_both_payoffs(game_number, env_short_name_payoffs, new_epoch)
    print("\tWill train and test both, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train attacker network against current defender equilibrium and mix of recents
    # train defender network against current attacker equilibrium and mix of recents
    run_train_test_all(graph_name, env_short_name_payoffs, new_epoch, \
        env_name_vs_mixed_att, env_name_vs_mixed_def, port_lock_name, env_short_name_tsv, \
        max_timesteps_def_init, max_timesteps_att_init, save_count)

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
    def_model_to_add = None
    if is_def_beneficial:
        def_model_to_add = get_def_model_name(env_short_name_payoffs, new_epoch, 0)
    att_model_to_add = None
    if is_att_beneficial:
        att_model_to_add = get_att_model_name(env_short_name_payoffs, new_epoch, 0)
    run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
        new_epoch, env_short_name_payoffs, def_model_to_add, att_model_to_add, graph_name)
    # append name of each new network (if it beneficially deviates) to list of networks to
    # use in equilibrium search
    run_append_net_names(env_short_name_payoffs, def_model_to_add, att_model_to_add)

    chdir("..")
    # pwd is ~/
    # add new payoff data to game object (from new beneficially deviating network(s))
    run_add_new_data(game_number, env_short_name_payoffs, new_epoch)

    print("\tWill continue after round: " + str(new_epoch), flush=True)

    cur_epoch += 1
    new_epoch += 1

    print("\tWill run gambit, epoch: " + str(cur_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # get Nash equilibrium of current strategies
    run_gambit(game_number, cur_epoch, env_short_name_payoffs)
    # create TSV file for current attacker and defender equilibrium strategies
    run_create_tsv_curve(game_number, cur_epoch, env_short_name_tsv, \
        env_short_name_payoffs)

    chdir("pythoncode")
    # pwd is ~/pythoncode
    # set the mixed-strategy opponents to use current TSV file strategies
    run_update_strats(env_short_name_tsv, env_short_name_payoffs, new_epoch)
    print("\tWill get def payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    print("\tWill get att payoffs, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # sample and estimate mean payoff of each defender strategy
    # vs. new attacker equilibrium
    # sample and estimate mean payoff of each attacker strategy
    # vs. new defender equilibrium
    run_gen_both_payoffs(game_number, env_short_name_payoffs, new_epoch)

    run_create_retrain_strat(old_strat_disc_fact, env_short_name_payoffs)

    print("\tWill train and test both, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # train attacker network against current defender equilibrium and mix of recents
    # train defender network against current attacker equilibrium and mix of recents
    run_train_retrain_all(graph_name, env_short_name_payoffs, new_epoch, \
        env_name_vs_mixed_att, env_name_vs_mixed_def, port_lock_name, env_short_name_tsv, \
        max_timesteps_def_init, max_timesteps_def_retrain, max_timesteps_att_init, \
        max_timesteps_att_retrain, save_count)

    # call select_best_curve.py for attacker and defender
    best_def_index = get_select_best_curve(env_short_name_payoffs, new_epoch, True, \
        save_count)
    best_att_index = get_select_best_curve(env_short_name_payoffs, new_epoch, False, \
        save_count)
    if best_def_index is None and best_att_index is None:
        # neither network beneficially deviates, so stop
        print("\tConverged after round: " + str(new_epoch), flush=True)
        return False

    print("\tWill generate new columns, epoch: " + str(new_epoch)+ ", time: " + \
        str(datetime.datetime.now()), flush=True)

    def_model_to_add = None
    att_model_to_add = None
    if best_def_index is not None:
        def_model_to_add = get_def_model_name(env_short_name_payoffs, new_epoch, \
            best_def_index)
    if best_att_index is not None:
        att_model_to_add = get_def_model_name(env_short_name_payoffs, new_epoch, \
            best_att_index)
    run_gen_new_cols(env_name_def_net, env_name_att_net, env_name_both, new_col_count, \
        new_epoch, env_short_name_payoffs, def_model_to_add, att_model_to_add, graph_name)
    return True

def main(game_number, env_short_name_tsv, env_short_name_payoffs, \
        env_name_def_net, env_name_att_net, \
        env_name_both, graph_name, \
        env_name_vs_mixed_def, env_name_vs_mixed_att, new_col_count, \
        old_strat_disc_fact, save_count, port_lock_name, max_timesteps_def_init, \
        max_timesteps_def_retrain, max_timesteps_att_init, max_timesteps_att_retrain):
    '''
    Call method to run first epoch (epoch 0), and beginning of second epoch (epoch 1).
    '''
    if old_strat_disc_fact <= 0.0 or old_strat_disc_fact > 1.0:
        raise ValueError("old_strat_disc_fact must be in (0, 1]: " + \
            str(old_strat_disc_fact))
    if save_count < 1:
        raise ValueError("save_count must be >= 1: " + str(save_count))

    # first epoch is always 0
    my_epoch = 0
    print("\tWill run epoch: " + str(my_epoch) + ", time: " + \
        str(datetime.datetime.now()), flush=True)
    # indicates whether a beneficial deviation was found
    should_continue = run_init_epoch(game_number, env_short_name_tsv, \
        env_short_name_payoffs, env_name_def_net, env_name_att_net, env_name_both, \
        graph_name, env_name_vs_mixed_def, env_name_vs_mixed_att, \
        new_col_count, old_strat_disc_fact, save_count, \
        port_lock_name, max_timesteps_def_init, max_timesteps_def_retrain, \
        max_timesteps_att_init, max_timesteps_att_retrain)
    if should_continue:
        # proceed from epoch 1
        print("\tShould continue from epoch: 1, time: " + \
            str(datetime.datetime.now()), flush=True)
    else:
        # did not find beneficial deviation from initial equilibrium
        print("\tConverged in epoch 0, time: " + str(datetime.datetime.now()), flush=True)

'''
example: python3 master_dq_runner_curve_init.py 3013 sl29_randNoAndB sl29 \
    DepgraphJava29N-v0 DepgraphJavaEnvAtt29N-v0 DepgraphJavaEnvBoth29N-v0 \
    SepLayerGraph0_noAnd_B.json DepgraphJavaEnvVsMixedDef29N-v0 \
    DepgraphJavaEnvVsMixedAtt29N-v0 0.7 4 1000 s29 700000 400000 700000 400000

'''
if __name__ == '__main__':
    if len(sys.argv) != 18:
        raise ValueError("Need 17 args: game_number, env_short_name_tsv, " + \
            "env_short_name_payoffs, env_name_def_net, env_name_att_net, " + \
            "env_name_both, graph_name, env_name_vs_mixed_def, "  + \
            "env_name_vs_mixed_att, new_col_count, " + \
            "old_strat_disc_fact, save_count, " + \
            "port_lock_name, max_timesteps_def_init, max_timesteps_def_retrain, " + \
            "max_timesteps_att_init, max_timesteps_att_retrain")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_TSV = sys.argv[2]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[3]
    ENV_NAME_DEF_NET = sys.argv[4]
    ENV_NAME_ATT_NET = sys.argv[5]
    ENV_NAME_BOTH = sys.argv[6]
    GRAPH_NAME = sys.argv[7]
    ENV_NAME_VS_MIXED_DEF = sys.argv[8]
    ENV_NAME_VS_MIXED_ATT = sys.argv[9]
    NEW_COL_COUNT = int(sys.argv[10])
    OLD_STRAT_DISC_FACT = float(sys.argv[11])
    SAVE_COUNT = int(sys.argv[12])
    PORT_LOCK_NAME = sys.argv[13]
    MAX_TIMESTEPS_DEF_INIT = int(sys.argv[14])
    MAX_TIMESTEPS_DEF_RETRAIN = int(sys.argv[15])
    MAX_TIMESTEPS_ATT_INIT = int(sys.argv[16])
    MAX_TIMESTEPS_ATT_RETRAIN = int(sys.argv[17])
    main(GAME_NUMBER, ENV_SHORT_NAME_TSV, ENV_SHORT_NAME_PAYOFFS, \
        ENV_NAME_DEF_NET, ENV_NAME_ATT_NET, ENV_NAME_BOTH, GRAPH_NAME, \
        ENV_NAME_VS_MIXED_DEF, ENV_NAME_VS_MIXED_ATT, NEW_COL_COUNT, \
        OLD_STRAT_DISC_FACT, SAVE_COUNT, PORT_LOCK_NAME, \
        MAX_TIMESTEPS_DEF_INIT, MAX_TIMESTEPS_DEF_RETRAIN, MAX_TIMESTEPS_ATT_INIT, \
        MAX_TIMESTEPS_ATT_RETRAIN)
