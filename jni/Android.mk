LOCAL_PATH:= $(call my-dir)

Gallery2_jni_cflags := \
    -Wall -Wextra -Werror \
    -Wno-error=constant-conversion \
    -Wno-unused-parameter \

# to fix implicit conversion from 'int' to 'char', (255 to -1, 128 to -128)

include $(CLEAR_VARS)

LOCAL_CFLAGS += -DEGL_EGLEXT_PROTOTYPES
LOCAL_CFLAGS += $(Gallery2_jni_cflags)

LOCAL_SRC_FILES := jni_egl_fence.cpp

LOCAL_SDK_VERSION := 9

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libjni_gallery_eglfence

LOCAL_PRODUCT_MODULE := true

LOCAL_LDLIBS := -llog -lEGL


include $(BUILD_SHARED_LIBRARY)

# Filtershow

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc
LOCAL_SDK_VERSION := 9
LOCAL_MODULE    := libjni_gallery_filters
LOCAL_PRODUCT_MODULE := true
LOCAL_SRC_FILES := filters/gradient.c \
                   filters/saturated.c \
                   filters/exposure.c \
                   filters/edge.c \
                   filters/contrast.c \
                   filters/hue.c \
                   filters/shadows.c \
                   filters/highlight.c \
                   filters/hsv.c \
                   filters/vibrance.c \
                   filters/geometry.c \
                   filters/negative.c \
                   filters/redEyeMath.c \
                   filters/fx.c \
                   filters/wbalance.c \
                   filters/redeye.c \
                   filters/bwfilter.c \
                   filters/tinyplanet.cc \
                   filters/kmeans.cc

LOCAL_CFLAGS += -ffast-math -O3 -funroll-loops
LOCAL_CFLAGS += $(Gallery2_jni_cflags)
LOCAL_LDLIBS := -llog -ljnigraphics
LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)
