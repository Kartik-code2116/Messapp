package com.example.messapp.ui.mess.reports;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.models.Student;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubscriptionReportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String messId;
    private String messName = "Mess";

    private TextView textStatTotal, textStatActive, textStatExpired;
    private EditText editSearchReport;
    private View chipFilterAll, chipFilterActive, chipFilterExpired;
    private TextView textChipAll, textChipActive, textChipExpired;
    private RecyclerView recyclerSubscriptionReport;
    private View layoutEmptyState;
    private View progressLoadingReport;
    private View btnExportPdf;
    private Spinner spinnerMonthFilter;
    private Spinner spinnerYearFilter;

    private List<Student> allStudentsList = new ArrayList<>();
    private List<Student> monthFilteredList = new ArrayList<>();
    private List<Student> filteredStudentsList = new ArrayList<>();
    private Map<String, Long> studentJoinDates = new HashMap<>();
    private Map<String, List<Long>> studentTransactions = new HashMap<>();

    private String currentSearchQuery = "";
    private String currentFilter = "ALL"; // "ALL", "ACTIVE", "EXPIRED"

    private ReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_report);

        db = FirebaseFirestore.getInstance();
        messId = getIntent().getStringExtra("EXTRA_MESS_ID");

        if (messId == null) {
            Toast.makeText(this, "Mess ID is required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        fetchMessDetails();
        fetchSubscriptionReportData();
    }

    private void initViews() {
        textStatTotal = findViewById(R.id.text_stat_total);
        textStatActive = findViewById(R.id.text_stat_active);
        textStatExpired = findViewById(R.id.text_stat_expired);
        editSearchReport = findViewById(R.id.edit_search_report);
        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterActive = findViewById(R.id.chip_filter_active);
        chipFilterExpired = findViewById(R.id.chip_filter_expired);
        textChipAll = findViewById(R.id.text_chip_all);
        textChipActive = findViewById(R.id.text_chip_active);
        textChipExpired = findViewById(R.id.text_chip_expired);
        recyclerSubscriptionReport = findViewById(R.id.recycler_subscription_report);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        progressLoadingReport = findViewById(R.id.progress_loading_report);
        btnExportPdf = findViewById(R.id.btn_export_pdf);

        recyclerSubscriptionReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(this, filteredStudentsList, studentJoinDates);
        recyclerSubscriptionReport.setAdapter(adapter);

        setupSpinners();
    }

    private void setupSpinners() {
        spinnerMonthFilter = findViewById(R.id.spinner_month_filter);
        spinnerYearFilter = findViewById(R.id.spinner_year_filter);

        String[] months = {
            "All Months", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonthFilter.setAdapter(monthAdapter);

        Calendar currentCal = Calendar.getInstance();
        int currentYear = currentCal.get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        years.add(String.valueOf(currentYear));
        years.add(String.valueOf(currentYear - 1));
        years.add(String.valueOf(currentYear - 2));

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearFilter.setAdapter(yearAdapter);

        // Set default month selection to current month + 1 (since 0 is All Months)
        int currentMonthIndex = currentCal.get(Calendar.MONTH) + 1;
        spinnerMonthFilter.setSelection(currentMonthIndex);
        spinnerYearFilter.setSelection(0); // Current year

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (allStudentsList != null && !allStudentsList.isEmpty()) {
                    applyFiltersAndSearch();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerMonthFilter.setOnItemSelectedListener(spinnerListener);
        spinnerYearFilter.setOnItemSelectedListener(spinnerListener);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        btnExportPdf.setOnClickListener(v -> generateAndOpenPDFReport());

        editSearchReport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFiltersAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipFilterAll.setOnClickListener(v -> selectFilter("ALL"));
        chipFilterActive.setOnClickListener(v -> selectFilter("ACTIVE"));
        chipFilterExpired.setOnClickListener(v -> selectFilter("EXPIRED"));
    }

    private void fetchMessDetails() {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null) {
                            messName = name;
                        }
                    }
                });
    }

    private void fetchSubscriptionReportData() {
        showLoading(true);
        // Query 1: Fetch transactions to determine when each student first joined
        db.collection("transactions")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(transactionDocs -> {
                    studentJoinDates.clear();
                    studentTransactions.clear();
                    for (DocumentSnapshot doc : transactionDocs.getDocuments()) {
                        String userId = doc.getString("userId");
                        Long timestamp = doc.getLong("timestamp");
                        if (userId != null && timestamp != null) {
                            // Join date calculation (earliest timestamp)
                            if (!studentJoinDates.containsKey(userId) || timestamp < studentJoinDates.get(userId)) {
                                studentJoinDates.put(userId, timestamp);
                            }
                            // Store all transaction dates per student
                            if (!studentTransactions.containsKey(userId)) {
                                studentTransactions.put(userId, new ArrayList<>());
                            }
                            studentTransactions.get(userId).add(timestamp);
                        }
                    }

                    // Query 2: Fetch all students of this mess
                    fetchStudents();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    fetchStudents(); // fallback to students query
                });
    }

    private void fetchStudents() {
        db.collection("users")
                .whereEqualTo("role", "USER")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(userDocs -> {
                    allStudentsList.clear();
                    for (DocumentSnapshot doc : userDocs.getDocuments()) {
                        Student student = doc.toObject(Student.class);
                        if (student != null) {
                            student.setUserId(doc.getId());
                            allStudentsList.add(student);
                        }
                    }
                    // Sort students alphabetically by name
                    Collections.sort(allStudentsList, (s1, s2) -> {
                        String name1 = s1.getName() != null ? s1.getName().toLowerCase() : "";
                        String name2 = s2.getName() != null ? s2.getName().toLowerCase() : "";
                        return name1.compareTo(name2);
                    });

                    applyFiltersAndSearch();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private long[] getSelectedMonthBoundaries() {
        int monthPos = spinnerMonthFilter.getSelectedItemPosition();
        if (monthPos == 0) {
            // "All Months" selected
            return new long[]{0, Long.MAX_VALUE};
        }

        int selectedMonth = monthPos - 1; // 0 for Jan, 11 for Dec
        String yearStr = (String) spinnerYearFilter.getSelectedItem();
        int selectedYear = Integer.parseInt(yearStr);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long monthEnd = cal.getTimeInMillis();

        return new long[]{monthStart, monthEnd};
    }

    private boolean doesStudentBelongToMonth(Student student, long monthStart, long monthEnd) {
        if (monthStart == 0) {
            return true;
        }

        // 1. Check if the student made a transaction within this month
        List<Long> txs = studentTransactions.get(student.getUserId());
        if (txs != null) {
            for (long tx : txs) {
                if (tx >= monthStart && tx <= monthEnd) {
                    return true;
                }
            }
        }

        // 2. Check if their subscription duration overlaps with this month
        long lunchExp = student.getLunchSubscriptionExpiry();
        long dinnerExp = student.getDinnerSubscriptionExpiry();
        long oneTimeExp = student.getOneTimeMealExpiry();
        long subExp = student.getSubscriptionExpiry();

        long maxExp = "ONE_TIME".equals(student.getSubscriptionType())
                ? (oneTimeExp > 0 ? oneTimeExp : subExp)
                : Math.max(lunchExp > 0 ? lunchExp : subExp, dinnerExp > 0 ? dinnerExp : subExp);

        if (maxExp > 0) {
            Long joinDate = studentJoinDates.get(student.getUserId());
            long startBound = (joinDate != null && joinDate > 0) ? joinDate : 0;

            // Overlap check: [startBound, maxExp] overlaps with [monthStart, monthEnd]
            if (startBound <= monthEnd && maxExp >= monthStart) {
                return true;
            }
        }

        return false;
    }

    private void calculateStatistics(List<Student> list, long nowOrMonthStart) {
        int total = list.size();
        int active = 0;
        int expired = 0;

        for (Student student : list) {
            if (isStudentActiveForMonth(student, nowOrMonthStart)) {
                active++;
            } else {
                expired++;
            }
        }

        textStatTotal.setText(String.valueOf(total));
        textStatActive.setText(String.valueOf(active));
        textStatExpired.setText(String.valueOf(expired));
    }

    private boolean isStudentActiveForMonth(Student student, long nowOrMonthStart) {
        long lunchExp = student.getLunchSubscriptionExpiry();
        long dinnerExp = student.getDinnerSubscriptionExpiry();
        long oneTimeExp = student.getOneTimeMealExpiry();
        long subExp = student.getSubscriptionExpiry();

        long maxExp = "ONE_TIME".equals(student.getSubscriptionType())
                ? (oneTimeExp > 0 ? oneTimeExp : subExp)
                : Math.max(lunchExp > 0 ? lunchExp : subExp, dinnerExp > 0 ? dinnerExp : subExp);

        return maxExp > 0 && maxExp >= nowOrMonthStart;
    }

    private void selectFilter(String filter) {
        currentFilter = filter;

        // Reset all backgrounds & text colors
        resetChipStyle(chipFilterAll, textChipAll);
        resetChipStyle(chipFilterActive, textChipActive);
        resetChipStyle(chipFilterExpired, textChipExpired);

        // Highlight selected
        if ("ALL".equals(filter)) {
            setSelectedChipStyle(chipFilterAll, textChipAll);
        } else if ("ACTIVE".equals(filter)) {
            setSelectedChipStyle(chipFilterActive, textChipActive);
        } else if ("EXPIRED".equals(filter)) {
            setSelectedChipStyle(chipFilterExpired, textChipExpired);
        }

        applyFiltersAndSearch();
    }

    private void setSelectedChipStyle(View chip, TextView text) {
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) chip;
        card.setStrokeColor(getResources().getColor(R.color.brand_primary));
        card.setStrokeWidth((int) (1.5 * getResources().getDisplayMetrics().density));
        text.setTextColor(getResources().getColor(R.color.brand_primary));
    }

    private void resetChipStyle(View chip, TextView text) {
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) chip;
        card.setStrokeColor(getResources().getColor(R.color.neutral_300));
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        text.setTextColor(getResources().getColor(R.color.text_body));
    }

    private void applyFiltersAndSearch() {
        filteredStudentsList.clear();
        monthFilteredList.clear();

        long[] boundaries = getSelectedMonthBoundaries();
        long monthStart = boundaries[0];
        long monthEnd = boundaries[1];

        // 1. Filter by selected Month/Year
        for (Student student : allStudentsList) {
            if (doesStudentBelongToMonth(student, monthStart, monthEnd)) {
                monthFilteredList.add(student);
            }
        }

        // 2. Calculate statistics for this month/year selection
        long now = System.currentTimeMillis();
        long nowOrMonthStart = (monthStart == 0) ? now : monthStart;
        calculateStatistics(monthFilteredList, nowOrMonthStart);

        // 3. Apply Search & Status filters to monthFilteredList
        for (Student student : monthFilteredList) {
            // Apply Search Query
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String name = student.getName() != null ? student.getName().toLowerCase() : "";
                String email = student.getEmail() != null ? student.getEmail().toLowerCase() : "";
                matchesSearch = name.contains(currentSearchQuery.toLowerCase()) || email.contains(currentSearchQuery.toLowerCase());
            }

            if (!matchesSearch) continue;

            // Apply Filter Chips
            boolean isActive = isStudentActiveForMonth(student, nowOrMonthStart);
            if ("ACTIVE".equals(currentFilter) && !isActive) {
                continue;
            }
            if ("EXPIRED".equals(currentFilter) && isActive) {
                continue;
            }

            filteredStudentsList.add(student);
        }

        adapter.notifyDataSetChanged();

        if (filteredStudentsList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerSubscriptionReport.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerSubscriptionReport.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean loading) {
        progressLoadingReport.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerSubscriptionReport.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * NATIVE A4 HIGH-FIDELITY PDF REPORT GENERATOR WITH PAGINATION
     */
    /**
     * NATIVE A4 HIGH-FIDELITY PDF REPORT GENERATOR WITH PAGINATION
     */
    private void generateAndOpenPDFReport() {
        if (monthFilteredList.isEmpty()) {
            Toast.makeText(this, "No records to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        int pageWidth = 595; // A4 Width in pts
        int pageHeight = 842; // A4 Height in pts

        // Setup common paints
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setColor(0xFF3D2415); // Brand secondary (dark brown text)
        titlePaint.setTextSize(18f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint subTitlePaint = new Paint();
        subTitlePaint.setColor(0xFFF97316); // Brand Primary Orange
        subTitlePaint.setTextSize(10f);
        subTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        subTitlePaint.setLetterSpacing(0.1f);

        Paint metaPaint = new Paint();
        metaPaint.setColor(0xFF8D6E52); // neutrals 500
        metaPaint.setTextSize(9f);
        metaPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        Paint tableHeaderPaint = new Paint();
        tableHeaderPaint.setColor(0xFF3D2415); // Table header text
        tableHeaderPaint.setTextSize(9f);
        tableHeaderPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint tableBodyPaint = new Paint();
        tableBodyPaint.setColor(0xFF452A1C); // neutrals 800 body text
        tableBodyPaint.setTextSize(8.5f);
        tableBodyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        Paint statusActivePaint = new Paint();
        statusActivePaint.setColor(0xFF10B981); // Emerald active green
        statusActivePaint.setTextSize(8.5f);
        statusActivePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint statusExpiredPaint = new Paint();
        statusExpiredPaint.setColor(0xFFDC2626); // Red expired
        statusExpiredPaint.setTextSize(8.5f);
        statusExpiredPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint linePaint = new Paint();
        linePaint.setColor(0xFFE5C7A6); // neutrals 300 divider
        linePaint.setStrokeWidth(0.8f);

        Paint primaryLinePaint = new Paint();
        primaryLinePaint.setColor(0xFFF97316); // primary orange accent line
        primaryLinePaint.setStrokeWidth(2f);

        Paint zebraPaint = new Paint();
        zebraPaint.setColor(0xFFFFF8ED); // alternating row cream color

        // Pagination and layout limits
        long[] boundaries = getSelectedMonthBoundaries();
        long monthStart = boundaries[0];
        long monthEnd = boundaries[1];
        long now = System.currentTimeMillis();
        long nowOrMonthStart = (monthStart == 0) ? now : monthStart;

        int currentItemIndex = 0;
        int totalItems = monthFilteredList.size();
        int pageNumber = 1;

        // Statistics computation for the report header
        int totalCount = totalItems;
        int activeCount = 0;
        int expiredCount = 0;
        for (Student s : monthFilteredList) {
            if (isStudentActiveForMonth(s, nowOrMonthStart)) {
                activeCount++;
            } else {
                expiredCount++;
            }
        }

        // Layout constants
        int tableTopYFirstPage = 210;
        int tableTopYOtherPages = 90;
        int rowHeight = 24;
        int rowsPerPageFirst = 22; // Fits 22 rows on first page
        int rowsPerPageOthers = 27; // Fits 27 rows on subsequent pages

        int totalPages = 1;
        if (totalItems > rowsPerPageFirst) {
            totalPages = 1 + (int) Math.ceil((double) (totalItems - rowsPerPageFirst) / rowsPerPageOthers);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String generatedDateStr = dateFormat.format(new Date());

        while (currentItemIndex < totalItems) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // 1. Draw Page Border & Frame
            Paint framePaint = new Paint();
            framePaint.setColor(0xFFFFF8ED); // Soft cream page border background
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setStrokeWidth(8f);
            canvas.drawRect(0, 0, pageWidth, pageHeight, framePaint);

            // 2. Draw Header
            if (pageNumber == 1) {
                // Top Brand Accent Line
                canvas.drawRect(25, 20, pageWidth - 25, 23, primaryLinePaint);

                // Mess Name and Report Title
                canvas.drawText(messName.toUpperCase(), 35, 48, titlePaint);

                String subtitleStr = "SUBSCRIPTION REPORT - ALL TIME";
                if (monthStart != 0) {
                    String monthName = spinnerMonthFilter.getSelectedItem().toString().toUpperCase();
                    String yearStr = spinnerYearFilter.getSelectedItem().toString();
                    subtitleStr = "SUBSCRIPTION REPORT - " + monthName + " " + yearStr;
                }
                canvas.drawText(subtitleStr, 35, 62, subTitlePaint);

                // Meta Info Block (Right-aligned)
                canvas.drawText("Report ID: SUB-" + messId.substring(0, Math.min(messId.length(), 6)).toUpperCase(), pageWidth - 180, 42, metaPaint);
                canvas.drawText("Date: " + generatedDateStr, pageWidth - 180, 54, metaPaint);
                canvas.drawText("Format: Modern Native PDF", pageWidth - 180, 66, metaPaint);

                // Analytics Summary Box (Rounded Rectangle)
                Paint cardBgPaint = new Paint();
                cardBgPaint.setColor(0xFFFFFFFF);
                cardBgPaint.setStyle(Paint.Style.FILL);
                
                Paint cardBorderPaint = new Paint();
                cardBorderPaint.setColor(0xFFE5C7A6);
                cardBorderPaint.setStyle(Paint.Style.STROKE);
                cardBorderPaint.setStrokeWidth(1f);

                canvas.drawRoundRect(35, 85, pageWidth - 35, 145, 12, 12, cardBgPaint);
                canvas.drawRoundRect(35, 85, pageWidth - 35, 145, 12, 12, cardBorderPaint);

                // Draw Stats inside Summary Box
                Paint statsLabelPaint = new Paint();
                statsLabelPaint.setColor(0xFF8D6E52);
                statsLabelPaint.setTextSize(9f);
                statsLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                Paint statsValPaint = new Paint();
                statsValPaint.setColor(0xFF3D2415);
                statsValPaint.setTextSize(16f);
                statsValPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                // Column 1: Total
                canvas.drawText("TOTAL STUDENTS", 60, 108, statsLabelPaint);
                canvas.drawText(String.valueOf(totalCount), 60, 130, statsValPaint);

                // Column 2: Active
                statsValPaint.setColor(0xFF10B981);
                canvas.drawText("ACTIVE", 240, 108, statsLabelPaint);
                canvas.drawText(String.valueOf(activeCount), 240, 130, statsValPaint);

                // Column 3: Expired
                statsValPaint.setColor(0xFFDC2626);
                canvas.drawText("EXPIRED", 410, 108, statsLabelPaint);
                canvas.drawText(String.valueOf(expiredCount), 410, 130, statsValPaint);
            } else {
                // Subsequent page headers
                canvas.drawRect(25, 20, pageWidth - 25, 22, primaryLinePaint);

                String headerStr = messName + " - Subscription Report";
                if (monthStart != 0) {
                    String monthName = spinnerMonthFilter.getSelectedItem().toString();
                    String yearStr = spinnerYearFilter.getSelectedItem().toString();
                    headerStr = messName + " - " + monthName + " " + yearStr + " Report";
                }
                canvas.drawText(headerStr, 35, 38, titlePaint);
                canvas.drawText("Page " + pageNumber + " of " + totalPages, pageWidth - 100, 38, metaPaint);
                canvas.drawLine(25, 48, pageWidth - 25, 48, linePaint);
            }

            // 3. Draw Table Headers
            int tableTopY = (pageNumber == 1) ? tableTopYFirstPage : tableTopYOtherPages;
            canvas.drawRect(35, tableTopY, pageWidth - 35, tableTopY + 20, zebraPaint);
            canvas.drawLine(35, tableTopY, pageWidth - 35, tableTopY, linePaint);
            canvas.drawLine(35, tableTopY + 20, pageWidth - 35, tableTopY + 20, linePaint);

            // Columns positions: Sr(35), Name(65), Type(220), Join Date(310), Expiry Date(400), Remaining Days/Status(490)
            canvas.drawText("SR.", 40, tableTopY + 13, tableHeaderPaint);
            canvas.drawText("STUDENT NAME", 65, tableTopY + 13, tableHeaderPaint);
            canvas.drawText("TYPE", 220, tableTopY + 13, tableHeaderPaint);
            canvas.drawText("JOIN DATE", 310, tableTopY + 13, tableHeaderPaint);
            canvas.drawText("EXPIRY DATE", 400, tableTopY + 13, tableHeaderPaint);
            canvas.drawText("STATUS", 490, tableTopY + 13, tableHeaderPaint);

            // 4. Draw Rows
            int y = tableTopY + 20;
            int limit = (pageNumber == 1) ? rowsPerPageFirst : rowsPerPageOthers;

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            for (int i = 0; i < limit && currentItemIndex < totalItems; i++) {
                Student student = monthFilteredList.get(currentItemIndex);
                int srNo = currentItemIndex + 1;

                // Alternate Row Zebra Painting
                if (i % 2 == 1) {
                    canvas.drawRect(35, y, pageWidth - 35, y + rowHeight, zebraPaint);
                }

                // Draw Row Horizontal Grid Line
                canvas.drawLine(35, y + rowHeight, pageWidth - 35, y + rowHeight, linePaint);

                // Serial No
                canvas.drawText(String.valueOf(srNo), 40, y + 15, tableBodyPaint);

                // Student Name (Truncate if too long)
                String name = student.getName() != null ? student.getName() : "Anonymous";
                if (name.length() > 24) name = name.substring(0, 22) + "..";
                canvas.drawText(name, 65, y + 15, tableBodyPaint);

                // Subscription Type
                String typeStr = student.getSubscriptionType() != null ? student.getSubscriptionType() : "NONE";
                if ("ONE_TIME".equals(typeStr)) typeStr = "ONE TIME";
                canvas.drawText(typeStr, 220, y + 15, tableBodyPaint);

                // Join Date (Earliest transaction timestamp)
                Long joinTimestamp = studentJoinDates.get(student.getUserId());
                String joinDateStr = (joinTimestamp != null && joinTimestamp > 0) ? sdf.format(new Date(joinTimestamp)) : "Pre-joined";
                canvas.drawText(joinDateStr, 310, y + 15, tableBodyPaint);

                // Expiry Date
                long lunchExp = student.getLunchSubscriptionExpiry();
                long dinnerExp = student.getDinnerSubscriptionExpiry();
                long oneTimeExp = student.getOneTimeMealExpiry();
                long subExp = student.getSubscriptionExpiry();
                long maxExp = "ONE_TIME".equals(student.getSubscriptionType())
                        ? (oneTimeExp > 0 ? oneTimeExp : subExp)
                        : Math.max(lunchExp > 0 ? lunchExp : subExp, dinnerExp > 0 ? dinnerExp : subExp);

                String expiryDateStr = maxExp > 0 ? sdf.format(new Date(maxExp)) : "No Sub";
                canvas.drawText(expiryDateStr, 400, y + 15, tableBodyPaint);

                // Status Column
                if (isStudentActiveForMonth(student, nowOrMonthStart)) {
                    if (maxExp > now) {
                        long daysLeft = (maxExp - now) / (1000 * 60 * 60 * 24);
                        String daysStr = daysLeft == 0 ? "Expires today" : daysLeft + " days left";
                        canvas.drawText(daysStr, 490, y + 15, statusActivePaint);
                    } else {
                        canvas.drawText("ACTIVE", 490, y + 15, statusActivePaint);
                    }
                } else if (maxExp > 0) {
                    canvas.drawText("EXPIRED", 490, y + 15, statusExpiredPaint);
                } else {
                    canvas.drawText("NO ACTIVE SUB", 490, y + 15, tableBodyPaint);
                }

                y += rowHeight;
                currentItemIndex++;
            }

            // 5. Draw Footer (Drawn on all pages)
            canvas.drawLine(35, pageHeight - 45, pageWidth - 35, pageHeight - 45, linePaint);
            Paint footerTextPaint = new Paint();
            footerTextPaint.setColor(0xFF8D6E52);
            footerTextPaint.setTextSize(8f);
            footerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

            canvas.drawText("Confidential - Generated via MessApp", 35, pageHeight - 32, footerTextPaint);
            canvas.drawText("Page " + pageNumber + " of " + totalPages, pageWidth - 100, pageHeight - 32, footerTextPaint);

            pdfDocument.finishPage(page);
            pageNumber++;
        }

        // Write to Cache Dir & Public Shared Storage
        savePDFToDisk(pdfDocument);
    }

    private void savePDFToDisk(PdfDocument pdfDocument) {
        String reportTag = "AllTime";
        int monthPos = spinnerMonthFilter.getSelectedItemPosition();
        if (monthPos > 0) {
            String monthName = spinnerMonthFilter.getSelectedItem().toString();
            String yearStr = spinnerYearFilter.getSelectedItem().toString();
            reportTag = monthName + "_" + yearStr;
        }
        String filename = "Subscription_Report_" + reportTag + "_" + System.currentTimeMillis() + ".pdf";

        // Step 1: Write to Cache Directory for quick View/Share capability
        File cacheFile = new File(getCacheDir(), "subscription_report.pdf");
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            pdfDocument.writeTo(fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to cache report PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            pdfDocument.close();
            return;
        }

        // Step 2: Write to Shared public directory (Downloads)
        boolean publicSaved = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Modern scoped storage (API 29+)
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        pdfDocument.writeTo(os);
                        os.close();
                        publicSaved = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Legacy Storage (API < 29)
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File publicFile = new File(downloadsDir, filename);
            try {
                FileOutputStream fos = new FileOutputStream(publicFile);
                pdfDocument.writeTo(fos);
                fos.close();
                publicSaved = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        pdfDocument.close();

        if (publicSaved) {
            Toast.makeText(this, "Report downloaded to Downloads folder: " + filename, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Report saved in temporary directory", Toast.LENGTH_SHORT).show();
        }

        // Step 3: Prompt viewer / share sheet immediately using Cache File FileProvider Uri
        openGeneratedPDF(cacheFile);
    }

    private void openGeneratedPDF(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, "com.example.messapp.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        // Share capability
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Standard System Chooser
        Intent chooserIntent = Intent.createChooser(intent, "Open Subscription Report");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{shareIntent});
        
        try {
            startActivity(chooserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No application found to view PDF reports. Please install a PDF viewer.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * BEAUTIFUL RECYCLER VIEW ADAPTER DESIGN
     */
    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

        private final Context context;
        private final List<Student> studentsList;
        private final Map<String, Long> joinDates;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        public ReportAdapter(Context context, List<Student> studentsList, Map<String, Long> joinDates) {
            this.context = context;
            this.studentsList = studentsList;
            this.joinDates = joinDates;
        }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_subscription_report_row, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            Student student = studentsList.get(position);
            holder.bind(student, joinDates, dateFormat, context);
        }

        @Override
        public int getItemCount() {
            return studentsList.size();
        }

        static class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView textAvatarInitial, textStudentName, textStudentEmail, textStatusPill;
            TextView textJoinDate, textExpiryDate, textSubscriptionType, textRemainingDays;
            View layoutStatusBadge;

            public ReportViewHolder(@NonNull View itemView) {
                super(itemView);
                textAvatarInitial = itemView.findViewById(R.id.text_avatar_initial);
                textStudentName = itemView.findViewById(R.id.text_student_name);
                textStudentEmail = itemView.findViewById(R.id.text_student_email);
                textStatusPill = itemView.findViewById(R.id.text_status_pill);
                textJoinDate = itemView.findViewById(R.id.text_join_date);
                textExpiryDate = itemView.findViewById(R.id.text_expiry_date);
                textSubscriptionType = itemView.findViewById(R.id.text_subscription_type);
                textRemainingDays = itemView.findViewById(R.id.text_remaining_days);
                layoutStatusBadge = itemView.findViewById(R.id.layout_status_badge);
            }

            public void bind(Student student, Map<String, Long> joinDates, SimpleDateFormat dateFormat, Context context) {
                // Name & Initial Avatar
                String name = student.getName() != null && !student.getName().isEmpty() ? student.getName() : "Anonymous";
                textStudentName.setText(name);
                textAvatarInitial.setText(name.substring(0, 1).toUpperCase());

                // Email
                textStudentEmail.setText(student.getEmail());

                // Subscription Type
                String subType = student.getSubscriptionType() != null ? student.getSubscriptionType() : "NONE";
                if ("ONE_TIME".equals(subType)) {
                    subType = "ONE TIME";
                }
                textSubscriptionType.setText(subType);

                // Join Date (Earliest transaction timestamp or fallback "Pre-joined")
                Long joinTs = joinDates.get(student.getUserId());
                if (joinTs != null && joinTs > 0) {
                    textJoinDate.setText(dateFormat.format(new Date(joinTs)));
                } else {
                    textJoinDate.setText("Pre-joined");
                }

                // Expiry calculation
                long now = System.currentTimeMillis();
                long lunchExp = student.getLunchSubscriptionExpiry();
                long dinnerExp = student.getDinnerSubscriptionExpiry();
                long oneTimeExp = student.getOneTimeMealExpiry();
                long subExp = student.getSubscriptionExpiry();

                long maxExp = "ONE_TIME".equals(student.getSubscriptionType())
                        ? (oneTimeExp > 0 ? oneTimeExp : subExp)
                        : Math.max(lunchExp > 0 ? lunchExp : subExp, dinnerExp > 0 ? dinnerExp : subExp);

                if (maxExp > 0) {
                    textExpiryDate.setText(dateFormat.format(new Date(maxExp)));
                } else {
                    textExpiryDate.setText("No Expiry");
                }

                // Remaining days & status pill colors
                if (maxExp > now) {
                    long daysLeft = (maxExp - now) / (1000 * 60 * 60 * 24);
                    textStatusPill.setText("ACTIVE");
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_success);
                    
                    textRemainingDays.setTextColor(context.getResources().getColor(R.color.state_success));
                    if (daysLeft == 0) {
                        textRemainingDays.setText("Expires today");
                    } else {
                        textRemainingDays.setText(daysLeft + " days left");
                    }
                } else if (maxExp > 0) {
                    textStatusPill.setText("EXPIRED");
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_danger);
                    
                    textRemainingDays.setTextColor(context.getResources().getColor(R.color.state_error));
                    textRemainingDays.setText("Expired");
                } else {
                    textStatusPill.setText("NO SUB");
                    layoutStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_neutral);
                    
                    textRemainingDays.setTextColor(context.getResources().getColor(R.color.text_caption));
                    textRemainingDays.setText("No Subscription");
                }
            }
        }
    }
}
