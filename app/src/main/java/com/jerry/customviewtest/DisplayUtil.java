package com.jerry.customviewtest;

import java.lang.reflect.Method;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.util.Log;
import android.view.View;

public class DisplayUtil {

    public static final int NOTCH_IN_SCREEN_VIVO = 0x00000020;//是否有凹槽
    private static final String TAG = "DisplayUtil";

    private DisplayUtil() {
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(double pxValue) {
        return (int) (pxValue / getDisplayDensity() + 0.5f);
    }

    /**
     * 获取屏幕密度
     */
    public static float getDisplayDensity() {
        return Resources.getSystem().getDisplayMetrics().density;
    }

    /**
     * 获取手机屏幕的像素高
     */
    public static int getDisplayHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取手机屏幕的像素宽
     */
    public static int getDisplayWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    /**
     * 获得View在屏幕的位置
     */
    public static Rect getRectOnScreen(View v) {
        if (v == null) {
            return new Rect();
        }
        int[] point = new int[2];
        v.getLocationOnScreen(point);
        return new Rect(point[0], point[1], point[0] + v.getWidth(), point[1] + v.getHeight());
    }

    /**
     * 获取dimen的像素值
     */
    public static int getDimensionPixelSize(Context context, @DimenRes int dimenId) {
        return context.getResources().getDimensionPixelSize(dimenId);
    }

    public static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

        // Get EGL Display
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0]) {
                maximumTextureSize = textureSize[0];
            }
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(double dipValue) {
        return (int) (dipValue * getDisplayDensity() + 0.5f);
    }

    /**
     * 获取手机状态栏的高度
     */
    public static int getStatusBarHeight(Context context) {
        if (hasCutoutOppo(context)) {
            return 80;
        }
        if (hasCutoutHW(context)) {
            int height = getCutoutHW(context);
            if (height <= 0) {
                return getNormalHeight(context);
            }
            return height;
        }
        if (hasCutoutVivo(context)) {
            return dip2px(30);
        }

        return getNormalHeight(context);
    }

    private static int getNormalHeight(Context context) {
        if (context == null) {
            return dip2px(24);
        }
        View decorView = ((Activity) context).getWindow().getDecorView();
        if (decorView == null) {
            return 0;
        }
        int statusHeight;
        Rect localRect = new Rect();
        decorView.getWindowVisibleDisplayFrame(localRect);
        statusHeight = localRect.top;
        if (0 == statusHeight) {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
            if (0 < resourceId) {
                statusHeight = resources.getDimensionPixelOffset(resourceId);
            } else {
                statusHeight = dip2px(24);
            }
        }

//        if (Build.VERSION.SDK_INT > 27) {
//            DisplayCutout displayCutout = decorView.getRootWindowInsets().getDisplayCutout();
//            int cutoutHeight = displayCutout.getSafeInsetTop();
//            if (cutoutHeight > 0) {
//                statusHeight = cutoutHeight;
//            }
//        }
        return statusHeight;
    }

    private static boolean hasCutoutOppo(Context context) {
        return context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
    }

    private static boolean hasCutoutVivo(Context context) {
        boolean ret = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class FtFeature = cl.loadClass("android.util.FtFeature");
            Method isFeatureSupport = FtFeature.getMethod("isFeatureSupport", int.class);
            ret = (boolean) isFeatureSupport.invoke(FtFeature, NOTCH_IN_SCREEN_VIVO);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "hasNotchInScreen ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "hasNotchInScreen NoSuchMethodException");
        } catch (Exception e) {
            Log.e(TAG, "hasNotchInScreen Exception");
        }
        return ret;
    }

    public static boolean hasCutoutHW(Context context) {
        boolean ret = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
            ret = (boolean) get.invoke(HwNotchSizeUtil);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "hasNotchInScreen ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "hasNotchInScreen NoSuchMethodException");
        } catch (Exception e) {
            Log.e(TAG, "hasNotchInScreen Exception");
        }
        return ret;
    }

    public static int getCutoutHW(Context context) {
        int[] ret = new int[]{0, 0};
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("getNotchSize");
            ret = (int[]) get.invoke(HwNotchSizeUtil);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "getNotchSize ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "getNotchSize NoSuchMethodException");
        } catch (Exception e) {
            Log.e(TAG, "getNotchSize Exception");
        }
        return ret[1];
    }

//    @RequiresApi(api = 28)
//    public static boolean hasCutout(@NonNull Activity activity) {
//        View decorView = activity.getWindow().getDecorView();
//        DisplayCutout displayCutout = decorView.getRootWindowInsets().getDisplayCutout();
//        return displayCutout.getSafeInsetTop() > 0;
//    }
}
