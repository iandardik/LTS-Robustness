#!/bin/sh

echo 'Oyster'
echo '------'
echo 'time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env human.lts --ctrl gates.lts --prop p.lts --env-prop envp_in_then_out.lts --largest-delta-size --naive-bf'
time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env human.lts --ctrl gates.lts --prop p.lts --env-prop envp_in_then_out.lts --largest-delta-size --naive-bf
echo
