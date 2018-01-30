
import os
import matplotlib.pyplot as plt

def getLines(fileName):
    lines = None
    with open(fileName) as f:
        lines = f.readlines()
    return lines

def getRewardsList(lines):
    return getValuesList(lines, "episode reward")

def getEpisodesList(lines):
    return getValuesList(lines, "episodes")

def getValuesList(lines, stringIndicator):
    result = []
    for line in lines:
        if stringIndicator in line:
            firstBarIndex = line.find("|")
            secondBarIndex = line.find("|", firstBarIndex + 1)
            thirdBarIndex = line.find("|", secondBarIndex + 1)
            innerPart = line[secondBarIndex + 1:thirdBarIndex].strip()
            floatValue = float(innerPart)
            result.append(floatValue)
    return result

def myPlot(episodes, rewards, save_name):
    fig, ax = plt.subplots()
    plt.plot(episodes, rewards, lw=3, c='blue')

    labelSize = 20
    plt.xlabel("Episode", fontsize=labelSize)
    plt.ylabel("Defender\nreward", fontsize=labelSize)
    plt.tight_layout()
    #plt.show()
    plt.savefig(save_name)

def main():
    base_name = "tdj_conv4"
    file_name = base_name + ".txt"
    save_name = base_name + ".pdf"
    lines = getLines(file_name)
    episodes_list = getEpisodesList(lines)
    rewards_list = getRewardsList(lines)
    # print(rewards_list)
    myPlot(episodes_list, rewards_list, save_name)

if __name__ == "__main__":
    main()