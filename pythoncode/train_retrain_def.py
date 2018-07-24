import sys
import subprocess
import time
import os.path

PORT_DIR = "../gym/gym/gym/envs/board_game/"

def write_def_port(port_lock_name, is_train, def_port):
    port_name = PORT_DIR + port_lock_name + "_train_def_port.txt"
    if not is_train:
        port_name = PORT_DIR + port_lock_name + "_eval_def_port.txt"
    with open(port_name, 'w') as file:
        file.write(str(def_port) + "\n")

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

def run_train_retrain(env_short_name, new_epoch, env_name_att_net, def_port, \
    port_lock_name, env_short_name_tsv, max_timesteps_def_init, max_timesteps_def_retrain):
    cmd_list = ["python3", "train_dg_java_mlp_def_and_retrain.py", env_name_att_net, \
        env_short_name, str(new_epoch), str(def_port), str(port_lock_name), \
        env_short_name_tsv, str(max_timesteps_def_init), str(max_timesteps_def_retrain)]
# env_name, env_short_name, new_epoch, def_port, port_lock_name, env_short_name_tsv, \
#    max_timesteps_def_init, max_timesteps_def_retrain
    def_out_name = "defVMixed_" + env_short_name + "_epoch" + str(new_epoch) + ".txt"
    if os.path.isfile(def_out_name):
        print("Skipping: " + def_out_name + " already exists.")
        return
    with open(def_out_name, "w") as file:
        subprocess.call(cmd_list, stdout=file)

def main(graph_name, env_short_name, new_epoch, env_name_att_net, port_lock_name, \
    def_port, env_short_name_tsv, max_timesteps_def_init, max_timesteps_def_retrain):
    env_process = start_and_return_env_process(graph_name, def_port)
    is_train = True
    write_def_port(port_lock_name, is_train, def_port)
    run_train_retrain(env_short_name, new_epoch, env_name_att_net, def_port, \
        port_lock_name, env_short_name_tsv, max_timesteps_def_init, \
        max_timesteps_def_retrain)

    close_env_process(env_process)

'''
example: python3 train_test_def.py SepLayerGraph0_noAnd_B.json sl29 16 \
    DepgraphJavaEnvVsMixedAtt29N-v0 s29 25333 sl29_randNoAndB 700000 400000
requires local files:
<port_lock_name>_train_def_lock.txt
<port_lock_name>_eval_def_lock.txt
<port_lock_name>_train_def_port.txt
<port_lock_name>_eval_def_port.txt
'''
if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: graph_name, env_short_name, new_epoch, " + \
            "env_name_att_net, port_lock_name, def_port, env_short_name_tsv, " + \
            "max_timesteps_def_init, max_timesteps_def_retrain")
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_ATT_NET = sys.argv[4]
    PORT_LOCK_NAME = sys.argv[5]
    DEF_PORT = int(sys.argv[6])
    ENV_SHORT_NAME_TSV = sys.argv[7]
    MAX_TIMESTEPS_DEF_INIT = int(sys.argv[8])
    MAX_TIMESTEPS_DEF_RETRAIN = int(sys.argv[9])
    main(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_ATT_NET, PORT_LOCK_NAME, \
        DEF_PORT, ENV_SHORT_NAME_TSV, MAX_TIMESTEPS_DEF_INIT, MAX_TIMESTEPS_DEF_RETRAIN)
