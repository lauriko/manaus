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

sbt "run-main com.getjenny.manaus.commands.KeywordExtraction --raw_conversation data/conversations.txt --word_frequencies data/word_frequency.tsv"

## run from zip packet

./manaus-0.1/bin/manaus --raw_conversation data/conversations.txt --word_frequencies data/word_frequency.tsv
