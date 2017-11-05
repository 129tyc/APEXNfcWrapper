package com.tyc129.nfcwrapper;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;
import com.apex.iot.card.*;
import com.apex.iot.card.nfc.NFCDevice;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Code on 2017/10/12 0012.
 *
 * @author 谈永成
 * @version 1.0
 */
public abstract class NfcActivity extends AppCompatActivity {

    private enum RWState {
        NONE,
        READ,
        WRITE
    }

    private static final String TAG = "Nfc BaseActivity";
    private static final int MSG_READ_FILES = 0;
    private boolean supportNfc;

    private Device device;
    private CardListenerImpl cardListenerImpl;
    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] filters;
    private String[][] techList;
    private Map<String, Long> fileMap;
    private List<String> writtenContents;
    private String writtenCardName;
    private NfcWrapListener nfcWrapListener;
    private NfcCommander nfcCommander;
    private Handler cardHandler;
    private RWState rwState;

    /**
     * 从子Activity获得Nfc监听器
     *
     * @return 子Activity的监听器实例
     */
    protected abstract NfcWrapListener onSetNfcWrapListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nfcWrapListener = onSetNfcWrapListener();
        if (nfcWrapListener == null) {
            throw new NullPointerException("No NfcWrapListener!");
        }
        nfcCommander = new NfcCommanderImpl();
        nfcWrapListener.setNfcCommander(nfcCommander);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (supportNfc && adapter != null) {
            adapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (supportNfc && adapter != null && adapter.isEnabled()) {
            adapter.disableForegroundDispatch(this);

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (supportNfc)
            clearState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (supportNfc) {
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) &&
                    device != null && device.getType().equals(Device.TYPE_NFC)) {
                ((NFCDevice) device).setIntent(intent);
            } else {
                nfcWrapListener.onNfcTagDetected(false, null);
            }
        }
    }

    private void clearState() {
        if (device != null && cardListenerImpl != null) {
            device.removeCardListener(cardListenerImpl);
        }
        rwState = RWState.NONE;
    }

    private void startRead() {
        if (device == null) return;
        device.unlock();
        device.setMode(Device.MODE_READ_ONLY);
        rwState = RWState.READ;
        if (cardListenerImpl != null)
            device.removeCardListener(cardListenerImpl);
        cardListenerImpl = new CardListenerImpl();
        device.addCardListener(cardListenerImpl);
    }

    private void startWrite() {
        if (device == null) return;
        device.unlock();
        device.setMode(Device.MODE_WRITE_ONLY);
        rwState = RWState.WRITE;
        if (cardListenerImpl != null)
            device.removeCardListener(cardListenerImpl);
        cardListenerImpl = new CardListenerImpl();
        device.addCardListener(cardListenerImpl);
    }

    private boolean readCard(Card card, CardFileReader reader, Map<String, Long> fileMap, CardFileReadListener readListener)
            throws LockException, UnauthorizedException, ModeException {
        if (fileMap == null) {
            fileMap = new ArrayMap<>();
        }
        if (card == null) {
            Log.e(TAG, "No Card to Read!");
            return false;
        }
        if (reader == null) {
            Log.e(TAG, "No CardReader to Read!");
            return false;
        }
        if (readListener == null) {
            Log.e(TAG, "No Listener to Read!");
            return false;
        }
        List<CardFile> cardFiles = card.listFile();
        long contentLength = card.getFileLength();
        for (CardFile file : cardFiles) {
            if (file.isCached()) {
                Log.i(TAG, "ReadingFile has been cached--->" + file.getFile().getAbsolutePath());
                fileMap.put(file.getFile().getAbsolutePath(), new File(file.getFile().getAbsolutePath()).length());
            }
        }
        Log.i(TAG, "Reading Done!");
        boolean allCached = checkIsAllFileCached(fileMap, contentLength);
        if (!allCached) {
            reader.setCardFileReadListener(readListener);
            reader.read(cardFiles);
            return false;
        } else {
            return true;
        }
    }

    private boolean writeTextToCard(Card card, CardFileWriter writer, CardFileWriteListener writeListener,
                                    List<String> text, String cardName, int CardType)
            throws ModeException, PackFormatException, IOException {
        if (card == null) {
            Log.e(TAG, "No Card to Write!");
            return false;
        }
        if (writer == null) {
            Log.e(TAG, "No Writer to Write!");
            return false;
        }
        if (writeListener == null) {
            Log.e(TAG, "No Listener to Write!");
            return false;
        }
        if (text == null) {
            Log.e(TAG, "No Content to Write!");
            return false;
        }
        if (cardName == null || cardName.equals("")) {
            cardName = UUID.randomUUID().toString();
            cardName = "map_" + cardName.substring(cardName.length() - 5);
        }
        card.setName(cardName);
        card.setType(CardType);
        List<File> files = new ArrayList<>();
        for (String e :
                writtenContents) {
            File temp = new File(
                    getCacheDir() + File.separator + UUID.randomUUID().toString().replace("-", ""));
            temp.createNewFile();
            if (temp.exists()) {
                temp.setWritable(true);
                FileWriter fileWriter = new FileWriter(temp);
                fileWriter.write(e);
                fileWriter.flush();
                files.add(temp);
                fileWriter.close();
            }
        }
        writer.write(files, writeListener, false);
        files.clear();
        for (File e :
                files) {
            e.delete();
        }
        return true;
    }

    private boolean initNfc() {
        rwState = RWState.NONE;
        boolean result;
        adapter = NfcAdapter.getDefaultAdapter(this);
        result = adapter != null && adapter.isEnabled();
        if (result) {
            loadDriver();
            device = DriverFactory.getDevice(Device.TYPE_NFC);
            result = device != null;
            if (result) {
                fileMap = new ArrayMap<>();
                writtenContents = new ArrayList<>();
                device.initContext(this);
                cardHandler = new CardHandler(nfcWrapListener, fileMap);
                pendingIntent =
                        PendingIntent.getActivity(this, 0,
                                new Intent(this, getClass()).
                                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                filters = new IntentFilter[]{
                        new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                        new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                        new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                };
                techList = new String[][]{
                        new String[]{Ndef.class.getName()},
                        new String[]{NdefFormatable.class.getName()}
                };
            }
        }
        return result;
    }

    private void loadDriver() {
        try {
            Class.forName("com.apex.iot.card.driver.MainDriverLoader");
            Log.i(TAG, "成功加载NFC设备驱动实现！");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "加载NFC设备驱动发生异常，请确保相关的类库已经导入项目程序中！", e);
        }
    }

    private boolean checkIsAllFileCached(Map<String, Long> fileMap, long totalLength) {
        long globeAmount = 0;
        for (Long amount : fileMap.values()) {
            globeAmount += amount;
        }
        if (globeAmount == totalLength) {
            Log.i(TAG, "Card Reading has finished!");
            return true;
        }
        Log.i(TAG, "Card Reading has not finished!");
        return false;
    }


    private static class CardHandler extends Handler {
        private WeakReference<Map<String, Long>> mapWeakReference;
        private WeakReference<NfcWrapListener> wrapListenerWeakReference;

        CardHandler(NfcWrapListener nfcWrapListener, Map<String, Long> fileMap) {
            mapWeakReference = new WeakReference<>(fileMap);
            wrapListenerWeakReference = new WeakReference<>(nfcWrapListener);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_READ_FILES: {
                    Map<String, Long> map = mapWeakReference.get();
                    NfcWrapListener listener = wrapListenerWeakReference.get();
                    if (map != null) {
                        List<String> filePaths = new ArrayList<>();
                        for (Map.Entry<String, Long> e :
                                map.entrySet()) {
                            filePaths.add(e.getKey());
                        }
                        List<String> result = readFiles(filePaths);
                        Log.i(TAG, "Reading Files Success!");
                        if (listener != null)
                            listener.onNfcReadDone(result);
                    }
                }
            }
        }

        private static List<String> readFiles(@NonNull List<String> filePaths) {
            List<String> result = new ArrayList<>();
            String temp;
            StringBuilder builder = new StringBuilder();
            try {
                for (String e :
                        filePaths) {
                    FileReader fileReader = new FileReader(e);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    while ((temp = bufferedReader.readLine()) != null) {
                        builder.append(temp);
                    }
                    result.add(builder.toString());
                    builder.delete(0, builder.length());
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return result;
        }
    }

    private class NfcCommanderImpl implements NfcCommander {

        @Override
        public void acquireNfcInit() {
            supportNfc = initNfc();
            if (nfcWrapListener != null)
                nfcWrapListener.onNfcFuncDetected(supportNfc);
        }

        @Override
        public void acquireReadTag() {
            startRead();
            nfcWrapListener.onNfcReadStart();
        }

        @Override
        public void acquireWriteTag(String cardName, List<String> contents) {
            if (contents == null || contents.isEmpty()) {
                return;
            }
            if (writtenContents == null) {
                writtenContents = new ArrayList<>();
            }
            if (cardName == null) {
                cardName = "";
            }
            writtenContents.clear();
            writtenContents.addAll(contents);
            writtenCardName = cardName;
            startWrite();
            nfcWrapListener.onNfcWrittenStart();
        }
    }

    private class CardListenerImpl implements CardListener {

        @Override
        public void onCardAttached(CardEvent cardEvent) {
            Log.i(TAG, "Card Attached!");
        }

        @Override
        public void onCardChanged(CardEvent cardEvent) {
            Log.i(TAG, "Card Changed!" + rwState);
            final Card card = cardEvent.getNewCard();
            if (card != null) {
                switch (rwState) {
                    case NONE: {
                        Log.i(TAG, "No Func!");
                        break;
                    }
                    case READ: {
                        fileMap.clear();
                        TagInfo info = new TagInfo()
                                .setTagName(card.getName())
                                .setTagID(card.getId())
                                .setCreateDate(card.getCreatedTime());
                        nfcWrapListener.onNfcTagDetected(true, info);
                        try {
                            CardFileReader cardFileReader = device.getCardFileReader(card, "");
                            if (readCard(card, cardFileReader, fileMap,
                                    new CardReadListener(device, cardFileReader, cardHandler, fileMap, card.getFileLength()))) {
                                cardHandler.sendEmptyMessage(MSG_READ_FILES);
                            }
                        } catch (LockException | UnauthorizedException | ModeException e) {
                            device.removeCardListener(cardListenerImpl);
                            Log.e(TAG, e.getMessage(), e);
                        }
                        break;
                    }
                    case WRITE: {
                        try {
                            CardFileWriter writer = device.getCardFileWriter(card, "");
                            writeTextToCard(card, writer, new CardWriteListener(device, cardListenerImpl, writer),
                                    writtenContents, writtenCardName, Card.TYPE_NAME_CARD);
                            writtenContents.clear();
                        } catch (ModeException | IOException | LockException e) {
                            Log.e(TAG, e.getMessage(), e);
                        } catch (PackFormatException | UnauthorizedException e) {
                            Log.e(TAG, e.getMessage(), e);
                            device.removeCardListener(cardListenerImpl);
                        }
                        break;
                    }
                }
                rwState = RWState.NONE;
            } else {
                nfcWrapListener.onNfcTagDetected(false, null);
            }
        }

        @Override
        public void onCardNotApplicable(CardEvent cardEvent) {
            Log.w(TAG, "Card Not Applicable!");
        }

        @Override
        public void onCardTimeout(CardEvent cardEvent) {
            Log.w(TAG, "Card Timeout!");
        }

        @Override
        public void onCardUnattached(CardEvent cardEvent) {
            Log.i(TAG, "Card Unattached!");
        }

        @Override
        public void onCardException(CardEvent cardEvent) {
            Log.e(TAG, cardEvent.getMessage());
        }
    }

    private class CardWriteListener implements CardFileWriteListener {

        private Device device;
        private CardListener cardListener;
        private CardFileWriter writer;

        CardWriteListener(@NonNull Device device, @NonNull CardListener cardListener,
                          @NonNull CardFileWriter writer) {
            this.device = device;
            this.cardListener = cardListener;
            this.writer = writer;
        }

        @Override
        public void onWriteStart(CardFileWriteEvent cardFileWriteEvent) {
            Log.i(TAG, "Card Writing Start!");

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(NfcActivity.this, "Writing Process Start!", Toast.LENGTH_SHORT)
//                            .show();
//                }
//            });
        }

        @Override
        public void onWriteProgress(final CardFileWriteEvent cardFileWriteEvent) {
            Log.i(TAG, "Card Writing Progress!");

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Card writeCard = cardFileWriteEvent.getCard();
//                    mainView.setText("Card Name:" + writeCard.getName() +
//                            "\nCurrProgress:" + (int) (cardFileWriteEvent.getTotalAmount() * 100f /
//                            cardFileWriteEvent.getTargetAmount()) + "%");
//                }
//            });
        }

        @Override
        public void onWriteFinish(CardFileWriteEvent cardFileWriteEvent) {
            Log.i(TAG, "Card Writing Finish!");
            if (device != null) {
                device.unlock();
                if (cardListener != null) {
                    device.removeCardListener(cardListener);
                }
            }
            if (writer != null) {
                writer.close();
            }
            nfcWrapListener.onNfcWrittenDone();
        }

        @Override
        public void onWritePause(CardFileWriteEvent cardFileWriteEvent) {
            Log.i(TAG, "Card Writing Pause!");
        }

        @Override
        public void onWriteResume(CardFileWriteEvent cardFileWriteEvent) {
            Log.i(TAG, "Card Writing Resume!");
        }

        @Override
        public void onWriteCancel(CardFileWriteEvent cardFileWriteEvent) {
            Log.i(TAG, "Card Writing Cancel!");
            if (writer != null) {
                writer.close();
            }
            if (device != null) {
                device.unlock();
                if (cardListener != null) {
                    device.removeCardListener(cardListener);
                }
            }
        }
    }

    private class CardReadListener implements CardFileReadListener {
        private Map<String, Long> fileMap;
        private long contentLength;
        private Device device;
        private CardFileReader reader;
        private Handler cardHandler;

        CardReadListener(@NonNull Device device, @NonNull CardFileReader reader, @NonNull Handler cardHandler,
                         @NonNull Map<String, Long> fileMap, long contentLength) {
            this.fileMap = fileMap;
            this.contentLength = contentLength;
            this.device = device;
            this.reader = reader;
            this.cardHandler = cardHandler;
        }

        @Override
        public void onReadStart(CardFileReadEvent cardFileReadEvent) {
            Log.i(TAG, "On Reading Start");
        }

        @Override
        public void onReadProgress(CardFileReadEvent cardFileReadEvent) {
            Log.i(TAG, "On Reading Progress " + cardFileReadEvent.getData().length);
            fileMap.put(cardFileReadEvent.getCardFile().getFile().getAbsolutePath(),
                    (long) cardFileReadEvent.getTotalAmount());
        }

        @Override
        public void onReadFinish(CardFileReadEvent cardFileReadEvent) {
            if (checkIsAllFileCached(fileMap, contentLength)) {
                cardHandler.sendEmptyMessage(MSG_READ_FILES);
                //删
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(NfcActivity.this, "Reading Process Finish!", Toast.LENGTH_SHORT)
                                .show();
                    }
                });
            }
            if (device != null) {
                device.unlock();
                if (cardListenerImpl != null) {
                    device.removeCardListener(cardListenerImpl);
                }
            }
            if (reader != null) {
                reader.close();
            }

        }

        @Override
        public void onReadPause(CardFileReadEvent cardFileReadEvent) {
            Log.i(TAG, "On Reading Pause");
        }

        @Override
        public void onReadResume(CardFileReadEvent cardFileReadEvent) {
            Log.i(TAG, "On Reading Resume");
        }

        @Override
        public void onReadCancel(CardFileReadEvent cardFileReadEvent) {
            Log.i(TAG, "On Reading Cancel");
            if (device != null) {
                device.unlock();
                if (cardListenerImpl != null) {
                    device.removeCardListener(cardListenerImpl);
                }
            }
            if (reader != null)
                reader.close();
        }
    }
}
