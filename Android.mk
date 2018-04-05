LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.fragment_fragment \
    androidx.legacy_legacy-support-core-ui \
    androidx.core_core \
    androidx.legacy_legacy-support-v13

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.android.gallery3d.common2 \
    xmp_toolkit \
    mp4parser

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-renderscript-files-under, src) \
    $(call all-java-files-under, src_pd)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := Gallery2

LOCAL_OVERRIDES_PACKAGES := Gallery Gallery3D GalleryNew3D

LOCAL_SDK_VERSION := current

LOCAL_JNI_SHARED_LIBRARIES := \
    libjni_eglfence \
    libjni_filtershow_filters \
    libjni_jpegstream

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

LOCAL_JARJAR_RULES := $(LOCAL_PATH)/jarjar-rules.txt

include $(BUILD_PACKAGE)

ifeq ($(strip $(LOCAL_PACKAGE_OVERRIDES)),)

# Use the following include to make gallery test apk
include $(call all-makefiles-under, $(LOCAL_PATH))

endif
