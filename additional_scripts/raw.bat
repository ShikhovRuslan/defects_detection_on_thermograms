@rem Извлекает из указанной термограммы необработанные температурные данные в файл <thermogram_name>POSTFIX_RAW_TEMPS.pgm
@rem в указанной папке.
@rem %1 - термограмма
@rem %2 - папка, содержащая файл с необработанными температурными данными термограммы

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^EXIFTOOL" config.txt') do set EXIFTOOL=%%i
set EXIFTOOL=%EXIFTOOL:~1%

for /f "tokens=2 delims==" %%i in ('findstr "^SED" config.txt') do set SED=%%i
set SED="%SED:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^POSTFIX_RAW_TEMPS" config.txt') do set POSTFIX_RAW_TEMPS=%%i
set POSTFIX_RAW_TEMPS=%POSTFIX_RAW_TEMPS:~1%


for %%a in (%1) do set OUTPUT_FILE=%%~na
set OUTPUT_FILE=%2\%OUTPUT_FILE%%POSTFIX_RAW_TEMPS%

%EXIFTOOL% -b -RawThermalImage %1 | convert - %OUTPUT_FILE%.png

convert %OUTPUT_FILE%.png -compress none %OUTPUT_FILE%.pgm

%SED% -i 1,3d %OUTPUT_FILE%.pgm

del %OUTPUT_FILE%.png
del .\sed*.