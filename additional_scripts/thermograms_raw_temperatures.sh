#!/bin/bash
# Извлекает из всех термограмм из папки DIR_THERMOGRAMS необработанные температурные данные в файлы в папке
# ./SUBDIR_RAW_TEMPS.

DIR_THERMOGRAMS=$(grep "^DIR_THERMOGRAMS" config.txt | cut -d'=' -f 2 | sed 's/^ //')

SUBDIR_RAW_TEMPS=$(grep "^SUBDIR_RAW_TEMPS" config.txt | cut -d'=' -f 2 | sed 's/^ //')


for file in "$DIR_THERMOGRAMS"/*; do
  bash raw.sh "$file" ./"$SUBDIR_RAW_TEMPS"
done