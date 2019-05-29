package test.xzheng.com.webservertest1;


import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyWebServer extends SimpleWebServer {
    public static final String LOCAL_HOST_URL = "http://127.0.0.1";
    private static final String TAG = MyWebServer.class.getSimpleName();
    private static final String MIME_TYPE_JSON = "application/json";


    private Context context;
    private String localHostUrl;

    private JSONObject configTemplate;
    private String adsTemplate;

    private File uploadDir;
    private String endCardName;

    public MyWebServer(Context context, int port, File rootDir, File uploadDir, boolean quiet) {
        super(null, port, rootDir, quiet);
        this.context = context;
        localHostUrl = LOCAL_HOST_URL + ":" + port;
        this.uploadDir = uploadDir;
        setup();
    }

    public void setup() {
        try {
            String configStr = contentOfAsset("config.json");
            configTemplate = new JSONObject(configStr);
            JSONObject endpoints = configTemplate.optJSONObject("endpoints");
            if(endpoints != null) {
                final String okUrl = localUrlWithPath("ok");
                endpoints.put("new", okUrl);
                endpoints.put("report_ad", okUrl);
                endpoints.put("ads", localUrlWithPath("ads"));
                endpoints.put( "will_play_ad", okUrl);
                endpoints.put( "log", okUrl);
                endpoints.put( "ri", okUrl);
            }
            adsTemplate = contentOfAsset("ads.json");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load template json file for SDK API", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse config json", e);
        }

        File[] uploadedFiles = uploadDir.listFiles();
        if(uploadedFiles != null && uploadedFiles.length > 0) {
            for(File file : uploadedFiles) {
                if(file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                    endCardName = file.getName();
                    break;
                }
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "serve: " + session.getUri());
        //intercept sdk api request
        Method method = session.getMethod();
        String uri = session.getUri();
        if(Method.POST.equals(method)) {
            //for post method
            if("/config".equals(uri)) {
                return handleConfig(session);
            } else if("/ads".equals(uri)) {
                return handleAds(session);
            } else if("/ok".equals(uri)) {
                return handleOK(session);
            } else if("/upload".equals(uri)) {
                return handleUpload(session);
            } else {
                //not supported
                Log.e(TAG, "not supported uri:" + uri);
            }
        }

        return super.serve(session);
    }

    private Response handleUpload(IHTTPSession session) {

        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (ResponseException e) {
            Log.e(TAG, "Failed to parse body for session", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse body for session", e);
        }

        Map<String, List<String>> params = session.getParameters();
        List<String> filename = params.get("bundle");
        String tmpFilePath = files.get("bundle");
        if(filename == null || filename.isEmpty() || tmpFilePath == null) {
            return null;
        }

        String name = filename.get(0);
        File dst = new File(uploadDir, name);
        if (dst.exists()) {
            // Response for confirm to overwrite
        }
        File src = new File(tmpFilePath);
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException ioe) {
            // Response for failed
        }
        // Response for success
        //save the end card name
        endCardName = name;
        String res = "{\"msg\": \"Upload successfully\", \"code\":0, \"data\": \"\"}";

        return newFixedLengthResponse(Status.OK, MIME_TYPE_JSON, res);
    }

    private Response handleConfig(IHTTPSession session) {
        if(configTemplate != null) {
            return newFixedLengthResponse(Status.OK, MIME_TYPE_JSON, configTemplate.toString());
        }
        return null;
    }

    private Response handleAds(IHTTPSession session) {
        String res = adsTemplate;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, 14);
        long expiry = cal.getTimeInMillis();

        Map<String, String> vars = new HashMap<>();
        vars.put("${postBundle}", endCardURL());
        vars.put("${videoURL}", localUrlWithPath("countdown_video.mp4"));
        vars.put("${expiry}", String.valueOf(expiry));
        for(String target : vars.keySet()) {
            String replacement = vars.get(target);
            if(replacement != null) {
                res = res.replace(target, replacement);
            }
        }
        return newFixedLengthResponse(Status.OK, MIME_TYPE_JSON, res);
    }

    private Response handleOK(IHTTPSession session) {
        String res = "{\"msg\": \"ok\", \"code\":200 }";
        return newFixedLengthResponse(Status.OK, MIME_TYPE_JSON, res);
    }

    private String contentOfAsset(String assetName) throws IOException  {
        InputStream in = context.getAssets().open(assetName);
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    private String localUrlWithPath(String path) {
        return localHostUrl + "/" + path;
    }

    private String endCardURL() {
        if(endCardName != null) {
            String uploadURL = localUrlWithPath(uploadDir.getName());
            return uploadURL + "/" + endCardName;
        } else {
            return localUrlWithPath("endcard.zip");
        }
    }


}
