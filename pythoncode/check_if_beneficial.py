import sys
import json

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def get_file_lines(file_name):
    '''
    Return the file's text as one string.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines]
    return lines

def get_net_payoff(file_name):
    lines = get_file_lines(file_name)
    line_3 = lines[2]
    prefix = "Mean reward: "
    value_str = line_3[len(prefix):]
    return float(value_str)

def get_eq_payoff(file_name):
    eq_json = get_json_data(file_name)
    return eq_json["mean_reward"]

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def main(env_short_name, new_epoch, is_defender):
    eq_payoff_file = None
    net_payoff_file = None
    if is_defender:
        eq_payoff_file = "out_defPayoffs_" + env_short_name + "_randNoAndB_epoch" + \
            str(new_epoch - 1) + ".txt"
        net_payoff_file = "def_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + "_enj.txt"
    else:
        eq_payoff_file = "out_attPayoffs_" + env_short_name + "_randNoAndB_epoch" + \
            str(new_epoch - 1) + ".txt"
        net_payoff_file = "att_" + env_short_name + "_randNoAndB_epoch" + str(new_epoch) + "_enj.txt"

    eq_payoff = get_eq_payoff(eq_payoff_file)
    net_payoff = get_net_payoff(net_payoff_file)
    print(net_payoff > eq_payoff)

# example: python3 check_if_beneficial.py sl29_randNoAndB 14 True
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: env_short_name, new_epoch, is_defender")
    ENV_SHORT_NAME = sys.argv[1]
    NEW_EPOCH = int(sys.argv[2])
    IS_DEFENDER = get_truth_value(sys.argv[3])
    main(ENV_SHORT_NAME, NEW_EPOCH, IS_DEFENDER)
