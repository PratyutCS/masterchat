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
 * Usage: ApiClient.init(tokenManager) once, then ApiClient.getSyncApi(), etc.
 */
public class ApiClient {

    private static Retrofit retrofit;
    private static TokenManager tokenManager;

    /**
     * Initialize the API client. Call once from Application.
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
                    String token = (tokenManager != null) ? tokenManager.getToken() : null;
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

    private static void ensureInitialized() {
        if (retrofit == null) {
            // Fallback initialization if something went wrong (e.g. process death)
            // This is a safety net.
            init(new TokenManager(com.masterapp.chat.ChatApplication.getAppContext()));
        }
    }

    public static <T> T create(Class<T> service) {
        ensureInitialized();
        return retrofit.create(service);
    }
    
    public static SyncApi getSyncApi() {
        ensureInitialized();
        return retrofit.create(SyncApi.class);
    }

    /** Get the Auth API interface */
    public static AuthApi getAuthApi() {
        ensureInitialized();
        return retrofit.create(AuthApi.class);
    }

    /** Get the User API interface */
    public static UserApi getUserApi() {
        ensureInitialized();
        return retrofit.create(UserApi.class);
    }

    /** Get the Conversation API interface */
    public static ConversationApi getConversationApi() {
        ensureInitialized();
        return retrofit.create(ConversationApi.class);
    }

    /** Get the Message API interface */
    public static MessageApi getMessageApi() {
        ensureInitialized();
        return retrofit.create(MessageApi.class);
    }

    /** Legacy support for instance-based calls */
    public static ApiClient getInstance(android.content.Context context) {
        return new ApiClient();
    }
}
