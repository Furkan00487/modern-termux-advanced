LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libtermux-bootstrap
LOCAL_SRC_FILES := termux-bootstrap-zip.S termux-bootstrap.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := termux-clipboard-api
LOCAL_MODULE_FILENAME := libtermux-clipboard-api.so
LOCAL_SRC_FILES := termux-api.c termux-api-broadcast.c
LOCAL_LDFLAGS := -pie
include $(BUILD_EXECUTABLE)
