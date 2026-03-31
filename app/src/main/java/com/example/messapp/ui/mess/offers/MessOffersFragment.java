package com.example.messapp.ui.mess.offers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentMessOffersBinding;
import com.example.messapp.models.Offer;
import com.example.messapp.ui.user.OfferAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MessOffersFragment extends Fragment {

    private FragmentMessOffersBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private OfferAdapter offerAdapter;
    private List<Offer> offerList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessOffersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        offerList = new ArrayList<>();
        offerAdapter = new OfferAdapter(offerList, true);
        binding.recyclerViewOffers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewOffers.setAdapter(offerAdapter);

        fetchMessIdAndLoadOffers();

        binding.fabAddOffer.setOnClickListener(v -> navigateToAddOffer());

        return root;
    }

    private void fetchMessIdAndLoadOffers() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to view offers", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            loadOffers(currentMessId);
                        } else {
                            Toast.makeText(getContext(), "Mess ID not found for this user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching mess owner data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOffers(String messId) {
        db.collection("offers")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    offerList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Offer offer = document.toObject(Offer.class);
                        offerList.add(offer);
                    }
                    offerAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading offers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToAddOffer() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_messOffersFragment_to_addOfferFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
