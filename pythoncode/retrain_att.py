import sys
import subprocess
import time

def start_and_return_env_process(graph_name):
    cmd = "exec java -jar ../depgraphpy4jattvseither/depgraphpy4jattvsnetorheuristic.jar " \
        + graph_name
    env_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)
    return env_process

def close_env_process(env_process):
    sleep_sec = 5
    time.sleep(sleep_sec)
    env_process.kill()

def run_retraining(env_short_name, new_epoch, env_name_vs_mixed_def):
    cmd_list = ["python3", "retrain_dg_java_mlp_att_vs_mixed.py", env_name_vs_mixed_def, \
        env_short_name, str(new_epoch)]
    subprocess.call(cmd_list)

def main(graph_name, env_short_name, new_epoch, env_name_vs_mixed_def):
    env_process = start_and_return_env_process(graph_name)
    run_retraining(env_short_name, new_epoch, env_name_vs_mixed_def)
    close_env_process(env_process)

'''
example: python3 retrain_att.py SepLayerGraph0_noAnd_B.json sl29 16 \
    DepgraphJavaEnvVsMixedDef29N-v0
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: graph_name, env_short_name, new_epoch, " + \
            "env_name_vs_mixed_def")
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_VS_MIXED_DEF = sys.argv[4]
    main(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_VS_MIXED_DEF)
