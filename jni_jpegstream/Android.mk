LOCAL_PATH:= $(call my-dir)

# Jpeg Streaming native

include $(CLEAR_VARS)

LOCAL_MODULE        := libjni_gallery_jpegstream

LOCAL_NDK_STL_VARIANT := c++_static

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
                    $(LOCAL_PATH)/src

LOCAL_STATIC_LIBRARIES := libjpeg_static_ndk

LOCAL_SDK_VERSION   := 9
LOCAL_ARM_MODE := arm
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_CFLAGS    += -ffast-math -O3 -funroll-loops
LOCAL_CFLAGS += -Wall -Wextra -Werror
LOCAL_LDLIBS := -llog

LOCAL_CPP_EXTENSION := .cpp
LOCAL_SRC_FILES     := \
    src/inputstream_wrapper.cpp \
    src/jpegstream.cpp \
    src/jerr_hook.cpp \
    src/jpeg_hook.cpp \
    src/jpeg_writer.cpp \
    src/jpeg_reader.cpp \
    src/outputstream_wrapper.cpp \
    src/stream_wrapper.cpp


include $(BUILD_SHARED_LIBRARY)
