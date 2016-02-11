#!/bin/bash

rsync -avh --checksum \
    ~/git/zambia-stock-cards/ZambiaStockCards/input/stock-cards-all.csv .
rsync -avh --checksum \
    ~/git/zambia-paper/data/stockout-survey/output.csv stockout-survey.csv
