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
            // ✅ التحقق من وجود الصيدلية بالترخيص أو الإيميل (مش مجرد count)
            if (pharmacyRepository.findByLicenseNumber("PH-2024-001").isPresent() ||
                    pharmacyRepository.findByEmail("test@smartpharma.eg").isPresent()) {
                System.out.println("✅ Test data already exists, skipping initialization...");
                return;
            }

            System.out.println("🚀 Initializing test data...");

            // 1. إنشاء الصيدلية
            Pharmacy pharmacy = Pharmacy.builder()
                    .name("صيدلية الشفاء النموذجية")
                    .licenseNumber("PH-2024-001")
                    .email("test@smartpharma.eg")
                    .phone("01012345678")
                    .address("القاهرة، مدينة نصر، شارع الطيران")
                    .subscriptionStatus(Pharmacy.SubscriptionStatus.ACTIVE)
                    .planType(Pharmacy.PlanType.PROFESSIONAL)
                    .build();

            pharmacyRepository.save(pharmacy);
            System.out.println("✅ Pharmacy created: " + pharmacy.getName());

            // 2. إنشاء مدير النظام (Admin)
            User admin = User.builder()
                    .pharmacy(pharmacy)
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("أحمد محمد")
                    .phone("01012345678")
                    .role(User.UserRole.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
            System.out.println("✅ Admin user created: admin / admin123");

            // 3. إنشاء صيدلي
            User pharmacist = User.builder()
                    .pharmacy(pharmacy)
                    .username("pharmacist")
                    .password(passwordEncoder.encode("pharm123"))
                    .fullName("محمد علي")
                    .phone("01098765432")
                    .role(User.UserRole.PHARMACIST)
                    .isActive(true)
                    .build();

            userRepository.save(pharmacist);
            System.out.println("✅ Pharmacist user created: pharmacist / pharm123");

            // 4. إنشاء منتجات تجريبية
            String[][] productsData = {
                    {"بنادول إكسترا", "Paracetamol + Caffeine", "1234567890123", "مسكنات", "BOX"},
                    {"أوجمنت 1 جم", "Amoxicillin + Clavulanic Acid", "1234567890124", "مضادات حيوية", "BOX"},
                    {"كونكور 5 مجم", "Bisoprolol", "1234567890125", "قلب وأوعية", "BOX"},
                    {"أوميبرازول 20 مجم", "Omeprazole", "1234567890126", "معدة", "BOX"},
                    {"فولتارين 50 مجم", "Diclofenac", "1234567890127", "مسكنات", "BOX"},
                    {"أوجمنت 625 مجم", "Amoxicillin + Clavulanic Acid", "1234567890128", "مضادات حيوية", "BOX"},
                    {"بانادول أطفال", "Paracetamol Suspension", "1234567890129", "مسكنات أطفال", "BOTTLE"},
                    {"بروفين 400 مجم", "Ibuprofen", "1234567890130", "مسكنات", "BOX"},
                    {"زيرتك 10 مجم", "Cetirizine", "1234567890131", "حساسية", "BOX"},
                    {"أموكسيل 500 مجم", "Amoxicillin", "1234567890132", "مضادات حيوية", "BOX"}
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
                        .prescriptionRequired(prodData[3].contains("مضادات") || prodData[3].contains("قلب"))
                        .sellPrice(new BigDecimal("25.00"))
                        .buyPrice(new BigDecimal("15.00"))
                        .build();

                productRepository.save(product);

                // إنشاء دفعة مخزون لكل منتج
                StockBatch batch = StockBatch.builder()
                        .product(product)
                        .pharmacy(pharmacy)
                        .batchNumber("BATCH-" + product.getId())
                        .quantityInitial(50)
                        .quantityCurrent(50)
                        .expiryDate(LocalDate.now().plusMonths(18))
                        .buyPrice(new BigDecimal("15.00"))
                        .sellPrice(new BigDecimal("25.00"))
                        .location("رف-" + (product.getId() % 5 + 1))
                        .status(StockBatch.BatchStatus.ACTIVE)
                        .build();

                stockBatchRepository.save(batch);
            }

            System.out.println("✅ " + productsData.length + " Products created with stock batches");
            System.out.println("🎉 Test data initialization completed!");
            System.out.println("\n📋 Login Credentials:");
            System.out.println("   Admin: admin / admin123");
            System.out.println("   Pharmacist: pharmacist / pharm123");
        };
    }
}