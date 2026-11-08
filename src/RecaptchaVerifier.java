import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class RecaptchaVerifier {

    public static final String SITE_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public static boolean verify(String gRecaptchaResponse) throws IOException {
        URL verifyUrl = new URL(SITE_VERIFY_URL);

        // Open Connection to URL
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) verifyUrl.openConnection();

        // Add Request Header
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        httpsURLConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        // The verify ReCaptcha request is sent as a URL and parameters
        String postParams = "secret=" + RecaptchaConstants.SECRET_KEY + "&response=" + gRecaptchaResponse;

        // Send Request
        httpsURLConnection.setDoOutput(true);

        // Write data to the URL connection i.e., send data to the ReCaptcha Server.
        try (OutputStream outStream = httpsURLConnection.getOutputStream()) {
            outStream.write(postParams.getBytes());
            outStream.flush();
        }

        // Get the InputStream from Connection to read data sent from the server.
        try (InputStream inputStream = httpsURLConnection.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            JsonObject jsonObject = new Gson().fromJson(inputStreamReader, JsonObject.class);
            return jsonObject.get("success").getAsBoolean();
        }
    }
}