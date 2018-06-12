import sys

def write_line(file_name, line):
    with open(file_name, "w") as file:
        file.write(line)

def main(config_env_short_name, tsv_env_short_name, new_epoch):
    gym_folder = "../gym/gym/gym/envs/board_game/"
    config_file_name_att = gym_folder + config_env_short_name + "_att_config.py"
    config_file_name_def = gym_folder + config_env_short_name + "_def_config.py"

    tsv_name_att = tsv_env_short_name + "_epoch" + str(new_epoch) + "_att.tsv"
    tsv_name_def = tsv_env_short_name + "_epoch" + str(new_epoch) + "_def.tsv"

    write_line(config_file_name_att, "ATT_MIXED_STRAT_FILE = \"" + tsv_name_att + "\"")
    write_line(config_file_name_def, "DEF_MIXED_STRAT_FILE = \"" + tsv_name_def + "\"")

# example: python3 update_opponent_strats.py sl29 sl29_randNoAndB 15
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: config_env_short_name, tsv_env_short_name, " + \
            "new_epoch")
    CONFIG_ENV_SHORT_NAME = sys.argv[1]
    TSV_ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    main(CONFIG_ENV_SHORT_NAME, TSV_ENV_SHORT_NAME, NEW_EPOCH)
