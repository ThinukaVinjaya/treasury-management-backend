package com.batch.treasury_management.service;

import com.batch.treasury_management.dto.*;
import com.batch.treasury_management.entity.Contribution;
import com.batch.treasury_management.entity.User;
import com.batch.treasury_management.exceptions.ResourceNotFoundException;
import com.batch.treasury_management.repository.ContributionRepository;
import com.batch.treasury_management.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionService transactionService;
    private final ContributionService contributionService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ContributionRepository contributionRepository;

    // ===================== MONTHLY MAIN FUND REPORT =====================
    public byte[] generateMonthlyMainFundReport(YearMonth month) {
        MonthlyFundReportDTO report = buildMonthlyReportData(month);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("🏛️ Main Batch Fund Monthly Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Month: " + month, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Generated on: " + new java.util.Date()));
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            addTableRow(summaryTable, "Monthly Income", report.getMonthlyIncome().toString());
            addTableRow(summaryTable, "Monthly Expense", report.getMonthlyExpense().toString());
            addTableRow(summaryTable, "Monthly Contributions", report.getMonthlyContributions().toString());
            addTableRow(summaryTable, "Net Balance", report.getNetBalance().toString());
            document.add(summaryTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("✅ PAID MEMBERS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(createPaidTable(report.getPaidContributions()));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("⏳ UNPAID MEMBERS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(createUnpaidTable(report.getUnpaidUsers()));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate monthly fund report", e);
        }

        return out.toByteArray();
    }

    // ===================== PIVOT CONTRIBUTION REPORT (Months as Columns) =====================
    public byte[] generateAllUsersContributionReport(YearMonth startMonth, YearMonth endMonth, String userId) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            String titleText = (userId != null && !userId.trim().isEmpty())
                    ? "USER CONTRIBUTION STATUS REPORT" : "ALL USERS CONTRIBUTION STATUS REPORT";

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("📊 " + titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph period = new Paragraph("Period: " + startMonth + " to " + endMonth,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            period.setAlignment(Element.ALIGN_CENTER);
            document.add(period);
            document.add(new Paragraph("Generated on: " + new java.util.Date()));
            document.add(new Paragraph(" "));

            // Generate months list
            List<YearMonth> months = new ArrayList<>();
            YearMonth current = startMonth;
            while (!current.isAfter(endMonth)) {
                months.add(current);
                current = current.plusMonths(1);
            }

            // Dynamic columns: Username + FullName + Months
            int numColumns = 2 + months.size();
            PdfPTable table = new PdfPTable(numColumns);
            table.setWidthPercentage(100);

            float[] widths = new float[numColumns];
            widths[0] = 25f;
            widths[1] = 30f;
            for (int i = 2; i < numColumns; i++) widths[i] = 15f;
            table.setWidths(widths);

            // Header Row
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            table.addCell(new Phrase("Username", headerFont));
            table.addCell(new Phrase("Full Name", headerFont));

            for (YearMonth m : months) {
                table.addCell(new Phrase(m.toString(), headerFont));
            }

            // Data Rows
            List<User> users = getUsersForReport(userId);
            Font redFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.RED);
            Font greenFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GREEN);

            for (User user : users) {
                table.addCell(user.getUsername() != null ? user.getUsername() : "");
                table.addCell(user.getFullName() != null ? user.getFullName() : "");

                for (YearMonth month : months) {
                    addMonthStatusCell(table, user, month, redFont, greenFont);
                }
            }

            document.add(table);

            // Summary
            document.add(new Paragraph(" "));
            Paragraph summary = new Paragraph("Report Generated Successfully",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
            document.add(summary);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate contribution period report: " + e.getMessage(), e);
        }
    }

    private void addMonthStatusCell(PdfPTable table, User user, YearMonth month, Font redFont, Font greenFont) {
        try {
            Optional<Contribution> opt = contributionRepository
                    .findByUserIdAndMonthAndEventIdIsNull(user.getId(), month);

            if (opt.isPresent()) {
                Contribution c = opt.get();
                if (Boolean.TRUE.equals(c.isPaid())) {
                    table.addCell(new Phrase("PAID", greenFont));
                } else {
                    table.addCell(new Phrase("UNPAID", redFont));
                }
            } else {
                // NOT GENERATED → Show as UNPAID (as per your request)
                table.addCell(new Phrase("UNPAID", redFont));
            }
        } catch (Exception e) {
            table.addCell(new Phrase("UNPAID", redFont));
        }
    }

    private List<User> getUsersForReport(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            return List.of(user);
        }

        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> !u.isDeleted())
                .sorted(Comparator.comparing(User::getFullName))
                .collect(Collectors.toList());
    }

    // ===================== JSON DETAILS FOR SPECIFIC USER =====================
    public List<UserContributionDetailDTO> getUserContributionDetails(
            String userId, YearMonth startMonth, YearMonth endMonth) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<UserContributionDetailDTO> details = new ArrayList<>();

        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            UserContributionDetailDTO dto = new UserContributionDetailDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setFullName(user.getFullName());
            dto.setMonth(current);

            Optional<Contribution> opt = contributionRepository
                    .findByUserIdAndMonthAndEventIdIsNull(user.getId(), current);

            if (opt.isPresent()) {
                Contribution c = opt.get();
                dto.setAmount(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
                dto.setPaid(c.isPaid());
                dto.setStatus(c.isPaid() ? "PAID" : "UNPAID");
            } else {
                dto.setAmount(BigDecimal.ZERO);
                dto.setPaid(false);
                dto.setStatus("NOT GENERATED");
            }

            details.add(dto);
            current = current.plusMonths(1);
        }
        return details;
    }

    // ===================== HELPER METHODS =====================
    private MonthlyFundReportDTO buildMonthlyReportData(YearMonth month) {
        MonthlyFundReportDTO report = new MonthlyFundReportDTO();
        report.setMonth(month);

        List<TransactionResponse> transactions = transactionService.getMainFundTransactionsByMonth(month);

        BigDecimal income = transactions.stream()
                .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = transactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        report.setMonthlyIncome(income);
        report.setMonthlyExpense(expense);
        report.setNetBalance(income.subtract(expense));

        List<ContributionResponse> paid = contributionService.getPaidContributionsByMonth(month);
        report.setPaidContributions(paid);
        report.setMonthlyContributions(paid.stream()
                .map(ContributionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        report.setUnpaidUsers(userService.getUnpaidUsersForMonth(month));

        return report;
    }

    private PdfPTable createPaidTable(List<ContributionResponse> paidList) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(new Phrase("Username", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(new Phrase("Full Name", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(new Phrase("Month", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(new Phrase("Amount", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

        for (ContributionResponse c : paidList) {
            Optional<User> userOpt = userRepository.findById(c.getUserId());
            String username = userOpt.map(User::getUsername).orElse("Unknown");
            String fullName = userOpt.map(User::getFullName).orElse("Unknown");

            table.addCell(username);
            table.addCell(fullName);
            table.addCell(c.getMonth().toString());
            table.addCell(c.getAmount() != null ? c.getAmount().toString() : "0");
        }
        return table;
    }

    private PdfPTable createUnpaidTable(List<UserResponse> unpaidUsers) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.addCell(new Phrase("Username", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(new Phrase("Full Name", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(new Phrase("Amount", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

        for (UserResponse user : unpaidUsers) {
            table.addCell(user.getUsername());
            table.addCell(user.getFullName());
            table.addCell("0");
        }
        return table;
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        table.addCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        table.addCell(value);
    }
}