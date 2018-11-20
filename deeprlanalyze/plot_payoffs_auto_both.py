import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from append_net_names import get_truth_value
from plot_payoffs_auto import get_cutoff_dev_payoffs, get_all_payoffs, get_round_count, \
    get_cutoff_dev_payoffs

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def plot_payoffs_both(do_def_eq_payoffs, do_def_dev_payoffs, \
    do_att_eq_payoffs, do_att_dev_payoffs, hado_def_eq_payoffs, hado_def_dev_payoffs, \
    hado_att_eq_payoffs, hado_att_dev_payoffs, is_r30):
    fig, ax = plt.subplots()
    fig.set_size_inches(10, 5)

    my_lw = 2
    if len(do_def_dev_payoffs) == len(do_def_eq_payoffs) - 1:
        do_def_dev_payoffs.append(do_def_eq_payoffs[-1])
    if len(do_att_dev_payoffs) == len(do_att_eq_payoffs) - 1:
        do_att_dev_payoffs.append(do_att_eq_payoffs[-1])
    if len(hado_def_dev_payoffs) == len(hado_def_eq_payoffs) - 1:
        hado_def_dev_payoffs.append(hado_def_eq_payoffs[-1])
    if len(hado_att_dev_payoffs) == len(hado_att_eq_payoffs) - 1:
        hado_att_dev_payoffs.append(hado_att_eq_payoffs[-1])

    y_min = min(min(do_def_eq_payoffs), max(hado_def_eq_payoffs))
    y_max = max(max(do_att_dev_payoffs), max(hado_att_dev_payoffs))
    x_max = max(len(do_def_eq_payoffs), len(hado_def_eq_payoffs))

    ax = plt.subplot(1, 2, 1)
    plt.plot(range(len(do_def_eq_payoffs)), do_def_eq_payoffs, lw=my_lw, label='Def. eq.')
    plt.plot(range(len(do_def_dev_payoffs)), do_def_dev_payoffs, linestyle='--', \
        lw=my_lw, label='Def. dev.')

    plt.plot(range(len(do_att_eq_payoffs)), do_att_eq_payoffs, lw=my_lw, label='Att. eq.')
    plt.plot(range(len(do_att_dev_payoffs)), do_att_dev_payoffs, linestyle='--', \
        lw=my_lw, label='Att. dev.')

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Expected payoff', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    plt.title("DO-EGTA", fontsize=20)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 5
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.set_ylim(y_min - 25, y_max + 25)
    ax.set_xlim(-1, x_max + 1)
    ax.legend()
    plt.tick_params(
        axis='x',          # changes apply to the x-axis
        which='both',      # both major and minor ticks are affected
        top='off'         # ticks along the top edge are off
    )
    plt.tick_params(
        axis='y',          # changes apply to the y-axis
        which='both',      # both major and minor ticks are affected
        right='off'         # ticks along the right edge are off
    )
    plt.tight_layout()

    ax = plt.subplot(1, 2, 2)
    plt.plot(range(len(hado_def_eq_payoffs)), hado_def_eq_payoffs, lw=my_lw, \
        label='Def. eq.')
    plt.plot(range(len(hado_def_dev_payoffs)), hado_def_dev_payoffs, linestyle='--', \
        lw=my_lw, label='Def. dev.')

    plt.plot(range(len(hado_att_eq_payoffs)), hado_att_eq_payoffs, lw=my_lw, \
        label='Att. eq.')
    plt.plot(range(len(hado_att_dev_payoffs)), hado_att_dev_payoffs, linestyle='--', \
        lw=my_lw, label='Att. dev.')

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Expected payoff', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    plt.title("HADO-EGTA", fontsize=20)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 5
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.set_ylim(y_min - 25, y_max + 25)
    ax.set_xlim(-1, x_max + 1)
    ax.legend()
    plt.tick_params(
        axis='x',          # changes apply to the x-axis
        which='both',      # both major and minor ticks are affected
        top='off'         # ticks along the top edge are off
    )
    plt.tick_params(
        axis='y',          # changes apply to the y-axis
        which='both',      # both major and minor ticks are affected
        right='off'         # ticks along the right edge are off
    )
    plt.tight_layout()

    #plt.show()
    result_name = "r30_payoffs_vs_round_both.pdf"
    if not is_r30:
        result_name = "s29_payoffs_vs_round_both.pdf"
    savefig(result_name)

def main(is_r30):
    do_env_short_name_tsv = "d30n1"
    do_env_short_name_payoffs = "d30n1"
    hado_env_short_name_tsv = "d30cd1_randNoAndB"
    hado_env_short_name_payoffs = "d30cd1"
    game_number = 3014
    if not is_r30:
        do_env_short_name_tsv = "s29n1"
        do_env_short_name_payoffs = "s29n1"
        hado_env_short_name_tsv = "s29cs1_randNoAndB"
        hado_env_short_name_payoffs = "s29cs1"
        game_number = 3013
    do_round_count = get_round_count(do_env_short_name_tsv)
    do_att_eq_payoffs, do_def_eq_payoffs, do_att_net_payoffs, do_def_net_payoffs = \
        get_all_payoffs(do_round_count, do_env_short_name_tsv, do_env_short_name_payoffs, \
            game_number)
    do_att_net_payoffs = get_cutoff_dev_payoffs(do_att_eq_payoffs, do_att_net_payoffs)
    do_def_net_payoffs = get_cutoff_dev_payoffs(do_def_eq_payoffs, do_def_net_payoffs)
    print(do_att_eq_payoffs)
    print(do_att_net_payoffs)
    print(do_def_eq_payoffs)
    print(do_def_net_payoffs)
    hado_round_count = get_round_count(hado_env_short_name_tsv)
    hado_att_eq_payoffs, hado_def_eq_payoffs, hado_att_net_payoffs, hado_def_net_payoffs = \
        get_all_payoffs(hado_round_count, hado_env_short_name_tsv, \
            hado_env_short_name_payoffs, game_number)
    hado_att_net_payoffs = get_cutoff_dev_payoffs(hado_att_eq_payoffs, hado_att_net_payoffs)
    hado_def_net_payoffs = get_cutoff_dev_payoffs(hado_def_eq_payoffs, hado_def_net_payoffs)
    print(hado_att_eq_payoffs)
    print(hado_att_net_payoffs)
    print(hado_def_eq_payoffs)
    print(hado_def_net_payoffs)
    plot_payoffs_both(do_def_eq_payoffs, do_def_net_payoffs, \
        do_att_eq_payoffs, do_att_net_payoffs, hado_def_eq_payoffs, hado_def_net_payoffs, \
        hado_att_eq_payoffs, hado_att_net_payoffs, is_r30)

'''
example: python3 plot_payoffs_auto_both.py True
'''
if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ValueError("Need 1 args: is_r30")
    IS_R30 = get_truth_value(sys.argv[1])
    main(IS_R30)
