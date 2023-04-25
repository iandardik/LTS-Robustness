#!/bin/sh

pushd running_example/
echo
./run_plain.sh
popd

pushd therac25/
echo
./run_plain.sh
popd

pushd voting/
echo
./run_plain.sh
popd

pushd oyster/
echo
./run_plain.sh
popd

pushd pump/
echo
./run_plain.sh
popd
