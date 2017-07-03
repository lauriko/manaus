# Keyword Extractor

Temporary project to implement a topic extractor.

In a nutshell,...


## Compile

```bash
sbt compile
```

## zip packet generation

```bash
sbt dist
```

## run from sbt

```bash
sbt "run-main com.getjenny.manaus.commands.CalculateKeywordsForSentences --raw_conversation data/conversations.txt --word_frequencies statistics_data/english/word_frequency.tsv --output_file data/output.csv"
```

The input format is a semicolumn separated value file with the following fields:
```sentence, tokenized_sentence, type, conv_id, sentence_id```

The output format is a semicolumn separated value file with the following fields:
```sentence, tokenized_sentence, type, conv_id, sentence_id, keywords```

```bash
sbt "run-main com.getjenny.manaus.commands.CalculateKeywordsForSentences --raw_conversation data/conversations.txt --word_frequencies statistics_data/english/word_frequency.tsv --output_file data/output.csv
```

## run from zip packet

```bash
./manaus-0.1/bin/calculate-keywords-for-sentences --raw_conversation data/conversations.txt --word_frequencies statistics_data/english/word_frequency.tsv
```

## Docker

### generate a docker container

```bash
sbt sbt docker:publishLocal
```

### publish on docker cloud

```bash
sbt sbt docker:publish
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

