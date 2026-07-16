package com.smartpharma.config;

import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer {

    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initTestData() {
        return args -> {
            if (pharmacyRepository.findByLicenseNumber("PH-2024-001").isPresent() ||
                    pharmacyRepository.findByEmail("test@smartpharma.eg").isPresent()) {
                return;
            }

            Pharmacy pharmacy = Pharmacy.builder()
                    .name("Al Shifa Model Pharmacy")
                    .licenseNumber("PH-2024-001")
                    .email("test@smartpharma.eg")
                    .phone("01012345678")
                    .address("Cairo, Nasr City, Al Tayaran Street")
                    .subscriptionStatus(Pharmacy.SubscriptionStatus.ACTIVE)
                    .planType(Pharmacy.PlanType.PROFESSIONAL)
                    .build();

            pharmacyRepository.save(pharmacy);
            User admin = User.builder()
                    .pharmacy(pharmacy)
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Ahmed Mohamed")
                    .phone("01012345678")
                    .role(User.UserRole.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);

            User pharmacist = User.builder()
                    .pharmacy(pharmacy)
                    .username("pharmacist")
                    .password(passwordEncoder.encode("pharm123"))
                    .fullName("Mohamed Ali")
                    .phone("01098765432")
                    .role(User.UserRole.PHARMACIST)
                    .isActive(true)
                    .build();

            userRepository.save(pharmacist);
            String[][] productsData = {
                    {"Panadol Extra", "Paracetamol + Caffeine", "1234567890123", "Painkillers", "BOX"},
                    {"Augmentin 1g", "Amoxicillin + Clavulanic Acid", "1234567890124", "Antibiotics", "BOX"},
                    {"Concor 5mg", "Bisoprolol", "1234567890125", "Cardiovascular", "BOX"},
                    {"Omeprazole 20mg", "Omeprazole", "1234567890126", "Stomach", "BOX"},
                    {"Voltaren 50mg", "Diclofenac", "1234567890127", "Painkillers", "BOX"},
                    {"Augmentin 625mg", "Amoxicillin + Clavulanic Acid", "1234567890128", "Antibiotics", "BOX"},
                    {"Panadol Children", "Paracetamol Suspension", "1234567890129", "Children Painkillers", "BOTTLE"},
                    {"Brufen 400mg", "Ibuprofen", "1234567890130", "Painkillers", "BOX"},
                    {"Zyrtec 10mg", "Cetirizine", "1234567890131", "Allergy", "BOX"},
                    {"Amoxil 500mg", "Amoxicillin", "1234567890132", "Antibiotics", "BOX"}
            };

            for (String[] prodData : productsData) {
                Product product = Product.builder()
                        .pharmacy(pharmacy)
                        .name(prodData[0])
                        .scientificName(prodData[1])
                        .barcode(prodData[2])
                        .category(prodData[3])
                        .unitType(prodData[4])
                        .minStockLevel(10)
                        .prescriptionRequired(prodData[3].contains("Antibiotics") || prodData[3].contains("Cardiovascular"))
                        .sellPrice(new BigDecimal("25.00"))
                        .buyPrice(new BigDecimal("15.00"))
                        .build();

                productRepository.save(product);

                StockBatch batch = StockBatch.builder()
                        .product(product)
                        .pharmacy(pharmacy)
                        .batchNumber("BATCH-" + product.getId())
                        .quantityInitial(50)
                        .quantityCurrent(50)
                        .expiryDate(LocalDate.now().plusMonths(18))
                        .buyPrice(new BigDecimal("15.00"))
                        .sellPrice(new BigDecimal("25.00"))
                        .location("Shelf-" + (product.getId() % 5 + 1))
                        .status(StockBatch.BatchStatus.ACTIVE)
                        .build();

                stockBatchRepository.save(batch);
            }
        };
    }
}