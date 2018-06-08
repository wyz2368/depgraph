import sys

def get_file_lines(file_name):
    '''
    Return the file's text as one string.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines]
    return lines

def append_line(file_name, line):
    with open(file_name, "a") as file: # open in append mode
        if does_file_end_with_empty_line(file_name):
            file.write("\n")
        file.write(line + "\n")

def file_has_network(file_name, net_name):
    return net_name in get_file_lines(file_name)

def does_file_end_with_empty_line(file_name):
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    if not lines:
        return False # there is no terminating empty line, because file has no lines
    return not lines[-1].strip() # lines[-1].strip() is true if the last line is non-empty

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def main(env_short_name, new_epoch, def_pkl_prefix, att_pkl_prefix, should_add_def, \
    should_add_att):
    if not should_add_att and not should_add_def:
        raise ValueError("Must add at least one file: " + str(should_add_def) + \
            "\t" + str(should_add_att))
    if should_add_def:
        def_name_new = def_pkl_prefix + str(new_epoch) + ".pkl"
        old_def_file_name = "oldDefNetNames_" + env_short_name + ".txt"
        if file_has_network(old_def_file_name, def_name_new):
            print("Skipping: " + old_def_file_name + ", already added.")
        else:
            append_line(old_def_file_name, def_name_new)
        if not file_has_network(old_def_file_name, def_name_new):
            raise ValueError(old_def_file_name)

        cur_def_file_name = "defNetStrings_" + env_short_name + ".txt"
        if file_has_network(cur_def_file_name, def_name_new):
            print("Skipping: " + cur_def_file_name + ", already added.")
        else:
            append_line(cur_def_file_name, def_name_new)
        if not file_has_network(cur_def_file_name, def_name_new):
            raise ValueError(cur_def_file_name)

    if should_add_att:
        att_name_new = att_pkl_prefix + str(new_epoch) + "_att.pkl"
        old_att_file_name = "oldAttNetNames_" + env_short_name + ".txt"
        if file_has_network(old_att_file_name, att_name_new):
            print("Skipping: " + old_att_file_name + ", already added.")
        else:
            append_line(old_att_file_name, att_name_new)
        if not file_has_network(old_att_file_name, att_name_new):
            raise ValueError(old_att_file_name)

        cur_att_file_name = "attNetStrings_" + env_short_name + ".txt"
        if file_has_network(cur_att_file_name, att_name_new):
            print("Skipping: " + cur_att_file_name + ", already added.")
        else:
            append_line(cur_att_file_name, att_name_new)
        if not file_has_network(cur_att_file_name, att_name_new):
            raise ValueError(cur_att_file_name)

'''
example: python3 append_net_names.py sl29 15 dg_sl29_dq_mlp_rand_epoch \
    dg_sl29_dq_mlp_rand_epoch True False
'''
if __name__ == "__main__":
    if len(sys.argv) != 7:
        raise ValueError("Needs 6 arguments: env_short_name, new_epoch,\n" + \
            "def_pkl_prefix, att_pkl_prefix, should_add_def, should_add_att")
    ENV_SHORT_NAME = sys.argv[1]
    NEW_EPOCH = int(sys.argv[2])
    DEF_PKL_PREFIX = sys.argv[3]
    ATT_PKL_PREFIX = sys.argv[4]
    SHOULD_ADD_DEF = get_truth_value(sys.argv[5])
    SHOULD_ATT_ATT = get_truth_value(sys.argv[6])
    main(ENV_SHORT_NAME, NEW_EPOCH, DEF_PKL_PREFIX, ATT_PKL_PREFIX, SHOULD_ADD_DEF, \
        SHOULD_ATT_ATT)
