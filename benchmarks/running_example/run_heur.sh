jarfile="../../bin/LTS-Robustness.jar"

echo "Running Example"
echo "---------------"
echo "time java -jar ${jarfile} --verbose --env env.lts --ctrl ctrl.lts --prop p.lts --largest-delta-size"
time java -jar "${jarfile}" --verbose --env env.lts --ctrl ctrl.lts --prop p.lts --largest-delta-size
echo
