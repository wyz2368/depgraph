'''
Add the given attacker and defender network file names to the appropriate files listing
network strategy names to include in the game, skipping either name(s) if it is "None".
'''
import sys

def get_file_lines(file_name):
    '''
    Return the file's text as one string.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines]
    lines = [x for x in lines if x]
    return lines

def append_line(file_name, line):
    '''
    Append the given line to the file, adding a newline afterward if the file previously
    ended in an empty line.
    '''
    with open(file_name, "a") as file: # open in append mode
        if does_file_end_with_empty_line(file_name):
            file.write("\n")
        file.write(line + "\n")

def file_has_network(file_name, net_name):
    '''
    Returns true if a (stripped, non-empty) line in the given file matches net_name.
    '''
    return net_name in get_file_lines(file_name)

def does_file_end_with_empty_line(file_name):
    '''
    Returns true if the file is nonempty, and its last line is empty or only whitespace.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    if not lines:
        return False # there is no terminating empty line, because file has no lines
    return not lines[-1].strip() # lines[-1].strip() is true if the last line is non-empty

def get_truth_value(str_input):
    '''
    Return True or False after string conversion, else throw error.
    '''
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def append_names_curve(env_short_name, def_model_to_add, att_model_to_add):
    '''
    For whichever of def_model_to_add or att_model_to_add are not None,
    append that name(s) to the appropriate files that list network strategies to include
    in the game.
    '''
    if def_model_to_add is None and att_model_to_add is None:
        raise ValueError("Must add at least one file: " + str(def_model_to_add) + \
            "\t" + str(att_model_to_add))
    if def_model_to_add is not None:
        def_name_new = def_model_to_add
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

    if att_model_to_add is not None:
        att_name_new = att_model_to_add
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
example: python3 append_net_names_curve.py sl29 \
    dg_s29_dq_mlp_rand_epoch17_afterRetrain_r1.pkl None
'''
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: env_short_name, def_model_to_add, " + \
            "att_model_to_add")
    ENV_SHORT_NAME = sys.argv[1]
    DEF_MODEL_TO_ADD = sys.argv[2]
    if DEF_MODEL_TO_ADD == "None":
        DEF_MODEL_TO_ADD = None
    ATT_MODEL_TO_ADD = sys.argv[3]
    if ATT_MODEL_TO_ADD == "None":
        ATT_MODEL_TO_ADD = None
    try:
        append_names_curve(ENV_SHORT_NAME, DEF_MODEL_TO_ADD, ATT_MODEL_TO_ADD)
    except ValueError:
        sys.exit(1)
