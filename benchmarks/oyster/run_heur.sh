#!/bin/sh

jarfile="../../bin/LTS-Robustness.jar"

echo "Oyster"
echo "------"
echo "time java -jar ${jarfile} --verbose --env human.lts --ctrl gates.lts --prop p.lts --env-prop envp_in_then_out.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env human.lts --ctrl gates.lts --prop p.lts --env-prop envp_in_then_out.lts --largest-delta-size
echo
