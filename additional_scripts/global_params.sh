#!/bin/bash
# Извлекает общие для всех термограмм параметры: Focal Length, Planck R1, Planck R2, Planck O, Planck B,
# Planck F, emissivity, Reflected Apparent Temperature, Raw Thermal Image Height, Raw Thermal Image Width
# - из данных EXIF первой термограммы из папки THERMOGRAMS_DIR в файл ./OUTPUT_SUBDIR/global_params.txt в
# формате JSON.

THERMOGRAMS_DIR=$(grep "^THERMOGRAMS_DIR" config.txt | cut -d'=' -f 2 | sed 's/^ //')

OUTPUT_SUBDIR=$(grep "^OUTPUT_SUBDIR" config.txt | cut -d'=' -f 2 | sed 's/^ //')


for file in $THERMOGRAMS_DIR; do
  echo $THERMOGRAMS_DIR
  for file in $THERMOGRAMS_DIR/*; do
    FIRST_THERMOGRAM=$file
    break 1
  done
done
OUTPUT_FILE=$OUTPUT_SUBDIR/global_params.txt
TAGS="-EXIF:FocalLength -FLIR:PlanckR1 -FLIR:PlanckR2 -FLIR:PlanckO -FLIR:PlanckB -FLIR:PlanckF -FLIR:emissivity -FLIR:ReflectedApparentTemperature -FLIR:RawThermalImageHeight -FLIR:RawThermalImageWidth"

exiftool -j -n $TAGS -r $FIRST_THERMOGRAM -w+! %0f$OUTPUT_FILE

sed "s/[][]//g" $OUTPUT_FILE -i

rm -f ./sed*.