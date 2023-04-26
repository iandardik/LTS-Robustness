#!/bin/bash

pushd running_example/
echo
./run_heur.sh
popd

pushd therac25/
echo
./run_heur.sh
popd

pushd voting/
echo
./run_heur.sh
popd

pushd oyster/
echo
./run_heur.sh
popd

pushd pump/
echo
./run_heur.sh
popd
