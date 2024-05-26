package com.efrat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeocodingExample {
    private static final String API_KEY = "3d1cd444ff9b4cb7982e1fbd72150120"; 

    public static void main(String[] args) {
        String inputFilePath = "input.json";
        String outputFilePath = "output.json";

        try {
            // קריאת קובץ JSON
            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();

            System.out.println("קובץ ה-input נקרא בהצלחה");

            // עיבוד JSON
            JSONArray addresses = new JSONArray(jsonString.toString());
            JSONArray outputAddresses = new JSONArray();

            for (int i = 0; i < addresses.length(); i++) {
                JSONObject address = addresses.getJSONObject(i);
                String street = address.getString("street");
                String houseNumber = address.getString("houseNumber");
                String city = address.getString("city");

                // תרגום הכתובת לאנגלית בצורה ידנית
                String translatedStreet = translateStreetToEnglish(street);
                String translatedCity = translateCityToEnglish(city);

                String postcode = getPostcode(translatedStreet, houseNumber, translatedCity);
                address.put("postcode", postcode);
                outputAddresses.put(address);

                System.out.println("עיבוד כתובת: " + street + " " + houseNumber + ", " + city + " - מיקוד: " + postcode);
            }

            // כתיבת קובץ JSON חדש עם מיקודים
            FileWriter file = new FileWriter(outputFilePath);
            file.write(outputAddresses.toString(4)); // 4 רווחים להזחה
            file.close();

            System.out.println("המיקודים נוספו בהצלחה לקובץ JSON");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String translateStreetToEnglish(String street) {
        // תרגום ידני של שמות רחובות
        switch (street) {
            case "בן יהודה":
                return "Ben Yehuda";
            case "אנה פרנק":
                return "Anne Frank";
            case "חזון איש":
                return "Hazon Ish";
            // הוסף תרגומים נוספים לפי הצורך
            default:
                return street;
        }
    }

    public static String translateCityToEnglish(String city) {
        // תרגום ידני של שמות ערים
        switch (city) {
            case "פתח תקווה":
                return "Petah Tikva";
            case "בני ברק":
                return "Bnei Brak";
            // הוסף תרגומים נוספים לפי הצורך
            default:
                return city;
        }
    }

    public static String getPostcode(String street, String houseNumber, String city) throws IOException {
        String address = URLEncoder.encode(street + " " + houseNumber + ", " + city + ", Israel", "UTF-8");
        String urlString = "https://api.opencagedata.com/geocode/v1/json?q=" + address + "&key=" + API_KEY + "&language=en&countrycode=il";

        System.out.println("URL: " + urlString); // הוסף הודעת דיבוג

        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true).build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build()) {
                HttpGet request = new HttpGet(urlString);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    System.out.println("HTTP Status Code: " + statusCode); // הוסף הודעת דיבוג

                    if (statusCode != 200) {
                        System.err.println("Failed : HTTP error code : " + statusCode);
                        return "Error";
                    }

                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    System.out.println("Response JSON: " + jsonResponse); // הוסף הודעת דיבוג

                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    if (jsonObject.has("results") && jsonObject.getJSONArray("results").length() > 0) {
                        JSONArray results = jsonObject.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject result = results.getJSONObject(i);
                            JSONObject components = result.getJSONObject("components");
                            System.out.println("תוצאה: " + result.toString()); // הוסף הודעת דיבוג

                            if (components.has("postcode")) {
                                return components.getString("postcode");
                            }
                        }
                        return "Not Found";
                    } else {
                        return "Not Found";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
