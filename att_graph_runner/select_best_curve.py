import sys
import json

# makes weight of current equilibrium approch 0.5 from above, if discounting is 0.7,
# as number of epochs approaches infinity.
CUR_EQ_WEIGHT = 2. / 7

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

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def get_net_payoff(file_name):
    lines = get_file_lines(file_name)
    prefix = "Mean reward: "
    for i in range(1, len(lines)):
        if lines[i].startswith(prefix):
            value_str = lines[i][len(prefix):]
            return float(value_str)
    raise ValueError("Prefix not found: " + str(lines))

def get_eq_payoff(file_name):
    eq_json = get_json_data(file_name)
    return eq_json["mean_reward"]

def get_cur_eq_payoff(env_short_name, is_defender, new_epoch):
    '''
    Get the expected payoff for the agent in the current equilibrium.
    If is_defender, this will be the defender's expected payoff.
    '''
    eq_payoff_file = None
    if is_defender:
        eq_payoff_file = "out_defPayoffs_" + env_short_name + "_randNoAndB_epoch" + \
            str(new_epoch - 1) + ".txt"
    else:
        eq_payoff_file = "out_attPayoffs_" + env_short_name + "_randNoAndB_epoch" + \
            str(new_epoch - 1) + ".txt"
    return get_eq_payoff(eq_payoff_file)

def get_eval_file_name(env_short_name, is_defender, is_retrain_opponent, new_epoch, \
    retrain_number):
    '''
    Returns the name of the file containing the sample expected payoff for the agent,
    for the given epoch of training, agent type, and opponent type.
    If is_defender, the file will hold the defender's expected payoff.
    If is_retrain_opponent, the payoff will be based on playing against the retraining
    mixed strategy; otherwise, based on playing against the current equilibrium mixed
    strategy.
    If retrain_number is 0, will be for the initially trained agent; otherwise, for the
    given index of fine-tuned agent.
    '''
    if is_defender:
        result = "def_" + env_short_name + "_randNoAndB_epoch" + \
                str(new_epoch) + "_r" + str(retrain_number) + "_enj"
        if is_retrain_opponent:
            result += "_vsRetrain.txt"
        else:
            result += "_vsEq.txt"
        return result
    result = "att_" + env_short_name + "_randNoAndB_epoch" + \
        str(new_epoch) + "_r" + str(retrain_number) + "_enj"
    if is_retrain_opponent:
        result += "_vsRetrain.txt"
    else:
        result += "_vsEq.txt"
    return result

def get_best_retrain_number(env_short_name, new_epoch, is_defender, save_count):
    '''
    Returns the index in {0, . . ., save_count} of the best trained or retrained agent,
    as defined below. The agent will be a defender if is_defender, in new_epoch round of
    training.
    The best agent is defined as an agent that gets strictly higher expected payoff than the
    current equilibrium agent against the current equilibrium opponent, and that achieves
    the highest weighted mean of this gain in expected payoff, and the gain in expected
    payoff against the retraining mixed strategy, relative to the initially trained (but not
    fine-tuned) agent from this training round.
    The weighting is as follows:
        J = CUR_EQ_WEIGHT * (myPayoffVsEq - eqPayoffVsEq) + \
            (1 - CUR_EQ_WEIGHT) * (myPayoffVsRetrainMix - firstTrainedNetPayoffVsRetrainMix)
    We set the constraint that
        (myPayoffVsEq - eqPayoffVsEq) > 0.
    If none of the (save_count + 1) trained nets meets this constraint return None.
    Otherwise, return the integer in {0, . . ., save_count} of the best.
    '''
    if CUR_EQ_WEIGHT <= 0.0 or CUR_EQ_WEIGHT >= 1.0:
        raise ValueError("Invalid CUR_EQ_WEIGHT: " + str(CUR_EQ_WEIGHT))
    cur_eq_payoff = get_cur_eq_payoff(env_short_name, is_defender, new_epoch)

    vs_eq_payoffs = []
    vs_retrain_payoffs = []
    for retrain_number in range(save_count):
        vs_eq_file = get_eval_file_name(env_short_name, is_defender, False, new_epoch, \
            retrain_number)
        try:
            vs_eq_payoff = get_net_payoff(vs_eq_file)
        except ValueError:
            sys.exit(1)
        vs_eq_payoffs.append(vs_eq_payoff)
        vs_retrain_file = get_eval_file_name(env_short_name, is_defender, True, new_epoch, \
            retrain_number)
        try:
            vs_retrain_payoff = get_net_payoff(vs_retrain_file)
        except ValueError:
            sys.exit(1)
        vs_retrain_payoffs.append(vs_retrain_payoff)

    vs_eq_gains = [x - cur_eq_payoff for x in vs_eq_payoffs]
    vs_retrain_gains = [x - vs_retrain_payoffs[0] for x in vs_retrain_payoffs]

    is_eligible = [x > 0 for x in vs_eq_gains]
    scores = []
    for i in range(len(vs_eq_payoffs)):
        if is_eligible[i]:
            cur_score = CUR_EQ_WEIGHT * vs_eq_gains[i] + \
                (1.0 - CUR_EQ_WEIGHT) * vs_retrain_gains[i]
            scores.append(cur_score)
        else:
            scores.append(float('-inf'))
    if True not in is_eligible:
        return None
    result = scores.index(max(scores))
    if not is_eligible[result]:
        raise ValueError("Illegal result: " + str(result) + ", " + str(is_eligible))
    return result

def main(env_short_name, new_epoch, is_defender, save_count):
    try:
        result = get_best_retrain_number(env_short_name, new_epoch, is_defender, save_count)
    except ValueError:
        sys.exit(1)
    return result

# example: python3 select_best_curve.py sl29_randNoAndB 14 True 3
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: env_short_name, new_epoch, is_defender, " + \
            "save_count")
    ENV_SHORT_NAME = sys.argv[1]
    NEW_EPOCH = int(sys.argv[2])
    try:
        IS_DEFENDER = get_truth_value(sys.argv[3])
    except ValueError:
        sys.exit(1)
    SAVE_COUNT = int(sys.argv[4])
    main(ENV_SHORT_NAME, NEW_EPOCH, IS_DEFENDER, SAVE_COUNT)
