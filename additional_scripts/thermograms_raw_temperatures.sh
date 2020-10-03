#!/bin/bash
# Извлекает из всех термограмм из папки THERMOGRAMS_DIR необработанные температурные данные в файлы в папке .\RAW_SUBDIR.

THERMOGRAMS_DIR=$(grep "^THERMOGRAMS_DIR" config.txt | cut -d'=' -f 2 | sed 's/^ //')

RAW_SUBDIR=$(grep "^RAW_SUBDIR" config.txt | cut -d'=' -f 2 | sed 's/^ //')


for file in $THERMOGRAMS_DIR/*; do
  bash raw.sh $file ./$RAW_SUBDIR
done