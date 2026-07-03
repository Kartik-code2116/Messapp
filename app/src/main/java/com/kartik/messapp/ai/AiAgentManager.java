package com.kartik.messapp.ai;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.kartik.messapp.BuildConfig;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiAgentManager {
    private static final String TAG = "AiAgentManager";
    private static AiAgentManager instance;
    private ChatFutures chatFutures;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private boolean isFirstMessage = true;

    // RAG System Context base
    private static final String APP_CONTEXT = 
        "You are the official AI Assistant for 'MessApp'. " +
        "MessApp is an Android application that connects users (students/professionals) with local Messes (meal providers). " +
        "Users can discover messes, view their weekly menus, read reviews, and subscribe to a mess. " +
        "Mess Owners can use the app to manage their mess profile, update their daily/weekly menu, view their subscribers, and track revenue. " +
        "Subscriptions are handled within the app. " +
        "Keep your answers concise, helpful, and strictly related to MessApp features.";

    private AiAgentManager() {
        initModel();
    }

    public static synchronized AiAgentManager getInstance() {
        if (instance == null) {
            instance = new AiAgentManager();
        }
        return instance;
    }

    private void initModel() {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.7f;
        
        // System instruction is available in newer SDKs, but we can also just prepend it if needed.
        // For generativeai 0.9.0, we construct it:
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash-latest",
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build(),
                Collections.singletonList(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH))
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        chatFutures = model.startChat();
    }

    public interface ChatCallback {
        void onSuccess(String response);
        void onError(Throwable throwable);
    }

    public void sendMessage(String userMessage, ChatCallback callback) {
        if (chatFutures == null) {
            callback.onError(new IllegalStateException("Chat not initialized"));
            return;
        }

        String prompt = isFirstMessage ? APP_CONTEXT + "\n\nUser Question: " + userMessage : userMessage;
        isFirstMessage = false;

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> future = chatFutures.sendMessage(content);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (result != null && result.getText() != null) {
                    callback.onSuccess(result.getText());
                } else {
                    callback.onError(new Exception("Empty response"));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Chat error", t);
                callback.onError(t);
            }
        }, executor);
    }
}
