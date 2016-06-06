LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += org.codeaurora.gallery.common
LOCAL_STATIC_JAVA_LIBRARIES += xmp_toolkit
LOCAL_STATIC_JAVA_LIBRARIES += mp4parser

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-renderscript-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, src_pd)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_AAPT_FLAGS := --auto-add-overlay

LOCAL_PACKAGE_NAME := Gallery2

LOCAL_PRIVILEGED_MODULE := true

LOCAL_OVERRIDES_PACKAGES := Gallery Gallery3D GalleryNew3D

LOCAL_AAPT_FLAGS += --rename-manifest-package com.android.gallery3d

LOCAL_SDK_VERSION := current
LOCAL_MIN_SDK_VERSION := 23

LOCAL_JNI_SHARED_LIBRARIES := libjni_gallery_eglfence libjni_gallery_filters libjni_gallery_jpegstream
LOCAL_SHARED_LIBRARIES += libjni_dualcamera libjni_trueportrait libjni_filtergenerator

LOCAL_REQUIRED_MODULES := libts_detected_face_jni libts_face_beautify_jni

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

include $(BUILD_PACKAGE)

ifeq ($(strip $(LOCAL_PACKAGE_OVERRIDES)),)

# Use the following include to make gallery test apk
include $(call all-makefiles-under, $(LOCAL_PATH))

endif
