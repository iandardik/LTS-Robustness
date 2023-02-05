#!/bin/sh

echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env human.lts --ctrl gates.lts --prop p.lts --env-prop envp_in_then_out.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env human.lts --ctrl gates.lts --prop p.lts --env-prop envp_in_then_out.lts --largest-delta-size
