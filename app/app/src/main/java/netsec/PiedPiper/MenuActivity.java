package netsec.PiedPiper;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import java.net.HttpURLConnection;
import java.util.TimeZone;

import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;



public class MenuActivity extends AppCompatActivity {

    enum ServerAction {
        USER_REGISTER,
        REQUEST_TOKEN,
        CREATE_OBJECT,
        UPLOAD_OBJECT,
        GET_OBJECT,
        CONVERT_FILE
    }

    private final String TAG = this.getClass().getSimpleName();

    private Button mRegisterButton;
    private Button mTokenButton;
    private Button mEncryptButton;
    private Button mDecryptButton;
    private Button mCreateObject;
    private Button mUploadObject;
    private Button mGetObject;
    private Button mConvertFile;

    private byte[] plainText;
    private byte[] cipherText;

    private byte[] aesKey;

    String responseServer;
    String token;
    String objectID;
    TextView txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        txt = (TextView) findViewById(R.id.text);

        aesKey = SimpleCrypto.generateKey("Thisismypassword");
        if (aesKey == null) {
            Log.e("onCreate", "Unable to generate key");
        }
        plainText = "This is my plaintext".getBytes();
        cipherText = "".getBytes();

        mRegisterButton = (Button)findViewById(R.id.userRegister);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessButton processButton = new ProcessButton();
                processButton.execute(ServerAction.USER_REGISTER);
            }
        });

        mTokenButton = (Button)findViewById(R.id.getToken);
        mTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessButton processButton = new ProcessButton();
                processButton.execute(ServerAction.REQUEST_TOKEN);
            }
        });

        mEncryptButton = (Button)findViewById(R.id.encrypt);
        mEncryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    final byte[] finalPlain = plainText.clone();
                    cipherText = SimpleCrypto.encrypt(aesKey, finalPlain);
                    Log.i("Encrypt - Plain", SimpleCrypto.bytesToHex(plainText));
                    Log.i("Encrypt - Cipher", SimpleCrypto.bytesToHex(cipherText));
                    txt.setText("Plain: " + SimpleCrypto.bytesToHex(plainText) + "\nCipher: " + SimpleCrypto.bytesToHex(cipherText));

                } catch (Exception e) {
                    Log.e("Encrypt", e.toString());
                }
            }
        });
        mDecryptButton = (Button)findViewById(R.id.decrypt);
        mDecryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    final byte[] finalCipher = cipherText.clone();
                    plainText = SimpleCrypto.decrypt(aesKey, finalCipher);
                    Log.i("Decrypt - Cipher", SimpleCrypto.bytesToHex(cipherText));
                    Log.i("Decrypt - Plain", SimpleCrypto.bytesToHex(plainText));
                    txt.setText("Cipher: " + SimpleCrypto.bytesToHex(cipherText) + "\nPlain: " + SimpleCrypto.bytesToHex(plainText));

                } catch (Exception e) {
                    Log.e("Decrypt", e.toString());
                }
            }
        });
        mCreateObject = (Button)findViewById(R.id.createObject);
        mCreateObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessButton processButton = new ProcessButton();
                processButton.execute(ServerAction.CREATE_OBJECT);
            }
        });
        mUploadObject = (Button)findViewById(R.id.uploadObject);
        mUploadObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessButton processButton = new ProcessButton();
                processButton.execute(ServerAction.UPLOAD_OBJECT);
            }
        });
        mGetObject = (Button)findViewById(R.id.getObject);
        mGetObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessButton processButton = new ProcessButton();
                processButton.execute(ServerAction.GET_OBJECT);
            }
        });

        mConvertFile = (Button)findViewById(R.id.convertFile);
        mConvertFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcessButton processButton = new ProcessButton();
                processButton.execute(ServerAction.CONVERT_FILE);
            }
        });

    }
    /* Inner class to get response */
    class ProcessButton extends AsyncTask<ServerAction, Void, Void> {

        private String userRegister(String user, String pass) {
            HttpURLConnection urlConnection=null;
            String json = null;
            String reply = null;
            try {
                //Create User

                HttpResponse response;
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("username", user);
                jsonObject.accumulate("password", pass);
                json = jsonObject.toString();
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("https://pp.848.productions/user");
                httpPost.setEntity(new StringEntity(json, "UTF-8"));
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept-Encoding", "application/json");
                httpPost.setHeader("Accept-Language", "en-US");
                response = httpClient.execute(httpPost);
                InputStream inputStream = response.getEntity().getContent();
                StringifyStream str = new StringifyStream();
                reply = str.getStringFromInputStream(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return reply;
        }

        private String requestToken(String username, String password) {
            HttpURLConnection urlConnection=null;
            String json = null;
            String reply = null;
            try {
                SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date now = new Date();

                HttpResponse response;
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("foo", dateFormatGmt.format(now));
                jsonObject.accumulate("username", username);
                jsonObject.accumulate("password", password);
                json = jsonObject.toString();
                Log.i("getting:", json);
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("https://pp.848.productions/auth");
                httpPost.setEntity(new StringEntity(json, "UTF-8"));
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept-Encoding", "application/json");
                httpPost.setHeader("Accept-Language", "en-US");
                response = httpClient.execute(httpPost);
                Log.i("response", response.getStatusLine().getReasonPhrase());

                InputStream inputStream = response.getEntity().getContent();
                StringifyStream str = new StringifyStream();
                responseServer = str.getStringFromInputStream(inputStream);
                Log.d("GetToken Server Reply", responseServer);
                JSONObject replyJson = new JSONObject(responseServer);
                token = getHashCodeFromString(username + replyJson.getString("Nonce") + jsonObject.getString("foo"));

                Log.e("response", responseServer);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Device Token: " + token;
        }

        private String createObject(String token, String filename) {
            HttpURLConnection urlConnection=null;
            String json = null;
            String reply = null;
            try {
                SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date now = new Date();

                HttpResponse response;
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("username", token);
                jsonObject.accumulate("filename", filename);
                json = jsonObject.toString();
                Log.i("getting:", json);
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("https://pp.848.productions/object");
                httpPost.setEntity(new StringEntity(json, "UTF-8"));
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept-Encoding", "application/json");
                httpPost.setHeader("Accept-Language", "en-US");
                response = httpClient.execute(httpPost);
                Log.i("response", response.getStatusLine().getReasonPhrase());

                InputStream inputStream = response.getEntity().getContent();
                StringifyStream str = new StringifyStream();
                responseServer = str.getStringFromInputStream(inputStream);
                objectID = responseServer;
                Log.d("GetToken Server Reply", responseServer);
                JSONObject replyJson = new JSONObject(responseServer);

                Log.e("response", responseServer);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Device Token: " + objectID;
        }

        private int uploadObject(String objectID) {
            HttpURLConnection urlConnection=null;
            String json = null;
            String reply = null;
            int responseCode = 8888;

            try {
                HttpResponse response;
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("https://pp.848.productions/object/" + objectID);
                httpPost.setEntity(new ByteArrayEntity(plainText));
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept-Encoding", "application/json");
                httpPost.setHeader("Accept-Language", "en-US");
                response = httpClient.execute(httpPost);

                responseCode = response.getStatusLine().getStatusCode();

                Log.i("response", response.getStatusLine().getReasonPhrase());

                InputStream inputStream = response.getEntity().getContent();
                StringifyStream str = new StringifyStream();
                responseServer = str.getStringFromInputStream(inputStream);
                Log.d("GetToken Server Reply", responseServer);

                Log.e("response", responseServer);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return responseCode;
        }

        private String getObject(String username, String filename) {
            HttpURLConnection urlConnection=null;
            String json = null;
            String reply = null;
            try {
                SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date now = new Date();

                HttpResponse response;
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("username", username);
                jsonObject.accumulate("filename", filename);
                json = jsonObject.toString();
                Log.i("getting:", json);

                HttpClient httpClient = new DefaultHttpClient();

                HttpGetWithEntity  httpGet = new HttpGetWithEntity ("https://pp.848.productions/object");
                httpGet.setEntity(new StringEntity(json, "UTF-8"));

                response = httpClient.execute(httpGet);
                Log.i("response", response.getStatusLine().getReasonPhrase());

                InputStream inputStream = response.getEntity().getContent();
                StringifyStream str = new StringifyStream();
                responseServer = str.getStringFromInputStream(inputStream);
                Log.d("GetToken Server Reply", responseServer);
                //JSONObject replyJson = new JSONObject(responseServer);
                //token = getHashCodeFromString(username + replyJson.getString("Nonce") + jsonObject.getString("foo"));

                Log.e("response", responseServer);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return responseServer;
        }

        private String convertFile() {
//            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
//            String fileName = "file.txt";
//            String path  = baseDir + "/your folder(s)/" + fileName;
//
//            String found = "true";
//            //Find the directory for the SD Card using the API
//            //*Don't* hardcode "/sdcard"
////            File file = new File(getFilesDir() + "/" + "file.txt");
//            //Get the text file
            File file = new File("/storage/self/primary/file.txt");
//
//            int size = (int) file.length();
//            byte[] bytes = new byte[size];
//
//            try {
//                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
//                buf.read(bytes, 0, bytes.length);
//                buf.close();
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//            if(new File("/storage/self/primary/file.txt").exists())
//            {
//                found = "yay";
//            }
//                String output = new String(bytes);
//            String str = new String(bytes);

            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                    Log.i("Test", "text : "+text+" : end");
                }
                br.close();
            }
            catch (IOException e) {
                //You'll need to add proper error handling here
            }



            String str;

            if(file.exists()) {
                str = "Poop";
            } else {
                str = "nope";
            }
            Log.d("Text", ""+text);
            return "text : "+text+" : end";
        }



        @Override
        protected Void doInBackground(ServerAction... params) {

            Log.e("Entering doInBackground", params[0].name());

            HttpURLConnection urlConnection=null;
            String json = null;
            ServerAction action = params[0];
            String username = "user4321";
            String password = "pass4321";
            String filename = "testfile2";
            int res = 9999;

            switch (action) {
                case USER_REGISTER:
                    responseServer = userRegister(username, password);
                    break;
                case REQUEST_TOKEN:
                    responseServer = requestToken(username, password);
                    break;
                case CREATE_OBJECT:
                    responseServer = createObject(username, filename);
                    break;
                case UPLOAD_OBJECT:
                    res = uploadObject(objectID);
                    responseServer = "CODE " + res;
                    break;
                case GET_OBJECT:
                    responseServer = getObject(username, filename);
                    break;
                case CONVERT_FILE:
                    responseServer = convertFile();
                    break;
                default:
                    responseServer = "Action not registered";
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            txt.setText(responseServer);
        }
    }

    public static class StringifyStream {

        public static void main(String[] args) throws IOException {
            InputStream is = new ByteArrayInputStream("".getBytes());

            String result = getStringFromInputStream(is);

            System.out.println(result);
            System.out.println("Done");

        }

        // convert InputStream to String
        private static String getStringFromInputStream(InputStream is) {

            BufferedReader b_reader = null;
            StringBuilder s_builder = new StringBuilder();

            String line;
            try {
                b_reader = new BufferedReader(new InputStreamReader(is));
                while ((line = b_reader.readLine()) != null) {
                    s_builder.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (b_reader != null) {
                    try {
                        b_reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return s_builder.toString();
        }

    }

    private static String getHashCodeFromString(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(str.getBytes());
        byte byteData[] = md.digest();

        //convert the byte to hex format method 1
        StringBuffer hashCodeBuffer = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            hashCodeBuffer.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return hashCodeBuffer.toString();
    }


    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "ON RESUME");
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        Log.d(TAG, "ON RESTART");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "---ON DESTROY---");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "ON PAUSE");
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.d(TAG, "ON START");
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d(TAG, "ON STOP");
    }
}
