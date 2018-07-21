import sys
import subprocess
import time
import os.path

MIN_PORT = 25333
MAX_PORT = 26333
PORTS_PER_ROUND = 4

def get_lines(file_name):
    lines = None
    with open(file_name) as f:
        lines = f.readlines()
    lines = [x.strip() for x in lines]
    lines = [x for x in lines if x]
    return lines

def read_def_port(env_short_name, is_train):
    port_name = env_short_name + "_train_def_port.txt"
    if not is_train:
        port_name = env_short_name + "_eval_def_port.txt"
    lines = get_lines(port_name)
    port = int(lines[0])
    if port < MIN_PORT or port % 2 != 1:
        raise ValueError("Invalid def port: " + str(port))
    return port

def write_def_port(env_short_name, is_train, def_port):
    port_name = env_short_name + "_train_def_port.txt"
    if not is_train:
        port_name = env_short_name + "_eval_def_port.txt"
    with open(port_name, 'w') as file:
        file.write(str(def_port) + "\n")

def is_def_unlocked(env_short_name, is_train):
    lock_name = env_short_name + "_train_def_lock.txt"
    if not is_train:
        lock_name = env_short_name + "_eval_def_lock.txt"
    lines = get_lines(lock_name)
    return int(lines[0]) == 0

def lock_def(env_short_name, is_train):
    if not is_def_unlocked(env_short_name, is_train):
        raise ValueError("Invalid state")
    lock_name = env_short_name + "_train_def_lock.txt"
    if not is_train:
        lock_name = env_short_name + "_eval_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("1\n")

def wait_for_def_lock(env_short_name, is_train):
    sleep_time = 5
    while not is_def_unlocked(env_short_name, is_train):
        time.sleep(sleep_time)

def start_and_return_env_process(graph_name, def_port):
    cmd = "exec java -jar ../depgraphpy4jdefvseither/depgraphpy4jdefvsnetorheuristic.jar " \
        + graph_name + " " + str(def_port)
    env_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)
    return env_process

def close_env_process(env_process):
    sleep_sec = 5
    time.sleep(sleep_sec)
    env_process.kill()

def run_training(env_short_name, new_epoch, env_name_att_net, def_port):
    cmd_list = ["python3", "train_dg_java_mlp_def_vs_mixed.py", env_name_att_net, \
        env_short_name, str(new_epoch), str(def_port)]
    def_out_name = "defVMixed_" + env_short_name + "_epoch" + str(new_epoch) + ".txt"
    if os.path.isfile(def_out_name):
        print("Skipping: " + def_out_name + " already exists.")
        return
    with open(def_out_name, "w") as file:
        subprocess.call(cmd_list, stdout=file)

def run_evaluation(env_short_name, new_epoch, env_name_att_net, def_port):
    cmd_list = ["python3", "enjoy_depgraph_data_vs_mixed.py", env_name_att_net, \
        env_short_name, str(new_epoch), str(def_port)]
    def_out_name_enj = "def_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
        "_enj.txt"
    if os.path.isfile(def_out_name_enj):
        print("Skipping: " + def_out_name_enj + " already exists.")
        return
    with open(def_out_name_enj, "w") as file:
        subprocess.call(cmd_list, stdout=file)

def main(graph_name, env_short_name, new_epoch, env_name_att_net):
    is_train = True
    wait_for_def_lock(env_short_name, is_train)
    lock_def(env_short_name, is_train)

    def_port = read_def_port(env_short_name, is_train)
    def_port += PORTS_PER_ROUND
    if def_port >= MAX_PORT:
        def_port = MIN_PORT
    env_process = start_and_return_env_process(graph_name, def_port)

    write_def_port(env_short_name, is_train, def_port)
    run_training(env_short_name, new_epoch, env_name_att_net, def_port)

    is_train = False
    wait_for_def_lock(env_short_name, is_train)
    lock_def(env_short_name, is_train)
    write_def_port(env_short_name, is_train, def_port)
    run_evaluation(env_short_name, new_epoch, env_name_att_net, def_port)
    close_env_process(env_process)

'''
example: python3 train_test_def.py SepLayerGraph0_noAnd_B.json sl29 16 \
    DepgraphJavaEnvVsMixedAtt29N-v0
requires local files:
<env_short_name>_train_def_lock.txt
<env_short_name>_eval_def_lock.txt
<env_short_name>_train_def_port.txt
<env_short_name>_eval_def_port.txt
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: graph_name, env_short_name, new_epoch, " + \
            "env_name_att_net")
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_ATT_NET = sys.argv[4]
    main(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_ATT_NET)
