package com.tyc129.nfcwrapper;

import java.util.List;

/**
 * 艾派克Nfc事件监听器
 * 实例化并传递到NfcActivity可获得相对应的Nfc事件
 * Created by Code on 2017/10/12 0012.
 *
 * @author 谈永成
 * @version 1.0
 */
public interface NfcWrapListener {
    /**
     * 从NfcActivity获得Nfc控制器
     *
     * @param commander Nfc控制器
     */
    void setNfcCommander(NfcCommander commander);

    /**
     * 判断Nfc功能是否正常事件
     *
     * @param supportNfc 是否支持Nfc
     */
    void onNfcFuncDetected(boolean supportNfc);

    /**
     * 检测到Nfc标签事件
     *
     * @param tagValidity 标签是否有效
     * @param info        标签基本信息
     */
    void onNfcTagDetected(boolean tagValidity, TagInfo info);

    /**
     * 读卡开始事件
     */
    void onNfcReadStart();

    /**
     * 读卡结束事件
     *
     * @param contents 卡内容列表，每个元素代表一个文件
     */
    void onNfcReadDone(List<String> contents);

    /**
     * 写卡开始事件
     */
    void onNfcWrittenStart();

    /**
     * 写卡完成事件
     */
    void onNfcWrittenDone();
}
