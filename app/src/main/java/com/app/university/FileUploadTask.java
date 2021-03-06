package com.app.university;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Created by matt on 2015/2/18.
 */
class FileUploadTask extends AsyncTask<String, Integer, String> {

    private Context context;
    private ProgressBar myProgressbar;
    private ImageView mImageView;
    private long totalSize;
    private int mtotalFile;
    private int mCurrentFile;
    public AsyncResponse delegate=null;
    private String mMode = "";


    public interface AsyncResponse {
        void processFinish(String output);
    }

    class ProgressOutHttpEntity extends HttpEntityWrapper {

        private final ProgressListener listener;

        public ProgressOutHttpEntity(final HttpEntity entity, final ProgressListener listener) {
            super(entity);
            this.listener = listener;
        }

        public class CountingOutputStream extends FilterOutputStream {

            private final ProgressListener listener;
            private long transferred;

            CountingOutputStream(final OutputStream out, final ProgressListener listener) {
                super(out);
                this.listener = listener;
                this.transferred = 0;

            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                out.write(b, off, len);
                this.transferred += len;
                this.listener.transferred(this.transferred);
            }

            @Override
            public void write(final int b) throws IOException {
                out.write(b);
                this.transferred++;
                this.listener.transferred(this.transferred);
            }

        }

        @Override
        public void writeTo(final OutputStream out) throws IOException {
            this.wrappedEntity.writeTo(out instanceof CountingOutputStream ? out
                    : new CountingOutputStream(out, this.listener));
        }
    }


    interface ProgressListener {
        public void transferred(long transferedBytes);
    }

    public FileUploadTask(Context context, ProgressBar pb,ImageView image, String mode, int totalfile, int currentFile, AsyncResponse rsp) {
        this.context = context;
        this.myProgressbar = pb;
        this.mMode = mode;
        this.mImageView = image;
        this.delegate = rsp;
        this.mtotalFile = totalfile;
        this.mCurrentFile = currentFile;
    }

    @Override
    protected void onPreExecute() {
        //myProgressbar.setVisibility(View.VISIBLE);
        //myProgressbar.setText(0);

    }

    @Override
    protected String doInBackground(String... params) {

        SharedPreferences shareId = context.getSharedPreferences("ID", Context.MODE_PRIVATE);
        final String myid = shareId.getString(Data.USER_ID, "");
        final String mytoken = shareId.getString(Data.TOKEN, "");
        MultipartEntityBuilder entitys = MultipartEntityBuilder.create();
        entitys.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entitys.setCharset(Charset.forName(HTTP.UTF_8));
        File file = new File(params[0]);
        entitys.addPart("file", new FileBody(file));
        entitys.addTextBody("mode", params[1]);
        entitys.addTextBody(NETTag.USER_ID, myid);
        entitys.addTextBody(NETTag.TOKEN, mytoken);

        HttpEntity httpEntity = entitys.build();
        totalSize = httpEntity.getContentLength();
        ProgressOutHttpEntity progressHttpEntity = new ProgressOutHttpEntity(httpEntity,
                new ProgressListener() {
                    @Override
                    public void transferred(long transferedBytes) {
                        publishProgress((int) (100 * transferedBytes / totalSize / mtotalFile + 100 * (float)mCurrentFile /mtotalFile));
                    }
                });
        return uploadFile(params[0], params[1], progressHttpEntity);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        Log.d("FileUploadTask ", String.valueOf((int) (progress[0])));
        if(myProgressbar != null){
            myProgressbar.setProgress((int) (progress[0]));
        }

    }

    @Override
    protected void onPostExecute(String result) {


        try {
            JSONObject jsonObject = new JSONObject(result);
            if(jsonObject.getString(NETTag.RESULT).compareTo(NETTag.OK) == 0) {
                if(mImageView != null) {
                    mImageView.setImageURI(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/" + Data.FOLDER, Data.IMAGE_FILE_NAME)));
                    mImageView.invalidate();
                    SharedPreferences settings = context.getSharedPreferences ("ID", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(Data.FINAL_FACE_FILE_NAME,Data.FINAL_FACE_FILE_NAME);
                    editor.commit();

                    File orgImage = new File(Environment.getExternalStorageDirectory() + "/" + Data.FOLDER, Data.IMAGE_FILE_NAME);
                    File finalImage = new File(Environment.getExternalStorageDirectory() + "/" + Data.FOLDER, Data.FINAL_FACE_FILE_NAME);
                    finalImage.delete();
                    orgImage.renameTo(finalImage);
                    mImageView.setImageURI(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/" + Data.FOLDER, Data.FINAL_FACE_FILE_NAME)));
                    mImageView.invalidate();
                    Log.d("FileUploadTask 2", result);
                }

            }
            else{
                Log.d("FileUploadTask 1", "network error");
                Toast.makeText(context,  R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("FileUploadTask 1", result);
        if(delegate != null){
            delegate.processFinish(result);
        }

        //Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
    }


    public String uploadFile(String path, String mode, ProgressOutHttpEntity entity) {
        HttpClient httpClient=new DefaultHttpClient();

        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        HttpPost httpPost = new HttpPost(NETTag.API_UPLOAD_IMAGE);
        //httpPost.addHeader("Cookie", "JSESSIONID=" + Constant.sessionId);
        //httpPost.addHeader("Cookie", "signCiphertext=" + Constant.signCiphertext);
        //httpPost.addHeader("Cookie", "type=" + Constant.REQUEST_TYPE);
        //httpPost.addHeader("Cookie", "pcbVersion=" + Constant.PCB_VERSION);
        //httpPost.addHeader("Cookie", "deviceKey=" + Constant.deviceKey);
        try {
            httpPost.setEntity(entity);
            // httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            //HttpResponse httpResponse = httpClient.execute(httpPost);
            ResponseHandler<String> handler=new BasicResponseHandler();
            String response=new String(httpClient.execute(httpPost,handler).getBytes(), HTTP.UTF_8);
            Log.d("FileUploadTask ", response);

            return response;

        } catch (ClientProtocolException e) {

            e.printStackTrace();

        } catch (ConnectTimeoutException e) {

            e.printStackTrace();

        } catch (Exception e) {

            e.printStackTrace();

        } finally {
            if (httpClient != null && httpClient.getConnectionManager() != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
        return "";
    }
}