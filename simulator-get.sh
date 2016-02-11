#!/bin/bash

# Before running this script, make sure to do the following in the
# git/invsim repository
# 
#  1. git pull origin master
#  2. git clean -xn .

rsync -avz ~/git/invsim/InventorySimulator simulator/

