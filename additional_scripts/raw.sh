#!/bin/bash
# Извлекает из указанной термограммы необработанные температурные данные в файл <thermogram_name>POSTFIX_RAW.pgm
# в указанной папке.
# $1 - термограмма
# $2 - папка, содержащая файл с необработанными температурными данными термограммы

POSTFIX_RAW=$(grep "^POSTFIX_RAW" config.txt | cut -d'=' -f 2 | sed 's/^ //')


OUTPUT_FILE=$(basename $1 .${1##*.})
OUTPUT_FILE=$2/$OUTPUT_FILE$POSTFIX_RAW

exiftool -b -RawThermalImage $1 | convert - $OUTPUT_FILE.png

convert $OUTPUT_FILE.png -compress none $OUTPUT_FILE.pgm

sed -i 1,3d $OUTPUT_FILE.pgm

rm -f $OUTPUT_FILE.png
rm -f ./sed*.