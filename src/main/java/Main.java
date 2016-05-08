import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.Inflater;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Main
{
  //  org.json.JsonObject d;
    public static ArrayList<JSONObject> readData(File file) throws IOException, ParseException
    {
        FileInputStream fileInputStream = new FileInputStream(file);
        JSONParser jsonParser = new JSONParser();
        int c = 0;
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        String data = "";

        while ((c = fileInputStream.read()) != -1)
        {
            char character = (char)c;

            if(character == Constants.MEW_LINE)
            {
                int index = data.indexOf(Constants.LEFT_CURLY_BRACKET);
                if(index!=-1)
                {
                    String js = data.substring(index);
                    list.add((JSONObject) jsonParser.parse(js));
                }
                data = "";
            }
            else
            {
                data += character;
            }
        }

        return list;
    }

    public static boolean isImage(String url)
    {
        if( ( url.contains(Constants.JPG_EXTENSION) ||
              url.contains(Constants.JPEG_EXTENSION) ||
              url.contains(Constants.PNG_EXTENSION)) &&
              !(url.contains(Constants.CMS_ADMIN) || url.contains(Constants.ASSETS))
             )
        {
            return true;
        }

        return false;
    }

    public static ArrayList<JSONObject> getImageRequests(ArrayList<JSONObject> requests)
    {
        ArrayList<JSONObject> imageRequests = new ArrayList<JSONObject>();

        for(JSONObject jsonObject : requests)
        {
            String http_request = jsonObject.get(Constants.HTTP_REQUEST).toString();
            String[] strList = http_request.split(" ");

            if(strList.length != 3)
            {
                continue;
            }
            String url = strList[1];

            if(isImage(url))
            {
                jsonObject.put(Constants.URL, url);
                imageRequests.add(jsonObject);
            }
        }

        return imageRequests;
    }

    public static ArrayList<JSONObject> getInRangeRequests(long from, long to, ArrayList<JSONObject> requests)
    {
        ArrayList<JSONObject> inRange = new ArrayList<JSONObject>();

        for(JSONObject jsonObject : requests)
        {
            long timestamp = Long.parseLong(jsonObject.get(Constants.TIMESTAMP).toString());

            if(timestamp>from&&timestamp<to)
            {
                inRange.add(jsonObject);
            }
        }

        return inRange;
    }

    public static void getImageStats(int N, ArrayList<JSONObject> imageRequests, String type)
    {
        HashMap<String,JSONObject> map = new HashMap();

        for(JSONObject jsonObject : imageRequests)
        {
            String key = jsonObject.get(type).toString();
            JSONObject mapObject = null;

            if(map.containsKey(key))
            {
                mapObject = map.get(key);
                int count = Integer.parseInt(mapObject.get(Constants.COUNT).toString());
                int response_time = Integer.parseInt(mapObject.get(Constants.RESPONSE_TIME).toString());
                int _time = Integer.parseInt(jsonObject.get(Constants.RESPONSE_TIME).toString());
                mapObject.put(Constants.COUNT ,count+1);
                mapObject.put(Constants.RESPONSE_TIME , response_time + _time);
            }
            else
            {
                mapObject = new JSONObject();
                mapObject.put(Constants.COUNT,1);
                mapObject.put(Constants.KEY,key);
                int response_time = Integer.parseInt(jsonObject.get(Constants.RESPONSE_TIME).toString());
                mapObject.put(Constants.RESPONSE_TIME, response_time);
            }

            map.put(key, mapObject);
        }

        ArrayList<JSONObject> list = new ArrayList<JSONObject>();

        for(String key : map.keySet())
        {
            list.add(map.get(key));
        }

        printResults(list, N, type);
    }

    public static void printResults(ArrayList<JSONObject> requests, int N, String type)
    {
        Collections.sort(requests, new Comparator<JSONObject>()
        {
            public int compare(JSONObject a, JSONObject b)
            {
                int aCount = Integer.parseInt(a.get(Constants.COUNT).toString());
                int bCount = Integer.parseInt(b.get(Constants.COUNT).toString());

                return bCount-aCount;
            }
        });
        if(type.equalsIgnoreCase("url"))
        {
            System.out.println("ImageName          Count       Avg Response Time");
        }
        else{
            System.out.println("user          Count       Avg Response Time");
        }
        for(int i=0; i<N&&i<requests.size();i++)
        {
            JSONObject jsonObject = requests.get(i);
            String key = jsonObject.get(Constants.KEY).toString();
            int count = Integer.parseInt(jsonObject.get(Constants.COUNT).toString());
            int response_time = Integer.parseInt(jsonObject.get(Constants.RESPONSE_TIME).toString())/count;

            System.out.println(key+" "+count+" "+response_time);
        }
    }

    public static void main(String args[]) throws ParseException, IOException
    {
        //from to N type folder
        long from = Long.parseLong(args[0]);//1452203928;
        long to = Long.parseLong(args[1]);//1462658328;
        int N = Integer.parseInt(args[2]);
        int ty = Integer.parseInt(args[3]);
        String path = args[4];

        String type = Constants.REMOTE_ADDR;

        if(ty==1)
        {
            type = Constants.URL;
        }

        File folder = new File(path);
        ArrayList<JSONObject> imageRequests = new ArrayList<JSONObject>();

        for(File file : folder.listFiles())
        {
            imageRequests.addAll(getImageRequests(readData(file)));
        }

        System.out.println(imageRequests.size());
        getImageStats(N, imageRequests, type);
    }
}
