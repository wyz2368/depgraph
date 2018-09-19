import subprocess
import time
import random
import sys
from math import ceil, sqrt, isnan
from py4j.java_gateway import JavaGateway

GATEWAY = None
JAVA_GAME = None
ATTACKER_PURE = "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_" + \
    "2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0"
ATTACKER_MIXED = {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_" + \
    "2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 1.0}
GRAPH = "RandomGraph30N100E6T1_B.json"

def setup_default():
    def_strat = get_def_name([0.5, 0.5, 3.0])
    my_process = start_server(def_strat, ATTACKER_PURE, GRAPH)
    setup_gateway()

    sleep_sec = 5
    time.sleep(sleep_sec)
    return my_process

def start_server(def_strat, att_strat, graph_name):
    cmd = "exec java -jar dg4jnonetcli/dg4jnonetcli.jar " + def_strat + " " + att_strat + \
        " " + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)
    return my_process

def close_gateway():
    GATEWAY.close()
    GATEWAY.close_callback_server()
    GATEWAY.shutdown()

def close_process(my_process):
    my_process.kill()

def count_dict(mixed_strat, run_count):
    result = {}
    for pure_strat, weight in mixed_strat.items():
        if weight > 0.001:
            cur_count = int(ceil(weight * run_count))
            result[pure_strat] = cur_count
    while sum(result.values()) < run_count:
        key = random.choice(list(result.keys()))
        result[key] += 1
    while sum(result.values()) > run_count:
        key = random.choice(list(result.keys()))
        if result[key] > 0:
            result[key] -= 1
    to_remove = set()
    for pure_strat, weight in result.items():
        if weight == 0:
            to_remove.add(pure_strat)
    for pure_strat in to_remove:
        result.pop(pure_strat, None)
    if sum(result.values()) != run_count:
        raise ValueError("Invalid sum: " + str(sum(result.values())))
    if min(result.values()) <= 0:
        raise ValueError("Invalid value <= 0: " + str(result.values()))
    return result

def get_variance(counts, means, variances, overall_mean):
    # v = (1 / (n - 1)) * [\sum n_i (m_i - m)^2 + \sum (n_i - 1) v_i]
    total_count = sum(counts)
    term = 0
    for cur_index in range(len(means)):
        term += counts[cur_index] * ((means[cur_index] - overall_mean) ** 2) + \
            (counts[cur_index] - 1) * variances[cur_index]
    return term / (total_count - 1.)

def get_mean_payoffs(def_strat, att_mixed_strat, run_count):
    if JAVA_GAME is None or GATEWAY is None:
        raise ValueError("Must set up JAVA_GAME and GATEWAY first.")
    count_per_pure_att_strat = count_dict(att_mixed_strat, run_count)
    def_payoff = 0
    att_payoff = 0
    def_means = []
    def_vars = []
    att_means = []
    att_vars = []
    counts = []
    for att_pure_strat, cur_count in count_per_pure_att_strat.items():
        [cur_def_payoff, cur_att_payoff, cur_def_std_error, cur_att_std_error] = \
            JAVA_GAME.resetAndRunForGivenAgents(def_strat, att_pure_strat, cur_count)
        if isnan(cur_def_payoff) or isnan(cur_att_payoff) or isnan(cur_def_std_error) or \
            isnan(cur_att_std_error):
            raise ValueError("NaN returned from Java, attacker: " + att_pure_strat + \
                "\ndefender: " + def_strat + "\ncur_count: " + str(cur_count))
        def_payoff += cur_def_payoff * cur_count * 1. / run_count
        att_payoff += cur_att_payoff * cur_count * 1. / run_count
        def_means.append(cur_def_payoff)
        att_means.append(cur_att_payoff)
        def_vars.append((cur_def_std_error * sqrt(cur_count)) ** 2)
        att_vars.append((cur_att_std_error * sqrt(cur_count)) ** 2)
        counts.append(cur_count)
    def_variance = get_variance(counts, def_means, def_vars, def_payoff)
    att_variance = get_variance(counts, att_means, att_vars, att_payoff)
    def_std_error = sqrt(def_variance) / sqrt(run_count)
    att_std_error = sqrt(att_variance) / sqrt(run_count)
    return def_payoff, att_payoff, def_std_error, att_std_error

def setup_gateway():
    global GATEWAY
    GATEWAY = JavaGateway()
    global JAVA_GAME
    JAVA_GAME = GATEWAY.entry_point.getGame()

def convert_params_from_0_1(params_0_1):
    if len(params_0_1) != 3 or min(params_0_1) < 0 or max(params_0_1) > 1:
        raise ValueError("Invalid params: " + str(params_0_1))
    # defender ranges:
    # numResRatio [0, 1], numAttCandidateRatio [0, 1], qrParam [1, 5]
    result = params_0_1.copy()
    result[2] = result[2] * 4 + 1
    return result

def convert_params_to_0_1(params_java):
    if len(params_java) != 3 or min(params_java[:2]) < 0 or max(params_java[:2]) > 1 \
        or params_java[2] < 1 or params_java[2] > 5:
        raise ValueError("Invalid params: " + str(params_java))
    result = params_java.copy()
    result[2] = (result[2] - 1) * 1. / 4
    return result

def get_def_name(params_java):
    if len(params_java) != 3 or min(params_java[:2]) < 0 or max(params_java[:2]) > 1 \
        or params_java[2] < 1 or params_java[2] > 5:
        raise ValueError("Invalid params: " + str(params_java))
    fmt = "{0:.2f}"
    return "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_" + \
        fmt.format(params_java[0]) + \
        "_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_" + \
        "2.0_numAttCandidateRatio_" + fmt.format(params_java[1]) + "_qrParam_" + \
        fmt.format(params_java[2]) + "_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0"

def sample_mean_def_payoff(params_0_1, run_count, att_mixed_strat):
    params_java = convert_params_from_0_1(params_0_1)
    def_name = get_def_name(params_java)
    def_payoff, _, def_std_error, _ = get_mean_payoffs(def_name, att_mixed_strat, run_count)
    fmt = "{0:.2f}"
    print("Defender payoff std error: " + fmt.format(def_std_error), flush=True)
    return def_payoff

'''
example: python3 depgraph_connect.py 10
'''
if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: run_count")
    RUN_COUNT = int(sys.argv[1])
    PARAMS_0_1 = [0.5, 0.5, 0.5]
    sample_mean_def_payoff(PARAMS_0_1, RUN_COUNT, ATTACKER_MIXED)
