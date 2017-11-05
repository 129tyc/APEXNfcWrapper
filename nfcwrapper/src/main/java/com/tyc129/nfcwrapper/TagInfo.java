package com.tyc129.nfcwrapper;

import android.support.annotation.NonNull;

/**
 * 标签信息封装类
 * Created by Code on 2017/10/12 0012.
 *
 * @author 谈永成
 * @version 1.0
 */
public class TagInfo {
    /**
     * 标签ID
     */
    private String tagID;
    /**
     * 标签名称
     */
    private String tagName;
    /**
     * 标签创建时间
     */
    private long createDate;

    public TagInfo() {
        tagID = "";
        tagName = "";
        createDate = 0;
    }

    public String getTagID() {
        return tagID;
    }

    public TagInfo setTagID(@NonNull String tagID) {
        this.tagID = tagID;
        return this;
    }

    public String getTagName() {
        return tagName;
    }

    public TagInfo setTagName(@NonNull String tagName) {
        this.tagName = tagName;
        return this;
    }

    public long getCreateDate() {
        return createDate;
    }

    public TagInfo setCreateDate(long createDate) {
        this.createDate = createDate;
        return this;
    }
}
