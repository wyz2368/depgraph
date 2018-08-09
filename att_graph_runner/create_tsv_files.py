import sys
import os
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
    lines = [x.strip() for x in lines]
    return lines

def get_defender_lines(file_name):
    lines = get_file_lines(file_name)
    first_defender_line = lines.index("Defender mixed strategy:") + 1
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
    tolerance = 10 ** (-1 * decimal_places - 1)
    while sum(values) > 1.0 + tolerance:
        index = random.randint(0, len(values) - 1)
        values[index] -= 10 ** (-1 * decimal_places)
        values[index] = round(values[index], decimal_places)
    while sum(values) < 1.0 - tolerance:
        index = random.randint(0, len(values) - 1)
        values[index] += 10 ** (-1 * decimal_places)
        values[index] = round(values[index], decimal_places)
    if abs(sum(values) - 1.0) > tolerance or min(values) < 0.0 or max(values) > 1.0:
        raise ValueError("Wrong normalized values: " + str(values) + "\t" + str(sum(values)))
    return [strats[i] + "\t" + str(values[i]) for i in range(len(strats))]

def get_attacker_lines(file_name):
    lines = get_file_lines(file_name)
    after_last_attacker_line = lines.index("")
    return lines[1:after_last_attacker_line]

def get_gambit_result_name(game_number, tsv_epoch, env_short_name_payoffs):
    return "gambit_result_" + str(game_number) + "_" + str(tsv_epoch) + "_" + \
        env_short_name_payoffs + "_lcp_decode.txt"

def main(game_number, cur_epoch, env_short_name_tsv, env_short_name_payoffs):
    input_file = get_gambit_result_name(game_number, cur_epoch, env_short_name_payoffs)
    print("Input file: " + input_file)

    defender_lines = get_defender_lines(input_file)
    try:
        defender_lines = get_rounded_strategy_lines(defender_lines)
    except ValueError:
        sys.exit(1)

    def_output_file = env_short_name_tsv + "_epoch" + str(cur_epoch + 1) + \
        "_def.tsv"
    if os.path.isfile(def_output_file):
        print("Skipping: " + def_output_file + " already exists.")
    else:
        print_to_file(defender_lines, def_output_file)

    attacker_lines = get_attacker_lines(input_file)
    try:
        attacker_lines = get_rounded_strategy_lines(attacker_lines)
    except ValueError:
        sys.exit(1)

    att_output_file = env_short_name_tsv + "_epoch" + str(cur_epoch + 1) + \
        "_att.tsv"
    if os.path.isfile(att_output_file):
        print("Skipping: " + att_output_file + " already exists.")
    else:
        print_to_file(attacker_lines, att_output_file)

# example: python3 create_tsv_files.py 3013 0 sl29_randNoAndB sl29
if __name__ == "__main__":
    if len(sys.argv) != 5:
        raise ValueError("Needs 4 arguments: game_number, cur_epoch, " + \
            "env_short_name_tsv, env_short_name_payoffs")
    GAME_NUMBER = int(sys.argv[1])
    CUR_EPOCH = int(sys.argv[2])
    ENV_SHORT_NAME_TSV = sys.argv[3]
    ENV_SHORT_NAME_PAYOFFS = sys.argv[4]
    main(GAME_NUMBER, CUR_EPOCH, ENV_SHORT_NAME_TSV, ENV_SHORT_NAME_PAYOFFS)
