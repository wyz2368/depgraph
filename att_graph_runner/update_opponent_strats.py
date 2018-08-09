import sys

def write_line(file_name, line):
    with open(file_name, "w") as my_file:
        my_file.write(line)

def update_strats(port_lock_name, tsv_env_short_name, new_epoch):
    gym_folder = "../gym/gym/gym/envs/board_game/"
    config_file_name_att = gym_folder + port_lock_name + "_att_config.py"
    config_file_name_def = gym_folder + port_lock_name + "_def_config.py"

    tsv_name_att = tsv_env_short_name + "_epoch" + str(new_epoch) + "_att.tsv"
    tsv_name_def = tsv_env_short_name + "_epoch" + str(new_epoch) + "_def.tsv"

    print("Updating strats:")
    print("\t" + config_file_name_att + "\t" + tsv_name_att)
    print("\t" + config_file_name_def + "\t" + tsv_name_def)

    write_line(config_file_name_att, "ATT_MIXED_STRAT_FILE = \"" + tsv_name_att + "\"")
    write_line(config_file_name_def, "DEF_MIXED_STRAT_FILE = \"" + tsv_name_def + "\"")

# example: python3 update_opponent_strats.py s29 sl29_randNoAndB 15
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: port_lock_name, tsv_env_short_name, " + \
            "new_epoch")
    PORT_LOCK_NAME = sys.argv[1]
    TSV_ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    update_strats(PORT_LOCK_NAME, TSV_ENV_SHORT_NAME, NEW_EPOCH)
