import sys
import os
import csv
import random

def print_to_file(lines, file_name):
    with open(file_name, 'w') as file:
        for line in lines:
            file.write(line + "\n")

def get_file_lines(file_name):
    '''
    Return the file's text as a list of strings.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines] # strip leading and trailing whitespace per line.
    lines = [x for x in lines if x] # drop empty lines
    for line in lines:
        if lines.count(line) != 1:
            raise ValueError("Duplicate line: " + str(line))
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

def get_weighted_strategy(round_to_mixed_strat, weights):
    if len(round_to_mixed_strat) != len(weights):
        raise ValueError("Lengths must match: " + str(len(round_to_mixed_strat)) + \
            ", " + str(len(weights)))

    result = {}
    for round_num in range(len(weights)):
        cur_mixed_strat = round_to_mixed_strat[round_num]
        cur_round_weight = weights[round_num]
        for pure_strat, old_weight in cur_mixed_strat.items():
            added_weight = cur_round_weight * old_weight
            if pure_strat in result:
                result[pure_strat] += added_weight
            else:
                result[pure_strat] = added_weight
    return result

def get_truncated_strategy(weighted_strategy, strat_min_weight):
    result = {}
    decimal_places = 4
    for pure_strat, old_weight in weighted_strategy.items():
        if old_weight >= strat_min_weight:
            result[pure_strat] = round(old_weight, decimal_places)

    tolerance = 10 ** (-1 * decimal_places - 1)
    key_list = list(result.keys())
    while sum(result.values()) > 1.0 + tolerance:
        index = random.randint(0, len(key_list) - 1)
        cur_key = key_list[index]
        if result[cur_key] > strat_min_weight * 1.5:
            result[cur_key] -= 10 ** (-1 * decimal_places)
            result[cur_key] = round(result[cur_key], decimal_places)
    while sum(result.values()) < 1.0 - tolerance:
        index = random.randint(0, len(key_list) - 1)
        cur_key = key_list[index]
        result[cur_key] += 10 ** (-1 * decimal_places)
        result[cur_key] = round(result[cur_key], decimal_places)
    if abs(sum(result.values()) - 1.0) > tolerance or \
        min(result.values()) < 0.0 or max(result.values()) > 1.0:
        raise ValueError("Wrong normalized values: " + str(result.values()) \
            + "\t" + str(sum(result.values())))
    return result

def get_strategy_lines(truncated_strategy):
    keys_sorted = sorted(list(truncated_strategy.keys()))
    weights_sorted = [truncated_strategy[x] for x in keys_sorted]
    lines = [keys_sorted[i] + "\t" + str(weights_sorted[i]) for i in \
        range(len(keys_sorted))]
    return lines

def main(old_strat_disc_fact, strat_min_weight, is_defender, tsv_names_file, \
    env_short_name_tsv):
    # get epoch number to use, same as number of names in tsv_names_file
    cur_epoch = get_cur_epoch(tsv_names_file)
    if cur_epoch == 0:
        # there are no previous mixed strategies listed.
        raise ValueError("Must have at least one mixed strategy already.")
    if old_strat_disc_fact <= 0.0 or old_strat_disc_fact > 1.0:
        raise ValueError("old_strat_disc_fact must be in (0, 1]: " + \
            str(old_strat_disc_fact))
    if strat_min_weight < 0.0 or strat_min_weight > 0.01:
        raise ValueError("strat_min_weight must be in [0, 0.01]: " + str(strat_min_weight))

    type_string = "_def.tsv"
    if not is_defender:
        type_string = "_att.tsv"
    fmt = "{0:.2f}"
    output_file_name = env_short_name_tsv + "_epoch" + str(cur_epoch) + "_mixed" + \
        fmt.format(old_strat_disc_fact).replace('.', '_') + type_string
    if os.path.isfile(output_file_name):
        print("Skipping: " + output_file_name + " already exists.")
        return

    strat_file_names = get_file_lines(tsv_names_file)
    round_to_mixed_strat = get_round_to_mixed_strat(strat_file_names)
    weights = get_normalized_weights(old_strat_disc_fact, cur_epoch)
    weighted_strategy = get_weighted_strategy(round_to_mixed_strat, weights)
    truncated_strategy = get_truncated_strategy(weighted_strategy, strat_min_weight)
    strategy_lines = get_strategy_lines(truncated_strategy)
    print_to_file(strategy_lines, output_file_name)

'''
example: python3 create_weighted_mixed_strat.py 0.5 0.001 True def_strat_files_s29.txt s29
'''
if __name__ == "__main__":
    if len(sys.argv) != 6:
        raise ValueError("Needs 5 arguments: old_strat_disc_fact, strat_min_weight, " + \
            "is_defender, tsv_names_file, env_short_name_tsv")
    OLD_STRAT_DISC_FACT = float(sys.argv[1])
    STRAT_MIN_WEIGHT = float(sys.argv[2])
    IS_DEFENDER = get_truth_value(sys.argv[3])
    TSV_NAMES_FILE = sys.argv[4]
    ENV_SHORT_NAME_TSV = sys.argv[5]
    main(OLD_STRAT_DISC_FACT, STRAT_MIN_WEIGHT, IS_DEFENDER, TSV_NAMES_FILE, \
        ENV_SHORT_NAME_TSV)
