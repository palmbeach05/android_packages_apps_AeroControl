LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
 
LOCAL_SRC_FILES += \
    $(call all-java-files-under, src) \
    $(call all-java-files-under, library)

# The bundled library sources reference their own R classes (e.g.
# com.getbase.floatingactionbutton.R, com.db.williamchart.R). Since they are
# compiled straight into the AeroControl package instead of as separate
# library modules, their resource directories must be merged in and their
# packages must be told to aapt via --extra-packages so a matching R.java is
# generated for each of them.
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res

LOCAL_AAPT_FLAGS := \
    --extra-packages com.getbase.floatingactionbutton:com.echo.holographlibrary:com.cocosw.undobar:com.db.williamchart:fr.nicolaspomepuy.discreetapprate:com.github.amlcurran.showcaseview:com.ikimuhendis.ldrawer

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4_13 \
    aerocontrol-nineoldandroids \
    aerocontrol-support-annotations \
    aerocontrol-jetbrains-annotations

LOCAL_PACKAGE_NAME := AeroControl
LOCAL_CERTIFICATE := platform

LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)

# Support library v4
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    android-support-v4_13:libs/android-support-v4.jar \
    aerocontrol-nineoldandroids:libs/nineoldandroids-2.4.0.jar \
    aerocontrol-support-annotations:libs/android-support-annotations.jar \
    aerocontrol-jetbrains-annotations:library/HoloGraphLibrary/libs/annotations-12.0.jar

include $(BUILD_MULTI_PREBUILT)
