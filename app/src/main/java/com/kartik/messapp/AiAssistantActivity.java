package com.kartik.messapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.kartik.messapp.ai.AiAgentManager;

import java.util.ArrayList;
import java.util.List;

public class AiAssistantActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Add initial greeting
        messageList.add(new ChatMessage("Hello! I am the MessApp AI Assistant. How can I help you today?", false));
        chatAdapter.notifyItemInserted(0);

        sendButton.setOnClickListener(v -> {
            String text = messageEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessageToAi(text);
            }
        });
    }

    private void sendMessageToAi(String userText) {
        // Add User Message
        messageList.add(new ChatMessage(userText, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        
        messageEditText.setText("");
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);

        AiAgentManager.getInstance().sendMessage(userText, new AiAgentManager.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    messageList.add(new ChatMessage(response, false));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    Toast.makeText(AiAssistantActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static class ChatMessage {
        String text;
        boolean isUser;

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            if (message.isUser) {
                holder.userContainer.setVisibility(View.VISIBLE);
                holder.aiContainer.setVisibility(View.GONE);
                holder.userText.setText(message.text);
            } else {
                holder.userContainer.setVisibility(View.GONE);
                holder.aiContainer.setVisibility(View.VISIBLE);
                // Simple markdown cleanup if necessary, but just setting text for now
                holder.aiText.setText(message.text.replaceAll("\\*\\*", "")); 
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            View aiContainer, userContainer;
            TextView aiText, userText;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                aiContainer = itemView.findViewById(R.id.aiMessageContainer);
                userContainer = itemView.findViewById(R.id.userMessageContainer);
                aiText = itemView.findViewById(R.id.aiMessageText);
                userText = itemView.findViewById(R.id.userMessageText);
            }
        }
    }
}
