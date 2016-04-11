/**
 * Created by AlejandroSA on 15/03/2016.
 */
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import gmailsettings.GmailSettingsService;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Controller {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "AutomaticSignatures";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/google-credentials.json");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart.json
     */
    private static final List<String> SCOPES =
            Arrays.asList(
                    "https://apps-apis.google.com/a/feeds/emailsettings/2.0/",
                    "https://spreadsheets.google.com/feeds");

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static String DOMAIN_NAME = "cloudimpulsion.com";

    private static String [] COMPANY_DIRECTION = {"Barcelona Activa", "C/ Llacuna 162", "08018 Barcelona"};

    private static String SPREADSHEET_NAME = "TestingSignatures";

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                new FileInputStream("C:\\Users\\AlejandroSA\\IdeaProjects\\AutomaticSignaturesSS.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */

    public static void main(String[] args) throws Exception {

        //Get Credentials we are going to use
        Credential credential = authorize();

        //Create the service to work with
        SpreadsheetService service =
                new SpreadsheetService("MySpreadsheetIntegration-v1");

        // Give the credentials to work using Oauth2.0
        service.setOAuth2Credentials(credential);

        //Get the spreadsheet we are looking for
        SpreadsheetEntry spreadSheet = getSpreadSheet(service);

        //Retrieve the spreadsheet info
        ArrayList<ArrayList<String>> spreadSheetInfo = getSpreadSheetInfo(spreadSheet, service);

        //Call to method updateSignature
        updateSignature(credential, spreadSheetInfo);

        /*try {
            updateSignature(authorize());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private static SpreadsheetEntry getSpreadSheet (SpreadsheetService service) throws IOException, ServiceException {

        // Define the URL to request.  This should never change.
        URL SPREADSHEET_FEED_URL = new URL(
                "https://spreadsheets.google.com/feeds/spreadsheets/private/full");

        // Make a request to the API and get all spreadsheets.
        SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL,
                SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();

        if (spreadsheets.size() == 0) {
            // TODO: There were no spreadsheets, act accordingly.
        }

        // Search for the needed spreadsheet where we store the signature settings
        SpreadsheetEntry spreadsheet = null;

        for (SpreadsheetEntry entry: spreadsheets) {
            if (entry.getTitle().getPlainText().equals(SPREADSHEET_NAME))
            {
                //giving our spreadsheet the found spreadsheet to work with
                spreadsheet = entry;
            }
        }
        if (spreadsheet != null)
        {
            //Give the found spreadsheet and the service to retrieve spreadsheet information
            return spreadsheet;
        }
        else
        {
            return null;
            //TODO: The specified spreadsheet is not found at the list.
        }
    }

    /**
     * @param spreadsheet
     * @param service
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    private static ArrayList<ArrayList<String>> getSpreadSheetInfo(SpreadsheetEntry spreadsheet, SpreadsheetService service) throws IOException, ServiceException {
        //Print the spreadsheet name
        System.out.println(spreadsheet.getTitle().getPlainText());

        WorksheetFeed worksheetFeed = service.getFeed(
                spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
        List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
        WorksheetEntry worksheet = worksheets.get(0);

        // Fetch the list feed of the worksheet.
        URL listFeedUrl = worksheet.getListFeedUrl();
        System.out.println(listFeedUrl);
        ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);

        //Creating an arraylist with 5 arraylists to introduce email at 0, name at 1, lastNAme at 2, job at 3, phone at 4 and image at 5
        ArrayList<ArrayList<String>> emailData = new ArrayList<>();

        //Inicialize 6 arraylists which will contain every field
        for (int i = 0; i < 5; i++) {
            emailData.add(new ArrayList<>());
        }

        // Iterate through each row, printing its cell values.
        //WARNING, It just reads from the second row until the first blank row
        int column = 1;

        for (ListEntry row : listFeed.getEntries()) {

            // Iterate over the remaining columns, and print each cell value
            for (String tag : row.getCustomElements().getTags()) {
                switch (column)
                {
                    case 1:
                    {
                        //Adding the first column (email)
                        emailData.get(0).add(row.getCustomElements().getValue(tag));
                        break;
                    }
                    case 2:
                    {
                        //Adding the second column (name)
                        emailData.get(1).add(row.getCustomElements().getValue(tag));
                        break;
                    }
                    case 3:
                    {
                        //Adding the third column (lastName)
                        emailData.get(2).add(row.getCustomElements().getValue(tag));
                        break;
                    }
                    case 4:
                    {
                        //Adding the fourth column (job)
                        emailData.get(3).add(row.getCustomElements().getValue(tag));
                        break;
                    }
                    case 5:
                    {
                        //Adding the fifth column (phone)
                        emailData.get(4).add(row.getCustomElements().getValue(tag));
                        break;
                    }
                    case 6:
                    {
                        //Adding the fifth column (image)
                        emailData.get(4).add(row.getCustomElements().getValue(tag));
                        break;
                    }
                }
                column++;
                if (column > 6)
                {
                    break;
                }
            }
            column = 1;
        }
        System.out.println(emailData);
        return emailData;
    }

    /**
     * @param credential
     * @param emailsData is an ArrayList with ArrayLists of strings that includes all the info of the signatures we want to use
     * @throws Exception
     */
    private static void updateSignature(Credential credential, ArrayList<ArrayList<String>> emailsData) throws Exception {

        GmailSettingsService service = new GmailSettingsService(APPLICATION_NAME, DOMAIN_NAME, null, null){
            @Override
            public void setUserCredentials(String username, String password)
                    throws AuthenticationException {
                // Nothing to do here, just Overriding the old method and setting it to null so we can later setOauthCredentials to the service
            }};

        service.setOAuth2Credentials(credential);
        List users = new ArrayList();
        for (int i = 0; i < emailsData.size(); i++)
        {
            users.add(emailsData.get(0).get(i).split("@")[0]);

            String signature = "<div><span><span>"+emailsData.get(1).get(i)+" "+emailsData.get(2).get(i)+"|</span><span>&nbsp;"+emailsData.get(3).get(i)+" |&nbsp;</span><span> +34 "+emailsData.get(4).get(i)+"&nbsp;|</span><span>&nbsp;"+emailsData.get(0).get(i)+"</span></span></div>\n" +
                    "<div>\n" +
                    "    <table style=\"width: 100%;\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                    "        <tbody>\n" +
                    "            <tr>\n" +
                    "                <td valign=\"middle\"><span><span><span style=\"font-family: sans-serif;\">"+COMPANY_DIRECTION[0]+"</span><span>&nbsp;</span><span style=\"font-family: sans-serif;\"><br /></span><span style=\"font-family: sans-serif;\">"+COMPANY_DIRECTION[1]+"</span><span>&nbsp;<br /></span><span style=\"font-family: sans-serif;\">"+COMPANY_DIRECTION[2]+"</span><span style=\"font-family: sans-serif;\"><br /></span></span></span>\n" +
                    "                    <table style=\"width: 100%;\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                    "                        <tbody>\n" +
                    "                            <tr>\n"+
                    "                            </tr>\n" +
                    "                        </tbody>\n" +
                    "                    </table>\n" +
                    "                    <img class=\"CToWUd\" src=\"https://ci3.googleusercontent.com/proxy/JIqfv026_rS33kvOW19TVIy68FK3LnRvDc1CAJDGRoV-8_58m9Tl77DtUPO9H0-5qjXtKLIqu3wJJWVLngP9mp_L_4MCzg1vGw965v8TjehI1LY=s0-d-e1-ft#http://cloudimpulsion.com/wp-content/uploads/2014/03/logo.jpg\" alt=\"\" width=\"96\" height=\"52\" /></td>\n" +
                    "            </tr>\n" +
                    "            <tr>\n" +
                    "                <td valign=\"middle\">\n" +
                    "                    <div>&nbsp;</div>\n" +
                    "                    <span><a href=\"http://www.cloudimpulsion.com/\" target=\"_blank\"><span style=\"color: #ff9900;\">www.cloudimpulsion.com</span></a></span></td>\n" +
                    "            </tr>\n" +
                    "        </tbody>\n" +
                    "    </table>\n" +
                    "    <div><span>S&iacute;guenos en&nbsp;</span><a href=\"https://app.getsignals.com/link?url=https%3A%2F%2Ftwitter.com%2FCloudImpulsion&amp;ukey=agxzfnNpZ25hbHNjcnhyGAsSC1VzZXJQcm9maWxlGICAgILaqKYKDA&amp;k=baed42b1-60a3-4239-f23d-5703df35828f\" target=\"_blank\"><img class=\"CToWUd\" src=\"https://ci3.googleusercontent.com/proxy/ahqkVjVwc5inWf56mKXlUSR7eOalwMgUZgvBFPpbnT1jJiieBKy_1SQtW90eYaoe_7K25UTBY9HrfxJWZJ4oObpFLM9_OaKpsC_zEOhOLwF6D16NY5YD4b10j_qnqiaZuVVYWHBn5LYvG98PE_PcResqzDraPuJvL6Pds2Mpi9TEWI-BxkSFzfUftBpWlCd8yRgXi_yqvp1u=s0-d-e1-ft#https://www.brandmymail.com/proxy?url=https%3A//s3.amazonaws.com/static.brandmymail.com/v0.0.29/img/social_icons/16/twitter_16.png\" alt=\"twitter\" width=\"16\" height=\"16\" /></a><span>&nbsp;</span><a href=\"https://app.getsignals.com/link?url=http%3A%2F%2Fwww.linkedin.com%2Fcompany%2F3333421%3Ftrk%3Dprof-exp-company-name&amp;ukey=agxzfnNpZ25hbHNjcnhyGAsSC1VzZXJQcm9maWxlGICAgILaqKYKDA&amp;k=fe8c61aa-ac2a-4cf3-e371-277857111e2b\" target=\"_blank\"><img class=\"CToWUd\" src=\"https://ci3.googleusercontent.com/proxy/SaHQOrJVkZwD0Xk60YAJg1J5cSOFyvZLGS4zhKjfaN02Iqh-La5nhywoiD1M14BNAcPnffzVQUrzbTcnQ1CLc8_ju9Q5UaXCxPali6QfKoOSwmF2mSxYzScTHsxV6wuALwNppwlyww4JOxPpgKYbSAeNUTNADBrVgpmzO0wPdoBZ6rEouLlwZiqJ3HPSZpM9hgXk-YFTBrrLgA=s0-d-e1-ft#https://www.brandmymail.com/proxy?url=https%3A//s3.amazonaws.com/static.brandmymail.com/v0.0.29/img/social_icons/16/linkedin_16.png\" alt=\"linkedin\" width=\"16\" height=\"16\" /></a></div>\n" +
                    "</div>\n" +
                    "<div><em><span style=\"color: #ff9900;\">&iexcl;No te quedes atr&aacute;s en la carrera&nbsp;</span></em><em><span style=\"color: #ff9900;\">del&nbsp;</span></em><em><span style=\"color: #ff9900;\">Cloud Computing!</span></em></div>";

            service.changeSignature(users, signature);

            users.remove(0);
        }
    }
}