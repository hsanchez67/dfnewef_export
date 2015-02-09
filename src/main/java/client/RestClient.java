package client;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Created by hsanchez on 2/5/2015.
 */
public class RestClient {

    public static HttpClient httpClientAuth() throws NamingException {
        InitialContext context = new InitialContext();
        String blueboxUser = (String)context.lookup("client.bluebox.user");
        String blueboxPass = (String)context.lookup("client.bluebox.pass");

        HttpClient httpClient = new HttpClient();
        Credentials defaultcreds = new UsernamePasswordCredentials(blueboxUser, blueboxPass);
        httpClient.getState().setCredentials(AuthScope.ANY, defaultcreds);
        httpClient.getParams().setAuthenticationPreemptive(true);

        return httpClient;
    }
}

