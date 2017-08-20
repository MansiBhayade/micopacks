package dev.ukanth.iconmgr;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by ukanth on 15/8/17.
 */

public class Detail {

    private final int mIcon;
    private String mTitle;
    private final String mSubtitle;
    private final Detail.Type mType;


    private List<Bitmap> listIcons;

    public Detail(@DrawableRes int icon, String title, String subtitle, @NonNull Detail.Type type) {
        mIcon = icon;
        mTitle = title;
        mSubtitle = subtitle;
        mType = type;
    }

    @DrawableRes
    public int getIcon() {
        return mIcon;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSubtitle() {
        return mSubtitle;
    }

    public Detail.Type getType() {
        return mType;
    }

    public void setTitle(String title) {
        mTitle = title;
    }


    public List<Bitmap> getListIcons() {
        return listIcons;
    }

    public void setListIcons(List<Bitmap> listIcons) {
        this.listIcons = listIcons;
    }


    public enum Type {
        PERCENT,
        APPLY,
        TOTAL,
        MASK
    }

    public static class Style {

        private final Point mPoint;
        private final Detail.Style.Type mType;

        public Style(@NonNull Point point, @NonNull Detail.Style.Type type) {
            mPoint = point;
            mType = type;
        }

        public Point getPoint() {
            return mPoint;
        }

        public Type getType() {
            return mType;
        }

        public enum Type {
            CARD_SQUARE,
            CARD_LANDSCAPE,
            SQUARE,
            LANDSCAPE
        }
    }
}
