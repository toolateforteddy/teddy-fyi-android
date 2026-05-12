.PHONY: build clean install run

build:
	./gradlew assembleDebug

clean:
	./gradlew clean

install:
	adb install app/build/outputs/apk/debug/app-debug.apk

run:
	adb shell am start -n fyi.teddy.android/.MainActivity
