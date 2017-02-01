package com.github.gmazzo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.AnimRes;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public final class ResourceUtils {

    @NonNull
    public static Drawable[] getDrawableArray(@NonNull Resources resources, @ArrayRes int arrayRes) {
        TypedArray ta = resources.obtainTypedArray(arrayRes);
        try {
            Drawable drawables[] = new Drawable[ta.length()];
            for (int i = 0; i < drawables.length; i++) {
                drawables[i] = ta.getDrawable(i);
            }
            return drawables;

        } finally {
            ta.recycle();
        }
    }

    @Nullable
    public static Animation getAnimation(@NonNull Context context, @AnimRes int animRes) {
        return animRes != 0 ? AnimationUtils.loadAnimation(context, animRes) : null;
    }

    private ResourceUtils() {
    }

}
