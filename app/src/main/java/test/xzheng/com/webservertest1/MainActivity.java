package test.xzheng.com.webservertest1;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.vungle.warren.AdConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.network.VungleApiClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File assetsDir = this.getDir("assets", Context.MODE_PRIVATE);
        File uploadDir = new File(assetsDir, "upload");
        if(!uploadDir.exists()) {
            if(!uploadDir.mkdir()) {
                Log.e(TAG, "Failed to create upload dir");
            }
        }
        setupAssets();

        MyWebServer webServer = new MyWebServer(this, 8091, assetsDir, uploadDir, true);
        try {
            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String localHostURL = MyWebServer.LOCAL_HOST_URL + ":" + webServer.getListeningPort() + "/";

        //mock end point
        try {
            Field field = VungleApiClient.class.getDeclaredField("BASE_URL");
            field.setAccessible(true);
            field.set(null, localHostURL);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        final String appId = "CreativeTool";
        final String placementId = "LOCAL01";

        Button startSDKBtn = findViewById(R.id.btn_start);
        final Context appCtx = getApplicationContext();
        startSDKBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vungle.init(appId, appCtx, new InitCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Init Success");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Init Error", throwable);
                    }

                    @Override
                    public void onAutoCacheAdAvailable(String s) {
                        Log.d(TAG, "onAutoCacheAdAvailable:" + s);
                    }
                });
            }
        });

        Button loadAdBtn = findViewById(R.id.btn_load_ad);
        loadAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vungle.loadAd(placementId, new LoadAdCallback() {
                    @Override
                    public void onAdLoad(String s) {
                        Log.d(TAG, "onAdLoad:" + s);
                    }

                    @Override
                    public void onError(String s, Throwable throwable) {
                        Log.e(TAG, "onError:" + s, throwable);
                    }
                });
            }
        });

        Button playAdBtn = findViewById(R.id.btn_play_ad);
        playAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Vungle.canPlayAd(placementId)) {
                    AdConfig config = new AdConfig();
                    config.setAutoRotate(true);
                    Vungle.playAd(placementId, config, new PlayAdCallback() {
                        @Override
                        public void onAdStart(String s) {
                            Log.d(TAG, "onAdStart:" + s);
                        }

                        @Override
                        public void onAdEnd(String s, boolean b, boolean b1) {
                            Log.d(TAG, "onAdEnd:" + s);
                        }

                        @Override
                        public void onError(String s, Throwable throwable) {
                            Log.e(TAG, "onError" + s, throwable);
                        }
                    });
                }
            }
        });

    }

    private void setupAssets() {
        File assetsDir = this.getDir("assets", Context.MODE_PRIVATE);
        String[] assets = {"index.html", "main.js", "style.css",
                "countdown_video.mp4", "endcard.zip"};
        for(String asset : assets) {
            try {
                copyAssetToFile(asset, assetsDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyAssetToFile(String assetName, File targetDir) throws IOException {
        InputStream in = this.getAssets().open(assetName);
        File targetFile = new File(targetDir, assetName);
        if(!targetFile.exists()) {
            OutputStream out = new FileOutputStream(targetFile);
            copyFile(in, out);
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
    }
}
