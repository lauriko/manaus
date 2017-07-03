#!/usr/bin/env

docker run -i -t --rm --name manaus --volume `pwd`/manaus/data:/manaus/data --volume `pwd`/manaus/log:/manaus/log elegansio/manaus:latest /manaus/bin/continuous-keywords-update --temp_data_folder /manaus/data --host_map "172.17.0.1=9300" --interval_sec 120 --word_frequencies /manaus/data/english/word_frequency.tsv

