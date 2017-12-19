# Keyword Extractor

Library used by StarChat for topic extraction

## Make zip

```bash
sbt dist
```

## Extract keywords

```bash
sbt "runMain com.getjenny.manaus.commands.CalculateKeywordsForSentencesSimplerFormat \
--raw_conversations data/conversations.txt \
--separator , \
--word_frequencies statistics_data/english/word_frequency.tsv \
--output_file data/output.csv"
```

The input format is a  separator-separated value file with the following fields:
```sentence_id SEPARATOR sentence```

The output format is a semicolumn separated value file with the following fields:

```
sentence_id;"keyword1|score keyword2|score .... "
```

## run from zip packet

```bash
./manaus-0.1/bin/calculate-keywords-for-sentences --raw_conversations data/conversations.txt --word_frequencies statistics_data/english/word_frequency.tsv
```

## Docker

### generate a docker container

```bash
sbt docker:publishLocal
```

### publish on docker cloud

```bash
sbt docker:publish
```

### create a latest tag and publish

```bash
docker tag elegansio/manaus:<tag name> elegansio/manaus:latest

docker push elegansio/manaus:latest
```

### run commands from docker container

```bash
docker run -i -t --rm --name manaus elegansio/manaus:<version> <program>
```

example: get the list of programs:
```bash
docker run -i -t --rm --name manaus elegansio/manaus:latest ls manaus/bin
```

example: run a program
```bash
docker run -i -t --rm --name manaus elegansio/manaus:latest manaus/bin/get-dataset-from-e-s --help
```

### Calculating keywords for all the indexes

```bash
./manaus/bin/continuous-keywords-update-all-indexes --temp_data_folder manaus/data --host_map localhost=9300 --interval_sec 5 --word_frequencies manaus/statisti
cs_data --cluster_name starchat 
```

