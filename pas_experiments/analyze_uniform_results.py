import csv
from dg_annealing import get_truth_value

def analyze_results(results, max_p, error_tol):
    false_positives_sim = 0
    false_negatives_sim = 0
    true_positives_sim = 0
    true_negatives_sim = 0
    false_positives_unif = 0
    false_negatives_unif = 0
    true_positives_unif = 0
    true_negatives_unif = 0

    count_sim_higher = 0
    count_unif_higher = 0
    count_both_zero = 0
    count_both_equal_nonzero = 0

    for result in results:
        was_confirmed = results[0]
        simulated_anneal_ground_truth_est = result[1]
        uniform_ground_truth_est = result[2]
        should_confirm_sim = (simulated_anneal_ground_truth_est <= max_p)
        if should_confirm_sim:
            if was_confirmed:
                true_positives_sim += 1
            else:
                false_negatives_sim += 1
        else:
            if was_confirmed:
                false_positives_sim += 1
            else:
                true_negatives_sim += 1
        should_confirm_unif = (uniform_ground_truth_est <= max_p)
        if should_confirm_unif:
            if was_confirmed:
                true_positives_unif += 1
            else:
                false_negatives_unif += 1
        else:
            if was_confirmed:
                false_positives_unif += 1
            else:
                true_negatives_unif += 1

        if simulated_anneal_ground_truth_est > uniform_ground_truth_est:
            count_sim_higher += 1
        elif simulated_anneal_ground_truth_est < uniform_ground_truth_est:
            count_unif_higher += 1
        elif simulated_anneal_ground_truth_est > 0:
            count_both_equal_nonzero += 1
        else:
            count_both_zero += 1

    fraction_sim_higher = count_sim_higher * 1.0 / len(results)
    fraction_unif_higher = count_unif_higher * 1.0 / len(results)
    fraction_both_zero = count_both_zero * 1.0 / len(results)
    fraction_both_equal_nonzero = count_both_equal_nonzero * 1.0 / len(results)

    false_pos_rate_sim = false_positives_sim * 1. / len(results)
    false_neg_rate_sim = false_negatives_sim * 1. / len(results)
    true_pos_rate_sim = true_positives_sim * 1. / len(results)
    true_neg_rate_sim = true_negatives_sim * 1. / len(results)

    false_pos_rate_unif = false_positives_unif * 1. / len(results)
    false_neg_rate_unif = false_negatives_unif * 1. / len(results)
    true_pos_rate_unif = true_positives_unif * 1. / len(results)
    true_neg_rate_unif = true_negatives_unif * 1. / len(results)

    my_format = "{0:.4f}"

    print("max p: " + str(max_p))
    print("Error tolerance: " + str(error_tol))
    print("Examples: " + str(len(results)) + "\n")

    print("False positive rate sim: " + my_format.format(false_pos_rate_sim))
    print("False negative rate sim: " + my_format.format(false_neg_rate_sim))
    print("True positive rate sim: " + my_format.format(true_pos_rate_sim))
    print("True negative rate sim: " + my_format.format(true_neg_rate_sim) + "\n")

    print("False positive rate unif: " + my_format.format(false_pos_rate_unif))
    print("False negative rate unif: " + my_format.format(false_neg_rate_unif))
    print("True positive rate unif: " + my_format.format(true_pos_rate_unif))
    print("True negative rate unif: " + my_format.format(true_neg_rate_unif) + "\n")

    mean_sim_dev_prob = sum([x[1] for x in results]) * 1.0 / len(results)
    mean_unif_dev_prob = sum([x[2] for x in results]) * 1.0 / len(results)
    print("mean simulated annealing dev prob: " + my_format.format(mean_sim_dev_prob))
    print("mean uniform dev prob: " + my_format.format(mean_unif_dev_prob) + "\n")

    print("fraction sim higher: " + my_format.format(fraction_sim_higher))
    print("fraction unif higher: " + my_format.format(fraction_unif_higher))
    print("fraction both zero: " + my_format.format(fraction_both_zero))
    print("fraction both equal nonzero: " + my_format.format(fraction_both_equal_nonzero))

def get_results(file_name):
    result = []
    with open(file_name, "r") as csv_file:
        reader = csv.reader(csv_file, delimiter="\t")
        for row in reader:
            is_true = get_truth_value(row[0])
            simulated_anneal_ground_truth_est = float(row[1])
            uniform_ground_truth_est = float(row[2])
            result.append([is_true, simulated_anneal_ground_truth_est, \
                uniform_ground_truth_est])
    return result

def main(max_p, error_tolerance):
    input_file_name = "fpsb4_uniform_results.tsv"
    results = get_results(input_file_name)

    analyze_results(results, max_p, error_tolerance)

# python3 analyze_uniform_results.py
if __name__ == "__main__":
    MAX_P = 0.05
    ERROR_TOLERANCE = 0.1
    main(MAX_P, ERROR_TOLERANCE)
