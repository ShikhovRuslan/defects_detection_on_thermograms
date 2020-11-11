@rem Извлекает из всех термограмм из папки THERMOGRAMS_DIR необработанные температурные данные в файлы в папке
@rem .\SUBDIR_RAW_TEMPS.

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^THERMOGRAMS_DIR" config.txt') do set THERMOGRAMS_DIR=%%i
set THERMOGRAMS_DIR="%THERMOGRAMS_DIR:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^SUBDIR_RAW_TEMPS" config.txt') do set SUBDIR_RAW_TEMPS=%%i
set SUBDIR_RAW_TEMPS=%SUBDIR_RAW_TEMPS:~1%


for %%i in (%THERMOGRAMS_DIR%\*) do raw.bat "%%i" ".\%SUBDIR_RAW_TEMPS%"