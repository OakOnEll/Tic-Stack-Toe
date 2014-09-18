package com.shamanland.facebook.likebutton;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class FacebookLikeButton extends Button {
    public interface OnPageUrlChangeListener {
        void onPageUrlChanged(String newValue);
    }

    private String mPageUrl;
    private String mPageTitle;
    private String mPageText;
    @Deprecated
    private Bitmap mPagePicture;
    private String mPagePictureUrl;
    private int mPagePictureId;
    private String mAppId;
    private int mContentViewId;
    private FacebookLikeOptions mOptions;

    private OnPageUrlChangeListener mOnPageUrlChangeListener;

    public String getPageUrl() {
        return mPageUrl;
    }

    public void setPageUrl(String pageUrl) {
        mPageUrl = pageUrl;

        if (mOnPageUrlChangeListener != null) {
            mOnPageUrlChangeListener.onPageUrlChanged(mPageUrl);
        }
    }

    public String getPageTitle() {
        return mPageTitle;
    }

    public void setPageTitle(String pageTitle) {
        mPageTitle = pageTitle;
    }

    public String getPageText() {
        return mPageText;
    }

    public void setPageText(String pageText) {
        mPageText = pageText;
    }

    @Deprecated
    public Bitmap getPagePicture() {
        //noinspection deprecation
        return mPagePicture;
    }

    @Deprecated
    public void setPagePicture(Bitmap pagePicture) {
        //noinspection deprecation
        mPagePicture = pagePicture;
    }

    public String getPagePictureUrl() {
        return mPagePictureUrl;
    }

    public void setPagePictureUrl(String pagePictureUrl) {
        mPagePictureUrl = pagePictureUrl;
    }

    public int getPagePictureId() {
        return mPagePictureId;
    }

    public void setPagePictureId(int pagePictureId) {
        mPagePictureId = pagePictureId;
    }

    public String getAppId() {
        return mAppId;
    }

    public void setAppId(String appId) {
        mAppId = appId;
    }

    public int getContentViewId() {
        return mContentViewId;
    }

    public void setContentViewId(int contentViewId) {
        mContentViewId = contentViewId;
    }

    public FacebookLikeOptions getOptions() {
        return mOptions;
    }

    public void setOptions(FacebookLikeOptions options) {
        mOptions = options;
    }

    public void setOnPageUrlChangeListener(OnPageUrlChangeListener listener) {
        mOnPageUrlChangeListener = listener;

        if (mOnPageUrlChangeListener != null) {
            mOnPageUrlChangeListener.onPageUrlChanged(mPageUrl);
        }
    }

    public FacebookLikeButton(Context context) {
        super(context);
        init(null);
    }

    public FacebookLikeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public FacebookLikeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        Context c = getContext();
        if (c == null) {
            return;
        }

        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FacebookLikeButton, 0, 0);
        if (a == null) {
            return;
        }

        try {
            mPageUrl = a.getString(R.styleable.FacebookLikeButton_pageUrl);
            mPageTitle = a.getString(R.styleable.FacebookLikeButton_pageTitle);
            mPageText = a.getString(R.styleable.FacebookLikeButton_pageText);

            mPagePictureUrl = a.getString(R.styleable.FacebookLikeButton_pagePictureUrl);
            mPagePictureId = a.getResourceId(R.styleable.FacebookLikeButton_pagePictureId, 0);
            if (mPagePictureId == 0) {
                mPagePictureId = a.getResourceId(R.styleable.FacebookLikeButton_pagePicture, 0);
            }

            mAppId = a.getString(R.styleable.FacebookLikeButton_appId);
            mContentViewId = a.getResourceId(R.styleable.FacebookLikeButton_contentViewId, 0);

            mOptions = new FacebookLikeOptions();
            mOptions.titleOpen = getString(a, R.styleable.FacebookLikeButton_optTitleOpen, mOptions.titleOpen);
            mOptions.titleClose = getString(a, R.styleable.FacebookLikeButton_optTitleClose, mOptions.titleClose);
            mOptions.textOpen = getString(a, R.styleable.FacebookLikeButton_optTextOpen, mOptions.textOpen);
            mOptions.textClose = getString(a, R.styleable.FacebookLikeButton_optTextClose, mOptions.textClose);
            mOptions.pictureAttrs = getString(a, R.styleable.FacebookLikeButton_optPictureAttrs, mOptions.pictureAttrs);
            mOptions.layout = FacebookLikeOptions.Layout.values()[a.getInt(R.styleable.FacebookLikeButton_optLayout, 0)];
            mOptions.action = FacebookLikeOptions.Action.values()[a.getInt(R.styleable.FacebookLikeButton_optAction, 0)];
            mOptions.showFaces = a.getBoolean(R.styleable.FacebookLikeButton_optShowFaces, mOptions.showFaces);
            mOptions.share = a.getBoolean(R.styleable.FacebookLikeButton_optShare, mOptions.share);
        } finally {
            a.recycle();
        }
    }

    private String getString(TypedArray a, int index, String defValue) {
        String read = a.getString(index);
        return read != null ? read : defValue;
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            initAttrs(attrs);
        }

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                performLike();
            }
        });
    }

    public void performLike() {
        Context c = getContext();
        if (c == null) {
            return;
        }

        Intent intent = new Intent(c, FacebookLikeActivity.class);
        intent.putExtra(FacebookLikeActivity.PAGE_URL, mPageUrl);
        intent.putExtra(FacebookLikeActivity.PAGE_TITLE, mPageTitle);
        intent.putExtra(FacebookLikeActivity.PAGE_TEXT, mPageText);
        intent.putExtra(FacebookLikeActivity.PAGE_PICTURE_URL, mPagePictureUrl);
        intent.putExtra(FacebookLikeActivity.PAGE_PICTURE_ID, mPagePictureId);

        //noinspection deprecation
        if (mPagePictureUrl == null && mPagePictureId == 0 && mPagePicture != null) {
            //noinspection deprecation
            intent.putExtra(FacebookLikeActivity.PAGE_PICTURE, mPagePicture);
        }

        intent.putExtra(FacebookLikeActivity.APP_ID, mAppId);
        intent.putExtra(FacebookLikeActivity.CONTENT_VIEW_ID, mContentViewId);
        intent.putExtra(FacebookLikeActivity.OPTIONS, mOptions);
        c.startActivity(intent);
    }
}
