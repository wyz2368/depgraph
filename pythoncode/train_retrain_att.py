import sys
import subprocess
import time
import os.path

PORT_DIR = "../gym/gym/gym/envs/board_game/"

def get_lines(file_name):
    lines = None
    with open(file_name) as f:
        lines = f.readlines()
    lines = [x.strip() for x in lines]
    lines = [x for x in lines if x]
    return lines

def write_att_port(port_lock_name, is_train, att_port):
    port_name = PORT_DIR + port_lock_name + "_train_att_port.txt"
    if not is_train:
        port_name = PORT_DIR + port_lock_name + "_eval_att_port.txt"
    with open(port_name, 'w') as file:
        file.write(str(att_port) + "\n")

def is_att_unlocked(port_lock_name, is_train):
    lock_name = PORT_DIR + port_lock_name + "_train_att_lock.txt"
    if not is_train:
        lock_name = PORT_DIR + port_lock_name + "_eval_att_lock.txt"
    lines = get_lines(lock_name)
    return int(lines[0]) == 0

def lock_att(port_lock_name, is_train):
    if not is_att_unlocked(port_lock_name, is_train):
        raise ValueError("Invalid state")
    lock_name = PORT_DIR + port_lock_name + "_train_att_lock.txt"
    if not is_train:
        lock_name = PORT_DIR + port_lock_name + "_eval_att_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("1\n")

def wait_for_att_lock(port_lock_name, is_train):
    sleep_time = 5
    while not is_att_unlocked(port_lock_name, is_train):
        time.sleep(sleep_time)

def start_and_return_env_process(graph_name, att_port):
    cmd = "exec java -jar ../depgraphpy4jattvseither/depgraphpy4jattvsnetorheuristic.jar " \
        + graph_name + " " + str(att_port)
    env_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)
    return env_process

def close_env_process(env_process):
    sleep_sec = 5
    time.sleep(sleep_sec)
    env_process.kill()

def run_train_retrain(env_short_name, new_epoch, env_name_def_net, att_port, \
    port_lock_name, env_short_name_tsv, max_timesteps_att_init, max_timesteps_att_retrain):
    cmd_list = ["python3", "train_dg_java_mlp_att_and_retrain.py", env_name_def_net, \
        env_short_name, str(new_epoch), str(att_port), str(port_lock_name), \
        env_short_name_tsv, str(max_timesteps_att_init), str(max_timesteps_att_retrain)]
    att_out_name = "attVMixed_" + env_short_name + "_epoch" + str(new_epoch) + ".txt"
    if os.path.isfile(att_out_name):
        print("Skipping: " + att_out_name + " already exists.")
        return
    with open(att_out_name, "w") as file:
        subprocess.call(cmd_list, stdout=file)

def main(graph_name, env_short_name, new_epoch, env_name_def_net, def_port, \
    port_lock_name, env_short_name_tsv, max_timesteps_att_init, max_timesteps_att_retrain):
    att_port = def_port + 2
    env_process = start_and_return_env_process(graph_name, att_port)

    is_train = True
    wait_for_att_lock(port_lock_name, is_train)
    lock_att(port_lock_name, is_train)

    write_att_port(port_lock_name, is_train, att_port)
    run_train_retrain(env_short_name, new_epoch, env_name_def_net, att_port, \
        port_lock_name, env_short_name_tsv, max_timesteps_att_init, \
        max_timesteps_att_retrain)

    close_env_process(env_process)

'''
example: python3 train_test_att.py SepLayerGraph0_noAnd_B.json sl29 16 \
    DepgraphJavaEnvVsMixedDef29N-v0 25333 s29 sl29_randNoAndB 700000 700000
'''
if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: graph_name, env_short_name, new_epoch, " + \
            "env_name_def_net, def_port, port_lock_name, env_short_name_tsv, " + \
            "max_timesteps_att_init, max_timesteps_att_retrain")
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_DEF_NET = sys.argv[4]
    DEF_PORT = int(sys.argv[5])
    PORT_LOCK_NAME = sys.argv[6]
    ENV_SHORT_NAME_TSV = sys.argv[7]
    MAX_TIMESTEPS_ATT_INIT = int(sys.argv[8])
    MAX_TIMESTEPS_ATT_CONTINUE = int(sys.argv[9])
    main(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_DEF_NET, DEF_PORT, \
        PORT_LOCK_NAME, ENV_SHORT_NAME_TSV, MAX_TIMESTEPS_ATT_INIT, \
        MAX_TIMESTEPS_ATT_CONTINUE)
