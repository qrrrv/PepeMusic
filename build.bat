@echo off
echo ================================
echo   СБОРКА МУЗЫКАЛЬНОГО ПЛЕЕРА
echo   (Нативное Android приложение)
echo ================================
echo.

echo Этот скрипт соберет APK автоматически!
echo.

echo [1/3] Проверка окружения...
if not exist "gradlew.bat" (
    echo Создаю gradlew...
    echo @echo off > gradlew.bat
    echo echo Gradle Wrapper >> gradlew.bat
    echo java -jar gradle/wrapper/gradle-wrapper.jar %* >> gradlew.bat
)

echo.
echo [2/3] Выбери тип сборки:
echo 1 - Debug APK (быстрая сборка, для тестов)
echo 2 - Release APK (оптимизированная версия)
echo.
set /p choice="Твой выбор (1 или 2): "

if "%choice%"=="1" (
    echo.
    echo Собираю Debug APK...
    call gradlew assembleDebug
    if %errorlevel% equ 0 (
        echo.
        echo ================================
        echo   ✓ УСПЕШНО!
        echo ================================
        echo APK создан:
        echo %CD%\app\build\outputs\apk\debug\app-debug.apk
        echo.
        echo Скопируй этот файл на телефон и установи!
        start explorer.exe app\build\outputs\apk\debug
    )
) else if "%choice%"=="2" (
    echo.
    echo Собираю Release APK...
    call gradlew assembleRelease
    if %errorlevel% equ 0 (
        echo.
        echo ================================
        echo   ✓ УСПЕШНО!
        echo ================================
        echo APK создан:
        echo %CD%\app\build\outputs\apk\release\app-release.apk
        echo.
        echo Скопируй этот файл на телефон и установи!
        start explorer.exe app\build\outputs\apk\release
    )
) else (
    echo Неверный выбор!
)

echo.
echo [3/3] Готово!
pause
