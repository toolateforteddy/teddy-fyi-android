.PHONY: build build-full build-grocery clean install install-grocery run run-grocery test

# Both product flavours. `make build-full` / `make build-grocery` for one at a time.
build:
	./gradlew assembleDebug

build-full:
	./gradlew assembleFullDebug

build-grocery:
	./gradlew assembleGroceryDebug

clean:
	./gradlew clean

# The tests are shared by both flavours, so one variant task runs all of them.
test:
	./gradlew testFullDebugUnitTest

# Default to the physical device if one is connected
DEVICE_ID ?= $(shell adb devices | grep -v "List" | grep "device$$" | awk '{print $$1}' | head -n 1)


set_device_id:
	@if [ -z "$(DEVICE_ID)" ]; then \
		echo "No device found. Start an emulator or plug in your phone."; \
		exit 1; \
	fi
	@echo "Running on $(DEVICE_ID)..."

install: set_device_id
	adb -s $(DEVICE_ID) install app/build/outputs/apk/full/debug/app-full-debug.apk

install-grocery: set_device_id
	adb -s $(DEVICE_ID) install app/build/outputs/apk/grocery/debug/app-grocery-debug.apk

run: set_device_id
	adb -s $(DEVICE_ID) shell am start -n fyi.teddy.android/.MainActivity

# The grocery build carries its own applicationId so both can sit on one device.
run-grocery: set_device_id
	adb -s $(DEVICE_ID) shell am start -n fyi.teddy.android.grocery/fyi.teddy.android.MainActivity
