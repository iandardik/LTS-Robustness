#!/bin/sh

jarfile="../../bin/LTS-Robustness.jar"

echo "Voting wrt. P_cfm"
echo "-----------------"
echo "time java -jar ${jarfile} --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size
echo

echo "Voting wrt. P_all"
echo "-----------------"
echo "time java -jar ${jarfile} --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size
echo
