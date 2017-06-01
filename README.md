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

sbt "run-main com.getjenny.manaus.commands.CalculateKeywordsForSentences --raw_conversation data/conversations.txt --word_frequencies data/word_frequency.tsv --output_file data/output.csv"

The input format is a semicolumn separated value file with the following fields:
```sentence, tokenized_sentence, type, conv_id, sentence_id```

The output format is a semicolumn separated value file with the following fields:
```sentence, tokenized_sentence, type, conv_id, sentence_id, keywords```

sbt "run-main com.getjenny.manaus.commands.CalculateKeywordsForSentences --raw_conversation data/conversations.txt --word_frequencies data/word_frequency.tsv --output_file data/output.csv

## run from zip packet

./manaus-0.1/bin/calculate-keywords-for-sentences --raw_conversation data/conversations.txt --word_frequencies data/word_frequency.tsv
