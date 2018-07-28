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

def run_evaluation_def(env_short_name, new_epoch, env_name_vs_att, def_port, \
    env_short_name_tsv):
    cmd_list = ["python3", "enjoy_depgraph_data_vs_mixed_short.py", env_name_vs_att, \
        env_short_name, str(new_epoch), str(def_port), env_short_name_tsv]
    def_out_name_enj = "def_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
        "_enj.txt"
    if os.path.isfile(def_out_name_enj):
        print("Skipping: " + def_out_name_enj + " already exists.")
        return None, None

    def_process = None
    my_file = open(def_out_name_enj, "w")
    def_process = subprocess.Popen(cmd_list, stdout=my_file, stderr=subprocess.STDOUT)
    return def_process, my_file

def main(graph_name, env_short_name, new_epoch, env_name_vs_att, \
    port_lock_name, def_port, env_short_name_tsv):
    env_process_def = start_env_process_def(graph_name, def_port)
    write_def_port(port_lock_name, True, def_port)
    write_def_port(port_lock_name, False, def_port)
    def_process_enj, def_file_enj = run_evaluation_def(env_short_name, new_epoch, \
        env_name_vs_att, def_port, env_short_name_tsv)

    if def_process_enj is not None:
        print("Waiting for def evaluation")
        def_process_enj.wait()
        print("Finished wait, about to close file")
        def_file_enj.close()

    print("Closing env_process for defender")
    sleep_sec = 5
    time.sleep(sleep_sec)
    env_process_def.kill()

'''
example: python3 demo_wait_bug.py RandomGraph30N100E6T1_B.json d30d1 1 \
    DepgraphJavaEnvVsMixedAtt-v0 d30 25333 d30d1_randNoAndB > demo_wait1.txt
'''
if __name__ == '__main__':
    if len(sys.argv) != 8:
        raise ValueError("Need 7 args: graph_name, env_short_name, new_epoch, " + \
            "env_name_vs_att, port_lock_name, def_port, env_short_name_tsv")
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_VS_ATT = sys.argv[4]
    PORT_LOCK_NAME = sys.argv[5]
    DEF_PORT = int(sys.argv[6])
    ENV_SHORT_NAME_TSV = sys.argv[7]
    main(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_VS_ATT, \
        PORT_LOCK_NAME, DEF_PORT, ENV_SHORT_NAME_TSV)
