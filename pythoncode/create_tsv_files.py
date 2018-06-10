import sys
import os
import random

def print_to_file(lines, file_name):
    with open(file_name, 'w') as file:
        for line in lines:
            file.write(line + "\n")

def get_file_lines(file_name):
    '''
    Return the file's text as one string.
    '''
    lines = []
    with open(file_name) as file:
        lines = file.readlines()
    lines = [x.strip() for x in lines]
    return lines

def get_defender_lines(file_name):
    lines = get_file_lines(file_name)
    first_defender_line = lines.index("") + 2
    return lines[first_defender_line:]

def get_rounded_strategy_lines(input_lines):
    strats = []
    values = []
    decimal_places = 4
    for line in input_lines:
        parts = line.split("\t")
        strategy = parts[0]
        value = round(float(parts[1]), decimal_places)
        strats.append(strategy)
        values.append(value)
    if sum(values) < 0.9 or sum(values) > 1.1:
        raise ValueError("Wrong initial values: " + str(values))
    while sum(values) > 1.0:
        index = random.randint(0, len(values))
        values[index] -= 10 ** (-1 * decimal_places)
        values[index] = round(values[index], decimal_places)
    while sum(values) < 1.0:
        index = random.randint(0, len(values) - 1)
        values[index] += 10 ** (-1 * decimal_places)
        values[index] = round(values[index], decimal_places)
    if sum(values) != 1.0 or min(values) < 0.0 or max(values) > 1.0:
        raise ValueError("Wrong normalized values: " + str(values))
    return [strats[i] + "\t" + str(values[i]) for i in range(len(strats))]

def get_attacker_lines(file_name):
    lines = get_file_lines(file_name)
    after_last_attacker_line = lines.index("")
    return lines[1:after_last_attacker_line]

def main(game_number, cur_epoch, env_short_name):
    input_file = None
    if cur_epoch == 0:
        input_file = "gambit_result_" + str(game_number) + "_lcp_decode.txt"
    else:
        input_file = "gambit_result_" + str(game_number) + "_epoch" + str(cur_epoch) + \
            "_lcp_decode.txt"

    defender_lines = get_defender_lines(input_file)
    defender_lines = get_rounded_strategy_lines(defender_lines)
    def_output_file = env_short_name + "_epoch" + str(cur_epoch) + "_def.tsv"
    if os.path.isfile(def_output_file):
        print("Skipping: " + def_output_file + " already exists.")
    else:
        print_to_file(defender_lines, def_output_file)

    attacker_lines = get_attacker_lines(input_file)
    attacker_lines = get_rounded_strategy_lines(attacker_lines)
    att_output_file = env_short_name + "_epoch" + str(cur_epoch) + "_att.tsv"
    if os.path.isfile(att_output_file):
        print("Skipping: " + att_output_file + " already exists.")
    else:
        print_to_file(attacker_lines, att_output_file)

# example: python3 create_tsv_files.py 3013 0 sl29_randNoAndB
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: game_number, cur_epoch, env_short_name")
    GAME_NUMBER = int(sys.argv[1])
    CUR_EPOCH = int(sys.argv[2])
    ENV_SHORT_NAME = sys.argv[3]
    main(GAME_NUMBER, CUR_EPOCH, ENV_SHORT_NAME)
