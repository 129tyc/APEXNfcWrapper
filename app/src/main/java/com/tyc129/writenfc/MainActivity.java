package com.tyc129.writenfc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.tyc129.nfcwrapper.NfcActivity;
import com.tyc129.nfcwrapper.NfcCommander;
import com.tyc129.nfcwrapper.NfcWrapListener;
import com.tyc129.nfcwrapper.TagInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends NfcActivity {

    private NfcCommander nfcCommander;
    private TextView mainView;
    private EditText writeContent;
    private Button read;
    private Button write;
    private List<String> contents;
    private boolean support;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        contents = new ArrayList<>();
        nfcCommander.acquireNfcInit();
    }

    private void initView() {
        mainView = (TextView) this.findViewById(R.id.mainView);
        writeContent = (EditText) this.findViewById(R.id.writeContent);
        read = (Button) this.findViewById(R.id.read);
        write = (Button) this.findViewById(R.id.write);
    }

    private void initListener() {
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (support) {
                    nfcCommander.acquireReadTag();
                }
            }
        });
        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (support) {
                    if (contents == null)
                        contents = new ArrayList<>();
                    contents.clear();
                    contents.add(writeContent.getText().toString());
                    nfcCommander.acquireWriteTag("map_1234", contents);
                }
            }
        });
    }

    @Override
    protected NfcWrapListener onSetNfcWrapListener() {
        return new NfcWrapListenerImpl();
    }

    private class NfcWrapListenerImpl implements NfcWrapListener {

        @Override
        public void setNfcCommander(NfcCommander commander) {
            nfcCommander = commander;
        }

        @Override
        public void onNfcFuncDetected(boolean supportNfc) {
            support = supportNfc;
            if (!supportNfc) {
                Toast.makeText(MainActivity.this, "NFC Not Support!", Toast.LENGTH_LONG)
                        .show();
                read.setEnabled(false);
                write.setEnabled(false);
            } else {
                Toast.makeText(MainActivity.this, "NFC Support!", Toast.LENGTH_SHORT)
                        .show();
                read.setEnabled(true);
                write.setEnabled(true);
                initListener();
            }
        }

        @Override
        public void onNfcTagDetected(boolean tagValidity, final TagInfo info) {
            if (tagValidity) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, info.getTagName(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Tag is Invalid!", Toast.LENGTH_SHORT)
                                .show();
                    }
                });
            }
        }

        @Override
        public void onNfcReadStart() {
            mainView.setText("Please Attach NFC Tag on Phone to READ");
        }

        @Override
        public void onNfcReadDone(List<String> contents) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String e :
                    contents) {
                stringBuilder
                        .append(e)
                        .append("\n");
            }
            mainView.setText(stringBuilder);
        }

        @Override
        public void onNfcWrittenStart() {
            mainView.setText("Please Attach NFC Tag on Phone to WRITE");
        }

        @Override
        public void onNfcWrittenDone() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainView.setText("Writing Process Finish!");
                    writeContent.setText("");
                }
            });
        }
    }
}
