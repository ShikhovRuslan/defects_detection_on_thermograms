#!/bin/bash
# Копирует EXIF-параметры термограмм из папки DIR_THERMOGRAMS: GPS Latitude, GPS Longitude - в данные
# EXIF картинки с дефектами ./SUBDIR_DEFECTS/<thermogram_name>POSTFIX_DEFECTS.jpg.

DIR_THERMOGRAMS=$(grep "^DIR_THERMOGRAMS" config.txt | cut -d'=' -f 2 | sed 's/^ //')

SUBDIR_DEFECTS=$(grep "^SUBDIR_DEFECTS" config.txt | cut -d'=' -f 2 | sed 's/^ //')

POSTFIX_DEFECTS=$(grep "^POSTFIX_DEFECTS" config.txt | cut -d'=' -f 2 | sed 's/^ //')


TAGS="-exif:GPSLatitude -exif:GPSLongitude"

for file in "$DIR_THERMOGRAMS"/*; do
  THERMOGRAM_NAME=$(basename "$file" .${file##*.})
  exiftool -tagsFromFile "$file" $TAGS "./$SUBDIR_DEFECTS/$THERMOGRAM_NAME$POSTFIX_DEFECTS.jpg"
done

rm "./$SUBDIR_DEFECTS"/*_original