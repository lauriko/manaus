#!/usr/bin/env bash

WORKDIR=$( readlink -e "`dirname $0`/../")

docker run -i -t --rm --name manaus --volume `pwd`/manaus/data:/manaus/data --volume `pwd`/manaus/log:/manaus/log elegansio/manaus:latest rm -f /manaus/data/{__data_keywords.txt,__data_tokenized.txt}

docker run -i -t --rm --name manaus --volume ${WORKDIR}/manaus/data:/manaus/data --volume ${WORKDIR}/manaus/log:/manaus/log elegansio/manaus:latest /manaus/bin/get-tokens-from-e-s --output_file /manaus/data/__data_tokenized.txt --host_map "172.17.0.1=9300"

docker run -i -t --rm --name manaus --volume ${WORKDIR}/manaus/data:/manaus/data --volume ${WORKDIR}/manaus/log:/manaus/log elegansio/manaus:latest /manaus/bin/calculate-keywords-for-sentences-simpler-format --raw_conversations /manaus/data/__data_tokenized.txt --word_frequencies /manaus/statistics_data/english/word_frequency.tsv --output_file /manaus/data/__data_keywords.txt

docker run -i -t --rm --name manaus --volume ${WORKDIR}/manaus/data:/manaus/data --volume ${WORKDIR}/manaus/log:/manaus/log elegansio/manaus:latest /manaus/bin/upload-keywords --input_file manaus/data/__data_keywords.txt --host_map "172.17.0.1=9300"

