import sys
import subprocess
import time
import os.path

MIN_PORT = 25333
MAX_PORT = 26333
PORTS_PER_ROUND = 4
PORT_DIR = "../gym/gym/gym/envs/board_game/"

def get_lines(file_name):
    lines = None
    with open(file_name) as f:
        lines = f.readlines()
    lines = [x.strip() for x in lines]
    lines = [x for x in lines if x]
    return lines

def read_def_port(port_lock_name, is_train):
    port_name = PORT_DIR + port_lock_name + "_train_def_port.txt"
    if not is_train:
        port_name = PORT_DIR + port_lock_name + "_eval_def_port.txt"
    lines = get_lines(port_name)
    port = int(lines[0])
    if port < MIN_PORT or port % 2 != 1:
        raise ValueError("Invalid def port: " + str(port))
    return port

def write_def_port(port_lock_name, is_train, def_port):
    port_name = PORT_DIR + port_lock_name + "_train_def_port.txt"
    if not is_train:
        port_name = PORT_DIR + port_lock_name + "_eval_def_port.txt"
    with open(port_name, 'w') as file:
        file.write(str(def_port) + "\n")

def is_def_unlocked(port_lock_name, is_train):
    lock_name = PORT_DIR + port_lock_name + "_train_def_lock.txt"
    if not is_train:
        lock_name = PORT_DIR + port_lock_name + "_eval_def_lock.txt"
    lines = get_lines(lock_name)
    return int(lines[0]) == 0

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

def lock_def(port_lock_name, is_train):
    if not is_def_unlocked(port_lock_name, is_train):
        raise ValueError("Invalid state")
    lock_name = PORT_DIR + port_lock_name + "_train_def_lock.txt"
    if not is_train:
        lock_name = PORT_DIR + port_lock_name + "_eval_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("1\n")

def lock_att(port_lock_name, is_train):
    if not is_att_unlocked(port_lock_name, is_train):
        raise ValueError("Invalid state")
    lock_name = PORT_DIR + port_lock_name + "_train_att_lock.txt"
    if not is_train:
        lock_name = PORT_DIR + port_lock_name + "_eval_att_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("1\n")

def unlock_train_def(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_train_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def unlock_eval_def(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_eval_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def unlock_train_att(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_train_att_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def unlock_eval_att(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_eval_att_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def wait_for_def_lock(port_lock_name, is_train):
    sleep_time = 5
    while not is_def_unlocked(port_lock_name, is_train):
        time.sleep(sleep_time)

def wait_for_att_lock(port_lock_name, is_train):
    sleep_time = 5
    while not is_att_unlocked(port_lock_name, is_train):
        time.sleep(sleep_time)

def start_env_process_att(graph_name, def_port):
    cmd_list = ["java", "-jar",  \
        "depgraphpy4jattvseither/depgraphpy4jattvsnetorheuristic.jar", \
        graph_name, str(def_port)]

    env_process = subprocess.Popen(cmd_list, stdin=None, stdout=None, stderr=None, \
        close_fds=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)
    return env_process

def start_env_process_def(graph_name, def_port):
    cmd_list = ["java", "-jar",  \
        "depgraphpy4jdefvseither/depgraphpy4jdefvsnetorheuristic.jar", \
        graph_name, str(def_port)]

    env_process = subprocess.Popen(cmd_list, stdin=None, stdout=None, stderr=None, \
        close_fds=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)
    return env_process

def close_env_process(env_process):
    sleep_sec = 5
    time.sleep(sleep_sec)
    env_process.kill()

def run_training_def(env_short_name, new_epoch, env_name_vs_att, def_port, port_lock_name, \
    env_short_name_tsv, max_timesteps_def):
    cmd_list = ["python3", "train_dg_java_mlp_def_vs_mixed.py", env_name_vs_att, \
        env_short_name, str(new_epoch), str(def_port), str(port_lock_name), \
        env_short_name_tsv, str(max_timesteps_def)]
    def_out_name = "defVMixed_" + env_short_name + "_epoch" + str(new_epoch) + ".txt"
    if os.path.isfile(def_out_name):
        print("Skipping: " + def_out_name + " already exists.")
        unlock_train_def(port_lock_name)
        return None

    def_process = None
    my_file = open(def_out_name, "w")
    def_process = subprocess.Popen(cmd_list, stdout=my_file, stderr=subprocess.STDOUT)
    return def_process, my_file

def run_training_att(env_short_name, new_epoch, env_name_vs_def, att_port, port_lock_name, \
    env_short_name_tsv, max_timesteps_att):
    cmd_list = ["python3", "train_dg_java_mlp_att_vs_mixed.py", env_name_vs_def, \
        env_short_name, str(new_epoch), str(att_port), str(port_lock_name), \
        env_short_name_tsv, str(max_timesteps_att)]
    att_out_name = "attVMixed_" + env_short_name + "_epoch" + str(new_epoch) + ".txt"
    if os.path.isfile(att_out_name):
        print("Skipping: " + att_out_name + " already exists.")
        unlock_train_att(port_lock_name)
        return None

    att_process = None
    my_file = open(att_out_name, "w")
    att_process = subprocess.Popen(cmd_list, stdout=my_file, stderr=subprocess.STDOUT)
    return att_process, my_file

def run_evaluation_def(env_short_name, new_epoch, env_name_vs_att, def_port, \
    port_lock_name, env_short_name_tsv):
    cmd_list = ["python3", "enjoy_depgraph_data_vs_mixed.py", env_name_vs_att, \
        env_short_name, str(new_epoch), str(def_port), str(port_lock_name), \
        env_short_name_tsv]
    def_out_name_enj = "def_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
        "_enj.txt"
    if os.path.isfile(def_out_name_enj):
        print("Skipping: " + def_out_name_enj + " already exists.")
        unlock_eval_def(port_lock_name)
        return None

    def_process = None
    my_file = open(def_out_name_enj, "w")
    def_process = subprocess.Popen(cmd_list, stdout=my_file, stderr=subprocess.STDOUT)
    return def_process, my_file

def run_evaluation_att(env_short_name, new_epoch, env_name_vs_def, att_port, \
    port_lock_name, env_short_name_tsv):
    cmd_list = ["python3", "enjoy_dg_data_vs_mixed_def.py", env_name_vs_def, \
        env_short_name, str(new_epoch), str(att_port), str(port_lock_name), \
        env_short_name_tsv]
    att_out_name_enj = "att_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
        "_enj.txt"
    if os.path.isfile(att_out_name_enj):
        print("Skipping: " + att_out_name_enj + " already exists.")
        unlock_eval_att(port_lock_name)
        return None

    att_process = None
    my_file = open(att_out_name_enj, "w")
    att_process = subprocess.Popen(cmd_list, stdout=my_file, stderr=subprocess.STDOUT)
    return att_process, my_file

def run_both(graph_name, env_short_name, new_epoch, env_name_vs_att, env_name_vs_def, \
    port_lock_name, def_port, env_short_name_tsv, max_timesteps_def, max_timesteps_att):

    ### Setup

    env_process_def = start_env_process_def(graph_name, def_port)
    att_port = def_port + 2
    env_process_att = start_env_process_att(graph_name, att_port)

    ### Training

    is_train = True

    write_def_port(port_lock_name, is_train, def_port)
    def_process, def_file = run_training_def(env_short_name, new_epoch, env_name_vs_att, \
        def_port, port_lock_name, env_short_name_tsv, max_timesteps_def)

    wait_for_att_lock(port_lock_name, is_train)
    lock_att(port_lock_name, is_train)
    write_att_port(port_lock_name, is_train, att_port)
    att_process, att_file = run_training_att(env_short_name, new_epoch, env_name_vs_def, \
        att_port, port_lock_name, env_short_name_tsv, max_timesteps_att)

    if def_process is not None:
        def_process.communicate()
        def_file.close()
    if att_process is not None:
        att_process.communicate()
        att_file.close()

    ### Evaluation

    is_train = False

    wait_for_def_lock(port_lock_name, is_train)
    lock_def(port_lock_name, is_train)
    write_def_port(port_lock_name, is_train, def_port)
    def_process_enj, def_file_enj = run_evaluation_def(env_short_name, new_epoch, \
        env_name_vs_att, def_port, port_lock_name, env_short_name_tsv)

    wait_for_att_lock(port_lock_name, is_train)
    lock_att(port_lock_name, is_train)
    write_att_port(port_lock_name, is_train, att_port)
    att_process_enj, att_file_enj = run_evaluation_att(env_short_name, new_epoch, \
        env_name_vs_def, att_port, port_lock_name, env_short_name_tsv)

    if def_process_enj is not None:
        def_process_enj.communicate()
        def_file_enj.close()
    if att_process_enj is not None:
        att_process_enj.communicate()
        att_file_enj.close()

    ### Takedown

    print("Closing env_process for defender and attacker")
    close_env_process(env_process_def)
    close_env_process(env_process_att)
    print("Finished defender train and test")

'''
example: python3 train_test_both.py SepLayerGraph0_noAnd_B.json sl29 16 \
    DepgraphJavaEnvVsMixedAtt29N-v0 DepgraphJavaEnvVsMixedDef29N-v0 s29 25333 \
    sl29_randNoAndB 700000 700000
'''
if __name__ == '__main__':
    if len(sys.argv) != 11:
        raise ValueError("Need 10 args: graph_name, env_short_name, new_epoch, " + \
            "env_name_vs_att, env_name_vs_def, port_lock_name, def_port " + \
            "env_short_name_tsv, max_timesteps_def, max_timesteps_att")
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_VS_ATT = sys.argv[4]
    ENV_NAME_VS_DEF = sys.argv[5]
    PORT_LOCK_NAME = sys.argv[6]
    DEF_PORT = int(sys.argv[7])
    ENV_SHORT_NAME_TSV = sys.argv[8]
    MAX_TIMESTEPS_DEF = int(sys.argv[9])
    MAX_TIMESTEPS_ATT = int(sys.argv[10])
    run_both(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_VS_ATT, ENV_NAME_VS_DEF, \
        PORT_LOCK_NAME, DEF_PORT, ENV_SHORT_NAME_TSV, MAX_TIMESTEPS_DEF, MAX_TIMESTEPS_ATT)
