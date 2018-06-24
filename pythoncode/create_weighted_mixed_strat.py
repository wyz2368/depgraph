import sys
import os
import csv

def get_file_lines(file_name):
    '''
    Return the file's text as a list of strings.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines] # strip leading and trailing whitespace per line.
    lines = [x for x in lines if x] # drop empty lines
    return lines

def get_strat_from_file(strat_file):
    result = {}
    with open(strat_file, 'r') as tsv_in:
        rows = list(list(rec) for rec in csv.reader(tsv_in, delimiter='\t'))
        for row in rows:
            if row:
                strat = row[0]
                prob = float(row[1])
                if prob < 0.0 or prob > 1.0:
                    raise ValueError("Invalid prob: " + str(prob))
                if strat in result:
                    raise ValueError("Duplicate strat: " + strat)
                result[strat] = prob
    tol = 0.00001
    if abs(sum(result.values()) - 1.0) > tol:
        raise ValueError("Wrong sum of probabilities: " + \
            str(sum(result.values())))
    return result

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def get_cur_epoch(tsv_names_file):
    return len(get_file_lines(tsv_names_file))

def get_round_to_mixed_strat(strat_file_names):
    result = []
    for strat_file in strat_file_names:
        cur_strat = get_strat_from_file(strat_file)
        result.append(cur_strat)
    return result

def get_normalized_weights(old_strat_disc_fact, cur_epoch):
    raw_weights = [old_strat_disc_fact ** i for i in range(cur_epoch)]
    raw_weights.reverse()

    # raw_weights is [old_strat_disc_fact ** (cur_epoch - 1), . . . , \
    #     old_strat_disc_fact ** 0]

    normalizer = sum(raw_weights)
    return [x * 1.0 / normalizer for x in raw_weights]

def main(old_strat_disc_fact, strat_min_weight, is_defender, tsv_names_file, \
    env_short_name_tsv):
    cur_epoch = get_cur_epoch(tsv_names_file)
    if cur_epoch == 0:
        raise ValueError("Must have at least one mixed strategy already.")
    if old_strat_disc_fact <= 0.0 or old_strat_disc_fact > 1.0:
        raise ValueError("old_strat_disc_fact must be in (0, 1]: " + \
            str(old_strat_disc_fact))
    if strat_min_weight < 0.0 or strat_min_weight > 0.01:
        raise ValueError("strat_min_weight must be in [0, 0.01]: " + str(strat_min_weight))

    fmt = "{0:.2f}"
    type_string = "_def.tsv"
    if not is_defender:
        type_string = "_att.tsv"
    output_file_name = env_short_name_tsv + "_epoch" + str(cur_epoch) + "_mixed" + \
        fmt.format(old_strat_disc_fact) + type_string
    if os.path.isfile(output_file_name):
        print("Skipping: " + output_file_name + " already exists.")
        return

    strat_file_names = get_file_lines(tsv_names_file)
    round_to_mixed_strat = get_round_to_mixed_strat(strat_file_names)
    weights = get_normalized_weights(old_strat_disc_fact, cur_epoch)

# example: python3 create_weighted_mixed_strat.py 0.5 0.001 True def_mixed_strats.tsv s29
if __name__ == "__main__":
    if len(sys.argv) != 6:
        raise ValueError("Needs 5 arguments: old_strat_disc_fact, strat_min_weight, " + \
            "is_defender, tsv_names_file, env_short_name_tsv")
    OLD_STRAT_DISC_FACT = float(sys.argv[1])
    STRAT_MIN_WEIGHT = int(sys.argv[2])
    IS_DEFENDER = get_truth_value(sys.argv[3])
    TSV_NAMES_FILE = sys.argv[4]
    ENV_SHORT_NAME_TSV = sys.argv[5]
    main(OLD_STRAT_DISC_FACT, STRAT_MIN_WEIGHT, IS_DEFENDER, TSV_NAMES_FILE, \
        ENV_SHORT_NAME_TSV)
