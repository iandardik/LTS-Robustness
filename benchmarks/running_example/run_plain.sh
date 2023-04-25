jarfile="../../bin/LTS-Robustness.jar"

echo "Running Example"
echo "---------------"
echo "time timeout 5m java -jar ${jarfile} --verbose --env env.lts --ctrl ctrl.lts --prop p.lts --largest-delta-size --naive-bf"
time timeout 5m java -jar "${jarfile}" --verbose --env env.lts --ctrl ctrl.lts --prop p.lts --largest-delta-size --naive-bf
echo
