package com.tyc129.nfcwrapper;

import java.util.List;

/**
 * Nfc控制接口
 * 通过调用函数实现艾派克NFC读写功能
 * Created by Code on 2017/10/12 0012.
 *
 * @author 谈永成
 * @version 1.0
 */
public interface NfcCommander {
    /**
     * 请求初始化Nfc
     */
    void acquireNfcInit();

    /**
     * 请求读卡
     */
    void acquireReadTag();

    /**
     * 请求写卡
     * @param cardName 卡名称
     * @param contents 卡内容
     */
    void acquireWriteTag(String cardName, List<String> contents);
}
