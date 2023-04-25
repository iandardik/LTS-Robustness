#!/bin/sh

jarfile="../../bin/LTS-Robustness.jar"

# therac-25 w/bug
echo "Therac-25 w/bug"
echo "---------------"
echo "time java -jar ${jarfile} --verbose --env env_safe.lts --ctrl sys.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env env_safe.lts --ctrl sys.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size
echo

# therac-25 w/fix
echo "Therac-25 w/fix"
echo "---------------"
echo "time java -jar ${jarfile} --verbose --env env_safe.lts --ctrl sys_fixed.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env env_safe.lts --ctrl sys_fixed.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size
echo

# therac-20
echo "Therac-20"
echo "---------"
echo "time java -jar ${jarfile} --verbose --env env_safe.lts --ctrl sys_interlock.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env env_safe.lts --ctrl sys_interlock.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size
echo
