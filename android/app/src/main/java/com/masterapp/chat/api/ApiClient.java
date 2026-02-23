package com.masterapp.chat.api;

import com.masterapp.chat.util.Constants;
import com.masterapp.chat.util.TokenManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client.
 * 
 * Adds JWT token to every request via an OkHttp interceptor.
 * Usage: ApiClient.init(tokenManager) once, then ApiClient.getAuthApi(), etc.
 */
public class ApiClient {

    private static Retrofit retrofit;
    private static TokenManager tokenManager;

    /**
     * Initialize the API client. Call once from Application or first Activity.
     */
    public static void init(TokenManager tm) {
        tokenManager = tm;

        // Logging interceptor for debugging
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp client with auth interceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request.Builder builder = chain.request().newBuilder();
                    // Add JWT token if available
                    String token = tokenManager.getToken();
                    if (token != null) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Required for manual interface creation outside ViewModel
     */
    public static ApiClient getInstance(android.content.Context context) {
        // Assume init() was already called by Application class. 
        // We just return a dummy new wrapper that implements create fallback
        return new ApiClient();
    }

    public <T> T create(Class<T> service) {
        return retrofit.create(service);
    }
    
    public SyncApi getSyncApi() {
        return retrofit.create(SyncApi.class);
    }

    /** Get the Auth API interface */
    public static AuthApi getAuthApi() {
        return retrofit.create(AuthApi.class);
    }

    /** Get the User API interface */
    public static UserApi getUserApi() {
        return retrofit.create(UserApi.class);
    }

    /** Get the Conversation API interface */
    public static ConversationApi getConversationApi() {
        return retrofit.create(ConversationApi.class);
    }

    /** Get the Message API interface */
    public static MessageApi getMessageApi() {
        return retrofit.create(MessageApi.class);
    }
}
