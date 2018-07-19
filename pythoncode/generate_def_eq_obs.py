import sys
import csv
import enjoy_depgraph_data_for_obs_store as generate_obs

def write_observations_to_file(observations, out_file_name):
    with open(out_file_name, "w") as csv_file:
        writer = csv.writer(csv_file, delimiter=',')
        for observation in observations:
            writer.writerow(observation)

def get_lines(file_name):
    lines = None
    with open(file_name) as file:
        lines = file.readlines()
    lines = [line.strip() for line in lines]
    lines = [line for line in lines if line]
    return lines

def get_eq_from_file(file_name):
    lines = get_lines(file_name)
    result = {}
    for line in lines:
        line = line.strip()
        while "  " in line:
            line = line.replace("  ", " ")
        items = None
        if "\t" in line:
            items = line.split('\t')
        else:
            items = line.split(" ")
        strat = items[0].strip()
        weight = float(items[1].strip())
        result[strat] = weight
    return result

def get_eq_without_heuristics(eq_complete):
    result = {}
    for strat, weight in eq_complete.items():
        if ".pkl" in strat:
            result[strat] = weight
    if not result:
        raise ValueError("No network strategies are in the eq: " + str(eq_complete))
    total_weight = sum(result.values())
    for strat in result.keys():
        result[strat] = result[strat] / total_weight
    return result

def get_obs_for_network(env_name_att_net, def_strat, episodes, obs_per_episode):
    return generate_obs.generate_obs(env_name_att_net, def_strat, episodes, obs_per_episode)

def get_all_obs(network_to_weight, num_episodes, obs_per_episode, env_name_att_net):
    result = []
    for strat, weight in network_to_weight.items():
        cur_episodes = int(num_episodes * weight)
        if cur_episodes > 0:
            print("For net: " + strat + ", will get: " + str(cur_episodes))
            cur_result = get_obs_for_network(env_name_att_net, strat, cur_episodes, \
                obs_per_episode)
            result.extend(cur_result)
    return result

def main(env_name_att_net, tsv_file, num_episodes, obs_per_episode, out_file_name):
    tsv_strategy = get_eq_from_file(tsv_file)
    network_to_weight = get_eq_without_heuristics(tsv_strategy)
    observations = get_all_obs(network_to_weight, num_episodes, obs_per_episode, \
        env_name_att_net)
    write_observations_to_file(observations, out_file_name)

'''
python3 generate_def_eq_obs.py DepgraphJavaEnvVsMixedAtt-v0 d30_epoch14_def.tsv \
    500 10 obs_def_e14_vs_att.csv
'''
if __name__ == '__main__':
    if len(sys.argv) != 6:
        raise ValueError("Need 5 args: env_name_att_net, tsv_file, num_episodes, " + \
            "obs_per_episode, out_file_name")
    ENV_NAME_ATT_NET = sys.argv[1]
    TSV_FILE = sys.argv[2]
    NUM_EPISODES = int(sys.argv[3])
    OBS_PER_EPISODE = int(sys.argv[4])
    OUT_FILE_NAME = sys.argv[5]
    main(ENV_NAME_ATT_NET, TSV_FILE, NUM_EPISODES, OBS_PER_EPISODE, OUT_FILE_NAME)
