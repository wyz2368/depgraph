import sys
import os.path

def run_evaluation_att_net(env_short_name, new_epoch, env_name_def_net, save_name):
    cmd_list = ["python3", "enjoy_dg_data_vs_mixed_def.py", env_name_def_net, \
        env_short_name, str(new_epoch)]
    # att_out_name_enj = "att_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
    #    "_enj.txt"
    if os.path.isfile(save_name):
        print("Skipping: " + save_name + " already exists.")
        return
    with open(save_name, "w") as file:
        subprocess.call(cmd_list, stdout=file)

def run_evaluation_def_net(env_short_name, new_epoch, env_name_att_net, save_name):
    cmd_list = ["python3", "enjoy_depgraph_data_vs_mixed.py", env_name_att_net, \
        env_short_name, str(new_epoch)]
    # def_out_name_enj = "def_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + \
    #     "_enj.txt"
    if os.path.isfile(save_name):
        print("Skipping: " + save_name + " already exists.")
        return
    with open(save_name, "w") as file:
        subprocess.call(cmd_list, stdout=file)

def start_and_return_env_process(graph_name, is_defender_net):
    cmd = "exec java -jar ../depgraphpy4jattvseither/" \
        + "depgraphpy4jattvsnetorheuristic.jar " \
        + graph_name

    if is_defender_net:
        cmd = "exec java -jar ../depgraphpy4jdefvseither/" \
            + "depgraphpy4jdefvsnetorheuristic.jar " \
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

def write_line(file_name, line):
    with open(file_name, "w") as file:
        file.write(line)

def get_file_lines(file_name):
    '''
    Return the file's text as a list of strings.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines]
    lines = [x for x in lines if x] # remove empty lines
    return lines

def get_string_from_line(line):
    first_double_quote = line.index("\"")
    second_double_quote = line.index("\"", first_double_quote + 1)
    return line[first_double_quote + 1 : second_double_quote]

 def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def get_cur_tsv_name(config_env_short_name, is_defender_net):
    config_name = "~/gym/gym/envs/board_game/" + config_env_short_name
    if is_defender_net:
        config_name += "_att" # network defender is self, opponent is att
    else:
        config_name += "_def"
    config_name += _config.py

    lines = get_file_lines(config_name)
    if len(lines) != 1:
        raise ValueError("Invalid lines in config: " + str(lines))
    return get_string_from_line(lines[0])

def get_modified_tsv_name(tsv_env_short_name, cur_epoch, is_defender_net):
    type_string = "_def.tsv"
    if not is_defender_net:
        type_string = "_att.tsv"
    fmt = "{0:.2f}"
    output_file_name = env_short_name_tsv + "_epoch" + str(cur_epoch) + "_mixed" + \
        fmt.format(old_strat_disc_fact).replace('.', '_') + type_string

def set_config_name(config_env_short_name, modified_tsv_name, is_defender_net):
    gym_folder = "../gym/gym/gym/envs/board_game/"
    if is_defender_net:
        # network defender is self, opponent is att
        config_file_name_att = gym_folder + config_env_short_name + "_att_config.py"
        write_line(config_file_name_att, "ATT_MIXED_STRAT_FILE = \"" + \
            modified_tsv_name + "\"")
    else:
        config_file_name_def = gym_folder + config_env_short_name + "_def_config.py"
        write_line(config_file_name_def, "DEF_MIXED_STRAT_FILE = \"" + \
            modified_tsv_name + "\"")

def main(config_env_short_name, tsv_env_short_name, cur_epoch, old_strat_disc_fact, \
    save_count, graph_name, env_name_opp_mixed, is_defender_net, runs_per_pair):
    if cur_epoch < 1:
        raise ValueError("cur_epoch must be >= 1: " + str(cur_epoch))
    if old_strat_disc_fact <= 0.0 or old_strat_disc_fact > 1.0:
        raise ValueError("old_strat_disc_fact must be in (0, 1]: " + \
            str(old_strat_disc_fact))
    if save_count < 1:
        raise ValueError("save_count must be >= 1: " + str(save_count))
    if runs_per_pair < 2:
        raise ValueError("runs_per_pair must be >= 2: " + str(runs_per_pair))

    result_file_name = "curve_" + tsv_env_short_name + "_e" + str(cur_epoch)
    if is_defender_net:
        result_file_name += "_def.json"
    else:
        result_file_name += "_att.json"
    if os.path.isfile(result_file_name):
        raise ValueError("Skipping: " + result_file_name + " already exists.")

    old_tsv_name = get_cur_tsv_name(config_env_short_name, is_defender_net)
    print("old_tsv_name: " + old_tsv_name)
    modified_tsv_name = get_modified_tsv_name(tsv_env_short_name, cur_epoch, is_defender_net)

    set_config_name(config_env_short_name, modified_tsv_name, is_defender_net)

    env_process = start_and_return_env_process(graph_name, is_defender_net)

    if is_defender_net:
        pass
    else:
        pass

if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: config_env_short_name, tsv_env_short_name, " + \
            "cur_epoch, old_strat_disc_fact, save_count, graph_name, " + \
            "env_name_opp_mixed, is_defender_net, runs_per_pair"
            )
    CONFIG_ENV_SHORT_NAME = sys.argv[1]
    TSV_ENV_SHORT_NAME = sys.argv[2]
    CUR_EPOCH = int(sys.argv[3])
    OLD_STRAT_DISC_FACT = float(sys.argv[4])
    SAVE_COUNT = int(sys.argv[5])
    GRAPH_NAME = sys.argv[6]
    ENV_NAME_OPP_MIXED = sys.argv[7]
    IS_DEFENDER_NET = get_truth_value(sys.argv[8])
    RUNS_PER_PAIR = int(sys.argv[9])
    main(CONFIG_ENV_SHORT_NAME, TSV_ENV_SHORT_NAME, CUR_EPOCH, OLD_STRAT_DISC_FACT, \
        SAVE_COUNT, GRAPH_NAME, ENV_NAME_OPP_MIXED, IS_DEFENDER_NET, RUNS_PER_PAIR)
