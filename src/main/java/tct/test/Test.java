package tct.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    private static final String CSV_FILE_PATH
            = "./data_1.csv";

    public static List<String> PLACES = List.of("", "ha-noi", "da-nang", "can-tho", "khanh-hoa",
            "hai-phong", "quang-ninh", "hue", "binh-thuan", "lam-dong",
            "vung-tau", "dong-nai", "binh-duong", "hai-duong",
            "nam-dinh", "tien-giang", "phu-quoc", "nghe-an", "quang-nam",
            "long-an", "dien-bien", "binh-dinh", "thanh-hoa", "bac-ninh", "thai-nguyen");

//    public static List<String> PLACES = List.of("", "ha-noi");

//    public static List<String> PLACES = List.of("ha-noi");

    public static List<Integer> placeNb = new ArrayList<>();

    public static int maxPage = 50;

    public static CSVWriter writer;

    public static List<String> resIds = new ArrayList<>();

    public static int counter = 0;

    public static boolean getResId(String place, String page) throws UnirestException {
        try {
            HttpResponse<String> response = Unirest.get("https://www.foody.vn/__get/Place/HomeListPlace?t=1669633096455&page=" + page + "&lat=20.542592&lon=105.91258&count=12&districtId=&cateId=&cuisineId=&isReputation=&type=1")
                    .header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Connection", "keep-alive")
                    .header("Referer", "https://www.foody.vn/" + place)
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .asString();

            JSONObject jsonObjectBody = new JSONObject(response.getBody());

            JSONArray c = jsonObjectBody.getJSONArray("Items");

            if (c == null || c.length() <= 0) {
                return false;
            }

            for (int i = 0; i < c.length(); i++) {
                JSONObject obj = c.getJSONObject(i);
                if (!resIds.contains(String.valueOf(obj.getNumber("Id")))) {
                    resIds.add(String.valueOf(obj.getNumber("Id")));
                }
            }

            return true;
        } catch (Exception ignored) {
        }
        return true;
    }

    public static void crawl(String lastId, String resId, String place) throws UnirestException, InterruptedException {
        System.out.println(resId);
        Thread.sleep(10);
        try {
            HttpResponse<String> response = Unirest.post("https://www.foody.vn/__get/Review/ResLoadMore?t=1669629677461&ResId=" + resId + "&Count=10&LastId=" + lastId + "&Type=1&fromOwner=&isLatest=true")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    // .header("Content-Length", "0")
                    .header("Origin", "https://www.foody.vn")
                    .header("Referer", "https://www.foody.vn/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("sec-ch-ua", "\"Google Chrome\";v=\"107\", \"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Linux\"")
                    .asString();

            JSONObject jsonObjectBody = new JSONObject(response.getBody());


            JSONArray c = jsonObjectBody.getJSONArray("Items");

            // Recursion stop conditions
            if (c == null || c.length() == 0) {
                return;
            }

            for (int i = 0; i < c.length(); i++) {
                JSONObject obj = c.getJSONObject(i);
                Number score = obj.getNumber("AvgRating");
                String rate = "0";
                if (score.doubleValue() >= 6) {
                    rate = "1";
                }
                writer.writeNext(
                        new String[]{
                                String.valueOf(obj.getNumber("Id")),
                                obj.getString("Description").replaceAll("\n", ""),
                                String.valueOf(obj.getNumber("AvgRating")),
                                rate
                        });
            }
            if (jsonObjectBody.getNumber("LastId") != null) {
                crawl(String.valueOf(jsonObjectBody.getNumber("LastId")), resId, place);
                System.out.println("Recursion");
            }
        } catch (Exception ignored) {
        }

    }

    public static void main(String[] args) throws IOException, UnirestException {
        Unirest.setTimeouts(0, 0);
        File file = new File(CSV_FILE_PATH);
        try {
            FileWriter outputFile = new FileWriter(file);

            writer = new CSVWriter(outputFile, CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.DEFAULT_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            writer.writeNext(new String[]{"RevId", "Comment", "Score", "Rating"}, true);

            PLACES.forEach(place -> {
                counter = 0;
                for (int i = 1; i <= maxPage; ++i) {
                    try {
                        if (!getResId(place, String.valueOf(i))) {
                            break;
                        }
                    } catch (UnirestException ignored) {
                    }
                }
            });

            resIds.parallelStream().forEach(resId -> {
                        try {
                            crawl("", String.valueOf(resId), "");
                        } catch (UnirestException | InterruptedException ignored) {
                        }
                    }
            );

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
