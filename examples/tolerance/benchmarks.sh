#!/bin/sh

pushd running_example/
./run.sh
popd

pushd therac25/lt_based/
./run.sh
popd

pushd voting/ian2/
./run.sh
popd
