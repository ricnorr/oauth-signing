package ricnorr.jb2021taskauth.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class LoginController {


    @Value("${client_id}")
    private String clientId;

    @Value("${client_secret}")
    private String clientSecret;


    @RequestMapping(value = "/url/signin/buttonClick")
    public void loginButton(final HttpServletResponse response) throws IOException {
        response.sendRedirect(
                "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&" +
                        "redirect_uri=http://localhost:8080/url/signin/button" +
                        "&scope=https://www.googleapis.com/auth/userinfo.profile" +
                        "&client_id=" + clientId +
                        "&access_type=offline");
    }

    @RequestMapping(value = "/url/signin/button", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public
    @ResponseBody
    void loginButtonHandle(final HttpServletRequest request, final HttpServletResponse httpServletResponse, final HttpSession session) throws IOException {
        Map<String, String> response = makeHttpRequest("https://oauth2.googleapis.com/token", MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                HttpMethod.POST,
                CollectionUtils.toMultiValueMap(Map.of("client_id", List.of(clientId),
                        "client_secret", List.of(clientSecret),
                        "grant_type", List.of("authorization_code"),
                        "redirect_uri", List.of("http://localhost:8080/url/signin/button"),
                        "code", List.of(request.getParameter("code")))));
        response = makeHttpRequest(
                "https://www.googleapis.com/oauth2/v1/userinfo",
                MediaType.APPLICATION_JSON_VALUE,
                HttpMethod.GET,
                CollectionUtils.toMultiValueMap(Map.of("access_token", List.of(response.get("access_token")))));
        session.setAttribute("username", response.get("name"));
        httpServletResponse.sendRedirect("/url/home");
    }

    private Map<String, String> makeHttpRequest(final String url, final String contentType, final HttpMethod method, final MultiValueMap<String, String> params) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", contentType);
        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParams(params);
        return new RestTemplate().exchange(
                builder.toUriString(),
                method,
                new HttpEntity<>(headers),
                (new ParameterizedTypeReference<Map<String, String>>() {
                })).getBody();
    }


    @RequestMapping(value = "/url/home", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody
    String home(final HttpSession session) {
        if (session.getAttribute("username") != null) {
            return "Welcome " + session.getAttribute("username") + ". This is a home page!" + createButton("/url/signout", "signout");
        } else {
            return "Welcome new user! This is a home page" + createButton("/url/signin", "signin");
        }

    }

    private String createButton(final String url, final String action) {
        return "<form action=" + url + " method = get><button>" + action + "</button></form>";
    }

    @RequestMapping(value = "/url/signin", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public @ResponseBody
    String signIn(final HttpSession session) {
        if (session.getAttribute("username") != null) {
            return "You are already signed in as " + session.getAttribute("username") + createButton("/url/home", "go to home page");
        } else {
            return "<form action = /url/signin/buttonClick method = get><button>Sign in using Google Account</button><form>";
        }

    }


    @RequestMapping(value = "/url/signout", method = RequestMethod.GET)
    void signOut(final HttpServletResponse response, final HttpSession session) throws IOException {
        if (session.getAttribute("username") != null) {
            session.removeAttribute("username");
        }
        response.sendRedirect("/url/home");
    }

    @RequestMapping(value = "/error", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    String error() {
        return "not found";
    }
}
