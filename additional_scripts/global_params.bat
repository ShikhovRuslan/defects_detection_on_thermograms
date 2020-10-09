@rem Извлекает общие для всех термограмм параметры: Focal Length, Planck R1, Planck R2, Planck O, Planck B, 
@rem Planck F, emissivity, Reflected Apparent Temperature, Raw Thermal Image Height, Raw Thermal Image Width 
@rem - из данных EXIF первой термограммы из папки THERMOGRAMS_DIR в файл .\OUTPUT_SUBDIR\global_params.txt в 
@rem формате JSON.

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^EXIFTOOL" config.txt') do set EXIFTOOL=%%i
set EXIFTOOL=%EXIFTOOL:~1%

for /f "tokens=2 delims==" %%i in ('findstr "^SED" config.txt') do set SED=%%i
set SED="%SED:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^THERMOGRAMS_DIR" config.txt') do set THERMOGRAMS_DIR=%%i
set THERMOGRAMS_DIR="%THERMOGRAMS_DIR:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^OUTPUT_SUBDIR" config.txt') do set OUTPUT_SUBDIR=%%i
set OUTPUT_SUBDIR=%OUTPUT_SUBDIR:~1%


for %%i in (%THERMOGRAMS_DIR%\*) do (
  set FIRST_THERMOGRAM="%%i"
  goto :continue
)
:continue
set OUTPUT_FILE=".\%OUTPUT_SUBDIR%\global_params.txt"
set TAGS_1=-EXIF:FocalLength -FLIR:PlanckR1 -FLIR:PlanckR2 -FLIR:PlanckO -FLIR:PlanckB -FLIR:PlanckF -FLIR:emissivity
set TAGS_2=-FLIR:ReflectedApparentTemperature -FLIR:RawThermalImageHeight -FLIR:RawThermalImageWidth
set TAGS=%TAGS_1% %TAGS_2%

%EXIFTOOL% -j -n %TAGS% -r %FIRST_THERMOGRAM% -w+! %%0f%OUTPUT_FILE%

%SED% "s/[][]//g" %OUTPUT_FILE% -i

del .\sed*.