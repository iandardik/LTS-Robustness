#!/bin/sh

for envpf in $(ls envp*.lts)
do
    cmd="time java -jar /Users/idardik/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --env env_fc.lts --ctrl sys_no_fc.lts --prop p.lts --env-prop ${envpf} --print-fsp"
    echo "${cmd}"
    #`${cmd}`
    time java -jar /Users/idardik/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --env env_fc.lts --ctrl sys_no_fc.lts --prop p.lts --env-prop "${envpf}" --print-fsp
done
