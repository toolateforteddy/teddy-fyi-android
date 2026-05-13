.PHONY: build clean install run

build:
	./gradlew assembleDebug

clean:
	./gradlew clean

# Default to the physical device if one is connected
DEVICE_ID ?= $(shell adb devices | grep -v "List" | grep "device$$" | awk '{print $$1}' | head -n 1)


set_device_id:
	@if [ -z "$(DEVICE_ID)" ]; then \
		echo "No device found. Start an emulator or plug in your phone."; \
		exit 1; \
	fi
	@echo "Running on $(DEVICE_ID)..."

install: set_device_id
	adb -s $(DEVICE_ID) install app/build/outputs/apk/debug/app-debug.apk

run: set_device_id
	adb -s $(DEVICE_ID) shell am start -n fyi.teddy.android/.MainActivity