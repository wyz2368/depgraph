import csv

def analyze_results_from_file(results, max_p, error_tol):
    false_positives = 0
    false_negatives = 0
    true_positives = 0
    true_negatives = 0

    for result in results:
        was_confirmed = result[0]
        should_confirm = (result[1] <= max_p)
        if should_confirm:
            if was_confirmed:
                true_positives += 1
            else:
                false_negatives += 1
        else:
            if was_confirmed:
                false_positives += 1
            else:
                true_negatives += 1
    false_pos_rate = false_positives * 1. / len(results)
    false_neg_rate = false_negatives * 1. / len(results)
    true_pos_rate = true_positives * 1. / len(results)
    true_neg_rate = true_negatives * 1. / len(results)

    print("Error tolerance: " + str(error_tol))
    print("Examples: " + str(len(results)))
    print("False positive rate: " + str(false_pos_rate))
    print("False negative rate: " + str(false_neg_rate))
    print("True positive rate: " + str(true_pos_rate))
    print("True negative rate: " + str(true_neg_rate), flush=True)

def get_truth_value(item):
    return item == "True"

def get_results(file_name):
    result = []
    with open(file_name, "r") as csv_file:
        reader = csv.reader(csv_file, delimiter="\t")
        for row in reader:
            is_true = get_truth_value(row[0])
            ground_truth_p = float(row[2])
            result.append([is_true, ground_truth_p])
    return result

def main(max_p, error_tolerance):
    out_file_name = "fpsb4_results.tsv"
    results = get_results(out_file_name)
    analyze_results_from_file(results, max_p, error_tolerance)

if __name__ == "__main__":
    MAX_P = 0.05
    ERROR_TOLERANCE = 0.1
    main(MAX_P, ERROR_TOLERANCE)
