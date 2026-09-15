package lk.ijse.etechbackend.config;

import lk.ijse.etechbackend.entity.*;
import lk.ijse.etechbackend.enumiration.*;
import lk.ijse.etechbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BadgeRepository badgeRepository;
    private final ProductRepository productRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final LegalPolicyRepository legalPolicyRepository;
    private final HotDealRepository hotDealRepository;
    private final HomeDealBannerRepository homeDealBannerRepository;
    private final DealBundleRepository dealBundleRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final NewsletterCampaignRepository newsletterCampaignRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockTransferRepository stockTransferRepository;
    private final ProductReviewRepository productReviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final PolicySectionRepository policySectionRepository;

    @Override
    public void run(String... args) {
        log.info("--- DATA INITIALIZER IS RUNNING! ---");
        seedBranches();
        seedUsers();
        seedCategories();
        seedBrands();
        seedBadges();
        seedProducts();
        seedBusinessProfile();
        seedPolicies();
        seedPromotions();
        seedNewsletterSubscribers();
        seedNewsletterCampaigns();
        seedOrders();
        seedStockTransfers();
        seedProductReviews();
        log.info("--- DATA INITIALIZER FINISHED! ---");
    }

    private void seedBranches() {
        if (branchRepository.count() == 0) {
            log.info("Seeding default branch warehouse hubs...");

            List<Branch> branches = List.of(
                    Branch.builder()
                            .id("BR-COL")
                            .name("Colombo Main Hub")
                            .city("Colombo")
                            .address("450 Galle Road, Colombo 03")
                            .phone("+94 11 234 5678")
                            .email("colombo@etechcomputers.lk")
                            .latitude(new BigDecimal("6.89820000"))
                            .longitude(new BigDecimal("79.85430000"))
                            .baseShippingRate(new BigDecimal("350.00"))
                            .active(true)
                            .build(),
                    Branch.builder()
                            .id("BR-KAN")
                            .name("Kandy Tech Hub")
                            .city("Kandy")
                            .address("12 Peradeniya Road, Kandy")
                            .phone("+94 81 222 3344")
                            .email("kandy@etechcomputers.lk")
                            .latitude(new BigDecimal("7.29060000"))
                            .longitude(new BigDecimal("80.63370000"))
                            .baseShippingRate(new BigDecimal("450.00"))
                            .active(true)
                            .build(),
                    Branch.builder()
                            .id("BR-GAL")
                            .name("Galle Coastal Branch")
                            .city("Galle")
                            .address("88 Main Street, Galle Fort")
                            .phone("+94 91 223 4455")
                            .email("galle@etechcomputers.lk")
                            .latitude(new BigDecimal("6.05350000"))
                            .longitude(new BigDecimal("80.22100000"))
                            .baseShippingRate(new BigDecimal("450.00"))
                            .active(true)
                            .build(),
                    Branch.builder()
                            .id("BR-MAT")
                            .name("Matara Express Center")
                            .city("Matara")
                            .address("34 Anagarika Dharmapala Mawatha, Matara")
                            .phone("+94 41 224 5566")
                            .email("matara@etechcomputers.lk")
                            .latitude(new BigDecimal("5.95490000"))
                            .longitude(new BigDecimal("80.55500000"))
                            .baseShippingRate(new BigDecimal("500.00"))
                            .active(true)
                            .build()
            );

            branchRepository.saveAll(branches);
            log.info("Successfully seeded {} branch warehouse hubs", branches.size());
        }
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("superadmin")) {
            log.info("Seeding Super Admin root account...");
            User superAdmin = User.builder()
                    .name("System Owner & Super Admin")
                    .username("superadmin")
                    .email("superadmin@etech.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(UserRole.SUPERADMIN)
                    .assignedBranch(null)
                    .build();
            userRepository.save(superAdmin);
        }

        if (!userRepository.existsByUsername("admin")) {
            log.info("Seeding Store Administrator account...");
            User admin = User.builder()
                    .name("Store Administrator")
                    .username("admin")
                    .email("admin@etech.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(UserRole.ADMIN)
                    .assignedBranch(null)
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("staff_colombo")) {
            log.info("Seeding Branch Staff account...");
            Branch colomboBranch = branchRepository.findById("BR-COL").orElse(null);
            User staff = User.builder()
                    .name("Colombo Branch Operations")
                    .username("staff_colombo")
                    .email("staff.colombo@etech.com")
                    .passwordHash(passwordEncoder.encode("staff123"))
                    .role(UserRole.STAFF)
                    .assignedBranch(colomboBranch)
                    .build();
            userRepository.save(staff);
        }

        if (!userRepository.existsByUsername("kasun")) {
            log.info("Seeding Customer account...");
            User customer = User.builder()
                    .name("Kasun Perera")
                    .username("kasun")
                    .email("kasun.p@gmail.com")
                    .passwordHash(passwordEncoder.encode("customer123"))
                    .role(UserRole.CUSTOMER)
                    .assignedBranch(null)
                    .build();
            userRepository.save(customer);
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            log.info("Seeding catalog categories with supercategory hierarchy...");

            // 1. Top-Level Main Categories (Super Categories)
            Category catSystems = Category.builder()
                    .id("cat-systems")
                    .name("Computers & Systems")
                    .slug("systems")
                    .icon("💻")
                    .description("Gaming laptops, mobile workstations, custom desktop rigs, and all-in-one PCs")
                    .featured(true)
                    .displayOrder(1)
                    .superCategory(null)
                    .build();

            Category catComponents = Category.builder()
                    .id("cat-components")
                    .name("PC Components")
                    .slug("components")
                    .icon("⚙️")
                    .description("Processors, graphics cards, motherboards, RAM, power supplies, and cases")
                    .featured(true)
                    .displayOrder(2)
                    .superCategory(null)
                    .build();

            Category catPeripherals = Category.builder()
                    .id("cat-peripherals")
                    .name("Peripherals & Accessories")
                    .slug("peripherals")
                    .icon("🖱️")
                    .description("Gaming mice, mechanical keyboards, audio headsets, and streaming gear")
                    .featured(true)
                    .displayOrder(3)
                    .superCategory(null)
                    .build();

            categoryRepository.saveAll(List.of(catSystems, catComponents, catPeripherals));

            // 2. Subcategories with Parent Supercategories
            Category catLaptops = Category.builder()
                    .id("cat-laptops")
                    .name("Laptops & Notebooks")
                    .slug("laptops")
                    .icon("💻")
                    .description("High-performance gaming laptops, ultrabooks, and professional creator workstations")
                    .featured(true)
                    .displayOrder(1)
                    .superCategory(catSystems)
                    .build();

            Category catDesktops = Category.builder()
                    .id("cat-desktops")
                    .name("Custom Gaming Desktops")
                    .slug("desktops")
                    .icon("🖥️")
                    .description("Handcrafted enthusiast desktop rigs, liquid-cooled powerhouses, and workstations")
                    .featured(true)
                    .displayOrder(2)
                    .superCategory(catSystems)
                    .build();

            Category catStorage = Category.builder()
                    .id("cat-storage")
                    .name("Storage & Memory")
                    .slug("storage")
                    .icon("💾")
                    .description("Gen4/Gen5 NVMe SSDs, high-capacity HDDs, and DDR5 RAM kits")
                    .featured(false)
                    .displayOrder(1)
                    .superCategory(catComponents)
                    .build();

            Category catHardware = Category.builder()
                    .id("cat-hardware")
                    .name("Core PC Parts")
                    .slug("hardware")
                    .icon("🔧")
                    .description("CPUs, enthusiast GPUs, thermal pastes, and motherboards")
                    .featured(true)
                    .displayOrder(2)
                    .superCategory(catComponents)
                    .build();

            Category catMonitors = Category.builder()
                    .id("cat-monitors")
                    .name("Monitors & Displays")
                    .slug("monitors")
                    .icon("🖥️")
                    .description("High refresh rate gaming monitors, 4K OLED displays, and ultrawide panels")
                    .featured(true)
                    .displayOrder(1)
                    .superCategory(catPeripherals)
                    .build();

            Category catKeyboards = Category.builder()
                    .id("cat-keyboards")
                    .name("Keyboards & Mice")
                    .slug("keyboards")
                    .icon("⌨️")
                    .description("Mechanical keyboards, analog switches, and ultra-lightweight esports mice")
                    .featured(true)
                    .displayOrder(2)
                    .superCategory(catPeripherals)
                    .build();

            Category catNetworking = Category.builder()
                    .id("cat-networking")
                    .name("Networking Gear")
                    .slug("networking")
                    .icon("🌐")
                    .description("Wi-Fi 7 gaming routers, mesh network systems, and Gigabit switches")
                    .featured(false)
                    .displayOrder(3)
                    .superCategory(catPeripherals)
                    .build();

            categoryRepository.saveAll(List.of(catLaptops, catDesktops, catStorage, catHardware, catMonitors, catKeyboards, catNetworking));
            log.info("Successfully seeded catalog categories with supercategory hierarchy!");
        }
    }

    private void seedBrands() {
        if (brandRepository.count() == 0) {
            log.info("Seeding official manufacturer partner brands...");

            List<Brand> brands = List.of(
                    Brand.builder()
                            .id("brd-asus")
                            .name("ASUS")
                            .slug("asus")
                            .logoUrl("https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=200&auto=format&fit=crop&q=80")
                            .country("Taiwan")
                            .foundedYear("1989")
                            .websiteUrl("https://www.asus.com")
                            .tagline("In Search of Incredible")
                            .description("Leading provider of ROG gaming hardware, laptops, motherboards, and displays.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(1)
                            .build(),
                    Brand.builder()
                            .id("brd-msi")
                            .name("MSI")
                            .slug("msi")
                            .logoUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=200&auto=format&fit=crop&q=80")
                            .country("Taiwan")
                            .foundedYear("1986")
                            .websiteUrl("https://www.msi.com")
                            .tagline("True Gaming")
                            .description("World leader in AI PCs, gaming laptops, graphics cards, and enthusiast components.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(2)
                            .build(),
                    Brand.builder()
                            .id("brd-corsair")
                            .name("Corsair")
                            .slug("corsair")
                            .logoUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("1994")
                            .websiteUrl("https://www.corsair.com")
                            .tagline("Game On")
                            .description("High-performance gaming gear, liquid cooling, power supplies, and iCUE ecosystem.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(3)
                            .build(),
                    Brand.builder()
                            .id("brd-intel")
                            .name("Intel")
                            .slug("intel")
                            .logoUrl("https://images.unsplash.com/photo-1518770660439-4636190af475?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("1968")
                            .websiteUrl("https://www.intel.com")
                            .tagline("Do More")
                            .description("Cutting-edge Core Ultra processors and advanced semiconductor innovation.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(4)
                            .build(),
                    Brand.builder()
                            .id("brd-logitech")
                            .name("Logitech")
                            .slug("logitech")
                            .logoUrl("https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=200&auto=format&fit=crop&q=80")
                            .country("Switzerland")
                            .foundedYear("1981")
                            .websiteUrl("https://www.logitechg.com")
                            .tagline("Defy Logic")
                            .description("Industry standard in esports mice, mechanical keyboards, and LIGHTSPEED technology.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(5)
                            .build(),
                    Brand.builder()
                            .id("brd-razer")
                            .name("Razer")
                            .slug("razer")
                            .logoUrl("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=200&auto=format&fit=crop&q=80")
                            .country("USA / Singapore")
                            .foundedYear("2005")
                            .websiteUrl("https://www.razer.com")
                            .tagline("For Gamers. By Gamers.")
                            .description("Global lifestyle brand for gamers featuring Chroma RGB and precision analog switches.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(6)
                            .build(),
                    Brand.builder()
                            .id("brd-nvidia")
                            .name("NVIDIA")
                            .slug("nvidia")
                            .logoUrl("https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("1993")
                            .websiteUrl("https://www.nvidia.com")
                            .tagline("The Way It's Meant to be Played")
                            .description("World pioneer of GPUs, real-time ray tracing, and DLSS AI neural rendering.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(7)
                            .build(),
                    Brand.builder()
                            .id("brd-amd")
                            .name("AMD")
                            .slug("amd")
                            .logoUrl("https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("1969")
                            .websiteUrl("https://www.amd.com")
                            .tagline("Together We Advance")
                            .description("Innovator in Ryzen 3D V-Cache processors and Radeon RDNA graphics architecture.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(8)
                            .build(),
                    Brand.builder()
                            .id("brd-samsung")
                            .name("Samsung")
                            .slug("samsung")
                            .logoUrl("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=200&auto=format&fit=crop&q=80")
                            .country("South Korea")
                            .foundedYear("1938")
                            .websiteUrl("https://www.samsung.com")
                            .tagline("Inspire the World, Create the Future")
                            .description("World leader in V-NAND NVMe solid-state drives, DDR5 memory, and QD-OLED displays.")
                            .featured(true)
                            .status(Status.ACTIVE)
                            .displayOrder(9)
                            .build(),
                    Brand.builder()
                            .id("brd-gigabyte")
                            .name("Gigabyte")
                            .slug("gigabyte")
                            .logoUrl("https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=200&auto=format&fit=crop&q=80")
                            .country("Taiwan")
                            .foundedYear("1986")
                            .websiteUrl("https://www.gigabyte.com")
                            .tagline("Upgrade Your Life")
                            .description("Premier manufacturer of AORUS gaming motherboards, GPUs, and high-spec systems.")
                            .featured(false)
                            .status(Status.ACTIVE)
                            .displayOrder(10)
                            .build(),
                    Brand.builder()
                            .id("brd-apple")
                            .name("Apple")
                            .slug("apple")
                            .logoUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("1976")
                            .websiteUrl("https://www.apple.com")
                            .tagline("Think Different")
                            .description("Creator of revolutionary M-series silicon Macs, Retina displays, and creative workflows.")
                            .featured(false)
                            .status(Status.ACTIVE)
                            .displayOrder(11)
                            .build(),
                    Brand.builder()
                            .id("brd-hyperx")
                            .name("HyperX")
                            .slug("hyperx")
                            .logoUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("2002")
                            .websiteUrl("https://www.hyperx.com")
                            .tagline("We're All Gamers")
                            .description("Industry renowned Cloud gaming headsets, QuadCast microphones, and memory solutions.")
                            .featured(false)
                            .status(Status.ACTIVE)
                            .displayOrder(12)
                            .build(),
                    Brand.builder()
                            .id("brd-lianli")
                            .name("Lian Li")
                            .slug("lianli")
                            .logoUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=200&auto=format&fit=crop&q=80")
                            .country("Taiwan")
                            .foundedYear("1983")
                            .websiteUrl("https://www.lian-li.com")
                            .tagline("Feel the Difference")
                            .description("Master craftsmanship in brushed aluminum PC cases, UNI FAN modular cooling, and streamers.")
                            .featured(false)
                            .status(Status.ACTIVE)
                            .displayOrder(13)
                            .build(),
                    Brand.builder()
                            .id("brd-nzxt")
                            .name("NZXT")
                            .slug("nzxt")
                            .logoUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=200&auto=format&fit=crop&q=80")
                            .country("USA")
                            .foundedYear("2004")
                            .websiteUrl("https://www.nzxt.com")
                            .tagline("Make Building Extraordinary")
                            .description("Distinctive minimalist chassis designs, Kraken AIO liquid coolers, and CAM control software.")
                            .featured(false)
                            .status(Status.ACTIVE)
                            .displayOrder(14)
                            .build()
            );

            brandRepository.saveAll(brands);
            log.info("Successfully seeded {} manufacturer partner brands", brands.size());
        }
    }

    private void seedBadges() {
        if (badgeRepository.count() == 0) {
            log.info("Seeding storefront product badges and automated rules...");

            List<Badge> badges = List.of(
                    Badge.builder()
                            .id("bdg-hotdeal")
                            .name("Hot Deal")
                            .slug("hotdeal")
                            .colorKey("rose")
                            .colorHex("#e11d48")
                            .purpose("Active promotional discount campaign")
                            .standardDescription("Discounted hardware with active countdown timer")
                            .ruleType(BadgeRuleType.system)
                            .criteria("promo_active")
                            .priority(1)
                            .isSystemDefault(true)
                            .canEdit(false)
                            .canDelete(false)
                            .status(Status.ACTIVE)
                            .build(),
                    Badge.builder()
                            .id("bdg-bestseller")
                            .name("Bestseller")
                            .slug("bestseller")
                            .colorKey("amber")
                            .colorHex("#d97706")
                            .purpose("High sales volume leader")
                            .standardDescription("Top selling product based on customer order volume")
                            .ruleType(BadgeRuleType.automatic)
                            .criteria("sales_gt_50")
                            .priority(2)
                            .isSystemDefault(true)
                            .canEdit(true)
                            .canDelete(false)
                            .status(Status.ACTIVE)
                            .build(),
                    Badge.builder()
                            .id("bdg-toprated")
                            .name("Top Rated")
                            .slug("toprated")
                            .colorKey("emerald")
                            .colorHex("#059669")
                            .purpose("Customer rating average >= 4.8")
                            .standardDescription("Highly rated product with excellent user feedback")
                            .ruleType(BadgeRuleType.automatic)
                            .criteria("rating_gte_4.8")
                            .priority(3)
                            .isSystemDefault(true)
                            .canEdit(true)
                            .canDelete(false)
                            .status(Status.ACTIVE)
                            .build(),
                    Badge.builder()
                            .id("bdg-new")
                            .name("New Arrival")
                            .slug("new")
                            .colorKey("sky")
                            .colorHex("#0284c7")
                            .purpose("Recently added hardware")
                            .standardDescription("Latest generation hardware catalog addition")
                            .ruleType(BadgeRuleType.manual)
                            .criteria("created_within_30d")
                            .priority(4)
                            .isSystemDefault(true)
                            .canEdit(true)
                            .canDelete(false)
                            .status(Status.ACTIVE)
                            .build()
            );

            badgeRepository.saveAll(badges);
            log.info("Successfully seeded {} badges", badges.size());
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            log.info("Seeding catalog products with specs, gallery, and branch warehouse inventory...");

            Branch colombo = branchRepository.findById("BR-COL").orElse(null);
            Branch galle = branchRepository.findById("BR-GAL").orElse(null);
            Branch matara = branchRepository.findById("BR-MAT").orElse(null);
            Branch kandy = branchRepository.findById("BR-KAN").orElse(null);

            // 1. ROG Strix SCAR 18
            Product p1 = Product.builder()
                    .name("ROG Strix SCAR 18 (2026)")
                    .category(categoryRepository.findBySlug("laptops").orElse(null))
                    .brand(brandRepository.findBySlug("asus").orElse(null))
                    .badge(badgeRepository.findBySlug("toprated").orElse(null))
                    .price(new BigDecimal("849999.00"))
                    .originalPrice(new BigDecimal("899999.00"))
                    .rating(new BigDecimal("4.9"))
                    .reviewsCount(48)
                    .description("Flagship 18-inch Mini-LED gaming laptop powered by Intel Core Ultra 9 & NVIDIA RTX 4090.")
                    .fullDescription("Dominate Windows 11 gaming with the 2026 ROG Strix SCAR 18. Equipped with an Intel Core Ultra 9 185H processor, NVIDIA GeForce RTX 4090 Laptop GPU with 175W max TGP, 64GB DDR5 memory, and lightning-fast 4TB PCIe 4.0 NVMe RAID 0 storage. Features Conductonaut Extreme liquid metal and Tri-Fan Technology.")
                    .sku("ETC-LAP-001")
                    .warranty("3-Year Official Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(3)
                    .build();

            p1.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());
            p1.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=800&auto=format&fit=crop&q=80").displayOrder(1).build());
            p1.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?w=800&auto=format&fit=crop&q=80").displayOrder(2).build());

            p1.addSpec(Specs.builder().name("Processor").description("Intel Core Ultra 9 185H (24 Cores, up to 5.8GHz)").build());
            p1.addSpec(Specs.builder().name("Graphics").description("NVIDIA GeForce RTX 4090 16GB GDDR6 (175W)").build());
            p1.addSpec(Specs.builder().name("Memory").description("64GB DDR5 5600MHz Dual-Channel").build());
            p1.addSpec(Specs.builder().name("Storage").description("4TB NVMe PCIe 4.0 SSD (2TB x 2 RAID 0)").build());
            p1.addSpec(Specs.builder().name("Display").description("18.0\" QHD+ (2560x1600) 240Hz Mini-LED HDR 1100").build());

            p1.addFeature(Features.builder().featureName("Conductonaut Extreme Liquid Metal on CPU & GPU").build());
            p1.addFeature(Features.builder().featureName("Tri-Fan Cooling with Anti-Dust Technology").build());

            if (colombo != null) p1.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(6).build());
            if (galle != null) p1.addBranchInventory(BranchInventory.builder().branch(galle).quantity(3).build());
            if (matara != null) p1.addBranchInventory(BranchInventory.builder().branch(matara).quantity(2).build());
            if (kandy != null) p1.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(2).build());

            // 2. MSI Titan 18 HX
            Product p2 = Product.builder()
                    .name("MSI Titan 18 HX Dragon Edition")
                    .category(categoryRepository.findBySlug("laptops").orElse(null))
                    .brand(brandRepository.findBySlug("msi").orElse(null))
                    .badge(badgeRepository.findBySlug("toprated").orElse(null))
                    .price(new BigDecimal("799999.00"))
                    .originalPrice(new BigDecimal("849999.00"))
                    .rating(new BigDecimal("4.8"))
                    .reviewsCount(32)
                    .description("Extreme desktop replacement with mechanical Cherry MX keyboard and vapor chamber cooling.")
                    .fullDescription("MSI Titan 18 HX combines extreme desktop-grade computing with unmatched portability. Driven by Intel 14th Gen Core i9-14900HX, RTX 4090 GPU, 18-inch 4K 120Hz Mini-LED display, and world's first seamless RGB haptic touchpad.")
                    .sku("ETC-LAP-002")
                    .warranty("2-Year International Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(2)
                    .build();

            p2.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());
            p2.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1593642634367-d91a135587b5?w=800&auto=format&fit=crop&q=80").displayOrder(1).build());

            p2.addSpec(Specs.builder().name("Processor").description("Intel Core i9-14900HX (24 Cores, 32 Threads, 5.8GHz)").build());
            p2.addSpec(Specs.builder().name("Graphics").description("NVIDIA GeForce RTX 4090 16GB GDDR6 (175W OverBoost)").build());
            p2.addSpec(Specs.builder().name("Memory").description("64GB DDR5 5600MHz (Upgradable to 192GB)").build());
            p2.addSpec(Specs.builder().name("Storage").description("2TB PCIe Gen5 NVMe M.2 SSD").build());
            p2.addSpec(Specs.builder().name("Display").description("18.0\" UHD+ (3840x2400) 120Hz 100% DCI-P3 Mini-LED").build());

            p2.addFeature(Features.builder().featureName("Cherry MX Ultra Low Profile Mechanical Keyboard").build());
            p2.addFeature(Features.builder().featureName("Vapor Chamber Cooler with Dual 3D Blade Fans").build());

            if (colombo != null) p2.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(4).build());
            if (galle != null) p2.addBranchInventory(BranchInventory.builder().branch(galle).quantity(2).build());
            if (kandy != null) p2.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(1).build());

            // 3. ASUS TUF Gaming GeForce RTX 4070 Ti Super
            Product p3 = Product.builder()
                    .name("ASUS TUF Gaming GeForce RTX 4070 Ti SUPER 16GB")
                    .category(categoryRepository.findBySlug("components").orElse(null))
                    .brand(brandRepository.findBySlug("asus").orElse(null))
                    .badge(badgeRepository.findBySlug("hotdeal").orElse(null))
                    .price(new BigDecimal("325000.00"))
                    .originalPrice(new BigDecimal("345000.00"))
                    .rating(new BigDecimal("4.9"))
                    .reviewsCount(85)
                    .description("Military-grade durability, dual ball fan bearings, and robust heatsink for 4K ray tracing.")
                    .fullDescription("Built with auto-extreme automated manufacturing, TUF capacitors rated for 20,000 hours at 105C, and axial-tech fans scaled up for 21% more airflow.")
                    .sku("ETC-GPU-001")
                    .warranty("3-Year Replacement Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(5)
                    .build();

            p3.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p3.addSpec(Specs.builder().name("Architecture").description("Ada Lovelace (AD103-275)").build());
            p3.addSpec(Specs.builder().name("CUDA Cores").description("8,448 CUDA Cores").build());
            p3.addSpec(Specs.builder().name("Memory").description("16GB GDDR6X (256-bit bus, 21 Gbps)").build());
            p3.addSpec(Specs.builder().name("Boost Clock").description("2640 MHz (OC Mode) / 2610 MHz (Default)").build());
            p3.addSpec(Specs.builder().name("Power Requirement").description("750W Recommended PSU (16-pin 12VHPWR)").build());

            p3.addFeature(Features.builder().featureName("Axial-tech Fans with Dual Ball Bearings").build());
            p3.addFeature(Features.builder().featureName("Vented Aluminum Exoskeleton Backplate").build());

            if (colombo != null) p3.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(12).build());
            if (galle != null) p3.addBranchInventory(BranchInventory.builder().branch(galle).quantity(6).build());
            if (matara != null) p3.addBranchInventory(BranchInventory.builder().branch(matara).quantity(4).build());
            if (kandy != null) p3.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(5).build());

            // 4. Corsair Dominator Titanium DDR5
            Product p4 = Product.builder()
                    .name("Corsair Dominator Titanium DDR5 64GB (2x32GB) 6600MHz")
                    .category(categoryRepository.findBySlug("components").orElse(null))
                    .brand(brandRepository.findBySlug("corsair").orElse(null))
                    .badge(badgeRepository.findBySlug("new").orElse(null))
                    .price(new BigDecimal("115000.00"))
                    .originalPrice(new BigDecimal("125000.00"))
                    .rating(new BigDecimal("5.0"))
                    .reviewsCount(21)
                    .description("Premium forged aluminum styling, patented DHX cooling, and 11 vibrant CAPELLIX RGB LEDs.")
                    .fullDescription("Corsair Dominator Titanium combines clean forged aluminum styling with precision memory performance and customizable top bar architecture.")
                    .sku("ETC-RAM-001")
                    .warranty("Lifetime Limited Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(4)
                    .build();

            p4.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p4.addSpec(Specs.builder().name("Memory Type").description("DDR5 UDIMM Desktop Memory").build());
            p4.addSpec(Specs.builder().name("Capacity").description("64GB Kit (2 x 32GB)").build());
            p4.addSpec(Specs.builder().name("Tested Speed").description("6600 MT/s (PC5-52800)").build());
            p4.addSpec(Specs.builder().name("Tested Latency").description("CL32-39-39-76 (1.40V)").build());
            p4.addSpec(Specs.builder().name("Performance Profile").description("Intel XMP 3.0 & AMD EXPO Ready").build());

            p4.addFeature(Features.builder().featureName("Patented Dual-Path DHX Cooling System").build());
            p4.addFeature(Features.builder().featureName("11 Addressable Ultra-Bright CAPELLIX LEDs").build());

            if (colombo != null) p4.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(15).build());
            if (galle != null) p4.addBranchInventory(BranchInventory.builder().branch(galle).quantity(8).build());
            if (matara != null) p4.addBranchInventory(BranchInventory.builder().branch(matara).quantity(6).build());
            if (kandy != null) p4.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(7).build());

            // 5. Logitech G PRO X SUPERLIGHT 2
            Product p5 = Product.builder()
                    .name("Logitech G PRO X SUPERLIGHT 2 Wireless Gaming Mouse")
                    .category(categoryRepository.findBySlug("peripherals").orElse(null))
                    .brand(brandRepository.findBySlug("logitech").orElse(null))
                    .badge(badgeRepository.findBySlug("bestseller").orElse(null))
                    .price(new BigDecimal("49500.00"))
                    .originalPrice(new BigDecimal("55000.00"))
                    .rating(new BigDecimal("4.9"))
                    .reviewsCount(142)
                    .description("Ultra-lightweight 60g wireless esports mouse with LIGHTFORCE hybrid optical-mechanical switches.")
                    .fullDescription("Engineered with the world's leading esports professionals. Features the HERO 2 sensor with sub-micron tracking up to 32,000 DPI and true 4,000Hz wireless polling rate via LIGHTSPEED.")
                    .sku("ETC-MOU-001")
                    .warranty("2-Year Official Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(10)
                    .build();

            p5.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p5.addSpec(Specs.builder().name("Sensor").description("HERO 2 Optical Sensor (100 - 32,000 DPI, 500+ IPS)").build());
            p5.addSpec(Specs.builder().name("Weight").description("60 grams Ultra-Lightweight").build());
            p5.addSpec(Specs.builder().name("Polling Rate").description("4,000Hz (0.25ms) Wireless via LIGHTSPEED").build());
            p5.addSpec(Specs.builder().name("Switches").description("LIGHTFORCE Hybrid Optical-Mechanical").build());
            p5.addSpec(Specs.builder().name("Battery Life").description("Up to 95 Hours Continuous Motion").build());

            p5.addFeature(Features.builder().featureName("Zero-Additive PTFE Glides for Ultra-Low Friction").build());
            p5.addFeature(Features.builder().featureName("POWERPLAY Wireless Charging Compatible").build());

            if (colombo != null) p5.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(25).build());
            if (galle != null) p5.addBranchInventory(BranchInventory.builder().branch(galle).quantity(14).build());
            if (matara != null) p5.addBranchInventory(BranchInventory.builder().branch(matara).quantity(10).build());
            if (kandy != null) p5.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(12).build());

            // 6. Razer Huntsman V3 Pro
            Product p6 = Product.builder()
                    .name("Razer Huntsman V3 Pro Analog Gaming Keyboard")
                    .category(categoryRepository.findBySlug("peripherals").orElse(null))
                    .brand(brandRepository.findBySlug("razer").orElse(null))
                    .badge(badgeRepository.findBySlug("toprated").orElse(null))
                    .price(new BigDecimal("78000.00"))
                    .originalPrice(new BigDecimal("85000.00"))
                    .rating(new BigDecimal("4.8"))
                    .reviewsCount(59)
                    .description("Gen-2 Analog Optical switches with Rapid Trigger and adjustable actuation from 0.1 to 4.0 mm.")
                    .fullDescription("Maximize responsiveness for competitive FPS gaming. Rapid Trigger mode allows instant keystroke reset without physical rebound for ultra-fast counter-strafing.")
                    .sku("ETC-KEY-001")
                    .warranty("2-Year Official Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(5)
                    .build();

            p6.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p6.addSpec(Specs.builder().name("Switch Type").description("Razer Gen-2 Analog Optical Switches").build());
            p6.addSpec(Specs.builder().name("Actuation Range").description("Adjustable from 0.1mm to 4.0mm").build());
            p6.addSpec(Specs.builder().name("Rapid Trigger").description("Instant Keystroke Reset from 0.1mm sensitivity").build());
            p6.addSpec(Specs.builder().name("Keycaps").description("Textured Doubleshot PBT Keycaps").build());
            p6.addSpec(Specs.builder().name("Top Plate").description("5052 Brushed Aluminum Top Plate").build());

            p6.addFeature(Features.builder().featureName("Multi-Function Digital Dial with 3 Dedicated Control Buttons").build());
            p6.addFeature(Features.builder().featureName("Magnetic Firm Leatherette Ergonomic Wrist Rest").build());

            if (colombo != null) p6.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(18).build());
            if (galle != null) p6.addBranchInventory(BranchInventory.builder().branch(galle).quantity(9).build());
            if (matara != null) p6.addBranchInventory(BranchInventory.builder().branch(matara).quantity(5).build());
            if (kandy != null) p6.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(8).build());

            // 7. ASUS ROG Swift OLED PG32UCDM
            Product p7 = Product.builder()
                    .name("ASUS ROG Swift OLED PG32UCDM 32\" 4K 240Hz")
                    .category(categoryRepository.findBySlug("monitors").orElse(null))
                    .brand(brandRepository.findBySlug("asus").orElse(null))
                    .badge(badgeRepository.findBySlug("hotdeal").orElse(null))
                    .price(new BigDecimal("420000.00"))
                    .originalPrice(new BigDecimal("450000.00"))
                    .rating(new BigDecimal("5.0"))
                    .reviewsCount(18)
                    .description("32-inch 4K QD-OLED gaming panel with 240Hz refresh rate and 0.03ms response time.")
                    .fullDescription("Featuring 3rd Gen QD-OLED technology, custom graphene heatsink, DisplayPort 1.4 (DSC), HDMI 2.1, and 90W USB-C Power Delivery.")
                    .sku("ETC-MON-001")
                    .warranty("3-Year OLED Burn-in Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(3)
                    .build();

            p7.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p7.addSpec(Specs.builder().name("Panel Type").description("31.5\" 4K (3840x2160) 3rd Gen QD-OLED").build());
            p7.addSpec(Specs.builder().name("Refresh Rate").description("240Hz Ultra-Fluid Refresh Rate").build());
            p7.addSpec(Specs.builder().name("Response Time").description("0.03ms (GtG) Lightning Response").build());
            p7.addSpec(Specs.builder().name("Color / HDR").description("99% DCI-P3, Delta E < 2, 1000 nits Peak HDR").build());
            p7.addSpec(Specs.builder().name("Connectivity").description("DP 1.4 (DSC), HDMI 2.1 x2, USB-C 90W PD, KVM Switch").build());

            p7.addFeature(Features.builder().featureName("Custom Graphene Heatsink & Clear Pixel Edge Tech").build());
            p7.addFeature(Features.builder().featureName("Uniform Brightness Mode to prevent ABL fluctuations").build());

            if (colombo != null) p7.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(5).build());
            if (galle != null) p7.addBranchInventory(BranchInventory.builder().branch(galle).quantity(2).build());
            if (kandy != null) p7.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(2).build());

            // 8. Samsung 990 PRO 2TB NVMe SSD
            Product p8 = Product.builder()
                    .name("Samsung 990 PRO 2TB NVMe M.2 PCIe Gen 4.0 SSD")
                    .category(categoryRepository.findBySlug("storage").orElse(null))
                    .brand(brandRepository.findBySlug("samsung").orElse(null))
                    .badge(badgeRepository.findBySlug("bestseller").orElse(null))
                    .price(new BigDecimal("68000.00"))
                    .originalPrice(new BigDecimal("74000.00"))
                    .rating(new BigDecimal("4.9"))
                    .reviewsCount(64)
                    .description("Blazing fast 7,450 MB/s sequential read performance engineered for hardcore gaming and heavy workloads.")
                    .fullDescription("Samsung 990 PRO reaches near max performance of PCIe 4.0. Featuring in-house Pascal controller, nickel coating for heat control, and intelligent thermal management.")
                    .sku("ETC-SSD-001")
                    .warranty("5-Year Official Limited Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(8)
                    .build();

            p8.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p8.addSpec(Specs.builder().name("Interface").description("PCIe Gen 4.0 x4, NVMe 2.0").build());
            p8.addSpec(Specs.builder().name("Form Factor").description("M.2 (2280) Single-Sided").build());
            p8.addSpec(Specs.builder().name("Sequential Read").description("Up to 7,450 MB/s").build());
            p8.addSpec(Specs.builder().name("Sequential Write").description("Up to 6,900 MB/s").build());
            p8.addSpec(Specs.builder().name("Endurance").description("1200 TBW (Terabytes Written)").build());

            p8.addFeature(Features.builder().featureName("Samsung Dynamic Thermal Guard & Magician Software").build());

            if (colombo != null) p8.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(20).build());
            if (galle != null) p8.addBranchInventory(BranchInventory.builder().branch(galle).quantity(10).build());
            if (matara != null) p8.addBranchInventory(BranchInventory.builder().branch(matara).quantity(8).build());
            if (kandy != null) p8.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(12).build());

            // 9. Intel Core i9-14900KS
            Product p9 = Product.builder()
                    .name("Intel Core i9-14900KS Special Edition Processor")
                    .category(categoryRepository.findBySlug("components").orElse(null))
                    .brand(brandRepository.findBySlug("intel").orElse(null))
                    .badge(badgeRepository.findBySlug("new").orElse(null))
                    .price(new BigDecimal("215000.00"))
                    .originalPrice(new BigDecimal("230000.00"))
                    .rating(new BigDecimal("4.9"))
                    .reviewsCount(29)
                    .description("The world's fastest desktop processor reaching unprecedented 6.2 GHz out-of-the-box clock speed.")
                    .fullDescription("Featuring 24 cores (8 Performance, 16 Efficient), 32 threads, and Intel Thermal Velocity Boost. Designed for extreme enthusiasts and world-record overclocking.")
                    .sku("ETC-CPU-001")
                    .warranty("3-Year Boxed Processor Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(4)
                    .build();

            p9.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p9.addSpec(Specs.builder().name("Cores / Threads").description("24 Cores (8P + 16E) / 32 Threads").build());
            p9.addSpec(Specs.builder().name("Max Turbo Clock").description("Up to 6.2 GHz with Thermal Velocity Boost").build());
            p9.addSpec(Specs.builder().name("Intel Smart Cache").description("36MB L3 Cache + 32MB L2 Cache").build());
            p9.addSpec(Specs.builder().name("Socket").description("LGA1700 (Intel 600 & 700 Series Chipsets)").build());
            p9.addSpec(Specs.builder().name("Base Power").description("150W Processor Base Power (253W Max Turbo)").build());

            p9.addFeature(Features.builder().featureName("Intel Application Optimization (APO) Supported").build());

            if (colombo != null) p9.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(8).build());
            if (galle != null) p9.addBranchInventory(BranchInventory.builder().branch(galle).quantity(3).build());
            if (kandy != null) p9.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(4).build());

            // 10. Lian Li O11 Dynamic EVO RGB Chassis
            Product p10 = Product.builder()
                    .name("Lian Li O11 Dynamic EVO RGB Dual-Chamber Chassis")
                    .category(categoryRepository.findBySlug("components").orElse(null))
                    .brand(brandRepository.findBySlug("lianli").orElse(null))
                    .badge(badgeRepository.findBySlug("bestseller").orElse(null))
                    .price(new BigDecimal("62000.00"))
                    .originalPrice(new BigDecimal("68000.00"))
                    .rating(new BigDecimal("4.8"))
                    .reviewsCount(44)
                    .description("Dual-chamber panoramic chassis with seamless L-shaped diffused ARGB lighting strips.")
                    .fullDescription("Offers dual-directional versatility (can be inverted to right-hand view), support for up to 3 x 420mm radiators, and pillar-less panoramic tempered glass design.")
                    .sku("ETC-CAS-001")
                    .warranty("1-Year Replacement Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(5)
                    .build();

            p10.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p10.addSpec(Specs.builder().name("Chassis Type").description("Dual-Chamber Mid-Tower Showcase").build());
            p10.addSpec(Specs.builder().name("Motherboard Support").description("E-ATX (under 280mm) / ATX / Micro-ATX / Mini-ITX").build());
            p10.addSpec(Specs.builder().name("Radiator Support").description("Up to 3x 420mm or 3x 360mm Radiators").build());
            p10.addSpec(Specs.builder().name("GPU Clearance").description("Up to 455.7mm Length, 169mm Height").build());

            p10.addFeature(Features.builder().featureName("Dual Diffused L-Shaped ARGB Lighting Strips").build());

            if (colombo != null) p10.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(10).build());
            if (galle != null) p10.addBranchInventory(BranchInventory.builder().branch(galle).quantity(4).build());
            if (matara != null) p10.addBranchInventory(BranchInventory.builder().branch(matara).quantity(3).build());
            if (kandy != null) p10.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(5).build());

            // 11. ASUS ROG Rapture GT-BE98 Wi-Fi 7 Router
            Product p11 = Product.builder()
                    .name("ASUS ROG Rapture GT-BE98 Quad-Band Wi-Fi 7 Gaming Router")
                    .category(categoryRepository.findBySlug("networking").orElse(null))
                    .brand(brandRepository.findBySlug("asus").orElse(null))
                    .badge(badgeRepository.findBySlug("new").orElse(null))
                    .price(new BigDecimal("185000.00"))
                    .originalPrice(new BigDecimal("199000.00"))
                    .rating(new BigDecimal("4.9"))
                    .reviewsCount(15)
                    .description("World's first quad-band Wi-Fi 7 gaming router delivering speeds up to 25 Gbps with dual 10G ports.")
                    .fullDescription("Featuring 320 MHz channels, 4096-QAM, Multi-Link Operation (MLO), triple-level game acceleration, and robust commercial-grade AiProtection network security.")
                    .sku("ETC-ROU-001")
                    .warranty("3-Year Official Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(3)
                    .build();

            p11.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p11.addSpec(Specs.builder().name("Network Standard").description("IEEE 802.11be Wi-Fi 7 (BE25000 Quad-Band)").build());
            p11.addSpec(Specs.builder().name("Data Rate").description("Up to 25 Gbps aggregate throughput").build());
            p11.addSpec(Specs.builder().name("Wired Ports").description("2x 10G Ports + 4x 2.5G Ports + 1x 1G Port").build());
            p11.addSpec(Specs.builder().name("Processor").description("2.6 GHz 64-bit Quad-Core CPU with 2GB DDR4 RAM").build());

            p11.addFeature(Features.builder().featureName("Multi-Link Operation (MLO) for zero packet loss").build());

            if (colombo != null) p11.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(6).build());
            if (kandy != null) p11.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(3).build());

            // 12. HyperX Cloud III Wireless Headset
            Product p12 = Product.builder()
                    .name("HyperX Cloud III Wireless Gaming Headset")
                    .category(categoryRepository.findBySlug("peripherals").orElse(null))
                    .brand(brandRepository.findBySlug("hyperx").orElse(null))
                    .badge(badgeRepository.findBySlug("bestseller").orElse(null))
                    .price(new BigDecimal("48000.00"))
                    .originalPrice(new BigDecimal("52000.00"))
                    .rating(new BigDecimal("4.8"))
                    .reviewsCount(51)
                    .description("Legendary comfort with 120-hour battery life, 53mm angled drivers, and DTS Headphone:X Spatial Audio.")
                    .fullDescription("The evolution of the legendary Cloud II. Re-engineered angled 53mm drivers tuned for optimal gaming audio, ultra-clear 10mm noise-canceling mic with internal mesh pop filter.")
                    .sku("ETC-HED-001")
                    .warranty("2-Year Official Warranty")
                    .alertEnabled(true)
                    .lowStockMargin(7)
                    .build();

            p12.addImage(ProductImage.builder().imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&auto=format&fit=crop&q=80").displayOrder(0).build());

            p12.addSpec(Specs.builder().name("Driver").description("Custom Dynamic 53mm with Neodymium Magnets").build());
            p12.addSpec(Specs.builder().name("Battery Life").description("Up to 120 Hours on Single Charge").build());
            p12.addSpec(Specs.builder().name("Wireless Type").description("2.4 GHz Ultra-Low Latency Wireless").build());
            p12.addSpec(Specs.builder().name("Spatial Audio").description("DTS Headphone:X Lifetime Spatial Audio").build());

            p12.addFeature(Features.builder().featureName("Signature HyperX Memory Foam Headband & Ear Cushions").build());

            if (colombo != null) p12.addBranchInventory(BranchInventory.builder().branch(colombo).quantity(15).build());
            if (galle != null) p12.addBranchInventory(BranchInventory.builder().branch(galle).quantity(8).build());
            if (matara != null) p12.addBranchInventory(BranchInventory.builder().branch(matara).quantity(5).build());
            if (kandy != null) p12.addBranchInventory(BranchInventory.builder().branch(kandy).quantity(8).build());

            productRepository.saveAll(List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12));
            log.info("Successfully seeded 12 sample products with accurate hardware specs and inventories!");
        }
    }

    private void seedBusinessProfile() {
        if (businessProfileRepository.count() == 0) {
            log.info("Seeding company business profile...");
            BusinessProfile profile = BusinessProfile.builder()
                    .id(1)
                    .storeName("ETech Computers (Pvt) Ltd")
                    .tagline("Sri Lanka's Premier Next-Gen High Performance Computing & Gaming Hub")
                    .registrationNo("PV-00249581")
                    .taxId("TIN-100294829-7000")
                    .isoCert("ISO 9001:2015 Certified")
                    .supportEmail("support@etechcomputers.lk")
                    .hotline("1330")
                    .headquarters("450 Galle Road, Kollupitiya, Colombo 03, Sri Lanka")
                    .workingHours("Mon - Fri: 09:00 AM - 07:00 PM | Sat - Sun: 10:00 AM - 05:00 PM")
                    .missionStatement("Empowering gamers, creators, and enterprises with the latest high-performance computing hardware.")
                    .companyStory("Founded in Colombo, ETech Computers has grown to be Sri Lanka's leading enthusiast computer hardware supplier.")
                    .build();
            businessProfileRepository.save(profile);
            log.info("Successfully seeded business profile!");
        }
    }

    private void seedPolicies() {
        if (legalPolicyRepository.count() == 0) {
            log.info("Seeding legal policies with standard frontend IDs...");
            LegalPolicy lp1 = LegalPolicy.builder()
                    .id("terms")
                    .title("Terms of Service")
                    .subtitle("Store terms, purchase agreements, order conditions, and customer service level agreements.")
                    .lastUpdated("January 2026")
                    .build();
            lp1.addPolicySection(PolicySections.builder()
                    .id("terms-1")
                    .sectionTitle("Order Fulfillment & Pricing")
                    .sectionContent("All hardware prices are listed in Sri Lankan Rupees (LKR) with applicable VAT included. Orders are subject to stock validation and branch warehouse confirmation.")
                    .bulletPoints("All listed prices are in Sri Lankan Rupees (LKR) and include applicable taxes | Pre-orders and custom workstation deposits require 50% advance confirmation | Typographical pricing errors will be rectified with immediate full refunds | Component reservations are held for a maximum of 24 hours")
                    .legalPolicy(lp1)
                    .build());
            lp1.addPolicySection(PolicySections.builder()
                    .id("terms-2")
                    .sectionTitle("Shipping & Delivery Guidelines")
                    .sectionContent("We offer nationwide secure delivery with end-to-end tracking.")
                    .bulletPoints("Standard courier dispatch takes 1 to 2 business days from the nearest warehouse | Pre-assembled custom PC builds undergo 24-hour thermal stress testing prior to dispatch | Fragile hardware packages are insured and packed with specialized anti-shock foam | Doorstep delivery pin coordinates confirmed via customer checkout map")
                    .legalPolicy(lp1)
                    .build());

            LegalPolicy lp2 = LegalPolicy.builder()
                    .id("warranty")
                    .title("Guarantee & Warranty Terms")
                    .subtitle("Official distributor manufacturer warranty policies, RMA claims process, replacement procedures, and SLA details.")
                    .lastUpdated("January 2026")
                    .build();
            lp2.addPolicySection(PolicySections.builder()
                    .id("warranty-1")
                    .sectionTitle("Manufacturer Warranty Coverage")
                    .sectionContent("All laptops, GPUs, motherboards, and monitors are backed by authentic manufacturer warranties ranging from 2 to 3 years.")
                    .bulletPoints("2-Year full hardware replacement warranty on custom workstation and gaming builds | Direct factory warranty tags with verifiable manufacturer serial numbers | 30-day rapid replacement policy on components with verified hardware defects | Zero service fees on warranty diagnosis conducted at any of our regional hubs")
                    .legalPolicy(lp2)
                    .build());
            lp2.addPolicySection(PolicySections.builder()
                    .id("warranty-2")
                    .sectionTitle("RMA Return & Claim Protocols")
                    .sectionContent("Hardware claims can be submitted at any of our regional branch hubs across Colombo, Galle, Matara, or Kandy for priority diagnosis.")
                    .bulletPoints("Standard 3 to 5 business day diagnosis window for hardware RMA claims | Loaner hardware units available for critical business workstation downtime | Transparent online ticket tracking from intake to manufacturer dispatch | Free return shipping once RMA repair or replacement is completed")
                    .legalPolicy(lp2)
                    .build());

            LegalPolicy lp3 = LegalPolicy.builder()
                    .id("privacy")
                    .title("Privacy Policy & Data Security")
                    .subtitle("User data security, cookie consent, tracking regulations, and GDPR/local privacy policy compliance.")
                    .lastUpdated("January 2026")
                    .build();

            lp3.addPolicySection(PolicySections.builder()
                    .id("privacy-1")
                    .sectionTitle("Information Collection")
                    .sectionContent("At ETech Computers, we prioritize customer privacy. When you browse or place an order from our hardware store, we collect minimal necessary details including:")
                    .bulletPoints("Account Registration Details: Full name, username, email address, phone number, and password hashes | Order & Delivery Information: Billing and shipping addresses required for fulfillment of computer rigs | Payment Processing: Encrypted transaction tokens without storing raw card numbers or CVV | Technical Diagnostics: Anonymized browser information, IP addresses, and device signatures")
                    .legalPolicy(lp3)
                    .build());

            lp3.addPolicySection(PolicySections.builder()
                    .id("privacy-2")
                    .sectionTitle("Data Protection Standards")
                    .sectionContent("We implement industry-standard AES-256 encryption and TLS 1.3 protocol standards across all store transactions.")
                    .bulletPoints("Customer accounts secured behind multi-layered database firewalls | Zero third-party data broker sharing or marketing monetization of personal data | Verified logistics courier sharing strictly for parcel delivery fulfillment | Full customer rights to request complete data export or account deletion")
                    .legalPolicy(lp3)
                    .build());

            LegalPolicy lp4 = LegalPolicy.builder()
                    .id("returns")
                    .title("Return & Refund Policy")
                    .subtitle("7-day return guarantee, replacement procedures, and hassle-free refunds.")
                    .lastUpdated("January 2026")
                    .build();

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-1")
                    .sectionTitle("7-Day Return Eligibility")
                    .sectionContent("We offer a 7-day hassle-free return window from the date of package delivery. To be eligible for a return:")
                    .bulletPoints("The hardware component or peripheral must be unused, sealed in its original anti-static packaging with all manufacturer accessories, manuals, and warranty barcodes intact | Proof of purchase (ETech Order ID, digital invoice, or registered email) must be presented | Open-box clearance items and digital software licenses are non-returnable unless defective on arrival")
                    .legalPolicy(lp4)
                    .build());

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-2")
                    .sectionTitle("Defective On Arrival (DOA) Claims")
                    .sectionContent("If your hardware arrives defective or cosmetically damaged in transit, report the issue within 48 hours of delivery. We will arrange express pickup and issue an immediate one-to-one replacement after initial technical inspection.")
                    .bulletPoints("Express courier pickup arranged within 24 hours of notification | Free diagnostic assessment at Colombo Technical Hub | Instant replacement provided if defect is verified on intake")
                    .legalPolicy(lp4)
                    .build());

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-3")
                    .sectionTitle("Refund Processing & Reimbursements")
                    .sectionContent("Once returned hardware is inspected at our Colombo Technical Center (typically 2-3 business days), refunds are processed via the original payment method:")
                    .bulletPoints("Credit/Debit Card Payments: Refund reflects within 3 to 5 business banking days | Bank Transfer / COD Orders: Direct transfer to customer's nominated Sri Lankan bank account within 48 hours | Store Credit: Instant credit voucher valid for 12 months on any component")
                    .legalPolicy(lp4)
                    .build());

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-4")
                    .sectionTitle("Restocking Conditions")
                    .sectionContent("Returns due to change-of-mind with unsealed packaging may be subject to a 10% restocking fee to cover anti-static recertification and testing costs.")
                    .bulletPoints("Applies only to unsealed non-defective components returned within 7 days | No restocking fees on manufacturer defect or transit damage returns | Components must pass complete diagnostic benchmarks before refund approval")
                    .legalPolicy(lp4)
                    .build());

            legalPolicyRepository.saveAll(List.of(lp1, lp2, lp3, lp4));
            log.info("Successfully seeded 4 legal policies with matching frontend IDs!");
        } else if (!legalPolicyRepository.existsById("returns")) {
            LegalPolicy lp4 = LegalPolicy.builder()
                    .id("returns")
                    .title("Return & Refund Policy")
                    .subtitle("7-day return guarantee, replacement procedures, and hassle-free refunds.")
                    .lastUpdated("January 2026")
                    .build();

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-1")
                    .sectionTitle("7-Day Return Eligibility")
                    .sectionContent("We offer a 7-day hassle-free return window from the date of package delivery. To be eligible for a return:")
                    .bulletPoints("The hardware component or peripheral must be unused, sealed in its original anti-static packaging with all manufacturer accessories, manuals, and warranty barcodes intact | Proof of purchase (ETech Order ID, digital invoice, or registered email) must be presented | Open-box clearance items and digital software licenses are non-returnable unless defective on arrival")
                    .legalPolicy(lp4)
                    .build());

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-2")
                    .sectionTitle("Defective On Arrival (DOA) Claims")
                    .sectionContent("If your hardware arrives defective or cosmetically damaged in transit, report the issue within 48 hours of delivery. We will arrange express pickup and issue an immediate one-to-one replacement after initial technical inspection.")
                    .bulletPoints("Express courier pickup arranged within 24 hours of notification | Free diagnostic assessment at Colombo Technical Hub | Instant replacement provided if defect is verified on intake")
                    .legalPolicy(lp4)
                    .build());

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-3")
                    .sectionTitle("Refund Processing & Reimbursements")
                    .sectionContent("Once returned hardware is inspected at our Colombo Technical Center (typically 2-3 business days), refunds are processed via the original payment method:")
                    .bulletPoints("Credit/Debit Card Payments: Refund reflects within 3 to 5 business banking days | Bank Transfer / COD Orders: Direct transfer to customer's nominated Sri Lankan bank account within 48 hours | Store Credit: Instant credit voucher valid for 12 months on any component")
                    .legalPolicy(lp4)
                    .build());

            lp4.addPolicySection(PolicySections.builder()
                    .id("returns-4")
                    .sectionTitle("Restocking Conditions")
                    .sectionContent("Returns due to change-of-mind with unsealed packaging may be subject to a 10% restocking fee to cover anti-static recertification and testing costs.")
                    .bulletPoints("Applies only to unsealed non-defective components returned within 7 days | No restocking fees on manufacturer defect or transit damage returns | Components must pass complete diagnostic benchmarks before refund approval")
                    .legalPolicy(lp4)
                    .build());

            legalPolicyRepository.save(lp4);
            log.info("Successfully added missing Return & Refund Policy (returns) to database!");
        }
    }

    private void seedPromotions() {
        if (homeDealBannerRepository.count() == 0) {
            log.info("Seeding home deal banner...");
            HomeDealBanner banner = HomeDealBanner.builder()
                    .id(1)
                    .dealTag("WEEKEND TECH BLOWOUT")
                    .heading("Next-Gen AI & Extreme Gaming Powerhouses")
                    .subtitle("Save up to 25% on ultra-high performance RTX 40-Series gaming laptops and OLED displays.")
                    .buttonText("Shop Weekend Deals")
                    .buttonUrl("#deals")
                    .durationSeconds(86400 * 3)
                    .isActive(true)
                    .build();
            homeDealBannerRepository.save(banner);
        }

        if (hotDealRepository.count() == 0) {
            log.info("Seeding hot deals...");
            Product rtxGpu = productRepository.findBySku("ETC-GPU-001").orElse(null);
            Product oledMon = productRepository.findBySku("ETC-MON-001").orElse(null);

            if (rtxGpu != null) {
                hotDealRepository.save(HotDeal.builder()
                        .product(rtxGpu)
                        .originalPrice(rtxGpu.getPrice())
                        .promoPrice(new BigDecimal("299999.00"))
                        .discountPercent(10)
                        .durationSeconds(86400 * 3)
                        .isActive(true)
                        .build());
            }
            if (oledMon != null) {
                hotDealRepository.save(HotDeal.builder()
                        .product(oledMon)
                        .originalPrice(oledMon.getPrice())
                        .promoPrice(new BigDecimal("389999.00"))
                        .discountPercent(12)
                        .durationSeconds(86400 * 4)
                        .isActive(true)
                        .build());
            }
        }

        if (dealBundleRepository.count() == 0) {
            log.info("Seeding deal bundles...");
            Product lap = productRepository.findBySku("ETC-LAP-001").orElse(null);
            Product mouse = productRepository.findBySku("ETC-MOU-001").orElse(null);
            Product kb = productRepository.findBySku("ETC-KEY-001").orElse(null);

            if (lap != null && mouse != null && kb != null) {
                DealBundle bundle = DealBundle.builder()
                        .title("Ultimate ROG RTX 4090 Battlestation Pro Bundle")
                        .subtitle("Flagship ROG SCAR 18 paired with Logitech Superlight 2 mouse and Razer Huntsman V3 Pro keyboard.")
                        .imageUrl("https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=800&auto=format&fit=crop&q=80")
                        .price(new BigDecimal("929999.00"))
                        .originalPrice(new BigDecimal("977499.00"))
                        .targetQuota(20)
                        .soldCount(4)
                        .durationSeconds(86400 * 5)
                        .isActive(true)
                        .build();

                bundle.addBundleItem(BundleItem.builder().product(lap).quantity(1).displayOrder(1).build());
                bundle.addBundleItem(BundleItem.builder().product(mouse).quantity(1).displayOrder(2).build());
                bundle.addBundleItem(BundleItem.builder().product(kb).quantity(1).displayOrder(3).build());

                dealBundleRepository.save(bundle);
                log.info("Successfully seeded deal bundle!");
            }
        }
    }

    private void seedNewsletterSubscribers() {
        if (newsletterSubscriberRepository.count() == 0) {
            log.info("Seeding newsletter subscribers...");
            List<NewsletterSubscriber> subscribers = List.of(
                    NewsletterSubscriber.builder().email("kasun.p@gmail.com").name("Kasun Perera").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("john.gamer@gmail.com").name("John Gamer").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("saman.tech@sltnet.lk").name("Saman Perera").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("dinesh.developer@gmail.com").name("Dinesh Developer").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("sarath.electronics@yahoo.com").name("Sarath Gunawardena").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("chaminda.v@gmail.com").name("Chaminda V").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("gaming.beast99@gmail.com").name("Gaming Beast").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("anushka.sen@hotmail.com").name("Anushka Sen").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("priyantha.k@gmail.com").name("Priyantha K").status(SubscriberStatus.SUBSCRIBED).build(),
                    NewsletterSubscriber.builder().email("techie.ravindu@outlook.com").name("Ravindu Jay").status(SubscriberStatus.SUBSCRIBED).build()
            );
            newsletterSubscriberRepository.saveAll(subscribers);
            log.info("Successfully seeded {} newsletter subscribers!", subscribers.size());
        }
    }

    private void seedNewsletterCampaigns() {
        if (newsletterCampaignRepository.count() == 0) {
            log.info("Seeding newsletter broadcast campaigns...");
            List<NewsletterCampaign> campaigns = List.of(
                    NewsletterCampaign.builder()
                            .id("camp_20260825_01")
                            .subject("Weekend Flash Deals - Save up to 25% on ROG & MSI Hardware")
                            .preheader("Exclusive limited-time price drops on flagship GPUs and OLED monitors.")
                            .category("FLASH_DEALS")
                            .targetSegment("ALL_ACTIVE")
                            .contentHtml("<h1>Weekend Flash Deals</h1><p>Enjoy incredible discounts across all flagship hardware this weekend at ETech!</p>")
                            .recipientsCount(10)
                            .status("DELIVERED")
                            .authorName("Store Admin")
                            .build(),
                    NewsletterCampaign.builder()
                            .id("camp_20260902_02")
                            .subject("Next-Gen Intel Core Ultra & NVIDIA RTX Super Arrivals")
                            .preheader("Explore cutting-edge enthusiast rigs and official partner hardware.")
                            .category("NEW_ARRIVALS")
                            .targetSegment("ALL_ACTIVE")
                            .contentHtml("<h1>New Arrivals at ETech</h1><p>The newest generation of AI PCs and enthusiast hardware has officially landed.</p>")
                            .recipientsCount(10)
                            .status("DELIVERED")
                            .authorName("Store Admin")
                            .build()
            );
            newsletterCampaignRepository.saveAll(campaigns);
            log.info("Successfully seeded {} newsletter campaigns!", campaigns.size());
        }
    }

    private void seedOrders() {
        if (orderRepository.count() == 0) {
            log.info("Seeding initial orders with order items...");

            User kasun = userRepository.findByUsername("kasun").orElse(null);
            Branch colombo = branchRepository.findById("BR-COL").orElse(null);
            Branch kandy = branchRepository.findById("BR-KAN").orElse(null);
            Branch galle = branchRepository.findById("BR-GAL").orElse(null);

            Product lap1 = productRepository.findBySku("ETC-LAP-001").orElse(null);
            Product mouse = productRepository.findBySku("ETC-MOU-001").orElse(null);
            Product gpu = productRepository.findBySku("ETC-GPU-001").orElse(null);
            Product ram = productRepository.findBySku("ETC-RAM-001").orElse(null);
            Product monitor = productRepository.findBySku("ETC-MON-001").orElse(null);

            if (colombo != null && lap1 != null && mouse != null) {
                // Order 1: Delivered
                Order o1 = Order.builder()
                        .orderCode("ORD-20260901-001")
                        .user(kasun)
                        .customerName(kasun != null ? kasun.getName() : "Kasun Perera")
                        .customerEmail(kasun != null ? kasun.getEmail() : "kasun.p@gmail.com")
                        .customerPhone("+94 77 123 4567")
                        .shippingAddress("125/4 Galle Road")
                        .city("Colombo")
                        .fulfillmentBranch(colombo)
                        .distanceKm(new BigDecimal("4.50"))
                        .subtotal(new BigDecimal("899499.00"))
                        .shippingFee(BigDecimal.ZERO)
                        .tax(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("899499.00"))
                        .status(OrderStatus.Delivered)
                        .paymentMethod("Credit / Debit Card")
                        .build();

                o1.addItem(OrderItem.builder()
                        .product(lap1)
                        .productName(lap1.getName())
                        .productSku(lap1.getSku())
                        .unitPrice(lap1.getPrice())
                        .quantity(1)
                        .totalPrice(lap1.getPrice())
                        .build());

                o1.addItem(OrderItem.builder()
                        .product(mouse)
                        .productName(mouse.getName())
                        .productSku(mouse.getSku())
                        .unitPrice(mouse.getPrice())
                        .quantity(1)
                        .totalPrice(mouse.getPrice())
                        .build());

                orderRepository.save(o1);
            }

            if (kandy != null && gpu != null && ram != null) {
                // Order 2: Processing
                Order o2 = Order.builder()
                        .orderCode("ORD-20260905-002")
                        .user(kasun)
                        .customerName(kasun != null ? kasun.getName() : "Kasun Perera")
                        .customerEmail(kasun != null ? kasun.getEmail() : "kasun.p@gmail.com")
                        .customerPhone("+94 77 123 4567")
                        .shippingAddress("42 Peradeniya Road")
                        .city("Kandy")
                        .fulfillmentBranch(kandy)
                        .distanceKm(new BigDecimal("8.20"))
                        .subtotal(new BigDecimal("440000.00"))
                        .shippingFee(new BigDecimal("1500.00"))
                        .tax(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("441500.00"))
                        .status(OrderStatus.Processing)
                        .paymentMethod("Bank Wire Transfer")
                        .build();

                o2.addItem(OrderItem.builder()
                        .product(gpu)
                        .productName(gpu.getName())
                        .productSku(gpu.getSku())
                        .unitPrice(gpu.getPrice())
                        .quantity(1)
                        .totalPrice(gpu.getPrice())
                        .build());

                o2.addItem(OrderItem.builder()
                        .product(ram)
                        .productName(ram.getName())
                        .productSku(ram.getSku())
                        .unitPrice(ram.getPrice())
                        .quantity(1)
                        .totalPrice(ram.getPrice())
                        .build());

                orderRepository.save(o2);
            }

            if (galle != null && monitor != null) {
                // Order 3: Pending (Guest order)
                Order o3 = Order.builder()
                        .orderCode("ORD-20260910-003")
                        .user(null)
                        .customerName("Nimal Silva")
                        .customerEmail("nimal.silva@outlook.com")
                        .customerPhone("+94 71 987 6543")
                        .shippingAddress("18 Matara Road")
                        .city("Galle")
                        .fulfillmentBranch(galle)
                        .distanceKm(new BigDecimal("12.00"))
                        .subtotal(new BigDecimal("420000.00"))
                        .shippingFee(new BigDecimal("2500.00"))
                        .tax(BigDecimal.ZERO)
                        .totalAmount(new BigDecimal("422500.00"))
                        .status(OrderStatus.Pending)
                        .paymentMethod("Cash on Delivery (COD)")
                        .build();

                o3.addItem(OrderItem.builder()
                        .product(monitor)
                        .productName(monitor.getName())
                        .productSku(monitor.getSku())
                        .unitPrice(monitor.getPrice())
                        .quantity(1)
                        .totalPrice(monitor.getPrice())
                        .build());

                orderRepository.save(o3);
            }

            log.info("Successfully seeded 3 sample orders with order items!");
        }
    }

    private void seedStockTransfers() {
        if (stockTransferRepository.count() == 0) {
            log.info("Seeding inter-branch stock transfers...");

            Branch colombo = branchRepository.findById("BR-COL").orElse(null);
            Branch kandy = branchRepository.findById("BR-KAN").orElse(null);
            Branch galle = branchRepository.findById("BR-GAL").orElse(null);
            Branch matara = branchRepository.findById("BR-MAT").orElse(null);

            Product lap1 = productRepository.findBySku("ETC-LAP-001").orElse(null);
            Product gpu = productRepository.findBySku("ETC-GPU-001").orElse(null);
            Product mouse = productRepository.findBySku("ETC-MOU-001").orElse(null);

            if (colombo != null && kandy != null && lap1 != null) {
                StockTransfer t1 = StockTransfer.builder()
                        .id("TRF-2026-001")
                        .product(lap1)
                        .fromBranch(colombo)
                        .toBranch(kandy)
                        .quantity(2)
                        .status(StockTransferStatus.RECEIVED)
                        .reason("High customer demand in Kandy region")
                        .initiatedBy("staff_colombo")
                        .notes("Transferred via secure express logistics. Accepted by Kandy branch manager.")
                        .build();
                stockTransferRepository.save(t1);
            }

            if (colombo != null && galle != null && gpu != null) {
                StockTransfer t2 = StockTransfer.builder()
                        .id("TRF-2026-002")
                        .product(gpu)
                        .fromBranch(colombo)
                        .toBranch(galle)
                        .quantity(3)
                        .status(StockTransferStatus.IN_TRANSIT)
                        .reason("Store stock replenishment")
                        .initiatedBy("staff_colombo")
                        .notes("In transit via ETech regional transit van.")
                        .build();
                stockTransferRepository.save(t2);
            }

            if (kandy != null && matara != null && mouse != null) {
                StockTransfer t3 = StockTransfer.builder()
                        .id("TRF-2026-003")
                        .product(mouse)
                        .fromBranch(kandy)
                        .toBranch(matara)
                        .quantity(5)
                        .status(StockTransferStatus.PENDING)
                        .reason("Southern showroom stock balance")
                        .initiatedBy("staff_colombo")
                        .notes("Awaiting dispatch scheduling.")
                        .build();
                stockTransferRepository.save(t3);
            }

            log.info("Successfully seeded 3 inter-branch stock transfers!");
        }
    }

    private void seedProductReviews() {
        if (productReviewRepository.count() == 0) {
            log.info("Seeding product reviews...");
            Product lap = productRepository.findBySku("ETC-LAP-001").orElse(null);
            User kasun = userRepository.findByUsername("kasun").orElse(null);

            if (lap != null) {
                ProductReview review = ProductReview.builder()
                        .id("REV-10001")
                        .product(lap)
                        .user(kasun)
                        .userName("Kasun Perera")
                        .userEmail(kasun != null ? kasun.getEmail() : "kasun.p@gmail.com")
                        .rating(5)
                        .comment("Unbelievable power! Handles 4K gaming and 3D rendering like a breeze. Truly best laptop in Sri Lanka.")
                        .build();
                productReviewRepository.save(review);
                log.info("Successfully seeded product review!");
            }
        }
    }
}
