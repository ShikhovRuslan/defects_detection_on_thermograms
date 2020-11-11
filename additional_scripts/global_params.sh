#!/bin/bash
# Извлекает общие для всех термограмм параметры: Focal Length, Planck R1, Planck R2, Planck O, Planck B,
# Planck F, emissivity, Reflected Apparent Temperature, Raw Thermal Image Height, Raw Thermal Image Width
# - из данных EXIF первой термограммы из папки DIR_THERMOGRAMS в файл ./SUBDIR_OUTPUT/global_params.txt в
# формате JSON.

DIR_THERMOGRAMS=$(grep "^DIR_THERMOGRAMS" config.txt | cut -d'=' -f 2 | sed 's/^ //')

SUBDIR_OUTPUT=$(grep "^SUBDIR_OUTPUT" config.txt | cut -d'=' -f 2 | sed 's/^ //')


for file in "$DIR_THERMOGRAMS"/*; do
  FIRST_THERMOGRAM=$file
  break 1
done
OUTPUT_FILE=$SUBDIR_OUTPUT/global_params.txt
TAGS_1="-EXIF:FocalLength -FLIR:PlanckR1 -FLIR:PlanckR2 -FLIR:PlanckO -FLIR:PlanckB -FLIR:PlanckF -FLIR:emissivity"
TAGS_2="-FLIR:ReflectedApparentTemperature -FLIR:RawThermalImageHeight -FLIR:RawThermalImageWidth"
TAGS="$TAGS_1 $TAGS_2"

exiftool -j -n $TAGS -r "$FIRST_THERMOGRAM" -w+! %0f"$OUTPUT_FILE"

sed "s/[][]//g" "$OUTPUT_FILE" -i

rm -f ./sed*.