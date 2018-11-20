import sys
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as tick
from pylab import savefig
from plot_payoffs_auto import get_round_count, get_all_payoffs

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def get_gains(old_payoffs, new_payoffs):
    result = []
    for i in range(len(old_payoffs)):
        if len(new_payoffs) > i:
            if new_payoffs[i] is None:
                result.append(0)
            else:
                result.append(max(new_payoffs[i] - old_payoffs[i], 0))
    return result

def plot_gains(def_gains, att_gains, env_short_name_payoffs):
    fig, ax = plt.subplots()
    fig.set_size_inches(8, 5)

    my_lw = 2
    plt.plot(range(len(def_gains)), def_gains, lw=my_lw, label='Def. gain')
    plt.plot(range(len(att_gains)), att_gains, linestyle='--', lw=my_lw, \
        label='Att. gain')

    ax.axhline(0, color='black', lw=1)
    ax.axvline(0, color='black', lw=1)

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Payoff gain', fontsize=16)
    plt.xlabel('Training round', fontsize=16)
    ax.yaxis.set_ticks_position('left')
    ax.tick_params(labelsize=14)
    tick_spacing = 2
    ax.xaxis.set_major_locator(tick.MultipleLocator(tick_spacing))
    ax.legend()
    y_max = max(max(def_gains), max(att_gains))
    ax.set_ylim(-10, y_max + 10)
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
    savefig(env_short_name_payoffs + "_gain_vs_round.pdf")

def main(game_number, env_short_name_payoffs, env_short_name_tsv):
    round_count = get_round_count(env_short_name_tsv)
    att_eq_payoffs, def_eq_payoffs, att_net_payoffs, def_net_payoffs = \
        get_all_payoffs(round_count, env_short_name_tsv, env_short_name_payoffs, \
            game_number)
    print(att_eq_payoffs)
    print(att_net_payoffs)
    print(def_eq_payoffs)
    print(def_net_payoffs)
    att_gains = get_gains(att_eq_payoffs, att_net_payoffs)
    def_gains = get_gains(def_eq_payoffs, def_net_payoffs)
    plot_gains(def_gains, att_gains, env_short_name_payoffs)

'''
example: python3 plot_gains_auto.py 3013 s29m1 s29m1_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: game_number, env_short_name_payoffs, " + \
            "env_short_name_tsv")
    GAME_NUMBER = int(sys.argv[1])
    ENV_SHORT_NAME_PAYOFFS = sys.argv[2]
    ENV_SHORT_NAME_TSV = sys.argv[3]
    main(GAME_NUMBER, ENV_SHORT_NAME_PAYOFFS, ENV_SHORT_NAME_TSV)
