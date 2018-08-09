import matplotlib.pyplot as plt
from matplotlib import cm
import matplotlib.ticker as ticker
import numpy as np
from pylab import savefig

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

STRATEGY_SHORT_NAMES = {
    "RANDOM_WALK:numRWSample_100_qrParam_3.0": "RANDOM_WALK",
    "UNIFORM:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_stdev_0.0": "UNIFORM",
    "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.3_qrParam_3.0_stdev_0.0": "VP_.3_3",
    "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_1.0_stdev_0.0": "VP_.5_1",
    "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": "VP_.5_3",
    "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_1.0_numRWSample_30_isRandomized_0.0": "vsRW_1_1_0",
    "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": "vsRW_1_3_0",
    "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": "vsRW_1_3_1",
    "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_1.0_numRWSample_30_isRandomized_1.0": "vsRW_3_1_1",
    "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": "vsRW_3_3_1",
    "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": "vsRW_3_3_1",
    "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.3_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": "vsVP_.3_.3_3",
    "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_1.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": "vsVP_.3_.5_1",
    "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": "vsVP_.3_.5_3",
    "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.3_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": "vsVP_.5_.3_3",
    "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_1.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": "vsVP_.5_.5_1",
    "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": "vsVP_.5_.5_3"
}

D30_ATTACKER_HEURISTICS = [
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 1.0},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.10,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.3_qrParam_3.0_stdev_0.0": 0.65
    },
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.63},
    {},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.23,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.18
    },
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.39},
    {},
    {},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.28},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.34,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.26
    },
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.06},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.31},
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.08},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.33},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.14,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.15
    },
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.20,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.24
    },
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.71},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.38},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.35,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.11
    },
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.44,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.09
    },
    {},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.2,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.03
    },
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.37},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.43,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.08
    }
]

d30_defender_heuristics = [
    {"vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_1.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 1.0},
    {
        "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.22,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.02,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.02
    },
    {},
    {"vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.15},
    {
        "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.08,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.11
    },
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.05},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.11},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.08},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.08},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.03},
    {},
    {},
    {},
    {"vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_1.0_numRWSample_30_isRandomized_0.0": 0.07},
    {},
    {},
    {"vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_1.0_numRWSample_30_isRandomized_0.0": 0.16},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.09},
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.02,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.04
    },
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.01},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.07},
    {},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.04},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.06}
]

s29_attacker_heuristics = [
    {
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.3_qrParam_3.0_stdev_0.0": 0.63,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.37
    },
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.46,
        "UNIFORM:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_stdev_0.0": 0.02,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.3_qrParam_3.0_stdev_0.0": 0.02,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.02
    },
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.3_qrParam_3.0_stdev_0.0": 0.46},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.32},
    {},
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.36},
    {},
    {},
    {},
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.07},
    {},
    {},
    {},
    {
        "RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.11,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.19
    },
    {},
    {},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.15},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.30},
    {
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.3_qrParam_3.0_stdev_0.0": 0.6,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_1.0_stdev_0.0": 0.03
    },
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_1.0_stdev_0.0": 0.24},
    {},
    {"RANDOM_WALK:numRWSample_100_qrParam_3.0": 0.13},
    {},
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.01},
    {},
    {},
    {},
    {},
    {},
    {},
    {},
    {"VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_1.0_stdev_0.0": 0.05},
    {
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_1.0_stdev_0.0": 0.16,
        "VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0": 0.02
    }
]

s29_defender_heuristics = [
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.56,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.44
    },
    {
        "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.01,
        "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.29,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_1.0_numRWSample_30_isRandomized_1.0": 0.01,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.01,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.01,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_1.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.01,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.01,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_1.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.01
    },
    {
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.3_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.55,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.12
    },
    {
        "vsRANDOM_WALK:logisParam_1.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.02,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.17
    },
    {"vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.3_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.20},
    {
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.26,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.17
    },
    {"vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.80},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_1.0_numRWSample_30_isRandomized_1.0": 0.37},
    {"vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.44},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.45},
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.28,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.34
    },
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.44},
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.19,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.47
    },
    {"vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.34},
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.50,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.14
    },
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.20,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.3_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.14
    },
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.21},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.16},
    {},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.09},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.12},
    {},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.30},
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_0.0": 0.05,
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.16
    },
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.04,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.27
    },
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.27},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.3},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.48},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.16},
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.29,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.17
    },
    {
        "vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.35,
        "vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_3.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0": 0.03
    },
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.14},
    {"vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0": 0.14}
]

def plot_heuristic_use(epoch_to_heuristics, figure_title, save_title):
    heuristics = [x.keys() for x in epoch_to_heuristics]
    heuristics = sorted(list(set().union(*heuristics)))
    short_heuristics = [STRATEGY_SHORT_NAMES[x] for x in heuristics]
    epochs = len(epoch_to_heuristics)
    matrix = []
    for epoch in range(epochs):
        cur_epoch = epoch_to_heuristics[epoch]
        cur_row = []
        for heuristic in heuristics:
            if heuristic in cur_epoch:
                cur_row.append(cur_epoch[heuristic])
            else:
                cur_row.append(0)
        matrix.append(cur_row)

    for_imshow = np.array(matrix)

    fig, ax = plt.subplots()
    fig.set_size_inches(1.8 + len(heuristics) * 0.2, 6)

    cmap = cm.get_cmap('jet')
    im = ax.imshow(for_imshow, interpolation="nearest", cmap=cmap)

    tick_spacing = 2
    ax.xaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))
    ax.yaxis.set_major_locator(ticker.MultipleLocator(tick_spacing))

    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    plt.ylabel('Training round', fontsize=16)
    plt.xlabel('Heuristic used', fontsize=16)
    plt.title(figure_title, fontsize=20)

    plt.xticks(np.arange(len(heuristics)), tuple(short_heuristics), rotation=90)

    cbar = fig.colorbar(im, fraction=0.046, pad=0.04)
    cbar.ax.tick_params(labelsize=12)

    ax.tick_params(labelsize=14)
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

    # plt.show()
    savefig(save_title)

plot_heuristic_use(D30_ATTACKER_HEURISTICS, "$r_{30}$ attacker", "d30_att_heuristics.pdf")
plot_heuristic_use(d30_defender_heuristics, "$r_{30}$ defender", "d30_def_heuristics.pdf")
plot_heuristic_use(s29_attacker_heuristics, "$s_{29}$ attacker", "s29_att_heuristics.pdf")
plot_heuristic_use(s29_defender_heuristics, "$s_{29}$ defender", "s29_def_heuristics.pdf")
