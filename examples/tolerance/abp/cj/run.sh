#!/bin/sh
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl abp.lts --prop p.lts"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl abp.lts --prop p.lts
